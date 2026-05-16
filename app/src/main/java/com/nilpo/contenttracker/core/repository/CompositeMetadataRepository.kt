package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion

class CompositeMetadataRepository(
    private val tmdb: TmdbMetadataRepository,
    private val aniList: AniListMetadataRepository,
    private val openLibrary: OpenLibraryMetadataRepository,
    private val googleBooks: GoogleBooksMetadataRepository,
    private val rawg: RawgMetadataRepository,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        return buildList {
            val tmdbTypes = request.mediaTypes.intersect(setOf(MediaType.Movie, MediaType.TvShow))
            if (tmdbTypes.isNotEmpty()) {
                addAll(tmdb.searchSuggestions(request.copy(mediaTypes = tmdbTypes)))
            }
            if (MediaType.Anime in request.mediaTypes) {
                addAll(aniList.searchSuggestions(request.copy(mediaTypes = setOf(MediaType.Anime))))
            }
            if (MediaType.Book in request.mediaTypes) {
                addAll(
                    bookSuggestions(
                        request = request.copy(mediaTypes = setOf(MediaType.Book)),
                    ),
                )
            }
            if (MediaType.Game in request.mediaTypes) {
                addAll(rawg.searchSuggestions(request.copy(mediaTypes = setOf(MediaType.Game))))
            }
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        return when (suggestion.source) {
            MetadataSource.Tmdb -> tmdb.getSuggestionDetails(suggestion)
            MetadataSource.AniList -> aniList.getSuggestionDetails(suggestion)
            MetadataSource.OpenLibrary -> openLibrary.getSuggestionDetails(suggestion)
            MetadataSource.GoogleBooks -> googleBooks.getSuggestionDetails(suggestion)
            MetadataSource.Rawg -> rawg.getSuggestionDetails(suggestion)
            else -> suggestion
        }
    }

    private suspend fun bookSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val openLibrarySuggestions = runCatching { openLibrary.searchSuggestions(request) }.getOrDefault(emptyList())
        val googleBooksSuggestions = runCatching { googleBooks.searchSuggestions(request) }.getOrDefault(emptyList())

        return (openLibrarySuggestions + googleBooksSuggestions)
            .groupBy { it.bookMatchKey() }
            .values
            .map { it.mergeBookSuggestions() }
            .sortedByDescending { it.bookQualityScore(request.query) }
            .take(20)
    }
}

private fun List<MetadataSuggestion>.mergeBookSuggestions(): MetadataSuggestion {
    val preferred = maxBy { suggestion ->
        when (suggestion.source) {
            MetadataSource.OpenLibrary -> 2
            MetadataSource.GoogleBooks -> 1
            else -> 0
        }
    }
    val fallback = firstOrNull { it != preferred }

    return preferred.copy(
        coverUrl = preferred.coverUrl ?: fallback?.coverUrl,
        synopsis = preferred.synopsis ?: fallback?.synopsis,
        progressTotal = preferred.progressTotal ?: fallback?.progressTotal,
        genres = preferred.genres.ifEmpty { fallback?.genres.orEmpty() },
        creators = preferred.creators.ifEmpty { fallback?.creators.orEmpty() },
        credits = preferred.credits.ifEmpty { fallback?.credits.orEmpty() },
        externalRating = preferred.externalRating ?: fallback?.externalRating,
        popularityScore = preferred.popularityScore ?: fallback?.popularityScore,
    )
}

private fun MetadataSuggestion.bookMatchKey(): String {
    return listOfNotNull(
        title.normalizedBookKey(),
        creators.firstOrNull()?.normalizedBookKey(),
        releaseYear?.toString(),
    ).joinToString("|")
}

private fun MetadataSuggestion.bookQualityScore(query: String): Int {
    val normalizedQuery = query.normalizedBookKey()
    var score = 0
    if (title.normalizedBookKey() == normalizedQuery) score += 50
    if (title.contains(query, ignoreCase = true)) score += 20
    if (creators.isNotEmpty()) score += 15
    if (coverUrl != null) score += 12
    if (progressTotal != null) score += 10
    if (synopsis != null) score += 8
    if (genres.isNotEmpty()) score += 5
    if (externalRating != null) score += 5
    if (source == MetadataSource.OpenLibrary) score += 10
    return score
}

private fun String.normalizedBookKey(): String {
    return lowercase()
        .replace(Regex("""\([^)]*\)"""), "")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
}
