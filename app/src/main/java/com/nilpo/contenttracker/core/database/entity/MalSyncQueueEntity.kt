package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * The latest local state that still needs to be projected to MyAnimeList.
 *
 * One row per title deliberately coalesces rapid progress edits. The worker reads the current
 * session when it sends the row, so MAL receives the app's newest committed state rather than an
 * obsolete series of intermediate payloads.
 */
@Entity(
    tableName = "mal_sync_queue",
    primaryKeys = ["mediaItemId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("state"), Index("updatedAtEpochMillis")],
)
data class MalSyncQueueEntity(
    val mediaItemId: Long,
    val malId: Int,
    val state: String,
    val attemptCount: Int,
    val lastError: String? = null,
    val updatedAtEpochMillis: Long,
    val lastAttemptAtEpochMillis: Long? = null,
    val lastSuccessAtEpochMillis: Long? = null,
    /** SHA-256 fingerprint of the payload most recently accepted by MAL. */
    val lastSyncedPayloadHash: String? = null,
)
