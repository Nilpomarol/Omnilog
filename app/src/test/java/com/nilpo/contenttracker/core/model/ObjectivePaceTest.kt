package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObjectivePaceTest {
    private val start = LocalDate.of(2026, 7, 1)
    private val end = LocalDate.of(2026, 7, 31)

    private fun progress(current: Int, target: Int = 100) = ObjectiveProgress(
        objective = Objective(
            name = "Test",
            metric = ObjectiveMetric.ProgressUnits,
            unit = ObjectiveUnit.Pages,
            mediaType = MediaType.Book,
            targetValue = target,
            startDate = start,
            endDate = end,
        ),
        currentValue = current,
    )

    @Test
    fun expectedFractionIsLinearAcrossWindow() {
        // 30-day span (Jul 1 -> Jul 31). Day 15 (Jul 16) is halfway.
        val pace = progress(current = 0).pace(today = LocalDate.of(2026, 7, 16))
        assertEquals(0.5f, pace.expectedFraction, 0.02f)
    }

    @Test
    fun expectedFractionIsZeroBeforeAndOneAfterWindow() {
        assertEquals(0f, progress(0).pace(today = start.minusDays(3)).expectedFraction, 0f)
        assertEquals(1f, progress(0).pace(today = end.plusDays(3)).expectedFraction, 0f)
    }

    @Test
    fun daysRemainingCountsWholeDaysAfterToday() {
        val pace = progress(0).pace(today = LocalDate.of(2026, 7, 19))
        assertEquals(12, pace.daysRemaining)
    }

    @Test
    fun daysRemainingIsZeroOnAndAfterEndDate() {
        assertEquals(0, progress(0).pace(today = end).daysRemaining)
        assertEquals(0, progress(0).pace(today = end.plusDays(5)).daysRemaining)
    }

    @Test
    fun remainingPerDayDividesRemainingUnitsOverRemainingDaysIncludingToday() {
        // 742/1200 with 12 whole days left -> 13 working days incl. today -> ceil(458/13) = 36.
        val pace = progress(current = 742, target = 1200).pace(today = LocalDate.of(2026, 7, 19))
        assertEquals(36.0, pace.remainingPerDay, 0.0)
    }

    @Test
    fun remainingPerDayIsZeroWhenTargetReached() {
        val pace = progress(current = 120, target = 100).pace(today = LocalDate.of(2026, 7, 16))
        assertEquals(0.0, pace.remainingPerDay, 0.0)
    }

    @Test
    fun unitsVsExpectedIsPositiveWhenAheadAndNegativeWhenBehind() {
        // Halfway (Jul 16) expects 50 of 100.
        assertEquals(30, progress(current = 80).pace(today = LocalDate.of(2026, 7, 16)).unitsVsExpected)
        assertEquals(-30, progress(current = 20).pace(today = LocalDate.of(2026, 7, 16)).unitsVsExpected)
    }

    @Test
    fun statusIsAheadWhenProgressLeadsExpected() {
        // Halfway through time, but 80% done.
        val pace = progress(current = 80).pace(today = LocalDate.of(2026, 7, 16))
        assertEquals(ObjectiveStatus.Ahead, pace.status)
    }

    @Test
    fun statusIsBehindWhenProgressTrailsExpected() {
        // Halfway through time, only 20% done.
        val pace = progress(current = 20).pace(today = LocalDate.of(2026, 7, 16))
        assertEquals(ObjectiveStatus.Behind, pace.status)
    }

    @Test
    fun statusIsOnTrackWithinTolerance() {
        // Halfway through time, ~50% done.
        val pace = progress(current = 50).pace(today = LocalDate.of(2026, 7, 16))
        assertEquals(ObjectiveStatus.OnTrack, pace.status)
    }

    @Test
    fun statusIsCompletedWhenTargetMetEvenBeforeExpiry() {
        val pace = progress(current = 100).pace(today = LocalDate.of(2026, 7, 10))
        assertEquals(ObjectiveStatus.Completed, pace.status)
    }

    @Test
    fun statusIsMissedWhenExpiredWithoutReachingTarget() {
        val pace = progress(current = 60).pace(today = end.plusDays(1))
        assertEquals(ObjectiveStatus.Missed, pace.status)
    }

    @Test
    fun completedTakesPrecedenceOverExpiry() {
        val pace = progress(current = 100).pace(today = end.plusDays(1))
        assertEquals(ObjectiveStatus.Completed, pace.status)
    }
}
