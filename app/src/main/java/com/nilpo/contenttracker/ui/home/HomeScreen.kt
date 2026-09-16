package com.nilpo.contenttracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
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
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import androidx.compose.material3.HorizontalDivider
import com.nilpo.contenttracker.ui.add.label
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.partialSearchFailureMessage
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    var filtersExpanded by remember { mutableStateOf(false) }
    val groupMode = uiState.browseMode.groupMode()
    val groupedItems = remember(
        uiState.trackedItems,
        uiState.browseMode,
        uiState.sortMode,
        uiState.sortDirection,
        uiState.advancedFilters.authors,
    ) {
        buildHomeGroups(
            items = uiState.trackedItems,
            groupMode = groupMode,
            sortMode = uiState.sortMode,
            sortDirection = uiState.sortDirection,
            selectedCreators = uiState.advancedFilters.authors,
        )
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
            modifier = Modifier.fillMaxSize(),
            // A small top inset: the top bar already carries the section title, so the controls sit close under it.
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The search field lives in the top bar, so it stays put while the list scrolls.
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
                        onAdvancedFiltersChange = onAdvancedFiltersChange,
                    )
                    if (filtersExpanded) {
                        AdvancedFiltersSheet(
                            filters = uiState.advancedFilters,
                            section = section,
                            availableItems = sectionItems,
                            resultCount = uiState.trackedItems.size,
                            onFiltersChange = onAdvancedFiltersChange,
                            onDismiss = { filtersExpanded = false },
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
                                enter = GroupItemsEnter,
                                exit = GroupItemsExit,
                            ) {
                                Column(
                                    // Same gap as between rows, so a header's hairline sits centred above the first item.
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    // One block so the hairlines sit evenly instead of fighting the grid's item spacing.
                    else -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            apiResults.forEachIndexed { index, suggestion ->
                                if (index > 0) HorizontalDivider(color = OmnilogTheme.colors.appLine)
                                MetadataSuggestionRow(
                                    suggestion = suggestion,
                                    accent = suggestion.mediaType.sectionAccent(),
                                    duplicateState = duplicateStateForSuggestion(suggestion),
                                    horizontalPadding = 0.dp,
                                    onClick = { onApiSuggestionSelected(suggestion) },
                                )
                            }
                        }
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
 * The library's controls: status as colour-coded pills, then one line to arrange what is left, with
 * order and grouping as separate outlined menus, and filters beside the layout switch. A summary
 * appears only while search or filters hide part of the chosen status.
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
    onAdvancedFiltersChange: (HomeAdvancedFilters) -> Unit,
) {
    val statusCounts = remember(sectionItems) {
        sectionItems.groupingBy { it.currentSession?.status }.eachCount()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        StatusPills(
            selectedStatus = statusFilter,
            counts = statusCounts,
            totalCount = sectionItems.size,
            onStatusSelected = onStatusFilterChange,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortMenu(
                sortMode = sortMode,
                sortDirection = sortDirection,
                accent = accent,
                onSortModeChange = onSortModeChange,
                onSortDirectionChange = onSortDirectionChange,
            )
            GroupMenu(
                section = section,
                browseMode = browseMode,
                accent = accent,
                onBrowseModeChange = onBrowseModeChange,
            )
            // Takes whatever width is left, so an active filter's label shortens instead of widening the row.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                FiltersButton(
                    filters = advancedFilters,
                    section = section,
                    onClick = onAdvancedFiltersClick,
                    onClear = { onAdvancedFiltersChange(HomeAdvancedFilters()) },
                )
            }
            // Grouped lists are always rows, so the switch steps aside rather than sitting there disabled.
            AnimatedVisibility(visible = browseMode == HomeBrowseMode.Items) {
                DisplayModeToggle(
                    selectedMode = displayMode,
                    accent = accent,
                    onModeSelected = onDisplayModeChange,
                )
            }
        }
        if (searchActive || advancedFilters.isActive) {
            NarrowedSummary(
                visibleCount = visibleCount,
                tabCount = statusFilter?.let { statusCounts[it] ?: 0 } ?: sectionItems.size,
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

/** A filter's shape: a soft rounded pill, filled with [selectedColor] while it is the active choice. */
@Composable
private fun FilterPill(
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = OmnilogTheme.colors.appInk,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) selectedColor else OmnilogTheme.colors.appPanel,
        animationSpec = tween(durationMillis = 220),
        label = "pillContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) OmnilogTheme.colors.appBackground else OmnilogTheme.colors.appInk,
        animationSpec = tween(durationMillis = 220),
        label = "pillContent",
    )
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .selectable(selected = selected, role = role, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(contentColor)
    }
}

@Composable
private fun PillText(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Status as single-choice pills carrying their counts; the chosen one fills with its status colour. */
@Composable
private fun StatusPills(
    selectedStatus: TrackingStatus?,
    counts: Map<TrackingStatus?, Int>,
    totalCount: Int,
    onStatusSelected: (TrackingStatus?) -> Unit,
) {
    // Statuses with nothing in them stay out of the way; a selected one stays so it can be left.
    val options = listOf<TrackingStatus?>(null) +
        StatusTabOrder.filter { (counts[it] ?: 0) > 0 || it == selectedStatus }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { status ->
            val selected = status == selectedStatus
            val statusColor = status?.stateColor
            FilterPill(
                selected = selected,
                role = Role.Tab,
                onClick = { onStatusSelected(status) },
                selectedColor = statusColor ?: OmnilogTheme.colors.appInk,
            ) { content ->
                PillText(status?.label() ?: stringResource(R.string.filter_all_short), content)
                Text(
                    text = (if (status == null) totalCount else counts[status] ?: 0).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * An arrangement control, set apart from the filled filter pills: an outlined stadium that takes
 * [activeColor] while it changes the list from its default.
 */
@Composable
internal fun ArrangeButton(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeColor: Color = OmnilogTheme.colors.appInk,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val contentColor = if (active) activeColor else OmnilogTheme.colors.appInk
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .border(1.dp, if (active) activeColor else OmnilogTheme.colors.appLine, shape)
            .clickable(onClickLabel = onClickLabel, role = Role.DropdownList, onClick = onClick)
            .heightIn(min = 34.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(contentColor)
    }
}

/**
 * The way into the filter sheet. Idle it is just the icon; while filters are on it grows into a filled
 * pill naming the one filter (or how many there are) with its own clear button, so filters can be
 * dropped without opening the sheet. The label ellipsizes rather than widening the row.
 */
@Composable
private fun FiltersButton(
    filters: HomeAdvancedFilters,
    section: MediaSection,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val labels = filters.labels(section)
    val filtersLabel = stringResource(R.string.filter_more)
    if (labels.isEmpty()) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = filtersLabel,
                modifier = Modifier.size(20.dp),
                tint = OmnilogTheme.colors.appMuted,
            )
        }
    } else {
        val ink = OmnilogTheme.colors.appBackground
        Row(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(OmnilogTheme.colors.appInk),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No filter icon here: the filled pill with its clear button already reads as an active
            // filter, and the label needs that width far more on a narrow row.
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClickLabel = filtersLabel, onClick = onClick)
                    .heightIn(min = 36.dp)
                    .padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillText(
                    labels.singleOrNull() ?: pluralStringResource(R.plurals.filter_active_count, labels.size, labels.size),
                    ink,
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp)
                    .clickable(role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.empty_filtered_clear),
                    modifier = Modifier.size(16.dp),
                    tint = ink,
                )
            }
        }
    }
}

