package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectivePace
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.pace
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.ComparisonBasis
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.chipStyle
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.common.objectiveTargetUnitLabel
import com.nilpo.contenttracker.ui.common.objectiveProgressLabel
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.common.paceStatus
import androidx.compose.ui.text.style.TextOverflow
import com.nilpo.contenttracker.ui.stats.deltaChipColor
import com.nilpo.contenttracker.ui.profile.FormatCountStrip
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * A calendar-year overview: the year's goals as a scrolling strip of tiles, then, under a hairline, the
 * year's completions with a month chart and per-format totals. Each part keeps its own destination.
 */
@Composable
internal fun HomeRhythmCard(
    items: List<TrackedMedia>,
    objectives: List<ObjectiveProgress>,
    onStatsClick: () -> Unit,
    onObjectivesClick: (objectiveId: Long?) -> Unit,
) {
    val today = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var menuOpen by remember { mutableStateOf(false) }
    val years = remember(items, objectives, today.year) {
        (items.flatMap { it.sessions }.mapNotNull { it.finishedAt?.year } +
            objectives.map { it.objective.endDate.year } + today.year)
            .filter { it <= today.year }.distinct().sortedDescending()
    }
    val snapshot = remember(items, today, year) {
        StatsCalculator(today = today).calculate(items, StatsFilters(period = StatsPeriod.Year(year)))
    }
    // Goals that need attention lead the strip; settled ones trail it.
    val yearGoals = remember(objectives, today, year) {
        objectives.filter { it.objective.endDate.year == year }
            .sortedBy { GoalOrder.indexOf(it.paceStatus(today)) }
    }

    // The gutter belongs to each part rather than the whole, so the goal strip can run to the screen
    // edge like Home's carousels.
    val gutter = Modifier.padding(horizontal = DetailGutter)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(gutter, verticalAlignment = Alignment.CenterVertically) {
            DetailSectionTitle(
                text = stringResource(R.string.home_rhythm_title),
                modifier = Modifier.weight(1f),
            )
            Box {
                // The creator page's sort pill: an outline around the choice and the arrow that opens it.
                ArrangeButton(onClick = { menuOpen = true }, onClickLabel = stringResource(R.string.stats_period_this_year)) { content ->
                    Text(
                        if (year == today.year) stringResource(R.string.stats_period_this_year) else year.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp), tint = content)
                }
                OmnilogDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    years.forEach { option ->
                        OmnilogDropdownItem(
                            text = if (option == today.year) stringResource(R.string.stats_period_this_year) else option.toString(),
                            selected = option == year,
                            accent = OmnilogTheme.accents.Dashboard,
                            onClick = { year = option; menuOpen = false },
                        )
                    }
                }
            }
        }
        // A past year without goals has nothing to add, so only this year offers to create one.
        if (yearGoals.isNotEmpty() || year == today.year) {
            // Keyed on the year so switching years starts the strip from its first goal.
            key(year) { RhythmGoals(yearGoals, today, onObjectivesClick) }
        }
        // The year sits on the page like the editorial sections, set off by a hairline, not a panel.
        HorizontalDivider(modifier = gutter.padding(top = 8.dp), color = OmnilogTheme.colors.appLine)
        Column(
            gutter.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onStatsClick).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RhythmLead(snapshot)
            RhythmChart(snapshot, year, today)
            RhythmTotals(snapshot)
        }
    }
}

private val GoalOrder = listOf(
    ObjectiveStatus.Behind,
    ObjectiveStatus.OnTrack,
    ObjectiveStatus.Ahead,
    ObjectiveStatus.Completed,
    ObjectiveStatus.Missed,
)

/**
 * One square tile per goal on a free-scrolling strip. Several tiles share the width, so a strip that
 * runs past the edge shows a cut-off tile as its hint.
 */
