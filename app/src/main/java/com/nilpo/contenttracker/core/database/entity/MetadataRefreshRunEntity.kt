package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-started, durable provider-metadata refresh. */
@Entity(
    tableName = "metadata_refresh_runs",
    indices = [Index("state"), Index("updatedAtEpochMillis")],
)
data class MetadataRefreshRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val state: String,
    val totalCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)
