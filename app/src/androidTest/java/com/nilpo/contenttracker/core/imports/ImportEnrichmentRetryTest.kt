package com.nilpo.contenttracker.core.imports

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import com.nilpo.contenttracker.core.cover.CoverRepository
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.MetadataProviderHttpException
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.OfflineMediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportEnrichmentRetryTest {
    private lateinit var database: ContentTrackerDatabase
    private var enrichmentManager: ImportEnrichmentManager? = null

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ContentTrackerDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        enrichmentManager?.close()
        database.close()
    }

    @Test
    fun rateLimitStopsRetryingAndCompletesBatchWithIssuesAfterThreeAttempts() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mediaRepository = OfflineMediaRepository(database)
        val imported = mediaRepository.importMyAnimeListAccount(
            listOf(
                MyAnimeListImportItem(
                    malId = 5114,
                    title = "Fullmetal Alchemist: Brotherhood",
                    seriesType = "TV",
                    episodeTotal = 64,
                    watchedEpisodes = 0,
                    startedAt = null,
                    finishedAt = null,
                    rating = null,
                    status = TrackingStatus.Planned,
                    notes = null,
                    tags = emptyList(),
                ),
            ),
        )
        val batchId = requireNotNull(imported.importBatchId)
        val metadataRepository = RateLimitedMetadataRepository()
        val manager = ImportEnrichmentManager(
            context = context,
            importDao = database.importDao(),
            mediaDao = database.mediaDao(),
            mediaRepository = mediaRepository,
            metadataRepository = metadataRepository,
            coverRepository = CoverRepository(context, ImageLoader.Builder(context).build()),
        ).also { enrichmentManager = it }

        val first = manager.processBatch(batchId)
        val second = manager.processBatch(batchId)
        val third = manager.processBatch(batchId)

        assertTrue(first is ImportWorkerOutcome.Retry)
        assertEquals(240_000L, (first as ImportWorkerOutcome.Retry).delayMillis)
        assertTrue(second is ImportWorkerOutcome.Retry)
        assertEquals(ImportWorkerOutcome.Complete, third)
        assertEquals(ImportBatchState.CompletedWithIssues.name, database.importDao().getBatch(batchId)?.state)

        val item = database.importDao().observeItems().first().single()
        assertEquals(ImportItemState.Failed.name, item.state)
        assertEquals(3, item.attemptCount)
        assertTrue(item.retryable)
        assertEquals("Provider rate limit (HTTP 429)", item.lastError)

        assertEquals(batchId, database.importDao().retryIssueAndResumeBatch(item.id, System.currentTimeMillis()))
        assertEquals(ImportBatchState.Enriching.name, database.importDao().getBatch(batchId)?.state)
        val reset = database.importDao().getItem(item.id)
        assertEquals(ImportItemState.Pending.name, reset?.state)
        assertEquals(0, reset?.attemptCount)
        assertEquals(null, reset?.lastError)

        database.importDao().finishItem(
            itemId = item.id,
            state = ImportItemState.Failed.name,
            providerSource = null,
            providerExternalId = null,
            matchKind = null,
            candidateReferencesJson = null,
            retryable = false,
            lastError = "HTTP 403",
            now = System.currentTimeMillis(),
            completedAtEpochMillis = System.currentTimeMillis(),
        )
        assertEquals(null, database.importDao().retryIssueAndResumeBatch(item.id, System.currentTimeMillis()))
    }

    @Test
    fun incompleteAppliedItemCanBeRequeuedUntilUserDismissesItsCoverageGap() = runBlocking {
        val dao = database.importDao()
        val batchId = dao.insertBatch(
            ImportBatchEntity(
                source = ImportSource.StoryGraphCsv.name,
                state = ImportBatchState.Completed.name,
                totalCount = 1,
                createdAtEpochMillis = 100,
                updatedAtEpochMillis = 200,
                completedAtEpochMillis = 200,
            ),
        )
        val itemId = dao.insertItems(
            listOf(
                ImportBatchItemEntity(
                    batchId = batchId,
                    sourceKey = "storygraph:book-1",
                    mediaItemId = null,
                    state = ImportItemState.Applied.name,
                    attemptCount = 3,
                    retryable = true,
                    lastError = "old diagnostic",
                    updatedAtEpochMillis = 200,
                    completedAtEpochMillis = 200,
                ),
            ),
        ).single()

        assertEquals(batchId, dao.retryCoverageAndResumeBatch(itemId, now = 300))
        assertEquals(ImportBatchState.Enriching.name, dao.getBatch(batchId)?.state)
        val reset = dao.getItem(itemId)
        assertEquals(ImportItemState.Pending.name, reset?.state)
        assertEquals(0, reset?.attemptCount)
        assertEquals(false, reset?.retryable)
        assertEquals(null, reset?.lastError)
        assertEquals(null, reset?.completedAtEpochMillis)

        dao.finishItem(
            itemId = itemId,
            state = ImportItemState.Applied.name,
            providerSource = null,
            providerExternalId = null,
            matchKind = null,
            candidateReferencesJson = null,
            retryable = false,
            lastError = null,
            now = 400,
            completedAtEpochMillis = 400,
        )
        assertEquals(1, dao.dismissCoverage(itemId, now = 500))
        assertEquals(null, dao.retryCoverageAndResumeBatch(itemId, now = 600))
    }

    private class RateLimitedMetadataRepository : MetadataRepository {
        override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> = emptyList()

        override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
            throw MetadataProviderHttpException(
                statusCode = 429,
                requestUrl = "https://provider.test/anime/5114",
                retryAfterMillis = 240_000L,
            )
        }

        override suspend fun resolveImportedExternalId(
            source: MetadataSource,
            externalId: String,
            mediaType: MediaType,
        ): MetadataSuggestion? = null
    }
}
