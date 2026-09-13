package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGroupsTest {
    @Test
    fun authorGroupsRespectRecentSort() {
        val old = trackedBook(1, "Old", "A Author", updatedAt = 100)
        val recent = trackedBook(2, "Recent", "Z Author", updatedAt = 500)

        val groups = buildHomeGroups(
            items = listOf(old, recent),
            groupMode = HomeGroupMode.Author,
            sortMode = HomeSortMode.Recent,
            sortDirection = HomeSortDirection.Descending,
        )

        assertEquals(listOf("Z Author", "A Author"), groups.map { it.title })
    }

    @Test
    fun authorGroupsRespectRatingSort() {
        val lower = trackedBook(1, "Lower", "A Author", rating = 12)
        val higher = trackedBook(2, "Higher", "Z Author", rating = 18)

        val groups = buildHomeGroups(
            items = listOf(lower, higher),
            groupMode = HomeGroupMode.Author,
            sortMode = HomeSortMode.Rating,
            sortDirection = HomeSortDirection.Descending,
        )

        assertEquals(listOf("Z Author", "A Author"), groups.map { it.title })
    }

    @Test
    fun collectionGroupsUseAggregateProgress() {
        val finished = MediaCollection(1, "Finished")
        val started = MediaCollection(2, "Started")
        val groups = buildHomeGroups(
            items = listOf(
                trackedBook(1, "Done", "Author", collection = finished, status = TrackingStatus.Completed),
                trackedBook(2, "Started", "Author", collection = started, status = TrackingStatus.InProgress),
            ),
            groupMode = HomeGroupMode.Collection,
            sortMode = HomeSortMode.Progress,
            sortDirection = HomeSortDirection.Descending,
        )

        assertEquals(listOf("Finished", "Started"), groups.map { it.title })
    }

    @Test
    fun selectedCreatorDoesNotLeakCoauthorGroups() {
        val coauthored = TrackedMedia(
            item = MediaItem(
                id = 1,
                type = MediaType.Book,
                title = "Shared",
                creators = listOf("Selected Author", "Other Author"),
            ),
            sessions = listOf(session(1, 1)),
        )

        val groups = buildHomeGroups(
            items = listOf(coauthored),
            groupMode = HomeGroupMode.Author,
            selectedCreators = setOf("Selected Author"),
        )

        assertEquals(listOf("Selected Author"), groups.map { it.title })
    }

    private fun trackedBook(
        id: Long,
        title: String,
        author: String,
        collection: MediaCollection? = null,
        status: TrackingStatus = TrackingStatus.InProgress,
        rating: Int? = null,
        updatedAt: Long = 0,
    ) = TrackedMedia(
        item = MediaItem(
            id = id,
            type = MediaType.Book,
            title = title,
            collectionId = collection?.id,
            creators = listOf(author),
        ),
        collection = collection,
        sessions = listOf(session(id, id, status, rating, updatedAt)),
    )

    private fun session(
        id: Long,
        mediaItemId: Long,
        status: TrackingStatus = TrackingStatus.InProgress,
        rating: Int? = null,
        updatedAt: Long = 0,
    ) = TrackingSession(
        id = id,
        mediaItemId = mediaItemId,
        sessionNumber = 1,
        status = status,
        ratingHalfPoints = rating,
        updatedAtEpochMillis = updatedAt,
    )
}
