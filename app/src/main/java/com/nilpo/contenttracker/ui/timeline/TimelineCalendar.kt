package com.nilpo.contenttracker.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.core.timeline.TimelineEntryKind
import com.nilpo.contenttracker.core.timeline.TimelineFilters
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.timeline.toSnapshot
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * The Registre page's calendar tab: the chronology as a month you can see at once.
 *
 * It shows milestones only — starts, finishes, pauses and revisits — since a day shows one cover and
 * those are what a month is remembered by. It follows the Registre page's format filter but not its
 * period: tapping the month title opens a month and year picker instead. Tapping a day lists its entries under the
 * calendar with Cronologia's own rows, which open the session's Activitat.
 */
@Composable
fun TimelineCalendarScreen(
    entries: List<TimelineEntry>,
    isLoading: Boolean,
    sessionActivitySheet: @Composable (sessionId: Long, onDismiss: () -> Unit) -> Unit,
    // The Registre page's format filter. There is no period here: the month picker moves through time.
    mediaTypes: Set<MediaType>,
    modifier: Modifier = Modifier,
) {
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var openSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay)
    val preferences = rememberTimelinePreferences()
    val visibility by rememberTimelineVisibility(preferences)
    // Milestones only. A day's square holds one cover, and a month of sittings would bury the days
    // something actually began or ended; the progress behind them stays a swipe away in Cronologia.
    val scoped = remember(entries, mediaTypes) {
        entries.filter { it.kind != TimelineEntryKind.Progress && it.mediaType in mediaTypes }
    }

    val snapshot = remember(scoped, visibility) {
        scoped.toSnapshot(
            TimelineFilters(
                excludedMediaTypes = visibility.hiddenMediaTypes,
            ),
        )
    }
    // A month's totals still count the sittings the grid leaves out: the days show milestones, but
    // "1793 pàgines" is what the month was actually worth.
    val allGroups = remember(entries, mediaTypes, visibility) {
        entries.filter { it.mediaType in mediaTypes }
            .toSnapshot(TimelineFilters(excludedMediaTypes = visibility.hiddenMediaTypes))
            .groups.filter { it.date != null }.groupBy { YearMonth.from(it.date) }
    }
    val entriesByDay = remember(snapshot) {
        snapshot.groups.mapNotNull { group -> group.date?.let { it to group.entries } }.toMap()
    }
    // From the first recorded month to now.
    val months = remember(entriesByDay) {
        val current = YearMonth.now()
        val first = entriesByDay.keys.minOrNull()?.let(YearMonth::from) ?: current
        generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(current) }.toList().ifEmpty { listOf(current) }
    }
    val dayEntries = selectedDate?.let { entriesByDay[it] }.orEmpty()

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        ) {
            when {
                isLoading -> item(key = "calendar-loading") {
                    OmnilogStatusPanel(
                        text = stringResource(R.string.timeline_loading),
                        accent = OmnilogTheme.accents.Dashboard,
                        showProgressIndicator = true,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                snapshot.unfilteredEntryCount == 0 -> item(key = "calendar-empty") {
                    OmnilogEmptyState(
                        title = stringResource(R.string.timeline_empty_title),
                        body = stringResource(R.string.timeline_empty_body),
                        accent = OmnilogTheme.accents.Dashboard,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    item(key = "calendar") {
                        // Keyed on the range, so a filter that changes the first month starts the pager afresh.
                        androidx.compose.runtime.key(months.first(), months.last()) {
                            MonthCalendar(
                                months = months,
                                entriesByDay = entriesByDay,
                                selectedDate = selectedDate,
                                onDateSelected = { selectedEpochDay = it?.toEpochDay() },
                                monthSummary = { month ->
                                    timelineMonthSummary(
                                        progressByUnit = allGroups[month].orEmpty().monthProgress(),
                                        completedCount = allGroups[month].orEmpty().sumOf { it.completedCount },
                                    )
                                },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                    item(key = "calendar-day") {
                        Text(
                            text = selectedDate?.takeIf { dayEntries.isNotEmpty() }?.calendarDayTitle()
                                ?: stringResource(R.string.timeline_calendar_pick_day),
                            modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 20.dp, bottom = 4.dp),
                            style = if (dayEntries.isNotEmpty()) {
                                MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal)
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (dayEntries.isNotEmpty()) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                        )
                    }
                    itemsIndexed(dayEntries, key = { _, entry -> entry.stableKey }) { index, entry ->
                        TimelineDiaryRow(
                            entry = entry,
                            showDate = false,
                            isLast = index == dayEntries.lastIndex,
                            onClick = { openSessionId = entry.sessionId },
                            modifier = Modifier.animateItem(),
                            showProgress = visibility.showsHistory(entry.mediaType),
                        )
                    }
                }
            }
        }
    }

    openSessionId?.let { sessionId -> sessionActivitySheet(sessionId) { openSessionId = null } }
}

/**
 * One page per month, swiped or stepped.
 *
 * A day with activity shows the cover of what mattered most that day, so the month reads as a wall of
 * what you were into rather than a grid of dots, plus a count when more than one title shared the day.
 *
 * [months] runs oldest to newest; the pager opens on [selectedDate]'s month, or the newest.
 */
@Composable
private fun MonthCalendar(
    months: List<YearMonth>,
    entriesByDay: Map<LocalDate, List<TimelineEntry>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    monthSummary: @Composable (YearMonth) -> String,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val initialPage = selectedDate?.let { months.indexOf(YearMonth.from(it)) }?.takeIf { it >= 0 } ?: months.lastIndex
    val pagerState = rememberPagerState(initialPage = initialPage) { months.size }
    val scope = rememberCoroutineScope()
    val currentOnDateSelected by rememberUpdatedState(onDateSelected)
    val currentSelection by rememberUpdatedState(selectedDate)
    val currentEntries by rememberUpdatedState(entriesByDay)

    // Landing on another month selects its latest active day, so the list under the calendar follows.
    LaunchedEffect(pagerState, months) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val month = months.getOrNull(page) ?: return@collect
            if (currentSelection?.let(YearMonth::from) != month) {
                currentOnDateSelected(currentEntries.keys.filter { YearMonth.from(it) == month }.maxOrNull())
            }
        }
    }
    // A day from the neighbouring month is selected first, then its month is paged in, so the
    // landing above keeps the chosen day rather than jumping to the month's latest.
    val selectOtherMonthDay: (LocalDate) -> Unit = { date ->
        val page = months.indexOf(YearMonth.from(date))
        if (page >= 0) {
            onDateSelected(date)
            scope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    val month = months.getOrNull(pagerState.currentPage) ?: return
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MonthHeader(
            month = month,
            summary = monthSummary(month),
            pagerState = pagerState,
            onStep = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            onPick = { showPicker = true },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailGutter),
            horizontalArrangement = Arrangement.spacedBy(CellSpacing),
        ) {
            (0 until 7).forEach { index ->
                Text(
                    text = firstDayOfWeek.plus(index.toLong()).getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { months[it].toString() },
            verticalAlignment = Alignment.Top,
        ) { page ->
            MonthGrid(
                month = months[page],
                firstDayOfWeek = firstDayOfWeek,
                entriesByDay = entriesByDay,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onOtherMonthDaySelected = selectOtherMonthDay,
            )
        }
    }

    if (showPicker) {
        MonthPickerSheet(
            months = months,
            current = month,
            activeDaysByMonth = remember(entriesByDay) { entriesByDay.keys.groupingBy { YearMonth.from(it) }.eachCount() },
            onPick = { picked ->
                showPicker = false
                months.indexOf(picked).takeIf { it >= 0 }?.let { page -> scope.launch { pagerState.scrollToPage(page) } }
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Jumps straight to a month: the year stepped with arrows, its twelve months below in a compact
 * four-by-three grid. A month outside your history is shown but cannot be chosen, and a month with
 * activity carries a dot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerSheet(
    months: List<YearMonth>,
    current: YearMonth,
    activeDaysByMonth: Map<YearMonth, Int>,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = OmnilogTheme.accents.Dashboard
    val firstYear = months.first().year
    val lastYear = months.last().year
    var year by rememberSaveable { mutableStateOf(current.year) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OmnilogTheme.colors.appPanel,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DetailGutter - 8.dp, end = DetailGutter - 8.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }, enabled = year > firstYear) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.timeline_calendar_previous_year))
                }
                Text(
                    text = year.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = OmnilogTheme.colors.appInk,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { year++ }, enabled = year < lastYear) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.timeline_calendar_next_year))
                }
            }
            // Four across, short names, and a dot for a month with activity: the whole year at a glance.
            (1..12).chunked(4).forEach { row ->
                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { monthNumber ->
                        val option = YearMonth.of(year, monthNumber)
                        val available = option in months
                        val isCurrent = option == current
                        val activeDays = activeDaysByMonth[option] ?: 0
                        val name = option.month.getDisplayName(TextStyle.SHORT_STANDALONE, OmnilogLocale)
                            .trimEnd('.')
                            .replaceFirstChar { it.titlecase(OmnilogLocale) }
                        val description = listOfNotNull(
                            option.month.getDisplayName(TextStyle.FULL_STANDALONE, OmnilogLocale),
                            activeDays.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.timeline_calendar_active_days, it, it) },
                        ).joinToString(", ")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) accent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable(enabled = available) { onPick(option) }
                                .graphicsLayer { alpha = if (available) 1f else 0.3f }
                                .clearAndSetSemantics {
                                    contentDescription = description
                                    selected = isCurrent
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) accent else OmnilogTheme.colors.appInk,
                                maxLines = 1,
                            )
                            if (activeDays > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp)
                                        .size(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isCurrent) accent else OmnilogTheme.colors.appMuted),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, summary: String, pagerState: PagerState, onStep: (Int) -> Unit, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter - 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onStep(pagerState.currentPage - 1) }, enabled = pagerState.currentPage > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.timeline_calendar_previous))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = stringResource(R.string.timeline_calendar_pick_month), onClick = onPick)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The title opens the month picker, and the arrow beside it says so.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = month.calendarTitle(),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                    color = OmnilogTheme.colors.appInk,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = OmnilogTheme.colors.appMuted,
                )
            }
            Text(
                text = summary.ifEmpty { stringResource(R.string.timeline_calendar_month_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        IconButton(onClick = { onStep(pagerState.currentPage + 1) }, enabled = pagerState.currentPage < pagerState.pageCount - 1) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.timeline_calendar_next))
        }
    }
}

