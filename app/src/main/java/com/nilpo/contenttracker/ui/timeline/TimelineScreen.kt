package com.nilpo.contenttracker.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.core.timeline.TimelineEntryKind
import com.nilpo.contenttracker.core.timeline.TimelineFilters
import com.nilpo.contenttracker.core.timeline.TimelineMediaFilter
import com.nilpo.contenttracker.core.timeline.toRecap
import com.nilpo.contenttracker.core.timeline.toSnapshot
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogDropdownChip
import com.nilpo.contenttracker.ui.common.OmnilogEmptyState
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

@Composable
fun TimelineScreen(
    entries: List<TimelineEntry>,
    isLoading: Boolean,
    onMediaClick: (Long) -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaFilterName by rememberSaveable { mutableStateOf(TimelineMediaFilter.All.name) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var showConfiguration by rememberSaveable { mutableStateOf(false) }
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
    // Keyed on the year as well as the snapshot: with none chosen the recap falls back to the
    // current year, and the snapshot alone cannot say whether a year was asked for.
    val recap = remember(snapshot, selectedYear) {
        snapshot.entries.toRecap(year = selectedYear, today = LocalDate.now())
    }
    val listState = rememberLazyListState()
    val hasFilters = mediaFilter != TimelineMediaFilter.All || selectedYear != null
    // The library has events, but the visibility settings hide every one of them.
    val isHiddenByConfiguration = entries.isNotEmpty() && snapshot.unfilteredEntryCount == 0

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        when {
            isLoading -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimelinePageHeading(onConfigure = { showConfiguration = true })
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
                TimelinePageHeading(onConfigure = { showConfiguration = true })
                OmnilogEmptyState(
                    title = stringResource(
                        if (isHiddenByConfiguration) {
                            R.string.timeline_hidden_empty_title
                        } else {
                            R.string.timeline_empty_title
                        },
                    ),
                    body = stringResource(
                        if (isHiddenByConfiguration) {
                            R.string.timeline_hidden_empty_body
                        } else {
                            R.string.timeline_empty_body
                        },
                    ),
                    accent = OmnilogTheme.accents.Dashboard,
                    primaryAction = EmptyStateAction(
                        label = stringResource(
                            if (isHiddenByConfiguration) {
                                R.string.timeline_settings_open
                            } else {
                                R.string.timeline_browse_library
                            },
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            ) {
                item(key = "timeline-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimelinePageHeading(onConfigure = { showConfiguration = true })
                        TimelineFilterBar(
                            selectedMedia = mediaFilter,
                            selectedYear = selectedYear,
                            availableYears = snapshot.availableYears,
                            onMediaSelected = { mediaFilterName = it.name },
                            onYearSelected = { selectedYear = it },
                        )
                        if (recap.hasContent) {
                            TimelineRecapCard(recap = recap, onMediaClick = onMediaClick)
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
                            modifier = Modifier.padding(top = 18.dp),
                        )
                    }
                } else {
                    val lastEntryKey = snapshot.entries.lastOrNull()?.stableKey
                    snapshot.groups.forEach { group ->
                        item(key = "day:${group.date ?: "unknown"}") {
                            TimelineDayNode(
                                label = group.date?.timelineGutterDate()
                                    ?: TimelineGutterDate(
                                        lead = stringResource(R.string.timeline_unknown_date),
                                    ),
                                progressByUnit = group.progressByUnit,
                                completedCount = group.completedCount,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        itemsIndexed(
                            group.entries,
                            key = { _, entry -> entry.stableKey },
                        ) { index, entry ->
                            TimelineCardRow(
                                entry = entry,
                                onClick = { onMediaClick(entry.mediaItemId) },
                                // Only the final card in the whole list truncates the rail; within
                                // a day the line must carry on to the next day's node.
                                isLast = entry.stableKey == lastEntryKey,
                                // Null on the first entry of a day, which is what tells the row it
                                // has a day node above it rather than a sibling.
                                previousKind = group.entries.getOrNull(index - 1)?.kind,
                            )
                        }
                    }
                }
            }
        }
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
private fun TimelinePageHeading(onConfigure: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.timeline_title),
            modifier = Modifier.weight(1f),
            color = OmnilogTheme.colors.appInk,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        IconButton(onClick = onConfigure) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.timeline_settings_open),
                tint = OmnilogTheme.accents.Dashboard,
            )
        }
    }
}

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

