package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.BookEditionMetadata
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataProviderHttpException
import com.nilpo.contenttracker.core.repository.MetadataSearchResult
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.io.IOException
import java.util.Locale

enum class ImportSource {
    MalApi,
    MalXml,
    ImdbCsv,
    StoryGraphCsv,
}

enum class ImportBatchState {
    Previewing,
    ReadyToImport,
    Importing,
    Enriching,
    Paused,
    Completed,
    CompletedWithIssues,
    Cancelled,
}

enum class ImportItemState {
    Pending,
    Staged,
    Resolving,
    NeedsReview,
    Applied,
    NoMatch,
    Unavailable,
    Failed,
    Skipped,
    Cancelled,
}

data class ImportedMetadataInput(
    val mediaItemId: Long,
    val importSource: ImportSource,
    val sourceExternalId: String?,
    val malId: Int?,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val creators: List<String> = emptyList(),
)

data class ProviderReference(
    val source: MetadataSource,
    val externalId: String,
    val mediaType: MediaType,
    val evidence: String,
    val title: String? = null,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val coverUrl: String? = null,
    val subtitle: String? = null,
    val creators: List<String> = emptyList(),
    val credits: List<MediaCredit> = emptyList(),
    val language: String? = null,
    val progressTotal: Int? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String? = null,
    val sourceUrl: String? = null,
    val publishers: List<String> = emptyList(),
    val identifiers: List<String> = emptyList(),
    val format: String? = null,
    val ratingScore: Double? = null,
    val ratingMaxScore: Double? = null,
    val ratingVoteCount: Int? = null,
    val externalRatings: List<MetadataExternalRatingSuggestion> = emptyList(),
) {
    fun toSuggestion(fallbackTitle: String): MetadataSuggestion = MetadataSuggestion(
        source = source,
        externalId = externalId,
        mediaType = mediaType,
        title = title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        language = language,
        genres = genres,
        creators = creators,
        credits = credits,
        progressTotal = progressTotal,
        coverUrl = coverUrl,
        synopsis = synopsis,
        sourceUrl = sourceUrl,
        externalRating = ratingScore?.let { score ->
            MetadataRatingSuggestion(
                score = score,
                maxScore = ratingMaxScore ?: 5.0,
                voteCount = ratingVoteCount,
            )
        },
        externalRatings = externalRatings,
        malId = if (source == MetadataSource.Jikan) externalId.toIntOrNull() else null,
        subtitle = subtitle,
        publishers = publishers,
        identifiers = identifiers,
        bookEdition = toBookEditionMetadata(),
    )

    private fun toBookEditionMetadata(): BookEditionMetadata? {
        if (mediaType != MediaType.Book) return null
        val isbn = identifiers.firstOrNull { identifier ->
            identifier.normalizeIsbn().isValidIsbn()
        }?.normalizeIsbn()
        val isEditionReference = externalId.startsWith("/books/") ||
            (source == MetadataSource.GoogleBooks && isbn != null) ||
            !format.isNullOrBlank()
        if (!isEditionReference) return null
        return BookEditionMetadata(
            externalId = externalId,
            title = title,
            releaseYear = releaseYear,
            language = language,
            pageCount = progressTotal,
            coverUrl = coverUrl,
            isbn = isbn,
            format = format,
            publisher = publishers.firstOrNull(),
            sourceUrl = sourceUrl,
        )
    }
}

sealed interface ImportedResolution {
    data class Exact(val reference: ProviderReference) : ImportedResolution

    data class Candidates(val references: List<ProviderReference>) : ImportedResolution

    data object NoMatch : ImportedResolution

    data class Unavailable(val reason: String) : ImportedResolution

    data class Failed(
        val retryable: Boolean,
        val diagnostic: String,
        val retryAfterMillis: Long? = null,
    ) : ImportedResolution
}

