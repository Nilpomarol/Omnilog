package com.nilpo.contenttracker.core.model

data class AddTrackingSessionRequest(
    val mediaItemId: Long,
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    val progressTotal: Int? = null,
    val platformName: String? = null,
    val platformType: ConsumptionPlatformType = ConsumptionPlatformType.Other,
)
