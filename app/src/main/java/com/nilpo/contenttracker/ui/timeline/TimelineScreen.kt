package com.nilpo.contenttracker.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.timeline.TimelineDayGroup
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.core.timeline.TimelineFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.contains
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
    // The Registre page's shared filters.
    mediaTypes: Set<MediaType>,
    period: StatsPeriod,
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfiguration by rememberSaveable { mutableStateOf(false) }
    var openSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUndated by rememberSaveable { mutableStateOf(false) }
    headerActions.onSettingsRequested = { showConfiguration = true }
    val preferences = rememberTimelinePreferences()
    val visibility by rememberTimelineVisibility(preferences)
    val scoped = remember(entries, mediaTypes, period) {
        entries.filter { it.mediaType in mediaTypes && period.contains(it.date) }
    }
    val snapshot = remember(scoped, visibility) {
        scoped.toSnapshot(
            TimelineFilters(
                excludedMediaTypes = visibility.hiddenMediaTypes,
                historyMediaTypes = visibility.historyMediaTypes,
            ),
        )
    }
    // Whether anything at all would show without the page's filters, which decides between "nothing
    // yet", "hidden by settings" and "nothing for these filters".
    val libraryCount = remember(entries, visibility) {
        entries.toSnapshot(
            TimelineFilters(excludedMediaTypes = visibility.hiddenMediaTypes, historyMediaTypes = visibility.historyMediaTypes),
        ).unfilteredEntryCount
    }
    // A month's totals count every entry, including the ones the history setting keeps off the list.
    val monthTotals = remember(scoped, visibility) {
        scoped.toSnapshot(TimelineFilters(excludedMediaTypes = visibility.hiddenMediaTypes))
            .groups.filter { it.date != null }.groupBy { YearMonth.from(it.date) }
    }
    // Ordered newest first, as the snapshot is; groupBy keeps that order.
    val months = remember(snapshot) {
        snapshot.groups.filter { it.date != null }.groupBy { YearMonth.from(it.date) }
    }
    val undated = remember(snapshot) { snapshot.groups.firstOrNull { it.date == null }?.entries.orEmpty() }
    val listState = rememberLazyListState()
    // The library has events, but the visibility settings hide every one of them.
    val isHiddenByConfiguration = entries.isNotEmpty() && libraryCount == 0
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

            libraryCount == 0 -> Column(
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
                        HiddenProgressNote(visibility = visibility, onOpenSettings = { showConfiguration = true })
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
                                onClick = onClearFilters,
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
internal fun List<TimelineDayGroup>.monthProgress() = flatMap { it.progressByUnit.entries }
    .groupBy({ it.key }, { it.value })
    .mapValues { (_, amounts) -> amounts.sum() }


/**
 * Says when progress entries are being hidden, and links to the setting. Without it, hidden progress
 * left no trace on the screen.
 */
@Composable
internal fun HiddenProgressNote(visibility: TimelineVisibility, onOpenSettings: () -> Unit) {
    val visibleTypes = MediaType.entries.filter { visibility.isVisible(it) }
    val quietTypes = visibleTypes.filterNot { visibility.showsHistory(it) }
    if (quietTypes.isEmpty()) return
    Text(
        text = if (quietTypes.size == visibleTypes.size) {
            stringResource(R.string.timeline_history_hidden_all)
        } else {
            stringResource(R.string.timeline_history_hidden_note, quietTypes.map { it.timelineSettingsLabel() }.joinToString(", "))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings)
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = OmnilogTheme.colors.appMuted,
    )
}
