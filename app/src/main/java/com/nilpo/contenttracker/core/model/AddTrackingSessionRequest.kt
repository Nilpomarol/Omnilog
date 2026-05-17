package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class AddTrackingSessionRequest(
    val mediaItemId: Long,
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    val rating: Int? = null,
    val notes: String? = null,
    val startedAt: LocalDate? = null,
    val finishedAt: LocalDate? = null,
    val platformName: String? = null,
    val platformType: ConsumptionPlatformType = ConsumptionPlatformType.Other,
)
