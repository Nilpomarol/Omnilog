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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession

@Composable
fun ItemDetailsSection(
    item: MediaItem,
    currentSession: TrackingSession?,
    onSaveItemDetails: (String, Int?, OwnershipType) -> Unit,
    onSavePlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
) {
    var isEditing by rememberSaveable(item.id, currentSession?.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_item_details),
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

        if (isEditing) {
            ItemDetailsEditor(
                item = item,
                currentSession = currentSession,
                onSaveItemDetails = onSaveItemDetails,
                onSavePlatform = onSavePlatform,
            )
        } else {
            ItemDetailsSummary(
                item = item,
                currentSession = currentSession,
            )
        }
    }
}

@Composable
private fun ItemDetailsSummary(
    item: MediaItem,
    currentSession: TrackingSession?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.field_ownership_type) + ": " + item.ownership.type.label(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
        val totalText = item.progressTotal?.let { total ->
            stringResource(R.string.progress_total_value, total)
        } ?: stringResource(R.string.progress_total_unknown)

        Text(
            text = totalText,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )

        val platformText = currentSession?.platform?.let { platform ->
            stringResource(R.string.platform_label, platform.name)
        } ?: stringResource(R.string.platform_none)

        Text(
            text = platformText,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OwnershipType.label(): String {
    return when (this) {
        OwnershipType.None -> stringResource(R.string.ownership_none)
        OwnershipType.Physical -> stringResource(R.string.ownership_physical)
        OwnershipType.Digital -> stringResource(R.string.ownership_digital)
        OwnershipType.Subscription -> stringResource(R.string.ownership_subscription)
        OwnershipType.Borrowed -> stringResource(R.string.ownership_borrowed)
    }
}
