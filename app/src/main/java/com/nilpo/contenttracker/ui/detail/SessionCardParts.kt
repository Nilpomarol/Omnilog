package com.nilpo.contenttracker.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────────────────────
// State — one mapping, shared by every session surface
// ─────────────────────────────────────────────────────────────

/**
 * The label, mark and colour a tracking status is drawn with.
 *
 * One mapping rather than the two near-identical private copies the current session card and the
 * history rows each carried. They had already drifted: only one of them handled an unspecified accent.
 */
data class SessionStateVisual(
    val label: String,
    @DrawableRes val icon: Int,
    val color: Color,
)

/**
 * The same five marks every other status surface already draws — the home groups, the related-media
 * rows, the tracking-status picker. This one used to reach for generic Material glyphs instead
 * (`Icons.Filled.Star`, `PlayArrow`...), and `Paused` drew the identical `PlayArrow` triangle
 * `InProgress` does, so the one status that most needs to look different from "playing" looked
 * exactly like it.
 */
@Composable
fun sessionStateVisual(status: TrackingStatus): SessionStateVisual = when (status) {
    TrackingStatus.Planned -> SessionStateVisual(
        label = stringResource(R.string.status_planned),
        icon = R.drawable.ic_state_planned,
        color = OmnilogTheme.accents.Planned,
    )
    TrackingStatus.InProgress -> SessionStateVisual(
        label = stringResource(R.string.status_in_progress),
        icon = R.drawable.ic_state_in_progress,
        color = OmnilogTheme.accents.InProgress,
    )
    TrackingStatus.Completed -> SessionStateVisual(
        label = stringResource(R.string.status_completed),
        icon = R.drawable.ic_state_completed,
        color = OmnilogTheme.accents.Completed,
    )
    TrackingStatus.Paused -> SessionStateVisual(
        label = stringResource(R.string.status_paused),
        icon = R.drawable.ic_state_paused,
        color = OmnilogTheme.accents.Paused,
    )
    TrackingStatus.Dropped -> SessionStateVisual(
        label = stringResource(R.string.status_dropped),
        icon = R.drawable.ic_state_dropped,
        color = OmnilogTheme.accents.Dropped,
    )
}

/**
 * The state, filled rather than outlined.
 *
 * The old pill was the state colour at 14% behind a 32% border, on a card already outlined in the
 * same colour at 30% — three washes of one hue and no solid instance of it anywhere. Filling the chip
 * is what makes the state the loudest thing on the card, which is the point: the status is the single
 * fact the card exists to report.
 */
