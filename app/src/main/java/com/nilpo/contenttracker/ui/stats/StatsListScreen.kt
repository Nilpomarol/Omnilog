package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.home.MediaCard
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/** The lists a stats shelf can open in full. */
enum class StatsListKind(val titleRes: Int) {
    BestRated(R.string.stats_best_rated),
    Collections(R.string.stats_best_rated_collections),
    Creators(R.string.stats_top_creators),
    Revisited(R.string.stats_revisit_breakdown),
}

/**
 * Everything behind one of the stats page's shelves, for the same period and formats, in the same
 * order. Titles use the library's list row with the figure that ranked them at its end; creators and
 * collections, which have no cover of their own, lead with a small fan of their titles.
 */
@Composable
fun StatsListScreen(
    kind: StatsListKind,
    periodCode: String,
    mediaFilterName: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    onCreatorClick: (String, MediaType) -> Unit,
    onCollectionClick: (Long, MediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val period = statsPeriodFromCode(periodCode)
    val mediaFilter = StatsMediaFilter.entries.firstOrNull { it.name == mediaFilterName } ?: StatsMediaFilter.All
    val snapshot = remember(items, period, mediaFilter) {
        StatsCalculator(listLimit = Int.MAX_VALUE).calculate(items, StatsFilters(period = period, mediaTypes = mediaFilter.types))
    }
    val scope = listOfNotNull(period.label(), mediaFilter.takeIf { it != StatsMediaFilter.All }?.label()).joinToString(" · ")

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "scope") {
                Text(
                    text = scope,
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            when (kind) {
                StatsListKind.BestRated -> itemsIndexed(snapshot.bestRatedItems, key = { _, it -> it.trackedMedia.item.id }) { _, stat ->
                    MediaCard(
                        trackedMedia = stat.trackedMedia,
                        accent = stat.trackedMedia.item.type.statsColor(),
                        onClick = { onMediaClick(stat.trackedMedia) },
                        trailingAction = { TrailingFigure(stat.bestScore.roundedStatValue(), stat.trackedMedia.item.type.statsColor()) },
                    )
                }
                StatsListKind.Revisited -> itemsIndexed(snapshot.mostRevisitedItems, key = { _, it -> it.trackedMedia.item.id }) { _, stat ->
                    MediaCard(
                        trackedMedia = stat.trackedMedia,
                        accent = stat.trackedMedia.item.type.statsColor(),
                        onClick = { onMediaClick(stat.trackedMedia) },
                        trailingAction = {
                            TrailingFigure(stringResource(R.string.stats_revisit_value, stat.value), stat.trackedMedia.item.type.statsColor())
                        },
                    )
                }
                StatsListKind.Creators -> itemsIndexed(snapshot.creatorStats, key = { _, it -> it.name }) { index, stat ->
                    GroupRow(
                        covers = stat.coverUrls,
                        title = stat.name,
                        caption = if (stat.averageRating != null) {
                            stringResource(R.string.stats_creator_titles_and_rating, stat.completedTitles, stat.averageRating.roundedStatValue())
                        } else {
                            stringResource(R.string.stats_creator_titles_only, stat.completedTitles)
                        },
                        figure = stat.completedTitles.toString(),
                        accent = stat.mediaType.statsColor(),
                        showDivider = index > 0,
                        onClick = { onCreatorClick(stat.name, stat.mediaType) },
                    )
                }
                StatsListKind.Collections -> itemsIndexed(snapshot.bestRatedCollections, key = { _, it -> it.id }) { index, stat ->
                    GroupRow(
                        covers = stat.coverUrls,
                        title = stat.name,
                        caption = stringResource(R.string.stats_collection_rated_titles, stat.ratedTitles),
                        figure = stat.averageRating.roundedStatValue(),
                        accent = stat.mediaType.statsColor(),
                        showDivider = index > 0,
                        onClick = { onCollectionClick(stat.id, stat.mediaType) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailingFigure(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 8.dp),
        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
        color = color,
        maxLines = 1,
    )
}

/** A creator or collection: three overlapping covers, its name and what it holds, and its figure. */
@Composable
private fun GroupRow(
    covers: List<String>,
    title: String,
    caption: String,
    figure: String,
    accent: Color,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        if (showDivider) HorizontalDivider(color = OmnilogTheme.colors.appLine, modifier = Modifier.padding(bottom = 10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .clearAndSetSemantics { contentDescription = "$title, $figure. $caption" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val shown = covers.take(3)
            Box(modifier = Modifier.width(GroupCoverWidth + GroupCoverOffset * 2).height(GroupCoverHeight + 6.dp)) {
                shown.indices.reversed().forEach { index ->
                    MetadataCoverImage(
                        coverUrl = shown[index],
                        modifier = Modifier
                            .padding(start = GroupCoverOffset * index, top = 3.dp * index)
                            .width(GroupCoverWidth)
                            .height(GroupCoverHeight),
                        shape = RoundedCornerShape(5.dp),
                    )
                }
                if (shown.isEmpty()) {
                    Box(
                        Modifier
                            .width(GroupCoverWidth)
                            .height(GroupCoverHeight)
                            .background(accent.copy(alpha = 0.2f), RoundedCornerShape(5.dp)),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = caption, style = MaterialTheme.typography.bodySmall, color = OmnilogTheme.colors.appMuted, maxLines = 1)
            }
            TrailingFigure(figure, accent)
        }
    }
}

private val GroupCoverWidth = 50.dp
private val GroupCoverHeight = 75.dp
private val GroupCoverOffset = 14.dp

internal fun StatsPeriod.code(): String = when (this) {
    StatsPeriod.AllTime -> "all"
    StatsPeriod.ThisYear -> "year"
    StatsPeriod.Last12Months -> "12m"
    is StatsPeriod.Year -> year.toString()
}

internal fun statsPeriodFromCode(code: String): StatsPeriod = when (code) {
    "all" -> StatsPeriod.AllTime
    "12m" -> StatsPeriod.Last12Months
    "year" -> StatsPeriod.ThisYear
    else -> code.toIntOrNull()?.let { StatsPeriod.Year(it) } ?: StatsPeriod.ThisYear
}
