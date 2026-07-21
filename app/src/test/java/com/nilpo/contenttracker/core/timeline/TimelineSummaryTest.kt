package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSummaryTest {
    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun streakCountsBackFromTodayWhileDaysAreConsecutive() {
        val summary = entriesOn(today, today.minusDays(1), today.minusDays(2)).toSummary(today)

        assertEquals(3, summary.currentStreak)
    }

    /** Nothing logged yet today is not a broken streak — the day is not over. */
    @Test
    fun streakSurvivesADayThatHasNotBeenLoggedYet() {
        val summary = entriesOn(today.minusDays(1), today.minusDays(2)).toSummary(today)

        assertEquals(2, summary.currentStreak)
    }

    @Test
    fun streakIsZeroOnceAFullDayHasBeenMissed() {
        val summary = entriesOn(today.minusDays(2), today.minusDays(3)).toSummary(today)

        assertEquals(0, summary.currentStreak)
        assertEquals(2, summary.bestStreak)
    }

    @Test
    fun bestStreakFindsTheLongestRunEvenWhenItIsNotTheCurrentOne() {
        val summary = entriesOn(
            today,
            today.minusDays(5),
            today.minusDays(6),
            today.minusDays(7),
            today.minusDays(8),
        ).toSummary(today)

        assertEquals(1, summary.currentStreak)
        assertEquals(4, summary.bestStreak)
    }

    @Test
    fun repeatedEntriesOnOneDayCountAsASingleActiveDay() {
        val summary = entriesOn(today, today, today).toSummary(today)

        assertEquals(1, summary.currentStreak)
        assertEquals(1, summary.bestStreak)
    }

    @Test
    fun undatedEntriesNeverContributeToStreaks() {
        val summary = (entriesOn(today) + entry(date = null)).toSummary(today)

        assertEquals(1, summary.currentStreak)
        assertEquals(1, summary.bestStreak)
    }

    @Test
    fun heatmapIsWholeWeeksEndingWithTheWeekContainingToday() {
        val summary = emptyList<TimelineEntry>().toSummary(today, weeks = 3)

        assertEquals(3, summary.heatmapWeeks.size)
        assertTrue(summary.heatmapWeeks.all { it.size == 7 })
        // 2026-07-20 is a Monday, so it opens the final row.
        assertEquals(today, summary.heatmapWeeks.last().first().date)
        assertEquals(LocalDate.of(2026, 7, 6), summary.heatmapWeeks.first().first().date)
    }

    @Test
    fun heatmapMarksDaysAfterTodayAsFutureRatherThanEmpty() {
        val summary = emptyList<TimelineEntry>().toSummary(today, weeks = 1)

        val week = summary.heatmapWeeks.single()
        assertFalse(week.first().isInFuture)
        assertTrue(week.drop(1).all { it.isInFuture })
    }

    @Test
    fun heatmapCountsEveryEntryOnADayNotJustTheFirst() {
        // Two weeks because today is a Monday: yesterday sits in the preceding row, which also
        // checks that counting works across a week boundary.
        val summary = entriesOn(today, today, today.minusDays(1)).toSummary(today, weeks = 2)

        val days = summary.heatmapWeeks.flatten()
        assertEquals(2, days.first { it.date == today }.entryCount)
        assertEquals(1, days.first { it.date == today.minusDays(1) }.entryCount)
        assertEquals(2, summary.busiestDayCount)
    }

    /** Episodes and pages are different units; a combined total would be meaningless. */
    @Test
    fun totalsStaySeparatedByUnit() {
        val entries = TimelineBuilder().buildEntries(
            listOf(
                mediaWithProgress(id = 1, type = MediaType.Anime, values = listOf(4, 9)),
                mediaWithProgress(id = 2, type = MediaType.Book, values = listOf(100, 180)),
            ),
        )

        val totals = entries.toSummary(today).totalsByUnit

        assertEquals(5, totals[TimelineProgressUnit.Episodes])
        assertEquals(80, totals[TimelineProgressUnit.Pages])
    }

    @Test
    fun completionsAreCountedSeparatelyFromProgress() {
        val entries = TimelineBuilder().buildEntries(
            listOf(completedMedia(id = 1, type = MediaType.Book, finishedAt = today)),
        )

        assertEquals(1, entries.toSummary(today).completedCount)
    }

    @Test
    fun anEmptyLibraryReportsNoActivityRatherThanZeroedStats() {
        val summary = emptyList<TimelineEntry>().toSummary(today)

        assertFalse(summary.hasActivity)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.busiestDayCount)
    }

    private fun entriesOn(vararg dates: LocalDate?): List<TimelineEntry> =
        dates.mapIndexed { index, date -> entry(date = date, key = "e$index") }

    private fun entry(date: LocalDate?, key: String = "e") = TimelineEntry(
        stableKey = key,
        mediaItemId = 1,
        sessionId = 1,
        mediaType = MediaType.Book,
        mediaTitle = "Title",
        coverUrl = null,
        date = date,
        kind = TimelineEntryKind.Start,
        visitNumber = 1,
    )

    private fun mediaWithProgress(id: Long, type: MediaType, values: List<Int>) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Title $id", progressTotal = 500),
        sessions = listOf(
            TrackingSession(
                id = id * 10,
                mediaItemId = id,
                sessionNumber = 1,
                status = TrackingStatus.InProgress,
                progressUpdates = values.mapIndexed { index, value ->
                    ProgressUpdate(
                        id = id * 100 + index,
                        mediaItemId = id,
                        sessionId = id * 10,
                        progressValue = value,
                        loggedAt = today.minusDays((values.size - index).toLong()),
                        hasKnownDate = true,
                        createdAtEpochMillis = (index + 1) * 100L,
                        countsTowardObjectives = true,
                    )
                },
            ),
        ),
    )

    private fun completedMedia(id: Long, type: MediaType, finishedAt: LocalDate) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Title $id", progressTotal = 300),
        sessions = listOf(
            TrackingSession(
                id = id * 10,
                mediaItemId = id,
                sessionNumber = 1,
                status = TrackingStatus.Completed,
                finishedAt = finishedAt,
                progressUpdates = emptyList(),
            ),
        ),
    )
}
