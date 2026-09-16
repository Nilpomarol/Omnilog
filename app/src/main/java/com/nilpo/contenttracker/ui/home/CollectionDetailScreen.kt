package com.nilpo.contenttracker.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import com.nilpo.contenttracker.ui.add.FloatingPrimaryAction
import com.nilpo.contenttracker.ui.add.FloatingSecondaryAction
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.detail.BarPaddingReclaim
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.detail.QuietDivider
import com.nilpo.contenttracker.ui.detail.QuietQuickAction
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlin.math.roundToInt

/** Room under the last row so it can scroll clear of the floating save and cancel buttons. */
private val FloatingActionClearance = 112.dp
private val ReorderRowHeight = 84.dp

// The scroll over which the transparent app bar earns its surface back, as on the item page.
private val BarFadeDistance = 48.dp

/**
 * A collection as the app's editorial pages draw it: stacked covers beside a serif title and how far
 * through it you are, one strong action to add to it and the list in its own order, each row carrying
 * its number.
 *
 * Reordering swaps the page for movable cards. Each card can be dragged by its handle, nudged a place
 * with its arrows or given an exact number, and nothing is stored until the floating save. Every
 * question the page asks comes up from the bottom as a sheet.
 */
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
    /** The app bar's title: empty on the page, which names itself, and the mode's name while reordering. */
    onTopBarTitleChange: (String) -> Unit = {},
    /** The app bar's height; the page scrolls under the bar, which draws its own surface. */
    topInset: Dp = 0.dp,
    /** How solid the transparent app bar should be: clear over the header, solid once it scrolls away. */
    onTopBarOpacityChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var nameText by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var showRenameSheet by rememberSaveable(collection.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }
    var showDiscardReorderConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }
    var isReordering by rememberSaveable(collection.id) { mutableStateOf(false) }
    var draftOrderValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var itemPendingRemoval by remember { mutableStateOf<TrackedMedia?>(null) }
    var itemPendingMove by remember { mutableStateOf<TrackedMedia?>(null) }
    var itemPendingPosition by remember { mutableStateOf<TrackedMedia?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val removeDoneMessage = stringResource(
        R.string.collection_remove_item_done,
        itemPendingRemoval?.item?.title?.let(::displayMediaTitle).orEmpty(),
    )
    val reorderTitle = stringResource(R.string.collection_reorder_title)
    val sortedItems = items.sortedWith(collectionItemComparator())
    // A function rather than a value so the drag callbacks, registered once per card, still read the
    // draft as it is at the moment of the gesture.
    val draftOrder: () -> List<TrackedMedia> = {
        sortedItems.sortedWith(
            compareBy<TrackedMedia> { trackedMedia ->
                draftOrderValues[trackedMedia.item.id].toCollectionOrderOrNull()
                    ?: trackedMedia.item.collectionSortOrder
                    ?: Double.MAX_VALUE
            }.thenBy { trackedMedia -> trackedMedia.item.releaseYear ?: Int.MAX_VALUE }
                .thenBy { trackedMedia -> trackedMedia.item.title.lowercase() },
        )
    }
    val displayedItems = if (isReordering) draftOrder() else sortedItems
    val hasUnsavedReorder = isReordering && draftOrderValues != sortedItems.toDraftOrderValues()
    val nextCollectionOrder = (items.maxOfOrNull { it.item.collectionSortOrder ?: 0.0 } ?: 0.0) + 1.0
    val showPositions = items.any { it.item.collectionSortOrder != null }

    val moveItem: (Int, Int) -> Unit = { fromIndex, toIndex ->
        draftOrderValues = draftOrder().moveItem(fromIndex, toIndex).toSequentialDraftOrderValues()
    }
    val startReorder = {
        draftOrderValues = sortedItems.toDraftOrderValues()
        isReordering = true
    }
    val stopReorder = {
        isReordering = false
        draggingId = null
        draftOrderValues = sortedItems.toDraftOrderValues()
    }
    val requestBack = {
        when {
            hasUnsavedReorder -> showDiscardReorderConfirmation = true
            isReordering -> stopReorder()
            else -> onBack()
        }
    }
    // The dragged card follows the finger; once it has travelled past half of its neighbour it takes
    // that neighbour's place, and the offset is corrected by the distance the swap just moved it.
    val dragBy: (Long, Float) -> Unit = { id, delta ->
        dragOffset += delta
        val order = draftOrder()
        val index = order.indexOfFirst { it.item.id == id }
        val layoutInfo = listState.layoutInfo
        val step = { neighbour: TrackedMedia? ->
            layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == neighbour?.item?.id }
                ?.let { it.size + layoutInfo.mainAxisItemSpacing }
        }
        val nextStep = step(order.getOrNull(index + 1))
        val previousStep = step(order.getOrNull(index - 1))
        if (nextStep != null && dragOffset > nextStep / 2f) {
            moveItem(index, index + 1)
            dragOffset -= nextStep
        } else if (previousStep != null && dragOffset < -previousStep / 2f) {
            moveItem(index, index - 1)
            dragOffset += previousStep
        }
    }

    val latestRequestBack by rememberUpdatedState(requestBack)
    DisposableEffect(collection.id) {
        onRegisterBackRequest { latestRequestBack() }
        onDispose {
            onRegisterBackRequest(null)
            onTopBarTitleChange("")
        }
    }
    SideEffect {
        onTopBarTitleChange(if (isReordering) reorderTitle else "")
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
    TopBarOpacityEffect(listState = listState, solid = isReordering, onOpacityChange = onTopBarOpacityChange)
    BackHandler(enabled = isReordering) {
        requestBack()
    }

    Box(modifier = modifier.background(OmnilogTheme.colors.appBackground)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                // The page opens tucked up under the bar by as much as the item page does, so both start at
                // the same height; reorder cards start clear of it.
                top = if (isReordering) topInset + 4.dp else topInset - BarPaddingReclaim,
                bottom = if (isReordering) FloatingActionClearance else 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (isReordering) 8.dp else 0.dp),
        ) {
            if (isReordering) {
                item(key = "reorder_intro") {
                    ReorderIntro(
                        accent = accent,
                        canSortByYear = items.count { it.item.releaseYear != null } > 1,
                        onSortByYear = {
                            draftOrderValues = draftOrder()
                                .sortedBy { it.item.releaseYear ?: Int.MAX_VALUE }
                                .toSequentialDraftOrderValues()
                        },
                    )
                }
                itemsIndexed(
                    items = displayedItems,
                    key = { _, trackedMedia -> trackedMedia.item.id },
                ) { index, trackedMedia ->
                    val id = trackedMedia.item.id
                    val dragging = draggingId == id
                    ReorderItemRow(
                        trackedMedia = trackedMedia,
                        position = draftOrderValues[id].orEmpty(),
                        canMoveUp = index > 0,
                        canMoveDown = index < displayedItems.lastIndex,
                        dragging = dragging,
                        accent = accent,
                        onDragStart = {
                            draggingId = id
                            dragOffset = 0f
                        },
                        onDrag = { delta -> dragBy(id, delta) },
                        onDragEnd = {
                            draggingId = null
                            dragOffset = 0f
                        },
                        onMoveUp = { moveItem(index, index - 1) },
                        onMoveDown = { moveItem(index, index + 1) },
                        onPositionClick = { itemPendingPosition = trackedMedia },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            // The held card rides the finger above the others; the rest slide
                            // into the gaps it leaves.
                            .then(
                                if (dragging) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer { translationY = dragOffset }
                                } else {
                                    Modifier.animateItem()
                                },
                            ),
                    )
                }
            } else {
                item(key = "header") {
                    CollectionHeader(
                        collection = collection,
                        items = sortedItems,
                        accent = accent,
                        averageRating = items.collectionAverageRating(),
                        progress = items.takeIf { it.isNotEmpty() }?.collectionProgressSummary(),
                    )
                }
                item(key = "actions") {
                    CollectionActions(
                        canReorder = items.size > 1,
                        accent = accent,
                        onAdd = { onAddToCollection(collection, nextCollectionOrder) },
                        onReorder = startReorder,
                        onRename = { showRenameSheet = true },
                        onDelete = { showDeleteConfirmation = true },
                        modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 20.dp),
                    )
                }
                item(key = "items_title") {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = DetailGutter),
                            color = OmnilogTheme.colors.appLine,
                        )
                        DetailSectionTitle(
                            text = stringResource(R.string.collection_items_section),
                            modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 20.dp),
                        )
                    }
                }
                if (items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.collection_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogTheme.colors.appMuted,
                            modifier = Modifier.padding(start = DetailGutter, top = 12.dp, end = DetailGutter),
                        )
                    }
                } else {
                    items(
                        items = sortedItems,
                        key = { trackedMedia -> trackedMedia.item.id },
                    ) { trackedMedia ->
                        Row(
                            modifier = Modifier.padding(
                                start = if (showPositions) 12.dp else DetailGutter,
                                end = DetailGutter,
                                top = 12.dp,
                            ),
                        ) {
                            if (showPositions) {
                                PositionNumeral(trackedMedia.item.collectionSortOrder)
                            }
                            Box(modifier = Modifier.weight(1f)) {
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

        if (isReordering) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = DetailGutter, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingSecondaryAction(
                    text = stringResource(R.string.cancel),
                    onClick = requestBack,
                )
                FloatingPrimaryAction(
                    text = stringResource(
                        if (hasUnsavedReorder) R.string.collection_reorder_save else R.string.collection_reorder_no_changes,
                    ),
                    icon = Icons.Filled.Check,
                    enabled = hasUnsavedReorder,
                    onClick = {
                        onUpdateCollectionItemOrder(
                            collection.id,
                            displayedItems.toCollectionItemOrders(draftOrderValues),
                        )
                        isReordering = false
                    },
                )
            }
        }
    }

    if (showRenameSheet) {
        val closeRename = {
            showRenameSheet = false
            nameText = collection.name
        }
        CollectionSheet(
            title = stringResource(R.string.collection_rename_title),
            confirmText = stringResource(R.string.save),
            confirmEnabled = nameText.isNotBlank(),
            onDismiss = closeRename,
            onConfirm = {
                onRenameCollection(collection.id, nameText.trim())
                showRenameSheet = false
            },
        ) {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text(stringResource(R.string.field_collection)) },
                singleLine = true,
                colors = omnilogModalTextFieldColors(accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDeleteConfirmation) {
        CollectionSheet(
            title = stringResource(R.string.delete_collection_title),
            message = stringResource(R.string.delete_collection_message, collection.name),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                onDeleteCollection(collection.id)
                showDeleteConfirmation = false
                onBack()
            },
        )
    }

    if (showDiscardReorderConfirmation) {
        CollectionSheet(
            title = stringResource(R.string.collection_unsaved_reorder_title),
            message = stringResource(R.string.collection_unsaved_reorder_message),
            confirmText = stringResource(R.string.discard),
            destructive = true,
            onDismiss = { showDiscardReorderConfirmation = false },
            onConfirm = {
                showDiscardReorderConfirmation = false
                stopReorder()
            },
        )
    }

    itemPendingPosition?.let { trackedMedia ->
        PositionSheet(
            trackedMedia = trackedMedia,
            initialValue = draftOrderValues[trackedMedia.item.id].orEmpty(),
            accent = accent,
            onDismiss = { itemPendingPosition = null },
            onConfirm = { value ->
                draftOrderValues = draftOrderValues + (trackedMedia.item.id to value)
                itemPendingPosition = null
            },
        )
    }

    itemPendingRemoval?.let { trackedMedia ->
        CollectionSheet(
            title = stringResource(R.string.collection_remove_item),
            message = stringResource(
                R.string.collection_remove_item_message,
                displayMediaTitle(trackedMedia.item.title),
            ),
            confirmText = stringResource(R.string.collection_remove_item),
            destructive = true,
            onDismiss = { itemPendingRemoval = null },
            onConfirm = {
                onUpdateMediaItemCollection(trackedMedia, null, null)
                onCollectionActionMessage(removeDoneMessage)
                itemPendingRemoval = null
            },
        )
    }

    itemPendingMove?.let { trackedMedia ->
        val targetCollections = trackedMedia.availableCollections
            .filter { availableCollection -> availableCollection.id != collection.id }
        MoveCollectionSheet(
            itemTitle = displayMediaTitle(trackedMedia.item.title),
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

// ─────────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────────

/**
 * The collection's identity, laid out like an item's: its covers stacked where an item's single cover
 * sits, then an accent overline, the serif name, how many and which years, the average score and,
 * closing the column, how far through it you are.
 */
@Composable
private fun CollectionHeader(
    collection: MediaCollection,
    items: List<TrackedMedia>,
    accent: Color,
    averageRating: Double?,
    progress: CollectionProgressSummary?,
) {
    EditorialPageHeader(
        overline = stringResource(R.string.collection_modal_title),
        title = collection.name,
        items = items,
        accent = accent,
        averageRating = averageRating,
        image = if (items.isEmpty()) {
            null
        } else {
            {
                CollectionCoverStack(
                    coverStack = items.collectionCoverStack(),
                    itemCount = items.size,
                    modifier = Modifier.size(width = 128.dp, height = 192.dp),
                )
            }
        },
    ) {
        progress?.let { summary ->
            CollectionProgress(
                summary = summary,
                accent = accent,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * The head of a page about a group of works — a collection, an author, a studio — in the item page's
 * layout: the group's picture where an item's cover sits, then an accent overline, the serif name, how
 * many works and which years, the average score and whatever else the page adds under them.
 */
@Composable
internal fun EditorialPageHeader(
    overline: String,
    title: String,
    items: List<TrackedMedia>,
    accent: Color,
    averageRating: Double?,
    image: (@Composable () -> Unit)?,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val years = items.mapNotNull { it.item.releaseYear }
    val yearRange = when {
        years.isEmpty() -> null
        years.min() == years.max() -> years.min().toString()
        else -> "${years.min()}–${years.max()}"
    }
    val facts = listOfNotNull(
        stringResource(R.string.collection_item_count, items.size),
        yearRange,
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        image?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = overline.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = accent,
            )
            BasicText(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 1.22.em,
                    color = OmnilogTheme.colors.appInk,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 18.sp,
                    maxFontSize = 26.sp,
                    stepSize = 1.sp,
                ),
            )
            Text(
                text = facts,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            averageRating?.let { rating ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent,
                    )
                    Text(
                        text = stringResource(R.string.collection_average_short, rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appInk,
                    )
                }
            }
            extra()
        }
    }
}

/** How much of the collection is done, as a line of text over the same bar the library uses. */
@Composable
private fun CollectionProgress(
    summary: CollectionProgressSummary,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = summary.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${(summary.progressFraction.coerceIn(0f, 1f) * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        GroupProgressBar(fraction = summary.progressFraction, color = accent)
    }
}

/**
 * The detail page's action row: adding takes the room, and reordering, renaming and deleting sit
 * beside it as quiet captioned icons, so none of them hides behind a menu.
 */
@Composable
private fun CollectionActions(
    canReorder: Boolean,
    accent: Color,
    onAdd: () -> Unit,
    onReorder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onAdd,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.collection_add_item),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        QuietDivider(modifier = Modifier.padding(start = 8.dp))
        if (canReorder) {
            QuietQuickAction(
                text = stringResource(R.string.collection_reorder),
                icon = painterResource(R.drawable.ic_reorder),
                accent = accent,
                selected = false,
                stacked = true,
                onClick = onReorder,
            )
            QuietDivider()
        }
        QuietQuickAction(
            text = stringResource(R.string.edit),
            icon = rememberVectorPainter(Icons.Filled.Edit),
            accent = accent,
            selected = false,
            stacked = true,
            onClick = onRename,
        )
        QuietDivider()
        QuietQuickAction(
            text = stringResource(R.string.delete),
            icon = rememberVectorPainter(Icons.Filled.Delete),
            accent = accent,
            selected = false,
            stacked = true,
            onClick = onDelete,
        )
    }
}

/** A row's place in the collection, set in the margin like a chapter number. */
@Composable
private fun PositionNumeral(sortOrder: Double?) {
    Text(
        text = sortOrder?.let(::formatCollectionOrder).orEmpty(),
        modifier = Modifier
            .width(28.dp)
            .padding(top = 2.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily),
        color = OmnilogTheme.colors.appMuted,
        maxLines = 1,
    )
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
        showCollection = false,
        trailingAction = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.collection_item_menu),
                        tint = OmnilogTheme.colors.appMuted,
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

// ─────────────────────────────────────────────────────────────
// Sheets
// ─────────────────────────────────────────────────────────────

/**
 * Every question this page asks, in one shape: a serif title and what it means, whatever the answer
 * needs, and the one button that commits it — red when it takes something away. Swiping the sheet
 * away is the way out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionSheet(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = OmnilogTheme.colors.appInk,
                )
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
            content()
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** An exact place for one item, decimals allowed, applied to the draft rather than saved. */
@Composable
private fun PositionSheet(
    trackedMedia: TrackedMedia,
    initialValue: String,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(trackedMedia.item.id) { mutableStateOf(initialValue.replace('.', ',')) }
    val parsed = value.toCollectionOrderInput()

    CollectionSheet(
        title = stringResource(R.string.collection_sheet_position),
        message = displayMediaTitle(trackedMedia.item.title),
        confirmText = stringResource(R.string.save),
        confirmEnabled = parsed.toCollectionOrderOrNull() != null,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(parsed) },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                value = input.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
            },
            label = { Text(stringResource(R.string.collection_position_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = omnilogModalTextFieldColors(accent),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(text = stringResource(R.string.collection_position_help))
            },
        )
    }
}

/** Where else the item can go: a search over the other collections as a list with a tick on the pick. */
@Composable
private fun MoveCollectionSheet(
    itemTitle: String,
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

    CollectionSheet(
        title = stringResource(R.string.collection_move_item_title),
        message = itemTitle,
        confirmText = stringResource(R.string.collection_move_item),
        confirmEnabled = selectedCollection != null,
        onDismiss = onDismiss,
        onConfirm = { selectedCollection?.let(onMove) },
    ) {
        if (availableCollections.isEmpty()) {
            Text(
                text = stringResource(R.string.collection_move_item_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            return@CollectionSheet
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.collection_search)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = omnilogModalTextFieldColors(accent),
        )
        if (visibleCollections.isEmpty()) {
            Text(
                text = stringResource(R.string.collection_move_item_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                itemsIndexed(
                    items = visibleCollections,
                    key = { _, collection -> collection.id },
                ) { index, targetCollection ->
                    if (index > 0) HorizontalDivider(color = OmnilogTheme.colors.appLine)
                    val selected = targetCollection.id == selectedCollectionId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { selectedCollectionId = targetCollection.id }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = targetCollection.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) accent else OmnilogTheme.colors.appInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reordering
// ─────────────────────────────────────────────────────────────

/**
 * The three ways to move a card, side by side in one small panel, each over the very mark it refers
 * to, plus a one-tap sort by year for the common case of a series in release order.
 */
@Composable
private fun ReorderIntro(
    accent: Color,
    canSortByYear: Boolean,
    onSortByYear: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OmnilogTheme.colors.appLine, shape)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            // Top-aligned so the marks share one line even when a caption wraps to a second.
            verticalAlignment = Alignment.Top,
        ) {
            ReorderHint(
                text = stringResource(R.string.collection_reorder_hint_drag),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = OmnilogTheme.colors.appMuted,
                )
            }
            VerticalDivider(modifier = Modifier.height(24.dp).align(Alignment.CenterVertically), color = OmnilogTheme.colors.appLine)
            ReorderHint(
                text = stringResource(R.string.collection_reorder_hint_arrows),
                modifier = Modifier.weight(1f),
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent,
                    )
                }
            }
            VerticalDivider(modifier = Modifier.height(24.dp).align(Alignment.CenterVertically), color = OmnilogTheme.colors.appLine)
            ReorderHint(
                text = stringResource(R.string.collection_reorder_hint_position),
                modifier = Modifier.weight(1f),
            ) {
                PositionChipFace(text = "1", accent = accent, compact = true)
            }
        }
        if (canSortByYear) {
            TextButton(
                onClick = onSortByYear,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reorder),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.collection_reorder_by_year),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun ReorderHint(
    text: String,
    modifier: Modifier = Modifier,
    mark: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            mark()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A movable card: the grip on the left for dragging, the position number that opens an exact entry,
 * the cover and title, and a pair of arrows for one step at a time — the arrows also being the way to
 * reorder without dragging at all.
 */
@Composable
private fun ReorderItemRow(
    trackedMedia: TrackedMedia,
    position: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragging: Boolean,
    accent: Color,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPositionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = displayMediaTitle(trackedMedia.item.title)
    val shownPosition = position.replace('.', ',')
    val shape = RoundedCornerShape(14.dp)
    val elevation by animateDpAsState(targetValue = if (dragging) 10.dp else 0.dp, label = "reorderLift")
    // The gesture is registered once per card, so it reaches the callbacks through these.
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ReorderRowHeight)
            .shadow(elevation = elevation, shape = shape)
            .background(OmnilogTheme.colors.appPanel, shape)
            .border(
                width = if (dragging) 1.5.dp else 1.dp,
                color = if (dragging) accent else OmnilogTheme.colors.appLine,
                shape = shape,
            )
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(ReorderRowHeight)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { latestOnDragStart() },
                        onDragEnd = { latestOnDragEnd() },
                        onDragCancel = { latestOnDragEnd() },
                    ) { change, dragAmount ->
                        change.consume()
                        latestOnDrag(dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = if (dragging) accent else OmnilogTheme.colors.appMuted,
            )
        }
        val positionDescription = stringResource(R.string.collection_position_edit, title, shownPosition)
        Surface(
            onClick = onPositionClick,
            modifier = Modifier.semantics { contentDescription = positionDescription },
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
        ) {
            PositionChipFace(text = shownPosition, accent = accent)
        }
        MetadataCoverImage(
            coverUrl = trackedMedia.item.coverUrl,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 40.dp, height = 60.dp),
            shape = RoundedCornerShape(4.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ReorderMetadataLine(trackedMedia = trackedMedia)
        }
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.collection_move_up, title),
                tint = if (canMoveUp) accent else OmnilogTheme.colors.appLine,
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.collection_move_down, title),
                tint = if (canMoveDown) accent else OmnilogTheme.colors.appLine,
            )
        }
    }
}

