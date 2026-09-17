package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * STATS-03: the chart data behind each presentation must keep its expected
 * scale, preserve gaps, and never mix incomparable units or raw stored codes.
 */
class StatsChartFormsTest {
    private val today = LocalDate.of(2026, 7, 8)
    private val calculator = StatsCalculator(today)

    // --- Rating distribution: fixed 1-10 scale ---

    @Test
    fun ratingDistributionListsEveryPositionIncludingUnusedScores() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, rating = 3, finishedAt = LocalDate.of(2026, 2, 1)),
                    completedSession(id = 2, mediaItemId = 1, sessionNumber = 2, rating = 9, finishedAt = LocalDate.of(2026, 3, 1)),
                    completedSession(id = 3, mediaItemId = 1, sessionNumber = 3, rating = 9, finishedAt = LocalDate.of(2026, 4, 1)),
                ),
            ),
        )

        val distribution = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = setOf(MediaType.Book)),
        ).ratingDistribution

        assertEquals((1..10).map { it.toString() }, distribution.map { bucket -> bucket.key })
        assertEquals(1, distribution.first { it.key == "3" }.value)
        assertEquals(2, distribution.first { it.key == "9" }.value)
        // Entirely missing scores stay visible as zero-value buckets.
        assertEquals(0, distribution.first { it.key == "5" }.value)
        assertTrue(distribution.first { it.key == "5" }.segments.isEmpty())
        assertEquals(0, distribution.first { it.key == "10" }.value)
    }

    @Test
    fun ratingDistributionCountsAHalfPointUnderTheWholePointAboveIt() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    // 7,5 and 8: the chart has ten bars, so both are counted at 8 and the average
                    // below is what keeps the half visible.
                    completedSession(id = 1, mediaItemId = 1, ratingHalfPoints = 15, finishedAt = LocalDate.of(2026, 2, 1)),
                    completedSession(id = 2, mediaItemId = 1, sessionNumber = 2, ratingHalfPoints = 16, finishedAt = LocalDate.of(2026, 3, 1)),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = setOf(MediaType.Book)),
        )

        assertEquals(2, snapshot.ratingDistribution.first { it.key == "8" }.value)
        assertEquals(0, snapshot.ratingDistribution.first { it.key == "7" }.value)
        assertEquals(7.75, snapshot.averageRating!!, 0.0001)
    }

    @Test
    fun ratingDistributionForEmptyLibraryStillListsAllPositions() {
        val distribution = calculator.calculate(
            items = emptyList(),
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
        ).ratingDistribution

        assertEquals(10, distribution.size)
        assertTrue(distribution.all { bucket -> bucket.value == 0 })
    }

    // --- Rating trend: gaps stay gaps, counts stay per point ---

    @Test
    fun ratingTrendKeepsGapMonthsAsUnratedPoints() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, rating = 8, finishedAt = LocalDate.of(2026, 1, 10)),
                    completedSession(id = 2, mediaItemId = 1, sessionNumber = 2, rating = 6, finishedAt = LocalDate.of(2026, 4, 10)),
                    completedSession(id = 3, mediaItemId = 1, sessionNumber = 3, rating = 7, finishedAt = LocalDate.of(2026, 4, 20)),
                ),
            ),
        )

        val trend = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = setOf(MediaType.Book)),
        ).ratingTrend

        // Jan..Jul of 2026: real monthly positions are preserved.
        assertEquals(7, trend.size)
        assertEquals(8.0, trend.first { it.key == "2026-01" }.averageRating ?: 0.0, 0.001)
        assertEquals(1, trend.first { it.key == "2026-01" }.ratingCount)
        assertNull(trend.first { it.key == "2026-02" }.averageRating)
        assertEquals(0, trend.first { it.key == "2026-02" }.ratingCount)
        assertNull(trend.first { it.key == "2026-03" }.averageRating)
        assertEquals(6.5, trend.first { it.key == "2026-04" }.averageRating ?: 0.0, 0.001)
        assertEquals(2, trend.first { it.key == "2026-04" }.ratingCount)
        assertNull(trend.first { it.key == "2026-07" }.averageRating)
    }

    @Test
    fun trendSegmentsNeverBridgeMonthsWithoutRatings() {
        fun point(key: String, rating: Double?, count: Int) = RatingTrendPoint(
            key = key,
            label = key,
            averageRating = rating,
            ratingCount = count,
        )

        // Isolated rated months separated by a gap: nothing to connect.
        assertEquals(
            emptyList<Pair<Int, Int>>(),
            connectedRatingTrendSegments(
                listOf(
                    point("2026-01", 8.0, 1),
                    point("2026-02", null, 0),
                    point("2026-03", 6.0, 2),
                ),
            ),
        )
        // Only adjacent rated months connect around the gap.
        assertEquals(
            listOf(0 to 1, 3 to 4),
            connectedRatingTrendSegments(
                listOf(
                    point("2026-01", 8.0, 1),
                    point("2026-02", 7.0, 2),
                    point("2026-03", null, 0),
                    point("2026-04", 6.0, 1),
                    point("2026-05", 9.0, 3),
                ),
            ),
        )
        assertEquals(emptyList<Pair<Int, Int>>(), connectedRatingTrendSegments(emptyList()))
        assertEquals(
            emptyList<Pair<Int, Int>>(),
            connectedRatingTrendSegments(listOf(point("2026-01", 8.0, 1))),
        )
    }

    // --- Consumption: each medium keeps its own unit ---

    @Test
    fun progressTotalsStayPerMediumWithoutCrossUnitAggregation() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                progressTotal = 300,
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, progressCurrent = 300, finishedAt = LocalDate.of(2026, 2, 1)),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Movie,
                progressTotal = 120,
                sessions = listOf(
                    completedSession(id = 2, mediaItemId = 2, progressCurrent = 120, finishedAt = LocalDate.of(2026, 3, 1)),
                ),
            ),
            trackedMedia(
                id = 3,
                type = MediaType.Game,
                progressTotal = 40,
                sessions = listOf(
                    completedSession(id = 3, mediaItemId = 3, progressCurrent = 40, finishedAt = LocalDate.of(2026, 4, 1)),
                ),
            ),
        )

        val totals = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
        ).progressTotals

        assertEquals(300, totals.single { it.mediaType == MediaType.Book }.value)
        assertEquals(120, totals.single { it.mediaType == MediaType.Movie }.value)
        assertEquals(40, totals.single { it.mediaType == MediaType.Game }.value)
        // Pages, minutes, and hours are never merged into one value.
        assertEquals(3, totals.size)
    }

    @Test
    fun estimatedTimeConvertsEachMediumWithItsOwnFactor() {
        val totals = listOf(
            ProgressTotalStats(mediaType = MediaType.Anime, value = 10),
            ProgressTotalStats(mediaType = MediaType.Book, value = 100),
            ProgressTotalStats(mediaType = MediaType.Movie, value = 120),
            ProgressTotalStats(mediaType = MediaType.TvShow, value = 4),
            ProgressTotalStats(mediaType = MediaType.Game, value = 3),
        )

        val estimated = estimatedTimeByMedium(totals)

        fun minutesOf(mediaType: MediaType) = estimated.single { it.mediaType == mediaType }.minutes
        assertEquals(220.0, minutesOf(MediaType.Anime), 0.001)
        assertEquals(125.0, minutesOf(MediaType.Book), 0.001)
        assertEquals(120.0, minutesOf(MediaType.Movie), 0.001)
        assertEquals(200.0, minutesOf(MediaType.TvShow), 0.001)
        assertEquals(180.0, minutesOf(MediaType.Game), 0.001)
    }

    @Test
    fun estimatedTimeSkipsMediaWithoutConsumption() {
        val estimated = estimatedTimeByMedium(
            listOf(
                ProgressTotalStats(mediaType = MediaType.Book, value = 80),
                ProgressTotalStats(mediaType = MediaType.Game, value = 0),
            ),
        )

        // A single medium remains available, but zero totals never render and
        // the UI requires two media before showing the comparison at all.
        assertEquals(listOf(MediaType.Book), estimated.map { it.mediaType })
        assertEquals(100.0, estimated.single().minutes, 0.001)
        assertTrue(estimatedTimeByMedium(emptyList()).isEmpty())
    }

    @Test
    fun mediumStatsExposeWhetherCompletionShareIsComparable() {
        fun completionMediumCount(items: List<TrackedMedia>): Int {
            return calculator.calculate(
                items = items,
                filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
            ).mediumStats.count { stat -> stat.completionSessionCount > 0 }
        }

        val oneMedium = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, finishedAt = LocalDate.of(2026, 2, 1)),
                ),
            ),
        )
        val twoMedia = oneMedium + trackedMedia(
            id = 2,
            type = MediaType.Game,
            sessions = listOf(
                completedSession(id = 2, mediaItemId = 2, finishedAt = LocalDate.of(2026, 3, 1)),
            ),
        )

        // One medium with data: the completion-share module must stay hidden.
        assertEquals(1, completionMediumCount(oneMedium))
        // Two media with data: a part-to-whole share becomes interpretable.
        assertEquals(2, completionMediumCount(twoMedia))
        assertEquals(0, completionMediumCount(emptyList()))
    }

    // --- Genres are overlapping mentions, not exclusive shares ---

    @Test
    fun genreCountsAreOverlappingMentionsNotExclusiveShares() {
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                genres = listOf("Sci-Fi", "Drama"),
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, finishedAt = LocalDate.of(2026, 2, 1)),
                ),
            ),
            trackedMedia(
                id = 2,
                type = MediaType.Movie,
                genres = listOf("Drama"),
                sessions = listOf(
                    completedSession(id = 2, mediaItemId = 2, finishedAt = LocalDate.of(2026, 3, 1)),
                ),
            ),
        )

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
        )

        assertEquals(2, snapshot.topGenres.first { it.label == "Drama" }.value)
        assertEquals(1, snapshot.topGenres.first { it.label == "Sci-Fi" }.value)
        // Mentions across genres exceed the completed-title count, so a
        // part-to-whole reading of these numbers would be wrong.
        val mentionTotal = snapshot.topGenres.sumOf { it.value }
        assertTrue(mentionTotal > snapshot.uniqueTitlesCompleted)
    }

    @Test
    fun languageBreakdownNormalizesKnownLegacyCustomAndUnknownValues() {
        fun bookWithLanguage(id: Long, language: String?): TrackedMedia {
            return trackedMedia(
                id = id,
                type = MediaType.Book,
                language = language,
                sessions = listOf(
                    completedSession(id = id, mediaItemId = id, finishedAt = LocalDate.of(2026, 2, 1)),
                ),
            )
        }

        val items = listOf(
            bookWithLanguage(1, "ca"),
            bookWithLanguage(2, "en"),
            bookWithLanguage(3, "english"),
            bookWithLanguage(4, "ENG"),
            bookWithLanguage(5, "Klingon"),
            bookWithLanguage(6, null),
            bookWithLanguage(7, "  "),
        )

        val breakdown = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = setOf(MediaType.Book)),
        ).languageBreakdown

        // Legacy variants collapse into one normalized bucket.
        assertEquals(3, breakdown.stats.single { it.code == "en" }.value)
        assertEquals(1, breakdown.stats.single { it.code == "ca" }.value)
        // Custom values survive as their own bucket.
        assertEquals(1, breakdown.stats.single { it.code == "Klingon" }.value)
        assertTrue(breakdown.stats.none { it.code == "english" || it.code == "ENG" })
        // Missing and blank values get no block: they are the denominator, not a language.
        assertTrue(breakdown.stats.none { it.code == null })
        assertEquals(5, breakdown.recordedTitles)
        assertEquals(7, breakdown.totalTitles)
        // Largest first, then alphabetical.
        assertEquals(listOf("en", "ca", "Klingon"), breakdown.stats.map { it.code })
    }

    @Test
    fun languageBreakdownIsEmptyForEmptyLibrary() {
        val breakdown = calculator.calculate(
            items = emptyList(),
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
        ).languageBreakdown

        assertTrue(breakdown.stats.isEmpty())
        assertTrue(breakdown.includedMediaTypes.isEmpty())
        assertTrue(breakdown.excludedMediaTypes.isEmpty())
    }

    // --- Dense data keeps every guarantee at once ---

    @Test
    fun denseLibraryKeepsScaleGapAndUnitGuaranteesTogether() {
        val items = MediaType.entries.mapIndexed { index, mediaType ->
            val id = (index + 1).toLong()
            trackedMedia(
                id = id,
                type = mediaType,
                progressTotal = (index + 1) * 100,
                genres = listOf("Genre$index", "Shared"),
                language = if (index % 2 == 0) "ca" else "english",
                sessions = listOf(
                    completedSession(
                        id = id * 10,
                        mediaItemId = id,
                        progressCurrent = (index + 1) * 100,
                        rating = index + 2,
                        finishedAt = LocalDate.of(2026, 1 + index, 5),
                    ),
                ),
            )
        }

        val snapshot = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = MediaType.entries.toSet()),
        )

        assertEquals(10, snapshot.ratingDistribution.size)
        assertEquals(MediaType.entries.size, snapshot.progressTotals.size)
        assertEquals(MediaType.entries.size, snapshot.topGenres.first { it.label == "Shared" }.value)
        // One title per medium, so no medium can clear the language coverage floor however
        // faithfully each one is tagged.
        assertEquals(MediaType.entries.size, snapshot.languageBreakdown.excludedMediaTypes.size)
        assertTrue(snapshot.languageBreakdown.stats.isEmpty())
        // Ratings land in months 1..5, so every adjacent pair connects and
        // the unrated remaining months stay disconnected.
        assertEquals(
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4),
            connectedRatingTrendSegments(snapshot.ratingTrend),
        )
    }

    private fun trackedMedia(
        id: Long,
        type: MediaType,
        progressTotal: Int? = null,
        genres: List<String> = emptyList(),
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
                language = language,
            ),
            sessions = sessions,
        )
    }

    private fun completedSession(
        id: Long,
        mediaItemId: Long,
        sessionNumber: Int = 1,
        progressCurrent: Int = 0,
        rating: Int? = null,
        ratingHalfPoints: Int? = rating?.let(RatingHalfPoints::fromWholePoints),
        finishedAt: LocalDate? = null,
    ): TrackingSession {
        return TrackingSession(
            id = id,
            mediaItemId = mediaItemId,
            sessionNumber = sessionNumber,
            status = TrackingStatus.Completed,
            progressCurrent = progressCurrent,
            ratingHalfPoints = ratingHalfPoints,
            finishedAt = finishedAt,
        )
    }
}
