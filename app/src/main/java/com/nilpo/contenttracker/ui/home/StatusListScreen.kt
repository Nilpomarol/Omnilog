package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/** Cross-media destination behind Home's "show all" status sections. */
@Composable
fun StatusListScreen(
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayMode by rememberSaveable { mutableStateOf(HomeDisplayMode.List) }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyVerticalGrid(
            columns = if (displayMode == HomeDisplayMode.Grid) GridCells.Fixed(2) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.status_list_count, items.size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    StatusDisplayToggle(
                        selectedMode = displayMode,
                        onModeSelected = { displayMode = it },
                    )
                }
            }
            items(items, key = { it.item.id }) { trackedMedia ->
                if (displayMode == HomeDisplayMode.Grid) {
                    MediaGridCard(trackedMedia = trackedMedia, onClick = { onMediaClick(trackedMedia) })
                } else {
                    MediaCard(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.statusListAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDisplayToggle(
    selectedMode: HomeDisplayMode,
    onModeSelected: (HomeDisplayMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row {
            HomeDisplayMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                IconButton(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (selected) OmnilogTheme.accents.Dashboard.copy(alpha = 0.18f)
                            else Color.Transparent,
                        ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (mode == HomeDisplayMode.List) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                        ),
                        contentDescription = stringResource(
                            if (mode == HomeDisplayMode.List) R.string.view_list else R.string.view_grid,
                        ),
                        tint = if (selected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaType.statusListAccent() = when (this) {
    MediaType.Anime -> MediaSection.Anime.themedAccent()
    MediaType.Book -> MediaSection.Books.themedAccent()
    MediaType.Movie, MediaType.TvShow -> MediaSection.Movies.themedAccent()
    MediaType.Game -> MediaSection.Games.themedAccent()
}
