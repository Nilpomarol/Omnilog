package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun AddSeasonEditor(
    existingSeasons: List<SeasonProgress>,
    onAddSeason: (Int, Int?) -> Unit,
) {
    val nextSeasonNumber = (existingSeasons.maxOfOrNull { it.seasonNumber } ?: 0) + 1
    var seasonNumberText by remember(existingSeasons.size) {
        mutableStateOf(nextSeasonNumber.toString())
    }
    var totalText by remember { mutableStateOf("") }

    val seasonNumber = seasonNumberText.toIntOrNull()
    val isDuplicate = seasonNumber != null && existingSeasons.any { it.seasonNumber == seasonNumber }
    val canSave = seasonNumber != null && seasonNumber > 0 && !isDuplicate

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = seasonNumberText,
                onValueChange = { value ->
                    seasonNumberText = value.filter { it.isDigit() }
                },
                label = { Text(stringResource(R.string.field_season_number)) },
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

        if (isDuplicate) {
            Text(text = stringResource(R.string.season_duplicate_error))
        }

        Button(
            enabled = canSave,
            onClick = {
                onAddSeason(seasonNumber ?: return@Button, totalText.toIntOrNull())
                seasonNumberText = ((existingSeasons.maxOfOrNull { it.seasonNumber } ?: 0) + 2).toString()
                totalText = ""
            },
        ) {
            Text(text = stringResource(R.string.add_season))
        }
    }
}
