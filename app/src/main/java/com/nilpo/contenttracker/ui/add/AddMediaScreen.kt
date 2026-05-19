package com.nilpo.contenttracker.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MediaMetadataHero
import com.nilpo.contenttracker.ui.common.MediaMetadataHeroGenres
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatExternalRatingOnTen
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AddMediaScreen(
    initialMediaType: MediaType,
    availableMediaTypes: List<MediaType>,
    onSave: (AddTrackedMediaRequest) -> Unit,
    metadataUiState: MetadataSearchUiState,
    onMetadataQueryChange: (String) -> Unit,
    onMetadataSearch: () -> Unit,
    onMetadataSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(AddMediaStep.Search) }
    var title by remember { mutableStateOf("") }
    var totalProgress by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf(initialMediaType) }
    var selectedStatus by remember { mutableStateOf(TrackingStatus.Planned) }
    var selectedOwnershipType by remember { mutableStateOf(OwnershipType.None) }
    var selectedPlatformType by remember { mutableStateOf(ConsumptionPlatformType.Other) }
    var initialProgress by remember { mutableStateOf("0") }
    var initialRating by remember { mutableStateOf<Int?>(null) }
    var initialNotes by remember { mutableStateOf("") }
    var initialStartedAt by remember { mutableStateOf(LocalDate.now().toString()) }
    var initialFinishedAt by remember { mutableStateOf("") }
    val selectedMetadataSuggestion = metadataUiState.selectedSuggestion

    LaunchedEffect(selectedMetadataSuggestion) {
        selectedMetadataSuggestion?.let { suggestion ->
            title = suggestion.title
            totalProgress = suggestion.progressTotal?.toString().orEmpty()
            selectedMediaType = suggestion.mediaType
            if (step != AddMediaStep.Manual) {
                step = AddMediaStep.Review
            }
        }
    }
    val applyStatus: (TrackingStatus) -> Unit = { status ->
        selectedStatus = status
        if (status == TrackingStatus.Completed) {
            totalProgress.toIntOrNull()?.let { maxProgress ->
                initialProgress = maxProgress.toString()
            }
            if (initialFinishedAt.isBlank()) {
                initialFinishedAt = LocalDate.now().toString()
            }
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        when (step) {
            AddMediaStep.Search -> {
                MetadataSearchStep(
                    initialMediaType = initialMediaType,
                    uiState = metadataUiState,
                    onQueryChange = onMetadataQueryChange,
                    onSearch = onMetadataSearch,
                    onSuggestionSelected = onMetadataSuggestionSelected,
                    duplicateStateForSuggestion = duplicateStateForSuggestion,
                    onManualAdd = {
                        selectedMediaType = initialMediaType
                        title = ""
                        totalProgress = ""
                        step = AddMediaStep.Manual
                    },
                    onCancel = onCancel,
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (step) {
                        AddMediaStep.Review -> MetadataReviewStep(
                    suggestion = selectedMetadataSuggestion,
                    isLoadingDetails = metadataUiState.isLoadingDetails,
                    hasDetailsError = metadataUiState.hasDetailsError,
                    selectedMediaType = selectedMediaType,
                    title = title,
                    totalProgress = totalProgress,
                    selectedStatus = selectedStatus,
                    onStatusSelected = applyStatus,
                    initialProgress = initialProgress,
                    onInitialProgressChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        val maxProgress = totalProgress.toIntOrNull()
                        initialProgress = maxProgress?.let { max ->
                            digits.toIntOrNull()?.coerceIn(0, max)?.toString() ?: digits
                        } ?: digits
                    },
                    initialRating = initialRating,
                    onInitialRatingSelected = { initialRating = it },
                    initialStartedAt = initialStartedAt,
                    onInitialStartedAtChange = { initialStartedAt = it },
                    initialFinishedAt = initialFinishedAt,
                    onInitialFinishedAtChange = { initialFinishedAt = it },
                    initialNotes = initialNotes,
                    onInitialNotesChange = { initialNotes = it },
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                initialStatus = selectedStatus,
                                initialProgress = initialProgress.toIntOrNull() ?: 0,
                                initialRating = initialRating,
                                initialNotes = initialNotes.takeIf { it.isNotBlank() },
                                initialStartedAt = initialStartedAt.toLocalDateOrNull(),
                                initialFinishedAt = initialFinishedAt.toLocalDateOrNull(),
                                isOwned = selectedOwnershipType != OwnershipType.None,
                                ownershipType = selectedOwnershipType,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                                originalTitle = selectedMetadataSuggestion?.originalTitle,
                                releaseYear = selectedMetadataSuggestion?.releaseYear,
                                genres = selectedMetadataSuggestion?.genres.orEmpty(),
                                creators = selectedMetadataSuggestion?.creators.orEmpty(),
                                credits = selectedMetadataSuggestion?.credits.orEmpty(),
                                sourceUrl = selectedMetadataSuggestion?.sourceUrl,
                                externalRating = selectedMetadataSuggestion?.externalRating,
                                externalRatings = selectedMetadataSuggestion?.externalRatings.orEmpty(),
                                popularityScore = selectedMetadataSuggestion?.popularityScore,
                                rankingPosition = selectedMetadataSuggestion?.rankingPosition,
                                rankingLabel = selectedMetadataSuggestion?.rankingLabel,
                                providerCollectionTitle = selectedMetadataSuggestion?.collectionTitle,
                                ratingDistributionJson = selectedMetadataSuggestion?.ratingDistributionJson,
                                popularityJson = selectedMetadataSuggestion?.popularityJson,
                                rankingJson = selectedMetadataSuggestion?.rankingJson,
                                metadataSource = selectedMetadataSuggestion?.source,
                                metadataExternalId = selectedMetadataSuggestion?.externalId,
                                coverUrl = selectedMetadataSuggestion?.coverUrl,
                                synopsis = selectedMetadataSuggestion?.synopsis,
                            ),
                        )
                    },
                    onCancel = onCancel,
                )
                        AddMediaStep.Manual -> ManualAddStep(
                    availableMediaTypes = availableMediaTypes,
                    selectedMediaType = selectedMediaType,
                    onMediaTypeSelected = { selectedMediaType = it },
                    title = title,
                    onTitleChange = { title = it },
                    totalProgress = totalProgress,
                    onTotalProgressChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        totalProgress = digits
                        if (selectedStatus == TrackingStatus.Completed) {
                            initialProgress = digits
                        }
                    },
                    platform = platform,
                    onPlatformChange = { platform = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = applyStatus,
                    selectedOwnershipType = selectedOwnershipType,
                    onOwnershipTypeSelected = { selectedOwnershipType = it },
                    selectedPlatformType = selectedPlatformType,
                    onPlatformTypeSelected = { selectedPlatformType = it },
                    initialProgress = initialProgress,
                    onInitialProgressChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        val maxProgress = totalProgress.toIntOrNull()
                        initialProgress = maxProgress?.let { max ->
                            digits.toIntOrNull()?.coerceIn(0, max)?.toString() ?: digits
                        } ?: digits
                    },
                    initialRating = initialRating,
                    onInitialRatingSelected = { initialRating = it },
                    initialStartedAt = initialStartedAt,
                    onInitialStartedAtChange = { initialStartedAt = it },
                    initialFinishedAt = initialFinishedAt,
                    onInitialFinishedAtChange = { initialFinishedAt = it },
                    initialNotes = initialNotes,
                    onInitialNotesChange = { initialNotes = it },
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                initialStatus = selectedStatus,
                                initialProgress = initialProgress.toIntOrNull() ?: 0,
                                initialRating = initialRating,
                                initialNotes = initialNotes.takeIf { it.isNotBlank() },
                                initialStartedAt = initialStartedAt.toLocalDateOrNull(),
                                initialFinishedAt = initialFinishedAt.toLocalDateOrNull(),
                                isOwned = selectedOwnershipType != OwnershipType.None,
                                ownershipType = selectedOwnershipType,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                            ),
                        )
                    },
                    onCancel = onCancel,
                )
                        AddMediaStep.Search -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataSearchStep(
    initialMediaType: MediaType,
    uiState: MetadataSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onManualAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    LaunchedEffect(uiState.query) {
        if (uiState.query.trim().length >= 2) {
            delay(450)
            onSearch()
        }
    }
    val accent = initialMediaType.sectionAccent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashboardStyleSearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange,
            isLoading = uiState.isLoading,
            accent = accent,
            onSearch = onSearch,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataSearchResults(
                uiState = uiState,
                onSuggestionSelected = onSuggestionSelected,
                duplicateStateForSuggestion = duplicateStateForSuggestion,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onManualAdd,
                colors = ButtonDefaults.textButtonColors(contentColor = accent),
            ) {
                Text(text = stringResource(R.string.add_manual))
            }
        }
    }
}

