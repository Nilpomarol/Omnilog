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

class GoogleBooksMetadataRepository(
    private val apiKey: String,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (apiKey.isBlank() || MediaType.Book !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val response = getJson(
                "https://www.googleapis.com/books/v1/volumes" +
                    "?q=$encodedQuery&maxResults=10&key=$apiKey",
            )
            val items = response.optJSONArray("items") ?: return@withContext emptyList()
            List(items.length()) { items.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion() }
        }
    }

    // Google Books search results already contain all needed fields.
    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion = suggestion

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val info = optJSONObject("volumeInfo") ?: return null
        val title = info.optString("title").takeIf { it.isNotBlank() } ?: return null

        val coverUrl = info.optJSONObject("imageLinks")
            ?.optString("thumbnail")
            ?.takeIf { it.isNotBlank() }
            ?.replace("http://", "https://")

        val releaseYear = info.optString("publishedDate")
            .take(4)
            .toIntOrNull()
            ?.takeIf { it > 0 }

        val categories = info.optJSONArray("categories")?.let { arr ->
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val averageRating = info.optDouble("averageRating", 0.0)
        val ratingsCount = info.optInt("ratingsCount", 0)

        return MetadataSuggestion(
            source = MetadataSource.GoogleBooks,
            externalId = id,
            mediaType = MediaType.Book,
            title = title,
            releaseYear = releaseYear,
            coverUrl = coverUrl,
            synopsis = info.optString("description").takeIf { it.isNotBlank() },
            progressTotal = info.optInt("pageCount", 0).takeIf { it > 0 },
            genres = categories,
            sourceUrl = info.optString("infoLink").takeIf { it.isNotBlank() },
            externalRating = if (averageRating > 0.0) {
                MetadataRatingSuggestion(
                    score = averageRating,
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
