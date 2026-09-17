package com.nilpo.contenttracker.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.activity.SessionActivity
import com.nilpo.contenttracker.core.activity.SessionActivityKind
import com.nilpo.contenttracker.core.activity.activity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.timeline.DotHalo
import com.nilpo.contenttracker.ui.timeline.MilestoneDotSize
import com.nilpo.contenttracker.ui.timeline.ProgressDotSize
import com.nilpo.contenttracker.ui.timeline.TimelineGutterDate
import com.nilpo.contenttracker.ui.timeline.TimelineRail
import com.nilpo.contenttracker.ui.timeline.gutterWidth
import com.nilpo.contenttracker.ui.timeline.headline
import com.nilpo.contenttracker.ui.timeline.timelineGutterDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A session's activity, read as a diary: what happened, on which day, and where it left you. See
 * `docs/features/activity.md`.
 *
 * Newest day first, so running totals always read in order and a re-dated entry moves to the day it
 * claims. Undated rows close the list: they belong to the session but to no day in it.
 *
 * Hung off the Timeline's own gutter and rail — the date in the gutter once per day, a bead per row —
 * because it is the same chronology at the scale of one session. Status changes take the larger bead
 * in their state's colour and heavier type; progress entries stay light. Every row that holds
 * progress shows both what was added and the total it reached.
 *
 * Tapping a row edits it in place: the sheet swaps its list for [ActivityRowEditor], and back returns
 * to the list. One surface rather than a dialog over a sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(
    session: TrackingSession,
    mediaType: MediaType,
    progressTotal: Int?,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = session.activity()
    // Games log hours against no fixed length, so their totals stand alone.
    val itemTotal = progressTotal?.takeIf { it > 0 && mediaType != MediaType.Game }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    // Deleted, or re-keyed by an edit, while open: nothing left to edit, so back to the list.
    val editingRow = editingKey?.let { key -> rows.firstOrNull { it.key == key } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        BackHandler(enabled = editingRow != null) { editingKey = null }

        // The list and the editor are two pages of one sheet: the editor comes in from the side it
        // leaves back to, and the sheet eases to the new height instead of jumping.
        AnimatedContent(
            targetState = editingRow,
            contentKey = { it?.key },
            transitionSpec = {
                val forward = targetState != null
                val enter = slideInHorizontally(SheetPageSpec) { width -> if (forward) width / 5 else -width / 5 } +
                    fadeIn(tween(220, delayMillis = 60))
                val exit = slideOutHorizontally(SheetPageSpec) { width -> if (forward) -width / 5 else width / 5 } +
                    fadeOut(tween(120))
                (enter togetherWith exit).using(SizeTransform(clip = false) { _, _ -> SheetSizeSpec })
            },
            label = "activitySheetPage",
        ) { editingRow ->
            if (editingRow != null) {
                val orderedEvents = session.statusEvents.sortedWith(compareBy({ it.createdAtEpochMillis }, { it.id }))
                val eventIndex = orderedEvents.indexOfFirst { it.id == editingRow.statusEvent?.id }
                ActivityRowEditor(
                    row = editingRow,
                    allEntries = session.progressUpdates,
                    sessionStartedAt = session.startedAt,
                    mediaType = mediaType,
                    progressTotal = itemTotal,
                    accent = accent,
                    // Only the newest transition shown decides where the session stands; the repository
                    // applies the same rule when deleting.
                    revertsSession = editingRow.statusEvent != null &&
                        rows.filter { it.statusEvent != null }.maxByOrNull { it.recordedAtEpochMillis }?.key == editingRow.key,
                    // The repository's bounds: the start and the dated transitions either side.
                    minimumDate = listOfNotNull(
                        session.startedAt,
                        orderedEvents.take(eventIndex.coerceAtLeast(0)).lastOrNull { it.hasKnownDate }?.occurredOn,
                    ).maxOrNull(),
                    maximumDate = orderedEvents.drop(eventIndex + 1).firstOrNull { it.hasKnownDate }?.occurredOn
                        .takeIf { eventIndex >= 0 },
                    onUpdateStatusEventDate = onUpdateStatusEventDate,
                    onDeleteStatusEvent = onDeleteStatusEvent,
                    onUpdateProgressUpdate = onUpdateProgressUpdate,
                    onDeleteProgressUpdate = onDeleteProgressUpdate,
                    onBack = { editingKey = null },
                    modifier = Modifier.navigationBarsPadding(),
                )
            } else {
                ActivityList(
                    session = session,
                    rows = rows,
                    mediaType = mediaType,
                    itemTotal = itemTotal,
                    accent = accent,
                    onRowClick = { editingKey = it.key },
                )
            }
        }
    }
}

private val SheetPageSpec = spring<IntOffset>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
private val SheetSizeSpec = spring<IntSize>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

