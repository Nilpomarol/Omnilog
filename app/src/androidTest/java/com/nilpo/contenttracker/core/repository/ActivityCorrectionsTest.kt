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
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Corrections made from Activitat: deleting transitions and entries. */
@RunWith(AndroidJUnit4::class)
class ActivityCorrectionsTest {
    private lateinit var database: ContentTrackerDatabase
    private lateinit var dao: MediaDao
    private lateinit var repository: OfflineMediaRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ContentTrackerDatabase::class.java,
        ).build()
        dao = database.mediaDao()
        repository = OfflineMediaRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun deletingTheNewestVisibleTransitionRestoresTheStateItLeft() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "Paused")
        val pause = event(mediaId, sessionId, "InProgress", "Paused", at = 100)
        // Left behind when the resume between two pauses was deleted: a move into the state it left.
        event(mediaId, sessionId, "Paused", "Paused", at = 300)

        requireNotNull(repository.deleteSessionStatusEvent(pause))

        assertEquals("InProgress", requireNotNull(dao.getTrackingSession(sessionId)).status)
        assertTrue(dao.getSessionStatusEventsForSession(sessionId).isEmpty())
    }

    @Test
    fun theTransitionsOwnPreviousStatusWinsOverItsNeighbour() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "Dropped", finishedDay = 20_010)
        event(mediaId, sessionId, "InProgress", "Paused", at = 100)
        // The session was resumed without a logged row before this drop.
        val drop = event(mediaId, sessionId, "InProgress", "Dropped", at = 200)

        requireNotNull(repository.deleteSessionStatusEvent(drop))

        val session = requireNotNull(dao.getTrackingSession(sessionId))
        assertEquals("InProgress", session.status)
        assertNull(session.finishedAtEpochDay)
    }

    @Test
    fun deletingAnOlderTransitionLeavesTheSessionAlone() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "InProgress")
        val completion = event(mediaId, sessionId, "InProgress", "Completed", at = 100)
        event(mediaId, sessionId, "Completed", "InProgress", at = 200)

        requireNotNull(repository.deleteSessionStatusEvent(completion))

        assertEquals("InProgress", requireNotNull(dao.getTrackingSession(sessionId)).status)
        assertEquals(listOf("InProgress"), dao.getSessionStatusEventsForSession(sessionId).map { it.previousStatus })
    }

    @Test
    fun deletingAnEntryBelowTheTotalReopensACompletedSessionAndUndoRestoresIt() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "Completed", progress = 12, finishedDay = 20_010)
        val finalEntry = dao.insertProgressUpdate(
            ProgressUpdateEntity(mediaItemId = mediaId, sessionId = sessionId, amount = 12, loggedAtEpochDay = 20_010),
        )
        event(mediaId, sessionId, "InProgress", "Completed", at = 100)
        val before = requireNotNull(dao.getTrackingSession(sessionId))

        val recovery = requireNotNull(repository.deleteProgressUpdate(finalEntry))

        val reopened = requireNotNull(dao.getTrackingSession(sessionId))
        assertEquals("InProgress", reopened.status)
        assertNull(reopened.finishedAtEpochDay)
        val events = dao.getSessionStatusEventsForSession(sessionId)
        assertEquals(TrackingStatus.Completed.name, events.last().previousStatus)
        assertEquals(TrackingStatus.InProgress.name, events.last().status)

        assertTrue(repository.restoreDeletion(recovery))

        assertEquals(before, dao.getTrackingSession(sessionId))
        assertEquals(1, dao.getSessionStatusEventsForSession(sessionId).size)
        assertEquals(listOf(12), dao.getProgressUpdatesForSession(sessionId).map { it.amount })
    }

    @Test
    fun editingAnEntryThatStillReachesTheTotalKeepsTheCompletion() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "Completed", progress = 12, finishedDay = 20_010)
        val entry = dao.insertProgressUpdate(
            ProgressUpdateEntity(mediaItemId = mediaId, sessionId = sessionId, amount = 12, loggedAtEpochDay = 20_010),
        )

        repository.updateProgressUpdate(entry, amount = 14, loggedAt = null, coversPeriod = null)

        assertEquals("Completed", requireNotNull(dao.getTrackingSession(sessionId)).status)
    }

    @Test
    fun deletingTheStartReturnsToPlannedAndClearsTheDateItStamped() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "InProgress", startedDay = 20_000)
        val start = event(mediaId, sessionId, "Planned", "InProgress", at = 100)

        requireNotNull(repository.deleteSessionStatusEvent(start))

        val session = requireNotNull(dao.getTrackingSession(sessionId))
        assertEquals("Planned", session.status)
        assertNull(session.startedAtEpochDay)
    }

    @Test
    fun anEndingCanForgetItsDayAndTheSnapshotFollows() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "Completed", finishedDay = 20_000)
        val completion = event(mediaId, sessionId, "InProgress", "Completed", at = 100)

        assertTrue(repository.updateSessionStatusEventDate(completion, null))

        assertEquals(false, requireNotNull(dao.getSessionStatusEvent(completion)).hasKnownDate)
        assertNull(requireNotNull(dao.getTrackingSession(sessionId)).finishedAtEpochDay)
    }

    @Test
    fun aDayOutOfOrderWithDatedNeighboursIsRefusedButPlaceholdersDoNotBlock() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "InProgress")
        val pause = event(mediaId, sessionId, "InProgress", "Paused", at = 100)
        val resume = event(mediaId, sessionId, "Paused", "InProgress", at = 200)

        assertEquals(false, repository.updateSessionStatusEventDate(resume, java.time.LocalDate.ofEpochDay(19_990)))
        repository.updateSessionStatusEventDate(pause, null)
        assertTrue(repository.updateSessionStatusEventDate(resume, java.time.LocalDate.ofEpochDay(19_990)))
    }

    @Test
    fun startingFromPlannedDatesTheTransitionOnTheStartDate() = runBlocking {
        val (_, sessionId) = insertSession(status = "Planned")

        requireNotNull(
            repository.updateSessionDetails(
                sessionId, TrackingStatus.InProgress, 0, null, null,
                startedAt = java.time.LocalDate.ofEpochDay(19_990), finishedAt = null,
            ),
        )

        assertEquals(19_990L, dao.getSessionStatusEventsForSession(sessionId).single().occurredOnEpochDay)
    }

    @Test
    fun correctingTheStartDateMovesTheOpeningTransition() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "InProgress", startedDay = 20_000)
        val start = event(mediaId, sessionId, "Planned", "InProgress", at = 100)

        requireNotNull(
            repository.updateSessionDetails(
                sessionId, TrackingStatus.InProgress, 0, null, null,
                startedAt = java.time.LocalDate.ofEpochDay(19_995), finishedAt = null,
            ),
        )

        assertEquals(19_995L, requireNotNull(dao.getSessionStatusEvent(start)).occurredOnEpochDay)
    }

    @Test
    fun redatingTheOpeningTransitionMovesTheStartDateEvenBeforeIt() = runBlocking {
        val (mediaId, sessionId) = insertSession(status = "InProgress", startedDay = 20_000)
        val start = event(mediaId, sessionId, "Planned", "InProgress", at = 100, day = 20_001)

        assertTrue(repository.updateSessionStatusEventDate(start, java.time.LocalDate.ofEpochDay(19_998)))

        assertEquals(19_998L, requireNotNull(dao.getTrackingSession(sessionId)).startedAtEpochDay)
    }

    private suspend fun insertSession(
        status: String,
        progress: Int = 0,
        finishedDay: Long? = null,
        startedDay: Long? = null,
    ): Pair<Long, Long> {
        val mediaId = dao.insertMediaItem(
            MediaItemEntity(type = MediaType.Anime.name, title = "Title", progressTotal = 12),
        )
        val sessionId = dao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaId,
                sessionNumber = 1,
                status = status,
                progressCurrent = progress,
                finishedAtEpochDay = finishedDay,
                startedAtEpochDay = startedDay,
                updatedAtEpochMillis = 50,
            ),
        )
        return mediaId to sessionId
    }

    private suspend fun event(mediaId: Long, sessionId: Long, from: String, to: String, at: Long, day: Long = 20_000): Long =
        dao.insertSessionStatusEvent(
            SessionStatusEventEntity(
                mediaItemId = mediaId,
                sessionId = sessionId,
                previousStatus = from,
                status = to,
                createdAtEpochMillis = at,
                occurredOnEpochDay = day,
            ),
        )
}
