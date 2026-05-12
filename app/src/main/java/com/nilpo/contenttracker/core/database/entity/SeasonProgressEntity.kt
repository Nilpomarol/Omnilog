package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_progress",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackingSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackingSessionId")],
)
data class SeasonProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackingSessionId: Long,
    val seasonNumber: Int,
    val progressCurrent: Int,
    val progressTotal: Int? = null,
)