@Composable
fun SessionStateChip(
    visual: SessionStateVisual,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        // Fully rounded, like every other pill in the app — the title's ordinal badge, the genre
        // row, the collection chip. A 7dp corner was this card's own invention and read as a button
        // that had lost its label rather than as a piece of the same family.
        shape = RoundedCornerShape(percent = 50),
        color = visual.color,
        contentColor = onStateColor(),
    ) {
        Row(
            modifier = Modifier.padding(start = 9.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(visual.icon), contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/**
 * A quieter statement of the same fact, for the history cards.
 *
 * A filled chip on every past session would give five of them equal billing with the live one; the
 * dot keeps the colour coding and gives up the emphasis.
 */
@Composable
fun SessionStateDot(
    visual: SessionStateVisual,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = visual.color, shape = CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = visual.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Ink for text sitting on a filled state colour.
 *
 * The state accents are tuned to glow on charcoal in dark and to hold contrast as ink on paper in
 * light, so a chip filled with one needs the opposite tone on top in each theme. This mirrors what
 * the colour scheme already declares for its own filled surfaces.
 */
@Composable
private fun onStateColor(): Color = MaterialTheme.colorScheme.onPrimary

// ─────────────────────────────────────────────────────────────
// Dates
// ─────────────────────────────────────────────────────────────

/**
 * When the session started and when it ended, on one line.
 *
 * This was two labelled columns with the dates set at `titleMedium` — a deliberate promotion, on the
 * grounds that a card about a period of time should not bury its dates. It went too far. Two stacked
 * rows of uppercase caption over bold date, for two facts that are checked rather than read, cost
 * more of the card's height than anything else on it and made the card the tallest thing on the page.
 *
 * The form is the one the library rows already use — `Començat: 06/04/26 · Acabat: 22/07/26` — so the
 * same two facts now read the same way wherever you meet them, and a two-digit year is plenty inside
 * an app where every date is recent. The values keep ink against the muted labels, which is what is
 * left of the promotion and enough of it: they are still the only ink on the line.
 *
 * A missing date is an em-dash rather than a sentence; the old copy spelled out "Acabat (data
 * desconeguda)", which apologised at length for something the mark conveys. When neither date is
 * known the row drops out completely — the state chip has already said where the session stands.
 *
 * What the second label is called depends on what happened to the session — see [endLabel].
 */
@Composable
fun SessionDatesRow(
    session: TrackingSession,
    mediaType: MediaType,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    trailing: String? = null,
) {
    if (session.startedAt == null && session.finishedAt == null) return
    val showFinished = session.finishedAt != null || session.status.endsSession
    val style = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateFact(
            label = stringResource(R.string.session_date_started),
            value = session.startedAt,
            style = style,
        )
        if (showFinished) {
            Text(text = "·", style = style, color = OmnilogTheme.colors.appMuted)
            DateFact(
                label = endLabel(session = session, mediaType = mediaType),
                value = session.finishedAt,
                style = style,
            )
        }
        trailing?.let { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogTheme.colors.appMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

/** `Començat: 06/04/26` — the label muted, the date in ink, on one line. */
@Composable
private fun DateFact(label: String, value: LocalDate?, style: TextStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label:",
            style = style,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
        Text(
            text = value?.formatSessionDate() ?: MissingDateMark,
            style = style,
            fontWeight = FontWeight.Bold,
            color = if (value != null) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
    }
}

/**
 * What the second date is called, which is not always "Acabat".
 *
 * The column used to be labelled "Acabat" whatever had happened, so a paused session read as having
 * finished on the day you put it down and an abandoned one read as having been completed. The status
 * knows better, and this is the one place on the card where it can say so in a word.
 *
 * The one exception is a film seen in a single sitting: you *saw* it, you did not *complete* it, and
 * that is the one medium where "one sitting" is the normal case rather than the edge case. A book or
 * a game read or played in one go is rare enough that "Acabat" still fits it.
 */
@Composable
private fun endLabel(session: TrackingSession, mediaType: MediaType): String = stringResource(
    when {
        session.status == TrackingStatus.Paused -> R.string.session_date_paused
        session.status == TrackingStatus.Dropped -> R.string.session_date_dropped
        mediaType == MediaType.Movie && session.progressUpdates.size <= 1 -> R.string.session_date_watched
        else -> R.string.session_date_finished
    },
)

/** What an unrecorded date looks like. Not a sentence, and not a zero. */
private const val MissingDateMark = "—"

/** Displays '13 jul.' if it is the current year, otherwise '13 jul. 2025'. */
fun LocalDate.formatSessionDate(now: LocalDate = LocalDate.now()): String {
    val monthName = month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, OmnilogLocale)
    return if (year == now.year) {
        "$dayOfMonth $monthName"
    } else {
        "$dayOfMonth $monthName $year"
    }
}

// ─────────────────────────────────────────────────────────────
// Recency
// ─────────────────────────────────────────────────────────────

/**
 * How long ago the session was last touched, in the coarsest unit that still says something.
 *
 * The card used to read `updatedAtEpochMillis` for this, which is a bookkeeping timestamp rather
 * than a claim about the session: it moves on edits that are not activity — for instance a rating
 * change on an old, otherwise-untouched entry — so a session could read as "fa 2 dies" from a tap
 * that had nothing to do with progress. The finish date and the dated history — sittings, status
 * changes — are the things that actually happened, so the most recent of those is what counts.
 */
@Composable
fun sessionRecencyLabel(session: TrackingSession): String? {
    val updated = session.lastActivityDate() ?: return null
    val days = ChronoUnit.DAYS.between(updated, LocalDate.now())
    if (days < 0) return null

    return when {
        days == 0L -> stringResource(R.string.session_recency_today)
        days == 1L -> stringResource(R.string.session_recency_yesterday)
        days < 7L -> pluralStringResource(R.plurals.session_recency_days, days.toInt(), days.toInt())
        days < 28L -> {
            val weeks = (days / 7).toInt()
            pluralStringResource(R.plurals.session_recency_weeks, weeks, weeks)
        }
        else -> {
            val months = (days / 30).toInt().coerceAtLeast(1)
            pluralStringResource(R.plurals.session_recency_months, months, months)
        }
    }
}

/** The most recent date something actually happened: finishing, a sitting, or a status change. */
fun TrackingSession.lastActivityDate(): LocalDate? {
    val historyDates = progressUpdates.filter { it.hasKnownDate }.map { it.loggedAt } +
        statusEvents.map { it.occurredOn }
    return (historyDates + listOfNotNull(finishedAt)).maxOrNull()
}

// ─────────────────────────────────────────────────────────────
// Type-specific copy
// ─────────────────────────────────────────────────────────────

/**
 * What the card's one button offers, named for what is actually being recorded.
 *
 * A generic "Registra progrés" would be correct for every type and natural for none — you log
 * episodes, or pages, or hours, and the word for it is the fastest confirmation that the app
 * understands what it is holding.
 */
@Composable
fun logProgressLabel(mediaType: MediaType): String = stringResource(
    when (mediaType) {
        MediaType.Anime, MediaType.TvShow -> R.string.session_log_episodes
        MediaType.Book -> R.string.session_log_pages
        MediaType.Movie -> R.string.session_log_minutes
        MediaType.Game -> R.string.session_log_hours
    },
)
