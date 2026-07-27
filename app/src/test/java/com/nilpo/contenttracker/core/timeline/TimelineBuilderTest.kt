package com.nilpo.contenttracker.core.timeline

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineBuilderTest {
    private val builder = TimelineBuilder()
    private val day = LocalDate.of(2026, 7, 20)

    @Test
    fun everyEntryCarriesItsOwnAmountAndTheRunningTotal() {
        val result = buildWithUpdates(update(1, 40, day), update(2, 25, day.plusDays(1)))

        assertEquals(listOf(25, 40), result.entries.map { it.progress?.delta })
        assertEquals(listOf(65, 40), result.entries.map { it.progress?.value })
    }

    @Test
    fun theFirstEntryCountsInFullBecauseNothingPrecedesIt() {
        val entry = buildWithUpdates(update(1, 180, day)).entries.single()

        assertEquals(TimelineEntryKind.Progress, entry.kind)
        assertEquals(180, entry.progress?.value)
        assertEquals(180, entry.progress?.delta)
    }

    @Test
    fun sameDayFinalProgressAndCompletionAreMerged() {
        val session = session(
            status = TrackingStatus.Completed,
            finishedAt = day,
            rating = 9,
            updates = listOf(
                update(1, 50, day, created = 100),
                update(2, 50, day, created = 200),
            ),
        )
        val entries = builder.build(listOf(media(sessions = listOf(session)))).entries

        assertEquals(2, entries.size)
        // Same day, with Completion at rank 1 (top of day) and progress at rank 2.
        val completion = entries.first()
        assertEquals(TimelineEntryKind.Completion, completion.kind)
        assertEquals(100, completion.progress?.value)
        assertEquals(50, completion.progress?.delta)
        assertEquals(9, completion.rating)
        assertEquals("completion:1:10", completion.stableKey)
        assertEquals("progress:1:10:1", entries.last().stableKey)
    }

    @Test
    fun startsAndDynamicVisitNumbersProduceDistinctEvents() {
        val first = session(id = 10, number = 1, startedAt = day.minusDays(2))
        val revisit = session(id = 20, number = 3, startedAt = day)
        val entries = builder.build(listOf(media(sessions = listOf(revisit, first)))).entries

        assertEquals(listOf(TimelineEntryKind.Revisit, TimelineEntryKind.Start), entries.map { it.kind })
        assertEquals(listOf(2, 1), entries.map { it.visitNumber })
        assertEquals(listOf("start:1:20", "start:1:10"), entries.map { it.stableKey })
    }

    @Test
    fun deletingAnEarlierSessionRecalculatesVisitsWithoutChangingStableIdentity() {
        val first = session(id = 10, number = 1, startedAt = day.minusDays(2))
        val deletedMiddle = session(id = 20, number = 2, startedAt = day.minusDays(1))
        val later = session(id = 30, number = 3, startedAt = day)
        val before = builder.build(listOf(media(sessions = listOf(first, deletedMiddle, later))))
            .entries.first { it.sessionId == 30L }
        val after = builder.build(listOf(media(sessions = listOf(first, later))))
            .entries.first { it.sessionId == 30L }

        assertEquals(3, before.visitNumber)
        assertEquals(2, after.visitNumber)
        assertEquals(before.stableKey, after.stableKey)
        assertEquals(TimelineEntryKind.Revisit, after.kind)
    }

    @Test
    fun deletingTheFirstSessionPromotesTheSurvivorToAFirstVisit() {
        val deletedFirst = session(id = 10, number = 1, startedAt = day.minusDays(1))
        val survivor = session(id = 30, number = 3, startedAt = day)
        val before = builder.build(listOf(media(sessions = listOf(deletedFirst, survivor))))
            .entries.first { it.sessionId == 30L }
        val after = builder.build(listOf(media(sessions = listOf(survivor))))
            .entries.single()

        assertEquals(TimelineEntryKind.Revisit, before.kind)
        assertEquals(TimelineEntryKind.Start, after.kind)
        assertEquals(1, after.visitNumber)
        assertEquals(before.stableKey, after.stableKey)
    }

    @Test
    fun completedSessionWithoutFinishDateDoesNotInventACompletionDate() {
        val entries = builder.build(
            listOf(media(sessions = listOf(session(status = TrackingStatus.Completed))))
        ).entries

        assertTrue(entries.isEmpty())
    }

    @Test
    fun baselineProgressProducesNoEntryButStillCountsInTheTotal() {
        val session = session(baselineProgress = 90, updates = listOf(update(2, 10, day.plusDays(1))))
        val entries = builder.build(listOf(media(sessions = listOf(session)))).entries

        assertEquals(1, entries.size)
        assertEquals(10, entries.single().progress?.delta)
        assertEquals(100, entries.single().progress?.value)
    }

    @Test
    fun deletingAnEntryLeavesItsNeighboursUntouched() {
        val first = update(1, 10, day)
        val deleted = update(2, 10, day.plusDays(1))
        val latest = update(3, 10, day.plusDays(2))
        val before = buildWithUpdates(first, deleted, latest).entries.first()
        val after = buildWithUpdates(first, latest).entries.first()

        // The amount is unchanged, because it never depended on the row before it. Only the
        // running total moves, since there is now less in front of it.
        assertEquals(10, before.progress?.delta)
        assertEquals(10, after.progress?.delta)
        assertEquals(30, before.progress?.value)
        assertEquals(20, after.progress?.value)
        assertFalse(buildWithUpdates(first, latest).entries.any { it.stableKey.endsWith(":2") })
    }

    @Test
    fun theLastEntryOnTheFinishingDayIsFoldedIntoTheCompletion() {
        val completed = session(
            status = TrackingStatus.Completed,
            finishedAt = day,
            updates = listOf(update(1, 90, day)),
        )
        val entries = builder.build(listOf(media(sessions = listOf(completed)))).entries

        assertEquals(1, entries.size)
        assertEquals(TimelineEntryKind.Completion, entries.single().kind)
        assertEquals(90, entries.single().progress?.value)
        assertEquals(90, entries.single().progress?.delta)
    }

    @Test
    fun aSameDayCompletionKeepsTheStartEvent() {
        val completed = session(
            startedAt = day,
            status = TrackingStatus.Completed,
            finishedAt = day,
            updates = listOf(update(1, 90, day)),
        )
        val entries = builder.build(listOf(media(sessions = listOf(completed)))).entries

        assertEquals(
            listOf(TimelineEntryKind.Completion, TimelineEntryKind.Start),
            entries.map { it.kind },
        )
        assertEquals(90, entries.first().progress?.value)
    }

    @Test
    fun unknownProgressDatesSortAfterEveryDatedEntry() {
        val known = update(1, 20, day)
        val unknown = update(2, 30, day.plusDays(5), knownDate = false)
        val result = buildWithUpdates(known, unknown)

        assertEquals(listOf(day, null), result.entries.map { it.date })
        assertNull(result.entries.last().date)
    }

    @Test
    fun mediaAndYearFiltersApplyWithoutPromotingUnknownDates() {
        val book2025 = media(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(session(updates = listOf(update(1, 10, LocalDate.of(2025, 2, 1))))),
        )
        val anime2026 = media(
            id = 2,
            type = MediaType.Anime,
            sessions = listOf(session(id = 20, updates = listOf(update(2, 2, day)))),
        )
        val unknownBook = media(
            id = 3,
            type = MediaType.Book,
            sessions = listOf(session(id = 30, updates = listOf(update(3, 3, day, knownDate = false)))),
        )

        val result = builder.build(
            listOf(book2025, anime2026, unknownBook),
            TimelineFilters(media = TimelineMediaFilter.Books, year = 2025),
        )

        assertEquals(listOf(1L), result.entries.map { it.mediaItemId })
        assertEquals(listOf(2026, 2025), result.availableYears)
    }

    @Test
    fun excludedMediaTypesNeverContributeEntriesOrYears() {
        val visible = media(
            id = 1,
            type = MediaType.Book,
            sessions = listOf(session(updates = listOf(update(1, 10, LocalDate.of(2025, 2, 1))))),
        )
        val hidden = media(
            id = 2,
            type = MediaType.Anime,
            sessions = listOf(session(id = 20, updates = listOf(update(2, 2, day)))),
        )

        val result = builder.build(
            listOf(visible, hidden),
            TimelineFilters(excludedMediaTypes = setOf(MediaType.Anime)),
        )

        assertEquals(listOf(1L), result.entries.map { it.mediaItemId })
        assertEquals(listOf(2025), result.availableYears)
        assertEquals(1, result.unfilteredEntryCount)
    }

    @Test
    fun orderingAndStableKeysAreDeterministicForTies() {
        val updates = listOf(
            update(id = 9, amount = 30, date = day, created = 200),
            update(id = 7, amount = 20, date = day, created = 200),
            update(id = 3, amount = 10, date = day, created = 100),
        )
        val first = buildWithUpdates(*updates.reversed().toTypedArray()).entries
        val second = buildWithUpdates(*updates.toTypedArray()).entries

        assertEquals(first.map { it.stableKey }, second.map { it.stableKey })
        // Same day: oldest first by creation time, then by id for the two that tie on it.
        assertEquals(listOf("progress:1:10:3", "progress:1:10:7", "progress:1:10:9"), first.map { it.stableKey })
        assertEquals(first.map { it.stableKey }.size, first.map { it.stableKey }.toSet().size)
    }

    @Test
    fun aDroppedSessionWithAFinishDateBecomesADroppedEntry() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(status = TrackingStatus.Dropped, finishedAt = day),
                    ),
                ),
            ),
        )

        val dropped = result.entries.single { it.kind == TimelineEntryKind.Dropped }
        assertEquals(day, dropped.date)
        // A drop carries no verdict, so nothing should have put a score on it.
        assertEquals(null, dropped.rating)
    }

    /** No date means no day to sit on. Inventing one would place the event on the wrong day. */
    @Test
    fun aDroppedSessionWithoutAFinishDateProducesNoEntry() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(status = TrackingStatus.Dropped, finishedAt = null),
                    ),
                ),
            ),
        )

        assertTrue(result.entries.none { it.kind == TimelineEntryKind.Dropped })
    }

    /** Both endings open a day, so neither can be ordered after the progress that led to it. */
    @Test
    fun aDropIsOrderedBeforeTheSameDaysProgress() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.Dropped,
                            startedAt = day,
                            finishedAt = day,
                            updates = listOf(update(1, 40, day), update(2, 90, day)),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(TimelineEntryKind.Dropped, result.entries.first().kind)
        assertEquals(TimelineEntryKind.Start, result.entries.last().kind)
    }

    /** The whole point of the log: one status column could only ever remember the last of these. */
    @Test
    fun repeatedPausesAndResumesEachBecomeTheirOwnEntry() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.InProgress,
                            statusEvents = listOf(
                                statusEvent(1, TrackingStatus.Paused, day),
                                statusEvent(2, TrackingStatus.InProgress, day.plusDays(10)),
                                statusEvent(3, TrackingStatus.Paused, day.plusDays(20)),
                                statusEvent(4, TrackingStatus.InProgress, day.plusDays(30)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val kinds = result.entries
            .filter { it.kind == TimelineEntryKind.Paused || it.kind == TimelineEntryKind.Resumed }
            .sortedBy { it.date }
            .map { it.kind }
        assertEquals(
            listOf(
                TimelineEntryKind.Paused,
                TimelineEntryKind.Resumed,
                TimelineEntryKind.Paused,
                TimelineEntryKind.Resumed,
            ),
            kinds,
        )
    }

    /**
     * The only way to undo an accidental pause: the log is append-only, so a pause corrected the
     * same day has to disappear from the reading rather than be deleted from the record.
     */
    @Test
    fun aPauseResumedTheSameDayShowsNeitherRow() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.InProgress,
                            statusEvents = listOf(
                                statusEvent(1, TrackingStatus.Paused, day),
                                statusEvent(2, TrackingStatus.InProgress, day),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(result.entries.none { it.kind == TimelineEntryKind.Paused })
        assertTrue(result.entries.none { it.kind == TimelineEntryKind.Resumed })
    }

    /** A real break survives. Only the same-day correction is treated as noise. */
    @Test
    fun aPauseResumedOnALaterDayKeepsBothRows() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.InProgress,
                            statusEvents = listOf(
                                statusEvent(1, TrackingStatus.Paused, day),
                                statusEvent(2, TrackingStatus.InProgress, day.plusDays(1)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Paused })
        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Resumed })
    }

    /** An open pause has no resume to compare against, so it must not be collapsed away. */
    @Test
    fun aPauseThatIsStillOpenIsAlwaysShown() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.Paused,
                            statusEvents = listOf(statusEvent(1, TrackingStatus.Paused, day)),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Paused })
    }

    /** Starting a planned title moves it into progress, which is not the same as resuming it. */
    @Test
    fun movingIntoProgressWithoutAPauseIsNotAResume() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.InProgress,
                            statusEvents = listOf(statusEvent(1, TrackingStatus.InProgress, day)),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(result.entries.none { it.kind == TimelineEntryKind.Resumed })
    }

    /** Two pauses with no resume between them is one pause, however the rows got written. */
    @Test
    fun aSecondPauseWithoutAResumeIsNotADuplicateEntry() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.Paused,
                            statusEvents = listOf(
                                statusEvent(1, TrackingStatus.Paused, day),
                                statusEvent(2, TrackingStatus.Paused, day.plusDays(5)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Paused })
    }

    /** Endings come from the session, so the log must not emit a second copy of them. */
    @Test
    fun anEndingInTheLogDoesNotDoubleTheEndingFromTheSession() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.Dropped,
                            finishedAt = day,
                            statusEvents = listOf(statusEvent(1, TrackingStatus.Dropped, day)),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Dropped })
    }

    @Test
    fun reopeningASessionDoesNotEraseItsRecordedCompletion() {
        val result = builder.build(
            listOf(
                media(
                    sessions = listOf(
                        session(
                            status = TrackingStatus.InProgress,
                            finishedAt = null,
                            statusEvents = listOf(
                                statusEvent(1, TrackingStatus.Completed, day),
                                statusEvent(2, TrackingStatus.InProgress, day.plusDays(2)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, result.entries.count { it.kind == TimelineEntryKind.Completion })
        assertTrue(result.entries.none { it.kind == TimelineEntryKind.Resumed })
    }

    private fun statusEvent(id: Long, status: TrackingStatus, on: LocalDate) = SessionStatusEvent(
        id = id,
        sessionId = 10,
        status = status,
        occurredOn = on,
        createdAtEpochMillis = id * 100,
    )

    private fun buildWithUpdates(vararg updates: ProgressUpdate): TimelineSnapshot =
        builder.build(listOf(media(sessions = listOf(session(updates = updates.toList())))))

    private fun media(
        id: Long = 1,
        type: MediaType = MediaType.Book,
        sessions: List<TrackingSession>,
    ) = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Title $id", progressTotal = 300),
        sessions = sessions,
    )

    private fun session(
        id: Long = 10,
        number: Int = 1,
        status: TrackingStatus = TrackingStatus.InProgress,
        startedAt: LocalDate? = null,
        finishedAt: LocalDate? = null,
        rating: Int? = null,
        baselineProgress: Int = 0,
        updates: List<ProgressUpdate> = emptyList(),
        statusEvents: List<SessionStatusEvent> = emptyList(),
    ) = TrackingSession(
        id = id,
        mediaItemId = 1,
        sessionNumber = number,
        status = status,
        baselineProgress = baselineProgress,
        startedAt = startedAt,
        finishedAt = finishedAt,
        rating = rating,
        progressUpdates = updates,
        statusEvents = statusEvents,
    )

    private fun update(
        id: Long,
        amount: Int,
        date: LocalDate,
        created: Long = id * 100,
        knownDate: Boolean = true,
    ) = ProgressUpdate(
        id = id,
        mediaItemId = 1,
        sessionId = 10,
        amount = amount,
        loggedAt = date,
        hasKnownDate = knownDate,
        createdAtEpochMillis = created,
    )
}
