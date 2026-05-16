package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.MediaMetadataSecondary
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi

@Composable
fun ItemDetailsSection(
    item: MediaItem,
    credits: List<MediaCredit>,
    collection: MediaCollection?,
    availableCollections: List<MediaCollection>,
    currentSession: TrackingSession?,
    isEditing: Boolean,
    onSaveItemDetails: (String, Long?, String?, Int?, OwnershipType) -> Unit,
    onSavePlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_item_details),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f))

        if (isEditing) {
            ItemDetailsEditor(
                item = item,
                collection = collection,
                availableCollections = availableCollections,
                currentSession = currentSession,
                onSaveItemDetails = onSaveItemDetails,
                onSavePlatform = onSavePlatform,
            )
        } else {
            ItemDetailsSummary(
                item = item,
                credits = credits,
                collection = collection,
                currentSession = currentSession,
            )
        }
    }
}

@Composable
private fun ItemDetailsSummary(
    item: MediaItem,
    credits: List<MediaCredit>,
    collection: MediaCollection?,
    currentSession: TrackingSession?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MediaMetadataSecondary(metadata = item.toMediaMetadataUi(credits))

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

        Text(
            text = stringResource(
                R.string.collection_summary,
                collection?.name ?: stringResource(R.string.collection_none),
            ),
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
