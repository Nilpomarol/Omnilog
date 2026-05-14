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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    val selectedMedia = uiState.trackedItems.firstOrNull { it.item.id == selectedMediaId }
    val selectedCollection = uiState.trackedItems
        .mapNotNull { it.collection }
        .firstOrNull { it.id == selectedCollectionId }
    val selectedCollectionItems = uiState.trackedItems
        .filter { it.collection?.id == selectedCollectionId }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val backupJson = viewModel.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(backupJson.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                pendingImportJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().toString(Charsets.UTF_8)
                    }
                }
            }
        }
    }

    Scaffold(
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

    pendingImportJson?.let { backupJson ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(text = stringResource(R.string.import_backup_title)) },
            text = { Text(text = stringResource(R.string.import_backup_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importBackupJson(backupJson)
                        selectedMediaId = null
                        selectedCollectionId = null
                        isAdding = false
                        pendingImportJson = null
                    },
                ) {
                    Text(text = stringResource(R.string.import_backup_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