/** Resolves import provenance without allowing a title-only match to be applied automatically. */
class ImportedMetadataResolver(
    private val metadataRepository: MetadataRepository,
) {
    suspend fun resolve(input: ImportedMetadataInput): ImportedResolution {
        return try {
            when (input.importSource) {
                ImportSource.MalApi,
                ImportSource.MalXml -> resolveMal(input)
                ImportSource.ImdbCsv -> resolveImdb(input)
                ImportSource.StoryGraphCsv -> resolveStoryGraph(input)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: MetadataProviderHttpException) {
            error.toImportedResolution()
        } catch (error: IOException) {
            ImportedResolution.Failed(
                retryable = true,
                diagnostic = error.message ?: "Network request failed",
            )
        } catch (error: Throwable) {
            ImportedResolution.Failed(
                retryable = true,
                diagnostic = error.message ?: error::class.simpleName.orEmpty().ifBlank { "Provider request failed" },
            )
        }
    }

    private suspend fun resolveMal(input: ImportedMetadataInput): ImportedResolution {
        val malId = input.malId?.takeIf { it > 0 }
            ?: input.sourceExternalId?.toIntOrNull()?.takeIf { it > 0 }
        if (malId != null) {
            return ImportedResolution.Exact(
                ProviderReference(
                    source = MetadataSource.Jikan,
                    externalId = malId.toString(),
                    mediaType = MediaType.Anime,
                    evidence = "Exact MAL ID $malId",
                ),
            )
        }
        return titleCandidates(input, setOf(MediaType.Anime))
    }

    private suspend fun resolveImdb(input: ImportedMetadataInput): ImportedResolution {
        if (!metadataRepository.isImportedSourceAvailable(MetadataSource.Imdb)) {
            return ImportedResolution.Unavailable("TMDB_API_KEY is not configured")
        }
        val imdbId = input.sourceExternalId?.trim()?.lowercase()
            ?.takeIf { it.matches(ImdbIdPattern) }
        if (imdbId != null) {
            val exact = metadataRepository.resolveImportedExternalId(
                source = MetadataSource.Imdb,
                externalId = imdbId,
                mediaType = input.mediaType,
            )
            if (exact != null) {
                return ImportedResolution.Exact(exact.toReference("TMDB external-ID match for $imdbId"))
            }
        }
        return titleCandidates(input, setOf(input.mediaType))
    }

    private suspend fun resolveStoryGraph(input: ImportedMetadataInput): ImportedResolution {
        val isbn = input.sourceExternalId?.normalizeIsbn()?.takeIf { it.isValidIsbn() }
        if (isbn != null) {
            val exact = metadataRepository.resolveImportedExternalId(
                source = MetadataSource.StoryGraph,
                externalId = isbn,
                mediaType = MediaType.Book,
            )
            if (exact != null) {
                val assessment = exact.storyGraphCandidateAssessment(input, isbn)
                val requiresReview = assessment.authorConflict
                val reference = exact.toReference(
                    assessment.evidence(
                        reviewRequired = requiresReview,
                        exactIsbn = isbn,
                        forceExactEdition = true,
                    ),
                )
                return if (requiresReview) {
                    ImportedResolution.Candidates(listOf(reference))
                } else {
                    ImportedResolution.Exact(reference)
                }
            }
        }
        return storyGraphCandidates(input, isbn)
    }

    private suspend fun storyGraphCandidates(
        input: ImportedMetadataInput,
        isbn: String?,
    ): ImportedResolution {
        val search = metadataRepository.searchSuggestionsWithDiagnostics(
            MetadataSearchRequest(query = input.title, mediaTypes = setOf(MediaType.Book)),
        )
        val assessed = search.suggestions
            .asSequence()
            .filter { it.mediaType == MediaType.Book }
            .map { suggestion -> suggestion.storyGraphCandidateAssessment(input, isbn) }
            .filter(StoryGraphCandidateAssessment::isPlausible)
            .sortedByDescending(StoryGraphCandidateAssessment::score)
            .toList()

        // A search result can still prove an edition when a provider's direct ISBN endpoint has no
        // record. Title agreement protects against malformed third-party identifier data; a clear
        // author conflict always goes to review instead of being applied.
        val exactEdition = assessed.firstOrNull { candidate ->
            candidate.exactEditionIsbn &&
                candidate.titleMatch == BookTitleMatch.Exact &&
                !candidate.authorConflict
        }
        if (exactEdition != null) {
            return ImportedResolution.Exact(
                exactEdition.suggestion.toReference(
                    exactEdition.evidence(reviewRequired = false, exactIsbn = isbn),
                ),
            )
        }

        val candidates = assessed
            .distinctBy { candidate -> candidate.suggestion.storyGraphCandidateKey() }
            .take(MaxCandidates)
            .map { candidate ->
                candidate.suggestion.toReference(candidate.evidence(reviewRequired = true, exactIsbn = isbn))
            }
        return when {
            candidates.isNotEmpty() -> ImportedResolution.Candidates(candidates)
            search.failedSources.isNotEmpty() -> search.toFailureResolution()
            else -> ImportedResolution.NoMatch
        }
    }

    private suspend fun titleCandidates(
        input: ImportedMetadataInput,
        mediaTypes: Set<MediaType>,
    ): ImportedResolution {
        val search = metadataRepository.searchSuggestionsWithDiagnostics(
            MetadataSearchRequest(query = input.title, mediaTypes = mediaTypes),
        )
        val candidates = search.suggestions
            .filter { it.mediaType in mediaTypes }
            .sortedByDescending { suggestion -> suggestion.candidateScore(input) }
            .take(MaxCandidates)
            .map { suggestion -> suggestion.toReference("Coincidència per títol; cal revisar-la") }
        return when {
            candidates.isNotEmpty() -> ImportedResolution.Candidates(candidates)
            search.failedSources.isNotEmpty() -> search.toFailureResolution()
            else -> ImportedResolution.NoMatch
        }
    }
}

private fun MetadataProviderHttpException.toImportedResolution(): ImportedResolution = when (statusCode) {
    401, 403 -> ImportedResolution.Unavailable("Provider authorization failed (HTTP $statusCode)")
    404 -> ImportedResolution.NoMatch
    429 -> ImportedResolution.Failed(
        retryable = true,
        diagnostic = "Provider rate limit (HTTP 429)",
        retryAfterMillis = retryAfterMillis,
    )
    else -> ImportedResolution.Failed(
        retryable = statusCode >= 500,
        diagnostic = "Provider request failed (HTTP $statusCode)",
        retryAfterMillis = retryAfterMillis,
    )
}

private fun MetadataSearchResult.toFailureResolution(): ImportedResolution {
    val retryableFailures = failures.filter { it.retryable }
    if (retryableFailures.isNotEmpty()) {
        return ImportedResolution.Failed(
            retryable = true,
            diagnostic = retryableFailures.joinToString(", ") { it.diagnostic },
            retryAfterMillis = retryableFailures.mapNotNull { it.retryAfterMillis }.maxOrNull(),
        )
    }
    val authorizationFailure = failures.firstOrNull { it.statusCode == 401 || it.statusCode == 403 }
    if (authorizationFailure != null) {
        return ImportedResolution.Unavailable(authorizationFailure.diagnostic)
    }
    return ImportedResolution.Failed(
        retryable = failures.isEmpty(),
        diagnostic = failures.joinToString(", ") { it.diagnostic }
            .ifBlank { "Metadata search failed: ${failedSources.joinToString { it.name }}" },
    )
}

internal fun List<ProviderReference>.toJson(): String = JSONArray().apply {
    forEach { reference ->
        put(
            JSONObject()
                .put("source", reference.source.name)
                .put("externalId", reference.externalId)
                .put("mediaType", reference.mediaType.name)
                .put("evidence", reference.evidence)
                .put("title", reference.title)
                .put("originalTitle", reference.originalTitle)
                .put("releaseYear", reference.releaseYear)
                .put("coverUrl", reference.coverUrl)
                .put("subtitle", reference.subtitle)
                .put("creators", JSONArray(reference.creators))
                .put("credits", JSONArray().apply {
                    reference.credits.forEach { credit ->
                        put(
                            JSONObject()
                                .put("personName", credit.personName)
                                .put("roleType", credit.roleType.name)
                                .put("characterName", credit.characterName)
                                .put("sortOrder", credit.sortOrder)
                                .put("metadataSource", credit.metadataSource?.name),
                        )
                    }
                })
                .put("language", reference.language)
                .put("progressTotal", reference.progressTotal)
                .put("genres", JSONArray(reference.genres))
                .put("synopsis", reference.synopsis)
                .put("sourceUrl", reference.sourceUrl)
                .put("publishers", JSONArray(reference.publishers))
                .put("identifiers", JSONArray(reference.identifiers))
                .put("format", reference.format)
                .put("ratingScore", reference.ratingScore)
                .put("ratingMaxScore", reference.ratingMaxScore)
                .put("ratingVoteCount", reference.ratingVoteCount)
                .put("externalRatings", JSONArray().apply {
                    reference.externalRatings.forEach { rating ->
                        put(
                            JSONObject()
                                .put("source", rating.source.name)
                                .put("score", rating.score)
                                .put("maxScore", rating.maxScore)
                                .put("voteCount", rating.voteCount)
                                .put("scoreDescriptor", rating.scoreDescriptor),
                        )
                    }
                }),
        )
    }
}.toString()

internal fun String?.toProviderReferences(): List<ProviderReference> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val value = array.optJSONObject(index) ?: continue
                val source = runCatching { MetadataSource.valueOf(value.getString("source")) }.getOrNull()
                    ?: continue
                val mediaType = runCatching { MediaType.valueOf(value.getString("mediaType")) }.getOrNull()
                    ?: continue
                val externalId = value.optString("externalId").takeIf { it.isNotBlank() } ?: continue
                add(
                    ProviderReference(
                        source = source,
                        externalId = externalId,
                        mediaType = mediaType,
                        evidence = value.optString("evidence"),
                        title = value.optString("title").takeIf { it.isNotBlank() && it != "null" },
                        originalTitle = value.optString("originalTitle").takeIf { it.isNotBlank() && it != "null" },
                        releaseYear = value.optInt("releaseYear").takeIf { it > 0 },
                        coverUrl = value.optString("coverUrl").takeIf { it.isNotBlank() && it != "null" },
                        subtitle = value.nullableString("subtitle"),
                        creators = value.stringList("creators"),
                        credits = value.creditList("credits"),
                        language = value.nullableString("language"),
                        progressTotal = value.optInt("progressTotal").takeIf { it > 0 },
                        genres = value.stringList("genres"),
                        synopsis = value.nullableString("synopsis"),
                        sourceUrl = value.nullableString("sourceUrl"),
                        publishers = value.stringList("publishers"),
                        identifiers = value.stringList("identifiers"),
                        format = value.nullableString("format"),
                        ratingScore = value.optDouble("ratingScore").takeIf { it > 0.0 },
                        ratingMaxScore = value.optDouble("ratingMaxScore").takeIf { it > 0.0 },
                        ratingVoteCount = value.optInt("ratingVoteCount").takeIf { it > 0 },
                        externalRatings = value.externalRatingList("externalRatings"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}

internal fun String.normalizeIsbn(): String = filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()

internal fun String.isValidIsbn(): Boolean = when (length) {
    10 -> {
        if (!take(9).all(Char::isDigit) || !(last().isDigit() || last() == 'X')) {
            false
        } else {
            mapIndexed { index, char ->
                val value = if (char == 'X') 10 else char.digitToInt()
                (10 - index) * value
            }.sum() % 11 == 0
        }
    }
    13 -> all(Char::isDigit) && mapIndexed { index, char ->
        char.digitToInt() * if (index % 2 == 0) 1 else 3
    }.sum() % 10 == 0
    else -> false
}

private fun MetadataSuggestion.toReference(evidence: String): ProviderReference = ProviderReference(
    source = source,
    externalId = externalId,
    mediaType = mediaType,
    evidence = evidence,
    title = title,
    originalTitle = originalTitle,
    releaseYear = releaseYear,
    coverUrl = coverUrl,
    subtitle = subtitle,
    creators = creators,
    credits = credits,
    language = language,
    progressTotal = progressTotal,
    genres = genres,
    synopsis = synopsis,
    sourceUrl = sourceUrl,
    publishers = (publishers + listOfNotNull(bookEdition?.publisher)).distinct(),
    identifiers = (listOfNotNull(bookEdition?.isbn) + identifiers)
        .distinctBy { identifier -> identifier.normalizeIsbn().ifBlank { identifier.trim().lowercase() } },
    format = bookEdition?.format,
    ratingScore = externalRating?.score,
    ratingMaxScore = externalRating?.maxScore,
    ratingVoteCount = externalRating?.voteCount,
    externalRatings = externalRatings,
)

private fun JSONObject.nullableString(key: String): String? =
    optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return List(array.length()) { index -> array.optString(index) }
        .filter { it.isNotBlank() && it != "null" }
        .distinct()
}

private fun JSONObject.creditList(key: String): List<MediaCredit> {
    val array = optJSONArray(key) ?: return emptyList()
    return List(array.length()) { index -> array.optJSONObject(index) }
        .mapNotNull { value ->
            value ?: return@mapNotNull null
            val personName = value.optString("personName").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val role = runCatching { MediaCreditRole.valueOf(value.optString("roleType")) }.getOrNull()
                ?: return@mapNotNull null
            MediaCredit(
                personName = personName,
                roleType = role,
                characterName = value.nullableString("characterName"),
                sortOrder = value.optInt("sortOrder"),
                metadataSource = value.nullableString("metadataSource")?.let { source ->
                    runCatching { MetadataSource.valueOf(source) }.getOrNull()
                },
            )
        }
}

private fun JSONObject.externalRatingList(key: String): List<MetadataExternalRatingSuggestion> {
    val array = optJSONArray(key) ?: return emptyList()
    return List(array.length()) { index -> array.optJSONObject(index) }
        .mapNotNull { value ->
            value ?: return@mapNotNull null
            val source = runCatching {
                ExternalRatingSource.valueOf(value.optString("source"))
            }.getOrNull() ?: return@mapNotNull null
            val score = value.optDouble("score").takeIf { it > 0.0 } ?: return@mapNotNull null
            val maxScore = value.optDouble("maxScore").takeIf { it > 0.0 } ?: return@mapNotNull null
            MetadataExternalRatingSuggestion(
                source = source,
                score = score,
                maxScore = maxScore,
                voteCount = value.optInt("voteCount").takeIf { it > 0 },
                scoreDescriptor = value.nullableString("scoreDescriptor"),
            )
        }
}

private fun MetadataSuggestion.candidateScore(input: ImportedMetadataInput): Int {
    var score = 0
    if (title.normalizedTitle() == input.title.normalizedTitle()) score += 100
    if (originalTitle?.normalizedTitle() == input.originalTitle?.normalizedTitle() && originalTitle != null) score += 35
    if (releaseYear != null && releaseYear == input.releaseYear) score += 30
    if (coverUrl != null) score += 5
    return score
}

private data class StoryGraphCandidateAssessment(
    val suggestion: MetadataSuggestion,
    val titleMatch: BookTitleMatch,
    val authorMatch: Boolean,
    val authorConflict: Boolean,
    val isbnMatch: Boolean,
    val exactEditionIsbn: Boolean,
    val score: Int,
) {
    fun isPlausible(): Boolean = when {
        exactEditionIsbn || isbnMatch -> true
        authorConflict -> false
        else -> titleMatch != BookTitleMatch.None
    }

    fun evidence(
        reviewRequired: Boolean,
        exactIsbn: String?,
        forceExactEdition: Boolean = false,
    ): String = buildList {
        when {
            (exactEditionIsbn || forceExactEdition) && exactIsbn != null ->
                add("ISBN exacte de l'edició: $exactIsbn")
            isbnMatch && exactIsbn != null -> add("ISBN present a l'obra: $exactIsbn")
        }
        when (titleMatch) {
            BookTitleMatch.Exact -> add("títol exacte")
            BookTitleMatch.Similar -> add("títol similar")
            BookTitleMatch.None -> Unit
        }
        when {
            authorMatch -> add("autoria coincident")
            authorConflict -> add("autoria diferent")
            suggestion.creators.isEmpty() -> add("autoria no disponible")
        }
        if (reviewRequired) add("cal revisar-la")
    }.joinToString(" · ")
}

private enum class BookTitleMatch { None, Similar, Exact }

private fun MetadataSuggestion.storyGraphCandidateAssessment(
    input: ImportedMetadataInput,
    isbn: String?,
): StoryGraphCandidateAssessment {
    val titleMatch = storyGraphTitleMatch(input.title)
    val authorMatch = input.creators.hasMatchingPerson(creators)
    val authorConflict = input.creators.isNotEmpty() && creators.isNotEmpty() && !authorMatch
    val normalizedIdentifiers = (identifiers + listOfNotNull(bookEdition?.isbn))
        .map(String::normalizeIsbn)
        .filter(String::isValidIsbn)
        .toSet()
    val isbnMatch = isbn != null && isbn in normalizedIdentifiers
    val exactEditionIsbn = isbn != null && bookEdition?.isbn
        ?.normalizeIsbn()
        ?.takeIf(String::isValidIsbn) == isbn

    var score = when (titleMatch) {
        BookTitleMatch.Exact -> 220
        BookTitleMatch.Similar -> 100
        BookTitleMatch.None -> 0
    }
    if (exactEditionIsbn) score += 600
    else if (isbnMatch) score += 280
    if (authorMatch) score += 180
    if (authorConflict) score -= 350
    if (releaseYear != null && releaseYear == input.releaseYear) score += 30
    if (progressTotal != null) score += 12
    if (coverUrl != null) score += 8
    if (synopsis != null) score += 5

    return StoryGraphCandidateAssessment(
        suggestion = this,
        titleMatch = titleMatch,
        authorMatch = authorMatch,
        authorConflict = authorConflict,
        isbnMatch = isbnMatch,
        exactEditionIsbn = exactEditionIsbn,
        score = score,
    )
}

private fun MetadataSuggestion.storyGraphTitleMatch(importedTitle: String): BookTitleMatch {
    val imported = importedTitle.normalizedBookEvidence()
    if (imported.isBlank()) return BookTitleMatch.None
    val candidates = buildList {
        add(title)
        subtitle?.takeIf { it.isNotBlank() }?.let { add("$title $it") }
    }.map(String::normalizedBookEvidence)

    if (candidates.any { it == imported }) return BookTitleMatch.Exact
    val importedTokens = imported.split(' ').filter(String::isNotBlank).toSet()
    val similar = candidates.any { candidate ->
        if (
            minOf(candidate.length, imported.length) >= 6 &&
            (candidate.contains(imported) || imported.contains(candidate))
        ) {
            true
        } else {
            val candidateTokens = candidate.split(' ').filter(String::isNotBlank).toSet()
            val union = importedTokens union candidateTokens
            union.isNotEmpty() && (importedTokens intersect candidateTokens).size.toDouble() / union.size >= 0.60
        }
    }
    return if (similar) BookTitleMatch.Similar else BookTitleMatch.None
}

private fun List<String>.hasMatchingPerson(other: List<String>): Boolean =
    any { imported -> other.any { candidate -> imported.samePersonAs(candidate) } }

private fun String.samePersonAs(other: String): Boolean {
    val first = normalizedBookEvidence().split(' ').filter(String::isNotBlank).toSet()
    val second = other.normalizedBookEvidence().split(' ').filter(String::isNotBlank).toSet()
    if (first.isEmpty() || second.isEmpty()) return false
    if (first == second) return true
    val common = (first intersect second).size
    return common >= 2 && common.toDouble() / minOf(first.size, second.size) >= 0.67
}

private fun MetadataSuggestion.storyGraphCandidateKey(): String {
    val editionIsbn = bookEdition?.isbn
        ?.normalizeIsbn()
        ?.takeIf(String::isValidIsbn)
    return editionIsbn?.let { "isbn:$it" } ?: "${source.name}:$externalId"
}

private fun String.normalizedBookEvidence(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("""\([^)]*\)"""), " ")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private fun String.normalizedTitle(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private val ImdbIdPattern = Regex("tt[0-9]{7,10}")
private const val MaxCandidates = 5
