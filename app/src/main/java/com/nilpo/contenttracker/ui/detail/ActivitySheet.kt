package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.activity.SessionActivity
import com.nilpo.contenttracker.core.activity.SessionActivityKind
import com.nilpo.contenttracker.core.activity.activity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A session's activity: what was logged, how much, and when. See `docs/features/activity.md`.
 *
 * Built as a miniature of the Timeline rather than as a list of its own, because it is the same
 * content at a smaller scale — one title's chronology instead of the whole library's. Both read the
 * same derivation, [activity], and entries hang off a rail exactly as they do there, so moving
 * between the two surfaces does not mean learning a second way to read the same facts.
 *
 * A sheet rather than a modal. Activitat is something to read, and a sheet is put down rather than
 * closed with a button. It also lets a long history scroll without the session card growing to
 * hold it.
 *
 * Every row is acted on by tapping the row itself. The old surface put a 32dp edit button and a
 * 32dp delete button side by side on each line — two targets below the minimum touch size, one of
 * them destructive, a thumb's width apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(
    session: TrackingSession,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = session.activity()
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingStatusId by rememberSaveable { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_title),
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )

            // The list keeps every vertical gesture, so the sheet is only ever closed from the top.
            //
            // A sheet drags on whatever its content leaves unconsumed, which would make a downward
            // swipe anywhere in the list a dismissal — including at the top of a long history, mid
            // read. Swallowing the leftovers here (this connection sits below the sheet's own, so it
            // is offered them first) leaves the handle and the title as the only places that close
            // it. Both are at the top, both are deliberate, and neither can be hit by overshooting
            // a scroll. Tapping the scrim and the back gesture still work as they always do.
            val holdGestures = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = available

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity = available
                }
            }

            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .nestedScroll(holdGestures),
            ) {
                items(items = rows, key = { it.key }) { row ->
                    ActivityRailRow(
                        row = row,
                        isLast = row.key == rows.last().key,
                        mediaType = mediaType,
                        accent = accent,
                        onClick = {
                            when (val target = row.editingTarget()) {
                                is ActivityEditTarget.Progress -> editingEntryId = target.id
                                is ActivityEditTarget.Status -> editingStatusId = target.id
                                null -> Unit
                            }
                        },
                    )
                }
            }
        }
    }

    editingEntryId?.let { id ->
        // Searched across every row: a folded entry lives on its milestone, not on a row of its own.
        val row = rows.firstOrNull { it.progress?.id == id }
        val update = row?.progress
        if (update == null) {
            editingEntryId = null
        } else {
            ActivityEntryEditor(
                update = update,
                window = row.window,
                // Provider totals are metadata and may be corrected below recorded history.
                headroom = null,
                mediaType = mediaType,
                accent = accent,
                onSave = { amount, loggedAt, coversPeriod ->
                    onUpdateProgressUpdate(id, amount, loggedAt, coversPeriod)
                    editingEntryId = null
                },
                onDelete = {
                    onDeleteProgressUpdate(id)
                    editingEntryId = null
                },
                onDismiss = { editingEntryId = null },
            )
        }
    }

    editingStatusId?.let { id ->
        val row = rows.firstOrNull { it.statusEvent?.id == id }
        val event = row?.statusEvent
        if (event == null) {
            editingStatusId = null
        } else {
            val orderedStatusEvents = session.statusEvents.sortedWith(
                compareBy({ it.createdAtEpochMillis }, { it.id }),
            )
            val statusIndex = orderedStatusEvents.indexOfFirst { it.id == id }
            val minimumDate = listOfNotNull(
                session.startedAt,
                orderedStatusEvents.getOrNull(statusIndex - 1)?.occurredOn,
            ).maxOrNull()
            val maximumDate = orderedStatusEvents.getOrNull(statusIndex + 1)?.occurredOn
            StatusEventEditor(
                event = event,
                label = stringResource(row.kind.labelRes()),
                accent = accent,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
                onSaveDate = { date ->
                    onUpdateStatusEventDate(id, date)
                    editingStatusId = null
                },
                onDelete = {
                    onDeleteStatusEvent(id)
                    editingStatusId = null
                },
                onDismiss = { editingStatusId = null },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Editing
// ─────────────────────────────────────────────────────────────

internal sealed interface ActivityEditTarget {
    data class Progress(val id: Long) : ActivityEditTarget
    data class Status(val id: Long) : ActivityEditTarget
}

/**
 * An ending's own transition is what its row is about, so it wins over the entry folded into it.
 * Every other row edits its entry first.
 */
internal fun SessionActivity.editingTarget(): ActivityEditTarget? {
    val progressTarget = progress?.let { ActivityEditTarget.Progress(it.id) }
    val statusTarget = statusEvent?.let { ActivityEditTarget.Status(it.id) }
    return when (kind) {
        SessionActivityKind.Completed, SessionActivityKind.Dropped -> statusTarget ?: progressTarget
        else -> progressTarget ?: statusTarget
    }
}

// ─────────────────────────────────────────────────────────────
// The rail
// ─────────────────────────────────────────────────────────────

private val RailWidth = 20.dp
private val EntryBeadSize = 6.dp
private val StatusBeadSize = 9.dp
private val BeadCentreOffset = 16.dp

/**
 * One row hung off the rail.
 *
 * A status change takes the larger bead in its own accent and an entry takes a small one in the
 * line's own tone — the same rule the Timeline uses to tell a milestone from the noise around it.
 */
@Composable
private fun ActivityRailRow(
    row: SessionActivity,
    isLast: Boolean,
    mediaType: MediaType,
    accent: Color,
    onClick: () -> Unit,
) {
    val description = row.describe(mediaType)
    val isEntry = row.kind == SessionActivityKind.Progress

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = row.editingTarget() != null, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        ActivityRail(
            beadColor = if (isEntry) OmnilogTheme.colors.appLine else row.kind.accent(accent),
            beadSize = if (isEntry) EntryBeadSize else StatusBeadSize,
            isLast = isLast,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (isEntry) {
                    Text(
                        text = row.dateLabel(),
                        color = if (row.date != null) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (row.date != null) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Present only on an entry marked as covering a period, and only when there is a
                    // real span to name. Derived from the entry before it, never stored or asked for.
                    row.window?.let { window ->
                        Text(
                            text = stringResource(R.string.activity_window, window.from.formatActivityDate()),
                            color = OmnilogTheme.colors.appMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = row.dateLabel(),
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(row.kind.labelRes()),
                        color = row.kind.accent(accent),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
            }
            val update = row.progress
            val runningTotal = row.runningTotal
            if (update != null && runningTotal != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(
                            R.string.activity_amount,
                            update.amount,
                            progressUnitLabel(mediaType = mediaType, value = update.amount),
                        ),
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Text(
                        text = runningTotal.toString(),
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRail(beadColor: Color, beadSize: Dp, isLast: Boolean) {
    Box(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
        // The line stops at the last bead rather than trailing off into empty space below it.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(1.dp)
                .then(if (isLast) Modifier.height(BeadCentreOffset) else Modifier.fillMaxHeight())
                .background(OmnilogTheme.colors.appLine),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BeadCentreOffset - beadSize / 2)
                .size(beadSize)
                .clip(CircleShape)
                .background(beadColor),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Labels
// ─────────────────────────────────────────────────────────────

@Composable
private fun SessionActivity.dateLabel(): String =
    date?.formatActivityDate() ?: stringResource(R.string.activity_date_unknown)

internal fun SessionActivityKind.labelRes(): Int = when (this) {
    SessionActivityKind.Started -> R.string.activity_started
    SessionActivityKind.Progress -> R.string.activity_edit_title
    SessionActivityKind.Paused -> R.string.activity_paused
    SessionActivityKind.Resumed -> R.string.activity_resumed
    SessionActivityKind.Reopened -> R.string.activity_reopened
    SessionActivityKind.Replanned -> R.string.activity_replanned
    SessionActivityKind.Completed -> R.string.activity_finished
    SessionActivityKind.Dropped -> R.string.activity_abandoned
}

/** [sessionAccent] marks the session's own start, which has no status colour of its own. */
@Composable
private fun SessionActivityKind.accent(sessionAccent: Color): Color = when (this) {
    SessionActivityKind.Started, SessionActivityKind.Progress -> sessionAccent
    SessionActivityKind.Paused -> OmnilogTheme.accents.Paused
    SessionActivityKind.Completed -> OmnilogTheme.accents.Completed
    SessionActivityKind.Dropped -> OmnilogTheme.accents.Dropped
    SessionActivityKind.Replanned -> OmnilogTheme.accents.Planned
    SessionActivityKind.Resumed, SessionActivityKind.Reopened -> OmnilogTheme.accents.InProgress
}

@Composable
private fun SessionActivity.describe(mediaType: MediaType): String {
    val update = progress
    if (kind == SessionActivityKind.Progress && update != null) {
        return stringResource(
            R.string.activity_entry_description,
            dateLabel(),
            update.amount,
            progressUnitLabel(mediaType = mediaType, value = update.amount),
            runningTotal ?: update.amount,
        )
    }
    val base = dateLabel() + " · " + stringResource(kind.labelRes())
    return if (update != null && runningTotal != null) {
        base + " · +" + update.amount + " " + progressUnitLabel(mediaType = mediaType, value = update.amount) + " · " + runningTotal
    } else {
        base
    }
}

/** Follows the reader's locale rather than one hard-coded ordering. */
internal fun LocalDate.formatActivityDate(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
