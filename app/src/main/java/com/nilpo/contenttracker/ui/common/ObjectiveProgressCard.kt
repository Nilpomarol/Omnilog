package com.nilpo.contenttracker.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.canonicalUnit
import com.nilpo.contenttracker.core.model.pace
import com.nilpo.contenttracker.ui.home.GoalRing
import com.nilpo.contenttracker.ui.home.StatusChip
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One objective as a list row held to the height of its progress ring: the ring with the format's
 * mark and the percentage inside, and beside it three lines that fill exactly that height — the goal
 * in serif, the period and the figure against the target, then the pace beside the list's status
 * chip. A hairline under the row, as under the library's.
 *
 * The whole row opens the objective's editor.
 */
@Composable
fun ObjectiveProgressRow(
    progress: ObjectiveProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val objective = progress.objective
    val pace = remember(progress, today) { progress.pace(today) }
    val accent = objective.mediaType.objectiveAccent()
    val (statusLabel, statusColor, _) = pace.status.chipStyle(OmnilogTheme.colors.appMuted)
    val isActive = pace.status == ObjectiveStatus.Ahead ||
        pace.status == ObjectiveStatus.OnTrack ||
        pace.status == ObjectiveStatus.Behind
    val dividerColor = OmnilogTheme.colors.appLine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), stroke)
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp)
            // The ring sets the height; the text spreads to fill it, and only grows it when the
            // system font scale leaves no other choice.
            .height(IntrinsicSize.Min)
            .heightIn(min = RingDiameter),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            GoalRing(progress.percentage, pace, accent, diameter = RingDiameter, strokeWidth = 7.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ObjectiveMediaIcon(
                    mediaType = objective.mediaType,
                    accent = if (pace.status == ObjectiveStatus.Missed) OmnilogTheme.colors.appMuted else accent,
                    size = 22.dp,
                )
                Text(
                    text = "${(progress.percentage * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = objectiveSentenceTitle(objective),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                        append(objective.rangeLabel())
                    }
                    append("  ·  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = OmnilogTheme.colors.appInk)) {
                        append(formatObjectiveNumber(progress.currentValue))
                    }
                    append(" de ${formatObjectiveNumber(objective.targetValue)} ${objectiveTargetUnitLabel(objective)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = objective.paceLine(progress, pace, isActive),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) pace.status.paceColor(OmnilogTheme.colors.appMuted) else OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(label = statusLabel, color = statusColor)
            }
        }
    }
}

private val RingDiameter = 84.dp

/** One line on the pace: ahead or behind and the days left while it runs, how it ended once it has. */
private fun Objective.paceLine(
    progress: ObjectiveProgress,
    pace: com.nilpo.contenttracker.core.model.ObjectivePace,
    isActive: Boolean,
): String {
    val remaining = targetValue - progress.currentValue
    return when {
        isActive -> "${pace.deltaText()} · ${pace.daysRemainingLabel()}"
        remaining < 0 -> "${formatObjectiveNumber(-remaining)} ${unitWord(-remaining)} de més"
        remaining == 0 -> "Just a l'objectiu"
        else -> "Van faltar ${formatObjectiveNumber(remaining)} ${unitWord(remaining)}"
    }
}

private fun Objective.unitWord(value: Int): String = objectiveTargetUnitLabel(copy(targetValue = value))

/** What the objective sets out to do, as a sentence: `Llegir 40 llibres`, `Veure 200 episodis de sèries`. */
fun objectiveSentenceTitle(objective: Objective): String {
    val option = ObjectiveUnitOption(objective.metric, objective.mediaType)
    return objectiveVerb(objective.mediaType).replaceFirstChar { it.titlecase(catalan) } + " " +
        objectiveSentenceAmount(option, objective.targetValue)
}

/** Pace status for this snapshot, for callers that need the status without the full pace object. */
fun ObjectiveProgress.paceStatus(today: LocalDate = LocalDate.now()): ObjectiveStatus =
    pace(today).status

/**
 * The objective's format, drawn as the same icon the bottom navigation uses for that media.
 *
 * Bare rather than boxed: the tinted rounded square the letter used to sit in was chrome the icon
 * does not need, and the accent now lives in the icon itself. A fixed [size] is safe here where it
 * was not for the letter — a vector does not grow with the system font scale, so it cannot clip
 * itself the way the text badge did (UX-09).
 *
 * Films and series each have their own glyph, since an objective names one media type and not the
 * pair the "Cinema i TV" section covers.
 */
@Composable
fun ObjectiveMediaIcon(mediaType: MediaType?, accent: Color, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val description = objectiveMediaLabelFor(mediaType)
    val iconRes = mediaType.navIconRes()
    if (iconRes == null) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = description,
            tint = accent,
            modifier = Modifier.size(size),
        )
    } else {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = accent,
            modifier = Modifier.size(size),
        )
    }
}

/** Null means "every format", which has no navigation icon — callers draw a star instead. */
@DrawableRes
private fun MediaType?.navIconRes(): Int? = when (this) {
    MediaType.Anime -> R.drawable.ic_nav_anime
    MediaType.Book -> R.drawable.ic_nav_books
    // Not the navigation bar's film strip: that tab covers films and series together, so it cannot
    // tell them apart. Wherever a single media type is named, the two need their own glyphs — side
    // by side in the profile they were distinguishable only by colour.
    MediaType.Movie -> R.drawable.ic_media_movie
    MediaType.TvShow -> R.drawable.ic_media_series
    MediaType.Game -> R.drawable.ic_nav_games
    null -> null
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

@Composable
@ReadOnlyComposable
private fun ObjectiveStatus.paceColor(muted: Color): Color = when (this) {
    ObjectiveStatus.Behind -> OmnilogTheme.accents.Paused
    ObjectiveStatus.Ahead -> OmnilogTheme.accents.Completed
    else -> muted
}

@Composable
@ReadOnlyComposable
internal fun ObjectiveStatus.chipStyle(muted: Color): Triple<String, Color, Boolean> = when (this) {
    ObjectiveStatus.Completed -> Triple("Completat", OmnilogTheme.accents.Completed, true)
    ObjectiveStatus.Ahead -> Triple("Avançat", OmnilogTheme.accents.Completed, false)
    ObjectiveStatus.OnTrack -> Triple("Al dia", OmnilogTheme.accents.InProgress, false)
    ObjectiveStatus.Behind -> Triple("Endarrerit", OmnilogTheme.accents.Paused, false)
    ObjectiveStatus.Missed -> Triple("No assolit", OmnilogTheme.accents.Dropped, false)
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


/** The media accent an objective is drawn in, shared by the profile card and the dashboard rings. */
@Composable
@ReadOnlyComposable
fun MediaType?.objectiveAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
    null -> OmnilogTheme.accents.Dashboard
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
    "${formatObjectiveNumber(progress.currentValue)} de ${formatObjectiveNumber(progress.objective.targetValue)} ${objectiveTargetUnitLabel(progress.objective)}"

fun objectiveTargetLabel(objective: Objective): String =
    "${objective.targetValue} ${objectiveTargetUnitLabel(objective)}"

/** The unit a goal counts in, pluralised for its target: `llibres`, `pàgines`. */
fun objectiveTargetUnitLabel(objective: Objective): String = when (objective.metric) {
    ObjectiveMetric.CompletedTitles -> completedTitleUnitLabel(objective.mediaType, objective.targetValue)
    ObjectiveMetric.ProgressUnits -> objectiveUnitLabel(objective.canonicalUnit(), objective.targetValue)
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
