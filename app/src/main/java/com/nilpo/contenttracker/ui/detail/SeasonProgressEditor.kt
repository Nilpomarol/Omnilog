package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.nilpo.contenttracker.core.model.SeasonProgress

@Composable
fun SeasonProgressEditor(
    season: SeasonProgress,
    onSave: (Int) -> Unit,
    onSaveTotal: (Int?) -> Unit,
    onDelete: () -> Unit,
) {
    var progressText by remember { mutableStateOf(season.progressCurrent.toString()) }
    var totalText by remember { mutableStateOf(season.progressTotal?.toString().orEmpty()) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(season.id, season.progressCurrent) {
        progressText = season.progressCurrent.toString()
    }

    LaunchedEffect(season.id, season.progressTotal) {
        totalText = season.progressTotal?.toString().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.season_title, season.seasonNumber),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { isConfirmingDelete = true }) {
                Text(text = stringResource(R.string.delete_season))
            }
        }

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
            Button(
                enabled = progressText.isNotBlank(),
                onClick = {
                    onSave(progressText.toIntOrNull() ?: 0)
                },
            ) {
                Text(text = stringResource(R.string.update_progress))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            Button(
                onClick = {
                    onSaveTotal(totalText.toIntOrNull())
                },
            ) {
                Text(text = stringResource(R.string.update_total_progress))
            }
        }
    }

    if (isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            title = { Text(text = stringResource(R.string.delete_season_title)) },
            text = { Text(text = stringResource(R.string.delete_season_message, season.seasonNumber)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmingDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingDelete = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
