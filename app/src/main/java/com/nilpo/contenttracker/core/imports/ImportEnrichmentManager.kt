package com.nilpo.contenttracker.core.imports

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nilpo.contenttracker.ContentTrackerApplication
import com.nilpo.contenttracker.core.cover.CoverRepository
import com.nilpo.contenttracker.core.database.dao.ImportDao
import com.nilpo.contenttracker.core.database.dao.ImportCoverageRow
import com.nilpo.contenttracker.core.database.dao.ImportIssueRow
import com.nilpo.contenttracker.core.database.dao.ImportReviewRow
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataProviderHttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class ImportBatchProgress(
    val batchId: Long,
    val source: ImportSource,
    val state: ImportBatchState,
    val totalCount: Int,
    val appliedCount: Int,
    val needsReviewCount: Int,
    val issueCount: Int,
    val retryableIssueCount: Int,
    val coverageGapCount: Int,
    val pendingCount: Int,
    val cancelledCount: Int,
    val createdAtEpochMillis: Long = 0L,
    val completedAtEpochMillis: Long? = null,
) {
    val processedCount: Int
        get() = (totalCount - pendingCount).coerceIn(0, totalCount)
}

data class ImportEnrichmentState(
    val activeBatch: ImportBatchProgress? = null,
    val recentBatches: List<ImportBatchProgress> = emptyList(),
    val historyBatches: List<ImportBatchProgress> = emptyList(),
    val reviewItems: List<ImportReviewItem> = emptyList(),
    val issueItems: List<ImportIssueItem> = emptyList(),
    val coverageItems: List<ImportCoverageItem> = emptyList(),
    val pendingCompletion: ImportCompletionSummary? = null,
)

data class ImportCompletionSummary(
    val batchId: Long,
    val source: ImportSource,
    val totalCount: Int,
    val processedCount: Int,
    val appliedCount: Int,
    val needsReviewCount: Int,
    val issueCount: Int,
    val coverageGapCount: Int,
) {
    val needsAttention: Boolean
        get() = needsReviewCount > 0 || issueCount > 0 || coverageGapCount > 0
}

internal fun ImportBatchProgress.hasRemainingImportAction(): Boolean =
    state != ImportBatchState.Cancelled &&
        (state != ImportBatchState.Completed || coverageGapCount > 0)

internal fun ImportBatchProgress.canDeleteFromHistory(): Boolean =
    state in setOf(ImportBatchState.Completed, ImportBatchState.Cancelled) &&
        !hasRemainingImportAction()

data class ImportReviewItem(
    val itemId: Long,
    val batchId: Long,
    val mediaItemId: Long,
    val title: String,
    val mediaType: MediaType,
    val matchKind: String?,
    val selectedReference: ProviderReference?,
    val candidates: List<ProviderReference>,
)

data class ImportIssueItem(
    val itemId: Long,
    val batchId: Long,
    val mediaItemId: Long,
    val title: String,
    val mediaType: MediaType,
    val importSource: ImportSource,
    val state: ImportItemState,
    val retryable: Boolean,
    val attemptCount: Int,
    val lastError: String?,
) {
    val canRetry: Boolean
        get() = state == ImportItemState.NoMatch ||
            state == ImportItemState.Unavailable ||
            (state == ImportItemState.Failed && retryable)
}

enum class ImportMetadataGap {
    Cover,
    Synopsis,
    ReleaseYear,
    Creators,
    Genres,
    ProgressTotal,
}

data class ImportCoverageItem(
    val itemId: Long,
    val batchId: Long,
    val mediaItemId: Long,
    val title: String,
    val mediaType: MediaType,
    val importSource: ImportSource,
    val missingFields: Set<ImportMetadataGap>,
)

data class ImportReviewDraft(
    val itemId: Long,
    val batchId: Long,
    val title: String,
    val reference: ProviderReference,
    val preview: MetadataRefreshPreview,
)

sealed interface ImportReviewApplyOutcome {
    data object Applied : ImportReviewApplyOutcome
    data class Changed(val draft: ImportReviewDraft) : ImportReviewApplyOutcome
}

sealed interface ImportWorkerOutcome {
    data object Complete : ImportWorkerOutcome
    data object MoreWork : ImportWorkerOutcome
    data class Retry(val delayMillis: Long) : ImportWorkerOutcome
}

