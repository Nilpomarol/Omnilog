package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.nilpo.contenttracker.core.model.ExternalTracking
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
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
                        item.progressTotal,
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
                    item.progressTotal,
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.collection_modal_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text(text = stringResource(R.string.cancel), color = accent)
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            selectedCollectionId = null
                        },
                        label = { Text(stringResource(R.string.collection_search)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            cursorColor = accent,
                        ),
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            cursorColor = accent,
                        ),
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.detail_external_tracking),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text(text = stringResource(R.string.cancel), color = accent)
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (externalTracking.isEmpty()) {
                    item {
                        OptionSelector(
                            label = stringResource(R.string.field_external_tracking_source),
                            options = ExternalTrackingSource.entries,
                            selectedOption = selectedSource,
                            optionLabel = { source -> source.label() },
                            onOptionSelected = { source -> selectedSource = source },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = externalItemId,
                            onValueChange = { externalItemId = it },
                            label = { Text(stringResource(R.string.field_external_tracking_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                cursorColor = accent,
                            ),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.field_external_tracking_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                cursorColor = accent,
                            ),
                        )
                    }
                } else {
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                cursorColor = accent,
            ),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.field_external_tracking_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                cursorColor = accent,
            ),
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
            progressTotal = item.progressTotal,
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
