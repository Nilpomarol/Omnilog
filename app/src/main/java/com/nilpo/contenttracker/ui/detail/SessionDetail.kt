package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.seasonSummary
import com.nilpo.contenttracker.ui.common.sessionLabel
import com.nilpo.contenttracker.ui.common.sessionSummary

@Composable
fun SessionDetail(
    session: TrackingSession,
    seasons: List<SeasonProgress>,
    accent: Color,
    onDelete: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var showDeleteConfirmation by rememberSaveable(session.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = sessionLabel(session),
                color = accent,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trailingContent?.invoke()
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text(text = stringResource(R.string.delete))
                    }
                }
            }
        }
        Text(
            text = sessionSummary(session),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
        seasonSummary(seasons)?.let { seasonText ->
            Text(
                text = seasonText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        session.platform?.let { platform ->
            Text(
                text = stringResource(R.string.platform_label, platform.name),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        session.notes?.let { notes ->
            Text(
                text = notes,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
            )
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
