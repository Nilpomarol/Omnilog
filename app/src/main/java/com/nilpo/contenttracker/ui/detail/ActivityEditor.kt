package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.activity.SessionActivity
import com.nilpo.contenttracker.core.activity.SessionActivityKind
import com.nilpo.contenttracker.core.activity.activityWindows
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.ui.common.CompletionDateRow
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.QuickProgressRail
import com.nilpo.contenttracker.ui.common.contentColorOn
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

/** Which part of a row a delete confirmation is about. */
private enum class PendingDelete { Transition, Entry }

/**
 * Edits one Activitat row inside the Activitat sheet: whatever it holds, in one place.
 *
 * Built from the quick session sheet's own parts — the big editable figure over its rail, the date
 * row, one full-width action — so correcting history feels like logging it. It replaced a centred
 * dialog of label–value rows, which read as a settings form for something that is a sitting.
 *
 * A row can own a status transition, a progress entry, or both — a finish that absorbed the day's
 * last pages, a start that absorbed its first. Each part keeps its own section, its own delete, and
 * its own write; Save sends only the parts that changed.
 *
 * The entry is edited as the position it reached, the way the quick sheet sets progress: the rail
 * and the figure are the running total, and the increment the entry stores is what separates it from
 * the total before it, named underneath. A position at or below that earlier total would be an entry
 * worth nothing, which is a deletion instead.
 *
 * The transition: its day, not knowing it, or removing it. Only the newest transition decides where
 * the session stands, so [revertsSession] makes the delete confirmation say whether it changes.
 * [minimumDate] and [maximumDate] are the days the repository accepts; the picker rules out days
 * before the first, and Save stays off past the second with the range named.
 */
