package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MalSyncPayloadTest {
    @Test
    fun mapsCurrentSessionAndCompletedRewatches() {
        val payload = buildMalSyncPayload(
            item = anime(),
            sessions = listOf(
                session(1, TrackingStatus.Completed, progress = 12),
                session(2, TrackingStatus.Completed, progress = 12),
                session(3, TrackingStatus.InProgress, progress = 4, rating = 8, notes = "Second revisit"),
            ),
        )!!

        assertEquals("watching", payload.status)
        assertEquals(4, payload.watchedEpisodes)
        assertEquals(8, payload.score)
        assertEquals("Second revisit", payload.comments)
        assertTrue(payload.isRewatching)
        assertEquals(1, payload.completedRewatches)
    }

    @Test
    fun missingOptionalValuesExplicitlyClearMalProjection() {
        val payload = buildMalSyncPayload(
            item = anime(),
            sessions = listOf(session(1, TrackingStatus.Planned, progress = 0)),
        )!!

        assertEquals("plan_to_watch", payload.status)
        assertEquals(0, payload.score)
        assertEquals("", payload.comments)
        assertEquals("", payload.startDate)
        assertEquals("", payload.finishDate)
        assertFalse(payload.isRewatching)
    }

    @Test
    fun mapsDatesAndFormEncoding() {
        val payload = buildMalSyncPayload(
            item = anime(),
            sessions = listOf(
                session(
                    number = 1,
                    status = TrackingStatus.Completed,
                    progress = 12,
                    startedAt = LocalDate.of(2026, 1, 2),
                    finishedAt = LocalDate.of(2026, 1, 3),
                ),
            ),
        )!!

        assertEquals("2026-01-02", payload.startDate)
        assertEquals("2026-01-03", payload.finishDate)
        assertEquals("a%26b=x+y", mapOf("a&b" to "x y").toFormBody())
    }

    @Test
    fun mapsEveryLocalStatusToMal() {
        val expected = mapOf(
            TrackingStatus.Planned to "plan_to_watch",
            TrackingStatus.InProgress to "watching",
            TrackingStatus.Completed to "completed",
            TrackingStatus.Paused to "on_hold",
            TrackingStatus.Dropped to "dropped",
        )

        expected.forEach { (status, malStatus) ->
            val payload = buildMalSyncPayload(anime(), listOf(session(1, status, progress = 0)))
            assertEquals(malStatus, payload?.status)
        }
    }

    private fun anime() = MediaItemEntity(type = MediaType.Anime.name, title = "Anime", malId = 1)

    private fun session(
        number: Int,
        status: TrackingStatus,
        progress: Int,
        rating: Int? = null,
        notes: String? = null,
        startedAt: LocalDate? = null,
        finishedAt: LocalDate? = null,
    ) = TrackingSessionEntity(
        id = number.toLong(),
        mediaItemId = 1,
        sessionNumber = number,
        status = status.name,
        progressCurrent = progress,
        rating = rating,
        notes = notes,
        startedAtEpochDay = startedAt?.toEpochDay(),
        finishedAtEpochDay = finishedAt?.toEpochDay(),
    )
}
