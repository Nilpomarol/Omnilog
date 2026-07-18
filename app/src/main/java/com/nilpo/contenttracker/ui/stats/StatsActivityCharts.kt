package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.stats.EstimatedTimeStat
import com.nilpo.contenttracker.core.stats.estimatedTimeWaffle
import com.nilpo.contenttracker.core.stats.MediumStats
import com.nilpo.contenttracker.core.stats.MinTitlesForLengthRange
import com.nilpo.contenttracker.core.stats.connectedRatingTrendSegments
import com.nilpo.contenttracker.core.stats.RatingTrendPoint
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlin.math.roundToInt


@Composable
internal fun MonthlyBarChart(buckets: List<StatsBucket>) {
    val maxValue = buckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val midpoint = maxValue / 2
    val legendMediaTypes = buckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (maxValue > 0) {
                    Column(
                        modifier = Modifier.width(24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                        ) {
                            Text(
                                text = maxValue.toString(),
                                modifier = Modifier.align(Alignment.TopEnd),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogTheme.colors.appMuted,
                                maxLines = 1,
                            )
                            if (midpoint > 0 && midpoint != maxValue) {
                                Text(
                                    text = midpoint.toString(),
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OmnilogTheme.colors.appMuted,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = "0",
                                modifier = Modifier.align(Alignment.BottomEnd),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogTheme.colors.appMuted,
                                maxLines = 1,
                            )
                        }
                        Box(modifier = Modifier.height(18.dp))
                    }
                }
                buckets.forEach { bucket ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            val barHeight = if (maxValue == 0 || bucket.value == 0) {
                                2
                            } else {
                                (95 * bucket.value / maxValue).coerceAtLeast(2)
                            }
                            if (bucket.value > 0 && bucket.segments.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight.dp)
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    bucket.segments.asReversed().forEach { segment ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(segment.value.toFloat())
                                                .background(segment.mediaType.statsColor()),
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight.dp)
                                        .background(
                                            color = if (bucket.value > 0) {
                                                OmnilogColors.Dashboard
                                            } else {
                                                OmnilogTheme.colors.appLine.copy(alpha = 0.58f)
                                            },
                                            shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                                        ),
                                )
                            }
                        }
                        Text(
                            text = bucket.label,
                            modifier = Modifier.height(18.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
        }
    }
}

