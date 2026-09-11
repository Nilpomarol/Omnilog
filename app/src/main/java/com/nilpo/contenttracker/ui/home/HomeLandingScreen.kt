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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.progressLabel
import com.nilpo.contenttracker.ui.common.rememberActiveFilterPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenActiveSections
import com.nilpo.contenttracker.ui.common.writeHiddenActiveSections
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.timeline.TimelineRecentActivity
import java.time.LocalDate

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
    onQuickComplete: (TrackedMedia, QuickCompletion) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val objectiveProgress = remember(items, uiState.objectives) { ObjectiveCalculator().calculate(items, uiState.objectives) }.filter { it.objective.archivedAtEpochMillis == null }
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
    val today = LocalDate.now()
    val plannedCandidates = remember(items, today) { prioritizeHomePlannedItems(items, today) }
    // Longest-stalled first: the whole point of the section is the titles you have stopped
    // noticing, so the ones you touched most recently are the least useful to surface.
    val pausedCandidates = items
        .filter { it.currentSession?.status == TrackingStatus.Paused }
        .sortedBy { it.currentSession?.updatedAtEpochMillis ?: 0L }
    val isVisibleNow = { media: TrackedMedia ->
        media.item.type.dashboardSection() !in hiddenActiveSections
    }

    val activeItemsVisible = activeCandidates.filter(isVisibleNow).take(8)
    val plannedItems = plannedCandidates.take(8)
    val pausedItems = pausedCandidates.take(8)

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
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
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
                        HomeRhythmCard(
                            items = items,
                            objectives = objectiveProgress,
                            onStatsClick = onStatsClick,
                            onObjectivesClick = onObjectivesClick,
                        )
                    }
                    item {
                        TimelineRecentActivity(
                            entries = timelineEntries,
                            onEntryClick = { onTimelineClick() },
                            maxEntries = 1,
                            onViewAll = onTimelineClick,
                        )
                    }

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
    val creator = trackedMedia.creatorNames().firstOrNull()
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
 * progress sheet — it only appears on tiles whose status can act on it (see [HomeCollectionCard]).
 */
