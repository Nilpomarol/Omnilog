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
import com.nilpo.contenttracker.core.model.TrackingSession

@Composable
fun NotesEditor(
    session: TrackingSession,
    onSave: (String?) -> Unit,
) {
    var notesText by remember { mutableStateOf(session.notes.orEmpty()) }

    LaunchedEffect(session.id, session.notes) {
        notesText = session.notes.orEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text(stringResource(R.string.field_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Button(
            onClick = { onSave(notesText) },
        ) {
            Text(text = stringResource(R.string.update_notes))
        }
    }
}
