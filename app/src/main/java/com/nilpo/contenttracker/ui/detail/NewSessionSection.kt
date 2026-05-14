package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun NewSessionSection(
    mediaItemId: Long,
    currentSession: TrackingSession?,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
) {
    var isAdding by rememberSaveable(mediaItemId) { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf(TrackingStatus.Planned) }
    var progressText by rememberSaveable { mutableStateOf("0") }
    var totalText by rememberSaveable(currentSession?.id) {
        mutableStateOf(currentSession?.progressTotal?.toString().orEmpty())
    }
    var platformName by rememberSaveable(currentSession?.id) {
        mutableStateOf(currentSession?.platform?.name.orEmpty())
    }
    var selectedPlatformType by rememberSaveable(currentSession?.id) {
        mutableStateOf(currentSession?.platform?.type ?: ConsumptionPlatformType.Other)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isAdding) {
            Button(onClick = { isAdding = true }) {
                Text(text = stringResource(R.string.start_new_session))
            }
        } else {
            DetailSectionTitle(text = stringResource(R.string.new_session_title))

            StatusSelector(
                selectedStatus = selectedStatus,
                onStatusSelected = { selectedStatus = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = progressText,
                    onValueChange = { value ->
                        progressText = value.filter { it.isDigit() }
                    },
                    label = { Text(stringResource(R.string.field_progress)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { value ->
                        totalText = value.filter { it.isDigit() }
                    },
                    label = { Text(stringResource(R.string.field_total_progress)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { isAdding = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        onStartNewSession(
                            AddTrackingSessionRequest(
                                mediaItemId = mediaItemId,
                                status = selectedStatus,
                                progressCurrent = progressText.toIntOrNull() ?: 0,
                                progressTotal = totalText.toIntOrNull(),
                                platformName = platformName,
                                platformType = selectedPlatformType,
                            ),
                        )
                        isAdding = false
                        selectedStatus = TrackingStatus.Planned
                        progressText = "0"
                    },
                ) {
                    Text(text = stringResource(R.string.create_session))
                }
            }
        }
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
