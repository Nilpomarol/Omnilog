package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * How far a session has got, drawn in the shape of the thing being tracked.
 *
 * One bar with the fraction written on it works for every medium and says nothing about any of them.
 * These borrow the shape of how the thing is actually consumed instead: episodes are countable so they
 * get one cell each, a book is read in sittings so its bar is cut where the reading stopped, a film is
 * a strip of frames. That is also what makes a book stop looking like an anime on a page where the
 * only other difference is the accent colour.
 *
 * The choice is made from the data, not from a preference: a medium whose total is too large to count
 * out falls back to a quartered bar, and a session with no total at all gets no bar of any kind — see
 * [ActivityWeeks].
 */
@Composable
fun SessionProgressGraphic(
    progressCurrent: Int,
    progressTotal: Int?,
    mediaType: MediaType,
    progressUpdates: List<ProgressUpdate>,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    faded: Boolean = false,
    track: Color? = null,
) {
    // Dropped sessions fade rather than change form. The shape still says how far you got; the
    // washed-out fill is what says you are not going any further.
    val shaded = if (faded) modifier.alpha(DroppedAlpha) else modifier
    val total = progressTotal?.takeIf { it > 0 }
    // What an unreached unit is drawn in. Translucent accent works on a flat panel, where the only
    // thing behind it is the panel. On the live session card there is artwork behind it, and a
    // translucent track picks the drawing up and turns the graphic mottled — so that card passes an
    // opaque colour instead and the track stays a track.
    val empty = track ?: color.copy(alpha = EmptyAlpha)

    when {
        total == null -> ActivityWeeks(
            updates = progressUpdates,
            color = color,
            modifier = shaded,
            compact = compact,
            empty = empty,
        )
        mediaType == MediaType.Book -> SittingBar(
            reached = progressCurrent.coerceIn(0, total),
            total = total,
            updates = progressUpdates,
            color = color,
            modifier = shaded,
            compact = compact,
            empty = empty,
        )
        mediaType == MediaType.Movie -> FilmStrip(
            fraction = fractionOf(progressCurrent, total),
            color = color,
            modifier = shaded,
            compact = compact,
            empty = empty,
        )
        total <= MaxCountableUnits -> UnitGrid(
            total = total,
            done = progressCurrent.coerceIn(0, total),
            color = color,
            modifier = shaded,
            compact = compact,
            empty = empty,
        )
        else -> QuarterBar(
            fraction = fractionOf(progressCurrent, total),
            color = color,
            modifier = shaded,
            compact = compact,
            empty = empty,
        )
    }
}

/**
 * Above this a grid stops being countable and starts being texture — the cells fall below a couple of
 * millimetres and you can no longer tell eleven from twelve, which was the entire reason to draw one
 * cell per unit. Long-running series land here and get the quartered bar instead.
 */
private const val MaxCountableUnits = 60

private const val DroppedAlpha = 0.55f

/**
 * How present an unreached cell is by default: enough to count against, not enough to read as
 * progress. Only holds up on a flat surface — see the `track` parameter for what a card with
 * artwork behind it passes instead.
 */
private const val EmptyAlpha = 0.11f

private fun fractionOf(current: Int, total: Int): Float =
    (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)

// ─────────────────────────────────────────────────────────────
// Episodes — one cell each
// ─────────────────────────────────────────────────────────────

/**
 * One cell per episode, filled solid up to where you are.
 *
 * The same graphic the timeline recap strip draws for months, at the scale of a single title. Nothing
 * here is interpolated: twenty-eight cells is twenty-eight real episodes, so the picture cannot claim
 * more precision than the data has.
 *
 * Every reached cell is the same strength. Dimming everything behind the last one to mark "where you
 * are" only reads as a current position while a session is still moving — on a paused, completed or
 * dropped one it just looked like most of the bar had faded for no reason.
 */
