package com.nilpo.contenttracker.core.cover

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nilpo.contenttracker.ContentTrackerApplication
import com.nilpo.contenttracker.core.model.MediaType
import kotlinx.coroutines.flow.first

object CoverSyncScheduler {
    private const val UNIQUE_WORK_NAME = "omnilog_cover_sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<CoverSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

class CoverSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? ContentTrackerApplication
            ?: return Result.failure()
        val coverUrls = application.mediaRepository
            .observeTrackedMedia(MediaType.entries.toSet())
            .first()
            .map { it.item.coverUrl }

        val sync = application.coverRepository.persistAll(coverUrls)
        return if (sync.failures.isNotEmpty() && runAttemptCount < MAX_RETRY_COUNT) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

private const val MAX_RETRY_COUNT = 3
