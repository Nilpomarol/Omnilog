package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus

@Composable
fun DetailScreen(
    trackedMedia: TrackedMedia,
    accent: Color,
    onBack: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    onUpdateSessionProgress: (Long, Int) -> Unit,
    onUpdateSessionStatus: (Long, TrackingStatus) -> Unit,
    onUpdateSessionRating: (Long, Int?) -> Unit,
    onUpdateSessionNotes: (Long, String?) -> Unit,
    onDeletePastSession: (Long) -> Unit,
    onAddExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTrackingSynced: (Long, Boolean) -> Unit,
    onDeleteExternalTracking: (Long) -> Unit,
    onUpdateMediaItemDetails: (Long, String, Long?, String?, Int?, OwnershipType) -> Unit,
    onUpdateSessionPlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
    onDeleteMediaItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSession = trackedMedia.currentSession
    val pastSessions = trackedMedia.sessions
        .filter { session -> session.id != currentSession?.id }
        .sortedBy { it.sessionNumber }
    var showDeleteConfirmation by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text(text = stringResource(R.string.delete))
                    }
                }
            }

            item {
                Text(
                    text = trackedMedia.item.title,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            item {
                ItemDetailsSection(
                    item = trackedMedia.item,
                    collection = trackedMedia.collection,
                    availableCollections = trackedMedia.availableCollections,
                    currentSession = currentSession,
                    onSaveItemDetails = { title, collectionId, newCollectionName, progressTotal, ownershipType ->
                        onUpdateMediaItemDetails(
                            trackedMedia.item.id,
                            title,
                            collectionId,
                            newCollectionName,
                            progressTotal,
                            ownershipType,
                        )
                    },
                    onSavePlatform = onUpdateSessionPlatform,
                )
            }

            item {
                NewSessionSection(
                    mediaItemId = trackedMedia.item.id,
                    currentSession = currentSession,
                    onStartNewSession = onStartNewSession,
                )
            }

            if (currentSession != null) {
                item {
                    CurrentSessionSection(
                        session = currentSession,
                        progressTotal = trackedMedia.item.progressTotal,
                        accent = accent,
                        onUpdateSessionProgress = onUpdateSessionProgress,
                        onUpdateSessionStatus = onUpdateSessionStatus,
                        onUpdateSessionRating = onUpdateSessionRating,
                        onUpdateSessionNotes = onUpdateSessionNotes,
                    )
                }
            }

            item {
                DetailSectionTitle(text = stringResource(R.string.detail_history))
            }

            if (pastSessions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                    )
                }
            }

            items(pastSessions) { session ->
                PastSessionSection(
                    session = session,
                    progressTotal = trackedMedia.item.progressTotal,
                    accent = accent,
                    onUpdateSessionProgress = onUpdateSessionProgress,
                    onUpdateSessionStatus = onUpdateSessionStatus,
                    onUpdateSessionRating = onUpdateSessionRating,
                    onUpdateSessionNotes = onUpdateSessionNotes,
                    onUpdateSessionPlatform = onUpdateSessionPlatform,
                    onDeleteSession = { onDeletePastSession(session.id) },
                )
            }

            if (trackedMedia.externalRatings.isNotEmpty()) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_external_scores))
                }
                items(trackedMedia.externalRatings) { rating ->
                    Text(
                        text = stringResource(
                            R.string.external_rating,
                            rating.source.name,
                            rating.score,
                            rating.maxScore,
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    )
                }
            }

            item {
                ExternalTrackingEditor(
                    externalTracking = trackedMedia.externalTracking,
                    onAddExternalTracking = { source, externalItemId, url ->
                        onAddExternalTracking(
                            trackedMedia.item.id,
                            source,
                            externalItemId,
                            url,
                        )
                    },
                    onUpdateSynced = onUpdateExternalTrackingSynced,
                    onDelete = onDeleteExternalTracking,
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.delete_media_title)) },
            text = { Text(text = stringResource(R.string.delete_media_message, trackedMedia.item.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMediaItem(trackedMedia.item.id)
                        showDeleteConfirmation = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
