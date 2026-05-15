package com.nilpo.contenttracker.core.model

data class MetadataSuggestion(
    val source: MetadataSource,
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val collectionTitle: String? = null,
    val releaseYear: Int? = null,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val sourceUrl: String? = null,
    val externalRating: MetadataRatingSuggestion? = null,
)

data class MetadataRatingSuggestion(
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
)

data class MetadataSearchRequest(
    val query: String,
    val mediaTypes: Set<MediaType>,
)

enum class MetadataSource {
    AniList,
    Jikan,
    OpenLibrary,
    GoogleBooks,
    Tmdb,
    Rawg,
}
