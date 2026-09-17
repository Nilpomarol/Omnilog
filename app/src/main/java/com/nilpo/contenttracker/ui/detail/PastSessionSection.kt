package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
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
    onUpdateStatusEventDate: (Long, LocalDate?) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteSession: () -> Unit,
) {
    var isEditing by rememberSaveable(session.id) { mutableStateOf(false) }

    SessionDetail(
        session = session,
        visitNumber = visitNumber,
        progressTotal = progressTotal,
        mediaType = mediaType,
        onDeleteProgressUpdate = onDeleteProgressUpdate,
        onDeleteStatusEvent = onDeleteStatusEvent,
        onUpdateStatusEventDate = onUpdateStatusEventDate,
        onUpdateProgressUpdate = onUpdateProgressUpdate,
        onDelete = onDeleteSession,
        trailingContent = {
            IconButton(onClick = { isEditing = true }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = OmnilogTheme.colors.appMuted,
                    modifier = Modifier.size(20.dp),
                )
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
