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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ProgressHistoryAction(
    updates: List<ProgressUpdate>,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdateDate: (Long, LocalDate?) -> Unit,
) {
    val visibleUpdates = updates.filter { it.countsTowardObjectives }
    if (visibleUpdates.isEmpty()) return

    var showHistory by rememberSaveable(visibleUpdates.size, visibleUpdates.lastOrNull()?.id) {
        mutableStateOf(false)
    }

    TextButton(
        onClick = { showHistory = true },
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(text = stringResource(R.string.progress_history_open, visibleUpdates.size))
    }

    if (showHistory) {
        ProgressHistoryModal(
            updates = updates,
            mediaType = mediaType,
            accent = accent,
            onDeleteProgressUpdate = onDeleteProgressUpdate,
            onUpdateProgressUpdateDate = onUpdateProgressUpdateDate,
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
    onUpdateProgressUpdateDate: (Long, LocalDate?) -> Unit,
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
                sortedUpdates.filter { it.countsTowardObjectives }.asReversed().forEach { update ->
                    val delta = deltasById[update.id] ?: update.progressValue
                    ProgressHistoryRow(
                        update = update,
                        delta = delta,
                        mediaType = mediaType,
                        accent = accent,
                        onDelete = { onDeleteProgressUpdate(update.id) },
                        onUpdateDate = { loggedAt -> onUpdateProgressUpdateDate(update.id, loggedAt) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressHistoryRow(
    update: ProgressUpdate,
    delta: Int,
    mediaType: MediaType,
    accent: Color,
    onDelete: () -> Unit,
    onUpdateDate: (LocalDate?) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable(update.id) { mutableStateOf(false) }
    val dateLabel = if (update.hasKnownDate) {
        update.loggedAt.formatDate()
    } else {
        stringResource(R.string.progress_history_date_unknown)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showDatePicker = true },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = dateLabel,
                        color = OmnilogColors.AppMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (update.hasKnownDate) {
                    TextButton(
                        onClick = { onUpdateDate(null) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.progress_history_remove_date),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = update.loggedAt
                .takeIf { update.hasKnownDate }
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onUpdateDate(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate(),
                            )
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
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
                        dateLabel,
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
