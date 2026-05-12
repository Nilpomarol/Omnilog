package com.nilpo.contenttracker.core.model

data class ExternalRating(
    val id: Long,
    val mediaItemId: Long,
    val source: ExternalRatingSource,
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
)

enum class ExternalRatingSource {
    Mal,
    Imdb,
    Metacritic,
    Goodreads,
    GoogleBooks,
    Tmdb,
    Rawg,
}
