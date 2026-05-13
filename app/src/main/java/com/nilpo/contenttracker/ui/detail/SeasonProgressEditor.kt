package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.SeasonProgress

@Composable
fun SeasonProgressEditor(
    season: SeasonProgress,
    onSave: (Int) -> Unit,
) {
    var progressText by remember { mutableStateOf(season.progressCurrent.toString()) }

    LaunchedEffect(season.id, season.progressCurrent) {
        progressText = season.progressCurrent.toString()
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
            label = { Text(stringResource(R.string.field_season_progress, season.seasonNumber)) },
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
            Text(text = stringResource(R.string.update_season_progress))
        }
    }
}
