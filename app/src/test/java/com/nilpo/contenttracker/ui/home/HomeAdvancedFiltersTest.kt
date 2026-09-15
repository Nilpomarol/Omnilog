package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.ConsumptionPlatform
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAdvancedFiltersTest {
    private val ownedNinetiesFilm = trackedItem(
        id = 1,
        type = MediaType.Movie,
        releaseYear = 1994,
        progressTotal = 142,
        isOwned = true,
        platform = ConsumptionPlatformType.Physical,
    )
    private val streamedShow = trackedItem(
        id = 2,
        type = MediaType.TvShow,
        releaseYear = 2021,
        progressTotal = 10,
        platform = ConsumptionPlatformType.Streaming,
    )
    private val undatedFilm = trackedItem(id = 3, type = MediaType.Movie)
    private val items = listOf(ownedNinetiesFilm, streamedShow, undatedFilm)

    @Test
    fun noFiltersKeepsEverything() {
        assertEquals(items, items.filterByAdvancedFilters(HomeAdvancedFilters()))
    }

    @Test
    fun eachNewFilterNarrowsOnItsOwn() {
        assertEquals(listOf(streamedShow), items.filterByAdvancedFilters(HomeAdvancedFilters(types = setOf(MediaType.TvShow))))
        assertEquals(listOf(ownedNinetiesFilm), items.filterByAdvancedFilters(HomeAdvancedFilters(ownedOnly = true)))
        // An item without a release year or a total can never match a span.
        assertEquals(listOf(ownedNinetiesFilm), items.filterByAdvancedFilters(HomeAdvancedFilters(releaseYears = 1990..1999)))
        assertEquals(listOf(ownedNinetiesFilm), items.filterByAdvancedFilters(HomeAdvancedFilters(lengthRange = 100..200)))
        assertEquals(listOf(streamedShow), items.filterByAdvancedFilters(HomeAdvancedFilters(lengthRange = 10..10)))
        assertEquals(
            listOf(streamedShow),
            items.filterByAdvancedFilters(HomeAdvancedFilters(platformTypes = setOf(ConsumptionPlatformType.Streaming))),
        )
    }

    @Test
    fun valuesWithinAFilterWidenAndFiltersTogetherNarrow() {
        assertEquals(
            listOf(ownedNinetiesFilm, streamedShow),
            items.filterByAdvancedFilters(
                HomeAdvancedFilters(platformTypes = setOf(ConsumptionPlatformType.Physical, ConsumptionPlatformType.Streaming)),
            ),
        )
        assertEquals(
            emptyList<TrackedMedia>(),
            items.filterByAdvancedFilters(HomeAdvancedFilters(ownedOnly = true, types = setOf(MediaType.TvShow))),
        )
    }

    @Test
    fun histogramBinsCoverTheSpanAndCountEveryValue() {
        val years = listOf(1974, 1994, 1994, 2021, 2025)
        val bins = histogram(years, 1974..2025)

        assertEquals(1974, bins.first().first.first)
        assertTrue(bins.last().first.last >= 2025)
        assertTrue(bins.size <= 24)
        assertEquals(years.size, bins.sumOf { it.second })
        // A short span keeps one bar per value.
        assertEquals(5, histogram(listOf(1, 5), 1..5).size)
    }

    private fun trackedItem(
        id: Long,
        type: MediaType,
        releaseYear: Int? = null,
        progressTotal: Int? = null,
        isOwned: Boolean = false,
        platform: ConsumptionPlatformType? = null,
    ): TrackedMedia = TrackedMedia(
        item = MediaItem(
            id = id,
            type = type,
            title = "Item $id",
            releaseYear = releaseYear,
            progressTotal = progressTotal,
            isOwned = isOwned,
        ),
        sessions = listOf(
            TrackingSession(
                id = id,
                mediaItemId = id,
                sessionNumber = 1,
                status = TrackingStatus.Completed,
                platform = platform?.let { ConsumptionPlatform(name = "Platform", type = it) },
            ),
        ),
    )
}
