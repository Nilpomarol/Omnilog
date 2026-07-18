package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

data class CollectionMemberPreview(
    val title: String,
    val order: Double?,
)

data class CollectionPickerOption(
    val collection: MediaCollection,
    val itemCount: Int,
    val coverStack: List<String>,
    val members: List<CollectionMemberPreview>,
    val nextOrder: Double,
    val isOrdered: Boolean,
)

data class CollectionPickerResult(
    val collectionId: Long?,
    val newCollectionName: String?,
    val sortOrder: Double?,
)

fun List<TrackedMedia>.toCollectionPickerOptions(forType: MediaType? = null): List<CollectionPickerOption> {
    return filter { it.collection != null }
        .filter { forType == null || it.item.type == forType }
        .groupBy { it.collection!!.id }
        .mapNotNull { (_, items) ->
            val collection = items.firstNotNullOfOrNull { it.collection } ?: return@mapNotNull null
            val sorted = items.sortedWith(
                compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
                    .thenBy { it.item.title.lowercase() },
            )
            val orders = items.mapNotNull { it.item.collectionSortOrder }
            CollectionPickerOption(
                collection = collection,
                itemCount = items.size,
                coverStack = sorted.mapNotNull { it.item.coverUrl }.take(3),
                members = sorted.map { CollectionMemberPreview(it.item.title, it.item.collectionSortOrder) },
                nextOrder = (orders.maxOrNull() ?: 0.0) + 1.0,
                isOrdered = orders.isNotEmpty(),
            )
        }
        .sortedBy { it.collection.name.lowercase() }
}

