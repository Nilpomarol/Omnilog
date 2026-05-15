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

class RawgMetadataRepository(
    private val apiKey: String,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (apiKey.isBlank() || MediaType.Game !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val response = getJson(
                "https://api.rawg.io/api/games?search=$encodedQuery&page_size=10&key=$apiKey",
            )
            val results = response.optJSONArray("results") ?: return@withContext emptyList()
            List(results.length()) { results.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion() }
        }
    }

    // Developers require a separate detail fetch; leaving creators empty until implemented.
    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion = suggestion

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val name = optString("name").takeIf { it.isNotBlank() } ?: return null
        val slug = optString("slug").takeIf { it.isNotBlank() }

        val coverUrl = optString("background_image").takeIf { it.isNotBlank() }

        val releaseYear = optString("released")
            .take(4)
            .toIntOrNull()
            ?.takeIf { it > 0 }

        val genres = optJSONArray("genres")?.let { arr ->
            List(arr.length()) { arr.getJSONObject(it).optString("name") }.filter { it.isNotBlank() }
        } ?: emptyList()

        val rating = optDouble("rating", 0.0)
        val ratingsCount = optInt("ratings_count", 0)
        val playtime = optInt("playtime", 0).takeIf { it > 0 }

        return MetadataSuggestion(
            source = MetadataSource.Rawg,
            externalId = id.toString(),
            mediaType = MediaType.Game,
            title = name,
            releaseYear = releaseYear,
            coverUrl = coverUrl,
            progressTotal = playtime,
            genres = genres,
            sourceUrl = slug?.let { "https://rawg.io/games/$it" },
            externalRating = if (rating > 0.0) {
                MetadataRatingSuggestion(
                    score = rating,
                    maxScore = 5.0,
                    voteCount = ratingsCount.takeIf { it > 0 },
                )
            } else {
                null
            },
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    }
}
