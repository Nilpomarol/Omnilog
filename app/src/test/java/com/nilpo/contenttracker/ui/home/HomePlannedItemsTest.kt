package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePlannedItemsTest {
    private val today = LocalDate.of(2026, 9, 10)

    @Test
    fun collectionMatchesLeadAndBothTiersKeepRecentActivityOrder() {
        val items = listOf(
            media(1, 10, TrackingStatus.InProgress),
            media(2, 20, TrackingStatus.Completed, finishedAt = today.minusDays(3)),
            media(3, 10, updated = 10),
            media(4, 20, updated = 20),
            media(5, 30, updated = 100),
            media(6, null, updated = 90),
        )
        assertEquals(listOf(4L, 3L, 5L, 6L), rankedIds(items))
    }

    @Test
    fun recentWindowIncludesTodayAnd29DaysAgoButNotOldFutureOrUndatedCompletions() {
        val dates = listOf(today, today.minusDays(29), today.minusDays(30), today.plusDays(1), null)
        val completed = dates.mapIndexed { index, date ->
            media(index.toLong(), index.toLong(), TrackingStatus.Completed, updated = 999, finishedAt = date)
        }
        val planned = dates.indices.map { index ->
            media(10L + index, index.toLong(), updated = index.toLong())
        }
        assertEquals(listOf(11L, 10L, 14L, 13L, 12L), rankedIds(completed + planned))
    }

    @Test
    fun missingCollectionsAndPausedItemsDoNotPromotePlannedTitles() {
        val items = listOf(
            media(1, null, TrackingStatus.InProgress),
            media(2, 20, TrackingStatus.Paused),
            media(3, null, updated = 1),
            media(4, 20, updated = 2),
            media(5, 30, updated = 3),
        )
        assertEquals(listOf(5L, 4L, 3L), rankedIds(items))
    }

    @Test
    fun recentCompletedHistoryStillCountsAfterStartingAnotherSessionAndPriorityPrecedesLimit() {
        val completed = media(1, 10, TrackingStatus.Completed, finishedAt = today)
        val revisitingLater = completed.copy(sessions = completed.sessions + TrackingSession(
            id = 100, mediaItemId = 1, sessionNumber = 2, status = TrackingStatus.Paused,
        ))
        val unrelated = (2L..10L).map { media(it, 20, updated = 100 + it) }
        val continuation = media(11, 10, updated = 1)
        val result = prioritizeHomePlannedItems(unrelated + revisitingLater + continuation, today).take(8)
        assertEquals(11L, result.first().item.id)
        assertEquals(8, result.size)
    }

    @Test
    fun earlierVolumesLeadEvenWhenLaterVolumesWereUpdatedMoreRecently() {
        val items = listOf(
            media(1, 10, TrackingStatus.InProgress),
            media(4, 10, updated = 100, order = 4.0),
            media(3, 10, updated = 1, order = 3.0),
            media(5, 20, updated = 200, order = 1.0),
        )
        assertEquals(listOf(3L, 4L, 5L), rankedIds(items))
    }

    @Test
    fun collectionOrderingPreservesOtherCollectionsSlotsAndStandaloneRecency() {
        val items = listOf(
            media(4, 10, updated = 100, order = 4.0),
            media(8, 20, updated = 90, order = 8.0),
            media(9, null, updated = 80),
            media(3, 10, updated = 70, order = 3.0),
            media(7, 20, updated = 60, order = 7.0),
        )
        assertEquals(listOf(3L, 7L, 9L, 4L, 8L), rankedIds(items))
    }

    @Test
    fun fractionalPositionsSortNumericallyAndUnknownPositionsComeLast() {
        val items = listOf(
            media(1, 10, updated = 100),
            media(2, 10, updated = 90, order = 4.0),
            media(3, 10, updated = 80, order = 3.5),
            media(4, 10, updated = 70, order = 3.0),
            media(5, 10, updated = 60, order = 3.0),
        )
        assertEquals(listOf(4L, 5L, 3L, 2L, 1L), rankedIds(items))
    }

    private fun rankedIds(items: List<TrackedMedia>) =
        prioritizeHomePlannedItems(items, today).map { it.item.id }

    private fun media(
        id: Long,
        collection: Long?,
        status: TrackingStatus = TrackingStatus.Planned,
        updated: Long = 0,
        finishedAt: LocalDate? = null,
        order: Double? = null,
    ) = TrackedMedia(
        item = MediaItem(id = id, type = MediaType.Book, title = "Title $id", collectionId = collection,
            collectionSortOrder = order),
        sessions = listOf(TrackingSession(
            id = id, mediaItemId = id, sessionNumber = 1, status = status,
            updatedAtEpochMillis = updated, finishedAt = finishedAt,
        )),
    )
}