@Composable
fun ActivityRowEditor(
    row: SessionActivity,
    allEntries: List<ProgressUpdate>,
    sessionStartedAt: LocalDate?,
    mediaType: MediaType,
    progressTotal: Int?,
    accent: Color,
    revertsSession: Boolean,
    minimumDate: LocalDate?,
    maximumDate: LocalDate?,
    onUpdateStatusEventDate: (Long, LocalDate?) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = row.statusEvent
    val entry = row.progress
    val key = row.key
    val rowAccent = row.kind.accent(accent)

    // The total before this entry, in the dated sequence the running totals follow.
    val totalBefore = entry?.let { (row.runningTotal ?: it.amount) - it.amount } ?: 0

    var statusEpochDay by rememberSaveable(key) { mutableStateOf(event?.occurredOn?.toEpochDay() ?: 0L) }
    var statusKnown by rememberSaveable(key) { mutableStateOf(event?.hasKnownDate ?: false) }
    var reachedText by rememberSaveable(key) { mutableStateOf(entry?.let { (totalBefore + it.amount).toString() }.orEmpty()) }
    var entryEpochDay by rememberSaveable(key) { mutableStateOf(entry?.loggedAt?.toEpochDay() ?: 0L) }
    var entryKnown by rememberSaveable(key) { mutableStateOf(entry?.hasKnownDate ?: false) }
    var coversPeriod by rememberSaveable(key) { mutableStateOf(entry?.coversPeriod ?: false) }
    var pendingDelete by rememberSaveable(key) { mutableStateOf<PendingDelete?>(null) }

    val statusDate = LocalDate.ofEpochDay(statusEpochDay)
    val statusInRange = !statusKnown ||
        (minimumDate?.isAfter(statusDate) != true && maximumDate?.isBefore(statusDate) != true)
    val statusChanged = event != null &&
        (statusKnown != event.hasKnownDate || (statusKnown && statusEpochDay != event.occurredOn.toEpochDay()))

    val amount = reachedText.toIntOrNull()?.minus(totalBefore)?.takeIf { it > 0 }
    val entryDate = LocalDate.ofEpochDay(entryEpochDay)
    val entryChanged = entry != null && (
        amount != entry.amount || entryKnown != entry.hasKnownDate ||
            (entryKnown && entryEpochDay != entry.loggedAt.toEpochDay()) ||
            (coversPeriod && entryKnown) != entry.coversPeriod
        )
    // The span a period would claim, recomputed for the draft day so the switch names what it will do.
    val draftWindow = entry?.takeIf { entryKnown }?.let { current ->
        activityWindows(
            updates = allEntries.map { if (it.id == current.id) it.copy(loggedAt = entryDate, coversPeriod = true) else it },
            sessionStartedAt = sessionStartedAt,
        )[current.id]
    }
    val canSave = (statusChanged || entryChanged) && statusInRange && (entry == null || amount != null)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.activity_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (row.kind != SessionActivityKind.Progress) {
                        Box(modifier = Modifier.size(10.dp).background(rowAccent, CircleShape))
                    }
                    Text(
                        text = stringResource(
                            if (row.kind == SessionActivityKind.Progress) R.string.activity_edit_title else row.kind.labelRes(),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                    )
                }
            }
        }

        if (event != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (entry != null) EditorSectionLabel(stringResource(R.string.activity_section_transition))
                CompletionDateRow(
                    date = statusDate,
                    earliestDate = minimumDate,
                    label = stringResource(R.string.activity_date_field_label),
                    accent = rowAccent,
                    onDateChange = {
                        statusEpochDay = it.toEpochDay()
                        statusKnown = true
                    },
                    known = statusKnown,
                    onClear = { statusKnown = false },
                )
                AnimatedVisibility(visible = !statusInRange, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Text(
                        text = rangeHint(minimumDate, maximumDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (entry != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (event != null || row.kind != SessionActivityKind.Progress) {
                    EditorSectionLabel(stringResource(R.string.activity_folded_entry_label))
                }
                QuickProgressRail(
                    text = reachedText,
                    total = progressTotal,
                    mediaType = mediaType,
                    accent = accent,
                    onTextChange = { reachedText = it },
                )
                val incrementColor by animateColorAsState(
                    targetValue = if (amount != null) accent else MaterialTheme.colorScheme.error,
                    label = "entryIncrement",
                )
                Text(
                    text = if (amount != null) {
                        stringResource(
                            R.string.activity_entry_increment,
                            stringResource(R.string.activity_amount, amount, progressUnitLabel(mediaType = mediaType, value = amount)),
                        )
                    } else {
                        stringResource(R.string.activity_entry_below_previous, totalBefore)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = incrementColor,
                )
                CompletionDateRow(
                    date = entryDate,
                    earliestDate = null,
                    label = stringResource(R.string.activity_date_field_label),
                    accent = accent,
                    onDateChange = {
                        entryEpochDay = it.toEpochDay()
                        entryKnown = true
                    },
                    known = entryKnown,
                    onClear = { entryKnown = false },
                )
                // Only a dated entry has an end to measure a span back from.
                AnimatedVisibility(visible = entryKnown, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    PeriodToggle(
                        checked = coversPeriod,
                        supporting = draftWindow?.let {
                            stringResource(R.string.activity_covers_period_window, it.from.formatActivityDate())
                        } ?: stringResource(R.string.activity_covers_period_hint),
                        accent = accent,
                        onCheckedChange = { coversPeriod = it },
                    )
                }
            }
        }

        Column {
            Button(
                onClick = {
                    if (event != null && statusChanged) {
                        onUpdateStatusEventDate(event.id, statusDate.takeIf { statusKnown })
                    }
                    if (entry != null && entryChanged && amount != null) {
                        onUpdateProgressUpdate(entry.id, amount, entryDate.takeIf { entryKnown }, coversPeriod && entryKnown)
                    }
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = contentColorOn(accent)),
            ) {
                Text(text = stringResource(R.string.save), fontWeight = FontWeight.ExtraBold)
            }
            // Under Save and in the error colour: reachable, never where the thumb lands by default.
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (event != null) {
                    TextButton(onClick = { pendingDelete = PendingDelete.Transition }, modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.activity_delete_status),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (entry != null) {
                    TextButton(onClick = { pendingDelete = PendingDelete.Entry }, modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.activity_delete_entry),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        val label = stringResource(row.kind.labelRes())
        val eventDay = if (event?.hasKnownDate == true) {
            event.occurredOn.formatActivityDate()
        } else {
            stringResource(R.string.activity_date_unknown)
        }
        val foldedAmount = entry?.let {
            stringResource(R.string.activity_amount, it.amount, progressUnitLabel(mediaType = mediaType, value = it.amount))
        }
        OmnilogAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(
                if (target == PendingDelete.Transition) R.string.activity_delete_status_title else R.string.activity_delete_entry_title,
            ),
            text = {
                Text(
                    text = when (target) {
                        PendingDelete.Transition -> listOfNotNull(
                            stringResource(
                                if (revertsSession) R.string.activity_delete_status_message else R.string.activity_delete_status_message_history,
                                label,
                                eventDay,
                            ),
                            foldedAmount?.let { stringResource(R.string.activity_delete_status_keeps_entry, it) },
                        ).joinToString(" ")
                        PendingDelete.Entry -> stringResource(
                            R.string.activity_delete_entry_message,
                            entry?.amount ?: 0,
                            progressUnitLabel(mediaType = mediaType, value = entry?.amount ?: 0),
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        when (target) {
                            PendingDelete.Transition -> event?.let { onDeleteStatusEvent(it.id) }
                            PendingDelete.Entry -> entry?.let { onDeleteProgressUpdate(it.id) }
                        }
                        onBack()
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun EditorSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogTheme.colors.appMuted,
    )
}

/** The period switch as a tactile row, the shape of the quick sheet's finish toggle. */
@Composable
private fun PeriodToggle(
    checked: Boolean,
    supporting: String,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = OmnilogTheme.colors
    val container by animateColorAsState(if (checked) accent.copy(alpha = 0.10f) else palette.appBackground, label = "periodFill")
    val outline by animateColorAsState(if (checked) accent else palette.appLine, label = "periodOutline")
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Switch },
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, outline),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.activity_covers_period),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.appInk,
                )
                Text(text = supporting, style = MaterialTheme.typography.labelMedium, color = palette.appMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = contentColorOn(accent),
                    checkedTrackColor = accent,
                    checkedBorderColor = accent,
                ),
            )
        }
    }
}

@Composable
private fun rangeHint(minimumDate: LocalDate?, maximumDate: LocalDate?): String = when {
    minimumDate != null && maximumDate != null -> stringResource(
        R.string.activity_date_out_of_range,
        minimumDate.formatActivityDate(),
        maximumDate.formatActivityDate(),
    )
    minimumDate != null -> stringResource(R.string.activity_date_not_before, minimumDate.formatActivityDate())
    else -> stringResource(R.string.activity_date_not_after, maximumDate?.formatActivityDate().orEmpty())
}

/** A running total, against the item's total when it has one: `370 / 412`, or `42` for hours. */
@Composable
internal fun formatActivityTotal(total: Int, progressTotal: Int?): String =
    if (progressTotal != null) {
        stringResource(R.string.activity_total_of, total, progressTotal)
    } else {
        total.toString()
    }
