package com.nilpo.contenttracker.core.activity

import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

enum class SessionActivityKind {
    Started,
    Progress,
    Paused,
    Resumed,

    /** Back in progress after a completion or an abandonment. */
    Reopened,

    /** Back on the list. */
    Replanned,
    Completed,
    Dropped,
}

/**
 * One thing that happened in a session: a progress entry, a status transition, or the session's own
 * start/end when no transition records it.
 *
 * A milestone may carry the progress entry it absorbed (see [activity]), so a row can own both a
 * [statusEvent] and a [progress] entry, and each stays independently editable.
 */
data class SessionActivity(
    val kind: SessionActivityKind,
    /** Null when the day is unknown. Never a placeholder. */
    val date: LocalDate?,
    /** The instant this was written. Fixes sequence; zero when nothing recorded it. */
    val recordedAtEpochMillis: Long,
    val progress: ProgressUpdate? = null,
    /** Baseline plus every dated increment up to and including [progress]. */
    val runningTotal: Int? = null,
    val window: ActivityWindow? = null,
    val statusEvent: SessionStatusEvent? = null,
    /**
     * Stable identity: `entry:<id>`, `status:<id>`, or `session:<kind>` for the session's own start
     * and snapshot ending. The start keeps `session:Started` even when a transition backs it.
     */
    val key: String = when {
        kind == SessionActivityKind.Progress -> "entry:${progress?.id}"
        statusEvent != null -> "status:${statusEvent.id}"
        else -> "session:${kind.name}"
    },
)

/**
 * A session's history as both Activitat and the Timeline read it: newest day first, undated rows
 * last (undated progress sits just above a dated start instead, since it can only have followed it),
 * and rows within one day in the order they were recorded.
 *
 * Ordered by date rather than by when rows were written, so running totals always read in sequence
 * and a re-dated entry moves to the day it now claims.
 *
 * Every transition that changed the session's state is a row, because a row the user cannot see is
 * a row they cannot correct. Surface-specific noise rules (the Timeline collapsing same-day
 * pause/resume pairs, or skipping reopenings) are applied by the surface on top of this, never by
 * re-deriving it.
 *
 * Folding: the last entry dated on a completion's day becomes part of that completion, and the first
 * remaining entry dated on the start day becomes part of the start, so a day that began or ended a
 * session reads as one event. Completion claims first when both fall on one day.
 *
 * Running totals accumulate in date order (baseline + increments), whatever order rows are shown in.
 */
