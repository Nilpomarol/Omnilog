package com.nilpo.contenttracker.core.model

data class MediaCredit(
    val id: Long = 0,
    val mediaItemId: Long = 0,
    val personName: String,
    val roleType: MediaCreditRole,
    val characterName: String? = null,
    val personImageUrl: String? = null,
    /** The provider's original logo canvas, not the dimensions of a resized delivery URL. */
    val personImageAspectRatio: Float? = null,
    val characterImageUrl: String? = null,
    val sortOrder: Int = 0,
    val metadataSource: MetadataSource? = null,
)

enum class MediaCreditRole {
    Author,
    Director,
    Creator,
    Studio,
    Developer,
    Publisher,
    Cast,
    VoiceActor,
}