@Composable
private fun MetadataSearchResults(
    uiState: MetadataSearchUiState,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
) {
    when {
        uiState.isLoading -> SearchStatePanel(text = stringResource(R.string.metadata_search_loading))
        uiState.hasError -> SearchStatePanel(
            text = stringResource(R.string.metadata_search_error),
            color = MaterialTheme.colorScheme.error,
        )
        uiState.hasSearched && uiState.suggestions.isEmpty() -> SearchStatePanel(
            text = stringResource(R.string.metadata_search_empty),
        )
        !uiState.hasSearched -> SearchStatePanel(
            text = stringResource(R.string.metadata_search_prompt),
        )
        else -> uiState.suggestions.forEach { suggestion ->
            MetadataSuggestionRow(
                suggestion = suggestion,
                accent = suggestion.mediaType.sectionAccent(),
                duplicateState = duplicateStateForSuggestion(suggestion),
                onClick = { onSuggestionSelected(suggestion) },
            )
        }
    }
}

@Composable
private fun MetadataReviewStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    selectedMediaType: MediaType,
    title: String,
    totalProgress: String,
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    initialProgress: String,
    onInitialProgressChange: (String) -> Unit,
    initialRating: Int?,
    onInitialRatingSelected: (Int?) -> Unit,
    initialStartedAt: String,
    onInitialStartedAtChange: (String) -> Unit,
    initialFinishedAt: String,
    onInitialFinishedAtChange: (String) -> Unit,
    initialNotes: String,
    onInitialNotesChange: (String) -> Unit,
    onBackToSearch: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val accent = selectedMediaType.sectionAccent()

    suggestion?.let {
        MetadataReviewPreview(
            suggestion = it,
            isLoadingDetails = isLoadingDetails,
            hasDetailsError = hasDetailsError,
        )
    }

    ReviewActionRow(
        title = title,
        accent = accent,
        isLoadingDetails = isLoadingDetails,
        onBackToSearch = onBackToSearch,
        onCancel = onCancel,
        onSave = onSave,
    )

    FirstSessionForm(
        mediaType = selectedMediaType,
        progressTotal = totalProgress.toIntOrNull(),
        selectedStatus = selectedStatus,
        onStatusSelected = onStatusSelected,
        initialProgress = initialProgress,
        onInitialProgressChange = onInitialProgressChange,
        initialRating = initialRating,
        onInitialRatingSelected = onInitialRatingSelected,
        initialStartedAt = initialStartedAt,
        onInitialStartedAtChange = onInitialStartedAtChange,
        initialFinishedAt = initialFinishedAt,
        onInitialFinishedAtChange = onInitialFinishedAtChange,
        initialNotes = initialNotes,
        onInitialNotesChange = onInitialNotesChange,
        accent = accent,
    )
}

