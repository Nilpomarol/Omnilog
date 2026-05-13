package com.nilpo.contenttracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
    mediaType: MediaType,
    onSave: (AddTrackedMediaRequest) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var totalProgress by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var isOwned by remember { mutableStateOf(false) }

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = isOwned,
                    onCheckedChange = { isOwned = it },
                )
                Text(text = stringResource(R.string.field_owned))
            }

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
                                type = mediaType,
                                title = title,
                                progressTotal = totalProgress.toIntOrNull(),
                                isOwned = isOwned,
                                ownershipType = if (isOwned) OwnershipType.Physical else OwnershipType.None,
                                platformName = platform.takeIf { it.isNotBlank() },
                                platformType = ConsumptionPlatformType.Other,
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
