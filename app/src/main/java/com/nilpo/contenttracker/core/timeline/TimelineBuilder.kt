package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus

/**
 * Builds consumption facts from the current snapshot models. It deliberately ignores mutable
 * metadata and [TrackingSession.updatedAtEpochMillis], which is not a consumption timestamp.
 */
class TimelineBuilder {
    /**
     * Derives every consumption event in the library, newest first. This walks all sessions and
     * progress updates, so it is the expensive half and belongs off the main thread. The result
     * depends only on [items] — applying filters afterwards needs no rebuild.
     */
    fun buildEntries(items: List<TrackedMedia>): List<TimelineEntry> =
        items.flatMap(::entriesForMedia).sortedWith(entryComparator)

    fun build(
        items: List<TrackedMedia>,
        filters: TimelineFilters = TimelineFilters(),
    ): TimelineSnapshot = buildEntries(items).toSnapshot(filters)

    private fun entriesForMedia(media: TrackedMedia): List<TimelineEntry> =
        media.orderedSessions.flatMap { session ->
            entriesForSession(
                media = media,
                session = session,
                visitNumber = media.visitNumber(session),
                isRevisit = media.isRevisit(session),
            )
        }

    private fun entriesForSession(
        media: TrackedMedia,
        session: TrackingSession,
        visitNumber: Int,
        isRevisit: Boolean,
    ): List<TimelineEntry> {
        val entries = mutableListOf<TimelineEntry>()
        val orderedUpdates = session.progressUpdates.sortedWith(progressUpdateComparator)

        // Entries are increments, so the total at each point is the session's baseline plus
        // everything logged up to here. Nothing needs filtering out: every row is a real sitting,
        // and progress that predates tracking is the baseline rather than a row.
        val runningTotals = mutableMapOf<Long, Int>()
        var runningTotal = session.baselineProgress
        orderedUpdates.forEach { update ->
            runningTotal += update.amount
            runningTotals[update.id] = runningTotal
        }

        orderedUpdates.forEach { update ->
            entries += TimelineEntry(
                stableKey = "progress:${media.item.id}:${session.id}:${update.id}",
                mediaItemId = media.item.id,
                sessionId = session.id,
                mediaType = media.item.type,
                mediaTitle = media.item.title,
                coverUrl = media.item.coverUrl,
                platformName = session.platform?.name,
                creator = media.item.creators.firstOrNull(),
                date = update.loggedAt.takeIf { update.hasKnownDate },
                kind = TimelineEntryKind.Progress,
                visitNumber = visitNumber,
                progress = TimelineProgress(
                    value = runningTotals[update.id] ?: update.amount,
                    delta = update.amount,
                ),
                progressTotal = media.item.progressTotal,
                sortEpochMillis = update.createdAtEpochMillis,
                sourceId = update.id,
            )
        }

        session.startedAt?.let { startedAt ->
            entries += TimelineEntry(
                // The identity stays stable if deleting an earlier session changes this event from
                // a revisit into a first visit (or shifts its displayed visit number).
                stableKey = "start:${media.item.id}:${session.id}",
                mediaItemId = media.item.id,
                sessionId = session.id,
                mediaType = media.item.type,
                mediaTitle = media.item.title,
                coverUrl = media.item.coverUrl,
                platformName = session.platform?.name,
                creator = media.item.creators.firstOrNull(),
                date = startedAt,
                kind = if (isRevisit) TimelineEntryKind.Revisit else TimelineEntryKind.Start,
                visitNumber = visitNumber,
                sourceId = session.id,
            )
        }

        // Pauses and resumes come from the status log, which is the only place a repeated transition
        // survives. They are paired up before anything is emitted, because whether a pause is worth
        // showing depends on how it ended.
        pauseSpans(session.statusEvents).forEach { span ->
            // Set aside and picked up again the same day: that is a tap and its correction, not a
            // break in a reading history. Dropping both is also the only way to undo an accidental
            // pause, since the log itself is append-only — see `TimelineBuilder.pauseSpans`.
            if (span.resumed?.occurredOn == span.paused.occurredOn) return@forEach

            entries += statusEntry(media, session, visitNumber, span.paused, TimelineEntryKind.Paused)
            span.resumed?.let { resumed ->
                entries += statusEntry(media, session, visitNumber, resumed, TimelineEntryKind.Resumed)
            }
        }

        // Terminal transitions are history, even when the session is later reopened. The session
        // fields are only the current snapshot; using them alone made old completions disappear.
        session.statusEvents
            .filter { it.status == TrackingStatus.Completed || it.status == TrackingStatus.Dropped }
            .forEach { event ->
                val isCompletion = event.status == TrackingStatus.Completed
                val terminal = TimelineEntry(
                    stableKey = "status:${media.item.id}:${session.id}:${event.id}",
                    mediaItemId = media.item.id,
                    sessionId = session.id,
                    mediaType = media.item.type,
                    mediaTitle = media.item.title,
                    coverUrl = media.item.coverUrl,
                    platformName = session.platform?.name,
                    creator = media.item.creators.firstOrNull(),
                    date = event.occurredOn,
                    kind = if (isCompletion) TimelineEntryKind.Completion else TimelineEntryKind.Dropped,
                    visitNumber = visitNumber,
                    progressTotal = media.item.progressTotal,
                    rating = session.rating.takeIf { isCompletion },
                    sortEpochMillis = event.createdAtEpochMillis,
                    sourceId = event.id,
                )
                if (isCompletion) {
                    val finalUpdate = orderedUpdates.lastOrNull { update ->
                        update.hasKnownDate && update.loggedAt == event.occurredOn &&
                            update.createdAtEpochMillis <= event.createdAtEpochMillis
                    }
                    if (finalUpdate != null) {
                        entries.removeAll { it.sourceId == finalUpdate.id }
                        entries += terminal.copy(
                            progress = TimelineProgress(
                                value = runningTotals[finalUpdate.id] ?: session.progressCurrent,
                                delta = finalUpdate.amount,
                            ),
                        )
                    } else {
                        entries += terminal
                    }
                } else {
                    entries += terminal
                }
            }

        // Abandoning something is a dated event in its own right, and the only one the session model
        // can already tell us about: `finishedAt` is the day the session stopped, whichever way it
        // stopped. Sessions dropped before the app began dating that transition have no date and
        // produce nothing — inventing one would put the event on a day it did not happen.
        if (
            session.status == TrackingStatus.Dropped && session.finishedAt != null &&
            session.statusEvents.none { it.status == TrackingStatus.Dropped }
        ) {
            entries += TimelineEntry(
                stableKey = "dropped:${media.item.id}:${session.id}",
                mediaItemId = media.item.id,
                sessionId = session.id,
                mediaType = media.item.type,
                mediaTitle = media.item.title,
                coverUrl = media.item.coverUrl,
                platformName = session.platform?.name,
                creator = media.item.creators.firstOrNull(),
                date = session.finishedAt,
                kind = TimelineEntryKind.Dropped,
                visitNumber = visitNumber,
                progressTotal = media.item.progressTotal,
                // Deliberately no rating: abandoning something is not a verdict on it, and the
                // milestone card only shows a score for completions anyway.
                sourceId = session.id,
            )
        }

        if (
            session.status == TrackingStatus.Completed && session.finishedAt != null &&
            session.statusEvents.none { it.status == TrackingStatus.Completed }
        ) {
            val finishedAt = session.finishedAt
            val completion = TimelineEntry(
                stableKey = "completion:${media.item.id}:${session.id}",
                mediaItemId = media.item.id,
                sessionId = session.id,
                mediaType = media.item.type,
                mediaTitle = media.item.title,
                coverUrl = media.item.coverUrl,
                platformName = session.platform?.name,
                creator = media.item.creators.firstOrNull(),
                date = finishedAt,
                kind = TimelineEntryKind.Completion,
                visitNumber = visitNumber,
                progressTotal = media.item.progressTotal,
                rating = session.rating,
                sourceId = session.id,
            )
            // The last entry logged on the finishing day is folded into the completion, so a day
            // that ended a session reads as one event rather than as progress followed by a finish.
            // Corrections no longer need handling here: they are edits to an entry, so there is
            // never a row describing progress the user has since revoked.
            val finalUpdate = orderedUpdates.lastOrNull { update ->
                update.hasKnownDate && update.loggedAt == finishedAt
            }
            if (finalUpdate != null) {
                entries.removeAll { it.sourceId == finalUpdate.id }
                entries += completion.copy(
                    progress = TimelineProgress(
                        value = runningTotals[finalUpdate.id] ?: session.progressCurrent,
                        delta = finalUpdate.amount,
                    ),
                    sortEpochMillis = finalUpdate.createdAtEpochMillis,
                )
            } else {
                entries += completion
            }
        }

        return entries
    }