@Composable
private fun MetadataReviewPreview(
    suggestion: MetadataSuggestion,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
) {
    val metadata = suggestion.toMediaMetadataUi()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MediaMetadataHero(
            metadata = metadata,
            isLoadingDetails = isLoadingDetails,
        )
        if (hasDetailsError) {
            SearchStatePanel(
                text = stringResource(R.string.metadata_details_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        MediaMetadataHeroGenres(metadata = metadata)
        metadata.synopsis?.let { synopsis ->
            ReviewMetadataSection(
                title = stringResource(R.string.metadata_summary),
                body = synopsis,
            )
        }
    }
}

@Composable
private fun ReviewMetadataSection(
    title: String,
    body: String,
) {
    var isExpanded by remember(body) { mutableStateOf(false) }
    val shouldCollapse = body.length > 260

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = OmnilogColors.AppMuted,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogColors.AppInk.copy(alpha = 0.84f),
            maxLines = if (shouldCollapse && !isExpanded) 5 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        if (shouldCollapse) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(text = stringResource(if (isExpanded) R.string.show_less else R.string.show_more))
                }
            }
        }
    }
}

@Composable
private fun ReviewActionRow(
    title: String,
    accent: Color,
    isLoadingDetails: Boolean,
    onBackToSearch: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBackToSearch) {
            Text(text = stringResource(R.string.back))
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
        Button(
            enabled = title.isNotBlank() && !isLoadingDetails,
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.White,
            ),
        ) {
            Text(text = stringResource(R.string.create_session))
        }
    }
}

