package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaType
import java.time.LocalDate

enum class TimelineEntryKind {
    Progress,
    Start,
    Revisit,
    Completion,

    /**
     * Giving up on something, which is as much a part of a consumption history as finishing it.
     *
     * Derived from a session that is [com.nilpo.contenttracker.core.model.TrackingStatus.Dropped]
     * and carries a finish date. A session dropped before the app started dating that transition has
     * no day to land on and produces no entry — see `TimelineBuilder`.
     */
    Dropped,

    /**
     * Setting something aside, and picking it up again.
     *
     * Unlike every other kind these come from the session status log rather than from the session
     * itself, because pausing repeats and a single status column only remembers the last one. They
     * therefore exist only for transitions made after the log was introduced.
     */
    Paused,
    Resumed,
}

data class TimelineProgress(
    val value: Int,
    val delta: Int? = null,
)

data class TimelineEntry(
    val stableKey: String,
    val mediaItemId: Long,
    val sessionId: Long,
    val mediaType: MediaType,
    val mediaTitle: String,
    val coverUrl: String?,
    val date: LocalDate?,
    val kind: TimelineEntryKind,
    val visitNumber: Int,
    val progress: TimelineProgress? = null,
    val progressTotal: Int? = null,
    val rating: Int? = null,
    /** Where it was consumed, when the session recorded it. */
    val platformName: String? = null,
    /** The first credited creator, for the card's supporting line. */
    val creator: String? = null,
    internal val sortEpochMillis: Long = 0,
    internal val sourceId: Long = 0,
) {
    /** Null when the total is unknown, so callers can hide the bar rather than invent a denominator. */
    val progressFraction: Float?
        get() {
            val total = progressTotal?.takeIf { it > 0 } ?: return null
            val current = progress?.value ?: return null
            return (current.toFloat() / total).coerceIn(0f, 1f)
        }
}

/**
 * The unit a media type advances in. Deltas may only be summed within one unit — episodes, pages
 * and minutes are not interchangeable, so a combined "units" total would be meaningless.
 */
enum class TimelineProgressUnit {
    Episodes,
    Pages,
    Minutes,
    Hours,
}

val MediaType.timelineProgressUnit: TimelineProgressUnit
    get() = when (this) {
        MediaType.Anime, MediaType.TvShow -> TimelineProgressUnit.Episodes
        MediaType.Book -> TimelineProgressUnit.Pages
        MediaType.Movie -> TimelineProgressUnit.Minutes
        MediaType.Game -> TimelineProgressUnit.Hours
    }

enum class TimelineMediaFilter {
    All,
    Anime,
    Books,
    MoviesAndTv,
    Games,
    ;

    fun includes(type: MediaType): Boolean = when (this) {
        All -> true
        Anime -> type == MediaType.Anime
        Books -> type == MediaType.Book
        MoviesAndTv -> type == MediaType.Movie || type == MediaType.TvShow
        Games -> type == MediaType.Game
    }
}

/**
 * [historyMediaTypes] lists the types whose per-update progress rows are shown. Completions, starts
 * and revisits are milestones and always pass — only the high-volume progress rows are optional.
 * The default includes every type so the model stays unfiltered unless a caller opts out.
 */
data class TimelineFilters(
    val media: TimelineMediaFilter = TimelineMediaFilter.All,
    val year: Int? = null,
    val excludedMediaTypes: Set<MediaType> = emptySet(),
    val historyMediaTypes: Set<MediaType> = MediaType.entries.toSet(),
)

/**
 * One day's events, plus that day's own totals.
 *
 * The screen renders days as marks on a continuous rail rather than as section headings, so this
 * is a run of the timeline rather than a self-contained section.
 */
data class TimelineDayGroup(
    val date: LocalDate?,
    val entries: List<TimelineEntry>,
) {
    val completedCount: Int = entries.count { it.kind == TimelineEntryKind.Completion }

    /** Kept per unit rather than summed, for the reason given on [TimelineProgressUnit]. */
    val progressByUnit: Map<TimelineProgressUnit, Int> = entries
        .mapNotNull { entry ->
            entry.progress?.delta?.let { delta -> entry.mediaType.timelineProgressUnit to delta }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, deltas) -> deltas.sum() }
}

data class TimelineSnapshot(
    val groups: List<TimelineDayGroup>,
    val availableYears: List<Int>,
    val unfilteredEntryCount: Int,
) {
    val entries: List<TimelineEntry> = groups.flatMap { it.entries }
}
