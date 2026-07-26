package com.nilpo.contenttracker.core.refresh

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
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.dao.MetadataRefreshDao
import com.nilpo.contenttracker.core.database.entity.MetadataRefreshItemEntity
import com.nilpo.contenttracker.core.database.entity.MetadataRefreshRunEntity
import com.nilpo.contenttracker.core.imports.ImportEnrichmentExecutionGate
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.toMetadataFailureDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

enum class MetadataRefreshRunState {
    Refreshing,
    Completed,
    CompletedWithIssues,
    Cancelled,
}

enum class MetadataRefreshItemState {
    Pending,
    Processing,
    Applied,
    Unchanged,
    Skipped,
    Failed,
    Cancelled,
}

data class MetadataRefreshProgress(
    val runId: Long,
    val state: MetadataRefreshRunState,
    val totalCount: Int,
    val appliedCount: Int,
    val unchangedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val pendingCount: Int,
) {
    val processedCount: Int
        get() = (totalCount - pendingCount).coerceIn(0, totalCount)
}

data class MetadataRefreshState(
    val activeRun: MetadataRefreshProgress? = null,
    val latestCompletedRun: MetadataRefreshProgress? = null,
)

data class MetadataRefreshStart(
    val queuedCount: Int,
    val alreadyRunning: Boolean,
)

sealed interface MetadataRefreshWorkerOutcome {
    data object Complete : MetadataRefreshWorkerOutcome
    data object MoreWork : MetadataRefreshWorkerOutcome
    data class Retry(val delayMillis: Long) : MetadataRefreshWorkerOutcome
}

