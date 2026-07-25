package com.nilpo.contenttracker.ui.detail

import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one list Activitat reads from: entries and status changes interleaved, newest first.
 */
class ActivityRowsTest {
    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun runningTotalsAccumulateFromTheSessionBaseline() {
        val rows = activityRows(
            updates = listOf(entry(id = 1, day = 2, amount = 30), entry(id = 2, day = 5, amount = 20)),
            statusEvents = emptyList(),
            baselineProgress = 100,
            sessionStartedAt = null,
        )

        // Newest first, so the later entry leads with the higher total.
        assertEquals(listOf(150, 130), rows.map { (it as ActivityRow.Entry).runningTotal })
    }

    @Test
    fun withNoBaselineTotalsStartFromTheFirstEntry() {
        val rows = activityRows(
            updates = listOf(entry(id = 1, day = 2, amount = 30)),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = null,
        )

        assertEquals(30, (rows.single() as ActivityRow.Entry).runningTotal)
    }

    @Test
    fun statusChangesAreInterleavedByWhenTheyWereRecorded() {
        // A pause recorded between two entries sits between them, which is the whole reason the two
        // histories share one list.
        val rows = activityRows(
            updates = listOf(
                entry(id = 1, day = 2, amount = 30, createdAt = 100),
                entry(id = 2, day = 5, amount = 20, createdAt = 300),
            ),
            statusEvents = listOf(statusEvent(id = 9, createdAt = 200, status = TrackingStatus.Paused)),
            baselineProgress = 0,
            sessionStartedAt = null,
        )

        assertEquals(listOf("entry:2", "status:9", "entry:1"), rows.map { it.key })
    }

    @Test
    fun terminalTransitionsRemainVisibleHistory() {
        val rows = activityRows(
            updates = listOf(entry(id = 1, day = 2, amount = 30)),
            statusEvents = listOf(
                statusEvent(id = 8, createdAt = 200, status = TrackingStatus.Completed),
                statusEvent(id = 9, createdAt = 300, status = TrackingStatus.Paused),
            ),
            baselineProgress = 0,
            sessionStartedAt = null,
        )

        assertEquals(listOf("status:9", "status:8", "entry:1"), rows.map { it.key })
    }

    @Test
    fun enteringProgressWithoutAPauseIsNotShownAsAResume() {
        val rows = activityRows(
            updates = emptyList(),
            statusEvents = listOf(
                statusEvent(id = 8, createdAt = 200, status = TrackingStatus.InProgress),
            ),
            baselineProgress = 0,
            sessionStartedAt = null,
        )

        assertTrue(rows.isEmpty())
    }

    @Test
    fun onlyPeriodEntriesCarryAWindow() {
        val rows = activityRows(
            updates = listOf(
                entry(id = 1, day = 2, amount = 30),
                entry(id = 2, day = 20, amount = 20, coversPeriod = true),
            ),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
        )

        val byId = rows.filterIsInstance<ActivityRow.Entry>().associateBy { it.update.id }
        assertNull(byId.getValue(1L).window)
        assertEquals(LocalDate.of(2026, 3, 2), byId.getValue(2L).window?.from)
    }

    @Test
    fun anEmptySessionProducesNoRows() {
        val rows = activityRows(
            updates = emptyList(),
            statusEvents = emptyList(),
            baselineProgress = 40,
            sessionStartedAt = null,
        )

        assertTrue(rows.isEmpty())
    }

    @Test
    fun theSessionOpensAndClosesTheList() {
        val rows = activityRows(
            updates = listOf(entry(id = 1, day = 5, amount = 30)),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
            sessionFinishedAt = LocalDate.of(2026, 3, 20),
            sessionStatus = TrackingStatus.Completed,
        )

        // Newest first, so the finish leads and the start closes it out, whatever the entries did.
        assertEquals(
            listOf("milestone:Finished", "entry:1", "milestone:Started"),
            rows.map { it.key },
        )
    }

    @Test
    fun anAbandonedSessionSaysSoRatherThanClaimingItWasFinished() {
        val rows = activityRows(
            updates = emptyList(),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
            sessionFinishedAt = LocalDate.of(2026, 3, 20),
            sessionStatus = TrackingStatus.Dropped,
        )

        assertEquals(MilestoneKind.Abandoned, (rows.first() as ActivityRow.Milestone).kind)
    }

    @Test
    fun anUnfinishedSessionHasAStartButNoEnd() {
        val rows = activityRows(
            updates = emptyList(),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
            sessionFinishedAt = null,
            sessionStatus = TrackingStatus.InProgress,
        )

        assertEquals(listOf("milestone:Started"), rows.map { it.key })
    }

    @Test
    fun aFinishDateOnAnInProgressSessionIsNotAnEnding() {
        // A stale finish date on a session that is not actually over would otherwise print a
        // milestone that contradicts the session's own status.
        val rows = activityRows(
            updates = emptyList(),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = null,
            sessionFinishedAt = LocalDate.of(2026, 3, 20),
            sessionStatus = TrackingStatus.InProgress,
        )

        assertTrue(rows.isEmpty())
    }

    @Test
    fun finalProgressUpdateFoldsIntoFinishedMilestone() {
        val rows = activityRows(
            updates = listOf(
                entry(id = 1, day = 5, amount = 30),
                entry(id = 2, day = 20, amount = 50),
            ),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
            sessionFinishedAt = LocalDate.of(2026, 3, 20),
            sessionStatus = TrackingStatus.Completed,
        )

        assertEquals(listOf("milestone:Finished", "entry:1", "milestone:Started"), rows.map { it.key })
        val finished = rows.first() as ActivityRow.Milestone
        assertEquals(50, finished.update?.amount)
        assertEquals(80, finished.runningTotal)
    }

    @Test
    fun firstProgressUpdateFoldsIntoStartedMilestone() {
        val rows = activityRows(
            updates = listOf(
                entry(id = 1, day = 1, amount = 30),
                entry(id = 2, day = 10, amount = 50),
            ),
            statusEvents = emptyList(),
            baselineProgress = 0,
            sessionStartedAt = start,
            sessionFinishedAt = null,
            sessionStatus = TrackingStatus.InProgress,
        )

        assertEquals(listOf("entry:2", "milestone:Started"), rows.map { it.key })
        val started = rows.last() as ActivityRow.Milestone
        assertEquals(30, started.update?.amount)
        assertEquals(30, started.runningTotal)
    }

    @Test
    fun finalProgressUpdateFoldsIntoCompletedStatusEvent() {
        val rows = activityRows(
            updates = listOf(
                entry(id = 1, day = 20, amount = 50, createdAt = 100),
            ),
            statusEvents = listOf(
                statusEvent(id = 8, createdAt = 200, status = TrackingStatus.Completed, day = 20),
            ),
            baselineProgress = 0,
            sessionStartedAt = null,
        )

        assertEquals(listOf("status:8"), rows.map { it.key })
        val status = rows.single() as ActivityRow.Status
        assertEquals(50, status.update?.amount)
        assertEquals(50, status.runningTotal)
    }

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
    ) = SessionStatusEvent(
        id = id,
        sessionId = 10,
        status = status,
        occurredOn = LocalDate.of(2026, 3, day),
        createdAtEpochMillis = createdAt,
    )
}
