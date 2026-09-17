package com.nilpo.contenttracker.ui.detail

import com.nilpo.contenttracker.core.activity.SessionActivity
import com.nilpo.contenttracker.core.activity.SessionActivityKind
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityEditTargetTest {
    private val day = LocalDate.of(2026, 3, 4)
    private val update = ProgressUpdate(id = 1, mediaItemId = 1, sessionId = 10, amount = 30, loggedAt = day)
    private val event = SessionStatusEvent(
        id = 8,
        sessionId = 10,
        status = TrackingStatus.Completed,
        occurredOn = day,
        createdAtEpochMillis = 200,
    )

    /** The start's folded entry used to open nothing: the lookup only searched plain entry rows. */
    @Test
    fun aStartWithAFoldedEntryEditsTheEntry() {
        val row = SessionActivity(SessionActivityKind.Started, day, 0, progress = update)

        assertEquals(ActivityEditTarget.Progress(1), row.editingTarget())
    }

    @Test
    fun aCompletionWithAFoldedEntryEditsTheTransition() {
        val row = SessionActivity(SessionActivityKind.Completed, day, 200, progress = update, statusEvent = event)

        assertEquals(ActivityEditTarget.Status(8), row.editingTarget())
    }

    @Test
    fun aSnapshotCompletionFallsBackToItsEntry() {
        val row = SessionActivity(SessionActivityKind.Completed, day, 0, progress = update)

        assertEquals(ActivityEditTarget.Progress(1), row.editingTarget())
    }

    @Test
    fun aBareSnapshotMilestoneIsNotEditable() {
        assertEquals(null, SessionActivity(SessionActivityKind.Started, day, 0).editingTarget())
    }
}
