package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.ProgressTotalStats
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.rememberDashboardPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenDashboardSections
import com.nilpo.contenttracker.ui.common.writeHiddenDashboardSections
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeLandingScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onSectionSearch: (MediaSection, String) -> Unit,
    onStatsClick: () -> Unit,
    onObjectivesClick: () -> Unit,
    onAddToSection: (MediaSection) -> Unit,
    onImportBackup: () -> Unit,
    onQuickSetProgress: (TrackedMedia, Int) -> Unit = { _, _ -> },
    onQuickComplete: (TrackedMedia) -> Unit = {},
    onQuickStart: (TrackedMedia) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val objectiveProgress = remember(items, uiState.objectives) { ObjectiveCalculator().calculate(items, uiState.objectives) }.filter { it.objective.archivedAtEpochMillis == null && !it.isExpired(LocalDate.now()) }
    val dashboardPreferences = rememberDashboardPreferences()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    // Settings owns this control (UX-18); Home only renders the result and offers a way out.
    val hiddenSections by rememberHiddenDashboardSections(dashboardPreferences)
    val dashboardListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val normalizedSearchQuery = searchQuery.trim()
    val searchMatches = items
        .filter { it.matchesDashboardQuery(normalizedSearchQuery) }
        .sortedBy { displayMediaTitle(it.item.title).lowercase() }
    // Each carousel's candidates before the section filter: the daily sections need to know whether
    // they had anything to begin with, so their empty copy can name the filter as the reason.
    val activeCandidates = items
        .filter { it.currentSession?.status == TrackingStatus.InProgress }
        .sortedByDescending { it.latestActivityMillis() }
    val plannedCandidates = items
        .filter { it.currentSession?.status == TrackingStatus.Planned }
        .sortedByDescending { it.latestActivityMillis() }
    // Longest-stalled first: the whole point of the section is the titles you have stopped
    // noticing, so the ones you touched most recently are the least useful to surface.
    val pausedCandidates = items
        .filter { it.currentSession?.status == TrackingStatus.Paused }
        .sortedBy { it.currentSession?.updatedAtEpochMillis ?: 0L }
    // Ordered strictly by finish date. A completion with no recorded finish date has no honest
    // position on a recency axis, so it is left out rather than placed by a proxy: updatedAt moves
    // whenever any field changes, which would promote a years-old entry the moment its notes were
    // edited.
    val completedCandidates = items
        .mapNotNull { media -> media.completionDate()?.let { date -> media to date } }
        .sortedByDescending { (_, date) -> date }
        .map { (media, _) -> media }

    val isSectionVisible = { media: TrackedMedia ->
        media.item.type.dashboardSection() !in hiddenSections
    }

    val activeItemsVisible = activeCandidates.filter(isSectionVisible).take(8)
    val plannedItems = plannedCandidates.filter(isSectionVisible).take(8)
    val pausedItems = pausedCandidates.filter(isSectionVisible).take(8)
    val completedItems = completedCandidates.filter(isSectionVisible).take(8)

    LaunchedEffect(dashboardListState.isScrollInProgress) {
        if (dashboardListState.isScrollInProgress) {
            showSearchOverlay = false
        }
    }

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = dashboardListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    DashboardSearch(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            showSearchOverlay = it.isNotBlank()
                        },
                        onClear = {
                            searchQuery = ""
                            showSearchOverlay = false
                        },
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                showSearchOverlay = true
                            }
                        },
                    )
                }

                if (items.isEmpty()) {
                    item {
                        EmptyHomeState(
                            onAddToSection = onAddToSection,
                            onImportBackup = onImportBackup,
                        )
                    }
                } else {
                    // Only when a filter is actually on. The dashboard should never quietly leave
                    // content out, but the default — nothing hidden — costs no space at all.
                    if (hiddenSections.isNotEmpty()) {
                        item {
                            DashboardFilterNotice(
                                hiddenSections = MediaSection.entries.filter { it in hiddenSections },
                                onShowAll = { dashboardPreferences.writeHiddenDashboardSections(emptySet()) },
                            )
                        }
                    }

                    item {
                        HomeActiveCarousel(
                            title = stringResource(R.string.home_active_title),
                            items = activeItemsVisible,
                            onMediaClick = onMediaClick,
                            onQuickSetProgress = onQuickSetProgress,
                            onQuickComplete = onQuickComplete,
                            isFiltered = activeCandidates.isNotEmpty(),
                        )
                    }

                    item {
                        HomeCarousel(
                            title = stringResource(R.string.home_planned_title),
                            items = plannedItems,
                            onMediaClick = onMediaClick,
                            emptyText = if (plannedCandidates.isNotEmpty()) {
                                stringResource(R.string.home_planned_filtered_empty)
                            } else {
                                stringResource(R.string.home_planned_empty)
                            },
                            onQuickStart = onQuickStart,
                        )
                    }

                    item {
                        DashboardObjectivesPreview(
                            objectives = objectiveProgress,
                            onClick = onObjectivesClick,
                        )
                    }

                    item {
                        DashboardAnalyticsPreview(
                            items = items,
                            onClick = onStatsClick,
                        )
                    }

                    // Below the goal and analytics cards: the rediscover-and-review half of the
                    // library. Both hide when empty — unlike the two carousels above, which are
                    // daily surfaces worth explaining, these are only worth space when populated.
                    if (pausedItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_paused_title),
                                items = pausedItems,
                                onMediaClick = onMediaClick,
                                onQuickStart = onQuickStart,
                            )
                        }
                    }

                    if (completedItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_completed_title),
                                items = completedItems,
                                onMediaClick = onMediaClick,
                            )
                        }
                    }
                }
            }
            if (normalizedSearchQuery.isNotEmpty() && showSearchOverlay) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 76.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            focusManager.clearFocus()
                            showSearchOverlay = false
                        },
                )
                DashboardSearchOverlay(
                    query = normalizedSearchQuery,
                    matches = searchMatches,
                    onMediaClick = {
                        searchQuery = ""
                        onMediaClick(it)
                    },
                    onSectionSearch = { section ->
                        val query = normalizedSearchQuery
                        searchQuery = ""
                        onSectionSearch(section, query)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 16.dp, top = 76.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DashboardAnalyticsPreview(
    items: List<TrackedMedia>,
    onClick: () -> Unit,
) {
    val snapshot = remember(items) {
        StatsCalculator().calculate(items, StatsFilters(period = StatsPeriod.ThisYear))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_analytics_preview_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_analytics_preview_open),
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogColors.Dashboard,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiStat(
                    value = snapshot.completedInPeriod.toString(),
                    label = stringResource(R.string.home_analytics_preview_completed),
                    valueColor = OmnilogColors.Completed,
                    modifier = Modifier.weight(1f),
                )
                snapshot.averageRating?.let { average ->
                    KpiStat(
                        value = ratingFormat.format(average),
                        label = stringResource(R.string.home_analytics_average_rating),
                        valueColor = OmnilogColors.AppInk,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            VolumeChips(totals = snapshot.progressTotals)
            MonthlyActivityPreview(buckets = snapshot.completedByMonth)
        }
    }
}

/**
 * A headline figure over its own label. Stacked rather than side-by-side so the label is free to
 * wrap at large font scales instead of fighting the value for one line.
 */
@Composable
private fun KpiStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = OmnilogColors.AppMuted,
        )
    }
}

