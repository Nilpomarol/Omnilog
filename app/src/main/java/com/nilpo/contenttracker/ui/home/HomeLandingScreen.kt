package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeLandingScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onSectionSearch: (MediaSection, String) -> Unit,
    onStatsClick: () -> Unit,
    onObjectivesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val objectiveProgress = remember(items, uiState.objectives) { ObjectiveCalculator().calculate(items, uiState.objectives) }.filter { it.objective.archivedAtEpochMillis == null && !it.isExpired(LocalDate.now()) }
    val context = LocalContext.current
    val dashboardPreferences = remember(context) {
        context.getSharedPreferences("omnilog_dashboard_preferences", android.content.Context.MODE_PRIVATE)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    var hideGamesFromActive by rememberSaveable {
        mutableStateOf(dashboardPreferences.getBoolean(HideGamesFromActivePreferenceKey, false))
    }
    val dashboardListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val normalizedSearchQuery = searchQuery.trim()
    val searchMatches = items
        .filter { it.matchesDashboardQuery(normalizedSearchQuery) }
        .sortedBy { displayMediaTitle(it.item.title).lowercase() }
    val activeItems = items
        .filter { it.currentSession?.status == TrackingStatus.InProgress }
        .sortedByDescending { it.latestActivityMillis() }
    val activeItemsVisible = activeItems
        .filter { trackedMedia -> !hideGamesFromActive || trackedMedia.item.type != MediaType.Game }
        .take(8)
    val activeItemsHasGames = activeItems.any { trackedMedia -> trackedMedia.item.type == MediaType.Game }
    val topRatedItems = items
        .filter { it.bestRating() != null }
        .sortedWith(
            compareByDescending<TrackedMedia> { it.bestRating() ?: 0 }
                .thenByDescending { it.latestActivityMillis() },
        )
        .take(8)
    val recentItems = items
        .filter { it.latestActivityMillis() > 0L }
        .sortedByDescending { it.latestActivityMillis() }
        .take(8)

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
                        EmptyHomeState()
                    }
                } else {
                    if (activeItems.isNotEmpty()) {
                        item {
                            HomeActiveCarousel(
                                title = stringResource(R.string.home_active_title),
                                items = activeItemsVisible,
                                onMediaClick = onMediaClick,
                                trailingContent = if (activeItemsHasGames) {
                                    {
                                        DashboardToggle(
                                            label = stringResource(R.string.home_active_hide_games),
                                            checked = hideGamesFromActive,
                                            onCheckedChange = { isChecked ->
                                                hideGamesFromActive = isChecked
                                                dashboardPreferences
                                                    .edit()
                                                    .putBoolean(HideGamesFromActivePreferenceKey, isChecked)
                                                    .apply()
                                            },
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }
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

                    if (topRatedItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_top_rated_title),
                                items = topRatedItems,
                                onMediaClick = onMediaClick,
                            )
                        }
                    }

                    if (recentItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_recent_title),
                                items = recentItems,
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
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = snapshot.completedInPeriod.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.Completed,
                )
                Column(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_analytics_preview_completed),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppInk,
                    )
                    Text(
                        text = stringResource(R.string.home_analytics_preview_description),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MonthlyActivityPreview(buckets = snapshot.completedByMonth)
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
            .height(86.dp),
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

@Composable
private fun HomeCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionTitle(
            title = title,
            trailingContent = trailingContent,
        )
        if (items.isEmpty()) {
            EmptyCarouselState()
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
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
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionTitle(
            title = title,
            trailingContent = trailingContent,
        )
        if (items.isEmpty()) {
            EmptyCarouselState()
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { trackedMedia ->
                    HomeMediaTile(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.sectionAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCarouselState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine.copy(alpha = 0.74f)),
    ) {
        Text(
            text = stringResource(R.string.home_active_filtered_empty),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogColors.AppMuted,
        )
    }
}

@Composable
private fun DashboardSectionTitle(
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
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
            color = OmnilogColors.AppInk,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OmnilogColors.AppLine),
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun DashboardToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(24.dp)
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(999.dp),
        color = if (checked) OmnilogColors.Dashboard.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (checked) OmnilogColors.Dashboard.copy(alpha = 0.42f) else OmnilogColors.AppLine.copy(alpha = 0.70f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (checked) OmnilogColors.Dashboard else OmnilogColors.AppMuted.copy(alpha = 0.70f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeMediaTile(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
) {
    val session = trackedMedia.currentSession
    val isGame = trackedMedia.item.type == MediaType.Game

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
                            color = if (isGame) accent else Color.White.copy(alpha = 0.78f),
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

@Composable
private fun EmptyHomeState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Text(
            text = stringResource(R.string.home_empty_state),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogColors.AppMuted,
        )
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

private fun TrackingSession.statsDate(): LocalDate? {
    return finishedAt
        ?: startedAt
        ?: updatedAtEpochMillis.takeIf { millis -> millis > 0L }?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
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
    MediaType.Book -> "pagines"
    MediaType.Movie -> "min"
    MediaType.Game -> "h"
}

private fun TrackingSession.updatedDate(): LocalDate? {
    if (updatedAtEpochMillis <= 0L) return null
    return Instant.ofEpochMilli(updatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun TrackedMedia.latestActivityMillis(): Long =
    sessions.maxOfOrNull { it.updatedAtEpochMillis } ?: 0L

private fun TrackedMedia.bestRating(): Int? =
    sessions.mapNotNull { it.rating }.maxOrNull()

private const val HideGamesFromActivePreferenceKey = "hide_games_from_active"
