package com.nilpo.contenttracker.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.CollectionRatingStat
import com.nilpo.contenttracker.core.stats.CreatorStat
import com.nilpo.contenttracker.core.stats.RatingTrendPoint
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSegment
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.core.stats.estimatedTimeByMedium
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.OmnilogDropdownChip
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.home.RhythmComparison
import com.nilpo.contenttracker.ui.home.compactCount
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The period read as a short story rather than a wall of charts: how much you finished, how you
 * rated it, what it was made of, what you liked, what you went back to, and (kept apart because it
 * ignores the period) where the library stands now. Each chapter appears only when it has something
 * to say, and each shelf opens its full list.
 */
@Composable
internal fun StatsScreen(
    items: List<TrackedMedia>,
    // The Registre page's shared filters.
    mediaFilter: StatsMediaFilter,
    period: StatsPeriod,
    onMediaClick: (TrackedMedia) -> Unit,
    onCreatorClick: (String, MediaType) -> Unit,
    onCollectionClick: (Long, MediaType) -> Unit,
    onSeeAll: (kind: StatsListKind, periodCode: String, mediaFilterName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periodCode = period.code()
    val mediaFilterName = mediaFilter.name
    val snapshot = remember(items, period, mediaFilter) {
        StatsCalculator().calculate(items, StatsFilters(period = period, mediaTypes = mediaFilter.types))
    }
    val reveals = rememberSaveable(saver = RevealRegistry.Saver) { RevealRegistry() }
    val scope = "$periodCode|$mediaFilterName"
    val seeAll: (StatsListKind) -> Unit = { onSeeAll(it, periodCode, mediaFilterName) }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            item(key = "lead") {
                StatsLead(snapshot, reveal = reveals.reveal("$scope|lead"))
            }
            if (snapshot.averageRating != null) {
                item(key = "ratings") { RatingsChapter(snapshot, reveals, scope, onMediaClick, onCollectionClick, seeAll) }
            }
            val formats = snapshot.formatRows()
            if (formats.isNotEmpty()) {
                item(key = "formats") { FormatsChapter(formats, reveals, scope) }
            }
            if (snapshot.topGenres.isNotEmpty() || snapshot.creatorStats.isNotEmpty() || snapshot.languageBreakdown.stats.isNotEmpty()) {
                item(key = "tastes") { TastesChapter(snapshot, reveals, scope, onCreatorClick, seeAll) }
            }
            if (snapshot.revisitedTitles > 0 || snapshot.mostRevisitedItems.isNotEmpty()) {
                item(key = "revisits") { RevisitsChapter(snapshot, onMediaClick, seeAll) }
            }
            if (snapshot.statusBreakdown.any { it.value > 0 }) {
                item(key = "library") { LibraryChapter(snapshot, reveal = reveals.reveal("$scope|library")) }
            }
        }
    }
}

/**
 * Which charts have already drawn themselves in.
 *
 * A chart animates the first time it comes into view for a given period and filter, and never again
 * for the same ones: not when it scrolls back into view, not when a list is opened and closed. A
 * change of filter is new data, so its charts draw in again.
 */
private class RevealRegistry(val played: MutableSet<String> = mutableSetOf()) {
    @Composable
    fun reveal(key: String): Float {
        val progress = remember(key) { Animatable(if (key in played) 1f else 0f) }
        LaunchedEffect(key) {
            if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
            played += key
        }
        return progress.value
    }

    companion object {
        val Saver = listSaver<RevealRegistry, String>(save = { it.played.toList() }, restore = { RevealRegistry(it.toMutableSet()) })
    }
}

// ─────────────────────────────────────────────────────────────
// Lead
// ─────────────────────────────────────────────────────────────

/**
 * The period's result, set like Home's rhythm: the count in serif, the comparison as a sentence and
 * the columns under it, then a shelf of supporting figures.
 */
@Composable
private fun StatsLead(snapshot: StatsSnapshot, reveal: Float) {
    val columns = periodColumns(snapshot)
    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(modifier = Modifier.padding(horizontal = DetailGutter), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = snapshot.completionSessions.toString(),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = stringResource(R.string.home_rhythm_completed),
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            RhythmComparison(snapshot)
        }
        if (snapshot.completionSessions == 0) {
            StatsEmptyText(stringResource(R.string.stats_activity_hero_empty))
        } else {
            StackedColumns(
                buckets = columns,
                height = 88.dp,
                description = { bucket -> stringResource(R.string.stats_column_description, bucket.label, bucket.value) },
                reveal = reveal,
                modifier = Modifier.padding(horizontal = DetailGutter),
                labelEvery = ceil(columns.size / 12.0).toInt().coerceAtLeast(1),
                isFuture = { bucket -> bucket.key.length == 7 && YearMonth.parse(bucket.key).isAfter(YearMonth.from(today)) },
            )
        }
        val figures = leadFigures(snapshot)
        if (figures.isNotEmpty()) FigureCarousel(figures)
    }
}

