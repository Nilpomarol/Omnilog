package com.nilpo.contenttracker.core.model

data class ExternalTracking(
    val id: Long,
    val mediaItemId: Long,
    val source: ExternalTrackingSource,
    val externalItemId: String? = null,
    val url: String? = null,
    val isSynced: Boolean = false,
)

enum class ExternalTrackingSource {
    Mal,
    Imdb,
    StoryGraph,
    Goodreads,
    Letterboxd,
    Tmdb,
    Rawg,
    Backloggd,
    Other,
}
