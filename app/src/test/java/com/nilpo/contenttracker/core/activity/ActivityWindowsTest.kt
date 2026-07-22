package com.nilpo.contenttracker.core.activity

import com.nilpo.contenttracker.core.model.ProgressUpdate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage windows are derived from the sequence, never stored and never asked for. See
 * `docs/omnilog-activity-concept.md`.
 */
class ActivityWindowsTest {
    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun aPeriodEntryMeasuresBackToThePreviousEntry() {
        val windows = activityWindows(
            updates = listOf(
                entry(id = 1, day = 2),
                entry(id = 2, day = 15, coversPeriod = true),
            ),
            sessionStartedAt = start,
        )

        assertEquals(
            ActivityWindow(from = date(2), to = date(15)),
            windows[2],
        )
    }

    @Test
    fun theFirstEntryMeasuresBackToTheSessionStart() {
        // The start is when the baseline was true, so it is the honest floor for a first entry.
        val windows = activityWindows(
            updates = listOf(entry(id = 1, day = 15, coversPeriod = true)),
            sessionStartedAt = start,
        )

        assertEquals(ActivityWindow(from = start, to = date(15)), windows[1])
    }

    @Test
    fun sittingsGetNoWindow() {
        val windows = activityWindows(
            updates = listOf(entry(id = 1, day = 2), entry(id = 2, day = 15)),
            sessionStartedAt = start,
        )

        assertTrue(windows.isEmpty())
    }

    @Test
    fun aPeriodEntryOnTheSameDayAsThePreviousOneCoversNothingWorthShowing() {
        val windows = activityWindows(
            updates = listOf(
                entry(id = 1, day = 15),
                entry(id = 2, day = 15, coversPeriod = true),
            ),
            sessionStartedAt = start,
        )

        assertNull(windows[2])
    }

    @Test
    fun aFirstEntryWithNoSessionStartHasNothingToMeasureFrom() {
        val windows = activityWindows(
            updates = listOf(entry(id = 1, day = 15, coversPeriod = true)),
            sessionStartedAt = null,
        )

        assertTrue(windows.isEmpty())
    }

    @Test
    fun undatedEntriesAreSkippedAndDoNotBecomeABoundForOthers() {
        // The undated entry gets no window of its own, and the one after it measures back past it
        // to the last entry that actually had a date.
        val windows = activityWindows(
            updates = listOf(
                entry(id = 1, day = 2),
                entry(id = 2, day = 9, hasKnownDate = false, coversPeriod = true),
                entry(id = 3, day = 15, coversPeriod = true),
            ),
            sessionStartedAt = start,
        )

        assertNull(windows[2])
        assertEquals(ActivityWindow(from = date(2), to = date(15)), windows[3])
    }

    @Test
    fun entriesOutOfOrderAreSequencedBeforeMeasuring() {
        val windows = activityWindows(
            updates = listOf(
                entry(id = 1, day = 15, coversPeriod = true),
                entry(id = 2, day = 2),
            ),
            sessionStartedAt = start,
        )

        assertEquals(ActivityWindow(from = date(2), to = date(15)), windows[1])
    }

    private fun date(day: Int) = LocalDate.of(2026, 3, day)

    private fun entry(
        id: Long,
        day: Int,
        hasKnownDate: Boolean = true,
        coversPeriod: Boolean = false,
    ) = ProgressUpdate(
        id = id,
        mediaItemId = 1,
        sessionId = 10,
        amount = 10,
        loggedAt = date(day),
        hasKnownDate = hasKnownDate,
        createdAtEpochMillis = id * 100,
        coversPeriod = coversPeriod,
    )
}
