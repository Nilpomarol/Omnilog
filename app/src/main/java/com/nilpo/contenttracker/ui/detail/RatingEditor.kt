package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.nilpo.contenttracker.core.model.TrackingSession

@Composable
fun RatingEditor(
    session: TrackingSession,
    onSave: (Int?) -> Unit,
) {
    var ratingText by remember { mutableStateOf(session.rating?.toString().orEmpty()) }

    LaunchedEffect(session.id, session.rating) {
        ratingText = session.rating?.toString().orEmpty()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = ratingText,
            onValueChange = { value ->
                ratingText = value.filter { it.isDigit() }.take(2)
            },
            label = { Text(stringResource(R.string.field_rating)) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Button(
            enabled = ratingText.isNotBlank(),
            onClick = {
                onSave(ratingText.toIntOrNull())
            },
        ) {
            Text(text = stringResource(R.string.update_rating))
        }
        TextButton(
            onClick = {
                ratingText = ""
                onSave(null)
            },
        ) {
            Text(text = stringResource(R.string.clear_rating))
        }
    }
}