@Composable
private fun HomeCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    emptyText: String? = null,
    onQuickCommitProgress: ((TrackedMedia, Int) -> Unit)? = null,
    onQuickComplete: ((TrackedMedia, QuickCompletion) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardSectionTitle(title = title)
        if (items.isEmpty()) {
            emptyText?.let { EmptyCarouselState(text = it) }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.item.id }) { trackedMedia ->
                    HomeCollectionCard(
                        trackedMedia = trackedMedia,
                        accent = OmnilogTheme.accents.Dashboard,
                        onClick = { onMediaClick(trackedMedia) },
                        onQuickCommitProgress = onQuickCommitProgress?.let { commit ->
                            { value: Int -> commit(trackedMedia, value) }
                        },
                        onQuickComplete = onQuickComplete?.let { complete ->
                            { completion: QuickCompletion -> complete(trackedMedia, completion) }
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
    onQuickComplete: (TrackedMedia, QuickCompletion) -> Unit,
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
                items(items, key = { it.item.id }) { trackedMedia ->
                    HomeCollectionCard(
                        trackedMedia = trackedMedia,
                        accent = OmnilogTheme.accents.Dashboard,
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
 * The section heading: bold, friendly type, open space, and an optional control
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
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OmnilogTheme.colors.appInk,
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

/** Continue and planned cards retain the existing progress sheet and its tracking rules. */
@Composable
private fun HomeCollectionCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    onQuickCommitProgress: ((Int) -> Unit)? = null,
    onQuickComplete: ((QuickCompletion) -> Unit)? = null,
) {
    val session = trackedMedia.currentSession
    val isActive = session?.status == TrackingStatus.InProgress
    val isPaused = session?.status == TrackingStatus.Paused
    var showQuickSheet by remember(trackedMedia.item.id) { mutableStateOf(false) }
    val canUpdate = session?.status in setOf(TrackingStatus.InProgress, TrackingStatus.Planned) &&
        onQuickCommitProgress != null && onQuickComplete != null
    val title = displayMediaTitle(trackedMedia.item.title)
    if (isPaused) {
        Column(
            modifier = Modifier.width(88.dp).clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.width(88.dp).height(132.dp),
                shape = RoundedCornerShape(5.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else if (isActive) {
        HomeContinueCard(
            trackedMedia = trackedMedia,
            accent = accent,
            onClick = onClick,
            onProgressClick = if (canUpdate) ({ showQuickSheet = true }) else null,
        )
    } else {
        HomePlannedCard(
            trackedMedia = trackedMedia,
            accent = accent,
            onClick = onClick,
            onStartClick = if (canUpdate) ({ showQuickSheet = true }) else null,
        )
    }
    if (showQuickSheet && canUpdate) {
        QuickProgressSheet(
            trackedMedia = trackedMedia,
            accent = accent,
            onCommit = { onQuickCommitProgress?.invoke(it); showQuickSheet = false },
            onComplete = { onQuickComplete?.invoke(it); showQuickSheet = false },
            onDismiss = { showQuickSheet = false },
        )
    }
}

/**
 * A borderless planned item: cover, title and collection at the top, and a small Començar button
 * at the bottom. No card surface, so `Ara mateix` stays visually primary.
 */
@Composable
private fun HomePlannedCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    onStartClick: (() -> Unit)?,
) {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val collectionLabel = formatCollectionDisplayName(
        trackedMedia.collection?.name,
        trackedMedia.item.collectionSortOrder,
    )

    // Two items plus ~24dp of the third cover (32dp page padding, two 10dp gaps). The floor keeps
    // room for the Començar button, so narrow phones trade the peek for an intact button. Larger
    // text grows every item's height uniformly.
    Row(
        modifier = Modifier.width(((screenWidth - 76.dp) / 2).coerceIn(180.dp, 232.dp))
            .height(116.dp + 64.dp * (fontScale - 1f))
            .clickable(onClick = onClick),
    ) {
        MetadataCoverImage(
            coverUrl = trackedMedia.item.coverUrl,
            modifier = Modifier.width(72.dp).height(108.dp),
            shape = RoundedCornerShape(6.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        Column(
            // The button's 48dp touch target leaves 8dp below its 32dp visual, so the item is
            // 8dp taller than the cover to line their bottom edges up.
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 10.dp),
        ) {
            Text(
                text = displayMediaTitle(trackedMedia.item.title),
                style = MaterialTheme.typography.titleSmall,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            collectionLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            onStartClick?.let { onStart ->
                Button(
                    onClick = onStart,
                    modifier = Modifier.heightIn(min = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(start = 8.dp, end = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.12f),
                        contentColor = accent,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_planned_start),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
@Composable
private fun HomeMediaTypeLabel(type: MediaType) {
    Text(
        text = stringResource(when (type) {
            MediaType.Anime -> R.string.media_type_anime
            MediaType.Book -> R.string.media_type_book
            MediaType.Movie -> R.string.media_type_movie
            MediaType.TvShow -> R.string.media_type_tv_show
            MediaType.Game -> R.string.media_type_game
        }),
        modifier = Modifier.padding(top = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = when (type) {
            MediaType.Anime -> OmnilogTheme.accents.Anime
            MediaType.Book -> OmnilogTheme.accents.Books
            MediaType.Movie -> OmnilogTheme.accents.Movie
            MediaType.TvShow -> OmnilogTheme.accents.Series
            MediaType.Game -> OmnilogTheme.accents.Games
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Equal geometry across the carousel, including titles with no known progress total. */
@Composable
private fun HomeContinueCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    onProgressClick: (() -> Unit)?,
) {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    // Accessibility changes the whole carousel's height uniformly, never individual cards.
    val cardHeight = 132.dp + 100.dp * (fontScale - 1f)
    val session = trackedMedia.currentSession
    val total = trackedMedia.item.progressTotal
        ?.takeIf { it > 0 && trackedMedia.item.type != MediaType.Game }

    Surface(
        modifier = Modifier.width(minOf(300.dp, (screenWidth - 32.dp) * 0.86f)).height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Row(modifier = Modifier.fillMaxSize().clickable(onClick = onClick)) {
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.width(96.dp).fillMaxHeight(),
                shape = RoundedCornerShape(0.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
            ) {
                Text(
                    text = displayMediaTitle(trackedMedia.item.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HomeMediaTypeLabel(trackedMedia.item.type)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = session.progressLabel(total, trackedMedia.item.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (total != null) {
                            ProgressBar(session.progressFraction(total), accent)
                        }
                    }
                    onProgressClick?.let { onProgress ->
                        // Small visual control with a full 48dp touch target.
                        IconButton(onClick = onProgress, modifier = Modifier.size(48.dp)) {
                            Box(
                                modifier = Modifier.size(28.dp)
                                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.quick_progress_open),
                                    tint = accent,
                                    modifier = Modifier.size(18.dp),
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
private fun ProgressBar(
    fraction: Float,
    color: Color,
) {
    val animatedFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "Home progress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogTheme.colors.appLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
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
        creatorNames().any { it.contains(query, ignoreCase = true) } ||
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
