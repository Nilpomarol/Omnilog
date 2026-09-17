package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
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
}
