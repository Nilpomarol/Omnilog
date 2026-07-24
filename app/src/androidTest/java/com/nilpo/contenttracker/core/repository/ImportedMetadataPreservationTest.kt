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
import com.nilpo.contenttracker.core.mal.buildMalSyncPayload
import com.nilpo.contenttracker.core.mal.toStagedMalImportItem
import com.nilpo.contenttracker.core.mal.toStagingJson
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

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
    fun malAccountPageCheckpointSurvivesReloadAndCanBeDiscarded() = runBlocking {
        val importDao = database.importDao()
        val now = 123_456L
        val batchId = importDao.insertBatch(
            ImportBatchEntity(
                source = "MalApi",
                state = "Previewing",
                totalCount = 0,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        val item = MyAnimeListImportItem(
            malId = 5114,
            title = "Fullmetal Alchemist: Brotherhood",
            seriesType = "TV",
            episodeTotal = 64,
            watchedEpisodes = 64,
            startedAt = LocalDate.of(2020, 1, 2),
            finishedAt = LocalDate.of(2020, 3, 4),
            rating = 10,
            status = TrackingStatus.Completed,
            notes = "Imported",
            tags = listOf("favorite"),
        )

        importDao.saveMalAccountStagingPage(
            batchId = batchId,
            items = listOf(
                ImportBatchItemEntity(
                    batchId = batchId,
                    sourceKey = "mal:5114",
                    sourceExternalId = "5114",
                    normalizedPayloadJson = item.toStagingJson(),
                    state = "Staged",
                    updatedAtEpochMillis = now,
                ),
            ),
            totalCount = 1,
            continuationUrl = "https://api.myanimelist.net/v2/users/@me/animelist?offset=100",
            now = now,
        )

        val reloadedBatch = requireNotNull(importDao.getMalAccountStagingBatch())
        val reloadedItem = importDao.getItemsForBatch(reloadedBatch.id)
            .single()
            .normalizedPayloadJson
            ?.toStagedMalImportItem()
        assertEquals("Previewing", reloadedBatch.state)
        assertEquals(1, reloadedBatch.totalCount)
        assertEquals(item, reloadedItem)

        importDao.saveMalAccountStagingPage(
            batchId = batchId,
            items = emptyList(),
            totalCount = 1,
            continuationUrl = null,
            now = now + 1,
        )
        assertEquals("ReadyToImport", requireNotNull(importDao.getMalAccountStagingBatch()).state)
        assertEquals(1, importDao.deleteMalAccountStagingBatches())
        assertEquals(null, importDao.getMalAccountStagingBatch())
    }

    @Test
    fun providerIdentitySurvivesEnrichmentAndHistoryDeletion() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val importDao = database.importDao()
        val mediaDao = database.mediaDao()

        val imdbCsv = """Title,Title Type,Const
            Original IMDb title,Movie,tt1375666
        """.trimIndent()
        val imdbImport = repository.importPreparedImdbCsv(repository.prepareImdbCsv(imdbCsv))
        val imdbMediaId = imdbImport.importedMediaItemIds.single()
        val imdbBatchId = requireNotNull(imdbImport.importBatchId)
        assertEquals("tt1375666", requireNotNull(mediaDao.getMediaItem(imdbMediaId)).imdbId)

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                MetadataRefreshPreview(
                    mediaItemId = imdbMediaId,
                    refreshed = MetadataSuggestion(
                        source = MetadataSource.Tmdb,
                        externalId = "27205",
                        mediaType = MediaType.Movie,
                        title = "Enriched IMDb title",
                    ),
                    changes = emptyList(),
                ),
                emptySet(),
            ),
        )
        importDao.updateBatchState(imdbBatchId, "Completed", null, 2L, 2L)
        assertEquals(1, importDao.deleteFinishedBatch(imdbBatchId))
        assertEquals(MetadataSource.Tmdb.name, requireNotNull(mediaDao.getMediaItem(imdbMediaId)).metadataSource)
        assertEquals(0, repository.previewImdbCsv(imdbCsv).importableRows)
        assertEquals(1, repository.previewImdbCsv(imdbCsv).skippedDuplicateRows)

        val storyGraphCsv = """Title,Authors,Read Status,ISBN/UID
            Original book,Writer,read,9780441478125
        """.trimIndent()
        val storyGraphImport = repository.importPreparedStoryGraphCsv(
            repository.prepareStoryGraphCsv(storyGraphCsv),
        )
        val storyGraphMediaId = storyGraphImport.importedMediaItemIds.single()
        val storyGraphBatchId = requireNotNull(storyGraphImport.importBatchId)
        assertEquals(
            "9780441478125",
            requireNotNull(mediaDao.getMediaItem(storyGraphMediaId)).storyGraphId,
        )

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                MetadataRefreshPreview(
                    mediaItemId = storyGraphMediaId,
                    refreshed = MetadataSuggestion(
                        source = MetadataSource.GoogleBooks,
                        externalId = "volume-1",
                        mediaType = MediaType.Book,
                        title = "Enriched book title",
                    ),
                    changes = emptyList(),
                ),
                emptySet(),
            ),
        )
        importDao.updateBatchState(storyGraphBatchId, "Completed", null, 3L, 3L)
        assertEquals(1, importDao.deleteFinishedBatch(storyGraphBatchId))
        assertEquals(
            MetadataSource.GoogleBooks.name,
            requireNotNull(mediaDao.getMediaItem(storyGraphMediaId)).metadataSource,
        )
        assertEquals(0, repository.previewStoryGraphCsv(storyGraphCsv).importableRows)
        assertEquals(1, repository.previewStoryGraphCsv(storyGraphCsv).skippedDuplicateRows)

        val backup = repository.exportBackupJson()
        repository.importBackupJson(backup)
        assertEquals("tt1375666", requireNotNull(mediaDao.getMediaItem(imdbMediaId)).imdbId)
        assertEquals(
            "9780441478125",
            requireNotNull(mediaDao.getMediaItem(storyGraphMediaId)).storyGraphId,
        )
        assertEquals(0, repository.previewImdbCsv(imdbCsv).importableRows)
        assertEquals(0, repository.previewStoryGraphCsv(storyGraphCsv).importableRows)
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

    @Test
    fun completedImdbSeriesReceivesEnrichedEpisodeTotalAsImportedBaseline() = runBlocking {
        val dao = database.mediaDao()
        val mediaId = dao.insertMediaItem(
            MediaItemEntity(
                type = MediaType.TvShow.name,
                title = "Imported series",
                progressTotal = null,
                metadataSource = MetadataSource.Imdb.name,
                metadataExternalId = "tt10048342",
            ),
        )
        val sessionId = dao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaId,
                sessionNumber = 1,
                status = "Completed",
                progressCurrent = 0,
                baselineProgress = 0,
                rating = 8,
                finishedAtEpochDay = 19_500,
                updatedAtEpochMillis = 123_456,
            ),
        )
        val repository = OfflineMediaRepository(database)
        val preview = MetadataRefreshPreview(
            mediaItemId = mediaId,
            refreshed = MetadataSuggestion(
                source = MetadataSource.Tmdb,
                externalId = "87739",
                mediaType = MediaType.TvShow,
                title = "Imported series",
                progressTotal = 7,
            ),
            changes = emptyList(),
        )

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                preview,
                setOf(MetadataRefreshField.ProgressTotal),
            ),
        )

        assertEquals(7, requireNotNull(dao.getMediaItem(mediaId)).progressTotal)
        assertEquals(7, requireNotNull(dao.getTrackingSession(sessionId)).progressCurrent)
        assertEquals(7, requireNotNull(dao.getTrackingSession(sessionId)).baselineProgress)
        assertTrue(dao.getProgressUpdatesForSession(sessionId).isEmpty())
    }

    @Test
    fun storyGraphRereadsBecomeSeparateSessionsWithoutUsingDateAdded() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val result = repository.importStoryGraphCsv(
            """Title,Authors,ISBN/UID,Format,Read Status,Date Added,Last Date Read,Dates Read,Read Count,Star Rating,Review,Tags,Owned?
                A Reread,Writer,9780441478125,paperback,read,2019/01/01,2024/03/12,"2020/02/01-2020/02/10, 2024/03/01-2024/03/12",2,4.5,Latest review,,yes
            """.trimIndent(),
        )
        val mediaId = result.importedMediaItemIds.single()
        val sessions = database.mediaDao().getTrackingSessions(mediaId)

        assertEquals(2, sessions.size)
        assertEquals(listOf(1, 2), sessions.map(TrackingSessionEntity::sessionNumber))
        assertEquals(listOf("Completed", "Completed"), sessions.map(TrackingSessionEntity::status))
        assertEquals(LocalDate.of(2020, 2, 1).toEpochDay(), sessions[0].startedAtEpochDay)
        assertEquals(LocalDate.of(2020, 2, 10).toEpochDay(), sessions[0].finishedAtEpochDay)
        assertEquals(LocalDate.of(2024, 3, 1).toEpochDay(), sessions[1].startedAtEpochDay)
        assertEquals(LocalDate.of(2024, 3, 12).toEpochDay(), sessions[1].finishedAtEpochDay)
        assertEquals(null, sessions[0].rating)
        assertEquals(9, sessions[1].rating)
        assertEquals("Latest review", sessions[1].notes)
        assertFalse(sessions.any { it.startedAtEpochDay == LocalDate.of(2019, 1, 1).toEpochDay() })
    }

    @Test
    fun storyGraphReimportRemainsDuplicateAfterEnrichmentReplacesProviderIdentity() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val formattedCsv =
            """Title,Authors,ISBN/UID,Format,Read Status,Read Count
                Imported Book,Writer,978-0-441-47812-5,paperback,read,1
            """.trimIndent()
        val imported = repository.importStoryGraphCsv(formattedCsv)
        val mediaId = imported.importedMediaItemIds.single()

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                MetadataRefreshPreview(
                    mediaItemId = mediaId,
                    refreshed = MetadataSuggestion(
                        source = MetadataSource.OpenLibrary,
                        externalId = "/works/OL1W",
                        mediaType = MediaType.Book,
                        title = "Imported Book",
                    ),
                    changes = emptyList(),
                ),
                selectedFields = emptySet(),
            ),
        )
        assertEquals(MetadataSource.OpenLibrary.name, database.mediaDao().getMediaItem(mediaId)?.metadataSource)

        val compactCsv = formattedCsv.replace("978-0-441-47812-5", "9780441478125")
        val preview = repository.previewStoryGraphCsv(compactCsv)
        val repeatedImport = repository.importStoryGraphCsv(compactCsv)

        assertEquals(0, preview.importableRows)
        assertEquals(1, preview.skippedDuplicateRows)
        assertEquals(0, repeatedImport.importedRows)
        assertEquals(1, repeatedImport.skippedDuplicateRows)
        assertEquals(1, database.mediaDao().getMediaItems().size)
    }

    @Test
    fun imdbReimportRemainsDuplicateAfterEnrichmentChangesIdentityTitleAndYear() = runBlocking {
        val repository = OfflineMediaRepository(database)
        val csv =
            """Const,Your Rating,Date Rated,Title,Title Type,IMDb Rating,Runtime (mins),Year
                tt1375666,9,2024-01-31,Imported Movie,Movie,8.8,148,2010
            """.trimIndent()
        val imported = repository.importImdbCsv(csv)
        val mediaId = imported.importedMediaItemIds.single()

        assertTrue(
            repository.applyImportedMediaItemMetadataRefresh(
                MetadataRefreshPreview(
                    mediaItemId = mediaId,
                    refreshed = MetadataSuggestion(
                        source = MetadataSource.Tmdb,
                        externalId = "27205",
                        mediaType = MediaType.Movie,
                        title = "Enriched Localized Title",
                        releaseYear = 2011,
                    ),
                    changes = emptyList(),
                ),
                selectedFields = setOf(MetadataRefreshField.Title, MetadataRefreshField.ReleaseYear),
            ),
        )
        val enrichedItem = requireNotNull(database.mediaDao().getMediaItem(mediaId))
        assertEquals(MetadataSource.Tmdb.name, enrichedItem.metadataSource)
        assertEquals("Enriched Localized Title", enrichedItem.title)
        assertEquals(2011, enrichedItem.releaseYear)

        val preview = repository.previewImdbCsv(csv.replace("tt1375666", " TT1375666 "))
        val repeatedImport = repository.importImdbCsv(csv)

        assertEquals(0, preview.importableRows)
        assertEquals(1, preview.skippedDuplicateRows)
        assertEquals(0, repeatedImport.importedRows)
        assertEquals(1, repeatedImport.skippedDuplicateRows)
        assertEquals(1, database.mediaDao().getMediaItems().size)
    }

    @Test
    fun malCompletedRewatchesBecomeSeparateSessionsWithoutOutboundSync() = runBlocking {
        var outboundMalNotifications = 0
        val repository = OfflineMediaRepository(database) { outboundMalNotifications += 1 }
        val result = repository.importMyAnimeListAccount(
            listOf(
                MyAnimeListImportItem(
                    malId = 5114,
                    title = "Repeated anime",
                    seriesType = "TV",
                    episodeTotal = 12,
                    watchedEpisodes = 12,
                    startedAt = LocalDate.of(2024, 1, 2),
                    finishedAt = LocalDate.of(2024, 1, 3),
                    rating = 9,
                    status = TrackingStatus.Completed,
                    notes = "Latest watch",
                    tags = listOf("favorite", "rewatch"),
                    completedRewatches = 2,
                ),
            ),
        )
        val mediaId = result.importedMediaItemIds.single()
        val sessions = database.mediaDao().getTrackingSessions(mediaId)

        assertTrue(result.importBatchId != null)
        assertEquals(3, sessions.size)
        assertEquals(listOf(1, 2, 3), sessions.map(TrackingSessionEntity::sessionNumber))
        assertTrue(sessions.all { it.status == TrackingStatus.Completed.name })
        assertEquals(listOf(12, 12, 12), sessions.map(TrackingSessionEntity::progressCurrent))
        assertEquals(listOf(12, 12, 12), sessions.map(TrackingSessionEntity::baselineProgress))
        assertEquals(listOf(null, null, 9), sessions.map(TrackingSessionEntity::rating))
        assertEquals("Latest watch", sessions.last().notes)
        assertEquals(LocalDate.of(2024, 1, 2).toEpochDay(), sessions.last().startedAtEpochDay)
        assertEquals(LocalDate.of(2024, 1, 3).toEpochDay(), sessions.last().finishedAtEpochDay)
        assertEquals(0, outboundMalNotifications)

        val importedItem = requireNotNull(database.mediaDao().getMediaItem(mediaId))
        assertEquals(null, importedItem.genresJson)
        assertEquals("[\"favorite\",\"rewatch\"]", importedItem.tagsJson)

        val roundTrip = requireNotNull(
            buildMalSyncPayload(importedItem, sessions),
        )
        assertEquals(2, roundTrip.completedRewatches)
        assertFalse(roundTrip.isRewatching)
        assertEquals(listOf("favorite", "rewatch"), roundTrip.tags)
    }

    @Test
    fun providerImportRollsBackMediaAndBatchWhenAnyRowFails() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_test_import
            BEFORE INSERT ON media_items
            WHEN NEW.title = 'Fail import'
            BEGIN
                SELECT RAISE(ABORT, 'forced import failure');
            END
            """.trimIndent(),
        )
        val repository = OfflineMediaRepository(database)
        fun item(malId: Int, title: String) = MyAnimeListImportItem(
            malId = malId,
            title = title,
            seriesType = "TV",
            episodeTotal = 12,
            watchedEpisodes = 0,
            startedAt = null,
            finishedAt = null,
            rating = null,
            status = TrackingStatus.Planned,
            notes = null,
            tags = emptyList(),
        )

        val failure = runCatching {
            repository.importMyAnimeListAccount(
                listOf(item(1, "Inserted first"), item(2, "Fail import")),
            )
        }.exceptionOrNull()

        assertTrue(failure != null)
        assertTrue(database.mediaDao().getMediaItems().isEmpty())
        assertTrue(database.importDao().getResumableBatches().isEmpty())
    }
}
