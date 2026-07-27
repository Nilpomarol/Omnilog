package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.normalizeSteamAppId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Best-effort metadata provider for games available on Steam but missing from RAWG.
 *
 * Steam's storefront search is public but not part of its documented Web API, so callers use
 * this only as a fallback and surface any provider failures through the normal diagnostics.
 */
class SteamMetadataRepository : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Game !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            steamAppIdFromSearchQuery(query)?.let { appId ->
                return@withContext listOfNotNull(fetchSuggestionByAppId(appId))
            }
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val results = getJson(
                "https://store.steampowered.com/api/storesearch/?term=$encodedQuery&l=english&cc=us",
            ).optJSONArray("items") ?: return@withContext emptyList()
            List(results.length()) { results.optJSONObject(it) }
                .mapNotNull(::steamSearchSuggestion)
                .take(MAX_SEARCH_RESULTS)
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (suggestion.source != MetadataSource.Steam) return suggestion
        val appId = normalizeSteamAppId(suggestion.externalId) ?: return suggestion

        return withContext(Dispatchers.IO) {
            coroutineScope {
                val detailsDeferred = async {
                    getJson(
                        "https://store.steampowered.com/api/appdetails?appids=$appId&l=english&cc=us",
                    )
                }
                val ratingDeferred = async { fetchSteamRating(appId) }
                val response = detailsDeferred.await().optJSONObject(appId) ?: return@coroutineScope suggestion
                if (!response.optBoolean("success", false)) return@coroutineScope suggestion
                val data = response.optJSONObject("data") ?: return@coroutineScope suggestion
                data.toSteamSuggestion(
                    base = suggestion,
                    appId = appId,
                    steamRating = ratingDeferred.await(),
                )
            }
        }
    }

    private fun fetchSteamRating(appId: String): MetadataExternalRatingSuggestion? {
        val reviewSummary = getJson(
            "https://store.steampowered.com/appreviews/$appId?json=1&language=all&purchase_type=all&num_per_page=0",
        ).optJSONObject("query_summary") ?: return null
        val positive = reviewSummary.optInt("total_positive", 0)
        val total = reviewSummary.optInt("total_reviews", 0)
        if (total <= 0) return null
        return MetadataExternalRatingSuggestion(
            source = ExternalRatingSource.Steam,
            score = positive.toDouble() / total.toDouble() * 100.0,
            maxScore = 100.0,
            voteCount = total,
            scoreDescriptor = steamReviewScoreDescriptor(
                description = reviewSummary.optString("review_score_desc"),
                score = reviewSummary.optInt("review_score", 0),
            ),
        )
    }

    private fun fetchSuggestionByAppId(appId: String): MetadataSuggestion? {
        val response = getJson(
            "https://store.steampowered.com/api/appdetails?appids=$appId&l=english&cc=us",
        ).optJSONObject(appId) ?: return null
        if (!response.optBoolean("success", false)) return null
        val data = response.optJSONObject("data") ?: return null
        if (!data.optString("type").equals("game", ignoreCase = true)) return null
        return data.toSteamSuggestion(
            base = MetadataSuggestion(
                source = MetadataSource.Steam,
                externalId = appId,
                mediaType = MediaType.Game,
                title = "Steam app $appId",
                sourceUrl = steamStoreUrl(appId),
                popularityJson = steamPopularityJson(appId),
            ),
            appId = appId,
            steamRating = null,
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
            connection.readTimeout = REQUEST_TIMEOUT_MILLIS
            connection.requestMethod = "GET"
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (statusCode !in 200..299) {
                throw MetadataProviderHttpException(
                    statusCode = statusCode,
                    requestUrl = url,
                    responseDetail = responseText,
                )
            }
            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }
}

