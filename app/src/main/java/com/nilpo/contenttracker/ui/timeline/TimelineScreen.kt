package com.nilpo.contenttracker.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.timeline.TimelineDayGroup
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.core.timeline.TimelineFilters
import com.nilpo.contenttracker.core.timeline.TimelineMediaFilter
import com.nilpo.contenttracker.core.timeline.toSnapshot
import com.nilpo.contenttracker.ui.TimelineHeaderActions
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogDropdownChip
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * The library's diary: months as chapters, days in the gutter, one row per thing that happened.
 *
 * Tapping a row opens that session's Activitat over the screen through [sessionActivitySheet], so a
 * wrong entry is fixed where it was noticed rather than two screens away.
 */
@Composable
fun TimelineScreen(
    entries: List<TimelineEntry>,
    isLoading: Boolean,
    headerActions: TimelineHeaderActions,
    onBrowseLibrary: () -> Unit,
    sessionActivitySheet: @Composable (sessionId: Long, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaFilterName by rememberSaveable { mutableStateOf(TimelineMediaFilter.All.name) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var showConfiguration by rememberSaveable { mutableStateOf(false) }
    var openSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUndated by rememberSaveable { mutableStateOf(false) }
    headerActions.onSettingsRequested = { showConfiguration = true }
    val preferences = rememberTimelinePreferences()
    val visibility by rememberTimelineVisibility(preferences)
    val mediaFilter = TimelineMediaFilter.entries.firstOrNull { it.name == mediaFilterName }
        ?: TimelineMediaFilter.All

    val snapshot = remember(entries, mediaFilter, selectedYear, visibility) {
        entries.toSnapshot(
            TimelineFilters(
                media = mediaFilter,
                year = selectedYear,
                excludedMediaTypes = visibility.hiddenMediaTypes,
                historyMediaTypes = visibility.historyMediaTypes,
            ),
        )
    }
    // A month's totals count every entry, including the ones the history setting keeps off the list.
    val monthTotals = remember(entries, mediaFilter, selectedYear, visibility) {
        entries.toSnapshot(
            TimelineFilters(media = mediaFilter, year = selectedYear, excludedMediaTypes = visibility.hiddenMediaTypes),
        ).groups.filter { it.date != null }.groupBy { YearMonth.from(it.date) }
    }
    // Ordered newest first, as the snapshot is; groupBy keeps that order.
    val months = remember(snapshot) {
        snapshot.groups.filter { it.date != null }.groupBy { YearMonth.from(it.date) }
    }
    val undated = remember(snapshot) { snapshot.groups.firstOrNull { it.date == null }?.entries.orEmpty() }
    val visibleTypes = MediaType.entries.filter { visibility.isVisible(it) }
    val quietTypes = visibleTypes.filterNot { visibility.showsHistory(it) }
    val listState = rememberLazyListState()
    val hasFilters = mediaFilter != TimelineMediaFilter.All || selectedYear != null
    // The library has events, but the visibility settings hide every one of them.
    val isHiddenByConfiguration = entries.isNotEmpty() && snapshot.unfilteredEntryCount == 0
    val openSession: (TimelineEntry) -> Unit = { openSessionId = it.sessionId }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        when {
            isLoading -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OmnilogStatusPanel(
                    text = stringResource(R.string.timeline_loading),
                    accent = OmnilogTheme.accents.Dashboard,
                    showProgressIndicator = true,
                )
            }

            snapshot.unfilteredEntryCount == 0 -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OmnilogEmptyState(
                    title = stringResource(
                        if (isHiddenByConfiguration) R.string.timeline_hidden_empty_title else R.string.timeline_empty_title,
                    ),
                    body = stringResource(
                        if (isHiddenByConfiguration) R.string.timeline_hidden_empty_body else R.string.timeline_empty_body,
                    ),
                    accent = OmnilogTheme.accents.Dashboard,
                    primaryAction = EmptyStateAction(
                        label = stringResource(
                            if (isHiddenByConfiguration) R.string.timeline_settings_open else R.string.timeline_browse_library,
                        ),
                        onClick = if (isHiddenByConfiguration) {
                            { showConfiguration = true }
                        } else {
                            onBrowseLibrary
                        },
                    ),
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            ) {
                item(key = "timeline-header") {
                    Column(
                        modifier = Modifier.padding(horizontal = DetailGutter),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TimelineFilterBar(
                            selectedMedia = mediaFilter,
                            selectedYear = selectedYear,
                            availableYears = snapshot.availableYears,
                            onMediaSelected = { mediaFilterName = it.name },
                            onYearSelected = { selectedYear = it },
                        )
                        // Hidden progress used to leave no trace on the screen; say so, and where to change it.
                        if (quietTypes.isNotEmpty()) {
                            Text(
                                text = if (quietTypes.size == visibleTypes.size) {
                                    stringResource(R.string.timeline_history_hidden_all)
                                } else {
                                    stringResource(
                                        R.string.timeline_history_hidden_note,
                                        quietTypes.map { it.timelineSettingsLabel() }.joinToString(", "),
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showConfiguration = true }
                                    .padding(vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                    }
                }

                if (snapshot.entries.isEmpty() && hasFilters) {
                    item(key = "timeline-filtered-empty") {
                        OmnilogEmptyState(
                            title = stringResource(R.string.timeline_filtered_empty_title),
                            body = stringResource(R.string.timeline_filtered_empty_body),
                            accent = OmnilogTheme.accents.Dashboard,
                            primaryAction = EmptyStateAction(
                                label = stringResource(R.string.timeline_clear_filters),
                                onClick = {
                                    mediaFilterName = TimelineMediaFilter.All.name
                                    selectedYear = null
                                },
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        )
                    }
                } else {
                    val firstMonth = months.keys.firstOrNull()
                    months.forEach { (month, groups) ->
                        item(key = "month:$month") {
                            TimelineMonthHeader(
                                title = month.title(),
                                progressByUnit = monthTotals[month].orEmpty().monthProgress(),
                                completedCount = groups.sumOf { it.completedCount },
                                modifier = Modifier.animateItem(),
                                isFirst = month == firstMonth,
                            )
                        }
                        val monthEntries = groups.flatMap { it.entries }
                        itemsIndexed(monthEntries, key = { _, entry -> entry.stableKey }) { index, entry ->
                            TimelineDiaryRow(
                                entry = entry,
                                showDate = index == 0 || monthEntries[index - 1].date != entry.date,
                                isLast = index == monthEntries.lastIndex,
                                onClick = { openSession(entry) },
                                modifier = Modifier.animateItem(),
                                showProgress = visibility.showsHistory(entry.mediaType),
                            )
                        }
                    }

                    // Undated rows cannot sit on a day, so they wait folded at the end until opened and dated.
                    if (undated.isNotEmpty()) {
                        item(key = "undated") {
                            TimelineUndatedHeader(
                                count = undated.size,
                                expanded = showUndated,
                                onToggle = { showUndated = !showUndated },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        if (showUndated) {
                            itemsIndexed(undated, key = { _, entry -> entry.stableKey }) { index, entry ->
                                TimelineDiaryRow(
                                    entry = entry,
                                    showDate = false,
                                    isLast = index == undated.lastIndex,
                                    onClick = { openSession(entry) },
                                    modifier = Modifier.animateItem(),
                                    showProgress = visibility.showsHistory(entry.mediaType),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    openSessionId?.let { sessionId ->
        sessionActivitySheet(sessionId) { openSessionId = null }
    }

    if (showConfiguration) {
        TimelineConfigurationSheet(
            visibility = visibility,
            onVisibilityChange = preferences::writeTimelineVisibility,
            onDismiss = { showConfiguration = false },
        )
    }
}

@Composable
private fun TimelineUndatedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "undatedChevron")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .heightIn(min = 48.dp)
            .padding(start = DetailGutter, end = DetailGutter, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailSectionTitle(text = stringResource(R.string.timeline_unknown_date))
            Text(
                text = pluralStringResource(R.plurals.timeline_undated_count, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = OmnilogTheme.colors.appMuted,
            modifier = Modifier.rotate(rotation),
        )
    }
}

/** `Setembre`, or `Setembre de 2025` outside the current year. */
@Composable
private fun YearMonth.title(): String {
    val name = month.getDisplayName(TextStyle.FULL_STANDALONE, OmnilogLocale)
        .replaceFirstChar { it.titlecase(OmnilogLocale) }
    return if (year == LocalDate.now().year) name else stringResource(R.string.timeline_month_year, name, year)
}

/** Summed per unit, never across units; see `TimelineProgressUnit`. */
private fun List<TimelineDayGroup>.monthProgress() = flatMap { it.progressByUnit.entries }
    .groupBy({ it.key }, { it.value })
    .mapValues { (_, amounts) -> amounts.sum() }

/** Two dropdowns rather than chip rows: the year list grows without bound as the library ages. */
@Composable
private fun TimelineFilterBar(
    selectedMedia: TimelineMediaFilter,
    selectedYear: Int?,
    availableYears: List<Int>,
    onMediaSelected: (TimelineMediaFilter) -> Unit,
    onYearSelected: (Int?) -> Unit,
) {
    val yearOptions: List<Int?> = listOf(null) + availableYears

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OmnilogDropdownChip(
            selectedOption = selectedMedia,
            options = TimelineMediaFilter.entries,
            optionLabel = { it.label() },
            onOptionSelected = onMediaSelected,
            modifier = Modifier.weight(1f),
            isActive = { it != TimelineMediaFilter.All },
            optionColor = { it.accent() },
            optionIcon = { filter, tint ->
                Icon(
                    painter = painterResource(filter.dropdownIconResId),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tint,
                )
            },
        )
        OmnilogDropdownChip(
            selectedOption = selectedYear,
            options = yearOptions,
            optionLabel = { it?.toString() ?: stringResource(R.string.timeline_all_time) },
            onOptionSelected = onYearSelected,
            modifier = Modifier.weight(1f),
            isActive = { it != null },
            optionColor = { OmnilogTheme.accents.Dashboard },
        )
    }
}

@Composable
private fun TimelineMediaFilter.label(): String = when (this) {
    TimelineMediaFilter.All -> stringResource(R.string.timeline_filter_all)
    TimelineMediaFilter.Anime -> stringResource(R.string.nav_anime)
    TimelineMediaFilter.Books -> stringResource(R.string.nav_books)
    TimelineMediaFilter.MoviesAndTv -> stringResource(R.string.nav_movies_tv)
    TimelineMediaFilter.Games -> stringResource(R.string.nav_games)
}

@Composable
private fun TimelineMediaFilter.accent() = when (this) {
    TimelineMediaFilter.All -> OmnilogTheme.accents.Dashboard
    TimelineMediaFilter.Anime -> OmnilogTheme.accents.Anime
    TimelineMediaFilter.Books -> OmnilogTheme.accents.Books
    TimelineMediaFilter.MoviesAndTv -> OmnilogTheme.accents.Movie
    TimelineMediaFilter.Games -> OmnilogTheme.accents.Games
}

private val TimelineMediaFilter.dropdownIconResId: Int
    get() = when (this) {
        TimelineMediaFilter.All -> R.drawable.ic_group_items
        TimelineMediaFilter.Anime -> R.drawable.ic_nav_anime
        TimelineMediaFilter.Books -> R.drawable.ic_nav_books
        TimelineMediaFilter.MoviesAndTv -> R.drawable.ic_media_movie
        TimelineMediaFilter.Games -> R.drawable.ic_nav_games
    }
