package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.common.paceStatus
import androidx.compose.ui.text.style.TextOverflow
import com.nilpo.contenttracker.ui.stats.DeltaChip
import com.nilpo.contenttracker.ui.stats.comparisonBasisLabelShort
import com.nilpo.contenttracker.ui.stats.deltaChipColor
import com.nilpo.contenttracker.ui.stats.intMetricDelta
import com.nilpo.contenttracker.ui.stats.metricDescription
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/** A calendar-year overview, with independent destinations for activity and goals. */
@Composable
internal fun HomeRhythmCard(
    items: List<TrackedMedia>,
    objectives: List<ObjectiveProgress>,
    onStatsClick: () -> Unit,
    onObjectivesClick: () -> Unit,
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
    val yearGoals = objectives.filter { it.objective.endDate.year == year }
    val fontScale = LocalDensity.current.fontScale

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_rhythm_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
            )
            Box {
                TextButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(start = 8.dp)) {
                    Text(
                        if (year == today.year) stringResource(R.string.stats_period_this_year) else year.toString(),
                        color = OmnilogTheme.colors.appInk,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = OmnilogTheme.colors.appInk)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    years.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(if (option == today.year) stringResource(R.string.stats_period_this_year) else option.toString()) },
                            onClick = { year = option; menuOpen = false },
                        )
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Keep the goals beside the chart; reserve stacking for enlarged text.
            val wide = maxWidth >= 560.dp && fontScale <= 1.3f
            val stacked = maxWidth < 300.dp || fontScale > 1.3f
            val activity: @Composable (Modifier) -> Unit = { modifier ->
                Surface(
                    onClick = onStatsClick,
                    modifier = modifier,
                    shape = RoundedCornerShape(16.dp),
                    color = OmnilogTheme.colors.appPanel,
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RhythmLead(snapshot)
                        RhythmChart(snapshot, year, today, compact = fontScale > 1.3f)
                        RhythmTotals(snapshot)
                    }
                }
            }
            val goals: @Composable (Modifier) -> Unit = { modifier ->
                RhythmGoals(yearGoals, today, onObjectivesClick, modifier)
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activity(Modifier.fillMaxWidth())
                    goals(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activity(Modifier.weight(1f).fillMaxHeight())
                    goals(Modifier.width(if (wide) 164.dp else 124.dp).fillMaxHeight())
                }
            }
        }
    }
}

/** The year's completions, with the change against the comparable window of the year before. */
@Composable
private fun RhythmLead(snapshot: StatsSnapshot) {
    val value = snapshot.completionSessions.toString()
    val label = stringResource(R.string.home_rhythm_completed)
    val delta = intMetricDelta(snapshot.deltas.completionSessions)
    val basis = snapshot.deltas.basis?.takeIf { delta != null }?.let { comparisonBasisLabelShort(it) }
    val description = metricDescription(label, value, delta, basis)
    // Figure, change, and label side by side: stacking them stretched the goals tile beside it.
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = OmnilogTheme.colors.appInk, maxLines = 1)
        delta?.let { DeltaChip(text = it.text, color = deltaChipColor(it.value)) }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = OmnilogTheme.colors.appMuted, maxLines = 1)
            basis?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = OmnilogTheme.colors.appMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RhythmChart(snapshot: StatsSnapshot, year: Int, today: LocalDate, compact: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val buckets = (1..12).map { month ->
        snapshot.completionSessionsByMonth.firstOrNull { it.key == YearMonth.of(year, month).toString() }
    }
    val maximum = buckets.maxOf { it?.value ?: 0 }.coerceAtLeast(1)
    // Months still to come stay blank, so they don't read as months where nothing was finished.
    val lastMonth = if (year == today.year) today.monthValue else 12
    val colors = MediaType.entries.associateWith { it.objectiveAccent() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEachIndexed { index, bucket ->
                val value = bucket?.value ?: 0
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
                    if (past) {
                        Column(
                            Modifier.width(8.dp).height(if (value == 0) 2.dp else (40f * value / maximum).coerceAtLeast(5f).dp)
                                .clip(RoundedCornerShape(4.dp)).background(OmnilogTheme.colors.appLine),
                        ) {
                            bucket?.segments?.filter { it.value > 0 }?.forEach { segment ->
                                Box(Modifier.fillMaxWidth().weight(segment.value.toFloat()).background(colors.getValue(segment.mediaType)))
                            }
                        }
                    }
                }
            }
        }
        // On compact cards each label spans two bars, leaving all twelve bars visible.
        Row(Modifier.fillMaxWidth()) {
            (1..12 step if (compact) 2 else 1).forEach { month ->
                val current = year == today.year && month == today.monthValue
                Text(
                    YearMonth.of(year, month).month.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (current) FontWeight.Bold else null,
                    color = when {
                        current -> OmnilogTheme.colors.appInk
                        month > lastMonth -> OmnilogTheme.colors.appMuted.copy(alpha = 0.45f)
                        else -> OmnilogTheme.colors.appMuted
                    },
                    textAlign = if (compact) TextAlign.Start else TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RhythmTotals(snapshot: StatsSnapshot) {
    val formats = listOf(
        MediaType.Book to R.string.home_rhythm_books,
        MediaType.TvShow to R.string.home_rhythm_series,
        MediaType.Movie to R.string.home_rhythm_movies,
        MediaType.Anime to R.string.home_rhythm_anime,
        MediaType.Game to R.string.home_rhythm_games,
    )
    // One row, spread across the panel; wraps only when narrow screens or large text leave no room.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        formats.forEach { (type, label) ->
            val count = snapshot.mediumStats.firstOrNull { it.mediaType == type }?.completionSessionCount ?: 0
            // Preserve the four reference rows and include games whenever this year has any.
            if (type != MediaType.Game || count > 0) {
                val description = "$count ${stringResource(label)}"
                // Empty formats step back so the ones with activity carry the colour.
                val tint = if (count > 0) type.objectiveAccent() else OmnilogTheme.colors.appMuted.copy(alpha = 0.6f)
                Row(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ObjectiveMediaIcon(type, tint, 20.dp)
                    Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
                }
            }
        }
    }
}

@Composable
private fun RhythmGoals(goals: List<ObjectiveProgress>, today: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    val accent = OmnilogTheme.accents.Completed
    val completed = goals.count { it.isComplete }
    val behind = goals.count { it.paceStatus(today) == ObjectiveStatus.Behind }
    val behindLabel = if (behind > 0) pluralStringResource(R.plurals.home_objectives_summary_behind, behind, behind) else null
    val description = if (goals.isEmpty()) {
        stringResource(R.string.home_rhythm_goals_empty)
    } else {
        listOfNotNull(stringResource(R.string.home_rhythm_goals_description, completed, goals.size), behindLabel).joinToString(". ")
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.lerp(OmnilogTheme.colors.appPanel, accent, 0.18f),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 12.dp).clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_rhythm_goals),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = accent,
            )
            if (goals.isEmpty()) {
                // A bare "0 / 0" reads as a failure; invite a first goal instead.
                Text(
                    stringResource(R.string.home_rhythm_goals_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("$completed / ${goals.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = OmnilogTheme.colors.appInk)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_rhythm_goals), style = MaterialTheme.typography.bodySmall, color = OmnilogTheme.colors.appMuted)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = accent)
                }
                behindLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.accents.Dashboard,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
