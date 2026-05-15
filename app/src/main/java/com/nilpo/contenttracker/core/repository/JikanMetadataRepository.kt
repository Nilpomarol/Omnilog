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

class JikanMetadataRepository : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Anime !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            runCatching {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val response = getJson("https://api.jikan.moe/v4/anime?q=$encodedQuery&limit=10&sfw=true")
                val data = response.optJSONArray("data") ?: return@runCatching emptyList()
                List(data.length()) { data.getJSONObject(it) }
                    .mapNotNull { it.toMetadataSuggestion() }
            }.getOrElse { emptyList() }
        }
    }

    // Jikan search results already contain all needed fields (episodes, synopsis, cover, genres, score),
    // so no separate detail fetch is required.
    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion = suggestion

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optLong("mal_id", 0L).takeIf { it > 0L } ?: return null
        val titleEnglish = optString("title_english").takeIf { it.isNotBlank() }
        val titleRomaji = optString("title").takeIf { it.isNotBlank() } ?: return null
        val title = titleEnglish ?: titleRomaji
        val originalTitle = if (titleEnglish != null && titleRomaji != titleEnglish) titleRomaji else null

        val coverUrl = optJSONObject("images")
            ?.optJSONObject("jpg")
            ?.optString("large_image_url")
            ?.takeIf { it.isNotBlank() }

        val score = optDouble("score", 0.0)
        val year = optInt("year", 0).takeIf { it > 0 }
            ?: optJSONObject("aired")
                ?.optJSONObject("prop")
                ?.optJSONObject("from")
                ?.optInt("year", 0)
                ?.takeIf { it > 0 }

        val genres = optJSONArray("genres")?.let { arr ->
            List(arr.length()) { arr.getJSONObject(it).optString("name") }.filter { it.isNotBlank() }
        } ?: emptyList()

        return MetadataSuggestion(
            source = MetadataSource.Jikan,
            externalId = id.toString(),
            mediaType = MediaType.Anime,
            title = title,
            originalTitle = originalTitle,
            releaseYear = year,
            coverUrl = coverUrl,
            synopsis = optString("synopsis").takeIf { it.isNotBlank() },
            progressTotal = optInt("episodes", 0).takeIf { it > 0 },
            genres = genres,
            sourceUrl = optString("url").takeIf { it.isNotBlank() },
            externalRating = if (score > 0.0) {
                MetadataRatingSuggestion(
                    score = score,
                    maxScore = 10.0,
                    voteCount = optInt("scored_by", 0).takeIf { it > 0 },
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
