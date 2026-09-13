package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsPlannedRevisitTest {
    @Test
    fun plannedRepeatSessionIsNotCountedAsRevisited() {
        val media = TrackedMedia(
            item = MediaItem(
                id = 1,
                type = MediaType.Book,
                title = "Test",
            ),
            sessions = listOf(
                TrackingSession(
                    id = 1,
                    mediaItemId = 1,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    finishedAt = LocalDate.of(2026, 1, 1),
                ),
                TrackingSession(
                    id = 2,
                    mediaItemId = 1,
                    sessionNumber = 2,
                    status = TrackingStatus.Planned,
                    updatedAtEpochMillis = 1_700_000_000_000,
                ),
            ),
        )

        val snapshot = StatsCalculator(LocalDate.of(2026, 9, 13)).calculate(
            items = listOf(media),
            filters = StatsFilters(
                period = StatsPeriod.AllTime,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(0, snapshot.revisitCount)
        assertEquals(0, snapshot.revisitBreakdown.sumOf { it.value })
        assertEquals(0, snapshot.mostRevisitedItems.sumOf { it.value })
    }
}
