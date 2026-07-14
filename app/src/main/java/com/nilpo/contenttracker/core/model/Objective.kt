package com.nilpo.contenttracker.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class ObjectiveMetric {
    CompletedTitles,
    ProgressUnits,
}

enum class ObjectiveUnit {
    Titles,
    Pages,
    Episodes,
    Minutes,
    Hours,
}

/**
 * The semantic definition behind an objective. Progress units only make sense for one concrete
 * media type because pages, episodes, minutes, and hours cannot be added together.
 */
data class ObjectiveDefinition(
    val metric: ObjectiveMetric,
    val mediaType: MediaType?,
) {
    val canonicalUnit: ObjectiveUnit?
        get() = when (metric) {
            ObjectiveMetric.CompletedTitles -> ObjectiveUnit.Titles
            ObjectiveMetric.ProgressUnits -> mediaType?.progressObjectiveUnit()
        }

    val isValid: Boolean
        get() = canonicalUnit != null
}

data class Objective(
    val id: Long = 0,
    val name: String,
    val metric: ObjectiveMetric,
    val unit: ObjectiveUnit,
    val mediaType: MediaType? = null,
    val targetValue: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val archivedAtEpochMillis: Long? = null,
)

fun Objective.definition(): ObjectiveDefinition = ObjectiveDefinition(
    metric = metric,
    mediaType = mediaType,
)

/**
 * Returns the unit implied by the objective definition. The stored unit remains a fallback for
 * legacy or externally-created records so they can still be displayed while being repaired.
 */
fun Objective.canonicalUnit(): ObjectiveUnit = definition().canonicalUnit ?: unit

fun MediaType.progressObjectiveUnit(): ObjectiveUnit = when (this) {
    MediaType.Book -> ObjectiveUnit.Pages
    MediaType.Anime, MediaType.TvShow -> ObjectiveUnit.Episodes
    MediaType.Movie -> ObjectiveUnit.Minutes
    MediaType.Game -> ObjectiveUnit.Hours
}

data class ObjectiveProgress(
    val objective: Objective,
    val currentValue: Int,
) {
    /** Progress as a ratio of the target; values above 1.0 represent overachievement. */
    val percentage: Float
        get() = if (objective.targetValue <= 0) 0f else {
            currentValue.toFloat() / objective.targetValue.toFloat()
        }

    val isComplete: Boolean
        get() = currentValue >= objective.targetValue

    fun isExpired(today: LocalDate): Boolean = today.isAfter(objective.endDate)
}

enum class ObjectiveStatus {
    Ahead,
    OnTrack,
    Behind,
    Completed,
    Missed,
}

/**
 * Time-based pace information for an objective, derived from where [today] sits inside the
 * objective's date window versus how much progress has actually been made.
 */
data class ObjectivePace(
    val status: ObjectiveStatus,
    /** Where progress "should" be today, 0f..1f, spread linearly across the window. */
    val expectedFraction: Float,
    /** Whole days left after today (0 on or after the end date). */
    val daysRemaining: Int,
    /** Units still needed per remaining day (today included) to reach the target. */
    val remainingPerDay: Double,
    /**
     * Current value minus where progress "should" be today. Positive = that many units ahead of
     * the expected pace; negative = that many units to make up to get back on track.
     */
    val unitsVsExpected: Int,
    val isExpired: Boolean,
) {
    val isBehind: Boolean get() = status == ObjectiveStatus.Behind
}

/**
 * Computes pace for this progress snapshot. Pure: pass [today] explicitly so it stays testable
 * and independent of the value the [ObjectiveCalculator] was built with.
 *
 * [tolerance] is the band around the expected fraction within which progress counts as "on track".
 */
fun ObjectiveProgress.pace(
    today: LocalDate = LocalDate.now(),
    tolerance: Float = 0.05f,
): ObjectivePace {
    val start = objective.startDate
    val end = objective.endDate
    val spanDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
    val expectedFraction = when {
        !today.isAfter(start) -> 0f
        !today.isBefore(end) -> 1f
        spanDays <= 0L -> 1f
        else -> (ChronoUnit.DAYS.between(start, today).toFloat() / spanDays.toFloat())
    }.coerceIn(0f, 1f)

    val daysRemaining = ChronoUnit.DAYS.between(today, end).coerceAtLeast(0L).toInt()
    val expired = today.isAfter(end)
    val remainingUnits = (objective.targetValue - currentValue).coerceAtLeast(0)
    // Include today in the working days so the last day still asks for one day's worth.
    val workingDays = if (expired) 0 else daysRemaining + 1
    val remainingPerDay = when {
        remainingUnits == 0 -> 0.0
        workingDays <= 0 -> remainingUnits.toDouble()
        else -> ceil(remainingUnits.toDouble() / workingDays.toDouble())
    }

    val status = when {
        isComplete -> ObjectiveStatus.Completed
        expired -> ObjectiveStatus.Missed
        percentage >= expectedFraction + tolerance -> ObjectiveStatus.Ahead
        percentage < expectedFraction - tolerance -> ObjectiveStatus.Behind
        else -> ObjectiveStatus.OnTrack
    }

    val expectedValue = (expectedFraction * objective.targetValue).roundToInt()

    return ObjectivePace(
        status = status,
        expectedFraction = expectedFraction,
        daysRemaining = daysRemaining,
        remainingPerDay = remainingPerDay,
        unitsVsExpected = currentValue - expectedValue,
        isExpired = expired,
    )
}
