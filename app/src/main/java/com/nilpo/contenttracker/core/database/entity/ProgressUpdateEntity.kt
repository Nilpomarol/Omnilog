package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged sitting. See `docs/omnilog-activity-concept.md`.
 *
 * [amount] is an increment — what was consumed on this occasion — not a running total. Cumulative
 * values were tried first and made every row's meaning depend on its neighbours: correcting one
 * entry silently changed the apparent size of the next, and a correction below the previous value
 * surfaced as negative progress the user never made.
 *
 * Progress that predates tracking is not a row at all. It lives on [TrackingSessionEntity] as a
 * baseline, because where you started is a position rather than something that happened.
 */
@Entity(
    tableName = "progress_updates",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id", "mediaItemId"],
            childColumns = ["sessionId", "mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("mediaItemId"),
        Index("sessionId"),
        Index(value = ["sessionId", "mediaItemId"]),
    ],
)
data class ProgressUpdateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val sessionId: Long,
    val amount: Int,
    val loggedAtEpochDay: Long,
    val hasKnownDate: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /**
     * Whether this entry accumulated over a span rather than happening in one sitting. Defaults to
     * sitting: under-claiming is safer than printing a span that never happened. Set only from the
     * activity surface, never asked at log time.
     */
    val coversPeriod: Boolean = false,
)