/** The tinted number tile, shared by the cards and the hint that explains them. */
@Composable
private fun PositionChipFace(
    text: String,
    accent: Color,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 7.dp else 10.dp)
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = if (compact) 24.dp else 44.dp,
                minHeight = if (compact) 24.dp else 44.dp,
            )
            .background(accent.copy(alpha = 0.12f), shape)
            .border(1.dp, accent.copy(alpha = 0.4f), shape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium)
                .copy(fontFamily = SerifFontFamily),
            color = accent,
            maxLines = 1,
        )
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
        trackedMedia.currentSession?.ratingHalfPoints?.let { add(formatRatingHalfPoints(it)) }
    }
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = OmnilogTheme.colors.appMuted,
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

// ─────────────────────────────────────────────────────────────
// Order helpers
// ─────────────────────────────────────────────────────────────

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

/**
 * Keeps a transparent app bar readable over a page that starts under it: clear while the header sits
 * beneath, gaining its surface over the first stretch of scroll, and solid once the header has gone
 * or whenever the page asks for it.
 */
@Composable
internal fun TopBarOpacityEffect(
    listState: LazyListState,
    solid: Boolean,
    onOpacityChange: (Float) -> Unit,
) {
    val fadeDistancePx = with(LocalDensity.current) { BarFadeDistance.toPx() }
    val latestSolid by rememberUpdatedState(solid)
    LaunchedEffect(listState) {
        snapshotFlow {
            if (latestSolid || listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
            }
        }.collect { onOpacityChange(it) }
    }
}
