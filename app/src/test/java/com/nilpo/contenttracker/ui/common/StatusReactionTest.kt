package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusReactionTest {
    @Test
    fun deletedStatusEventUsesTheSpecificTransitionCopy() {
        assertEquals(
            R.string.deletion_undo_status_event_started,
            statusEventDeletionMessageResId(TrackingStatus.Planned, TrackingStatus.InProgress),
        )
        assertEquals(
            R.string.deletion_undo_status_event_resumed,
            statusEventDeletionMessageResId(TrackingStatus.Paused, TrackingStatus.InProgress),
        )
        assertEquals(
            R.string.deletion_undo_status_event_paused,
            statusEventDeletionMessageResId(TrackingStatus.InProgress, TrackingStatus.Paused),
        )
        assertEquals(
            R.string.deletion_undo_status_event_completed,
            statusEventDeletionMessageResId(TrackingStatus.InProgress, TrackingStatus.Completed),
        )
        assertEquals(
            R.string.deletion_undo_status_event_dropped,
            statusEventDeletionMessageResId(TrackingStatus.InProgress, TrackingStatus.Dropped),
        )
    }

    @Test
    fun loggedProgressShowsItsStepWhileSteppingAwayDoesNot() {
        val logged = reaction(TrackingStatus.InProgress, TrackingStatus.InProgress, from = 10, to = 11, total = 12)
        assertFalse(logged.statusChanged)
        assertEquals(1, logged.progressDelta)
        assertTrue(logged.showsProgress)

        val corrected = reaction(TrackingStatus.InProgress, TrackingStatus.InProgress, from = 11, to = 9, total = 12)
        assertEquals(-2, corrected.progressDelta)
        assertTrue(corrected.showsProgress)

        assertTrue(reaction(TrackingStatus.Planned, TrackingStatus.InProgress, from = 0, to = 0, total = 12).showsProgress)
        assertFalse(reaction(TrackingStatus.InProgress, TrackingStatus.Paused, from = 5, to = 5, total = 12).showsProgress)
        // A game has no total, so a start with nothing logged has no number worth showing.
        assertFalse(reaction(TrackingStatus.Planned, TrackingStatus.InProgress, from = 0, to = 0, total = null).showsProgress)
    }

    private fun reaction(previous: TrackingStatus, status: TrackingStatus, from: Int, to: Int, total: Int?) = StatusReaction(
        title = "Title",
        coverUrl = null,
        previousStatus = previous,
        status = status,
        mediaType = MediaType.TvShow,
        previousProgress = from,
        progress = to,
        progressTotal = total,
    )
}
