package com.nilpo.contenttracker.core.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The delete offered from the live-session editor: unlike [OfflineMediaRepository.deletePastSession],
 * it targets the latest session, and only when an earlier one survives to take its place.
 */
@RunWith(AndroidJUnit4::class)
class DeleteCurrentSessionTest {
    private lateinit var database: ContentTrackerDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ContentTrackerDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun deletingCurrentSessionHandsTheTitleBackToThePreviousOne() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val dao = database.mediaDao()
        val mediaId = insertMedia(dao)
        insertSession(dao, mediaId, number = 1, status = "Completed")
        val second = insertSession(dao, mediaId, number = 2, status = "Completed")
        val latest = insertSession(dao, mediaId, number = 3, status = "Planned")

        val recovery = repository.deleteCurrentSession(latest)

        assertNotNull(recovery)
        val remaining = dao.getTrackingSessions(mediaId)
        assertEquals(listOf(1, 2), remaining.map { it.sessionNumber }.sorted())
        assertEquals(second, remaining.maxByOrNull { it.sessionNumber }!!.id)
    }

    @Test
    fun deletedCurrentSessionCanBeRestoredWithItsHistory() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val dao = database.mediaDao()
        val mediaId = insertMedia(dao)
        insertSession(dao, mediaId, number = 1, status = "Completed")
        val latest = insertSession(dao, mediaId, number = 2, status = "InProgress", progress = 6)
        dao.insertProgressUpdate(
            ProgressUpdateEntity(
                mediaItemId = mediaId,
                sessionId = latest,
                amount = 6,
                loggedAtEpochDay = 20_001,
                createdAtEpochMillis = 123_457,
            ),
        )
        dao.insertSessionStatusEvent(
            SessionStatusEventEntity(
                mediaItemId = mediaId,
                sessionId = latest,
                previousStatus = "Planned",
                status = "InProgress",
                createdAtEpochMillis = 123_458,
                occurredOnEpochDay = 20_000,
            ),
        )

        val recovery = requireNotNull(repository.deleteCurrentSession(latest))
        assertNull(dao.getTrackingSession(latest))
        assertTrue(dao.getProgressUpdatesForSession(latest).isEmpty())

        assertTrue(repository.restoreDeletion(recovery))

        assertEquals(listOf(1, 2), dao.getTrackingSessions(mediaId).map { it.sessionNumber }.sorted())
        assertEquals(6, requireNotNull(dao.getTrackingSession(latest)).progressCurrent)
        assertEquals(listOf(6), dao.getProgressUpdatesForSession(latest).map { it.amount })
        assertEquals(1, dao.getSessionStatusEventsForSession(latest).size)
    }

    @Test
    fun refusesWhenItIsTheOnlySession() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val dao = database.mediaDao()
        val mediaId = insertMedia(dao)
        val only = insertSession(dao, mediaId, number = 1, status = "InProgress")

        assertNull(repository.deleteCurrentSession(only))
        assertNotNull(dao.getTrackingSession(only))
    }

    @Test
    fun refusesAnEarlierSessionSoOnlyTheLiveOneIsDeletable() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val dao = database.mediaDao()
        val mediaId = insertMedia(dao)
        val first = insertSession(dao, mediaId, number = 1, status = "Completed")
        insertSession(dao, mediaId, number = 2, status = "InProgress")

        assertNull(repository.deleteCurrentSession(first))
        assertEquals(2, dao.getTrackingSessions(mediaId).size)
        assertNotNull(dao.getTrackingSession(first))
    }

    private suspend fun insertMedia(dao: MediaDao): Long =
        dao.insertMediaItem(
            MediaItemEntity(
                type = MediaType.Anime.name,
                title = "Reread title",
                progressTotal = 12,
            ),
        )

    private suspend fun insertSession(
        dao: MediaDao,
        mediaId: Long,
        number: Int,
        status: String,
        progress: Int = 0,
    ): Long =
        dao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaId,
                sessionNumber = number,
                status = status,
                progressCurrent = progress,
                baselineProgress = 0,
                updatedAtEpochMillis = 123_456,
            ),
        )
}