/** Order behind its own outlined button. Choosing the current order again reverses it. */
@Composable
internal fun SortMenu(
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    accent: Color,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRes = if (sortDirection == HomeSortDirection.Ascending) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
    val directionLabel = stringResource(
        if (sortDirection == HomeSortDirection.Ascending) R.string.sort_direction_ascending
        else R.string.sort_direction_descending,
    )
    val sortLabel = stringResource(R.string.sort_label)

    Box {
        ArrangeButton(
            onClick = { expanded = true },
            onClickLabel = sortLabel,
            modifier = Modifier.semantics { stateDescription = directionLabel },
        ) { content ->
            Icon(
                painter = painterResource(arrowRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = content,
            )
            PillText(sortMode.label(), content)
        }
        OmnilogDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
        }
    }
}

/** Grouping behind its own outlined button, which takes the section accent while the list is grouped. */
@Composable
private fun GroupMenu(
    section: MediaSection,
    browseMode: HomeBrowseMode,
    accent: Color,
    onBrowseModeChange: (HomeBrowseMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val grouped = browseMode != HomeBrowseMode.Items
    val groupLabel = stringResource(R.string.group_by_label)

    Box {
        ArrangeButton(
            onClick = { expanded = true },
            onClickLabel = groupLabel,
            active = grouped,
            activeColor = accent,
        ) { content ->
            Icon(
                painter = painterResource(browseMode.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = content,
            )
            PillText(if (grouped) browseMode.label(section) else groupLabel, content)
        }
        OmnilogDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

/** How much of the chosen status search and filters leave visible; the filter pill names the filters. */
@Composable
private fun NarrowedSummary(
    visibleCount: Int,
    tabCount: Int,
) {
    Text(
        text = stringResource(R.string.list_item_count, visibleCount, tabCount),
        modifier = Modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = OmnilogTheme.colors.appMuted,
        maxLines = 1,
    )
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

/** Stops a scroll inside the sheet from dragging the sheet itself once the content reaches its end. */
private val SheetScrollIsolation = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(x = 0f, y = available.y)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(x = 0f, y = available.y)
}

private val RatingThresholds = listOf(5, 6, 7, 8, 9)
private const val MaxHistogramBins = 24
private val RangeThumbSize = 22.dp

/**
 * The filter sheet, applied as you go: every change lands immediately, so there is nothing to confirm
 * and removing a filter is one gesture. Short, fixed choices are pills, long lists are a search box over
 * a checklist, and spans are a histogram over a two-thumb slider. Each offers only what the section's
 * items carry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFiltersSheet(
    filters: HomeAdvancedFilters,
    section: MediaSection,
    availableItems: List<TrackedMedia>,
    resultCount: Int,
    onFiltersChange: (HomeAdvancedFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(availableItems) { filterOptions(availableItems) }
    // Lengths only compare within one unit, so the filter waits until the section or the type filter
    // settles on a single type (films count minutes, series episodes).
    val lengthType = filters.types.singleOrNull() ?: section.types.singleOrNull()
    val lengthValues = remember(availableItems, lengthType) {
        availableItems
            .filter { it.item.type == lengthType }
            .mapNotNull { it.item.progressTotal?.takeIf { total -> total > 0 } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The page ground rather than the panel tone, so unselected pills stand out against it.
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.85f)
                    .nestedScroll(SheetScrollIsolation)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.filter_sheet_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.filter_result_count, resultCount, resultCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                    TextButton(
                        onClick = { onFiltersChange(HomeAdvancedFilters()) },
                        enabled = filters.isActive,
                    ) {
                        Text(stringResource(R.string.filter_clear_all))
                    }
                }

                if (options.types.size > 1) {
                    FilterSection(stringResource(R.string.filter_type)) {
                        PillFlow {
                            options.types.keys.sortedBy { it.ordinal }.forEach { type ->
                                ToggleFilterPill(
                                    label = stringResource(type.labelResId),
                                    count = options.types[type],
                                    selected = type in filters.types,
                                    // Another type can mean another unit, so a length span set for the old one goes.
                                    onClick = {
                                        onFiltersChange(filters.copy(types = filters.types.toggle(type), lengthRange = null))
                                    },
                                )
                            }
                        }
                    }
                }

                FilterSection(stringResource(R.string.filter_my_rating)) {
                    PillFlow {
                        RatingThresholds.forEach { minimum ->
                            ToggleFilterPill(
                                label = stringResource(R.string.filter_rating_at_least, minimum),
                                selected = filters.minimumUserRating == minimum,
                                onClick = {
                                    onFiltersChange(
                                        filters.copy(minimumUserRating = minimum.takeIf { filters.minimumUserRating != it }),
                                    )
                                },
                            )
                        }
                    }
                }

                FilterSection(stringResource(R.string.filter_external_rating)) {
                    PillFlow {
                        RatingThresholds.forEach { minimum ->
                            ToggleFilterPill(
                                label = stringResource(R.string.filter_rating_at_least, minimum),
                                selected = filters.minimumExternalRating == minimum,
                                onClick = {
                                    onFiltersChange(
                                        filters.copy(minimumExternalRating = minimum.takeIf { filters.minimumExternalRating != it }),
                                    )
                                },
                            )
                        }
                    }
                }

                if (lengthType != null) {
                    lengthValues.spanOrNull()?.let { bounds ->
                        val unit = progressUnitLabel(lengthType, bounds.last)
                        RangeFilter(
                            title = stringResource(R.string.filter_length),
                            values = lengthValues,
                            bounds = bounds,
                            selected = filters.lengthRange,
                            format = { stringResource(R.string.filter_range_units, it.first, it.last, unit) },
                            onSelectedChange = { onFiltersChange(filters.copy(lengthRange = it)) },
                        )
                    }
                }

                if (options.authors.isNotEmpty()) {
                    SearchableFilterDropdown(
                        title = stringResource(section.creatorFilterLabelResId),
                        counts = options.authors,
                        selected = filters.authors,
                        onToggle = { onFiltersChange(filters.copy(authors = filters.authors.toggle(it))) },
                    )
                }
                if (options.genres.isNotEmpty()) {
                    SearchableFilterDropdown(
                        title = stringResource(R.string.filter_genres),
                        counts = options.genres,
                        selected = filters.genres,
                        onToggle = { onFiltersChange(filters.copy(genres = filters.genres.toggle(it))) },
                    )
                }

                options.releaseYears.spanOrNull()?.let { bounds ->
                    RangeFilter(
                        title = stringResource(R.string.filter_release_year),
                        values = options.releaseYears,
                        bounds = bounds,
                        selected = filters.releaseYears,
                        format = { stringResource(R.string.filter_range, it.first, it.last) },
                        onSelectedChange = { onFiltersChange(filters.copy(releaseYears = it)) },
                    )
                }

                if (options.platformTypes.isNotEmpty()) {
                    FilterSection(stringResource(R.string.filter_platform)) {
                        PillFlow {
                            options.platformTypes.keys.sortedBy { it.ordinal }.forEach { platformType ->
                                ToggleFilterPill(
                                    label = platformType.label(),
                                    count = options.platformTypes[platformType],
                                    selected = platformType in filters.platformTypes,
                                    onClick = {
                                        onFiltersChange(filters.copy(platformTypes = filters.platformTypes.toggle(platformType)))
                                    },
                                )
                            }
                        }
                    }
                }

                if (options.ownedCount > 0) {
                    FilterSection(stringResource(R.string.filter_other)) {
                        PillFlow {
                            ToggleFilterPill(
                                label = stringResource(R.string.owned_label),
                                count = options.ownedCount,
                                selected = filters.ownedOnly,
                                onClick = { onFiltersChange(filters.copy(ownedOnly = !filters.ownedOnly)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What a section's items can be filtered by, each value with how many items carry it. */
private class FilterOptions(
    val authors: Map<String, Int>,
    val genres: Map<String, Int>,
    val types: Map<MediaType, Int>,
    val platformTypes: Map<ConsumptionPlatformType, Int>,
    val releaseYears: List<Int>,
    val ownedCount: Int,
)

private fun filterOptions(items: List<TrackedMedia>) = FilterOptions(
    authors = items.flatMap { it.creatorNames().cleanValues() }.groupingBy { it }.eachCount(),
    genres = items.flatMap { it.item.genres.cleanValues() }.groupingBy { it }.eachCount(),
    types = items.groupingBy { it.item.type }.eachCount(),
    platformTypes = items.mapNotNull { it.currentSession?.platform?.type }.groupingBy { it }.eachCount(),
    releaseYears = items.mapNotNull { it.item.releaseYear },
    ownedCount = items.count { it.item.isOwned },
)

private fun List<String>.cleanValues(): List<String> = map(String::trim).filter(String::isNotBlank).distinct()

/** The lowest to highest value, or null when there is nothing to choose between. */
private fun List<Int>.spanOrNull(): IntRange? = if (distinct().size < 2) null else min()..max()

/**
 * Counts [values] into at most [MaxHistogramBins] equal-width bins covering [bounds]: a span of a few
 * years gets a bar per year, a span of a thousand pages bars of about forty.
 */
internal fun histogram(values: List<Int>, bounds: IntRange): List<Pair<IntRange, Int>> {
    val span = bounds.last - bounds.first + 1
    val width = (span + MaxHistogramBins - 1) / MaxHistogramBins
    val binCount = (span + width - 1) / width
    val counts = IntArray(binCount)
    values.forEach { counts[((it - bounds.first) / width).coerceIn(0, binCount - 1)]++ }
    return counts.mapIndexed { index, count ->
        (bounds.first + index * width)..(bounds.first + (index + 1) * width - 1) to count
    }
}

/** A titled block, with the current choice at the end of the title line when there is one to show. */
@Composable
private fun FilterSection(
    title: String,
    value: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appInk,
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        content()
    }
}

/** Pills that wrap onto further lines, so nothing in the sheet scrolls sideways. */
@Composable
private fun PillFlow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun ToggleFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
) {
    FilterPill(selected = selected, role = Role.Checkbox, onClick = onClick) { content ->
        PillText(label, content)
        count?.let {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.65f),
                maxLines = 1,
            )
        }
    }
}