/**
 * What the year actually consisted of, in each medium's own unit — `1.482 pàgines · 96 episodis`.
 *
 * One chip per medium rather than a single headline figure: the values are different units, so a
 * "biggest" number would be meaningless (pages always dwarf episodes) and a sum would be nonsense.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolumeChips(
    totals: List<ProgressTotalStats>,
    modifier: Modifier = Modifier,
) {
    val visible = totals.filter { it.value > 0 }
    if (visible.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEach { total ->
            val accent = total.mediaType.sectionAccent()
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            ) {
                Text(
                    text = "${volumeFormat.format(total.value)} ${total.mediaType.progressUnit()}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MonthlyActivityPreview(
    buckets: List<StatsBucket>,
    modifier: Modifier = Modifier,
) {
    val visibleBuckets = buckets.takeLast(7)
    val maxValue = visibleBuckets.maxOfOrNull { bucket -> bucket.value }?.coerceAtLeast(1) ?: 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Min, not fixed: the month labels scale with the system font and a hard height
            // clips them at large scales (UX-09). The bars keep their own fixed height.
            .heightIn(min = 86.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        visibleBuckets.forEach { bucket ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val barHeight = if (bucket.value == 0) 2 else {
                        (62 * bucket.value / maxValue).coerceAtLeast(4)
                    }
                    if (bucket.segments.isNotEmpty()) {
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
                                        .background(segment.mediaType.sectionAccent()),
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .background(
                                    if (bucket.value > 0) OmnilogColors.Dashboard else OmnilogColors.AppLine.copy(alpha = 0.58f),
                                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
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
private fun DashboardSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = if (query.isNotBlank()) OmnilogColors.Dashboard else OmnilogColors.AppMuted,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && query.isNotBlank()) {
                            onClick()
                        }
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = OmnilogColors.AppInk,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(OmnilogColors.Dashboard),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.search_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = OmnilogColors.AppMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = OmnilogColors.AppMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardSearchOverlay(
    query: String,
    matches: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    onSectionSearch: (MediaSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 336.dp),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        shadowElevation = 10.dp,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (matches.isNotEmpty()) {
                items(matches) { trackedMedia ->
                    Column {
                        DashboardSearchResultRow(
                            trackedMedia = trackedMedia,
                            onClick = { onMediaClick(trackedMedia) },
                        )
                        if (trackedMedia != matches.last()) {
                            HorizontalDivider(color = OmnilogColors.AppLine.copy(alpha = 0.58f))
                        }
                    }
                }
            } else {
                items(MediaSection.entries.toList()) { section ->
                    Column {
                        DashboardSectionSearchRow(
                            query = query,
                            section = section,
                            onClick = { onSectionSearch(section) },
                        )
                        if (section != MediaSection.entries.last()) {
                            HorizontalDivider(color = OmnilogColors.AppLine.copy(alpha = 0.58f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSearchResultRow(
    trackedMedia: TrackedMedia,
    onClick: () -> Unit,
) {
    val section = trackedMedia.item.type.dashboardSection()
    val creator = trackedMedia.item.creators.firstOrNull()
    val collection = formatCollectionDisplayName(
        trackedMedia.collection?.name,
        trackedMedia.item.collectionSortOrder,
    )
    val typeLabel = stringResource(section.titleResId)
    val secondary = listOfNotNull(
        typeLabel,
        creator,
        collection?.let { stringResource(R.string.collection_summary, it) },
    ).joinToString(" | ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataCoverImage(
            coverUrl = trackedMedia.item.coverUrl,
            modifier = Modifier.size(width = 38.dp, height = 56.dp),
            shape = RoundedCornerShape(5.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = displayMediaTitle(trackedMedia.item.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogColors.AppMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trackedMedia.currentSession?.let { session ->
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(session.status.iconResId),
                    contentDescription = session.status.label(),
                    modifier = Modifier.size(18.dp),
                    tint = session.status.stateColor,
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionSearchRow(
    query: String,
    section: MediaSection,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(section.iconResId),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = section.accent,
        )
        Text(
            text = stringResource(
                R.string.dashboard_search_in_section,
                query,
                stringResource(section.titleResId),
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A titled row of media tiles. Pass [emptyText] for a section that should explain itself when it
 * has nothing (the daily surfaces near the top); omit it and the caller is expected to skip the
 * section entirely instead. Pass [onQuickStart] to give each tile a start/resume action — it only
 * appears on tiles whose status can actually be started (see [HomeMediaTile]).
 */
