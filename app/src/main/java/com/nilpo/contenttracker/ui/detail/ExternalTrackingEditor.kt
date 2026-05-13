package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalTracking
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun ExternalTrackingEditor(
    externalTracking: List<ExternalTracking>,
    onAddExternalTracking: (ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateSynced: (Long, Boolean) -> Unit,
) {
    var selectedSource by remember { mutableStateOf(ExternalTrackingSource.Mal) }
    var externalItemId by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        externalTracking.forEach { tracking ->
            ExternalTrackingRow(
                tracking = tracking,
                onUpdateSynced = onUpdateSynced,
            )
        }

        OptionSelector(
            label = stringResource(R.string.field_external_tracking_source),
            options = ExternalTrackingSource.entries,
            selectedOption = selectedSource,
            optionLabel = { source -> source.label() },
            onOptionSelected = { source -> selectedSource = source },
        )

        OutlinedTextField(
            value = externalItemId,
            onValueChange = { externalItemId = it },
            label = { Text(stringResource(R.string.field_external_tracking_id)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.field_external_tracking_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = {
                onAddExternalTracking(selectedSource, externalItemId, url)
                externalItemId = ""
                url = ""
            },
        ) {
            Text(text = stringResource(R.string.add_external_tracking))
        }
    }
}

@Composable
private fun ExternalTrackingRow(
    tracking: ExternalTracking,
    onUpdateSynced: (Long, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val syncText = if (tracking.isSynced) {
                stringResource(R.string.synced_yes)
            } else {
                stringResource(R.string.synced_no)
            }
            Text(
                text = "${tracking.source.label()} - $syncText",
                style = MaterialTheme.typography.bodyLarge,
            )
            tracking.externalItemId?.let { itemId ->
                Text(
                    text = stringResource(R.string.external_tracking_id_value, itemId),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }
            tracking.url?.let { trackingUrl ->
                Text(
                    text = trackingUrl,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }
        }
        TextButton(
            onClick = {
                onUpdateSynced(tracking.id, !tracking.isSynced)
            },
        ) {
            val buttonText = if (tracking.isSynced) {
                stringResource(R.string.mark_external_tracking_pending)
            } else {
                stringResource(R.string.mark_external_tracking_synced)
            }
            Text(text = buttonText)
        }
    }
}

@Composable
private fun ExternalTrackingSource.label(): String {
    return when (this) {
        ExternalTrackingSource.Mal -> "MAL"
        ExternalTrackingSource.Imdb -> "IMDb"
        ExternalTrackingSource.StoryGraph -> "StoryGraph"
        ExternalTrackingSource.Goodreads -> "Goodreads"
        ExternalTrackingSource.Letterboxd -> "Letterboxd"
        ExternalTrackingSource.Tmdb -> "TMDb"
        ExternalTrackingSource.Rawg -> "RAWG"
        ExternalTrackingSource.Backloggd -> "Backloggd"
        ExternalTrackingSource.Other -> stringResource(R.string.external_tracking_other)
    }
}
