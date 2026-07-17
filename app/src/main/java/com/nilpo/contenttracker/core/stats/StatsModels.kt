package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus

sealed interface StatsPeriod {
    data object AllTime : StatsPeriod
    data object ThisYear : StatsPeriod
    data object Last12Months : StatsPeriod
    data class Year(val year: Int) : StatsPeriod
}

data class StatsFilters(
    val period: StatsPeriod = StatsPeriod.ThisYear,
    val mediaTypes: Set<MediaType> = MediaType.entries.toSet(),
)

data class StatsSnapshot(
    val filters: StatsFilters,
    val totalTitles: Int,
    val uniqueTitlesCompleted: Int,
    val completionSessions: Int,
    val activeNow: Int,
    val plannedNow: Int,
    val averageRating: Double?,
    val revisitCount: Int,
    val completionSessionsByMonth: List<StatsBucket>,
    val mediumStats: List<MediumStats>,
    val ratingTrend: List<RatingTrendPoint>,
    val ratingDistribution: List<StatsBucket>,
    val statusBreakdown: List<StatusStatsBucket>,
    val progressTotals: List<ProgressTotalStats>,
    val revisitBreakdown: List<RevisitStats>,
    val topGenres: List<RankedStat>,
    val topCreators: List<RankedStat>,
    val languageBreakdown: List<StatsBucket>,
    val bestRatedItems: List<RatedMediaStat>,
    val mostRevisitedItems: List<RevisitedMediaStat>,
    val topLevelSummary: StatsTopLevelSummary,
    val deltas: PeriodDelta = PeriodDelta(),
)

data class StatsTopLevelSummary(
    val consumptionHighlight: ConsumptionHighlight? = null,
    val observation: StatsObservation? = null,
)

/** A consumption value kept together with the medium that defines its unit. */
data class ConsumptionHighlight(
    val mediaType: MediaType,
    val value: Int,
)

/** Pure, locally generated observations that the UI can render with localized copy. */
sealed interface StatsObservation {
    data class BusiestMonth(
        val key: String,
        val label: String,
        val completionSessions: Int,
    ) : StatsObservation

    data class HighestRatedMedium(
        val mediaType: MediaType,
        val averageRating: Double,
    ) : StatsObservation
}

data class StatsBucket(
    val key: String,
    val label: String,
    val value: Int,
    val segments: List<StatsSegment> = emptyList(),
)

data class StatsSegment(
    val mediaType: MediaType,
    val value: Int,
)

data class MediumStats(
    val mediaType: MediaType,
    val completionSessionCount: Int,
    val averageRating: Double?,
    val averageLength: Double?,
    val totalLength: Int,
)

data class RatingTrendPoint(
    val key: String,
    val label: String,
    val averageRating: Double?,
    val ratingCount: Int,
)

data class StatusStatsBucket(
    val status: TrackingStatus,
    val value: Int,
)

data class ProgressTotalStats(
    val mediaType: MediaType,
    val value: Int,
)

data class RevisitStats(
    val mediaType: MediaType,
    val value: Int,
)

data class RevisitedMediaStat(
    val trackedMedia: TrackedMedia,
    val value: Int,
)

data class RatedMediaStat(
    val trackedMedia: TrackedMedia,
    val bestRating: Int,
)

data class RankedStat(
    val label: String,
    val value: Int,
)

/**
 * The window a [PeriodDelta] is measured against, so the UI can name the
 * comparison basis next to the chip instead of showing a bare arrow.
 */
sealed interface ComparisonBasis {
    /** Year-to-date vs the same date range of [year]. */
    data class SamePeriodOfYear(val year: Int) : ComparisonBasis

    /** A historical calendar year vs the full calendar [year] before it. */
    data class FullYear(val year: Int) : ComparisonBasis

    /** The current 12-month window vs the 12 months immediately before it. */
    data object Previous12Months : ComparisonBasis
}

/**
 * Change vs the previous comparison window for the period-bound KPIs.
 * Each field is null when there is no previous window (AllTime) or no prior
 * data for that field, so the UI can omit the chip instead of showing a
 * misleading zero delta.
 */
data class PeriodDelta(
    val basis: ComparisonBasis? = null,
    val completionSessions: Int? = null,
    val averageRating: Double? = null,
    val revisits: Int? = null,
)