@Composable
private fun FirstSessionForm(
    mediaType: MediaType,
    progressTotal: Int?,
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    initialProgress: String,
    onInitialProgressChange: (String) -> Unit,
    initialRating: Int?,
    onInitialRatingSelected: (Int?) -> Unit,
    initialStartedAt: String,
    onInitialStartedAtChange: (String) -> Unit,
    initialFinishedAt: String,
    onInitialFinishedAtChange: (String) -> Unit,
    initialNotes: String,
    onInitialNotesChange: (String) -> Unit,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormSectionHeader(title = stringResource(R.string.field_status))
        ReviewStatusSelector(
            selectedStatus = selectedStatus,
            accent = accent,
            onStatusSelected = onStatusSelected,
        )

        FormDivider()

        FormSectionHeader(title = stringResource(R.string.field_progress))
        ReviewProgressField(
            value = initialProgress,
            progressTotal = progressTotal,
            mediaType = mediaType,
            accent = accent,
            onValueChange = onInitialProgressChange,
        )

        FormDivider()

        FormSectionHeader(title = stringResource(R.string.field_rating))
        ReviewRatingSelector(
            currentRating = initialRating,
            accent = accent,
            onRatingSelected = onInitialRatingSelected,
        )

        FormDivider()

        FormSectionHeader(title = stringResource(R.string.session_dates))
        ReviewDateField(
            label = stringResource(R.string.session_started_label),
            value = initialStartedAt,
            accent = accent,
            onValueChange = onInitialStartedAtChange,
        )
        ReviewDateField(
            label = stringResource(R.string.session_finished_label),
            value = initialFinishedAt,
            accent = accent,
            onValueChange = onInitialFinishedAtChange,
        )

        FormDivider()

        FormSectionHeader(title = stringResource(R.string.field_notes))
        ReviewNotesField(
            value = initialNotes,
            accent = accent,
            onValueChange = onInitialNotesChange,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogColors.AppMuted,
        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
    )
}

@Composable
private fun FormDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = OmnilogColors.AppLine.copy(alpha = 0.70f),
    )
}

@Composable
private fun ReviewStatusSelector(
    selectedStatus: TrackingStatus,
    accent: Color,
    onStatusSelected: (TrackingStatus) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(TrackingStatus.entries) { status ->
            val isSelected = status == selectedStatus
            Surface(
                onClick = { onStatusSelected(status) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) accent.copy(alpha = 0.18f) else OmnilogColors.AppPanel,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) accent.copy(alpha = 0.78f) else OmnilogColors.AppLine,
                ),
            ) {
                Text(
                    text = status.label(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isSelected) accent else OmnilogColors.AppMuted,
                )
            }
        }
    }
}

