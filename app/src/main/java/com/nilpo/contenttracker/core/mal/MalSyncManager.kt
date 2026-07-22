package com.nilpo.contenttracker.core.mal

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nilpo.contenttracker.ContentTrackerApplication
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.MalSyncQueueEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.resolveMyAnimeListId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class MalSyncState(
    val isAvailable: Boolean,
    val isConnected: Boolean = false,
    val accountName: String? = null,
    val isSyncEnabled: Boolean = false,
    val isAuthorizing: Boolean = false,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val lastSuccessAtEpochMillis: Long? = null,
    val error: String? = null,
)

enum class MalWorkerOutcome { Success, Retry, AuthorizationRequired }

sealed interface MalSyncEvent {
    data class Updated(val animeTitles: List<String>) : MalSyncEvent
    data class Failed(
        val animeTitle: String?,
        val willRetry: Boolean,
        val authorizationRequired: Boolean = false,
    ) : MalSyncEvent
}

class MalSyncManager(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val clientId: String,
    private val redirectUri: String,
    private val tokenStore: MalTokenStore = MalTokenStore(context),
    private val apiClient: MalApiClient = MalApiClient(clientId, redirectUri),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEvents = MutableSharedFlow<MalSyncEvent>(extraBufferCapacity = 8)
    val state: StateFlow<MalSyncState> = mutableState.asStateFlow()
    val events: SharedFlow<MalSyncEvent> = mutableEvents.asSharedFlow()

    init {
        scope.launch {
            mediaDao.observeMalSyncQueue().collectLatest { queue ->
                mutableState.update { current ->
                    current.copy(
                        pendingCount = queue.count { it.state == PendingState },
                        failedCount = queue.count { it.state == FailedState },
                        lastSuccessAtEpochMillis = queue.mapNotNull { it.lastSuccessAtEpochMillis }.maxOrNull(),
                    )
                }
            }
        }
    }

    fun beginAuthorization(): String? {
        if (clientId.isBlank()) {
            mutableState.update { it.copy(error = "Falta MAL_CLIENT_ID.") }
            return null
        }
        val verifier = secureRandomString(64)
        val state = secureRandomString(32)
        tokenStore.savePendingAuthorization(MalPendingAuthorization(verifier, state))
        mutableState.update { it.copy(isAuthorizing = true, error = null) }
        return Uri.parse(AuthorizationUrl).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge", verifier)
            // MAL currently requires the verifier itself as the challenge.
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    suspend fun handleAuthorizationRedirect(uri: Uri): Result<Unit> = runCatching {
        require(isExpectedRedirect(uri)) { "Resposta OAuth inesperada." }
        uri.getQueryParameter("error")?.let { throw IllegalStateException("MAL ha rebutjat l'autorització.") }
        val pending = requireNotNull(tokenStore.readPendingAuthorization()) { "L'autorització ha caducat." }
        require(uri.getQueryParameter("state") == pending.state) { "La resposta OAuth no és vàlida." }
        val code = requireNotNull(uri.getQueryParameter("code")) { "MAL no ha retornat cap codi." }

        val exchanged = apiClient.exchangeAuthorizationCode(code, pending.verifier)
        val account = apiClient.getCurrentAccount(exchanged.accessToken)
        tokenStore.saveTokens(exchanged.copy(accountName = account.name))
        tokenStore.setSyncEnabled(false)
        tokenStore.clearPendingAuthorization()
        mutableState.update {
            it.copy(
                isConnected = true,
                accountName = account.name,
                isSyncEnabled = false,
                isAuthorizing = false,
                error = null,
            )
        }
    }.onFailure { error ->
        tokenStore.clearPendingAuthorization()
        mutableState.update {
            it.copy(isAuthorizing = false, error = error.message ?: "No s'ha pogut connectar amb MAL.")
        }
    }

    fun disconnect() {
        tokenStore.clear()
        mutableState.update {
            it.copy(
                isConnected = false,
                accountName = null,
                isSyncEnabled = false,
                isAuthorizing = false,
                isSyncing = false,
                error = null,
            )
        }
    }

    fun resumePendingSync() {
        if (tokenStore.isSyncEnabled() && tokenStore.readTokens() != null) {
            MalSyncScheduler.enqueue(context)
        }
    }

    suspend fun queueMediaItem(mediaItemId: Long) {
        queueMediaItem(mediaItemId, schedule = true)
    }

    private suspend fun queueMediaItem(mediaItemId: Long, schedule: Boolean) {
        val item = mediaDao.getMediaItem(mediaItemId) ?: return
        if (item.type != MediaType.Anime.name) return
        val malId = resolveMyAnimeListId(
            explicitMalId = item.malId,
            metadataSource = item.metadataSource,
            metadataExternalId = item.metadataExternalId,
            popularityJson = item.popularityJson,
            sourceUrl = item.sourceUrl,
        ) ?: return
        if (item.malId != malId) mediaDao.updateMalId(mediaItemId, malId)

        val existing = mediaDao.getMalSyncQueueItem(mediaItemId)
        mediaDao.insertMalSyncQueueItem(
            MalSyncQueueEntity(
                mediaItemId = mediaItemId,
                malId = malId,
                state = PendingState,
                attemptCount = 0,
                lastError = null,
                updatedAtEpochMillis = System.currentTimeMillis(),
                lastAttemptAtEpochMillis = existing?.lastAttemptAtEpochMillis,
                lastSuccessAtEpochMillis = existing?.lastSuccessAtEpochMillis,
            ),
        )
        if (schedule && tokenStore.isSyncEnabled() && tokenStore.readTokens() != null) {
            MalSyncScheduler.enqueue(context)
        }
    }

    suspend fun enableAndSyncAll() {
        check(tokenStore.readTokens() != null) { "Connecta el compte de MAL primer." }
        tokenStore.setSyncEnabled(true)
        mutableState.update { it.copy(isSyncEnabled = true, error = null) }
        syncAllIfEnabled()
    }

    suspend fun syncAllIfEnabled() {
        if (!tokenStore.isSyncEnabled() || tokenStore.readTokens() == null) return
        mediaDao.getMediaItems().forEach { item -> queueMediaItem(item.id, schedule = false) }
        MalSyncScheduler.enqueue(context)
    }

    suspend fun processPending(isFinalAttempt: Boolean = false): MalWorkerOutcome {
        if (!tokenStore.isSyncEnabled()) return MalWorkerOutcome.Success
        var tokens = tokenStore.readTokens() ?: run {
            mutableEvents.tryEmit(
                MalSyncEvent.Failed(
                    animeTitle = oldestPendingTitle(),
                    willRetry = false,
                    authorizationRequired = true,
                ),
            )
            return MalWorkerOutcome.AuthorizationRequired
        }
        val updatedTitles = mutableListOf<String>()
        var failureEvent: MalSyncEvent.Failed? = null
        mutableState.update { it.copy(isSyncing = true, error = null) }
        return try {
            tokens = ensureFresh(tokens)
            var processed = 0
            while (processed < MaxItemsPerRun) {
                val pending = mediaDao.getPendingMalSyncQueue(minOf(BatchSize, MaxItemsPerRun - processed))
                if (pending.isEmpty()) break
                for (queueItem in pending) {
                    val item = mediaDao.getMediaItem(queueItem.mediaItemId)
                    val sessions = item?.let { mediaDao.getTrackingSessions(it.id) }
                    val payload = if (item != null && sessions != null) buildMalSyncPayload(item, sessions) else null
                    if (payload == null) {
                        mediaDao.deleteMalSyncQueueItem(queueItem.mediaItemId)
                        processed++
                        continue
                    }

                    val attemptAt = System.currentTimeMillis()
                    try {
                        apiClient.updateAnimeList(tokens.accessToken, queueItem.malId, payload)
                    } catch (error: MalApiException) {
                        if (error.statusCode == 401) {
                            tokens = refreshOrDisconnect(tokens)
                            apiClient.updateAnimeList(tokens.accessToken, queueItem.malId, payload)
                        } else {
                            throw error
                        }
                    }
                    mediaDao.insertMalSyncQueueItem(
                        queueItem.copy(
                            state = SyncedState,
                            attemptCount = 0,
                            lastError = null,
                            lastAttemptAtEpochMillis = attemptAt,
                            lastSuccessAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                    item?.title?.let(updatedTitles::add)
                    processed++
                }
            }
            if (mediaDao.getPendingMalSyncQueue(1).isEmpty()) {
                MalWorkerOutcome.Success
            } else {
                MalWorkerOutcome.Retry
            }
        } catch (error: MalAuthorizationRequiredException) {
            failureEvent = MalSyncEvent.Failed(
                animeTitle = oldestPendingTitle(),
                willRetry = false,
                authorizationRequired = true,
            )
            MalWorkerOutcome.AuthorizationRequired
        } catch (error: MalApiException) {
            val willRetry = error.isRetryable && !isFinalAttempt
            val failedTitle = markOldestPendingFailure(error.message, retryable = willRetry)
            failureEvent = MalSyncEvent.Failed(failedTitle, willRetry)
            if (willRetry) MalWorkerOutcome.Retry else MalWorkerOutcome.Success
        } catch (error: Exception) {
            val willRetry = !isFinalAttempt
            val failedTitle = markOldestPendingFailure(error.message, retryable = willRetry)
            failureEvent = MalSyncEvent.Failed(failedTitle, willRetry)
            if (willRetry) MalWorkerOutcome.Retry else MalWorkerOutcome.Success
        } finally {
            when {
                failureEvent != null -> mutableEvents.tryEmit(failureEvent)
                updatedTitles.isNotEmpty() -> mutableEvents.tryEmit(MalSyncEvent.Updated(updatedTitles))
            }
            mutableState.update { it.copy(isSyncing = false) }
        }
    }

    private suspend fun ensureFresh(tokens: MalTokens): MalTokens {
        if (tokens.expiresAtEpochMillis > System.currentTimeMillis() + RefreshLeewayMillis) return tokens
        return refreshOrDisconnect(tokens)
    }

    private suspend fun refreshOrDisconnect(tokens: MalTokens): MalTokens = try {
        apiClient.refreshTokens(tokens).also(tokenStore::saveTokens)
    } catch (error: MalApiException) {
        if (error.statusCode == 400 || error.statusCode == 401 || error.statusCode == 403) {
            tokenStore.clear()
            mutableState.update {
                it.copy(isConnected = false, accountName = null, isSyncEnabled = false, error = "Torna a connectar MAL.")
            }
            throw MalAuthorizationRequiredException()
        }
        throw error
    }

    private suspend fun markOldestPendingFailure(message: String?, retryable: Boolean): String? {
        val item = mediaDao.getPendingMalSyncQueue(1).firstOrNull() ?: return null
        val animeTitle = mediaDao.getMediaItem(item.mediaItemId)?.title
        mediaDao.insertMalSyncQueueItem(
            item.copy(
                state = if (retryable) PendingState else FailedState,
                attemptCount = item.attemptCount + 1,
                lastError = message?.take(MaxStoredErrorLength),
                lastAttemptAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        mutableState.update { it.copy(error = message) }
        return animeTitle
    }

    private suspend fun oldestPendingTitle(): String? = mediaDao.getPendingMalSyncQueue(1)
        .firstOrNull()
        ?.let { mediaDao.getMediaItem(it.mediaItemId)?.title }

    private fun initialState(): MalSyncState {
        val tokens = tokenStore.readTokens()
        return MalSyncState(
            isAvailable = clientId.isNotBlank(),
            isConnected = tokens != null,
            accountName = tokens?.accountName,
            isSyncEnabled = tokens != null && tokenStore.isSyncEnabled(),
        )
    }

    private fun isExpectedRedirect(uri: Uri): Boolean {
        val expected = Uri.parse(redirectUri)
        return uri.scheme == expected.scheme && uri.authority == expected.authority
    }
}

object MalSyncScheduler {
    private const val UniqueWorkName = "omnilog_mal_sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<MalSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

class MalSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? ContentTrackerApplication ?: return Result.failure()
        return when (
            application.malSyncManager.processPending(
                isFinalAttempt = runAttemptCount >= MaxWorkerRetries,
            )
        ) {
            MalWorkerOutcome.Success -> Result.success()
            MalWorkerOutcome.AuthorizationRequired -> Result.success()
            MalWorkerOutcome.Retry -> if (runAttemptCount < MaxWorkerRetries) Result.retry() else Result.failure()
        }
    }
}

private class MalAuthorizationRequiredException : Exception()

private fun secureRandomString(byteCount: Int): String {
    val bytes = ByteArray(byteCount).also(SecureRandom()::nextBytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

private const val AuthorizationUrl = "https://myanimelist.net/v1/oauth2/authorize"
private const val PendingState = "Pending"
private const val SyncedState = "Synced"
private const val FailedState = "Failed"
private const val BatchSize = 25
private const val MaxItemsPerRun = 100
private const val MaxStoredErrorLength = 300
private const val RefreshLeewayMillis = 60_000L
private const val MaxWorkerRetries = 5
