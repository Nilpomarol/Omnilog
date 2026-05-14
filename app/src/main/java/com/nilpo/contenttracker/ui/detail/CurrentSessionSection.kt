package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus

@Composable
fun CurrentSessionSection(
    session: TrackingSession,
    seasons: List<SeasonProgress>,
    accent: Color,
    onUpdateSessionProgress: (Long, Int) -> Unit,
    onUpdateSessionProgressTotal: (Long, Int?) -> Unit,
    onUpdateSessionStatus: (Long, TrackingStatus) -> Unit,
    onUpdateSessionRating: (Long, Int?) -> Unit,
    onUpdateSessionNotes: (Long, String?) -> Unit,
    onUpdateSeasonProgress: (Long, Int) -> Unit,
    onAddSeasonProgress: (Long, Int, Int?) -> Unit,
    mediaType: MediaType,
) {
    var isEditing by rememberSaveable(session.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_current_session),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { isEditing = !isEditing }) {
                val label = if (isEditing) {
                    stringResource(R.string.done_editing)
                } else {
                    stringResource(R.string.edit)
                }
                Text(text = label)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f))

        SessionDetail(
            session = session,
            seasons = seasons,
            accent = accent,
        )

        if (isEditing) {
            ProgressEditor(
                session = session,
                onSave = { progress ->
                    onUpdateSessionProgress(session.id, progress)
                },
            )
            TotalProgressEditor(
                session = session,
                onSave = { total ->
                    onUpdateSessionProgressTotal(session.id, total)
                },
            )
            StatusSelector(
                selectedStatus = session.status,
                onStatusSelected = { status ->
                    onUpdateSessionStatus(session.id, status)
                },
            )
            RatingEditor(
                session = session,
                onSave = { rating ->
                    onUpdateSessionRating(session.id, rating)
                },
            )
            NotesEditor(
                session = session,
                onSave = { notes ->
                    onUpdateSessionNotes(session.id, notes)
                },
            )
            seasons.forEach { season ->
                SeasonProgressEditor(
                    season = season,
                    onSave = { progress ->
                        onUpdateSeasonProgress(season.id, progress)
                    },
                )
            }
            if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
                AddSeasonEditor(
                    existingSeasons = seasons,
                    onAddSeason = { seasonNumber, total ->
                        onAddSeasonProgress(session.id, seasonNumber, total)
                    },
                )
            }
        }
    }
}
