package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia

@Composable
fun CollectionDetailScreen(
    collection: MediaCollection,
    items: List<TrackedMedia>,
    accent: Color,
    onBack: () -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by rememberSaveable(collection.id) { mutableStateOf(false) }
    var nameText by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var showDeleteConfirmation by rememberSaveable(collection.id) { mutableStateOf(false) }

    LaunchedEffect(collection.id, collection.name) {
        nameText = collection.name
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                    TextButton(onClick = { isEditing = !isEditing }) {
                        val label = if (isEditing) {
                            stringResource(R.string.done_editing)
                        } else {
                            stringResource(R.string.edit)
                        }
                        Text(text = label)
                    }
                }
            }

            item {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            if (isEditing) {
                item {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text(stringResource(R.string.field_collection)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = nameText.isNotBlank(),
                            onClick = {
                                onRenameCollection(collection.id, nameText)
                                isEditing = false
                            },
                        ) {
                            Text(text = stringResource(R.string.save))
                        }
                        TextButton(onClick = { showDeleteConfirmation = true }) {
                            Text(text = stringResource(R.string.delete))
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.collection_item_count, items.size),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            items(items.sortedBy { it.item.title }) { trackedMedia ->
                MediaCard(
                    trackedMedia = trackedMedia,
                    accent = accent,
                    onClick = { onMediaClick(trackedMedia) },
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.delete_collection_title)) },
            text = { Text(text = stringResource(R.string.delete_collection_message, collection.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCollection(collection.id)
                        showDeleteConfirmation = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