/**
 * A span of years or lengths. A histogram shows how the section's items spread across it, with the
 * bars inside the chosen span inked, above a slim two-thumb slider whose ends are labelled with the
 * lowest and highest value. The full span means no filter; the list refilters when a thumb is let go,
 * so dragging stays smooth, while the bars and the title's value follow the thumbs live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeFilter(
    title: String,
    values: List<Int>,
    bounds: IntRange,
    selected: IntRange?,
    format: @Composable (IntRange) -> String,
    onSelectedChange: (IntRange?) -> Unit,
) {
    val full = bounds.first.toFloat()..bounds.last.toFloat()
    var draft by remember(selected, bounds) {
        mutableStateOf(
            selected?.let { it.first.coerceIn(bounds).toFloat()..it.last.coerceIn(bounds).toFloat() } ?: full,
        )
    }
    val shown = draft.start.roundToInt()..draft.endInclusive.roundToInt()
    val bins = remember(values, bounds) { histogram(values, bounds) }

    FilterSection(
        title = title,
        value = if (shown == bounds) stringResource(R.string.filter_any) else format(shown),
    ) {
        Column {
            // Inset by half a thumb so the bars line up with the stretch of track the thumbs travel.
            RangeHistogram(
                bins = bins,
                selected = shown,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = RangeThumbSize / 2),
            )
            RangeSlider(
                value = draft,
                onValueChange = { draft = it },
                valueRange = full,
                onValueChangeFinished = {
                    val range = draft.start.roundToInt()..draft.endInclusive.roundToInt()
                    onSelectedChange(range.takeIf { it != bounds })
                },
                startThumb = { RangeThumb() },
                endThumb = { RangeThumb() },
                track = { RangeTrack(it) },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = bounds.first.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
                Text(
                    text = bounds.last.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
    }
}

@Composable
private fun RangeHistogram(
    bins: List<Pair<IntRange, Int>>,
    selected: IntRange,
    modifier: Modifier = Modifier,
) {
    val peak = bins.maxOf { it.second }.coerceAtLeast(1)
    val inside = OmnilogTheme.colors.appInk
    val outside = OmnilogTheme.colors.appMuted.copy(alpha = 0.25f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bins.forEach { (range, count) ->
            val inSpan = range.last >= selected.first && range.first <= selected.last
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Empty bins keep a sliver so the baseline reads as continuous.
                    .fillMaxHeight(if (count == 0) 0.05f else 0.15f + 0.85f * count / peak)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(if (inSpan) inside else outside),
            )
        }
    }
}

/** An ink dot with a ring of the sheet's ground, so it sits cleanly over the track. */
@Composable
private fun RangeThumb() {
    Box(
        modifier = Modifier
            .size(RangeThumbSize)
            .clip(CircleShape)
            .background(OmnilogTheme.colors.appInk)
            .border(4.dp, OmnilogTheme.colors.appBackground, CircleShape),
    )
}

