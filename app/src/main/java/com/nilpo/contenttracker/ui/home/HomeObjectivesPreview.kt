package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.pace
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.formatObjectiveNumber
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.common.paceStatus
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

// A hard four. Objectives past this are simply not drawn — no overflow ring — so the four that are
// shown get the full width of the row and can be large enough to read at a glance. The header's
// roll-up still counts every objective, so nothing is hidden without a trace.
private const val VisibleRings = 4
private val RingSize = 80.dp
private val RingStroke = 6.dp
private val RingIconSize = 28.dp

/**
 * UX-17: the dashboard's objectives preview is a single card, not one card per objective.
 *
 * The card is a strip of rings rather than a list of rows, which is what separates it from the
 * Profile section: Profile is where objectives are read in detail and managed, the dashboard is a
 * glance. A strip also keeps the card's height fixed no matter how many objectives exist, where
 * stacked rows grew the card and pushed the rest of the dashboard down.
 *
 * Renders nothing when there are no objectives; the place to create one is Profile, which owns
 * that action.
 */
@Composable
fun DashboardObjectivesPreview(
    objectives: List<ObjectiveProgress>,
    onClick: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    if (objectives.isEmpty()) return

    // Worst first, so the objective needing attention is leftmost and is never the one truncated.
    val ordered = remember(objectives, today) {
        objectives.sortedBy { it.paceStatus(today).stripPriority() }
    }
    val visible = ordered.take(VisibleRings)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_objectives_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = objectives.statusSummary(today),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_objectives_open),
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogTheme.accents.Dashboard,
                )
            }

            // Cells share the width evenly rather than being fixed: the label needs whatever room
            // the row can spare to stay on one line, and that varies with screen width.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                visible.forEach { progress ->
                    ObjectiveRing(
                        progress = progress,
                        today = today,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * One objective as a ring: the arc is progress, the notch is where progress should be today, and
 * the icon inside names the format.
 *
 * The notch is the ring's version of the expected-progress tick the Profile card draws on its bar
 * — without it a ring says how far along you are but not whether that is good.
 */
@Composable
private fun ObjectiveRing(
    progress: ObjectiveProgress,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val objective = progress.objective
    val pace = remember(progress, today) { progress.pace(today) }
    val accent = objective.mediaType.objectiveAccent()
    val trackColor = OmnilogTheme.colors.appBackground
    val inkColor = OmnilogTheme.colors.appInk
    val lineColor = OmnilogTheme.colors.appLine
    val arcColor = when (pace.status) {
        ObjectiveStatus.Completed -> OmnilogTheme.accents.Completed
        ObjectiveStatus.Missed -> lineColor
        else -> accent
    }
    val isActive = pace.status == ObjectiveStatus.Ahead ||
        pace.status == ObjectiveStatus.OnTrack ||
        pace.status == ObjectiveStatus.Behind
    // A behind objective's notch sits ahead of its arc, so it carries the warning colour — it is
    // the gap between the two that the reader is being asked to notice.
    val notchColor = if (pace.status == ObjectiveStatus.Behind) OmnilogTheme.accents.Dashboard else inkColor
    val fraction = progress.percentage.coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(RingSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = RingStroke.toPx()
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(size.width - stroke, size.height - stroke)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                if (fraction > 0f) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                if (isActive) {
                    val angle = Math.toRadians((-90f + 360f * pace.expectedFraction).toDouble())
                    val radius = (size.width - stroke) / 2f
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    val inner = radius - stroke / 2f - 1.dp.toPx()
                    val outer = radius + stroke / 2f + 1.dp.toPx()
                    drawLine(
                        color = notchColor,
                        start = Offset(
                            centre.x + (inner * cos(angle)).toFloat(),
                            centre.y + (inner * sin(angle)).toFloat(),
                        ),
                        end = Offset(
                            centre.x + (outer * cos(angle)).toFloat(),
                            centre.y + (outer * sin(angle)).toFloat(),
                        ),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
            ObjectiveMediaIcon(
                mediaType = objective.mediaType,
                accent = arcColor,
                size = RingIconSize,
            )
        }

        // UX-01/UX-02: value and target, never a bare percentage — a ring already shows the
        // proportion, so the numbers are here to say what is actually being counted. One line, with
        // the current value carrying the weight so it still reads first.
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pace.status == ObjectiveStatus.Behind) {
                            OmnilogTheme.accents.Dashboard
                        } else {
                            inkColor
                        },
                    ),
                ) {
                    append(formatObjectiveNumber(progress.currentValue))
                }
                append(" de ${formatObjectiveNumber(objective.targetValue)}")
            },
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Behind first, finished last — the strip is ordered by how much attention each objective wants. */
private fun ObjectiveStatus.stripPriority(): Int = when (this) {
    ObjectiveStatus.Behind -> 0
    ObjectiveStatus.OnTrack -> 1
    ObjectiveStatus.Ahead -> 2
    ObjectiveStatus.Completed -> 3
    ObjectiveStatus.Missed -> 4
}

/**
 * Roll-up shown beside the card title, e.g. `2 al dia · 1 endarrerit`. Only non-zero groups appear,
 * so the line stays short enough to sit on one row next to the title.
 */
@Composable
private fun List<ObjectiveProgress>.statusSummary(today: LocalDate): String {
    val byStatus = groupingBy { it.paceStatus(today) }.eachCount()
    val onTrack = (byStatus[ObjectiveStatus.OnTrack] ?: 0) + (byStatus[ObjectiveStatus.Ahead] ?: 0)
    val behind = byStatus[ObjectiveStatus.Behind] ?: 0
    val completed = byStatus[ObjectiveStatus.Completed] ?: 0

    return listOfNotNull(
        onTrack.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_on_track, it, it)
        },
        behind.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_behind, it, it)
        },
        completed.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_completed, it, it)
        },
    ).joinToString(" · ")
}
