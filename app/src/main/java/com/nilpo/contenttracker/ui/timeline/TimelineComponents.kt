package com.nilpo.contenttracker.ui.timeline

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.core.timeline.TimelineEntryKind
import com.nilpo.contenttracker.core.timeline.TimelineProgressUnit
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Sized to the widest single line the gutter can hold, at the base font scale.
 *
 * The worst case is not the longest month but the widest one: `30 MARÇ` beats `30 FEBR.`, because
 * `M` is a third wider than `F` and a `Ç` outweighs a full stop. Every letter of the abbreviation
 * has to be checked, not just counted.
 *
 * Every dp here is a dp of the card's width given away, so it is sized to that worst case and no
 * further — `12sp` of bold uppercase `30 MARÇ` plus [GutterHaloClearance]. The uppercase is what
 * sets the floor; it cost 6dp over the lowercase form.
 *
 * The year does not enter into this. It takes a line of its own, which costs nothing because the
 * gutter reports no height at all (see [TimelineDayNode]).
 */
private val DateGutterWidth = 58.dp

/**
 * How far the gutter's text stops short of the rail.
 *
 * Not decoration: the day bead is [DayDotSize] + two [DayDotHalo]s wide against a [RailWidth] rail,
 * so it overflows `(9 + 8 - 10) / 2 = 3.5dp` past its own column on each side. Anything closer than
 * that gets painted over by the halo's background disc.
 */
private val GutterHaloClearance = 4.dp

/**
 * Air for the uppercase standalone words only.
 *
 * A real date gets none: at 11sp in a 48dp column every dp of tracking is a dp closer to wrapping,
 * and `30 març` has no room to spare.
 */
private val GutterTracking = 0.4.sp

/**
 * The gutter, grown with the system font setting.
 *
 * A fixed dp width holds a fixed number of glyphs only at the base scale; at 1.3x the same `30 març`
 * is 30% wider and the column is not, so it truncates. Scaled the way `MediaCard` pins its own
 * height, and read by the day node and every card row alike so the two never disagree.
 */
@Composable
@ReadOnlyComposable
private fun gutterWidth(): Dp = DateGutterWidth * LocalDensity.current.fontScale
private val RailWidth = 10.dp
private val RailLineWidth = 2.dp

/**
 * Three sizes of bead, one neutral thread.
 *
 * The line used to take each entry's media accent, which turned a mixed day into a stack of
 * differently coloured sticks rather than one spine. It is now always the neutral `appLine`, and
 * every drop of colour has moved into the beads — where the size says what tier of event it is and
 * the colour says what kind of thing it happened to.
 */
private val DayDotSize = 9.dp
private val MilestoneDotSize = 7.dp
private val ProgressDotSize = 4.dp

/**
 * The ring of background colour each bead punches out of the line behind it.
 *
 * Without it a bead in the line's own tone is invisible and a bead in another tone looks laid on
 * top; with it, the thread visibly passes behind. The day marker gets the wider ring because it is
 * the one place the timeline is genuinely meant to break.
 */
private val DotHalo = 3.dp
private val DayDotHalo = 4.dp

private val MilestoneCardPadding = 9.dp
// A true poster: the height is set and the width follows from the ratio. The height is chosen to
// clear the tallest the text column can get — two pills, two lines of title, one line of figures —
// so the poster is always the taller of the two and never leaves a gap beneath itself.
private val MilestoneCoverHeight = 96.dp
private const val MilestoneCoverAspect = 2f / 3f
private val TypeIconSize = 12.dp

private val PulseCoverWidth = 30.dp
// A true 2:3 like the milestone poster, so the two covers are the same object at two sizes.
private const val PulseCoverAspect = 2f / 3f
private val PulseVerticalPadding = 8.dp
// The row carries no card, so the tap target has to come from the row itself: 45dp of cover plus
// 8dp above and below, comfortably over the 48dp minimum.
private val PulseMinHeight = 61.dp
// Where the rail stops under the last progress row of the whole list.
private val PulseRailStop = 30.dp

