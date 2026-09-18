package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.common.OmnilogPanelPadding
import com.nilpo.contenttracker.ui.common.OmnilogPanelGroup
import com.nilpo.contenttracker.ui.common.OmnilogPanelDivider
import com.nilpo.contenttracker.ui.common.OmnilogChapterTitle
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
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
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
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
    /** Opens the profile's objectives, at the given one when a tile was tapped. */
    onObjectivesClick: (objectiveId: Long?) -> Unit,
    onStatusClick: (TrackingStatus) -> Unit,
    onOpenSection: (MediaSection) -> Unit,
    onImportFrom: (ImportSource) -> Unit,
    onImportBackup: () -> Unit,
    onQuickCommitProgress: (TrackedMedia, Int) -> Unit = { _, _ -> },
    onQuickComplete: (TrackedMedia, QuickCompletion) -> Unit = { _, _ -> },
    searchOpen: Boolean = false,
    searchQuery: String = "",
    searchFocused: Boolean = false,
    onSearchClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val objectiveProgress = remember(items, uiState.objectives) { ObjectiveCalculator().calculate(items, uiState.objectives) }.filter { it.objective.archivedAtEpochMillis == null }
    val activeFilterPreferences = rememberActiveFilterPreferences()
    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    // Settings owns the Ara mateix filter; Home only renders the result.
    val hiddenActiveSections by rememberHiddenActiveSections(activeFilterPreferences)
    val dashboardListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val normalizedSearchQuery = searchQuery.trim()
    val closeSearch = onSearchClose
    // The field lives in the top bar. Typing, or focusing it again after tapping away from the
    // results, brings them back over the list.
    LaunchedEffect(searchQuery, searchFocused) {
        if (searchFocused) showSearchOverlay = true
    }
    BackHandler(enabled = searchOpen, onBack = closeSearch)
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Search takes over the header itself, so Ara mateix leads Home and the list never moves.
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = dashboardListState,
                    // No side padding of its own: sections keep the editorial pages' gutter, and the
                    // carousels spend it as content padding so their covers run to the screen edge.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (items.isEmpty()) {
                        item {
                            EmptyHomeState(
                                onOpenSection = onOpenSection,
                                onImportFrom = onImportFrom,
                                onImportBackup = onImportBackup,
                            )
                        }
                    } else {
                        item {
                            HomeActiveCarousel(
                                title = stringResource(R.string.home_active_title),
                                items = activeItemsVisible,
                                onMediaClick = onMediaClick,
                                onQuickCommitProgress = onQuickCommitProgress,
                                onQuickComplete = onQuickComplete,
                                isFiltered = hiddenActiveSections.isNotEmpty() && activeCandidates.isNotEmpty(),
                                onShowAll = { onStatusClick(TrackingStatus.InProgress) },
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
                                onShowAll = { onStatusClick(TrackingStatus.Planned) },
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
                                onViewAll = onTimelineClick,
                                modifier = Modifier.padding(horizontal = DetailGutter),
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
                                    onShowAll = { onStatusClick(TrackingStatus.Paused) },
                                )
                            }
                        }
                    }
                }
                // This box starts just below the header's field, so the results sit a few dp under it.
                if (normalizedSearchQuery.isNotEmpty() && showSearchOverlay) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 6.dp)
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
                            closeSearch()
                            onMediaClick(it)
                        },
                        onSectionSearch = { section ->
                            val query = normalizedSearchQuery
                            closeSearch()
                            onSectionSearch(section, query)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(start = DetailGutter, top = 6.dp, end = DetailGutter),
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
    onShowAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DashboardSectionTitle(title = title, onClick = onShowAll, modifier = Modifier.padding(horizontal = DetailGutter))
        if (items.isEmpty()) {
            emptyText?.let { EmptyCarouselState(text = it) }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = DetailGutter),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
    onMediaClick: (TrackedMedia) -> Unit,
    onQuickCommitProgress: (TrackedMedia, Int) -> Unit,
    onQuickComplete: (TrackedMedia, QuickCompletion) -> Unit,
    isFiltered: Boolean,
    onShowAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DashboardSectionTitle(title = title, onClick = onShowAll, modifier = Modifier.padding(horizontal = DetailGutter))
        if (items.isEmpty()) {
            EmptyCarouselState(
                text = if (isFiltered) {
                    stringResource(R.string.home_active_filtered_empty)
                } else {
                    stringResource(R.string.home_active_empty)
                },
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = DetailGutter),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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

/** An empty section says so in plain muted text, as the collection page does. */
@Composable
private fun EmptyCarouselState(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = DetailGutter),
        style = MaterialTheme.typography.bodyMedium,
        color = OmnilogTheme.colors.appMuted,
    )
}