private val CellSpacing = 4.dp

/**
 * The month in whole weeks: the first week starts on the locale's first day even when that falls in
 * the month before, and the last runs on into the month after. Those borrowed days are dimmed, and
 * tapping one pages to its month.
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    entriesByDay: Map<LocalDate, List<TimelineEntry>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onOtherMonthDaySelected: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val start = month.atDay(1).let { first -> first.minusDays(((first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong()) }
    val end = month.atEndOfMonth().let { last -> last.plusDays(((firstDayOfWeek.value + 6 - last.dayOfWeek.value) % 7).toLong()) }
    val weeks = ((end.toEpochDay() - start.toEpochDay() + 1) / 7).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter),
        verticalArrangement = Arrangement.spacedBy(CellSpacing),
    ) {
        (0 until weeks).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(CellSpacing)) {
                (0 until 7).forEach { weekday ->
                    val date = start.plusDays((week * 7 + weekday).toLong())
                    val inMonth = YearMonth.from(date) == month
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(CellAspect),
                    ) {
                        DayCell(
                            date = date,
                            entries = entriesByDay[date].orEmpty(),
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                            isSelected = inMonth && date == selectedDate,
                            isOutsideMonth = !inMonth,
                            onClick = { if (inMonth) onDateSelected(date) else onOtherMonthDaySelected(date) },
                        )
                    }
                }
            }
        }
    }
}

/** A little taller than square, so a day's cover keeps something of its poster shape. */
private const val CellAspect = 0.72f

