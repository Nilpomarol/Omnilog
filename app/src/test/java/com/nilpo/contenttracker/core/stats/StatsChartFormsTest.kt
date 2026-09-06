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
    fun genrePieDataKeepsTopFiveAndAggregatesTheRestAsOthers() {
        val stats = listOf(
            RankedStat("A", 10),
            RankedStat("B", 8),
            RankedStat("C", 6),
            RankedStat("D", 5),
            RankedStat("E", 4),
            RankedStat("F", 3),
            RankedStat("G", 1),
        )

        val pie = genrePieData(stats)

        assertEquals(listOf("A", "B", "C", "D", "E"), pie.top.map { stat -> stat.label })
        assertEquals(4, pie.othersMentions)
    }

    @Test
    fun genrePieDataHasNoOthersSliceForFiveOrFewerGenres() {
        val pie = genrePieData(listOf(RankedStat("A", 2), RankedStat("B", 1)))

        assertEquals(2, pie.top.size)
        assertEquals(0, pie.othersMentions)

        val empty = genrePieData(emptyList())
        assertTrue(empty.top.isEmpty())
        assertEquals(0, empty.othersMentions)
    }

    @Test
    fun topGenresAreNotTruncatedSoTheOthersSliceIsHonest() {
        val genres = (1..9).map { index -> "Genre$index" }
        val items = listOf(
            trackedMedia(
                id = 1,
                type = MediaType.Book,
                genres = genres,
                sessions = listOf(
                    completedSession(id = 1, mediaItemId = 1, finishedAt = LocalDate.of(2026, 2, 1)),
                ),
            ),
        )

        val topGenres = calculator.calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.ThisYear, mediaTypes = setOf(MediaType.Book)),
        ).topGenres

        // The old cap of 8 would silently drop mentions that Altres must count.
        assertEquals(9, topGenres.size)
        assertEquals(4, genrePieData(topGenres).othersMentions)
    }

    // --- Languages: normalized codes, merged legacy variants ---

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

    @Test
    fun pictogramKeepsEveryRowInAReadableBand() {
        // Totals spanning four orders of magnitude must all stay drawable.
        listOf(1, 7, 41, 96, 1_482, 12_500, 480_000).forEach { value ->
            val scale = pictogramScale(value)
            assertTrue(
                "value=$value produced ${scale.totalBlocks} blocks",
                scale.totalBlocks in 1..17,
            )
        }
    }

    @Test
    fun pictogramBlocksAccountForTheWholeTotal() {
        listOf(1, 7, 41, 96, 1_482, 12_500).forEach { value ->
            val scale = pictogramScale(value)
            val covered = scale.fullBlocks * scale.blockUnit
            assertTrue("value=$value overcounted", covered <= value)
            // The partial block exists exactly when full blocks fall short.
            assertEquals(value != covered, scale.hasPartialBlock)
        }
    }

    @Test
    fun pictogramPicksRoundBlockSizes() {
        assertEquals(100, pictogramScale(1_482).blockUnit)
        assertEquals(14, pictogramScale(1_482).fullBlocks)
        assertTrue(pictogramScale(1_482).hasPartialBlock)
        // Small totals stay one block per unit rather than collapsing to nothing.
        assertEquals(1, pictogramScale(7).blockUnit)
        assertEquals(7, pictogramScale(7).fullBlocks)
    }

    @Test
    fun pictogramFillsMostOfTheRowForRealisticTotals() {
        // The ladder exists so rows do not render half empty. Anything above a
        // handful of units should reach most of the way across the row.
        listOf(41, 68, 96, 350, 1_240, 1_482, 12_500).forEach { value ->
            val scale = pictogramScale(value)
            assertTrue(
                "value=$value filled only ${scale.totalBlocks} blocks",
                scale.totalBlocks >= 12,
            )
        }
    }

    @Test
    fun pictogramHandlesEmptyTotals() {
        val scale = pictogramScale(0)
        assertEquals(0, scale.totalBlocks)
        assertEquals(false, scale.hasPartialBlock)
    }


    @Test
    fun waffleSquaresAlwaysSumToTheGridTotal() {
        // Rounding each medium on its own would let the grid claim more or fewer hours than
        // the section reports. Largest-remainder allocation must keep the sum exact.
        val cases = listOf(
            listOf(1_852.5, 2_112.0, 2_050.0, 1_240.0, 4_080.0),
            listOf(10.0, 10.0, 10.0),
            listOf(59.0, 1.0),
            listOf(100_000.0, 3.0),
            listOf(7.0),
        )
        cases.forEach { minutes ->
            val stats = minutes.mapIndexed { index, value ->
                EstimatedTimeStat(mediaType = MediaType.entries[index % MediaType.entries.size], minutes = value)
            }
            val waffle = estimatedTimeWaffle(stats)
            val expected = Math.round(minutes.sum() / waffle.minutesPerSquare).toInt().coerceAtLeast(1)
            assertEquals("minutes=$minutes", expected, waffle.totalSquares)
        }
    }

    @Test
    fun waffleStaysWithinItsSquareBudget() {
        listOf(60.0, 6_000.0, 600_000.0, 6_000_000.0).forEach { total ->
            val waffle = estimatedTimeWaffle(
                listOf(EstimatedTimeStat(mediaType = MediaType.Book, minutes = total)),
            )
            assertTrue("total=$total gave ${waffle.totalSquares}", waffle.totalSquares in 1..150)
        }
    }

    @Test
    fun waffleSquareStaysWithinFiftyHoursForRealisticTotals() {
        // A square has to stay a quantity a reader can hold. Fifty hours is the ceiling, and it
        // holds across every total this app plausibly reaches in a period.
        listOf(10, 100, 500, 1_000, 2_000, 3_000, 4_800, 7_500).forEach { hours ->
            val waffle = estimatedTimeWaffle(
                listOf(EstimatedTimeStat(mediaType = MediaType.Game, minutes = hours * 60.0)),
            )
            assertTrue(
                "$hours h gave a ${waffle.minutesPerSquare / 60.0} h square",
                waffle.minutesPerSquare <= 3_000.0,
            )
        }
    }

    @Test
    fun waffleOrdersSegmentsLargestFirst() {
        val waffle = estimatedTimeWaffle(
            listOf(
                EstimatedTimeStat(mediaType = MediaType.Book, minutes = 600.0),
                EstimatedTimeStat(mediaType = MediaType.Game, minutes = 4_000.0),
                EstimatedTimeStat(mediaType = MediaType.Movie, minutes = 1_800.0),
            ),
        )
        assertEquals(
            listOf(MediaType.Game, MediaType.Movie, MediaType.Book),
            waffle.segments.map { segment -> segment.mediaType },
        )
    }

    @Test
    fun waffleIsEmptyWithoutTime() {
        val waffle = estimatedTimeWaffle(emptyList())
        assertEquals(0, waffle.totalSquares)
        assertTrue(waffle.segments.isEmpty())
    }


    @Test
    fun waffleGridGrowsWithTheTotal() {
        // The grid used to look near-identical for wildly different totals, and could even draw
        // fewer squares for more time. Bigger totals must now read as visibly bigger grids.
        fun squares(hours: Double): Int = estimatedTimeWaffle(
            listOf(EstimatedTimeStat(mediaType = MediaType.Game, minutes = hours * 60.0)),
        ).totalSquares

        val small = squares(200.0)
        val large = squares(4_600.0)
        assertTrue("200 h drew $small, 4600 h drew $large", large - small >= 10)

        assertTrue(squares(20.0) < squares(200.0))
        assertTrue(squares(200.0) < squares(2_000.0))
        assertTrue(squares(5.0) < squares(50.0))
    }

    @Test
    fun waffleGridNeverShrinksMeaningfullyAsTimeGrows() {
        // Snapping the square size to a readable increment leaves a small wobble; a bounded grid
        // cannot avoid it entirely. What matters is that it stays proportionally small, so the
        // tolerance is a share of the grid rather than a flat count — losing four squares from
        // thirty is visible, losing four from ninety is not.
        var previous = 0
        var worstDrop = 0.0
        var worstAt = 0
        (1..20_000).forEach { hours ->
            val squares = estimatedTimeWaffle(
                listOf(EstimatedTimeStat(mediaType = MediaType.Game, minutes = hours * 60.0)),
            ).totalSquares
            // A single square is allowed to move regardless: at the floor of eight squares one
            // tile is already an eighth of the grid, and that is not a lurch worth failing on.
            if (squares < previous - 1) {
                val drop = (previous - squares).toDouble() / previous
                if (drop > worstDrop) {
                    worstDrop = drop
                    worstAt = hours
                }
            }
            previous = squares
        }
        assertTrue(
            "worst drop was ${(worstDrop * 100).toInt()}% at $worstAt h",
            worstDrop <= 0.10,
        )
    }

}
