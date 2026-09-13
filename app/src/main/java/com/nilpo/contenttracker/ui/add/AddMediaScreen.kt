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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.BookEditionMetadata
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSeasonSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.LanguageDropdown
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.common.MediaMetadataHero
import com.nilpo.contenttracker.ui.common.MediaMetadataHeroGenres
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.OmnilogDropdownField
import com.nilpo.contenttracker.ui.common.partialSearchFailureMessage
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.common.SynopsisText
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.common.TrackingDateRange
import com.nilpo.contenttracker.ui.common.TrackingNotesField
import com.nilpo.contenttracker.ui.common.TrackingProgressField
import com.nilpo.contenttracker.ui.common.TrackingRatingSelector
import com.nilpo.contenttracker.ui.common.TrackingStatusSelector
import com.nilpo.contenttracker.ui.common.CollectionEntryChip
import com.nilpo.contenttracker.ui.common.CollectionPickerOption
import com.nilpo.contenttracker.ui.common.CollectionPickerSheet
import com.nilpo.contenttracker.ui.common.bestCollectionMatch
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.toCollectionOrderInput
import com.nilpo.contenttracker.ui.common.toCollectionOrderOrNull
import com.nilpo.contenttracker.ui.common.toCollectionPickerOptions
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.delay
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AddMediaScreen(
    initialMediaType: MediaType,
    availableMediaTypes: List<MediaType>,
    library: List<TrackedMedia>,
    initialCollection: MediaCollection? = null,
    initialCollectionName: String? = null,
    initialCollectionOrder: String? = null,
    onSave: (AddTrackedMediaRequest) -> Unit,
    metadataUiState: MetadataSearchUiState,
    onMetadataQueryChange: (String) -> Unit,
    onMetadataSearch: () -> Unit,
    onMetadataSearchSubmitted: () -> Unit,
    onMetadataSuggestionSelected: (MetadataSuggestion) -> Unit,
    onMetadataDetailsRetry: () -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember {
        mutableStateOf(
            if (metadataUiState.selectedSuggestion != null) AddMediaStep.Review else AddMediaStep.Search,
        )
    }
    var title by remember { mutableStateOf("") }
    var totalProgress by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(ItemLanguage.Original) }
    var platform by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf(initialMediaType) }
    var selectedStatus by remember { mutableStateOf(TrackingStatus.Planned) }
    var isOwned by remember { mutableStateOf(false) }
    var selectedPlatformType by remember { mutableStateOf(ConsumptionPlatformType.Other) }
    var initialProgress by remember { mutableStateOf("0") }
    var initialRating by remember { mutableStateOf<Int?>(null) }
    var initialNotes by remember { mutableStateOf("") }
    var initialStartedAt by remember { mutableStateOf("") }
    var initialFinishedAt by remember { mutableStateOf("") }
    var collectionName by remember { mutableStateOf(initialCollection?.name ?: initialCollectionName.orEmpty()) }
    var collectionOrder by remember { mutableStateOf(initialCollectionOrder.orEmpty()) }
    var selectedSeason by remember { mutableStateOf<MetadataSeasonSuggestion?>(null) }
    var selectedBookEdition by remember { mutableStateOf<BookEditionMetadata?>(null) }
    var useWholeSeries by remember { mutableStateOf(false) }
    val selectedMetadataSuggestion = metadataUiState.selectedSuggestion
    val selectedMetadataForForm = selectedMetadataSuggestion?.let { suggestion ->
        selectedBookEdition?.toMetadataSuggestion(suggestion)
            ?: selectedSeason?.toMetadataSuggestion(suggestion)
            ?: suggestion
    }
    val collectionOptionsForType = remember(library, selectedMediaType) {
        library.toCollectionPickerOptions(forType = selectedMediaType)
    }
    val matchedCollection = collectionOptionsForType.map { it.collection }.bestCollectionMatch(collectionName)
        ?: initialCollection?.takeIf { collection ->
            collection.name.equals(collectionName.trim(), ignoreCase = true)
        }

    fun applyMetadataSuggestion(suggestion: MetadataSuggestion, season: MetadataSeasonSuggestion? = null) {
        title = suggestion.title
        language = ItemLanguage.normalize(suggestion.language) ?: ItemLanguage.Original
        totalProgress = suggestion.progressTotal
            ?.takeUnless { suggestion.mediaType == MediaType.Game }
            ?.toString()
            .orEmpty()
        selectedMediaType = suggestion.mediaType
        if (season != null) {
            collectionName = suggestion.collectionTitle.orEmpty()
            collectionOrder = season.seasonNumber.toString()
        }
    }

    LaunchedEffect(selectedMetadataSuggestion?.source, selectedMetadataSuggestion?.externalId) {
        selectedSeason = null
        selectedBookEdition = null
        useWholeSeries = false
    }

    LaunchedEffect(selectedMetadataForForm, metadataUiState.isLoadingDetails) {
        selectedMetadataSuggestion?.let { suggestion ->
            if (
                suggestion.source == MetadataSource.OpenLibrary &&
                suggestion.mediaType == MediaType.Book &&
                selectedBookEdition == null &&
                (metadataUiState.isLoadingDetails || suggestion.bookEditionSuggestions.isNotEmpty())
            ) {
                selectedMediaType = suggestion.mediaType
                if (step != AddMediaStep.Manual) {
                    step = AddMediaStep.BookEdition
                }
                return@LaunchedEffect
            }
            if (
                !useWholeSeries &&
                suggestion.shouldUseTvSeasonPicker(metadataUiState.isLoadingDetails) &&
                selectedSeason == null
            ) {
                selectedMediaType = suggestion.mediaType
                if (step != AddMediaStep.Manual) {
                    step = AddMediaStep.Season
                }
                return@LaunchedEffect
            }
        }

        selectedMetadataForForm?.let { suggestion ->
            applyMetadataSuggestion(suggestion, selectedSeason)
            if (step != AddMediaStep.Manual) {
                step = AddMediaStep.Review
            }
        }
    }
    val applyStatus: (TrackingStatus) -> Unit = { status ->
        selectedStatus = status
        if (status == TrackingStatus.InProgress && initialStartedAt.isBlank()) {
            initialStartedAt = LocalDate.now().toString()
        }
        if (status == TrackingStatus.Completed) {
            selectedMediaType.effectiveProgressTotal(totalProgress)?.let { maxProgress ->
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
                    onSearchSubmitted = onMetadataSearchSubmitted,
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
                        AddMediaStep.Season -> MetadataSeasonSelectionStep(
                            suggestion = selectedMetadataSuggestion,
                            isLoadingDetails = metadataUiState.isLoadingDetails,
                            hasDetailsError = metadataUiState.hasDetailsError,
                            accent = selectedMediaType.sectionAccent(),
                            onSeasonSelected = { season ->
                                useWholeSeries = false
                                selectedSeason = season
                                selectedMetadataSuggestion?.let { seriesSuggestion ->
                                    applyMetadataSuggestion(season.toMetadataSuggestion(seriesSuggestion), season)
                                    step = AddMediaStep.Review
                                }
                            },
                            onUseSeries = {
                                selectedSeason = null
                                useWholeSeries = true
                                selectedMetadataSuggestion?.let { suggestion ->
                                    applyMetadataSuggestion(suggestion)
                                    step = AddMediaStep.Review
                                }
                            },
                            onRetryDetails = onMetadataDetailsRetry,
                            onBackToSearch = { step = AddMediaStep.Search },
                            onCancel = onCancel,
                        )
                        AddMediaStep.BookEdition -> BookEditionSelectionStep(
                            suggestion = selectedMetadataSuggestion,
                            isLoadingDetails = metadataUiState.isLoadingDetails,
                            hasDetailsError = metadataUiState.hasDetailsError,
                            accent = selectedMediaType.sectionAccent(),
                            onEditionSelected = { edition ->
                                selectedBookEdition = edition
                                selectedMetadataSuggestion?.let { work ->
                                    applyMetadataSuggestion(edition.toMetadataSuggestion(work))
                                    step = AddMediaStep.Review
                                }
                            },
                            onRetryDetails = onMetadataDetailsRetry,
                            onBackToSearch = { step = AddMediaStep.Search },
                            onCancel = onCancel,
                        )
                        AddMediaStep.Review -> MetadataReviewStep(
                    suggestion = selectedMetadataForForm,
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
                        val maxProgress = selectedMediaType.effectiveProgressTotal(totalProgress)
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
                    platform = platform,
                    onPlatformChange = { platform = it },
                    selectedPlatformType = selectedPlatformType,
                    onPlatformTypeSelected = { selectedPlatformType = it },
                    isOwned = isOwned,
                    onOwnedChange = { isOwned = it },
                    availableCollections = collectionOptionsForType,
                    itemTitle = title,
                    providerCollectionTitle = selectedMetadataForForm?.collectionTitle,
                    collectionName = collectionName,
                    onCollectionNameChange = { collectionName = it },
                    collectionOrder = collectionOrder,
                    onCollectionOrderChange = { collectionOrder = it.toCollectionOrderInput() },
                    onRetryDetails = onMetadataDetailsRetry,
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = selectedMediaType.effectiveProgressTotal(totalProgress),
                                initialStatus = selectedStatus,
                                initialProgress = initialProgress.toIntOrNull() ?: 0,
                                initialRatingHalfPoints = initialRating,
                                initialNotes = initialNotes.takeIf { it.isNotBlank() },
                                initialStartedAt = initialStartedAt.toLocalDateOrNull(),
                                initialFinishedAt = initialFinishedAt.toLocalDateOrNull(),
                                isOwned = isOwned,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                                collectionId = matchedCollection?.id,
                                newCollectionName = if (matchedCollection == null) {
                                    collectionName.trim().takeIf { it.isNotBlank() }
                                } else {
                                    null
                                },
                                collectionSortOrder = collectionOrder.toCollectionOrderOrNull(),
                                originalTitle = selectedMetadataForForm?.originalTitle,
                                releaseYear = selectedMetadataForForm?.releaseYear,
                                language = ItemLanguage.normalize(selectedMetadataForForm?.language)
                                    .takeUnless { selectedMediaType == MediaType.Game },
                                genres = selectedMetadataForForm?.genres.orEmpty(),
                                creators = selectedMetadataForForm?.creators.orEmpty(),
                                credits = selectedMetadataForForm?.credits.orEmpty(),
                                sourceUrl = selectedMetadataForForm?.sourceUrl,
                                externalRating = selectedMetadataForForm?.externalRating,
                                externalRatings = selectedMetadataForForm?.externalRatings.orEmpty(),
                                popularityScore = selectedMetadataForForm?.popularityScore,
                                rankingPosition = selectedMetadataForForm?.rankingPosition,
                                rankingLabel = selectedMetadataForForm?.rankingLabel,
                                providerCollectionTitle = selectedMetadataForForm?.collectionTitle,
                                ratingDistributionJson = selectedMetadataForForm?.ratingDistributionJson,
                                popularityJson = selectedMetadataForForm?.popularityJson,
                                rankingJson = selectedMetadataForForm?.rankingJson,
                                metadataSource = selectedMetadataForForm?.source,
                                metadataExternalId = selectedMetadataForForm?.externalId,
                                malId = selectedMetadataForForm?.malId,
                                coverUrl = selectedMetadataForForm?.coverUrl,
                                synopsis = selectedMetadataForForm?.synopsis,
                            ),
                        )
                    },
                    onCancel = onCancel,
                )
                        AddMediaStep.Manual -> ManualAddStep(
                    availableMediaTypes = availableMediaTypes,
                    selectedMediaType = selectedMediaType,
                    onMediaTypeSelected = {
                        selectedMediaType = it
                        if (it == MediaType.Game) {
                            totalProgress = ""
                        }
                    },
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
                    language = language.takeUnless { selectedMediaType == MediaType.Game },
                    onLanguageChange = if (selectedMediaType == MediaType.Game) {
                        null
                    } else {
                        { value -> language = ItemLanguage.normalize(value) ?: ItemLanguage.Original }
                    },
                    platform = platform,
                    onPlatformChange = { platform = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = applyStatus,
                    isOwned = isOwned,
                    onOwnedChange = { isOwned = it },
                    selectedPlatformType = selectedPlatformType,
                    onPlatformTypeSelected = { selectedPlatformType = it },
                    initialProgress = initialProgress,
                    onInitialProgressChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        val maxProgress = selectedMediaType.effectiveProgressTotal(totalProgress)
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
                    availableCollections = collectionOptionsForType,
                    itemTitle = title,
                    providerCollectionTitle = null,
                    collectionName = collectionName,
                    onCollectionNameChange = { collectionName = it },
                    collectionOrder = collectionOrder,
                    onCollectionOrderChange = { collectionOrder = it.toCollectionOrderInput() },
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = selectedMediaType.effectiveProgressTotal(totalProgress),
                                language = ItemLanguage.normalize(language)
                                    .takeUnless { selectedMediaType == MediaType.Game },
                                initialStatus = selectedStatus,
                                initialProgress = initialProgress.toIntOrNull() ?: 0,
                                initialRatingHalfPoints = initialRating,
                                initialNotes = initialNotes.takeIf { it.isNotBlank() },
                                initialStartedAt = initialStartedAt.toLocalDateOrNull(),
                                initialFinishedAt = initialFinishedAt.toLocalDateOrNull(),
                                isOwned = isOwned,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                                collectionId = matchedCollection?.id,
                                newCollectionName = if (matchedCollection == null) {
                                    collectionName.trim().takeIf { it.isNotBlank() }
                                } else {
                                    null
                                },
                                collectionSortOrder = collectionOrder.toCollectionOrderOrNull(),
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
    onSearchSubmitted: () -> Unit,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
    onManualAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.query) {
        if (uiState.query.trim().length >= 3) {
            delay(300)
            onSearch()
        } else if (uiState.query.isBlank()) {
            onSearch()
        }
    }
    // Dismiss the keyboard once results are on screen so they are not hidden behind it.
    LaunchedEffect(uiState.hasSearched, uiState.isLoading, uiState.suggestions.size) {
        if (uiState.hasSearched && !uiState.isLoading && uiState.suggestions.isNotEmpty()) {
            keyboardController?.hide()
        }
    }
    val accent = initialMediaType.sectionAccent()
    val submitSearch = {
        keyboardController?.hide()
        onSearchSubmitted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AddScreenHeader(
            title = initialMediaType.addTitle(),
            subtitle = stringResource(R.string.add_search_subtitle),
        )
        if (initialMediaType == MediaType.Game) {
            Text(
                text = stringResource(R.string.steam_id_search_hint),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }

        DashboardStyleSearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onSearchSubmitted = submitSearch,
            isLoading = uiState.isLoading,
            accent = accent,
            leadingIcon = Icons.Filled.Search,
        ) {
            if (uiState.query.isNotBlank()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = OmnilogTheme.colors.appMuted,
                    )
                }
            }
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            } else {
                FilledTonalIconButton(
                    onClick = submitSearch,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = accent.copy(alpha = 0.16f),
                        contentColor = accent,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_action),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataSearchResults(
                uiState = uiState,
                accent = accent,
                onSuggestionSelected = onSuggestionSelected,
                onRetrySearch = submitSearch,
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
            OutlinedButton(
                onClick = onManualAdd,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
            ) {
                Text(text = stringResource(R.string.add_manual))
            }
        }
    }
}

