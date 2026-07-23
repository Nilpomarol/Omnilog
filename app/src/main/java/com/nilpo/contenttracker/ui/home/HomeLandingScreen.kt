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
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.nilpo.contenttracker.core.stats.ComparisonBasis
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.ui.common.CoverScrim
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.stats.MetricBand
import com.nilpo.contenttracker.ui.stats.MetricBandLead
import com.nilpo.contenttracker.ui.stats.MetricBandSupporting
import com.nilpo.contenttracker.ui.stats.comparisonBasisLabel
import com.nilpo.contenttracker.ui.stats.comparisonBasisLabelShort
import com.nilpo.contenttracker.ui.stats.intMetricDelta
import com.nilpo.contenttracker.ui.stats.ratingMetricDelta
import com.nilpo.contenttracker.ui.common.progressLabel
import com.nilpo.contenttracker.ui.common.rememberActiveFilterPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenActiveSections
import com.nilpo.contenttracker.ui.common.writeHiddenActiveSections
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.timeline.TimelineRecentActivity
import com.nilpo.contenttracker.ui.theme.OnCoverInk
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeLandingScreen(
    uiState: HomeUiState,
    timelineEntries: List<TimelineEntry>,
    onMediaClick: (TrackedMedia) -> Unit,
    onSectionSearch: (MediaSection, String) -> Unit,
    onStatsClick: () -> Unit,
    onTimelineClick: () -> Unit,
    onObjectivesClick: () -> Unit,
    onAddToSection: (MediaSection) -> Unit,
    onImportBackup: () -> Unit,
    onQuickCommitProgress: (TrackedMedia, Int) -> Unit = { _, _ -> },
    onQuickComplete: (TrackedMedia, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val objectiveProgress = remember(items, uiState.objectives) { ObjectiveCalculator().calculate(items, uiState.objectives) }.filter { it.objective.archivedAtEpochMillis == null && !it.isExpired(LocalDate.now()) }
    val activeFilterPreferences = rememberActiveFilterPreferences()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    // Settings owns the Ara mateix filter; Home renders the result and offers a way out.
    val hiddenActiveSections by rememberHiddenActiveSections(activeFilterPreferences)
    val dashboardListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val normalizedSearchQuery = searchQuery.trim()
    val searchMatches = items
        .filter { it.matchesDashboardQuery(normalizedSearchQuery) }
        .sortedBy { displayMediaTitle(it.item.title).lowercase() }
    // Ara mateix needs its candidates before filtering so its empty copy can name the filter as
    // the reason. The remaining Home sections deliberately use the complete library.
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

    val isVisibleNow = { media: TrackedMedia ->
        media.item.type.dashboardSection() !in hiddenActiveSections
    }

    val activeItemsVisible = activeCandidates.filter(isVisibleNow).take(8)
    val plannedItems = plannedCandidates.take(8)
    val pausedItems = pausedCandidates.take(8)
    val completedItems = completedCandidates.take(8)

    LaunchedEffect(dashboardListState.isScrollInProgress) {
        if (dashboardListState.isScrollInProgress) {
            showSearchOverlay = false
        }
    }

    Surface(
        modifier = modifier,
        color = OmnilogTheme.colors.appBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = dashboardListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
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
                    item {
                        HomeActiveCarousel(
                            title = stringResource(R.string.home_active_title),
                            items = activeItemsVisible,
                            hiddenSections = MediaSection.entries.filter { it in hiddenActiveSections },
                            onShowAllSections = {
                                activeFilterPreferences.writeHiddenActiveSections(emptySet())
                            },
                            onMediaClick = onMediaClick,
                            onQuickCommitProgress = onQuickCommitProgress,
                            onQuickComplete = onQuickComplete,
                            isFiltered = hiddenActiveSections.isNotEmpty() && activeCandidates.isNotEmpty(),
                        )
                    }

                    item {
                        HomeCarousel(
                            title = stringResource(R.string.home_planned_title),
                            items = plannedItems,
                            onMediaClick = onMediaClick,
                            emptyText = stringResource(R.string.home_planned_empty),
                            onQuickCommitProgress = onQuickCommitProgress,
                            onQuickComplete = onQuickComplete,
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

                    item {
                        TimelineRecentActivity(
                            entries = timelineEntries,
                            onEntryClick = { mediaItemId ->
                                items.firstOrNull { it.item.id == mediaItemId }?.let(onMediaClick)
                            },
                            onViewAll = onTimelineClick,
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
                                onQuickCommitProgress = onQuickCommitProgress,
                                onQuickComplete = onQuickComplete,
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
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_analytics_preview_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Condensed here so it shares the title's line instead of costing the card a row.
                // Each metric still announces the basis in full to screen readers.
                snapshot.deltas.basis?.let { basis ->
                    Text(
                        text = comparisonBasisLabelShort(basis),
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_analytics_preview_open),
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogTheme.accents.Dashboard,
                )
            }
            DashboardPeriodMetrics(snapshot = snapshot)
            MonthlyActivityPreview(buckets = snapshot.completionSessionsByMonth)
        }
    }
}

/**
 * The year's KPIs in one band: completion sessions as the lead figure, with the average rating and
 * revisit count as supporting values behind a rule. Each carries its change against the same period
 * of last year, and the basis is named once beneath the band rather than on every chip.
 *
 * Consumption totals deliberately do not appear here — they are per-medium values in units that
 * cannot be compared, and the statistics page shows them where that context exists.
 */
@Composable
private fun DashboardPeriodMetrics(
    snapshot: StatsSnapshot,
    modifier: Modifier = Modifier,
) {
    val basisLabel = snapshot.deltas.basis?.let { basis -> comparisonBasisLabel(basis) }

    MetricBand(
        modifier = modifier,
        lead = {
            MetricBandLead(
                value = snapshot.completionSessions.toString(),
                label = stringResource(R.string.home_analytics_preview_completed),
                accent = OmnilogTheme.accents.Completed,
                delta = intMetricDelta(snapshot.deltas.completionSessions),
                basisLabel = basisLabel,
                leadValueStyle = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1.1f),
            )
        },
        supporting = {
            MetricBandSupporting(
                label = stringResource(R.string.home_analytics_average_rating_short),
                accessibleLabel = stringResource(R.string.home_analytics_average_rating),
                value = snapshot.averageRating?.let { average -> ratingFormat.format(average) } ?: "—",
                accent = OmnilogTheme.colors.appInk,
                delta = ratingMetricDelta(snapshot.deltas.averageRating),
                basisLabel = basisLabel,
                valueStyle = MaterialTheme.typography.titleMedium,
            )
            MetricBandSupporting(
                label = stringResource(R.string.home_analytics_revisits_short),
                accessibleLabel = stringResource(R.string.stats_summary_revisits),
                value = snapshot.revisitCount.toString(),
                accent = OmnilogTheme.accents.Books,
                delta = intMetricDelta(snapshot.deltas.revisits),
                basisLabel = basisLabel,
                valueStyle = MaterialTheme.typography.titleMedium,
            )
        },
    )
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
            .heightIn(min = 92.dp),
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
                        .height(70.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val barHeight = if (bucket.value == 0) 2 else {
                        (70 * bucket.value / maxValue).coerceAtLeast(4)
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
                                    if (bucket.value > 0) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appLine.copy(alpha = 0.58f),
                                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                                ),
                        )
                    }
                }
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
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
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = if (query.isNotBlank()) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appMuted,
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
                    color = OmnilogTheme.colors.appInk,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(OmnilogTheme.accents.Dashboard),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.search_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = OmnilogTheme.colors.appMuted,
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
                        tint = OmnilogTheme.colors.appMuted,
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
        color = OmnilogTheme.colors.appPanel.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
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
                            HorizontalDivider(color = OmnilogTheme.colors.appLine.copy(alpha = 0.58f))
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
                            HorizontalDivider(color = OmnilogTheme.colors.appLine.copy(alpha = 0.58f))
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
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
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
            tint = section.themedAccent(),
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
            color = OmnilogTheme.colors.appInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A titled row of media tiles. Pass [emptyText] for a section that should explain itself when it
 * has nothing (the daily surfaces near the top); omit it and the caller is expected to skip the
 * section entirely instead. Pass the quick action callbacks to give each tile a button opening the
 * progress sheet — it only appears on tiles whose status can act on it (see [HomeMediaTile]).
 */
@Composable
private fun HomeCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    emptyText: String? = null,
    onQuickCommitProgress: ((TrackedMedia, Int) -> Unit)? = null,
    onQuickComplete: ((TrackedMedia, Int) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardSectionTitle(title = title)
        if (items.isEmpty()) {
            emptyText?.let { EmptyCarouselState(text = it) }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                        onQuickCommitProgress = onQuickCommitProgress?.let { commit ->
                            { value: Int -> commit(trackedMedia, value) }
                        },
                        onQuickComplete = onQuickComplete?.let { complete ->
                            { value: Int -> complete(trackedMedia, value) }
                        },
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
    hiddenSections: List<MediaSection>,
    onShowAllSections: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    onQuickCommitProgress: (TrackedMedia, Int) -> Unit,
    onQuickComplete: (TrackedMedia, Int) -> Unit,
    isFiltered: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardSectionTitle(title = title) {
            if (hiddenSections.isNotEmpty()) {
                ActiveFilterIndicator(
                    hiddenSections = hiddenSections,
                    onShowAll = onShowAllSections,
                )
            }
        }
        if (items.isEmpty()) {
            EmptyCarouselState(
                text = if (isFiltered) {
                    stringResource(R.string.home_active_filtered_empty)
                } else {
                    stringResource(R.string.home_active_empty)
                },
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                        onQuickCommitProgress = { onQuickCommitProgress(trackedMedia, it) },
                        onQuickComplete = { onQuickComplete(trackedMedia, it) },
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
        color = OmnilogTheme.colors.appPanel.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine.copy(alpha = 0.74f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

/**
 * The carousel section heading: a title, a hairline reaching the far edge, and an optional control
 * riding at the end of it.
 *
 * [trailingContent] should stay compact. The row's height is whatever its tallest child is, so a
 * control with a 48.dp minimum — a Material `TextButton`, say — silently sets the height of every
 * heading on the page.
 */
@Composable
private fun DashboardSectionTitle(
    title: String,
    trailingContent: @Composable (() -> Unit)? = null,
) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OmnilogTheme.colors.appLine),
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun ActiveFilterIndicator(
    hiddenSections: List<MediaSection>,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (hiddenSections.size == 1) {
        stringResource(
            R.string.home_active_hidden_section_indicator,
            stringResource(hiddenSections.single().titleResId),
        )
    } else {
        stringResource(R.string.home_active_hidden_sections_indicator, hiddenSections.size)
    }
    Surface(
        onClick = onShowAll,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = OmnilogTheme.accents.Dashboard.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, OmnilogTheme.accents.Dashboard.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 9.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.accents.Dashboard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.home_active_hidden_sections_show_all),
                modifier = Modifier.size(14.dp),
                tint = OmnilogTheme.accents.Dashboard,
            )
        }
    }
}

// A 2:3 poster, so the cover fills the tile without cropping. Keep the ratio if you resize:
// the dashboard stacks several carousels, and tile height is what decides how many are reachable
// without scrolling.
private val TileWidth = 132.dp
private val TileHeight = 198.dp

@Composable
private fun HomeMediaTile(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    onQuickCommitProgress: ((Int) -> Unit)? = null,
    onQuickComplete: ((Int) -> Unit)? = null,
) {
    val session = trackedMedia.currentSession
    val isGame = trackedMedia.item.type == MediaType.Game
    // Planned and Paused open the very same sheet as In progress. Starting something is just a
    // progress commit that happens to promote the status, so there is no reason for the dashboard
    // to offer a blind status flip that guesses you are at zero.
    val quickActionsEnabled = onQuickCommitProgress != null && onQuickComplete != null &&
        session != null &&
        (
            session.status == TrackingStatus.InProgress ||
                session.status == TrackingStatus.Paused ||
                session.status == TrackingStatus.Planned
            )
    var showQuickSheet by remember(trackedMedia.item.id) { mutableStateOf(false) }
    LaunchedEffect(quickActionsEnabled) {
        if (!quickActionsEnabled) showQuickSheet = false
    }

    Surface(
        modifier = Modifier
            .width(TileWidth)
            .height(TileHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (trackedMedia.item.isOwned) {
                    OwnedBadge()
                }
                CardStatusIcon(status = session?.status)
            }
            CoverScrim()
            if (quickActionsEnabled) {
                // One destination, but the glyph still previews what the sheet will lead with:
                // a play mark for something not currently running, a plus for adding to a total
                // already in motion.
                TileQuickActionButton(
                    icon = if (session?.status == TrackingStatus.InProgress) {
                        Icons.Filled.Add
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = when (session?.status) {
                        TrackingStatus.Paused -> stringResource(R.string.home_paused_resume)
                        TrackingStatus.Planned -> stringResource(R.string.home_planned_start)
                        else -> stringResource(R.string.quick_progress_open)
                    },
                    accent = accent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    onClick = { showQuickSheet = true },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // The title gets the full tile width. Only the progress line shares its row with
                // the rating — the rating is short and the progress label is too, whereas titles
                // need every pixel at this tile size.
                Text(
                    text = displayMediaTitle(trackedMedia.item.title),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnCoverInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // The rating stands beside the whole progress block — label and bar both — so it
                // gets the height of the two stacked together and can stay large. The title keeps
                // the full width above it, which is what it needs at this tile size.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = session.progressLabel(
                                progressTotal = trackedMedia.item.progressTotal
                                    .takeUnless { trackedMedia.item.type == MediaType.Game },
                                mediaType = trackedMedia.item.type,
                            ),
                            style = if (isGame) {
                                MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp)
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            fontWeight = if (isGame) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.SemiBold
                            },
                            color = if (isGame) accent else OnCoverInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                    RatingSlot(rating = session?.rating, accent = accent)
                }
            }
        }
    }

    if (showQuickSheet && quickActionsEnabled) {
        QuickProgressSheet(
            trackedMedia = trackedMedia,
            accent = accent,
            onCommit = {
                onQuickCommitProgress?.invoke(it)
                showQuickSheet = false
            },
            onComplete = {
                onQuickComplete?.invoke(it)
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
    // Filled like the status and owned pills opposite it, but a little larger: those are read-only
    // badges, this one is the tile's only tap target and pill size was too small to hit reliably —
    // the rounded corner clips the touch area that would otherwise extend past it.
    Surface(
        onClick = onClick,
        modifier = modifier.size(30.dp),
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.86f),
        contentColor = Color.Black,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
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
    // Sized to its content and skipped entirely when unrated, so an unrated tile gives the whole
    // row back to the progress label instead of holding an empty column open.
    if (rating == null) return
    Text(
        text = rating.toString(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
    )
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
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = stringResource(R.string.home_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            FlowRow(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaSection.entries.forEach { section ->
                    OutlinedButton(
                        onClick = { onAddToSection(section) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = section.themedAccent()),
                        border = BorderStroke(1.dp, section.themedAccent().copy(alpha = 0.58f)),
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
                    color = OmnilogTheme.accents.Dashboard,
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
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogTheme.accents.Planned
        TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
        TrackingStatus.Completed -> OmnilogTheme.accents.Completed
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
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

@Composable
@ReadOnlyComposable
private fun MediaType.sectionAccent(): Color =
    when (this) {
        MediaType.Anime -> MediaSection.Anime.themedAccent()
        MediaType.Book -> MediaSection.Books.themedAccent()
        MediaType.Movie,
        MediaType.TvShow,
            -> MediaSection.Movies.themedAccent()
        MediaType.Game -> MediaSection.Games.themedAccent()
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
        MediaSection.Movies -> R.drawable.ic_media_movie
        MediaSection.Games -> R.drawable.ic_nav_games
    }

private fun TrackedMedia.matchesDashboardQuery(query: String): Boolean {
    if (query.isBlank()) return false
    return item.title.contains(query, ignoreCase = true) ||
        item.originalTitle?.contains(query, ignoreCase = true) == true ||
        item.creators.any { it.contains(query, ignoreCase = true) } ||
        item.genres.any { it.contains(query, ignoreCase = true) } ||
        item.tags.any { it.contains(query, ignoreCase = true) } ||
        collection?.name?.contains(query, ignoreCase = true) == true
}

private fun TrackingSession?.progressFraction(progressTotal: Int?): Float {
    val current = this?.progressCurrent ?: 0
    if (progressTotal == null || progressTotal <= 0) return 0f
    return current.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
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

private val ratingFormat: NumberFormat = NumberFormat.getNumberInstance(catalan).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}
