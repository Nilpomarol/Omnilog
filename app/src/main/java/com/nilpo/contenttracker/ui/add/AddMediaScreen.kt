package com.nilpo.contenttracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.OwnershipType

@Composable
fun AddMediaScreen(
    initialMediaType: MediaType,
    availableMediaTypes: List<MediaType>,
    onSave: (AddTrackedMediaRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var totalProgress by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf(initialMediaType) }
    var selectedOwnershipType by remember { mutableStateOf(OwnershipType.None) }
    var selectedPlatformType by remember { mutableStateOf(ConsumptionPlatformType.Other) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.add_media_title),
                style = MaterialTheme.typography.headlineLarge,
            )

            if (availableMediaTypes.size > 1) {
                OptionSelector(
                    label = stringResource(R.string.field_media_type),
                    options = availableMediaTypes,
                    selectedOption = selectedMediaType,
                    optionLabel = { it.label() },
                    onOptionSelected = { selectedMediaType = it },
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = totalProgress,
                onValueChange = { value -> totalProgress = value.filter { it.isDigit() } },
                label = { Text(stringResource(R.string.field_total_progress)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text(stringResource(R.string.field_platform)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OptionSelector(
                label = stringResource(R.string.field_ownership_type),
                options = OwnershipType.entries,
                selectedOption = selectedOwnershipType,
                optionLabel = { it.label() },
                onOptionSelected = { selectedOwnershipType = it },
            )

            OptionSelector(
                label = stringResource(R.string.field_platform_type),
                options = ConsumptionPlatformType.entries,
                selectedOption = selectedPlatformType,
                optionLabel = { it.label() },
                onOptionSelected = { selectedPlatformType = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            AddTrackedMediaRequest(
                                type = selectedMediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                isOwned = selectedOwnershipType != OwnershipType.None,
                                ownershipType = selectedOwnershipType,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = selectedPlatformType,
                            ),
                        )
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun <T> OptionSelector(
    label: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                if (option == selectedOption) {
                    Button(onClick = { onOptionSelected(option) }) {
                        Text(text = optionLabel(option))
                    }
                } else {
                    TextButton(onClick = { onOptionSelected(option) }) {
                        Text(text = optionLabel(option))
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaType.label(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.media_type_anime)
        MediaType.Book -> stringResource(R.string.media_type_book)
        MediaType.Movie -> stringResource(R.string.media_type_movie)
        MediaType.TvShow -> stringResource(R.string.media_type_tv_show)
        MediaType.Game -> stringResource(R.string.media_type_game)
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