@Composable
private fun ActivityList(
    session: TrackingSession,
    rows: List<SessionActivity>,
    mediaType: MediaType,
    itemTotal: Int?,
    accent: Color,
    onRowClick: (SessionActivity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = DetailGutter)) {
            DetailSectionTitle(
                text = stringResource(R.string.activity_title),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listOfNotNull(
                    pluralStringResource(R.plurals.activity_summary_rows, rows.size, rows.size),
                    session.startedAt?.let { stringResource(R.string.activity_window, it.formatActivityDate()) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }

        // The list keeps every vertical gesture, so the sheet is only ever closed from the top.
        //
        // A sheet drags on whatever its content leaves unconsumed, which would make a downward swipe
        // anywhere in the list a dismissal — including at the top of a long history, mid read.
        // Swallowing the leftovers here (this connection sits below the sheet's own, so it is offered
        // them first) leaves the handle and the heading as the only places that close it. Tapping the
        // scrim and the back gesture still work as they always do.
        val holdGestures = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                    available

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(top = 16.dp)
                .heightIn(max = 520.dp)
                .nestedScroll(holdGestures),
        ) {
            itemsIndexed(items = rows, key = { _, row -> row.key }) { index, row ->
                val previous = rows.getOrNull(index - 1)
                ActivityDiaryRow(
                    row = row,
                    // Rows removed, re-dated or reopened glide to their new place rather than cutting.
                    modifier = Modifier.animateItem(),
                    // A day names itself once; its later rows hang under it.
                    showDate = previous == null || previous.date != row.date,
                    isLast = index == rows.lastIndex,
                    mediaType = mediaType,
                    itemTotal = itemTotal,
                    ratingHalfPoints = session.ratingHalfPoints.takeIf { row.kind == SessionActivityKind.Completed },
                    accent = accent,
                    onClick = if (row.progress != null || row.statusEvent != null) {
                        { onRowClick(row) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** Where the bead sits: level with the middle of the row's first line. */
private val RowDotTop = 9.dp
private val RowVerticalPadding = 10.dp

/**
 * One line of the diary, hung off the Timeline's rail: the date in the gutter, a bead, and what
 * happened — plus, whenever progress is involved, how much and where it left the session.
 */
@Composable
private fun ActivityDiaryRow(
    row: SessionActivity,
    showDate: Boolean,
    isLast: Boolean,
    mediaType: MediaType,
    itemTotal: Int?,
    ratingHalfPoints: Int?,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isEntry = row.kind == SessionActivityKind.Progress
    val update = row.progress
    val amountText = update?.let {
        stringResource(R.string.activity_amount, it.amount, progressUnitLabel(mediaType = mediaType, value = it.amount))
    }
    val totalText = row.runningTotal?.let { formatActivityTotal(it, itemTotal) }
    val label = if (isEntry) null else stringResource(row.kind.labelRes())
    val description = listOfNotNull(
        row.date?.formatActivityDate() ?: stringResource(R.string.activity_date_unknown),
        label,
        ratingHalfPoints?.let { stringResource(R.string.rating_value, formatRatingHalfPoints(it)) },
        amountText,
        totalText?.let { stringResource(R.string.activity_total_after, it) },
    ).joinToString(", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = DetailGutter - 8.dp, end = DetailGutter)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier
                .width(gutterWidth())
                .padding(top = RowVerticalPadding, end = 4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (showDate) {
                val gutter = row.date?.timelineGutterDate()
                    ?: TimelineGutterDate(lead = stringResource(R.string.activity_date_unknown))
                Text(
                    text = gutter.headline(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    softWrap = gutter.month == null,
                    maxLines = if (gutter.month == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                gutter.year?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        TimelineRail(
            dotColor = if (isEntry) OmnilogTheme.colors.appLine.copy(alpha = 1f) else row.kind.accent(accent),
            dotSize = if (isEntry) ProgressDotSize else MilestoneDotSize,
            haloWidth = DotHalo,
            haloColor = OmnilogTheme.colors.appPanel,
            dotTop = RowVerticalPadding + RowDotTop - (if (isEntry) ProgressDotSize else MilestoneDotSize) / 2 - DotHalo,
            stopAt = if (isLast) RowVerticalPadding + RowDotTop else null,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = RowVerticalPadding, bottom = RowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ratingHalfPoints?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RatingStars(halfPoints = rating, starSize = 15.dp, accent = accent)
                            Text(
                                text = formatRatingHalfPoints(rating),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = OmnilogTheme.colors.appInk,
                            )
                        }
                    }
                } else if (amountText != null) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                    )
                }
                // Present only on an entry marked as covering a period with a real span to name.
                row.window?.let { window ->
                    Text(
                        text = stringResource(R.string.activity_window, window.from.formatActivityDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (totalText != null) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // An entry already names its amount beside the rail; a milestone names it here.
                    if (!isEntry && amountText != null) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Labels
// ─────────────────────────────────────────────────────────────

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
internal fun SessionActivityKind.accent(sessionAccent: Color): Color = when (this) {
    SessionActivityKind.Started, SessionActivityKind.Progress -> sessionAccent
    SessionActivityKind.Paused -> OmnilogTheme.accents.Paused
    SessionActivityKind.Completed -> OmnilogTheme.accents.Completed
    SessionActivityKind.Dropped -> OmnilogTheme.accents.Dropped
    SessionActivityKind.Replanned -> OmnilogTheme.accents.Planned
    SessionActivityKind.Resumed, SessionActivityKind.Reopened -> OmnilogTheme.accents.InProgress
}

/** Follows the reader's locale rather than one hard-coded ordering. */
internal fun LocalDate.formatActivityDate(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
