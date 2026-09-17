package com.nilpo.contenttracker.ui.common

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompletionReactionTest {
    private val today = LocalDate.of(2026, 9, 16)

    @Test
    fun reachedGoalsLeadThenTitleCountsThenSpecificTypes() {
        val otherBook = media(2, MediaType.Book, listOf(session(3, TrackingStatus.Completed, finishedAt = today)))
        val before = media(1, MediaType.Book, listOf(session(1, TrackingStatus.InProgress)))
        val after = media(1, MediaType.Book, listOf(session(1, TrackingStatus.Completed, finishedAt = today)))
        val anyMedia = objective(1, mediaType = null, target = 10)
        val books = objective(2, mediaType = MediaType.Book, target = 10)
        val reachedAnyMedia = objective(3, mediaType = null, target = 2)

        val steps = objectiveSteps(
            before = listOf(before, otherBook),
            after = listOf(after, otherBook),
            objectives = listOf(anyMedia, books, reachedAnyMedia),
            today = today,
        )

        assertEquals(listOf(3L, 2L, 1L), steps.map { it.progress.objective.id })
        assertTrue(steps[0].reached)
        assertFalse(steps[1].reached)
        assertEquals(1, steps[1].previousValue)
        assertEquals(2, steps[1].progress.currentValue)
    }

    @Test
    fun pageGoalsMoveThroughProgressEntries() {
        val before = media(1, MediaType.Book, listOf(session(1, TrackingStatus.InProgress, updates = listOf(update(90)))))
        val after = media(1, MediaType.Book, listOf(session(1, TrackingStatus.InProgress, updates = listOf(update(90), update(20)))))
        val pages = objective(1, MediaType.Book, target = 100, metric = ObjectiveMetric.ProgressUnits)

        val step = objectiveSteps(listOf(before), listOf(after), listOf(pages), today).single()

        assertTrue(step.reached)
        assertEquals(90, step.previousValue)
        assertEquals(110, step.progress.currentValue)
    }

    @Test
    fun archivedAndUnmovedGoalsAreLeftOut() {
        val item = media(1, MediaType.Book, listOf(session(1, TrackingStatus.Completed, finishedAt = today)))
        val archived = objective(1, MediaType.Book, target = 1).copy(archivedAtEpochMillis = 1)

        assertTrue(objectiveSteps(listOf(item), listOf(item), listOf(objective(2, MediaType.Book, 1)), today).isEmpty())
        assertTrue(objectiveSteps(emptyList(), listOf(item), listOf(archived), today).isEmpty())
    }

    @Test
    fun aCelebratedGoalStepsAsideForAnotherReachedByTheSameWrite() {
        val before = media(1, MediaType.TvShow, listOf(session(1, TrackingStatus.InProgress)))
        val after = media(1, MediaType.TvShow, listOf(session(1, TrackingStatus.Completed, finishedAt = today)))
        val celebrated = objective(8, MediaType.TvShow, target = 1)
        val fresh = objective(10, MediaType.TvShow, target = 1)

        val steps = objectiveSteps(listOf(before), listOf(after), listOf(celebrated, fresh), today)
            .withoutCelebrated { it.id == celebrated.id }

        assertEquals(listOf(10L, 8L), steps.map { it.progress.objective.id })
        assertTrue(steps[0].reached)
        assertFalse(steps[1].reached)
    }

    @Test
    fun completionReportsDurationVisitAndLeadingStep() {
        val first = session(1, TrackingStatus.Completed, finishedAt = LocalDate.of(2025, 3, 1))
        val second = session(2, TrackingStatus.InProgress)
        val book = media(1, MediaType.Book, listOf(first, second))

        val reaction = completionReaction(book, second, today.minusDays(12), today, steps = emptyList())

        assertEquals(12L, reaction.daysTaken)
        assertEquals(2, reaction.visitNumber)
        assertNull(reaction.objective)
        assertNull(completionReaction(book, second, null, today, emptyList()).daysTaken)
    }

    private fun objective(
        id: Long,
        mediaType: MediaType?,
        target: Int,
        metric: ObjectiveMetric = ObjectiveMetric.CompletedTitles,
    ) = Objective(
        id = id,
        name = "Objective $id",
        metric = metric,
        unit = if (metric == ObjectiveMetric.CompletedTitles) ObjectiveUnit.Titles else ObjectiveUnit.Pages,
        mediaType = mediaType,
        targetValue = target,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 12, 31),
    )

    private fun media(id: Long, type: MediaType, sessions: List<TrackingSession>) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Item $id"),
        sessions = sessions,
    )

    private fun session(
        id: Long,
        status: TrackingStatus,
        finishedAt: LocalDate? = null,
        updates: List<ProgressUpdate> = emptyList(),
    ) = TrackingSession(
        id = id,
        mediaItemId = id,
        sessionNumber = id.toInt(),
        status = status,
        finishedAt = finishedAt,
        progressUpdates = updates,
    )

    private fun update(amount: Int) = ProgressUpdate(
        id = amount.toLong(),
        mediaItemId = 1,
        sessionId = 1,
        amount = amount,
        loggedAt = today,
    )
}
