package com.nilpo.contenttracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection

@Composable
fun ContentTrackerApp(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    val selectedMedia = uiState.trackedItems.firstOrNull { it.item.id == selectedMediaId }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MediaSection.entries.forEach { section ->
                    val title = stringResource(section.titleResId)
                    NavigationBarItem(
                        selected = uiState.selectedSection == section,
                        onClick = {
                            selectedMediaId = null
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
                onCancel = { isAdding = false },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedMedia == null) {
            HomeScreen(
                uiState = uiState,
                onMediaClick = { selectedMediaId = it.item.id },
                onAddClick = { isAdding = true },
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
                onUpdateSessionProgressTotal = viewModel::updateSessionProgressTotal,
                onUpdateSessionStatus = viewModel::updateSessionStatus,
                onUpdateSessionRating = viewModel::updateSessionRating,
                onUpdateSessionNotes = viewModel::updateSessionNotes,
                onUpdateSeasonProgress = viewModel::updateSeasonProgress,
                onUpdateSeasonProgressTotal = viewModel::updateSeasonProgressTotal,
                onDeleteSeasonProgress = viewModel::deleteSeasonProgress,
                onDeletePastSession = viewModel::deletePastSession,
                onAddSeasonProgress = viewModel::addSeasonProgress,
                onAddExternalTracking = viewModel::addExternalTracking,
                onUpdateExternalTrackingSynced = viewModel::updateExternalTrackingSynced,
                onDeleteExternalTracking = viewModel::deleteExternalTracking,
                onUpdateMediaItemDetails = viewModel::updateMediaItemDetails,
                onUpdateSessionPlatform = viewModel::updateSessionPlatform,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
