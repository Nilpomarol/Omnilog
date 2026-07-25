package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.database.entity.MalSyncQueueEntity

internal const val MalSyncPendingState = "Pending"
internal const val MalSyncSyncedState = "Synced"
internal const val MalSyncFailedState = "Failed"

internal fun isMalPayloadAlreadySynced(
    lastSyncedPayloadHash: String?,
    payloadHash: String,
    force: Boolean,
): Boolean = !force && lastSyncedPayloadHash == payloadHash

internal fun MalSyncQueueEntity.afterSuccessfulPayload(
    sentPayloadHash: String,
    latestPayloadHash: String?,
    attemptedAtEpochMillis: Long,
    succeededAtEpochMillis: Long,
): MalSyncQueueEntity = copy(
    // A newer local mutation remains pending even though MAL accepted the older request.
    state = if (latestPayloadHash == sentPayloadHash) MalSyncSyncedState else MalSyncPendingState,
    attemptCount = 0,
    lastError = null,
    lastAttemptAtEpochMillis = attemptedAtEpochMillis,
    lastSuccessAtEpochMillis = succeededAtEpochMillis,
    lastSyncedPayloadHash = sentPayloadHash,
)

internal fun MalSyncQueueEntity.forRetry(now: Long): MalSyncQueueEntity = copy(
    state = MalSyncPendingState,
    attemptCount = 0,
    lastError = null,
    updatedAtEpochMillis = now,
)
