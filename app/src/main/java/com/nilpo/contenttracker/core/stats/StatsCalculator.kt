package com.nilpo.contenttracker.core.stats

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsCalculator(
    private val today: LocalDate = LocalDate.now(),
) {
    fun calculate(
        items: List<TrackedMedia>,
        filters: StatsFilters,
    ): StatsSnapshot {
        val filteredItems = items.filter { trackedMedia -> trackedMedia.item.type in filters.mediaTypes }
        val completedSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.status == TrackingStatus.Completed }
                .filter { session -> filters.period.contains(session.finishedAt, today) }
                .map { session -> trackedMedia to session }
        }
        val ratedSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.rating != null }
                .filter { session -> filters.period.contains(session.statsDate(), today) }
                .map { session -> trackedMedia to session }
        }
        val revisitSessions = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.isRevisit }
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
            .mapNotNull { (_, session) -> session.rating }
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
            topGenres = rankedStrings(completedItems.flatMap { trackedMedia -> trackedMedia.item.genres }),
            topCreators = rankedStrings(completedItems.flatMap { trackedMedia -> trackedMedia.item.creators }),
            languageBreakdown = languageBreakdown(completedItems),
            bestRatedItems = bestRatedItems(ratedSessions),
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
                .count { session ->
                    session.status == TrackingStatus.Completed && inScope(session.finishedAt)
                }
        }
        val ratings = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.rating != null }
                .filter { session -> inScope(session.statsDate()) }
                .mapNotNull { session -> session.rating }
        }
        val revisits = filteredItems.flatMap { trackedMedia ->
            trackedMedia.sessions
                .filter { session -> session.isRevisit }
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
        completedSessions: List<Pair<TrackedMedia, TrackingSession>>,
        period: StatsPeriod,
    ): List<StatsBucket> {
        val monthValues = completedSessions
            .mapNotNull { (trackedMedia, session) ->
                session.finishedAt?.let { date -> YearMonth.from(date) to trackedMedia.item.type }
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
        val ratingValues = ratedSessions
            .mapNotNull { (trackedMedia, session) ->
                session.rating
                    ?.takeIf { rating -> rating in 1..10 }
                    ?.let { rating -> rating to trackedMedia.item.type }
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
                val rating = session.rating?.takeIf { value -> value in 1..10 } ?: return@mapNotNull null
                val date = session.statsDate() ?: return@mapNotNull null
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
        completedSessions: List<Pair<TrackedMedia, TrackingSession>>,
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
                    .mapNotNull { (_, session) -> session.rating }
                val lengths = completions.mapNotNull { (trackedMedia, session) ->
                    trackedMedia.item.progressTotal
                        ?.takeIf { total -> total > 0 }
                        ?: session.progressCurrent.takeIf { progress -> progress > 0 }
                }
                val hasStats = completions.isNotEmpty() || ratings.isNotEmpty() || lengths.isNotEmpty()
                if (!hasStats) {
                    null
                } else {
                    MediumStats(
                        mediaType = mediaType,
                        completionSessionCount = completions.size,
                        averageRating = ratings.takeIf { values -> values.isNotEmpty() }?.average(),
                        averageLength = lengths.takeIf { values -> values.isNotEmpty() }?.average(),
                        totalLength = lengths.sum(),
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
        completedSessions: List<Pair<TrackedMedia, TrackingSession>>,
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

    private fun rankedStrings(values: List<String>): List<RankedStat> {
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
            .take(8)
            .map { entry -> RankedStat(entry.key, entry.value) }
    }

    private fun languageBreakdown(items: List<TrackedMedia>): List<StatsBucket> {
        return items
            .map { trackedMedia -> trackedMedia.item.language?.trim().takeUnless { it.isNullOrBlank() } ?: "Desconegut" }
            .groupingBy { language -> language }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { entry -> entry.value }
                    .thenBy { entry -> entry.key.lowercase() },
            )
            .take(8)
            .map { entry ->
                StatsBucket(
                    key = entry.key,
                    label = entry.key,
                    value = entry.value,
                )
            }
    }

    private fun bestRatedItems(
        ratedSessions: List<Pair<TrackedMedia, TrackingSession>>,
    ): List<RatedMediaStat> {
        return ratedSessions
            .groupBy { (trackedMedia, _) -> trackedMedia.item.id }
            .mapNotNull { (_, sessions) ->
                val trackedMedia = sessions.firstOrNull()?.first ?: return@mapNotNull null
                val bestRating = sessions.mapNotNull { (_, session) -> session.rating }.maxOrNull()
                bestRating?.let { rating -> trackedMedia to rating }
            }
            .sortedWith(
                compareByDescending<Pair<TrackedMedia, Int>> { (_, rating) -> rating }
                    .thenBy { (trackedMedia, _) -> trackedMedia.item.title.lowercase() },
            )
            .take(8)
            .map { (trackedMedia, rating) -> RatedMediaStat(trackedMedia = trackedMedia, bestRating = rating) }
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
