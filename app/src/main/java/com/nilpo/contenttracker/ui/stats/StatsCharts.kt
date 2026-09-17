package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.stats.RatingTrendPoint
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.connectedRatingTrendSegments
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A chapter: the serif title, and at most one line under it. The parts inside a chapter are named
 * by a [PartLabel] only, so a title is never followed by a description, a label and a second one.
 */
@Composable
internal fun StatsChapter(
    title: String,
    note: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = DetailGutter),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DetailSectionTitle(text = title, modifier = Modifier.semantics { heading() })
            note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = OmnilogTheme.colors.appMuted) }
        }
        content()
    }
}

/**
 * The name of a part inside a chapter, set small and in capitals so it reads as a label rather than
 * another heading. [onSeeAll] adds the link to the full list behind a shelf.
 */
@Composable
internal fun PartLabel(text: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter)
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(OmnilogLocale),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = OmnilogTheme.colors.appMuted,
        )
        onSeeAll?.let { open ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = open)
                    .semantics { role = Role.Button }
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stats_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = OmnilogTheme.accents.Dashboard,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogTheme.accents.Dashboard,
                )
            }
        }
    }
}

/** One tile in the lead's carousel: a serif value, what it is, and optionally how it moved. */
internal data class StatsFigure(
    val value: String,
    val label: String,
    val color: Color? = null,
    val delta: String? = null,
    val deltaColor: Color? = null,
)

/**
 * The period's supporting figures as a row that scrolls past the edge: no tiles, just each figure
 * over its label, split by hairlines like the profile's counts.
 */