    /** A pause and the transition that ended it, or null if it is still open. */
    private data class PauseSpan(
        val paused: SessionStatusEvent,
        val resumed: SessionStatusEvent?,
    )

    /**
     * Groups the log into pauses and whatever ended each one.
     *
     * A resume only counts after a pause — otherwise starting a planned title would read as
     * "resumed" — and a second pause with no resume between is the same pause, however the rows came
     * to be written.
     *
     * Completing or abandoning also closes a span, but produces no resume: the ending itself is
     * derived from the session, which holds the dates that predate this log, so emitting it here
     * would double it.
     */
    private fun pauseSpans(events: List<SessionStatusEvent>): List<PauseSpan> {
        val spans = mutableListOf<PauseSpan>()
        var open: SessionStatusEvent? = null
        events.forEach { event ->
            when (event.status) {
                TrackingStatus.Paused -> if (open == null) open = event

                TrackingStatus.InProgress -> open?.let { paused ->
                    spans += PauseSpan(paused, event)
                    open = null
                }

                else -> open?.let { paused ->
                    spans += PauseSpan(paused, resumed = null)
                    open = null
                }
            }
        }
        // Still paused right now: a span with no end is exactly what that means.
        open?.let { spans += PauseSpan(it, resumed = null) }
        return spans
    }