@Composable
private fun DayCell(
    date: LocalDate,
    entries: List<TimelineEntry>,
    isToday: Boolean,
    isFuture: Boolean,
    isSelected: Boolean,
    isOutsideMonth: Boolean,
    onClick: () -> Unit,
) {
    val lead = entries.minByOrNull { it.kind.calendarRank }
    val titles = entries.distinctBy { it.mediaItemId }.size
    val shape = RoundedCornerShape(6.dp)
    val description = if (entries.isEmpty()) {
        date.dayOfMonth.toString()
    } else {
        listOfNotNull(
            date.timelineCompactDate(),
            pluralStringResource(R.plurals.timeline_calendar_day_titles, titles, titles),
            lead?.let { displayMediaTitle(it.mediaTitle) },
        ).joinToString(", ")
    }
    // Borrowed days and days still to come recede; the month's own days carry the page.
    val dim = when {
        isOutsideMonth -> 0.35f
        isFuture -> 0.45f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (lead != null) dim else 1f }
            .clip(shape)
            .then(if (entries.isNotEmpty()) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                when {
                    isSelected -> Modifier.border(BorderStroke(2.dp, OmnilogTheme.colors.appInk), shape)
                    lead == null -> Modifier.background(OmnilogTheme.colors.appPanel.copy(alpha = if (isOutsideMonth || isFuture) 0.3f else 0.7f))
                    else -> Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
            },
    ) {
        if (lead != null) {
            val coverShape = RoundedCornerShape(if (isSelected) 4.dp else 6.dp)
            MetadataCoverImage(
                coverUrl = lead.coverUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSelected) 3.dp else 0.dp)
                    .clip(coverShape),
                shape = coverShape,
            )
            // A soft shade behind the number, so it reads on any artwork.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
            )
            if (titles > 1) {
                Text(
                    text = "+${titles - 1}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 3.dp, end = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier
                .padding(start = 5.dp, top = 2.dp)
                .graphicsLayer { alpha = if (lead == null) dim else 1f },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color = when {
                lead != null -> Color.White
                isToday -> OmnilogTheme.accents.Dashboard
                else -> OmnilogTheme.colors.appMuted
            },
        )
    }
}

/** What a day's cover should be: the most telling thing that happened that day. */
private val TimelineEntryKind.calendarRank: Int
    get() = when (this) {
        TimelineEntryKind.Completion -> 0
        TimelineEntryKind.Dropped -> 1
        TimelineEntryKind.Start, TimelineEntryKind.Revisit -> 2
        TimelineEntryKind.Paused, TimelineEntryKind.Resumed -> 3
        TimelineEntryKind.Progress -> 4
    }

/** `Setembre de 2026`: the calendar pages across years, so it always names one. */
@Composable
private fun YearMonth.calendarTitle(): String {
    val name = month.getDisplayName(TextStyle.FULL_STANDALONE, OmnilogLocale).replaceFirstChar { it.titlecase(OmnilogLocale) }
    return stringResource(R.string.timeline_month_year, name, year)
}

/** `Dimecres, 10 de setembre`. */
private fun LocalDate.calendarDayTitle(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM", OmnilogLocale)).replaceFirstChar { it.titlecase(OmnilogLocale) }
