package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackingStatusTest {

    /**
     * Pinned as a set rather than asserted case by case: adding a status should fail this test and
     * force a decision about whether it ends a session, instead of silently defaulting to "no".
     */
    @Test
    fun onlyCompletingAndAbandoningEndASession() {
        assertEquals(
            setOf(TrackingStatus.Completed, TrackingStatus.Dropped),
            TrackingStatus.entries.filter { it.endsSession }.toSet(),
        )
    }

    /**
     * A pause interrupts rather than ends, so it must never invite a finish date. One written there
     * would claim the session was over, and would be left stale the moment it was resumed.
     */
    @Test
    fun pausingDoesNotEndASession() {
        assertFalse(TrackingStatus.Paused.endsSession)
    }
}
