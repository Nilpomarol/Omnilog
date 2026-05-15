package com.nilpo.contenttracker.core.model

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val title: String,
    val collectionId: Long? = null,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val metadataExternalId: String? = null,
    val metadataSource: MetadataSource? = null,
    val ownership: Ownership = Ownership(isOwned = false),
)
