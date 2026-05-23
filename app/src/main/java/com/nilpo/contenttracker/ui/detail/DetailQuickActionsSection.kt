package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ExternalTracking
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun DetailQuickActionsSection(
    item: MediaItem,
    collection: MediaCollection?,
    availableCollections: List<MediaCollection>,
    currentSession: TrackingSession?,
    externalTracking: List<ExternalTracking>,
    accent: Color,
    onSaveItemDetails: (String, Long?, String?, Double?, Int?, OwnershipType) -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    onAddExternalTracking: (ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTrackingSynced: (Long, Boolean) -> Unit,
) {
    var showCollectionDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showNewSessionDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showExternalTrackingDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    val hasExternalTracking = externalTracking.isNotEmpty()
    val isExternalTrackingSynced = hasExternalTracking && externalTracking.all { it.isSynced }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionButton(
                text = if (item.ownership.isOwned) {
                    stringResource(R.string.owned_label)
                } else {
                    stringResource(R.string.owned_action_add)
                },
                accent = accent,
                selected = item.ownership.isOwned,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSaveItemDetails(
                        item.title,
                        item.collectionId,
                        null,
                        item.collectionSortOrder,
                        item.effectiveProgressTotal(),
                        if (item.ownership.isOwned) OwnershipType.None else OwnershipType.Physical,
                    )
                },
            )
            QuickActionButton(
                text = formatCollectionDisplayName(collection?.name, item.collectionSortOrder)
                    ?: stringResource(R.string.collection_action_add),
                accent = accent,
                selected = collection != null,
                modifier = Modifier.weight(1f),
                onClick = { showCollectionDialog = true },
            )
            QuickActionButton(
                text = when {
                    !hasExternalTracking -> stringResource(R.string.external_tracking_not_tracked)
                    isExternalTrackingSynced -> stringResource(R.string.external_tracking_updated)
                    else -> stringResource(R.string.external_tracking_pending)
                },
                accent = accent,
                selected = isExternalTrackingSynced,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!hasExternalTracking) {
                        showExternalTrackingDialog = true
                    } else {
                        externalTracking.forEach { tracking ->
                            onUpdateExternalTrackingSynced(tracking.id, !isExternalTrackingSynced)
                        }
                    }
                },
            )
            QuickActionButton(
                text = stringResource(R.string.new_session_title),
                accent = accent,
                modifier = Modifier.weight(1f),
                onClick = { showNewSessionDialog = true },
            )
        }
    }

    if (showCollectionDialog) {
        CollectionDialog(
            collection = collection,
            availableCollections = availableCollections,
            currentSortOrder = item.collectionSortOrder,
            accent = accent,
            onDismiss = { showCollectionDialog = false },
            onSave = { collectionId, newCollectionName, collectionSortOrder ->
                onSaveItemDetails(
                    item.title,
                    collectionId,
                    newCollectionName,
                    collectionSortOrder,
                    item.effectiveProgressTotal(),
                    item.ownership.type,
                )
                showCollectionDialog = false
            },
        )
    }

    if (showExternalTrackingDialog) {
        ExternalTrackingDialog(
            externalTracking = externalTracking,
            accent = accent,
            onDismiss = { showExternalTrackingDialog = false },
            onAddExternalTracking = { source, externalItemId, url ->
                onAddExternalTracking(source, externalItemId, url)
                showExternalTrackingDialog = false
            },
            onUpdateExternalTracking = { _, _, _, _ -> },
            onUpdateSynced = onUpdateExternalTrackingSynced,
            onDelete = {},
        )
    }

    if (showNewSessionDialog) {
        NewSessionDialog(
            item = item,
            currentSession = currentSession,
            accent = accent,
            onDismiss = { showNewSessionDialog = false },
            onStartNewSession = {
                onStartNewSession(it)
                showNewSessionDialog = false
            },
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    accent: Color,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        accent.copy(alpha = 0.18f)
    } else {
        OmnilogColors.AppPanel
    }
    val borderColor = if (selected) {
        accent.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
    }
    val contentColor = if (selected) {
        accent
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CollectionDialog(
    collection: MediaCollection?,
    availableCollections: List<MediaCollection>,
    currentSortOrder: Double?,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (Long?, String?, Double?) -> Unit,
) {
    var selectedCollectionId by rememberSaveable(collection?.id) { mutableStateOf(collection?.id) }
    var searchQuery by rememberSaveable(collection?.id) { mutableStateOf(collection?.name.orEmpty()) }
    var orderText by rememberSaveable(collection?.id, currentSortOrder) {
        mutableStateOf(currentSortOrder?.let(::formatCollectionOrder).orEmpty())
    }
    val selectedCollection = availableCollections.firstOrNull { it.id == selectedCollectionId }
    val trimmedQuery = searchQuery.trim()
    val visibleCollections = availableCollections.filter { availableCollection ->
        availableCollection.name.contains(trimmedQuery, ignoreCase = true)
    }
    val exactCollectionNameMatch = availableCollections.firstOrNull { availableCollection ->
        availableCollection.name.equals(trimmedQuery, ignoreCase = true)
    }
    val canCreateCollection = trimmedQuery.isNotBlank() && exactCollectionNameMatch == null

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.collection_modal_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val collectionIdToSave = selectedCollectionId ?: exactCollectionNameMatch?.id
                            val newCollectionName = if (collectionIdToSave == null) {
                                trimmedQuery.takeIf { it.isNotBlank() }
                            } else {
                                null
                            }
                            val hasCollection = collectionIdToSave != null || newCollectionName != null
                            onSave(
                                collectionIdToSave,
                                newCollectionName,
                                orderText.toCollectionOrderOrNull()?.takeIf { hasCollection },
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    selectedCollectionId = null
                },
                label = { Text(stringResource(R.string.collection_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = omnilogModalTextFieldColors(accent),
            )

            OutlinedTextField(
                value = orderText,
                onValueChange = { value ->
                    orderText = value.toCollectionOrderInput()
                },
                label = { Text(stringResource(R.string.field_collection_order)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = omnilogModalTextFieldColors(accent),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CollectionOptionRow(
                        text = stringResource(R.string.collection_none),
                        selected = selectedCollection == null && trimmedQuery.isBlank(),
                        accent = accent,
                        onClick = {
                            selectedCollectionId = null
                            searchQuery = ""
                        },
                    )
                }
                items(visibleCollections) { availableCollection ->
                    CollectionOptionRow(
                        text = availableCollection.name,
                        selected = availableCollection.id == selectedCollectionId,
                        accent = accent,
                        onClick = {
                            selectedCollectionId = availableCollection.id
                            searchQuery = availableCollection.name
                        },
                    )
                }
                if (canCreateCollection) {
                    item {
                        CollectionOptionRow(
                            text = stringResource(R.string.collection_create_from_search, trimmedQuery),
                            selected = selectedCollection == null,
                            accent = accent,
                            onClick = {
                                selectedCollectionId = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionOptionRow(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else OmnilogColors.AppPanel,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
        contentColor = if (selected) accent else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

private fun String.toCollectionOrderOrNull(): Double? {
    return replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it >= 0.0 }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExternalRatingsDialog(
    ratings: List<ExternalRating>,
    primaryScore: Double?,
    primaryMaxScore: Double?,
    accent: Color,
    onDismiss: () -> Unit,
    onAddExternalRating: (ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var selectedSource by rememberSaveable { mutableStateOf(ExternalRatingSource.Mal) }
    var score by rememberSaveable { mutableStateOf("") }
    var maxScore by rememberSaveable { mutableStateOf("10") }
    var voteCount by rememberSaveable { mutableStateOf("") }
    var makePrimary by rememberSaveable { mutableStateOf(ratings.isEmpty()) }
    val parsedScore = score.toDecimalOrNull()
    val parsedMaxScore = maxScore.toDecimalOrNull()

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_external_scores),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = OmnilogColors.AppPanel,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OptionSelector(
                        label = stringResource(R.string.field_external_rating_source),
                        options = ExternalRatingSource.entries,
                        selectedOption = selectedSource,
                        optionLabel = { it.displayName() },
                        onOptionSelected = { selectedSource = it },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = score,
                            onValueChange = { score = it },
                            label = { Text(stringResource(R.string.field_external_rating_score)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = omnilogModalTextFieldColors(accent),
                        )
                        OutlinedTextField(
                            value = maxScore,
                            onValueChange = { maxScore = it },
                            label = { Text(stringResource(R.string.field_external_rating_max)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = omnilogModalTextFieldColors(accent),
                        )
                    }
                    OutlinedTextField(
                        value = voteCount,
                        onValueChange = { voteCount = it },
                        label = { Text(stringResource(R.string.field_external_rating_users)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = omnilogModalTextFieldColors(accent),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = makePrimary, onCheckedChange = { makePrimary = it })
                        Text(text = stringResource(R.string.make_primary_external_rating), color = OmnilogColors.AppMuted)
                    }
                    Button(
                        enabled = parsedScore != null && parsedMaxScore != null && parsedMaxScore > 0.0,
                        onClick = {
                            onAddExternalRating(
                                selectedSource,
                                parsedScore ?: return@Button,
                                parsedMaxScore ?: return@Button,
                                voteCount.toIntOrNull(),
                                makePrimary,
                            )
                            score = ""
                            maxScore = "10"
                            voteCount = ""
                            makePrimary = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(text = stringResource(R.string.add_external_rating))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ratings) { rating ->
                    ExternalRatingManageRow(
                        rating = rating,
                        isPrimary = rating.matchesPrimary(primaryScore, primaryMaxScore),
                        accent = accent,
                        onUpdateExternalRating = onUpdateExternalRating,
                        onSetPrimary = onSetPrimary,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExternalRatingManageRow(
    rating: ExternalRating,
    isPrimary: Boolean,
    accent: Color,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var selectedSource by rememberSaveable(rating.id) { mutableStateOf(rating.source) }
    var score by rememberSaveable(rating.id) { mutableStateOf(rating.score.cleanDecimal()) }
    var maxScore by rememberSaveable(rating.id) { mutableStateOf(rating.maxScore.cleanDecimal()) }
    var voteCount by rememberSaveable(rating.id) { mutableStateOf(rating.voteCount?.toString().orEmpty()) }
    val parsedScore = score.toDecimalOrNull()
    val parsedMaxScore = maxScore.toDecimalOrNull()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, if (isPrimary) accent else OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OptionSelector(
                label = stringResource(R.string.field_external_rating_source),
                options = ExternalRatingSource.entries,
                selectedOption = selectedSource,
                optionLabel = { it.displayName() },
                onOptionSelected = { selectedSource = it },
            )
            if (isPrimary) {
                Text(
                    text = stringResource(R.string.primary_external_rating),
                    color = accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text(stringResource(R.string.field_external_rating_score)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = omnilogModalTextFieldColors(accent),
                )
                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { maxScore = it },
                    label = { Text(stringResource(R.string.field_external_rating_max)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = omnilogModalTextFieldColors(accent),
                )
            }
            OutlinedTextField(
                value = voteCount,
                onValueChange = { voteCount = it },
                label = { Text(stringResource(R.string.field_external_rating_users)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = omnilogModalTextFieldColors(accent),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = parsedScore != null && parsedMaxScore != null && parsedMaxScore > 0.0,
                    onClick = {
                        onUpdateExternalRating(
                            rating.id,
                            selectedSource,
                            parsedScore ?: return@TextButton,
                            parsedMaxScore ?: return@TextButton,
                            voteCount.toIntOrNull(),
                            isPrimary,
                        )
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
                TextButton(onClick = { onSetPrimary(rating.id) }) {
                    Text(text = stringResource(R.string.make_primary_external_rating))
                }
                TextButton(onClick = { onDelete(rating.id) }) {
                    Text(
                        text = stringResource(R.string.delete_external_rating),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun String.toDecimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

private fun Double.cleanDecimal(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun ExternalRating.matchesPrimary(primaryScore: Double?, primaryMaxScore: Double?): Boolean {
    return primaryScore != null &&
        primaryMaxScore != null &&
        kotlin.math.abs(score - primaryScore) < 0.001 &&
        kotlin.math.abs(maxScore - primaryMaxScore) < 0.001
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExternalTrackingDialog(
    externalTracking: List<ExternalTracking>,
    accent: Color,
    onDismiss: () -> Unit,
    onAddExternalTracking: (ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateSynced: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var selectedSource by rememberSaveable { mutableStateOf(ExternalTrackingSource.Mal) }
    var externalItemId by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_external_tracking),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    if (externalTracking.isEmpty()) {
                        Button(
                            onClick = {
                                onAddExternalTracking(
                                    selectedSource,
                                    externalItemId,
                                    url,
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text(text = stringResource(R.string.add_external_tracking))
                        }
                    }
                }
            }

            if (externalTracking.isEmpty()) {
                OptionSelector(
                    label = stringResource(R.string.field_external_tracking_source),
                    options = ExternalTrackingSource.entries,
                    selectedOption = selectedSource,
                    optionLabel = { source -> source.label() },
                    onOptionSelected = { source -> selectedSource = source },
                )
                OutlinedTextField(
                    value = externalItemId,
                    onValueChange = { externalItemId = it },
                    label = { Text(stringResource(R.string.field_external_tracking_id)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = omnilogModalTextFieldColors(accent),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.field_external_tracking_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = omnilogModalTextFieldColors(accent),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(externalTracking) { tracking ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = OmnilogColors.AppPanel,
                            border = BorderStroke(1.dp, OmnilogColors.AppLine),
                        ) {
                            ExternalTrackingManageRow(
                                tracking = tracking,
                                accent = accent,
                                onUpdateExternalTracking = onUpdateExternalTracking,
                                onUpdateSynced = onUpdateSynced,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalTrackingManageRow(
    tracking: ExternalTracking,
    accent: Color,
    onUpdateExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateSynced: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var selectedSource by rememberSaveable(tracking.id) { mutableStateOf(tracking.source) }
    var externalItemId by rememberSaveable(tracking.id) { mutableStateOf(tracking.externalItemId.orEmpty()) }
    var url by rememberSaveable(tracking.id) { mutableStateOf(tracking.url.orEmpty()) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OptionSelector(
            label = stringResource(R.string.field_external_tracking_source),
            options = ExternalTrackingSource.entries,
            selectedOption = selectedSource,
            optionLabel = { source -> source.label() },
            onOptionSelected = { source -> selectedSource = source },
        )
        Text(
            text = if (tracking.isSynced) {
                stringResource(R.string.external_tracking_updated)
            } else {
                stringResource(R.string.external_tracking_pending)
            },
            color = if (tracking.isSynced) accent else OmnilogColors.Paused,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = externalItemId,
            onValueChange = { externalItemId = it },
            label = { Text(stringResource(R.string.field_external_tracking_id)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = omnilogModalTextFieldColors(accent),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.field_external_tracking_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = omnilogModalTextFieldColors(accent),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    onUpdateExternalTracking(
                        tracking.id,
                        selectedSource,
                        externalItemId,
                        url,
                    )
                },
            ) {
                Text(text = stringResource(R.string.save))
            }
            TextButton(onClick = { onUpdateSynced(tracking.id, false) }) {
                Text(text = stringResource(R.string.mark_external_tracking_pending))
            }
            TextButton(onClick = { onDelete(tracking.id) }) {
                Text(
                    text = stringResource(R.string.delete_external_tracking),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun NewSessionDialog(
    item: MediaItem,
    currentSession: TrackingSession?,
    accent: Color,
    onDismiss: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
) {
    val draftSession = TrackingSession(
        id = -item.id,
        mediaItemId = item.id,
        sessionNumber = (currentSession?.sessionNumber ?: 0) + 1,
        status = TrackingStatus.Planned,
        progressCurrent = 0,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SessionEditorScreen(
            session = draftSession,
            progressTotal = item.effectiveProgressTotal(),
            mediaType = item.type,
            accent = accent,
            titleResId = R.string.new_session_title,
            onBack = onDismiss,
            onSaveSessionDetails = { _, status, progress, rating, notes, startedAt, finishedAt ->
                onStartNewSession(
                    AddTrackingSessionRequest(
                        mediaItemId = item.id,
                        status = status,
                        progressCurrent = progress,
                        rating = rating,
                        notes = notes,
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                    ),
                )
            },
        )
    }
}

private fun MediaItem.effectiveProgressTotal(): Int? {
    return progressTotal.takeUnless { type == MediaType.Game }
}

@Composable
private fun ExternalTrackingSource.label(): String =
    when (this) {
        ExternalTrackingSource.Mal -> "MAL"
        ExternalTrackingSource.Imdb -> "IMDb"
        ExternalTrackingSource.StoryGraph -> "StoryGraph"
        ExternalTrackingSource.Goodreads -> "Goodreads"
        ExternalTrackingSource.Letterboxd -> "Letterboxd"
        ExternalTrackingSource.Tmdb -> "TMDb"
        ExternalTrackingSource.Rawg -> "RAWG"
        ExternalTrackingSource.Backloggd -> "Backloggd"
        ExternalTrackingSource.Other -> stringResource(R.string.external_tracking_other)
    }
