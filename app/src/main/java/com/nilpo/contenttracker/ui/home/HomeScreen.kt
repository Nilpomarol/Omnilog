package com.nilpo.contenttracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    metadataUiState: MetadataSearchUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
    onAuthorClick: (String) -> Unit,
    onManualAddClick: () -> Unit,
    onImportRequested: (() -> Unit)?,
    onSearchQueryChange: (String) -> Unit,
    onMetadataQueryChange: (String) -> Unit,
    onMetadataSearch: () -> Unit,
    onMetadataSearchSubmitted: () -> Unit,
    onApiSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onBrowseModeChange: (HomeBrowseMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onAdvancedFiltersChange: (HomeAdvancedFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    var filtersExpanded by remember { mutableStateOf(false) }
    val groupMode = uiState.browseMode.groupMode()
    val groupedItems = remember(uiState.trackedItems, uiState.browseMode, uiState.sortMode, uiState.sortDirection) {
        buildHomeGroups(uiState.trackedItems, groupMode, uiState.sortMode, uiState.sortDirection)
    }
    val collapsedGroupKeysState = remember(section, uiState.browseMode, groupedItems.map { it.key }) {
        mutableStateOf(
            if (uiState.browseMode != HomeBrowseMode.Items) {
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
        if (uiState.searchQuery.trim().length >= 3) {
            delay(300)
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
                        onSearchSubmitted = onMetadataSearchSubmitted,
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
                        section = section,
                        statusFilter = uiState.statusFilter,
                        browseMode = uiState.browseMode,
                        sortMode = uiState.sortMode,
                        sortDirection = uiState.sortDirection,
                        advancedFilters = uiState.advancedFilters,
                        accent = section.accent,
                        onStatusFilterChange = onStatusFilterChange,
                        onBrowseModeChange = onBrowseModeChange,
                        onSortModeChange = onSortModeChange,
                        onSortDirectionChange = onSortDirectionChange,
                        onAdvancedFiltersClick = { filtersExpanded = true },
                    )
                    if (filtersExpanded) {
                        AdvancedFiltersSheet(
                            currentFilters = uiState.advancedFilters,
                            section = section,
                            availableItems = uiState.allTrackedItems.filter { it.item.type in section.types },
                            accent = section.accent,
                            onDismiss = { filtersExpanded = false },
                            onApply = {
                                onAdvancedFiltersChange(it)
                                filtersExpanded = false
                            },
                        )
                    }
                    ListItemCounter(
                        visibleCount = uiState.trackedItems.size,
                        totalCount = sectionItemCount,
                    )
                }
            }

            if (uiState.trackedItems.isEmpty() && uiState.searchQuery.isBlank()) {
                item {
                    if (sectionItemCount == 0) {
                        OmnilogEmptyState(
                            title = stringResource(section.emptyTitleResId),
                            body = stringResource(section.emptyMessageResId),
                            accent = section.accent,
                            iconResId = section.navIconResId,
                            primaryAction = EmptyStateAction(
                                label = stringResource(section.addActionResId),
                                onClick = onManualAddClick,
                            ),
                            secondaryAction = section.importActionResId?.let { importLabelResId ->
                                onImportRequested?.let { onImport ->
                                    EmptyStateAction(
                                        label = stringResource(importLabelResId),
                                        onClick = onImport,
                                    )
                                }
                            },
                        )
                    } else {
                        OmnilogEmptyState(
                            title = stringResource(R.string.empty_filtered_title),
                            body = pluralStringResource(
                                R.plurals.empty_filtered_body,
                                sectionItemCount,
                                sectionItemCount,
                                stringResource(section.titleResId),
                            ),
                            accent = section.accent,
                            primaryAction = EmptyStateAction(
                                label = stringResource(R.string.empty_filtered_clear),
                                onClick = {
                                    onStatusFilterChange(null)
                                    onAdvancedFiltersChange(HomeAdvancedFilters())
                                },
                            ),
                        )
                    }
                }
            } else if (uiState.browseMode != HomeBrowseMode.Items) {
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
                                section = section,
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
                                onAuthorClick = onAuthorClick,
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
                item(key = "external_section_header") {
                    SearchSectionHeader(
                        title = stringResource(R.string.search_section_external),
                        count = if (metadataUiState.isLoading) null else apiResults.size,
                        hint = stringResource(R.string.search_section_external_hint),
                    )
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
                    metadataUiState.hasSearched && apiResults.isEmpty() -> item {
                        SearchStatePanel(text = stringResource(R.string.metadata_search_empty))
                    }
                    else -> items(apiResults) { suggestion ->
                        val suggestionAccent = suggestion.mediaType.sectionAccent()
                        MetadataSuggestionRow(
                            suggestion = suggestion,
                            accent = suggestionAccent,
                            duplicateState = duplicateStateForSuggestion(suggestion),
                            borderColor = suggestionAccent.copy(alpha = 0.55f),
                            onClick = { onApiSuggestionSelected(suggestion) },
                        )
                    }
                }

                if (metadataUiState.hasPartialError) {
                    item {
                        SearchStatePanel(
                            text = stringResource(R.string.metadata_search_partial_error),
                            color = OmnilogColors.AppMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int?,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OmnilogColors.AppInk,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogColors.AppMuted,
            )
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
    section: MediaSection,
    statusFilter: TrackingStatus?,
    browseMode: HomeBrowseMode,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    advancedFilters: HomeAdvancedFilters,
    accent: Color,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onBrowseModeChange: (HomeBrowseMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onAdvancedFiltersClick: () -> Unit,
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupModeSegmented(
            section = section,
            selectedMode = browseMode,
            accent = accent,
            onModeSelected = onBrowseModeChange,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                modifier = Modifier.fillMaxWidth(),
                label = statusFilter?.label() ?: stringResource(R.string.filter_all_statuses),
                selected = statusFilter != null,
                color = statusFilter?.stateColor ?: accent,
                onClick = { statusExpanded = true },
            )
            OmnilogDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false },
            ) {
                OmnilogDropdownItem(
                    text = stringResource(R.string.filter_all_statuses),
                    selected = statusFilter == null,
                    accent = accent,
                    onClick = { onStatusFilterChange(null); statusExpanded = false },
                )
                TrackingStatus.entries.forEach { status ->
                    OmnilogDropdownItem(
                        text = status.label(),
                        selected = statusFilter == status,
                        accent = status.stateColor,
                        labelColor = status.stateColor,
                        onClick = { onStatusFilterChange(status); statusExpanded = false },
                    )
                }
            }
        }

        Box {
            SortChip(
                sortMode = sortMode,
                sortDirection = sortDirection,
                accent = accent,
                onOpenMenu = { sortExpanded = true },
                onToggleDirection = {
                    onSortDirectionChange(
                        if (sortDirection == HomeSortDirection.Ascending) {
                            HomeSortDirection.Descending
                        } else {
                            HomeSortDirection.Ascending
                        },
                    )
                },
            )
            OmnilogDropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false },
            ) {
                HomeSortMode.entries.forEach { mode ->
                    OmnilogDropdownItem(
                        text = mode.label(),
                        selected = sortMode == mode,
                        accent = accent,
                        onClick = { onSortModeChange(mode); sortExpanded = false },
                    )
                }
            }
        }

        AdvancedFiltersButton(
            activeCount = advancedFilters.activeCount,
            color = accent,
            onClick = onAdvancedFiltersClick,
        )
        }
    }
}

