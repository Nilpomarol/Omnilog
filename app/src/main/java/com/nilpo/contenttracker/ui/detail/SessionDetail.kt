package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.RatingMeterCompact
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

/**
 * A session that is over, deliberately demoted.
 *
 * This used to be drawn at exactly the weight of the live card — same 12dp panel, same tinted border,
 * same full-size rating strip — so a title you had been through three times showed four identical
 * boxes and nothing said which one was happening now. Here the state is a dot rather than a filled
 * chip, the graphic is the compact variant, and the whole card is a summary you can scan down.
 *
 * What it keeps is the state colour, because reading a run of these top to bottom — green, red,
 * green — is the fastest account of how a title has gone, and that only works if every card is
 * coloured by its own outcome.
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionStateDot(
                    visual = visual,
                    text = "${visual.label} · ${visitLabel(visitNumber)}",
                    modifier = Modifier.weight(1f),
                )
                session.rating?.let { rating ->
                    RatingMeterCompact(rating = rating, accent = visual.color)
                }
            }

            SessionProgressGraphic(
                progressCurrent = session.progressCurrent,
                progressTotal = progressTotal,
                mediaType = mediaType,
                progressUpdates = session.progressUpdates,
                color = visual.color,
                compact = true,
            )

            SessionDatesRow(session = session, mediaType = mediaType, compact = true)

            session.platform?.let { platform ->
                Text(
                    text = stringResource(R.string.platform_label, platform.name),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    color = OmnilogTheme.colors.appInk.copy(alpha = 0.70f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // The controls sit under the summary rather than beside the heading. On the old card they
            // shared the top row with the status and the visit number, which left the state — the one
            // thing you scan a history for — competing with three buttons for the same line.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityAction(
                    updates = session.progressUpdates,
                    statusEvents = session.statusEvents,
                    baselineProgress = session.baselineProgress,
                    sessionStartedAt = session.startedAt,
                    sessionFinishedAt = session.finishedAt,
                    sessionStatus = session.status,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    accent = visual.color,
                    onDeleteProgressUpdate = onDeleteProgressUpdate,
                    onUpdateProgressUpdate = onUpdateProgressUpdate,
                    onDeleteStatusEvent = onDeleteStatusEvent,
                    onUpdateStatusEventDate = onUpdateStatusEventDate,
                )
                trailingContent?.invoke()
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
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
