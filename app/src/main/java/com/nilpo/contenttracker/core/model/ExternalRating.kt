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
    AniList,
    Mal,
    Imdb,
    Metacritic,
    Goodreads,
    GoogleBooks,
    OpenLibrary,
    Tmdb,
    Rawg,
    RottenTomatoes,
    Steam,
    FilmAffinity,
    StoryGraph,
}
