package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryInvariantTest {
    @Test
    fun missingHistoryBecomesBaselineWithoutChangingVisibleProgress() {
        val (sessions, updates) = normalizeIncrementBackup(
            sessions = listOf(session(progress = 100, baseline = 10)),
            updates = listOf(entry(id = 1, amount = 20)),
        )

        assertEquals(80, sessions.single().baselineProgress)
        assertEquals(20, updates.single().amount)
        assertInvariant(sessions.single(), updates)
    }

    @Test
    fun staleCacheNeverDeletesCanonicalHistory() {
        val (sessions, updates) = normalizeIncrementBackup(
            sessions = listOf(session(progress = 100, baseline = 0)),
            updates = listOf(
                entry(id = 1, amount = 70, day = 1),
                entry(id = 2, amount = 50, day = 2),
            ),
        )

        assertEquals(listOf(70, 50), updates.map { it.amount })
        assertEquals(120, sessions.single().progressCurrent)
        assertInvariant(sessions.single(), updates)
    }

    @Test
    fun cacheBelowBaselineAndEntriesIsRebuilt() {
        val (sessions, updates) = normalizeIncrementBackup(
            sessions = listOf(session(progress = 50, baseline = 100)),
            updates = listOf(entry(id = 1, amount = 30)),
        )

        assertEquals(listOf(30), updates.map { it.amount })
        assertEquals(100, sessions.single().baselineProgress)
        assertEquals(130, sessions.single().progressCurrent)
        assertInvariant(sessions.single(), updates)
    }

    private fun assertInvariant(
        session: TrackingSessionEntity,
        updates: List<ProgressUpdateEntity>,
    ) {
        assertEquals(
            session.progressCurrent,
            session.baselineProgress + updates.sumOf { it.amount },
        )
        assertTrue(updates.all { it.amount > 0 })
    }

    private fun session(progress: Int, baseline: Int) = TrackingSessionEntity(
        id = 10,
        mediaItemId = 1,
        sessionNumber = 1,
        status = "InProgress",
        progressCurrent = progress,
        baselineProgress = baseline,
    )

    private fun entry(id: Long, amount: Int, day: Long = id) = ProgressUpdateEntity(
        id = id,
        mediaItemId = 1,
        sessionId = 10,
        amount = amount,
        loggedAtEpochDay = day,
        createdAtEpochMillis = id,
    )
}