/** Coordinates a single user-requested, restart-safe refresh of every directly linked item. */
class MetadataRefreshManager(
    private val context: Context,
    private val refreshDao: MetadataRefreshDao,
    private val mediaDao: MediaDao,
    private val mediaRepository: MediaRepository,
    private val metadataRepository: MetadataRepository,
    private val coverRepository: CoverRepository,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<MetadataRefreshState> = combine(
        refreshDao.observeRuns(),
        refreshDao.observeItems(),
    ) { runs, items ->
        val progress = runs.mapNotNull { run -> run.toProgress(items.filter { it.runId == run.id }) }
        MetadataRefreshState(
            activeRun = progress.firstOrNull { it.state == MetadataRefreshRunState.Refreshing },
            latestCompletedRun = progress.firstOrNull {
                it.state in setOf(
                    MetadataRefreshRunState.Completed,
                    MetadataRefreshRunState.CompletedWithIssues,
                )
            },
        )
    }.stateIn(scope, SharingStarted.Eagerly, MetadataRefreshState())

    override fun close() {
        scope.cancel()
    }

    suspend fun start(): Result<MetadataRefreshStart> = runCatching {
        refreshDao.getActiveRun()?.let { active ->
            return@runCatching MetadataRefreshStart(active.totalCount, alreadyRunning = true)
        }
        val eligibleIds = mediaDao.getMediaItems()
            .asSequence()
            .filter { item -> item.metadataExternalId?.isNotBlank() == true }
            .filter { item -> item.metadataSource.toDirectRefreshSource() != null }
            .map { it.id }
            .toList()
        if (eligibleIds.isEmpty()) return@runCatching MetadataRefreshStart(0, alreadyRunning = false)

        val now = System.currentTimeMillis()
        val runId = refreshDao.insertRun(
            MetadataRefreshRunEntity(
                state = MetadataRefreshRunState.Refreshing.name,
                totalCount = eligibleIds.size,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        refreshDao.insertItems(
            eligibleIds.map { mediaItemId ->
                MetadataRefreshItemEntity(
                    runId = runId,
                    mediaItemId = mediaItemId,
                    state = MetadataRefreshItemState.Pending.name,
                    updatedAtEpochMillis = now,
                )
            },
        )
        MetadataRefreshScheduler.enqueue(context, runId)
        MetadataRefreshStart(eligibleIds.size, alreadyRunning = false)
    }

    fun resumePending() {
        scope.launch {
            refreshDao.getResumableRuns().forEach { run ->
                MetadataRefreshScheduler.enqueue(context, run.id)
            }
        }
    }

    suspend fun cancel(runId: Long) {
        if (refreshDao.getRun(runId)?.state != MetadataRefreshRunState.Refreshing.name) return
        MetadataRefreshScheduler.cancel(context, runId)
        val now = System.currentTimeMillis()
        refreshDao.cancelUnfinishedItems(runId, now)
        refreshDao.updateRunState(runId, MetadataRefreshRunState.Cancelled.name, now, now)
    }

    suspend fun processRun(runId: Long): MetadataRefreshWorkerOutcome {
        val run = refreshDao.getRun(runId) ?: return MetadataRefreshWorkerOutcome.Complete
        if (run.state != MetadataRefreshRunState.Refreshing.name) return MetadataRefreshWorkerOutcome.Complete
        refreshDao.resetInterruptedItems(runId, System.currentTimeMillis())
        val pending = refreshDao.getPendingItems(runId, ItemsPerWorkerRun)
        var retryDelayMillis: Long? = null

        for (item in pending) {
            if (refreshDao.claimPendingItem(item.id, System.currentTimeMillis()) == 0) continue
            val attempt = item.attemptCount + 1
            try {
                val retryAfter = ImportEnrichmentExecutionGate.run { refreshItem(item) }
                if (retryAfter != null) {
                    retryDelayMillis = maxOf(retryDelayMillis ?: 0L, retryAfter)
                    break
                }
            } catch (error: CancellationException) {
                withContext(NonCancellable) {
                    refreshDao.resetInterruptedItems(runId, System.currentTimeMillis())
                }
                throw error
            } catch (error: Throwable) {
                val failure = error.toMetadataFailureDetails()
                val retryable = failure.retryable && attempt < MaxItemAttempts
                val now = System.currentTimeMillis()
                refreshDao.finishItem(
                    itemId = item.id,
                    state = if (retryable) MetadataRefreshItemState.Pending.name else MetadataRefreshItemState.Failed.name,
                    retryable = retryable,
                    error = failure.diagnostic,
                    now = now,
                    completedAt = if (retryable) null else now,
                )
                if (retryable) {
                    retryDelayMillis = maxOf(
                        retryDelayMillis ?: 0L,
                        retryDelayMillis(attempt, failure.retryAfterMillis),
                    )
                    break
                }
            }
        }

        if (retryDelayMillis != null) return MetadataRefreshWorkerOutcome.Retry(retryDelayMillis)
        if (refreshDao.pendingCount(runId) > 0) return MetadataRefreshWorkerOutcome.MoreWork

        val now = System.currentTimeMillis()
        val finalState = if (refreshDao.failureCount(runId) > 0) {
            MetadataRefreshRunState.CompletedWithIssues
        } else {
            MetadataRefreshRunState.Completed
        }
        refreshDao.updateRunState(runId, finalState.name, now, now)
        return MetadataRefreshWorkerOutcome.Complete
    }

    /** Returns a retry delay when the provider asked us to stop processing this run for now. */
    private suspend fun refreshItem(item: MetadataRefreshItemEntity): Long? {
        val now = System.currentTimeMillis()
        val mediaItemId = item.mediaItemId ?: run {
            refreshDao.finishItem(
                item.id,
                MetadataRefreshItemState.Skipped.name,
                retryable = false,
                error = "The title no longer exists",
                now = now,
                completedAt = now,
            )
            return null
        }
        val preview = mediaRepository.previewMediaItemMetadataRefresh(mediaItemId, metadataRepository) ?: run {
            refreshDao.finishItem(
                item.id,
                MetadataRefreshItemState.Skipped.name,
                retryable = false,
                error = "The title no longer has a supported provider link",
                now = now,
                completedAt = now,
            )
            return null
        }
        val automaticFields = preview.changes
            .filterNot { it.isLocallyOverridden }
            .map { it.field }
            .toSet()
        if (automaticFields.isEmpty()) {
            refreshDao.finishItem(
                item.id,
                MetadataRefreshItemState.Unchanged.name,
                retryable = false,
                error = null,
                now = now,
                completedAt = now,
            )
            return null
        }
        check(mediaRepository.applyAutomaticMediaItemMetadataRefresh(preview)) {
            "The title changed before metadata could be applied"
        }
        if (MetadataRefreshField.Cover in automaticFields) {
            preview.refreshed.coverUrl?.let { coverRepository.persist(it) }
        }
        refreshDao.finishItem(
            item.id,
            MetadataRefreshItemState.Applied.name,
            retryable = false,
            error = null,
            now = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis(),
        )
        return null
    }
}

object MetadataRefreshScheduler {
    fun enqueue(context: Context, runId: Long, delayMillis: Long = 0L) {
        val requestBuilder = OneTimeWorkRequestBuilder<MetadataRefreshWorker>()
            .setInputData(Data.Builder().putLong(RunIdKey, runId).build())
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
        if (delayMillis > 0L) requestBuilder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(runId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            requestBuilder.build(),
        )
    }

    fun cancel(context: Context, runId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(runId))
    }

    private fun uniqueWorkName(runId: Long): String = "omnilog_metadata_refresh_$runId"
}

class MetadataRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val runId = inputData.getLong(RunIdKey, 0L).takeIf { it > 0L } ?: return Result.failure()
        val application = applicationContext as? ContentTrackerApplication ?: return Result.failure()
        return when (val outcome = application.metadataRefreshManager.processRun(runId)) {
            MetadataRefreshWorkerOutcome.Complete -> Result.success()
            MetadataRefreshWorkerOutcome.MoreWork -> {
                MetadataRefreshScheduler.enqueue(applicationContext, runId)
                Result.success()
            }
            is MetadataRefreshWorkerOutcome.Retry -> {
                MetadataRefreshScheduler.enqueue(applicationContext, runId, outcome.delayMillis)
                Result.success()
            }
        }
    }
}