@Composable
internal fun FigureCarousel(figures: List<StatsFigure>) {
    LazyRow(contentPadding = PaddingValues(horizontal = DetailGutter)) {
        items(figures, key = { it.label }) { figure ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                if (figure != figures.first()) {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxHeight(),
                        color = OmnilogTheme.colors.appLine,
                    )
                }
                Column(
                    modifier = Modifier
                        .width(FigureTileWidth)
                        .padding(start = if (figure == figures.first()) 0.dp else 16.dp, end = 12.dp)
                        .clearAndSetSemantics {
                            contentDescription = listOfNotNull(figure.label, figure.value, figure.delta).joinToString(", ")
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = figure.value,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                            color = figure.color ?: OmnilogTheme.colors.appInk,
                            maxLines = 1,
                        )
                        figure.delta?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(bottom = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = figure.deltaColor ?: OmnilogTheme.colors.appMuted,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = figure.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val FigureTileWidth = 118.dp

/** Figures side by side, split by hairlines, the way the profile sets its counts. */
@Composable
internal fun FigureStrip(figures: List<StatsFigure>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        figures.forEachIndexed { index, figure ->
            if (index > 0) {
                VerticalDivider(modifier = Modifier.fillMaxHeight(0.7f), color = OmnilogTheme.colors.appLine)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = "${figure.label}, ${figure.value}" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = figure.value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = figure.color ?: OmnilogTheme.colors.appInk,
                    maxLines = 1,
                )
                Text(
                    text = figure.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * Columns on a shared baseline, each stacked by format in the format colours, with a label under
 * every column: Home's month chart in a general form, serving months, years and scores alike.
 *
 * [reveal] runs from 0 to 1 as the columns rise.
 */
@Composable
internal fun StackedColumns(
    buckets: List<StatsBucket>,
    height: Dp,
    description: @Composable (StatsBucket) -> String,
    reveal: Float,
    modifier: Modifier = Modifier,
    showValues: Boolean = false,
    labelEvery: Int = 1,
    // A column not yet reached (a month still to come) stays a bare baseline.
    isFuture: (StatsBucket) -> Boolean = { false },
) {
    val maximum = (buckets.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + if (showValues) 16.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                val text = description(bucket)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clearAndSetSemantics { if (!isFuture(bucket)) contentDescription = text },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (showValues && bucket.value > 0) {
                        Text(
                            text = bucket.value.toString(),
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .graphicsLayer { alpha = reveal },
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    if (bucket.value > 0 && !isFuture(bucket)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(((height.value * bucket.value / maximum).coerceAtLeast(4f) * reveal).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            // Reversed so the first format sits on the baseline.
                            bucket.segments.filter { it.value > 0 }.reversed().forEach { segment ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(segment.value.toFloat())
                                        .background(segment.mediaType.statsColor()),
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    text = if (index % labelEvery == 0) bucket.label else "",
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(unbounded = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFuture(bucket)) OmnilogTheme.colors.appMuted.copy(alpha = 0.45f) else OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * The monthly average as a line you can read: a scale fitted to the values with its three marks
 * labelled, every month named under its point, and one month (the latest, or the one tapped)
 * called out with its value and how many ratings it averages. Only neighbouring months that both
 * have ratings are joined, so a month without any breaks the line.
 */
@Composable
internal fun RatingTrendLine(
    points: List<RatingTrendPoint>,
    monthLabel: (RatingTrendPoint) -> String,
    color: Color,
    reveal: Float,
    modifier: Modifier = Modifier,
) {
    val rated = points.mapNotNull { it.averageRating }
    if (rated.isEmpty()) return
    // Fitted to the data, but at least three points tall so small moves do not read as cliffs.
    var low = floor(rated.min() - 0.5).coerceAtLeast(0.0)
    var high = ceil(rated.max() + 0.5).coerceAtMost(10.0)
    if (high - low < 3.0) {
        val pad = (3.0 - (high - low)) / 2
        low = (low - pad).coerceAtLeast(0.0)
        high = (high + pad).coerceAtMost(10.0)
    }
    var selectedIndex by remember(points) { mutableStateOf(points.indexOfLast { it.averageRating != null }) }
    val selected = points.getOrNull(selectedIndex)
    val gridColor = OmnilogTheme.colors.appLine
    val coreColor = OmnilogTheme.colors.appBackground
    val chartHeight = 112.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        selected?.averageRating?.let { value ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = value.roundedStatValue(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = color,
                )
                Text(
                    text = "${selected.label} · ${pluralStringResource(R.plurals.stats_ratings_count, selected.ratingCount, selected.ratingCount)}",
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier
                    .height(chartHeight)
                    .width(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(high, (high + low) / 2, low).forEach {
                    Text(text = it.roundedStatValue(), style = MaterialTheme.typography.labelSmall, color = OmnilogTheme.colors.appMuted)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                            .clipToBounds(),
                    ) {
                        // The labels' first and last lines sit half a text line in from each edge.
                        val inset = 7.dp.toPx()
                        val usable = size.height - inset * 2
                        val slot = size.width / points.size
                        fun at(index: Int, rating: Double) = Offset(
                            slot * index + slot / 2,
                            inset + usable * (1f - ((rating - low) / (high - low)).toFloat().coerceIn(0f, 1f)),
                        )
                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            val y = inset + usable * fraction
                            drawLine(
                                gridColor.copy(alpha = 0.7f),
                                Offset(0f, y),
                                Offset(size.width, y),
                                1.dp.toPx(),
                                pathEffect = if (fraction == 0.5f) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null,
                            )
                        }
                        if (selectedIndex >= 0 && points[selectedIndex].averageRating != null) {
                            val x = slot * selectedIndex + slot / 2
                            drawLine(gridColor, Offset(x, inset), Offset(x, size.height - inset), 1.dp.toPx())
                        }
                        // The line draws left to right as it reveals.
                        val revealX = size.width * reveal
                        connectedRatingTrendSegments(points).forEach { (from, to) ->
                            val start = at(from, points[from].averageRating!!)
                            val end = at(to, points[to].averageRating!!)
                            if (start.x > revealX) return@forEach
                            val t = ((revealX - start.x) / (end.x - start.x)).coerceIn(0f, 1f)
                            drawLine(color, start, Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t), 2.dp.toPx())
                        }
                        points.forEachIndexed { index, point ->
                            val rating = point.averageRating ?: return@forEachIndexed
                            val center = at(index, rating)
                            if (center.x > revealX) return@forEachIndexed
                            val radius = if (index == selectedIndex) 5.dp.toPx() else 3.5.dp.toPx()
                            drawCircle(color, radius, center)
                            drawCircle(coreColor, radius * 0.45f, center)
                        }
                    }
                    // One tap target per month, laid over the canvas.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                    ) {
                        points.forEachIndexed { index, point ->
                            val description = point.averageRating?.let {
                                stringResource(R.string.stats_rating_trend_latest, point.label, it, point.ratingCount)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(
                                        if (description != null) {
                                            Modifier
                                                .clickable { selectedIndex = index }
                                                .semantics { contentDescription = description }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    points.forEachIndexed { index, point ->
                        Text(
                            text = monthLabel(point),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/** A ranked row: an optional mark, the name, a bar against the largest value, and the figure. */
internal data class RankedBar(
    val label: String,
    val value: Int,
    val color: Color,
    val valueText: String = value.toString(),
    val leading: (@Composable () -> Unit)? = null,
)

@Composable
internal fun RankedBars(bars: List<RankedBar>, reveal: Float, modifier: Modifier = Modifier, labelWidth: Dp = 104.dp) {
    val maximum = (bars.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        bars.forEach { bar ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "${bar.label}: ${bar.valueText}" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.width(labelWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    bar.leading?.invoke()
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((bar.value.toFloat() / maximum * reveal).coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(bar.color),
                    )
                }
                Text(
                    text = bar.valueText,
                    modifier = Modifier.widthIn(min = 32.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = OmnilogTheme.colors.appMuted,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A value on a fixed scale, as a dot on a track: for comparing averages without implying a sum. */
internal data class ScaleDot(
    val label: String,
    val value: Double,
    val color: Color,
    val leading: (@Composable () -> Unit)? = null,
)

@Composable
internal fun ScaleDots(dots: List<ScaleDot>, maximum: Double, reveal: Float, modifier: Modifier = Modifier) {
    val track = OmnilogTheme.colors.appLine
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        dots.forEach { dot ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "${dot.label}: ${dot.value.roundedStatValue()}" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.width(104.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dot.leading?.invoke()
                    Text(text = dot.label, style = MaterialTheme.typography.bodyMedium, color = OmnilogTheme.colors.appInk, maxLines = 1)
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp),
                ) {
                    val y = size.height / 2
                    val end = size.width - 6.dp.toPx()
                    drawLine(track, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                    val x = end * (dot.value / maximum).toFloat().coerceIn(0f, 1f) * reveal
                    drawLine(dot.color.copy(alpha = 0.5f), Offset(0f, y), Offset(x, y), 2.dp.toPx())
                    drawCircle(dot.color, 6.dp.toPx(), Offset(x.coerceAtLeast(6.dp.toPx()), y))
                }
                Text(
                    text = dot.value.roundedStatValue(),
                    modifier = Modifier.widthIn(min = 32.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = dot.color,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** One segment of a [ShareBar]. */
internal data class ShareSegment(val value: Double, val color: Color)

/** Parts of a whole as one rounded bar with a hairline gap between neighbours; grows in from the left. */
@Composable
internal fun ShareBar(segments: List<ShareSegment>, reveal: Float, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val positive = segments.filter { it.value > 0.0 }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(reveal.coerceAtLeast(0.01f))
                .height(height)
                .clip(RoundedCornerShape(height / 2)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            positive.forEach { segment ->
                Box(
                    Modifier
                        .weight(segment.value.toFloat())
                        .fillMaxHeight()
                        .background(segment.color),
                )
            }
        }
    }
}

/** A legend entry: a colour dot and its text. */
@Composable
internal fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = OmnilogTheme.colors.appInk, maxLines = 1)
    }
}

/** A cover shelf that runs to the screen edge, starting in line with the page's gutter. */
@Composable
internal fun <T> CoverShelf(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DetailGutter),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = key) { content(it) }
    }
}

/** A title on the shelf: the cover, one figure in the format's colour, and the title. */
@Composable
internal fun ShelfCover(
    coverUrl: String?,
    title: String,
    figure: String,
    figureColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(ShelfCoverWidth)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = "$title, $figure" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MetadataCoverImage(
            coverUrl = coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(6.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = figure,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                color = figureColor,
                maxLines = 1,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal val ShelfCoverWidth = 96.dp

@Composable
internal fun deltaChipColor(delta: Number): Color {
    val value = delta.toDouble()
    return when {
        value > 0.0 -> OmnilogTheme.accents.Completed
        value < 0.0 -> OmnilogTheme.accents.Dropped
        else -> OmnilogTheme.colors.appMuted
    }
}

internal fun intDeltaText(delta: Int): String = when {
    delta > 0 -> "▲$delta"
    delta < 0 -> "▼${-delta}"
    else -> "="
}

internal fun ratingDeltaText(delta: Double): String = when {
    delta > 0.0 -> "▲%.1f".format(delta)
    delta < 0.0 -> "▼%.1f".format(-delta)
    else -> "="
}

@Composable
internal fun StatsEmptyText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter),
        style = MaterialTheme.typography.bodyMedium,
        color = OmnilogTheme.colors.appMuted,
    )
}
