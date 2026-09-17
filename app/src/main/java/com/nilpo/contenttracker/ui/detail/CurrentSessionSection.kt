package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.remember
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.StatusChangeSheet
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.ArrowDropDown
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.TrackingDateRange
import com.nilpo.contenttracker.ui.common.TrackingNotesField
import com.nilpo.contenttracker.ui.common.TrackingProgressField
import com.nilpo.contenttracker.ui.common.TrackingRatingSelector
import com.nilpo.contenttracker.ui.common.TrackingStatusSelector
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────
// Entry point — the card, its status menu, and what they open
// ─────────────────────────────────────────────────────────────

@Composable
fun CurrentSessionSection(
    trackedMedia: TrackedMedia,
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onQuickComplete: (QuickCompletion) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    // Absent when there is no earlier session to fall back to — see DetailScreen.
    onDeleteSession: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showEditor by rememberSaveable(session.id) { mutableStateOf(false) }
    // A state picked from the card's menu, waiting on its sheet.
    var pendingStatus by rememberSaveable(session.id) { mutableStateOf<TrackingStatus?>(null) }

    SessionCard(
        session = session,
        progressTotal = progressTotal,
        mediaType = mediaType,
        onEditClick = { showEditor = true },
        onStatusSelected = { status ->
            if (status == TrackingStatus.Planned) {
                // Back on the list: nothing to ask, so no sheet. Progress and dates are kept as
                // they are; the editor is the place to clear them.
                onUpdateSessionDetails(
                    session.id,
                    status,
                    session.progressCurrent,
                    session.ratingHalfPoints,
                    session.notes,
                    session.startedAt,
                    null,
                )
            } else {
                pendingStatus = status
            }
        },
        onDeleteProgressUpdate = onDeleteProgressUpdate,
        onDeleteStatusEvent = onDeleteStatusEvent,
        onUpdateStatusEventDate = onUpdateStatusEventDate,
        onUpdateProgressUpdate = onUpdateProgressUpdate,
        modifier = modifier,
    )

    pendingStatus?.let { target ->
        val targetVisual = sessionStateVisual(target)
        StatusChangeSheet(
            trackedMedia = trackedMedia,
            targetStatus = target,
            targetLabel = targetVisual.label,
            accent = targetVisual.color,
            onConfirm = { change ->
                pendingStatus = null
                if (change.status == TrackingStatus.Completed) {
                    // The quick path, so finishing from here gets the same undo as finishing from Home.
                    onQuickComplete(
                        QuickCompletion(
                            progress = change.progress,
                            ratingHalfPoints = change.ratingHalfPoints,
                            finishedAt = change.date ?: LocalDate.now(),
                        ),
                    )
                } else {
                    onUpdateSessionDetails(
                        session.id,
                        change.status,
                        change.progress,
                        change.ratingHalfPoints,
                        session.notes,
                        // Only leaving Planned stamps a start. A paused or reopened session began
                        // when it began, and inventing today would misfile it in stats.
                        session.startedAt
                            ?: LocalDate.now().takeIf { session.status == TrackingStatus.Planned },
                        change.date,
                    )
                }
            },
            onDismiss = { pendingStatus = null },
        )
    }

    if (showEditor) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SessionEditorScreen(
                session = session,
                progressTotal = progressTotal,
                mediaType = mediaType,
                accent = accent,
                onBack = { showEditor = false },
                onSaveSessionDetails = onUpdateSessionDetails,
                onDeleteSession = onDeleteSession?.let {
                    {
                        it()
                        showEditor = false
                    }
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// The card
// ─────────────────────────────────────────────────────────────

/**
 * The live session, straight on the page rather than inside a panel.
 *
 * The whole section opens the session editor. The status on its first line is a control of its own: a
 * menu of the five states, each opening the short sheet that change needs. The position sits opposite
 * it, one plain bar under both, then the dates and a quiet link into the session's activity.
 *
 * The log button lives in the action row below, and the rating stands beside the providers' scores
 * further down rather than being repeated here.
 */
@Composable
private fun SessionCard(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    onEditClick: () -> Unit,
    onStatusSelected: (TrackingStatus) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = sessionStateVisual(session.status)
    val recency = sessionRecencyLabel(session)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = stringResource(R.string.edit_current_session),
                onClick = onEditClick,
            ),
    ) {
        // Start 4 plus the status menu's own 4 puts the status mark on the gutter; see DetailScreen.
        Column(
            modifier = Modifier.padding(start = 4.dp, end = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusMenu(current = session.status, onStatusSelected = onStatusSelected)
                Spacer(modifier = Modifier.weight(1f))
                ProgressFigure(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                )
            }

            SessionProgressBar(
                progressCurrent = session.progressCurrent,
                progressTotal = progressTotal,
                color = visual.color,
                track = OmnilogTheme.colors.appLine,
                modifier = Modifier.padding(start = 4.dp),
            )

            // Nothing at all when nothing is known: the status line has already said where things stand.
            if (session.startedAt != null || session.finishedAt != null || recency != null) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SessionDatesRow(
                        session = session,
                        mediaType = mediaType,
                        modifier = Modifier.weight(1f),
                        compact = true,
                        trailing = recency,
                    )
                    ActivityAction(
                        session = session,
                        mediaType = mediaType,
                        accent = visual.color,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                        onUpdateStatusEventDate = onUpdateStatusEventDate,
                    )
                }
            }

            session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appInk.copy(alpha = 0.72f),
                )
            }
        }
    }
}