private fun String?.toDirectRefreshSource(): MetadataSource? = runCatching {
    MetadataSource.valueOf(this.orEmpty())
}.getOrNull()?.takeIf { source ->
    source in setOf(
        MetadataSource.AniList,
        MetadataSource.Jikan,
        MetadataSource.OpenLibrary,
        MetadataSource.GoogleBooks,
        MetadataSource.Tmdb,
        MetadataSource.Rawg,
    )
}

private fun MetadataRefreshRunEntity.toProgress(
    items: List<MetadataRefreshItemEntity>,
): MetadataRefreshProgress? {
    val state = runCatching { MetadataRefreshRunState.valueOf(state) }.getOrNull() ?: return null
    fun count(itemState: MetadataRefreshItemState): Int = items.count { it.state == itemState.name }
    return MetadataRefreshProgress(
        runId = id,
        state = state,
        totalCount = totalCount,
        appliedCount = count(MetadataRefreshItemState.Applied),
        unchangedCount = count(MetadataRefreshItemState.Unchanged),
        skippedCount = count(MetadataRefreshItemState.Skipped),
        failedCount = count(MetadataRefreshItemState.Failed),
        pendingCount = count(MetadataRefreshItemState.Pending) + count(MetadataRefreshItemState.Processing),
    )
}

private fun retryDelayMillis(attempt: Int, retryAfterMillis: Long?): Long {
    val exponentialDelay = 15_000L * (1L shl (attempt - 1).coerceAtMost(5))
    return maxOf(retryAfterMillis ?: 0L, exponentialDelay).coerceAtMost(MaxRetryDelayMillis)
}

private const val RunIdKey = "metadata_refresh_run_id"
private const val ItemsPerWorkerRun = 10
private const val MaxItemAttempts = 4
private const val MaxRetryDelayMillis = 30L * 60L * 1000L
