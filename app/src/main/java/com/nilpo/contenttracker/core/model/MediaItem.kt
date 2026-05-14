package com.nilpo.contenttracker.core.model

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val title: String,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val externalId: String? = null,
    val sourceApi: String? = null,
    val ownership: Ownership = Ownership(isOwned = false),
)
