package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun PastSessionSection(
    session: TrackingSession,
    seasons: List<SeasonProgress>,
    accent: Color,
    mediaType: MediaType,
    onUpdateSessionProgress: (Long, Int) -> Unit,
    onUpdateSessionProgressTotal: (Long, Int?) -> Unit,
    onUpdateSessionStatus: (Long, TrackingStatus) -> Unit,
    onUpdateSessionRating: (Long, Int?) -> Unit,
    onUpdateSessionNotes: (Long, String?) -> Unit,
    onUpdateSessionPlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
    onUpdateSeasonProgress: (Long, Int) -> Unit,
    onUpdateSeasonProgressTotal: (Long, Int?) -> Unit,
    onDeleteSeasonProgress: (Long) -> Unit,
    onDeleteSession: () -> Unit,
) {
    var isEditing by rememberSaveable(session.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionDetail(
            session = session,
            seasons = seasons,
            accent = accent,
            onDelete = onDeleteSession,
            trailingContent = {
                TextButton(onClick = { isEditing = !isEditing }) {
                    val label = if (isEditing) {
                        stringResource(R.string.done_editing)
                    } else {
                        stringResource(R.string.edit)
                    }
                    Text(text = label)
                }
            },
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
            SessionPlatformEditor(
                session = session,
                onSavePlatform = onUpdateSessionPlatform,
            )
            if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
                seasons.forEach { season ->
                    SeasonProgressEditor(
                        season = season,
                        onSave = { progress ->
                            onUpdateSeasonProgress(season.id, progress)
                        },
                        onSaveTotal = { total ->
                            onUpdateSeasonProgressTotal(season.id, total)
                        },
                        onDelete = {
                            onDeleteSeasonProgress(season.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionPlatformEditor(
    session: TrackingSession,
    onSavePlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
) {
    var platformName by remember { mutableStateOf(session.platform?.name.orEmpty()) }
    var selectedPlatformType by remember {
        mutableStateOf(session.platform?.type ?: ConsumptionPlatformType.Other)
    }

    LaunchedEffect(session.id, session.platform) {
        platformName = session.platform?.name.orEmpty()
        selectedPlatformType = session.platform?.type ?: ConsumptionPlatformType.Other
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = platformName,
            onValueChange = { platformName = it },
            label = { Text(stringResource(R.string.field_platform)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OptionSelector(
            label = stringResource(R.string.field_platform_type),
            options = ConsumptionPlatformType.entries,
            selectedOption = selectedPlatformType,
            optionLabel = { platformType -> platformType.label() },
            onOptionSelected = { platformType -> selectedPlatformType = platformType },
        )

        Button(
            onClick = {
                onSavePlatform(session.id, platformName, selectedPlatformType)
            },
        ) {
            Text(text = stringResource(R.string.update_platform))
        }
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
