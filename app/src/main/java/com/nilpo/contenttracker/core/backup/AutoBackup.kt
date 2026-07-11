package com.nilpo.contenttracker.core.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nilpo.contenttracker.ContentTrackerApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class AutoBackupConfiguration(
    val directoryUri: Uri?,
    val frequency: AutoBackupFrequency,
    val lastSuccessAtEpochMillis: Long?,
)

enum class AutoBackupFrequency(
    val label: String,
    val intervalDays: Long,
) {
    Weekly(label = "Setmanal", intervalDays = 7),
    Monthly(label = "Mensual", intervalDays = 30),
    EveryTwoMonths(label = "Cada 2 mesos", intervalDays = 60),
    ;

    companion object {
        fun fromStoredValue(value: String?): AutoBackupFrequency {
            return entries.firstOrNull { it.name == value } ?: Monthly
        }
    }
}

object AutoBackupPreferences {
    private const val PreferencesName = "omnilog_auto_backup"
    private const val DirectoryUriKey = "directory_uri"
    private const val FrequencyKey = "frequency"
    private const val LastSuccessAtKey = "last_success_at"

    fun read(context: Context): AutoBackupConfiguration {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        return AutoBackupConfiguration(
            directoryUri = preferences.getString(DirectoryUriKey, null)?.let(Uri::parse),
            frequency = AutoBackupFrequency.fromStoredValue(preferences.getString(FrequencyKey, null)),
            lastSuccessAtEpochMillis = preferences
                .getLong(LastSuccessAtKey, 0L)
                .takeIf { it > 0L },
        )
    }

    fun saveDirectory(context: Context, directoryUri: Uri) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(DirectoryUriKey, directoryUri.toString())
            .apply()
    }

    fun saveFrequency(context: Context, frequency: AutoBackupFrequency) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(FrequencyKey, frequency.name)
            .apply()
    }

    fun markBackupSucceeded(context: Context) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putLong(LastSuccessAtKey, System.currentTimeMillis())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(DirectoryUriKey)
            .remove(LastSuccessAtKey)
            .apply()
    }
}

object AutoBackupScheduler {
    private const val UniqueWorkName = "omnilog_auto_backup"
    private const val InitialWorkName = "omnilog_auto_backup_initial"

    fun activate(context: Context, frequency: AutoBackupFrequency) {
        schedule(context, frequency)
        WorkManager.getInstance(context).enqueueUniqueWork(
            InitialWorkName,
            ExistingWorkPolicy.REPLACE,
            initialRequest(),
        )
    }

    fun schedule(context: Context, frequency: AutoBackupFrequency) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            newRequest(frequency),
        )
    }

    fun ensureScheduled(context: Context) {
        val configuration = AutoBackupPreferences.read(context)
        if (configuration.directoryUri != null) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UniqueWorkName,
                ExistingPeriodicWorkPolicy.KEEP,
                newRequest(configuration.frequency),
            )
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(UniqueWorkName)
            cancelUniqueWork(InitialWorkName)
        }
    }

    private fun newRequest(frequency: AutoBackupFrequency) =
        PeriodicWorkRequestBuilder<AutoBackupWorker>(frequency.intervalDays, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()

    private fun initialRequest() =
        OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()
}

class AutoBackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val configuration = AutoBackupPreferences.read(applicationContext)
        val directoryUri = configuration.directoryUri ?: return@withContext Result.success()

        try {
            val backupJson = (applicationContext as ContentTrackerApplication)
                .mediaRepository
                .exportBackupJson()
            val directoryDocumentId = DocumentsContract.getTreeDocumentId(directoryUri)
            val directoryDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                directoryUri,
                directoryDocumentId,
            )
            val backupUri = checkNotNull(
                DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    directoryDocumentUri,
                    "application/json",
                    autoBackupFileName(),
                ),
            ) { "Could not create automatic backup file" }
            checkNotNull(applicationContext.contentResolver.openOutputStream(backupUri)) {
                "Could not open automatic backup destination"
            }.use { outputStream ->
                outputStream.write(backupJson.toByteArray(Charsets.UTF_8))
            }
            AutoBackupPreferences.markBackupSucceeded(applicationContext)
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: IllegalStateException) {
            Result.retry()
        } catch (_: IllegalArgumentException) {
            Result.failure()
        }
    }
}

private fun autoBackupFileName(): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
    return "omnilog-auto-backup-$timestamp.json"
}
