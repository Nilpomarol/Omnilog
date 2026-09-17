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
    /**
     * Distinct titles finished again in the period, and the same count split by medium.
     *
     * Counted from completions rather than from every repeat session, so these are always a
     * subset of [uniqueTitlesCompleted] and the pair can honestly be read as "N of M titles".
     * [revisitCount] remains the looser session tally and is a different number by design.
     */
    val revisitedTitles: Int,
    val revisitedTitleBreakdown: List<RevisitStats>,
    val topGenres: List<RankedStat>,
    val topCreators: List<RankedStat>,
    val creatorStats: List<CreatorStat>,
    val languageBreakdown: LanguageCoverage,
    val bestRatedItems: List<RatedMediaStat>,
    val bestRatedCollections: List<CollectionRatingStat>,
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

/**
 * [lengths] holds one measured length per completed title, in the medium's own unit, so the chart
 * can draw the spread rather than only its summary. Average, range and sample count are all derived
 * from that one list: an average length means nothing on its own — 342 pages is only long or short
 * relative to the rest of what you read — and a mean alone also hides the single 120-episode series
 * that produced it. Deriving rather than storing them keeps the mark, the dots and the end labels
 * describing the same population.
 */
data class MediumStats(
    val mediaType: MediaType,
    val completionSessionCount: Int,
    val averageRating: Double?,
    val measuredTitles: List<LengthSample>,
    val totalLength: Int,
) {
    val lengths: List<Int> get() = measuredTitles.map { sample -> sample.length }
    val averageLength: Double? get() = lengths.takeIf { values -> values.isNotEmpty() }?.average()
    // The list is sorted, so the ends are the extremes and no separate scan can disagree with it.
    val shortest: LengthSample? get() = measuredTitles.firstOrNull()
    val longest: LengthSample? get() = measuredTitles.lastOrNull()
    val shortestLength: Int? get() = shortest?.length
    val longestLength: Int? get() = longest?.length
    val lengthSamples: Int get() = measuredTitles.size
}

/**
 * One completed title's length in its medium's own unit, kept with the title so the ends of the
 * strip can be named rather than left as anonymous dots.
 */
data class LengthSample(
    val length: Int,
    val title: String,
)

/**
 * Below this many measured titles a medium shows its average without a strip: with one title the
 * shortest, longest and average are the same number, and the axis would draw a full-width claim
 * about a spread that was never observed.
 */
const val MinTitlesForLengthRange = 3

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
    /** Out of ten, so a half point survives: 7,5 is 7.5 here. */
    val bestScore: Double,
)

data class RankedStat(
    val label: String,
    val value: Int,
)

/**
 * A creator you have finished work by, with enough to show a card for them.
 *
 * [averageRating] is null when fewer than [MinRatedTitlesForCreatorAverage] of their titles carry
 * a rating: a single score is not an average, and showing one would let one title outrank a body
 * of work. Each rated title contributes once however many times it was rated, so a title you
 * scored on three passes does not outweigh three separate titles.
 */
data class CreatorStat(
    val name: String,
    val completedTitles: Int,
    val ratedTitles: Int,
    val averageRating: Double?,
    val mediaType: MediaType,
    val coverUrls: List<String>,
)

const val MinRatedTitlesForCreatorAverage = 2

/**
 * A collection ranked by how you scored the titles in it.
 *
 * Unlike [CreatorStat] this one is ranked by the rating rather than merely showing it, so the
 * threshold is a gate on entry: a collection with fewer than
 * [MinRatedTitlesForCollectionAverage] rated titles is left out entirely instead of appearing
 * without a score. A single rating is not an average, and single ratings sit at the extremes, so
 * admitting them would hand the top of the list to the collections you have barely touched.
 *
 * As in [CreatorStat], each rated title contributes once however many times it was rated.
 */
data class CollectionRatingStat(
    val id: Long,
    val name: String,
    val ratedTitles: Int,
    val averageRating: Double,
    val mediaType: MediaType,
    val coverUrls: List<String>,
)

const val MinRatedTitlesForCollectionAverage = 2

/**
 * Completed-title count per normalized language code (`ItemLanguage.normalize`);
 * a null [code] groups titles without a usable stored language. The UI resolves
 * codes to display labels so legacy stored variants share one friendly bucket.
 */
data class LanguageStat(
    val code: String?,
    val value: Int,
)

/**
 * The language split, restricted to the media where you actually record a language.
 *
 * Pooling every medium made the chart mostly "unknown", and that share measured which media you
 * consume rather than which languages you use: books nearly always carry a language, the rest
 * nearly never do. So a medium earns its way in by clearing both
 * [MinLanguageCoverageShare] and [MinLanguageRecordedTitles], and the ones that do not are named
 * in [excludedMediaTypes] instead of being folded in as an unknown block.
 *
 * Both gates are needed: the share alone admits a medium with one tagged title out of one, and the
 * count alone admits a medium with five tagged out of two hundred.
 *
 * [stats] never contains a null code — untagged titles inside an included medium are left out of
 * the shape and accounted for by [recordedTitles] against [totalTitles], which the UI must state.
 */
data class LanguageCoverage(
    val stats: List<LanguageStat>,
    val includedMediaTypes: List<MediaType>,
    val excludedMediaTypes: List<MediaType>,
    val recordedTitles: Int,
    val totalTitles: Int,
)

const val MinLanguageCoverageShare = 0.5
const val MinLanguageRecordedTitles = 3

/**
 * Estimated consumption time per medium, in minutes. Fixed approximate
 * factors convert each medium's native unit onto one time axis so consumption
 * becomes comparable across media. These are estimates by design and must be
 * presented as such; the native-unit totals remain the source of truth.
 */
data class EstimatedTimeStat(
    val mediaType: MediaType,
    val minutes: Double,
)

/** Approximate conversion factors, kept in one place so they are easy to tune. */
object EstimatedTimeFactors {
    const val MinutesPerPage = 1.25
    const val MinutesPerAnimeEpisode = 22.0
    const val MinutesPerTvEpisode = 50.0
    const val MinutesPerMovieMinute = 1.0
    const val MinutesPerGameHour = 60.0
}

fun estimatedTimeByMedium(totals: List<ProgressTotalStats>): List<EstimatedTimeStat> {
    return totals
        .filter { total -> total.value > 0 }
        .map { total ->
            EstimatedTimeStat(
                mediaType = total.mediaType,
                minutes = total.value * total.mediaType.estimatedMinutesPerUnit(),
            )
        }
}

private fun MediaType.estimatedMinutesPerUnit(): Double {
    return when (this) {
        MediaType.Book -> EstimatedTimeFactors.MinutesPerPage
        MediaType.Anime -> EstimatedTimeFactors.MinutesPerAnimeEpisode
        MediaType.TvShow -> EstimatedTimeFactors.MinutesPerTvEpisode
        MediaType.Movie -> EstimatedTimeFactors.MinutesPerMovieMinute
        MediaType.Game -> EstimatedTimeFactors.MinutesPerGameHour
    }
}

/**
 * Index pairs of [points] whose trend segment may be drawn. Only adjacent
 * months that both have ratings connect, so months without ratings render as
 * a break in the line instead of a fabricated continuous trend.
 */
fun connectedRatingTrendSegments(points: List<RatingTrendPoint>): List<Pair<Int, Int>> {
    return (0 until points.size - 1)
        .filter { index ->
            points[index].averageRating != null && points[index + 1].averageRating != null
        }
        .map { index -> index to index + 1 }
}

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