@Composable
private fun MetadataSearchResults(
    uiState: MetadataSearchUiState,
    accent: Color,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
    onRetrySearch: () -> Unit,
    duplicateStateForSuggestion: (MetadataSuggestion) -> MetadataDuplicateState,
) {
    val retryAction = EmptyStateAction(
        label = stringResource(R.string.retry_action),
        onClick = onRetrySearch,
    )
    when {
        // The search bar already spins while a query runs, so this panel stays text-only.
        uiState.isLoading -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_loading),
            accent = accent,
        )
        uiState.hasError -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_error),
            accent = accent,
            textColor = MaterialTheme.colorScheme.error,
            action = retryAction,
        )
        uiState.hasSearched && uiState.suggestions.isEmpty() -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_empty),
            accent = accent,
        )
        !uiState.hasSearched -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_prompt),
            accent = accent,
        )
        else -> {
            if (uiState.hasPartialError) {
                OmnilogStatusPanel(
                    text = partialSearchFailureMessage(uiState.failedSources.toList()),
                    accent = accent,
                    action = retryAction,
                )
            }
            uiState.suggestions.forEach { suggestion ->
                MetadataSuggestionRow(
                    suggestion = suggestion,
                    accent = suggestion.mediaType.sectionAccent(),
                    duplicateState = duplicateStateForSuggestion(suggestion),
                    onClick = { onSuggestionSelected(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun MetadataSeasonSelectionStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    accent: Color,
    onSeasonSelected: (MetadataSeasonSuggestion) -> Unit,
    onUseSeries: () -> Unit,
    onRetryDetails: () -> Unit,
    onBackToSearch: () -> Unit,
    onCancel: () -> Unit,
) {
    AddScreenHeader(
        title = stringResource(R.string.metadata_season_picker_title),
        subtitle = suggestion?.title,
    )

    when {
        isLoadingDetails -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_season_picker_loading),
            accent = accent,
            showProgressIndicator = true,
        )
        hasDetailsError -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_details_error),
            accent = accent,
            textColor = MaterialTheme.colorScheme.error,
            action = EmptyStateAction(
                label = stringResource(R.string.retry_action),
                onClick = onRetryDetails,
            ),
        )
        suggestion?.seasonSuggestions.isNullOrEmpty() -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_season_picker_empty),
            accent = accent,
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestion?.seasonSuggestions.orEmpty().forEach { season ->
                SeasonSuggestionRow(
                    season = season,
                    accent = accent,
                    onClick = { onSeasonSelected(season) },
                )
            }
        }
    }

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
        TextButton(
            enabled = !isLoadingDetails,
            colors = ButtonDefaults.textButtonColors(contentColor = accent),
            onClick = onUseSeries,
        ) {
            Text(text = stringResource(R.string.metadata_season_use_series))
        }
    }
}

