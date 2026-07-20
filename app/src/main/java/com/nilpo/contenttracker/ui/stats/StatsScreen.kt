package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.MinRatedTitlesForCollectionAverage
import com.nilpo.contenttracker.core.stats.estimatedTimeByMedium
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    onCreatorClick: (String, MediaType) -> Unit,
    onCollectionClick: (Long, MediaType) -> Unit,
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
    val hasRatings = snapshot.averageRating != null
    // Window once and gate on the same slice that gets drawn. Gating on the full trend while
    // rendering only the tail let the section open with a single dot and no line, whenever the
    // rated months were more than twelve apart.
    val ratingTrendPoints = snapshot.ratingTrend.takeLast(12)
    val hasRatingTrend = ratingTrendPoints.count { point -> point.averageRating != null } >= 2
    val hasConsumption = snapshot.progressTotals.any { total -> total.value > 0 }
    val hasRevisits = snapshot.revisitCount > 0
    // The share chart is drawn from repeated *titles*, which can be zero while repeat
    // sessions are not — a repeat still in progress counts as a session but has not been
    // finished again. Gate on what the chart actually draws.
    val hasRevisitedTitles = snapshot.revisitedTitles > 0
    val hasContentMix = snapshot.topGenres.isNotEmpty() ||
        snapshot.topCreators.isNotEmpty() || snapshot.languageBreakdown.stats.isNotEmpty()
    // Cross-medium modules render only when at least two media types can
    // actually be compared on that module's own scale.
    val completionShareMediumCount = snapshot.mediumStats.count { stat -> stat.completionSessionCount > 0 }
    val ratedMediumCount = snapshot.mediumStats.count { stat -> stat.averageRating != null }
    val lengthMediumCount = snapshot.mediumStats.count { stat -> stat.averageLength != null }
    val hasMediumComparisons = completionShareMediumCount >= 2 ||
        ratedMediumCount >= 2 || lengthMediumCount >= 2
    val hasCurrentStatus = snapshot.statusBreakdown.any { bucket -> bucket.value > 0 }

    Surface(
        modifier = modifier,
        color = OmnilogTheme.colors.appBackground,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OmnilogTheme.colors.appBackground)
                        .padding(bottom = 4.dp),
                ) {
                    StatsFilterBar(
                        selectedPeriod = selectedPeriod,
                        periodOptions = periodOptions,
                        onPeriodSelected = { selectedPeriod = it },
                        selectedMediaFilter = selectedMediaFilter,
                        onMediaFilterSelected = { selectedMediaFilter = it },
                    )
                }
            }
            item {
                StatsActivityHero(snapshot = snapshot)
            }
            if (hasRatings) {
                item {
                    StatsSection(title = stringResource(R.string.stats_rating_distribution)) {
                        RatingDistributionHistogram(
                            buckets = snapshot.ratingDistribution,
                            emptyText = stringResource(R.string.stats_empty_ratings),
                        )
                    }
                }
                if (hasRatingTrend) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_rating_trend),
                            // The chart only ever draws twelve months. For a bounded period that
                            // is the whole period, but under "Tot" it is a window the filter does
                            // not imply, so the subtitle has to say so.
                            subtitle = if (selectedPeriod == StatsPeriod.AllTime) {
                                stringResource(R.string.stats_rating_trend_subtitle_all_time)
                            } else {
                                stringResource(R.string.stats_rating_trend_subtitle)
                            },
                        ) {
                            RatingTrendChart(points = ratingTrendPoints)
                        }
                    }
                }
                if (snapshot.bestRatedItems.isNotEmpty()) {
                    item {
                        StatsSection(title = stringResource(R.string.stats_best_rated)) {
                            StatsMediaStrip(
                                items = snapshot.bestRatedItems.map { stat -> stat.trackedMedia },
                                onMediaClick = onMediaClick,
                                statLabel = { trackedMedia ->
                                    snapshot.bestRatedItems
                                        .firstOrNull { stat -> stat.trackedMedia.item.id == trackedMedia.item.id }
                                        ?.bestRating
                                        ?.let { rating -> stringResource(R.string.rating_value, rating) }
                                },
                            )
                        }
                    }
                }
                if (snapshot.bestRatedCollections.isNotEmpty()) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_best_rated_collections),
                            // The threshold is part of the claim: without it the list reads as
                            // "your best collections" when it is "your best well-rated ones".
                            subtitle = stringResource(
                                R.string.stats_best_rated_collections_subtitle,
                                MinRatedTitlesForCollectionAverage,
                            ),
                        ) {
                            CollectionRatingCarousel(
                                stats = snapshot.bestRatedCollections,
                                emptyText = stringResource(R.string.stats_empty_activity),
                                onCollectionClick = { stat -> onCollectionClick(stat.id, stat.mediaType) },
                            )
                        }
                    }
                }
            }
            if (hasConsumption || hasRevisits) {
                if (hasConsumption) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_progress_totals),
                            subtitle = stringResource(R.string.stats_units_not_comparable),
                        ) {
                            ProgressTotals(totals = snapshot.progressTotals)
                        }
                    }
                }
                val estimatedTimes = estimatedTimeByMedium(snapshot.progressTotals)
                if (estimatedTimes.size >= 2) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_estimated_time_title),
                            subtitle = stringResource(R.string.stats_estimated_time_subtitle),
                        ) {
                            EstimatedTimeWaffle(stats = estimatedTimes)
                        }
                    }
                }
                if (hasRevisitedTitles) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_revisit_breakdown),
                            subtitle = stringResource(R.string.stats_revisit_share_subtitle),
                        ) {
                            RevisitShareChart(
                                revisitedTitles = snapshot.revisitedTitles,
                                completedTitles = snapshot.uniqueTitlesCompleted,
                                breakdown = snapshot.revisitedTitleBreakdown,
                                revisitSessions = snapshot.revisitCount,
                            )
                        }
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
            if (hasContentMix) {
                if (snapshot.topGenres.isNotEmpty()) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_top_genres),
                            subtitle = stringResource(R.string.stats_top_genres_subtitle),
                        ) {
                            GenrePieChart(
                                stats = snapshot.topGenres,
                                emptyText = stringResource(R.string.stats_empty_metadata),
                            )
                        }
                    }
                }
                if (snapshot.creatorStats.isNotEmpty()) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_top_creators),
                            subtitle = stringResource(R.string.stats_top_creators_subtitle),
                        ) {
                            CreatorCarousel(
                                stats = snapshot.creatorStats,
                                emptyText = stringResource(R.string.stats_empty_metadata),
                                onCreatorClick = { stat -> onCreatorClick(stat.name, stat.mediaType) },
                            )
                        }
                    }
                }
                if (snapshot.languageBreakdown.stats.isNotEmpty()) {
                    item {
                        val coverage = snapshot.languageBreakdown
                        // Naming the media in the title stops the section claiming to describe the
                        // whole library when it only describes the part that records a language.
                        val scope = coverage.includedMediaTypes
                            .map { mediaType -> mediaType.pluralLabel() }
                            .joinToString(separator = ", ")
                        StatsSection(
                            title = if (coverage.excludedMediaTypes.isEmpty()) {
                                stringResource(R.string.stats_languages)
                            } else {
                                stringResource(R.string.stats_languages_scoped, scope)
                            },
                            subtitle = stringResource(
                                R.string.stats_languages_coverage,
                                coverage.recordedTitles,
                                coverage.totalTitles,
                            ),
                        ) {
                            LanguageMosaicChart(
                                coverage = coverage,
                                emptyText = stringResource(R.string.stats_empty_metadata),
                            )
                        }
                    }
                }
            }
            if (hasCurrentStatus || hasMediumComparisons) {
                if (hasCurrentStatus) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_status_breakdown),
                            subtitle = stringResource(R.string.stats_status_breakdown_subtitle),
                        ) {
                            StatusBreakdown(buckets = snapshot.statusBreakdown)
                        }
                    }
                }
                if (completionShareMediumCount >= 2) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_completion_sessions_by_medium),
                            subtitle = stringResource(R.string.stats_completion_share_subtitle),
                        ) {
                            CompletionSharePie(stats = snapshot.mediumStats)
                        }
                    }
                }
                if (ratedMediumCount >= 2) {
                    item {
                        StatsSection(title = stringResource(R.string.stats_average_rating_by_medium)) {
                            AverageRatingDotPlot(stats = snapshot.mediumStats)
                        }
                    }
                }
                if (lengthMediumCount >= 2) {
                    item {
                        StatsSection(
                            title = stringResource(R.string.stats_average_length_by_medium),
                            subtitle = stringResource(R.string.stats_length_scale_caption),
                        ) {
                            AverageLengthRanges(stats = snapshot.mediumStats)
                        }
                    }
                }
            }
        }
    }
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
                color = OmnilogTheme.accents.Dashboard,
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
                color = selectedMediaFilter.themedAccent(),
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
                                color = filter.themedAccent(),
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
        color = if (selected) color.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.50f) else OmnilogTheme.colors.appLine),
        contentColor = if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
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
private fun StatsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appLine,
                )
            }
            subtitle?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        content()
    }
}
