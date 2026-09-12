package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.res.pluralStringResource
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
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
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

/**
 * A calendar-year overview in one full-width card: activity on top, goals as a row along the
 * bottom. Each part keeps its own destination.
 */
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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_rhythm_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
            )
            Box {
                // Not a TextButton: its 48dp minimum height pushed the card away from this header.
                // Compose still extends a small clickable's touch area to the 48dp minimum.
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { menuOpen = true }
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (year == today.year) stringResource(R.string.stats_period_this_year) else year.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = OmnilogTheme.colors.appInk)
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
        // One container rather than two tiles, so the module reads as a whole and the goals no longer
        // stretch to the chart's height. The Surface clips each part's ripple to the card's corners.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = OmnilogTheme.colors.appPanel,
        ) {
            Column {
                Column(
                    Modifier.fillMaxWidth().clickable(onClick = onStatsClick).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RhythmLead(snapshot)
                    RhythmChart(snapshot, year, today)
                    RhythmTotals(snapshot)
                }
                HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = OmnilogTheme.colors.appLine)
                RhythmGoals(yearGoals, today, onObjectivesClick)
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
    // Figure, change, and label side by side, to keep the card short.
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
private fun RhythmChart(snapshot: StatsSnapshot, year: Int, today: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    val buckets = (1..12).map { month ->
        snapshot.completionSessionsByMonth.firstOrNull { it.key == YearMonth.of(year, month).toString() }
    }
    val values = buckets.map { it?.value ?: 0 }
    val highest = values.maxOrNull() ?: 0
    val maximum = highest.coerceAtLeast(1)
    // Only the busiest month carries its number; the rest read against it.
    val peak = values.indexOf(highest).takeIf { highest > 0 }
    // Months still to come stay blank, so they don't read as months where nothing was finished.
    val lastMonth = if (year == today.year) today.monthValue else 12
    val colors = MediaType.entries.associateWith { it.objectiveAccent() }
    Column {
        // Columns stand on a shared baseline, wide enough at full card width to read as a chart
        // rather than a row of dots. A month with nothing finished is simply the bare baseline.
        // The minimum height keeps the card steady between years; large text may still grow it.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp),
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
                Column(
                    Modifier.weight(1f).clearAndSetSemantics { if (past) contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (index == peak) {
                        Text(
                            value.toString(),
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    if (past && value > 0) {
                        Column(
                            Modifier.width(14.dp).height((48f * value / maximum).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(OmnilogTheme.colors.appLine),
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
        Spacer(Modifier.height(6.dp))
        // Quarter marks only — a letter under every bar was cramped. Each is centred on its bar and may
        // overhang the empty neighbouring cells, so large text needs no separate layout.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..12).forEach { month ->
                if (month % 3 != 1) {
                    Spacer(Modifier.weight(1f))
                } else {
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

/** The goals as one line along the card's foot: `2 / 5 objectius · 1 endarrerit`. */
@Composable
private fun RhythmGoals(goals: List<ObjectiveProgress>, today: LocalDate, onClick: () -> Unit) {
    val accent = OmnilogTheme.accents.Completed
    val completed = goals.count { it.isComplete }
    val behind = goals.count { it.paceStatus(today) == ObjectiveStatus.Behind }
    val behindLabel = if (behind > 0) pluralStringResource(R.plurals.home_objectives_summary_behind, behind, behind) else null
    val emptyLabel = stringResource(R.string.home_rhythm_goals_empty)
    val goalsLabel = stringResource(R.string.home_rhythm_goals)
    val description = if (goals.isEmpty()) {
        emptyLabel
    } else {
        listOfNotNull(stringResource(R.string.home_rhythm_goals_description, completed, goals.size), behindLabel).joinToString(". ")
    }
    val ink = OmnilogTheme.colors.appInk
    val muted = OmnilogTheme.colors.appMuted
    // Ochre, not the olive the card already uses, so it reads as a warning.
    val warning = OmnilogTheme.accents.Paused
    // One Text so the line wraps as a sentence at large font scales instead of overflowing.
    val line = buildAnnotatedString {
        if (goals.isEmpty()) {
            withStyle(SpanStyle(color = muted)) { append(emptyLabel) }
        } else {
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) { append("$completed / ${goals.size}") }
            withStyle(SpanStyle(color = muted)) { append(" $goalsLabel") }
            behindLabel?.let {
                withStyle(SpanStyle(color = muted)) { append("  ·  ") }
                withStyle(SpanStyle(color = warning, fontWeight = FontWeight.SemiBold)) { append(it) }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_rhythm_goals),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accent,
        )
        Text(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = muted)
    }
}
