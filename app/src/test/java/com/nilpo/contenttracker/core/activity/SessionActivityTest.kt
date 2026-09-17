package com.nilpo.contenttracker.core.activity

import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The one derivation Activitat and the Timeline both read. */
class SessionActivityTest {
    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun runningTotalsAccumulateFromTheSessionBaseline() {
        val rows = session(
            updates = listOf(entry(id = 1, day = 2, amount = 30), entry(id = 2, day = 5, amount = 20)),
            baselineProgress = 100,
        ).activity()

        // Newest first, so the later entry leads with the higher total.
        assertEquals(listOf(150, 130), rows.map { it.runningTotal })
    }

    @Test
    fun statusChangesAreInterleavedByWhenTheyWereRecorded() {
        val rows = session(
            updates = listOf(
                entry(id = 1, day = 2, amount = 30, createdAt = 100),
                entry(id = 2, day = 5, amount = 20, createdAt = 300),
            ),
            statusEvents = listOf(statusEvent(id = 9, createdAt = 200, status = TrackingStatus.Paused)),
        ).activity()

        assertEquals(listOf("entry:2", "status:9", "entry:1"), rows.map { it.key })
    }

    @Test
    fun terminalTransitionsRemainVisibleHistory() {
        val rows = session(
            updates = listOf(entry(id = 1, day = 2, amount = 30)),
            statusEvents = listOf(
                statusEvent(id = 8, createdAt = 200, status = TrackingStatus.Completed),
                statusEvent(id = 9, createdAt = 300, status = TrackingStatus.Paused),
            ),
        ).activity()

        assertEquals(listOf("status:9", "status:8", "entry:1"), rows.map { it.key })
    }

    @Test
    fun enteringProgressWithoutAKnownPredecessorIsNotShownAsAResume() {
        val rows = session(
            statusEvents = listOf(statusEvent(id = 8, createdAt = 200, status = TrackingStatus.InProgress)),
        ).activity()

        assertTrue(rows.isEmpty())
    }

    /** Hiding these made the newest visible row not the newest row, so deleting it restored nothing. */
    @Test
    fun everyRealTransitionIsARow() {
        val rows = session(
            statusEvents = listOf(
                statusEvent(1, 100, TrackingStatus.InProgress, previous = TrackingStatus.Planned),
                statusEvent(2, 200, TrackingStatus.Completed, previous = TrackingStatus.InProgress),
                statusEvent(3, 300, TrackingStatus.InProgress, previous = TrackingStatus.Completed),
                statusEvent(4, 400, TrackingStatus.Dropped, previous = TrackingStatus.InProgress),
                statusEvent(5, 500, TrackingStatus.InProgress, previous = TrackingStatus.Dropped),
                statusEvent(6, 600, TrackingStatus.Planned, previous = TrackingStatus.InProgress),
            ),
        ).activity()

        assertEquals(
            listOf(
                SessionActivityKind.Replanned,
                SessionActivityKind.Reopened,
                SessionActivityKind.Dropped,
                SessionActivityKind.Reopened,
                SessionActivityKind.Completed,
                SessionActivityKind.Started,
            ),
            rows.map { it.kind },
        )
    }

    @Test
    fun aTransitionIntoTheStateItLeftIsNotARow() {
        // What deleting the resume between two pauses leaves behind.
        val rows = session(
            statusEvents = listOf(
                statusEvent(1, 100, TrackingStatus.Paused, previous = TrackingStatus.InProgress),
                statusEvent(3, 300, TrackingStatus.Paused, previous = TrackingStatus.Paused),
            ),
        ).activity()

        assertEquals(listOf("status:1"), rows.map { it.key })
    }

    @Test
    fun onlyPeriodEntriesCarryAWindow() {
        val rows = session(
            startedAt = start,
            updates = listOf(
                entry(id = 1, day = 2, amount = 30),
                entry(id = 2, day = 20, amount = 20, coversPeriod = true),
            ),
        ).activity()

        val byId = rows.filter { it.kind == SessionActivityKind.Progress }.associateBy { it.progress?.id }
        assertNull(byId.getValue(1L).window)
        assertEquals(LocalDate.of(2026, 3, 2), byId.getValue(2L).window?.from)
    }

    @Test
    fun aBaselineAloneProducesNoRows() {
        assertTrue(session(baselineProgress = 40).activity().isEmpty())
    }

    @Test
    fun theSessionOpensAndClosesTheList() {
        val rows = session(
            startedAt = start,
            finishedAt = LocalDate.of(2026, 3, 20),
            status = TrackingStatus.Completed,
            updated = 1_000,
            updates = listOf(entry(id = 1, day = 5, amount = 30)),
        ).activity()

        assertEquals(listOf("session:Completed", "entry:1", "session:Started"), rows.map { it.key })
    }

    @Test
    fun anAbandonedSessionSaysSoRatherThanClaimingItWasFinished() {
        val rows = session(
            startedAt = start,
            finishedAt = LocalDate.of(2026, 3, 20),
            status = TrackingStatus.Dropped,
            updated = 1_000,
        ).activity()

        assertEquals(SessionActivityKind.Dropped, rows.first().kind)
    }

