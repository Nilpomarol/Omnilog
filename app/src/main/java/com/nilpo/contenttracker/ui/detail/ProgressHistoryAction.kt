package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ProgressHistoryAction(
    updates: List<ProgressUpdate>,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
) {
    if (updates.isEmpty()) return

    var showHistory by rememberSaveable(updates.size, updates.lastOrNull()?.id) {
        mutableStateOf(false)
    }

    TextButton(
        onClick = { showHistory = true },
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(text = stringResource(R.string.progress_history_open, updates.size))
    }

    if (showHistory) {
        ProgressHistoryModal(
            updates = updates,
            mediaType = mediaType,
            accent = accent,
            onDeleteProgressUpdate = onDeleteProgressUpdate,
            onDismiss = { showHistory = false },
        )
    }
}

@Composable
private fun ProgressHistoryModal(
    updates: List<ProgressUpdate>,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sortedUpdates = updates.sortedWith(compareBy<ProgressUpdate> { it.loggedAt }.thenBy { it.createdAtEpochMillis })
    val deltasById = sortedUpdates.mapIndexed { index, update ->
        val previousProgress = sortedUpdates.getOrNull(index - 1)?.progressValue ?: 0
        update.id to (update.progressValue - previousProgress)
    }.toMap()

    OmnilogModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.progress_history_title),
                    color = OmnilogColors.AppInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sortedUpdates.asReversed().forEach { update ->
                    val delta = deltasById[update.id] ?: update.progressValue
                    ProgressHistoryRow(
                        update = update,
                        delta = delta,
                        mediaType = mediaType,
                        accent = accent,
                        onDelete = { onDeleteProgressUpdate(update.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressHistoryRow(
    update: ProgressUpdate,
    delta: Int,
    mediaType: MediaType,
    accent: Color,
    onDelete: () -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = update.loggedAt.formatDate(),
                color = OmnilogColors.AppMuted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.progress_history_delta,
                    delta,
                    progressHistoryUnitLabel(mediaType = mediaType, value = abs(delta)),
                    update.progressValue,
                ),
                color = accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { pendingDelete = update.id },
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
            )
        }
    }

    pendingDelete?.let {
        OmnilogAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.delete_progress_update_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_progress_update_message,
                        update.loggedAt.formatDate(),
                        stringResource(
                            R.string.progress_history_delta,
                            delta,
                            progressHistoryUnitLabel(mediaType = mediaType, value = abs(delta)),
                            update.progressValue,
                        ),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        pendingDelete = null
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun progressHistoryUnitLabel(mediaType: MediaType, value: Int): String =
    when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
            -> if (value == 1) {
            stringResource(R.string.progress_unit_episode_one)
        } else {
            stringResource(R.string.progress_unit_episode_many)
        }
        MediaType.Book -> if (value == 1) {
            stringResource(R.string.progress_unit_page_one)
        } else {
            stringResource(R.string.progress_unit_page_many)
        }
        MediaType.Movie -> if (value == 1) {
            stringResource(R.string.progress_unit_minute_one)
        } else {
            stringResource(R.string.progress_unit_minute_many)
        }
        MediaType.Game -> if (value == 1) {
            stringResource(R.string.progress_unit_hour_one)
        } else {
            stringResource(R.string.progress_unit_hour_many)
        }
    }

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
