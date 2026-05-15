package com.nilpo.contenttracker.core.model

data class MediaCredit(
    val id: Long = 0,
    val mediaItemId: Long = 0,
    val personName: String,
    val roleType: MediaCreditRole,
    val characterName: String? = null,
    val sortOrder: Int = 0,
    val metadataSource: MetadataSource? = null,
)

enum class MediaCreditRole {
    Author,
    Director,
    Creator,
    Studio,
    Developer,
    Cast,
    VoiceActor,
}
