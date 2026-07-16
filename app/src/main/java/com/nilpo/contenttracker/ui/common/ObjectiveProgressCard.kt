package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.canonicalUnit
import com.nilpo.contenttracker.core.model.pace
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Single objective card shared by the Profile section and the dashboard preview so the two
 * surfaces can never drift. Renders the pace visuals: an expected-progress tick on the bar,
 * a status chip, and a context-aware rate line.
 *
 * Pass [onClick] to make the whole card tappable (dashboard → open Profile). Pass [onEdit] and/or
 * [onDelete] to expose an overflow menu (Profile management).
 */
@Composable
fun ObjectiveProgressCard(
    progress: ObjectiveProgress,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val objective = progress.objective
    val pace = remember(progress, today) { progress.pace(today) }
    val accent = objective.mediaType.objectiveAccent()
    val fillColor = when (pace.status) {
        ObjectiveStatus.Completed -> OmnilogColors.Completed
        ObjectiveStatus.Missed -> OmnilogColors.AppLine
        else -> accent
    }
    val borderColor = when (pace.status) {
        ObjectiveStatus.Completed -> OmnilogColors.Completed.copy(alpha = 0.45f)
        else -> OmnilogColors.AppLine
    }
    val isActive = pace.status == ObjectiveStatus.Ahead ||
        pace.status == ObjectiveStatus.OnTrack ||
        pace.status == ObjectiveStatus.Behind

    val cardModifier = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(14.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                AccentBadge(objective.mediaType, accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = objectiveProgressLabel(progress),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppInk,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = objective.cardSubtitle(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                StatusChip(pace.status)
                if (onEdit != null || onDelete != null) {
                    ObjectiveOverflowMenu(onEdit = onEdit, onDelete = onDelete)
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Objectiu: ${objectiveTargetLabel(objective)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pace.status == ObjectiveStatus.Completed) OmnilogColors.Completed else OmnilogColors.AppInk,
                )
                Text(
                    text = "${(progress.percentage * 100).roundToInt()}%",
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 1.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pace.status == ObjectiveStatus.Completed) OmnilogColors.Completed else OmnilogColors.AppMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }

            ObjectiveProgressBar(
                fraction = progress.percentage,
                expectedFraction = pace.expectedFraction,
                fillColor = fillColor,
                showMarker = isActive,
            )

            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = pace.daysRemainingLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                    )
                    Text(
                        text = pace.deltaText(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = pace.status.paceColor(),
                    )
                }
            }
        }
    }
}

/**
 * One objective rendered as a chrome-less row for the dashboard's single objectives card (UX-17).
 *
 * Unlike [ObjectiveProgressCard] this draws no Surface of its own — the parent card provides the
 * panel — so several objectives stack without turning into a wall of cards. The ahead/behind text
 * hint is dropped here because the parent's header carries a status roll-up; pace survives as the
 * expected-progress tick on the bar and the status-coloured percentage.
 */
@Composable
fun ObjectiveSummaryRow(
    progress: ObjectiveProgress,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val objective = progress.objective
    val pace = remember(progress, today) { progress.pace(today) }
    val accent = objective.mediaType.objectiveAccent()
    val fillColor = when (pace.status) {
        ObjectiveStatus.Completed -> OmnilogColors.Completed
        ObjectiveStatus.Missed -> OmnilogColors.AppLine
        else -> accent
    }
    val isActive = pace.status == ObjectiveStatus.Ahead ||
        pace.status == ObjectiveStatus.OnTrack ||
        pace.status == ObjectiveStatus.Behind

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AccentBadge(objective.mediaType, accent, size = 24.dp)
            Text(
                // UX-01/UX-02: value and target together — never a bare percentage.
                text = objectiveProgressLabel(progress),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OmnilogColors.AppInk,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = "${(progress.percentage * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = pace.status.summaryPercentColor(),
            )
        }
        ObjectiveProgressBar(
            fraction = progress.percentage,
            expectedFraction = pace.expectedFraction,
            fillColor = fillColor,
            showMarker = isActive,
            height = 5.dp,
        )
    }
}

/** Pace status for this snapshot, for callers that need the status without the full pace object. */
fun ObjectiveProgress.paceStatus(today: LocalDate = LocalDate.now()): ObjectiveStatus =
    pace(today).status

/**
 * [size] is a floor, not a fixed box: the letter scales with the system font, so a hard `size()`
 * clips it to a sliver at large scales (UX-09). Padding lets the badge grow to fit instead.
 */
