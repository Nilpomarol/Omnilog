package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.database.entity.MalSyncQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MalSyncQueueStateTest {
    @Test
    fun unchangedPayloadIsSkippedUnlessTheUserExplicitlySyncsEverything() {
        assertTrue(isMalPayloadAlreadySynced("same", "same", force = false))
        assertFalse(isMalPayloadAlreadySynced("same", "same", force = true))
        assertFalse(isMalPayloadAlreadySynced("old", "new", force = false))
    }

    @Test
    fun newerLocalPayloadStaysPendingAfterAnOlderPutSucceeds() {
        val afterFirstPut = queue().afterSuccessfulPayload(
            sentPayloadHash = "first",
            latestPayloadHash = "second",
            attemptedAtEpochMillis = 10,
            succeededAtEpochMillis = 11,
        )

        assertEquals(MalSyncPendingState, afterFirstPut.state)
        assertEquals("first", afterFirstPut.lastSyncedPayloadHash)

        val afterSecondPut = afterFirstPut.afterSuccessfulPayload(
            sentPayloadHash = "second",
            latestPayloadHash = "second",
            attemptedAtEpochMillis = 12,
            succeededAtEpochMillis = 13,
        )

        assertEquals(MalSyncSyncedState, afterSecondPut.state)
        assertEquals("second", afterSecondPut.lastSyncedPayloadHash)
    }

    @Test
    fun retryResetsFailureWithoutForgettingTheLastConfirmedPayload() {
        val retried = queue(
            state = MalSyncFailedState,
            attemptCount = 3,
            lastError = "temporary failure",
            lastSyncedPayloadHash = "confirmed",
        ).forRetry(now = 20)

        assertEquals(MalSyncPendingState, retried.state)
        assertEquals(0, retried.attemptCount)
        assertNull(retried.lastError)
        assertEquals("confirmed", retried.lastSyncedPayloadHash)
        assertEquals(20, retried.updatedAtEpochMillis)
    }

    private fun queue(
        state: String = MalSyncPendingState,
        attemptCount: Int = 0,
        lastError: String? = null,
        lastSyncedPayloadHash: String? = null,
    ) = MalSyncQueueEntity(
        mediaItemId = 1,
        malId = 2,
        state = state,
        attemptCount = attemptCount,
        lastError = lastError,
        updatedAtEpochMillis = 1,
        lastSyncedPayloadHash = lastSyncedPayloadHash,
    )
}
