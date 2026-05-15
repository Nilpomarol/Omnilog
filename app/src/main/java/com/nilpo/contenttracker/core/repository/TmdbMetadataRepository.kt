package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TmdbMetadataRepository(
    private val apiKey: String,
) : MetadataRepository {
    override suspend fun searchSuggestions(
        request: MetadataSearchRequest,
    ): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (apiKey.isBlank() || query.isBlank()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            buildList {
                if (MediaType.Movie in request.mediaTypes) {
                    addAll(searchEndpoint(query = query, endpoint = "movie", mediaType = MediaType.Movie))
                }
                if (MediaType.TvShow in request.mediaTypes) {
                    addAll(searchEndpoint(query = query, endpoint = "tv", mediaType = MediaType.TvShow))
                }
            }.sortedByDescending { it.externalRating?.score ?: 0.0 }
        }
    }

    override suspend fun getSuggestionDetails(
        suggestion: MetadataSuggestion,
    ): MetadataSuggestion {
        if (apiKey.isBlank() || suggestion.source != MetadataSource.Tmdb) {
            return suggestion
        }

        return withContext(Dispatchers.IO) {
            val endpoint = when (suggestion.mediaType) {
                MediaType.Movie -> "movie"
                MediaType.TvShow -> "tv"
                else -> return@withContext suggestion
            }
            runCatching {
                getJson(
                    "https://api.themoviedb.org/3/$endpoint/${suggestion.externalId}" +
                        "?api_key=$apiKey&language=en-US",
                ).toDetailedSuggestion(suggestion)
            }.getOrElse { suggestion }
        }
    }

    private fun searchEndpoint(
        query: String,
        endpoint: String,
        mediaType: MediaType,
    ): List<MetadataSuggestion> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            "https://api.themoviedb.org/3/search/$endpoint" +
                "?api_key=$apiKey&query=$encodedQuery&include_adult=false&language=en-US",
        )
        val results = response.optJSONArray("results") ?: return emptyList()

        return List(results.length()) { index -> results.getJSONObject(index) }
            .mapNotNull { result -> result.toMetadataSuggestion(mediaType) }
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
            externalRating = if (voteAverage > 0.0) {
                MetadataRatingSuggestion(
                    score = voteAverage,
                    maxScore = 10.0,
                    voteCount = optInt("vote_count", 0).takeIf { it > 0 },
                )
            } else {
                null
            },
        )
    }

    private fun JSONObject.toDetailedSuggestion(base: MetadataSuggestion): MetadataSuggestion {
        val posterPath = optString("poster_path").takeIf { it.isNotBlank() }
        val collectionTitle = optJSONObject("belongs_to_collection")
            ?.optString("name")
            ?.takeIf { it.isNotBlank() }
        val progressTotal = when (base.mediaType) {
            MediaType.Movie -> optInt("runtime", 0).takeIf { it > 0 }
            MediaType.TvShow -> optInt("number_of_episodes", 0).takeIf { it > 0 }
            else -> base.progressTotal
        }

        return base.copy(
            collectionTitle = collectionTitle ?: base.collectionTitle,
            genres = optJSONArray("genres").toStringList("name"),
            progressTotal = progressTotal ?: base.progressTotal,
            coverUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: base.coverUrl,
            synopsis = optString("overview").takeIf { it.isNotBlank() } ?: base.synopsis,
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"

        return connection.inputStream.bufferedReader().use { reader ->
            JSONObject(reader.readText())
        }
    }
}

private fun org.json.JSONArray?.toStringList(fieldName: String): List<String> {
    if (this == null) {
        return emptyList()
    }

    return List(length()) { index ->
        getJSONObject(index).optString(fieldName)
    }.filter { it.isNotBlank() }
}