/** Coordinates durable post-import metadata resolution and safe field application. */
class ImportEnrichmentManager(
    private val context: Context,
    private val importDao: ImportDao,
    private val mediaDao: MediaDao,
    private val mediaRepository: MediaRepository,
    private val metadataRepository: MetadataRepository,
    private val coverRepository: CoverRepository,
    private val animeTitlePreference: () -> AnimeTitlePreference = {
        AnimeTitlePreference.EnglishWithJapaneseOriginal
    },
    private val resolver: ImportedMetadataResolver = ImportedMetadataResolver(metadataRepository),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<ImportEnrichmentState> = combine(
        importDao.observeBatches(),
        importDao.observeItems(),
        importDao.observeReviewItems(),
        importDao.observeIssueItems(),
        importDao.observeCoverageItems(),
    ) { batches, items, reviewRows, issueRows, coverageRows ->
        val coverageItems = coverageRows.mapNotNull(ImportCoverageRow::toCoverageItem)
        val progress = batches.mapNotNull { batch ->
            batch.toProgress(
                items = items.filter { it.batchId == batch.id },
                coverageGapCount = coverageItems.count { it.batchId == batch.id },
            )
        }
        val pendingCompletion = batches
            .asSequence()
            .filter { batch ->
                !batch.completionNotified &&
                    batch.completedAtEpochMillis != null &&
                    batch.state in CompletionBatchStateNames
            }
            .minByOrNull { it.completedAtEpochMillis ?: Long.MAX_VALUE }
            ?.let { batch -> progress.firstOrNull { it.batchId == batch.id } }
            ?.toCompletionSummary()
        ImportEnrichmentState(
            activeBatch = progress.firstOrNull { it.state == ImportBatchState.Enriching || it.state == ImportBatchState.Paused },
            // The hub is an action queue, not an import history. Terminal batches stay in Room for
            // diagnostics and idempotency, but disappear once they have nothing left for the user.
            recentBatches = progress
                .filter(ImportBatchProgress::hasRemainingImportAction),
            historyBatches = progress.filter { it.state in TerminalBatchStates },
            reviewItems = reviewRows.mapNotNull(ImportReviewRow::toReviewItem),
            issueItems = issueRows.mapNotNull(ImportIssueRow::toIssueItem),
            coverageItems = coverageItems,
            pendingCompletion = pendingCompletion,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ImportEnrichmentState())

    fun enqueue(batchId: Long?) {
        if (batchId == null) return
        ImportEnrichmentScheduler.enqueue(context, batchId)
    }

    fun resumePending() {
        scope.launch {
            adoptUnqueuedImports()
            importDao.getResumableBatches()
                .filter { it.state == ImportBatchState.Enriching.name }
                .forEach { ImportEnrichmentScheduler.enqueue(context, it.id) }
        }
    }

    private suspend fun adoptUnqueuedImports() {
        val candidates = importDao.getUnqueuedImportedMedia()
        candidates.groupBy { candidate ->
            when (candidate.metadataSource) {
                "Jikan" -> ImportSource.MalApi
                "Imdb" -> ImportSource.ImdbCsv
                "StoryGraph" -> ImportSource.StoryGraphCsv
                else -> null
            }
        }.forEach { (source, rows) ->
            if (source == null || rows.isEmpty()) return@forEach
            val now = System.currentTimeMillis()
            val batchId = importDao.insertBatch(
                ImportBatchEntity(
                    source = source.name,
                    state = ImportBatchState.Enriching.name,
                    totalCount = rows.size,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            importDao.insertItems(
                rows.map { row ->
                    ImportBatchItemEntity(
                        batchId = batchId,
                        sourceKey = "backfill:${row.mediaItemId}",
                        sourceExternalId = if (source == ImportSource.MalApi) {
                            row.malId?.toString()
                        } else {
                            row.metadataExternalId
                        },
                        mediaItemId = row.mediaItemId,
                        state = ImportItemState.Pending.name,
                        updatedAtEpochMillis = now,
                    )
                },
            )
        }
    }

    suspend fun pause(batchId: Long) {
        if (importDao.getBatch(batchId)?.state != ImportBatchState.Enriching.name) return
        ImportEnrichmentScheduler.cancel(context, batchId)
        val now = System.currentTimeMillis()
        importDao.resetInterruptedItems(batchId, now)
        importDao.updateBatchState(batchId, ImportBatchState.Paused.name, null, now, null)
    }

    suspend fun resume(batchId: Long) {
        if (importDao.getBatch(batchId)?.state != ImportBatchState.Paused.name) return
        val now = System.currentTimeMillis()
        importDao.updateBatchState(batchId, ImportBatchState.Enriching.name, null, now, null)
        ImportEnrichmentScheduler.enqueue(context, batchId)
    }

    suspend fun retryIssues(batchId: Long) {
        if (importDao.getBatch(batchId)?.state != ImportBatchState.CompletedWithIssues.name) return
        val now = System.currentTimeMillis()
        // Repair partial StoryGraph enrichment produced by older builds before spending network calls
        // retrying missing metadata. The DAO limits this to imported completed sessions with no history.
        val repairedCount = mediaDao.reconcileCompletedImportedBookProgressForBatch(batchId)
        val resetCount = importDao.retryEligibleIssues(batchId, now)
        if (resetCount == 0) {
            if (repairedCount > 0) {
                val finalState = if (importDao.issueCount(batchId) > 0) {
                    ImportBatchState.CompletedWithIssues
                } else {
                    ImportBatchState.Completed
                }
                importDao.updateBatchState(batchId, finalState.name, null, now, now)
            }
            return
        }
        importDao.updateBatchState(batchId, ImportBatchState.Enriching.name, null, now, null)
        runCatching { ImportEnrichmentScheduler.enqueue(context, batchId) }
    }

    suspend fun retryIssue(itemId: Long): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val batchId = requireNotNull(importDao.retryIssueAndResumeBatch(itemId, now)) {
            "Import issue is no longer retryable"
        }
        scheduleRetry(batchId)
    }

    suspend fun skipIssue(itemId: Long): Result<Unit> = runCatching {
        val item = requireNotNull(importDao.getItem(itemId)) { "Import issue no longer exists" }
        check(item.state in IssueItemStateNames) { "Import issue is already resolved" }
        finishReviewItem(item, ImportItemState.Skipped, reference = null)
    }

    suspend fun completeIssueAfterManualLink(
        itemId: Long,
        suggestion: MetadataSuggestion,
    ): Result<Unit> = runCatching {
        val item = requireNotNull(importDao.getItem(itemId)) { "Import issue no longer exists" }
        check(item.state in IssueItemStateNames) { "Import issue is already resolved" }
        check(item.mediaItemId != null) { "Imported title no longer exists" }
        finishReviewItem(
            item = item,
            state = ImportItemState.Applied,
            reference = ProviderReference(
                source = suggestion.source,
                externalId = suggestion.externalId,
                mediaType = suggestion.mediaType,
                evidence = "Coincidència seleccionada manualment",
            ),
        )
    }

    suspend fun dismissCoverage(itemId: Long): Result<Unit> = runCatching {
        check(importDao.dismissCoverage(itemId, System.currentTimeMillis()) == 1) {
            "Metadata coverage item is no longer available"
        }
    }

    suspend fun retryCoverage(itemId: Long): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val batchId = requireNotNull(importDao.retryCoverageAndResumeBatch(itemId, now)) {
            "Import coverage item is no longer refreshable"
        }
        scheduleRetry(batchId)
    }

    private fun scheduleRetry(batchId: Long) {
        // The queue transition is durable. Startup recovery will enqueue it if WorkManager is
        // temporarily unavailable, so a successful user action must not be reported as lost.
        runCatching { ImportEnrichmentScheduler.enqueue(context, batchId) }
    }

    suspend fun acknowledgeCompletion(batchId: Long) {
        importDao.markCompletionNotified(batchId)
    }

    suspend fun deleteHistoryBatch(batchId: Long): Result<Unit> = runCatching {
        val batch = state.value.historyBatches.firstOrNull { it.batchId == batchId }
        check(batch?.canDeleteFromHistory() == true) {
            "Import history entry still has work requiring attention"
        }
        check(importDao.deleteFinishedBatch(batchId) == 1) {
            "Import history entry is no longer removable"
        }
    }

    suspend fun clearFinishedHistory(): Result<Int> = runCatching {
        val batchIds = state.value.historyBatches
            .filter(ImportBatchProgress::canDeleteFromHistory)
            .map(ImportBatchProgress::batchId)
        if (batchIds.isEmpty()) 0 else importDao.deleteFinishedBatches(batchIds)
    }

    suspend fun cancelEnrichment(batchId: Long) {
        val state = importDao.getBatch(batchId)?.state ?: return
        if (state !in setOf(
                ImportBatchState.Enriching.name,
                ImportBatchState.Paused.name,
                ImportBatchState.CompletedWithIssues.name,
            )
        ) {
            return
        }
        ImportEnrichmentScheduler.cancel(context, batchId)
        importDao.cancelBatch(batchId, System.currentTimeMillis())
    }

    /** Reprocesses MAL review items and every other MAL-linked anime using the selected title policy. */
    suspend fun enqueueAnimeTitleRefresh(): Result<Int> = runCatching {
        val now = System.currentTimeMillis()
        val reviewItems = importDao.getMalReviewItems()
        if (reviewItems.isNotEmpty()) {
            importDao.resetReviewItems(reviewItems.map { it.id }, now)
            reviewItems.map { it.batchId }.distinct().forEach { batchId ->
                importDao.updateBatchState(batchId, ImportBatchState.Enriching.name, null, now, null)
                ImportEnrichmentScheduler.enqueue(context, batchId)
            }
        }

        val reviewMediaIds = reviewItems.mapNotNull { it.mediaItemId }.toSet()
        val activeMediaIds = importDao.getActiveMalMediaIds().toSet()
        val remaining = mediaDao.getMediaItems().filter { item ->
            item.type == MediaType.Anime.name && item.malId != null &&
                item.id !in reviewMediaIds && item.id !in activeMediaIds
        }
        if (remaining.isNotEmpty()) {
            val batchId = importDao.insertBatch(
                ImportBatchEntity(
                    source = ImportSource.MalApi.name,
                    state = ImportBatchState.Enriching.name,
                    totalCount = remaining.size,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            importDao.insertItems(
                remaining.map { item ->
                    ImportBatchItemEntity(
                        batchId = batchId,
                        sourceKey = "title-preference:${item.id}",
                        sourceExternalId = item.malId.toString(),
                        mediaItemId = item.id,
                        state = ImportItemState.Pending.name,
                        updatedAtEpochMillis = now,
                    )
                },
            )
            ImportEnrichmentScheduler.enqueue(context, batchId)
        }
        reviewItems.size + remaining.size
    }

    /** Applies import-owned fields for the chosen match, then returns only manual conflicts to the user. */
    suspend fun prepareReview(
        itemId: Long,
        selectedReference: ProviderReference? = null,
    ): Result<ImportReviewDraft?> = runCatching {
        val item = requireNotNull(importDao.getItem(itemId)) { "Import review item no longer exists" }
        check(item.state == ImportItemState.NeedsReview.name) { "Import review item is already resolved" }
        val mediaItemId = requireNotNull(item.mediaItemId) { "Imported title no longer exists" }
        val mediaItem = requireNotNull(mediaDao.getMediaItem(mediaItemId)) { "Imported title no longer exists" }
        val currentMediaType = requireNotNull(runCatching { MediaType.valueOf(mediaItem.type) }.getOrNull()) {
            "Imported title has an unsupported media type"
        }
        val reference = selectedReference ?: item.selectedProviderReference(currentMediaType)
            ?: item.candidateReferencesJson.toProviderReferences().firstOrNull()
            ?: error("No metadata candidate is available")
        check(importDao.selectReviewReference(item.id, reference.source.name, reference.externalId, System.currentTimeMillis()) == 1) {
            "Import review item changed"
        }
        val preview = requireNotNull(
            mediaRepository.previewMediaItemMetadataLink(
                mediaItemId = mediaItemId,
                suggestion = reference.toSuggestion(mediaItem.title),
                metadataRepository = metadataRepository,
            ),
        ) { "Metadata is no longer available for this match" }
        val automaticFields = automaticFieldsFor(item, preview)
        applyReviewFields(preview, automaticFields)
        val remainingPreview = preview.copy(
            changes = preview.changes.filterNot { it.field in automaticFields },
        )
        if (remainingPreview.changes.isEmpty()) {
            finishReviewItem(item, ImportItemState.Applied, reference)
            null
        } else {
            ImportReviewDraft(
                itemId = item.id,
                batchId = item.batchId,
                title = mediaItem.title,
                reference = reference,
                preview = remainingPreview,
            )
        }
    }

    /** Rebuilds the preview from current Room state so an older dialog cannot overwrite a newer edit. */
    suspend fun applyReview(
        displayedDraft: ImportReviewDraft,
        selectedFields: Set<MetadataRefreshField>,
    ): Result<ImportReviewApplyOutcome> = runCatching {
        val item = requireNotNull(importDao.getItem(displayedDraft.itemId)) { "Import review item no longer exists" }
        check(item.state == ImportItemState.NeedsReview.name) { "Import review item is already resolved" }
        val mediaItemId = requireNotNull(item.mediaItemId) { "Imported title no longer exists" }
        val mediaItem = requireNotNull(mediaDao.getMediaItem(mediaItemId)) { "Imported title no longer exists" }
        val freshPreview = requireNotNull(
            mediaRepository.previewMediaItemMetadataLink(
                mediaItemId = mediaItemId,
                suggestion = displayedDraft.reference.toSuggestion(mediaItem.title),
                metadataRepository = metadataRepository,
            ),
        ) { "Metadata is no longer available for this match" }
        val automaticFields = automaticFieldsFor(item, freshPreview)
        applyReviewFields(freshPreview, automaticFields)
        val freshRemaining = freshPreview.changes.filterNot { it.field in automaticFields }
        val remainingPreview = freshPreview.copy(changes = freshRemaining)
        if (!remainingPreview.selectedChangesMatch(displayedDraft.preview, selectedFields)) {
            ImportReviewApplyOutcome.Changed(
                displayedDraft.copy(preview = remainingPreview),
            )
        } else {
            applyReviewFields(freshPreview, selectedFields)
            finishReviewItem(item, ImportItemState.Applied, displayedDraft.reference)
            ImportReviewApplyOutcome.Applied
        }
    }

    suspend fun skipReview(itemId: Long): Result<Unit> = runCatching {
        val item = requireNotNull(importDao.getItem(itemId)) { "Import review item no longer exists" }
        check(item.state == ImportItemState.NeedsReview.name) { "Import review item is already resolved" }
        val mediaType = item.mediaItemId?.let { mediaDao.getMediaItem(it) }?.type
            ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }
        finishReviewItem(item, ImportItemState.Skipped, mediaType?.let(item::selectedProviderReference))
    }

    suspend fun processBatch(batchId: Long): ImportWorkerOutcome {
        val batch = importDao.getBatch(batchId) ?: return ImportWorkerOutcome.Complete
        if (batch.state != ImportBatchState.Enriching.name) return ImportWorkerOutcome.Complete
        val now = System.currentTimeMillis()
        importDao.resetInterruptedItems(batchId, now)
        importDao.resetRetryableFailures(batchId, MaxItemAttempts, now)
        importDao.updateBatchState(batchId, ImportBatchState.Enriching.name, null, now, null)

        val pending = importDao.getPendingItems(batchId, ItemsPerWorkerRun)
        var retryDelayMillis: Long? = null
        for (item in pending) {
            if (importDao.claimPendingItem(item.id, System.currentTimeMillis()) == 0) continue
            val currentAttempt = item.attemptCount + 1
            try {
                val delay = processItem(batch, item, currentAttempt)
                if (delay != null) {
                    retryDelayMillis = maxOf(retryDelayMillis ?: 0L, delay)
                    break
                }
            } catch (error: CancellationException) {
                withContext(NonCancellable) {
                    importDao.resetInterruptedItems(batchId, System.currentTimeMillis())
                }
                throw error
            } catch (error: MetadataProviderHttpException) {
                val retryable = error.statusCode == 429 || error.statusCode >= 500
                finishItem(
                    item = item,
                    state = when (error.statusCode) {
                        401, 403 -> ImportItemState.Unavailable
                        404 -> ImportItemState.NoMatch
                        else -> ImportItemState.Failed
                    },
                    retryable = retryable,
                    error = when (error.statusCode) {
                        401, 403 -> "Provider authorization failed (HTTP ${error.statusCode})"
                        429 -> "Provider rate limit (HTTP 429)"
                        else -> error.message
                    },
                )
                if (retryable && currentAttempt < MaxItemAttempts) {
                    retryDelayMillis = maxOf(
                        retryDelayMillis ?: 0L,
                        enrichmentRetryDelayMillis(currentAttempt, error.retryAfterMillis),
                    )
                    break
                }
            } catch (error: Throwable) {
                finishItem(
                    item = item,
                    state = ImportItemState.Failed,
                    retryable = true,
                    error = error.message ?: error::class.simpleName,
                )
                if (currentAttempt < MaxItemAttempts) {
                    retryDelayMillis = maxOf(
                        retryDelayMillis ?: 0L,
                        enrichmentRetryDelayMillis(currentAttempt),
                    )
                    break
                }
            }
        }

        if (retryDelayMillis != null) {
            val retryAt = System.currentTimeMillis()
            importDao.updateBatchState(
                batchId,
                ImportBatchState.Enriching.name,
                "Temporary provider failure; retry scheduled",
                retryAt,
                null,
            )
            return ImportWorkerOutcome.Retry(retryDelayMillis)
        }
        if (importDao.pendingCount(batchId) > 0) return ImportWorkerOutcome.MoreWork
        if (importDao.retryableFailureCount(batchId, MaxItemAttempts) > 0) {
            return ImportWorkerOutcome.Retry(enrichmentRetryDelayMillis(attempt = 1))
        }

        if (importDao.getBatch(batchId)?.state != ImportBatchState.Enriching.name) {
            return ImportWorkerOutcome.Complete
        }

        val completedAt = System.currentTimeMillis()
        val finalState = if (importDao.issueCount(batchId) > 0) {
            ImportBatchState.CompletedWithIssues
        } else {
            ImportBatchState.Completed
        }
        importDao.updateBatchState(batchId, finalState.name, null, completedAt, completedAt)
        return ImportWorkerOutcome.Complete
    }

    private suspend fun processItem(
        batch: ImportBatchEntity,
        item: ImportBatchItemEntity,
        currentAttempt: Int,
    ): Long? {
        val mediaItemId = item.mediaItemId
        val mediaItem = mediaItemId?.let { mediaDao.getMediaItem(it) }
        if (mediaItem == null) {
            finishItem(item, ImportItemState.Skipped, error = "The imported title no longer exists")
            return null
        }
        val mediaType = runCatching { MediaType.valueOf(mediaItem.type) }.getOrNull()
        val importSource = runCatching { ImportSource.valueOf(batch.source) }.getOrNull()
        if (mediaType == null || importSource == null) {
            finishItem(item, ImportItemState.Unavailable, error = "Unsupported import source or media type")
            return null
        }

        val resolution = resolver.resolve(
            ImportedMetadataInput(
                mediaItemId = mediaItem.id,
                importSource = importSource,
                sourceExternalId = item.sourceExternalId,
                malId = mediaItem.malId,
                mediaType = mediaType,
                title = mediaItem.title,
                originalTitle = mediaItem.originalTitle,
                releaseYear = mediaItem.releaseYear,
                creators = mediaItem.toDomain().creators,
            ),
        )
        return when (resolution) {
            is ImportedResolution.Exact -> {
                applyExact(item, mediaItem.title, resolution.reference, importSource)
                null
            }
            is ImportedResolution.Candidates -> finishItem(
                item = item,
                state = ImportItemState.NeedsReview,
                reference = resolution.references.firstOrNull(),
                matchKind = "Candidates",
                candidateReferencesJson = resolution.references.toJson(),
            ).let { null }
            ImportedResolution.NoMatch ->
                finishItem(item, ImportItemState.NoMatch, matchKind = "NoMatch").let { null }
            is ImportedResolution.Unavailable -> finishItem(
                item,
                ImportItemState.Unavailable,
                matchKind = "Unavailable",
                error = resolution.reason,
            ).let { null }
            is ImportedResolution.Failed -> {
                finishItem(
                    item,
                    ImportItemState.Failed,
                    matchKind = "Failed",
                    retryable = resolution.retryable,
                    error = resolution.diagnostic,
                )
                if (resolution.retryable && currentAttempt < MaxItemAttempts) {
                    enrichmentRetryDelayMillis(currentAttempt, resolution.retryAfterMillis)
                } else {
                    null
                }
            }
        }
    }

    private suspend fun applyExact(
        item: ImportBatchItemEntity,
        fallbackTitle: String,
        reference: ProviderReference,
        importSource: ImportSource,
    ) {
        val mediaItemId = requireNotNull(item.mediaItemId)
        val preview = mediaRepository.previewMediaItemMetadataLink(
            mediaItemId = mediaItemId,
            suggestion = reference.toSuggestion(fallbackTitle),
            metadataRepository = metadataRepository,
        ) ?: run {
            finishItem(item, ImportItemState.NoMatch, reference, matchKind = "Exact")
            return
        }
        val appliedFields = preview.autoApplicableImportedFields(
            importSource = importSource,
            animeTitlePreference = animeTitlePreference(),
        )
        if (appliedFields.isNotEmpty()) {
            check(mediaRepository.applyImportedMediaItemMetadataRefresh(preview, appliedFields)) {
                "The imported title changed before metadata could be applied"
            }
            if (preview.refreshed.coverUrl != null && MetadataRefreshField.Cover in appliedFields) {
                coverRepository.persist(preview.refreshed.coverUrl)
            }
        }
        val handledFields = appliedFields + preview.equivalentMalUrlFields(reference)
        val requiresReview = preview.hasReviewChangesAfter(handledFields)
        finishItem(
            item = item,
            state = if (requiresReview) ImportItemState.NeedsReview else ImportItemState.Applied,
            reference = reference,
            matchKind = "Exact",
        )
    }

    private suspend fun finishItem(
        item: ImportBatchItemEntity,
        state: ImportItemState,
        reference: ProviderReference? = null,
        matchKind: String? = null,
        candidateReferencesJson: String? = null,
        retryable: Boolean = false,
        error: String? = null,
    ) {
        val now = System.currentTimeMillis()
        importDao.finishItem(
            itemId = item.id,
            state = state.name,
            providerSource = reference?.source?.name,
            providerExternalId = reference?.externalId,
            matchKind = matchKind,
            candidateReferencesJson = candidateReferencesJson,
            retryable = retryable,
            lastError = error?.replace(Regex("\\s+"), " ")?.take(MaxStoredErrorLength),
            now = now,
            completedAtEpochMillis = if (state in TerminalItemStates) now else null,
        )
    }

    private suspend fun applyReviewFields(
        preview: MetadataRefreshPreview,
        fields: Set<MetadataRefreshField>,
    ) {
        if (fields.isEmpty()) return
        check(mediaRepository.applyImportedMediaItemMetadataRefresh(preview, fields)) {
            "The imported title changed before metadata could be applied"
        }
        if (MetadataRefreshField.Cover in fields) {
            preview.refreshed.coverUrl?.let { coverRepository.persist(it) }
        }
    }

    private suspend fun automaticFieldsFor(
        item: ImportBatchItemEntity,
        preview: MetadataRefreshPreview,
    ): Set<MetadataRefreshField> {
        val source = importDao.getBatch(item.batchId)?.source
            ?.let { sourceName -> runCatching { ImportSource.valueOf(sourceName) }.getOrNull() }
            ?: error("Import batch no longer exists")
        return preview.autoApplicableImportedFields(source, animeTitlePreference())
    }

    private suspend fun finishReviewItem(
        item: ImportBatchItemEntity,
        state: ImportItemState,
        reference: ProviderReference?,
    ) {
        finishItem(item, state, reference, matchKind = item.matchKind)
        if (importDao.getBatch(item.batchId)?.state == ImportBatchState.Cancelled.name) return
        if (importDao.pendingCount(item.batchId) == 0) {
            val now = System.currentTimeMillis()
            val finalState = if (importDao.issueCount(item.batchId) > 0) {
                ImportBatchState.CompletedWithIssues
            } else {
                ImportBatchState.Completed
            }
            importDao.updateBatchState(item.batchId, finalState.name, null, now, now)
        }
    }
}

object ImportEnrichmentScheduler {
    fun enqueue(context: Context, batchId: Long, delayMillis: Long = 0L) {
        val requestBuilder = OneTimeWorkRequestBuilder<ImportEnrichmentWorker>()
            .setInputData(Data.Builder().putLong(BatchIdKey, batchId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
        if (delayMillis > 0L) {
            requestBuilder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        }
        val request = requestBuilder.build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(batchId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    fun cancel(context: Context, batchId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(batchId))
    }

    private fun uniqueWorkName(batchId: Long): String = "omnilog_import_enrichment_$batchId"
}

class ImportEnrichmentWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val batchId = inputData.getLong(BatchIdKey, 0L).takeIf { it > 0L } ?: return Result.failure()
        val application = applicationContext as? ContentTrackerApplication ?: return Result.failure()
        return when (val outcome = application.importEnrichmentManager.processBatch(batchId)) {
            ImportWorkerOutcome.Complete -> Result.success()
            ImportWorkerOutcome.MoreWork -> {
                ImportEnrichmentScheduler.enqueue(applicationContext, batchId)
                Result.success()
            }
            is ImportWorkerOutcome.Retry -> {
                ImportEnrichmentScheduler.enqueue(applicationContext, batchId, outcome.delayMillis)
                Result.success()
            }
        }
    }
}

private fun ImportBatchEntity.toProgress(
    items: List<ImportBatchItemEntity>,
    coverageGapCount: Int,
): ImportBatchProgress? {
    val source = runCatching { ImportSource.valueOf(source) }.getOrNull() ?: return null
    val state = runCatching { ImportBatchState.valueOf(state) }.getOrNull() ?: return null
    return ImportBatchProgress(
        batchId = id,
        source = source,
        state = state,
        totalCount = totalCount,
        appliedCount = items.count { it.state == ImportItemState.Applied.name },
        needsReviewCount = items.count { it.state == ImportItemState.NeedsReview.name },
        issueCount = items.count {
            it.state in setOf(
                ImportItemState.Failed.name,
                ImportItemState.Unavailable.name,
                ImportItemState.NoMatch.name,
            )
        },
        retryableIssueCount = items.count { item ->
            item.state == ImportItemState.NoMatch.name ||
                item.state == ImportItemState.Unavailable.name ||
                (item.state == ImportItemState.Failed.name && item.retryable)
        },
        coverageGapCount = coverageGapCount,
        pendingCount = items.count { it.state == ImportItemState.Pending.name || it.state == ImportItemState.Resolving.name },
        cancelledCount = items.count { it.state == ImportItemState.Cancelled.name },
        createdAtEpochMillis = createdAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
    )
}

private fun ImportBatchProgress.toCompletionSummary(): ImportCompletionSummary =
    ImportCompletionSummary(
        batchId = batchId,
        source = source,
        totalCount = totalCount,
        processedCount = processedCount,
        appliedCount = appliedCount,
        needsReviewCount = needsReviewCount,
        issueCount = issueCount,
        coverageGapCount = coverageGapCount,
    )

private fun ImportReviewRow.toReviewItem(): ImportReviewItem? {
    val parsedMediaType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
    val candidates = candidateReferencesJson.toProviderReferences()
    val selected = providerSource?.let { sourceName ->
        val source = runCatching { com.nilpo.contenttracker.core.model.MetadataSource.valueOf(sourceName) }.getOrNull()
            ?: return@let null
        val externalId = providerExternalId?.takeIf { it.isNotBlank() } ?: return@let null
        candidates.firstOrNull { it.source == source && it.externalId == externalId }
            ?: ProviderReference(source, externalId, parsedMediaType, "Resolved provider match")
    }
    return ImportReviewItem(
        itemId = itemId,
        batchId = batchId,
        mediaItemId = mediaItemId,
        title = title,
        mediaType = parsedMediaType,
        matchKind = matchKind,
        selectedReference = selected,
        candidates = candidates.ifEmpty { listOfNotNull(selected) },
    )
}

private fun ImportIssueRow.toIssueItem(): ImportIssueItem? {
    val parsedMediaType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
    val parsedSource = runCatching { ImportSource.valueOf(importSource) }.getOrNull() ?: return null
    val parsedState = runCatching { ImportItemState.valueOf(state) }.getOrNull() ?: return null
    return ImportIssueItem(
        itemId = itemId,
        batchId = batchId,
        mediaItemId = mediaItemId,
        title = title,
        mediaType = parsedMediaType,
        importSource = parsedSource,
        state = parsedState,
        retryable = retryable,
        attemptCount = attemptCount,
        lastError = lastError,
    )
}

internal fun ImportCoverageRow.toCoverageItem(): ImportCoverageItem? {
    val parsedMediaType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
    val parsedSource = runCatching { ImportSource.valueOf(importSource) }.getOrNull() ?: return null
    val missingFields = buildSet {
        if (coverUrl.isNullOrBlank()) add(ImportMetadataGap.Cover)
        if (synopsis.isNullOrBlank()) add(ImportMetadataGap.Synopsis)
        if (releaseYear == null || releaseYear <= 0) add(ImportMetadataGap.ReleaseYear)
        if (!creatorsJson.hasJsonValues()) add(ImportMetadataGap.Creators)
        if (!genresJson.hasJsonValues()) add(ImportMetadataGap.Genres)
        if (parsedMediaType != MediaType.Game && (progressTotal == null || progressTotal <= 0)) {
            add(ImportMetadataGap.ProgressTotal)
        }
    }
    if (missingFields.isEmpty()) return null
    return ImportCoverageItem(
        itemId = itemId,
        batchId = batchId,
        mediaItemId = mediaItemId,
        title = title,
        mediaType = parsedMediaType,
        importSource = parsedSource,
        missingFields = missingFields,
    )
}

private fun String?.hasJsonValues(): Boolean {
    if (isNullOrBlank()) return false
    return runCatching { org.json.JSONArray(this).length() > 0 }.getOrDefault(false)
}

private fun ImportBatchItemEntity.selectedProviderReference(mediaType: MediaType): ProviderReference? {
    val source = providerSource?.let { sourceName ->
        runCatching { com.nilpo.contenttracker.core.model.MetadataSource.valueOf(sourceName) }.getOrNull()
    } ?: return null
    val externalId = providerExternalId?.takeIf { it.isNotBlank() } ?: return null
    return candidateReferencesJson.toProviderReferences()
        .firstOrNull { it.source == source && it.externalId == externalId }
        ?: ProviderReference(source, externalId, mediaType, "Resolved provider match")
}

private val TerminalItemStates = setOf(
    ImportItemState.NeedsReview,
    ImportItemState.Applied,
    ImportItemState.NoMatch,
    ImportItemState.Unavailable,
    ImportItemState.Failed,
    ImportItemState.Skipped,
    ImportItemState.Cancelled,
)
private const val BatchIdKey = "import_batch_id"
private const val ItemsPerWorkerRun = 10
private const val MaxItemAttempts = 3
private const val MaxStoredErrorLength = 300
private const val BaseRetryDelayMillis = 60_000L
private const val MaxRetryDelayMillis = 6 * 60 * 60 * 1_000L

internal fun enrichmentRetryDelayMillis(
    attempt: Int,
    providerRetryAfterMillis: Long? = null,
): Long {
    val exponent = (attempt - 1).coerceIn(0, 5)
    val exponentialDelay = BaseRetryDelayMillis * (1L shl exponent)
    return maxOf(exponentialDelay, providerRetryAfterMillis ?: 0L)
        .coerceAtMost(MaxRetryDelayMillis)
}

private val IssueItemStateNames = setOf(
    ImportItemState.Failed.name,
    ImportItemState.Unavailable.name,
    ImportItemState.NoMatch.name,
)

private val CompletionBatchStateNames = setOf(
    ImportBatchState.Completed.name,
    ImportBatchState.CompletedWithIssues.name,
)

private val TerminalBatchStates = setOf(
    ImportBatchState.Completed,
    ImportBatchState.CompletedWithIssues,
    ImportBatchState.Cancelled,
)
