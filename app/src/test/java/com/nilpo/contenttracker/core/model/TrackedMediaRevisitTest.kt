package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedMediaRevisitTest {
    @Test
    fun plannedRepeatSessionIsNotCountedAsARevisitUntilItStarts() {
        val firstVisit = session(id = 1, sessionNumber = 1, status = TrackingStatus.Completed)
        val plannedRepeat = session(id = 2, sessionNumber = 2, status = TrackingStatus.Planned)
        val media = trackedMedia(firstVisit, plannedRepeat)

        assertEquals(0, media.revisitCount)
        assertFalse(media.isRevisit(plannedRepeat))
    }

    @Test
    fun startedRepeatSessionCountsAsARevisit() {
        val firstVisit = session(id = 1, sessionNumber = 1, status = TrackingStatus.Completed)
        val startedRepeat = session(id = 2, sessionNumber = 2, status = TrackingStatus.InProgress)
        val media = trackedMedia(firstVisit, startedRepeat)

        assertEquals(1, media.revisitCount)
        assertTrue(media.isRevisit(startedRepeat))
    }

    private fun trackedMedia(vararg sessions: TrackingSession) = TrackedMedia(
        item = MediaItem(
            id = 1,
            type = MediaType.Book,
            title = "Test",
        ),
        sessions = sessions.toList(),
    )

    private fun session(
        id: Long,
        sessionNumber: Int,
        status: TrackingStatus,
    ) = TrackingSession(
        id = id,
        mediaItemId = 1,
        sessionNumber = sessionNumber,
        status = status,
    )
}
