package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class TrackingSession(
    val id: Long,
    val mediaItemId: Long,
    /**
     * Monotonic per-title allocation number, not a position. Deleting a past session does not
     * renumber the survivors — undo reinserts a session under its original number and refuses if
     * that number is taken — so this can be sparse and can start above 1. To ask which time
     * through a title a session was, use [TrackedMedia.visitNumber] or [TrackedMedia.isRevisit].
     */
    val sessionNumber: Int,
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    val rating: Int? = null,
    val notes: String? = null,
    val platform: ConsumptionPlatform? = null,
    val startedAt: LocalDate? = null,
    val finishedAt: LocalDate? = null,
    val updatedAtEpochMillis: Long = 0,
    val progressUpdates: List<ProgressUpdate> = emptyList(),
)
