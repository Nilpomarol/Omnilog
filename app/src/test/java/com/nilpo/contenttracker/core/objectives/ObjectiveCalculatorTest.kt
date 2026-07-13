package com.nilpo.contenttracker.core.objectives

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ObjectiveCalculatorTest {
    private val today = LocalDate.of(2026, 7, 13)
    private val calculator = ObjectiveCalculator(today)

    @Test
    fun countsDistinctCompletedTitlesInsidePeriod() {
        val item = trackedMedia(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(
                session(1, finishedAt = LocalDate.of(2026, 7, 2)),
                session(2, finishedAt = LocalDate.of(2026, 7, 8)),
            ),
        )
        val objective = objective(ObjectiveMetric.CompletedTitles, ObjectiveUnit.Titles, MediaType.Book, 1)

        val result = calculator.calculate(listOf(item), listOf(objective)).single()

        assertEquals(1, result.currentValue)
        assertTrue(result.isComplete)
    }

    @Test
    fun sumsProgressDeltasOnlyInsidePeriod() {
        val item = trackedMedia(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(
                session(
                    1,
                    updates = listOf(
                        update(100, LocalDate.of(2026, 6, 30)),
                        update(160, LocalDate.of(2026, 7, 2)),
                        update(220, LocalDate.of(2026, 7, 8)),
                    ),
                ),
            ),
        )
        val objective = objective(ObjectiveMetric.ProgressUnits, ObjectiveUnit.Pages, MediaType.Book, 100)

        val result = calculator.calculate(listOf(item), listOf(objective)).single()

        assertEquals(120, result.currentValue)
    }

    @Test
    fun ignoresProgressCorrectionsButUsesThemAsFutureBaselines() {
        val item = trackedMedia(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(
                session(
                    1,
                    updates = listOf(
                        update(100, LocalDate.of(2026, 6, 30)),
                        update(150, LocalDate.of(2026, 7, 13), countsTowardObjectives = false),
                        update(160, LocalDate.of(2026, 7, 14)),
                    ),
                ),
            ),
        )
        val objective = objective(ObjectiveMetric.ProgressUnits, ObjectiveUnit.Pages, MediaType.Book, 10)

        val result = calculator.calculate(listOf(item), listOf(objective)).single()

        assertEquals(10, result.currentValue)
    }

    @Test
    fun undatedProgressDoesNotCountButRemainsTheNextBaseline() {
        val item = trackedMedia(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(
                session(
                    1,
                    updates = listOf(
                        update(100, LocalDate.of(2026, 6, 30)),
                        update(150, LocalDate.of(2026, 7, 5), hasKnownDate = false),
                        update(160, LocalDate.of(2026, 7, 6)),
                    ),
                ),
            ),
        )
        val objective = objective(ObjectiveMetric.ProgressUnits, ObjectiveUnit.Pages, MediaType.Book, 10)

        val result = calculator.calculate(listOf(item), listOf(objective)).single()

        assertEquals(10, result.currentValue)
    }
    @Test
    fun completedTitlesWithoutFinishDateAreNotPlacedInDatePeriods() {
        val item = trackedMedia(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(session(1, finishedAt = null).copy(status = TrackingStatus.Completed)),
        )
        val objective = objective(ObjectiveMetric.CompletedTitles, ObjectiveUnit.Titles, MediaType.Book, 1)

        val result = calculator.calculate(listOf(item), listOf(objective)).single()

        assertEquals(0, result.currentValue)
    }

    private fun objective(
        metric: ObjectiveMetric,
        unit: ObjectiveUnit,
        mediaType: MediaType?,
        target: Int,
    ) = Objective(
        name = "Test objective",
        metric = metric,
        unit = unit,
        mediaType = mediaType,
        targetValue = target,
        startDate = LocalDate.of(2026, 7, 1),
        endDate = LocalDate.of(2026, 7, 31),
    )

    private fun trackedMedia(id: Long, type: MediaType, sessions: List<TrackingSession>) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Item $id"),
        sessions = sessions,
    )

    private fun session(
        number: Int,
        finishedAt: LocalDate? = null,
        updates: List<ProgressUpdate> = emptyList(),
    ) = TrackingSession(
        id = number.toLong(),
        mediaItemId = 1,
        sessionNumber = number,
        status = if (finishedAt != null) TrackingStatus.Completed else TrackingStatus.InProgress,
        progressUpdates = updates,
        finishedAt = finishedAt,
    )

    private fun update(
        value: Int,
        date: LocalDate,
        countsTowardObjectives: Boolean = true,
        hasKnownDate: Boolean = true,
    ) = ProgressUpdate(
        id = value.toLong(),
        mediaItemId = 1,
        sessionId = 1,
        progressValue = value,
        loggedAt = date,
        countsTowardObjectives = countsTowardObjectives,
        hasKnownDate = hasKnownDate,
    )
}