    @Test
    fun aFinishDateOnAnInProgressSessionIsNotAnEnding() {
        val rows = session(finishedAt = LocalDate.of(2026, 3, 20)).activity()

        assertTrue(rows.isEmpty())
    }

    @Test
    fun finalProgressFoldsIntoTheSnapshotCompletion() {
        val rows = session(
            startedAt = start,
            finishedAt = LocalDate.of(2026, 3, 20),
            status = TrackingStatus.Completed,
            updates = listOf(entry(id = 1, day = 5, amount = 30), entry(id = 2, day = 20, amount = 50)),
        ).activity()

        assertEquals(listOf("session:Completed", "entry:1", "session:Started"), rows.map { it.key })
        assertEquals(50, rows.first().progress?.amount)
        assertEquals(80, rows.first().runningTotal)
    }

    @Test
    fun firstProgressFoldsIntoTheStart() {
        val rows = session(
            startedAt = start,
            updates = listOf(entry(id = 1, day = 1, amount = 30), entry(id = 2, day = 10, amount = 50)),
        ).activity()

        assertEquals(listOf("entry:2", "session:Started"), rows.map { it.key })
        assertEquals(30, rows.last().progress?.amount)
        assertEquals(30, rows.last().runningTotal)
    }

    @Test
    fun theOpeningTransitionMergesIntoTheStart() {
        val rows = session(
            startedAt = LocalDate.of(2026, 3, 4),
            statusEvents = listOf(
                statusEvent(1, 100, TrackingStatus.InProgress, previous = TrackingStatus.Planned),
            ),
        ).activity()

        val started = rows.single()
        assertEquals("session:Started", started.key)
        assertEquals(1L, started.statusEvent?.id)
    }

    @Test
    fun finalProgressFoldsIntoTheCompletedTransition() {
        val rows = session(
            updates = listOf(entry(id = 1, day = 20, amount = 50, createdAt = 100)),
            statusEvents = listOf(statusEvent(id = 8, createdAt = 200, status = TrackingStatus.Completed, day = 20)),
        ).activity()

        val completed = rows.single()
        assertEquals("status:8", completed.key)
        assertEquals(50, completed.progress?.amount)
        assertEquals(50, completed.runningTotal)
    }

    @Test
    fun unknownDaysAreNullRatherThanAPlaceholder() {
        val rows = session(
            updates = listOf(entry(id = 1, day = 9, amount = 5).copy(hasKnownDate = false)),
            statusEvents = listOf(
                statusEvent(2, 200, TrackingStatus.Paused).copy(hasKnownDate = false),
            ),
        ).activity()

        assertTrue(rows.all { it.date == null })
    }

    @Test
    fun anEndingCannotPrecedeTheStartOrADatedTransition() {
        val paused = statusEvent(1, 100, TrackingStatus.Paused, day = 10, previous = TrackingStatus.InProgress)

        assertEquals(LocalDate.of(2026, 3, 10), session(startedAt = start, statusEvents = listOf(paused)).earliestEndingDate())
        assertEquals(start, session(startedAt = start).earliestEndingDate())
        // An undated transition's day is a placeholder and constrains nothing.
        assertNull(session(statusEvents = listOf(paused.copy(hasKnownDate = false))).earliestEndingDate())
    }

    private fun session(
        status: TrackingStatus = TrackingStatus.InProgress,
        startedAt: LocalDate? = null,
        finishedAt: LocalDate? = null,
        baselineProgress: Int = 0,
        updated: Long = 0,
        updates: List<ProgressUpdate> = emptyList(),
        statusEvents: List<SessionStatusEvent> = emptyList(),
    ) = TrackingSession(
        id = 10,
        mediaItemId = 1,
        sessionNumber = 1,
        status = status,
        baselineProgress = baselineProgress,
        startedAt = startedAt,
        finishedAt = finishedAt,
        updatedAtEpochMillis = updated,
        progressUpdates = updates,
        statusEvents = statusEvents,
    )

    private fun entry(
        id: Long,
        day: Int,
        amount: Int,
        createdAt: Long = id * 100,
        coversPeriod: Boolean = false,
    ) = ProgressUpdate(
        id = id,
        mediaItemId = 1,
        sessionId = 10,
        amount = amount,
        loggedAt = LocalDate.of(2026, 3, day),
        hasKnownDate = true,
        createdAtEpochMillis = createdAt,
        coversPeriod = coversPeriod,
    )

    private fun statusEvent(
        id: Long,
        createdAt: Long,
        status: TrackingStatus,
        day: Int = 4,
        previous: TrackingStatus? = null,
    ) = SessionStatusEvent(
        id = id,
        sessionId = 10,
        previousStatus = previous,
        status = status,
        occurredOn = LocalDate.of(2026, 3, day),
        createdAtEpochMillis = createdAt,
    )
}
