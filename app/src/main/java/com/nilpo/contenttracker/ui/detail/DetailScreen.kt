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
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus

@Composable
fun DetailScreen(
    trackedMedia: TrackedMedia,
    accent: Color,
    onBack: () -> Unit,
    onStartNewSession: () -> Unit,
    onUpdateSessionProgress: (Long, Int) -> Unit,
    onUpdateSessionStatus: (Long, TrackingStatus) -> Unit,
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
                Button(onClick = onStartNewSession) {
                    Text(text = stringResource(R.string.start_new_session))
                }
            }

            if (currentSession != null) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_current_session))
                    SessionDetail(
                        session = currentSession,
                        seasons = trackedMedia.seasonsFor(currentSession),
                        accent = accent,
                    )
                    ProgressEditor(
                        session = currentSession,
                        onSave = { progress ->
                            onUpdateSessionProgress(currentSession.id, progress)
                        },
                    )
                    StatusSelector(
                        selectedStatus = currentSession.status,
                        onStatusSelected = { status ->
                            onUpdateSessionStatus(currentSession.id, status)
                        },
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

            if (trackedMedia.externalTracking.isNotEmpty()) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_external_tracking))
                }
                items(trackedMedia.externalTracking) { tracking ->
                    val syncText = if (tracking.isSynced) {
                        stringResource(R.string.synced_yes)
                    } else {
                        stringResource(R.string.synced_no)
                    }
                    Text(
                        text = "${tracking.source.name} - $syncText",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}
