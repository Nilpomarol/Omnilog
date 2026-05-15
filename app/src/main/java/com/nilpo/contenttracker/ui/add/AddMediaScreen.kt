package com.nilpo.contenttracker.ui.add

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector
import com.nilpo.contenttracker.ui.detail.StatusSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun AddMediaScreen(
    initialMediaType: MediaType,
    availableMediaTypes: List<MediaType>,
    onSave: (AddTrackedMediaRequest) -> Unit,
    metadataUiState: MetadataSearchUiState,
    onMetadataQueryChange: (String) -> Unit,
    onMetadataSearch: () -> Unit,
    onMetadataSuggestionSelected: (MetadataSuggestion) -> Unit,
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

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                AddMediaStep.Search -> MetadataSearchStep(
                    uiState = metadataUiState,
                    onQueryChange = onMetadataQueryChange,
                    onSearch = onMetadataSearch,
                    onSuggestionSelected = onMetadataSuggestionSelected,
                    onManualAdd = {
                        selectedMediaType = initialMediaType
                        title = ""
                        totalProgress = ""
                        step = AddMediaStep.Manual
                    },
                    onCancel = onCancel,
                )
                AddMediaStep.Review -> MetadataReviewStep(
                    suggestion = selectedMetadataSuggestion,
                    isLoadingDetails = metadataUiState.isLoadingDetails,
                    availableMediaTypes = availableMediaTypes,
                    selectedMediaType = selectedMediaType,
                    onMediaTypeSelected = { selectedMediaType = it },
                    title = title,
                    onTitleChange = { title = it },
                    totalProgress = totalProgress,
                    onTotalProgressChange = { value -> totalProgress = value.filter { it.isDigit() } },
                    platform = platform,
                    onPlatformChange = { platform = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    selectedOwnershipType = selectedOwnershipType,
                    onOwnershipTypeSelected = { selectedOwnershipType = it },
                    selectedPlatformType = selectedPlatformType,
                    onPlatformTypeSelected = { selectedPlatformType = it },
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                initialStatus = selectedStatus,
                                isOwned = selectedOwnershipType != OwnershipType.None,
                                ownershipType = selectedOwnershipType,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
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
                    onTotalProgressChange = { value -> totalProgress = value.filter { it.isDigit() } },
                    platform = platform,
                    onPlatformChange = { platform = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    selectedOwnershipType = selectedOwnershipType,
                    onOwnershipTypeSelected = { selectedOwnershipType = it },
                    selectedPlatformType = selectedPlatformType,
                    onPlatformTypeSelected = { selectedPlatformType = it },
                    onBackToSearch = { step = AddMediaStep.Search },
                    onSave = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                initialStatus = selectedStatus,
                                isOwned = selectedOwnershipType != OwnershipType.None,
                                ownershipType = selectedOwnershipType,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                            ),
                        )
                    },
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun MetadataSearchStep(
    uiState: MetadataSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
    onManualAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    LaunchedEffect(uiState.query) {
        if (uiState.query.trim().length >= 2) {
            delay(450)
            onSearch()
        }
    }

    Text(
        text = stringResource(R.string.metadata_search_title),
        style = MaterialTheme.typography.headlineLarge,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.metadata_search_label)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Button(
            enabled = uiState.query.isNotBlank() && !uiState.isLoading,
            onClick = onSearch,
        ) {
            Text(text = stringResource(R.string.search_action))
        }
    }

    MetadataSearchResults(
        uiState = uiState,
        onSuggestionSelected = onSuggestionSelected,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
        TextButton(onClick = onManualAdd) {
            Text(text = stringResource(R.string.add_manual))
        }
    }
}

