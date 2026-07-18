package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StatsCalculatorTest {
    private val today = LocalDate.of(2026, 7, 8)
    private val calculator = StatsCalculator(today)

    @Test
    fun thisYearStatsUseFinishedCompletedSessions() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                progressTotal = 300,
                genres = listOf("Sci-Fi"),
                creators = listOf("Author One"),
                language = "ca",
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        progressCurrent = 300,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 7, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                progressTotal = 200,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        progressCurrent = 200,
                        rating = 9,
                        finishedAt = LocalDate.of(2025, 12, 31),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(2, snapshot.totalTitles)
        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertEquals(1, snapshot.completionSessions)
        assertEquals(8.0, snapshot.averageRating ?: 0.0, 0.001)
        val ratingEight = snapshot.ratingDistribution.first { it.key == "8" }
        assertEquals(1, ratingEight.value)
        assertEquals(1, ratingEight.segments.first { it.mediaType == MediaType.Book }.value)
        assertEquals(300, snapshot.progressTotals.first { it.mediaType == MediaType.Book }.value)
        val bookStats = snapshot.mediumStats.first { it.mediaType == MediaType.Book }
        assertEquals(1, bookStats.completionSessionCount)
        assertEquals(8.0, bookStats.averageRating ?: 0.0, 0.001)
        assertEquals(300.0, bookStats.averageLength ?: 0.0, 0.001)
        assertEquals(1, snapshot.topGenres.first { it.label == "Sci-Fi" }.value)
        assertEquals(1, snapshot.topCreators.first { it.label == "Author One" }.value)
        assertEquals(1, snapshot.languageBreakdown.first { it.code == "ca" }.value)
    }

    @Test
    fun mediaFiltersLimitStatsToSelectedTypes() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                progressTotal = 120,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        progressCurrent = 120,
                        rating = 7,
                        finishedAt = LocalDate.of(2026, 3, 10),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Game,
                progressTotal = 40,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        progressCurrent = 40,
                        rating = 10,
                        finishedAt = LocalDate.of(2026, 3, 12),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Game),
            ),
        )

        assertEquals(1, snapshot.totalTitles)
        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertEquals(1, snapshot.completionSessions)
        assertEquals(10.0, snapshot.averageRating ?: 0.0, 0.001)
        assertEquals(40, snapshot.progressTotals.first { it.mediaType == MediaType.Game }.value)
    }

    @Test
    fun allTimeTotalsIncludeUndatedCompletionsButMonthlyChartDoesNot() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                progressTotal = 240,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        progressCurrent = 240,
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                progressTotal = 180,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        progressCurrent = 180,
                        finishedAt = LocalDate.of(2025, 12, 31),
                    ),
                ),
            ),
        )

        val allTime = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.AllTime,
                mediaTypes = setOf(MediaType.Book),
            ),
        )
        val thisYear = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(2, allTime.uniqueTitlesCompleted)
        assertEquals(2, allTime.completionSessions)
        assertEquals(1, allTime.completionSessionsByMonth.sumOf { bucket -> bucket.value })
        assertEquals(0, thisYear.uniqueTitlesCompleted)
        assertEquals(0, thisYear.completionSessions)
    }

    @Test
    fun yearPeriodScopesStatsToSelectedCalendarYear() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 9,
                        finishedAt = LocalDate.of(2025, 3, 12),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        rating = 6,
                        finishedAt = LocalDate.of(2026, 3, 12),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.Year(2025),
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertEquals(1, snapshot.completionSessions)
        assertEquals(9.0, snapshot.averageRating ?: 0.0, 0.001)
        assertEquals(12, snapshot.completionSessionsByMonth.size)
        assertEquals(1, snapshot.completionSessionsByMonth.first { bucket -> bucket.key == "2025-03" }.value)
    }

    @Test
    fun uniqueTitleAndCompletionSessionDefinitionsStayDistinct() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 2, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 6, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertEquals(2, snapshot.completionSessions)
        assertEquals(2, snapshot.completionSessionsByMonth.sumOf { bucket -> bucket.value })
        assertEquals(2, snapshot.mediumStats.single().completionSessionCount)
    }

    @Test
    fun ratingTrendAveragesRatingsByMonth() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 6,
                        finishedAt = LocalDate.of(2026, 1, 4),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 1, 20),
                    ),
                    session(
                        id = 3,
                        mediaItemId = 1,
                        sessionNumber = 3,
                        status = TrackingStatus.Completed,
                        rating = 10,
                        finishedAt = LocalDate.of(2026, 2, 3),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )
        val january = snapshot.ratingTrend.first { point -> point.key == "2026-01" }
        val february = snapshot.ratingTrend.first { point -> point.key == "2026-02" }

        assertEquals(7.0, january.averageRating ?: 0.0, 0.001)
        assertEquals(2, january.ratingCount)
        assertEquals(10.0, february.averageRating ?: 0.0, 0.001)
        assertEquals(1, february.ratingCount)
    }

    @Test
    fun monthlyActivityKeepsMediaTypeSegments() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 7, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Game,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 7, 2),
                    ),
                ),
            ),
            trackedMedia(
                id = 3,
                type = MediaType.Movie,
                sessions = listOf(
                    session(
                        id = 3,
                        mediaItemId = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 6, 15),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = MediaType.entries.toSet(),
            ),
        )
        val july = snapshot.completionSessionsByMonth.first { bucket -> bucket.key == "2026-07" }

        assertEquals(2, july.value)
        assertEquals(1, july.segments.first { segment -> segment.mediaType == MediaType.Book }.value)
        assertEquals(1, july.segments.first { segment -> segment.mediaType == MediaType.Game }.value)
    }

    @Test
    fun currentStatusStatsAreCurrentStateBased() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Anime,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2024, 1, 2),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.InProgress,
                        startedAt = LocalDate.of(2026, 6, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Anime,
                sessions = listOf(
                    session(
                        id = 3,
                        mediaItemId = 2,
                        status = TrackingStatus.Planned,
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Anime),
            ),
        )

        assertEquals(1, snapshot.activeNow)
        assertEquals(1, snapshot.plannedNow)
        assertEquals(1, snapshot.revisitCount)
        assertEquals(1, snapshot.statusBreakdown.first { it.status == TrackingStatus.InProgress }.value)
        assertEquals(1, snapshot.statusBreakdown.first { it.status == TrackingStatus.Planned }.value)
    }

    @Test
    fun revisitStatsRespectPeriodAndMediaType() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 4, 10),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 2, 12),
                    ),
                    session(
                        id = 3,
                        mediaItemId = 1,
                        sessionNumber = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2024, 9, 20),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.TvShow,
                sessions = listOf(
                    session(
                        id = 4,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 1, 5),
                    ),
                    session(
                        id = 5,
                        mediaItemId = 2,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 3, 7),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = MediaType.entries.toSet(),
            ),
        )

        assertEquals(2, snapshot.revisitCount)
        assertEquals(1, snapshot.revisitBreakdown.first { it.mediaType == MediaType.Book }.value)
        assertEquals(1, snapshot.revisitBreakdown.first { it.mediaType == MediaType.TvShow }.value)
        assertEquals(1, snapshot.mostRevisitedItems.first { it.trackedMedia.item.id == 1L }.value)
    }

    @Test
    fun thisYearMonthRangeExcludesFutureMonths() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 1, 4),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // today is 2026-07-08, so the chart must stop at July, not extend to Dec.
        assertEquals(7, snapshot.completionSessionsByMonth.size)
        assertEquals("2026-07", snapshot.completionSessionsByMonth.last().key)
    }

    @Test
    fun metadataStatsStayEmptyWhenNoPeriodCompletions() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                progressTotal = 200,
                genres = listOf("Sci-Fi"),
                creators = listOf("Author One"),
                language = "ca",
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        progressCurrent = 200,
                        finishedAt = LocalDate.of(2025, 12, 31),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // The only completion is out of period, so content-mix must stay empty
        // instead of silently falling back to the whole library.
        assertTrue(snapshot.topGenres.none { it.label == "Sci-Fi" })
        assertTrue(snapshot.topCreators.none { it.label == "Author One" })
        assertTrue(snapshot.languageBreakdown.none { it.code == "ca" })
    }

    @Test
    fun periodDeltaComparesAgainstPreviousYear() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    // current period (2026): 2 completions, ratings 8 and 6 -> avg 7
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 3, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        rating = 6,
                        finishedAt = LocalDate.of(2026, 5, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                sessions = listOf(
                    // same period of 2025 (Jan 1 - Jul 8): 1 completion, rating 9
                    session(
                        id = 3,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        rating = 9,
                        finishedAt = LocalDate.of(2025, 6, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // The hero compares completion sessions, matching the monthly chart:
        // 2 current sessions vs 1 previous session -> +1.
        assertEquals(1, snapshot.deltas.completionSessions)
        // avg 7.0 now vs 9.0 before -> -2.0
        assertEquals(-2.0, snapshot.deltas.averageRating ?: 0.0, 0.001)
        // 1 revisit now (session 2) vs 0 before -> +1
        assertEquals(1, snapshot.deltas.revisits)
    }

    @Test
    fun thisYearDeltaComparesYearToDateNotFullPreviousYear() {
        // today is 2026-07-08, so the comparison window is 2025-01-01..2025-07-08.
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 3, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 2,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 5, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 3,
                type = MediaType.Book,
                sessions = listOf(
                    // inclusive end of the previous window: counts
                    session(
                        id = 3,
                        mediaItemId = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 7, 8),
                    ),
                ),
            ),
            trackedMedia(
                id = 4,
                type = MediaType.Book,
                sessions = listOf(
                    // one day past the year-to-date cutoff: excluded
                    session(
                        id = 4,
                        mediaItemId = 4,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 7, 9),
                    ),
                ),
            ),
            trackedMedia(
                id = 5,
                type = MediaType.Book,
                sessions = listOf(
                    // late previous year: excluded from a year-to-date comparison
                    session(
                        id = 5,
                        mediaItemId = 5,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 12, 31),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // 2 completed now vs 1 in the same date range of 2025 -> +1.
        // The old whole-previous-year window would have produced 2 - 3 = -1.
        assertEquals(1, snapshot.deltas.completionSessions)
    }

    @Test
    fun deltaBasisNamesTheComparisonWindow() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 3, 1),
                    ),
                ),
            ),
        )

        fun basisFor(period: StatsPeriod): ComparisonBasis? {
            return calculator.calculate(
                items = items,
                filters = StatsFilters(period = period, mediaTypes = setOf(MediaType.Book)),
            ).deltas.basis
        }

        assertEquals(ComparisonBasis.SamePeriodOfYear(2025), basisFor(StatsPeriod.ThisYear))
        assertEquals(ComparisonBasis.FullYear(2024), basisFor(StatsPeriod.Year(2025)))
        assertEquals(ComparisonBasis.Previous12Months, basisFor(StatsPeriod.Last12Months))
        assertNull(basisFor(StatsPeriod.AllTime))
    }

    @Test
    fun periodDeltaIsNullForAllTime() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 3, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.AllTime,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // AllTime has no previous comparison window -> no chips should render.
        assertNull(snapshot.deltas.completionSessions)
        assertNull(snapshot.deltas.averageRating)
        assertNull(snapshot.deltas.revisits)
    }

    @Test
    fun periodDeltaHidesChipWhenPreviousWindowHasNoData() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    // current period has a completion; previous window (2025) has none.
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 3, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        // completed delta still computable (+1); rating delta is null because
        // the previous window had no ratings, so the UI omits that chip.
        assertEquals(1, snapshot.deltas.completionSessions)
        assertNull(snapshot.deltas.averageRating)
    }

    @Test
    fun allTimeContentMixUsesOnlyDistinctCompletedTitles() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                genres = listOf("Sci-Fi"),
                creators = listOf("Author One"),
                language = "ca",
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 2, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                genres = listOf("Fantasy"),
                creators = listOf("Author Two"),
                language = "en",
                sessions = listOf(
                    session(
                        id = 3,
                        mediaItemId = 2,
                        status = TrackingStatus.Planned,
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.AllTime,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.topGenres.single { it.label == "Sci-Fi" }.value)
        assertTrue(snapshot.topGenres.none { it.label == "Fantasy" })
        assertEquals(1, snapshot.topCreators.single { it.label == "Author One" }.value)
        assertTrue(snapshot.topCreators.none { it.label == "Author Two" })
        assertEquals(1, snapshot.languageBreakdown.single { it.code == "ca" }.value)
        assertTrue(snapshot.languageBreakdown.none { it.code == "en" })
    }

    @Test
    fun bestRatedBadgeUsesOnlyRatingsInsideActivePeriodAndMediaFilter() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        rating = 10,
                        finishedAt = LocalDate.of(2025, 5, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        rating = 7,
                        finishedAt = LocalDate.of(2026, 5, 1),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Game,
                sessions = listOf(
                    session(
                        id = 3,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        rating = 9,
                        finishedAt = LocalDate.of(2026, 5, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.bestRatedItems.size)
        assertEquals(1L, snapshot.bestRatedItems.single().trackedMedia.item.id)
        assertEquals(7, snapshot.bestRatedItems.single().bestRating)
    }

    @Test
    fun last12MonthsUsesMatchingCurrentAndPreviousPartialMonthBoundaries() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 8, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 7, 8),
                    ),
                    session(
                        id = 3,
                        mediaItemId = 1,
                        sessionNumber = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 7, 31),
                    ),
                    session(
                        id = 4,
                        mediaItemId = 1,
                        sessionNumber = 4,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 7, 9),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 5,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2024, 8, 1),
                    ),
                    session(
                        id = 6,
                        mediaItemId = 2,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 7, 8),
                    ),
                    session(
                        id = 7,
                        mediaItemId = 2,
                        sessionNumber = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 7, 9),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.Last12Months,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(2, snapshot.completionSessions)
        assertEquals(0, snapshot.deltas.completionSessions)
        assertEquals(ComparisonBasis.Previous12Months, snapshot.deltas.basis)
    }

    @Test
    fun currentYearPeriodExcludesFutureDatesAndUsesYearToDateComparison() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 1, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 7, 9),
                    ),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 3,
                        mediaItemId = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 1, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.Year(2026),
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.completionSessions)
        assertEquals(0, snapshot.deltas.completionSessions)
        assertEquals(ComparisonBasis.SamePeriodOfYear(2025), snapshot.deltas.basis)
    }

    @Test
    fun historicalYearIncludesBothCalendarBoundariesOnly() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    session(
                        id = 1,
                        mediaItemId = 1,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 1, 1),
                    ),
                    session(
                        id = 2,
                        mediaItemId = 1,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2025, 12, 31),
                    ),
                    session(
                        id = 3,
                        mediaItemId = 1,
                        sessionNumber = 3,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2024, 12, 31),
                    ),
                    session(
                        id = 4,
                        mediaItemId = 1,
                        sessionNumber = 4,
                        status = TrackingStatus.Completed,
                        finishedAt = LocalDate.of(2026, 1, 1),
                    ),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(
                period = StatsPeriod.Year(2025),
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(2, snapshot.completionSessions)
        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertEquals(2, snapshot.completionSessionsByMonth.sumOf { bucket -> bucket.value })
        assertEquals(ComparisonBasis.FullYear(2024), snapshot.deltas.basis)
    }

    @Test
    fun topLevelSummaryDegradesGracefullyForEmptyData() {
        val snapshot = calculator.calculate(
            items = emptyList(),
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = MediaType.entries.toSet(),
            ),
        )

        assertEquals(0, snapshot.uniqueTitlesCompleted)
        assertNull(snapshot.averageRating)
        assertEquals(0, snapshot.revisitCount)
        assertNull(snapshot.topLevelSummary.consumptionHighlight)
        assertNull(snapshot.topLevelSummary.observation)
    }

    @Test
    fun sparseSummaryKeepsActivityWithoutInventingRatingOrConsumption() {
        val snapshot = calculator.calculate(
            items = listOf(
                trackedMedia(
                    id = 1,
                    type = MediaType.Book,
                    sessions = listOf(
                        session(
                            id = 1,
                            mediaItemId = 1,
                            status = TrackingStatus.Completed,
                            finishedAt = LocalDate.of(2026, 6, 1),
                        ),
                    ),
                ),
            ),
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = setOf(MediaType.Book),
            ),
        )

        assertEquals(1, snapshot.uniqueTitlesCompleted)
        assertNull(snapshot.averageRating)
        assertEquals(0, snapshot.revisitCount)
        assertNull(snapshot.topLevelSummary.consumptionHighlight)
        val observation = snapshot.topLevelSummary.observation as StatsObservation.BusiestMonth
        assertEquals("2026-06", observation.key)
        assertEquals(1, observation.completionSessions)
    }

    @Test
    fun denseSummaryChoosesConsumptionByComparableSessionCountNotUnitMagnitude() {
        val snapshot = calculator.calculate(
            items = listOf(
                trackedMedia(
                    id = 1,
                    type = MediaType.Book,
                    progressTotal = 2_000,
                    sessions = listOf(
                        session(
                            id = 1,
                            mediaItemId = 1,
                            status = TrackingStatus.Completed,
                            progressCurrent = 2_000,
                            rating = 9,
                            finishedAt = LocalDate.of(2026, 5, 1),
                        ),
                    ),
                ),
                trackedMedia(
                    id = 2,
                    type = MediaType.Game,
                    progressTotal = 2,
                    sessions = listOf(
                        session(
                            id = 2,
                            mediaItemId = 2,
                            status = TrackingStatus.Completed,
                            progressCurrent = 2,
                            rating = 7,
                            finishedAt = LocalDate.of(2026, 5, 2),
                        ),
                        session(
                            id = 3,
                            mediaItemId = 2,
                            sessionNumber = 2,
                            status = TrackingStatus.Completed,
                            progressCurrent = 2,
                            rating = 8,
                            finishedAt = LocalDate.of(2026, 6, 2),
                        ),
                    ),
                ),
            ),
            filters = StatsFilters(
                period = StatsPeriod.ThisYear,
                mediaTypes = MediaType.entries.toSet(),
            ),
        )

        val highlight = snapshot.topLevelSummary.consumptionHighlight
        assertEquals(MediaType.Game, highlight?.mediaType)
        assertEquals(4, highlight?.value)
        assertEquals(2, snapshot.uniqueTitlesCompleted)
        assertEquals(8.0, snapshot.averageRating ?: 0.0, 0.001)
        assertEquals(1, snapshot.revisitCount)
    }

    @Test
    fun summaryRespondsToEveryPeriodAndMediaFilter() {
        val items = MediaType.entries.mapIndexed { index, mediaType ->
            val id = (index + 1).toLong()
            trackedMedia(
                id = id,
                type = mediaType,
                progressTotal = (index + 1) * 10,
                sessions = listOf(
                    session(
                        id = id * 10,
                        mediaItemId = id,
                        status = TrackingStatus.Completed,
                        progressCurrent = (index + 1) * 10,
                        rating = 8,
                        finishedAt = LocalDate.of(2026, 6, 1),
                    ),
                    session(
                        id = id * 10 + 1,
                        mediaItemId = id,
                        sessionNumber = 2,
                        status = TrackingStatus.Completed,
                        progressCurrent = (index + 1) * 10,
                        rating = 6,
                        finishedAt = LocalDate.of(2025, 6, 1),
                    ),
                ),
            )
        }
        val mediaFilters = listOf(
            MediaType.entries.toSet(),
            setOf(MediaType.Anime),
            setOf(MediaType.Book),
            setOf(MediaType.Movie),
            setOf(MediaType.TvShow),
            setOf(MediaType.Game),
            setOf(MediaType.Movie, MediaType.TvShow),
        )
        val periods = listOf(
            StatsPeriod.AllTime,
            StatsPeriod.ThisYear,
            StatsPeriod.Last12Months,
            StatsPeriod.Year(2025),
        )

        periods.forEach { period ->
            mediaFilters.forEach { mediaTypes ->
                val snapshot = calculator.calculate(
                    items = items,
                    filters = StatsFilters(period = period, mediaTypes = mediaTypes),
                )
                val includesBothYears = period == StatsPeriod.AllTime
                val isPreviousYear = period == StatsPeriod.Year(2025)
                val expectedAverage = when {
                    includesBothYears -> 7.0
                    isPreviousYear -> 6.0
                    else -> 8.0
                }
                val expectedRevisits = if (includesBothYears || isPreviousYear) mediaTypes.size else 0
                val expectedHighlightType = mediaTypes.minBy { mediaType -> mediaType.ordinal }
                val perSessionValue = (expectedHighlightType.ordinal + 1) * 10
                val expectedHighlightValue = perSessionValue * if (includesBothYears) 2 else 1
                val expectedObservationYear = if (isPreviousYear) 2025 else 2026

                assertEquals(mediaTypes.size, snapshot.uniqueTitlesCompleted)
                assertEquals(expectedAverage, snapshot.averageRating ?: 0.0, 0.001)
                assertEquals(expectedRevisits, snapshot.revisitCount)
                assertEquals(expectedHighlightType, snapshot.topLevelSummary.consumptionHighlight?.mediaType)
                assertEquals(expectedHighlightValue, snapshot.topLevelSummary.consumptionHighlight?.value)
                val observation = snapshot.topLevelSummary.observation as StatsObservation.BusiestMonth
                assertEquals("$expectedObservationYear-06", observation.key)
                assertEquals(mediaTypes.size, observation.completionSessions)
            }
        }
    }

    @Test
    fun highestRatedMediumIsUsedOnlyAsMeaningfulFallbackObservation() {
        val snapshot = calculator.calculate(
            items = listOf(
                trackedMedia(
                    id = 1,
                    type = MediaType.Book,
                    sessions = listOf(
                        session(
                            id = 1,
                            mediaItemId = 1,
                            status = TrackingStatus.Completed,
                            rating = 9,
                        ),
                    ),
                ),
                trackedMedia(
                    id = 2,
                    type = MediaType.Game,
                    sessions = listOf(
                        session(
                            id = 2,
                            mediaItemId = 2,
                            status = TrackingStatus.Completed,
                            rating = 7,
                        ),
                    ),
                ),
            ),
            filters = StatsFilters(
                period = StatsPeriod.AllTime,
                mediaTypes = MediaType.entries.toSet(),
            ),
        )

        val observation = snapshot.topLevelSummary.observation as StatsObservation.HighestRatedMedium
        assertEquals(MediaType.Book, observation.mediaType)
        assertEquals(9.0, observation.averageRating, 0.001)
        assertNull(snapshot.topLevelSummary.consumptionHighlight)
    }

    private fun trackedMedia(
        id: Long,
        type: MediaType,
        progressTotal: Int? = null,
        genres: List<String> = emptyList(),
        creators: List<String> = emptyList(),
        language: String? = null,
        sessions: List<TrackingSession>,
    ): TrackedMedia {
        return TrackedMedia(
            item = MediaItem(
                id = id,
                type = type,
                title = "Item $id",
                progressTotal = progressTotal,
                genres = genres,
                creators = creators,
                language = language,
            ),
            sessions = sessions,
        )
    }

    private fun session(
        id: Long,
        mediaItemId: Long,
        sessionNumber: Int = 1,
        status: TrackingStatus,
        progressCurrent: Int = 0,
        rating: Int? = null,
        startedAt: LocalDate? = null,
        finishedAt: LocalDate? = null,
    ): TrackingSession {
        return TrackingSession(
            id = id,
            mediaItemId = mediaItemId,
            sessionNumber = sessionNumber,
            status = status,
            progressCurrent = progressCurrent,
            rating = rating,
            startedAt = startedAt,
            finishedAt = finishedAt,
        )
    }
}
