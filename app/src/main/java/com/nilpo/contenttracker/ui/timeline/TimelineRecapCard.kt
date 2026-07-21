package com.nilpo.contenttracker.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.timeline.TimelineRecap
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.format.TextStyle

private const val CoverAspect = 2f / 3f
private val CoverGap = 5.dp

/**
 * How many covers a card's width holds — deliberately not a whole number.
 *
 * The half slot is the scroll affordance: a cover clipped by the card's edge is what says the row
 * continues. Derived from the measured width rather than fixed in dp so that half lands on every
 * screen size, instead of a fixed cover happening to end flush on some of them and hiding that there
 * is more to see.
 */
private const val CoversPerWidth = 5.5f

private val BucketHeight = 8.dp

/** The floor an empty bucket is drawn at — visible as a track, not as activity. */
private const val EmptyBucketAlpha = 0.10f
private const val MinFilledAlpha = 0.28f

/**
 * Every third cell carries its month. Twelve captions under twelve cells is more type than the
 * graphic is worth, and a quarterly anchor is enough to locate any cell by counting from it.
 *
 * Indexed rather than keyed on the calendar, so it lands on the same rhythm whether the strip starts
 * in January or eleven months back from today.
 */
private const val LabelEveryNthBucket = 3

/**
 * What the filters currently select, summarised — a recap rather than a status readout.
 *
 * Replaces a panel that showed a rolling three-week heatmap regardless of the year chosen above it.
 * The strip switches between the months of one year and the years of the whole library, so the
 * graphic always matches the period's own granularity, and the covers do the summarising because a
 * finished title is a thing you recognise rather than a number you parse.
 */
@Composable
fun TimelineRecapCard(
    recap: TimelineRecap,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = OmnilogTheme.accents.Dashboard

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecapHeadline(recap = recap, accent = accent)
            RecapStrip(recap = recap, accent = accent)

            if (recap.completedCount > 0) {
                if (recap.completed.isNotEmpty()) {
                    RecapCovers(recap = recap, onMediaClick = onMediaClick)
                }
                RecapMix(recap = recap)
            } else {
                Text(
                    text = if (recap.inProgressCount > 0) {
                        pluralStringResource(
                            R.plurals.timeline_recap_empty_active,
                            recap.inProgressCount,
                            recap.inProgressCount,
                        )
                    } else {
                        stringResource(R.string.timeline_recap_empty)
                    },
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RecapHeadline(recap: TimelineRecap, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = recap.completedCount.toString(),
            color = OmnilogTheme.colors.appInk,
            // headlineMedium's 28sp overshot the mock by 4sp, and in a card this size the figure was
            // reading as a page heading rather than as one number among several.
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = pluralStringResource(R.plurals.timeline_recap_completed, recap.completedCount),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 3.dp),
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        // The period, stated where the year filter's own chip sits above it.
        Text(
            text = recap.periodLabel(),
            modifier = Modifier.padding(bottom = 3.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/**
 * Twelve months, or one cell per year of the library.
 *
 * Intensity is scaled against the busiest bucket in view rather than a fixed threshold, so a quiet
 * year still shows a shape instead of twelve near-empty cells.
 */
@Composable
private fun RecapStrip(recap: TimelineRecap, accent: Color) {
    if (recap.buckets.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            recap.buckets.forEach { bucket ->
                val alpha = when {
                    bucket.count == 0 || recap.busiestBucket <= 0 -> EmptyBucketAlpha
                    else -> MinFilledAlpha +
                        (1f - MinFilledAlpha) * (bucket.count.toFloat() / recap.busiestBucket)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(BucketHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.copy(alpha = alpha)),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            recap.bucketLabels().forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Every finished title in the period, as a scrolling row of covers.
 *
 * Lazy on purpose: a well-used year can hold a hundred titles, and composing them all to draw six
 * would make the whole timeline scroll badly for a row most of which is off-screen.
 *
 * Keyed by media item id so a filter change re-uses the covers a period has in common with the last
 * one instead of reloading every image.
 */
@Composable
private fun RecapCovers(recap: TimelineRecap, onMediaClick: (Long) -> Unit) {
    val shape = RoundedCornerShape(4.dp)

    BoxWithConstraints {
        // The gaps live between the covers, so there is one fewer of them than there are covers.
        val coverWidth = (maxWidth - CoverGap * (CoversPerWidth - 1)) / CoversPerWidth

        LazyRow(horizontalArrangement = Arrangement.spacedBy(CoverGap)) {
            items(recap.completed, key = { it.mediaItemId }) { item ->
                MetadataCoverImage(
                    coverUrl = item.coverUrl,
                    modifier = Modifier
                        .width(coverWidth)
                        .aspectRatio(CoverAspect)
                        // Clipped before the click so the ripple follows the cover's corners rather
                        // than the square its bounds describe.
                        .clip(shape)
                        .clickable { onMediaClick(item.mediaItemId) }
                        // The cover carries no text, so without this the row reads as a run of
                        // unlabelled buttons to a screen reader.
                        .semantics { contentDescription = item.title },
                    shape = shape,
                )
            }
        }
    }
}

/**
 * How many titles of each medium, not how many pages or hours.
 *
 * Counting items is what makes the line comparable: `8 llibres` against `3 jocs` is a real
 * comparison, where `412 pàgines` against `9 hores` only looks like one.
 */
@Composable
private fun RecapMix(recap: TimelineRecap) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        recap.countsByType.forEach { entry ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(entry.type.recapAccent()),
                )
                Text(
                    text = entry.type.countLabel(entry.count),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** `2025` — the filtered year, or the current one when the filter is showing everything. */
private fun TimelineRecap.periodLabel(): String = year.toString()

/**
 * One label per bucket, blank for the ones left unlabelled.
 *
 * Returned at full length rather than as a sparse map so the labels can share the strip's own
 * weights and stay aligned under their cells without a second measuring pass.
 */
@Composable
private fun TimelineRecap.bucketLabels(): List<String> = buckets.mapIndexed { index, bucket ->
    if (index % LabelEveryNthBucket == 0) {
        bucket.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, OmnilogLocale)
    } else {
        ""
    }
}

@Composable
private fun MediaType.countLabel(count: Int): String = pluralStringResource(
    when (this) {
        MediaType.Anime -> R.plurals.timeline_recap_type_anime
        MediaType.Book -> R.plurals.timeline_recap_type_book
        MediaType.Movie -> R.plurals.timeline_recap_type_movie
        MediaType.TvShow -> R.plurals.timeline_recap_type_tv_show
        MediaType.Game -> R.plurals.timeline_recap_type_game
    },
    count,
    count,
)

@Composable
private fun MediaType.recapAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
}
