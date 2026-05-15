package com.nilpo.contenttracker.core.model

data class AddTrackedMediaRequest(
    val type: MediaType,
    val title: String,
    val progressTotal: Int?,
    val initialStatus: TrackingStatus,
    val isOwned: Boolean,
    val ownershipType: OwnershipType,
    val platformName: String?,
    val platformType: ConsumptionPlatformType,
    val metadataSource: MetadataSource? = null,
    val metadataExternalId: String? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
)
