package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RawgRecommendationRepository(
    private val apiKey: String,
) : RecommendationProvider {
    override suspend fun getRecommendations(item: MediaItem): List<MetadataSuggestion> {
        val externalId = item.metadataExternalId?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (apiKey.isBlank() || item.type != MediaType.Game || item.metadataSource != MetadataSource.Rawg) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val details = runCatching {
                getJson("https://api.rawg.io/api/games/$externalId?key=$apiKey")
            }.getOrNull() ?: return@withContext emptyList()

            val developerRecommendations = fetchByFilter(
                filterName = "developers",
                filterIds = details.optJSONArray("developers").toIdList(),
                currentExternalId = externalId,
            )
            val developerIds = developerRecommendations.map { suggestion -> suggestion.externalId }.toSet()

            val genreRecommendations = details
                .optJSONArray("genres")
                .toIdList()
                .take(MAX_GENRE_IDS)
                .flatMap { genreId ->
                    fetchByFilter(
                        filterName = "genres",
                        filterIds = listOf(genreId),
                        currentExternalId = externalId,
                    )
                }
                .groupBy { suggestion -> suggestion.externalId }
                .map { (_, matches) ->
                    GenreRecommendationCandidate(
                        suggestion = matches.first(),
                        sharedGenreCount = matches.size,
                    )
                }
                .filterNot { candidate -> candidate.suggestion.externalId in developerIds }
                .sortedWith(
                    compareByDescending<GenreRecommendationCandidate> { it.sharedGenreCount }
                        .thenByDescending { it.suggestion.externalRating?.score ?: 0.0 }
                        .thenByDescending { it.suggestion.popularityScore ?: 0.0 }
                        .thenBy { it.suggestion.title.lowercase() },
                )
                .map { candidate -> candidate.suggestion }

            (developerRecommendations + genreRecommendations)
                .distinctBy { suggestion -> suggestion.externalId }
                .take(MAX_RECOMMENDATIONS)
        }
    }

    private fun fetchByFilter(
        filterName: String,
        filterIds: List<Long>,
        currentExternalId: String,
    ): List<MetadataSuggestion> {
        if (filterIds.isEmpty()) return emptyList()

        val response = runCatching {
            getJson(
                "https://api.rawg.io/api/games" +
                    "?$filterName=" + filterIds.take(MAX_FILTER_IDS).joinToString(",") +
                    "&ordering=-rating&page_size=$PAGE_SIZE&key=$apiKey",
            )
        }.getOrNull() ?: return emptyList()

        val results = response.optJSONArray("results") ?: return emptyList()
        return List(results.length()) { index -> results.getJSONObject(index) }
            .mapNotNull { result -> result.toMetadataSuggestion() }
            .filter { suggestion -> suggestion.externalId != currentExternalId }
    }

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val title = optString("name").takeIf { it.isNotBlank() } ?: return null
        val slug = optString("slug").takeIf { it.isNotBlank() }
        val rawgRating = optDouble("rating", 0.0).takeIf { it > 0.0 }?.let {
            MetadataRatingSuggestion(
                score = it,
                maxScore = 5.0,
                voteCount = optInt("ratings_count", 0).takeIf { count -> count > 0 },
            )
        }
        val metacritic = optInt("metacritic", 0).takeIf { it > 0 }
        val preferredRating = metacritic?.let {
            MetadataRatingSuggestion(score = it.toDouble(), maxScore = 100.0)
        } ?: rawgRating
        val genres = optJSONArray("genres")?.let { array ->
            List(array.length()) { index -> array.optJSONObject(index)?.optString("name").orEmpty() }
                .filter { it.isNotBlank() }
        }.orEmpty()

        return MetadataSuggestion(
            source = MetadataSource.Rawg,
            externalId = id.toString(),
            mediaType = MediaType.Game,
            title = title,
            releaseYear = optString("released").take(4).toIntOrNull()?.takeIf { it > 0 },
            coverUrl = optString("background_image").takeIf { it.isNotBlank() },
            genres = genres,
            sourceUrl = slug?.let { "https://rawg.io/games/$it" }
                ?: "https://rawg.io/games/$id",
            popularityScore = optInt("added", 0).takeIf { it > 0 }?.toDouble(),
            externalRating = preferredRating,
            externalRatings = buildList {
                rawgRating?.let {
                    add(
                        MetadataExternalRatingSuggestion(
                            source = ExternalRatingSource.Rawg,
                            score = it.score,
                            maxScore = it.maxScore,
                            voteCount = it.voteCount,
                        ),
                    )
                }
                metacritic?.let {
                    add(
                        MetadataExternalRatingSuggestion(
                            source = ExternalRatingSource.Metacritic,
                            score = it.toDouble(),
                            maxScore = 100.0,
                        ),
                    )
                }
            },
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { reader -> JSONObject(reader.readText()) }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_FILTER_IDS = 3
        const val MAX_GENRE_IDS = 5
        const val MAX_RECOMMENDATIONS = 8
    }
}

private data class GenreRecommendationCandidate(
    val suggestion: MetadataSuggestion,
    val sharedGenreCount: Int,
)

private fun JSONArray?.toIdList(): List<Long> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index)?.optLong("id", 0L) ?: 0L }
        .filter { it > 0L }
}