@Composable
private fun RhythmGoals(goals: List<ObjectiveProgress>, today: LocalDate, onClick: (objectiveId: Long?) -> Unit) {
    if (goals.isEmpty()) {
        GoalTileSurface(Modifier.padding(horizontal = DetailGutter).fillMaxWidth(), { onClick(null) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_rhythm_goals), null, Modifier.size(22.dp), tint = OmnilogTheme.accents.Completed)
                Text(
                    stringResource(R.string.home_rhythm_goals_empty),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = OmnilogTheme.colors.appMuted)
            }
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = DetailGutter),
        horizontalArrangement = Arrangement.spacedBy(GoalTileSpacing),
    ) {
        items(goals, key = { it.objective.id }) { goal ->
            GoalTile(goal, today) { onClick(goal.objective.id) }
        }
    }
}

/**
 * A near-square tile: the format's mark at the centre of a pace-aware progress ring, and under it
 * the count beside what it counts — pàgines, títols, animes — in its colour. The pace shows in the ring itself, and screen
 * readers still get the status word.
 */
@Composable
private fun GoalTile(progress: ObjectiveProgress, today: LocalDate, onClick: () -> Unit) {
    val pace = remember(progress, today) { progress.pace(today) }
    val accent = progress.objective.mediaType.objectiveAccent()
    val locale = LocalConfiguration.current.locales[0]
    val statusLabel = pace.status.chipStyle(OmnilogTheme.colors.appMuted).first
    val description = "${objectiveProgressLabel(progress)}. $statusLabel"
    GoalTileSurface(Modifier.width(GoalTileWidth), onClick) {
        Column(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                GoalRing(progress.percentage, pace, accent, diameter = GoalRingDiameter, strokeWidth = 7.dp)
                val iconTint = if (pace.status == ObjectiveStatus.Missed) OmnilogTheme.colors.appMuted else accent
                ObjectiveMediaIcon(progress.objective.mediaType, iconTint, 28.dp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = OmnilogTheme.colors.appInk)) {
                        append("${compactCount(progress.currentValue, locale)}/${compactCount(progress.objective.targetValue, locale)}")
                    }
                    append("  ")
                    withStyle(SpanStyle(color = accent)) { append(objectiveTargetUnitLabel(progress.objective)) }
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val GoalRingDiameter = 84.dp

/**
 * The ring carries the pace the status word used to: when behind, a pale arc runs on to where
 * progress should be by today; a completed goal closes in the completed colour; a missed one greys.
 */
@Composable
internal fun GoalRing(
    fraction: Float,
    pace: ObjectivePace,
    accent: Color,
    diameter: androidx.compose.ui.unit.Dp = 64.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val track = OmnilogTheme.colors.appBackground
    val fill = when (pace.status) {
        ObjectiveStatus.Completed -> OmnilogTheme.accents.Completed
        ObjectiveStatus.Missed -> OmnilogTheme.colors.appMuted
        else -> accent
    }
    val shortfall = accent.copy(alpha = 0.3f)
    Canvas(Modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        fun arc(color: Color, sweep: Float) {
            if (sweep <= 0f) return
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * sweep.coerceAtMost(1f),
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        arc(track, 1f)
        if (pace.status == ObjectiveStatus.Behind) arc(shortfall, pace.expectedFraction)
        arc(fill, fraction)
    }
}

private val GoalTileWidth = 116.dp
private val GoalTileSpacing = 8.dp

/**
 * Shortens counts from a thousand up so `current/target` fits a tile: `8,5k`, `12k`. Rounds down, so
 * a goal never reads as met before it is.
 */
internal fun compactCount(value: Int, locale: Locale): String {
    if (value < 1000) return value.toString()
    val format = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (value < 10_000) 1 else 0
        roundingMode = RoundingMode.FLOOR
    }
    // BigDecimal, not a double: 8100 / 1000.0 is stored just under 8.1 and would floor to 8.
    return format.format(BigDecimal.valueOf(value.toLong(), 3)) + "k"
}

@Composable
private fun GoalTileSurface(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    // The Surface clips the ripple to the tile's corners.
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = OmnilogTheme.colors.appPanel) {
        Box(Modifier.clickable(onClick = onClick).padding(10.dp)) { content() }
    }
}

/** The year's total on its own line, and the comparison as a plain sentence under it. */
@Composable
private fun RhythmLead(snapshot: StatsSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                snapshot.completionSessions.toString(),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Text(
                stringResource(R.string.home_rhythm_completed),
                modifier = Modifier.alignByBaseline().padding(start = 6.dp).weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = OmnilogTheme.colors.appMuted)
        }
        RhythmComparison(snapshot)
    }
}

