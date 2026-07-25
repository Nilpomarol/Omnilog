package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.activity.ActivityWindow
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogModal
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Edits one entry: how much, when, and whether it accumulated over a span.
 *
 * Built from the same parts as the rest of the app's forms — a panel per field, its label above its
 * value, the number in an accent-tinted chip, and one full-width action in the session's colour.
 * It used to be a stock Material dialog with an outlined text field, which is what made it read as
 * a form bolted on from somewhere else.
 *
 * The amount is an increment — what was consumed on this occasion — so the field asks for that
 * rather than for a running total. Zero is not offered: an entry worth nothing is a deletion, which
 * is why delete lives here, under the save button, rather than as a second target on the row.
 *
 * The period switch is the only place `coversPeriod` is ever set. Logging never asks, because the
 * answer is the same almost every time and the common path should cost nothing. Turning it on needs
 * no second date either — the span is derived from the entry before this one, and the subtitle names
 * the span it will claim before you commit to it.
 */
@Composable
fun ActivityEntryEditor(
    update: ProgressUpdate,
    window: ActivityWindow?,
    headroom: Int?,
    mediaType: MediaType,
    accent: Color,
    onSave: (Int, LocalDate?, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftAmount by rememberSaveable(update.id) { mutableStateOf(update.amount.toString()) }
    var draftEpochDay by rememberSaveable(update.id) { mutableStateOf(update.loggedAt.toEpochDay()) }
    var hasKnownDate by rememberSaveable(update.id) { mutableStateOf(update.hasKnownDate) }
    var coversPeriod by rememberSaveable(update.id) { mutableStateOf(update.coversPeriod) }
    var showDatePicker by rememberSaveable(update.id) { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable(update.id) { mutableStateOf(false) }

    val parsedAmount = draftAmount.toIntOrNull()
    val isValid = parsedAmount != null &&
        parsedAmount > 0 &&
        (headroom == null || parsedAmount <= headroom)
    val date = LocalDate.ofEpochDay(draftEpochDay)

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
                    text = stringResource(R.string.activity_edit_title),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }

            AmountField(
                value = draftAmount,
                onValueChange = { input -> if (input.all(Char::isDigit)) draftAmount = input },
                headroom = headroom,
                mediaType = mediaType,
                accent = accent,
                isValid = isValid || draftAmount.isEmpty(),
            )

            DateField(
                date = date,
                hasKnownDate = hasKnownDate,
                accent = accent,
                onPick = { showDatePicker = true },
                onClear = { hasKnownDate = false },
            )

            // Offered only on a dated entry: with no end date there is nothing to measure a span
            // back from, so the switch would have nothing to act on.
            if (hasKnownDate) {
                CoversPeriodField(
                    checked = coversPeriod,
                    window = window,
                    accent = accent,
                    onCheckedChange = { coversPeriod = it },
                )
            }

            Button(
                onClick = {
                    parsedAmount?.let { amount ->
                        onSave(amount, date.takeIf { hasKnownDate }, coversPeriod && hasKnownDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Under the save button and in the error colour: reachable, but never the thing the
            // thumb lands on by default.
            TextButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.activity_delete_entry),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    if (showDatePicker) {
        EntryDatePicker(
            initialDate = date.takeIf { hasKnownDate },
            onPicked = { picked ->
                draftEpochDay = picked.toEpochDay()
                hasKnownDate = true
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (confirmingDelete) {
        OmnilogAlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = stringResource(R.string.activity_delete_entry_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.activity_delete_entry_message,
                        update.amount,
                        progressUnitLabel(mediaType = mediaType, value = update.amount),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Fields
// ─────────────────────────────────────────────────────────────

/** The panel every field in this form sits in, matching the tracking controls elsewhere. */
@Composable
private fun FieldPanel(
    accent: Color,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) {
            accent.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) accent.copy(alpha = 0.30f) else OmnilogTheme.colors.appLine,
        ),
        content = content,
    )
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    headroom: Int?,
    mediaType: MediaType,
    accent: Color,
    isValid: Boolean,
) {
    val parsed = value.toIntOrNull() ?: 0
    FieldPanel(accent = accent) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.activity_amount_field_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
                Text(
                    text = if (isValid) {
                        progressUnitLabel(mediaType = mediaType, value = parsed.coerceAtLeast(1))
                    } else if (headroom != null) {
                        stringResource(R.string.activity_amount_error_max, headroom)
                    } else {
                        stringResource(R.string.activity_amount_error)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValid) {
                        OmnilogTheme.colors.appInk
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.08f),
                border = BorderStroke(
                    1.dp,
                    if (isValid) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.error,
                ),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = accent,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                )
            }
        }
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    hasKnownDate: Boolean,
    accent: Color,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    FieldPanel(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onPick,
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.activity_date_field_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    Text(
                        text = if (hasKnownDate) {
                            date.formatActivityDate()
                        } else {
                            stringResource(R.string.activity_date_unknown)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (hasKnownDate) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (hasKnownDate) accent else OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (hasKnownDate) {
                TextButton(onClick = onClear, modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                        text = stringResource(R.string.activity_remove_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoversPeriodField(
    checked: Boolean,
    window: ActivityWindow?,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    FieldPanel(accent = accent, highlighted = checked) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.activity_covers_period),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = window?.let {
                        stringResource(R.string.activity_covers_period_window, it.from.formatActivityDate())
                    } ?: stringResource(R.string.activity_covers_period_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accent,
                    checkedBorderColor = accent,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePicker(
    initialDate: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPicked(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate(),
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * Edits a logged status change: the day it happened, or nothing at all.
 *
 * A transition used to be uneditable on the grounds that it records the moment it was made. That
 * held only while nothing offered to change it — pause a book on Friday, remember on Sunday, and the
 * log insisted on Sunday. The day is the user's to state; the recorded instant still fixes the order.
 *
 * Deleting removes this transition alone. The session falls back to what it was before, so removing
 * a resume leaves it paused, which is exactly where it stood.
 */
@Composable
fun StatusEventEditor(
    event: SessionStatusEvent,
    accent: Color,
    minimumDate: LocalDate? = null,
    maximumDate: LocalDate? = null,
    onSaveDate: (LocalDate) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftEpochDay by rememberSaveable(event.id) { mutableStateOf(event.occurredOn.toEpochDay()) }
    var showDatePicker by rememberSaveable(event.id) { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable(event.id) { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(draftEpochDay)
    val dateIsOrdered = minimumDate?.isAfter(date) != true && maximumDate?.isBefore(date) != true

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
                    text = stringResource(event.statusLabelRes()),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }

            FieldPanel(accent = accent) {
                Surface(onClick = { showDatePicker = true }, color = Color.Transparent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.activity_date_field_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                        Text(
                            text = date.formatActivityDate(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Button(
                onClick = { onSaveDate(date) },
                modifier = Modifier.fillMaxWidth(),
                enabled = draftEpochDay != event.occurredOn.toEpochDay() && dateIsOrdered,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.activity_delete_status),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    if (showDatePicker) {
        EntryDatePicker(
            initialDate = date,
            onPicked = { picked ->
                draftEpochDay = picked.toEpochDay()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (confirmingDelete) {
        OmnilogAlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = stringResource(R.string.activity_delete_status_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.activity_delete_status_message,
                        stringResource(event.statusLabelRes()),
                        event.occurredOn.formatActivityDate(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