@Composable
internal fun MonthlyLegend(mediaTypes: List<MediaType>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(mediaTypes, key = { mediaType -> mediaType.name }) { mediaType ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(mediaType.statsColor(), RoundedCornerShape(2.dp)),
                )
                Text(
                    text = mediaType.label(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun CompletionSharePie(stats: List<MediumStats>) {
    val completedStats = stats.filter { stat -> stat.completionSessionCount > 0 }
    val total = completedStats.sumOf { stat -> stat.completionSessionCount }

    if (total == 0) return

    StatsPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1.45f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(168.dp)) {
                    var startAngle = -90f
                    completedStats.forEach { stat ->
                        val sweep = 360f * stat.completionSessionCount.toFloat() / total.toFloat()
                        drawArc(
                            color = stat.mediaType.statsColor(),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(
                modifier = Modifier.weight(0.85f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                completedStats.forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(stat.mediaType.statsColor(), RoundedCornerShape(2.dp)),
                        )
                        Text(
                            text = stat.mediaType.label(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stat.completionSessionCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = stat.mediaType.statsColor(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AverageRatingDotPlot(stats: List<MediumStats>) {
    val ratedStats = stats.filter { stat -> stat.averageRating != null }

    if (ratedStats.isEmpty()) return

    StatsPanel {
        ratedStats.forEach { stat ->
            val averageRating = stat.averageRating ?: return@forEach
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stat.mediaType.label(),
                    modifier = Modifier.width(58.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val trackColor = OmnilogTheme.colors.appLine
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                ) {
                    val y = size.height / 2f
                    drawLine(
                        color = trackColor.copy(alpha = 0.72f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx(),
                    )
                    val x = (averageRating.toFloat() / 10f).coerceIn(0f, 1f) * size.width
                    drawCircle(
                        color = stat.mediaType.statsColor(),
                        radius = 5.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
                Text(
                    text = "%.1f".format(averageRating),
                    modifier = Modifier.width(34.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = stat.mediaType.statsColor(),
                    maxLines = 1,
                )
            }
        }
    }
}

private val LengthStripHeight = 26.dp
private val LengthDotRadius = 3.5.dp
private val LengthTrackStroke = 2.dp
private val LengthMarkStroke = 1.5.dp
private val LengthMarkReach = 7.dp

/**
 * Length per medium as a strip of one dot per completed title, with the average marked.
 *
 * Still no shared scale — pages, episodes, minutes and hours are not comparable magnitudes — but
 * each strip spans that medium's own shortest to longest title, so the average reads as a position
 * among real titles rather than as a bare number with nothing to be large or small against. Showing
 * the individual titles is the point: a mean hides the one 120-episode series that dragged it up,
 * and here that outlier sits visibly alone at the end of the strip. Every row therefore carries its
 * own end labels — the differing endpoints are what stop stacked strips being read as one axis.
 */
@Composable
internal fun AverageLengthRanges(stats: List<MediumStats>) {
    val lengthStats = stats.filter { stat -> stat.averageLength != null }

    if (lengthStats.isEmpty()) return

    StatsPanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            lengthStats.forEach { stat ->
                AverageLengthRow(stat = stat)
            }
        }
    }
}

@Composable
private fun AverageLengthRow(stat: MediumStats) {
    val average = stat.averageLength ?: return
    val accent = stat.mediaType.statsColor()
    val unit = stat.mediaType.progressUnit()
    val shortestSample = stat.shortest
    val longestSample = stat.longest
    val shortest = shortestSample?.length
    val longest = longestSample?.length
    // A range needs enough titles to have been observed, and needs to be a range at all: if every
    // measured title came in at the same length there is no span to place the average within.
    val hasRange = shortest != null && longest != null &&
        longest > shortest && stat.lengthSamples >= MinTitlesForLengthRange
    val averageText = average.roundedStatValue()
    val description = if (hasRange) {
        stringResource(
            R.string.stats_length_range_description,
            stat.mediaType.label(),
            averageText,
            unit,
            stat.lengthSamples,
            stringResource(
                R.string.stats_length_extreme,
                shortest.toString(),
                unit,
                shortestSample?.title.orEmpty(),
            ),
            stringResource(
                R.string.stats_length_extreme,
                longest.toString(),
                unit,
                longestSample?.title.orEmpty(),
            ),
        )
    } else {
        stringResource(
            R.string.stats_length_average_description,
            stat.mediaType.label(),
            averageText,
            unit,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stat.mediaType.label(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.stats_length_average_value, averageText, unit),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
        }
        if (hasRange) {
            val span = (longest - shortest).toFloat()
            val trackColor = OmnilogTheme.colors.appLine.copy(alpha = 0.5f)
            val markColor = OmnilogTheme.colors.appInk
            // Semi-transparent, so titles landing on the same length stack into a darker spot and
            // the strip reads as density rather than as a single title.
            val dotColor = accent.copy(alpha = 0.7f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LengthStripHeight),
            ) {
                val radius = LengthDotRadius.toPx()
                // Inset by the radius at both ends so the shortest and longest titles sit fully
                // inside the strip instead of being clipped in half by its edges.
                val left = radius
                val usable = (size.width - radius * 2f).coerceAtLeast(1f)
                val centerY = size.height / 2f

                drawLine(
                    color = trackColor,
                    start = Offset(left, centerY),
                    end = Offset(left + usable, centerY),
                    strokeWidth = LengthTrackStroke.toPx(),
                    cap = StrokeCap.Round,
                )
                stat.lengths.forEach { length ->
                    val x = left + usable * ((length - shortest) / span).coerceIn(0f, 1f)
                    drawCircle(color = dotColor, radius = radius, center = Offset(x, centerY))
                }
                val averageX = left + usable * (((average - shortest) / span).toFloat().coerceIn(0f, 1f))
                // Short enough to read as a mark on the strip rather than as a divider cutting it.
                val markReach = LengthMarkReach.toPx()
                drawLine(
                    color = markColor,
                    start = Offset(averageX, centerY - markReach),
                    end = Offset(averageX, centerY + markReach),
                    strokeWidth = LengthMarkStroke.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // The titles ride in the existing end labels rather than taking a row of their own, so
            // each end gets half the width and must truncate: the length stays readable because it
            // comes first, and the title is what gives way when it is too long.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.stats_length_extreme,
                        shortest.toString(),
                        unit,
                        shortestSample?.title.orEmpty(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.stats_length_extreme,
                        longest.toString(),
                        unit,
                        longestSample?.title.orEmpty(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}



/**
 * Fixed grid width, so row count reads the same on every screen. Tiles flex to fill the width,
 * which means the gap alone sets how big they are — widening it shrinks the tile and loosens the
 * grid in one move, without touching the column count and so without touching the row count.
 */
private const val WaffleColumns = 14
private val WaffleTileGap = 5.dp

/**
 * Estimated time as a single grid of equal squares.
 *
 * Every square is the same amount of time regardless of colour, which is only defensible because
 * this module has already converted each medium onto one axis. Total area is the estimated total;
 * each colour's area is its real share. Contrast with the consumption pictogram above, where a
 * block means a different thing on every row and counts must not be read across rows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EstimatedTimeWaffle(stats: List<EstimatedTimeStat>) {
    val waffle = estimatedTimeWaffle(stats)
    val visibleStats = stats
        .filter { stat -> stat.minutes > 0.0 }
        .sortedByDescending { stat -> stat.minutes }
    val totalMinutes = visibleStats.sumOf { stat -> stat.minutes }

    if (waffle.segments.isEmpty() || totalMinutes <= 0.0) return

    StatsPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = estimatedTimeLabel(totalMinutes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.stats_estimated_time_center_caption),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The square's value sits with the total rather than under the grid: it is a key to
            // reading the squares, so it belongs before them, not after.
            Text(
                text = stringResource(
                    R.string.stats_estimated_time_waffle_caption,
                    waffleUnitLabel(waffle.minutesPerSquare),
                ),
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
            )
        }
        // A fixed column count, rather than letting tiles wrap wherever they fit: the number of
        // rows is the quickest read of "how much", and it should mean the same thing on every
        // screen. Tiles flex to fill the width instead.
        val tiles = waffle.segments.flatMap { segment ->
            List(segment.squares) { segment.mediaType }
        }
        Column(verticalArrangement = Arrangement.spacedBy(WaffleTileGap)) {
            tiles.chunked(WaffleColumns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WaffleTileGap),
                ) {
                    row.forEach { mediaType ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    color = mediaType.statsColor().copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                    // Hold the last row's tiles to the same size as every other row's.
                    repeat(WaffleColumns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            visibleStats.forEach { stat ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(stat.mediaType.statsColor(), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        text = stat.mediaType.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                    )
                    Text(
                        text = estimatedTimeLabel(stat.minutes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = stat.mediaType.statsColor(),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The waffle square's own size. Spelled out as hours and minutes rather than a decimal, because
 * the square size is derived from the total and lands on values like 150 min — `2 h 30 min` reads
 * better than `2,5 h`, and unlike a rounded decimal it stays exactly what the squares are worth.
 */
internal fun waffleUnitLabel(minutes: Double): String {
    val totalMinutes = minutes.roundToInt().coerceAtLeast(1)
    val hours = totalMinutes / 60
    val remainder = totalMinutes % 60
    return when {
        hours == 0 -> "$remainder min"
        remainder == 0 -> "$hours h"
        else -> "$hours h $remainder min"
    }
}

internal fun estimatedTimeLabel(minutes: Double): String {
    return if (minutes < 60.0) {
        "%.0f min".format(minutes)
    } else {
        "${(minutes / 60.0).roundedStatValue()} h"
    }
}

/**
 * A value in its own unit inside an accent-colored bubble, with the unit and
 * medium label beneath. Every bubble is the same size on purpose: the color
 * keeps the chart feel while no size scale compares unlike units.
 */
@Composable
internal fun UnitValueBubble(
    value: String,
    mediaType: MediaType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = mediaType.statsColor().copy(alpha = 0.92f),
                    shape = RoundedCornerShape(999.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appBackground,
                maxLines = 1,
            )
        }
        Text(
            text = mediaType.progressUnit(),
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
        Text(
            text = mediaType.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun RatingTrendChart(points: List<RatingTrendPoint>) {
    val ratedPoints = points.filter { point -> point.averageRating != null }
    var selectedPointKey by remember(points) { mutableStateOf<String?>(null) }
    val displayedPoint = ratedPoints.firstOrNull { point -> point.key == selectedPointKey }
        ?: ratedPoints.lastOrNull()

    StatsPanel {
        if (ratedPoints.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_ratings))
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .width(26.dp)
                            .height(126.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "10",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                    val gridLineColor = OmnilogTheme.colors.appLine
                    val pointCoreColor = OmnilogTheme.colors.appPanel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(126.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val chartHeight = size.height
                            val maxIndex = (points.size - 1).coerceAtLeast(1)
                            val step = size.width / maxIndex.toFloat()
                            fun pointOffset(index: Int, rating: Double): Offset = Offset(
                                x = step * index.toFloat(),
                                y = chartHeight - (rating.toFloat() / 10f).coerceIn(0f, 1f) * chartHeight,
                            )
                            val offsetsByIndex = points.mapIndexed { index, point ->
                                point.averageRating?.let { rating -> pointOffset(index, rating) }
                            }

                            listOf(0f, 0.5f, 1f).forEach { fraction ->
                                val y = chartHeight * fraction
                                drawLine(
                                    color = gridLineColor.copy(alpha = 0.42f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                            // Only adjacent rated months connect; a month
                            // without ratings breaks the line instead of being
                            // bridged as if it were measured.
                            connectedRatingTrendSegments(points).forEach { (startIndex, endIndex) ->
                                val start = offsetsByIndex[startIndex] ?: return@forEach
                                val end = offsetsByIndex[endIndex] ?: return@forEach
                                drawLine(
                                    color = OmnilogColors.Books,
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.dp.toPx(),
                                )
                            }
                            offsetsByIndex.filterNotNull().forEach { point ->
                                drawCircle(
                                    color = OmnilogColors.Books,
                                    radius = 4.5.dp.toPx(),
                                    center = point,
                                )
                                drawCircle(
                                    color = pointCoreColor,
                                    radius = 2.dp.toPx(),
                                    center = point,
                                )
                            }
                            val displayedIndex = points.indexOfFirst { point -> point.key == displayedPoint?.key }
                            val displayedRating = displayedPoint?.averageRating
                            if (displayedIndex >= 0 && displayedRating != null) {
                                drawCircle(
                                    color = OmnilogColors.Books,
                                    radius = 8.dp.toPx(),
                                    center = pointOffset(displayedIndex, displayedRating),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxSize()) {
                            points.forEach { point ->
                                val rating = point.averageRating
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .then(
                                            if (rating != null) {
                                                val pointDescription = stringResource(
                                                    R.string.stats_rating_trend_latest,
                                                    point.label,
                                                    rating,
                                                    point.ratingCount,
                                                )
                                                Modifier
                                                    .semantics { contentDescription = pointDescription }
                                                    .clickable {
                                                        selectedPointKey = if (selectedPointKey == point.key) {
                                                            null
                                                        } else {
                                                            point.key
                                                        }
                                                    }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    points.forEachIndexed { index, point ->
                        val showLabel = points.size <= 6 || index == 0 || index == points.lastIndex || index % 2 == 1
                        Text(
                            text = if (showLabel) point.label else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    points.forEach { point ->
                        Text(
                            text = if (point.averageRating != null) "(${point.ratingCount})" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
            displayedPoint?.let { point ->
                Text(
                    text = stringResource(
                        R.string.stats_rating_trend_latest,
                        point.label,
                        point.averageRating ?: 0.0,
                        point.ratingCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedPointKey != null) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                )
            }
        }
    }
}
