package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackedMedia

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection
    val groupedItems = uiState.trackedItems
        .filter { it.collection != null }
        .groupBy { it.collection }
        .toSortedMap(compareBy(nullsLast()) { it?.name.orEmpty() })
    val ungroupedItems = uiState.trackedItems.filter { it.collection == null }
    val hasCollections = groupedItems.isNotEmpty()

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

            if (uiState.trackedItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(section.emptyMessageResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    )
                }
            } else if (hasCollections) {
                groupedItems.forEach { (collection, items) ->
                    item {
                        CollectionHeader(
                            title = collection?.name.orEmpty(),
                            count = items.size,
                        )
                    }
                    items(items.sortedBy { it.item.title }) { trackedMedia ->
                        MediaCard(
                            trackedMedia = trackedMedia,
                            accent = section.accent,
                            onClick = { onMediaClick(trackedMedia) },
                        )
                    }
                }

                if (ungroupedItems.isNotEmpty()) {
                    item {
                        CollectionHeader(
                            title = stringResource(R.string.collection_none),
                            count = ungroupedItems.size,
                        )
                    }
                    items(ungroupedItems.sortedBy { it.item.title }) { trackedMedia ->
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
private fun CollectionHeader(
    title: String,
    count: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
