package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.activity.SessionActivity
import com.nilpo.contenttracker.core.activity.SessionActivityKind
import com.nilpo.contenttracker.core.activity.activity
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession

/**
 * Builds consumption facts from the current snapshot models. It ignores mutable media metadata.
 * [TrackingSession.updatedAtEpochMillis] is used only as the last available ordering fallback for
 * a session milestone that predates the immutable status-event log.
 */
class TimelineBuilder {
    /**
     * Derives every consumption event in the library, newest first. This walks all sessions and
     * progress updates, so it is the expensive half and belongs off the main thread. The result
     * depends only on [items] — applying filters afterwards needs no rebuild.
     *
     * Undated rows are kept and sort last. They cannot be placed on a day, but leaving them out hid
     * them entirely; the screen folds them away at the end, where they can be opened and dated.
     */
    fun buildEntries(items: List<TrackedMedia>): List<TimelineEntry> =
        items
            .flatMap(::entriesForMedia)
            .sortedWith(entryComparator)

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
    ): List<TimelineEntry> = session.activity()
        .withoutSameDayCorrections()
        .mapNotNull { row ->
            val kind = when (row.kind) {
                SessionActivityKind.Progress -> TimelineEntryKind.Progress
                SessionActivityKind.Started -> if (isRevisit) TimelineEntryKind.Revisit else TimelineEntryKind.Start
                SessionActivityKind.Completed -> TimelineEntryKind.Completion
                SessionActivityKind.Dropped -> TimelineEntryKind.Dropped
                SessionActivityKind.Paused -> TimelineEntryKind.Paused
                SessionActivityKind.Resumed -> TimelineEntryKind.Resumed
                // Corrections to where a session stands, not consumption events.
                SessionActivityKind.Reopened, SessionActivityKind.Replanned -> return@mapNotNull null
            }
            val progress = row.progress
            TimelineEntry(
                stableKey = row.timelineKey(media.item.id, session.id),
                mediaItemId = media.item.id,
                sessionId = session.id,
                mediaType = media.item.type,
                mediaTitle = media.item.title,
                coverUrl = media.item.coverUrl,
                platformName = session.platform?.name,
                creator = media.item.creators.firstOrNull(),
                date = row.date,
                kind = kind,
                visitNumber = visitNumber,
                progress = progress?.let { TimelineProgress(value = row.runningTotal ?: it.amount, delta = it.amount) },
                // A bare start has no position to measure against a total.
                progressTotal = media.item.progressTotal.takeIf {
                    row.kind != SessionActivityKind.Started || progress != null
                },
                // Abandoning something is not a verdict on it.
                ratingHalfPoints = session.ratingHalfPoints.takeIf { kind == TimelineEntryKind.Completion },
                sortEpochMillis = row.recordedAtEpochMillis,
                sourceId = when {
                    row.kind == SessionActivityKind.Progress -> progress?.id ?: 0
                    row.kind == SessionActivityKind.Started -> session.id
                    else -> row.statusEvent?.id ?: session.id
                },
            )
        }

    /**
     * Set aside and picked up again the same day, or started and put back on the list the same day: a
     * tap and its correction, not something that happened to the title. The chronology drops both; the
     * item's own Activitat keeps them so they stay correctable.
     */
    private fun List<SessionActivity>.withoutSameDayCorrections(): List<SessionActivity> {
        val transitions = filter { it.statusEvent != null }.sortedBy { it.recordedAtEpochMillis }
        val noise = transitions.zipWithNext()
            .filter { (first, next) ->
                val undone = (first.kind == SessionActivityKind.Paused && next.kind == SessionActivityKind.Resumed) ||
                    (first.kind == SessionActivityKind.Started && next.kind == SessionActivityKind.Replanned)
                undone && first.date != null && first.date == next.date
            }
            .flatMap { it.toList() }
            .toSet()
        return filterNot { it in noise }
    }

    /** Keys predate the shared derivation and must not change: the list animates on them. */
    private fun SessionActivity.timelineKey(mediaItemId: Long, sessionId: Long): String = when (key) {
        "session:${SessionActivityKind.Started.name}" -> "start:$mediaItemId:$sessionId"
        "session:${SessionActivityKind.Completed.name}" -> "completion:$mediaItemId:$sessionId"
        "session:${SessionActivityKind.Dropped.name}" -> "dropped:$mediaItemId:$sessionId"
        else -> if (kind == SessionActivityKind.Progress) {
            "progress:$mediaItemId:$sessionId:${progress?.id}"
        } else {
            "status:$mediaItemId:$sessionId:${statusEvent?.id}"
        }
    }

    private companion object {
        /**
         * Days run newest first. Within one day, the most recently recorded event leads, matching
         * the ordering used by the item's activity sheet. Session milestones use their transition,
         * first-child, or session timestamp when available; semantic kind, source id, and stable key
         * settle legacy events whose sequence cannot be recovered and otherwise exact ties.
         */
        val entryComparator = compareByDescending<TimelineEntry> { it.date != null }
            .thenByDescending { it.date }
            .thenByDescending { it.withinDayOrder }
            .thenBy { it.kind.tieBreakRank }
            .thenByDescending { it.sourceId }
            .thenBy { it.stableKey }

        val TimelineEntry.withinDayOrder: Long
            get() = when {
                sortEpochMillis != 0L -> sortEpochMillis
                kind == TimelineEntryKind.Completion || kind == TimelineEntryKind.Dropped -> Long.MAX_VALUE
                kind == TimelineEntryKind.Start || kind == TimelineEntryKind.Revisit -> Long.MIN_VALUE
                else -> 0L
            }

        val TimelineEntryKind.tieBreakRank: Int
            get() = when (this) {
                TimelineEntryKind.Paused,
                TimelineEntryKind.Completion,
                TimelineEntryKind.Dropped,
                -> 1
                TimelineEntryKind.Progress -> 2
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
