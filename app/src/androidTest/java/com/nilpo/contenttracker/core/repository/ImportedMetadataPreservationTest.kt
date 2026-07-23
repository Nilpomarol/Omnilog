package com.nilpo.contenttracker.core.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportedMetadataPreservationTest {
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
    fun importedMetadataChangesOnlySelectedMetadataAndPreservesUserData() = runBlocking {
        val dao = database.mediaDao()
        val collectionId = dao.insertMediaCollection(MediaCollectionEntity(name = "Favourites"))
        val mediaId = dao.insertMediaItem(
            MediaItemEntity(
                type = MediaType.Anime.name,
                title = "Local title",
                collectionId = collectionId,
                collectionSortOrder = 7.5,
                progressTotal = 24,
                coverUrl = null,
                synopsis = null,
                malId = 5114,
                metadataOverrideFieldsCsv = MetadataRefreshField.Title.name,
                isOwned = true,
            ),
        )
        val sessionId = dao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaId,
                sessionNumber = 1,
                status = "InProgress",
                progressCurrent = 9,
                baselineProgress = 4,
                rating = 8,
                notes = "User review",
                startedAtEpochDay = 20_000,
                updatedAtEpochMillis = 123_456,
            ),
        )
        dao.insertProgressUpdate(
            ProgressUpdateEntity(
                mediaItemId = mediaId,
                sessionId = sessionId,
                amount = 5,
                loggedAtEpochDay = 20_001,
                createdAtEpochMillis = 123_457,
            ),
        )
        dao.insertSessionStatusEvent(
            SessionStatusEventEntity(
                mediaItemId = mediaId,
                sessionId = sessionId,
                previousStatus = "Planned",
                status = "InProgress",
                createdAtEpochMillis = 123_458,
                occurredOnEpochDay = 20_000,
            ),
        )
        val manualRatingId = dao.insertExternalRating(
            ExternalRatingEntity(
                mediaItemId = mediaId,
                source = "Manual",
                score = 9.0,
                maxScore = 10.0,
                origin = "Manual",
            ),
        )
        dao.updatePrimaryExternalRating(mediaId, 9.0, 10.0, null, manualRatingId)

        val sessionsBefore = dao.getTrackingSessions(mediaId)
        val progressBefore = dao.getProgressUpdates()
        val statusEventsBefore = dao.getSessionStatusEvents()
        val ratingsBefore = dao.getExternalRatingsForItem(mediaId)
        var outboundMalNotifications = 0
        val repository = OfflineMediaRepository(database) { outboundMalNotifications += 1 }
        val preview = MetadataRefreshPreview(
            mediaItemId = mediaId,
            refreshed = MetadataSuggestion(
                source = MetadataSource.Jikan,
                externalId = "5114",
                malId = 5114,
                mediaType = MediaType.Anime,
                title = "Provider title",
                progressTotal = 64,
                coverUrl = "https://example.test/cover.jpg",
                synopsis = "Provider synopsis",
                genres = listOf("Action", "Adventure"),
                sourceUrl = "https://myanimelist.net/anime/5114",
            ),
            changes = emptyList(),
        )

        val applied = repository.applyImportedMediaItemMetadataRefresh(
            preview = preview,
            selectedFields = setOf(
                MetadataRefreshField.Cover,
                MetadataRefreshField.Synopsis,
                MetadataRefreshField.Genres,
                MetadataRefreshField.SourceUrl,
            ),
        )

        assertTrue(applied)
        val mediaAfter = requireNotNull(dao.getMediaItem(mediaId))
        assertEquals("Local title", mediaAfter.title)
        assertEquals(24, mediaAfter.progressTotal)
        assertEquals("https://example.test/cover.jpg", mediaAfter.coverUrl)
        assertEquals("Provider synopsis", mediaAfter.synopsis)
        assertEquals("[\"Action\",\"Adventure\"]", mediaAfter.genresJson)
        assertEquals("https://myanimelist.net/anime/5114", mediaAfter.sourceUrl)
        assertEquals(collectionId, mediaAfter.collectionId)
        assertEquals(7.5, mediaAfter.collectionSortOrder)
        assertTrue(mediaAfter.isOwned)
        assertEquals(MetadataRefreshField.Title.name, mediaAfter.metadataOverrideFieldsCsv)
        assertEquals(manualRatingId, mediaAfter.primaryExternalRatingId)
        assertEquals(9.0, mediaAfter.externalRatingScore)

        assertEquals(sessionsBefore, dao.getTrackingSessions(mediaId))
        assertEquals(progressBefore, dao.getProgressUpdates())
        assertEquals(statusEventsBefore, dao.getSessionStatusEvents())
        assertEquals(ratingsBefore, dao.getExternalRatingsForItem(mediaId))
        assertFalse(dao.getMalSyncQueueItem(mediaId) != null)
        assertEquals(0, outboundMalNotifications)
    }

    @Test
    fun completedStoryGraphBookReceivesEnrichedPageTotalAsImportedBaseline() = runBlocking {
        val dao = database.mediaDao()
        val mediaId = dao.insertMediaItem(
            MediaItemEntity(
                type = MediaType.Book.name,
                title = "Imported book",
                progressTotal = null,
                metadataSource = MetadataSource.StoryGraph.name,
                metadataExternalId = "9780441478125",
                isOwned = true,
            ),
        )
        val sessionId = dao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaId,
                sessionNumber = 1,
                status = "Completed",
                progressCurrent = 0,
                baselineProgress = 0,
                rating = 9,
                notes = "Imported review",
                finishedAtEpochDay = 19_000,
                updatedAtEpochMillis = 123_456,
            ),
        )
        dao.insertSessionStatusEvent(
            SessionStatusEventEntity(
                mediaItemId = mediaId,
                sessionId = sessionId,
                previousStatus = "InProgress",
                status = "Completed",
                createdAtEpochMillis = 123_456,
                occurredOnEpochDay = 19_000,
            ),
        )
        val sessionBefore = requireNotNull(dao.getTrackingSession(sessionId))
        val statusEventsBefore = dao.getSessionStatusEventsForSession(sessionId)
        val repository = OfflineMediaRepository(database)
        val preview = MetadataRefreshPreview(
            mediaItemId = mediaId,
            refreshed = MetadataSuggestion(
                source = MetadataSource.GoogleBooks,
                externalId = "volume-1",
                mediaType = MediaType.Book,
                title = "Imported book",
                progressTotal = 304,
            ),
            changes = emptyList(),
        )

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                preview,
                setOf(MetadataRefreshField.ProgressTotal),
            ),
        )

        assertEquals(304, requireNotNull(dao.getMediaItem(mediaId)).progressTotal)
        assertEquals(
            sessionBefore.copy(progressCurrent = 304, baselineProgress = 304),
            dao.getTrackingSession(sessionId),
        )
        assertTrue(dao.getProgressUpdatesForSession(sessionId).isEmpty())
        assertEquals(statusEventsBefore, dao.getSessionStatusEventsForSession(sessionId))

        // A retry must also repair databases where an earlier build stored the total but left the
        // imported completed session at zero.
        assertEquals(1, dao.updateTrackingSession(sessionBefore))
        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                preview,
                setOf(MetadataRefreshField.ProgressTotal),
            ),
        )
        assertEquals(
            sessionBefore.copy(progressCurrent = 304, baselineProgress = 304),
            dao.getTrackingSession(sessionId),
        )
        assertTrue(dao.getProgressUpdatesForSession(sessionId).isEmpty())
        assertEquals(statusEventsBefore, dao.getSessionStatusEventsForSession(sessionId))

        // Manual retry also repairs this partial state without another successful provider response.
        assertEquals(1, dao.updateTrackingSession(sessionBefore))
        val importDao = database.importDao()
        val batchId = importDao.insertBatch(
            ImportBatchEntity(
                source = "StoryGraphCsv",
                state = "CompletedWithIssues",
                totalCount = 1,
                createdAtEpochMillis = 123_456,
                updatedAtEpochMillis = 123_456,
            ),
        )
        importDao.insertItems(
            listOf(
                ImportBatchItemEntity(
                    batchId = batchId,
                    sourceKey = "isbn:9780441478125",
                    sourceExternalId = "9780441478125",
                    mediaItemId = mediaId,
                    state = "Applied",
                    updatedAtEpochMillis = 123_456,
                ),
            ),
        )
        assertEquals(1, dao.reconcileCompletedImportedBookProgressForBatch(batchId))
        assertEquals(
            sessionBefore.copy(progressCurrent = 304, baselineProgress = 304),
            dao.getTrackingSession(sessionId),
        )
        assertTrue(dao.getProgressUpdatesForSession(sessionId).isEmpty())
        assertEquals(statusEventsBefore, dao.getSessionStatusEventsForSession(sessionId))
    }
}