@Composable
private fun SeasonSuggestionRow(
    season: MetadataSeasonSuggestion,
    accent: Color,
    onClick: () -> Unit,
) {
    val detailParts = buildList {
        season.releaseYear?.let { add(it.toString()) }
        season.progressTotal?.let { add(stringResource(R.string.metadata_season_episode_count, it)) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetadataCoverImage(
                coverUrl = season.coverUrl,
                modifier = Modifier.size(width = 48.dp, height = 72.dp),
                shape = RoundedCornerShape(6.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = season.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detailParts.isNotEmpty()) {
                    Text(
                        text = detailParts.joinToString(" | "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                season.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    SynopsisText(
                        body = synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ResultChip(
                text = season.seasonNumber.toString(),
                accent = accent,
            )
        }
    }
}

@Composable
private fun BookEditionSelectionStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    accent: Color,
    onEditionSelected: (BookEditionMetadata) -> Unit,
    onRetryDetails: () -> Unit,
    onBackToSearch: () -> Unit,
    onCancel: () -> Unit,
) {
    val editions = suggestion?.bookEditionSuggestions.orEmpty()
    val availableLanguages = remember(editions) {
        editions.mapNotNull { edition -> ItemLanguage.normalize(edition.language) }
            .distinct()
            .sorted()
    }
    var selectedLanguage by remember(suggestion?.externalId, availableLanguages) {
        mutableStateOf(suggestion?.language?.let(ItemLanguage::normalize)?.takeIf { it in availableLanguages })
    }
    val visibleEditions = editions.filter { edition ->
        selectedLanguage == null || ItemLanguage.normalize(edition.language) == selectedLanguage
    }

    AddScreenHeader(
        title = stringResource(R.string.book_edition_picker_title),
        subtitle = suggestion?.title,
    )

    when {
        isLoadingDetails -> OmnilogStatusPanel(
            text = stringResource(R.string.book_edition_picker_loading),
            accent = accent,
            showProgressIndicator = true,
        )
        hasDetailsError -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_details_error),
            accent = accent,
            textColor = MaterialTheme.colorScheme.error,
            action = EmptyStateAction(
                label = stringResource(R.string.retry_action),
                onClick = onRetryDetails,
            ),
        )
        editions.isEmpty() -> OmnilogStatusPanel(
            text = stringResource(R.string.book_edition_picker_empty),
            accent = accent,
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (availableLanguages.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    item {
                        EditionLanguageFilterChip(
                            label = stringResource(R.string.book_edition_all_languages),
                            selected = selectedLanguage == null,
                            accent = accent,
                            onClick = { selectedLanguage = null },
                        )
                    }
                    items(availableLanguages) { language ->
                        EditionLanguageFilterChip(
                            label = languageLabel(language),
                            selected = selectedLanguage == language,
                            accent = accent,
                            onClick = { selectedLanguage = language },
                        )
                    }
                }
            }
            visibleEditions.forEach { edition ->
                BookEditionSuggestionRow(
                    edition = edition,
                    accent = accent,
                    onClick = { onEditionSelected(edition) },
                )
            }
        }
    }

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
    }
}