    /**
     * A row derived from the status log. Keyed on the event's own id, so a session that is paused
     * and resumed repeatedly produces a distinct, stable row for each transition.
     */
    private fun statusEntry(
        media: TrackedMedia,
        session: TrackingSession,
        visitNumber: Int,
        event: SessionStatusEvent,
        kind: TimelineEntryKind,
    ) = TimelineEntry(
        stableKey = "status:${media.item.id}:${session.id}:${event.id}",
        mediaItemId = media.item.id,
        sessionId = session.id,
        mediaType = media.item.type,
        mediaTitle = media.item.title,
        coverUrl = media.item.coverUrl,
        platformName = session.platform?.name,
        creator = media.item.creators.firstOrNull(),
        date = event.occurredOn,
        kind = kind,
        visitNumber = visitNumber,
        progressTotal = media.item.progressTotal,
        sortEpochMillis = event.createdAtEpochMillis,
        sourceId = event.id,
    )

    private companion object {
        val progressUpdateComparator = compareBy<ProgressUpdate> { it.loggedAt }
            .thenBy { it.createdAtEpochMillis }
            .thenBy { it.id }

        /**
         * Days run newest first. Within one day, events run with endings/pauses at the top,
         * progress updates in the middle, and starts/resumes at the bottom.
         */
        val entryComparator = compareByDescending<TimelineEntry> { it.date != null }
            .thenByDescending { it.date }
            .thenBy { it.kind.semanticRank }
            .thenBy { it.sortEpochMillis }
            .thenBy { it.sourceId }
            .thenBy { it.stableKey }

        val TimelineEntryKind.semanticRank: Int
            get() = when (this) {
                // Both ways a session can end rank first, along with pauses.
                TimelineEntryKind.Paused,
                TimelineEntryKind.Completion,
                TimelineEntryKind.Dropped,
                -> 1
                TimelineEntryKind.Progress -> 2
                // Starts and resumes rank last, appearing at the bottom of the day.
                TimelineEntryKind.Start,
                TimelineEntryKind.Revisit,
                TimelineEntryKind.Resumed -> 3
            }
    }
}

/**
 * Filters and groups an already-derived, already-sorted entry list. Cheap by comparison with
 * [TimelineBuilder.buildEntries], so screens can call it from composition whenever filters change
 * instead of rebuilding the timeline.
 */
fun List<TimelineEntry>.toSnapshot(filters: TimelineFilters = TimelineFilters()): TimelineSnapshot {
    // Both of these are standing preferences rather than transient chip state, so they shape the
    // unfiltered count and the year list too — the chips must not change what "nothing to show"
    // means.
    val visibleEntries = filter { entry ->
        entry.mediaType !in filters.excludedMediaTypes &&
            (entry.kind != TimelineEntryKind.Progress || entry.mediaType in filters.historyMediaTypes)
    }
    val filteredEntries = visibleEntries.filter { entry ->
        filters.media.includes(entry.mediaType) &&
            (filters.year == null || entry.date?.year == filters.year)
    }
    return TimelineSnapshot(
        groups = filteredEntries
            .groupBy { it.date }
            .map { (date, entries) -> TimelineDayGroup(date, entries) },
        availableYears = visibleEntries.mapNotNull { it.date?.year }.distinct().sortedDescending(),
        unfilteredEntryCount = visibleEntries.size,
    )
}
