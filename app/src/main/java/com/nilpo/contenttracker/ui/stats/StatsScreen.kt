package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.MediumStats
import com.nilpo.contenttracker.core.stats.ProgressTotalStats
import com.nilpo.contenttracker.core.stats.RankedStat
import com.nilpo.contenttracker.core.stats.RatingTrendPoint
import com.nilpo.contenttracker.core.stats.RevisitStats
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSegment
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.core.stats.StatusStatsBucket
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.ZoneId
import kotlin.math.sqrt

@Composable
fun StatsScreen(
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPeriod by remember { mutableStateOf<StatsPeriod>(StatsPeriod.ThisYear) }
    var selectedMediaFilter by remember { mutableStateOf(StatsMediaFilter.All) }
    val periodOptions = remember(items) { statsPeriodOptions(items) }
    LaunchedEffect(periodOptions) {
        if (selectedPeriod !in periodOptions) {
            selectedPeriod = StatsPeriod.ThisYear
        }
    }
    val filters = remember(selectedPeriod, selectedMediaFilter) {
        StatsFilters(
            period = selectedPeriod,
            mediaTypes = selectedMediaFilter.types,
        )
    }
    val snapshot = remember(items, filters) {
        StatsCalculator().calculate(items, filters)
    }

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                StatsFilterBar(
                    selectedPeriod = selectedPeriod,
                    periodOptions = periodOptions,
                    onPeriodSelected = { selectedPeriod = it },
                    selectedMediaFilter = selectedMediaFilter,
                    onMediaFilterSelected = { selectedMediaFilter = it },
                )
            }
            item {
                StatsActivityHero(snapshot = snapshot)
            }
            item {
                StatsGroupHeader(title = stringResource(R.string.stats_group_overview))
            }
            item {
                StatsSection(title = stringResource(R.string.stats_status_breakdown)) {
                    StatusBreakdown(buckets = snapshot.statusBreakdown)
                }
            }
            item {
                StatsSection(title = stringResource(R.string.stats_medium_averages)) {
                    MediumStatsGraphs(stats = snapshot.mediumStats)
                }
            }
            item {
                StatsGroupHeader(title = stringResource(R.string.stats_group_ratings))
            }
            item {
                StatsSection(title = stringResource(R.string.stats_rating_distribution)) {
                    RatingDistributionHistogram(
                        buckets = snapshot.ratingDistribution,
                        emptyText = stringResource(R.string.stats_empty_ratings),
                    )
                }
            }
            item {
                StatsSection(title = stringResource(R.string.stats_rating_trend)) {
                    RatingTrendChart(points = snapshot.ratingTrend.takeLast(12))
                }
            }
            if (snapshot.bestRatedItems.isNotEmpty()) {
                item {
                    StatsSection(title = stringResource(R.string.stats_best_rated)) {
                        StatsMediaStrip(
                            items = snapshot.bestRatedItems,
                            onMediaClick = onMediaClick,
                            statLabel = { trackedMedia ->
                                trackedMedia.bestRating()?.let { rating -> stringResource(R.string.rating_value, rating) }
                            },
                        )
                    }
                }
            }
            item {
                StatsGroupHeader(title = stringResource(R.string.stats_group_content_mix))
            }
            item {
                StatsSection(title = stringResource(R.string.stats_top_genres)) {
                    GenrePieChart(
                        stats = snapshot.topGenres,
                        emptyText = stringResource(R.string.stats_empty_metadata),
                    )
                }
            }
            item {
                StatsSection(title = stringResource(R.string.stats_top_creators)) {
                    RankedList(
                        stats = snapshot.topCreators,
                        accent = OmnilogColors.Tv,
                        emptyText = stringResource(R.string.stats_empty_metadata),
                    )
                }
            }
            item {
                StatsSection(title = stringResource(R.string.stats_languages)) {
                    LanguageMosaicChart(
                        buckets = snapshot.languageBreakdown,
                        emptyText = stringResource(R.string.stats_empty_metadata),
                    )
                }
            }
            item {
                StatsGroupHeader(title = stringResource(R.string.stats_group_consumption))
            }
            item {
                StatsSection(title = stringResource(R.string.stats_progress_totals)) {
                    ProgressTotals(totals = snapshot.progressTotals)
                }
            }
            item {
                StatsSection(title = stringResource(R.string.stats_revisit_breakdown)) {
                    RevisitBreakdownChart(stats = snapshot.revisitBreakdown)
                }
            }
            if (snapshot.mostRevisitedItems.isNotEmpty()) {
                item {
                    StatsSection(title = stringResource(R.string.stats_most_revisited)) {
                        StatsMediaStrip(
                            items = snapshot.mostRevisitedItems.map { stat -> stat.trackedMedia },
                            onMediaClick = onMediaClick,
                            statLabel = { trackedMedia ->
                                val revisitCount = snapshot.mostRevisitedItems
                                    .firstOrNull { stat -> stat.trackedMedia.item.id == trackedMedia.item.id }
                                    ?.value
                                    ?: trackedMedia.revisitCount
                                stringResource(R.string.stats_revisit_value, revisitCount)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsGroupHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = OmnilogColors.AppMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatsFilterBar(
    selectedPeriod: StatsPeriod,
    periodOptions: List<StatsPeriod>,
    onPeriodSelected: (StatsPeriod) -> Unit,
    selectedMediaFilter: StatsMediaFilter,
    onMediaFilterSelected: (StatsMediaFilter) -> Unit,
) {
    var periodExpanded by remember { mutableStateOf(false) }
    var mediaExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = selectedPeriod.label(),
                selected = true,
                color = OmnilogColors.Dashboard,
                onClick = { periodExpanded = true },
            )
            DropdownMenu(
                expanded = periodExpanded,
                onDismissRequest = { periodExpanded = false },
            ) {
                periodOptions.forEach { period ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = period.label(),
                                fontWeight = if (selectedPeriod == period) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onPeriodSelected(period)
                            periodExpanded = false
                        },
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = selectedMediaFilter.label(),
                selected = selectedMediaFilter != StatsMediaFilter.All,
                color = selectedMediaFilter.accent,
                onClick = { mediaExpanded = true },
            )
            DropdownMenu(
                expanded = mediaExpanded,
                onDismissRequest = { mediaExpanded = false },
            ) {
                StatsMediaFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = filter.label(),
                                color = filter.accent,
                                fontWeight = if (selectedMediaFilter == filter) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                        onClick = {
                            onMediaFilterSelected(filter)
                            mediaExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) color.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.50f) else OmnilogColors.AppLine),
        contentColor = if (selected) OmnilogColors.AppInk else OmnilogColors.AppMuted,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun StatsActivityHero(snapshot: StatsSnapshot) {
    val buckets = snapshot.completedByMonth.takeLast(12)
    val maxValue = buckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val busiestMonth = buckets.maxByOrNull { bucket -> bucket.value }
        ?.takeIf { bucket -> bucket.value > 0 }
    val legendMediaTypes = buckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.Dashboard.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stats_activity_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = snapshot.filters.period.label(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.Dashboard,
                    maxLines = 1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = snapshot.completedInPeriod.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.Completed,
                )
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stats_completed_short),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppInk,
                    )
                    snapshot.deltas.completed?.let { delta ->
                        DeltaChip(
                            text = intDeltaText(delta),
                            color = deltaChipColor(delta),
                        )
                    }
                }
            }
            Text(
                text = busiestMonth?.let { bucket ->
                    stringResource(R.string.stats_activity_hero_busiest_month, bucket.label, bucket.value)
                } ?: stringResource(R.string.stats_activity_hero_empty),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ActivityHeroBars(
                buckets = buckets,
                maxValue = maxValue,
            )
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
        }
    }
}

