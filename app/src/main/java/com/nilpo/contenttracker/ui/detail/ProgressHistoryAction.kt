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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * The session's editable history: progress rows, and the status changes logged alongside them.
 *
 * Status events live here rather than getting a surface of their own because this is already where
 * the app teaches you to go to correct your history. They are delete-only — a logged transition
 * records the moment it was made, and nothing in the app can back-date one, so a wrong row can only
 * be removed, not corrected into being right.
 */
@Composable
fun ProgressHistoryAction(
    updates: List<ProgressUpdate>,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
    statusEvents: List<SessionStatusEvent> = emptyList(),
    onDeleteStatusEvent: (Long) -> Unit = {},
) {
    val visibleUpdates = updates.filter { it.countsTowardObjectives }
    // Pauses are worth opening the history for even when no progress was ever logged — otherwise an
    // accidental pause on an untouched title would have no way out at all.
    val visibleEvents = statusEvents.filter { it.status in EditableStatuses }
    if (visibleUpdates.isEmpty() && visibleEvents.isEmpty()) return

    var showHistory by rememberSaveable(visibleUpdates.size, visibleEvents.size, visibleUpdates.lastOrNull()?.id) {
        mutableStateOf(false)
    }

    TextButton(
        onClick = { showHistory = true },
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(
            text = stringResource(
                R.string.progress_history_open,
                visibleUpdates.size + visibleEvents.size,
            ),
        )
    }

    if (showHistory) {
        ProgressHistoryModal(
            updates = updates,
            progressTotal = progressTotal,
            mediaType = mediaType,
            accent = accent,
            onDeleteProgressUpdate = onDeleteProgressUpdate,
            onUpdateProgressUpdate = onUpdateProgressUpdate,
            statusEvents = visibleEvents,
            onDeleteStatusEvent = onDeleteStatusEvent,
            onDismiss = { showHistory = false },
        )
    }
}

/**
 * Only pauses and resumes are offered here. Starting, finishing and abandoning are already editable
 * through the session's own dates, and showing them twice would give two places to change one fact.
 */
private val EditableStatuses = setOf(TrackingStatus.Paused, TrackingStatus.InProgress)

@Composable
private fun ProgressHistoryModal(
    updates: List<ProgressUpdate>,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
    statusEvents: List<SessionStatusEvent>,
    onDeleteStatusEvent: (Long) -> Unit,
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
                    color = OmnilogTheme.colors.appInk,
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
                // One list, newest first: progress and status changes are the same history, and
                // splitting them into two sections would hide that a pause happened mid-run.
                historyEntries(
                    updates = sortedUpdates.filter { it.countsTowardObjectives },
                    statusEvents = statusEvents,
                ).forEach { entry ->
                    when (entry) {
                        is HistoryEntry.Progress -> ProgressHistoryRow(
                            update = entry.update,
                            delta = deltasById[entry.update.id] ?: entry.update.progressValue,
                            progressTotal = progressTotal,
                            mediaType = mediaType,
                            accent = accent,
                            onDelete = { onDeleteProgressUpdate(entry.update.id) },
                            onUpdate = { progressValue, loggedAt ->
                                onUpdateProgressUpdate(entry.update.id, progressValue, loggedAt)
                            },
                        )

                        is HistoryEntry.Status -> StatusHistoryRow(
                            event = entry.event,
                            onDelete = { onDeleteStatusEvent(entry.event.id) },
                        )
                    }
                }
            }
        }
    }
}

/** One line of the session's history, whichever table it came from. */
private sealed interface HistoryEntry {
    val sortKey: Long

    data class Progress(val update: ProgressUpdate) : HistoryEntry {
        override val sortKey = update.createdAtEpochMillis
    }

    data class Status(val event: SessionStatusEvent) : HistoryEntry {
        override val sortKey = event.createdAtEpochMillis
    }
}

/**
 * Interleaves the two histories, newest first.
 *
 * Ordered by the instant each row was written rather than by its date: a progress row can be
 * back-dated and a status change cannot, so their dates are not comparable, but the moment they were
 * recorded always is.
 */
