package com.nilpo.contenttracker.core.timeline

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class TimelineHeatmapDay(
    val date: LocalDate,
    val entryCount: Int,
    val isInFuture: Boolean,
)

data class TimelineSummary(
    val currentStreak: Int,
    val bestStreak: Int,
    /** Monday-first rows, oldest week first. The last row is the week containing today. */
    val heatmapWeeks: List<List<TimelineHeatmapDay>>,
    val totalsByUnit: Map<TimelineProgressUnit, Int>,
    val completedCount: Int,
) {
    val busiestDayCount: Int = heatmapWeeks.flatten().maxOfOrNull { it.entryCount } ?: 0

    val hasActivity: Boolean = currentStreak > 0 || completedCount > 0 || totalsByUnit.isNotEmpty()
}

/**
 * Aggregates already-filtered entries into the header summary.
 *
 * [today] is a parameter rather than a `LocalDate.now()` call so streaks and the heatmap window are
 * testable and so one render cannot straddle midnight. Undated entries take no part: they have no
 * day to land on, and counting them would inflate streaks with unknown dates.
 */
fun List<TimelineEntry>.toSummary(
    today: LocalDate,
    weeks: Int = 3,
): TimelineSummary {
    val countsByDate = mutableMapOf<LocalDate, Int>()
    val totalsByUnit = mutableMapOf<TimelineProgressUnit, Int>()
    var completedCount = 0

    forEach { entry ->
        val date = entry.date ?: return@forEach
        countsByDate[date] = (countsByDate[date] ?: 0) + 1
        if (entry.kind == TimelineEntryKind.Completion) completedCount++
        entry.progress?.delta?.let { delta ->
            val unit = entry.mediaType.timelineProgressUnit
            totalsByUnit[unit] = (totalsByUnit[unit] ?: 0) + delta
        }
    }

    val activeDates = countsByDate.keys
    return TimelineSummary(
        currentStreak = currentStreakEndingAt(today, activeDates),
        bestStreak = bestStreak(activeDates),
        heatmapWeeks = heatmapWeeks(today, weeks, countsByDate),
        totalsByUnit = totalsByUnit,
        completedCount = completedCount,
    )
}

/**
 * A streak that ran up to yesterday is still alive — today simply is not over yet. Only a gap of a
 * full day or more ends it, so the number never drops to zero just because it is early.
 */
private fun currentStreakEndingAt(today: LocalDate, activeDates: Set<LocalDate>): Int {
    var cursor = when {
        today in activeDates -> today
        today.minusDays(1) in activeDates -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (cursor in activeDates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun bestStreak(activeDates: Set<LocalDate>): Int {
    if (activeDates.isEmpty()) return 0
    val ordered = activeDates.sorted()
    var best = 1
    var run = 1
    for (index in 1 until ordered.size) {
        run = if (ordered[index - 1].plusDays(1) == ordered[index]) run + 1 else 1
        if (run > best) best = run
    }
    return best
}

private fun heatmapWeeks(
    today: LocalDate,
    weeks: Int,
    countsByDate: Map<LocalDate, Int>,
): List<List<TimelineHeatmapDay>> {
    if (weeks <= 0) return emptyList()
    // Anchored on whole weeks so the columns stay aligned under fixed weekday headings; the tail of
    // the current week renders as future cells rather than shifting everything along.
    val firstMonday = today
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks((weeks - 1).toLong())
    return (0 until weeks).map { week ->
        (0 until 7).map { dayOfWeek ->
            val date = firstMonday.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
            TimelineHeatmapDay(
                date = date,
                entryCount = countsByDate[date] ?: 0,
                isInFuture = date > today,
            )
        }
    }
}