@Composable
private fun UnitGrid(
    total: Int,
    done: Int,
    color: Color,
    empty: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val columns = gridColumns(total)
    val rows = ceil(total.toFloat() / columns).toInt()
    val cellHeight = if (compact) 6.dp else 11.dp
    val gap = if (compact) 2.dp else 3.dp
    val shape = RoundedCornerShape(if (compact) 1.5.dp else 2.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(columns) { column ->
                    val unit = row * columns + column + 1
                    if (unit > total) {
                        // The last row is usually short. Holding its place keeps every cell in the
                        // grid the same width instead of letting the final row stretch.
                        Spacer(modifier = Modifier.weight(1f))
                        return@repeat
                    }
                    val cellColor = if (unit <= done) color else empty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeight)
                            .clip(shape)
                            .background(cellColor),
                    )
                }
            }
        }
    }
}

/**
 * Column count that keeps the grid to at most three rows.
 *
 * Rows rather than a fixed column count because the cells share the card's width: a run of twelve and
 * a run of fifty both have to look deliberate, and a fixed twelve columns would give the fifty-episode
 * series five rows of dashes.
 */
internal fun gridColumns(total: Int): Int = when {
    total <= 16 -> total
    total <= 32 -> ceil(total / 2f).toInt()
    else -> ceil(total / 3f).toInt()
}

// ─────────────────────────────────────────────────────────────
// Books — the bar, split where you put it down
// ─────────────────────────────────────────────────────────────

/**
 * A book's progress, cut where the reading actually stopped.
 *
 * The fore-edge this replaces drew forty-eight evenly spaced strokes and a ribbon, which looked like
 * a book and said nothing a plain bar would not have: the strokes were fixed and the fill was a
 * proportion, so no mark on it corresponded to anything that happened. At the size it ran on the
 * card it read as hatching rather than as pages.
 *
 * The chunks here are real. Each one is a logged sitting, as wide as the share of the book it got
 * through, so the bar shows both how far you are and how you got there — a long holiday stretch and
 * a fortnight of ten-page nights look different, which is the thing about reading a book that a
 * percentage cannot carry. Same idea as the timeline's own strip, at the scale of one title.
 *
 * Drawn rather than laid out with weights, because sittings differ by orders of magnitude: two pages
 * of a seven-hundred-page book is a quarter of a percent, and a weighted row would render it as
 * nothing. [MinSittingWidth] gives every recorded sitting a visible mark, and the gaps come out of
 * the chunk rather than being added between them so the run still ends exactly at the fraction read.
 */
@Composable
private fun SittingBar(
    reached: Int,
    total: Int,
    updates: List<ProgressUpdate>,
    color: Color,
    empty: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val height = if (compact) 6.dp else 11.dp
    val radius = if (compact) 1.5.dp else 2.dp
    // Progress can predate the log — an import carries a page count with no sittings behind it, and
    // the editor can set one by hand. Whatever the sittings do not account for is drawn as a single
    // opening run, so the bar always adds up to where the reader actually is.
    val logged = updates.sumOf { it.amount }.coerceIn(0, reached)
    val runs = buildList {
        if (reached > logged) add(reached - logged)
        updates.forEach { if (it.amount > 0) add(it.amount) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val corner = CornerRadius(radius.toPx(), radius.toPx())
        val gap = GapWidth.toPx()
        val minWidth = MinSittingWidth.toPx()

        drawRoundRect(color = empty, size = size, cornerRadius = corner)

        var x = 0f
        runs.forEach { amount ->
            val span = (amount.toFloat() / total) * size.width
            val width = maxOf(span, minWidth)
            if (x >= size.width) return@forEach
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(
                    // The gap is taken out of the chunk, never added after it, so a run of many
                    // short sittings cannot push the total past where the reader has got to.
                    width = minOf(width - gap, size.width - x).coerceAtLeast(1f),
                    height = size.height,
                ),
                cornerRadius = corner,
            )
            x += width
        }
    }
}

/** The space between two sittings — enough to count them, not enough to lose a short one. */
private val GapWidth = 1.5.dp

/** No sitting is drawn thinner than this, however few pages it was. */
private val MinSittingWidth = 4.dp

// ─────────────────────────────────────────────────────────────
// Films — a strip of frames
// ─────────────────────────────────────────────────────────────

/**
 * Minutes on a perforated strip.
 *
 * A film is usually one sitting, so this mostly reads as started or not started — which is honestly
 * all there is to say about the middle of a film. The perforations are what make it a film rather
 * than a bar; they carry no value.
 */
