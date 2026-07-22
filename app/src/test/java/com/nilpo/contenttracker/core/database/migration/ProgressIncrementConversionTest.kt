package com.nilpo.contenttracker.core.database.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cumulative-to-increment conversion that migration 19→20 runs over every session.
 *
 * The invariant every case here defends: `baseline + sum(entries)` must equal the session's old
 * `progressCurrent`, which under cumulative storage is the chronologically last row's value. Getting
 * this wrong silently rewrites every total in the app, so each fixture asserts it explicitly as well
 * as asserting the shape of the result.
 */
class ProgressIncrementConversionTest {

    // ── Baselines ────────────────────────────────────────────────────────────

    @Test
    fun aSessionOfOnlyCatchUpRowsBecomesABaselineWithNoEntries() {
        val result = convert(
            catchUp(id = 1, value = 412, day = 100),
        )

        assertEquals(412, result.baselineProgress)
        assertTrue(result.entries.isEmpty())
        assertEquals(listOf(1L), result.deletedRowIds)
        result.assertTotals(oldProgressCurrent = 412)
    }

    @Test
    fun leadingCatchUpRowsCollapseToTheLastOfThem() {
        // Adding an item part-way through, then correcting what page you were really on.
        val result = convert(
            catchUp(id = 1, value = 180, day = 100),
            catchUp(id = 2, value = 200, day = 101),
        )

        assertEquals(200, result.baselineProgress)
        assertTrue(result.entries.isEmpty())
        result.assertTotals(oldProgressCurrent = 200)
    }

    @Test
    fun realLoggingAfterABaselineCountsOnlyWhatWasLoggedAfterIt() {
        val result = convert(
            catchUp(id = 1, value = 180, day = 100),
            logged(id = 2, value = 212, day = 101),
            logged(id = 3, value = 260, day = 102),
        )

        assertEquals(180, result.baselineProgress)
        assertEquals(listOf(32, 48), result.entries.map { it.amount })
        result.assertTotals(oldProgressCurrent = 260)
    }

    // ── Plain sequences ──────────────────────────────────────────────────────

