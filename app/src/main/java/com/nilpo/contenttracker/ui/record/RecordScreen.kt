package com.nilpo.contenttracker.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.stats.StatsMediaFilter
import com.nilpo.contenttracker.ui.stats.code
import com.nilpo.contenttracker.ui.stats.dropdownIconResId
import com.nilpo.contenttracker.ui.stats.label
import com.nilpo.contenttracker.ui.stats.statsPeriodFromCode
import com.nilpo.contenttracker.ui.stats.themedAccent
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

enum class RecordTab(val labelRes: Int) {
    Timeline(R.string.timeline_title),
    Calendar(R.string.timeline_calendar_title),
    Stats(R.string.stats_title),
}

/** What the Registre page is showing, shared by all three tabs. */
internal data class RecordFilters(
    val media: StatsMediaFilter,
    val period: StatsPeriod,
    val clear: () -> Unit,
) {
    val isFiltered: Boolean get() = media != StatsMediaFilter.All || period != StatsPeriod.AllTime
}

/**
 * Cronologia, Calendari and Estadístiques as tabs of one page: the same history, read as a diary, a
 * month at a glance, or figures. The pages swipe; [selectedTab] is hoisted so the top bar can show the
 * tab's own actions.
 *
 * The filters sit above the pages and belong to all three, so narrowing to one format or one year
 * carries across a swipe. The calendar has no use for a period, since its month picker moves through
 * time, so that word steps aside on that tab.
 */
@Composable
internal fun RecordScreen(
    selectedTab: RecordTab,
    onTabSelected: (RecordTab) -> Unit,
    periodOptions: List<StatsPeriod>,
    timeline: @Composable (RecordFilters) -> Unit,
    calendar: @Composable (RecordFilters) -> Unit,
    stats: @Composable (RecordFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaName by rememberSaveable { mutableStateOf(StatsMediaFilter.All.name) }
    var periodCode by rememberSaveable { mutableStateOf(StatsPeriod.ThisYear.code()) }
    val period = statsPeriodFromCode(periodCode).takeIf { it in periodOptions } ?: StatsPeriod.ThisYear
    val filters = RecordFilters(
        media = StatsMediaFilter.entries.firstOrNull { it.name == mediaName } ?: StatsMediaFilter.All,
        period = period,
        clear = {
            mediaName = StatsMediaFilter.All.name
            periodCode = StatsPeriod.AllTime.code()
        },
    )

    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { RecordTab.entries.size }
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentOnTabSelected(RecordTab.entries[it]) }
    }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) pagerState.animateScrollToPage(selectedTab.ordinal)
    }

    Column(modifier = modifier.background(OmnilogTheme.colors.appBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailGutter)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RecordTab.entries.forEach { tab ->
                RecordTabLabel(
                    text = stringResource(tab.labelRes),
                    selected = pagerState.currentPage == tab.ordinal,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        RecordFilterBar(
            filters = filters,
            periodOptions = periodOptions,
            // The calendar moves through time with its own month picker, so a period would fight it.
            showPeriod = pagerState.currentPage != RecordTab.Calendar.ordinal,
            onMediaSelected = { mediaName = it.name },
            onPeriodSelected = { periodCode = it.code() },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { RecordTab.entries[it].name },
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (RecordTab.entries[page]) {
                    RecordTab.Timeline -> timeline(filters)
                    RecordTab.Calendar -> calendar(filters)
                    RecordTab.Stats -> stats(filters)
                }
            }
        }
    }
}

/**
 * What, then when, written as two words you can tap rather than two controls.
 *
 * The old chips were a pair of outlined pills, which read as a form; here the filters are set in the
 * page's own type, with a caret to say they open, and take the media or dashboard colour once they
 * narrow anything. Both tabs and the calendar share them.
 */
@Composable
private fun RecordFilterBar(
    filters: RecordFilters,
    periodOptions: List<StatsPeriod>,
    showPeriod: Boolean,
    onMediaSelected: (StatsMediaFilter) -> Unit,
    onPeriodSelected: (StatsPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DetailGutter - 6.dp, end = DetailGutter, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterWord(
            label = filters.media.label(),
            active = filters.media != StatsMediaFilter.All,
            activeColor = filters.media.themedAccent(),
            leading = {
                Icon(
                    painter = painterResource(filters.media.dropdownIconResId),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = it,
                )
            },
        ) { dismiss ->
            StatsMediaFilter.entries.forEach { option ->
                OmnilogDropdownItem(
                    text = option.label(),
                    selected = option == filters.media,
                    accent = option.themedAccent(),
                    onClick = {
                        onMediaSelected(option)
                        dismiss()
                    },
                )
            }
        }
        AnimatedVisibility(visible = showPeriod, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "·",
                    modifier = Modifier.padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = OmnilogTheme.colors.appLine,
                )
                FilterWord(
                    label = filters.period.label(),
                    active = filters.period != StatsPeriod.ThisYear,
                    activeColor = OmnilogTheme.accents.Dashboard,
                ) { dismiss ->
                    periodOptions.forEach { option ->
                        OmnilogDropdownItem(
                            text = option.label(),
                            selected = option == filters.period,
                            accent = OmnilogTheme.accents.Dashboard,
                            onClick = {
                                onPeriodSelected(option)
                                dismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** One filter as a word with a caret, and its menu under it. */
@Composable
private fun FilterWord(
    label: String,
    active: Boolean,
    activeColor: Color,
    leading: (@Composable (Color) -> Unit)? = null,
    menu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val color = if (active) activeColor else OmnilogTheme.colors.appInk
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.DropdownList, onClick = { open = true })
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leading?.invoke(color)
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) activeColor else OmnilogTheme.colors.appMuted,
            )
        }
        OmnilogDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            menu { open = false }
        }
    }
}

/** A word with a rule under it when chosen: the page's type does the work, not a Material tab bar. */
@Composable
private fun RecordTabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val textColor by animateColorAsState(
        if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
        label = "recordTabText",
    )
    val ruleColor by animateColorAsState(
        if (selected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appBackground.copy(alpha = 0f),
        label = "recordTabRule",
    )
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(top = 8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ruleColor),
        )
    }
}
