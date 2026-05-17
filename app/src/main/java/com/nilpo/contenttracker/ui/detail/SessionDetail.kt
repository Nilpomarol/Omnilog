package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionDetail(
    session: TrackingSession,
    progressTotal: Int?,
    accent: Color,
    onDelete: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var showDeleteConfirmation by rememberSaveable(session.id) { mutableStateOf(false) }
    val statusColor = session.status.detailColor(accent)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = session.visitLabel(),
                        color = OmnilogColors.AppInk,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    StatusChip(
                        label = session.status.label(),
                        color = statusColor,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

            SessionProgress(
                session = session,
                progressTotal = progressTotal,
                color = statusColor,
            )

            SessionRating(rating = session.rating, color = statusColor)

            SessionMetaRow(session = session)

            session.platform?.let { platform ->
                Text(
                    text = stringResource(R.string.platform_label, platform.name),
                    color = OmnilogColors.AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    color = OmnilogColors.AppInk.copy(alpha = 0.70f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.delete_session_title)) },
            text = { Text(text = stringResource(R.string.delete_session_message, session.sessionNumber)) },
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
private fun TrackingSession.visitLabel(): String =
    when {
        sessionNumber == 1 -> stringResource(R.string.session_first_time)
        else -> stringResource(R.string.session_number, sessionNumber)
    }

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
        contentColor = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SessionProgress(
    session: TrackingSession,
    progressTotal: Int?,
    color: Color,
) {
    if (session.status == TrackingStatus.Planned && session.progressCurrent == 0) return

    val progressText = if (progressTotal != null && progressTotal > 0) {
        stringResource(R.string.progress_with_total, session.progressCurrent.coerceAtMost(progressTotal), progressTotal)
    } else {
        stringResource(R.string.progress_value, session.progressCurrent)
    }
    val fraction = progressTotal
        ?.takeIf { it > 0 }
        ?.let { total -> session.progressCurrent.toFloat().div(total.toFloat()).coerceIn(0f, 1f) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = progressText,
            color = OmnilogColors.AppInk.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (fraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(OmnilogColors.AppLine.copy(alpha = 0.78f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun SessionRating(rating: Int?, color: Color) {
    if (rating == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.rating_value, rating),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(end = 4.dp),
        )
        repeat(10) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (index < rating.coerceIn(0, 10)) {
                    color
                } else {
                    OmnilogColors.AppMuted.copy(alpha = 0.22f)
                },
            )
        }
    }
}

@Composable
private fun SessionMetaRow(session: TrackingSession) {
    val finishedAt = session.finishedAt?.let { stringResource(R.string.session_finished_at, it.formatDate()) }
    val startedAt = session.startedAt?.let { stringResource(R.string.session_started_at, it.formatDate()) }
    val updatedAt = session.updatedDate()?.let { stringResource(R.string.session_updated_at, it.formatDate()) }
    val values = listOfNotNull(finishedAt, startedAt, updatedAt)
    if (values.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.take(3).forEach { value ->
            Text(
                text = value,
                color = OmnilogColors.AppMuted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackingStatus.label(): String =
    when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }

private fun TrackingStatus.detailColor(accent: Color): Color =
    when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }.takeIf { it != Color.Unspecified } ?: accent

private fun TrackingSession.updatedDate(): LocalDate? {
    if (updatedAtEpochMillis <= 0L) return null
    return Instant.ofEpochMilli(updatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