@Composable
private fun GroupModeSegmented(
    section: MediaSection,
    selectedMode: HomeBrowseMode,
    accent: Color,
    onModeSelected: (HomeBrowseMode) -> Unit,
) {
    val modes = HomeBrowseMode.entries
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    val segmentHeight = 30.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
        ) {
            val segmentWidth = maxWidth / modes.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = tween(durationMillis = 220),
                label = "segmentIndicatorOffset",
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .height(segmentHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.20f)),
            )
            Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                modes.forEach { mode ->
                    val selected = mode == selectedMode
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) OmnilogColors.AppInk else OmnilogColors.AppMuted,
                        animationSpec = tween(durationMillis = 220),
                        label = "segmentContentColor",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(segmentHeight)
                            .clip(RoundedCornerShape(999.dp))
                            .selectable(
                                selected = selected,
                                onClick = { onModeSelected(mode) },
                                role = Role.Tab,
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(mode.iconRes()),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = mode.label(section),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor,
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

@Composable
private fun SortChip(
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    accent: Color,
    onOpenMenu: () -> Unit,
    onToggleDirection: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onOpenMenu() }
                    .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = sortMode.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 18.dp)
                    .background(accent.copy(alpha = 0.35f)),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onToggleDirection() }
                    .padding(horizontal = 9.dp, vertical = 7.dp),
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
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun HomeBrowseMode.iconRes(): Int = when (this) {
    HomeBrowseMode.Items -> R.drawable.ic_group_items
    HomeBrowseMode.Collections -> R.drawable.ic_group_collections
    HomeBrowseMode.Authors -> R.drawable.ic_group_authors
}
@Composable
private fun AdvancedFiltersButton(
    activeCount: Int,
    color: Color,
    onClick: () -> Unit,
) {
    Box {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(999.dp),
            color = if (activeCount > 0) color.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
            border = BorderStroke(1.dp, if (activeCount > 0) color.copy(alpha = 0.50f) else OmnilogColors.AppLine),
            contentColor = if (activeCount > 0) OmnilogColors.AppInk else OmnilogColors.AppMuted,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = stringResource(R.string.filter_more),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        if (activeCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp),
                shape = RoundedCornerShape(999.dp),
                color = color,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFiltersSheet(
    currentFilters: HomeAdvancedFilters,
    section: MediaSection,
    availableItems: List<TrackedMedia>,
    accent: Color,
    onDismiss: () -> Unit,
    onApply: (HomeAdvancedFilters) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(currentFilters) { mutableStateOf(currentFilters) }
    var authorQuery by remember { mutableStateOf("") }
    var genreQuery by remember { mutableStateOf("") }
    var authorsExpanded by remember(currentFilters) { mutableStateOf(currentFilters.authors.isNotEmpty()) }
    var genresExpanded by remember(currentFilters) { mutableStateOf(currentFilters.genres.isNotEmpty()) }
    val authors = remember(availableItems) {
        availableItems.flatMap { it.item.creators }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }
    val genres = remember(availableItems) {
        availableItems.flatMap { it.item.genres }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }
    val visibleAuthors = authors.filter { it.contains(authorQuery, ignoreCase = true) }
    val visibleGenres = genres.filter { it.contains(genreQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogColors.AppPanel,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            val sheetScrollIsolation = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = Offset(x = 0f, y = available.y)

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity = Velocity(x = 0f, y = available.y)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.82f)
                    .nestedScroll(sheetScrollIsolation)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.filter_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = { draft = HomeAdvancedFilters() },
                        enabled = draft.isActive,
                    ) {
                        Text(stringResource(R.string.filter_clear_all))
                    }
                }

                SearchableFilterDropdown(
                    title = stringResource(section.creatorFilterLabelResId),
                    query = authorQuery,
                    onQueryChange = { authorQuery = it },
                    expanded = authorsExpanded,
                    onExpandedChange = { authorsExpanded = it },
                    options = visibleAuthors,
                    selectedOptions = draft.authors,
                    accent = accent,
                    onOptionToggle = { author ->
                        draft = draft.copy(authors = draft.authors.toggle(author))
                    },
                )

                SearchableFilterDropdown(
                    title = stringResource(R.string.filter_genres),
                    query = genreQuery,
                    onQueryChange = { genreQuery = it },
                    expanded = genresExpanded,
                    onExpandedChange = { genresExpanded = it },
                    options = visibleGenres,
                    selectedOptions = draft.genres,
                    accent = accent,
                    onOptionToggle = { genre ->
                        draft = draft.copy(genres = draft.genres.toggle(genre))
                    },
                )

                RatingFilterSection(
                    title = stringResource(R.string.filter_external_rating),
                    selectedMinimum = draft.minimumExternalRating,
                    accent = accent,
                    onSelected = { draft = draft.copy(minimumExternalRating = it) },
                )
                RatingFilterSection(
                    title = stringResource(R.string.filter_my_rating),
                    selectedMinimum = draft.minimumUserRating,
                    accent = accent,
                    onSelected = { draft = draft.copy(minimumUserRating = it) },
                )

                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.filter_apply))
                }
            }
        }
    }
}

