package com.nilpo.contenttracker.core.model

data class ExternalRating(
    val id: Long,
    val mediaItemId: Long,
    val source: ExternalRatingSource,
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
    val origin: ExternalRatingOrigin = ExternalRatingOrigin.Provider,
)

enum class ExternalRatingOrigin {
    Provider,
    Manual,
}

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
