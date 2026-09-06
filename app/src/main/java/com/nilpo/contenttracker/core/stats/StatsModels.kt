package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.roundToInt

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

/** One medium's share of the estimated-time waffle, in whole squares. */
data class TimeWaffleSegment(
    val mediaType: MediaType,
    val squares: Int,
)

/**
 * Estimated time as a grid of equal squares.
 *
 * Unlike the consumption pictogram, every square here means the same thing across every medium,
 * because the values have already been converted onto one time axis. That is what makes a single
 * shared grid honest: total area is the total time, and each colour's area is its real share.
 */
data class TimeWaffle(
    val minutesPerSquare: Double,
    val segments: List<TimeWaffleSegment>,
) {
    val totalSquares: Int
        get() = segments.sumOf { segment -> segment.squares }
}

private const val MinWaffleSquares = 8
private const val MaxWaffleSquares = 150

/**
 * A square never stands for more than fifty hours. Past that the unit stops being something a
 * reader can hold — "1 square = 210 h" is an abstraction, not a quantity — so the grid grows
 * instead. It can only hold to [MaxWaffleSquares] squares' worth; beyond that the square has to
 * grow again to keep the grid inside the card.
 */
private const val MaxWaffleMinutesPerSquare = 3_000.0

/**
 * How many squares a total should draw.
 *
 * The grid has to stay inside a card, so square count cannot be proportional to time — at five
 * hours a square, a 4.600 h total would need 920 squares. But a fixed count is worse: it made
 * 200 h and 4.600 h draw near-identical grids, and could even draw a *smaller* grid for a larger
 * total. So the count grows on a log scale: clearly bigger for a bigger total, bounded by the
 * card. Magnitude is read precisely from the headline total and the stated square size; the grid
 * conveys it loosely, plus the composition.
 */
private fun targetWaffleSquares(totalMinutes: Double): Int {
    val hours = totalMinutes / 60.0
    if (hours <= 0.0) return MinWaffleSquares
    return (40.0 * log10(hours) - 20.0)
        .roundToInt()
        .coerceIn(MinWaffleSquares, MaxWaffleSquares)
}

/**
 * Rounding step for the square's own size, coarser as the square grows, so the label stays
 * readable — `50 min`, `2 h 30 min`, `26 h` — without snapping so hard that the count jumps.
 */
/** Readable increments for a square's size, in minutes. */
private val WaffleStepLadder = listOf(5.0, 10.0, 15.0, 20.0, 30.0, 60.0, 120.0, 180.0, 300.0, 600.0)

/**
 * Rounding increment for the square's size, kept proportional to the size itself.
 *
 * A fixed table does not work: a 15-minute step is nothing against a 10-hour square but a quarter
 * of a 65-minute one, and that coarseness made the grid lurch — going from 34 h to 35 h used to
 * lose six squares. Holding the step near a twelfth of the square keeps the label readable while
 * the count moves smoothly.
 */
private fun waffleUnitStep(rawMinutes: Double): Double {
    val budget = (rawMinutes / 12.0).coerceAtLeast(WaffleStepLadder.first())
    return WaffleStepLadder.lastOrNull { step -> step <= budget } ?: WaffleStepLadder.first()
}

/** Snaps a square size to its step, so the label stays a quantity a reader can hold. */
private fun roundToStep(rawMinutes: Double): Double {
    val step = waffleUnitStep(rawMinutes)
    return (round(rawMinutes / step) * step).coerceAtLeast(step)
}

/**
 * Splits the estimated total into whole squares, largest medium first.
 *
 * Uses largest-remainder allocation rather than rounding each medium independently, so the
 * squares always add up to the grid's own total — rounding each share on its own would let the
 * grid claim more or fewer hours than the section reports.
 *
 * A medium whose time is under half a square gets none, and is carried by the legend instead of
 * being inflated to a square it has not earned.
 */
