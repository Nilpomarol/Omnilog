package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
    onAddClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    val groupedItems = uiState.trackedItems.groupBy { it.collection }
    val showCollectionGroups = uiState.sortMode == HomeSortMode.Collection && groupedItems.isNotEmpty()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SectionHeader(
                    section = section,
                    onAddClick = onAddClick,
                )
            }

            item {
                BrowseControls(
                    searchQuery = uiState.searchQuery,
                    statusFilter = uiState.statusFilter,
                    sortMode = uiState.sortMode,
                    sortDirection = uiState.sortDirection,
                    onSearchQueryChange = onSearchQueryChange,
                    onStatusFilterChange = onStatusFilterChange,
                    onSortModeChange = onSortModeChange,
                    onSortDirectionChange = onSortDirectionChange,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                )
            }

            if (uiState.trackedItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(section.emptyMessageResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
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
        }
    }
}

@Composable
private fun BrowseControls(
    searchQuery: String,
    statusFilter: TrackingStatus?,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TrackingStatus?) -> Unit,
    onSortModeChange: (HomeSortMode) -> Unit,
    onSortDirectionChange: (HomeSortDirection) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(R.string.search_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OptionSelector(
            label = stringResource(R.string.filter_status),
            options = listOf<TrackingStatus?>(null) + TrackingStatus.entries,
            selectedOption = statusFilter,
            optionLabel = { status ->
                status?.label() ?: stringResource(R.string.filter_all_statuses)
            },
            onOptionSelected = onStatusFilterChange,
        )

        OptionSelector(
            label = stringResource(R.string.sort_label),
            options = HomeSortMode.entries,
            selectedOption = sortMode,
            optionLabel = { sort -> sort.label() },
            onOptionSelected = onSortModeChange,
        )

        OptionSelector(
            label = stringResource(R.string.sort_direction_label),
            options = HomeSortDirection.entries,
            selectedOption = sortDirection,
            optionLabel = { direction -> direction.label() },
            onOptionSelected = onSortDirectionChange,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onExportBackup) {
                Text(text = stringResource(R.string.export_backup))
            }
            TextButton(onClick = onImportBackup) {
                Text(text = stringResource(R.string.import_backup))
            }
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
