package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable lifecycle record for one provider import and its metadata enrichment work. */
@Entity(
    tableName = "import_batches",
    indices = [Index("state"), Index("updatedAtEpochMillis")],
)
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,
    val state: String,
    val totalCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val continuationUrl: String? = null,
    val diagnostic: String? = null,
    val reconnectRequired: Boolean = false,
)
