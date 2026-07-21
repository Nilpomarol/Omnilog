package com.nilpo.contenttracker.ui.detail

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

@Composable
fun PastSessionSection(
    session: TrackingSession,
    visitNumber: Int,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
    onDeleteSession: () -> Unit,
) {
    var isEditing by rememberSaveable(session.id) { mutableStateOf(false) }

    SessionDetail(
        session = session,
        visitNumber = visitNumber,
        progressTotal = progressTotal,
        mediaType = mediaType,
        accent = accent,
        onDeleteProgressUpdate = onDeleteProgressUpdate,
        onDeleteStatusEvent = onDeleteStatusEvent,
        onUpdateProgressUpdate = onUpdateProgressUpdate,
        onDelete = onDeleteSession,
        trailingContent = {
            TextButton(onClick = { isEditing = true }) {
                Text(text = stringResource(R.string.edit))
            }
        },
    )

    if (isEditing) {
        Dialog(
            onDismissRequest = { isEditing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SessionEditorScreen(
                session = session,
                progressTotal = progressTotal,
                mediaType = mediaType,
                accent = accent,
                onBack = { isEditing = false },
                onSaveSessionDetails = onUpdateSessionDetails,
            )
        }
    }
}
