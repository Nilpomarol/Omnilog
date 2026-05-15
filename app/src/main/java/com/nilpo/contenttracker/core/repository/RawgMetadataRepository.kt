package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
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

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (apiKey.isBlank() || suggestion.source != MetadataSource.Rawg) {
            return suggestion
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                getJson(
                    "https://api.rawg.io/api/games/${suggestion.externalId}?key=$apiKey",
                ).toDetailedSuggestion(suggestion)
            }.getOrElse { suggestion }
        }
    }

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

    private fun JSONObject.toDetailedSuggestion(base: MetadataSuggestion): MetadataSuggestion {
        val coverUrl = optString("background_image").takeIf { it.isNotBlank() }
        val releaseYear = optString("released")
            .take(4)
            .toIntOrNull()
            ?.takeIf { it > 0 }
        val rating = optDouble("rating", 0.0)
        val ratingsCount = optInt("ratings_count", 0)
        val playtime = optInt("playtime", 0).takeIf { it > 0 }
        val ratingsDistribution = optJSONArray("ratings")?.toString()

        return base.copy(
            releaseYear = releaseYear ?: base.releaseYear,
            coverUrl = coverUrl ?: base.coverUrl,
            synopsis = optString("description_raw").takeIf { it.isNotBlank() } ?: base.synopsis,
            genres = optJSONArray("genres").toStringList("name").ifEmpty { base.genres },
            creators = optJSONArray("developers").toStringList("name").ifEmpty { base.creators },
            credits = optJSONArray("developers").toCredits(MediaCreditRole.Developer, MetadataSource.Rawg)
                .ifEmpty { base.credits },
            progressTotal = playtime ?: base.progressTotal,
            popularityScore = optInt("added", 0).takeIf { it > 0 }?.toDouble() ?: base.popularityScore,
            ratingDistributionJson = ratingsDistribution ?: base.ratingDistributionJson,
            popularityJson = JSONObject()
                .put("added", optInt("added", 0))
                .put("ratingsCount", ratingsCount)
                .put("suggestionsCount", optInt("suggestions_count", 0))
                .toString(),
            externalRating = if (rating > 0.0) {
                MetadataRatingSuggestion(
                    score = rating,
                    maxScore = 5.0,
                    voteCount = ratingsCount.takeIf { it > 0 },
                )
            } else {
                base.externalRating
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

private fun org.json.JSONArray?.toStringList(fieldName: String): List<String> {
    if (this == null) return emptyList()
    return List(length()) { getJSONObject(it) }
        .map { it.optString(fieldName) }
        .filter { it.isNotBlank() }
}

private fun org.json.JSONArray?.toCredits(
    roleType: MediaCreditRole,
    source: MetadataSource,
): List<MediaCredit> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val obj = getJSONObject(index)
        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@List null
        MediaCredit(
            personName = name,
            roleType = roleType,
            sortOrder = index,
            metadataSource = source,
        )
    }.filterNotNull()
}