private fun historyEntries(
    updates: List<ProgressUpdate>,
    statusEvents: List<SessionStatusEvent>,
): List<HistoryEntry> = (
    updates.map(HistoryEntry::Progress) + statusEvents.map(HistoryEntry::Status)
    ).sortedByDescending { it.sortKey }

/**
 * A logged status change. Delete only — the row records when it was made, and nothing can back-date
 * one, so there is no field here that could be corrected.
 */
@Composable
private fun StatusHistoryRow(
    event: SessionStatusEvent,
    onDelete: () -> Unit,
) {
    var pendingDelete by rememberSaveable(event.id) { mutableStateOf(false) }
    val accent = when (event.status) {
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        else -> OmnilogTheme.accents.InProgress
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
            Text(
                text = event.occurredOn.formatDate(),
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (event.status == TrackingStatus.Paused) {
                        R.string.progress_history_paused
                    } else {
                        R.string.progress_history_resumed
                    },
                ),
                color = accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { pendingDelete = true },
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 32.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = accent),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
            )
        }
    }

    if (pendingDelete) {
        OmnilogAlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = stringResource(R.string.delete_status_event_title),
            text = {
                // Says the pair will go, because deleting one half alone would leave the log
                // describing something that never happened.
                Text(text = stringResource(R.string.delete_status_event_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        pendingDelete = false
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressHistoryRow(
    update: ProgressUpdate,
    delta: Int,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onDelete: () -> Unit,
    onUpdate: (Int, LocalDate?) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var showEditor by rememberSaveable(update.id) { mutableStateOf(false) }
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
            Text(
                text = dateLabel,
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.progress_history_delta,
                    delta,
                    progressUnitLabel(mediaType = mediaType, value = abs(delta)),
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
            onClick = { showEditor = true },
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 32.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = accent),
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.edit),
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

    if (showEditor) {
        ProgressHistoryEditDialog(
            update = update,
            progressTotal = progressTotal,
            mediaType = mediaType,
            accent = accent,
            onSave = { progressValue, loggedAt ->
                onUpdate(progressValue, loggedAt)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
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
                            progressUnitLabel(mediaType = mediaType, value = abs(delta)),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressHistoryEditDialog(
    update: ProgressUpdate,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onSave: (Int, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftProgress by rememberSaveable(update.id) {
        mutableStateOf(update.progressValue.toString())
    }
    var draftEpochDay by rememberSaveable(update.id) {
        mutableStateOf(update.loggedAt.toEpochDay())
    }
    var hasKnownDate by rememberSaveable(update.id) {
        mutableStateOf(update.hasKnownDate)
    }
    var showDatePicker by rememberSaveable(update.id) { mutableStateOf(false) }
    val parsedProgress = draftProgress.toIntOrNull()
    val isValidProgress = parsedProgress != null &&
        parsedProgress >= 0 &&
        (progressTotal == null || progressTotal <= 0 || parsedProgress <= progressTotal)
    val date = LocalDate.ofEpochDay(draftEpochDay)
    val unit = progressUnitLabel(mediaType = mediaType, value = parsedProgress ?: 0)

    OmnilogAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.progress_history_edit_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draftProgress,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) draftProgress = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(R.string.progress_history_value_label, unit))
                    },
                    isError = draftProgress.isNotEmpty() && !isValidProgress,
                    supportingText = if (!isValidProgress) {
                        {
                            Text(
                                text = if (progressTotal != null && progressTotal > 0) {
                                    stringResource(R.string.progress_history_value_error_max, progressTotal)
                                } else {
                                    stringResource(R.string.progress_history_value_error)
                                },
                            )
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            text = if (hasKnownDate) {
                                date.formatDate()
                            } else {
                                stringResource(R.string.progress_history_date_unknown)
                            },
                        )
                    }
                    if (hasKnownDate) {
                        TextButton(onClick = { hasKnownDate = false }) {
                            Text(text = stringResource(R.string.progress_history_remove_date))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsedProgress?.let { progress ->
                        onSave(progress, date.takeIf { hasKnownDate })
                    }
                },
                enabled = isValidProgress,
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                .takeIf { hasKnownDate }
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
                            draftEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toEpochDay()
                            hasKnownDate = true
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
}

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
