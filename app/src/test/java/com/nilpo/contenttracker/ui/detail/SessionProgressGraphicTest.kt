package com.nilpo.contenttracker.ui.detail

import com.nilpo.contenttracker.core.model.ProgressUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.ceil

class SessionProgressGraphicTest {

    // ── The episode grid ─────────────────────────────────────

    @Test
    fun aShortRunFitsOnOneRow() {
        assertEquals(12, gridColumns(12))
        assertEquals(16, gridColumns(16))
    }

    @Test
    fun everyRunUpToTheCountableCeilingStaysWithinThreeRows() {
        // The ceiling exists precisely so the cells stay big enough to count. A run that spilled to a
        // fourth row would be back to reading as texture, which is what the quartered bar is for.
        (1..60).forEach { total ->
            val rows = ceil(total.toFloat() / gridColumns(total)).toInt()
            assertTrue("$total episodes needed $rows rows", rows <= 3)
        }
    }

    @Test
    fun theLastRowIsNeverLeftAlmostEmpty() {
        // 28 over two rows is 14 and 14, not 16 and 12 — an obviously short final row reads as a gap
        // in the run rather than as the end of it. The strongest form of that promise: no run ever
        // leaves a final row less than half full.
        (1..60).forEach { total ->
            val columns = gridColumns(total)
            val remainder = total % columns
            assertTrue(
                "$total episodes left a final row of $remainder in $columns columns",
                remainder == 0 || remainder * 2 >= columns,
            )
        }
    }

    // ── The weekly strip ─────────────────────────────────────

    private val today = LocalDate.of(2026, 7, 23)

    private fun update(daysAgo: Long, amount: Int, dated: Boolean = true) = ProgressUpdate(
        id = daysAgo,
        mediaItemId = 1L,
        sessionId = 1L,
        amount = amount,
        loggedAt = today.minusDays(daysAgo),
        hasKnownDate = dated,
    )

    @Test
    fun logsLandInTheWeekTheyHappened() {
        val buckets = weeklyBuckets(
            updates = listOf(update(daysAgo = 0, amount = 3), update(daysAgo = 8, amount = 5)),
            weeks = 12,
            today = today,
        )

        assertEquals(12, buckets.size)
        // This week is the last cell; eight days back is two weeks ago, so third from the end.
        assertEquals(3, buckets.last())
        assertEquals(5, buckets[buckets.size - 2])
    }

    @Test
    fun sameWeekLogsAddUp() {
        val buckets = weeklyBuckets(
            updates = listOf(update(daysAgo = 1, amount = 2), update(daysAgo = 3, amount = 4)),
            weeks = 12,
            today = today,
        )

        assertEquals(6, buckets.last())
    }

    @Test
    fun undatedLogsAreLeftOut() {
        // An import can carry a progress figure with no day attached. Dropping it into an arbitrary
        // week would draw activity that never happened on a date nobody recorded.
        val buckets = weeklyBuckets(
            updates = listOf(update(daysAgo = 2, amount = 9, dated = false)),
            weeks = 12,
            today = today,
        )

        assertEquals(0, buckets.sum())
    }

    @Test
    fun logsOlderThanTheWindowAreLeftOut() {
        val buckets = weeklyBuckets(
            updates = listOf(update(daysAgo = 400, amount = 40), update(daysAgo = 2, amount = 1)),
            weeks = 12,
            today = today,
        )

        assertEquals(1, buckets.sum())
    }

    @Test
    fun logsDatedInTheFutureAreLeftOut() {
        // A hand-typed date can land ahead of today, and a negative offset would index off the end
        // of the strip.
        val buckets = weeklyBuckets(
            updates = listOf(update(daysAgo = -30, amount = 7)),
            weeks = 12,
            today = today,
        )

        assertEquals(0, buckets.sum())
    }
}