    @Test
    fun aSessionWithNoBaselineStartsFromZero() {
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 80, day = 101),
        )

        assertEquals(0, result.baselineProgress)
        assertEquals(listOf(50, 30), result.entries.map { it.amount })
        result.assertTotals(oldProgressCurrent = 80)
    }

    @Test
    fun aSingleLoggedRowKeepsItsWholeValue() {
        val result = convert(logged(id = 1, value = 32, day = 100))

        assertEquals(0, result.baselineProgress)
        assertEquals(listOf(32), result.entries.map { it.amount })
        result.assertTotals(oldProgressCurrent = 32)
    }

    @Test
    fun rowsAreOrderedByLoggedDayThenCreationThenId() {
        // Written out of order: a back-dated row inserted after a later one.
        val result = convert(
            logged(id = 1, value = 80, day = 102, createdAt = 10),
            logged(id = 2, value = 50, day = 101, createdAt = 20),
        )

        assertEquals(listOf(2L, 1L), result.entries.map { it.id })
        assertEquals(listOf(50, 30), result.entries.map { it.amount })
        result.assertTotals(oldProgressCurrent = 80)
    }

    @Test
    fun anEmptySessionConvertsToNothing() {
        val result = convert()

        assertEquals(0, result.baselineProgress)
        assertTrue(result.entries.isEmpty())
        assertTrue(result.deletedRowIds.isEmpty())
    }

    // ── Regressions ──────────────────────────────────────────────────────────

    @Test
    fun aRegressionInTheMiddleIsDroppedAndDoesNotInflateWhatFollows() {
        // 50, corrected down to 30, then advanced to 80. The old session total is 80, so the
        // increments must sum to 80 and not to 100.
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 30, day = 101),
            logged(id = 3, value = 80, day = 102),
        )

        assertTrue(2L in result.deletedRowIds)
        assertEquals(80, result.entries.sumOf { it.amount })
        result.assertTotals(oldProgressCurrent = 80)
    }

    @Test
    fun aRegressionAtTheEndTrimsBackwardsSoTheFinalTotalSurvives() {
        // The last thing the user said is that they are on page 30. That is the truth to preserve,
        // even though earlier rows claimed 80.
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 80, day = 101),
            logged(id = 3, value = 30, day = 102),
        )

        assertEquals(30, result.entries.sumOf { it.amount })
        result.assertTotals(oldProgressCurrent = 30)
    }

    @Test
    fun trimmingRemovesEntriesItEmptiesRatherThanKeepingZeroes() {
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 80, day = 101),
            logged(id = 3, value = 20, day = 102),
        )

        assertTrue(result.entries.none { it.amount <= 0 })
        assertEquals(20, result.entries.sumOf { it.amount })
        assertTrue(2L in result.deletedRowIds)
        result.assertTotals(oldProgressCurrent = 20)
    }

    @Test
    fun aDuplicateValueIsNotAnEntry() {
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 50, day = 101),
        )

        assertEquals(listOf(50), result.entries.map { it.amount })
        assertEquals(listOf(2L), result.deletedRowIds)
        result.assertTotals(oldProgressCurrent = 50)
    }

    // ── Dates ────────────────────────────────────────────────────────────────

    @Test
    fun anUnknownDateSurvivesConversion() {
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            logged(id = 2, value = 80, day = 101, hasKnownDate = false),
        )

        assertEquals(listOf(true, false), result.entries.map { it.hasKnownDate })
        result.assertTotals(oldProgressCurrent = 80)
    }

    @Test
    fun aCatchUpRowAfterRealLoggingBecomesAnUndatedEntry() {
        // Marking a session complete with a past finish date writes a catch-up row stamped with
        // today. It is not a baseline — logging already happened before it — and its date is
        // fiction, so it converts to an entry with no known date. That keeps it out of objectives
        // through the mechanism that already exists for undated rows.
        val result = convert(
            logged(id = 1, value = 50, day = 100),
            catchUp(id = 2, value = 412, day = 101),
        )

        assertEquals(0, result.baselineProgress)
        assertEquals(listOf(50, 362), result.entries.map { it.amount })
        assertEquals(listOf(true, false), result.entries.map { it.hasKnownDate })
        result.assertTotals(oldProgressCurrent = 412)
    }

    // ── Flagged rows on a finished session ───────────────────────────────────
    //
    // The old flag meant "this row's date is the day it was typed, not the day it was read". When
    // the session has a finish date there is an honest date to use, so the rows become real entries
    // instead of vanishing into a baseline.

    @Test
    fun aDayByDayReadLoggedAfterFinishingKeepsItsOwnDates() {
        // Six real sittings, all entered after the book was marked complete, so all were flagged.
        val result = convertSessionToIncrements(
            rows = listOf(
                catchUp(id = 1, value = 48, day = 100),
                catchUp(id = 2, value = 116, day = 101),
                catchUp(id = 3, value = 505, day = 105),
            ),
            finishedAtEpochDay = 105,
        )

        assertEquals(0, result.baselineProgress)
        assertEquals(listOf(48, 68, 389), result.entries.map { it.amount })
        assertEquals(listOf(100L, 101L, 105L), result.entries.map { it.loggedAtEpochDay })
        assertTrue(result.entries.all { it.hasKnownDate })
    }

    @Test
    fun aRowStampedAfterTheFinishMovesToTheFinishDay() {
        // Logged the day after finishing: the reading belongs on the finish day, not on the day it
        // was typed, and it must not be written off as pre-tracking progress.
        val result = convertSessionToIncrements(
            rows = listOf(catchUp(id = 1, value = 528, day = 101)),
            finishedAtEpochDay = 100,
        )

        assertEquals(0, result.baselineProgress)
        assertEquals(listOf(528), result.entries.map { it.amount })
        assertEquals(listOf(100L), result.entries.map { it.loggedAtEpochDay })
        assertTrue(result.entries.single().hasKnownDate)
    }

    @Test
    fun aBulkAddedBacklogStaysOnTheDayItWasActuallyFinished() {
        // Added during a library backfill years after reading it. Re-dating to the finish day keeps
        // it out of the period it was typed in, which is the whole point.
        val result = convertSessionToIncrements(
            rows = listOf(catchUp(id = 1, value = 412, day = 20000)),
            finishedAtEpochDay = 15000,
        )

        assertEquals(listOf(15000L), result.entries.map { it.loggedAtEpochDay })
    }

    @Test
    fun withoutAFinishDateFlaggedProgressIsStillABaseline() {
        // Nothing to date it by, so this really is progress that predates tracking.
        val result = convertSessionToIncrements(
            rows = listOf(catchUp(id = 1, value = 180, day = 100)),
            finishedAtEpochDay = null,
        )

        assertEquals(180, result.baselineProgress)
        assertTrue(result.entries.isEmpty())
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun convert(vararg rows: LegacyProgressRow): ConvertedSession =
        convertSessionToIncrements(rows.toList())

    /**
     * The invariant. [oldProgressCurrent] is what the cumulative model would have reported for this
     * session: the chronologically last row's value.
     */
    private fun ConvertedSession.assertTotals(oldProgressCurrent: Int) {
        assertEquals(
            "baseline + sum(entries) must equal the pre-migration session progress",
            oldProgressCurrent,
            baselineProgress + entries.sumOf { it.amount },
        )
        assertTrue("no entry may be zero or negative", entries.none { it.amount <= 0 })
    }

    private fun logged(
        id: Long,
        value: Int,
        day: Long,
        createdAt: Long = day,
        hasKnownDate: Boolean = true,
    ) = LegacyProgressRow(
        id = id,
        progressValue = value,
        loggedAtEpochDay = day,
        createdAtEpochMillis = createdAt,
        hasKnownDate = hasKnownDate,
        countsTowardObjectives = true,
    )

    private fun catchUp(
        id: Long,
        value: Int,
        day: Long,
        createdAt: Long = day,
    ) = LegacyProgressRow(
        id = id,
        progressValue = value,
        loggedAtEpochDay = day,
        createdAtEpochMillis = createdAt,
        hasKnownDate = true,
        countsTowardObjectives = false,
    )
}