@Composable
private fun ReviewProgressField(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_progress)) },
        supportingText = progressTotal?.takeIf { it > 0 }?.let { total ->
            {
                Text(text = "de $total ${progressUnitLabel(mediaType, total)}")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun ReviewRatingSelector(
    currentRating: Int?,
    accent: Color,
    onRatingSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            (1..10).forEach { rating ->
                val isSelected = rating == currentRating
                val isActive = currentRating != null && rating <= currentRating
                Surface(
                    onClick = { onRatingSelected(if (isSelected) null else rating) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) accent.copy(alpha = 0.20f) else OmnilogColors.AppPanel,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isActive) accent.copy(alpha = 0.74f) else OmnilogColors.AppLine,
                    ),
                ) {
                    Text(
                        text = rating.toString(),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isActive) accent else OmnilogColors.AppMuted,
                    )
                }
            }
        }
        if (currentRating != null) {
            Text(
                text = stringResource(R.string.rating_clear),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.78f),
                modifier = Modifier
                    .clickable { onRatingSelected(null) }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDateField(
    label: String,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember(label) { mutableStateOf(false) }
    val selectedMillis = value.toLocalDateOrNull()
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(10)) },
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = {
            TextButton(
                onClick = { showPicker = true },
                colors = ButtonDefaults.textButtonColors(contentColor = accent),
            ) {
                Text(text = stringResource(R.string.pick_date))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .toString(),
                            )
                        }
                        showPicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                ) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReviewNotesField(
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        placeholder = { Text(text = stringResource(R.string.notes_placeholder)) },
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        maxLines = 6,
    )
}

@Composable
private fun reviewTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = OmnilogColors.AppLine,
    cursorColor = accent,
    focusedLabelColor = accent,
)

@Composable
private fun ManualAddStep(
    availableMediaTypes: List<MediaType>,
    selectedMediaType: MediaType,
    onMediaTypeSelected: (MediaType) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    totalProgress: String,
    onTotalProgressChange: (String) -> Unit,
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    selectedOwnershipType: OwnershipType,
    onOwnershipTypeSelected: (OwnershipType) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
    initialProgress: String,
    onInitialProgressChange: (String) -> Unit,
    initialRating: Int?,
    onInitialRatingSelected: (Int?) -> Unit,
    initialStartedAt: String,
    onInitialStartedAtChange: (String) -> Unit,
    initialFinishedAt: String,
    onInitialFinishedAtChange: (String) -> Unit,
    initialNotes: String,
    onInitialNotesChange: (String) -> Unit,
    onBackToSearch: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val accent = selectedMediaType.sectionAccent()

    Text(
        text = stringResource(R.string.add_manual),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = OmnilogColors.AppInk,
    )

    TrackingSetupForm(
        availableMediaTypes = availableMediaTypes,
        selectedMediaType = selectedMediaType,
        onMediaTypeSelected = onMediaTypeSelected,
        title = title,
        onTitleChange = onTitleChange,
        totalProgress = totalProgress,
        onTotalProgressChange = onTotalProgressChange,
        platform = platform,
        onPlatformChange = onPlatformChange,
        selectedOwnershipType = selectedOwnershipType,
        onOwnershipTypeSelected = onOwnershipTypeSelected,
        selectedPlatformType = selectedPlatformType,
        onPlatformTypeSelected = onPlatformTypeSelected,
    )

    ReviewActionRow(
        title = title,
        accent = accent,
        isLoadingDetails = false,
        onBackToSearch = onBackToSearch,
        onCancel = onCancel,
        onSave = onSave,
    )

    FirstSessionForm(
        mediaType = selectedMediaType,
        progressTotal = totalProgress.toIntOrNull(),
        selectedStatus = selectedStatus,
        onStatusSelected = onStatusSelected,
        initialProgress = initialProgress,
        onInitialProgressChange = onInitialProgressChange,
        initialRating = initialRating,
        onInitialRatingSelected = onInitialRatingSelected,
        initialStartedAt = initialStartedAt,
        onInitialStartedAtChange = onInitialStartedAtChange,
        initialFinishedAt = initialFinishedAt,
        onInitialFinishedAtChange = onInitialFinishedAtChange,
        initialNotes = initialNotes,
        onInitialNotesChange = onInitialNotesChange,
        accent = accent,
    )
}

