package com.nilpo.contenttracker.core.model

data class ExternalRating(
    val id: Long,
    val mediaItemId: Long,
    val source: ExternalRatingSource,
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
    val scoreDescriptor: String? = null,
    val origin: ExternalRatingOrigin = ExternalRatingOrigin.Provider,
    /** The site's name when [source] is [ExternalRatingSource.Other]. */
    val customSourceName: String? = null,
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

    /** A site the app does not know, named by the user. Kept last so it sorts after the known ones. */
    Other,
    ;

    companion object {
        /**
         * The stored column holds a known source's enum name, or for [Other] the name the user typed.
         * So a custom site needs no column of its own, and one rating per source still holds per name.
         */
        fun fromStored(value: String): Pair<ExternalRatingSource, String?> =
            entries.firstOrNull { it != Other && it.name == value }?.let { it to null } ?: (Other to value)
    }
}

/** What goes in the stored source column; null for [ExternalRatingSource.Other] without a name. */
fun ExternalRatingSource.storedName(customName: String?): String? =
    if (this == ExternalRatingSource.Other) customName?.trim()?.takeIf { it.isNotEmpty() } else name

