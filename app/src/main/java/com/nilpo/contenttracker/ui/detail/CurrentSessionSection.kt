package com.nilpo.contenttracker.ui.detail

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
import androidx.compose.material3.FilledTonalIconButton
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
import com.nilpo.contenttracker.ui.common.RatingMeter
import com.nilpo.contenttracker.ui.common.TrackingDateRange
import com.nilpo.contenttracker.ui.common.TrackingNotesField
import com.nilpo.contenttracker.ui.common.TrackingProgressField
import com.nilpo.contenttracker.ui.common.TrackingRatingSelector
import com.nilpo.contenttracker.ui.common.TrackingStatusSelector
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────
// Entry point — swaps between read card and full edit screen
// ─────────────────────────────────────────────────────────────

@Composable
fun CurrentSessionSection(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onLogProgress: (() -> Unit)?,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by rememberSaveable(session.id) { mutableStateOf(false) }

    SessionCard(
        session = session,
        progressTotal = progressTotal,
        mediaType = mediaType,
        onLogProgress = onLogProgress,
        onEditClick = { showEditor = true },
        onDeleteProgressUpdate = onDeleteProgressUpdate,
        onDeleteStatusEvent = onDeleteStatusEvent,
        onUpdateStatusEventDate = onUpdateStatusEventDate,
        onUpdateProgressUpdate = onUpdateProgressUpdate,
        modifier = modifier,
    )

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
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// The card
// ─────────────────────────────────────────────────────────────

/**
 * The live session, and the page's centre of gravity.
 *
 * Three things changed from the card this replaces, and they are all the same change: the state is
 * the card's subject, so the state gets the colour. The status chip is filled rather than washed, the
 * progress graphic and the button are drawn in the same accent, and the tinted border that used to
 * outline the whole panel is gone — a border spends a colour on the shape of the card rather than on
 * anything the card is saying.
 *
 * The panel itself is flat `appPanel` at 18dp with no elevation, which is what every surface built
 * since already does: `TimelineRecapCard`, and the library panel inside `ProfileHeroCard`.
 *
 * There is one hero slot rather than a layout per status. What fills it is decided by the data — a
 * rating if there is one, the progress figure if there is not — and that is what let the five
 * per-status summary composables this file used to carry collapse into a single arrangement.
 */
@Composable
private fun SessionCard(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    onLogProgress: (() -> Unit)?,
    onEditClick: () -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = sessionStateVisual(session.status)
    val state = visual.color

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        // Sunk below the panel the rest of the app uses, and neutral: the artwork supplies the
        // colour, and the veil behind the content fades into this exact value.
        color = cardGround(),
    ) {
        // The artwork is drawn behind the content across the card's whole area, and the Surface's
        // shape clips it, so it bleeds off the corner rather than sitting inside a frame of its own.
        Column(
            modifier = Modifier
                .sessionCardArtwork(mediaType)
                .padding(16.dp),
            // 14dp between six blocks was most of why this card ran tall. The blocks are distinct
            // enough at 11 — the chip, the figure, the graphic and the dates are different shapes and
            // different weights, and none of them needed a gap to be told apart from its neighbour.
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            // Planned no longer forks off its own anatomy. It is the in-progress card at zero: the
            // same chip-and-edit row, the same figure — now showing the total instead of a current —
            // and the same track, empty rather than partway full. One less shape for the eye to
            // learn, at the cost of a row that used to be bare artwork now stating the obvious: zero.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionStateChip(visual = visual)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActivityAction(
                        updates = session.progressUpdates,
                        statusEvents = session.statusEvents,
                        baselineProgress = session.baselineProgress,
                        sessionStartedAt = session.startedAt,
                        sessionFinishedAt = session.finishedAt,
                        sessionStatus = session.status,
                        progressTotal = progressTotal,
                        mediaType = mediaType,
                        accent = state,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                        onUpdateStatusEventDate = onUpdateStatusEventDate,
                    )
                    FilledTonalIconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // The hero slot. A rating is a verdict and a progress figure is a position; when both
            // exist the verdict is the more interesting of the two, so it takes the slot and the
            // position drops to the caption under its own graphic.
            val rating = session.rating
            if (rating != null) {
                RatingMeter(rating = rating, accent = state)
            } else {
                ProgressFigure(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    color = state,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SessionProgressGraphic(
                    progressCurrent = session.progressCurrent,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    progressUpdates = session.progressUpdates,
                    color = state,
                    faded = session.status == TrackingStatus.Dropped,
                    track = cardTrack(),
                )
                progressCaption(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    ratingHasHero = rating != null,
                )?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }

            // Nothing at all when nothing is known. The row already drops itself in that case, and
            // the line that used to take its place — "Sense dates registrades" — spent a row of the
            // card announcing an absence the reader can see for themselves.
            SessionDatesRow(
                session = session,
                mediaType = mediaType,
                trailing = sessionRecencyLabel(session),
            )

            session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appInk.copy(alpha = 0.72f),
                )
            }

            StartAction(session = session, mediaType = mediaType, onLogProgress = onLogProgress, accent = state)
        }
    }
}

