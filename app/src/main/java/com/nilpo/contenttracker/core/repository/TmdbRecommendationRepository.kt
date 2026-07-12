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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TmdbRecommendationRepository(
    private val apiKey: String,
) : RecommendationProvider {
    private val tmdbApi = TmdbApiClient(apiKey)
    override suspend fun getRecommendations(item: MediaItem): List<MetadataSuggestion> {
        val externalId = item.metadataExternalId
            ?.substringBefore(":")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        if (apiKey.isBlank() || item.metadataSource != MetadataSource.Tmdb) return emptyList()

        val endpoint = when (item.type) {
            MediaType.Movie -> "movie"
            MediaType.TvShow -> "tv"
            else -> return emptyList()
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val response = tmdbApi.getJson(
                    "https://api.themoviedb.org/3/$endpoint/$externalId/recommendations" +
                        "?language=en-US&page=1",
                )
                val results = response.optJSONArray("results") ?: return@runCatching emptyList()
                List(results.length()) { index -> results.getJSONObject(index) }
                    .mapNotNull { result -> result.toMetadataSuggestion(item.type) }
            }.getOrDefault(emptyList())
        }
    }

    private fun JSONObject.toMetadataSuggestion(mediaType: MediaType): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val title = when (mediaType) {
            MediaType.Movie -> optString("title").takeIf { it.isNotBlank() }
            MediaType.TvShow -> optString("name").takeIf { it.isNotBlank() }
            else -> null
        } ?: return null
        val originalTitle = when (mediaType) {
            MediaType.Movie -> optString("original_title").takeIf { it.isNotBlank() && it != title }
            MediaType.TvShow -> optString("original_name").takeIf { it.isNotBlank() && it != title }
            else -> null
        }
        val date = when (mediaType) {
            MediaType.Movie -> optString("release_date")
            MediaType.TvShow -> optString("first_air_date")
            else -> ""
        }
        val posterPath = optString("poster_path").takeIf { it.isNotBlank() }
        val voteAverage = optDouble("vote_average", 0.0)
        val voteCount = optInt("vote_count", 0).takeIf { it > 0 }
        val externalRating = voteAverage.takeIf { it > 0.0 }?.let {
            MetadataRatingSuggestion(
                score = it,
                maxScore = 10.0,
                voteCount = voteCount,
            )
        }

        return MetadataSuggestion(
            source = MetadataSource.Tmdb,
            externalId = id.toString(),
            mediaType = mediaType,
            title = title,
            originalTitle = originalTitle,
            releaseYear = date.take(4).toIntOrNull(),
            coverUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            synopsis = optString("overview").takeIf { it.isNotBlank() },
            sourceUrl = when (mediaType) {
                MediaType.Movie -> "https://www.themoviedb.org/movie/$id"
                MediaType.TvShow -> "https://www.themoviedb.org/tv/$id"
                else -> null
            },
            popularityScore = voteCount?.toDouble(),
            externalRating = externalRating,
            externalRatings = externalRating?.let {
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.Tmdb,
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { reader -> JSONObject(reader.readText()) }
    }
}