/** Everything the period can say in one number, most telling first. Figures with nothing to say are left out. */
@Composable
private fun leadFigures(snapshot: StatsSnapshot): List<StatsFigure> {
    val locale = LocalConfiguration.current.locales[0]
    val numbers = NumberFormat.getIntegerInstance(locale)
    val months = snapshot.completionSessionsByMonth
    val busiest = months.maxByOrNull { it.value }?.takeIf { it.value > 0 }
    val hours = estimatedTimeByMedium(snapshot.progressTotals).sumOf { it.minutes } / 60.0
    val ratingCount = snapshot.ratingDistribution.sumOf { it.value }
    return listOfNotNull(
        snapshot.averageRating?.let { average ->
            StatsFigure(
                value = average.roundedStatValue(),
                label = stringResource(R.string.stats_figure_average_rating),
                delta = snapshot.deltas.averageRating?.let(::ratingDeltaText),
                deltaColor = snapshot.deltas.averageRating?.let { deltaChipColor(it) },
            )
        },
        StatsFigure(value = numbers.format(hours.roundToInt()), label = stringResource(R.string.stats_figure_hours))
            .takeIf { hours > 0 },
        busiest?.let {
            val month = YearMonth.parse(it.key)
            val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            StatsFigure(
                value = it.value.toString(),
                label = stringResource(
                    R.string.stats_figure_best_month,
                    if (snapshot.filters.period == StatsPeriod.AllTime) "$name ${month.year}" else name,
                ),
            )
        },
        months.size.takeIf { it > 0 && snapshot.completionSessions > 0 }?.let { count ->
            StatsFigure(
                value = (snapshot.completionSessions.toDouble() / count).roundedStatValue(),
                label = stringResource(R.string.stats_figure_per_month),
            )
        },
        StatsFigure(
            value = snapshot.uniqueTitlesCompleted.toString(),
            label = stringResource(R.string.stats_figure_unique_titles),
        ).takeIf { snapshot.uniqueTitlesCompleted != snapshot.completionSessions },
        StatsFigure(
            value = snapshot.revisitCount.toString(),
            label = stringResource(R.string.stats_figure_revisits),
            delta = snapshot.deltas.revisits?.let(::intDeltaText),
            deltaColor = snapshot.deltas.revisits?.let { deltaChipColor(it) },
        ).takeIf { snapshot.revisitCount > 0 },
        StatsFigure(value = ratingCount.toString(), label = stringResource(R.string.stats_figure_ratings)).takeIf { ratingCount > 0 },
    )
}

/**
 * The columns under the lead. A year is always its twelve months, with the ones still to come left
 * blank; twelve months are themselves; all time is one column per year, since a column per month
 * over a whole history is too thin to read.
 */
@Composable
private fun periodColumns(snapshot: StatsSnapshot): List<StatsBucket> {
    val locale = LocalConfiguration.current.locales[0]
    val months = snapshot.completionSessionsByMonth
    return when (val period = snapshot.filters.period) {
        StatsPeriod.AllTime -> months.groupBy { it.key.take(4) }.map { (year, buckets) ->
            StatsBucket(key = year, label = "’" + year.takeLast(2), value = buckets.sumOf { it.value }, segments = buckets.mergedSegments())
        }
        StatsPeriod.Last12Months -> months.map { it.copy(label = monthInitial(it.key, locale)) }
        StatsPeriod.ThisYear, is StatsPeriod.Year -> {
            val year = (period as? StatsPeriod.Year)?.year ?: LocalDate.now().year
            (1..12).map { month ->
                val key = YearMonth.of(year, month).toString()
                (months.firstOrNull { it.key == key } ?: StatsBucket(key = key, label = "", value = 0)).copy(label = monthInitial(key, locale))
            }
        }
    }
}

private fun monthInitial(key: String, locale: java.util.Locale): String =
    YearMonth.parse(key).month.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)

private fun List<StatsBucket>.mergedSegments(): List<StatsSegment> =
    flatMap { it.segments }
        .groupBy { it.mediaType }
        .map { (type, segments) -> StatsSegment(type, segments.sumOf { it.value }) }
        .sortedBy { it.mediaType.ordinal }

// ─────────────────────────────────────────────────────────────
// Ratings
// ─────────────────────────────────────────────────────────────