@Composable
private fun EditionLanguageFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else OmnilogTheme.colors.appPanel,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.70f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) accent else OmnilogTheme.colors.appMuted,
        )
    }
}

@Composable
private fun BookEditionSuggestionRow(
    edition: BookEditionMetadata,
    accent: Color,
    onClick: () -> Unit,
) {
    val details = buildList {
        edition.language?.let(::add)
        edition.releaseYear?.let { add(it.toString()) }
        edition.pageCount?.let { add(stringResource(R.string.book_edition_pages, it)) }
        edition.isbn?.let { add(stringResource(R.string.book_edition_isbn, it)) }
        edition.format?.let(::add)
        edition.publisher?.let(::add)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetadataCoverImage(
                coverUrl = edition.coverUrl,
                modifier = Modifier.size(width = 58.dp, height = 86.dp),
                shape = RoundedCornerShape(6.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                edition.title?.let { title ->
                    Text(
                        text = displayMediaTitle(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ResultChip(text = stringResource(R.string.book_edition_select), accent = accent)
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
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
    isOwned: Boolean,
    onOwnedChange: (Boolean) -> Unit,
    availableCollections: List<CollectionPickerOption>,
    itemTitle: String,
    providerCollectionTitle: String?,
    collectionName: String,
    onCollectionNameChange: (String) -> Unit,
    collectionOrder: String,
    onCollectionOrderChange: (String) -> Unit,
    onRetryDetails: () -> Unit,
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
            accent = accent,
            onRetryDetails = onRetryDetails,
        )
    }

    FirstSessionForm(
        mediaType = selectedMediaType,
        progressTotal = selectedMediaType.effectiveProgressTotal(totalProgress),
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
        accent = accent,
    )

    CollectionAssignmentForm(
        availableCollections = availableCollections,
        itemTitle = itemTitle,
        providerCollectionTitle = providerCollectionTitle,
        collectionName = collectionName,
        onCollectionNameChange = onCollectionNameChange,
        collectionOrder = collectionOrder,
        onCollectionOrderChange = onCollectionOrderChange,
        accent = accent,
    )

    OwnershipToggle(
        isOwned = isOwned,
        onOwnedChange = onOwnedChange,
        accent = accent,
    )

    OptionalAddDetails(
        platform = platform,
        onPlatformChange = onPlatformChange,
        selectedPlatformType = selectedPlatformType,
        onPlatformTypeSelected = onPlatformTypeSelected,
        initialNotes = initialNotes,
        onInitialNotesChange = onInitialNotesChange,
        accent = accent,
    )

    ReviewActionRow(
        title = title,
        selectedStatus = selectedStatus,
        accent = accent,
        isLoadingDetails = isLoadingDetails,
        onBackToSearch = onBackToSearch,
        onCancel = onCancel,
        onSave = onSave,
    )
}

@Composable
private fun MetadataReviewPreview(
    suggestion: MetadataSuggestion,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    accent: Color,
    onRetryDetails: () -> Unit,
) {
    val metadata = suggestion
        .copy(progressTotal = suggestion.progressTotal.takeUnless { suggestion.mediaType == MediaType.Game })
        .toMediaMetadataUi()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MediaMetadataHero(
            metadata = metadata,
            isLoadingDetails = isLoadingDetails,
            loadingAccent = accent,
        )
        if (hasDetailsError) {
            // Non-blocking: the copy says the available metadata is usable, so this offers a
            // retry without taking the review step away from the user.
            OmnilogStatusPanel(
                text = stringResource(R.string.metadata_details_error),
                accent = accent,
                textColor = MaterialTheme.colorScheme.error,
                action = EmptyStateAction(
                    label = stringResource(R.string.retry_action),
                    onClick = onRetryDetails,
                ),
                actionEnabled = !isLoadingDetails,
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
            color = OmnilogTheme.colors.appMuted,
            fontWeight = FontWeight.SemiBold,
        )
        SynopsisText(
            body = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appInk.copy(alpha = 0.84f),
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
    selectedStatus: TrackingStatus,
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
            Text(text = stringResource(R.string.add_with_status, selectedStatus.label()))
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
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormSectionHeader(title = stringResource(R.string.field_status))
        TrackingStatusSelector(
            selectedStatus = selectedStatus,
            accent = accent,
            onStatusSelected = onStatusSelected,
        )

        when (selectedStatus) {
            TrackingStatus.Planned -> Unit
            TrackingStatus.InProgress -> {
                ProgressFieldSection(
                    value = initialProgress,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    accent = selectedStatus.stateColor,
                    onValueChange = onInitialProgressChange,
                )
                DateFieldsSection(
                    startedAt = initialStartedAt,
                    onStartedAtChange = onInitialStartedAtChange,
                    accent = selectedStatus.stateColor,
                )
            }
            TrackingStatus.Completed -> {
                ProgressFieldSection(
                    value = initialProgress,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    labelResId = R.string.field_final_progress,
                    accent = selectedStatus.stateColor,
                    onValueChange = onInitialProgressChange,
                )
                RatingSection(
                    initialRating = initialRating,
                    accent = selectedStatus.stateColor,
                    onInitialRatingSelected = onInitialRatingSelected,
                )
                DateFieldsSection(
                    startedAt = initialStartedAt,
                    onStartedAtChange = onInitialStartedAtChange,
                    finishedAt = initialFinishedAt,
                    onFinishedAtChange = onInitialFinishedAtChange,
                    accent = selectedStatus.stateColor,
                )
            }
            TrackingStatus.Paused,
            TrackingStatus.Dropped,
                -> {
                ProgressFieldSection(
                    value = initialProgress,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    accent = selectedStatus.stateColor,
                    onValueChange = onInitialProgressChange,
                )
                RatingSection(
                    initialRating = initialRating,
                    accent = selectedStatus.stateColor,
                    onInitialRatingSelected = onInitialRatingSelected,
                )
                DateFieldsSection(
                    startedAt = initialStartedAt,
                    onStartedAtChange = onInitialStartedAtChange,
                    finishedAt = initialFinishedAt,
                    onFinishedAtChange = onInitialFinishedAtChange,
                    accent = selectedStatus.stateColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RatingSection(
    initialRating: Int?,
    accent: Color,
    onInitialRatingSelected: (Int?) -> Unit,
) {
    FormDivider()
    FormSectionHeader(title = stringResource(R.string.field_rating))
    TrackingRatingSelector(
        currentRatingHalfPoints = initialRating,
        accent = accent,
        onRatingSelected = onInitialRatingSelected,
    )
}

@Composable
private fun ProgressFieldSection(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    labelResId: Int = R.string.field_progress,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    FormDivider()
    TrackingProgressField(
        value = value,
        progressTotal = progressTotal,
        mediaType = mediaType,
        label = stringResource(labelResId),
        accent = accent,
        onValueChange = onValueChange,
    )
}

@Composable
private fun DateFieldsSection(
    startedAt: String,
    onStartedAtChange: (String) -> Unit,
    accent: Color,
    finishedAt: String? = null,
    onFinishedAtChange: ((String) -> Unit)? = null,
) {
    FormDivider()
    FormSectionHeader(title = stringResource(R.string.session_dates))
    TrackingDateRange(
        startedLabel = stringResource(R.string.session_started_label),
        startedValue = startedAt,
        accent = accent,
        onStartedValueChange = onStartedAtChange,
        finishedLabel = finishedAt?.let { stringResource(R.string.session_finished_label) },
        finishedValue = finishedAt,
        onFinishedValueChange = onFinishedAtChange,
    )
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogTheme.colors.appMuted,
        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
    )
}

@Composable
private fun FormDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = OmnilogTheme.colors.appLine.copy(alpha = 0.70f),
    )
}

@Composable
private fun ReviewProgressField(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    labelResId: Int = R.string.field_progress,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    val current = value.toIntOrNull() ?: 0
    val maximum = progressTotal ?: Int.MAX_VALUE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange((current - 1).coerceAtLeast(0).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                ),
            ) {
                Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Light)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.labelMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        val next = digits.toIntOrNull()?.coerceIn(0, maximum)?.toString() ?: digits
                        onValueChange(next)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = accent,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = reviewTextFieldColors(accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                progressTotal?.takeIf { it > 0 }?.let { total ->
                    Text(
                        text = "de $total ${progressUnitLabel(mediaType, total)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
            FilledTonalIconButton(
                onClick = { onValueChange((current + 1).coerceAtMost(maximum).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                ),
            ) {
                Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Light)
            }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear_date),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                        )
                    }
                }
                TextButton(
                    onClick = { showPicker = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                ) {
                    Text(text = stringResource(R.string.pick_date))
                }
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
    unfocusedBorderColor = OmnilogTheme.colors.appLine,
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
    language: String?,
    onLanguageChange: ((String) -> Unit)?,
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    isOwned: Boolean,
    onOwnedChange: (Boolean) -> Unit,
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
    availableCollections: List<CollectionPickerOption>,
    itemTitle: String,
    providerCollectionTitle: String?,
    collectionName: String,
    onCollectionNameChange: (String) -> Unit,
    collectionOrder: String,
    onCollectionOrderChange: (String) -> Unit,
    onBackToSearch: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val accent = selectedMediaType.sectionAccent()

    Text(
        text = stringResource(R.string.add_manual),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = OmnilogTheme.colors.appInk,
    )

    TrackingSetupForm(
        availableMediaTypes = availableMediaTypes,
        selectedMediaType = selectedMediaType,
        onMediaTypeSelected = onMediaTypeSelected,
        title = title,
        onTitleChange = onTitleChange,
        totalProgress = totalProgress,
        onTotalProgressChange = onTotalProgressChange,
    )

    FirstSessionForm(
        mediaType = selectedMediaType,
        progressTotal = selectedMediaType.effectiveProgressTotal(totalProgress),
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
        accent = accent,
    )

    CollectionAssignmentForm(
        availableCollections = availableCollections,
        itemTitle = itemTitle,
        providerCollectionTitle = providerCollectionTitle,
        collectionName = collectionName,
        onCollectionNameChange = onCollectionNameChange,
        collectionOrder = collectionOrder,
        onCollectionOrderChange = onCollectionOrderChange,
        accent = accent,
    )

    OwnershipToggle(
        isOwned = isOwned,
        onOwnedChange = onOwnedChange,
        accent = accent,
    )

    OptionalAddDetails(
        platform = platform,
        onPlatformChange = onPlatformChange,
        selectedPlatformType = selectedPlatformType,
        onPlatformTypeSelected = onPlatformTypeSelected,
        initialNotes = initialNotes,
        onInitialNotesChange = onInitialNotesChange,
        language = language,
        onLanguageChange = onLanguageChange,
        accent = accent,
    )

    ReviewActionRow(
        title = title,
        selectedStatus = selectedStatus,
        accent = accent,
        isLoadingDetails = false,
        onBackToSearch = onBackToSearch,
        onCancel = onCancel,
        onSave = onSave,
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
) {
    val accent = selectedMediaType.sectionAccent()
    if (availableMediaTypes.size > 1) {
        OmnilogDropdownField(
            selectedOption = selectedMediaType,
            options = availableMediaTypes,
            optionLabel = { it.label() },
            onOptionSelected = onMediaTypeSelected,
            label = stringResource(R.string.field_media_type),
            optionColor = { it.sectionAccent() },
            optionIcon = { mediaType, tint ->
                Icon(
                    painter = painterResource(mediaType.dropdownIconResId),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            },
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

    if (selectedMediaType != MediaType.Game) {
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
    }

}

@Composable
private fun OwnershipToggle(
    isOwned: Boolean,
    onOwnedChange: (Boolean) -> Unit,
    accent: Color,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onOwnedChange(!isOwned) },
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, if (isOwned) accent else OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = isOwned,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = accent,
                    checkmarkColor = Color.Black,
                    uncheckedColor = OmnilogTheme.colors.appMuted,
                ),
            )
            Text(
                text = stringResource(R.string.owned_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isOwned) FontWeight.Bold else FontWeight.Medium,
                color = OmnilogTheme.colors.appInk,
            )
        }
    }
}

@Composable
private fun OptionalAddDetails(
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
    initialNotes: String,
    onInitialNotesChange: (String) -> Unit,
    accent: Color,
    language: String? = null,
    onLanguageChange: ((String) -> Unit)? = null,
) {
    var showDetails by remember { mutableStateOf(false) }

    TextButton(
        onClick = { showDetails = !showDetails },
        colors = ButtonDefaults.textButtonColors(contentColor = accent),
    ) {
        Text(text = stringResource(if (showDetails) R.string.show_less else R.string.add_more_details))
    }

    if (!showDetails) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormDivider()
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
            label = stringResource(R.string.field_platform_type),
            options = ConsumptionPlatformType.entries,
            selectedOption = selectedPlatformType,
            optionLabel = { it.label() },
            onOptionSelected = onPlatformTypeSelected,
        )
        if (language != null && onLanguageChange != null) {
            LanguageDropdown(
                value = language,
                onValueChange = onLanguageChange,
                label = stringResource(R.string.field_language),
                accent = accent,
            )
        }
        FormSectionHeader(title = stringResource(R.string.field_notes))
        TrackingNotesField(
            value = initialNotes,
            accent = accent,
            onValueChange = onInitialNotesChange,
        )
    }
}

@Composable
private fun CollectionAssignmentForm(
    availableCollections: List<CollectionPickerOption>,
    itemTitle: String,
    providerCollectionTitle: String?,
    collectionName: String,
    onCollectionNameChange: (String) -> Unit,
    collectionOrder: String,
    onCollectionOrderChange: (String) -> Unit,
    accent: Color,
) {
    var showSheet by remember { mutableStateOf(false) }
    val trimmedName = collectionName.trim()
    val selectedOption = availableCollections.firstOrNull {
        it.collection.name.equals(trimmedName, ignoreCase = true)
    }
    val chipLabel = formatCollectionDisplayName(
        trimmedName.takeIf { it.isNotBlank() },
        collectionOrder.toCollectionOrderOrNull(),
    ) ?: stringResource(R.string.collection_sheet_title)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormSectionHeader(title = stringResource(R.string.field_collection))
        CollectionEntryChip(
            label = chipLabel,
            hasSelection = trimmedName.isNotBlank(),
            accent = accent,
            onClick = { showSheet = true },
        )
    }

    if (showSheet) {
        CollectionPickerSheet(
            itemTitle = itemTitle,
            providerCollectionTitle = providerCollectionTitle,
            options = availableCollections,
            initialCollectionId = selectedOption?.collection?.id,
            initialSortOrder = collectionOrder.toCollectionOrderOrNull(),
            accent = accent,
            onDismiss = { showSheet = false },
            onConfirm = { result ->
                val name = when {
                    result.collectionId != null ->
                        availableCollections
                            .firstOrNull { it.collection.id == result.collectionId }
                            ?.collection?.name
                            .orEmpty()
                    result.newCollectionName != null -> result.newCollectionName
                    else -> ""
                }
                onCollectionNameChange(name)
                onCollectionOrderChange(result.sortOrder?.let(::formatCollectionOrder).orEmpty())
                showSheet = false
            },
        )
    }
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
            color = OmnilogTheme.colors.appInk,
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        content?.invoke()
    }
}

@Composable
internal fun DashboardStyleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmitted: () -> Unit = {},
    isLoading: Boolean,
    accent: Color,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(
            1.dp,
            if (query.isNotBlank()) accent.copy(alpha = 0.58f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (query.isNotBlank()) accent else OmnilogTheme.colors.appMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmitted() }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = OmnilogTheme.colors.appInk,
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
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            content()
        }
    }
}

