package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRecapTest {
    private val today = LocalDate.of(2026, 7, 21)

    @Test
    fun aSelectedYearBucketsCompletionsByItsTwelveCalendarMonths() {
        val recap = listOf(
            completion(LocalDate.of(2025, 1, 4)),
            completion(LocalDate.of(2025, 11, 2)),
            completion(LocalDate.of(2025, 11, 20)),
        ).toRecap(year = 2025, today = today)

        assertEquals(12, recap.buckets.size)
        assertEquals(YearMonth.of(2025, 1), recap.buckets.first().month)
        assertEquals(YearMonth.of(2025, 12), recap.buckets.last().month)
        assertEquals(1, recap.buckets[0].count)
        assertEquals(2, recap.buckets[10].count)
        assertEquals(2, recap.busiestBucket)
        assertEquals(3, recap.completedCount)
    }

    /** With no year filtered the card still names a year, and it is the one containing [today]. */
    @Test
    fun noSelectedYearCoversTheCurrentCalendarYear() {
        val recap = emptyList<TimelineEntry>().toRecap(year = null, today = today)

        assertEquals(12, recap.buckets.size)
        assertEquals(2026, recap.year)
        assertEquals(YearMonth.of(2026, 1), recap.buckets.first().month)
        assertEquals(YearMonth.of(2026, 12), recap.buckets.last().month)
    }

    /**
     * The year is the scope, not just the drawing.
     *
     * December of the year before is the case that matters: a rolling window used to pull it in, so
     * the count disagreed with the year printed beside it.
     */
    @Test
    fun theCurrentYearExcludesEverythingOutsideIt() {
        val recap = listOf(
            completion(LocalDate.of(2026, 7, 1), id = 1),
            completion(LocalDate.of(2026, 1, 1), id = 2),
            // One day before the year opens.
            completion(LocalDate.of(2025, 12, 31), id = 3),
            completion(LocalDate.of(2019, 1, 1), id = 4),
        ).toRecap(year = null, today = today)

        assertEquals(2, recap.completedCount)
        assertEquals(listOf(1L, 2L), recap.completed.map { it.mediaItemId })
    }

    @Test
    fun typeCountsCountTitlesAndLeadWithTheLargest() {
        val recap = listOf(
            completion(LocalDate.of(2025, 1, 1), type = MediaType.Game, id = 1),
            completion(LocalDate.of(2025, 2, 1), type = MediaType.Book, id = 2),
            completion(LocalDate.of(2025, 3, 1), type = MediaType.Book, id = 3),
        ).toRecap(year = 2025, today = today)

        assertEquals(
            listOf(MediaType.Book to 2, MediaType.Game to 1),
            recap.countsByType.map { it.type to it.count },
        )
    }

    /** The card scrolls the covers, so every finished title has to be in the list, newest first. */
    @Test
    fun coversAreNewestFirstAndCoverEveryFinishedTitle() {
        val recap = (1..8).map { month ->
            completion(LocalDate.of(2025, month, 1), id = month.toLong())
        }.toRecap(year = 2025, today = today)

        assertEquals(8, recap.completedCount)
        assertEquals(8, recap.completed.size)
        // August is the newest, so it leads.
        assertEquals(8L, recap.completed.first().mediaItemId)
    }

    /** A title advanced but never finished in this period is what the empty state falls back to. */
    @Test
    fun inProgressCountsTitlesWithActivityButNoCompletion() {
        val recap = listOf(
            progress(LocalDate.of(2025, 4, 1), id = 1),
            progress(LocalDate.of(2025, 4, 9), id = 1),
            progress(LocalDate.of(2025, 5, 2), id = 2),
        ).toRecap(year = 2025, today = today)

        assertEquals(0, recap.completedCount)
        assertEquals(2, recap.inProgressCount)
        assertTrue(recap.hasContent)
    }

    /** Abandoning something ends it too: it is not still "in progress" for the rest of the year. */
    @Test
    fun aDroppedTitleIsNotCountedAsInProgress() {
        val recap = listOf(
            progress(LocalDate.of(2025, 4, 1), id = 1),
            dropped(LocalDate.of(2025, 4, 20), id = 1),
            progress(LocalDate.of(2025, 5, 2), id = 2),
        ).toRecap(year = 2025, today = today)

        assertEquals(1, recap.inProgressCount)
    }

    /** A drop is not an achievement — it must not inflate the count or take a cover slot. */
    @Test
    fun droppedTitlesDoNotCountAsCompleted() {
        val recap = listOf(
            dropped(LocalDate.of(2025, 3, 1), id = 1),
            completion(LocalDate.of(2025, 3, 2), id = 2),
        ).toRecap(year = 2025, today = today)

        assertEquals(1, recap.completedCount)
        assertEquals(listOf(2L), recap.completed.map { it.mediaItemId })
        assertEquals(1, recap.buckets.sumOf { it.count })
    }

    /** Finishing something is not also "in progress" — it would be counted in both lines. */
    @Test
    fun aFinishedTitleIsNotAlsoCountedAsInProgress() {
        val recap = listOf(
            progress(LocalDate.of(2025, 4, 1), id = 1),
            completion(LocalDate.of(2025, 4, 20), id = 1),
        ).toRecap(year = 2025, today = today)

        assertEquals(1, recap.completedCount)
        assertEquals(0, recap.inProgressCount)
    }

    /** Undated entries cannot be attributed to a month, so they must not inflate any figure. */
    @Test
    fun undatedEntriesTakeNoPart() {
        val recap = listOf(
            completion(LocalDate.of(2025, 2, 1)),
            completion(date = null, id = 99),
        ).toRecap(year = 2025, today = today)

        assertEquals(1, recap.completedCount)
        assertEquals(1, recap.buckets.sumOf { it.count })
    }

    @Test
    fun anEmptyPeriodHasNoContentToShow() {
        val recap = emptyList<TimelineEntry>().toRecap(year = 2025, today = today)

        assertFalse(recap.hasContent)
        assertEquals(0, recap.busiestBucket)
        assertEquals(12, recap.buckets.size)
    }

    private fun completion(
        date: LocalDate?,
        type: MediaType = MediaType.Book,
        id: Long = 1,
    ) = entry(date, type, id, TimelineEntryKind.Completion)

    private fun progress(
        date: LocalDate?,
        type: MediaType = MediaType.Book,
        id: Long = 1,
    ) = entry(date, type, id, TimelineEntryKind.Progress)

    private fun dropped(
        date: LocalDate?,
        type: MediaType = MediaType.Book,
        id: Long = 1,
    ) = entry(date, type, id, TimelineEntryKind.Dropped)

    private fun entry(
        date: LocalDate?,
        type: MediaType,
        id: Long,
        kind: TimelineEntryKind,
    ) = TimelineEntry(
        stableKey = "$kind:$id:$date",
        mediaItemId = id,
        sessionId = id * 10,
        mediaType = type,
        mediaTitle = "Title $id",
        coverUrl = null,
        date = date,
        kind = kind,
        visitNumber = 1,
    )
}