@Composable
private fun RatingsChapter(
    snapshot: StatsSnapshot,
    reveals: RevealRegistry,
    scope: String,
    onMediaClick: (TrackedMedia) -> Unit,
    onCollectionClick: (Long, MediaType) -> Unit,
    onSeeAll: (StatsListKind) -> Unit,
) {
    val average = snapshot.averageRating ?: return
    val locale = LocalConfiguration.current.locales[0]
    val ratingCount = snapshot.ratingDistribution.sumOf { it.value }
    val trend: List<RatingTrendPoint> = snapshot.ratingTrend.takeLast(12)

    StatsChapter(title = stringResource(R.string.stats_ratings_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = DetailGutter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = average.roundedStatValue(),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                        color = OmnilogTheme.colors.appInk,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.stats_ratings_average_caption),
                            style = MaterialTheme.typography.titleSmall,
                            color = OmnilogTheme.colors.appInk,
                        )
                        Text(
                            text = listOfNotNull(
                                pluralStringResource(R.plurals.stats_ratings_count, ratingCount, ratingCount),
                                snapshot.deltas.averageRating?.let(::ratingDeltaText),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
                StackedColumns(
                    buckets = snapshot.ratingDistribution,
                    height = 64.dp,
                    description = { bucket -> stringResource(R.string.stats_rating_column_description, bucket.label, bucket.value) },
                    reveal = reveals.reveal("$scope|distribution"),
                    modifier = Modifier.padding(horizontal = DetailGutter),
                    showValues = true,
                )
            }
            if (trend.count { it.averageRating != null } >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PartLabel(
                        if (snapshot.filters.period == StatsPeriod.AllTime) {
                            stringResource(R.string.stats_rating_by_month_last_12)
                        } else {
                            stringResource(R.string.stats_rating_by_month)
                        },
                    )
                    RatingTrendLine(
                        points = trend,
                        monthLabel = { monthInitial(it.key, locale) },
                        color = OmnilogTheme.accents.Dashboard,
                        reveal = reveals.reveal("$scope|trend"),
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
            if (snapshot.bestRatedItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PartLabel(stringResource(R.string.stats_best_rated), onSeeAll = { onSeeAll(StatsListKind.BestRated) })
                    CoverShelf(snapshot.bestRatedItems, key = { it.trackedMedia.item.id }) { stat ->
                        ShelfCover(
                            coverUrl = stat.trackedMedia.item.coverUrl,
                            title = displayMediaTitle(stat.trackedMedia.item.title),
                            figure = stat.bestScore.roundedStatValue(),
                            figureColor = stat.trackedMedia.item.type.statsColor(),
                            onClick = { onMediaClick(stat.trackedMedia) },
                        )
                    }
                }
            }
            if (snapshot.bestRatedCollections.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PartLabel(stringResource(R.string.stats_best_rated_collections), onSeeAll = { onSeeAll(StatsListKind.Collections) })
                    CoverShelf(snapshot.bestRatedCollections, key = { it.id }) { stat ->
                        CollectionFan(stat) { onCollectionClick(stat.id, stat.mediaType) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Formats
// ─────────────────────────────────────────────────────────────

private data class FormatRow(
    val mediaType: MediaType,
    val completed: Int,
    val total: Int,
    val minutes: Double,
    val averageLength: Double?,
    val averageRating: Double?,
)

private fun StatsSnapshot.formatRows(): List<FormatRow> {
    val minutes = estimatedTimeByMedium(progressTotals).associate { it.mediaType to it.minutes }
    return MediaType.entries.mapNotNull { type ->
        val medium = mediumStats.firstOrNull { it.mediaType == type }
        val total = progressTotals.firstOrNull { it.mediaType == type }?.value ?: 0
        if ((medium?.completionSessionCount ?: 0) == 0 && total == 0) return@mapNotNull null
        FormatRow(
            mediaType = type,
            completed = medium?.completionSessionCount ?: 0,
            total = total,
            minutes = minutes[type] ?: 0.0,
            averageLength = medium?.averageLength,
            averageRating = medium?.averageRating,
        )
    }
}

/**
 * The formats side by side, one question per part and a form that fits it: completions as bars, what
 * was consumed in each format's own unit, the one shared scale (estimated hours) as a share, ratings
 * as dots on the 0–10 scale, and the typical length. Unlike units never share an axis.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatsChapter(rows: List<FormatRow>, reveals: RevealRegistry, scope: String) {
    val locale = LocalConfiguration.current.locales[0]
    val numbers = NumberFormat.getIntegerInstance(locale)
    val icon: (MediaType) -> @Composable () -> Unit = { type -> { ObjectiveMediaIcon(mediaType = type, accent = type.statsColor(), size = 16.dp) } }

    StatsChapter(title = stringResource(R.string.stats_formats_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            if (rows.count { it.completed > 0 } >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_part_completed))
                    RankedBars(
                        bars = rows.filter { it.completed > 0 }.sortedByDescending { it.completed }.map {
                            RankedBar(it.mediaType.pluralLabel(), it.completed, it.mediaType.statsColor(), leading = icon(it.mediaType))
                        },
                        reveal = reveals.reveal("$scope|formats-completed"),
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
            if (rows.any { it.total > 0 }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_part_consumed))
                    FigureStrip(
                        figures = rows.filter { it.total > 0 }.map {
                            StatsFigure(
                                value = compactCount(it.total, locale),
                                label = "${progressUnitLabel(it.mediaType, it.total)} · ${it.mediaType.pluralLabel()}",
                                color = it.mediaType.statsColor(),
                            )
                        },
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
            val timed = rows.filter { it.minutes > 0.0 }
            if (timed.size >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_part_time))
                    Column(modifier = Modifier.padding(horizontal = DetailGutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShareBar(
                            segments = timed.map { ShareSegment(it.minutes, it.mediaType.statsColor()) },
                            reveal = reveals.reveal("$scope|formats-time"),
                            height = 14.dp,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            timed.forEach {
                                LegendDot(
                                    color = it.mediaType.statsColor(),
                                    text = stringResource(R.string.stats_time_legend, it.mediaType.pluralLabel(), numbers.format((it.minutes / 60).roundToInt())),
                                )
                            }
                        }
                    }
                }
            }
            val rated = rows.filter { it.averageRating != null }
            if (rated.size >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_part_rating))
                    ScaleDots(
                        dots = rated.map { ScaleDot(it.mediaType.pluralLabel(), it.averageRating!!, it.mediaType.statsColor(), icon(it.mediaType)) },
                        maximum = 10.0,
                        reveal = reveals.reveal("$scope|formats-rating"),
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
            val measured = rows.filter { it.averageLength != null }
            if (measured.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_part_length))
                    FigureStrip(
                        figures = measured.map {
                            StatsFigure(
                                value = numbers.format(it.averageLength!!.roundToInt()),
                                label = "${it.mediaType.progressUnit()} · ${it.mediaType.pluralLabel()}",
                                color = it.mediaType.statsColor(),
                            )
                        },
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Tastes
// ─────────────────────────────────────────────────────────────

@Composable
private fun TastesChapter(
    snapshot: StatsSnapshot,
    reveals: RevealRegistry,
    scope: String,
    onCreatorClick: (String, MediaType) -> Unit,
    onSeeAll: (StatsListKind) -> Unit,
) {
    val barColor = OmnilogTheme.accents.Dashboard
    StatsChapter(title = stringResource(R.string.stats_tastes_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            if (snapshot.topGenres.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_genres))
                    RankedBars(
                        bars = snapshot.topGenres.take(6).map { RankedBar(it.label, it.value, barColor) },
                        reveal = reveals.reveal("$scope|genres"),
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                }
            }
            if (snapshot.creatorStats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(stringResource(R.string.stats_top_creators), onSeeAll = { onSeeAll(StatsListKind.Creators) })
                    CoverShelf(snapshot.creatorStats, key = { it.name }) { stat ->
                        CreatorFan(stat) { onCreatorClick(stat.name, stat.mediaType) }
                    }
                }
            }
            val languages = snapshot.languageBreakdown
            if (languages.stats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartLabel(
                        if (languages.excludedMediaTypes.isEmpty()) {
                            stringResource(R.string.stats_languages)
                        } else {
                            stringResource(R.string.stats_languages_scoped, languages.includedMediaTypes.map { it.pluralLabel() }.joinToString(", "))
                        },
                    )
                    RankedBars(
                        bars = languages.stats.take(6).map { stat ->
                            val label = stat.code?.let { languageLabel(it).ifBlank { it } } ?: stringResource(R.string.stats_language_unknown)
                            RankedBar(label, stat.value, barColor)
                        },
                        reveal = reveals.reveal("$scope|languages"),
                        modifier = Modifier.padding(horizontal = DetailGutter),
                    )
                    // A footnote after the bars, and only when some titles have no language.
                    if (languages.recordedTitles < languages.totalTitles) {
                        Text(
                            text = stringResource(R.string.stats_languages_coverage, languages.recordedTitles, languages.totalTitles),
                            modifier = Modifier.padding(horizontal = DetailGutter),
                            style = MaterialTheme.typography.labelMedium,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Revisits
// ─────────────────────────────────────────────────────────────

@Composable
private fun RevisitsChapter(snapshot: StatsSnapshot, onMediaClick: (TrackedMedia) -> Unit, onSeeAll: (StatsListKind) -> Unit) {
    StatsChapter(title = stringResource(R.string.stats_revisit_breakdown)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = DetailGutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = snapshot.revisitedTitles.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = stringResource(R.string.stats_revisit_share_caption, snapshot.uniqueTitlesCompleted, snapshot.revisitCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            if (snapshot.mostRevisitedItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PartLabel(stringResource(R.string.stats_most_revisited_part), onSeeAll = { onSeeAll(StatsListKind.Revisited) })
                    CoverShelf(snapshot.mostRevisitedItems, key = { it.trackedMedia.item.id }) { stat ->
                        ShelfCover(
                            coverUrl = stat.trackedMedia.item.coverUrl,
                            title = displayMediaTitle(stat.trackedMedia.item.title),
                            figure = stringResource(R.string.stats_revisit_value, stat.value),
                            figureColor = stat.trackedMedia.item.type.statsColor(),
                            onClick = { onMediaClick(stat.trackedMedia) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Library now
// ─────────────────────────────────────────────────────────────

@Composable
private fun LibraryChapter(snapshot: StatsSnapshot, reveal: Float) {
    val buckets = TrackingStatus.entries.map { status -> status to (snapshot.statusBreakdown.firstOrNull { it.status == status }?.value ?: 0) }
    StatsChapter(
        title = stringResource(R.string.stats_library_title),
        note = stringResource(R.string.stats_status_breakdown_subtitle),
    ) {
        Column(modifier = Modifier.padding(horizontal = DetailGutter), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ShareBar(segments = buckets.map { (status, count) -> ShareSegment(count.toDouble(), status.stateColor) }, reveal = reveal)
            FigureStrip(
                figures = buckets.map { (status, count) ->
                    StatsFigure(value = count.toString(), label = status.label(), color = if (count > 0) status.stateColor else null)
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Fans: a creator or collection has no cover of its own, so it shows several
// ─────────────────────────────────────────────────────────────

private const val MaxFanCovers = 3
private val FanCoverWidth = 74.dp
private val FanCoverHeight = 106.dp
private val FanCoverOffset = 22.dp

@Composable
private fun CreatorFan(stat: CreatorStat, onClick: () -> Unit) {
    val caption = if (stat.averageRating != null) {
        stringResource(R.string.stats_creator_titles_and_rating, stat.completedTitles, stat.averageRating.roundedStatValue())
    } else {
        stringResource(R.string.stats_creator_titles_only, stat.completedTitles)
    }
    CoverFan(covers = stat.coverUrls, accent = stat.mediaType.statsColor(), title = stat.name, caption = caption, onClick = onClick)
}

@Composable
private fun CollectionFan(stat: CollectionRatingStat, onClick: () -> Unit) {
    CoverFan(
        covers = stat.coverUrls,
        accent = stat.mediaType.statsColor(),
        title = stat.name,
        caption = stringResource(R.string.stats_collection_rated_titles, stat.ratedTitles),
        figure = stat.averageRating.roundedStatValue(),
        onClick = onClick,
    )
}

@Composable
private fun CoverFan(
    covers: List<String>,
    accent: androidx.compose.ui.graphics.Color,
    title: String,
    caption: String,
    onClick: () -> Unit,
    figure: String? = null,
) {
    val shown = covers.take(MaxFanCovers)
    // The same footprint whether there is one cover or three, so neighbours on the shelf line up.
    val width = FanCoverWidth + FanCoverOffset * (MaxFanCovers - 1)
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(title, figure, caption).joinToString(". ")
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.width(width).height(FanCoverHeight + 8.dp)) {
            // Back to front, so the first cover lands on top.
            shown.indices.reversed().forEach { index ->
                MetadataCoverImage(
                    coverUrl = shown[index],
                    modifier = Modifier
                        .padding(start = FanCoverOffset * index, top = 4.dp * index)
                        .width(FanCoverWidth)
                        .height(FanCoverHeight),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            if (shown.isEmpty()) {
                Box(
                    Modifier
                        .width(FanCoverWidth)
                        .height(FanCoverHeight)
                        .background(accent.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            figure?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = accent,
                    maxLines = 1,
                )
            }
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = OmnilogTheme.colors.appInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = caption, style = MaterialTheme.typography.labelSmall, color = OmnilogTheme.colors.appMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