@Composable
private fun ActivityHeroBars(
    buckets: List<StatsBucket>,
    maxValue: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { bucket ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val barHeight = if (maxValue == 0 || bucket.value == 0) {
                        2
                    } else {
                        (116 * bucket.value / maxValue).coerceAtLeast(3)
                    }
                    if (bucket.value > 0 && bucket.segments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
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
                                    color = if (bucket.value > 0) OmnilogColors.Dashboard else OmnilogColors.AppLine.copy(alpha = 0.58f),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                ),
                        )
                    }
                }
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    chipText: String? = null,
    chipColor: Color? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
            if (chipText != null && chipColor != null) {
                DeltaChip(text = chipText, color = chipColor)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun intDeltaText(delta: Int): String {
    return when {
        delta > 0 -> "↑$delta"
        delta < 0 -> "↓${-delta}"
        else -> "—"
    }
}

private fun ratingDeltaText(delta: Double): String {
    return when {
        delta > 0.0 -> "↑%.1f".format(delta)
        delta < 0.0 -> "↓%.1f".format(-delta)
        else -> "—"
    }
}

@Composable
private fun deltaChipColor(delta: Number): Color {
    val value = delta.toDouble()
    return when {
        value > 0.0 -> OmnilogColors.Completed
        value < 0.0 -> OmnilogColors.Dropped
        else -> OmnilogColors.AppMuted
    }
}

@Composable
private fun KpiGroupCaption(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogColors.AppMuted,
        maxLines = 1,
    )
}