@Composable
internal fun MetadataSuggestionRow(
    suggestion: MetadataSuggestion,
    accent: Color,
    duplicateState: MetadataDuplicateState,
    showSourceChip: Boolean = true,
    borderColor: Color = OmnilogTheme.colors.appLine,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, borderColor),
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
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResultChip(text = suggestion.mediaType.label(), accent = accent)
                        suggestion.multiSeasonCount()?.let { seasonCount ->
                            ResultChip(
                                text = stringResource(R.string.metadata_multi_season_count, seasonCount),
                                accent = accent,
                            )
                        }
                        suggestion.releaseYear?.let { ResultChip(text = it.toString(), accent = accent) }
                        if (showSourceChip) {
                            ResultChip(text = suggestion.source.displayName())
                        }
                    }
                    suggestion.creators.firstOrNull()?.let { creator ->
                        Text(
                            text = creator,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    suggestion.externalRating?.let { rating ->
                        val source = suggestion.externalRatings.firstOrNull { candidate ->
                            candidate.score == rating.score && candidate.maxScore == rating.maxScore
                        }?.source
                        Text(
                            text = formatExternalRating(
                                score = rating.score,
                                maxScore = rating.maxScore,
                                mediaType = suggestion.mediaType,
                                source = source,
                            ),
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

internal class DuplicateMarker(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val contentDescriptionResId: Int,
)

@Composable
@ReadOnlyComposable
internal fun MetadataDuplicateState.marker(): DuplicateMarker? {
    return when (this) {
        MetadataDuplicateState.None -> null
        MetadataDuplicateState.Exact -> DuplicateMarker(
            icon = Icons.Filled.CheckCircle,
            tint = OmnilogTheme.accents.Completed,
            contentDescriptionResId = R.string.metadata_already_in_library,
        )
        MetadataDuplicateState.Possible -> DuplicateMarker(
            icon = Icons.Filled.Warning,
            tint = OmnilogTheme.accents.Paused,
            contentDescriptionResId = R.string.metadata_possible_duplicate,
        )
    }
}

enum class MetadataDuplicateState {
    None,
    Exact,
    Possible,
}

private fun MetadataSuggestion.shouldUseTvSeasonPicker(isLoadingDetails: Boolean): Boolean {
    return source == MetadataSource.Tmdb &&
        mediaType == MediaType.TvShow &&
        !externalId.contains(":season:") &&
        (isLoadingDetails || seasonSuggestions.isNotEmpty())
}

private fun MetadataSuggestion.multiSeasonCount(): Int? {
    if (source != MetadataSource.Tmdb || mediaType != MediaType.TvShow) return null
    val count = seasonSuggestions.count { it.seasonNumber > 0 }
    return count.takeIf { it > 1 }
}

@Composable
internal fun ResultChip(
    text: String,
    accent: Color? = null,
    modifier: Modifier = Modifier,
) {
    val chipColor = accent?.copy(alpha = 0.18f) ?: OmnilogTheme.colors.appLine.copy(alpha = 0.46f)
    val textColor = accent ?: OmnilogTheme.colors.appMuted
    Box(
        modifier = modifier
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

@Composable
@ReadOnlyComposable
private fun MediaType.sectionAccent(): Color {
    return when (this) {
        MediaType.Anime -> OmnilogTheme.accents.Anime
        MediaType.Book -> OmnilogTheme.accents.Books
        MediaType.Movie,
        MediaType.TvShow,
            -> OmnilogTheme.accents.Tv
        MediaType.Game -> OmnilogTheme.accents.Games
    }
}

private val MediaType.dropdownIconResId: Int
    get() = when (this) {
        MediaType.Anime -> R.drawable.ic_nav_anime
        MediaType.Book -> R.drawable.ic_nav_books
        MediaType.Movie -> R.drawable.ic_media_movie
        MediaType.TvShow -> R.drawable.ic_media_series
        MediaType.Game -> R.drawable.ic_nav_games
    }

private enum class AddMediaStep {
    Search,
    Season,
    BookEdition,
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
private fun MediaType.addTitle(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.add_title_anime)
        MediaType.Book -> stringResource(R.string.add_title_book)
        MediaType.Movie -> stringResource(R.string.add_title_movie)
        MediaType.TvShow -> stringResource(R.string.add_title_tv)
        MediaType.Game -> stringResource(R.string.add_title_game)
    }
}

@Composable
internal fun ConsumptionPlatformType.label(): String {
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

private val TrackingStatus.stateColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogTheme.accents.Planned
        TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
        TrackingStatus.Completed -> OmnilogTheme.accents.Completed
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
    }

private fun String.toLocalDateOrNull(): LocalDate? {
    return trim().takeIf { it.isNotBlank() }?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

private fun MediaType.effectiveProgressTotal(totalProgress: String): Int? {
    return totalProgress.toIntOrNull().takeUnless { this == MediaType.Game }
}

