package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun CollectionDetailScreen(
    collection: MediaCollection,
    items: List<TrackedMedia>,
    accent: Color,
    onBack: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onUpdateCollectionItemOrder: (Long, List<CollectionItemOrder>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameText by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var showRenameDialog by rememberSaveable(collection.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }
    var isReordering by rememberSaveable(collection.id) { mutableStateOf(false) }
    var draftOrderValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var menuExpanded by remember { mutableStateOf(false) }
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

    // Keep nameText in sync if the collection name changes externally (e.g. after a save)
    LaunchedEffect(collection.id, collection.name) {
        nameText = collection.name
    }
    LaunchedEffect(collection.id, sortedItems.map { it.item.id }) {
        if (!isReordering) {
            draftOrderValues = sortedItems.toDraftOrderValues()
        }
    }

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (isReordering) {
                            isReordering = false
                            draftOrderValues = sortedItems.toDraftOrderValues()
                        } else {
                            onBack()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = OmnilogColors.AppInk,
                    )
                }

                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                )

                if (isReordering) {
                    TextButton(
                        onClick = {
                            isReordering = false
                            draftOrderValues = sortedItems.toDraftOrderValues()
                        },
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onUpdateCollectionItemOrder(
                                collection.id,
                                displayedItems.toCollectionItemOrders(draftOrderValues),
                            )
                            isReordering = false
                        },
                    ) {
                        Text(text = stringResource(R.string.save), color = accent)
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
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
                                    showRenameDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.collection_reorder)) },
                                onClick = {
                                    menuExpanded = false
                                    draftOrderValues = sortedItems.toDraftOrderValues()
                                    isReordering = true
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
                                    showDeleteConfirmation = true
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = OmnilogColors.AppLine)

            // ── Item list ──────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.collection_item_count, items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                    )
                }

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
                                    draftOrderValues = draftOrderValues + (
                                        trackedMedia.item.id to displayedItems
                                            .suggestOrderBetween(
                                                beforeIndex = index - 2,
                                                afterIndex = index - 1,
                                                draftOrderValues = draftOrderValues,
                                            )
                                        )
                                },
                                onMoveDown = {
                                    draftOrderValues = draftOrderValues + (
                                        trackedMedia.item.id to displayedItems
                                            .suggestOrderBetween(
                                                beforeIndex = index + 1,
                                                afterIndex = index + 2,
                                                draftOrderValues = draftOrderValues,
                                            )
                                        )
                                },
                            )
                        } else {
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
    }

    // ── Rename dialog ──────────────────────────────────────────────────────
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                nameText = collection.name
            },
            title = { Text(text = stringResource(R.string.collection_rename_title)) },
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.delete_collection_title)) },
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

private fun List<TrackedMedia>.suggestOrderBetween(
    beforeIndex: Int,
    afterIndex: Int,
    draftOrderValues: Map<Long, String>,
): String {
    val beforeItem = getOrNull(beforeIndex)
    val afterItem = getOrNull(afterIndex)
    val before = beforeItem?.let { trackedMedia ->
        draftOrderValues[trackedMedia.item.id].toCollectionOrderOrNull()
            ?: trackedMedia.item.collectionSortOrder
    }
    val after = afterItem?.let { trackedMedia ->
        draftOrderValues[trackedMedia.item.id].toCollectionOrderOrNull()
            ?: trackedMedia.item.collectionSortOrder
    }
    val suggestion = when {
        before != null && after != null -> (before + after) / 2.0
        before != null -> before + 1.0
        after != null -> (after - 1.0).coerceAtLeast(0.0)
        else -> 0.0
    }
    return formatCollectionOrder(suggestion)
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
