package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.add.DashboardStyleSearchBar
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
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
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    val groupedItems = uiState.trackedItems.groupBy { it.collection }
    val showCollectionGroups = uiState.sortMode == HomeSortMode.Collection && groupedItems.isNotEmpty()
    val apiResults = metadataUiState.suggestions.filter { suggestion ->
        duplicateStateForSuggestion(suggestion) != MetadataDuplicateState.Exact
    }
    val showApiSection = metadataUiState.isLoading || metadataUiState.hasSearched

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DashboardStyleSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query ->
                        onSearchQueryChange(query)
                        onMetadataQueryChange(query)
                    },
                    isLoading = metadataUiState.isLoading,
                    accent = section.accent,
                    onSearch = onMetadataSearch,
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
            }

            item {
                BrowseControls(
                    statusFilter = uiState.statusFilter,
                    sortMode = uiState.sortMode,
                    sortDirection = uiState.sortDirection,
                    accent = section.accent,
                    onStatusFilterChange = onStatusFilterChange,
                    onSortModeChange = onSortModeChange,
                    onSortDirectionChange = onSortDirectionChange,
                )
            }

            if (uiState.trackedItems.isEmpty() && uiState.searchQuery.isBlank()) {
                item {
                    Text(
                        text = stringResource(section.emptyMessageResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OmnilogColors.AppMuted,
                    )
                }
            } else if (showCollectionGroups) {
                groupedItems.forEach { (collection, items) ->
                    item {
                        CollectionHeader(
                            title = collection?.name ?: stringResource(R.string.collection_none),
                            count = items.size,
                            onClick = collection?.let { { onCollectionClick(it) } },
                        )
                    }
                    items(items) { trackedMedia ->
                        MediaCard(
                            trackedMedia = trackedMedia,
                            accent = section.accent,
                            onClick = { onMediaClick(trackedMedia) },
                        )
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
private fun BrowseControls(
    statusFilter: TrackingStatus?,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    accent: Color,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dropdown
        Box {
            DropdownChip(
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

        // Sort dropdown
        Box {
            DropdownChip(
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

        // Direction toggle
        DirectionToggle(
            direction = sortDirection,
            accent = accent,
            onClick = {
                onSortDirectionChange(
                    if (sortDirection == HomeSortDirection.Ascending) {
                        HomeSortDirection.Descending
                    } else {
                        HomeSortDirection.Ascending
                    },
                )
            },
        )
    }
}

@Composable
private fun DropdownChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        color = if (selected) color.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.50f) else OmnilogColors.AppLine),
        contentColor = if (selected) color else OmnilogColors.AppMuted,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun DirectionToggle(
    direction: HomeSortDirection,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
        contentColor = accent,
    ) {
        Icon(
            imageVector = if (direction == HomeSortDirection.Ascending) {
                Icons.Filled.KeyboardArrowUp
            } else {
                Icons.Filled.KeyboardArrowDown
            },
            contentDescription = stringResource(R.string.sort_direction_label),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .size(14.dp),
        )
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
private fun HomeSortMode.label(): String {
    return when (this) {
        HomeSortMode.Title -> stringResource(R.string.sort_title)
        HomeSortMode.Collection -> stringResource(R.string.sort_collection)
        HomeSortMode.Progress -> stringResource(R.string.sort_progress)
        HomeSortMode.Rating -> stringResource(R.string.sort_rating)
        HomeSortMode.Recent -> stringResource(R.string.sort_recent)
    }
}

@Composable
private fun HomeSortDirection.label(): String {
    return when (this) {
        HomeSortDirection.Ascending -> stringResource(R.string.sort_direction_ascending)
        HomeSortDirection.Descending -> stringResource(R.string.sort_direction_descending)
    }
}

@Composable
private fun CollectionHeader(
    title: String,
    count: Int,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.collection_item_count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
        )
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
