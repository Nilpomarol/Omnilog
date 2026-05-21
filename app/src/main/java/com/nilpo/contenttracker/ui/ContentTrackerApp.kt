package com.nilpo.contenttracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.UnsupportedBackupSchemaException
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.add.AddCollectionOption
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.home.CollectionDetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeLandingScreen
import com.nilpo.contenttracker.ui.home.HomeUiEvent
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentTrackerApp(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metadataUiState by viewModel.metadataUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var showRestoreList by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingImportConfirmation by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingPossibleDuplicate by remember { mutableStateOf<PendingPossibleDuplicate?>(null) }
    var pendingCreatedMediaId by remember { mutableStateOf<Long?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<AppDestination>(AppDestination.Home) }
    var detailActions by remember { mutableStateOf(DetailHeaderActions()) }
    val backupActions = remember { BackupHeaderActions() }
    val selectedMedia = uiState.trackedItems.firstOrNull { it.item.id == selectedMediaId }
    val selectedCollection = uiState.trackedItems
        .mapNotNull { it.collection }
        .firstOrNull { it.id == selectedCollectionId }
    val selectedCollectionItems = uiState.trackedItems
        .filter { it.collection?.id == selectedCollectionId }
        .sortedWith(collectionItemComparator())
    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportErrorMessage = stringResource(R.string.backup_export_error)
    val importReadErrorMessage = stringResource(R.string.backup_import_read_error)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val importInvalidMessage = stringResource(R.string.backup_import_invalid)
    val importUnsupportedMessage = stringResource(R.string.backup_import_unsupported)
    val metadataRefreshSuccessMessage = stringResource(R.string.metadata_refresh_success)
    val metadataRefreshUnavailableMessage = stringResource(R.string.metadata_refresh_unavailable)
    val metadataRefreshErrorMessage = stringResource(R.string.metadata_refresh_error)
    val openTrackedMedia: (TrackedMedia) -> Unit = { trackedMedia ->
        viewModel.clearMetadataSearch()
        viewModel.selectSection(trackedMedia.item.type.homeSection())
        selectedDestination = AppDestination.Section
        selectedCollectionId = null
        selectedMediaId = trackedMedia.item.id
        isAdding = false
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = runCatching {
                    val backupJson = viewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        val bytes = backupJson.toByteArray(Charsets.UTF_8)
                        checkNotNull(context.contentResolver.openOutputStream(uri)) {
                            "Could not open backup destination"
                        }.use { outputStream ->
                            outputStream.write(bytes)
                        }
                    }
                }
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) exportSuccessMessage else exportErrorMessage,
                )
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)) {
                            "Could not open backup source"
                        }.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                val backupJson = readResult.getOrNull()
                if (backupJson == null) {
                    snackbarHostState.showSnackbar(importReadErrorMessage)
                    return@launch
                }

                val previewResult = runCatching {
                    PendingBackupImport(
                        json = backupJson,
                        preview = viewModel.previewBackupJson(backupJson),
                    )
                }
                pendingImport = previewResult.getOrNull()
                previewResult.exceptionOrNull()?.let { error ->
                    val message = when (error) {
                        is UnsupportedBackupSchemaException -> importUnsupportedMessage
                        else -> importInvalidMessage
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    backupActions.onExportBackupRequested = {
        exportBackupLauncher.launch("omnilog-backup-${LocalDate.now()}.json")
    }
    backupActions.onImportBackupRequested = {
        importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    }
    backupActions.onRestoreBackupRequested = {
        showRestoreList = true
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.MediaItemCreated -> {
                    pendingCreatedMediaId = event.mediaItemId
                }
                HomeUiEvent.MetadataRefreshSucceeded,
                HomeUiEvent.MetadataRefreshUnavailable,
                HomeUiEvent.MetadataRefreshFailed,
                    -> {
                    val message = when (event) {
                        HomeUiEvent.MetadataRefreshSucceeded -> metadataRefreshSuccessMessage
                        HomeUiEvent.MetadataRefreshUnavailable -> metadataRefreshUnavailableMessage
                        HomeUiEvent.MetadataRefreshFailed -> metadataRefreshErrorMessage
                        is HomeUiEvent.MediaItemCreated -> return@collect
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LaunchedEffect(uiState.allTrackedItems, pendingCreatedMediaId) {
        val mediaItemId = pendingCreatedMediaId ?: return@LaunchedEffect
        val trackedMedia = uiState.allTrackedItems.firstOrNull { it.item.id == mediaItemId }
            ?: return@LaunchedEffect

        viewModel.selectSection(trackedMedia.item.type.homeSection())
        selectedDestination = AppDestination.Section
        selectedCollectionId = null
        selectedMediaId = mediaItemId
        isAdding = false
        pendingCreatedMediaId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                OmnilogTopBar(
                    accent = when (selectedDestination) {
                        AppDestination.Home -> OmnilogColors.Dashboard
                        AppDestination.Section -> uiState.selectedSection.accent
                    },
                    showDetailActions = selectedMedia != null && !isAdding,
                    showBackupActions = selectedMedia == null && !isAdding,
                    detailActions = detailActions,
                    backupActions = backupActions,
                    onBack = { selectedMediaId = null },
                )
            },
            snackbarHost = {},
            bottomBar = {
                OmnilogBottomBar(
                    selectedDestination = selectedDestination,
                    selectedSection = uiState.selectedSection,
                    onHomeClick = {
                        selectedDestination = AppDestination.Home
                        selectedMediaId = null
                        selectedCollectionId = null
                        isAdding = false
                        viewModel.clearMetadataSearch()
                    },
                    onSectionClick = { section ->
                        selectedDestination = AppDestination.Section
                        selectedMediaId = null
                        selectedCollectionId = null
                        isAdding = false
                        viewModel.selectSection(section)
                        viewModel.clearMetadataSearch()
                    },
                )
            },
        ) { innerPadding ->
        if (isAdding) {
            AddMediaScreen(
                initialMediaType = uiState.selectedSection.defaultType,
                availableMediaTypes = uiState.selectedSection.types.toList(),
                availableCollections = uiState.allTrackedItems.toAddCollectionOptions(),
                onSave = { request ->
                    viewModel.addTrackedMedia(request)
                    viewModel.clearMetadataSearch()
                    isAdding = false
                },
                metadataUiState = metadataUiState,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = viewModel::searchMetadataSuggestions,
                onMetadataSuggestionSelected = { suggestion ->
                    when (val duplicate = uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> openTrackedMedia(duplicate.trackedMedia)
                        is DuplicateMatch.Possible -> {
                            pendingPossibleDuplicate = PendingPossibleDuplicate(
                                suggestion = suggestion,
                                trackedMedia = duplicate.trackedMedia,
                            )
                        }
                        DuplicateMatch.None -> viewModel.selectMetadataSuggestion(suggestion)
                    }
                },
                duplicateStateForSuggestion = { suggestion ->
                    when (uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> MetadataDuplicateState.Exact
                        is DuplicateMatch.Possible -> MetadataDuplicateState.Possible
                        DuplicateMatch.None -> MetadataDuplicateState.None
                    }
                },
                onCancel = {
                    viewModel.clearMetadataSearch()
                    isAdding = false
                    selectedCollectionId = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedDestination == AppDestination.Home) {
            HomeLandingScreen(
                uiState = uiState,
                onMediaClick = { trackedMedia ->
                    val section = trackedMedia.item.type.homeSection()
                    viewModel.selectSection(section)
                    selectedDestination = AppDestination.Section
                    selectedCollectionId = null
                    selectedMediaId = trackedMedia.item.id
                },
                onSectionSearch = { section, query ->
                    viewModel.selectSectionWithSearch(section, query)
                    selectedDestination = AppDestination.Section
                    selectedMediaId = null
                    selectedCollectionId = null
                    isAdding = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedCollection != null && selectedMedia == null) {
            CollectionDetailScreen(
                collection = selectedCollection,
                items = selectedCollectionItems,
                accent = uiState.selectedSection.accent,
                onBack = { selectedCollectionId = null },
                onMediaClick = { selectedMediaId = it.item.id },
                onRenameCollection = viewModel::updateMediaCollectionName,
                onDeleteCollection = viewModel::deleteMediaCollection,
                onUpdateCollectionItemOrder = viewModel::updateCollectionItemOrder,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedMedia == null) {
            HomeScreen(
                uiState = uiState,
                metadataUiState = metadataUiState,
                onMediaClick = { selectedMediaId = it.item.id },
                onCollectionClick = {
                    selectedCollectionId = it.id
                    isAdding = false
                },
                onManualAddClick = {
                    viewModel.clearMetadataSearch()
                    isAdding = true
                    selectedCollectionId = null
                },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = viewModel::searchMetadataSuggestions,
                onApiSuggestionSelected = { suggestion ->
                    when (val duplicate = uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> openTrackedMedia(duplicate.trackedMedia)
                        is DuplicateMatch.Possible -> {
                            pendingPossibleDuplicate = PendingPossibleDuplicate(
                                suggestion = suggestion,
                                trackedMedia = duplicate.trackedMedia,
                                requiresAddTransition = true,
                            )
                        }
                        DuplicateMatch.None -> {
                            viewModel.selectMetadataSuggestion(suggestion)
                            isAdding = true
                            selectedCollectionId = null
                        }
                    }
                },
                duplicateStateForSuggestion = { suggestion ->
                    when (uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> MetadataDuplicateState.Exact
                        is DuplicateMatch.Possible -> MetadataDuplicateState.Possible
                        DuplicateMatch.None -> MetadataDuplicateState.None
                    }
                },
                onStatusFilterChange = viewModel::updateStatusFilter,
                onSortModeChange = viewModel::updateSortMode,
                onSortDirectionChange = viewModel::updateSortDirection,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            val actions = remember(selectedMedia.item.id) {
                DetailHeaderActions()
            }
            actions.isRefreshingMetadata = uiState.refreshingMetadataItemId == selectedMedia.item.id
            detailActions = actions
            DetailScreen(
                trackedMedia = selectedMedia,
                accent = uiState.selectedSection.accent,
                headerActions = actions,
                onBack = { selectedMediaId = null },
                onStartNewSession = viewModel::startNewSession,
                onUpdateSessionDetails = viewModel::updateSessionDetails,
                onDeletePastSession = viewModel::deletePastSession,
                onAddExternalTracking = viewModel::addExternalTracking,
                onUpdateExternalTracking = viewModel::updateExternalTracking,
                onUpdateExternalTrackingSynced = viewModel::updateExternalTrackingSynced,
                onDeleteExternalTracking = viewModel::deleteExternalTracking,
                onUpdateMediaItemDetails = viewModel::updateMediaItemDetails,
                onUpdateMediaItemMetadata = viewModel::updateMediaItemMetadata,
                onRefreshMediaItemMetadata = viewModel::refreshMediaItemMetadata,
                onDeleteMediaItem = { mediaItemId ->
                    viewModel.deleteMediaItem(mediaItemId)
                    selectedMediaId = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, top = 82.dp, end = 16.dp),
        )
    }

    if (showRestoreList) {
        val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
        val safetyBackups = remember {
            context.getExternalFilesDir(null)
                ?.listFiles { _, name ->
                    name.startsWith("omnilog-pre-import-") && name.endsWith(".json")
                }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
        AlertDialog(
            onDismissRequest = { showRestoreList = false },
            title = { Text(text = stringResource(R.string.restore_previous_backup_title)) },
            text = {
                if (safetyBackups.isEmpty()) {
                    Text(text = stringResource(R.string.restore_previous_backup_empty))
                } else {
                    Column {
                        safetyBackups.forEach { file ->
                            val label = remember(file) {
                                val dt = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(file.lastModified()),
                                    ZoneId.systemDefault(),
                                )
                                dt.format(displayFormatter)
                            }
                            TextButton(
                                onClick = {
                                    showRestoreList = false
                                    coroutineScope.launch {
                                        val readResult = runCatching {
                                            withContext(Dispatchers.IO) {
                                                file.readText(Charsets.UTF_8)
                                            }
                                        }
                                        val backupJson = readResult.getOrNull()
                                        if (backupJson == null) {
                                            snackbarHostState.showSnackbar(importReadErrorMessage)
                                            return@launch
                                        }
                                        val previewResult = runCatching {
                                            PendingBackupImport(
                                                json = backupJson,
                                                preview = viewModel.previewBackupJson(backupJson),
                                            )
                                        }
                                        pendingImport = previewResult.getOrNull()
                                        previewResult.exceptionOrNull()?.let { error ->
                                            val message = when (error) {
                                                is UnsupportedBackupSchemaException -> importUnsupportedMessage
                                                else -> importInvalidMessage
                                            }
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 0.dp,
                                    vertical = 4.dp,
                                ),
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestoreList = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImport?.let { backupImport ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(text = stringResource(R.string.import_backup_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.import_backup_message_with_summary,
                        backupImport.preview.schemaVersion,
                        backupImport.preview.collectionCount,
                        backupImport.preview.mediaItemCount,
                        backupImport.preview.mediaCreditCount,
                        backupImport.preview.trackingSessionCount,
                        backupImport.preview.externalRatingCount,
                        backupImport.preview.externalTrackingCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        pendingImportConfirmation = backupImport
                    },
                ) {
                    Text(text = stringResource(R.string.import_backup_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImportConfirmation?.let { backupImport ->
        AlertDialog(
            onDismissRequest = { pendingImportConfirmation = null },
            title = { Text(text = stringResource(R.string.import_backup_final_title)) },
            text = { Text(text = stringResource(R.string.import_backup_final_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            runCatching {
                                val safetyJson = viewModel.exportBackupJson()
                                val timestamp = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
                                val file = File(
                                    context.getExternalFilesDir(null),
                                    "omnilog-pre-import-$timestamp.json",
                                )
                                withContext(Dispatchers.IO) {
                                    file.writeText(safetyJson, Charsets.UTF_8)
                                }
                            }
                            val result = runCatching {
                                viewModel.importBackupJson(backupImport.json)
                            }
                            pendingImportConfirmation = null
                            if (result.isSuccess) {
                                selectedMediaId = null
                                selectedCollectionId = null
                                isAdding = false
                                snackbarHostState.showSnackbar(importSuccessMessage)
                            } else {
                                val message = when (result.exceptionOrNull()) {
                                    is UnsupportedBackupSchemaException -> importUnsupportedMessage
                                    else -> importInvalidMessage
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.import_backup_final_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportConfirmation = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingPossibleDuplicate?.let { duplicate ->
        AlertDialog(
            onDismissRequest = { pendingPossibleDuplicate = null },
            title = { Text(text = stringResource(R.string.possible_duplicate_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.possible_duplicate_message,
                        duplicate.trackedMedia.item.title,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPossibleDuplicate = null
                        openTrackedMedia(duplicate.trackedMedia)
                    },
                ) {
                    Text(text = stringResource(R.string.possible_duplicate_open_existing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val suggestion = duplicate.suggestion
                        val needsTransition = duplicate.requiresAddTransition
                        pendingPossibleDuplicate = null
                        viewModel.selectMetadataSuggestion(suggestion)
                        if (needsTransition) {
                            isAdding = true
                            selectedCollectionId = null
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.possible_duplicate_continue_create))
                }
            },
        )
    }

}

private data class PendingBackupImport(
    val json: String,
    val preview: BackupPreview,
)

private data class PendingPossibleDuplicate(
    val suggestion: MetadataSuggestion,
    val trackedMedia: TrackedMedia,
    val requiresAddTransition: Boolean = false,
)

class DetailHeaderActions {
    var isEditingItemDetails by mutableStateOf(false)
    var isMenuExpanded by mutableStateOf(false)
    var isRefreshingMetadata by mutableStateOf(false)
    var onDeleteRequested: () -> Unit = {}
    var onManageExternalTrackingRequested: () -> Unit = {}
    var onRefreshMetadataRequested: () -> Unit = {}
}

class BackupHeaderActions {
    var isMenuExpanded by mutableStateOf(false)
    var onExportBackupRequested: () -> Unit = {}
    var onImportBackupRequested: () -> Unit = {}
    var onRestoreBackupRequested: () -> Unit = {}
}

private enum class AppDestination {
    Home,
    Section,
}

private sealed interface DuplicateMatch {
    data object None : DuplicateMatch
    data class Exact(val trackedMedia: TrackedMedia) : DuplicateMatch
    data class Possible(val trackedMedia: TrackedMedia) : DuplicateMatch
}

private fun List<TrackedMedia>.findDuplicateFor(suggestion: MetadataSuggestion): DuplicateMatch {
    val exactMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            trackedMedia.item.metadataSource == suggestion.source &&
            trackedMedia.item.metadataExternalId == suggestion.externalId
    }
    if (exactMatch != null) {
        return DuplicateMatch.Exact(exactMatch)
    }

    val possibleMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            trackedMedia.hasCompatibleReleaseYear(suggestion) &&
            trackedMedia.titleCandidates().intersect(suggestion.titleCandidates()).isNotEmpty()
    }

    return possibleMatch?.let(DuplicateMatch::Possible) ?: DuplicateMatch.None
}

private fun TrackedMedia.hasCompatibleReleaseYear(suggestion: MetadataSuggestion): Boolean {
    val existingYear = item.releaseYear
    val suggestionYear = suggestion.releaseYear
    return existingYear == null || suggestionYear == null || existingYear == suggestionYear
}

private fun TrackedMedia.titleCandidates(): Set<String> {
    return listOf(item.title, item.originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun MetadataSuggestion.titleCandidates(): Set<String> {
    return listOf(title, originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun String.normalizedDuplicateTitle(): String {
    val withoutMarks = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutMarks
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun com.nilpo.contenttracker.core.model.MediaType.homeSection(): MediaSection =
    when (this) {
        com.nilpo.contenttracker.core.model.MediaType.Anime -> MediaSection.Anime
        com.nilpo.contenttracker.core.model.MediaType.Book -> MediaSection.Books
        com.nilpo.contenttracker.core.model.MediaType.Movie,
        com.nilpo.contenttracker.core.model.MediaType.TvShow,
            -> MediaSection.Movies
        com.nilpo.contenttracker.core.model.MediaType.Game -> MediaSection.Games
    }

@Composable
private fun OmnilogBottomBar(
    selectedDestination: AppDestination,
    selectedSection: MediaSection,
    onHomeClick: () -> Unit,
    onSectionClick: (MediaSection) -> Unit,
) {
    Surface(
        color = HeaderBackground,
        contentColor = HeaderInk,
        shadowElevation = 12.dp,
    ) {
        Column {
            HorizontalDivider(color = HeaderLine.copy(alpha = 0.72f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderPanel.copy(alpha = 0.74f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OmnilogNavItem(
                    labelResId = R.string.nav_home,
                    iconResId = R.drawable.ic_nav_home,
                    accent = OmnilogColors.Dashboard,
                    selected = selectedDestination == AppDestination.Home,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f),
                )
                MediaSection.entries.forEach { section ->
                    OmnilogNavItem(
                        labelResId = section.titleResId,
                        iconResId = section.navIconResId(),
                        accent = section.accent,
                        selected = selectedDestination == AppDestination.Section && selectedSection == section,
                        onClick = { onSectionClick(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OmnilogNavItem(
    @StringRes labelResId: Int,
    @DrawableRes iconResId: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) accent else HeaderMuted.copy(alpha = 0.76f)
    val containerColor = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (selected) accent.copy(alpha = 0.36f) else Color.Transparent

    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(start = 4.dp, top = 7.dp, end = 4.dp, bottom = 11.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(if (selected) 28.dp else 26.dp),
                tint = contentColor,
            )
            Text(
                text = stringResource(labelResId),
                color = if (selected) HeaderInk else HeaderMuted.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@DrawableRes
private fun MediaSection.navIconResId(): Int {
    return when (this) {
        MediaSection.Anime -> R.drawable.ic_nav_anime
        MediaSection.Books -> R.drawable.ic_nav_books
        MediaSection.Movies -> R.drawable.ic_nav_tv
        MediaSection.Games -> R.drawable.ic_nav_games
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmnilogTopBar(
    accent: Color,
    showDetailActions: Boolean,
    showBackupActions: Boolean,
    detailActions: DetailHeaderActions,
    backupActions: BackupHeaderActions,
    onBack: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HeaderBackground,
            titleContentColor = HeaderInk,
            navigationIconContentColor = HeaderInk,
            actionIconContentColor = HeaderInk,
        ),
        navigationIcon = {
            if (showDetailActions) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = HeaderInk,
                    )
                    Text(
                        text = "‹",
                        color = Color.Transparent,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) {
                        append("Omni")
                    }
                    withStyle(SpanStyle(color = HeaderInk, fontWeight = FontWeight.ExtraBold)) {
                        append("log")
                    }
                },
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                )
            },
        actions = {
            if (showDetailActions) {
                Box {
                    IconButton(onClick = { detailActions.isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = HeaderMuted,
                        )
                        Text(
                            text = "⋮",
                            color = Color.Transparent,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    DropdownMenu(
                        expanded = detailActions.isMenuExpanded,
                        onDismissRequest = { detailActions.isMenuExpanded = false },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = HeaderPanel,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        HeaderMenuItem(
                            text = stringResource(
                                if (detailActions.isEditingItemDetails) {
                                    R.string.done_editing
                                } else {
                                    R.string.edit
                                },
                            ),
                            onClick = {
                                detailActions.isEditingItemDetails = !detailActions.isEditingItemDetails
                                detailActions.isMenuExpanded = false
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.detail_external_tracking),
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onManageExternalTrackingRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(
                                if (detailActions.isRefreshingMetadata) {
                                    R.string.refresh_metadata_loading
                                } else {
                                    R.string.refresh_metadata
                                },
                            ),
                            enabled = !detailActions.isRefreshingMetadata,
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onRefreshMetadataRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.delete),
                            destructive = true,
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onDeleteRequested()
                            },
                        )
                    }
                }
            } else if (showBackupActions) {
                Box {
                    IconButton(onClick = { backupActions.isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(R.string.account_menu),
                            tint = HeaderMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = backupActions.isMenuExpanded,
                        onDismissRequest = { backupActions.isMenuExpanded = false },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = HeaderPanel,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        HeaderMenuItem(
                            text = stringResource(R.string.export_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onExportBackupRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.import_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onImportBackupRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.restore_previous_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onRestoreBackupRequested()
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun HeaderMenuItem(
    text: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val textColor = when {
        !enabled -> HeaderMuted.copy(alpha = 0.62f)
        destructive -> MaterialTheme.colorScheme.error
        else -> HeaderInk
    }

    Row(
        modifier = Modifier
            .widthIn(min = 148.dp)
            .background(HeaderPanel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun collectionItemComparator(): Comparator<TrackedMedia> =
    compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
        .thenBy { it.item.releaseYear ?: Int.MAX_VALUE }
        .thenBy { it.item.title.lowercase() }

private fun List<TrackedMedia>.toAddCollectionOptions(): List<AddCollectionOption> {
    return filter { trackedMedia -> trackedMedia.collection != null }
        .groupBy { trackedMedia -> trackedMedia.collection!!.id }
        .mapNotNull { (_, trackedItems) ->
            val collection = trackedItems.firstNotNullOfOrNull { trackedMedia -> trackedMedia.collection }
                ?: return@mapNotNull null
            AddCollectionOption(
                collection = collection,
                mediaTypes = trackedItems.map { trackedMedia -> trackedMedia.item.type }.toSet(),
            )
        }
        .sortedBy { option -> option.collection.name.lowercase() }
}

private val HeaderBackground = OmnilogColors.AppBackground
private val HeaderPanel = OmnilogColors.AppPanel
private val HeaderLine = OmnilogColors.AppLine
private val HeaderInk = OmnilogColors.AppInk
private val HeaderMuted = OmnilogColors.AppMuted
