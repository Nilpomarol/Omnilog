package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class TrackingSession(
    val id: Long,
    val mediaItemId: Long,
    /**
     * Monotonic per-title allocation number, not a position. Deleting a past session does not
     * renumber the survivors — undo reinserts a session under its original number and refuses if
     * that number is taken — so this can be sparse and can start above 1. To ask which time
     * through a title a session was, use [TrackedMedia.visitNumber] or [TrackedMedia.isRevisit].
     */
    val sessionNumber: Int,
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    /**
     * Progress that predates tracking — where the user already was when this session began. Always
     * equal to [progressCurrent] minus the sum of [progressUpdates], and never surfaced as activity:
     * a starting position is not something that happened.
     */
    val baselineProgress: Int = 0,
    /**
     * The personal rating in half points, so 15 is 7,5 out of 10. See [RatingHalfPoints].
     *
     * Named for its unit rather than called `rating` because the unit changed under code that was
     * already reading the field: a name that still compiled everywhere would have silently doubled
     * every score it touched.
     */
    val ratingHalfPoints: Int? = null,
    val notes: String? = null,
    val platform: ConsumptionPlatform? = null,
    val startedAt: LocalDate? = null,
    val finishedAt: LocalDate? = null,
    val updatedAtEpochMillis: Long = 0,
    val progressUpdates: List<ProgressUpdate> = emptyList(),
    /**
     * Dated status changes, oldest first.
     *
     * Empty for every session that has not changed status since the log was introduced — the log is
     * append-only from that point and was not backfilled, because the days those older transitions
     * happened on were never recorded and inventing them would put events on the wrong dates.
     */
    val statusEvents: List<SessionStatusEvent> = emptyList(),
)

/**
 * One dated status change on a session.
 *
 * [status] is the status being moved *into*, so a pause and the resume that follows are two rows
 * rather than one row with a duration. Durations can be derived from the sequence; a sequence cannot
 * be derived from durations.
 */
data class SessionStatusEvent(
    val id: Long,
    val sessionId: Long,
    val previousStatus: TrackingStatus? = null,
    val status: TrackingStatus,
    val occurredOn: LocalDate,
    val createdAtEpochMillis: Long,
)
