package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsCalculator(
    private val today: LocalDate = LocalDate.now(),
) {
    private data class CompletedSession(
        val trackedMedia: TrackedMedia,
        val session: TrackingSession,
        val completedOn: LocalDate?,
    )

    fun calculate(
        items: List<TrackedMedia>,
        filters: StatsFilters,
    ): StatsSnapshot {
        val filteredItems = items.filter { trackedMedia -> trackedMedia.item.type in filters.mediaTypes }
        val completedSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions.flatMap { session ->
                session.completionDates()
                    .filter { date -> filters.period.contains(date, today) }
                    .map { date -> CompletedSession(trackedMedia, session, date) }
            }
        }
        // Ratings are dated by finishedAt, exactly like completions, so the rating charts can
        // never disagree with the completion count for the same period. A rating on a session
        // with no finish date has no honest position on a time axis and is therefore scoped to
        // AllTime only, rather than being placed by startedAt or by the row's last-modified
        // timestamp — the latter would move a rating between periods when unrelated fields
        // are edited.
        val ratedSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.ratingHalfPoints != null }
                .filter { session -> filters.period.contains(session.latestCompletionDate(), today) }
                .map { session -> trackedMedia to session }
        }
        val revisitSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> trackedMedia.isRevisit(session) }
                .filter { session -> filters.period.contains(session.statsDate(), today) }
                .map { session -> trackedMedia to session }
        }
        val completedItems = completedSessions
            .map { (trackedMedia, _) -> trackedMedia }
            .distinctBy { trackedMedia -> trackedMedia.item.id }
        val activeNow = filteredItems.count { trackedMedia ->
            trackedMedia.currentSession?.status == TrackingStatus.InProgress
        }
        val plannedNow = filteredItems.count { trackedMedia ->
            trackedMedia.currentSession?.status == TrackingStatus.Planned
        }
        val averageRating = ratedSessions
            .mapNotNull { (_, session) -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
            .takeIf { ratings -> ratings.isNotEmpty() }
            ?.average()
        val completionSessionsByMonth = completionSessionsByMonth(
            completedSessions = completedSessions,
            period = filters.period,
        )
        val mediumStats = mediumStats(
            mediaTypes = filters.mediaTypes,
            completedSessions = completedSessions,
            ratedSessions = ratedSessions,
        )
        val progressTotals = progressTotals(completedSessions)
        // Repeats measured against completions, so "N of M titles" compares like with like. A
        // repeat session still under way has not finished in this period and is not counted here,
        // though it still shows in the looser revisitCount tally.
        val revisitedCompletions = completedSessions
            .filter { (trackedMedia, session) -> trackedMedia.isRevisit(session) }
        val revisitedTitleIds = revisitedCompletions
            .map { (trackedMedia, _) -> trackedMedia.item.id }
            .distinct()

        return StatsSnapshot(
            filters = filters,
            totalTitles = filteredItems.size,
            uniqueTitlesCompleted = completedSessions
                .map { (trackedMedia, _) -> trackedMedia.item.id }
                .distinct()
                .size,
            completionSessions = completedSessions.size,
            activeNow = activeNow,
            plannedNow = plannedNow,
            averageRating = averageRating,
            revisitCount = revisitSessions.size,
            completionSessionsByMonth = completionSessionsByMonth,
            mediumStats = mediumStats,
            ratingTrend = ratingTrend(
                ratedSessions = ratedSessions,
                period = filters.period,
            ),
            ratingDistribution = ratingDistribution(ratedSessions),
            statusBreakdown = statusBreakdown(filteredItems),
            progressTotals = progressTotals,
            revisitBreakdown = revisitBreakdown(
                mediaTypes = filters.mediaTypes,
                revisitSessions = revisitSessions,
            ),
            revisitedTitles = revisitedTitleIds.size,
            revisitedTitleBreakdown = revisitedTitleBreakdown(revisitedCompletions),
            // Genres stay unlimited so the pie's Altres slice can aggregate
            // every remaining mention instead of only a truncated tail.
            topGenres = rankedStrings(
                values = completedItems.flatMap { trackedMedia -> trackedMedia.item.genres },
                limit = Int.MAX_VALUE,
            ),
            topCreators = rankedStrings(completedItems.flatMap { trackedMedia -> trackedMedia.item.creators }),
            creatorStats = creatorStats(completedItems = completedItems, ratedSessions = ratedSessions),
            languageBreakdown = languageBreakdown(completedItems),
            bestRatedItems = bestRatedItems(ratedSessions),
            bestRatedCollections = bestRatedCollections(ratedSessions),
            mostRevisitedItems = mostRevisitedItems(revisitSessions),
            topLevelSummary = topLevelSummary(
                visibleMonthlyActivity = completionSessionsByMonth.takeLast(12),
                mediumStats = mediumStats,
                progressTotals = progressTotals,
            ),
            deltas = calculateDelta(items = items, filters = filters),
        )
    }

    private fun topLevelSummary(
        visibleMonthlyActivity: List<StatsBucket>,
        mediumStats: List<MediumStats>,
        progressTotals: List<ProgressTotalStats>,
    ): StatsTopLevelSummary {
        val completionCountByMedium = mediumStats.associate { stat ->
            stat.mediaType to stat.completionSessionCount
        }
        val consumptionHighlight = progressTotals
            .asSequence()
            .filter { total -> total.value > 0 }
            // Relevance is decided using comparable completion-session counts.
            // Consumption magnitudes themselves retain their unit and are never
            // compared across pages, episodes, minutes, and hours.
            .sortedWith(
                compareByDescending<ProgressTotalStats> { total ->
                    completionCountByMedium[total.mediaType] ?: 0
                }.thenBy { total -> total.mediaType.ordinal },
            )
            .firstOrNull()
            ?.let { total ->
                ConsumptionHighlight(mediaType = total.mediaType, value = total.value)
            }

        val busiestMonth = visibleMonthlyActivity
            .asSequence()
            .filter { bucket -> bucket.value > 0 }
            .maxWithOrNull(
                compareBy<StatsBucket> { bucket -> bucket.value }
                    .thenBy { bucket -> bucket.key },
            )
        val observation = busiestMonth?.let { bucket ->
            StatsObservation.BusiestMonth(
                key = bucket.key,
                label = bucket.label,
                completionSessions = bucket.value,
            )
        } ?: highestRatedMediumObservation(mediumStats)

        return StatsTopLevelSummary(
            consumptionHighlight = consumptionHighlight,
            observation = observation,
        )
    }

    private fun highestRatedMediumObservation(
        mediumStats: List<MediumStats>,
    ): StatsObservation.HighestRatedMedium? {
        val ratedStats = mediumStats.filter { stat -> stat.averageRating != null }
        if (ratedStats.size < 2) return null

        val bestAverage = ratedStats.maxOf { stat -> stat.averageRating ?: 0.0 }
        val leaders = ratedStats.filter { stat -> stat.averageRating == bestAverage }
        val leader = leaders.singleOrNull() ?: return null
        return StatsObservation.HighestRatedMedium(
            mediaType = leader.mediaType,
            averageRating = bestAverage,
        )
    }

    /**
     * Difference between the current period and the immediately preceding
     * comparison window for the period-bound KPIs. Used for the small delta
     * chips shown next to the Periode tiles. Returns all-null for AllTime,
     * and null per-field when the previous window had no data for that field.
     */
    fun calculateDelta(
        items: List<TrackedMedia>,
        filters: StatsFilters,
    ): PeriodDelta {
        val window = filters.period.previousWindow(today) ?: return PeriodDelta()
        val filteredItems = items.filter { trackedMedia -> trackedMedia.item.type in filters.mediaTypes }

        val current = periodKpis(filteredItems, filters.period, window = null)
        val previous = periodKpis(filteredItems, period = null, window = window)

        return PeriodDelta(
            basis = filters.period.comparisonBasis(),
            completionSessions = current.completionSessions - previous.completionSessions,
            averageRating = current.averageRating
                ?.let { avg -> previous.averageRating?.let { avg - it } },
            revisits = current.revisits - previous.revisits,
        )
    }

    /**
     * Period-bound KPI counts scoped to either the selected [StatsPeriod]
     * (via [contains]) or an explicit [window] date range.
     */
    private data class PeriodKpis(
        val completionSessions: Int,
        val averageRating: Double?,
        val revisits: Int,
    )

    private fun periodKpis(
        filteredItems: List<TrackedMedia>,
        period: StatsPeriod?,
        window: ClosedRange<LocalDate>?,
    ): PeriodKpis {
        require((period != null) != (window != null)) { "Exactly one of period or window must be set" }
        fun inScope(date: LocalDate?): Boolean =
            period?.let { it.contains(date, today) } ?: window!!.contains(date)

        val completionSessions = filteredItems.sumOf { trackedMedia ->
            trackedMedia.sessions
                .sumOf { session -> session.completionDates().count(::inScope) }
        }
        val ratings = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.ratingHalfPoints != null }
                .filter { session -> inScope(session.latestCompletionDate()) }
                .mapNotNull { session -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
        }
        val revisits = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> trackedMedia.isRevisit(session) }
                .filter { session -> inScope(session.statsDate()) }
        }
            .size

        return PeriodKpis(
            completionSessions = completionSessions,
            averageRating = ratings.takeIf { it.isNotEmpty() }?.average(),
            revisits = revisits,
        )
    }

    private fun StatsPeriod.comparisonBasis(): ComparisonBasis? {
        return when (this) {
            StatsPeriod.AllTime -> null
            StatsPeriod.ThisYear -> ComparisonBasis.SamePeriodOfYear(today.year - 1)
            is StatsPeriod.Year -> when {
                year < today.year -> ComparisonBasis.FullYear(year - 1)
                year == today.year -> ComparisonBasis.SamePeriodOfYear(year - 1)
                else -> null
            }
            StatsPeriod.Last12Months -> ComparisonBasis.Previous12Months
        }
    }

    private fun StatsPeriod.previousWindow(today: LocalDate): ClosedRange<LocalDate>? {
        return when (this) {
            StatsPeriod.AllTime -> null
            StatsPeriod.ThisYear -> {
                // Like-for-like: year-to-date vs the same date range of the
                // previous year, not the whole previous calendar year.
                val prevYear = today.year - 1
                LocalDate.of(prevYear, 1, 1)..today.minusYears(1)
            }
            is StatsPeriod.Year -> when {
                year < today.year -> {
                    val prevYear = year - 1
                    LocalDate.of(prevYear, 1, 1)..LocalDate.of(prevYear, 12, 31)
                }
                year == today.year -> LocalDate.of(year - 1, 1, 1)..today.minusYears(1)
                else -> null
            }
            StatsPeriod.Last12Months -> {
                val currentStart = today.minusMonths(11).withDayOfMonth(1)
                currentStart.minusYears(1)..today.minusYears(1)
            }
        }
    }

    private fun ClosedRange<LocalDate>.contains(date: LocalDate?): Boolean {
        if (date == null) return false
        return !date.isBefore(start) && !date.isAfter(endInclusive)
    }

    private fun completionSessionsByMonth(
        completedSessions: List<CompletedSession>,
        period: StatsPeriod,
    ): List<StatsBucket> {
        val monthValues = completedSessions
            .mapNotNull { completion ->
                completion.completedOn?.let { date ->
                    YearMonth.from(date) to completion.trackedMedia.item.type
                }
            }
        val monthTotals = monthValues
            .groupingBy { (month, _) -> month }
            .eachCount()
        val segmentsByMonth = monthValues
            .groupBy(
                keySelector = { (month, _) -> month },
                valueTransform = { (_, mediaType) -> mediaType },
            )
            .mapValues { (_, mediaTypes) ->
                mediaTypes
                    .groupingBy { mediaType -> mediaType }
                    .eachCount()
                    .entries
                    .sortedBy { entry -> entry.key.ordinal }
                    .map { entry -> StatsSegment(mediaType = entry.key, value = entry.value) }
            }
        val months = monthRange(period, monthTotals.keys)

        return months.map { month ->
            StatsBucket(
                key = month.toString(),
                label = monthLabel(month),
                value = monthTotals[month] ?: 0,
                segments = segmentsByMonth[month].orEmpty(),
            )
        }
    }

    private fun monthRange(period: StatsPeriod, monthsWithValues: Set<YearMonth>): List<YearMonth> {
        return when (period) {
            StatsPeriod.ThisYear -> (1..today.monthValue).map { month -> YearMonth.of(today.year, month) }
            is StatsPeriod.Year -> {
                val lastMonth = if (period.year == today.year) today.monthValue else 12
                (1..lastMonth).map { month -> YearMonth.of(period.year, month) }
            }
            StatsPeriod.Last12Months -> defaultMonths()
            StatsPeriod.AllTime -> {
                if (monthsWithValues.isEmpty()) return defaultMonths()
                val first = monthsWithValues.minOrNull() ?: return defaultMonths()
                val last = monthsWithValues.maxOrNull() ?: return defaultMonths()
                generateSequence(first) { month -> month.plusMonths(1) }
                    .takeWhile { month -> !month.isAfter(last) }
                    .toList()
            }
        }
    }

    private fun defaultMonths(): List<YearMonth> {
        val currentMonth = YearMonth.from(today)
        return (11 downTo 0).map { offset -> currentMonth.minusMonths(offset.toLong()) }
    }

    private fun ratingDistribution(ratedSessions: List<Pair<TrackedMedia, TrackingSession>>): List<StatsBucket> {
        // Ten bars, so a half point falls in with the whole point above it: 7,5 is counted under
        // 8, the same way it rounds when it goes out to a provider. Twenty bars would be faithful
        // and unreadable at phone width, and the average below keeps the halves either way.
        // ponytail: whole-point buckets, split into halves if the distribution ever needs them.
        val ratingValues = ratedSessions
            .mapNotNull { (trackedMedia, session) ->
                session.ratingHalfPoints
                    ?.takeIf { halfPoints -> halfPoints in RatingHalfPoints.Min..RatingHalfPoints.Max }
                    ?.let { halfPoints -> RatingHalfPoints.toWholePoints(halfPoints) to trackedMedia.item.type }
            }
        val counts = ratingValues
            .groupingBy { (rating, _) -> rating }
            .eachCount()
        val segmentsByRating = ratingValues
            .groupBy(
                keySelector = { (rating, _) -> rating },
                valueTransform = { (_, mediaType) -> mediaType },
            )
            .mapValues { (_, mediaTypes) ->
                mediaTypes
                    .groupingBy { mediaType -> mediaType }
                    .eachCount()
                    .entries
                    .sortedBy { entry -> entry.key.ordinal }
                    .map { entry -> StatsSegment(mediaType = entry.key, value = entry.value) }
            }

        return (1..10).map { rating ->
            StatsBucket(
                key = rating.toString(),
                label = rating.toString(),
                value = counts[rating] ?: 0,
                segments = segmentsByRating[rating].orEmpty(),
            )
        }
    }

    private fun ratingTrend(
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
        period: StatsPeriod,
    ): List<RatingTrendPoint> {
        val ratingsByMonth = ratedSessions
            .mapNotNull { (_, session) ->
                val rating = session.ratingHalfPoints
                    ?.takeIf { value -> value in RatingHalfPoints.Min..RatingHalfPoints.Max }
                    ?.let(RatingHalfPoints::toScore)
                    ?: return@mapNotNull null
                // Same date as the period filter uses, so a rating cannot be filtered into the
                // period by one date and then bucketed into a month by another.
                val date = session.latestCompletionDate() ?: return@mapNotNull null
                YearMonth.from(date) to rating
            }
            .groupBy(
                keySelector = { (month, _) -> month },
                valueTransform = { (_, rating) -> rating },
            )
        val months = monthRange(period, ratingsByMonth.keys)

        return months.map { month ->
            val ratings = ratingsByMonth[month].orEmpty()
            RatingTrendPoint(
                key = month.toString(),
                label = monthLabel(month),
                averageRating = ratings.takeIf { values -> values.isNotEmpty() }?.average(),
                ratingCount = ratings.size,
            )
        }
    }

    private fun mediumStats(
        mediaTypes: Set<MediaType>,
        completedSessions: List<CompletedSession>,
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
    ): List<MediumStats> {
        return mediaTypes
            .sortedBy { mediaType -> mediaType.ordinal }
            .mapNotNull { mediaType ->
                val completions = completedSessions.filter { (trackedMedia, _) ->
                    trackedMedia.item.type == mediaType
                }
                val ratings = ratedSessions
                    .filter { (trackedMedia, _) -> trackedMedia.item.type == mediaType }
                    .mapNotNull { (_, session) -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
                val measuredTitles = completions.mapNotNull { (trackedMedia, session) ->
                    val length = trackedMedia.item.progressTotal
                        ?.takeIf { total -> total > 0 }
                        ?: session.progressCurrent.takeIf { progress -> progress > 0 }
                    length?.let { value -> LengthSample(length = value, title = trackedMedia.item.title) }
                }
                val hasStats = completions.isNotEmpty() || ratings.isNotEmpty() || measuredTitles.isNotEmpty()
                if (!hasStats) {
                    null
                } else {
                    MediumStats(
                        mediaType = mediaType,
                        completionSessionCount = completions.size,
                        averageRating = ratings.takeIf { values -> values.isNotEmpty() }?.average(),
                        // Title breaks ties, so which of two equally long titles gets named at the
                        // end of the strip stays the same between recompositions.
                        measuredTitles = measuredTitles.sortedWith(
                            compareBy({ sample -> sample.length }, { sample -> sample.title.lowercase() }),
                        ),
                        totalLength = measuredTitles.sumOf { sample -> sample.length },
                    )
                }
            }
    }

    private fun statusBreakdown(items: List<TrackedMedia>): List<StatusStatsBucket> {
        val counts = items
            .mapNotNull { trackedMedia -> trackedMedia.currentSession?.status }
            .groupingBy { status -> status }
            .eachCount()

        return TrackingStatus.entries.map { status ->
            StatusStatsBucket(
                status = status,
                value = counts[status] ?: 0,
            )
        }
    }

    private fun progressTotals(
        completedSessions: List<CompletedSession>,
    ): List<ProgressTotalStats> {
        return completedSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.type }
            .map { (mediaType, sessions) ->
                ProgressTotalStats(
                    mediaType = mediaType,
                    value = sessions.sumOf { (trackedMedia, session) ->
                        val total = trackedMedia.item.progressTotal
                        if (total != null && total > 0) total else session.progressCurrent
                    },
                )
            }
            .sortedBy { total -> total.mediaType.ordinal }
    }

    /**
     * Repeated titles per medium, counting each title once however many times it was finished
     * again, so the parts sum to the whole the headline states.
     */
    private fun revisitedTitleBreakdown(
        revisitedCompletions: List<CompletedSession>,
    ): List<RevisitStats> {
        return revisitedCompletions
            .distinctBy { (trackedMedia, _) -> trackedMedia.item.id }
            .groupingBy { (trackedMedia, _) -> trackedMedia.item.type }
            .eachCount()
            .entries
            .sortedByDescending { entry -> entry.value }
            .map { entry -> RevisitStats(mediaType = entry.key, value = entry.value) }
    }

    private fun revisitBreakdown(
        mediaTypes: Set<MediaType>,
        revisitSessions: List<Pair<TrackedMedia, TrackingSession>>,
    ): List<RevisitStats> {
        val counts = revisitSessions
            .groupingBy { (trackedMedia, _) -> trackedMedia.item.type }
            .eachCount()

        return mediaTypes
            .sortedBy { mediaType -> mediaType.ordinal }
            .map { mediaType ->
                RevisitStats(
                    mediaType = mediaType,
                    value = counts[mediaType] ?: 0,
                )
            }
    }

    private fun rankedStrings(values: List<String>, limit: Int = 8): List<RankedStat> {
        return values
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .groupingBy { value -> value }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { entry -> entry.value }
                    .thenBy { entry -> entry.key.lowercase() },
            )
            .take(limit)
            .map { entry -> RankedStat(entry.key, entry.value) }
    }

    /**
     * Creators whose work you finished in the period, ranked by how many of their titles you
     * completed. Ranking stays on the count even though the card shows a rating, so the order
     * always matches the section's question — who you followed most, not who scored best.
     */
    private fun creatorStats(
        completedItems: List<TrackedMedia>,
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
        limit: Int = 12,
    ): List<CreatorStat> {
        // One rating per title: a title rated on several passes must not outweigh several titles.
        val ratingByItemId = ratedSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.id }
            .mapValues { (_, sessions) ->
                sessions.mapNotNull { (_, session) -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }.average()
            }

        return completedItems
            .flatMap { trackedMedia ->
                trackedMedia.item.creators
                    .map { creator -> creator.trim() }
                    .filter { creator -> creator.isNotBlank() }
                    .distinct()
                    .map { creator -> creator to trackedMedia }
            }
            .groupBy({ (creator, _) -> creator }, { (_, trackedMedia) -> trackedMedia })
            .map { (creator, titles) ->
                val ratings = titles.mapNotNull { trackedMedia -> ratingByItemId[trackedMedia.item.id] }
                CreatorStat(
                    name = creator,
                    completedTitles = titles.size,
                    ratedTitles = ratings.size,
                    averageRating = ratings
                        .takeIf { values -> values.size >= MinRatedTitlesForCreatorAverage }
                        ?.average(),
                    // The accent only tints the card, so the medium they appear in most wins.
                    mediaType = titles
                        .groupingBy { trackedMedia -> trackedMedia.item.type }
                        .eachCount()
                        .entries
                        .sortedWith(
                            compareByDescending<Map.Entry<MediaType, Int>> { entry -> entry.value }
                                .thenBy { entry -> entry.key.ordinal },
                        )
                        .first()
                        .key,
                    coverUrls = titles
                        .sortedWith(
                            compareByDescending<TrackedMedia> { trackedMedia ->
                                ratingByItemId[trackedMedia.item.id] ?: -1.0
                            }.thenBy { trackedMedia -> trackedMedia.item.title.lowercase() },
                        )
                        .mapNotNull { trackedMedia -> trackedMedia.item.coverUrl }
                        .filter { url -> url.isNotBlank() }
                        .take(3),
                )
            }
            .sortedWith(
                compareByDescending<CreatorStat> { stat -> stat.completedTitles }
                    .thenBy { stat -> stat.name.lowercase() },
            )
            .take(limit)
    }

    /**
     * The language split over the media that record a language often enough to have one.
     *
     * A medium is judged on its own titles rather than against the library as a whole, so nothing
     * here hardcodes which medium that turns out to be: a medium starts appearing as soon as you
     * tag enough of it, and drops out if you stop.
     */
    private fun languageBreakdown(items: List<TrackedMedia>): LanguageCoverage {
        val included = mutableListOf<MediaType>()
        val excluded = mutableListOf<MediaType>()
        items
            .groupBy { trackedMedia -> trackedMedia.item.type }
            .forEach { (mediaType, mediaItems) ->
                val recorded = mediaItems
                    .count { trackedMedia -> ItemLanguage.normalize(trackedMedia.item.language) != null }
                val qualifies = recorded >= MinLanguageRecordedTitles &&
                    recorded.toDouble() / mediaItems.size >= MinLanguageCoverageShare
                if (qualifies) included += mediaType else excluded += mediaType
            }

        val includedItems = items.filter { trackedMedia -> trackedMedia.item.type in included }
        val recordedCodes = includedItems
            .mapNotNull { trackedMedia -> ItemLanguage.normalize(trackedMedia.item.language) }
        val stats = recordedCodes
            .groupingBy { code -> code }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { entry -> entry.value }
                    .thenBy { entry -> entry.key.lowercase() },
            )
            .take(8)
            .map { entry -> LanguageStat(code = entry.key, value = entry.value) }

        return LanguageCoverage(
            stats = stats,
            includedMediaTypes = included.sortedBy { mediaType -> mediaType.ordinal },
            excludedMediaTypes = excluded.sortedBy { mediaType -> mediaType.ordinal },
            // The full tally, not the truncated one: the caption states how much of the medium
            // carries a language, which stays true whether or not a long tail got cut from the
            // blocks.
            recordedTitles = recordedCodes.size,
            totalTitles = includedItems.size,
        )
    }

    /**
     * Collections ranked by the average of the ratings you gave the titles inside them.
     *
     * Ranking by a rating, rather than showing one alongside a count as [creatorStats] does, is
     * only honest above a floor: collections under [MinRatedTitlesForCollectionAverage] rated
     * titles are dropped rather than ranked, since one score would otherwise take the top spot on
     * no evidence. Ties break towards the collection with more rated titles, so the better-attested
     * average wins.
     */
    private fun bestRatedCollections(
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
        limit: Int = 12,
    ): List<CollectionRatingStat> {
        // One value per title, so a title rated on several passes cannot stand in for several
        // titles and clear the threshold on its own.
        val ratingByItem = ratedSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.id }
            .mapNotNull { (_, sessions) ->
                val trackedMedia = sessions.firstOrNull()?.first ?: return@mapNotNull null
                val ratings = sessions.mapNotNull { (_, session) -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
                if (ratings.isEmpty()) null else trackedMedia to ratings.average()
            }

        return ratingByItem
            .mapNotNull { (trackedMedia, rating) ->
                trackedMedia.collection?.let { collection -> Triple(collection, trackedMedia, rating) }
            }
            .groupBy { (collection, _, _) -> collection.id }
            .mapNotNull { (_, entries) ->
                if (entries.size < MinRatedTitlesForCollectionAverage) return@mapNotNull null
                val collection = entries.first().first
                CollectionRatingStat(
                    id = collection.id,
                    name = collection.name,
                    ratedTitles = entries.size,
                    averageRating = entries.map { (_, _, rating) -> rating }.average(),
                    // The accent only tints the card, so the medium the collection mostly sits in
                    // wins, matching how a creator card picks its colour.
                    mediaType = entries
                        .groupingBy { (_, trackedMedia, _) -> trackedMedia.item.type }
                        .eachCount()
                        .entries
                        .sortedWith(
                            compareByDescending<Map.Entry<MediaType, Int>> { entry -> entry.value }
                                .thenBy { entry -> entry.key.ordinal },
                        )
                        .first()
                        .key,
                    coverUrls = entries
                        .sortedWith(
                            compareByDescending<Triple<MediaCollection, TrackedMedia, Double>> { (_, _, rating) -> rating }
                                .thenBy { (_, trackedMedia, _) -> trackedMedia.item.title.lowercase() },
                        )
                        .mapNotNull { (_, trackedMedia, _) -> trackedMedia.item.coverUrl }
                        .filter { url -> url.isNotBlank() }
                        .take(3),
                )
            }
            .sortedWith(
                compareByDescending<CollectionRatingStat> { stat -> stat.averageRating }
                    .thenByDescending { stat -> stat.ratedTitles }
                    .thenBy { stat -> stat.name.lowercase() },
            )
            .take(limit)
    }

    private fun bestRatedItems(
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
    ): List<RatedMediaStat> {
        return ratedSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.id }
            .mapNotNull { (_, sessions) ->
                val trackedMedia = sessions.firstOrNull()?.first ?: return@mapNotNull null
                val bestScore = sessions
                    .mapNotNull { (_, session) -> session.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
                    .maxOrNull()
                bestScore?.let { score -> trackedMedia to score }
            }
            .sortedWith(
                compareByDescending<Pair<TrackedMedia, Double>> { (_, score) -> score }
                    .thenBy { (trackedMedia, _) -> trackedMedia.item.title.lowercase() },
            )
            .take(8)
            .map { (trackedMedia, score) -> RatedMediaStat(trackedMedia = trackedMedia, bestScore = score) }
    }

    private fun mostRevisitedItems(
        revisitSessions: List<Pair<TrackedMedia, TrackingSession>>,
    ): List<RevisitedMediaStat> {
        return revisitSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.id }
            .mapNotNull { (_, sessions) ->
                val trackedMedia = sessions.firstOrNull()?.first ?: return@mapNotNull null
                RevisitedMediaStat(
                    trackedMedia = trackedMedia,
                    value = sessions.size,
                )
            }
            .sortedWith(
                compareByDescending<RevisitedMediaStat> { stat -> stat.value }
                    .thenBy { stat -> stat.trackedMedia.item.title.lowercase() },
            )
            .take(8)
    }

    private fun TrackingSession.statsDate(): LocalDate? {
        if (status.endsSession) return latestCompletionDate()
        return finishedAt
            ?: startedAt
            ?: updatedAtEpochMillis.takeIf { millis -> millis > 0L }?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
    }

    private fun StatsPeriod.contains(date: LocalDate?, today: LocalDate): Boolean {
        if (date == null) return this == StatsPeriod.AllTime
        return when (this) {
            StatsPeriod.AllTime -> true
            StatsPeriod.ThisYear -> date.year == today.year && !date.isAfter(today)
            is StatsPeriod.Year -> date.year == year && (year < today.year || !date.isAfter(today))
            StatsPeriod.Last12Months -> {
                val start = today.minusMonths(11).withDayOfMonth(1)
                !date.isBefore(start) && !date.isAfter(today)
            }
        }
    }

    private fun monthLabel(month: YearMonth): String {
        return when (month.monthValue) {
            1 -> "gen."
            2 -> "feb."
            3 -> "març"
            4 -> "abr."
            5 -> "maig"
            6 -> "juny"
            7 -> "jul."
            8 -> "ago."
            9 -> "set."
            10 -> "oct."
            11 -> "nov."
            else -> "des."
        }
    }
}

/** Status events are canonical; the session finish date is the legacy/current fallback. */
fun TrackingSession.completionDates(): List<LocalDate?> {
    val recorded = statusEvents
        .filter { it.status == TrackingStatus.Completed }
        .map { event -> event.occurredOn.takeIf { event.hasKnownDate } }
    return recorded.ifEmpty {
        if (status == TrackingStatus.Completed) listOf(finishedAt) else emptyList()
    }
}

fun TrackingSession.latestCompletionDate(): LocalDate? =
    completionDates().filterNotNull().maxOrNull()