/**
 * The status, in serif with its own mark and a caret, opening a menu of all five states.
 *
 * Picking the current one does nothing; picking another hands it to [onStatusSelected], which decides
 * whether that change needs a sheet.
 */
@Composable
private fun StatusMenu(
    current: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val visual = sessionStateVisual(current)

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClickLabel = stringResource(R.string.status_change_title)) {
                    expanded = true
                }
                .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(visual.icon),
                contentDescription = null,
                tint = visual.color,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = visual.label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = OmnilogTheme.colors.appPanel,
        ) {
            TrackingStatus.entries.forEach { status ->
                val option = sessionStateVisual(status)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OmnilogTheme.colors.appInk,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            tint = option.color,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = if (status == current) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = OmnilogTheme.colors.appMuted,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        if (status != current) onStatusSelected(status)
                    },
                )
            }
        }
    }
}

/**
 * `214 / 384 pàgines   56%` — the count in serif ink, the scale and share muted beside it.
 *
 * The bare count on its own: a medium with no total, a game's hours, has no scale to show.
 */
@Composable
private fun ProgressFigure(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
) {
    val total = progressTotal?.takeIf { it > 0 }
    val current = if (total != null) {
        session.progressCurrent.coerceAtMost(total)
    } else {
        session.progressCurrent
    }
    val unit = progressUnitLabel(mediaType = mediaType, value = total ?: current)

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = current.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Normal,
            ),
            color = OmnilogTheme.colors.appInk,
        )
        Text(
            text = if (total != null) " / $total $unit" else " $unit",
            modifier = Modifier.padding(bottom = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
        if (total != null) {
            Text(
                text = "${current * 100 / total}%",
                modifier = Modifier.padding(start = 14.dp, bottom = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
            )
        }
    }
}

/**
 * What the log button offers, which depends on where the session is and not only on what it holds.
 *
 * Planned and Paused are both "tell me where you are" as far as the sheet behind this is concerned,
 * but they are not the same invitation, and a button reading `Registra episodis` on something you have
 * not started is asking the wrong question.
 */
@Composable
internal fun logActionLabel(status: TrackingStatus, mediaType: MediaType): String = when (status) {
    TrackingStatus.Planned -> sessionStartActionLabel(mediaType)
    TrackingStatus.Paused -> sessionResumeActionLabel(mediaType)
    else -> logProgressLabel(mediaType)
}

// ─────────────────────────────────────────────────────────────
// Full-page edit screen
// ─────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SessionEditorScreen(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    titleResId: Int = R.string.edit_current_session,
    onBack: () -> Unit,
    onSaveSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeleteSession: (() -> Unit)? = null,
) {
    var showDeleteConfirmation by rememberSaveable(session.id) { mutableStateOf(false) }
    var draftStatus by rememberSaveable(session.id) { mutableStateOf(session.status) }
    var draftProgressText by rememberSaveable(session.id) {
        mutableStateOf(
            session.progressCurrent.toString(),
        )
    }
    var draftRating by rememberSaveable(session.id) { mutableStateOf(session.ratingHalfPoints) }
    var draftNotes by rememberSaveable(session.id) { mutableStateOf(session.notes.orEmpty()) }
    var draftStartedAtText by rememberSaveable(session.id) { mutableStateOf(session.startedAt?.toString().orEmpty()) }
    var draftFinishedAtText by rememberSaveable(session.id) { mutableStateOf(session.finishedAt?.toString().orEmpty()) }
    // Metadata totals may be corrected below recorded progress. Merely opening and saving the editor
    // must never erase that history.
    val maxProgress = maxOf(progressTotal ?: Int.MAX_VALUE, session.progressCurrent)
    val parsedProgress = draftProgressText.toIntOrNull()
    val parsedStartedAt = draftStartedAtText.toLocalDateOrNull()
    val parsedFinishedAt = draftFinishedAtText.toLocalDateOrNull()
    val datesParse = (draftStartedAtText.isBlank() || parsedStartedAt != null) &&
        (draftFinishedAtText.isBlank() || parsedFinishedAt != null)
    val datesOrdered = parsedStartedAt == null || parsedFinishedAt == null ||
        !parsedFinishedAt.isBefore(parsedStartedAt)
    val eventsBeforeTerminal = if (
        draftStatus == session.status && draftStatus.endsSession &&
        session.statusEvents.lastOrNull()?.status == draftStatus
    ) {
        session.statusEvents.dropLast(1)
    } else {
        session.statusEvents
    }
    val transitionOrderValid = parsedFinishedAt == null ||
        eventsBeforeTerminal.lastOrNull()?.occurredOn?.isAfter(parsedFinishedAt) != true
    val canSave = parsedProgress != null && parsedProgress in 0..maxProgress &&
        datesParse && datesOrdered && transitionOrderValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = accent,
                        ),
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            onSaveSessionDetails(
                                session.id,
                                draftStatus,
                                parsedProgress ?: return@Button,
                                draftRating,
                                draftNotes.takeIf { it.isNotBlank() },
                                draftStartedAtText.toLocalDateOrNull(),
                                draftFinishedAtText.toLocalDateOrNull(),
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White,
                        ),
                        enabled = canSave,
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val stateColor = sessionStateVisual(draftStatus).color
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Status ───────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_status))
            TrackingStatusSelector(
                selectedStatus = draftStatus,
                accent = stateColor,
                onStatusSelected = { status ->
                    draftStatus = status
                    if (status == TrackingStatus.InProgress && draftStartedAtText.isBlank()) {
                        draftStartedAtText = LocalDate.now().toString()
                    }
                    // Offered, not imposed — the same way the start date is. Filling the field here
                    // rather than defaulting it on save is what keeps "finished, date unknown"
                    // expressible: the date is visible before saving and can be cleared again.
                    if (status.endsSession && draftFinishedAtText.isBlank()) {
                        draftFinishedAtText = LocalDate.now().toString()
                    }
                    if (!status.endsSession) {
                        draftFinishedAtText = ""
                    }
                    if (status == TrackingStatus.Completed && progressTotal != null && progressTotal > 0) {
                        draftProgressText = maxOf(
                            draftProgressText.toIntOrNull() ?: 0,
                            progressTotal,
                        ).toString()
                    }
                },
            )

            EditSectionDivider()

            // ── Progress ─────────────────────────────────────────
            TrackingProgressField(
                value = draftProgressText,
                progressTotal = progressTotal,
                mediaType = mediaType,
                label = stringResource(R.string.field_progress),
                accent = stateColor,
                onValueChange = { value -> draftProgressText = value },
            )

            EditSectionDivider()

            // ── Rating ───────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_rating))
            TrackingRatingSelector(
                currentRatingHalfPoints = draftRating,
                accent = stateColor,
                onRatingSelected = { draftRating = it },
            )

            EditSectionDivider()

            EditSectionHeader(title = stringResource(R.string.session_dates))
            TrackingDateRange(
                startedLabel = stringResource(R.string.session_started_label),
                startedValue = draftStartedAtText,
                accent = stateColor,
                onStartedValueChange = { draftStartedAtText = it },
                finishedLabel = stringResource(R.string.session_finished_label),
                finishedValue = draftFinishedAtText,
                onFinishedValueChange = { draftFinishedAtText = it },
            )

            EditSectionDivider()

            // ── Notes ────────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_notes))
            TrackingNotesField(
                value = draftNotes,
                accent = stateColor,
                onValueChange = { draftNotes = it },
            )

            // Deleting the live session hands the title back to the one before it, so it only
            // appears when there is a previous session to fall back to.
            onDeleteSession?.let {
                EditSectionDivider()
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.delete_session),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirmation && onDeleteSession != null) {
        OmnilogAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = stringResource(R.string.delete_session_title),
            text = { Text(text = stringResource(R.string.delete_current_session_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteSession()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
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

@Composable
private fun EditSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogTheme.colors.appMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EditSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

private fun String.toLocalDateOrNull(): LocalDate? =
    trim().takeIf { it.isNotBlank() }?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
