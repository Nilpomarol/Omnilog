package com.nilpo.contenttracker.ui.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val icon: ImageVector,
    val color: Color,
)

@Composable
fun sessionStateVisual(status: TrackingStatus): SessionStateVisual = when (status) {
    TrackingStatus.Planned -> SessionStateVisual(
        label = stringResource(R.string.status_planned),
        icon = Icons.Filled.Star,
        color = OmnilogTheme.accents.Planned,
    )
    TrackingStatus.InProgress -> SessionStateVisual(
        label = stringResource(R.string.status_in_progress),
        icon = Icons.Filled.PlayArrow,
        color = OmnilogTheme.accents.InProgress,
    )
    TrackingStatus.Completed -> SessionStateVisual(
        label = stringResource(R.string.status_completed),
        icon = Icons.Filled.CheckCircle,
        color = OmnilogTheme.accents.Completed,
    )
    TrackingStatus.Paused -> SessionStateVisual(
        label = stringResource(R.string.status_paused),
        icon = Icons.Filled.PlayArrow,
        color = OmnilogTheme.accents.Paused,
    )
    TrackingStatus.Dropped -> SessionStateVisual(
        label = stringResource(R.string.status_dropped),
        icon = Icons.Filled.Close,
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
        shape = RoundedCornerShape(7.dp),
        color = visual.color,
        contentColor = onStateColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = visual.icon, contentDescription = null, modifier = Modifier.size(12.dp))
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
 * When a session started and when it ended, stated plainly.
 *
 * These used to be `bodySmall` in the muted tone at the bottom of the card, below the notes — the
 * least prominent thing on a surface whose whole subject is a period of time. Here they are labelled
 * columns in ink, at a size that reads before the caption under the graphic.
 *
 * A missing date is an em-dash under its own label rather than a sentence. The old copy spelled it
 * out — "Acabat (data desconeguda)" — which spent a whole line apologising for a fact the shape of
 * the row conveys on its own. When neither date is known the row is dropped completely: the state
 * chip has already said the session is finished, so an entirely empty row would only be furniture.
 */
@Composable
fun SessionDatesRow(
    session: TrackingSession,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    trailing: String? = null,
) {
    val showFinished = session.finishedAt != null || session.status.endsSession
    if (session.startedAt == null && session.finishedAt == null) return

    val valueStyle = if (compact) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.titleMedium
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        DateColumn(
            label = stringResource(R.string.session_date_started),
            value = session.startedAt,
            valueStyle = valueStyle,
        )
        if (showFinished) {
            Text(
                text = "→",
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            DateColumn(
                label = stringResource(R.string.session_date_finished),
                value = session.finishedAt,
                valueStyle = valueStyle,
            )
        }
        trailing?.let { label ->
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogTheme.colors.appMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DateColumn(
    label: String,
    value: LocalDate?,
    valueStyle: androidx.compose.ui.text.TextStyle,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
        Text(
            text = value?.formatSessionDate() ?: MissingDateMark,
            style = valueStyle,
            fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal,
            color = if (value != null) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
    }
}

/** What an unrecorded date looks like. Not a sentence, and not a zero. */
private const val MissingDateMark = "—"

fun LocalDate.formatSessionDate(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

// ─────────────────────────────────────────────────────────────
// Recency
// ─────────────────────────────────────────────────────────────

/**
 * How long ago the session was last touched, in the coarsest unit that still says something.
 *
 * The card has carried `updatedAtEpochMillis` all along and only ever printed it as a date, which
 * answers a question nobody asks. Whether you have stalled is the useful reading, and that is a
 * distance rather than a point.
 */
@Composable
fun sessionRecencyLabel(session: TrackingSession): String? {
    val updated = session.updatedDate() ?: return null
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

fun TrackingSession.updatedDate(): LocalDate? {
    if (updatedAtEpochMillis <= 0L) return null
    return Instant.ofEpochMilli(updatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
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
