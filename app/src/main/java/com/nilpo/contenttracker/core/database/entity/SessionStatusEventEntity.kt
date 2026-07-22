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
 * So transitions need a log rather than a field. Normal state changes append rows. Explicit
 * corrections may re-date or delete one, while [createdAtEpochMillis] keeps sequence stable.
 *
 * Two times, and they mean different things. [createdAtEpochMillis] is when the row was written and
 * fixes the sequence; [occurredOnEpochDay] is the day the user says it happened.
 *
 * An earlier version of this table kept only the first and derived the day from it, on the grounds
 * that a status change cannot be back-dated. That was true only because nothing offered to set it:
 * you could pause a book on Friday, remember on Sunday, and the log would insist it was Sunday. Now
 * that the day is editable the two columns are no longer required to agree, which is exactly the
 * arrangement `progress_updates` already uses for `loggedAtEpochDay` and `createdAtEpochMillis`.
 *
 * Null means the day was never set explicitly and is derived from [createdAtEpochMillis], which is
 * what every row written before the column existed does.
 */
@Entity(
    tableName = "session_status_events",
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
        Index("sessionId"),
        Index("mediaItemId"),
        Index(value = ["sessionId", "mediaItemId"]),
    ],
)
data class SessionStatusEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val sessionId: Long,
    /** The state this transition left. Null only for rows created before schema 22. */
    val previousStatus: String? = null,
    val status: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val occurredOnEpochDay: Long? = null,
)
