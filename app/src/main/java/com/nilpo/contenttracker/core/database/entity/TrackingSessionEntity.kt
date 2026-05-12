package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaItemId")],
)
data class TrackingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val sessionNumber: Int,
    val status: String,
    val progressCurrent: Int = 0,
    val progressTotal: Int? = null,
    val rating: Int? = null,
    val notes: String? = null,
    val platformName: String? = null,
    val platformType: String? = null,
    val startedAtEpochDay: Long? = null,
    val finishedAtEpochDay: Long? = null,
)