/** A thin rounded track, inked between the two thumbs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeTrack(state: RangeSliderState) {
    val inactive = OmnilogTheme.colors.appMuted.copy(alpha = 0.25f)
    val active = OmnilogTheme.colors.appInk
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        val span = state.valueRange.endInclusive - state.valueRange.start
        val start = if (span == 0f) 0f else (state.activeRangeStart - state.valueRange.start) / span
        val end = if (span == 0f) 1f else (state.activeRangeEnd - state.valueRange.start) / span
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = inactive, cornerRadius = radius)
        drawRoundRect(
            color = active,
            topLeft = Offset(size.width * start, 0f),
            size = Size(size.width * (end - start), size.height),
            cornerRadius = radius,
        )
    }
}

/**
 * A long list of values such as authors or genres: a search field that opens a checklist, most common
 * first with how many items carry each. The field's supporting line names what is already chosen, so
 * the choice stays visible with the list closed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchableFilterDropdown(
    title: String,
    counts: Map<String, Int>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    // Re-asked as the keyboard slides in, so the field and its list settle above the keyboard, where the
    // matches stay visible while typing, instead of underneath it.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(expanded, imeBottom) {
        if (expanded) bringIntoView.bringIntoView()
    }
    val options = remember(counts, query) {
        val term = query.trim()
        counts.keys
            .filter { it.contains(term, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { counts.getValue(it) }.thenBy { it })
    }

    Column(
        modifier = Modifier.bringIntoViewRequester(bringIntoView),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text(title) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            },
            supportingText = if (selected.isEmpty()) {
                null
            } else {
                {
                    Text(
                        text = selected.sorted().joinToString(", "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        )
        if (expanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = OmnilogTheme.colors.appPanel,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .nestedScroll(SheetScrollIsolation)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    options.forEach { option ->
                        FilterCheckRow(
                            label = option,
                            count = counts[option],
                            checked = option in selected,
                            onClick = { onToggle(option) },
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
private fun FilterCheckRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = OmnilogTheme.colors.appInk,
                checkmarkColor = OmnilogTheme.colors.appBackground,
                uncheckedColor = OmnilogTheme.colors.appMuted,
            ),
        )
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = OmnilogTheme.colors.appInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        count?.let {
            Text(
                text = it.toString(),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/** One short label per active filter value, in the order the sheet lists them. */
@Composable
private fun HomeAdvancedFilters.labels(section: MediaSection): List<String> = buildList {
    types.sortedBy { it.ordinal }.forEach { add(stringResource(it.labelResId)) }
    minimumUserRating?.let {
        add("${stringResource(R.string.filter_my_rating)} ${stringResource(R.string.filter_rating_at_least, it)}")
    }
    minimumExternalRating?.let {
        add("${stringResource(R.string.filter_external_rating)} ${stringResource(R.string.filter_rating_at_least, it)}")
    }
    lengthRange?.let { range ->
        val lengthType = types.singleOrNull() ?: section.types.singleOrNull()
        add(
            if (lengthType != null) {
                stringResource(R.string.filter_range_units, range.first, range.last, progressUnitLabel(lengthType, range.last))
            } else {
                stringResource(R.string.filter_range, range.first, range.last)
            },
        )
    }
    addAll(authors.sorted())
    addAll(genres.sorted())
    releaseYears?.let {
        add("${stringResource(R.string.filter_release_year)} ${stringResource(R.string.filter_range, it.first, it.last)}")
    }
    platformTypes.sortedBy { it.ordinal }.forEach { add(it.label()) }
    if (ownedOnly) add(stringResource(R.string.owned_label))
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
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
