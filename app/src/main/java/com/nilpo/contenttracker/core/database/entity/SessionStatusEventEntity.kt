package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A dated record of a session changing status.
 *
 * The session's own `status` column is a snapshot: it says what a session is now, with no memory of
 * what it was. That is enough for finishing and for abandoning, which happen once and stop — but not
 * for pausing, which repeats. A session paused in March, resumed in May and paused again in August
 * carries exactly one status, and the first two transitions are simply gone.
 *
 * So pauses need a log rather than a field. Rows are append-only and never rewritten: the point is
 * the sequence, and editing history in place would defeat it.
 *
 * [createdAtEpochMillis] is the only time this table keeps, and the day is derived from it. There was
 * briefly a separate `occurredOnEpochDay` column alongside it, copied from the shape of
 * `progress_updates` — but that table stores its own day because progress genuinely can be
 * back-dated by the user, and a status change cannot. Two columns that must always agree are two
 * columns that can disagree, and they promptly did: one of them was being fed the session's
 * `finishedAt`, which is a different fact entirely and is legitimately unknown.
 */
@Entity(
    tableName = "session_status_events",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("mediaItemId")],
)
data class SessionStatusEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val sessionId: Long,
    val status: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
