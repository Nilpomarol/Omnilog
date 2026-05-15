package com.nilpo.contenttracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.UnsupportedBackupSchemaException
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.home.CollectionDetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MediaSection.entries.forEach { section ->
                    val title = stringResource(section.titleResId)
                    NavigationBarItem(
                        selected = uiState.selectedSection == section,
                        onClick = {
                            selectedMediaId = null
                            selectedCollectionId = null
                            isAdding = false
                            viewModel.selectSection(section)
                        },
                        label = { Text(title) },
                        icon = { Text(title.first().toString()) },
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
                    isAdding = false
                },
                metadataUiState = metadataUiState,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = viewModel::searchMetadataSuggestions,
                onCancel = {
                    isAdding = false
                    selectedCollectionId = null
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
            DetailScreen(
                trackedMedia = selectedMedia,
                accent = uiState.selectedSection.accent,
                onBack = { selectedMediaId = null },
                onStartNewSession = viewModel::startNewSession,
                onUpdateSessionProgress = viewModel::updateSessionProgress,
                onUpdateSessionStatus = viewModel::updateSessionStatus,
                onUpdateSessionRating = viewModel::updateSessionRating,
                onUpdateSessionNotes = viewModel::updateSessionNotes,
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
