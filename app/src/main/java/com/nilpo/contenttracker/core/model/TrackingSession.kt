package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class TrackingSession(
    val id: Long,
    val mediaItemId: Long,
    val sessionNumber: Int,
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    val rating: Int? = null,
    val notes: String? = null,
    val platform: ConsumptionPlatform? = null,
    val startedAt: LocalDate? = null,
    val finishedAt: LocalDate? = null,
) {
    val isRevisit: Boolean
        get() = sessionNumber > 1
}
