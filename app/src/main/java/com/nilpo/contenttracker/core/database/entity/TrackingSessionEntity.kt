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
    indices = [
        Index("mediaItemId"),
        // Child activity rows carry both ids for efficient media relations. This unique parent key
        // lets their composite foreign key prove that both ids describe the same session.
        Index(value = ["id", "mediaItemId"], unique = true),
    ],
)
data class TrackingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val sessionNumber: Int,
    val status: String,
    val progressCurrent: Int = 0,
    /**
     * Progress that predates tracking: where the user already was when this session was created,
     * from adding something already finished or already part-way through.
     *
     * It lives here rather than as a progress row because it is a position, not a sitting. Keeping
     * it as a row meant one flag had to mean "not activity", "not an objective" and "still counts
     * toward the total" at once, which is three jobs no boolean can hold.
     *
     * `progressCurrent` always equals this plus the sum of the session's entries.
     */
    val baselineProgress: Int = 0,
    val rating: Int? = null,
    val notes: String? = null,
    val platformName: String? = null,
    val platformType: String? = null,
    val startedAtEpochDay: Long? = null,
    val finishedAtEpochDay: Long? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