@Composable
private fun AccentBadge(mediaType: MediaType?, accent: Color, size: androidx.compose.ui.unit.Dp = 34.dp) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.16f),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = size, minHeight = size)
                .padding(horizontal = 5.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = mediaType.badgeLetter(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
        }
    }
}

@Composable
private fun StatusChip(status: ObjectiveStatus) {
    val (label, color, solid) = status.chipStyle()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (solid) color else color.copy(alpha = 0.16f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (solid) OmnilogColors.AppBackground else color,
        )
    }
}

@Composable
private fun ObjectiveOverflowMenu(onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Més opcions",
                tint = OmnilogColors.AppMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onEdit != null) {
                DropdownMenuItem(text = { Text("Edita") }, onClick = { expanded = false; onEdit() })
            }
            if (onDelete != null) {
                DropdownMenuItem(text = { Text("Elimina") }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}

@Composable
private fun ObjectiveProgressBar(
    fraction: Float,
    expectedFraction: Float,
    fillColor: Color,
    showMarker: Boolean,
    height: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(5.dp))
            .background(OmnilogColors.AppBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(fillColor),
        )
        if (showMarker) {
            // A zero-content box sized to the expected fraction; the tick sits at its trailing edge.
            Box(modifier = Modifier.fillMaxWidth(expectedFraction.coerceIn(0f, 1f))) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(OmnilogColors.AppInk),
                )
            }
        }
    }
}

private fun com.nilpo.contenttracker.core.model.ObjectivePace.daysRemainingLabel(): String = when {
    daysRemaining <= 0 -> "Últim dia"
    daysRemaining == 1 -> "1 dia restant"
    else -> "$daysRemaining dies restants"
}

/** How far ahead of, or behind, the expected pace — in the objective's own units. */
private fun com.nilpo.contenttracker.core.model.ObjectivePace.deltaText(): String = when {
    unitsVsExpected < 0 -> "${-unitsVsExpected} per recuperar"
    unitsVsExpected > 0 -> "${unitsVsExpected} per davant"
    else -> "Just al previst"
}

/**
 * Percentage colour for [ObjectiveSummaryRow]. Signals only the states worth signalling and keeps
 * full-contrast ink otherwise — this is the row's headline number, so the muted treatment
 * `paceColor` uses for supporting hints would under-serve it (UX-10).
 */
private fun ObjectiveStatus.summaryPercentColor(): Color = when (this) {
    ObjectiveStatus.Completed, ObjectiveStatus.Ahead -> OmnilogColors.Completed
    ObjectiveStatus.Behind -> OmnilogColors.Dashboard
    ObjectiveStatus.Missed -> OmnilogColors.AppMuted
    ObjectiveStatus.OnTrack -> OmnilogColors.AppInk
}

private fun ObjectiveStatus.paceColor(): Color = when (this) {
    ObjectiveStatus.Behind -> OmnilogColors.Dashboard
    ObjectiveStatus.Ahead -> OmnilogColors.Completed
    else -> OmnilogColors.AppMuted
}

private fun com.nilpo.contenttracker.core.model.ObjectivePace.hintColor(): Color = when (status) {
    ObjectiveStatus.Completed -> OmnilogColors.Completed
    ObjectiveStatus.Missed -> OmnilogColors.AppMuted
    else -> status.paceColor()
}

private fun ObjectiveStatus.chipStyle(): Triple<String, Color, Boolean> = when (this) {
    ObjectiveStatus.Completed -> Triple("Completat", OmnilogColors.Completed, true)
    ObjectiveStatus.Ahead -> Triple("Avançat", OmnilogColors.Completed, false)
    ObjectiveStatus.OnTrack -> Triple("Al dia", OmnilogColors.Completed, false)
    ObjectiveStatus.Behind -> Triple("Endarrerit", OmnilogColors.Dashboard, false)
    ObjectiveStatus.Missed -> Triple("No assolit", OmnilogColors.AppMuted, false)
}

private fun Objective.cardSubtitle(): String {
    val mediaLabel = objectiveMediaLabelFor(mediaType)
    return "$mediaLabel · ${rangeLabel()}"
}

private fun Objective.rangeLabel(): String {
    val monthEnd = startDate.withDayOfMonth(1).plusMonths(1).minusDays(1)
    val isWholeMonth = startDate.dayOfMonth == 1 && endDate == monthEnd
    val isWholeYear = startDate == startDate.withDayOfYear(1) &&
        endDate == startDate.withMonth(12).withDayOfMonth(31)
    return when {
        isWholeMonth -> startDate.month.getDisplayName(TextStyle.FULL, catalan)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(catalan) else it.toString() }
        isWholeYear -> startDate.year.toString()
        startDate.year == endDate.year ->
            "${startDate.format(shortDate)} – ${endDate.format(shortDate)}"
        else -> "${startDate.format(shortDateYear)} – ${endDate.format(shortDateYear)}"
    }
}

