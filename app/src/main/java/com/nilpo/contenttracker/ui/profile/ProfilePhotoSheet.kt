package com.nilpo.contenttracker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

// Three rows of four: enough to browse without the sheet becoming a scrolling wall.
private const val VisibleCovers = 12

/**
 * Where the profile picture comes from.
 *
 * Covers lead because they are the answer most of the time — the library is already full of images
 * the reader chose, and picking one takes a tap where the gallery takes a round trip out of the app.
 * The gallery, a URL and "remove" sit underneath as the escape hatches.
 *
 * This is also where the URL field and its errors moved to. They used to sit in the profile form,
 * which is now the hero itself, and neither belongs on a page that is mostly someone's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoSheet(
    coverItems: List<TrackedMedia>,
    imageUrl: String,
    isLoading: Boolean,
    errorMessage: String?,
    hasPhoto: Boolean,
    onCoverSelected: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onDownloadFromUrl: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }

    // Swallows whatever the cover grid does not use, so reaching the end of the covers does not
    // hand the gesture to the sheet and start dragging it shut. The grid scrolls first and only
    // the leftovers are consumed, so the grid itself still moves normally.
    val keepScrollInGrid = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Imatge de perfil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )

            if (coverItems.isNotEmpty()) {
                // The whole library is reachable, but only a slice is drawn: hundreds of covers in
                // a sheet is a wall to scroll, and the one you want is usually either recent or
                // known by name. Recents answer the first case, search answers the second.
                val matches = remember(coverItems, query) {
                    val trimmed = query.trim()
                    if (trimmed.isEmpty()) {
                        coverItems.take(VisibleCovers)
                    } else {
                        coverItems.filter {
                            displayMediaTitle(it.item.title).contains(trimmed, ignoreCase = true)
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cerca un títol") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Esborra la cerca",
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { query = "" },
                            )
                        }
                    },
                    singleLine = true,
                )

                if (matches.isEmpty()) {
                    Text(
                        text = "Cap títol coincideix amb «${query.trim()}».",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                } else {
                    // Bounded height so the grid scrolls inside the sheet rather than fighting it.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(212.dp)
                            .nestedScroll(keepScrollInGrid),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(matches, key = { it.item.id }) { tracked ->
                            val coverUrl = tracked.item.coverUrl
                            if (coverUrl != null) {
                                MetadataCoverImage(
                                    coverUrl = coverUrl,
                                    modifier = Modifier
                                        .aspectRatio(2f / 3f)
                                        .clickable { onCoverSelected(coverUrl) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = if (query.isBlank()) {
                            "Els més recents. Cerca per trobar-ne d'altres."
                        } else {
                            "${matches.size} de ${coverItems.size} títols"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onChooseFromGallery,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Tria de la galeria")
                }
                if (hasPhoto) {
                    OutlinedButton(onClick = onRemovePhoto) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Treu la imatge",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = imageUrl,
                onValueChange = onImageUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL de la imatge") },
                placeholder = { Text("https://exemple.com/foto.jpg") },
                singleLine = true,
                enabled = !isLoading,
            )
            OutlinedButton(
                onClick = onDownloadFromUrl,
                enabled = imageUrl.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isLoading) "Descarregant…" else "Fes servir aquesta URL")
            }

            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Desant la imatge…",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.accents.Dashboard,
                    )
                }
            }
        }
    }
}
