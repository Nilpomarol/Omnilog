package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/** One imported title's durable resolution and metadata-application state. */
@Entity(
    tableName = "import_batch_items",
    foreignKeys = [
        ForeignKey(
            entity = ImportBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
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
        Index(value = ["batchId", "sourceKey"], unique = true),
        Index("batchId"),
        Index("mediaItemId"),
        Index("state"),
        Index("updatedAtEpochMillis"),
    ],
)
data class ImportBatchItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val sourceKey: String,
    val sourceExternalId: String? = null,
    val normalizedPayloadJson: String? = null,
    val mediaItemId: Long? = null,
    val state: String,
    val providerSource: String? = null,
    val providerExternalId: String? = null,
    val matchKind: String? = null,
    val candidateReferencesJson: String? = null,
    val attemptCount: Int = 0,
    val retryable: Boolean = false,
    val lastError: String? = null,
    val updatedAtEpochMillis: Long,
    val lastAttemptAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val coverageDismissed: Boolean = false,
)