/**
 * The card's one saturated object, and the reason the card is worth tapping.
 *
 * Everything else on the card is accent-at-low-alpha, muted ink or artwork, so the eye lands here
 * without the button having to be large. It was a slab: full width at the default button height,
 * which on a planned card left a control taller than everything above it put together. Trimmed to
 * the height of a row instead, full width since it is the last thing on the card.
 */
@Composable
private fun StartAction(
    session: TrackingSession,
    mediaType: MediaType,
    onLogProgress: (() -> Unit)?,
    accent: Color,
) {
    val log = onLogProgress ?: return
    Button(
        onClick = log,
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionHeight),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            text = logActionLabel(status = session.status, mediaType = mediaType),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

private val ActionHeight = 40.dp

/**
 * Where the session has got to, as the largest thing on the card.
 *
 * The figure is the count on its own; the unit and the total are the caption beside it. The old card
 * set the whole sentence — "12 / 28 episodis" — at one weight, which made the number as hard to find
 * as everything around it.
 */
@Composable
private fun ProgressFigure(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    color: Color,
) {
    val total = progressTotal?.takeIf { it > 0 }
    val current = if (total != null) {
        session.progressCurrent.coerceAtMost(total)
    } else {
        session.progressCurrent
    }
    val unit = progressUnitLabel(mediaType = mediaType, value = total ?: current)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = current.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
        Text(
            text = if (total != null) {
                stringResource(R.string.session_progress_of_suffix, total, unit)
            } else {
                unit
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
        if (total != null && total > 0) {
            Text(
                text = "${current * 100 / total}%",
                modifier = Modifier.padding(bottom = 5.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
        }
    }
}

/**
 * The line under the graphic, or nothing when the figure above has already said it.
 *
 * Books get one either way: pages remaining is the number a reader actually wants, and the fore-edge
 * can show the proportion but cannot say how many are left.
 */
@Composable
private fun progressCaption(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    ratingHasHero: Boolean,
): String? {
    val total = progressTotal?.takeIf { it > 0 }

    if (mediaType == MediaType.Book && total != null) {
        val left = (total - session.progressCurrent).coerceAtLeast(0)
        if (left > 0) return pluralStringResource(R.plurals.session_pages_left, left, left)
    }
    if (!ratingHasHero) return null

    val current = if (total != null) {
        session.progressCurrent.coerceAtMost(total)
    } else {
        session.progressCurrent
    }
    val unit = progressUnitLabel(mediaType = mediaType, value = total ?: current)
    return if (total != null) {
        stringResource(R.string.session_progress_of, current, total, unit)
    } else {
        stringResource(R.string.session_progress_plain, current, unit)
    }
}

/**
 * What the button offers, which depends on where the session is and not only on what it holds.
 *
 * Planned and Paused are both "tell me where you are" as far as the sheet behind this is concerned,
 * but they are not the same invitation, and a button reading `Registra episodi` on something you have
 * not started is asking the wrong question.
 */
@Composable
private fun logActionLabel(status: TrackingStatus, mediaType: MediaType): String = when (status) {
    TrackingStatus.Planned -> stringResource(R.string.session_start_action)
    TrackingStatus.Paused -> stringResource(R.string.session_resume_action)
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
) {
    var draftStatus by rememberSaveable(session.id) { mutableStateOf(session.status) }
    var draftProgressText by rememberSaveable(session.id) {
        mutableStateOf(
            session.progressCurrent.toString(),
        )
    }
    var draftRating by rememberSaveable(session.id) { mutableStateOf(session.rating) }
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
                currentRating = draftRating,
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

            Spacer(modifier = Modifier.height(32.dp))
        }
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
