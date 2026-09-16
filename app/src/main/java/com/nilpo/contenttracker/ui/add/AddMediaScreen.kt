package com.nilpo.contenttracker.ui.add

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.BookEditionMetadata
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSeasonSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.AddMediaHeaderActions
import com.nilpo.contenttracker.ui.common.CollectionPickerOption
import com.nilpo.contenttracker.ui.common.CollectionPickerSheet
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.LanguageDropdown
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogDropdownField
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.common.QuickProgressRail
import com.nilpo.contenttracker.ui.common.SynopsisText
import com.nilpo.contenttracker.ui.common.TrackingDateRange
import com.nilpo.contenttracker.ui.common.TrackingNotesField
import com.nilpo.contenttracker.ui.common.TrackingRatingSelector
import com.nilpo.contenttracker.ui.common.bestCollectionMatch
import com.nilpo.contenttracker.ui.common.contentColorOn
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.common.partialSearchFailureMessage
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.common.toCollectionOrderInput
import com.nilpo.contenttracker.ui.common.toCollectionOrderOrNull
import com.nilpo.contenttracker.ui.common.toCollectionPickerOptions
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.detail.DetailDisclosureRow
import com.nilpo.contenttracker.ui.detail.DetailFieldLabel
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailHeader
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.detail.DetailSynopsis
import com.nilpo.contenttracker.ui.detail.sessionStateVisual
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

@Composable
fun AddMediaScreen(
    initialMediaType: MediaType,
    availableMediaTypes: List<MediaType>,
    library: List<TrackedMedia>,
    headerActions: AddMediaHeaderActions,
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
    modifier: Modifier = Modifier,
) {
    var step by remember {
        mutableStateOf(
            if (metadataUiState.selectedSuggestion != null) AddMediaStep.Review else AddMediaStep.Search,
        )
    }
    var title by remember { mutableStateOf("") }
    var totalProgress by remember { mutableStateOf("") }
    // Only asked on the manual step; a search result brings its own.
    var manualCreator by remember { mutableStateOf("") }
    var manualReleaseYear by remember { mutableStateOf("") }
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

    // The step's name lives in the top bar, beside the back arrow, which walks back through the steps
    // before it leaves the page.
    val headerTitle = when (step) {
        AddMediaStep.Search -> initialMediaType.addTitle()
        AddMediaStep.Season -> stringResource(R.string.metadata_season_picker_title)
        AddMediaStep.BookEdition -> stringResource(R.string.book_edition_picker_title)
        AddMediaStep.Review -> selectedMediaType.addTitle()
        AddMediaStep.Manual -> stringResource(R.string.add_manual)
    }
    SideEffect {
        headerActions.title = headerTitle
        headerActions.onStepBack = {
            when (step) {
                AddMediaStep.Search -> false
                // Back from the review returns to the picker that led to it, so another edition or
                // season is one step away rather than a new search.
                AddMediaStep.Review -> {
                    when {
                        selectedBookEdition != null -> {
                            selectedBookEdition = null
                            step = AddMediaStep.BookEdition
                        }
                        selectedSeason != null || useWholeSeries -> {
                            selectedSeason = null
                            useWholeSeries = false
                            step = AddMediaStep.Season
                        }
                        else -> step = AddMediaStep.Search
                    }
                    true
                }
                else -> {
                    // Picking the same result again must offer its editions and seasons again.
                    selectedBookEdition = null
                    selectedSeason = null
                    useWholeSeries = false
                    step = AddMediaStep.Search
                    true
                }
            }
        }
    }
    DisposableEffect(headerActions) {
        onDispose { headerActions.onStepBack = { false } }
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
        // Completed runs progress to the total; trying it and then picking another state must not
        // leave that behind as a "dropped at 100%".
        if (selectedStatus == TrackingStatus.Completed && status != TrackingStatus.Completed) {
            initialProgress = "0"
        }
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
    val onInitialProgressChange: (String) -> Unit = { value ->
        val digits = value.filter { it.isDigit() }
        val maxProgress = selectedMediaType.effectiveProgressTotal(totalProgress)
        initialProgress = maxProgress?.let { max ->
            digits.toIntOrNull()?.coerceIn(0, max)?.toString() ?: digits
        } ?: digits
    }

    // The status, its fields and the item's extras are the same on the review and the manual step,
    // so both take them as one slot rather than each re-plumbing thirty parameters.
    val trackingChoices: @Composable (showLanguage: Boolean) -> Unit = { showLanguage ->
        AddTrackingChoices(
            mediaType = selectedMediaType,
            progressTotal = selectedMediaType.effectiveProgressTotal(totalProgress),
            selectedStatus = selectedStatus,
            onStatusSelected = applyStatus,
            initialProgress = initialProgress,
            onInitialProgressChange = onInitialProgressChange,
            initialRating = initialRating,
            onInitialRatingSelected = { initialRating = it },
            initialStartedAt = initialStartedAt,
            onInitialStartedAtChange = { initialStartedAt = it },
            initialFinishedAt = initialFinishedAt,
            onInitialFinishedAtChange = { initialFinishedAt = it },
            availableCollections = collectionOptionsForType,
            itemTitle = title,
            providerCollectionTitle = selectedMetadataForForm?.collectionTitle.takeUnless { step == AddMediaStep.Manual },
            collectionName = collectionName,
            onCollectionNameChange = { collectionName = it },
            collectionOrder = collectionOrder,
            onCollectionOrderChange = { collectionOrder = it.toCollectionOrderInput() },
            isOwned = isOwned,
            onOwnedChange = { isOwned = it },
            platform = platform,
            onPlatformChange = { platform = it },
            selectedPlatformType = selectedPlatformType,
            onPlatformTypeSelected = { selectedPlatformType = it },
            notes = initialNotes,
            onNotesChange = { initialNotes = it },
            accent = selectedMediaType.sectionAccent(),
            language = language.takeIf { showLanguage && selectedMediaType != MediaType.Game },
            onLanguageChange = if (showLanguage && selectedMediaType != MediaType.Game) {
                { value -> language = ItemLanguage.normalize(value) ?: ItemLanguage.Original }
            } else {
                null
            },
        )
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        when (step) {
            AddMediaStep.Search -> MetadataSearchStep(
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
                    manualCreator = ""
                    manualReleaseYear = ""
                    initialProgress = "0"
                    step = AddMediaStep.Manual
                },
            )

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
            )

            AddMediaStep.Review -> MetadataReviewStep(
                suggestion = selectedMetadataForForm,
                isLoadingDetails = metadataUiState.isLoadingDetails,
                hasDetailsError = metadataUiState.hasDetailsError,
                collectionName = collectionName,
                collectionOrder = collectionOrder,
                title = title,
                selectedStatus = selectedStatus,
                accent = selectedMediaType.sectionAccent(),
                onRetryDetails = onMetadataDetailsRetry,
                trackingChoices = { trackingChoices(false) },
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
                creator = manualCreator,
                onCreatorChange = { manualCreator = it },
                releaseYear = manualReleaseYear,
                onReleaseYearChange = { value -> manualReleaseYear = value.filter { it.isDigit() }.take(4) },
                selectedStatus = selectedStatus,
                trackingChoices = { trackingChoices(true) },
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
                            creators = listOfNotNull(manualCreator.trim().takeIf { it.isNotBlank() }),
                            releaseYear = manualReleaseYear.toIntOrNull(),
                        ),
                    )
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shared page frame
// ─────────────────────────────────────────────────────────────

/**
 * Every add step on one frame: a fixed [header], the step's content scrolling under it, and its
 * action floating over the foot of the page.
 *
 * No bar and no fade hold the action — the button's own shadow separates it from whatever scrolls
 * beneath. Back and cancel live in the top bar's arrow, so a step never needs more than its one way
 * forward.
 */
@Composable
private fun AddStepScaffold(
    actions: (@Composable RowScope.() -> Unit)?,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            header()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = if (actions != null) FloatingActionClearance else 24.dp),
                content = content,
            )
        }
        if (actions != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = DetailGutter, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** Room left under the last row so it can scroll clear of the floating action. */
private val FloatingActionClearance = 112.dp

/** What the step is about — the work being picked from — at the head of a picker. */
@Composable
private fun AddStepContext(title: String?, detail: String? = null) {
    if (title.isNullOrBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter)
            .padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = displayMediaTitle(title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Normal,
            ),
            color = OmnilogTheme.colors.appInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        detail?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddHairline() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DetailGutter),
        color = OmnilogTheme.colors.appLine,
    )
}