@Composable
internal fun RhythmComparison(snapshot: StatsSnapshot) {
    val delta = snapshot.deltas.completionSessions ?: return
    val against = when (val basis = snapshot.deltas.basis ?: return) {
        is ComparisonBasis.SamePeriodOfYear -> stringResource(R.string.home_rhythm_basis_same_period, basis.year)
        is ComparisonBasis.FullYear -> stringResource(R.string.home_rhythm_basis_year, basis.year)
        ComparisonBasis.Previous12Months -> stringResource(R.string.home_rhythm_basis_previous_12_months)
    }
    val sentence = when {
        delta > 0 -> stringResource(R.string.home_rhythm_delta_more, delta, against)
        delta < 0 -> stringResource(R.string.home_rhythm_delta_less, delta.absoluteValue, against)
        else -> stringResource(R.string.home_rhythm_delta_same, against)
    }
    val arrow = when {
        delta > 0 -> "▲ "
        delta < 0 -> "▼ "
        else -> "= "
    }
    val color = deltaChipColor(delta)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(arrow) }
            append(sentence)
        },
        // The arrow only restates the sentence, so screen readers get the words alone.
        modifier = Modifier.clearAndSetSemantics { contentDescription = sentence },
        style = MaterialTheme.typography.bodySmall,
        color = OmnilogTheme.colors.appMuted,
    )
}

@Composable
private fun RhythmChart(snapshot: StatsSnapshot, year: Int, today: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    val buckets = (1..12).map { month ->
        snapshot.completionSessionsByMonth.firstOrNull { it.key == YearMonth.of(year, month).toString() }
    }
    val values = buckets.map { it?.value ?: 0 }
    val maximum = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    // Months still to come stay blank, so they don't read as months where nothing was finished.
    val lastMonth = if (year == today.year) today.monthValue else 12
    val colors = MediaType.entries.associateWith { it.objectiveAccent() }
    Column(Modifier.fillMaxWidth()) {
        // Columns stand on a shared baseline. A month with nothing finished is simply the bare baseline.
        Row(
            Modifier.fillMaxWidth().height(ChartHeight),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { index, value ->
                val past = index < lastMonth
                val description = stringResource(
                    R.string.home_month_activity,
                    YearMonth.of(year, index + 1).month.getDisplayName(TextStyle.FULL, locale),
                    value,
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { if (past) contentDescription = description },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (past && value > 0) {
                        // A hairline gap between formats keeps neighbouring colours from blending.
                        Column(
                            Modifier.fillMaxWidth(0.85f).height((ChartHeight.value * value / maximum).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            buckets[index]?.segments?.filter { it.value > 0 }?.forEach { segment ->
                                Box(Modifier.fillMaxWidth().weight(segment.value.toFloat()).background(colors.getValue(segment.mediaType)))
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        Spacer(Modifier.height(4.dp))
        // A narrow initial under every bar; each may overhang its cell, so large text needs no separate layout.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..12).forEach { month ->
                Text(
                    YearMonth.of(year, month).month.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    modifier = Modifier.weight(1f).wrapContentWidth(unbounded = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (month > lastMonth) OmnilogTheme.colors.appMuted.copy(alpha = 0.45f) else OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private val ChartHeight = 64.dp

/** The year's completions per format, as the profile's Acabats counts them. */
@Composable
private fun RhythmTotals(snapshot: StatsSnapshot) {
    FormatCountStrip(
        counts = MediaType.entries.map { type ->
            type to (snapshot.mediumStats.firstOrNull { it.mediaType == type }?.completionSessionCount ?: 0)
        },
    )
}
