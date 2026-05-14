package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun ItemDetailsEditor(
    item: MediaItem,
    collection: MediaCollection?,
    availableCollections: List<MediaCollection>,
    currentSession: TrackingSession?,
    onSaveItemDetails: (String, Long?, String?, Int?, OwnershipType) -> Unit,
    onSavePlatform: (Long, String?, ConsumptionPlatformType) -> Unit,
) {
    var title by remember { mutableStateOf(item.title) }
    var totalText by remember { mutableStateOf(item.progressTotal?.toString().orEmpty()) }
    var selectedCollectionId by remember { mutableStateOf(item.collectionId) }
    var newCollectionName by remember { mutableStateOf("") }
    var selectedOwnershipType by remember { mutableStateOf(item.ownership.type) }
    var platformName by remember { mutableStateOf(currentSession?.platform?.name.orEmpty()) }
    var selectedPlatformType by remember {
        mutableStateOf(currentSession?.platform?.type ?: ConsumptionPlatformType.Other)
    }

    LaunchedEffect(item.id, item.title, item.progressTotal, item.ownership.type) {
        title = item.title
        totalText = item.progressTotal?.toString().orEmpty()
        selectedCollectionId = item.collectionId
        newCollectionName = ""
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

        OutlinedTextField(
            value = totalText,
            onValueChange = { value -> totalText = value.filter { it.isDigit() } },
            label = { Text(stringResource(R.string.field_total_progress)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )

        CollectionSelector(
            collection = collection,
            availableCollections = availableCollections,
            selectedCollectionId = selectedCollectionId,
            newCollectionName = newCollectionName,
            onCollectionSelected = {
                selectedCollectionId = it?.id
                newCollectionName = ""
            },
            onNewCollectionNameChange = {
                newCollectionName = it
                selectedCollectionId = null
            },
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
                onSaveItemDetails(
                    title,
                    selectedCollectionId,
                    newCollectionName,
                    totalText.toIntOrNull(),
                    selectedOwnershipType,
                )
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
private fun CollectionSelector(
    collection: MediaCollection?,
    availableCollections: List<MediaCollection>,
    selectedCollectionId: Long?,
    newCollectionName: String,
    onCollectionSelected: (MediaCollection?) -> Unit,
    onNewCollectionNameChange: (String) -> Unit,
) {
    val selectedCollection = availableCollections.firstOrNull { it.id == selectedCollectionId }
    Text(
        text = stringResource(
            R.string.collection_summary,
            selectedCollection?.name ?: collection?.name ?: stringResource(R.string.collection_none),
        ),
    )

    if (availableCollections.isNotEmpty()) {
        OptionSelector(
            label = stringResource(R.string.field_collection),
            options = listOf<MediaCollection?>(null) + availableCollections,
            selectedOption = selectedCollection,
            optionLabel = { option ->
                option?.name ?: stringResource(R.string.collection_none)
            },
            onOptionSelected = onCollectionSelected,
        )
    }

    OutlinedTextField(
        value = newCollectionName,
        onValueChange = onNewCollectionNameChange,
        label = { Text(stringResource(R.string.field_new_collection)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    if (selectedCollectionId != null || collection != null || newCollectionName.isNotBlank()) {
        TextButton(
            onClick = {
                onCollectionSelected(null)
                onNewCollectionNameChange("")
            },
        ) {
            Text(text = stringResource(R.string.clear_collection))
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