@Composable
private fun FilmStrip(
    fraction: Float,
    color: Color,
    empty: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val gateHeight = if (compact) 9.dp else 16.dp
    val shape = RoundedCornerShape(2.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Perforations(color = color)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gateHeight)
                .clip(shape)
                .background(empty),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(color),
            )
        }
        Perforations(color = color)
    }
}

@Composable
private fun Perforations(color: Color) {
    val holeColor = color.copy(alpha = 0.34f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp),
    ) {
        val holeWidth = 4.dp.toPx()
        val stride = 9.dp.toPx()
        val radius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        var x = 0f
        while (x < size.width) {
            drawRoundRect(
                color = holeColor,
                topLeft = Offset(x, 0f),
                // The last hole is clipped by the strip's end rather than overhanging it.
                size = Size(minOf(holeWidth, size.width - x), size.height),
                cornerRadius = radius,
            )
            x += stride
        }
    }
}

// ─────────────────────────────────────────────────────────────
// No total — when, rather than how far
// ─────────────────────────────────────────────────────────────

/**
 * Twelve weeks of logged activity, for the sessions that have no finish line.
 *
 * Games are open-ended hours, so "how far through" has no answer, and any bar drawn here would be
 * inventing a denominator to fill. What is knowable is *when* — and when is also the useful thing,
 * because the question a shelved game raises is whether you have touched it lately.
 *
 * Heights are scaled against the busiest week in view rather than an absolute, so a quiet stretch
 * still shows a shape instead of twelve flat cells.
 */
@Composable
private fun ActivityWeeks(
    updates: List<ProgressUpdate>,
    color: Color,
    empty: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val weeks = if (compact) 8 else 12
    val maxHeight = if (compact) 12.dp else 22.dp
    val buckets = weeklyBuckets(updates = updates, weeks = weeks)
    val busiest = buckets.maxOrNull() ?: 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { amount ->
            // An empty week keeps a floor, so the strip reads as a row of weeks with nothing in some
            // of them rather than as a graphic with holes punched in it.
            val ratio = if (busiest <= 0) 0f else amount.toFloat() / busiest.toFloat()
            val height = maxHeight * (MinBucketRatio + (1f - MinBucketRatio) * ratio)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (amount > 0) color else empty),
            )
        }
    }
}

private const val MinBucketRatio = 0.18f

/**
 * Logged amounts summed into calendar weeks ending today, oldest first.
 *
 * Undated entries are left out. They exist because an import can carry a progress figure without the
 * day it happened, and placing them at an arbitrary date would put invented activity on the strip.
 */
internal fun weeklyBuckets(
    updates: List<ProgressUpdate>,
    weeks: Int,
    today: LocalDate = LocalDate.now(),
): List<Int> {
    val buckets = IntArray(weeks)
    updates.forEach { update ->
        if (!update.hasKnownDate) return@forEach
        val daysAgo = ChronoUnit.DAYS.between(update.loggedAt, today)
        if (daysAgo < 0) return@forEach
        val weeksAgo = (daysAgo / 7).toInt()
        if (weeksAgo >= weeks) return@forEach
        buckets[weeks - 1 - weeksAgo] += update.amount
    }
    return buckets.toList()
}

// ─────────────────────────────────────────────────────────────
// Long totals — a quartered bar
// ─────────────────────────────────────────────────────────────

/**
 * The fallback for a total too large to count out: one bar in four segments.
 *
 * Segments rather than tick marks drawn over a bar, because a tick has to contrast with both the
 * filled and the unfilled part of the track and no single colour does that in both themes. The gaps
 * let the panel show through instead, which needs no colour at all — and quarters are what make
 * "just past halfway" legible without a caption.
 */
@Composable
private fun QuarterBar(
    fraction: Float,
    color: Color,
    empty: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val height = if (compact) 6.dp else 11.dp
    val shape = RoundedCornerShape(3.dp)
    val quarter = 1f / QuarterCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(QuarterCount) { index ->
            val filled = ((fraction - index * quarter) / quarter).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(empty),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(filled)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(color),
                )
            }
        }
    }
}

private const val QuarterCount = 4
