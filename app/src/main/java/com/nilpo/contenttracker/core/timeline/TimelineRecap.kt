package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaType
import java.time.LocalDate
import java.time.YearMonth

/** One finished title, reduced to what the recap actually draws. */
data class TimelineRecapItem(
    val mediaItemId: Long,
    val title: String,
    val coverUrl: String?,
)

/** How many things of one medium were finished — comparable across media in a way units are not. */
data class TimelineRecapTypeCount(
    val type: MediaType,
    val count: Int,
)

/** One month of the recap's window, and what was finished in it. */
data class TimelineRecapBucket(
    val month: YearMonth,
    val count: Int,
)

/**
 * A recap of whatever the timeline's filters currently select.
 *
 * This replaces a rolling three-week window that described "now" no matter which year the filter
 * above it had chosen. Everything here is scoped to the same entries the list below is showing, so
 * the card is a caption for the filter rather than a widget that ignores it.
 *
 * Deliberately built from completions rather than from status: a completion is a dated event, so it
 * can be attributed to a period. The media item's own status is a snapshot with no history, which is
 * why "how many did I drop in 2025" cannot be answered here yet.
 */
data class TimelineRecap(
    val completedCount: Int,
    /**
     * Every finished title in the period, newest first.
     *
     * Uncapped because the card scrolls them: a cap would need a "+N" tile to stay honest, and that
     * tile only ever repeated the count already standing in the headline.
     */
    val completed: List<TimelineRecapItem>,
    /** Largest first, so the medium that defined the period leads. */
    val countsByType: List<TimelineRecapTypeCount>,
    /** Always twelve, oldest first. */
    val buckets: List<TimelineRecapBucket>,
    /** The year the card covers — the selected one, or the current one when no year is filtered. */
    val year: Int,
    /** Titles advanced but not finished. The card falls back to this when nothing was finished. */
    val inProgressCount: Int,
) {
    val busiestBucket: Int = buckets.maxOfOrNull { it.count } ?: 0

    /** False only when the window is genuinely empty, in which case the card should not appear. */
    val hasContent: Boolean = completedCount > 0 || inProgressCount > 0
}

/** The two ways a session stops for good. Either one ends a title's claim to being in progress. */
private val endingKinds = setOf(TimelineEntryKind.Completion, TimelineEntryKind.Dropped)

/**
 * The kinds that mean a title actually moved.
 *
 * Pausing is deliberately absent: setting something aside is not advancing it, and the fallback line
 * this feeds says "has avançat en N títols". A pause does not disqualify a title either — it is
 * simply not evidence on its own, so a title paused and later progressed still counts once.
 */
private val advancingKinds = setOf(
    TimelineEntryKind.Progress,
    TimelineEntryKind.Start,
    TimelineEntryKind.Revisit,
    TimelineEntryKind.Resumed,
)

/**
 * Aggregates entries into the recap.
 *
 * Always twelve calendar months of one year: the year chosen by the filter, or [today]'s when none
 * is. A calendar year rather than a rolling window because the recap is read as "what I finished in
 * 2026" — a window that silently reached back into the previous December made the figure disagree
 * with the year printed next to it.
 *
 * The window is applied here rather than trusted from the caller, so every figure on the card comes
 * from the same set of entries the strip was drawn from. Undated entries take no part — they cannot
 * be attributed to a month, and counting them would put figures under a heading they do not belong
 * to.
 */
fun List<TimelineEntry>.toRecap(
    year: Int?,
    today: LocalDate,
): TimelineRecap {
    val recapYear = year ?: today.year
    val months = (1..12).map { month -> YearMonth.of(recapYear, month) }
    val window = months.toSet()

    val dated = filter { entry -> entry.date?.let { YearMonth.from(it) in window } == true }
    val completions = dated
        .filter { it.kind == TimelineEntryKind.Completion }
        .sortedByDescending { it.date }

    // A title counts as in progress when the window holds activity for it but no ending of it.
    // Both ways a session can end disqualify it: something abandoned in March is not still in
    // progress in July, and counting it would make the empty state overstate what is live. Scoped to
    // the window on purpose too — something finished last year and untouched since is not "in
    // progress" in this year's recap.
    val endedIds = dated
        .filter { it.kind in endingKinds }
        .map { it.mediaItemId }
        .toSet()
    val inProgressCount = dated
        .filter { it.kind in advancingKinds }
        .filter { it.mediaItemId !in endedIds }
        .map { it.mediaItemId }
        .distinct()
        .size

    return TimelineRecap(
        completedCount = completions.size,
        completed = completions.map { entry ->
            TimelineRecapItem(
                mediaItemId = entry.mediaItemId,
                title = entry.mediaTitle,
                coverUrl = entry.coverUrl,
            )
        },
        countsByType = completions
            .groupingBy { it.mediaType }
            .eachCount()
            .map { (type, count) -> TimelineRecapTypeCount(type, count) }
            .sortedWith(
                compareByDescending<TimelineRecapTypeCount> { it.count }.thenBy { it.type.name },
            ),
        buckets = months.map { month ->
            TimelineRecapBucket(
                month = month,
                count = completions.count { entry ->
                    entry.date?.let { YearMonth.from(it) } == month
                },
            )
        },
        year = recapYear,
        inProgressCount = inProgressCount,
    )
}
