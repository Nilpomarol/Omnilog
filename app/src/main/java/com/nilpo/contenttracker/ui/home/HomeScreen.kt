package com.nilpo.contenttracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.add.DashboardStyleSearchBar
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.add.SearchStatePanel
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    metadataUiState: MetadataSearchUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
    onManualAddClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMetadataQueryChange: (String) -> Unit,
    onMetadataSearch: () -> Unit,
    onApiSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onGroupModeChange: (HomeGroupMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    val groupedItems = remember(uiState.trackedItems, uiState.groupMode, uiState.sortMode, uiState.sortDirection) {
        buildHomeGroups(uiState.trackedItems, uiState.groupMode, uiState.sortMode, uiState.sortDirection)
    }
    val collapsedGroupKeysState = remember(section, uiState.groupMode, groupedItems.map { it.key }) {
        mutableStateOf(
            if (uiState.groupMode == HomeGroupMode.Collection || uiState.groupMode == HomeGroupMode.Author) {
                groupedItems.map { it.key }
            } else {
                emptyList()
            },
        )
    }
    val collapsedGroupKeys = collapsedGroupKeysState.value
    val apiResults = metadataUiState.suggestions.filter { suggestion ->
        duplicateStateForSuggestion(suggestion) != MetadataDuplicateState.Exact
    }
    val showApiSection = metadataUiState.isLoading || metadataUiState.hasSearched
    val sectionItemCount = uiState.allTrackedItems.count { trackedMedia ->
        trackedMedia.item.type in section.types
    }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery.trim().length >= 2) {
            delay(450)
            onMetadataSearch()
        } else if (uiState.searchQuery.isBlank()) {
            onMetadataSearch()
        }
    }

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardStyleSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { query ->
                            onSearchQueryChange(query)
                            onMetadataQueryChange(query)
                        },
                        isLoading = metadataUiState.isLoading,
                        accent = section.accent,
                    ) {
                        IconButton(
                            onClick = onManualAddClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_item),
                                tint = section.accent,
                            )
                        }
                    }
                    BrowseControls(
                        statusFilter = uiState.statusFilter,
                        groupMode = uiState.groupMode,
                        sortMode = uiState.sortMode,
                        sortDirection = uiState.sortDirection,
                        accent = section.accent,
                        onStatusFilterChange = onStatusFilterChange,
                        onGroupModeChange = onGroupModeChange,
                        onSortModeChange = onSortModeChange,
                        onSortDirectionChange = onSortDirectionChange,
                    )
                    ListItemCounter(
                        visibleCount = uiState.trackedItems.size,
                        totalCount = sectionItemCount,
                    )
                }
            }

            if (uiState.trackedItems.isEmpty() && uiState.searchQuery.isBlank()) {
                item {
                    Text(
                        text = stringResource(section.emptyMessageResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OmnilogColors.AppMuted,
                    )
                }
            } else if (uiState.groupMode != HomeGroupMode.None) {
                groupedItems.forEach { group ->
                    val isCollapsed = group.key in collapsedGroupKeys
                    val groupItems = if (group.type == HomeGroupType.Collection) {
                        group.items.sortedWith(
                            compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
                                .thenBy { it.item.title.lowercase() },
                        )
                    } else {
                        group.items
                    }
                    item(key = group.key) {
                        Column {
                            HomeGroupHeader(
                                group = group,
                                accent = section.accent,
                                isCollapsed = isCollapsed,
                                onClick = {
                                    collapsedGroupKeysState.value = if (isCollapsed) {
                                        collapsedGroupKeys - group.key
                                    } else {
                                        collapsedGroupKeys + group.key
                                    }
                                },
                                onCollectionClick = onCollectionClick,
                            )
                            AnimatedVisibility(
                                visible = !isCollapsed,
                                enter = expandVertically(tween(300, delayMillis = 70), expandFrom = Alignment.Top) +
                                    fadeIn(tween(180, delayMillis = 120)),
                                exit = shrinkVertically(tween(210), shrinkTowards = Alignment.Top) +
                                    fadeOut(tween(110)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    groupItems.forEach { trackedMedia ->
                                        MediaCard(
                                            trackedMedia = trackedMedia,
                                            accent = section.accent,
                                            onClick = { onMediaClick(trackedMedia) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.trackedItems) { trackedMedia ->
                    MediaCard(
                        trackedMedia = trackedMedia,
                        accent = section.accent,
                        onClick = { onMediaClick(trackedMedia) },
                    )
                }
            }

            if (showApiSection) {
                if (uiState.trackedItems.isNotEmpty() && (apiResults.isNotEmpty() || metadataUiState.isLoading)) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = OmnilogColors.AppLine.copy(alpha = 0.60f),
                        )
                    }
                }

                when {
                    metadataUiState.isLoading -> item {
                        SearchStatePanel(text = stringResource(R.string.metadata_search_loading))
                    }
                    metadataUiState.hasError -> item {
                        SearchStatePanel(
                            text = stringResource(R.string.metadata_search_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    metadataUiState.hasSearched && apiResults.isEmpty() && uiState.trackedItems.isEmpty() -> item {
                        SearchStatePanel(text = stringResource(R.string.metadata_search_empty))
                    }
                    else -> items(apiResults) { suggestion ->
                        MetadataSuggestionRow(
                            suggestion = suggestion,
                            accent = suggestion.mediaType.sectionAccent(),
                            duplicateState = duplicateStateForSuggestion(suggestion),
                            onClick = { onApiSuggestionSelected(suggestion) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItemCounter(
    visibleCount: Int,
    totalCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.list_item_count, visibleCount, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = OmnilogColors.AppMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun BrowseControls(
    statusFilter: TrackingStatus?,
    groupMode: HomeGroupMode,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    accent: Color,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onGroupModeChange: (HomeGroupMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status
        Box(modifier = Modifier.weight(1.3f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = statusFilter?.label() ?: stringResource(R.string.filter_all_statuses),
                selected = statusFilter != null,
                color = statusFilter?.stateColor ?: accent,
                onClick = { statusExpanded = true },
            )
            DropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter_all_statuses)) },
                    onClick = { onStatusFilterChange(null); statusExpanded = false },
                )
                TrackingStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = status.label(),
                                color = status.stateColor,
                                fontWeight = if (statusFilter == status) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = { onStatusFilterChange(status); statusExpanded = false },
                    )
                }
            }
        }

        // Group
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = groupMode.label(),
                selected = groupMode != HomeGroupMode.None,
                color = accent,
                onClick = { groupExpanded = true },
            )
            DropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false },
            ) {
                HomeGroupMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode.label(),
                                fontWeight = if (groupMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = { onGroupModeChange(mode); groupExpanded = false },
                    )
                }
            }
        }

        // Sort
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = sortMode.label(),
                selected = true,
                color = accent,
                onClick = { sortExpanded = true },
            )
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false },
            ) {
                HomeSortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode.label(),
                                fontWeight = if (sortMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = { onSortModeChange(mode); sortExpanded = false },
                    )
                }
            }
        }

        // Direction
        Surface(
            onClick = {
                onSortDirectionChange(
                    if (sortDirection == HomeSortDirection.Ascending) {
                        HomeSortDirection.Descending
                    } else {
                        HomeSortDirection.Ascending
                    },
                )
            },
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(999.dp),
            color = accent.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
            contentColor = accent,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (sortDirection == HomeSortDirection.Ascending) {
                            R.drawable.ic_arrow_up
                        } else {
                            R.drawable.ic_arrow_down
                        },
                    ),
                    contentDescription = stringResource(R.string.sort_direction_label),
                    modifier = Modifier.size(18.dp),
                )
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
        contentColor = if (selected) color else OmnilogColors.AppMuted,
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
private fun HomeGroupMode.label(): String {
    return when (this) {
        HomeGroupMode.None -> stringResource(R.string.group_none)
        HomeGroupMode.Status -> stringResource(R.string.group_status)
        HomeGroupMode.Collection -> stringResource(R.string.group_collection)
        HomeGroupMode.Author -> stringResource(R.string.group_author)
    }
}

@Composable
private fun HomeSortMode.label(): String {
    return when (this) {
        HomeSortMode.Title -> stringResource(R.string.sort_title)
        HomeSortMode.Progress -> stringResource(R.string.sort_progress)
        HomeSortMode.Rating -> stringResource(R.string.sort_rating)
        HomeSortMode.Recent -> stringResource(R.string.sort_recent)
    }
}

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

private fun MediaType.sectionAccent() = when (this) {
    MediaType.Anime -> MediaSection.Anime.accent
    MediaType.Book -> MediaSection.Books.accent
    MediaType.Movie, MediaType.TvShow -> MediaSection.Movies.accent
    MediaType.Game -> MediaSection.Games.accent
}
