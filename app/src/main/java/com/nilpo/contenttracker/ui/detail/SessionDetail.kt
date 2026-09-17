package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

/**
 * A session that is over, as a row of the history rather than a card of its own.
 *
 * It keeps the live card's language — the tinted chip, the stars, the progress graphic — without a
 * panel around it, and the controls stay quiet icons in the header row. [SessionHistorySection] splits
 * the rows with hairlines, so several re-reads read as a chronology rather than a stack of boxes.
 */
@Composable
fun SessionDetail(
    session: TrackingSession,
    visitNumber: Int,
    progressTotal: Int?,
    mediaType: MediaType,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by rememberSaveable(session.id) { mutableStateOf(false) }
    val visual = sessionStateVisual(session.status)
    val state = visual.color

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionStateChip(visual = visual)
            Text(
                text = visitLabel(visitNumber),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityAction(
                    session = session,
                    mediaType = mediaType,
                    accent = state,
                    onDeleteProgressUpdate = onDeleteProgressUpdate,
                    onUpdateProgressUpdate = onUpdateProgressUpdate,
                    onDeleteStatusEvent = onDeleteStatusEvent,
                    onUpdateStatusEventDate = onUpdateStatusEventDate,
                    iconOnly = true,
                )
                trailingContent?.invoke()
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        session.ratingHalfPoints?.let { halfPoints ->
            SessionStars(halfPoints = halfPoints, accent = state, starSize = 18.dp)
        }

        SessionProgressBar(
            progressCurrent = session.progressCurrent,
            progressTotal = progressTotal,
            color = state,
            track = OmnilogTheme.colors.appLine,
            thickness = 4.dp,
        )

        SessionDatesRow(
            session = session,
            mediaType = mediaType,
            compact = true,
            trailing = sessionRecencyLabel(session),
        )

        session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                color = OmnilogTheme.colors.appInk.copy(alpha = 0.70f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDeleteConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = stringResource(R.string.delete_session_title),
            text = { Text(text = stringResource(R.string.delete_session_message, visitNumber)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteConfirmation = false
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

@Composable
private fun visitLabel(visitNumber: Int): String =
    when {
        visitNumber <= 1 -> stringResource(R.string.session_first_time)
        else -> stringResource(R.string.session_number, visitNumber)
    }
