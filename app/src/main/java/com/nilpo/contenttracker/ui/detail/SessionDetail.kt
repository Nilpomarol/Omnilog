package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import java.time.LocalDate

/**
 * A session that is over, drawn as a compact echo of the live card.
 *
 * It takes the live card's language — the same `cardGround`, the same type artwork, the same filled
 * state chip and accent-drawn progress graphic on the same lifted track — and quiets it: the artwork
 * is dimmed to [PastArtStrength] so a stack of re-reads does not shout as loudly as the live card
 * above them, the controls collapse to a row of icon discs in the header rather than a labelled row of
 * their own, and the hero figure and log button are gone. What is left is the same card at half the
 * height.
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        // A shade smaller than the live card's 18dp, so the two read as the same object at two sizes.
        shape = RoundedCornerShape(14.dp),
        color = cardGround(),
    ) {
        Column(
            modifier = Modifier
                .sessionCardArtwork(mediaType, strength = PastArtStrength)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The state, the visit it was, and every control — all on one line. The controls are the
            // live card's activity-and-edit row, extended with delete and reduced to bare discs, so
            // the state stays the loudest thing and the buttons tuck into the top corner.
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        accent = state,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                        onUpdateStatusEventDate = onUpdateStatusEventDate,
                        iconOnly = true,
                    )
                    trailingContent?.invoke()
                    if (onDelete != null) {
                        FilledTonalIconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            session.ratingHalfPoints?.let { halfPoints ->
                RatingLine(ratingHalfPoints = halfPoints, accent = state)
            }

            SessionProgressGraphic(
                progressCurrent = session.progressCurrent,
                progressTotal = progressTotal,
                mediaType = mediaType,
                progressUpdates = session.progressUpdates,
                color = state,
                compact = true,
                // Artwork sits behind the graphic here as it does on the live card, so the track has
                // to be the same opaque lift rather than a translucent accent the drawing shows through.
                track = cardTrack(),
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

/**
 * The rating, between the live card's hero figure and the old corner value in size.
 *
 * The live card sets a rating in `displayMedium`; a history card at that size would out-shout the
 * card it belongs under, and the corner figure it replaces was too small to register as the verdict.
 * `headlineSmall` with the app's star beside it lands in between — plainly a rating, plainly quieter
 * than the live one.
 */
@Composable
private fun RatingLine(ratingHalfPoints: Int, accent: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_kpi_rating),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(20.dp),
            tint = accent,
        )
        Text(
            text = formatRatingHalfPoints(RatingHalfPoints.coerce(ratingHalfPoints)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
        )
        Text(
            text = "/10",
            modifier = Modifier.padding(bottom = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent.copy(alpha = 0.62f),
        )
    }
}

/** How far the history cards pull the type artwork back from the live card's full strength. */
private const val PastArtStrength = 0.5f

@Composable
private fun visitLabel(visitNumber: Int): String =
    when {
        visitNumber <= 1 -> stringResource(R.string.session_first_time)
        else -> stringResource(R.string.session_number, visitNumber)
    }