@Composable
private fun HomeCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    emptyText: String? = null,
    onQuickStart: ((TrackedMedia) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionTitle(title = title)
        if (items.isEmpty()) {
            emptyText?.let { EmptyCarouselState(text = it) }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                        onQuickStart = onQuickStart?.let { start -> { start(trackedMedia) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActiveCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    onQuickSetProgress: (TrackedMedia, Int) -> Unit,
    onQuickComplete: (TrackedMedia) -> Unit,
    isFiltered: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionTitle(title = title)
        if (items.isEmpty()) {
            EmptyCarouselState(
                text = if (isFiltered) {
                    stringResource(R.string.home_active_filtered_empty)
                } else {
                    stringResource(R.string.home_active_empty)
                },
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                        onQuickSetProgress = { onQuickSetProgress(trackedMedia, it) },
                        onQuickComplete = { onQuickComplete(trackedMedia) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCarouselState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine.copy(alpha = 0.74f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
        )
    }
}

@Composable
private fun DashboardSectionTitle(title: String) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OmnilogColors.AppLine),
        )
    }
}

/**
 * Says so when the dashboard is leaving a section out, and undoes it in one tap. The control itself
 * lives in Settings — it is set once and then forgotten, which is exactly why the forgetting needs
 * to be visible here rather than nowhere.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardFilterNotice(
    hiddenSections: List<MediaSection>,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val names = hiddenSections.map { stringResource(it.titleResId) }.joinToString(", ")
    // Wraps at large font scales rather than the label crushing the action out of reach (UX-09).
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.home_hidden_sections_notice, names),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
        )
        Text(
            text = stringResource(R.string.home_hidden_sections_show_all),
            // Padding inside the clickable, so the tap target is bigger than the glyphs.
            modifier = Modifier
                .clickable(onClick = onShowAll)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.Dashboard,
        )
    }
}

@Composable
private fun HomeMediaTile(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    onQuickSetProgress: ((Int) -> Unit)? = null,
    onQuickComplete: (() -> Unit)? = null,
    onQuickStart: (() -> Unit)? = null,
) {
    val session = trackedMedia.currentSession
    val isGame = trackedMedia.item.type == MediaType.Game
    val quickActionsEnabled = onQuickSetProgress != null && onQuickComplete != null &&
        session != null &&
        (session.status == TrackingStatus.InProgress || session.status == TrackingStatus.Paused)
    val startActionEnabled = onQuickStart != null &&
        (session?.status == TrackingStatus.Planned || session?.status == TrackingStatus.Paused)
    var showQuickSheet by remember(trackedMedia.item.id) { mutableStateOf(false) }
    LaunchedEffect(quickActionsEnabled) {
        if (!quickActionsEnabled) showQuickSheet = false
    }

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
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (trackedMedia.item.ownership.isOwned) {
                    OwnedBadge()
                }
                CardStatusIcon(status = session?.status)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF17110D).copy(alpha = 0.16f),
                                Color(0xFF17110D).copy(alpha = 0.62f),
                                Color(0xFF15110E).copy(alpha = 0.98f),
                            ),
                        ),
                    ),
            )
            if (quickActionsEnabled) {
                TileQuickActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.quick_progress_open),
                    accent = accent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    onClick = { showQuickSheet = true },
                )
            } else if (startActionEnabled) {
                TileQuickActionButton(
                    icon = Icons.Filled.PlayArrow,
                    // Same action, different word: you start something planned, you resume a pause.
                    contentDescription = if (session?.status == TrackingStatus.Paused) {
                        stringResource(R.string.home_paused_resume)
                    } else {
                        stringResource(R.string.home_planned_start)
                    },
                    accent = accent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    onClick = { onQuickStart?.invoke() },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = displayMediaTitle(trackedMedia.item.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogColors.AppInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = session.progressLabel(
                                progressTotal = trackedMedia.item.progressTotal
                                    .takeUnless { trackedMedia.item.type == MediaType.Game },
                                mediaType = trackedMedia.item.type,
                            ),
                            style = if (isGame) {
                                MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            fontWeight = if (isGame) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isGame) accent else OmnilogColors.AppInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    RatingSlot(rating = session?.rating, accent = accent)
                }
                trackedMedia.item.progressTotal
                    .takeUnless { trackedMedia.item.type == MediaType.Game }
                    ?.takeIf { it > 0 }
                    ?.let { progressTotal ->
                        ProgressBar(
                            fraction = session.progressFraction(progressTotal),
                            color = accent,
                        )
                    }
            }
        }
    }

    if (showQuickSheet && quickActionsEnabled) {
        QuickProgressSheet(
            trackedMedia = trackedMedia,
            accent = accent,
            onSetProgress = { onQuickSetProgress?.invoke(it) },
            onComplete = {
                onQuickComplete?.invoke()
                showQuickSheet = false
            },
            onDismiss = { showQuickSheet = false },
        )
    }
}

@Composable
private fun TileQuickActionButton(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppBackground.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.60f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CardStatusIcon(
    status: TrackingStatus?,
    modifier: Modifier = Modifier,
) {
    val color = status?.stateColor ?: Color.White.copy(alpha = 0.28f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (status == null) 0.10f else 0.92f),
        contentColor = if (status == null) Color.White.copy(alpha = 0.34f) else Color.Black,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (status != null) {
                Icon(
                    painter = painterResource(status.iconResId),
                    contentDescription = status.label(),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RatingSlot(
    rating: Int?,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .width(42.dp)
            .height(32.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (rating == null) {
            Box(modifier = Modifier.height(32.dp))
        } else {
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.24f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

/**
 * First run: the library is empty, so every dashboard section below would be empty too.
 * Offers one entry point per media section rather than a generic add, because the choice of
 * section is what the rest of the product is organized around.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyHomeState(
    onAddToSection: (MediaSection) -> Unit,
    onImportBackup: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            Text(
                text = stringResource(R.string.home_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogColors.AppMuted,
            )
            FlowRow(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaSection.entries.forEach { section ->
                    OutlinedButton(
                        onClick = { onAddToSection(section) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = section.accent),
                        border = BorderStroke(1.dp, section.accent.copy(alpha = 0.58f)),
                    ) {
                        Icon(
                            painter = painterResource(section.navIconResId),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(section.titleResId),
                            modifier = Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            TextButton(
                onClick = onImportBackup,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_empty_import_backup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.Dashboard,
                )
            }
        }
    }
}

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

@Composable
private fun TrackingStatus.label(): String =
    when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }

private fun MediaType.sectionAccent(): Color =
    when (this) {
        MediaType.Anime -> MediaSection.Anime.accent
        MediaType.Book -> MediaSection.Books.accent
        MediaType.Movie,
        MediaType.TvShow,
            -> MediaSection.Movies.accent
        MediaType.Game -> MediaSection.Games.accent
    }

private fun MediaType.dashboardSection(): MediaSection =
    when (this) {
        MediaType.Anime -> MediaSection.Anime
        MediaType.Book -> MediaSection.Books
        MediaType.Movie,
        MediaType.TvShow,
            -> MediaSection.Movies
        MediaType.Game -> MediaSection.Games
    }

private val MediaSection.iconResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.drawable.ic_nav_anime
        MediaSection.Books -> R.drawable.ic_nav_books
        MediaSection.Movies -> R.drawable.ic_nav_movies_tv
        MediaSection.Games -> R.drawable.ic_nav_games
    }

private fun TrackedMedia.matchesDashboardQuery(query: String): Boolean {
    if (query.isBlank()) return false
    return item.title.contains(query, ignoreCase = true) ||
        item.originalTitle?.contains(query, ignoreCase = true) == true ||
        item.creators.any { it.contains(query, ignoreCase = true) } ||
        item.genres.any { it.contains(query, ignoreCase = true) } ||
        collection?.name?.contains(query, ignoreCase = true) == true
}

private fun TrackingSession?.progressFraction(progressTotal: Int?): Float {
    val current = this?.progressCurrent ?: 0
    if (progressTotal == null || progressTotal <= 0) return 0f
    return current.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
}

private fun TrackingSession?.progressLabel(progressTotal: Int?, mediaType: MediaType): String {
    val current = this?.progressCurrent ?: 0
    val unit = mediaType.progressUnit()
    return if (progressTotal != null) "$current/$progressTotal $unit" else "$current $unit"
}

private fun MediaType.progressUnit(): String = when (this) {
    MediaType.Anime,
    MediaType.TvShow,
        -> "episodis"
    MediaType.Book -> "pàgines"
    MediaType.Movie -> "min"
    MediaType.Game -> "h"
}

private fun TrackedMedia.latestActivityMillis(): Long =
    sessions.maxOfOrNull { it.updatedAtEpochMillis } ?: 0L

/** The date this was finished, or null if it is not completed or carries no finish date. */
private fun TrackedMedia.completionDate(): LocalDate? =
    currentSession
        ?.takeIf { it.status == TrackingStatus.Completed }
        ?.finishedAt

private val catalan = Locale("ca")

/** Grouped thousands, so a year's reading reads as `1.482` rather than `1482`. */
private val volumeFormat: NumberFormat = NumberFormat.getIntegerInstance(catalan)

private val ratingFormat: NumberFormat = NumberFormat.getNumberInstance(catalan).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}
