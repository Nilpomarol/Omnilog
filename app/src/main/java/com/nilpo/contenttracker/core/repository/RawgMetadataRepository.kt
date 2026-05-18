package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
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
        val metacritic = optInt("metacritic", 0)
        val playtime = optInt("playtime", 0).takeIf { it > 0 }
        val rawgRating = if (rating > 0.0) {
            MetadataRatingSuggestion(
                score = rating,
                maxScore = 5.0,
                voteCount = ratingsCount.takeIf { it > 0 },
            )
        } else {
            null
        }

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
            externalRating = metacritic.takeIf { it > 0 }?.let {
                MetadataRatingSuggestion(
                    score = it.toDouble(),
                    maxScore = 100.0,
                )
            } ?: rawgRating,
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
                metacritic.takeIf { it > 0 }?.let { score ->
                    add(
                        MetadataExternalRatingSuggestion(
                            source = ExternalRatingSource.Metacritic,
                            score = score.toDouble(),
                            maxScore = 100.0,
                        ),
                    )
                }
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
        val metacritic = optInt("metacritic", 0)
        val playtime = optInt("playtime", 0).takeIf { it > 0 }
        val ratingsDistribution = optJSONArray("ratings")?.toString()
        val steamRating = fetchSteamRating(base.externalId)
        val rawgRating = if (rating > 0.0) {
            MetadataRatingSuggestion(
                score = rating,
                maxScore = 5.0,
                voteCount = ratingsCount.takeIf { it > 0 },
            )
        } else {
            base.externalRating
            }

        val preferredRating = metacritic.takeIf { it > 0 }?.let {
            MetadataRatingSuggestion(
                score = it.toDouble(),
                maxScore = 100.0,
            )
        } ?: rawgRating

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
            externalRating = preferredRating,
            externalRatings = (
                base.externalRatings +
                    buildList {
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
                        metacritic.takeIf { it > 0 }?.let { score ->
                            add(
                                MetadataExternalRatingSuggestion(
                                    source = ExternalRatingSource.Metacritic,
                                    score = score.toDouble(),
                                    maxScore = 100.0,
                                ),
                            )
                        }
                        steamRating?.let(::add)
                    }
                ).distinctBy { it.source },
        )
    }

    private fun fetchSteamRating(rawgGameId: String): MetadataExternalRatingSuggestion? {
        return runCatching {
            val stores = getJson("https://api.rawg.io/api/games/$rawgGameId/stores?key=$apiKey")
                .optJSONArray("results")
                ?: return@runCatching null
            val steamStore = List(stores.length()) { stores.getJSONObject(it) }
                .firstOrNull { store ->
                    store.optJSONObject("store")?.optInt("id", 0) == 1
                }
                ?: return@runCatching null
            val steamUrl = steamStore.optString("url").takeIf { it.isNotBlank() }
                ?: return@runCatching null
            val appId = Regex("""/app/(\d+)""").find(steamUrl)?.groupValues?.getOrNull(1)
                ?: return@runCatching null
            val reviewSummary = getJson(
                "https://store.steampowered.com/appreviews/$appId?json=1&language=all&purchase_type=all&num_per_page=0",
            ).optJSONObject("query_summary") ?: return@runCatching null
            val positive = reviewSummary.optInt("total_positive", 0)
            val total = reviewSummary.optInt("total_reviews", 0)
            if (positive <= 0 || total <= 0) {
                null
            } else {
                MetadataExternalRatingSuggestion(
                    source = ExternalRatingSource.Steam,
                    score = positive.toDouble() / total.toDouble() * 100.0,
                    maxScore = 100.0,
                    voteCount = total,
                )
            }
        }.getOrNull()
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
