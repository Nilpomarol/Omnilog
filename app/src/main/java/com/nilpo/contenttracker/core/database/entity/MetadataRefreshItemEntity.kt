package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One snapshot item in a [MetadataRefreshRunEntity]. */
@Entity(
    tableName = "metadata_refresh_items",
    foreignKeys = [
        ForeignKey(
            entity = MetadataRefreshRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["runId", "mediaItemId"], unique = true),
        Index("runId"),
        Index("mediaItemId"),
        Index("state"),
    ],
)
data class MetadataRefreshItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val mediaItemId: Long?,
    val state: String,
    val attemptCount: Int = 0,
    val retryable: Boolean = false,
    val lastError: String? = null,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)