/**
 * The one strong action, full width and lifted, in the colour of the chosen status. The label says
 * exactly what will happen — `Afegeix com a Completat` — so the button doubles as a summary of the
 * choice above it.
 *
 * While it cannot be pressed, the label is [disabledReason] instead: a greyed button that only
 * repeats what it would do leaves a new user guessing what is missing.
 */
@Composable
private fun RowScope.AddSaveButton(
    selectedStatus: TrackingStatus,
    disabledReason: String?,
    onSave: () -> Unit,
) {
    val visual = sessionStateVisual(selectedStatus)
    val container by animateColorAsState(targetValue = visual.color, label = "addSaveColor")
    val enabled = disabledReason == null

    Button(
        onClick = onSave,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        // Disabled keeps an opaque surface and its lift: Material's translucent disabled fill let the
        // rows scrolling underneath show through the reason and made both unreadable.
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColorOn(container),
            disabledContainerColor = OmnilogTheme.colors.appPanel,
            disabledContentColor = OmnilogTheme.colors.appMuted,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 6.dp,
        ),
    ) {
        if (enabled) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
            )
        }
        Text(
            text = disabledReason ?: stringResource(R.string.add_with_status, visual.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The floating primary action in the app's own primary colour, for flows with no status to wear.
 * Like [AddSaveButton], a disabled one stays opaque and lifted so its label can say why.
 */
@Composable
internal fun RowScope.FloatingPrimaryAction(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = OmnilogTheme.colors.appPanel,
            disabledContentColor = OmnilogTheme.colors.appMuted,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 6.dp,
        ),
    ) {
        if (icon != null && enabled) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A secondary way forward: the save button's shape and lift, in paper rather than a status colour. */
@Composable
internal fun RowScope.FloatingSecondaryAction(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OmnilogTheme.colors.appPanel,
            contentColor = OmnilogTheme.colors.appInk,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Search
// ─────────────────────────────────────────────────────────────

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
    val explanation = stringResource(R.string.add_search_subtitle)
    val steamHint = stringResource(R.string.steam_id_search_hint)

    AddStepScaffold(
        header = {
            Column(
                modifier = Modifier.padding(horizontal = DetailGutter).padding(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                // The page's one explanation, until there are results to explain themselves.
                if (!uiState.hasSearched && !uiState.isLoading) {
                    Text(
                        text = if (initialMediaType == MediaType.Game) "$explanation\n$steamHint" else explanation,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        },
        actions = {
            FloatingSecondaryAction(
                text = stringResource(R.string.add_manual),
                icon = Icons.Filled.Add,
                onClick = onManualAdd,
            )
        },
    ) {
        MetadataSearchResults(
            uiState = uiState,
            accent = accent,
            onSuggestionSelected = onSuggestionSelected,
            onRetrySearch = submitSearch,
            duplicateStateForSuggestion = duplicateStateForSuggestion,
        )
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
    val gutter = Modifier.padding(horizontal = DetailGutter)
    val retryAction = EmptyStateAction(
        label = stringResource(R.string.retry_action),
        onClick = onRetrySearch,
    )
    when {
        // The search bar already spins while a query runs, so this panel stays text-only.
        uiState.isLoading -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_loading),
            accent = accent,
            modifier = gutter,
        )
        uiState.hasError -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_error),
            accent = accent,
            modifier = gutter,
            textColor = MaterialTheme.colorScheme.error,
            action = retryAction,
        )
        uiState.hasSearched && uiState.suggestions.isEmpty() -> OmnilogStatusPanel(
            text = stringResource(R.string.metadata_search_empty),
            accent = accent,
            modifier = gutter,
        )
        !uiState.hasSearched -> Unit
        else -> {
            if (uiState.hasPartialError) {
                OmnilogStatusPanel(
                    text = partialSearchFailureMessage(uiState.failedSources.toList()),
                    accent = accent,
                    modifier = gutter.padding(bottom = 8.dp),
                    action = retryAction,
                )
            }
            uiState.suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) AddHairline()
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

/**
 * A search result as a library row: the cover, a serif title, the creator, one quiet line of facts and
 * the provider's score. Rows are split by hairlines rather than boxed, the way the lists are.
 */
@Composable
internal fun MetadataSuggestionRow(
    suggestion: MetadataSuggestion,
    accent: Color,
    duplicateState: MetadataDuplicateState,
    showSource: Boolean = true,
    horizontalPadding: Dp = DetailGutter,
    onClick: () -> Unit,
) {
    val facts = listOfNotNull(
        suggestion.mediaType.label(),
        suggestion.multiSeasonCount()?.let { stringResource(R.string.metadata_multi_season_count, it) },
        suggestion.releaseYear?.toString(),
        suggestion.source.displayName().takeIf { showSource },
    )

    AddPickRow(
        coverUrl = suggestion.coverUrl,
        horizontalPadding = horizontalPadding,
        onClick = onClick,
        trailing = {
            val marker = duplicateState.marker()
            if (marker != null) {
                Icon(
                    imageVector = marker.icon,
                    contentDescription = stringResource(marker.contentDescriptionResId),
                    tint = marker.tint,
                )
            } else {
                PickChevron()
            }
        },
    ) {
        PickTitle(displayMediaTitle(suggestion.title))
        suggestion.creators.firstOrNull()?.let { creator ->
            Text(
                text = creator,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PickFacts(facts.joinToString(" · "))
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

/** The row every picker on these steps shares: cover left, text in the middle, a mark at the end. */
@Composable
private fun AddPickRow(
    coverUrl: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    horizontalPadding: Dp = DetailGutter,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataCoverImage(
            coverUrl = coverUrl,
            modifier = Modifier.size(width = 56.dp, height = 84.dp),
            shape = RoundedCornerShape(6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            content = content,
        )
        trailing()
    }
}

@Composable
private fun PickTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Normal,
        ),
        color = OmnilogTheme.colors.appInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PickFacts(text: String, maxLines: Int = 1) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = OmnilogTheme.colors.appMuted,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PickChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = OmnilogTheme.colors.appMuted,
    )
}

// ─────────────────────────────────────────────────────────────
// Season and edition pickers
// ─────────────────────────────────────────────────────────────

@Composable
private fun MetadataSeasonSelectionStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    accent: Color,
    onSeasonSelected: (MetadataSeasonSuggestion) -> Unit,
    onUseSeries: () -> Unit,
    onRetryDetails: () -> Unit,
) {
    val gutter = Modifier.padding(horizontal = DetailGutter)

    AddStepScaffold(
        header = {
            AddStepContext(
                title = suggestion?.title,
                detail = suggestion?.creators?.firstOrNull(),
            )
        },
        actions = {
            FloatingSecondaryAction(
                text = stringResource(R.string.metadata_season_use_series),
                enabled = !isLoadingDetails,
                onClick = onUseSeries,
            )
        },
    ) {
        when {
            isLoadingDetails -> OmnilogStatusPanel(
                text = stringResource(R.string.metadata_season_picker_loading),
                accent = accent,
                modifier = gutter,
                showProgressIndicator = true,
            )
            hasDetailsError -> OmnilogStatusPanel(
                text = stringResource(R.string.metadata_details_error),
                accent = accent,
                modifier = gutter,
                textColor = MaterialTheme.colorScheme.error,
                action = EmptyStateAction(
                    label = stringResource(R.string.retry_action),
                    onClick = onRetryDetails,
                ),
            )
            suggestion?.seasonSuggestions.isNullOrEmpty() -> OmnilogStatusPanel(
                text = stringResource(R.string.metadata_season_picker_empty),
                accent = accent,
                modifier = gutter,
            )
            else -> suggestion?.seasonSuggestions.orEmpty().forEachIndexed { index, season ->
                if (index > 0) AddHairline()
                val details = listOfNotNull(
                    season.releaseYear?.toString(),
                    season.progressTotal?.let { stringResource(R.string.metadata_season_episode_count, it) },
                )
                AddPickRow(
                    coverUrl = season.coverUrl,
                    onClick = { onSeasonSelected(season) },
                    trailing = { PickChevron() },
                ) {
                    PickTitle(season.title)
                    PickFacts(details.joinToString(" · "))
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
            }
        }
    }
}

/**
 * The work's editions, narrowed by a language dropdown and format pills.
 *
 * Language used to be a row of pills labelled with catalogue codes — `fre`, `heb`, `rum` — which only
 * meant something to a librarian. The dropdown names each language in the reader's own, and holds any
 * number of them without a sideways scroll. Format is the other thing people choose an edition by, so
 * the provider's free-text bindings are folded into the four that matter.
 */
@Composable
private fun BookEditionSelectionStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    accent: Color,
    onEditionSelected: (BookEditionMetadata) -> Unit,
    onRetryDetails: () -> Unit,
) {
    val gutter = Modifier.padding(horizontal = DetailGutter)
    val editions = suggestion?.bookEditionSuggestions.orEmpty()
    val availableLanguages = remember(editions) {
        editions.mapNotNull { edition -> ItemLanguage.normalize(edition.language) }
            .distinct()
            .sorted()
    }
    // Always opens on every language: preselecting the work's own language hid most editions behind a
    // filter the reader had not chosen.
    var selectedLanguage by remember(suggestion?.externalId) { mutableStateOf<String?>(null) }
    var selectedFormat by remember(suggestion?.externalId) { mutableStateOf<EditionFormat?>(null) }
    val inLanguage = editions.filter { edition ->
        selectedLanguage == null || ItemLanguage.normalize(edition.language) == selectedLanguage
    }
    val availableFormats = inLanguage.mapNotNull { editionFormatOf(it.format) }.distinct().sorted()
    // A format only filters when picking it would hide something.
    val showFormats = availableFormats.size > 1 ||
        (availableFormats.size == 1 && inLanguage.any { editionFormatOf(it.format) == null })
    val activeFormat = selectedFormat?.takeIf { showFormats && it in availableFormats }
    val visibleEditions = inLanguage.filter { activeFormat == null || editionFormatOf(it.format) == activeFormat }

    AddStepScaffold(
        header = {
            AddStepContext(
                title = suggestion?.title,
                detail = suggestion?.creators?.firstOrNull(),
            )
            if (!isLoadingDetails && availableLanguages.size > 1) {
                OmnilogDropdownField(
                    selectedOption = selectedLanguage,
                    options = listOf<String?>(null) + availableLanguages,
                    optionLabel = { code ->
                        code?.let { editionLanguageName(it) } ?: stringResource(R.string.book_edition_all_languages)
                    },
                    onOptionSelected = { selectedLanguage = it },
                    modifier = Modifier.padding(horizontal = DetailGutter).padding(top = 12.dp),
                    label = stringResource(R.string.field_language),
                )
            }
            if (!isLoadingDetails && showFormats) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = DetailGutter, vertical = 12.dp),
                ) {
                    item {
                        EditionFilterChip(
                            label = stringResource(R.string.book_edition_all_formats),
                            selected = activeFormat == null,
                            accent = accent,
                            onClick = { selectedFormat = null },
                        )
                    }
                    items(availableFormats) { format ->
                        EditionFilterChip(
                            label = stringResource(format.labelRes()),
                            selected = activeFormat == format,
                            accent = accent,
                            onClick = { selectedFormat = format },
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        actions = null,
    ) {
        when {
            isLoadingDetails -> OmnilogStatusPanel(
                text = stringResource(R.string.book_edition_picker_loading),
                accent = accent,
                modifier = gutter,
                showProgressIndicator = true,
            )
            hasDetailsError -> OmnilogStatusPanel(
                text = stringResource(R.string.metadata_details_error),
                accent = accent,
                modifier = gutter,
                textColor = MaterialTheme.colorScheme.error,
                action = EmptyStateAction(
                    label = stringResource(R.string.retry_action),
                    onClick = onRetryDetails,
                ),
            )
            visibleEditions.isEmpty() -> OmnilogStatusPanel(
                text = stringResource(R.string.book_edition_picker_empty),
                accent = accent,
                modifier = gutter,
            )
            else -> visibleEditions.forEachIndexed { index, edition ->
                if (index > 0) AddHairline()
                val details = buildList {
                    edition.language?.let { add(editionLanguageName(it)) }
                    edition.releaseYear?.let { add(it.toString()) }
                    edition.pageCount?.let { add(stringResource(R.string.book_edition_pages, it)) }
                    edition.format?.let(::add)
                    edition.publisher?.let(::add)
                    edition.isbn?.let { add(stringResource(R.string.book_edition_isbn, it)) }
                }
                AddPickRow(
                    coverUrl = edition.coverUrl,
                    onClick = { onEditionSelected(edition) },
                    trailing = { PickChevron() },
                ) {
                    PickTitle(displayMediaTitle(edition.title ?: suggestion?.title.orEmpty()))
                    PickFacts(details.joinToString(" · "), maxLines = 3)
                }
            }
        }
    }
}

/** A soft paper pill, tinted with the accent once it is the active filter. */
@Composable
private fun EditionFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) accent.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        border = if (selected) BorderStroke(1.dp, accent) else null,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else OmnilogTheme.colors.appInk,
        )
    }
}

/**
 * A language as the reader would say it. The app's own languages keep their labels; anything else a
 * catalogue sends — usually a three-letter MARC code — is named by the system in the device language,
 * and falls back to the code itself only when the system does not know it either.
 */
@Composable
private fun editionLanguageName(code: String): String {
    val normalized = ItemLanguage.normalize(code) ?: return code
    if (normalized in ItemLanguage.Defaults) return languageLabel(normalized)
    val tag = catalogLanguageTag(normalized)
    val displayLocale = LocalConfiguration.current.locales[0]
    val name = Locale.forLanguageTag(tag).getDisplayLanguage(displayLocale)
    return if (name.isBlank() || name.equals(tag, ignoreCase = true)) {
        code
    } else {
        name.replaceFirstChar { it.titlecase(displayLocale) }
    }
}

/** The categories people pick an edition by. Order is the order the pills appear in. */
internal enum class EditionFormat {
    Paperback,
    Hardcover,
    Digital,
    Audiobook,
}

/** Folds a provider's free-text binding ("Mass Market Paperback", "Audio CD") into an [EditionFormat]. */
internal fun editionFormatOf(format: String?): EditionFormat? {
    val value = format?.lowercase(Locale.ROOT) ?: return null
    return when {
        "audio" in value || "mp3" in value -> EditionFormat.Audiobook
        listOf("ebook", "e-book", "kindle", "epub", "digital", "electronic").any { it in value } ->
            EditionFormat.Digital
        listOf("hard", "cartoné", "cartone", "tapa dura", "library binding").any { it in value } ->
            EditionFormat.Hardcover
        listOf("paper", "soft", "mass market", "pocket", "tapa blanda", "rústica", "rustica").any { it in value } ->
            EditionFormat.Paperback
        else -> null
    }
}

/**
 * A catalogue language code as a locale tag the system can name. Library catalogues send MARC
 * (ISO 639-2/B) codes, several of which — `fre`, `ger`, `rum` — the system does not recognise.
 */
internal fun catalogLanguageTag(code: String): String {
    val lower = code.trim().lowercase(Locale.ROOT)
    return MarcLanguageTags[lower] ?: lower
}

private val MarcLanguageTags = mapOf(
    "fre" to "fr", "fra" to "fr", "ger" to "de", "deu" to "de", "ita" to "it", "por" to "pt",
    "rus" to "ru", "rum" to "ro", "ron" to "ro", "dut" to "nl", "nld" to "nl", "heb" to "he",
    "chi" to "zh", "zho" to "zh", "cze" to "cs", "ces" to "cs", "gre" to "el", "ell" to "el",
    "pol" to "pl", "swe" to "sv", "nor" to "no", "dan" to "da", "fin" to "fi", "tur" to "tr",
    "ara" to "ar", "kor" to "ko", "hun" to "hu", "ukr" to "uk", "glg" to "gl", "baq" to "eu",
    "eus" to "eu", "per" to "fa", "fas" to "fa", "arm" to "hy", "hye" to "hy", "geo" to "ka",
    "kat" to "ka", "ice" to "is", "isl" to "is", "slo" to "sk", "slk" to "sk", "wel" to "cy",
    "cym" to "cy", "alb" to "sq", "sqi" to "sq", "bul" to "bg", "hrv" to "hr", "srp" to "sr",
    "slv" to "sl", "lit" to "lt", "lav" to "lv", "est" to "et", "hin" to "hi", "ind" to "id",
    "vie" to "vi", "tha" to "th", "lat" to "la",
)

private fun EditionFormat.labelRes(): Int = when (this) {
    EditionFormat.Paperback -> R.string.book_edition_format_paperback
    EditionFormat.Hardcover -> R.string.book_edition_format_hardcover
    EditionFormat.Digital -> R.string.book_edition_format_digital
    EditionFormat.Audiobook -> R.string.book_edition_format_audiobook
}

// ─────────────────────────────────────────────────────────────
// Review and manual entry
// ─────────────────────────────────────────────────────────────

/**
 * The title as the detail page will show it, then the choice that matters: where you are with it.
 */
@Composable
private fun MetadataReviewStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
    hasDetailsError: Boolean,
    collectionName: String,
    collectionOrder: String,
    title: String,
    selectedStatus: TrackingStatus,
    accent: Color,
    onRetryDetails: () -> Unit,
    trackingChoices: @Composable () -> Unit,
    onSave: () -> Unit,
) {
    AddStepScaffold(
        actions = {
            AddSaveButton(
                selectedStatus = selectedStatus,
                disabledReason = when {
                    isLoadingDetails -> stringResource(R.string.add_disabled_loading)
                    title.isBlank() -> stringResource(R.string.add_disabled_needs_title)
                    else -> null
                },
                onSave = onSave,
            )
        },
    ) {
        suggestion?.let {
            // The collection shown is the one being assigned here, so a season lands as "Frieren #2"
            // before it is even saved.
            val metadata = it
                .copy(progressTotal = it.progressTotal.takeUnless { _ -> it.mediaType == MediaType.Game })
                .toMediaMetadataUi()
                .copy(
                    collectionName = collectionName.trim().takeIf { name -> name.isNotBlank() },
                    collectionSortOrder = collectionOrder.toCollectionOrderOrNull(),
                )
            DetailHeader(metadata = metadata, topInset = 8.dp)

            if (isLoadingDetails) {
                Column(
                    modifier = Modifier.padding(horizontal = DetailGutter).padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.metadata_details_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = accent,
                        trackColor = OmnilogTheme.colors.appLine,
                    )
                }
            }
            if (hasDetailsError) {
                // Non-blocking: the copy says the available metadata is usable, so this offers a
                // retry without taking the review step away from the user.
                OmnilogStatusPanel(
                    text = stringResource(R.string.metadata_details_error),
                    accent = accent,
                    modifier = Modifier.padding(horizontal = DetailGutter).padding(top = 14.dp),
                    textColor = MaterialTheme.colorScheme.error,
                    action = EmptyStateAction(
                        label = stringResource(R.string.retry_action),
                        onClick = onRetryDetails,
                    ),
                    actionEnabled = !isLoadingDetails,
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        trackingChoices()

        suggestion?.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            Column(
                modifier = Modifier.padding(horizontal = DetailGutter).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailFieldLabel(stringResource(R.string.metadata_summary))
                DetailSynopsis(body = synopsis)
            }
        }
    }
}

@Composable
private fun ManualAddStep(
    availableMediaTypes: List<MediaType>,
    selectedMediaType: MediaType,
    onMediaTypeSelected: (MediaType) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    totalProgress: String,
    onTotalProgressChange: (String) -> Unit,
    creator: String,
    onCreatorChange: (String) -> Unit,
    releaseYear: String,
    onReleaseYearChange: (String) -> Unit,
    selectedStatus: TrackingStatus,
    trackingChoices: @Composable () -> Unit,
    onSave: () -> Unit,
) {
    val accent = selectedMediaType.sectionAccent()

    AddStepScaffold(
        actions = {
            AddSaveButton(
                selectedStatus = selectedStatus,
                disabledReason = stringResource(R.string.add_disabled_needs_title).takeIf { title.isBlank() },
                onSave = onSave,
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DetailGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.add_manual_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            if (availableMediaTypes.size > 1) {
                MediaTypeChips(
                    types = availableMediaTypes,
                    selected = selectedMediaType,
                    onSelected = onMediaTypeSelected,
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
                    label = {
                        Text(stringResource(R.string.add_total_with_unit, progressUnitLabel(selectedMediaType, 2)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = reviewTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            // The creator and the year are what the lists, the header and the author pages are built
            // on, so they are worth the two fields. Everything heavier waits for the item's editor.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = creator,
                    onValueChange = onCreatorChange,
                    label = { Text(stringResource(selectedMediaType.creatorLabelRes())) },
                    modifier = Modifier.weight(1.8f),
                    singleLine = true,
                    colors = reviewTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = releaseYear,
                    onValueChange = onReleaseYearChange,
                    label = { Text(stringResource(R.string.field_release_year)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = reviewTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            ManualLaterHint(mediaType = selectedMediaType)
        }

        Spacer(modifier = Modifier.height(28.dp))
        trackingChoices()
    }
}

/**
 * Says what this form leaves out and where it goes instead, so a missing cover or synopsis reads as
 * "later, over there" rather than "not possible". Games have no metadata link, so they only get the
 * editor half.
 */
@Composable
private fun ManualLaterHint(mediaType: MediaType) {
    val linkLabel = when (mediaType) {
        MediaType.Anime -> stringResource(R.string.link_metadata_anime)
        MediaType.Book -> stringResource(R.string.link_metadata_book)
        MediaType.Movie, MediaType.TvShow -> stringResource(R.string.link_metadata_movie)
        MediaType.Game -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OmnilogTheme.colors.appPanel, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OmnilogTheme.colors.appMuted,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Text(
            text = listOfNotNull(
                stringResource(R.string.add_manual_later_hint),
                linkLabel?.let { stringResource(R.string.add_manual_later_link_hint, it) },
            ).joinToString(" "),
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

private fun MediaType.creatorLabelRes(): Int = when (this) {
    MediaType.Book -> R.string.add_creator_book
    MediaType.Movie -> R.string.add_creator_movie
    MediaType.TvShow -> R.string.add_creator_tv
    MediaType.Anime -> R.string.add_creator_anime
    MediaType.Game -> R.string.add_creator_game
}

/** The kinds this section holds, as pills with their own mark — clearer than a dropdown for two or three. */
@Composable
private fun MediaTypeChips(
    types: List<MediaType>,
    selected: MediaType,
    onSelected: (MediaType) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            val isSelected = type == selected
            val accent = type.sectionAccent()
            Surface(
                selected = isSelected,
                onClick = { onSelected(type) },
                modifier = Modifier.semantics { role = Role.RadioButton },
                shape = RoundedCornerShape(50),
                color = if (isSelected) accent.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
                border = if (isSelected) BorderStroke(1.dp, accent) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(type.dropdownIconResId),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) accent else OmnilogTheme.colors.appMuted,
                    )
                    Text(
                        text = type.label(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appInk,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Status, its fields, and the item's extras
// ─────────────────────────────────────────────────────────────

/**
 * Where the user is with the title, then the few extras that belong to the item.
 *
 * The status leads because it is the decision that shapes everything else: it is asked as a question,
 * every answer explains itself, and only the fields that answer needs appear under it. Collection,
 * ownership and the rarer details follow as ruled rows, each saying what it is for.
 */
@Composable
private fun AddTrackingChoices(
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
    availableCollections: List<CollectionPickerOption>,
    itemTitle: String,
    providerCollectionTitle: String?,
    collectionName: String,
    onCollectionNameChange: (String) -> Unit,
    collectionOrder: String,
    onCollectionOrderChange: (String) -> Unit,
    isOwned: Boolean,
    onOwnedChange: (Boolean) -> Unit,
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    accent: Color,
    language: String? = null,
    onLanguageChange: ((String) -> Unit)? = null,
) {
    var showCollectionSheet by remember { mutableStateOf(false) }

    Column(
        // animateContentSize clips to its bounds, so it wraps the gutter rather than sitting inside it:
        // inside, it cut the progress rail's thumb in half at either end of the track.
        modifier = Modifier
            .animateContentSize()
            .padding(horizontal = DetailGutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailSectionTitle(text = stringResource(R.string.add_status_title))
        Text(
            text = stringResource(R.string.add_status_help),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appMuted,
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatusPicker(selected = selectedStatus, onSelected = onStatusSelected)
        StatusFields(
            status = selectedStatus,
            mediaType = mediaType,
            progressTotal = progressTotal,
            initialProgress = initialProgress,
            onInitialProgressChange = onInitialProgressChange,
            initialRating = initialRating,
            onInitialRatingSelected = onInitialRatingSelected,
            initialStartedAt = initialStartedAt,
            onInitialStartedAtChange = onInitialStartedAtChange,
            initialFinishedAt = initialFinishedAt,
            onInitialFinishedAtChange = onInitialFinishedAtChange,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    AddHairline()

    val trimmedCollection = collectionName.trim()
    val collectionLabel = formatCollectionDisplayName(
        trimmedCollection.takeIf { it.isNotBlank() },
        collectionOrder.toCollectionOrderOrNull(),
    )
    AddOptionRow(
        icon = painterResource(R.drawable.ic_group_collections),
        title = stringResource(R.string.field_collection),
        summary = collectionLabel ?: stringResource(R.string.add_collection_hint),
        active = collectionLabel != null,
        accent = accent,
        onClick = { showCollectionSheet = true },
        trailing = { PickChevron() },
    )
    AddHairline()
    AddOptionRow(
        icon = painterResource(R.drawable.ic_owned_badge),
        title = stringResource(R.string.owned_label),
        summary = stringResource(R.string.add_owned_hint),
        active = isOwned,
        accent = accent,
        role = Role.Switch,
        onClick = { onOwnedChange(!isOwned) },
        trailing = {
            Switch(
                checked = isOwned,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = contentColorOn(accent),
                ),
            )
        },
    )
    AddHairline()
    DetailDisclosureRow(
        icon = rememberVectorPainter(Icons.Outlined.Info),
        title = stringResource(R.string.add_more_details),
        summary = listOf(platform, notes)
            .firstOrNull { it.isNotBlank() }
            ?: stringResource(R.string.add_more_details_hint),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DetailGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            DetailFieldLabel(stringResource(R.string.field_notes))
            TrackingNotesField(
                value = notes,
                accent = accent,
                onValueChange = onNotesChange,
            )
        }
    }
    AddHairline()

    if (showCollectionSheet) {
        val selectedOption = availableCollections.firstOrNull {
            it.collection.name.equals(trimmedCollection, ignoreCase = true)
        }
        CollectionPickerSheet(
            itemTitle = itemTitle,
            providerCollectionTitle = providerCollectionTitle,
            options = availableCollections,
            initialCollectionId = selectedOption?.collection?.id,
            initialSortOrder = collectionOrder.toCollectionOrderOrNull(),
            accent = accent,
            onDismiss = { showCollectionSheet = false },
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
                showCollectionSheet = false
            },
        )
    }
}

/**
 * The five states as tiles rather than a dropdown, each saying in a few words when to pick it.
 *
 * The three a new title usually starts in share the first row at full size; Paused and Dropped, which
 * are rarer at the moment of adding something, sit under them smaller. The whole group is one set of
 * radio buttons for assistive technology.
 */
@Composable
private fun StatusPicker(
    selected: TrackingStatus,
    onSelected: (TrackingStatus) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(TrackingStatus.Planned, TrackingStatus.InProgress, TrackingStatus.Completed).forEach { status ->
                StatusTile(
                    status = status,
                    selected = status == selected,
                    compact = false,
                    onClick = { onSelected(status) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(TrackingStatus.Paused, TrackingStatus.Dropped).forEach { status ->
                StatusTile(
                    status = status,
                    selected = status == selected,
                    compact = true,
                    onClick = { onSelected(status) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun StatusTile(
    status: TrackingStatus,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = sessionStateVisual(status)
    val container by animateColorAsState(
        targetValue = if (selected) visual.color.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        label = "statusTileContainer",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) visual.color else Color.Transparent,
        label = "statusTileOutline",
    )
    val iconTint = if (selected) visual.color else OmnilogTheme.colors.appMuted
    val hint = stringResource(status.addHintRes())

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.RadioButton },
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(1.5.dp, outline),
    ) {
        if (compact) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(visual.icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    StatusTileLabel(visual.label, selected)
                    StatusTileHint(hint, maxLines = 1)
                }
            }
        } else {
            // Mark and name share the first line, the hint wraps under them. The tint, outline and
            // weight carry the selection, as on the compact tiles, so there is no separate tick.
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(visual.icon),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                    StatusTileLabel(visual.label, selected)
                }
                StatusTileHint(hint, maxLines = 2)
            }
        }
    }
}

@Composable
private fun StatusTileLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = OmnilogTheme.colors.appInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusTileHint(text: String, maxLines: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = OmnilogTheme.colors.appMuted,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Only what the chosen state can answer: nothing for Planned; where you are and when you started for
 * In progress; a verdict and the dates once something has ended. A finished title with a known total
 * skips progress, since it is simply the total.
 */
@Composable
private fun StatusFields(
    status: TrackingStatus,
    mediaType: MediaType,
    progressTotal: Int?,
    initialProgress: String,
    onInitialProgressChange: (String) -> Unit,
    initialRating: Int?,
    onInitialRatingSelected: (Int?) -> Unit,
    initialStartedAt: String,
    onInitialStartedAtChange: (String) -> Unit,
    initialFinishedAt: String,
    onInitialFinishedAtChange: (String) -> Unit,
) {
    if (status == TrackingStatus.Planned) return
    val stateColor = sessionStateVisual(status).color
    val showsProgress = status != TrackingStatus.Completed || progressTotal == null
    val showsRating = status != TrackingStatus.InProgress
    val showsFinish = status != TrackingStatus.InProgress

    Column(
        modifier = Modifier.padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showsProgress) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailFieldLabel(
                    stringResource(
                        if (status == TrackingStatus.InProgress) R.string.field_progress else R.string.field_final_progress,
                    ),
                )
                QuickProgressRail(
                    text = initialProgress,
                    total = progressTotal,
                    mediaType = mediaType,
                    accent = stateColor,
                    onTextChange = onInitialProgressChange,
                )
            }
        }
        if (showsRating) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailFieldLabel(stringResource(R.string.field_rating))
                TrackingRatingSelector(
                    currentRatingHalfPoints = initialRating,
                    accent = stateColor,
                    onRatingSelected = onInitialRatingSelected,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailFieldLabel(stringResource(R.string.session_dates))
            TrackingDateRange(
                startedLabel = stringResource(R.string.session_started_label),
                startedValue = initialStartedAt,
                accent = stateColor,
                onStartedValueChange = onInitialStartedAtChange,
                finishedLabel = if (showsFinish) stringResource(R.string.session_finished_label) else null,
                finishedValue = initialFinishedAt.takeIf { showsFinish },
                onFinishedValueChange = onInitialFinishedAtChange.takeIf { showsFinish },
            )
        }
    }
}

/**
 * One of the item's extras as a ruled row: its mark, its name, a line saying what it does (or what it
 * is set to), and a control at the end. The whole row is the target.
 */
@Composable
private fun AddOptionRow(
    icon: Painter,
    title: String,
    summary: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    role: Role = Role.Button,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = role, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = DetailGutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (active) accent else OmnilogTheme.colors.appMuted,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFontFamily),
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) accent else OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun reviewTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = OmnilogTheme.colors.appLine,
    cursorColor = accent,
    focusedLabelColor = accent,
)

// ─────────────────────────────────────────────────────────────
// Shared with other screens
// ─────────────────────────────────────────────────────────────

@Composable
internal fun DashboardStyleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmitted: () -> Unit = {},
    isLoading: Boolean,
    accent: Color,
    leadingIcon: ImageVector? = null,
    placeholder: String = stringResource(R.string.metadata_search_label),
    fieldModifier: Modifier = Modifier,
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
                modifier = Modifier.weight(1f).then(fieldModifier),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmitted() }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = OmnilogTheme.colors.appInk,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = placeholder,
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

internal class DuplicateMarker(
    val icon: ImageVector,
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

private fun TrackingStatus.addHintRes(): Int = when (this) {
    TrackingStatus.Planned -> R.string.add_status_hint_planned
    TrackingStatus.InProgress -> R.string.add_status_hint_in_progress
    TrackingStatus.Completed -> R.string.add_status_hint_completed
    TrackingStatus.Paused -> R.string.add_status_hint_paused
    TrackingStatus.Dropped -> R.string.add_status_hint_dropped
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

private fun String.toLocalDateOrNull(): LocalDate? {
    return trim().takeIf { it.isNotBlank() }?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

private fun MediaType.effectiveProgressTotal(totalProgress: String): Int? {
    return totalProgress.toIntOrNull().takeUnless { this == MediaType.Game }
}
