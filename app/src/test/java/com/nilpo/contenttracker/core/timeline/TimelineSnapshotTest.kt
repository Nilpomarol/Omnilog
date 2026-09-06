package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the split between [TimelineBuilder.buildEntries] (expensive, derived once off the main
 * thread) and [toSnapshot] (cheap, re-run in composition as filters change). The screens depend on
 * these being interchangeable with a full rebuild.
 */
class TimelineSnapshotTest {
    private val builder = TimelineBuilder()

    private val library = listOf(
        media(id = 1, type = MediaType.Game, finishedAt = LocalDate.of(2026, 6, 6)),
        media(id = 2, type = MediaType.Anime, finishedAt = LocalDate.of(2026, 5, 4)),
        media(id = 3, type = MediaType.Book, finishedAt = LocalDate.of(2025, 3, 2)),
        media(id = 4, type = MediaType.Movie, finishedAt = LocalDate.of(2024, 1, 1)),
    )

    private val filterMatrix = listOf(
        TimelineFilters(),
        TimelineFilters(media = TimelineMediaFilter.Books),
        TimelineFilters(media = TimelineMediaFilter.MoviesAndTv),
        TimelineFilters(year = 2026),
        TimelineFilters(media = TimelineMediaFilter.Anime, year = 2026),
        TimelineFilters(media = TimelineMediaFilter.Anime, year = 2024),
        TimelineFilters(excludedMediaTypes = setOf(MediaType.Game, MediaType.Movie)),
        TimelineFilters(excludedMediaTypes = MediaType.entries.toSet()),
    )

    /**
     * The screens hold one derived entry list and re-filter it on every chip tap, so [toSnapshot]
     * must not disturb its receiver. A rebuild-equivalence assertion would be circular here —
     * [TimelineBuilder.build] delegates to exactly this pair — so this checks reuse instead.
     */
    @Test
    fun repeatedFilteringLeavesTheCachedEntryListUntouched() {
        val cachedEntries = builder.buildEntries(library)
        val snapshotsOnFirstPass = filterMatrix.map { cachedEntries.toSnapshot(it) }

        assertEquals(builder.buildEntries(library), cachedEntries)
        assertEquals(snapshotsOnFirstPass, filterMatrix.map { cachedEntries.toSnapshot(it) })
    }

    @Test
    fun entriesAreNewestFirstSoTakingThePrefixYieldsTheMostRecentActivity() {
        val dates = builder.buildEntries(library).map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2025, 3, 2),
                LocalDate.of(2024, 1, 1),
            ),
            dates,
        )
    }

    @Test
    fun undatedEntriesAreExcludedFromTheGlobalChronology() {
        val undated = TrackedMedia(
            item = MediaItem(id = 5, type = MediaType.Book, title = "Undated", progressTotal = 300),
            sessions = listOf(
                TrackingSession(
                    id = 50,
                    mediaItemId = 5,
                    sessionNumber = 1,
                    status = TrackingStatus.InProgress,
                    progressUpdates = listOf(
                        ProgressUpdate(
                            id = 50,
                            mediaItemId = 5,
                            sessionId = 50,
                            amount = 20,
                            loggedAt = LocalDate.of(2026, 7, 1),
                            hasKnownDate = false,
                            createdAtEpochMillis = 500,
                        ),
                    ),
                ),
            ),
        )
        val withUndated = library + undated
        val entries = builder.buildEntries(withUndated)

        assertEquals(4, entries.size)
        assertTrue(entries.none { it.date == null })
    }

    @Test
    fun hidingMediaTypesShrinksTheUnfilteredCountAndAvailableYears() {
        val snapshot = builder.buildEntries(library)
            .toSnapshot(TimelineFilters(excludedMediaTypes = setOf(MediaType.Game, MediaType.Anime)))

        assertEquals(2, snapshot.unfilteredEntryCount)
        assertEquals(listOf(2025, 2024), snapshot.availableYears)
    }

    /**
     * The full screen reads "the library has events but the settings hide all of them" from
     * `unfilteredEntryCount == 0`, so the media and year chips must leave that count alone.
     */
    @Test
    fun mediaAndYearFiltersDoNotAffectTheUnfilteredCountOrAvailableYears() {
        val cachedEntries = builder.buildEntries(library)

        val snapshot = cachedEntries.toSnapshot(
            TimelineFilters(media = TimelineMediaFilter.Books, year = 2025),
        )

        assertEquals(1, snapshot.entries.size)
        assertEquals(4, snapshot.unfilteredEntryCount)
        assertEquals(listOf(2026, 2025, 2024), snapshot.availableYears)
    }

    @Test
    fun hidingEveryMediaTypeEmptiesTheSnapshotWhileTheLibraryStillHasEntries() {
        val cachedEntries = builder.buildEntries(library)

        val snapshot = cachedEntries.toSnapshot(
            TimelineFilters(excludedMediaTypes = MediaType.entries.toSet()),
        )

        assertTrue(cachedEntries.isNotEmpty())
        assertEquals(0, snapshot.unfilteredEntryCount)
        assertTrue(snapshot.entries.isEmpty())
        assertTrue(snapshot.availableYears.isEmpty())
    }

    @Test
    fun entriesSharingADayCollapseIntoOneGroup() {
        val sameDay = LocalDate.of(2026, 6, 6)
        val entries = builder.buildEntries(
            listOf(
                media(id = 1, type = MediaType.Book, finishedAt = sameDay),
                media(id = 2, type = MediaType.Anime, finishedAt = sameDay),
            ),
        )

        val snapshot = entries.toSnapshot()

        assertEquals(1, snapshot.groups.size)
        assertEquals(sameDay, snapshot.groups.single().date)
        assertEquals(2, snapshot.groups.single().entries.size)
        assertEquals(2, snapshot.entries.size)
    }

    private fun media(
        id: Long,
        type: MediaType,
        finishedAt: LocalDate?,
    ) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Title $id", progressTotal = 300),
        sessions = listOf(
            TrackingSession(
                id = id * 10,
                mediaItemId = id,
                sessionNumber = 1,
                status = TrackingStatus.Completed,
                startedAt = null,
                finishedAt = finishedAt,
                ratingHalfPoints = null,
                progressUpdates = emptyList(),
            ),
        ),
    )
}