@Composable
fun CollectionEntryChip(
    label: String,
    hasSelection: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (hasSelection) accent.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        border = BorderStroke(
            width = if (hasSelection) 1.5.dp else 1.dp,
            color = if (hasSelection) accent.copy(alpha = 0.7f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = accent.copy(alpha = 0.18f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasSelection) accent else OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    itemTitle: String,
    providerCollectionTitle: String?,
    options: List<CollectionPickerOption>,
    initialCollectionId: Long?,
    initialSortOrder: Double?,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (CollectionPickerResult) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val collections = remember(options) { options.map { it.collection } }
    val initialName = remember(initialCollectionId, options) {
        options.firstOrNull { it.collection.id == initialCollectionId }?.collection?.name.orEmpty()
    }

    var step by rememberSaveable(initialCollectionId) { mutableStateOf(1) }
    var query by rememberSaveable(initialCollectionId) { mutableStateOf(initialName) }
    var selectedCollectionId by rememberSaveable(initialCollectionId) { mutableStateOf(initialCollectionId) }
    var orderText by rememberSaveable(initialCollectionId) {
        mutableStateOf(initialSortOrder?.let(::formatCollectionOrder).orEmpty())
    }
    var unordered by rememberSaveable(initialCollectionId) { mutableStateOf(false) }

    val trimmedQuery = query.trim()
    val autoMatch = remember(options, providerCollectionTitle, itemTitle) {
        collections.bestCollectionMatch(providerCollectionTitle ?: itemTitle)
    }
    val queryMatch = if (trimmedQuery.isBlank()) null else collections.bestCollectionMatch(trimmedQuery)
    val selectedOption = options.firstOrNull { it.collection.id == selectedCollectionId }
    val targetOption = selectedOption
        ?: queryMatch?.let { match -> options.firstOrNull { it.collection.id == match.id } }
    val isNewCollection = targetOption == null && trimmedQuery.isNotBlank()
    val hasTarget = targetOption != null || isNewCollection

    val quickSuggestions = if (trimmedQuery.isBlank()) {
        buildCollectionQuickSuggestions(
            availableCollections = collections,
            providerCollectionTitle = providerCollectionTitle,
            itemTitle = itemTitle,
        )
    } else {
        emptyList()
    }
    val filteredOptions = if (quickSuggestions.isEmpty()) {
        options.filter { trimmedQuery.isBlank() || it.collection.name.contains(trimmedQuery, ignoreCase = true) }
    } else {
        emptyList()
    }

    fun advanceOrConfirm() {
        val target = targetOption
        if (target != null && target.isOrdered) {
            orderText = if (target.collection.id == initialCollectionId && initialSortOrder != null) {
                formatCollectionOrder(initialSortOrder)
            } else {
                formatCollectionOrder(target.nextOrder)
            }
            unordered = false
            step = 2
        } else {
            onConfirm(
                CollectionPickerResult(
                    collectionId = target?.collection?.id,
                    newCollectionName = if (isNewCollection) trimmedQuery else null,
                    sortOrder = null,
                ),
            )
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
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step == 1) {
                StepOneContent(
                    itemTitle = itemTitle,
                    query = query,
                    onQueryChange = {
                        query = it
                        selectedCollectionId = null
                    },
                    accent = accent,
                    autoMatch = autoMatch,
                    onAutoMatchClick = { matched ->
                        query = matched.name
                        selectedCollectionId = matched.id
                    },
                    quickSuggestions = quickSuggestions,
                    onQuickSuggestionClick = { name ->
                        query = name
                        selectedCollectionId = collections.firstOrNull { it.name == name }?.id
                    },
                    filteredOptions = filteredOptions,
                    selectedCollectionId = selectedCollectionId,
                    onOptionClick = { option ->
                        selectedCollectionId = option.collection.id
                        query = option.collection.name
                    },
                    isNewCollection = isNewCollection,
                    newCollectionName = trimmedQuery,
                    hasInitialSelection = initialCollectionId != null,
                    onClearClick = {
                        onConfirm(CollectionPickerResult(null, null, null))
                    },
                    primaryEnabled = hasTarget,
                    primaryIsNext = targetOption?.isOrdered == true,
                    onPrimaryClick = { advanceOrConfirm() },
                )
            } else {
                val target = targetOption
                if (target == null) {
                    step = 1
                } else {
                    StepTwoContent(
                        itemTitle = itemTitle,
                        target = target,
                        orderText = orderText,
                        onOrderTextChange = { orderText = it.toCollectionOrderInput() },
                        unordered = unordered,
                        onUnorderedChange = { unordered = it },
                        accent = accent,
                        onBack = { step = 1 },
                        onConfirm = {
                            onConfirm(
                                CollectionPickerResult(
                                    collectionId = target.collection.id,
                                    newCollectionName = null,
                                    sortOrder = if (unordered) null else orderText.toCollectionOrderOrNull(),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOneContent(
    itemTitle: String,
    query: String,
    onQueryChange: (String) -> Unit,
    accent: Color,
    autoMatch: MediaCollection?,
    onAutoMatchClick: (MediaCollection) -> Unit,
    quickSuggestions: List<CollectionQuickSuggestion>,
    onQuickSuggestionClick: (String) -> Unit,
    filteredOptions: List<CollectionPickerOption>,
    selectedCollectionId: Long?,
    onOptionClick: (CollectionPickerOption) -> Unit,
    isNewCollection: Boolean,
    newCollectionName: String,
    hasInitialSelection: Boolean,
    onClearClick: () -> Unit,
    primaryEnabled: Boolean,
    primaryIsNext: Boolean,
    onPrimaryClick: () -> Unit,
) {
    Text(
        text = stringResource(R.string.collection_sheet_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = OmnilogTheme.colors.appInk,
    )
    Text(
        text = stringResource(R.string.collection_sheet_adding, itemTitle),
        style = MaterialTheme.typography.bodySmall,
        color = OmnilogTheme.colors.appMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.collection_sheet_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = omnilogModalTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )

    if (query.isBlank() && autoMatch != null) {
        Surface(
            onClick = { onAutoMatchClick(autoMatch) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
        ) {
            Text(
                text = stringResource(R.string.collection_sheet_match, autoMatch.name),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
    }

    if (quickSuggestions.isNotEmpty()) {
        SectionLabel(stringResource(R.string.collection_sheet_suggested))
        quickSuggestions.forEach { suggestion ->
            CollectionRow(
                title = suggestion.name,
                subtitle = stringResource(suggestion.labelResId),
                coverStack = emptyList(),
                selected = false,
                accent = accent,
                onClick = { onQuickSuggestionClick(suggestion.name) },
            )
        }
    }

    filteredOptions.forEach { option ->
        CollectionRow(
            title = option.collection.name,
            subtitle = stringResource(R.string.collection_item_count, option.itemCount),
            coverStack = option.coverStack,
            selected = option.collection.id == selectedCollectionId,
            accent = accent,
            onClick = { onOptionClick(option) },
        )
    }

    if (isNewCollection) {
        CollectionRow(
            title = stringResource(R.string.collection_create_from_search, newCollectionName),
            subtitle = null,
            coverStack = emptyList(),
            selected = true,
            accent = accent,
            leadingIcon = Icons.Filled.Add,
            onClick = { },
        )
    }

    if (hasInitialSelection) {
        TextButton(onClick = onClearClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.clear_collection), color = OmnilogTheme.colors.appMuted)
        }
    }

    Button(
        onClick = onPrimaryClick,
        enabled = primaryEnabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = if (primaryIsNext) {
                stringResource(R.string.collection_sheet_next)
            } else {
                stringResource(R.string.collection_sheet_confirm)
            },
            fontWeight = FontWeight.ExtraBold,
        )
        if (primaryIsNext) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepTwoContent(
    itemTitle: String,
    target: CollectionPickerOption,
    orderText: String,
    onOrderTextChange: (String) -> Unit,
    unordered: Boolean,
    onUnorderedChange: (Boolean) -> Unit,
    accent: Color,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(9.dp),
            color = OmnilogTheme.colors.appPanel,
            border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.back),
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.padding(4.dp).size(22.dp),
            )
        }
        Column {
            Text(
                text = target.collection.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.collection_sheet_adding, itemTitle),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    SectionLabel(stringResource(R.string.field_collection_order))

    if (!unordered) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton(symbol = "−", accent = accent) {
                val current = orderText.toCollectionOrderOrNull() ?: target.nextOrder
                val next = (current - 1.0).coerceAtLeast(0.0)
                onOrderTextChange(formatCollectionOrder(next))
            }
            OutlinedTextField(
                value = orderText,
                onValueChange = onOrderTextChange,
                modifier = Modifier.width(96.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = omnilogModalTextFieldColors(accent),
                shape = RoundedCornerShape(12.dp),
            )
            StepperButton(symbol = "+", accent = accent) {
                val current = orderText.toCollectionOrderOrNull() ?: (target.nextOrder - 1.0)
                onOrderTextChange(formatCollectionOrder(current + 1.0))
            }
            if (orderText.toCollectionOrderOrNull() == target.nextOrder) {
                Text(
                    text = stringResource(R.string.collection_sheet_next_slot),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = unordered,
            onCheckedChange = onUnorderedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.4f)),
        )
        Text(
            text = stringResource(R.string.collection_sheet_unordered),
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
    }

    if (!unordered) {
        PositionPreview(
            itemTitle = itemTitle,
            members = target.members,
            newOrder = orderText.toCollectionOrderOrNull(),
            accent = accent,
        )
    }

    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(stringResource(R.string.collection_sheet_confirm), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PositionPreview(
    itemTitle: String,
    members: List<CollectionMemberPreview>,
    newOrder: Double?,
    accent: Color,
) {
    data class PreviewRow(val title: String, val order: Double?, val isNew: Boolean)

    val rows = remember(members, newOrder, itemTitle) {
        (members.map { PreviewRow(it.title, it.order, false) } +
            PreviewRow(itemTitle, newOrder, true))
            .sortedWith(compareBy({ it.order ?: Double.MAX_VALUE }, { it.title.lowercase() }))
            .take(8)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appBackground,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (row.isNew) Modifier.padding(horizontal = 4.dp) else Modifier,
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (row.isNew) {
                                    Modifier.background(
                                        accent.copy(alpha = 0.14f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = row.order?.let(::formatCollectionOrder) ?: "–",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (row.isNew) accent else OmnilogTheme.colors.appMuted,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (row.isNew) FontWeight.Bold else FontWeight.Normal,
                            color = if (row.isNew) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (row.isNew) {
                            Text(
                                text = stringResource(R.string.collection_sheet_adding_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(text = symbol, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = OmnilogTheme.colors.appMuted,
    )
}

@Composable
private fun CollectionRow(
    title: String,
    subtitle: String?,
    coverStack: List<String>,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.7f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            when {
                leadingIcon != null -> {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.16f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(leadingIcon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                coverStack.isNotEmpty() -> {
                    Row {
                        coverStack.forEachIndexed { index, url ->
                            MetadataCoverImage(
                                coverUrl = url,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(width = 22.dp, height = 30.dp),
                            )
                            if (index < coverStack.lastIndex) Spacer(Modifier.width(2.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) accent else OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}
