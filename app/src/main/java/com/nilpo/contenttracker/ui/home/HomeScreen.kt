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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.add.DashboardStyleSearchBar
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.partialSearchFailureMessage
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
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
    onDisplayModeChange: (HomeDisplayMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onAdvancedFiltersChange: (HomeAdvancedFilters) -> Unit,
    searchExpanded: Boolean,
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
    val sectionItems = remember(uiState.allTrackedItems, section) {
        uiState.allTrackedItems.filter { it.item.type in section.types }
    }
    val sectionItemCount = sectionItems.size

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
        color = OmnilogTheme.colors.appBackground,
    ) {
        LazyVerticalGrid(
            columns = if (
                uiState.displayMode == HomeDisplayMode.Grid &&
                uiState.browseMode == HomeBrowseMode.Items
            ) {
                GridCells.Fixed(3)
            } else {
                GridCells.Fixed(1)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedVisibility(visible = searchExpanded || uiState.searchQuery.isNotBlank()) {
                        DashboardStyleSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { query ->
                                onSearchQueryChange(query)
                                onMetadataQueryChange(query)
                            },
                            onSearchSubmitted = onMetadataSearchSubmitted,
                            isLoading = metadataUiState.isLoading,
                            accent = section.themedAccent(),
                            leadingIcon = Icons.Filled.Search,
                        ) {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSearchQueryChange("")
                                        onMetadataQueryChange("")
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear_search),
                                        tint = OmnilogTheme.colors.appMuted,
                                    )
                                }
                            }
                        }
                    }
                    BrowseControls(
                        section = section,
                        sectionItems = sectionItems,
                        visibleCount = uiState.trackedItems.size,
                        searchActive = uiState.searchQuery.isNotBlank(),
                        statusFilter = uiState.statusFilter,
                        browseMode = uiState.browseMode,
                        displayMode = uiState.displayMode,
                        sortMode = uiState.sortMode,
                        sortDirection = uiState.sortDirection,
                        advancedFilters = uiState.advancedFilters,
                        accent = section.themedAccent(),
                        onStatusFilterChange = onStatusFilterChange,
                        onBrowseModeChange = onBrowseModeChange,
                        onDisplayModeChange = onDisplayModeChange,
                        onSortModeChange = onSortModeChange,
                        onSortDirectionChange = onSortDirectionChange,
                        onAdvancedFiltersClick = { filtersExpanded = true },
                        onClearAdvancedFilters = { onAdvancedFiltersChange(HomeAdvancedFilters()) },
                    )
                    if (filtersExpanded) {
                        AdvancedFiltersSheet(
                            currentFilters = uiState.advancedFilters,
                            section = section,
                            availableItems = sectionItems,
                            accent = section.themedAccent(),
                            onDismiss = { filtersExpanded = false },
                            onApply = {
                                onAdvancedFiltersChange(it)
                                filtersExpanded = false
                            },
                        )
                    }
                }
            }

            if (uiState.trackedItems.isEmpty() && uiState.searchQuery.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (sectionItemCount == 0) {
                        OmnilogEmptyState(
                            title = stringResource(section.emptyTitleResId),
                            body = stringResource(section.emptyMessageResId),
                            accent = section.themedAccent(),
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
                            accent = section.themedAccent(),
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
                    item(key = group.key, span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            HomeGroupHeader(
                                group = group,
                                section = section,
                                accent = section.themedAccent(),
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
                                            accent = section.themedAccent(),
                                            onClick = { onMediaClick(trackedMedia) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.trackedItems, key = { it.item.id }) { trackedMedia ->
                    if (uiState.displayMode == HomeDisplayMode.Grid) {
                        MediaGridCard(
                            trackedMedia = trackedMedia,
                            onClick = { onMediaClick(trackedMedia) },
                        )
                    } else {
                        MediaCard(
                            trackedMedia = trackedMedia,
                            accent = section.themedAccent(),
                            onClick = { onMediaClick(trackedMedia) },
                        )
                    }
                }
            }

            if (showApiSection) {
                item(key = "external_section_header", span = { GridItemSpan(maxLineSpan) }) {
                    SearchSectionHeader(
                        title = stringResource(R.string.search_section_external),
                        count = if (metadataUiState.isLoading) null else apiResults.size,
                        hint = stringResource(R.string.search_section_external_hint),
                    )
                }

                when {
                    // The section search bar already spins while a query runs.
                    metadataUiState.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_search_loading),
                            accent = section.themedAccent(),
                        )
                    }
                    metadataUiState.hasError -> item(span = { GridItemSpan(maxLineSpan) }) {
                        OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_search_error),
                            accent = section.themedAccent(),
                            textColor = MaterialTheme.colorScheme.error,
                            action = EmptyStateAction(
                                label = stringResource(R.string.retry_action),
                                onClick = onMetadataSearchSubmitted,
                            ),
                        )
                    }
                    metadataUiState.hasSearched && apiResults.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_search_empty),
                            accent = section.themedAccent(),
                        )
                    }
                    else -> items(apiResults, span = { GridItemSpan(maxLineSpan) }) { suggestion ->
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
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OmnilogStatusPanel(
                            text = partialSearchFailureMessage(metadataUiState.failedSources.toList()),
                            accent = section.themedAccent(),
                            action = EmptyStateAction(
                                label = stringResource(R.string.retry_action),
                                onClick = onMetadataSearchSubmitted,
                            ),
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
                color = OmnilogTheme.colors.appInk,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/**
 * The library's controls, quietest first: status as typographic tabs carrying their own counts, then
 * one toolbar line for order, filters and layout. A summary appears only while search or filters hide
 * part of the chosen tab, so the tab's count never has to be second-guessed.
 */
@Composable
private fun BrowseControls(
    section: MediaSection,
    sectionItems: List<TrackedMedia>,
    visibleCount: Int,
    searchActive: Boolean,
    statusFilter: TrackingStatus?,
    browseMode: HomeBrowseMode,
    displayMode: HomeDisplayMode,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    advancedFilters: HomeAdvancedFilters,
    accent: Color,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onBrowseModeChange: (HomeBrowseMode) -> Unit,
    onDisplayModeChange: (HomeDisplayMode) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onAdvancedFiltersClick: () -> Unit,
    onClearAdvancedFilters: () -> Unit,
) {
    val statusCounts = remember(sectionItems) {
        sectionItems.groupingBy { it.currentSession?.status }.eachCount()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        StatusTabs(
            selectedStatus = statusFilter,
            counts = statusCounts,
            totalCount = sectionItems.size,
            accent = accent,
            onStatusSelected = onStatusFilterChange,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortMenu(
                section = section,
                sortMode = sortMode,
                sortDirection = sortDirection,
                browseMode = browseMode,
                accent = accent,
                onSortModeChange = onSortModeChange,
                onSortDirectionChange = onSortDirectionChange,
                onBrowseModeChange = onBrowseModeChange,
                modifier = Modifier.weight(1f),
            )
            AdvancedFiltersButton(
                activeCount = advancedFilters.activeCount,
                accent = accent,
                onClick = onAdvancedFiltersClick,
            )
            // Grouped lists are always rows, so the switch steps aside rather than sitting there disabled.
            AnimatedVisibility(visible = browseMode == HomeBrowseMode.Items) {
                DisplayModeToggle(
                    selectedMode = displayMode,
                    accent = accent,
                    onModeSelected = onDisplayModeChange,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        if (searchActive || advancedFilters.isActive) {
            NarrowedSummary(
                visibleCount = visibleCount,
                tabCount = statusFilter?.let { statusCounts[it] ?: 0 } ?: sectionItems.size,
                filters = advancedFilters,
                accent = accent,
                onClearFilters = onClearAdvancedFilters,
            )
        }
    }
}

private val StatusTabOrder = listOf(
    TrackingStatus.InProgress,
    TrackingStatus.Planned,
    TrackingStatus.Completed,
    TrackingStatus.Paused,
    TrackingStatus.Dropped,
)

/** Status as text tabs over a hairline: the chosen one is inked and underlined in its status colour. */
@Composable
private fun StatusTabs(
    selectedStatus: TrackingStatus?,
    counts: Map<TrackingStatus?, Int>,
    totalCount: Int,
    accent: Color,
    onStatusSelected: (TrackingStatus?) -> Unit,
) {
    val hairline = OmnilogTheme.colors.appLine
    // Statuses with nothing in them stay out of the way; a selected one stays so it can be left.
    val options = listOf<TrackingStatus?>(null) +
        StatusTabOrder.filter { (counts[it] ?: 0) > 0 || it == selectedStatus }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(hairline, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
            }
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        options.forEach { status ->
            val selected = status == selectedStatus
            val statusColor = status?.stateColor ?: accent
            val ink by animateColorAsState(
                targetValue = if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                animationSpec = tween(durationMillis = 220),
                label = "statusTabInk",
            )
            val rule by animateColorAsState(
                targetValue = if (selected) statusColor else statusColor.copy(alpha = 0f),
                animationSpec = tween(durationMillis = 220),
                label = "statusTabRule",
            )
            Row(
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        onClick = { onStatusSelected(status) },
                        role = Role.Tab,
                    )
                    .drawBehind {
                        val stroke = 2.dp.toPx()
                        drawRect(rule, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
                    }
                    .heightIn(min = 46.dp)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = status?.label() ?: stringResource(R.string.filter_all_short),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1,
                )
                Text(
                    text = (if (status == null) totalCount else counts[status] ?: 0).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Order and grouping behind one quiet anchor that reads as the current arrangement. Choosing the
 * current order again reverses it, so direction needs no control of its own.
 */
@Composable
private fun SortMenu(
    section: MediaSection,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    browseMode: HomeBrowseMode,
    accent: Color,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onBrowseModeChange: (HomeBrowseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRes = if (sortDirection == HomeSortDirection.Ascending) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
    val directionLabel = stringResource(
        if (sortDirection == HomeSortDirection.Ascending) R.string.sort_direction_ascending
        else R.string.sort_direction_descending,
    )
    val anchorLabel = listOfNotNull(
        sortMode.label(),
        browseMode.takeIf { it != HomeBrowseMode.Items }?.label(section),
    ).joinToString(" · ")

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClickLabel = stringResource(R.string.sort_label)) { expanded = true }
                .semantics { stateDescription = directionLabel }
                .heightIn(min = 40.dp)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(arrowRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Text(
                text = anchorLabel,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = OmnilogTheme.colors.appMuted,
            )
        }
        OmnilogDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = stringResource(R.string.sort_label),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
            )
            HomeSortMode.entries.forEach { mode ->
                val selected = mode == sortMode
                OmnilogDropdownItem(
                    text = mode.label(),
                    selected = selected,
                    accent = accent,
                    leadingIcon = {
                        if (selected) {
                            Icon(
                                painter = painterResource(arrowRes),
                                contentDescription = directionLabel,
                                modifier = Modifier.size(18.dp),
                                tint = accent,
                            )
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        if (selected) {
                            onSortDirectionChange(
                                if (sortDirection == HomeSortDirection.Ascending) HomeSortDirection.Descending
                                else HomeSortDirection.Ascending,
                            )
                        } else {
                            onSortModeChange(mode)
                        }
                        expanded = false
                    },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = OmnilogTheme.colors.appLine,
            )
            Text(
                text = stringResource(R.string.group_by_label),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
            )
            HomeBrowseMode.entries.forEach { mode ->
                OmnilogDropdownItem(
                    text = mode.label(section),
                    selected = mode == browseMode,
                    accent = accent,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(mode.iconRes()),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        onBrowseModeChange(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** What search and filters are hiding: how much of the tab remains, and which filters did it. */
@Composable
private fun NarrowedSummary(
    visibleCount: Int,
    tabCount: Int,
    filters: HomeAdvancedFilters,
    accent: Color,
    onClearFilters: () -> Unit,
) {
    val summary = listOfNotNull(
        stringResource(R.string.list_item_count, visibleCount, tabCount),
        (filters.authors + filters.genres).sorted().joinToString(", ").ifEmpty { null },
        filters.minimumExternalRating?.let {
            "${stringResource(R.string.filter_external_rating)} ${stringResource(R.string.filter_rating_at_least, it)}"
        },
        filters.minimumUserRating?.let {
            "${stringResource(R.string.filter_my_rating)} ${stringResource(R.string.filter_rating_at_least, it)}"
        },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (filters.isActive) {
            TextButton(
                onClick = onClearFilters,
                colors = ButtonDefaults.textButtonColors(contentColor = accent),
            ) {
                Text(stringResource(R.string.empty_filtered_clear))
            }
        }
    }
}

/** List/grid switch for Home and the status lists: a pill with a sliding indicator. */
@Composable
internal fun DisplayModeToggle(
    selectedMode: HomeDisplayMode,
    accent: Color,
    onModeSelected: (HomeDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentWidth = 34.dp
    val segmentHeight = 30.dp
    val indicatorOffset by animateDpAsState(
        targetValue = segmentWidth * selectedMode.ordinal,
        animationSpec = tween(durationMillis = 220),
        label = "displayModeIndicatorOffset",
    )
    val currentLabel = stringResource(
        if (selectedMode == HomeDisplayMode.List) R.string.view_list else R.string.view_grid,
    )
    // Two modes, so the whole pill flips between them: no need to hit the exact half.
    Surface(
        onClick = {
            onModeSelected(
                if (selectedMode == HomeDisplayMode.List) HomeDisplayMode.Grid else HomeDisplayMode.List,
            )
        },
        modifier = modifier.semantics { stateDescription = currentLabel },
        shape = RoundedCornerShape(999.dp),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Box(modifier = Modifier.padding(2.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(segmentWidth, segmentHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.20f)),
            )
            Row {
                HomeDisplayMode.entries.forEach { mode ->
                    val tint by animateColorAsState(
                        targetValue = if (mode == selectedMode) accent else OmnilogTheme.colors.appMuted,
                        animationSpec = tween(durationMillis = 220),
                        label = "displayModeTint",
                    )
                    Box(
                        modifier = Modifier.size(segmentWidth, segmentHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (mode == HomeDisplayMode.List) R.drawable.ic_view_list
                                else R.drawable.ic_view_grid,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = tint,
                        )
                    }
                }
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
    accent: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (activeCount > 0) {
                    Badge(containerColor = accent, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Text(activeCount.toString())
                    }
                }
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = stringResource(R.string.filter_more),
                modifier = Modifier.size(20.dp),
                tint = if (activeCount > 0) accent else OmnilogTheme.colors.appMuted,
            )
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
        availableItems.flatMap { it.creatorNames() }
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
        containerColor = OmnilogTheme.colors.appPanel,
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
                color = OmnilogTheme.colors.appPanel.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
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
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
            }
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
        contentColor = OmnilogTheme.colors.appInk,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
            Text(
                text = label,
                color = if (selected) accent else OmnilogTheme.colors.appInk,
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        selectedLabelColor = OmnilogTheme.colors.appInk,
                    ),
                )
            }
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value


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

private val TrackingStatus.dropdownIconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

private fun HomeBrowseMode.groupMode(): HomeGroupMode = when (this) {
    HomeBrowseMode.Items -> HomeGroupMode.None
    HomeBrowseMode.Collections -> HomeGroupMode.Collection
    HomeBrowseMode.Authors -> HomeGroupMode.Author
}

@Composable
private fun HomeBrowseMode.label(section: MediaSection): String = when (this) {
    HomeBrowseMode.Items -> stringResource(R.string.browse_none)
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
@ReadOnlyComposable
private fun MediaType.sectionAccent() = when (this) {
    MediaType.Anime -> MediaSection.Anime.themedAccent()
    MediaType.Book -> MediaSection.Books.themedAccent()
    MediaType.Movie, MediaType.TvShow -> MediaSection.Movies.themedAccent()
    MediaType.Game -> MediaSection.Games.themedAccent()
}
