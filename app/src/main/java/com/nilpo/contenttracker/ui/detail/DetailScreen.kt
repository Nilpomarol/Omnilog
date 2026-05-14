package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
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
    onStartNewSession: () -> Unit,
    onUpdateSessionProgress: (Long, Int) -> Unit,
    onUpdateSessionProgressTotal: (Long, Int?) -> Unit,
    onUpdateSessionStatus: (Long, TrackingStatus) -> Unit,
    onUpdateSessionRating: (Long, Int?) -> Unit,
    onUpdateSessionNotes: (Long, String?) -> Unit,
    onUpdateSeasonProgress: (Long, Int) -> Unit,
    onAddExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTrackingSynced: (Long, Boolean) -> Unit,
    onDeleteExternalTracking: (Long) -> Unit,
    onUpdateMediaItemDetails: (Long, String, OwnershipType) -> Unit,
    onUpdateSessionPlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSession = trackedMedia.currentSession

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
                Button(onClick = onBack) {
                    Text(text = stringResource(R.string.back))
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
                    currentSession = currentSession,
                    onSaveItemDetails = { title, ownershipType ->
                        onUpdateMediaItemDetails(
                            trackedMedia.item.id,
                            title,
                            ownershipType,
                        )
                    },
                    onSavePlatform = onUpdateSessionPlatform,
                )
            }

            item {
                Button(onClick = onStartNewSession) {
                    Text(text = stringResource(R.string.start_new_session))
                }
            }

            if (currentSession != null) {
                item {
                    CurrentSessionSection(
                        session = currentSession,
                        seasons = trackedMedia.seasonsFor(currentSession),
                        accent = accent,
                        onUpdateSessionProgress = onUpdateSessionProgress,
                        onUpdateSessionProgressTotal = onUpdateSessionProgressTotal,
                        onUpdateSessionStatus = onUpdateSessionStatus,
                        onUpdateSessionRating = onUpdateSessionRating,
                        onUpdateSessionNotes = onUpdateSessionNotes,
                        onUpdateSeasonProgress = onUpdateSeasonProgress,
                    )
                }
            }

            item {
                DetailSectionTitle(text = stringResource(R.string.detail_history))
            }

            items(trackedMedia.sessions.sortedBy { it.sessionNumber }) { session ->
                SessionDetail(
                    session = session,
                    seasons = trackedMedia.seasonsFor(session),
                    accent = accent,
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
}
