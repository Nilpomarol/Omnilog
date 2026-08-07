package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-type history switch: progress rows are optional, milestones are not.
 */
class TimelineHistoryFilterTest {
    private val builder = TimelineBuilder()
    private val day = LocalDate.of(2026, 7, 18)

    private val library = listOf(
        trackedBook(),
        trackedAnime(),
    )

    private val entries = builder.buildEntries(library)

    @Test
    fun historyOffForATypeDropsItsProgressRowsButKeepsItsMilestones() {
        val snapshot = entries.toSnapshot(TimelineFilters(historyMediaTypes = setOf(MediaType.Anime)))

        val bookKinds = snapshot.entries.filter { it.mediaType == MediaType.Book }.map { it.kind }
        assertTrue(TimelineEntryKind.Progress !in bookKinds)
        assertTrue(TimelineEntryKind.Start in bookKinds)
        assertTrue(TimelineEntryKind.Completion in bookKinds)
    }

    @Test
    fun historyIsPerTypeSoOneTypeKeepsItsProgressWhileAnotherLosesIt() {
        val snapshot = entries.toSnapshot(TimelineFilters(historyMediaTypes = setOf(MediaType.Anime)))

        val progressTypes = snapshot.entries
            .filter { it.kind == TimelineEntryKind.Progress }
            .map { it.mediaType }
            .toSet()
        assertEquals(setOf(MediaType.Anime), progressTypes)
    }

    @Test
    fun historyOffEverywhereLeavesOnlyMilestones() {
        val snapshot = entries.toSnapshot(TimelineFilters(historyMediaTypes = emptySet()))

        assertTrue(snapshot.entries.isNotEmpty())
        assertTrue(snapshot.entries.none { it.kind == TimelineEntryKind.Progress })
    }

    /** History is a standing preference, so it shapes "is there anything to show" like hiding does. */
    @Test
    fun hidingHistoryReducesTheUnfilteredCount() {
        val withHistory = entries.toSnapshot(TimelineFilters())
        val withoutHistory = entries.toSnapshot(TimelineFilters(historyMediaTypes = emptySet()))

        assertTrue(withoutHistory.unfilteredEntryCount < withHistory.unfilteredEntryCount)
    }

    @Test
    fun theModelDefaultKeepsEveryTypesHistorySoCallersMustOptOut() {
        val snapshot = entries.toSnapshot()

        assertTrue(snapshot.entries.any { it.kind == TimelineEntryKind.Progress })
    }

    @Test
    fun dayTotalsKeepEpisodesAndPagesApart() {
        val group = entries.toSnapshot().groups.first { it.date == day }

        assertEquals(30, group.progressByUnit[TimelineProgressUnit.Pages])
        assertEquals(2, group.progressByUnit[TimelineProgressUnit.Episodes])
    }

    @Test
    fun dayTotalsCountCompletionsSeparatelyFromProgress() {
        val group = entries.toSnapshot().groups.first { it.date == day.plusDays(1) }

        assertEquals(1, group.completedCount)
    }

    /** Within one day these timestamps put the finish first and the synthetic start last. */
    @Test
    fun withinOneDayTheMostRecentlyRecordedEntryLeads() {
        val sameDayEntries = builder.buildEntries(listOf(sameDaySession()))

        assertEquals(
            listOf(
                TimelineEntryKind.Completion,
                TimelineEntryKind.Start,
            ),
            sameDayEntries.map { it.kind },
        )
        assertEquals(listOf(100, 100), sameDayEntries.map { it.progress?.delta })
    }

    private fun sameDaySession() = TrackedMedia(
        item = MediaItem(id = 3, type = MediaType.Book, title = "Same day", progressTotal = 300),
        sessions = listOf(
            TrackingSession(
                id = 30,
                mediaItemId = 3,
                sessionNumber = 1,
                status = TrackingStatus.Completed,
                startedAt = day,
                finishedAt = day,
                progressUpdates = listOf(
                    progressUpdate(id = 5, sessionId = 30, mediaItemId = 3, value = 100, date = day),
                    progressUpdate(id = 6, sessionId = 30, mediaItemId = 3, value = 100, date = day),
                ),
            ),
        ),
    )

    private fun trackedBook() = TrackedMedia(
        item = MediaItem(id = 1, type = MediaType.Book, title = "Book", progressTotal = 300),
        sessions = listOf(
            TrackingSession(
                id = 10,
                mediaItemId = 1,
                sessionNumber = 1,
                status = TrackingStatus.Completed,
                startedAt = day.minusDays(2),
                finishedAt = day.plusDays(1),
                progressUpdates = listOf(
                    progressUpdate(id = 1, sessionId = 10, mediaItemId = 1, value = 100, date = day.minusDays(1)),
                    progressUpdate(id = 2, sessionId = 10, mediaItemId = 1, value = 30, date = day),
                ),
            ),
        ),
    )

    private fun trackedAnime() = TrackedMedia(
        item = MediaItem(id = 2, type = MediaType.Anime, title = "Anime", progressTotal = 24),
        sessions = listOf(
            TrackingSession(
                id = 20,
                mediaItemId = 2,
                sessionNumber = 1,
                status = TrackingStatus.InProgress,
                startedAt = day.minusDays(3),
                progressUpdates = listOf(
                    progressUpdate(id = 3, sessionId = 20, mediaItemId = 2, value = 5, date = day.minusDays(1)),
                    progressUpdate(id = 4, sessionId = 20, mediaItemId = 2, value = 2, date = day),
                ),
            ),
        ),
    )

    private fun progressUpdate(
        id: Long,
        sessionId: Long,
        mediaItemId: Long,
        value: Int,
        date: LocalDate,
    ) = ProgressUpdate(
        id = id,
        mediaItemId = mediaItemId,
        sessionId = sessionId,
        amount = value,
        loggedAt = date,
        hasKnownDate = true,
        createdAtEpochMillis = id * 100,
    )
}
