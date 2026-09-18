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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.detail.RatingStars
import com.nilpo.contenttracker.ui.home.StatusChip
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
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
 * The year does not enter into this. It takes a line of its own below the date.
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
internal fun gutterWidth(): Dp = DateGutterWidth * LocalDensity.current.fontScale
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
internal val MilestoneDotSize = 7.dp
internal val ProgressDotSize = 4.dp

/**
 * The ring of background colour each bead punches out of the line behind it.
 *
 * Without it a bead in the line's own tone is invisible and a bead in another tone looks laid on
 * top; with it, the thread visibly passes behind. The day marker gets the wider ring because it is
 * the one place the timeline is genuinely meant to break.
 */
internal val DotHalo = 3.dp

/**
 * A month as a chapter of the diary: its name in the section serif, and what the month added up to.
 *
 * The rail stops at the end of each month and starts again under the next heading, so a month reads
 * as one run of days rather than as part of an endless thread.
 */
@Composable
internal fun TimelineMonthHeader(
    title: String,
    progressByUnit: Map<TimelineProgressUnit, Int>,
    completedCount: Int,
    modifier: Modifier = Modifier,
    // The first month sits right under the filters, which already give it air.
    isFirst: Boolean = false,
) {
    val summary = timelineMonthSummary(progressByUnit, completedCount)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = DetailGutter, end = DetailGutter, top = if (isFirst) 4.dp else 28.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DetailSectionTitle(text = title, modifier = Modifier.semantics { heading() })
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/** What a month added up to: completions, then progress per unit. Empty when nothing happened. */
@Composable
internal fun timelineMonthSummary(progressByUnit: Map<TimelineProgressUnit, Int>, completedCount: Int): String = buildList {
    when {
        completedCount == 1 -> add(stringResource(R.string.timeline_day_completed_one))
        completedCount > 1 -> add(stringResource(R.string.timeline_day_completed, completedCount))
    }
    progressByUnit.forEach { (unit, amount) ->
        add(stringResource(R.string.timeline_progress_current, amount, unit.label(amount)))
    }
}.joinToString(" · ")

/**
 * One line of the library's diary, in the same shape as a row of the item's Activitat: the date once
 * per day in the gutter, a bead on the rail, and what happened.
 *
 * Three tiers, so a finished title outweighs a pause and a pause outweighs a sitting:
 * - an ending (completed, abandoned) has the large cover, a heavier title, the outcome and the rating;
 * - a change of state (started, revisited, paused, resumed) has a small cover, the title and the state;
 * - a progress entry has a thumbnail, the amount, and a bar showing what it added to the ground covered.
 * The bead grows with the tier as well. No tier gets a card: size and weight carry the difference.
 */
@Composable
internal fun TimelineDiaryRow(
    entry: TimelineEntry,
    showDate: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Off when the type's progress history is hidden: a milestone's amount and position would then
    // be the only progress on screen, a partial figure read as the whole story.
    showProgress: Boolean = true,
) {
    val tier = when (entry.kind) {
        TimelineEntryKind.Completion, TimelineEntryKind.Dropped -> RowTier.Ending
        TimelineEntryKind.Progress -> RowTier.Progress
        else -> RowTier.Change
    }
    val accent = entry.mediaAccent()
    val description = entry.rowDescription()
    val delta = entry.deltaText()?.takeIf { showProgress }
    val position = entry.positionText()?.takeIf { showProgress }
    // Where the title's first line sits, measured from the top of the row: the bead and the date
    // both line up with it.
    val titleCenter = tier.verticalPadding + tier.titleLineHeight / 2

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(start = DetailGutter - 8.dp, end = DetailGutter),
    ) {
        Column(
            modifier = Modifier
                .width(gutterWidth())
                .padding(top = titleCenter - GutterLineHeight / 2, end = GutterHaloClearance),
            horizontalAlignment = Alignment.End,
        ) {
            if (showDate) {
                entry.date?.timelineGutterDate()?.let { gutter ->
                    Text(
                        text = gutter.headline(),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    gutter.year?.let { year ->
                        Text(
                            text = year,
                            color = OmnilogTheme.colors.appMuted,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = GutterTracking,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        TimelineRail(
            dotColor = if (tier == RowTier.Progress) OmnilogTheme.colors.appLine.copy(alpha = 1f) else entry.stateAccent(),
            dotSize = tier.dotSize,
            haloWidth = DotHalo,
            dotTop = titleCenter - tier.dotSize / 2 - DotHalo,
            stopAt = if (isLast) titleCenter else null,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = tier.verticalPadding, bottom = tier.verticalPadding),
        ) {
            when (tier) {
                RowTier.Progress -> ProgressLine(entry = entry, delta = delta, position = position, accent = accent)
                else -> MilestoneContent(entry = entry, tier = tier, delta = delta, position = position, accent = accent)
            }
        }
    }
}

private enum class RowTier(
    val verticalPadding: Dp,
    val titleLineHeight: Dp,
    val dotSize: Dp,
    val coverWidth: Dp,
) {
    Ending(verticalPadding = 14.dp, titleLineHeight = 24.dp, dotSize = 9.dp, coverWidth = 56.dp),
    Change(verticalPadding = 10.dp, titleLineHeight = 20.dp, dotSize = MilestoneDotSize, coverWidth = 36.dp),
    Progress(verticalPadding = 8.dp, titleLineHeight = 20.dp, dotSize = ProgressDotSize, coverWidth = 24.dp),
}

/** `labelMedium`'s line, which the gutter's date is set in. */
private val GutterLineHeight = 16.dp

/**
 * A sitting: a thumbnail, the title and what it added, over a bar where the faded run is the ground
 * already covered and the solid run is this entry's own gain. Without a total there is nothing to
 * measure against, so the bar gives way to the running figure alone.
 */
@Composable
private fun ProgressLine(entry: TimelineEntry, delta: String?, position: String?, accent: Color) {
    val meter = entry.meterFractions()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetadataCoverImage(
            coverUrl = entry.coverUrl,
            modifier = Modifier
                .width(RowTier.Progress.coverWidth)
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(2.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayMediaTitle(entry.mediaTitle),
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                delta?.let {
                    Text(text = it, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (meter != null) {
                    ProgressMeter(fractions = meter, color = accent, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                position?.let {
                    Text(text = it, color = OmnilogTheme.colors.appMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

/** Where the session already was, and where this entry left it, as fractions of the total. */
private data class MeterFractions(val prior: Float, val current: Float)

/** Null when there is no total to measure against, so no bar is drawn against a made-up one. */
private fun TimelineEntry.meterFractions(): MeterFractions? {
    val current = progressFraction ?: return null
    val total = progressTotal?.takeIf { it > 0 } ?: return null
    val prior = ((progress?.value ?: 0) - (progress?.delta ?: 0)).toFloat().div(total).coerceIn(0f, current)
    return MeterFractions(prior = prior, current = current)
}

/** Drawn rather than nested so the solid run can start partway along without the faded one showing through. */
@Composable
private fun ProgressMeter(fractions: MeterFractions, color: Color, modifier: Modifier = Modifier) {
    val trackColor = OmnilogTheme.colors.appLine
    Canvas(modifier = modifier.height(4.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        drawRoundRect(
            color = color.copy(alpha = 0.35f),
            size = Size(size.width * fractions.current, size.height),
            cornerRadius = radius,
        )
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

@Composable
private fun MilestoneContent(
    entry: TimelineEntry,
    tier: RowTier,
    delta: String?,
    position: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val isEnding = tier == RowTier.Ending
    val rating = entry.ratingHalfPoints?.takeIf { entry.kind == TimelineEntryKind.Completion }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetadataCoverImage(
            coverUrl = entry.coverUrl,
            modifier = Modifier
                .width(tier.coverWidth)
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(if (isEnding) 4.dp else 3.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isEnding) 4.dp else 2.dp),
        ) {
            Text(
                text = displayMediaTitle(entry.mediaTitle),
                color = OmnilogTheme.colors.appInk,
                style = if (isEnding) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = if (isEnding) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.kickerText(),
                color = entry.stateAccent(),
                style = if (isEnding) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                fontWeight = if (isEnding) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
            rating?.let { value ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RatingStars(halfPoints = value, starSize = 15.dp, accent = accent)
                    Text(
                        text = formatRatingHalfPoints(value),
                        color = OmnilogTheme.colors.appInk,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (position != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = position,
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
                delta?.let {
                    Text(
                        text = it,
                        color = accent,
                        style = if (isEnding) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                        fontWeight = if (isEnding) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
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
internal fun TimelineRail(
    dotColor: Color,
    dotSize: Dp,
    haloWidth: Dp,
    // The surface the rail sits on, which the halo has to match to punch the line out cleanly.
    haloColor: Color = OmnilogTheme.colors.appBackground,
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
                .background(haloColor),
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
 * The colour of a change of state, shared by its bead and its label. Starts and revisits take the
 * in-progress accent rather than the media one: the cover already says what kind of thing it is.
 */
@Composable
@ReadOnlyComposable
private fun TimelineEntry.stateAccent(): Color = when (kind) {
    TimelineEntryKind.Completion -> OmnilogTheme.accents.Completed
    TimelineEntryKind.Dropped -> OmnilogTheme.accents.Dropped
    TimelineEntryKind.Paused -> OmnilogTheme.accents.Paused
    else -> OmnilogTheme.accents.InProgress
}

/** What happened, under a milestone's title. */
@Composable
private fun TimelineEntry.kickerText(): String = when (kind) {
    TimelineEntryKind.Completion -> stringResource(R.string.timeline_completed)
    TimelineEntryKind.Dropped -> stringResource(R.string.timeline_kicker_dropped)
    TimelineEntryKind.Paused -> stringResource(R.string.timeline_kicker_paused)
    TimelineEntryKind.Resumed -> stringResource(R.string.timeline_kicker_resumed)
    TimelineEntryKind.Start -> stringResource(R.string.timeline_kicker_started)
    TimelineEntryKind.Revisit -> stringResource(R.string.timeline_kicker_revisit, visitNumber)
    // A progress row names its amount instead; see TimelineDiaryRow.
    TimelineEntryKind.Progress -> ""
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

/** The spoken form of a row: title, what happened, and when. */
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
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
    excludedMediaTypes: Set<MediaType> = emptySet(),
) {
    val preferences = rememberTimelinePreferences()
    val visibility by rememberTimelineVisibility(preferences)
    // Already sorted newest-first upstream, so this stops at the first match rather than filtering
    // and grouping the whole library.
    val entry = remember(entries, visibility, excludedMediaTypes) {
        entries.firstOrNull { entry ->
            entry.date != null &&
                visibility.isVisible(entry.mediaType) &&
                entry.mediaType !in excludedMediaTypes &&
                (entry.kind != TimelineEntryKind.Progress || visibility.showsHistory(entry.mediaType))
        }
    } ?: return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAll),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailSectionTitle(
                text = stringResource(R.string.timeline_recent_title),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.timeline_view_all),
                modifier = Modifier.size(18.dp),
                tint = OmnilogTheme.accents.Dashboard,
            )
        }
        // Heading and entry share one destination, so a single press anywhere reaches the chronology.
        TimelineLatestEntry(entry = entry, onClick = onViewAll)
    }
}

/**
 * The latest entry, borderless: what happened leads in the outcome's colour, and the numbers behind
 * it sit quietly on the line below.
 */
@Composable
private fun TimelineLatestEntry(entry: TimelineEntry, onClick: () -> Unit) {
    val accent = entry.railAccent()
    val action = entry.actionText()
    // ponytail: splits the composed sentence at its first " · " into status and detail; give
    // actionText a structured form if a translation ever drops that separator.
    val headline = action.substringBefore(" · ")
    val detail = action.substringAfter(" · ", missingDelimiterValue = "")
    val dateText = entry.date?.timelineCompactDate()
        ?: stringResource(R.string.timeline_unknown_date)
    val description = entry.rowDescription()

    // No panel: El teu ritme above is already a card, and a second one read as a list of cards.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataCoverImage(
            coverUrl = entry.coverUrl,
            modifier = Modifier
                .width(48.dp)
                .height(72.dp),
            shape = RoundedCornerShape(4.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayMediaTitle(entry.mediaTitle),
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dateText,
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            // The library list's chip, tinted with the entry's colour.
            StatusChip(label = headline, color = accent)
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
        TimelineEntryKind.Start -> listOfNotNull(
            stringResource(R.string.timeline_started),
            progressText,
        ).joinToString(" · ")
        TimelineEntryKind.Revisit -> listOfNotNull(
            stringResource(R.string.timeline_revisit, visitNumber),
            progressText,
        ).joinToString(" · ")
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
                // Finishing at the total is the normal case, and "672 de 672" says the number twice.
                if (progressTotal != null && finalProgress.value != progressTotal) {
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
            ratingHalfPoints?.let { stringResource(R.string.timeline_completion_rating, formatRatingHalfPoints(it)) },
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
internal fun TimelineGutterDate.headline(): AnnotatedString {
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