internal fun steamSearchSuggestion(item: JSONObject?): MetadataSuggestion? {
    item ?: return null
    val appId = normalizeSteamAppId(item.optLong("id", 0L).takeIf { it > 0L }?.toString()) ?: return null
    val title = item.optString("name").trim().takeIf { it.isNotBlank() } ?: return null
    // Store search also returns bundles and packages; those do not map to a single game entry.
    if (item.optString("type").equals("game", ignoreCase = true).not()) return null
    return MetadataSuggestion(
        source = MetadataSource.Steam,
        externalId = appId,
        mediaType = MediaType.Game,
        title = title,
        coverUrl = item.optString("tiny_image").takeIf { it.isNotBlank() },
        sourceUrl = steamStoreUrl(appId),
        popularityJson = steamPopularityJson(appId),
    )
}

/** Accepts an intentional Steam App ID lookup without treating ordinary numeric titles as IDs. */
internal fun steamAppIdFromSearchQuery(query: String): String? {
    val match = SteamAppIdSearchPattern.matchEntire(query.trim()) ?: return null
    return normalizeSteamAppId(match.groupValues[1])
}

private fun JSONObject.toSteamSuggestion(
    base: MetadataSuggestion,
    appId: String,
    steamRating: MetadataExternalRatingSuggestion?,
): MetadataSuggestion {
    val developers = optJSONArray("developers").toStringList()
    val publishers = optJSONArray("publishers").toStringList()
    val metacritic = optJSONObject("metacritic")
        ?.optInt("score", 0)
        ?.takeIf { it > 0 }
    val ratings = buildList {
        steamRating?.let(::add)
        metacritic?.let { score ->
            add(
                MetadataExternalRatingSuggestion(
                    source = ExternalRatingSource.Metacritic,
                    score = score.toDouble(),
                    maxScore = 100.0,
                ),
            )
        }
    }
    val primaryRating = steamRating?.toRatingSuggestion() ?: metacritic?.let { score ->
        MetadataRatingSuggestion(score = score.toDouble(), maxScore = 100.0)
    }

    return base.copy(
        title = optString("name").takeIf { it.isNotBlank() } ?: base.title,
        releaseYear = steamReleaseYear(optJSONObject("release_date")?.optString("date")) ?: base.releaseYear,
        coverUrl = optString("header_image").takeIf { it.isNotBlank() } ?: base.coverUrl,
        synopsis = optString("short_description").toSteamPlainText() ?: base.synopsis,
        genres = optJSONArray("genres").toStringList("description"),
        creators = developers,
        credits = developers.toSteamCredits(MediaCreditRole.Developer) +
            publishers.toSteamCredits(MediaCreditRole.Publisher, startOrder = developers.size),
        publishers = publishers,
        sourceUrl = steamStoreUrl(appId),
        popularityScore = optJSONObject("recommendations")
            ?.optInt("total", 0)
            ?.takeIf { it > 0 }
            ?.toDouble(),
        popularityJson = steamPopularityJson(appId),
        externalRating = primaryRating,
        externalRatings = mergeFreshExternalRatings(ratings, base.externalRatings),
    )
}

private fun steamStoreUrl(appId: String): String = "https://store.steampowered.com/app/$appId/"

private fun steamPopularityJson(appId: String): String = JSONObject()
    .put("steam", JSONObject().put("appId", appId))
    .toString()

private fun MetadataExternalRatingSuggestion.toRatingSuggestion(): MetadataRatingSuggestion {
    return MetadataRatingSuggestion(score = score, maxScore = maxScore, voteCount = voteCount)
}

private fun JSONArray?.toStringList(fieldName: String? = null): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        if (fieldName == null) optString(index) else optJSONObject(index)?.optString(fieldName)
    }.mapNotNull { it?.takeIf(String::isNotBlank) }
}

private fun List<String>.toSteamCredits(
    role: MediaCreditRole,
    startOrder: Int = 0,
): List<MediaCredit> = mapIndexed { index, name ->
    MediaCredit(
        personName = name,
        roleType = role,
        sortOrder = startOrder + index,
        metadataSource = MetadataSource.Steam,
    )
}

private fun String.toSteamPlainText(): String? = replace(Regex("<[^>]*>"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .takeIf { it.isNotBlank() }

private const val MAX_SEARCH_RESULTS = 10
private const val REQUEST_TIMEOUT_MILLIS = 10_000
private val SteamAppIdSearchPattern = Regex("""steam(?:\s+id)?\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