@Composable
private fun MetadataSearchResults(
    uiState: MetadataSearchUiState,
    onSuggestionSelected: (MetadataSuggestion) -> Unit,
) {
    when {
        uiState.isLoading -> Text(
            text = stringResource(R.string.metadata_search_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
        uiState.hasError -> Text(
            text = stringResource(R.string.metadata_search_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        uiState.hasSearched && uiState.suggestions.isEmpty() -> Text(
            text = stringResource(R.string.metadata_search_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
        else -> uiState.suggestions.forEach { suggestion ->
            MetadataSuggestionRow(
                suggestion = suggestion,
                onClick = { onSuggestionSelected(suggestion) },
            )
        }
    }
}

@Composable
private fun MetadataReviewStep(
    suggestion: MetadataSuggestion?,
    isLoadingDetails: Boolean,
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
    onBackToSearch: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBackToSearch) {
            Text(text = stringResource(R.string.back))
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
    }

    Text(
        text = stringResource(R.string.metadata_review_title),
        style = MaterialTheme.typography.headlineLarge,
    )

    suggestion?.let {
        MetadataDetailSummary(
            suggestion = it,
            isLoadingDetails = isLoadingDetails,
        )
    }

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
        selectedStatus = selectedStatus,
        onStatusSelected = onStatusSelected,
        selectedOwnershipType = selectedOwnershipType,
        onOwnershipTypeSelected = onOwnershipTypeSelected,
        selectedPlatformType = selectedPlatformType,
        onPlatformTypeSelected = onPlatformTypeSelected,
    )

    Button(
        enabled = title.isNotBlank(),
        onClick = onSave,
    ) {
        Text(text = stringResource(R.string.create_session))
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
    platform: String,
    onPlatformChange: (String) -> Unit,
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    selectedOwnershipType: OwnershipType,
    onOwnershipTypeSelected: (OwnershipType) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
    onBackToSearch: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBackToSearch) {
            Text(text = stringResource(R.string.back))
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
    }

    Text(
        text = stringResource(R.string.add_manual),
        style = MaterialTheme.typography.headlineLarge,
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
        selectedStatus = selectedStatus,
        onStatusSelected = onStatusSelected,
        selectedOwnershipType = selectedOwnershipType,
        onOwnershipTypeSelected = onOwnershipTypeSelected,
        selectedPlatformType = selectedPlatformType,
        onPlatformTypeSelected = onPlatformTypeSelected,
    )

    Button(
        enabled = title.isNotBlank(),
        onClick = onSave,
    ) {
        Text(text = stringResource(R.string.save))
    }
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
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
    selectedOwnershipType: OwnershipType,
    onOwnershipTypeSelected: (OwnershipType) -> Unit,
    selectedPlatformType: ConsumptionPlatformType,
    onPlatformTypeSelected: (ConsumptionPlatformType) -> Unit,
) {
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
    )

    OutlinedTextField(
        value = totalProgress,
        onValueChange = onTotalProgressChange,
        label = { Text(stringResource(R.string.field_total_progress)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )

    OutlinedTextField(
        value = platform,
        onValueChange = onPlatformChange,
        label = { Text(stringResource(R.string.field_platform)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    StatusSelector(
        selectedStatus = selectedStatus,
        onStatusSelected = onStatusSelected,
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
private fun MetadataDetailSummary(
    suggestion: MetadataSuggestion,
    isLoadingDetails: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CoverImage(
            coverUrl = suggestion.coverUrl,
            modifier = Modifier.size(width = 96.dp, height = 144.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = suggestion.displayTitle(),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.metadata_suggestion_source, suggestion.source.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            )
            suggestion.progressTotal?.let { total ->
                Text(
                    text = stringResource(R.string.progress_total_value, total),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (suggestion.genres.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.metadata_genres, suggestion.genres.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (isLoadingDetails) {
                Text(
                    text = stringResource(R.string.metadata_details_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }
        }
    }

    suggestion.synopsis?.let { synopsis ->
        Text(
            text = synopsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun MetadataSuggestionRow(
    suggestion: MetadataSuggestion,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = suggestion.coverUrl,
            modifier = Modifier.size(width = 52.dp, height = 78.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = suggestion.displayTitle(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = suggestion.mediaType.label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun CoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    var image by remember(coverUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(coverUrl) {
        image = coverUrl?.let { url ->
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        image?.let { loadedImage ->
            Image(
                bitmap = loadedImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } ?: Box(modifier = Modifier.fillMaxSize())
    }
}

private enum class AddMediaStep {
    Search,
    Review,
    Manual,
}

private fun MetadataSuggestion.displayTitle(): String {
    return listOfNotNull(
        title,
        releaseYear?.let { "($it)" },
    ).joinToString(" ")
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
