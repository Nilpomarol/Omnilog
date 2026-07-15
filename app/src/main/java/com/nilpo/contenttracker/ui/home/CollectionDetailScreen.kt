package com.nilpo.contenttracker.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun CollectionDetailScreen(
    collection: MediaCollection,
    items: List<TrackedMedia>,
    accent: Color,
    onBack: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    onAddToCollection: (MediaCollection, Double?) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onUpdateCollectionItemOrder: (Long, List<CollectionItemOrder>) -> Unit,
    onUpdateMediaItemCollection: (TrackedMedia, Long?, Double?) -> Unit,
    onCollectionActionMessage: (String) -> Unit,
    onRegisterBackRequest: (((() -> Unit)?) -> Unit) = {},
    modifier: Modifier = Modifier,
) {
    var nameText by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var showRenameDialog by rememberSaveable(collection.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }
    var showDiscardReorderConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }
    var isReordering by rememberSaveable(collection.id) { mutableStateOf(false) }
    var draftOrderValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var itemPendingRemoval by remember { mutableStateOf<TrackedMedia?>(null) }
    var itemPendingMove by remember { mutableStateOf<TrackedMedia?>(null) }
    val context = LocalContext.current
    val removeDoneMessage = stringResource(
        R.string.collection_remove_item_done,
        itemPendingRemoval?.item?.title?.let(::displayMediaTitle).orEmpty(),
    )
    val sortedItems = items.sortedWith(collectionItemComparator())
    val displayedItems = if (isReordering) {
        sortedItems.sortedWith(
            compareBy<TrackedMedia> { trackedMedia ->
                draftOrderValues[trackedMedia.item.id].toCollectionOrderOrNull()
                    ?: trackedMedia.item.collectionSortOrder
                    ?: Double.MAX_VALUE
            }.thenBy { trackedMedia -> trackedMedia.item.releaseYear ?: Int.MAX_VALUE }
                .thenBy { trackedMedia -> trackedMedia.item.title.lowercase() },
        )
    } else {
        sortedItems
    }
    val hasUnsavedReorder = isReordering && draftOrderValues != sortedItems.toDraftOrderValues()
    val nextCollectionOrder = (items.maxOfOrNull { it.item.collectionSortOrder ?: 0.0 } ?: 0.0) + 1.0
    val averageRating = items.collectionAverageRating()
    val progressSummary = items.collectionProgressSummary()
    val collectionCoverStack = sortedItems.mapNotNull { it.item.coverUrl }.take(3)
    val requestBack = {
        if (hasUnsavedReorder) {
            showDiscardReorderConfirmation = true
        } else if (isReordering) {
            isReordering = false
            draftOrderValues = sortedItems.toDraftOrderValues()
        } else {
            onBack()
        }
    }
    val latestRequestBack by rememberUpdatedState(requestBack)
    DisposableEffect(collection.id) {
        onRegisterBackRequest { latestRequestBack() }
        onDispose { onRegisterBackRequest(null) }
    }

    // Keep nameText in sync if the collection name changes externally (e.g. after a save)
    LaunchedEffect(collection.id, collection.name) {
        nameText = collection.name
    }
    LaunchedEffect(collection.id, sortedItems.map { it.item.id }) {
        if (!isReordering) {
            draftOrderValues = sortedItems.toDraftOrderValues()
        }
    }
    BackHandler(enabled = isReordering) {
        requestBack()
    }

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            if (isReordering) {
                ReorderHeaderBar(
                    collectionName = collection.name,
                    accent = accent,
                    onCancel = {
                        if (hasUnsavedReorder) {
                            showDiscardReorderConfirmation = true
                        } else {
                            isReordering = false
                            draftOrderValues = sortedItems.toDraftOrderValues()
                        }
                    },
                    onSave = {
                        onUpdateCollectionItemOrder(
                            collection.id,
                            displayedItems.toCollectionItemOrders(draftOrderValues),
                        )
                        isReordering = false
                    },
                )
            } else {
                CollectionHeroHeader(
                    collection = collection,
                    accent = accent,
                    coverStack = collectionCoverStack,
                    itemCount = items.size,
                    progressSummary = progressSummary,
                    averageRating = averageRating,
                    onAdd = { onAddToCollection(collection, nextCollectionOrder) },
                    onEdit = { showRenameDialog = true },
                    onReorder = {
                        draftOrderValues = sortedItems.toDraftOrderValues()
                        isReordering = true
                    },
                    onDelete = { showDeleteConfirmation = true },
                )
            }

            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.collection_items_section),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppMuted,
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        color = OmnilogColors.AppLine,
                    )
                }

            }
            // ── Item list ──────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (items.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.collection_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogColors.AppMuted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    items(
                        items = displayedItems,
                        key = { trackedMedia -> trackedMedia.item.id },
                    ) { trackedMedia ->
                        if (isReordering) {
                            val index = displayedItems.indexOfFirst { it.item.id == trackedMedia.item.id }
                            ReorderItemRow(
                                trackedMedia = trackedMedia,
                                orderText = draftOrderValues[trackedMedia.item.id].orEmpty(),
                                canMoveUp = index > 0,
                                canMoveDown = index < displayedItems.lastIndex,
                                accent = accent,
                                onOrderChange = { value ->
                                    draftOrderValues = draftOrderValues + (
                                        trackedMedia.item.id to value.toCollectionOrderInput()
                                        )
                                },
                                onMoveUp = {
                                    draftOrderValues = displayedItems
                                        .moveItem(fromIndex = index, toIndex = index - 1)
                                        .toSequentialDraftOrderValues()
                                },
                                onMoveDown = {
                                    draftOrderValues = displayedItems
                                        .moveItem(fromIndex = index, toIndex = index + 1)
                                        .toSequentialDraftOrderValues()
                                },
                            )
                        } else {
                            CollectionItemCard(
                                trackedMedia = trackedMedia,
                                accent = accent,
                                onMediaClick = onMediaClick,
                                onMoveClick = { itemPendingMove = trackedMedia },
                                onRemoveClick = { itemPendingRemoval = trackedMedia },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Rename dialog ──────────────────────────────────────────────────────
    if (showRenameDialog) {
        OmnilogAlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                nameText = collection.name
            },
            title = stringResource(R.string.collection_rename_title),
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.field_collection)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameText.isNotBlank(),
                    onClick = {
                        onRenameCollection(collection.id, nameText.trim())
                        showRenameDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        nameText = collection.name
                    },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    if (showDeleteConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = stringResource(R.string.delete_collection_title),
            text = { Text(text = stringResource(R.string.delete_collection_message, collection.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCollection(collection.id)
                        showDeleteConfirmation = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDiscardReorderConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showDiscardReorderConfirmation = false },
            title = stringResource(R.string.collection_unsaved_reorder_title),
            text = { Text(text = stringResource(R.string.collection_unsaved_reorder_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardReorderConfirmation = false
                        isReordering = false
                        draftOrderValues = sortedItems.toDraftOrderValues()
                    },
                ) {
                    Text(text = stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardReorderConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    itemPendingRemoval?.let { trackedMedia ->
        OmnilogAlertDialog(
            onDismissRequest = { itemPendingRemoval = null },
            title = stringResource(R.string.collection_remove_item),
            text = {
                Text(
                    text = stringResource(
                        R.string.collection_remove_item_message,
                        displayMediaTitle(trackedMedia.item.title),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateMediaItemCollection(trackedMedia, null, null)
                        onCollectionActionMessage(removeDoneMessage)
                        itemPendingRemoval = null
                    },
                ) {
                    Text(text = stringResource(R.string.collection_remove_item))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingRemoval = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    itemPendingMove?.let { trackedMedia ->
        val targetCollections = trackedMedia.availableCollections
            .filter { availableCollection -> availableCollection.id != collection.id }
        MoveCollectionDialog(
            availableCollections = targetCollections,
            accent = accent,
            onDismiss = { itemPendingMove = null },
            onMove = { targetCollection ->
                onUpdateMediaItemCollection(
                    trackedMedia,
                    targetCollection.id,
                    null,
                )
                onCollectionActionMessage(
                    context.getString(
                        R.string.collection_move_item_done,
                        displayMediaTitle(trackedMedia.item.title),
                        targetCollection.name,
                    ),
                )
                itemPendingMove = null
            },
        )
    }
}

@Composable
private fun CollectionHeroHeader(
    collection: MediaCollection,
    accent: Color,
    coverStack: List<String>,
    itemCount: Int,
    progressSummary: CollectionProgressSummary,
    averageRating: Double?,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Contained hero: a soft accent glow lives inside the rounded card (which
    // clips it), so the header reads as part of the app rather than a floating
    // spotlight on the page background.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(x = size.width / 2f, y = size.height * 0.28f),
                            radius = size.width * 0.60f,
                        ),
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Covers with overlaid actions ──────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (coverStack.isNotEmpty()) {
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            CollectionPosterStack(coverStack = coverStack)
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onAdd) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.collection_add_item),
                                tint = accent,
                            )
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.collection_menu),
                                    tint = OmnilogColors.AppMuted,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.collection_reorder)) },
                                    onClick = {
                                        menuExpanded = false
                                        onReorder()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }

                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
                )

                if (itemCount > 0) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HeroStatChip(
                            text = stringResource(R.string.collection_item_count, itemCount),
                            accent = accent,
                            emphasized = false,
                        )
                        averageRating?.let { rating ->
                            HeroStatChip(
                                text = stringResource(R.string.collection_average_rating, rating),
                                accent = accent,
                                emphasized = true,
                            )
                        }
                    }

                    // Progress label + bar share a single row.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = progressSummary.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            GroupProgressBar(
                                fraction = progressSummary.progressFraction,
                                color = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionPosterStack(coverStack: List<String>) {
    val posterWidth = 68.dp
    val posterHeight = 102.dp
    val fanOffset = 30.dp
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(posterWidth + fanOffset * 2)
            .height(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Draw back posters first so the primary cover sits on top.
        coverStack.getOrNull(1)?.let { url ->
            MetadataCoverImage(
                coverUrl = url,
                modifier = Modifier
                    .offset(x = -fanOffset)
                    .rotate(-11f)
                    .shadow(elevation = 6.dp, shape = shape, clip = false)
                    .size(width = posterWidth, height = posterHeight),
                shape = shape,
            )
        }
        coverStack.getOrNull(2)?.let { url ->
            MetadataCoverImage(
                coverUrl = url,
                modifier = Modifier
                    .offset(x = fanOffset)
                    .rotate(11f)
                    .shadow(elevation = 6.dp, shape = shape, clip = false)
                    .size(width = posterWidth, height = posterHeight),
                shape = shape,
            )
        }
        MetadataCoverImage(
            coverUrl = coverStack.firstOrNull(),
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = shape, clip = false)
                .size(width = posterWidth, height = posterHeight),
            shape = shape,
        )
    }
}

@Composable
private fun HeroStatChip(
    text: String,
    accent: Color,
    emphasized: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (emphasized) accent.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
        border = BorderStroke(
            1.dp,
            if (emphasized) accent.copy(alpha = 0.44f) else OmnilogColors.AppLine,
        ),
        contentColor = if (emphasized) accent else OmnilogColors.AppMuted,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReorderHeaderBar(
    collectionName: String,
    accent: Color,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.field_collection),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Text(
                    text = collectionName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
            TextButton(onClick = onSave) {
                Text(text = stringResource(R.string.save), color = accent)
            }
        }
    }
}

@Composable
private fun MoveCollectionDialog(
    availableCollections: List<MediaCollection>,
    accent: Color,
    onDismiss: () -> Unit,
    onMove: (MediaCollection) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val trimmedQuery = query.trim()
    val visibleCollections = availableCollections.filter { availableCollection ->
        availableCollection.name.contains(trimmedQuery, ignoreCase = true)
    }
    val selectedCollection = availableCollections.firstOrNull { it.id == selectedCollectionId }

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.collection_move_item_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    selectedCollectionId = null
                },
                placeholder = { Text(stringResource(R.string.collection_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = omnilogModalTextFieldColors(accent),
            )

            when {
                availableCollections.isEmpty() -> Text(
                    text = stringResource(R.string.collection_move_item_empty),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.AppMuted,
                )
                visibleCollections.isEmpty() -> Text(
                    text = stringResource(R.string.collection_move_item_no_matches),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.AppMuted,
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 238.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(
                        items = visibleCollections,
                        key = { collection -> collection.id },
                    ) { targetCollection ->
                        CollectionMoveOptionRow(
                            collection = targetCollection,
                            selected = targetCollection.id == selectedCollectionId,
                            accent = accent,
                            onClick = {
                                selectedCollectionId = targetCollection.id
                                query = targetCollection.name
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    enabled = selectedCollection != null,
                    onClick = {
                        selectedCollection?.let(onMove)
                    },
                ) {
                    Text(text = stringResource(R.string.collection_move_item), color = accent)
                }
            }
        }
    }
}

@Composable
private fun CollectionMoveOptionRow(
    collection: MediaCollection,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.58f) else OmnilogColors.AppLine),
        contentColor = if (selected) accent else OmnilogColors.AppInk,
    ) {
        Text(
            text = collection.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CollectionItemCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onMediaClick: (TrackedMedia) -> Unit,
    onMoveClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    MediaCard(
        trackedMedia = trackedMedia,
        accent = accent,
        onClick = { onMediaClick(trackedMedia) },
        trailingAction = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.collection_item_menu),
                        tint = OmnilogColors.AppMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.collection_move_item)) },
                        onClick = {
                            menuExpanded = false
                            onMoveClick()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.collection_remove_item),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemoveClick()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun ReorderItemRow(
    trackedMedia: TrackedMedia,
    orderText: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    accent: Color,
    onOrderChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = orderText,
                onValueChange = onOrderChange,
                label = { Text("#") },
                modifier = Modifier.width(68.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ReorderNudgeControls(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                accent = accent,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
            )
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.size(width = 48.dp, height = 72.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = displayMediaTitle(trackedMedia.item.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                trackedMedia.item.creators.firstOrNull()?.let { creator ->
                    Text(
                        text = creator,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ReorderMetadataLine(trackedMedia = trackedMedia)
            }
        }
    }
}

@Composable
private fun ReorderMetadataLine(trackedMedia: TrackedMedia) {
    val statusLabel = trackedMedia.currentSession?.status?.let { status ->
        stringResource(status.labelRes())
    }
    val parts = buildList {
        trackedMedia.item.releaseYear?.let { add(it.toString()) }
        statusLabel?.let { add(it) }
        trackedMedia.currentSession?.rating?.let { add("$it/10") }
    }
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogColors.AppMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun TrackingStatus.labelRes(): Int = when (this) {
    TrackingStatus.Planned -> R.string.status_planned
    TrackingStatus.InProgress -> R.string.status_in_progress
    TrackingStatus.Completed -> R.string.status_completed
    TrackingStatus.Paused -> R.string.status_paused
    TrackingStatus.Dropped -> R.string.status_dropped
}

@Composable
private fun ReorderNudgeControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    accent: Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(
        modifier = Modifier.width(38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompactArrowButton(
            enabled = canMoveUp,
            accent = accent,
            onClick = onMoveUp,
            content = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                )
            },
        )
        CompactArrowButton(
            enabled = canMoveDown,
            accent = accent,
            onClick = onMoveDown,
            content = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                )
            },
        )
    }
}

