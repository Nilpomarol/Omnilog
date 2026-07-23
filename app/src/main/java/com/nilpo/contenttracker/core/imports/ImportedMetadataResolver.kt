package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRepository
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

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
        malId = if (source == MetadataSource.Jikan) externalId.toIntOrNull() else null,
        subtitle = subtitle,
        publishers = publishers,
        identifiers = identifiers,
    )
}

sealed interface ImportedResolution {
    data class Exact(val reference: ProviderReference) : ImportedResolution

    data class Candidates(val references: List<ProviderReference>) : ImportedResolution

    data object NoMatch : ImportedResolution

    data class Unavailable(val reason: String) : ImportedResolution

    data class Failed(val retryable: Boolean, val diagnostic: String) : ImportedResolution
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
                return ImportedResolution.Exact(exact.toReference("Exact ISBN $isbn"))
            }
        }
        return titleCandidates(input, setOf(MediaType.Book))
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
            .map { suggestion -> suggestion.toReference("Title candidate; review required") }
        return when {
            candidates.isNotEmpty() -> ImportedResolution.Candidates(candidates)
            search.failedSources.isNotEmpty() -> ImportedResolution.Failed(
                retryable = true,
                diagnostic = "Metadata search failed: ${search.failedSources.joinToString { it.name }}",
            )
            else -> ImportedResolution.NoMatch
        }
    }
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
                .put("ratingVoteCount", reference.ratingVoteCount),
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
    language = language,
    progressTotal = progressTotal,
    genres = genres,
    synopsis = synopsis,
    sourceUrl = sourceUrl,
    publishers = (publishers + listOfNotNull(bookEdition?.publisher)).distinct(),
    identifiers = (identifiers + listOfNotNull(bookEdition?.isbn)).distinct(),
    format = bookEdition?.format,
    ratingScore = externalRating?.score,
    ratingMaxScore = externalRating?.maxScore,
    ratingVoteCount = externalRating?.voteCount,
)

private fun JSONObject.nullableString(key: String): String? =
    optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return List(array.length()) { index -> array.optString(index) }
        .filter { it.isNotBlank() && it != "null" }
        .distinct()
}

private fun MetadataSuggestion.candidateScore(input: ImportedMetadataInput): Int {
    var score = 0
    if (title.normalizedTitle() == input.title.normalizedTitle()) score += 100
    if (originalTitle?.normalizedTitle() == input.originalTitle?.normalizedTitle() && originalTitle != null) score += 35
    if (releaseYear != null && releaseYear == input.releaseYear) score += 30
    if (coverUrl != null) score += 5
    return score
}

private fun String.normalizedTitle(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private val ImdbIdPattern = Regex("tt[0-9]{7,10}")
private const val MaxCandidates = 5
