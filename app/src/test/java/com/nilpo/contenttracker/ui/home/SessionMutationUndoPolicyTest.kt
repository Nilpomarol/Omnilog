package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.repository.DeletionRecovery
import com.nilpo.contenttracker.core.repository.SessionHistoryState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMutationUndoPolicyTest {
    @Test
    fun editorSaveOffersUndoWhenOnlyStatusAndProgressChanged() {
        val before = session()
        val recovery = mutation(before, before.copy(status = "InProgress", progressCurrent = 45))

        assertTrue(recovery.isStatusAndProgressOnly())
    }

    @Test
    fun editorCompletionOffersUndoForTheDatesItFillsIn() {
        val inProgress = session().copy(status = "InProgress", progressCurrent = 11)
        val completed = inProgress.copy(status = "Completed", progressCurrent = 12, finishedAtEpochDay = 20_700)
        assertTrue(mutation(inProgress, completed).isStatusAndProgressOnly())

        val planned = session()
        val startedAndFinished = planned.copy(status = "Completed", startedAtEpochDay = 20_690, finishedAtEpochDay = 20_700)
        assertTrue(mutation(planned, startedAndFinished).isStatusAndProgressOnly())

        val reopened = completed.copy(status = "InProgress", finishedAtEpochDay = null)
        assertTrue(mutation(completed, reopened).isStatusAndProgressOnly())
    }

    @Test
    fun editorSaveDoesNotOfferUndoWhenAnExistingDateIsRewritten() {
        val started = session().copy(status = "InProgress", startedAtEpochDay = 20_600)
        val restarted = started.copy(status = "Completed", startedAtEpochDay = 20_650, finishedAtEpochDay = 20_700)
        assertFalse(mutation(started, restarted).isStatusAndProgressOnly())

        val completed = session().copy(status = "Completed", finishedAtEpochDay = 20_700)
        val dropped = completed.copy(status = "Dropped", finishedAtEpochDay = 20_710)
        assertFalse(mutation(completed, dropped).isStatusAndProgressOnly())
    }

    @Test
    fun editorSaveDoesNotOfferUndoWhenPersonalFieldsChanged() {
        val before = session()

        assertFalse(mutation(before, before.copy(ratingHalfPoints = 16)).isStatusAndProgressOnly())
        assertFalse(mutation(before, before.copy(notes = "Una nota")).isStatusAndProgressOnly())
        assertFalse(mutation(before, before.copy(startedAtEpochDay = 20_000)).isStatusAndProgressOnly())
        assertFalse(mutation(before, before.copy(finishedAtEpochDay = 20_001)).isStatusAndProgressOnly())
        assertFalse(mutation(before, before.copy(platformName = "Steam")).isStatusAndProgressOnly())
    }

    private fun mutation(
        before: TrackingSessionEntity,
        after: TrackingSessionEntity,
    ) = DeletionRecovery.SessionMutation(
        before = SessionHistoryState(before, progressUpdates = emptyList(), statusEvents = emptyList()),
        after = SessionHistoryState(after, progressUpdates = emptyList(), statusEvents = emptyList()),
    )

    private fun session() = TrackingSessionEntity(
        id = 1,
        mediaItemId = 2,
        sessionNumber = 1,
        status = "Planned",
        progressCurrent = 0,
    )
}
