package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val groupedItems = remember(uiState.trackedItems, uiState.groupMode) {
        buildHomeGroups(uiState.trackedItems, uiState.groupMode)
    }
    val collapsedGroupKeysState = remember(section, uiState.groupMode, groupedItems.map { it.key }) {
        mutableStateOf(
            if (uiState.groupMode == HomeGroupMode.Collection) {
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
                    groupMode = uiState.groupMode,
                    sortMode = uiState.sortMode,
                    sortDirection = uiState.sortDirection,
                    accent = section.accent,
                    onStatusFilterChange = onStatusFilterChange,
                    onGroupModeChange = onGroupModeChange,
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
                                enter = expandVertically(tween(300), expandFrom = Alignment.Top) +
                                    fadeIn(tween(220, delayMillis = 80)),
                                exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) +
                                    fadeOut(tween(150)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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

        Box {
            DropdownChip(
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

@Composable
private fun HomeSortDirection.label(): String {
    return when (this) {
        HomeSortDirection.Ascending -> stringResource(R.string.sort_direction_ascending)
        HomeSortDirection.Descending -> stringResource(R.string.sort_direction_descending)
    }
}

@Composable
private fun HomeGroupHeader(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    when (group.type) {
        HomeGroupType.Collection -> CollectionGroupCard(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
            onCollectionClick = onCollectionClick,
        )
        HomeGroupType.Status,
        HomeGroupType.Author,
        -> SimpleGroupHeader(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
        )
    }
}

@Composable
private fun SimpleGroupHeader(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (group.status != null) {
                Icon(
                    painter = painterResource(group.status.iconResId),
                    contentDescription = group.status.label(),
                    modifier = Modifier.size(18.dp),
                    tint = group.status.stateColor,
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.resolvedTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.collection_item_count, group.items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
        }
    }
}

@Composable
private fun CollectionGroupCard(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    AnimatedContent(
        targetState = isCollapsed,
        transitionSpec = {
            if (targetState) {
                // Collapsing: compact header → full card. Items are shrinking (220ms), grow card after.
                (fadeIn(tween(240, delayMillis = 180)) + expandVertically(tween(320, delayMillis = 180), expandFrom = Alignment.Top)) togetherWith
                    fadeOut(tween(130))
            } else {
                // Expanding: full card → compact header. Shrink card fast, items will grow below.
                fadeIn(tween(160, delayMillis = 60)) togetherWith
                    (fadeOut(tween(140)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top))
            }
        },
        label = "collection_card",
    ) { collapsed ->
        if (!collapsed) {
            CollectionGroupCompactHeader(group, accent, onClick, onCollectionClick)
        } else {
            CollectionGroupFullCard(group, accent, onClick, onCollectionClick)
        }
    }
}

@Composable
private fun CollectionGroupFullCard(
    group: HomeDisplayGroup,
    accent: Color,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    val summary = group.items.collectionProgressSummary()
    val mediaType = group.items.firstOrNull()?.item?.type
    val lastUpdatedMillis = group.items.collectionLastUpdatedMillis()
    val unitLabel = mediaType.collectionItemUnitLabel()
    val completedStr = stringResource(R.string.group_completed_count, summary.completedCount)
    val inProgressStr = stringResource(R.string.group_in_progress_count, summary.inProgressCount)
    val metaLine = "${group.items.size} $unitLabel · $completedStr · $inProgressStr"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetadataCoverImage(
                coverUrl = group.items.collectionCoverUrl(),
                modifier = Modifier
                    .size(width = 100.dp, height = 150.dp)
                    .clip(RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .clipToBounds(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = group.resolvedTitle(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = accent,
                        )
                        if (group.collection != null) {
                            Text(
                                text = stringResource(R.string.edit),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                                modifier = Modifier.clickable { onCollectionClick(group.collection) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.group_progress_prefix, summary.label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    GroupProgressBar(
                        fraction = summary.progressFraction,
                        color = accent,
                    )
                    if (lastUpdatedMillis != null) {
                        val date = Instant.ofEpochMilli(lastUpdatedMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                        Text(
                            text = stringResource(R.string.session_updated_at, date),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionGroupCompactHeader(
    group: HomeDisplayGroup,
    accent: Color,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.resolvedTitle(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.collection_item_count, group.items.size),
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogColors.AppMuted,
                maxLines = 1,
            )
            if (group.collection != null) {
                Text(
                    text = stringResource(R.string.edit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.clickable { onCollectionClick(group.collection) },
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
        }
    }
}

@Composable
private fun GroupProgressBar(
    fraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogColors.AppLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

private enum class HomeGroupType {
    Status,
    Collection,
    Author,
}

private data class HomeDisplayGroup(
    val key: String,
    val type: HomeGroupType,
    val title: String,
    val items: List<TrackedMedia>,
    val collection: MediaCollection? = null,
    val status: TrackingStatus? = null,
)

private data class CollectionProgressSummary(
    val label: String,
    val completedCount: Int,
    val inProgressCount: Int,
    val progressFraction: Float,
)

@Composable
private fun List<TrackedMedia>.collectionProgressSummary(): CollectionProgressSummary {
    val completed = count { it.currentSession?.status == TrackingStatus.Completed }
    val inProgress = count { it.currentSession?.status == TrackingStatus.InProgress }
    val mediaType = firstOrNull()?.item?.type
    var fraction = if (isNotEmpty()) completed.toFloat() / size.toFloat() else 0f
    val label = if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
        val current = sumOf { it.currentSession?.progressCurrent ?: 0 }
        val total = sumOf { it.item.progressTotal ?: 0 }
        if (total > 0) {
            fraction = current.toFloat() / total.toFloat()
            stringResource(R.string.group_progress_units, current, total, stringResource(R.string.progress_unit_episode_many))
        } else {
            stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
        }
    } else {
        stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
    }
    return CollectionProgressSummary(
        label = label,
        completedCount = completed,
        inProgressCount = inProgress,
        progressFraction = fraction,
    )
}

@Composable
private fun MediaType?.itemUnitLabel(): String {
    return when (this) {
        MediaType.Book -> stringResource(R.string.group_unit_books)
        MediaType.Game -> stringResource(R.string.group_unit_games)
        MediaType.Movie -> stringResource(R.string.group_unit_movies)
        MediaType.TvShow -> stringResource(R.string.group_unit_tv_shows)
        MediaType.Anime -> stringResource(R.string.group_unit_anime)
        null -> stringResource(R.string.group_unit_items)
    }
}

@Composable
private fun MediaType?.collectionItemUnitLabel(): String {
    return when (this) {
        MediaType.Anime,
        MediaType.TvShow,
        -> stringResource(R.string.group_unit_seasons)
        MediaType.Book -> stringResource(R.string.group_unit_books)
        MediaType.Movie -> stringResource(R.string.group_unit_movies)
        MediaType.Game -> stringResource(R.string.group_unit_games)
        null -> stringResource(R.string.group_unit_items)
    }
}

private fun buildHomeGroups(
    items: List<TrackedMedia>,
    groupMode: HomeGroupMode,
): List<HomeDisplayGroup> {
    return when (groupMode) {
        HomeGroupMode.None -> emptyList()
        HomeGroupMode.Status -> TrackingStatus.entries.mapNotNull { status ->
            val groupItems = items.filter { it.currentSession?.status == status }
            if (groupItems.isEmpty()) {
                null
            } else {
                HomeDisplayGroup(
                    key = "status:${status.name}",
                    type = HomeGroupType.Status,
                    title = status.name,
                    items = groupItems,
                    status = status,
                )
            }
        }
        HomeGroupMode.Collection -> items
            .groupBy { it.collection }
            .toList()
            .sortedWith(
                compareBy<Pair<MediaCollection?, List<TrackedMedia>>> { it.first == null }
                    .thenBy { it.first?.name?.lowercase().orEmpty() },
            )
            .map { (collection, groupItems) ->
                HomeDisplayGroup(
                    key = collection?.let { "collection:${it.id}" } ?: "collection:none",
                    type = HomeGroupType.Collection,
                    title = collection?.name ?: "",
                    items = groupItems,
                    collection = collection,
                )
            }
        HomeGroupMode.Author -> items
            .groupBy { it.item.creators.firstOrNull()?.trim().orEmpty() }
            .toList()
            .sortedBy { it.first.lowercase() }
            .map { (author, groupItems) ->
                HomeDisplayGroup(
                    key = "author:${author.lowercase()}",
                    type = HomeGroupType.Author,
                    title = author,
                    items = groupItems,
                )
            }
    }
}

@Composable
private fun HomeDisplayGroup.resolvedTitle(): String {
    return when {
        status != null -> status.label()
        title.isNotBlank() -> title
        type == HomeGroupType.Collection -> stringResource(R.string.collection_none)
        type == HomeGroupType.Author -> stringResource(R.string.group_author_unknown)
        else -> title
    }
}

private fun List<TrackedMedia>.collectionLastUpdatedMillis(): Long? =
    mapNotNull { it.currentSession?.updatedAtEpochMillis?.takeIf { ms -> ms > 0 } }.maxOrNull()

private fun List<TrackedMedia>.collectionCoverUrl(): String? {
    return sortedWith(
        compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
            .thenBy { it.item.title.lowercase() },
    ).firstOrNull { it.item.coverUrl != null }?.item?.coverUrl
}

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

private fun MediaType.sectionAccent() = when (this) {
    MediaType.Anime -> MediaSection.Anime.accent
    MediaType.Book -> MediaSection.Books.accent
    MediaType.Movie, MediaType.TvShow -> MediaSection.Movies.accent
    MediaType.Game -> MediaSection.Games.accent
}