@Composable
private fun SearchableFilterDropdown(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    selectedOptions: Set<String>,
    accent: Color,
    onOptionToggle: (String) -> Unit,
) {
    val dropdownScrollIsolation = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(x = 0f, y = available.y)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                if (!expanded) onExpandedChange(true)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(title) },
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }
            },
        )
        if (expanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = OmnilogColors.AppPanel.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .nestedScroll(dropdownScrollIsolation)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    options.forEach { option ->
                        FilterChoiceRow(
                            label = option,
                            selected = option in selectedOptions,
                            accent = accent,
                            onClick = { onOptionToggle(option) },
                        )
                    }
                    if (options.isEmpty()) {
                        Text(
                            text = stringResource(R.string.filter_no_matches),
                            modifier = Modifier.padding(12.dp),
                            color = OmnilogColors.AppMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    selectedCount: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            if (selectedCount > 0) {
                Text(
                    text = stringResource(R.string.filter_selected_count, selectedCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FilterChoiceRow(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
            Text(
                text = label,
                color = if (selected) accent else OmnilogColors.AppInk,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingFilterSection(
    title: String,
    selectedMinimum: Int?,
    accent: Color,
    onSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedMinimum == null,
                onClick = { onSelected(null) },
                label = { Text(stringResource(R.string.filter_any_rating)) },
            )
            (5..9).forEach { minimum ->
                FilterChip(
                    selected = selectedMinimum == minimum,
                    onClick = { onSelected(minimum) },
                    label = { Text(stringResource(R.string.filter_rating_at_least, minimum)) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.20f),
                        selectedLabelColor = OmnilogColors.AppInk,
                    ),
                )
            }
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

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
                .padding(horizontal = 12.dp, vertical = 7.dp),
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

private fun HomeBrowseMode.groupMode(): HomeGroupMode = when (this) {
    HomeBrowseMode.Items -> HomeGroupMode.None
    HomeBrowseMode.Collections -> HomeGroupMode.Collection
    HomeBrowseMode.Authors -> HomeGroupMode.Author
}

@Composable
private fun HomeBrowseMode.label(section: MediaSection): String = when (this) {
    HomeBrowseMode.Items -> stringResource(R.string.browse_items)
    HomeBrowseMode.Collections -> stringResource(R.string.browse_collections)
    HomeBrowseMode.Authors -> stringResource(section.creatorBrowseLabelResId)
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
