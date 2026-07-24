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
            failures = results.flatMap { it.failures },
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

    override suspend fun resolveImportedExternalId(
        source: MetadataSource,
        externalId: String,
        mediaType: MediaType,
    ): MetadataSuggestion? {
        return when (source) {
            MetadataSource.Imdb -> tmdb.findByImdbId(externalId, mediaType)
            MetadataSource.StoryGraph -> coroutineScope {
                val openLibraryRequest = async {
                    captureProviderResult { openLibrary.findByIsbn(externalId) }
                }
                val googleBooksRequest = async {
                    captureProviderResult { googleBooks.findByIsbn(externalId) }
                }
                val openLibraryMatch = openLibraryRequest.await()
                val googleMatch = googleBooksRequest.await()
                val openLibrarySuggestion = openLibraryMatch.getOrNull()
                mergeExactBookMatches(openLibrarySuggestion, googleMatch.getOrNull())
                    ?: openLibraryMatch.retryableExceptionOrNull()?.let { throw it }
                    ?: googleMatch.retryableExceptionOrNull()?.let { throw it }
            }
            else -> null
        }
    }

    override fun isImportedSourceAvailable(source: MetadataSource): Boolean = when (source) {
        MetadataSource.Imdb -> tmdb.isConfigured
        MetadataSource.StoryGraph -> true // OpenLibrary does not require a credential.
        else -> true
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
            .sortedByDescending { it.bookQualityScore(request.query) }
            .take(20),
            failedSources = results.flatMapTo(mutableSetOf()) { it.failedSources },
            failures = results.flatMap { it.failures },
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
        } catch (exception: Throwable) {
            ProviderSearchResult(
                failedSources = setOf(source),
                failures = listOf(exception.toSearchFailure(source)),
            )
        }
    }
}

internal fun mergeExactBookMatches(
    openLibrary: MetadataSuggestion?,
    googleBooks: MetadataSuggestion?,
): MetadataSuggestion? {
    val primary = openLibrary ?: return googleBooks
    val secondary = googleBooks ?: return primary
    val primaryEdition = primary.bookEdition
    val secondaryEdition = secondary.bookEdition
    val mergedEdition = when {
        primaryEdition != null -> primaryEdition.copy(
            title = primaryEdition.title ?: secondaryEdition?.title,
            releaseYear = primaryEdition.releaseYear ?: secondaryEdition?.releaseYear,
            language = primaryEdition.language ?: secondaryEdition?.language,
            pageCount = primaryEdition.pageCount ?: secondaryEdition?.pageCount,
            coverUrl = primaryEdition.coverUrl ?: secondaryEdition?.coverUrl,
            isbn = primaryEdition.isbn ?: secondaryEdition?.isbn,
            format = primaryEdition.format ?: secondaryEdition?.format,
            publisher = primaryEdition.publisher ?: secondaryEdition?.publisher,
            sourceUrl = primaryEdition.sourceUrl ?: secondaryEdition?.sourceUrl,
        )
        else -> secondaryEdition
    }
    val exactIsbn = mergedEdition?.isbn
    return primary.copy(
        originalTitle = primary.originalTitle ?: secondary.originalTitle,
        collectionTitle = primary.collectionTitle ?: secondary.collectionTitle,
        releaseYear = primary.releaseYear ?: secondary.releaseYear,
        language = primary.language ?: secondary.language,
        genres = mergeBookLists(primary.genres, secondary.genres),
        creators = mergeBookLists(primary.creators, secondary.creators),
        credits = (primary.credits + secondary.credits).distinctBy { credit ->
            "${credit.roleType.name}:${credit.personName.trim().lowercase()}"
        },
        progressTotal = primary.progressTotal ?: secondary.progressTotal,
        coverUrl = primary.coverUrl ?: secondary.coverUrl,
        synopsis = primary.synopsis ?: secondary.synopsis,
        sourceUrl = primary.sourceUrl ?: secondary.sourceUrl,
        popularityScore = primary.popularityScore ?: secondary.popularityScore,
        rankingPosition = primary.rankingPosition ?: secondary.rankingPosition,
        rankingLabel = primary.rankingLabel ?: secondary.rankingLabel,
        ratingDistributionJson = primary.ratingDistributionJson ?: secondary.ratingDistributionJson,
        popularityJson = primary.popularityJson ?: secondary.popularityJson,
        rankingJson = primary.rankingJson ?: secondary.rankingJson,
        externalRating = primary.externalRating ?: secondary.externalRating,
        externalRatings = (primary.externalRatings + secondary.externalRatings)
            .distinctBy { it.source },
        bookEdition = mergedEdition,
        bookEditionSuggestions = (primary.bookEditionSuggestions + secondary.bookEditionSuggestions)
            .distinctBy { it.externalId },
        subtitle = primary.subtitle ?: secondary.subtitle,
        publishers = mergeBookLists(
            listOfNotNull(mergedEdition?.publisher) + primary.publishers,
            secondary.publishers,
        ),
        identifiers = mergeBookLists(
            listOfNotNull(exactIsbn) + primary.identifiers,
            secondary.identifiers,
            normalize = { value -> value.filter(Char::isLetterOrDigit).uppercase() },
        ),
    )
}

private fun mergeBookLists(
    primary: List<String>,
    secondary: List<String>,
    normalize: (String) -> String = { it.trim().lowercase() },
): List<String> = (primary + secondary)
    .filter { it.isNotBlank() }
    .distinctBy(normalize)

private data class ProviderSearchResult(
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val failedSources: Set<MetadataSource> = emptySet(),
    val failures: List<MetadataSearchFailure> = emptyList(),
)

private fun Throwable.toSearchFailure(source: MetadataSource): MetadataSearchFailure {
    val failure = toMetadataFailureDetails(source.name)
    return MetadataSearchFailure(
        source = source,
        diagnostic = failure.diagnostic,
        retryable = failure.retryable,
        statusCode = failure.statusCode,
        retryAfterMillis = failure.retryAfterMillis,
    )
}

private fun Result<*>.retryableExceptionOrNull(): Throwable? = exceptionOrNull()?.takeUnless { error ->
    error is MetadataProviderHttpException && error.statusCode == 404
}

private suspend fun <T> captureProviderResult(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
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
    return normalizedMetadataMatchText(removeParenthetical = true)
}
