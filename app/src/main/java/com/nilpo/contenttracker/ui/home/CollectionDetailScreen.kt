package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia

@Composable
fun CollectionDetailScreen(
    collection: MediaCollection,
    items: List<TrackedMedia>,
    accent: Color,
    onBack: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Button(onClick = onBack) {
                    Text(text = stringResource(R.string.back))
                }
            }

            item {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.collection_item_count, items.size),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            items(items.sortedBy { it.item.title }) { trackedMedia ->
                MediaCard(
                    trackedMedia = trackedMedia,
                    accent = accent,
                    onClick = { onMediaClick(trackedMedia) },
                )
            }
        }
    }
}