/** The section heading: the editorial pages' serif, open space, and an arrow when it opens a full list. */
@Composable
private fun DashboardSectionTitle(
    title: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailSectionTitle(text = title, modifier = Modifier.weight(1f))
        // The heading itself opens the full list, marked with the same arrow as Activitat recent
        // rather than a pill button competing with the section's own controls.
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.show_all),
                modifier = Modifier.size(18.dp),
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
    var showQuickSheet by remember(trackedMedia.item.id) { mutableStateOf(false) }
    // Starting a planned title and resuming a paused one go through the same sheet, which promotes
    // either to In progress.
    val canUpdate = session?.status in setOf(TrackingStatus.InProgress, TrackingStatus.Planned, TrackingStatus.Paused) &&
        onQuickCommitProgress != null && onQuickComplete != null
    if (isActive) {
        HomeContinueCard(
            trackedMedia = trackedMedia,
            onClick = onClick,
            onProgressClick = if (canUpdate) ({ showQuickSheet = true }) else null,
        )
    } else {
        val isPaused = session?.status == TrackingStatus.Paused
        val item = trackedMedia.item
        HomePlannedCard(
            trackedMedia = trackedMedia,
            accent = accent,
            // For something already begun, where you left off says more than which collection it is in.
            detail = if (isPaused) {
                session.progressLabel(item.progressTotal?.takeIf { item.type != MediaType.Game }, item.type)
            } else {
                formatCollectionDisplayName(trackedMedia.collection?.name, item.collectionSortOrder)
            },
            actionLabel = stringResource(if (isPaused) R.string.home_paused_resume else R.string.home_planned_start),
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
 * A borderless planned or paused item: cover, title and [detail] at the top, and a small action
 * button (Començar or Reprèn) at the bottom. No card surface, so `Ara mateix` stays visually primary.
 */
@Composable
private fun HomePlannedCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    detail: String?,
    actionLabel: String,
    onClick: () -> Unit,
    onStartClick: (() -> Unit)?,
) {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)

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
            detail?.let {
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
                // A quiet action in the accent, as the editorial pages draw theirs, rather than a filled button.
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onStart)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent,
                    )
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
        }
    }
}
/**
 * Home's hero: a poster tile carrying only the title and progress over the artwork, with equal
 * geometry across the carousel, including titles with no known progress total.
 */
