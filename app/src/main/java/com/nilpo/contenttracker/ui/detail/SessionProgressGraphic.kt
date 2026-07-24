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
 * These borrow the physical form instead: episodes are countable so they get one cell each, a book is
 * a block of pages so it gets its fore-edge, a film is a strip of frames. That is also what makes a
 * book stop looking like an anime on a page where the only other difference is the accent colour.
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
) {
    // Dropped sessions fade rather than change form. The shape still says how far you got; the
    // washed-out fill is what says you are not going any further.
    val shaded = if (faded) modifier.alpha(DroppedAlpha) else modifier
    val total = progressTotal?.takeIf { it > 0 }

    when {
        total == null -> ActivityWeeks(
            updates = progressUpdates,
            color = color,
            modifier = shaded,
            compact = compact,
        )
        mediaType == MediaType.Book -> ForeEdge(
            fraction = fractionOf(progressCurrent, total),
            color = color,
            modifier = shaded,
            compact = compact,
        )
        mediaType == MediaType.Movie -> FilmStrip(
            fraction = fractionOf(progressCurrent, total),
            color = color,
            modifier = shaded,
            compact = compact,
        )
        total <= MaxCountableUnits -> UnitGrid(
            total = total,
            done = progressCurrent.coerceIn(0, total),
            color = color,
            modifier = shaded,
            compact = compact,
        )
        else -> QuarterBar(
            fraction = fractionOf(progressCurrent, total),
            color = color,
            modifier = shaded,
            compact = compact,
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

/** How present an unreached cell is: enough to count against, not enough to read as progress. */
private const val EmptyAlpha = 0.11f

/**
 * How far the cells behind you are held back from full strength.
 *
 * This is what marks the cell you are on without drawing anything extra on it: everything consumed is
 * present but muted, the current unit is the one at full colour, and the rest is track. A ring or a
 * halo would have needed the surface's own colour to cut a gap around the cell, which ties the
 * graphic to whatever panel it happens to be sitting on.
 */
private const val ConsumedAlpha = 0.55f

private fun fractionOf(current: Int, total: Int): Float =
    (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)

// ─────────────────────────────────────────────────────────────
// Episodes — one cell each
// ─────────────────────────────────────────────────────────────

/**
 * One cell per episode, filled up to where you are, with the current one at full strength.
 *
 * The same graphic the timeline recap strip draws for months, at the scale of a single title. Nothing
 * here is interpolated: twenty-eight cells is twenty-eight real episodes, so the picture cannot claim
 * more precision than the data has.
 */
@Composable
private fun UnitGrid(
    total: Int,
    done: Int,
    color: Color,
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
                    // Compact grids sit on the history cards, where the session is over and there is
                    // no "where you are" to mark — so they fill flat.
                    val cellColor = when {
                        unit == done && !compact -> color
                        unit <= done && compact -> color
                        unit <= done -> color.copy(alpha = ConsumedAlpha)
                        else -> color.copy(alpha = EmptyAlpha)
                    }
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
// Books — the page block, seen edge-on
// ─────────────────────────────────────────────────────────────

/**
 * A book's fore-edge: the board at the left, the page block beside it, and a ribbon at the page you
 * are on.
 *
 * Deliberately not one stroke per page. Four hundred and eighty-six strokes is not a graphic, and a
 * page is not a unit anyone counts the way they count episodes — what a reader wants to see is how
 * much of the block is behind the ribbon. The stroke count is therefore fixed and the fill is
 * proportional, which is the one place in this file a graphic is a proportion rather than a tally.
 */
@Composable
private fun ForeEdge(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val strokes = if (compact) 32 else 48
    val height = if (compact) 16.dp else 30.dp
    val ribbonWidth = 3.dp
    val readStrokes = (strokes * fraction).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        // The board. Full-strength colour against the paler block, so the graphic has a spine to be
        // read from rather than starting in mid-air.
        Box(
            modifier = Modifier
                .width(if (compact) 3.dp else 5.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                .background(color),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            ) {
                repeat(strokes) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (index < readStrokes) {
                                    color.copy(alpha = 0.64f)
                                } else {
                                    color.copy(alpha = 0.13f)
                                },
                            ),
                    )
                }
            }

            // Only while there is somewhere to be. At nought the ribbon would sit on the board, and
            // at the end there is no page left to mark.
            //
            // Placed with weights rather than by reading the block's measured width. The card can be
            // laid out inside a row measuring its intrinsic height — the session thread does exactly
            // that to size its rail — and intrinsic measurement is not supported by the subcompose
            // layout that `BoxWithConstraints` is built on, so reading the width here would throw on
            // any book with a history beside it.
            if (!compact && fraction > 0f && fraction < 1f) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(fraction))
                    Box(
                        modifier = Modifier
                            .width(ribbonWidth)
                            .fillMaxHeight()
                            .background(color),
                    )
                    Spacer(modifier = Modifier.weight(1f - fraction))
                }
            }
        }
    }
}

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
                .background(color.copy(alpha = EmptyAlpha)),
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
                    .background(if (amount > 0) color else color.copy(alpha = EmptyAlpha)),
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
                    .background(color.copy(alpha = EmptyAlpha)),
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