/** Between two entries of the same sort. */
private val SameKindGap = 8.dp
/** Between a progress row and a milestone card — the two read as different registers. */
private val KindChangeGap = 14.dp

private val MeterHeight = 3.dp
/** The share of the bar that was already there before today's session; the rest is today's gain. */
private const val MeterPriorAlpha = 0.42f

/**
 * A day as a labelled node on the rail.
 *
 * Deliberately a node rather than a section heading: the line runs through it and out the bottom,
 * so consecutive days read as one timeline instead of a stack of separate lists.
 */
@Composable
internal fun TimelineDayNode(
    label: TimelineGutterDate,
    progressByUnit: Map<TimelineProgressUnit, Int>,
    completedCount: Int,
    modifier: Modifier = Modifier,
) {
    val summaryParts = buildList {
        progressByUnit.forEach { (unit, delta) ->
            add(stringResource(R.string.timeline_delta_unit, delta, unit.label(delta)))
        }
        when {
            completedCount == 1 -> add(stringResource(R.string.timeline_day_completed_one))
            completedCount > 1 -> add(stringResource(R.string.timeline_day_completed, completedCount))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(gutterWidth())
                // The gutter reports no height whatsoever, then draws its full height downward from
                // the top. Without this the year's second line would make the day node taller and
                // push the day's first card down with it — a date getting longer is no reason for
                // the timeline to move. Reporting zero lets the line run on past the summary
                // instead, into the empty gutter of the card below.
                .height(0.dp)
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .padding(top = 1.dp, end = GutterHaloClearance),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = label.headline(),
                // One step up from the year beneath it, which is what keeps the two apart now that
                // they sit at the same weight.
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                // A date is forbidden from wrapping rather than merely expected not to: the column
                // is sized to clear the widest of them, but a locale or a font this code has never
                // seen must degrade to a clipped single line, never to a second one. Only
                // `Data desconeguda` — which has no day and no month — is allowed to break.
                softWrap = label.month == null,
                maxLines = if (label.month == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            label.year?.let { year ->
                Text(
                    text = year,
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = GutterTracking,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        TimelineRail(
            // Solid ink, and the only ink-coloured mark in the rail: a new day outranks anything
            // that happened during one, and it is the one bead that is not about a media type.
            dotColor = OmnilogTheme.colors.appInk,
            dotSize = DayDotSize,
            haloWidth = DayDotHalo,
            // The halo is 17dp tall and centres the bead 8.5dp down on its own; this nudge carries
            // it the rest of the way to the middle of the date, which sits 1dp lower on a 16sp line.
            dotTop = 0.5.dp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 2.dp, bottom = 8.dp),
        ) {
            if (summaryParts.isNotEmpty()) {
                Text(
                    text = summaryParts.joinToString(" · "),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * One entry, hung off the rail.
 *
 * Milestones and progress updates are deliberately different objects rather than two sizes of the
 * same card. Finishing a book is what the timeline is read for; advancing forty pages is the noise
 * around it. So a milestone gets a card and a cover while a progress update gets a bare row, and
 * the rail says the same thing again in miniature: a 7dp bead in the media colour against a 4dp one
 * in the line's own tone.
 *
 * [isLast] stops the line dangling past the final entry. [previousKind] is what sat above this row
 * within the same day, which is the only thing the row cannot work out for itself: it decides both
 * the hairline between two adjacent progress rows and how much air to leave above.
 *
 * All spacing between rows is applied on top rather than split between a bottom and a top padding,
 * so a single rule decides each gap and the two sides cannot drift apart.
 */
@Composable
fun TimelineCardRow(
    entry: TimelineEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    previousKind: TimelineEntryKind? = null,
) {
    val isMilestone = entry.kind != TimelineEntryKind.Progress
    val previousWasMilestone = previousKind?.let { it != TimelineEntryKind.Progress }
    val showDivider = previousKind == TimelineEntryKind.Progress &&
        entry.kind == TimelineEntryKind.Progress
    val topGap = when {
        // First of the day: the day node above already carries its own spacing.
        previousWasMilestone == null -> 0.dp
        previousWasMilestone != isMilestone -> KindChangeGap
        isMilestone -> SameKindGap
        // Two progress rows in a row: the hairline is the separation.
        else -> 0.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Spacer(modifier = Modifier.width(gutterWidth()))
        TimelineRail(
            // The bead takes the media colour, not the outcome colour, so it agrees with the type
            // pill on the card beside it. A progress row gets one too, in the line's own tone at
            // full opacity: present enough to mark the row, quiet enough not to rival a milestone.
            dotColor = if (isMilestone) {
                entry.mediaAccent()
            } else {
                OmnilogTheme.colors.appLine.copy(alpha = 1f)
            },
            dotSize = if (isMilestone) MilestoneDotSize else ProgressDotSize,
            haloWidth = DotHalo,
            // Centred rather than pinned, so it stays level with the middle of a card whose height
            // now depends on how far the title wraps.
            centerDot = true,
            stopAtHalf = isLast && isMilestone,
            stopAt = if (isLast && !isMilestone) PulseRailStop else null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = topGap),
        ) {
            if (showDivider) {
                HorizontalDivider(color = OmnilogTheme.colors.appLine)
            }
            if (isMilestone) {
                TimelineMilestoneCard(entry = entry, onClick = onClick)
            } else {
                TimelineProgressRow(entry = entry, onClick = onClick)
            }
        }
    }
}

/**
 * The rail column: one neutral thread, one haloed bead.
 *
 * The line fills the row's height so adjacent rows join seamlessly, which is why every caller sets
 * [IntrinsicSize.Min] and puts its spacing inside the content column.
 *
 * [dotTop] offsets the halo, not the bead — the bead sits at the halo's centre, so a bead with the
 * default offset lands `dotSize / 2 + haloWidth` below the top of the row.
 */
@Composable
private fun TimelineRail(
    dotColor: Color,
    dotSize: Dp,
    haloWidth: Dp,
    dotTop: Dp = 0.dp,
    centerDot: Boolean = false,
    stopAt: Dp? = null,
    stopAtHalf: Boolean = false,
) {
    Box(modifier = Modifier.width(RailWidth)) {
        Box(
            modifier = Modifier
                .width(RailLineWidth)
                .then(
                    when {
                        stopAtHalf -> Modifier.fillMaxHeight(0.5f)
                        stopAt != null -> Modifier.height(stopAt)
                        else -> Modifier.fillMaxHeight()
                    },
                )
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(OmnilogTheme.colors.appLine),
        )
        Box(
            modifier = (
                if (centerDot) {
                    Modifier.align(Alignment.Center)
                } else {
                    Modifier
                        .padding(top = dotTop)
                        .align(Alignment.TopCenter)
                }
                )
                // A haloed day bead is wider than the 10dp rail, so it has to be allowed past its
                // own column. `requiredSize` reports the rail's width upward and paints the overflow
                // centred, which lands it in the empty gutter and inside the content's start padding
                // — nothing is there to be covered.
                .requiredSize(dotSize + haloWidth * 2)
                .clip(CircleShape)
                .background(OmnilogTheme.colors.appBackground),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

/**
 * A milestone — completed, started, revisited — as the timeline's headline object.
 *
 * A poster beside a column of three fixed bands: the two pills, the title, and one line of figures.
 *
 * The poster is sized to clear the tallest that column can get, so it is always the taller of the
 * two and the card takes its height from the artwork rather than from the text. That is what keeps
 * a true 2:3 ratio possible — nothing about the cover depends on how far the title wraps, and the
 * slack when a title is short falls inside the text column where it reads as spacing, not as a hole
 * under the picture.
 */
@Composable
private fun TimelineMilestoneCard(entry: TimelineEntry, onClick: () -> Unit) {
    val description = entry.rowDescription()
    val total = entry.totalText()
    val meta = entry.metaLine()
    // A start or a revisit carries no verdict yet, so the rating belongs to completions alone.
    val rating = entry.rating?.takeIf { entry.kind == TimelineEntryKind.Completion }
    val hasFigures = rating != null || total != null || meta != null
    // Grows with the system font setting, the way MediaCard pins its own height: without this a
    // large text scale pushes the column past the poster and reopens the gap underneath it.
    val coverHeight = MilestoneCoverHeight * LocalDensity.current.fontScale

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(MilestoneCardPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetadataCoverImage(
                coverUrl = entry.coverUrl,
                modifier = Modifier
                    .height(coverHeight)
                    .aspectRatio(MilestoneCoverAspect),
                shape = RoundedCornerShape(3.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = coverHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimelinePill(
                        text = entry.mediaType.typeLabel(),
                        accent = entry.mediaAccent(),
                        iconRes = entry.mediaType.typeIconRes(),
                    )
                    TimelinePill(
                        text = entry.kickerText(),
                        accent = entry.stateAccent(),
                    )
                }
                Text(
                    text = displayMediaTitle(entry.mediaTitle),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasFigures) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The author leads and absorbs the slack; the rating closes the line from
                        // the right, where it lands under the state pill above it. With no author
                        // to fill it, a spacer holds the gap so the rating stays put.
                        if (meta != null) {
                            Text(
                                text = meta,
                                modifier = Modifier.weight(1f),
                                color = OmnilogTheme.colors.appMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        total?.let { text ->
                            Text(
                                text = text,
                                color = OmnilogTheme.colors.appMuted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        rating?.let { value ->
                            MilestoneRating(rating = value, accent = entry.mediaAccent())
                        }
                    }
                }
            }
        }
    }
}

/**
 * The rating, given the same glyph and weight it has on the library cards.
 *
 * It leads the footer rather than sitting among the other numbers: on a completion it is the one
 * figure that is a judgement rather than a measurement. The media accent — not the completion
 * green — so it matches how `MediaCard` colours the same value. No caption: the star says what it
 * is, exactly as it does on the library cards.
 */
@Composable
private fun MilestoneRating(rating: Int, accent: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_kpi_rating),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = stringResource(R.string.timeline_completion_rating, rating),
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/**
 * The app's status-pill shape, borrowed from `ObjectiveProgressCard`'s `StatusChip`.
 *
 * Two of these sit side by side on a milestone: what kind of thing it is, and what happened to it.
 * They were one pill for a while, which meant a single colour had to stand for both the media type
 * and the outcome — so a finished anime could be pink or green but not both.
 */
@Composable
private fun TimelinePill(text: String, accent: Color, @DrawableRes iconRes: Int? = null) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconRes?.let { res ->
                Icon(
                    painter = painterResource(res),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(TypeIconSize),
                )
            }
            Text(
                text = text,
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/** Singular, because the pill names this one item — `Llibre`, not `Llibres`. */
@Composable
private fun MediaType.typeLabel(): String = when (this) {
    MediaType.Anime -> stringResource(R.string.media_type_anime)
    MediaType.Book -> stringResource(R.string.media_type_book)
    MediaType.Movie -> stringResource(R.string.media_type_movie)
    MediaType.TvShow -> stringResource(R.string.media_type_tv_show)
    MediaType.Game -> stringResource(R.string.media_type_game)
}

/**
 * The state pill's colour. Starts and revisits take the in-progress accent rather than the media
 * one, so the two pills never come out the same colour and the pair stays readable as two facts.
 */
@Composable
@ReadOnlyComposable
private fun TimelineEntry.stateAccent(): Color = when (kind) {
    TimelineEntryKind.Completion -> OmnilogTheme.accents.Completed
    TimelineEntryKind.Dropped -> OmnilogTheme.accents.Dropped
    TimelineEntryKind.Paused -> OmnilogTheme.accents.Paused
    else -> OmnilogTheme.accents.InProgress
}

/**
 * Films and series get their own glyphs rather than the navigation bar's shared film strip; see
 * the note on `ObjectiveProgressCard.navIconRes`.
 */
@DrawableRes
private fun MediaType.typeIconRes(): Int = when (this) {
    MediaType.Anime -> R.drawable.ic_nav_anime
    MediaType.Book -> R.drawable.ic_nav_books
    MediaType.Movie -> R.drawable.ic_media_movie
    MediaType.TvShow -> R.drawable.ic_media_series
    MediaType.Game -> R.drawable.ic_nav_games
}

/**
 * A progress update as a bare row: no card, no border, no percentage.
 *
 * The right-hand column keeps today's gain and the running position apart — they used to run
 * together in one `+2 episodis · 14 de 28` string, which read as a single value. The meter says the
 * fraction, so the numeric percentage that used to sit beside the title is gone.
 */
@Composable
private fun TimelineProgressRow(entry: TimelineEntry, onClick: () -> Unit) {
    val accent = entry.mediaAccent()
    val description = entry.rowDescription()
    val meter = entry.meterFractions()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .heightIn(min = PulseMinHeight)
            .padding(vertical = PulseVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataCoverImage(
            coverUrl = entry.coverUrl,
            modifier = Modifier
                .width(PulseCoverWidth)
                .aspectRatio(PulseCoverAspect),
            shape = RoundedCornerShape(3.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = displayMediaTitle(entry.mediaTitle),
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(entry.mediaType.typeIconRes()),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(TypeIconSize),
                )
                entry.metaLine()?.let { meta ->
                    Text(
                        text = meta,
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            meter?.let { fractions ->
                Spacer(modifier = Modifier.height(3.dp))
                TimelineProgressMeter(fractions = fractions, color = accent)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            entry.deltaText()?.let { delta ->
                Text(
                    text = delta,
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
            entry.positionText()?.let { position ->
                Text(
                    text = position,
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Where the session already was, and what it added today. */
private data class MeterFractions(val prior: Float, val current: Float)

/**
 * Two tones on one bar: the faded run is the ground already covered, the solid run is this entry's
 * own gain. Drawn rather than nested so the solid segment can start partway along without the
 * translucent one blending through it.
 */
@Composable
private fun TimelineProgressMeter(fractions: MeterFractions, color: Color) {
    val trackColor = OmnilogTheme.colors.appLine
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(MeterHeight),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        if (fractions.current > 0f) {
            drawRoundRect(
                color = color.copy(alpha = MeterPriorAlpha),
                size = Size(size.width * fractions.current, size.height),
                cornerRadius = radius,
            )
        }
        if (fractions.current > fractions.prior) {
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * fractions.prior, 0f),
                size = Size(size.width * (fractions.current - fractions.prior), size.height),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Where it was consumed, falling back to who made it.
 *
 * No longer prefixed with the media type: the type icon says that now, and repeating `Anime` in
 * words next to the anime glyph was the same fact twice.
 */
private fun TimelineEntry.metaLine(): String? =
    platformName?.takeIf { it.isNotBlank() } ?: creator?.takeIf { it.isNotBlank() }

/** The chip above a milestone's title. Short enough to sit in a 6dp-padded box. */
@Composable
private fun TimelineEntry.kickerText(): String = when (kind) {
    TimelineEntryKind.Completion -> stringResource(R.string.timeline_completed)
    TimelineEntryKind.Dropped -> stringResource(R.string.timeline_kicker_dropped)
    TimelineEntryKind.Paused -> stringResource(R.string.timeline_kicker_paused)
    TimelineEntryKind.Resumed -> stringResource(R.string.timeline_kicker_resumed)
    TimelineEntryKind.Start -> stringResource(R.string.timeline_kicker_started)
    TimelineEntryKind.Revisit -> stringResource(R.string.timeline_kicker_revisit, visitNumber)
    // Progress entries never reach the milestone card; see TimelineCardRow.
    TimelineEntryKind.Progress -> ""
}

/** The full extent of the work, unit and all — `662 pàgines`, `179 minuts`. */
@Composable
private fun TimelineEntry.totalText(): String? {
    val total = progressTotal?.takeIf { it > 0 } ?: return null
    return stringResource(
        R.string.timeline_progress_current,
        total,
        progressUnitLabel(mediaType, total),
    )
}

/** Today's gain — absent when the update recorded a position rather than an increment. */
@Composable
private fun TimelineEntry.deltaText(): String? {
    val delta = progress?.delta ?: return null
    return stringResource(R.string.timeline_delta_unit, delta, progressUnitLabel(mediaType, delta))
}

/** Where the session now stands: `14/28`, or the bare value when no total is known. */
@Composable
private fun TimelineEntry.positionText(): String? {
    val current = progress?.value ?: return null
    val total = progressTotal?.takeIf { it > 0 }
    return if (total != null) {
        stringResource(R.string.timeline_progress_fraction, current, total)
    } else {
        stringResource(
            R.string.timeline_progress_current,
            current,
            progressUnitLabel(mediaType, current),
        )
    }
}

/** Null when there is no total to measure against, so no bar is drawn against a made-up one. */
private fun TimelineEntry.meterFractions(): MeterFractions? {
    val current = progressFraction ?: return null
    val total = progressTotal?.takeIf { it > 0 } ?: return null
    val delta = progress?.delta ?: 0
    val prior = ((progress?.value ?: 0) - delta).toFloat().div(total).coerceIn(0f, current)
    return MeterFractions(prior = prior, current = current)
}

/** The spoken form of a row. Both card shapes read out the same full sentence. */
@Composable
private fun TimelineEntry.rowDescription(): String = stringResource(
    R.string.timeline_entry_accessibility,
    displayMediaTitle(mediaTitle),
    actionText(),
    date?.timelineCompactDate() ?: stringResource(R.string.timeline_unknown_date),
)


/**
 * The dashboard preview.
 *
 * [excludedMediaTypes] stacks on top of the user's own activity settings so the host screen can
 * apply its filters too — the dashboard hides whole sections, and activity for a hidden section
 * should not reappear here.
 */
@Composable
fun TimelineRecentActivity(
    entries: List<TimelineEntry>,
    onEntryClick: (Long) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
    excludedMediaTypes: Set<MediaType> = emptySet(),
    maxEntries: Int = 3,
) {
    val preferences = rememberTimelinePreferences()
    val visibility by rememberTimelineVisibility(preferences)
    // Already sorted newest-first upstream, so this stops at the first few rather than filtering
    // and grouping the whole library.
    val recentEntries = remember(entries, visibility, excludedMediaTypes, maxEntries) {
        entries.asSequence()
            .filter { entry ->
                entry.date != null &&
                    visibility.isVisible(entry.mediaType) &&
                    entry.mediaType !in excludedMediaTypes &&
                    (entry.kind != TimelineEntryKind.Progress || visibility.showsHistory(entry.mediaType))
            }
            .take(maxEntries)
            .toList()
    }
    if (recentEntries.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // The heading is the way through, as on the objectives and analytics cards above. Only
            // the heading — the rows below lead to their own titles, so making the whole card
            // clickable would put two destinations under one press.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewAll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.timeline_recent_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.timeline_view_all),
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogTheme.accents.Dashboard,
                )
            }
            Column {
                recentEntries.forEachIndexed { index, entry ->
                    TimelineCompactRow(
                        entry = entry,
                        onClick = { onEntryClick(entry.mediaItemId) },
                    )
                    if (index < recentEntries.lastIndex) {
                        HorizontalDivider(color = OmnilogTheme.colors.appLine)
                    }
                }
            }
        }
    }
}

/** Dashboard-sized row: no card, no rail, no type label — the action's own colour carries the accent. */
@Composable
private fun TimelineCompactRow(entry: TimelineEntry, onClick: () -> Unit) {
    val accent = entry.railAccent()
    val action = entry.actionText()
    val dateText = entry.date?.timelineCompactDate()
        ?: stringResource(R.string.timeline_unknown_date)
    val description = stringResource(
        R.string.timeline_entry_accessibility,
        displayMediaTitle(entry.mediaTitle),
        action,
        dateText,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // No accent dot. It repeated the colour the action line already carries, and it was the
        // unlabelled half of the pair — the sentence reads without the dot, not the other way round.
        // Its width is what stopped "+42 pàg · total 214" ellipsizing on a narrow screen.
        MetadataCoverImage(
            coverUrl = entry.coverUrl,
            modifier = Modifier
                .width(30.dp)
                .height(45.dp),
            // Matches every other cover in this file. The default is 10.dp, which is a third of the
            // width at this size — enough to read as a rounded chip rather than as a book jacket.
            shape = RoundedCornerShape(3.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayMediaTitle(entry.mediaTitle),
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = action,
                color = accent,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = dateText,
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TimelineEntry.actionText(): String {
    val progressText = progress?.let { progress ->
        val unit = progressUnitLabel(mediaType, progress.delta ?: progress.value)
        when {
            progress.delta != null && progressTotal != null -> stringResource(
                R.string.timeline_progress_delta_total,
                progress.delta,
                unit,
                progress.value,
                progressTotal,
            )
            progress.delta != null -> stringResource(
                R.string.timeline_progress_delta,
                progress.delta,
                unit,
                progress.value,
            )
            else -> stringResource(
                R.string.timeline_progress_recorded,
                progress.value,
                progressUnitLabel(mediaType, progress.value),
            )
        }
    }
    return when (kind) {
        TimelineEntryKind.Progress -> progressText.orEmpty()
        TimelineEntryKind.Start -> stringResource(R.string.timeline_started)
        TimelineEntryKind.Revisit -> stringResource(R.string.timeline_revisit, visitNumber)
        TimelineEntryKind.Paused -> stringResource(R.string.timeline_paused)
        TimelineEntryKind.Resumed -> stringResource(R.string.timeline_resumed)
        // How far it got is the whole point of the sentence — "abandonat" alone says nothing about
        // whether you bounced off page 12 or gave up 400 pages in.
        TimelineEntryKind.Dropped -> listOfNotNull(
            stringResource(R.string.timeline_dropped),
            progress?.let { lastProgress ->
                if (progressTotal != null) {
                    stringResource(
                        R.string.timeline_completion_progress_total,
                        lastProgress.value,
                        progressTotal,
                        progressUnitLabel(mediaType, progressTotal),
                    )
                } else {
                    stringResource(
                        R.string.timeline_completion_progress_value,
                        lastProgress.value,
                        progressUnitLabel(mediaType, lastProgress.value),
                    )
                }
            },
        ).joinToString(" · ")
        TimelineEntryKind.Completion -> listOfNotNull(
            stringResource(R.string.timeline_completed),
            progress?.let { finalProgress ->
                if (progressTotal != null) {
                    stringResource(
                        R.string.timeline_completion_progress_total,
                        finalProgress.value,
                        progressTotal,
                        progressUnitLabel(mediaType, progressTotal),
                    )
                } else {
                    stringResource(
                        R.string.timeline_completion_progress_value,
                        finalProgress.value,
                        progressUnitLabel(mediaType, finalProgress.value),
                    )
                }
            },
            rating?.let { stringResource(R.string.timeline_completion_rating, it) },
        ).joinToString(" · ")
    }
}

@Composable
@ReadOnlyComposable
internal fun TimelineEntry.mediaAccent(): Color = when (mediaType) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
}

/**
 * The two ways a session can end take their outcome's accent, so on the dashboard's dense rows a
 * finished title and an abandoned one read as outcomes rather than as types.
 */
@Composable
@ReadOnlyComposable
internal fun TimelineEntry.railAccent(): Color = when (kind) {
    TimelineEntryKind.Completion -> OmnilogTheme.accents.Completed
    TimelineEntryKind.Dropped -> OmnilogTheme.accents.Dropped
    TimelineEntryKind.Paused -> OmnilogTheme.accents.Paused
    else -> mediaAccent()
}

@Composable
internal fun TimelineProgressUnit.label(value: Int): String = when (this) {
    TimelineProgressUnit.Episodes -> progressUnitLabel(MediaType.Anime, value)
    TimelineProgressUnit.Pages -> progressUnitLabel(MediaType.Book, value)
    TimelineProgressUnit.Minutes -> progressUnitLabel(MediaType.Movie, value)
    TimelineProgressUnit.Hours -> progressUnitLabel(MediaType.Game, value)
}

@Composable
internal fun LocalDate.timelineCompactDate(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> stringResource(R.string.timeline_today)
        today.minusDays(1) -> stringResource(R.string.timeline_yesterday)
        else -> format(DateTimeFormatter.ofPattern("d MMM yyyy", OmnilogLocale))
    }
}

/**
 * The gutter's date, split into the parts the gutter sets differently.
 *
 * A type rather than the newline-packed string this used to be: the three lines now take three
 * different sizes and weights, which one [Text] cannot do.
 */
internal data class TimelineGutterDate(
    /** The day of the month — or, for today and yesterday, the word that replaces the whole date. */
    val lead: String,
    /** Null exactly when [lead] is a word, which is what tells the gutter how to set it. */
    val month: String? = null,
    /** Only carried for dates outside the current year. */
    val year: String? = null,
)

/**
 * The date as one uppercase line — `30 FEBR.`, `AVUI` — with the day carrying the weight.
 *
 * One [Text] rather than two, because the day and the month are one reading and a [Row] of two
 * labels would let them break apart. The emphasis split is what keeps the hierarchy the two separate
 * sizes used to give: ink and heavy for the number, muted and light for the month beside it.
 */
@Composable
private fun TimelineGutterDate.headline(): AnnotatedString {
    val ink = OmnilogTheme.colors.appInk
    val muted = OmnilogTheme.colors.appMuted
    // `AVUI` and `AHIR` keep the uppercase — they are short, they never threaten the second line,
    // and set as words rather than a number they need something to mark them as a label.
    val isWord = month == null
    return buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = ink,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = if (isWord) GutterTracking else TextUnit.Unspecified,
            ),
        ) {
            append(if (isWord) lead.uppercase(OmnilogLocale) else lead)
        }
        month?.let { name ->
            // Uppercase and bold, but still muted: with the weight and the case now matching the
            // day, colour is the only thing left holding the two apart, and the day has to stay the
            // one that reads first.
            withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Bold)) {
                append(" ")
                append(name.uppercase(OmnilogLocale))
            }
        }
    }
}

/**
 * The gutter owns its line breaks: a standalone month avoids Catalan's longer "de jul." form, and
 * only the year is ever given a line of its own.
 */
@Composable
internal fun LocalDate.timelineGutterDate(): TimelineGutterDate {
    val today = LocalDate.now()
    return when {
        this == today -> TimelineGutterDate(lead = stringResource(R.string.timeline_today))
        this == today.minusDays(1) ->
            TimelineGutterDate(lead = stringResource(R.string.timeline_yesterday))

        else -> TimelineGutterDate(
            lead = dayOfMonth.toString(),
            month = month.getDisplayName(TextStyle.SHORT_STANDALONE, OmnilogLocale),
            year = year.takeIf { it != today.year }?.toString(),
        )
    }
}
