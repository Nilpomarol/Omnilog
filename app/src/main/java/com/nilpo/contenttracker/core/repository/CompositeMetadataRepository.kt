package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope

class CompositeMetadataRepository(
    private val tmdb: TmdbMetadataRepository,
    private val aniList: AniListMetadataRepository,
    private val openLibrary: OpenLibraryMetadataRepository,
    private val googleBooks: GoogleBooksMetadataRepository,
    private val rawg: RawgMetadataRepository,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        return searchSuggestionsWithDiagnostics(request).suggestions
    }

    override suspend fun searchSuggestionsWithDiagnostics(request: MetadataSearchRequest): MetadataSearchResult = coroutineScope {
        val searches = buildList {
            val tmdbTypes = request.mediaTypes.intersect(setOf(MediaType.Movie, MediaType.TvShow))
            if (tmdbTypes.isNotEmpty()) {
                add(async {
                    searchProvider(MetadataSource.Tmdb) {
                        tmdb.searchSuggestions(request.copy(mediaTypes = tmdbTypes))
                    }
                })
            }
            if (MediaType.Anime in request.mediaTypes) {
                add(async {
                    searchProvider(MetadataSource.AniList) {
                        aniList.searchSuggestions(request.copy(mediaTypes = setOf(MediaType.Anime)))
                    }
                })
            }
            if (MediaType.Book in request.mediaTypes) {
                add(async { bookSuggestions(request.copy(mediaTypes = setOf(MediaType.Book))) })
            }
            if (MediaType.Game in request.mediaTypes) {
                add(async {
                    searchProvider(MetadataSource.Rawg) {
                        rawg.searchSuggestions(request.copy(mediaTypes = setOf(MediaType.Game)))
                    }
                })
            }
        }
        val results = searches.awaitAll()
        MetadataSearchResult(
            suggestions = results.flatMap { it.suggestions },
            failedSources = results.flatMapTo(mutableSetOf()) { it.failedSources },
        )
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        return when (suggestion.source) {
            MetadataSource.Tmdb -> tmdb.getSuggestionDetails(suggestion)
            MetadataSource.AniList -> aniList.getSuggestionDetails(suggestion)
            MetadataSource.Jikan -> aniList.getSuggestionDetails(suggestion)
            MetadataSource.OpenLibrary -> openLibrary.getSuggestionDetails(suggestion)
            MetadataSource.GoogleBooks -> googleBooks.getSuggestionDetails(suggestion)
            MetadataSource.Rawg -> rawg.getSuggestionDetails(suggestion)
            else -> suggestion
        }
    }

    private suspend fun bookSuggestions(request: MetadataSearchRequest): ProviderSearchResult = coroutineScope {
        val openLibrarySearch = async {
            searchProvider(MetadataSource.OpenLibrary) { openLibrary.searchSuggestions(request) }
        }
        val googleBooksSearch = async {
            searchProvider(MetadataSource.GoogleBooks) { googleBooks.searchSuggestions(request) }
        }
        val results = awaitAll(openLibrarySearch, googleBooksSearch)

        ProviderSearchResult(
            suggestions = results.flatMap { it.suggestions }
            .groupBy { it.bookMatchKey() }
            .values
            .map { it.mergeBookSuggestions() }
            .sortedByDescending { it.bookQualityScore(request.query) }
            .take(20),
            failedSources = results.flatMapTo(mutableSetOf()) { it.failedSources },
        )
    }

    private suspend fun searchProvider(
        source: MetadataSource,
        search: suspend () -> List<MetadataSuggestion>,
    ): ProviderSearchResult {
        return try {
            ProviderSearchResult(suggestions = search())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            ProviderSearchResult(failedSources = setOf(source))
        }
    }
}

private data class ProviderSearchResult(
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val failedSources: Set<MetadataSource> = emptySet(),
)

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
        externalRatings = (preferred.externalRatings + fallback?.externalRatings.orEmpty()).distinctBy { it.source },
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
