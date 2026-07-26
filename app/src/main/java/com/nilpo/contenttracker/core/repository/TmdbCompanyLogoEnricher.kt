package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Best-effort logo enrichment for anime studio credits.
 *
 * TMDB calls these production companies, so no company is added from TMDB. It can only decorate a
 * studio AniList already supplied, and only when their names match exactly after trimming/case
 * normalization. That keeps a related production committee member from being presented as the
 * animation studio.
 */
class TmdbCompanyLogoEnricher(
    private val apiKey: String,
) {
    private val tmdbApi = TmdbApiClient(apiKey)

    suspend fun enrichAnimeStudioCredits(
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        credits: List<MediaCredit>,
    ): List<MediaCredit> {
        if (apiKey.isBlank() || credits.none { it.roleType == MediaCreditRole.Studio }) return credits

        return try {
            withContext(Dispatchers.IO) {
                val candidates = searchCandidates(title, originalTitle, releaseYear)
                    .sortedByDescending { it.matchScore(title, originalTitle, releaseYear) }
                    .take(MaximumDetailLookups)
                val logos = candidates
                    .flatMap { candidate ->
                        tmdbApi.getJson(
                            "https://api.themoviedb.org/3/${candidate.endpoint}/${candidate.id}" +
                                "?language=en-US",
                        ).productionCompanyLogos()
                    }
                    .toMap()
                credits.withMatchingCompanyLogos(logos)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            credits
        }
    }

    private fun searchCandidates(
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
    ): List<TmdbCompanyCandidate> {
        val queries = listOf(title, originalTitle)
            .filterNotNull()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::tmdbCompanyKey)
        return queries.flatMap { query ->
            listOf("tv", "movie").flatMap { endpoint ->
                val yearParameter = when {
                    releaseYear == null -> ""
                    endpoint == "tv" -> "&first_air_date_year=$releaseYear"
                    else -> "&primary_release_year=$releaseYear"
                }
                val response = tmdbApi.getJson(
                    "https://api.themoviedb.org/3/search/$endpoint?query=" +
                        URLEncoder.encode(query, "UTF-8") +
                        "&include_adult=false&language=en-US$yearParameter",
                )
                response.optJSONArray("results").toCompanyCandidates(endpoint)
            }
        }.distinctBy { candidate -> "${candidate.endpoint}:${candidate.id}" }
    }
}

private data class TmdbCompanyCandidate(
    val id: Long,
    val endpoint: String,
    val title: String,
    val originalTitle: String?,
    val releaseYear: Int?,
) {
    fun matchScore(queryTitle: String, queryOriginalTitle: String?, queryYear: Int?): Int {
        val titles = listOf(title, originalTitle).filterNotNull().map(::tmdbCompanyKey)
        val requestedTitles = listOf(queryTitle, queryOriginalTitle)
            .filterNotNull()
            .map(::tmdbCompanyKey)
        var score = if (titles.any { it in requestedTitles }) 100 else 0
        if (queryYear != null && releaseYear != null && queryYear == releaseYear) score += 10
        return score
    }
}

private fun JSONArray?.toCompanyCandidates(endpoint: String): List<TmdbCompanyCandidate> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index) }
        .filterNotNull()
        .mapNotNull { result ->
            val id = result.optLong("id", 0L).takeIf { it > 0L } ?: return@mapNotNull null
            val title = if (endpoint == "tv") result.optString("name") else result.optString("title")
            if (title.isBlank()) return@mapNotNull null
            val originalTitle = if (endpoint == "tv") {
                result.optString("original_name").takeIf { it.isNotBlank() }
            } else {
                result.optString("original_title").takeIf { it.isNotBlank() }
            }
            val releaseYear = if (endpoint == "tv") {
                result.optString("first_air_date")
            } else {
                result.optString("release_date")
            }.take(4).toIntOrNull()
            TmdbCompanyCandidate(id, endpoint, title, originalTitle, releaseYear)
        }
}

/** Returns name-key to logo URL entries from a TMDB movie or TV detail response. */
internal fun JSONObject.productionCompanyLogos(): List<Pair<String, String>> {
    val companies = optJSONArray("production_companies") ?: return emptyList()
    return List(companies.length()) { index -> companies.optJSONObject(index) }
        .filterNotNull()
        .mapNotNull { company ->
            val name = company.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val logoPath = company.optString("logo_path").takeIf { it.startsWith("/") }
                ?: return@mapNotNull null
            tmdbCompanyKey(name) to tmdbCompanyLogoUrl(logoPath)
        }
}

internal fun List<MediaCredit>.withMatchingCompanyLogos(
    logosByCompany: Map<String, String>,
): List<MediaCredit> = map { credit ->
    if (credit.roleType == MediaCreditRole.Studio) {
        logosByCompany[tmdbCompanyKey(credit.personName)]
            ?.let { logoUrl -> credit.copy(personImageUrl = credit.personImageUrl ?: logoUrl) }
            ?: credit
    } else {
        credit
    }
}

internal fun tmdbCompanyLogoUrl(logoPath: String): String =
    "https://image.tmdb.org/t/p/w500$logoPath"

private fun tmdbCompanyKey(name: String): String = name.trim().lowercase()

private const val MaximumDetailLookups = 2