fun estimatedTimeWaffle(
    stats: List<EstimatedTimeStat>,
    maxSquares: Int = MaxWaffleSquares,
): TimeWaffle {
    val positive = stats
        .filter { stat -> stat.minutes > 0.0 }
        .sortedByDescending { stat -> stat.minutes }
    val totalMinutes = positive.sumOf { stat -> stat.minutes }
    if (positive.isEmpty() || totalMinutes <= 0.0) {
        return TimeWaffle(minutesPerSquare = 60.0, segments = emptyList())
    }

    val rawUnit = (totalMinutes / targetWaffleSquares(totalMinutes))
        .coerceAtMost(MaxWaffleMinutesPerSquare)
    val unit = roundToStep(rawUnit)
        // Snapping to a step can round back up over the ceiling, so re-apply it.
        .coerceAtMost(MaxWaffleMinutesPerSquare)
        // Holding the square at fifty hours can push a very large total past the grid's budget.
        // The budget wins: an unbounded grid would run off the card.
        .let { candidate ->
            if (round(totalMinutes / candidate) > maxSquares) {
                roundToStep(totalMinutes / maxSquares)
            } else {
                candidate
            }
        }
    val targetSquares = round(totalMinutes / unit).toInt().coerceIn(1, maxSquares)

    val exact = positive.map { stat -> stat.minutes / unit }
    val counts = exact.map { value -> floor(value).toInt() }.toMutableList()
    var remaining = targetSquares - counts.sum()
    if (remaining > 0) {
        val byRemainder = exact.indices.sortedByDescending { index ->
            exact[index] - floor(exact[index])
        }
        for (index in byRemainder) {
            if (remaining <= 0) break
            counts[index]++
            remaining--
        }
    } else if (remaining < 0) {
        // The cap can bite before allocation does; trim the largest block repeatedly so the grid
        // never exceeds its budget, and never drops a medium that had earned a square. One pass
        // is not enough: a single dominant medium can be several squares over on its own.
        var overflow = -remaining
        while (overflow > 0) {
            val index = counts.indices.maxByOrNull { candidate -> counts[candidate] } ?: break
            if (counts[index] <= 1) break
            counts[index]--
            overflow--
        }
    }

    return TimeWaffle(
        minutesPerSquare = unit,
        segments = positive.mapIndexed { index, stat ->
            TimeWaffleSegment(mediaType = stat.mediaType, squares = counts[index])
        },
    )
}

/**
 * How one consumption total is drawn as a run of blocks.
 *
 * Each medium picks its own [blockUnit], so a row always lands in a readable
 * band instead of becoming three blocks or three hundred. That also means block
 * counts carry no meaning across rows — pages, episodes, minutes, and hours are
 * different units — so the UI must state the unit on every row rather than once
 * for the section.
 */
data class PictogramScale(
    val blockUnit: Int,
    val fullBlocks: Int,
    val hasPartialBlock: Boolean,
) {
    val totalBlocks: Int
        get() = fullBlocks + if (hasPartialBlock) 1 else 0
}

/**
 * Round block sizes, so a row reads `1 bloc = 100` rather than `1 bloc = 93`.
 *
 * Deliberately fine-grained: the unit chosen is the smallest one that still fits, so a sparse
 * ladder would leave rows half empty (a coarse jump from 5 to 10 halves the block count). These
 * steps keep most totals landing near [MaxPictogramBlocks] while staying mentally round.
 */
private val PictogramLadder = listOf(
    1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30, 40, 50, 60, 75, 100, 125, 150, 200, 250, 300,
    400, 500, 600, 750, 1_000, 1_250, 1_500, 2_000, 2_500, 3_000, 4_000, 5_000, 6_000, 7_500,
    10_000, 12_500, 15_000, 20_000, 25_000, 30_000, 40_000, 50_000, 75_000, 100_000,
)

private const val MaxPictogramBlocks = 16

private fun blocksFor(value: Int, unit: Int): Int = (value + unit - 1) / unit

/**
 * Picks the smallest round [PictogramLadder] unit that keeps a total within
 * [MaxPictogramBlocks]. Totals too large for the ladder fall back to an exact
 * division, trading a round unit for a bounded row.
 */
fun pictogramScale(value: Int): PictogramScale {
    if (value <= 0) return PictogramScale(blockUnit = 1, fullBlocks = 0, hasPartialBlock = false)

    val unit = PictogramLadder.firstOrNull { candidate -> blocksFor(value, candidate) <= MaxPictogramBlocks }
        ?: blocksFor(value, MaxPictogramBlocks)

    return PictogramScale(
        blockUnit = unit,
        fullBlocks = value / unit,
        hasPartialBlock = value % unit != 0,
    )
}

/**
 * The genre pie's slices: the strongest genre mentions plus one aggregated
 * remainder. Slices are mention counts, not exclusive title shares — a title
 * with several genres contributes to several slices.
 */
data class GenrePieData(
    val top: List<RankedStat>,
    val othersMentions: Int,
)

fun genrePieData(stats: List<RankedStat>, topCount: Int = 5): GenrePieData {
    val ranked = stats.filter { stat -> stat.value > 0 }
    val top = ranked.take(topCount)
    return GenrePieData(
        top = top,
        othersMentions = ranked.drop(topCount).sumOf { stat -> stat.value },
    )
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