@Composable
private fun StatsKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    chipText: String? = null,
    chipColor: Color? = null,
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    maxLines = 1,
                )
                if (chipText != null && chipColor != null) {
                    DeltaChip(text = chipText, color = chipColor)
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeltaChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.18f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = OmnilogColors.AppLine,
            )
        }
        content()
    }
}

@Composable
private fun MonthlyBarChart(buckets: List<StatsBucket>) {
    val maxValue = buckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val midpoint = maxValue / 2
    val legendMediaTypes = buckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
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
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                            )
                            if (midpoint > 0 && midpoint != maxValue) {
                                Text(
                                    text = midpoint.toString(),
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OmnilogColors.AppMuted,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = "0",
                                modifier = Modifier.align(Alignment.BottomEnd),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogColors.AppMuted,
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
                                                OmnilogColors.AppLine.copy(alpha = 0.58f)
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
                            color = OmnilogColors.AppMuted,
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
private fun MonthlyLegend(mediaTypes: List<MediaType>) {
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
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MediumStatsGraphs(stats: List<MediumStats>) {
    val visibleStats = stats.filter { stat ->
        stat.completedCount > 0 || stat.averageRating != null || stat.averageLength != null
    }

    StatsPanel {
        if (visibleStats.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            CompletedSharePie(stats = visibleStats)
            AverageRatingDotPlot(stats = visibleStats)
            AverageLengthVerticalBars(stats = visibleStats)
        }
    }
}

@Composable
private fun CompletedSharePie(stats: List<MediumStats>) {
    val completedStats = stats.filter { stat -> stat.completedCount > 0 }
    val total = completedStats.sumOf { stat -> stat.completedCount }

    if (total == 0) return

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
                    val sweep = 360f * stat.completedCount.toFloat() / total.toFloat()
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
                        color = OmnilogColors.AppInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stat.completedCount.toString(),
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

@Composable
private fun AverageRatingDotPlot(stats: List<MediumStats>) {
    val ratedStats = stats.filter { stat -> stat.averageRating != null }

    if (ratedStats.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stats_average_rating_by_medium),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
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
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                ) {
                    val y = size.height / 2f
                    drawLine(
                        color = OmnilogColors.AppLine.copy(alpha = 0.72f),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 68.dp, end = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogColors.AppMuted,
            )
            Text(
                text = "10",
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun AverageLengthVerticalBars(stats: List<MediumStats>) {
    val lengthStats = stats.filter { stat -> stat.averageLength != null }
    val maxLength = lengthStats.maxOfOrNull { stat -> stat.averageLength ?: 0.0 } ?: 0.0

    if (lengthStats.isEmpty() || maxLength <= 0.0) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stats_average_length_by_medium),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            lengthStats.forEach { stat ->
                val averageLength = stat.averageLength ?: return@forEach
                val barHeight = (82 * averageLength / maxLength).toInt().coerceAtLeast(3)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = averageLength.roundedStatValue(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = stat.mediaType.statsColor(),
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .background(
                                    color = stat.mediaType.statsColor(),
                                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                                ),
                        )
                    }
                    Text(
                        text = stat.mediaType.progressUnit(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                    )
                    Text(
                        text = stat.mediaType.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingTrendChart(points: List<RatingTrendPoint>) {
    val ratedPoints = points.filter { point -> point.averageRating != null }

    StatsPanel {
        if (ratedPoints.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_ratings))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp),
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
                            color = OmnilogColors.AppMuted,
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogColors.AppMuted,
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogColors.AppMuted,
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(126.dp),
                    ) {
                        val chartHeight = size.height
                        val maxIndex = (points.size - 1).coerceAtLeast(1)
                        val step = size.width / maxIndex.toFloat()
                        val positionedPoints = points.mapIndexedNotNull { index, point ->
                            val rating = point.averageRating ?: return@mapIndexedNotNull null
                            Offset(
                                x = step * index.toFloat(),
                                y = chartHeight - (rating.toFloat() / 10f).coerceIn(0f, 1f) * chartHeight,
                            )
                        }

                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            val y = chartHeight * fraction
                            drawLine(
                                color = OmnilogColors.AppLine.copy(alpha = 0.42f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        positionedPoints.zipWithNext().forEach { (start, end) ->
                            drawLine(
                                color = OmnilogColors.Books,
                                start = start,
                                end = end,
                                strokeWidth = 3.dp.toPx(),
                            )
                        }
                        positionedPoints.forEach { point ->
                            drawCircle(
                                color = OmnilogColors.Books,
                                radius = 4.5.dp.toPx(),
                                center = point,
                            )
                            drawCircle(
                                color = OmnilogColors.AppPanel,
                                radius = 2.dp.toPx(),
                                center = point,
                            )
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
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
            val latest = ratedPoints.last()
            Text(
                text = stringResource(
                    R.string.stats_rating_trend_latest,
                    latest.label,
                    latest.averageRating ?: 0.0,
                    latest.ratingCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun LanguageMosaicChart(
    buckets: List<StatsBucket>,
    emptyText: String,
) {
    val visibleBuckets = buckets.filter { bucket -> bucket.value > 0 }
    val topBuckets = visibleBuckets.take(2)
    val remainingBuckets = visibleBuckets.drop(2)

    StatsPanel {
        if (visibleBuckets.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (remainingBuckets.isEmpty()) 116.dp else 150.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (remainingBuckets.isEmpty()) 116.dp else 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    topBuckets.forEachIndexed { index, bucket ->
                        LanguageBlock(
                            bucket = bucket,
                            color = languageChartColor(index),
                            prominent = true,
                            modifier = Modifier.weight(bucket.value.toFloat()),
                        )
                    }
                }
                if (remainingBuckets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        remainingBuckets.forEachIndexed { index, bucket ->
                            LanguageBlock(
                                bucket = bucket,
                                color = languageChartColor(index + topBuckets.size),
                                prominent = false,
                                modifier = Modifier.weight(bucket.value.toFloat()),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBlock(
    bucket: StatsBucket,
    color: Color,
    prominent: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = if (prominent) Alignment.CenterStart else Alignment.Center,
    ) {
        Column(
            horizontalAlignment = if (prominent) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = bucket.label,
                style = if (prominent) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = bucket.value.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppBackground,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HorizontalBarChart(
    buckets: List<StatsBucket>,
    accent: Color,
    emptyText: String,
) {
    val visibleBuckets = buckets.filter { bucket -> bucket.value > 0 }
    val maxValue = visibleBuckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val legendMediaTypes = visibleBuckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    StatsPanel {
        if (visibleBuckets.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            visibleBuckets.forEach { bucket ->
                StatsBarRow(
                    label = bucket.label,
                    value = bucket.value,
                    maxValue = maxValue,
                    accent = accent,
                    segments = bucket.segments,
                )
            }
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
        }
    }
}

@Composable
private fun RatingDistributionHistogram(
    buckets: List<StatsBucket>,
    emptyText: String,
) {
    val visibleBuckets = buckets.filter { bucket -> bucket.value > 0 }
    val maxValue = visibleBuckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val legendMediaTypes = visibleBuckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    StatsPanel {
        if (visibleBuckets.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            visibleBuckets.forEach { bucket ->
                RatingDistributionRow(
                    bucket = bucket,
                    maxValue = maxValue,
                )
            }
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
        }
    }
}

@Composable
private fun RatingDistributionRow(
    bucket: StatsBucket,
    maxValue: Int,
) {
    val fillFraction = if (maxValue > 0) bucket.value.toFloat() / maxValue.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bucket.label,
                modifier = Modifier.width(18.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .background(OmnilogColors.AppLine.copy(alpha = 0.36f), RoundedCornerShape(5.dp)),
            ) {
                if (bucket.segments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(18.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    ) {
                        bucket.segments.forEach { segment ->
                            Box(
                                modifier = Modifier
                                    .weight(segment.value.toFloat())
                                    .height(18.dp)
                                    .background(segment.mediaType.statsColor()),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(18.dp)
                            .background(OmnilogColors.Books, RoundedCornerShape(5.dp)),
                    )
                }
            }
            Text(
                text = bucket.value.compactStatValue(),
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBreakdown(buckets: List<StatusStatsBucket>) {
    val visibleBuckets = buckets.filter { bucket -> bucket.value > 0 }
    val maxValue = visibleBuckets.maxOfOrNull { bucket -> bucket.value } ?: 0

    StatsPanel {
        if (visibleBuckets.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                visibleBuckets.forEach { bucket ->
                    StatusMiniBar(
                        bucket = bucket,
                        maxValue = maxValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMiniBar(
    bucket: StatusStatsBucket,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    val scaledHeight = if (maxValue > 0) {
        (70 * sqrt(bucket.value.toFloat() / maxValue.toFloat())).toInt().coerceAtLeast(8)
    } else {
        0
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = bucket.value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = bucket.status.stateColor,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledHeight.dp)
                    .background(
                        color = bucket.status.stateColor,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    ),
            )
        }
        Text(
            text = bucket.status.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProgressTotals(totals: List<ProgressTotalStats>) {
    val visibleTotals = totals.filter { total -> total.value > 0 }
    val maxValue = visibleTotals.maxOfOrNull { total -> total.value } ?: 0

    StatsPanel {
        if (visibleTotals.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                visibleTotals.forEach { total ->
                    ConsumptionTotalBubble(
                        total = total,
                        maxValue = maxValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumptionTotalBubble(
    total: ProgressTotalStats,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    val bubbleSize = if (maxValue > 0) {
        (44 + 38 * sqrt(total.value.toFloat() / maxValue.toFloat())).toInt().coerceIn(44, 82)
    } else {
        0
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(bubbleSize.dp)
                    .background(
                        color = total.mediaType.statsColor().copy(alpha = 0.92f),
                        shape = RoundedCornerShape(999.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = total.value.compactStatValue(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppBackground,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = total.mediaType.progressUnit(),
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogColors.AppMuted,
            maxLines = 1,
        )
        Text(
            text = total.mediaType.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RevisitBreakdownChart(stats: List<RevisitStats>) {
    val visibleStats = stats.filter { stat -> stat.value > 0 }
    val maxValue = visibleStats.maxOfOrNull { stat -> stat.value } ?: 0

    StatsPanel {
        if (visibleStats.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                visibleStats.forEach { stat ->
                    RevisitTypeBar(
                        stat = stat,
                        maxValue = maxValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RevisitTypeBar(
    stat: RevisitStats,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    val barHeight = if (maxValue > 0) {
        (22 + 76 * stat.value.toFloat() / maxValue.toFloat()).toInt().coerceIn(22, 98)
    } else {
        0
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = stat.value.compactStatValue(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = stat.mediaType.statsColor(),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight.dp)
                    .background(
                        color = stat.mediaType.statsColor(),
                        shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp),
                    ),
            )
        }
        Text(
            text = stat.mediaType.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GenrePieChart(
    stats: List<RankedStat>,
    emptyText: String,
) {
    val visibleStats = stats.filter { stat -> stat.value > 0 }
    val total = visibleStats.sumOf { stat -> stat.value }

    StatsPanel {
        if (visibleStats.isEmpty() || total == 0) {
            EmptyStatsText(text = emptyText)
        } else {
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
                        visibleStats.forEachIndexed { index, stat ->
                            val sweep = 360f * stat.value.toFloat() / total.toFloat()
                            drawArc(
                                color = genreChartColor(index),
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
                    visibleStats.forEachIndexed { index, stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(genreChartColor(index), RoundedCornerShape(2.dp)),
                            )
                            Text(
                                text = stat.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogColors.AppInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stat.value.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = genreChartColor(index),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankedList(
    stats: List<RankedStat>,
    accent: Color,
    emptyText: String,
) {
    val maxValue = stats.maxOfOrNull { stat -> stat.value } ?: 0

    StatsPanel {
        if (stats.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            stats.forEachIndexed { index, stat ->
                RankedAuthorBar(
                    stat = stat,
                    maxValue = maxValue,
                    accent = accent,
                    rank = index + 1,
                )
            }
        }
    }
}

@Composable
private fun RankedAuthorBar(
    stat: RankedStat,
    maxValue: Int,
    accent: Color,
    rank: Int,
) {
    val fillFraction = if (maxValue > 0) stat.value.toFloat() / maxValue.toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rank.toString().padStart(2, '0'),
                modifier = Modifier.width(22.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
            Text(
                text = stat.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stat.value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .height(18.dp)
                    .background(accent.copy(alpha = 0.92f), RoundedCornerShape(5.dp)),
            )
        }
    }
}

@Composable
private fun StatsPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content,
        )
    }
}

@Composable
private fun StatsBarRow(
    label: String,
    value: Int,
    maxValue: Int,
    accent: Color,
    segments: List<StatsSegment> = emptyList(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(OmnilogColors.AppLine.copy(alpha = 0.46f), RoundedCornerShape(999.dp)),
        ) {
            val fillFraction = if (maxValue > 0) value.toFloat() / maxValue.toFloat() else 0f
            if (segments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                ) {
                    segments.forEach { segment ->
                        Box(
                            modifier = Modifier
                                .weight(segment.value.toFloat())
                                .height(6.dp)
                                .background(segment.mediaType.statsColor()),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(6.dp)
                        .background(accent, RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Composable
private fun EmptyStatsText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogColors.AppMuted,
    )
}

@Composable
private fun StatsMediaStrip(
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    statLabel: @Composable (TrackedMedia) -> String?,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { trackedMedia ->
            StatsMediaTile(
                trackedMedia = trackedMedia,
                statLabel = statLabel(trackedMedia),
                onClick = { onMediaClick(trackedMedia) },
            )
        }
    }
}

@Composable
private fun StatsMediaTile(
    trackedMedia: TrackedMedia,
    statLabel: String?,
    onClick: () -> Unit,
) {
    val item = trackedMedia.item
    val creator = item.creators.firstOrNull()
    val genres = item.genres.take(2)
    val accent = item.type.statsColor()

    Surface(
        modifier = Modifier
            .width(164.dp)
            .height(246.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF17110D).copy(alpha = 0.14f),
                                Color(0xFF17110D).copy(alpha = 0.62f),
                                Color(0xFF15110E).copy(alpha = 0.98f),
                            ),
                        ),
                    ),
            )
            if (trackedMedia.item.ownership.isOwned || statLabel != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (trackedMedia.item.ownership.isOwned) {
                        OwnedBadge()
                    }
                    statLabel?.let { label ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent,
                            contentColor = OmnilogColors.AppBackground,
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = displayMediaTitle(item.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.type.label(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (creator != null) {
                    Text(
                        text = creator,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = OmnilogColors.AppPanel.copy(alpha = 0.92f),
                                contentColor = OmnilogColors.AppInk,
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPeriod.label(): String {
    return when (this) {
        StatsPeriod.AllTime -> stringResource(R.string.stats_period_all_time)
        StatsPeriod.ThisYear -> stringResource(R.string.stats_period_this_year)
        StatsPeriod.Last12Months -> stringResource(R.string.stats_period_last_12_months)
        is StatsPeriod.Year -> year.toString()
    }
}

private fun statsPeriodOptions(items: List<TrackedMedia>): List<StatsPeriod> {
    val yearOptions = items
        .flatMap { trackedMedia -> trackedMedia.sessions }
        .mapNotNull { session -> session.statsYear() }
        .distinct()
        .sortedDescending()
        .map { year -> StatsPeriod.Year(year) }

    return listOf(
        StatsPeriod.ThisYear,
        StatsPeriod.Last12Months,
        StatsPeriod.AllTime,
    ) + yearOptions
}

private fun TrackingSession.statsYear(): Int? {
    return finishedAt?.year
        ?: startedAt?.year
        ?: updatedAtEpochMillis.takeIf { millis -> millis > 0L }?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .year
        }
}

@Composable
private fun TrackingStatus.label(): String {
    return when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }
}

@Composable
private fun MediaType.label(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.media_type_anime)
        MediaType.Book -> stringResource(R.string.media_type_book)
        MediaType.Movie -> stringResource(R.string.media_type_movie)
        MediaType.TvShow -> stringResource(R.string.media_type_tv_show)
        MediaType.Game -> stringResource(R.string.media_type_game)
    }
}

private fun MediaType.progressUnit(): String {
    return when (this) {
        MediaType.Anime,
        MediaType.TvShow,
            -> "ep"
        MediaType.Book -> "pag"
        MediaType.Movie -> "min"
        MediaType.Game -> "h"
    }
}

private fun Double.roundedStatValue(): String {
    return if (this >= 10.0) {
        "%.0f".format(this)
    } else {
        "%.1f".format(this)
    }
}

private fun Int.compactStatValue(): String {
    return when {
        this >= 100_000 -> "${this / 1_000}k"
        this >= 10_000 -> "%.1fk".format(this / 1_000.0)
        else -> this.toString()
    }
}

private fun TrackedMedia.bestRating(): Int? {
    return sessions.mapNotNull { session -> session.rating }.maxOrNull()
}

private fun genreChartColor(index: Int): Color {
    val colors = listOf(
        OmnilogColors.Anime,
        OmnilogColors.Books,
        OmnilogColors.Tv,
        OmnilogColors.Games,
        OmnilogColors.Dashboard,
        OmnilogColors.Completed,
        OmnilogColors.Paused,
        OmnilogColors.Dropped,
    )
    return colors[index % colors.size]
}

private fun languageChartColor(index: Int): Color {
    val colors = listOf(
        OmnilogColors.Games,
        OmnilogColors.Tv,
        OmnilogColors.Books,
        Color(0xFF5E8FC4),
        OmnilogColors.Dashboard,
        OmnilogColors.Anime,
        OmnilogColors.Completed,
        OmnilogColors.Paused,
    )
    return colors[index % colors.size]
}

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

private fun MediaType.statsColor(): Color {
    return when (this) {
        MediaType.Anime -> MediaSection.Anime.accent
        MediaType.Book -> MediaSection.Books.accent
        MediaType.Movie -> MediaSection.Movies.accent
        MediaType.TvShow -> Color(0xFF5E8FC4)
        MediaType.Game -> MediaSection.Games.accent
    }
}

private enum class StatsMediaFilter(
    val types: Set<MediaType>,
    val accent: Color,
) {
    All(MediaType.entries.toSet(), OmnilogColors.Dashboard),
    Anime(setOf(MediaType.Anime), MediaSection.Anime.accent),
    Books(setOf(MediaType.Book), MediaSection.Books.accent),
    Movies(setOf(MediaType.Movie, MediaType.TvShow), MediaSection.Movies.accent),
    Games(setOf(MediaType.Game), MediaSection.Games.accent),
}

@Composable
private fun StatsMediaFilter.label(): String {
    return when (this) {
        StatsMediaFilter.All -> stringResource(R.string.stats_filter_all)
        StatsMediaFilter.Anime -> stringResource(R.string.nav_anime)
        StatsMediaFilter.Books -> stringResource(R.string.nav_books)
        StatsMediaFilter.Movies -> stringResource(R.string.nav_movies_tv)
        StatsMediaFilter.Games -> stringResource(R.string.nav_games)
    }
}
