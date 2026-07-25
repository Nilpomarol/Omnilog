package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
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
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.CollectionPickerSheet
import com.nilpo.contenttracker.ui.common.OmnilogDropdownField
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.toCollectionPickerOptions
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.localizedSteamScoreDescriptor
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Composable
fun DetailQuickActionsSection(
    item: MediaItem,
    collection: MediaCollection?,
    library: List<TrackedMedia>,
    currentSession: TrackingSession?,
    accent: Color,
    onSaveItemDetails: (String, Long?, String?, Double?, Int?, Boolean) -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCollectionDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showNewSessionDialog by rememberSaveable(item.id) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuietQuickAction(
                text = if (item.isOwned) {
                    stringResource(R.string.owned_label)
                } else {
                    stringResource(R.string.owned_action_add)
                },
                iconResId = R.drawable.ic_owned_badge,
                accent = accent,
                selected = item.isOwned,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSaveItemDetails(
                        item.title,
                        item.collectionId,
                        null,
                        item.collectionSortOrder,
                        item.effectiveProgressTotal(),
                        !item.isOwned,
                    )
                },
            )
            QuietQuickAction(
                text = formatCollectionDisplayName(collection?.name, item.collectionSortOrder)
                    ?: stringResource(R.string.collection_action_add),
                iconResId = R.drawable.ic_group_collections,
                accent = accent,
                selected = collection != null,
                modifier = Modifier.weight(1f),
                onClick = { showCollectionDialog = true },
            )
        }
        if (currentSession?.status != TrackingStatus.Planned) {
            NewSessionAction(
                accent = accent,
                onClick = { showNewSessionDialog = true },
            )
        }
    }

    if (showCollectionDialog) {
        val pickerOptions = remember(library, item.type) {
            library.toCollectionPickerOptions(forType = item.type)
        }
        CollectionPickerSheet(
            itemTitle = item.title,
            providerCollectionTitle = item.providerCollectionTitle,
            options = pickerOptions,
            initialCollectionId = collection?.id,
            initialSortOrder = item.collectionSortOrder,
            accent = accent,
            onDismiss = { showCollectionDialog = false },
            onConfirm = { result ->
                onSaveItemDetails(
                    item.title,
                    result.collectionId,
                    result.newCollectionName,
                    result.sortOrder,
                    item.effectiveProgressTotal(),
                    item.isOwned,
                )
                showCollectionDialog = false
            },
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
private fun QuietQuickAction(
    text: String,
    @androidx.annotation.DrawableRes iconResId: Int,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) accent.copy(alpha = 0.14f) else OmnilogTheme.colors.appPanel
    val borderColor = if (selected) accent.copy(alpha = 0.60f) else OmnilogTheme.colors.appLine
    val contentColor = if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appInk.copy(alpha = 0.85f)

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) accent else OmnilogTheme.colors.appMuted,
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NewSessionAction(
    accent: Color,
    onClick: () -> Unit,
) {
    val contentColor = if (accent.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = contentColor,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Text(
                text = stringResource(R.string.new_session_title),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExternalRatingsPage(
    title: String,
    mediaType: MediaType,
    ratings: List<ExternalRating>,
    primaryRatingId: Long?,
    accent: Color,
    onAddExternalRating: (ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    val defaultSource = mediaType.defaultExternalRatingSource()
    var selectedSource by rememberSaveable(mediaType) { mutableStateOf(defaultSource) }
    var score by rememberSaveable { mutableStateOf("") }
    var maxScoreText by rememberSaveable(mediaType) { mutableStateOf(defaultSource.defaultMaxScore().cleanDecimal()) }
    var voteCount by rememberSaveable { mutableStateOf("") }
    var makePrimary by rememberSaveable(mediaType) {
        mutableStateOf(ratings.isEmpty() || mediaType.prefersPrimarySource(defaultSource))
    }
    val parsedScore = score.toDecimalOrNull()
    val parsedMaxScore = maxScoreText.toDecimalOrNull()
    val availableSources = mediaType.externalRatingSources()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.detail_external_scores),
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                IconButton(onClick = { showAddForm = !showAddForm }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_external_rating),
                        tint = accent,
                    )
                }
            }
            HorizontalDivider(color = OmnilogTheme.colors.appLine)

            if (showAddForm) Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = OmnilogTheme.colors.appPanel,
                border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ExternalRatingSourceDropdown(
                        sources = availableSources,
                        selectedOption = selectedSource,
                        onOptionSelected = { source ->
                            selectedSource = source
                            maxScoreText = source.defaultMaxScore().cleanDecimal()
                            makePrimary = ratings.isEmpty() || mediaType.prefersPrimarySource(source)
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = score,
                            onValueChange = { score = it },
                            label = { Text(stringResource(R.string.field_external_rating_score)) },
                            modifier = Modifier.weight(.85f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = omnilogModalTextFieldColors(accent),
                        )
                        Text(
                            text = "/",
                            modifier = Modifier.padding(top = 18.dp),
                            color = OmnilogTheme.colors.appMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = maxScoreText,
                            onValueChange = { maxScoreText = it },
                            label = { Text(stringResource(R.string.field_external_rating_max)) },
                            modifier = Modifier.weight(.7f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = omnilogModalTextFieldColors(accent),
                        )
                        OutlinedTextField(
                            value = voteCount,
                            onValueChange = { voteCount = it },
                            label = { Text(stringResource(R.string.field_external_rating_users)) },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = omnilogModalTextFieldColors(accent),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = makePrimary, onCheckedChange = { makePrimary = it })
                            Text(
                                text = stringResource(R.string.make_primary_external_rating),
                                color = OmnilogTheme.colors.appMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Button(
                            enabled = parsedScore != null &&
                                parsedMaxScore != null &&
                                parsedMaxScore > 0.0 &&
                                parsedScore <= parsedMaxScore,
                            onClick = {
                                onAddExternalRating(
                                    selectedSource,
                                    parsedScore ?: return@Button,
                                    parsedMaxScore ?: return@Button,
                                    voteCount.toIntOrNull(),
                                    makePrimary,
                                )
                                score = ""
                                voteCount = ""
                                makePrimary = mediaType.prefersPrimarySource(selectedSource)
                                showAddForm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.Black,
                            ),
                        ) { Text(text = stringResource(R.string.add_external_rating)) }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ratings) { rating ->
                    ExternalRatingManageRow(
                        rating = rating,
                        mediaType = mediaType,
                        isPrimary = rating.id == primaryRatingId,
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
    mediaType: MediaType,
    isPrimary: Boolean,
    accent: Color,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var isEditing by rememberSaveable(rating.id) { mutableStateOf(false) }
    var selectedSource by rememberSaveable(rating.id) { mutableStateOf(rating.source) }
    var score by rememberSaveable(rating.id) { mutableStateOf(rating.score.cleanDecimal()) }
    var voteCount by rememberSaveable(rating.id) { mutableStateOf(rating.voteCount?.toString().orEmpty()) }
    val parsedScore = score.toDecimalOrNull()
    val maxScore = if (selectedSource == rating.source) rating.maxScore else selectedSource.defaultMaxScore()
    val availableSources = (mediaType.externalRatingSources() + selectedSource).distinct()
    val steamDescriptor = localizedSteamScoreDescriptor(
        mediaType = mediaType,
        source = rating.source,
        descriptor = rating.scoreDescriptor,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, if (isPrimary) accent else OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = rating.source.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appInk,
                        modifier = Modifier.weight(1f),
                    )
                    if (isPrimary) {
                        Text(
                            text = stringResource(R.string.primary_external_rating),
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatExternalRating(
                                score = rating.score,
                                maxScore = rating.maxScore,
                                mediaType = mediaType,
                                source = rating.source,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        steamDescriptor?.let { descriptor ->
                            Text(
                                text = descriptor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                    }
                    Text(
                        text = rating.voteCount?.let { "${it} ${stringResource(R.string.field_external_rating_users).lowercase()}" }.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    if (!isPrimary) {
                        IconButton(onClick = { onSetPrimary(rating.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = stringResource(R.string.make_primary_external_rating),
                                tint = accent,
                            )
                        }
                    }
                }
            } else {
            ExternalRatingSourceDropdown(
                sources = availableSources,
                selectedOption = selectedSource,
                onOptionSelected = { source ->
                    selectedSource = source
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text(stringResource(R.string.field_external_rating_score)) },
                    modifier = Modifier.weight(.75f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = omnilogModalTextFieldColors(accent),
                )
                Text(
                    text = if (
                        mediaType == MediaType.Game && selectedSource == ExternalRatingSource.Steam
                    ) "%" else "/${maxScore.cleanDecimal()}",
                    modifier = Modifier.padding(top = 18.dp),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = voteCount,
                    onValueChange = { voteCount = it },
                    label = { Text(stringResource(R.string.field_external_rating_users)) },
                    modifier = Modifier.weight(1.25f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = omnilogModalTextFieldColors(accent),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                TextButton(onClick = { isEditing = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                if (isPrimary) {
                    Text(
                        text = stringResource(R.string.primary_external_rating),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextButton(onClick = { onSetPrimary(rating.id) }) {
                        Text(text = stringResource(R.string.make_primary_external_rating))
                    }
                }
                TextButton(
                    enabled = parsedScore != null && parsedScore <= maxScore,
                    onClick = {
                        onUpdateExternalRating(
                            rating.id,
                            selectedSource,
                            parsedScore ?: return@TextButton,
                            maxScore,
                            voteCount.toIntOrNull(),
                            isPrimary,
                        )
                        isEditing = false
                    },
                ) { Text(text = stringResource(R.string.save)) }
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
}

private fun String.toDecimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

private fun Double.cleanDecimal(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Composable
private fun ExternalRatingSourceDropdown(
    sources: List<ExternalRatingSource>,
    selectedOption: ExternalRatingSource,
    onOptionSelected: (ExternalRatingSource) -> Unit,
) {
    OmnilogDropdownField(
        selectedOption = selectedOption,
        options = sources,
        optionLabel = { it.displayName() },
        onOptionSelected = onOptionSelected,
        label = stringResource(R.string.field_external_rating_source),
    )
}

private fun MediaType.defaultExternalRatingSource(): ExternalRatingSource {
    return when (this) {
        MediaType.Book -> ExternalRatingSource.Goodreads
        MediaType.Anime -> ExternalRatingSource.Mal
        MediaType.Movie,
        MediaType.TvShow,
            -> ExternalRatingSource.Imdb
        MediaType.Game -> ExternalRatingSource.Steam
    }
}

private fun ExternalRatingSource.defaultMaxScore(): Double {
    return when (this) {
        ExternalRatingSource.Goodreads,
        ExternalRatingSource.StoryGraph,
            -> 5.0
        ExternalRatingSource.RottenTomatoes,
        ExternalRatingSource.Metacritic,
        ExternalRatingSource.Steam,
            -> 100.0
        else -> 10.0
    }
}

private fun MediaType.prefersPrimarySource(source: ExternalRatingSource): Boolean {
    return when (this) {
        MediaType.Book -> source == ExternalRatingSource.Goodreads
        MediaType.Game -> source == ExternalRatingSource.Steam
        else -> false
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

private fun MediaType.externalRatingSources(): List<ExternalRatingSource> {
    return when (this) {
        MediaType.Anime -> listOf(ExternalRatingSource.AniList, ExternalRatingSource.Mal)
        MediaType.Book -> listOf(
            ExternalRatingSource.Goodreads,
            ExternalRatingSource.StoryGraph,
            ExternalRatingSource.GoogleBooks,
            ExternalRatingSource.OpenLibrary,
        )
        MediaType.Movie,
        MediaType.TvShow,
            -> listOf(
                ExternalRatingSource.Imdb,
                ExternalRatingSource.Tmdb,
                ExternalRatingSource.RottenTomatoes,
                ExternalRatingSource.Metacritic,
                ExternalRatingSource.FilmAffinity,
            )
        MediaType.Game -> listOf(
            ExternalRatingSource.Rawg,
            ExternalRatingSource.Metacritic,
            ExternalRatingSource.Steam,
        )
    }
}

@Composable
fun GoodreadsRatingPrompt(
    accent: Color,
    onSave: (Double, Int?) -> Unit,
    onSkipBook: () -> Unit,
    onDismiss: () -> Unit,
) {
    var score by rememberSaveable { mutableStateOf("") }
    var voteCount by rememberSaveable { mutableStateOf("") }
    val parsedScore = score.toDecimalOrNull()
    val parsedVoteCount = voteCount.toIntOrNull()

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add Goodreads rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = "This book has no Goodreads score yet.",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            OutlinedTextField(
                value = score,
                onValueChange = { score = it },
                label = { Text("Rating (out of 5)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = omnilogModalTextFieldColors(accent),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = voteCount,
                onValueChange = { voteCount = it },
                label = { Text("Users who rated it") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = omnilogModalTextFieldColors(accent),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(onClick = onSkipBook) {
                    Text(text = "Don't ask for this book")
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = parsedScore != null && parsedScore in 0.0..5.0 &&
                        (voteCount.isBlank() || parsedVoteCount != null),
                    onClick = {
                        onSave(parsedScore ?: return@Button, parsedVoteCount)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }
}
