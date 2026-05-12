package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
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
import com.nilpo.contenttracker.core.model.TrackedMedia

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = uiState.selectedSection

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
                SectionHeader(section = section)
            }

            if (uiState.trackedItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(section.emptyMessageResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    )
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