@Composable
private fun TrackingSetupForm(
    availableMediaTypes: List<MediaType>,
    selectedMediaType: MediaType,
    onMediaTypeSelected: (MediaType) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    totalProgress: String,
    onTotalProgressChange: (String) -> Unit,
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedOwnershipType: OwnershipType,
    onOwnershipTypeSelected: (OwnershipType) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
) {
    val accent = selectedMediaType.sectionAccent()
    if (availableMediaTypes.size > 1) {
        OptionSelector(
            label = stringResource(R.string.field_media_type),
            options = availableMediaTypes,
            selectedOption = selectedMediaType,
            optionLabel = { it.label() },
            onOptionSelected = onMediaTypeSelected,
        )
    }

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.field_title)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )

    OutlinedTextField(
        value = totalProgress,
        onValueChange = onTotalProgressChange,
        label = { Text(stringResource(R.string.field_total_progress)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )

    OutlinedTextField(
        value = platform,
        onValueChange = onPlatformChange,
        label = { Text(stringResource(R.string.field_platform)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = reviewTextFieldColors(accent),
        shape = RoundedCornerShape(12.dp),
    )

    OptionSelector(
        label = stringResource(R.string.field_ownership_type),
        options = OwnershipType.entries,
        selectedOption = selectedOwnershipType,
        optionLabel = { it.label() },
        onOptionSelected = onOwnershipTypeSelected,
    )

    OptionSelector(
        label = stringResource(R.string.field_platform_type),
        options = ConsumptionPlatformType.entries,
        selectedOption = selectedPlatformType,
        optionLabel = { it.label() },
        onOptionSelected = onPlatformTypeSelected,
    )
}

@Composable
private fun AddScreenHeader(
    title: String,
    subtitle: String? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppMuted,
            )
        }
        content?.invoke()
    }
}

@Composable
private fun DashboardStyleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    accent: Color,
    onSearch: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(
            1.dp,
            if (query.isNotBlank()) accent.copy(alpha = 0.58f) else OmnilogColors.AppLine,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = OmnilogColors.AppInk,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.metadata_search_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogColors.AppMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            TextButton(
                enabled = query.isNotBlank() && !isLoading,
                onClick = onSearch,
                colors = ButtonDefaults.textButtonColors(contentColor = accent),
            ) {
                Text(text = stringResource(R.string.search_action))
            }
            content()
        }
    }
}

@Composable
private fun SearchStatePanel(
    text: String,
    color: Color = OmnilogColors.AppMuted,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun MetadataSuggestionRow(
    suggestion: MetadataSuggestion,
    accent: Color,
    duplicateState: MetadataDuplicateState,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetadataCoverImage(
                    coverUrl = suggestion.coverUrl,
                    modifier = Modifier.size(width = 58.dp, height = 86.dp),
                    shape = RoundedCornerShape(6.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = displayMediaTitle(suggestion.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResultChip(text = suggestion.mediaType.label(), accent = accent)
                        suggestion.releaseYear?.let { ResultChip(text = it.toString(), accent = accent) }
                        ResultChip(text = suggestion.source.name)
                    }
                    suggestion.externalRating?.let { rating ->
                        Text(
                            text = formatExternalRatingOnTen(rating.score, rating.maxScore),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                        )
                    }
                }
            }
            val duplicateMarker = duplicateState.marker()
            if (duplicateMarker != null) {
                Icon(
                    imageVector = duplicateMarker.icon,
                    contentDescription = stringResource(duplicateMarker.contentDescriptionResId),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    tint = duplicateMarker.tint,
                )
            }
        }
    }
}

