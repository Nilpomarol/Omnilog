package com.nilpo.contenttracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.UnsupportedBackupSchemaException
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.home.CollectionDetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeLandingScreen
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<AppDestination>(AppDestination.Home) }
    var detailActions by remember { mutableStateOf(DetailHeaderActions()) }
    val selectedMedia = uiState.trackedItems.firstOrNull { it.item.id == selectedMediaId }
    val selectedCollection = uiState.trackedItems
        .mapNotNull { it.collection }
        .firstOrNull { it.id == selectedCollectionId }
    val selectedCollectionItems = uiState.trackedItems
        .filter { it.collection?.id == selectedCollectionId }
    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportErrorMessage = stringResource(R.string.backup_export_error)
    val importReadErrorMessage = stringResource(R.string.backup_import_read_error)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val importInvalidMessage = stringResource(R.string.backup_import_invalid)
    val importUnsupportedMessage = stringResource(R.string.backup_import_unsupported)
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

    Scaffold(
        topBar = {
            OmnilogTopBar(
                accent = when (selectedDestination) {
                    AppDestination.Home -> MaterialTheme.colorScheme.primary
                    AppDestination.Section -> uiState.selectedSection.accent
                },
                showDetailActions = selectedMedia != null && !isAdding,
                detailActions = detailActions,
                onBack = { selectedMediaId = null },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedDestination == AppDestination.Home,
                    onClick = {
                        selectedDestination = AppDestination.Home
                        selectedMediaId = null
                        selectedCollectionId = null
                        isAdding = false
                    },
                    label = { Text(stringResource(R.string.nav_home)) },
                    icon = {
                        NavMark(
                            label = "O",
                            accent = MaterialTheme.colorScheme.primary,
                            selected = selectedDestination == AppDestination.Home,
                        )
                    },
                )
                MediaSection.entries.forEach { section ->
                    val title = stringResource(section.titleResId)
                    val selected = selectedDestination == AppDestination.Section && uiState.selectedSection == section
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            selectedDestination = AppDestination.Section
                            selectedMediaId = null
                            selectedCollectionId = null
                            isAdding = false
                            viewModel.selectSection(section)
                        },
                        label = { Text(title) },
                        icon = {
                            NavMark(
                                label = section.navMark,
                                accent = section.accent,
                                selected = selected,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (isAdding) {
            AddMediaScreen(
                initialMediaType = uiState.selectedSection.defaultType,
                availableMediaTypes = uiState.selectedSection.types.toList(),
                onSave = { request ->
                    viewModel.addTrackedMedia(request)
                    viewModel.clearMetadataSearch()
                    isAdding = false
                },
                metadataUiState = metadataUiState,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = viewModel::searchMetadataSuggestions,
                onMetadataSuggestionSelected = viewModel::selectMetadataSuggestion,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedMedia == null) {
            HomeScreen(
                uiState = uiState,
                onMediaClick = { selectedMediaId = it.item.id },
                onCollectionClick = {
                    selectedCollectionId = it.id
                    isAdding = false
                },
                onAddClick = {
                    viewModel.clearMetadataSearch()
                    isAdding = true
                    selectedCollectionId = null
                },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onStatusFilterChange = viewModel::updateStatusFilter,
                onSortModeChange = viewModel::updateSortMode,
                onSortDirectionChange = viewModel::updateSortDirection,
                onExportBackup = {
                    exportBackupLauncher.launch("content-tracker-backup-${LocalDate.now()}.json")
                },
                onImportBackup = {
                    importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            val actions = remember(selectedMedia.item.id) {
                DetailHeaderActions()
            }
            detailActions = actions
            DetailScreen(
                trackedMedia = selectedMedia,
                accent = uiState.selectedSection.accent,
                headerActions = actions,
                onBack = { selectedMediaId = null },
                onStartNewSession = viewModel::startNewSession,
                onUpdateSessionProgress = viewModel::updateSessionProgress,
                onUpdateSessionStatus = viewModel::updateSessionStatus,
                onUpdateSessionRating = viewModel::updateSessionRating,
                onUpdateSessionNotes = viewModel::updateSessionNotes,
                onUpdateSessionDetails = viewModel::updateSessionDetails,
                onDeletePastSession = viewModel::deletePastSession,
                onAddExternalTracking = viewModel::addExternalTracking,
                onUpdateExternalTrackingSynced = viewModel::updateExternalTrackingSynced,
                onDeleteExternalTracking = viewModel::deleteExternalTracking,
                onUpdateMediaItemDetails = viewModel::updateMediaItemDetails,
                onUpdateSessionPlatform = viewModel::updateSessionPlatform,
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

    pendingImport?.let { backupImport ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(text = stringResource(R.string.import_backup_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.import_backup_message_with_summary,
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
                        coroutineScope.launch {
                            val result = runCatching {
                                viewModel.importBackupJson(backupImport.json)
                            }
                            if (result.isSuccess) {
                                selectedMediaId = null
                                selectedCollectionId = null
                                isAdding = false
                                pendingImport = null
                                snackbarHostState.showSnackbar(importSuccessMessage)
                            } else {
                                pendingImport = null
                                val message = when (result.exceptionOrNull()) {
                                    is UnsupportedBackupSchemaException -> importUnsupportedMessage
                                    else -> importInvalidMessage
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
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

}

private data class PendingBackupImport(
    val json: String,
    val preview: BackupPreview,
)

class DetailHeaderActions {
    var isEditingItemDetails by mutableStateOf(false)
    var isMenuExpanded by mutableStateOf(false)
    var onDeleteRequested: () -> Unit = {}
}

private enum class AppDestination {
    Home,
    Section,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmnilogTopBar(
    accent: Color,
    showDetailActions: Boolean,
    detailActions: DetailHeaderActions,
    onBack: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        navigationIcon = {
            if (showDetailActions) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        },
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                        append("Omni")
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)) {
                        append("log")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        actions = {
            if (showDetailActions) {
                Box {
                    IconButton(onClick = { detailActions.isMenuExpanded = true }) {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    DropdownMenu(
                        expanded = detailActions.isMenuExpanded,
                        onDismissRequest = { detailActions.isMenuExpanded = false },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
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
                            text = stringResource(R.string.delete),
                            destructive = true,
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onDeleteRequested()
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
    onClick: () -> Unit,
) {
    val textColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .widthIn(min = 148.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NavMark(
    label: String,
    accent: Color,
    selected: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
