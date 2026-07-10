package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun AuthorDetailScreen(
    author: String,
    items: List<TrackedMedia>,
    accent: Color,
    onBack: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val averageRating = items.collectionAverageRating()
    val sortedItems = items.sortedWith(
        compareBy<TrackedMedia> { it.collection == null }
            .thenBy { it.collection?.name?.lowercase().orEmpty() }
            .thenBy { it.item.collectionSortOrder ?: Double.MAX_VALUE }
            .thenBy { it.item.title.lowercase() },
    )

    Surface(modifier = modifier, color = OmnilogColors.AppBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = OmnilogColors.AppInk,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.author_page_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogColors.AppMuted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    averageRating?.let { rating ->
                        Text(
                            text = stringResource(R.string.collection_average_rating, rating),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = stringResource(R.string.collection_item_count, items.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                    )
                }
            }
            HorizontalDivider(color = OmnilogColors.AppLine)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sortedItems, key = { it.item.id }) { trackedMedia ->
                    MediaCard(
                        trackedMedia = trackedMedia,
                        accent = accent,
                        onClick = { onMediaClick(trackedMedia) },
                    )
                }
            }
        }
    }
}
