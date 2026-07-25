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
import com.nilpo.contenttracker.core.activity.ActivityWindow
import com.nilpo.contenttracker.core.activity.activityWindows
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A session's activity: what was logged, how much, and when. See `docs/omnilog-activity-concept.md`.
 *
 * Built as a miniature of the Timeline rather than as a list of its own, because it is the same
 * content at a smaller scale — one title's chronology instead of the whole library's. Entries hang
 * off a rail exactly as they do there, so moving between the two surfaces does not mean learning a
 * second way to read the same facts.
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
    updates: List<ProgressUpdate>,
    statusEvents: List<SessionStatusEvent>,
    baselineProgress: Int,
    sessionStartedAt: LocalDate?,
    sessionFinishedAt: LocalDate?,
    sessionStatus: TrackingStatus,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = activityRows(
        updates = updates,
        statusEvents = statusEvents,
        baselineProgress = baselineProgress,
        sessionStartedAt = sessionStartedAt,
        sessionFinishedAt = sessionFinishedAt,
        sessionStatus = sessionStatus,
    )
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
                            val update = row.update
                            if (update != null) {
                                editingEntryId = update.id
                            } else if (row is ActivityRow.Status) {
                                editingStatusId = row.event.id
                            }
                        },
                    )
                }
            }
        }
    }

    editingEntryId?.let { id ->
        val row = rows.filterIsInstance<ActivityRow.Entry>().firstOrNull { it.update.id == id }
        if (row == null) {
            editingEntryId = null
        } else {
            ActivityEntryEditor(
                update = row.update,
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
        val row = rows.filterIsInstance<ActivityRow.Status>().firstOrNull { it.event.id == id }
        if (row == null) {
            editingStatusId = null
        } else {
            val orderedStatusEvents = statusEvents.sortedWith(
                compareBy({ it.createdAtEpochMillis }, { it.id }),
            )
            val statusIndex = orderedStatusEvents.indexOfFirst { it.id == id }
            val minimumDate = listOfNotNull(
                sessionStartedAt,
                orderedStatusEvents.getOrNull(statusIndex - 1)?.occurredOn,
            ).maxOrNull()
            val maximumDate = orderedStatusEvents.getOrNull(statusIndex + 1)?.occurredOn
            StatusEventEditor(
                event = row.event,
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
// Rows
// ─────────────────────────────────────────────────────────────

/** One line of the session's chronology, whichever table it came from. */
sealed interface ActivityRow {
    val key: String
    val update: ProgressUpdate?
        get() = when (this) {
            is Entry -> update
            is Status -> update
            is Milestone -> update
        }

    data class Entry(
        override val update: ProgressUpdate,
        val runningTotal: Int,
        val window: ActivityWindow?,
    ) : ActivityRow {
        override val key = "entry:${update.id}"
    }

    data class Status(
        val event: SessionStatusEvent,
        override val update: ProgressUpdate? = null,
        val runningTotal: Int? = null,
    ) : ActivityRow {
        override val key = "status:${event.id}"
    }

    /**
     * The session opening or closing.
     *
     * Read-only unless combined with a progress update. These are the session's own `startedAt`
     * and `finishedAt`, which earn their place because without them the list starts mid-story:
     * a run of entries with no beginning and no end.
     */
    data class Milestone(
        val kind: MilestoneKind,
        val date: LocalDate,
        override val update: ProgressUpdate? = null,
        val runningTotal: Int? = null,
    ) : ActivityRow {
        override val key = "milestone:${kind.name}"
    }
}

enum class MilestoneKind { Started, Finished, Abandoned }

/**
 * Interleaves entries and status changes, newest first.
 *
 * Two orderings, deliberately, because they answer different questions.
 *
 * **The list** is ordered by the immutable instant each row was written. Entries and status changes
 * can both be re-dated as corrections, and a correction should not make rows jump around the list
 * it was made in.
 *
 * **Running totals** accumulate in date order instead. A total answers "where had I got to by
 * then", which is a claim about the order things were consumed, not the order they were typed. It
 * also has to agree with `TimelineBuilder`, which totals the same entries the same way — the two
 * surfaces showing one entry at two different totals would be worse than either ordering alone.
 *
 * The cost is that a back-dated entry can show a total that reads out of step with its position in
 * the list: re-date the newest entry to the earliest day and its total becomes the smallest while
 * it stays at the top. That is the honest consequence of the row sitting where it was recorded and
 * the total counting where it belongs.
 */
internal fun activityRows(
    updates: List<ProgressUpdate>,
    statusEvents: List<SessionStatusEvent>,
    baselineProgress: Int,
    sessionStartedAt: LocalDate?,
    sessionFinishedAt: LocalDate? = null,
    sessionStatus: TrackingStatus? = null,
): List<ActivityRow> {
    val windows = activityWindows(updates = updates, sessionStartedAt = sessionStartedAt)

    val orderedUpdates = updates.sortedWith(compareBy({ it.loggedAt }, { it.createdAtEpochMillis }, { it.id }))
    var runningTotal = baselineProgress
    val updateTotals = mutableMapOf<Long, Int>()
    val entryRows = mutableMapOf<Long, ActivityRow.Entry>()
    orderedUpdates.forEach { update ->
        runningTotal += update.amount
        updateTotals[update.id] = runningTotal
        entryRows[update.id] = ActivityRow.Entry(
            update = update,
            runningTotal = runningTotal,
            window = windows[update.id],
        )
    }

    val rawStatuses = meaningfulActivityStatuses(statusEvents)
    val claimedUpdateIds = mutableSetOf<Long>()

    val statusRows = rawStatuses.map { event ->
        if (event.status == TrackingStatus.Completed) {
            val finalUpdate = orderedUpdates.lastOrNull { update ->
                update.hasKnownDate && update.loggedAt == event.occurredOn &&
                    update.createdAtEpochMillis <= event.createdAtEpochMillis
            }
            if (finalUpdate != null) {
                claimedUpdateIds += finalUpdate.id
                ActivityRow.Status(
                    event = event,
                    update = finalUpdate,
                    runningTotal = updateTotals[finalUpdate.id],
                )
            } else {
                ActivityRow.Status(event)
            }
        } else {
            ActivityRow.Status(event)
        }
    }

    var finishedMilestone: ActivityRow.Milestone? = null
    sessionFinishedAt?.let { finished ->
        when (sessionStatus) {
            TrackingStatus.Completed -> if (statusRows.none { it.event.status == TrackingStatus.Completed }) {
                val finalUpdate = orderedUpdates.lastOrNull { update ->
                    update.hasKnownDate && update.loggedAt == finished
                }
                if (finalUpdate != null) {
                    claimedUpdateIds += finalUpdate.id
                    finishedMilestone = ActivityRow.Milestone(
                        kind = MilestoneKind.Finished,
                        date = finished,
                        update = finalUpdate,
                        runningTotal = updateTotals[finalUpdate.id],
                    )
                } else {
                    finishedMilestone = ActivityRow.Milestone(MilestoneKind.Finished, finished)
                }
            }
            TrackingStatus.Dropped -> if (statusRows.none { it.event.status == TrackingStatus.Dropped }) {
                finishedMilestone = ActivityRow.Milestone(MilestoneKind.Abandoned, finished)
            }
            else -> Unit
        }
    }

    var startedMilestone: ActivityRow.Milestone? = null
    sessionStartedAt?.let { started ->
        val firstUpdate = orderedUpdates.firstOrNull { update ->
            update.hasKnownDate && update.loggedAt == started && update.id !in claimedUpdateIds
        }
        if (firstUpdate != null) {
            claimedUpdateIds += firstUpdate.id
            startedMilestone = ActivityRow.Milestone(
                kind = MilestoneKind.Started,
                date = started,
                update = firstUpdate,
                runningTotal = updateTotals[firstUpdate.id],
            )
        } else {
            startedMilestone = ActivityRow.Milestone(MilestoneKind.Started, started)
        }
    }

    val remainingEntries = entryRows.filterKeys { it !in claimedUpdateIds }.values.toList()
    val milestones = listOfNotNull(startedMilestone, finishedMilestone)

    return (remainingEntries + statusRows + milestones).sortedByDescending { row ->
        when (row) {
            is ActivityRow.Entry -> row.update.createdAtEpochMillis
            is ActivityRow.Status -> row.event.createdAtEpochMillis
            is ActivityRow.Milestone -> when (row.kind) {
                MilestoneKind.Started -> Long.MIN_VALUE
                MilestoneKind.Finished, MilestoneKind.Abandoned -> Long.MAX_VALUE
            }
        }
    }
}

/** Removes dangling resumes and redundant pauses while retaining terminal history. */
internal fun meaningfulActivityStatuses(
    events: List<SessionStatusEvent>,
): List<SessionStatusEvent> = buildList {
    var pauseOpen = false
    events.sortedWith(compareBy({ it.createdAtEpochMillis }, { it.id })).forEach { event ->
        when (event.status) {
            TrackingStatus.Paused -> if (!pauseOpen) {
                add(event)
                pauseOpen = true
            }
            TrackingStatus.InProgress -> if (pauseOpen) {
                add(event)
                pauseOpen = false
            }
            TrackingStatus.Completed, TrackingStatus.Dropped -> {
                add(event)
                pauseOpen = false
            }
            TrackingStatus.Planned -> pauseOpen = false
        }
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
    row: ActivityRow,
    isLast: Boolean,
    mediaType: MediaType,
    accent: Color,
    onClick: () -> Unit,
) {
    val description = row.describe(mediaType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = row !is ActivityRow.Milestone || row.update != null, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        ActivityRail(
            beadColor = when (row) {
                is ActivityRow.Entry -> OmnilogTheme.colors.appLine
                is ActivityRow.Status -> row.event.statusAccent()
                is ActivityRow.Milestone -> accent
            },
            beadSize = if (row is ActivityRow.Entry) EntryBeadSize else StatusBeadSize,
            isLast = isLast,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (row) {
                is ActivityRow.Entry -> ActivityEntryBody(row, mediaType, accent)
                is ActivityRow.Status -> ActivityStatusBody(row, mediaType, accent)
                is ActivityRow.Milestone -> ActivityMilestoneBody(row, mediaType, accent)
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

@Composable
private fun ActivityEntryBody(row: ActivityRow.Entry, mediaType: MediaType, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = row.dateLabel(),
                color = if (row.update.hasKnownDate) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (row.update.hasKnownDate) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Present only on an entry marked as covering a period, and only when there is a real
            // span to name. Derived from the entry before it, never stored and never asked for.
            row.window?.let { window ->
                Text(
                    text = stringResource(R.string.activity_window, window.from.formatActivityDate()),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(
                    R.string.activity_amount,
                    row.update.amount,
                    progressUnitLabel(mediaType = mediaType, value = row.update.amount),
                ),
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                text = row.runningTotal.toString(),
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActivityStatusBody(row: ActivityRow.Status, mediaType: MediaType, accent: Color) {
    val update = row.update
    val runningTotal = row.runningTotal
    if (update != null && runningTotal != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = row.event.occurredOn.formatActivityDate(),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(row.event.statusLabelRes()),
                    color = row.event.statusAccent(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
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
    } else {
        Text(
            text = row.event.occurredOn.formatActivityDate(),
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = stringResource(row.event.statusLabelRes()),
            color = row.event.statusAccent(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActivityMilestoneBody(row: ActivityRow.Milestone, mediaType: MediaType, accent: Color) {
    val update = row.update
    val runningTotal = row.runningTotal
    if (update != null && runningTotal != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = row.date.formatActivityDate(),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(row.kind.labelRes()),
                    color = accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
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
    } else {
        Text(
            text = row.date.formatActivityDate(),
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = stringResource(row.kind.labelRes()),
            color = accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Labels
// ─────────────────────────────────────────────────────────────

@Composable
private fun ActivityRow.Entry.dateLabel(): String = if (update.hasKnownDate) {
    update.loggedAt.formatActivityDate()
} else {
    stringResource(R.string.activity_date_unknown)
}

internal fun MilestoneKind.labelRes(): Int = when (this) {
    MilestoneKind.Started -> R.string.activity_started
    MilestoneKind.Finished -> R.string.activity_finished
    MilestoneKind.Abandoned -> R.string.activity_abandoned
}

internal fun SessionStatusEvent.statusLabelRes(): Int =
    when (status) {
        TrackingStatus.Paused -> R.string.activity_paused
        TrackingStatus.Completed -> R.string.activity_finished
        TrackingStatus.Dropped -> R.string.activity_abandoned
        else -> R.string.activity_resumed
    }

@Composable
internal fun SessionStatusEvent.statusAccent(): Color = when (status) {
    TrackingStatus.Paused -> OmnilogTheme.accents.Paused
    TrackingStatus.Completed -> OmnilogTheme.accents.Completed
    TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
    else -> OmnilogTheme.accents.InProgress
}

@Composable
private fun ActivityRow.describe(mediaType: MediaType): String = when (this) {
    is ActivityRow.Entry -> stringResource(
        R.string.activity_entry_description,
        dateLabel(),
        update.amount,
        progressUnitLabel(mediaType = mediaType, value = update.amount),
        runningTotal,
    )

    is ActivityRow.Status -> {
        val base = event.occurredOn.formatActivityDate() + " · " + stringResource(event.statusLabelRes())
        if (update != null && runningTotal != null) {
            base + " · +" + update.amount + " " + progressUnitLabel(mediaType = mediaType, value = update.amount) + " · " + runningTotal
        } else {
            base
        }
    }

    is ActivityRow.Milestone -> {
        val base = date.formatActivityDate() + " · " + stringResource(kind.labelRes())
        if (update != null && runningTotal != null) {
            base + " · +" + update.amount + " " + progressUnitLabel(mediaType = mediaType, value = update.amount) + " · " + runningTotal
        } else {
            base
        }
    }
}

/** Follows the reader's locale rather than one hard-coded ordering. */
internal fun LocalDate.formatActivityDate(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