private class DuplicateMarker(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val contentDescriptionResId: Int,
)

private fun MetadataDuplicateState.marker(): DuplicateMarker? {
    return when (this) {
        MetadataDuplicateState.None -> null
        MetadataDuplicateState.Exact -> DuplicateMarker(
            icon = Icons.Filled.CheckCircle,
            tint = OmnilogColors.Completed,
            contentDescriptionResId = R.string.metadata_already_in_library,
        )
        MetadataDuplicateState.Possible -> DuplicateMarker(
            icon = Icons.Filled.Warning,
            tint = OmnilogColors.Paused,
            contentDescriptionResId = R.string.metadata_possible_duplicate,
        )
    }
}

enum class MetadataDuplicateState {
    None,
    Exact,
    Possible,
}

@Composable
private fun ResultChip(
    text: String,
    accent: Color? = null,
) {
    val chipColor = accent?.copy(alpha = 0.18f) ?: OmnilogColors.AppLine.copy(alpha = 0.46f)
    val textColor = accent ?: OmnilogColors.AppMuted
    Box(
        modifier = Modifier
            .background(
                color = chipColor,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun MediaType.sectionAccent(): Color {
    return when (this) {
        MediaType.Anime -> OmnilogColors.Anime
        MediaType.Book -> OmnilogColors.Books
        MediaType.Movie,
        MediaType.TvShow,
            -> OmnilogColors.Tv
        MediaType.Game -> OmnilogColors.Games
    }
}

private enum class AddMediaStep {
    Search,
    Review,
    Manual,
}

@Composable
private fun MediaType.label(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.media_type_anime)
        MediaType.Book -> stringResource(R.string.media_type_book)
        MediaType.Movie -> stringResource(R.string.media_type_movie)
        MediaType.TvShow -> stringResource(R.string.media_type_tv_show)
        MediaType.Game -> stringResource(R.string.media_type_game)
    }
}

@Composable
private fun OwnershipType.label(): String {
    return when (this) {
        OwnershipType.None -> stringResource(R.string.ownership_none)
        OwnershipType.Physical -> stringResource(R.string.ownership_physical)
        OwnershipType.Digital -> stringResource(R.string.ownership_digital)
        OwnershipType.Subscription -> stringResource(R.string.ownership_subscription)
        OwnershipType.Borrowed -> stringResource(R.string.ownership_borrowed)
    }
}

@Composable
private fun ConsumptionPlatformType.label(): String {
    return when (this) {
        ConsumptionPlatformType.Physical -> stringResource(R.string.platform_type_physical)
        ConsumptionPlatformType.DigitalStore -> stringResource(R.string.platform_type_digital_store)
        ConsumptionPlatformType.Streaming -> stringResource(R.string.platform_type_streaming)
        ConsumptionPlatformType.Ebook -> stringResource(R.string.platform_type_ebook)
        ConsumptionPlatformType.Library -> stringResource(R.string.platform_type_library)
        ConsumptionPlatformType.Other -> stringResource(R.string.platform_type_other)
    }
}

@Composable
private fun TrackingStatus.label(): String {
    return when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }
}

@Composable
private fun progressUnitLabel(mediaType: MediaType, value: Int): String {
    return when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
            -> if (value == 1) {
            stringResource(R.string.progress_unit_episode_one)
        } else {
            stringResource(R.string.progress_unit_episode_many)
        }
        MediaType.Book -> if (value == 1) {
            stringResource(R.string.progress_unit_page_one)
        } else {
            stringResource(R.string.progress_unit_page_many)
        }
        MediaType.Movie -> if (value == 1) {
            stringResource(R.string.progress_unit_minute_one)
        } else {
            stringResource(R.string.progress_unit_minute_many)
        }
        MediaType.Game -> if (value == 1) {
            stringResource(R.string.progress_unit_hour_one)
        } else {
            stringResource(R.string.progress_unit_hour_many)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return trim().takeIf { it.isNotBlank() }?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
