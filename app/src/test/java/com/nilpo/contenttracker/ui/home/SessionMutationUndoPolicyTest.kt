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
