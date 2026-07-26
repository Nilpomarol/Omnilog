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
import com.nilpo.contenttracker.core.model.steamAppIdFromMetadataJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RawgMetadataRepository(
    private val apiKey: String,
    private val igdbCompanies: IgdbCompanyMetadataEnricher = IgdbCompanyMetadataEnricher("", ""),
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

        // Failures propagate: the caller reports them and offers a retry, rather than silently
        // handing back the un-enriched suggestion as if the details had loaded.
        return withContext(Dispatchers.IO) {
            getJson(
                "https://api.rawg.io/api/games/${suggestion.externalId}?key=$apiKey",
            ).toDetailedSuggestion(suggestion)
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
            progressTotal = null,
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

    private suspend fun JSONObject.toDetailedSuggestion(base: MetadataSuggestion): MetadataSuggestion = coroutineScope {
        val rawgTitle = optString("name").takeIf { it.isNotBlank() }
        val rawgCoverUrl = optString("background_image").takeIf { it.isNotBlank() }
        val rawgReleaseYear = optString("released")
            .take(4)
            .toIntOrNull()
            ?.takeIf { it > 0 }
        val rating = optDouble("rating", 0.0)
        val ratingsCount = optInt("ratings_count", 0)
        val rawgMetacritic = optInt("metacritic", 0).takeIf { it > 0 }
        val ratingsDistribution = optJSONArray("ratings")?.toString()
        val igdbCompanyCreditsDeferred = async {
            igdbCompanies.findCompanyCredits(
                title = rawgTitle ?: base.title,
                releaseYear = rawgReleaseYear ?: base.releaseYear,
            )
        }
        val steamAppId = base.storedSteamAppId() ?: fetchSteamAppId(base.externalId)
        val steamMetadataDeferred = async { steamAppId?.let(::fetchSteamMetadata) }
        val steamRatingDeferred = async { steamAppId?.let(::fetchSteamRating) }
        val steamMetadata = steamMetadataDeferred.await()
        val steamRating = steamRatingDeferred.await()
        val igdbCompanyCredits = igdbCompanyCreditsDeferred.await()
        val rawgRating = if (rating > 0.0) {
            MetadataRatingSuggestion(
                score = rating,
                maxScore = 5.0,
                voteCount = ratingsCount.takeIf { it > 0 },
            )
        } else {
            base.externalRatings
                .firstOrNull { it.source == ExternalRatingSource.Rawg }
                ?.toRatingSuggestion()
        }
        val metacritic = steamMetadata?.metacritic ?: rawgMetacritic
        val freshRatings = buildList {
            // Steam is the preferred game score, so keep it first. This also disambiguates
            // equal Steam/Metacritic percentages in legacy primary-rating matching.
            steamRating?.let(::add)
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

        val preferredRating = steamRating?.toRatingSuggestion() ?: metacritic?.let {
            MetadataRatingSuggestion(
                score = it.toDouble(),
                maxScore = 100.0,
            )
        } ?: rawgRating ?: base.externalRating
        val developerCredits = steamMetadata?.developers
            .orEmpty()
            .toCredits(MediaCreditRole.Developer, MetadataSource.Rawg)
            .ifEmpty {
                optJSONArray("developers").toCredits(MediaCreditRole.Developer, MetadataSource.Rawg)
            }

        base.copy(
            title = steamMetadata?.title ?: rawgTitle ?: base.title,
            releaseYear = steamMetadata?.releaseYear ?: rawgReleaseYear ?: base.releaseYear,
            coverUrl = steamMetadata?.portraitCoverUrl
                ?: rawgCoverUrl
                ?: steamMetadata?.headerImageUrl
                ?: base.coverUrl,
            synopsis = steamMetadata?.synopsis
                ?: optString("description_raw").takeIf { it.isNotBlank() }
                ?: base.synopsis,
            genres = steamMetadata?.genres.orEmpty()
                .ifEmpty { optJSONArray("genres").toStringList("name") }
                .ifEmpty { base.genres },
            creators = steamMetadata?.developers.orEmpty()
                .ifEmpty { optJSONArray("developers").toStringList("name") }
                .ifEmpty { base.creators },
            credits = mergeGameCompanyCredits(developerCredits, igdbCompanyCredits)
                .ifEmpty { base.credits },
            publishers = (steamMetadata?.publishers.orEmpty() +
                igdbCompanyCredits
                    .filter { it.roleType == MediaCreditRole.Publisher }
                    .map { it.personName })
                .distinctBy { it.trim().lowercase() },
            progressTotal = null,
            sourceUrl = steamAppId?.let { "https://store.steampowered.com/app/$it/" } ?: base.sourceUrl,
            popularityScore = steamMetadata?.recommendations?.toDouble()
                ?: optInt("added", 0).takeIf { it > 0 }?.toDouble()
                ?: base.popularityScore,
            ratingDistributionJson = ratingsDistribution ?: base.ratingDistributionJson,
            popularityJson = buildPopularityJson(
                steamAppId = steamAppId,
                steamMetadata = steamMetadata,
                rawgAdded = optInt("added", 0),
                rawgRatingsCount = ratingsCount,
                rawgSuggestionsCount = optInt("suggestions_count", 0),
            ),
            externalRating = preferredRating,
            externalRatings = mergeFreshExternalRatings(freshRatings, base.externalRatings),
        )
    }

    private fun fetchSteamAppId(rawgGameId: String): String? {
        return runCatching {
            val stores = getJson("https://api.rawg.io/api/games/$rawgGameId/stores?key=$apiKey")
                .optJSONArray("results")
                ?: return@runCatching null
            List(stores.length()) { stores.getJSONObject(it) }
                .firstNotNullOfOrNull { store ->
                    steamAppIdForStore(
                        storeId = store.optInt("store_id", 0),
                        nestedStoreId = store.optJSONObject("store")?.optInt("id", 0),
                        url = store.optString("url"),
                    )
                }
        }.getOrNull()
    }

    private fun fetchSteamMetadata(appId: String): SteamGameMetadata? {
        return runCatching {
            val response = getJson(
                "https://store.steampowered.com/api/appdetails?appids=$appId&l=english",
            ).optJSONObject(appId) ?: return@runCatching null
            if (!response.optBoolean("success", false)) return@runCatching null
            val data = response.optJSONObject("data") ?: return@runCatching null
            val portraitCoverUrl = steamPortraitCoverUrl(appId).takeIf { url ->
                runCatching { urlExists(url) }.getOrDefault(false)
            }
            data.toSteamGameMetadata(portraitCoverUrl)
        }.getOrNull()
    }

    private fun fetchSteamRating(appId: String): MetadataExternalRatingSuggestion? {
        return runCatching {
            val reviewSummary = getJson(
                "https://store.steampowered.com/appreviews/$appId?json=1&language=all&purchase_type=all&num_per_page=0",
            ).optJSONObject("query_summary") ?: return@runCatching null
            val positive = reviewSummary.optInt("total_positive", 0)
            val total = reviewSummary.optInt("total_reviews", 0)
            if (total <= 0) {
                null
            } else {
                MetadataExternalRatingSuggestion(
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
        }.getOrNull()
    }

    private fun urlExists(url: String): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "HEAD"
            connection.responseCode in 200..399
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    }
}

private data class SteamGameMetadata(
    val title: String?,
    val releaseYear: Int?,
    val portraitCoverUrl: String?,
    val headerImageUrl: String?,
    val synopsis: String?,
    val genres: List<String>,
    val developers: List<String>,
    val publishers: List<String>,
    val categories: List<String>,
    val platforms: List<String>,
    val supportedLanguages: String?,
    val controllerSupport: String?,
    val isFree: Boolean?,
    val recommendations: Int?,
    val metacritic: Int?,
)

private fun JSONObject.toSteamGameMetadata(portraitCoverUrl: String?): SteamGameMetadata {
    val platformObject = optJSONObject("platforms")
    return SteamGameMetadata(
        title = optString("name").takeIf { it.isNotBlank() },
        releaseYear = steamReleaseYear(optJSONObject("release_date")?.optString("date")),
        portraitCoverUrl = portraitCoverUrl,
        headerImageUrl = optString("header_image").takeIf { it.isNotBlank() },
        synopsis = optString("short_description").takeIf { it.isNotBlank() },
        genres = optJSONArray("genres").toStringList("description"),
        developers = optJSONArray("developers").toStringList(),
        publishers = optJSONArray("publishers").toStringList(),
        categories = optJSONArray("categories").toStringList("description"),
        platforms = listOf("windows", "mac", "linux")
            .filter { platform -> platformObject?.optBoolean(platform, false) == true },
        supportedLanguages = optString("supported_languages").takeIf { it.isNotBlank() },
        controllerSupport = optString("controller_support").takeIf { it.isNotBlank() },
        isFree = optBoolean("is_free").takeIf { has("is_free") },
        recommendations = optJSONObject("recommendations")
            ?.optInt("total", 0)
            ?.takeIf { it > 0 },
        metacritic = optJSONObject("metacritic")
            ?.optInt("score", 0)
            ?.takeIf { it > 0 },
    )
}

private fun buildPopularityJson(
    steamAppId: String?,
    steamMetadata: SteamGameMetadata?,
    rawgAdded: Int,
    rawgRatingsCount: Int,
    rawgSuggestionsCount: Int,
): String {
    return JSONObject()
        .put("added", rawgAdded)
        .put("ratingsCount", rawgRatingsCount)
        .put("suggestionsCount", rawgSuggestionsCount)
        .apply {
            steamAppId?.let { appId ->
                put(
                    "steam",
                    JSONObject()
                        .put("appId", appId)
                        .putNullable("publishers", steamMetadata?.publishers?.let(::JSONArray))
                        .putNullable("categories", steamMetadata?.categories?.let(::JSONArray))
                        .putNullable("platforms", steamMetadata?.platforms?.let(::JSONArray))
                        .putNullable("supportedLanguages", steamMetadata?.supportedLanguages)
                        .putNullable("controllerSupport", steamMetadata?.controllerSupport)
                        .putNullable("isFree", steamMetadata?.isFree)
                        .putNullable("recommendations", steamMetadata?.recommendations),
                )
            }
        }
        .toString()
}

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
    if (value != null) put(name, value)
    return this
}

private fun MetadataSuggestion.storedSteamAppId(): String? {
    return steamAppIdFromMetadataJson(popularityJson)
}

internal fun steamAppIdForStore(
    storeId: Int,
    nestedStoreId: Int?,
    url: String,
): String? {
    if (storeId != STEAM_STORE_ID && nestedStoreId != STEAM_STORE_ID) return null
    return Regex("""/app/(\d+)""").find(url)?.groupValues?.getOrNull(1)
}

internal fun steamPortraitCoverUrl(appId: String): String {
    return "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/library_600x900.jpg"
}

internal fun steamReleaseYear(date: String?): Int? {
    return Regex("""\b(?:19|20)\d{2}\b""")
        .find(date.orEmpty())
        ?.value
        ?.toIntOrNull()
}

internal fun steamReviewScoreDescriptor(description: String?, score: Int): String? {
    description?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    return when (score) {
        1 -> "Overwhelmingly Negative"
        2 -> "Very Negative"
        3 -> "Negative"
        4 -> "Mostly Negative"
        5 -> "Mixed"
        6 -> "Mostly Positive"
        7 -> "Positive"
        8 -> "Very Positive"
        9 -> "Overwhelmingly Positive"
        else -> null
    }
}

internal fun mergeFreshExternalRatings(
    fresh: List<MetadataExternalRatingSuggestion>,
    existing: List<MetadataExternalRatingSuggestion>,
): List<MetadataExternalRatingSuggestion> {
    return (fresh + existing).distinctBy { it.source }
}

private fun MetadataExternalRatingSuggestion.toRatingSuggestion(): MetadataRatingSuggestion {
    return MetadataRatingSuggestion(
        score = score,
        maxScore = maxScore,
        voteCount = voteCount,
    )
}

private fun List<String>.toCredits(
    roleType: MediaCreditRole,
    source: MetadataSource,
): List<MediaCredit> {
    return mapIndexed { index, name ->
        MediaCredit(
            personName = name,
            roleType = roleType,
            sortOrder = index,
            metadataSource = source,
        )
    }
}

private const val STEAM_STORE_ID = 1

private fun org.json.JSONArray?.toStringList(fieldName: String): List<String> {
    if (this == null) return emptyList()
    return List(length()) { getJSONObject(it) }
        .map { it.optString(fieldName) }
        .filter { it.isNotBlank() }
}

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
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