fun TrackingSession.activity(): List<SessionActivity> {
    val updates = progressUpdates.sortedWith(
        compareBy<ProgressUpdate>({ it.loggedAt }, { it.createdAtEpochMillis }, { it.id }),
    )
    val totals = mutableMapOf<Long, Int>()
    var total = baselineProgress
    updates.forEach { update ->
        total += update.amount
        totals[update.id] = total
    }
    val claimed = mutableSetOf<Long>()
    fun claim(update: ProgressUpdate?): ProgressUpdate? = update?.also { claimed += it.id }
    fun unclaimedOn(day: LocalDate) = updates.filter { it.id !in claimed && it.hasKnownDate && it.loggedAt == day }

    val events = statusEvents.sortedWith(compareBy({ it.createdAtEpochMillis }, { it.id }))
    // The transition that opened the current run backs the session's start date and merges into its
    // row. Matched by position rather than by day: the two could drift apart before the repository
    // kept them in step, and matching on the day then showed one start twice.
    val startEvent = startedAt?.let {
        events.withIndex().lastOrNull { (index, event) ->
            val from = event.previousStatus ?: events.getOrNull(index - 1)?.status
            event.status == TrackingStatus.InProgress &&
                (from == TrackingStatus.Planned || (from == null && index == 0))
        }?.value
    }

    val rows = mutableListOf<SessionActivity>()
    events.forEachIndexed { index, event ->
        if (event === startEvent) return@forEachIndexed
        val from = event.previousStatus ?: events.getOrNull(index - 1)?.status
        // A transition into the state it left changes nothing.
        if (from == event.status) return@forEachIndexed
        val kind = when (event.status) {
            TrackingStatus.Paused -> SessionActivityKind.Paused
            TrackingStatus.Completed -> SessionActivityKind.Completed
            TrackingStatus.Dropped -> SessionActivityKind.Dropped
            TrackingStatus.Planned -> SessionActivityKind.Replanned
            TrackingStatus.InProgress -> when (from) {
                TrackingStatus.Paused -> SessionActivityKind.Resumed
                TrackingStatus.Completed, TrackingStatus.Dropped -> SessionActivityKind.Reopened
                TrackingStatus.Planned -> SessionActivityKind.Started
                // ponytail: a pre-schema-22 row with no predecessor cannot say what it left, so it is
                // not shown. Add a label for it only if such rows turn out to matter.
                else -> return@forEachIndexed
            }
        }
        val folded = if (kind == SessionActivityKind.Completed && event.hasKnownDate) {
            claim(unclaimedOn(event.occurredOn).lastOrNull { it.createdAtEpochMillis <= event.createdAtEpochMillis })
        } else {
            null
        }
        rows += SessionActivity(
            kind = kind,
            date = event.occurredOn.takeIf { event.hasKnownDate },
            recordedAtEpochMillis = event.createdAtEpochMillis,
            progress = folded,
            runningTotal = folded?.let { totals[it.id] },
            statusEvent = event,
        )
    }

    // Endings that predate the status log survive only as the session's own snapshot.
    val snapshotEnding = when (status) {
        TrackingStatus.Completed -> SessionActivityKind.Completed
        TrackingStatus.Dropped -> SessionActivityKind.Dropped
        else -> null
    }
    val finished = finishedAt
    if (snapshotEnding != null && finished != null && events.none { it.status == status }) {
        val folded = if (snapshotEnding == SessionActivityKind.Completed) claim(unclaimedOn(finished).lastOrNull()) else null
        rows += SessionActivity(
            kind = snapshotEnding,
            date = finished,
            recordedAtEpochMillis = folded?.createdAtEpochMillis ?: updatedAtEpochMillis,
            progress = folded,
            runningTotal = folded?.let { totals[it.id] },
        )
    }

    startedAt?.let { day ->
        val folded = claim(unclaimedOn(day).firstOrNull())
        rows += SessionActivity(
            kind = SessionActivityKind.Started,
            date = day,
            recordedAtEpochMillis = startEvent?.createdAtEpochMillis
                // Without the transition, the first thing written under the session proves it existed.
                ?: (updates.map { it.createdAtEpochMillis } + events.map { it.createdAtEpochMillis })
                    .filter { it > 0L }
                    .minOrNull()
                    ?.minus(1L)
                ?: updatedAtEpochMillis,
            progress = folded,
            runningTotal = folded?.let { totals[it.id] },
            statusEvent = startEvent,
            key = "session:${SessionActivityKind.Started.name}",
        )
    }

    val windows = activityWindows(updates = progressUpdates, sessionStartedAt = startedAt)
    updates.filter { it.id !in claimed }.forEach { update ->
        rows += SessionActivity(
            kind = SessionActivityKind.Progress,
            date = update.loggedAt.takeIf { update.hasKnownDate },
            recordedAtEpochMillis = update.createdAtEpochMillis,
            progress = update,
            runningTotal = totals[update.id],
            window = windows[update.id],
        )
    }

    val sorted = rows.sortedWith(
        compareBy<SessionActivity> { it.date == null }
            .thenByDescending { it.date }
            .thenByDescending { it.recordedAtEpochMillis },
    )
    if (startedAt == null) return sorted
    // Progress can only have come after the start, so undated entries sit just above it, not below.
    val (undated, rest) = sorted.partition { it.date == null && it.kind == SessionActivityKind.Progress }
    val at = rest.indexOfFirst { it.key == "session:${SessionActivityKind.Started.name}" }
    return rest.take(at) + undated + rest.drop(at)
}

/**
 * The earliest day a new ending may carry. An ending cannot come before the session started or
 * before a transition the user already dated; the repository refuses such a save, so the status
 * sheets use this to rule those days out instead of failing silently. Undated transitions carry
 * placeholder days and constrain nothing.
 */
fun TrackingSession.earliestEndingDate(): LocalDate? =
    (listOfNotNull(startedAt) + statusEvents.filter { it.hasKnownDate }.map { it.occurredOn }).maxOrNull()