@Composable
private fun HomeContinueCard(
    trackedMedia: TrackedMedia,
    onClick: () -> Unit,
    onProgressClick: (() -> Unit)?,
) {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    // About three and a half posters in view (16dp page padding, three 10dp gaps), so the carousel
    // visibly continues. The card is the 2:3 poster itself; larger text just rises further up it.
    val cardWidth = ((screenWidth - 46.dp) / 3.5f).coerceIn(100.dp, 150.dp)
    val type = trackedMedia.item.type
    // The text sits on a dark scrim in either theme, so it takes the dark-theme accents.
    val accent = when (type) {
        MediaType.Anime -> OmnilogColors.Anime
        MediaType.Book -> OmnilogColors.Books
        MediaType.Movie -> OmnilogColors.Movie
        MediaType.TvShow -> OmnilogColors.Series
        MediaType.Game -> OmnilogColors.Games
    }
    val session = trackedMedia.currentSession
    val total = trackedMedia.item.progressTotal?.takeIf { it > 0 && type != MediaType.Game }
    val shape = RoundedCornerShape(10.dp)
    // The scrim is kept light to show the artwork, so a soft shadow keeps text legible on busy covers.
    val legible = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), blurRadius = 6f)

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardWidth * 1.5f)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        MetadataCoverImage(
            coverUrl = trackedMedia.item.coverUrl,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 7.dp),
        ) {
            Text(
                text = displayMediaTitle(trackedMedia.item.title),
                style = MaterialTheme.typography.titleSmall.copy(shadow = legible),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val progress = session.progressLabel(total, type)
            if (total != null) {
                // The bar alone carries progress on the poster; the figure stays for screen readers.
                val fraction = ((session?.progressCurrent ?: 0).toFloat() / total).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .clearAndSetSemantics { contentDescription = progress },
                ) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accent))
                }
            } else {
                // Without a total (games, open-ended titles) there is nothing to fill, so keep the figure.
                Text(
                    text = progress,
                    style = MaterialTheme.typography.labelSmall.copy(shadow = legible),
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        onProgressClick?.let { onProgress ->
            // In the top corner so the text below keeps the poster's full width on narrow cards.
            // A small visual; Compose extends the touch area to 48dp.
            IconButton(
                onClick = onProgress,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(36.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.75f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.quick_progress_open),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * First run: the library is empty, so every dashboard section would be empty too. Home becomes a
 * one-screen guide in the Configuració style instead: how a title travels through Home first, then
 * the four sections as tiles that open each one, then the importers for what is already tracked
 * elsewhere. Kept compact enough to fit a normal phone without scrolling at the default text size.
 */
@Composable
private fun EmptyHomeState(
    onOpenSection: (MediaSection) -> Unit,
    onImportFrom: (ImportSource) -> Unit,
    onImportBackup: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OmnilogChapterTitle(
            title = stringResource(R.string.home_empty_title),
            note = stringResource(R.string.home_empty_state),
            modifier = Modifier.padding(horizontal = DetailGutter),
        )

        OmnilogPanelGroup(label = stringResource(R.string.home_empty_how_label), labelGutter = DetailGutter) {
            listOf(
                R.string.home_empty_how_start_title to R.string.home_empty_how_start_body,
                R.string.home_empty_how_progress_title to R.string.home_empty_how_progress_body,
                R.string.home_empty_how_finish_title to R.string.home_empty_how_finish_body,
            ).forEachIndexed { index, (titleResId, bodyResId) ->
                if (index > 0) OmnilogPanelDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OmnilogPanelPadding, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.width(20.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFontFamily),
                        color = OmnilogTheme.accents.Dashboard,
                        textAlign = TextAlign.Center,
                    )
                    Column {
                        Text(
                            text = stringResource(titleResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        Text(
                            text = stringResource(bodyResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
            }
        }

        OmnilogPanelGroup(label = stringResource(R.string.home_empty_sections_label), labelGutter = DetailGutter) {
            OnboardingTileRow(
                tiles = MediaSection.entries.map { section ->
                    OnboardingTile(
                        label = stringResource(section.titleResId),
                        onClick = { onOpenSection(section) },
                    ) { SectionMark(section) }
                },
            )
        }

        OmnilogPanelGroup(label = stringResource(R.string.home_empty_import_label), labelGutter = DetailGutter) {
            OnboardingTileRow(
                tiles = listOf(
                    ImportSource.ImdbCsv to ExternalRatingSource.Imdb,
                    ImportSource.StoryGraphCsv to ExternalRatingSource.StoryGraph,
                    ImportSource.MalXml to ExternalRatingSource.Mal,
                ).map { (source, logo) ->
                    OnboardingTile(label = logo.onboardingName(), onClick = { onImportFrom(source) }) {
                        // StoryGraph's mark sits padded on a paper chip, so it needs more height to read
                        // at the same size as the bare wordmarks beside it.
                        if (logo == ExternalRatingSource.StoryGraph) {
                            ProviderLogo(source = logo, height = 34.dp, maxWidth = 34.dp)
                        } else {
                            ProviderLogo(source = logo, height = 20.dp, maxWidth = 40.dp)
                        }
                    }
                } + OnboardingTile(
                    label = stringResource(R.string.home_empty_import_backup),
                    onClick = onImportBackup,
                ) {
                    // The launcher icon's foreground, scaled past its adaptive-icon safe margin so
                    // only the dark square with the OL mark shows.
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .graphicsLayer {
                                scaleX = 1.12f
                                scaleY = 1.12f
                            },
                    )
                },
            )
        }
    }
}

private class OnboardingTile(
    val label: String,
    val onClick: () -> Unit,
    val mark: @Composable () -> Unit,
)

/** Equal tiles across one panel: a mark above a short label, split by thin vertical rules. */
@Composable
private fun OnboardingTileRow(tiles: List<OnboardingTile>) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        tiles.forEachIndexed { index, tile ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(OmnilogTheme.colors.appLine),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = tile.onClick)
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.Center) { tile.mark() }
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = OmnilogTheme.colors.appInk,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The section's icon on a soft tint of its accent. */
@Composable
internal fun SectionMark(section: MediaSection, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .background(section.themedAccent().copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(section.navIconResId),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = section.themedAccent(),
        )
    }
}

private fun ExternalRatingSource.onboardingName(): String = when (this) {
    ExternalRatingSource.Imdb -> "IMDb"
    ExternalRatingSource.StoryGraph -> "StoryGraph"
    ExternalRatingSource.Mal -> "MyAnimeList"
    else -> name
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

private fun TrackedMedia.latestActivityMillis(): Long =
    sessions.maxOfOrNull { it.updatedAtEpochMillis } ?: 0L