private val catalan = Locale("ca")
private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", catalan)
private val shortDateYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", catalan)


private fun MediaType?.badgeLetter(): String = when (this) {
    MediaType.Anime -> "A"
    MediaType.Book -> "L"
    MediaType.Movie -> "P"
    MediaType.TvShow -> "S"
    MediaType.Game -> "J"
    null -> "★"
}


private fun MediaType?.objectiveAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogColors.Anime
    MediaType.Book -> OmnilogColors.Books
    MediaType.Movie -> OmnilogColors.Dashboard
    MediaType.TvShow -> OmnilogColors.Tv
    MediaType.Game -> OmnilogColors.Games
    null -> OmnilogColors.Dashboard
}

data class ObjectivePresentation(
    val title: String,
    val mediaLabel: String,
    val unit: ObjectiveUnit,
)

fun objectivePresentation(objective: Objective): ObjectivePresentation {
    val unit = objective.canonicalUnit()
    return ObjectivePresentation(
        title = objectiveDisplayTitle(
            metric = objective.metric,
            mediaType = objective.mediaType,
            targetValue = objective.targetValue,
            unit = unit,
        ),
        mediaLabel = objectiveMediaLabelFor(objective.mediaType),
        unit = unit,
    )
}

fun objectiveDisplayTitle(
    metric: ObjectiveMetric,
    mediaType: MediaType?,
    targetValue: Int,
    unit: ObjectiveUnit,
): String = when (metric) {
    ObjectiveMetric.CompletedTitles -> "$targetValue ${completedTitleLabel(mediaType, targetValue)}"
    ObjectiveMetric.ProgressUnits -> "$targetValue ${objectiveUnitLabel(unit, targetValue)}"
}

fun objectiveProgressLabel(progress: ObjectiveProgress): String =
    "${progress.currentValue} de ${objectiveTargetLabel(progress.objective)}"

fun objectiveTargetLabel(objective: Objective): String = when (objective.metric) {
    ObjectiveMetric.CompletedTitles ->
        "${objective.targetValue} ${completedTitleUnitLabel(objective.mediaType, objective.targetValue)}"
    ObjectiveMetric.ProgressUnits ->
        "${objective.targetValue} ${objectiveUnitLabel(objective.canonicalUnit(), objective.targetValue)}"
}

fun objectiveUnitLabel(unit: ObjectiveUnit, value: Int): String = when (unit) {
    ObjectiveUnit.Titles -> if (value == 1) "títol" else "títols"
    ObjectiveUnit.Pages -> if (value == 1) "pàgina" else "pàgines"
    ObjectiveUnit.Episodes -> if (value == 1) "episodi" else "episodis"
    ObjectiveUnit.Minutes -> if (value == 1) "minut" else "minuts"
    ObjectiveUnit.Hours -> if (value == 1) "hora" else "hores"
}

fun objectiveMediaLabelFor(mediaType: MediaType?): String = when (mediaType) {
    MediaType.Anime -> "Anime"
    MediaType.Book -> "Llibres"
    MediaType.Movie -> "Pel·lícules"
    MediaType.TvShow -> "Sèries"
    MediaType.Game -> "Jocs"
    null -> "Tots els formats"
}

private fun completedTitleLabel(mediaType: MediaType?, value: Int): String = when (mediaType) {
    MediaType.Anime -> plural(value, "anime completat", "anime completats")
    MediaType.Book -> plural(value, "llibre completat", "llibres completats")
    MediaType.Movie -> plural(value, "pel·lícula completada", "pel·lícules completades")
    MediaType.TvShow -> plural(value, "sèrie completada", "sèries completades")
    MediaType.Game -> plural(value, "joc completat", "jocs completats")
    null -> plural(value, "títol completat", "títols completats")
}

private fun completedTitleUnitLabel(mediaType: MediaType?, value: Int): String = when (mediaType) {
    MediaType.Anime -> plural(value, "anime", "animes")
    MediaType.Book -> plural(value, "llibre", "llibres")
    MediaType.Movie -> plural(value, "pel\u00b7l\u00edcula", "pel\u00b7l\u00edcules")
    MediaType.TvShow -> plural(value, "s\u00e8rie", "s\u00e8ries")
    MediaType.Game -> plural(value, "joc", "jocs")
    null -> plural(value, "t\u00edtol", "t\u00edtols")
}

private fun plural(value: Int, singular: String, plural: String): String =
    if (value == 1) singular else plural
