package com.nilpo.contenttracker.core.model

data class MetadataSuggestion(
    val source: MetadataSource,
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val collectionTitle: String? = null,
    val releaseYear: Int? = null,
    val language: String? = null,
    val genres: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    val credits: List<MediaCredit> = emptyList(),
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val sourceUrl: String? = null,
    val popularityScore: Double? = null,
    val rankingPosition: Int? = null,
    val rankingLabel: String? = null,
    val ratingDistributionJson: String? = null,
    val popularityJson: String? = null,
    val rankingJson: String? = null,
    val externalRating: MetadataRatingSuggestion? = null,
    val externalRatings: List<MetadataExternalRatingSuggestion> = emptyList(),
    val seasonSuggestions: List<MetadataSeasonSuggestion> = emptyList(),
    val bookEdition: BookEditionMetadata? = null,
    val bookEditionSuggestions: List<BookEditionMetadata> = emptyList(),
)

data class BookEditionMetadata(
    val externalId: String,
    val title: String? = null,
    val releaseYear: Int? = null,
    val language: String? = null,
    val pageCount: Int? = null,
    val coverUrl: String? = null,
    val isbn: String? = null,
    val format: String? = null,
    val publisher: String? = null,
    val sourceUrl: String? = null,
) {
    fun toMetadataSuggestion(work: MetadataSuggestion): MetadataSuggestion {
        return work.copy(
            externalId = externalId,
            title = title ?: work.title,
            releaseYear = releaseYear ?: work.releaseYear,
            language = language ?: work.language,
            progressTotal = pageCount ?: work.progressTotal,
            coverUrl = coverUrl ?: work.coverUrl,
            sourceUrl = sourceUrl ?: work.sourceUrl,
            bookEdition = this,
            bookEditionSuggestions = emptyList(),
        )
    }
}

data class MetadataSeasonSuggestion(
    val externalId: String,
    val seasonNumber: Int,
    val title: String,
    val releaseYear: Int? = null,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val sourceUrl: String? = null,
) {
    fun toMetadataSuggestion(series: MetadataSuggestion): MetadataSuggestion {
        return series.copy(
            externalId = externalId,
            title = "${series.title} - $title",
            originalTitle = null,
            collectionTitle = series.title,
            releaseYear = releaseYear ?: series.releaseYear,
            language = series.language,
            progressTotal = progressTotal,
            coverUrl = coverUrl ?: series.coverUrl,
            synopsis = synopsis ?: series.synopsis,
            sourceUrl = sourceUrl ?: series.sourceUrl,
            seasonSuggestions = emptyList(),
        )
    }
}

data class MetadataRatingSuggestion(
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
)

data class MetadataExternalRatingSuggestion(
    val source: ExternalRatingSource,
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
    Imdb,
    StoryGraph,
}