@Composable
private fun CompactArrowButton(
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = OmnilogColors.AppLine.copy(alpha = if (enabled) 0.92f else 0.4f),
        contentColor = if (enabled) accent else OmnilogColors.AppMuted.copy(alpha = 0.36f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

private fun collectionItemComparator(): Comparator<TrackedMedia> =
    compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
        .thenBy { it.item.releaseYear ?: Int.MAX_VALUE }
        .thenBy { it.item.title.lowercase() }

private fun List<TrackedMedia>.toDraftOrderValues(): Map<Long, String> {
    return mapIndexed { index, trackedMedia ->
        trackedMedia.item.id to formatCollectionOrder(
            trackedMedia.item.collectionSortOrder ?: (index + 1).toDouble(),
        )
    }.toMap()
}

private fun List<TrackedMedia>.toSequentialDraftOrderValues(): Map<Long, String> {
    return mapIndexed { index, trackedMedia ->
        trackedMedia.item.id to (index + 1).toString()
    }.toMap()
}

private fun List<TrackedMedia>.moveItem(fromIndex: Int, toIndex: Int): List<TrackedMedia> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this

    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

private fun List<TrackedMedia>.toCollectionItemOrders(
    draftOrderValues: Map<Long, String>,
): List<CollectionItemOrder> {
    return mapIndexed { index, trackedMedia ->
        CollectionItemOrder(
            mediaItemId = trackedMedia.item.id,
            sortOrder = draftOrderValues[trackedMedia.item.id].toCollectionOrderOrNull()
                ?: trackedMedia.item.collectionSortOrder
                ?: (index + 1).toDouble(),
        )
    }
}

private fun String.toCollectionOrderInput(): String {
    val normalized = replace(',', '.')
    val builder = StringBuilder()
    var hasSeparator = false

    normalized.forEach { character ->
        when {
            character.isDigit() -> builder.append(character)
            character == '.' && !hasSeparator -> {
                builder.append(character)
                hasSeparator = true
            }
        }
    }

    return builder.toString().take(8)
}

private fun String?.toCollectionOrderOrNull(): Double? {
    return this
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it >= 0.0 }
}
