package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun ItemDetailsEditor(
    item: MediaItem,
    currentSession: TrackingSession?,
    onSaveItemDetails: (String, OwnershipType) -> Unit,
    onSavePlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
) {
    var title by remember { mutableStateOf(item.title) }
    var selectedOwnershipType by remember { mutableStateOf(item.ownership.type) }
    var platformName by remember { mutableStateOf(currentSession?.platform?.name.orEmpty()) }
    var selectedPlatformType by remember {
        mutableStateOf(currentSession?.platform?.type ?: ConsumptionPlatformType.Other)
    }

    LaunchedEffect(item.id, item.title, item.ownership.type) {
        title = item.title
        selectedOwnershipType = item.ownership.type
    }

    LaunchedEffect(currentSession?.id, currentSession?.platform) {
        platformName = currentSession?.platform?.name.orEmpty()
        selectedPlatformType = currentSession?.platform?.type ?: ConsumptionPlatformType.Other
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OptionSelector(
            label = stringResource(R.string.field_ownership_type),
            options = OwnershipType.entries,
            selectedOption = selectedOwnershipType,
            optionLabel = { ownershipType -> ownershipType.label() },
            onOptionSelected = { ownershipType -> selectedOwnershipType = ownershipType },
        )

        Button(
            enabled = title.isNotBlank(),
            onClick = {
                onSaveItemDetails(title, selectedOwnershipType)
            },
        ) {
            Text(text = stringResource(R.string.update_item_details))
        }

        currentSession?.let { session ->
            OutlinedTextField(
                value = platformName,
                onValueChange = { platformName = it },
                label = { Text(stringResource(R.string.field_platform)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OptionSelector(
                label = stringResource(R.string.field_platform_type),
                options = ConsumptionPlatformType.entries,
                selectedOption = selectedPlatformType,
                optionLabel = { platformType -> platformType.label() },
                onOptionSelected = { platformType -> selectedPlatformType = platformType },
            )

            Button(
                onClick = {
                    onSavePlatform(session.id, platformName, selectedPlatformType)
                },
            ) {
                Text(text = stringResource(R.string.update_platform))
            }
        }
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

@Composable
private fun ConsumptionPlatformType.label(): String {
    return when (this) {
        ConsumptionPlatformType.Physical -> stringResource(R.string.platform_type_physical)
        ConsumptionPlatformType.DigitalStore -> stringResource(R.string.platform_type_digital_store)
        ConsumptionPlatformType.Streaming -> stringResource(R.string.platform_type_streaming)
        ConsumptionPlatformType.Ebook -> stringResource(R.string.platform_type_ebook)
        ConsumptionPlatformType.Library -> stringResource(R.string.platform_type_library)
        ConsumptionPlatformType.Other -> stringResource(R.string.platform_type_other)
    }
}
