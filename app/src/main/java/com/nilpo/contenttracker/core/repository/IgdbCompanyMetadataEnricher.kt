package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MetadataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Optional IGDB lookup used to add a supplied company logo beside game developers and publishers.
 *
 * RAWG remains the game's primary provider. IGDB is deliberately best-effort: a missing key,
 * expired Twitch token, or an ambiguous game match never prevents normal game metadata from loading.
 */
class IgdbCompanyMetadataEnricher(
    private val clientId: String,
    private val clientSecret: String,
) {
    private var cachedToken: IgdbAccessToken? = null

    private val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    suspend fun findCompanyCredits(title: String, releaseYear: Int?): List<MediaCredit> {
        if (!isConfigured || title.isBlank()) return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                val token = accessToken()
                val query = """
                    search "${title.escapeIgdbQueryText()}";
                    fields name,first_release_date,involved_companies.developer,
                        involved_companies.publisher,involved_companies.company.name,
                        involved_companies.company.logo.url,involved_companies.company.logo.width,
                        involved_companies.company.logo.height;
                    limit 5;
                """.trimIndent()
                val games = postIgdbQuery(query, token.value)
                games.bestIgdbGame(title, releaseYear)?.toCompanyCredits().orEmpty()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun accessToken(): IgdbAccessToken {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { it.expiresAtMillis > now + TokenRefreshBufferMillis }?.let { return it }

        val form = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "client_credentials",
        ).entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        val response = JSONObject(
            postJson(
                url = TwitchTokenUrl,
                body = form,
                contentType = "application/x-www-form-urlencoded",
            ),
        )
        val token = response.optString("access_token").takeIf { it.isNotBlank() }
            ?: error("IGDB token response did not include an access token")
        val expiresInSeconds = response.optLong("expires_in", DefaultTokenLifetimeSeconds)
            .coerceAtLeast(60L)
        return IgdbAccessToken(
            value = token,
            expiresAtMillis = now + expiresInSeconds * 1_000L,
        ).also { cachedToken = it }
    }

    private fun postIgdbQuery(query: String, token: String): JSONArray {
        return postJson(
            url = IgdbGamesUrl,
            body = query,
            contentType = "text/plain",
            headers = mapOf(
                "Client-ID" to clientId,
                "Authorization" to "Bearer $token",
            ),
        ).let(::JSONArray)
    }
}

private data class IgdbAccessToken(
    val value: String,
    val expiresAtMillis: Long,
)

private fun JSONArray.bestIgdbGame(title: String, releaseYear: Int?): JSONObject? {
    val normalizedTitle = title.normalizedIgdbTitle()
    return List(length()) { index -> optJSONObject(index) }
        .filterNotNull()
        .maxByOrNull { candidate ->
            val candidateTitle = candidate.optString("name").normalizedIgdbTitle()
            var score = when {
                candidateTitle == normalizedTitle -> 100
                candidateTitle.contains(normalizedTitle) || normalizedTitle.contains(candidateTitle) -> 50
                else -> 0
            }
            val candidateYear = candidate.optLong("first_release_date", 0L)
                .takeIf { it > 0L }
                ?.let { epochSeconds -> java.time.Instant.ofEpochSecond(epochSeconds).atZone(java.time.ZoneOffset.UTC).year }
            if (releaseYear != null && candidateYear != null) {
                score += (10 - kotlin.math.abs(releaseYear - candidateYear) * 3).coerceAtLeast(0)
            }
            score
        }
        ?.takeIf { candidate ->
            val candidateTitle = candidate.optString("name").normalizedIgdbTitle()
            candidateTitle == normalizedTitle ||
                candidateTitle.contains(normalizedTitle) || normalizedTitle.contains(candidateTitle)
        }
}

internal fun JSONObject.toCompanyCredits(): List<MediaCredit> {
    val involvedCompanies = optJSONArray("involved_companies") ?: return emptyList()
    return List(involvedCompanies.length()) { index ->
        val involved = involvedCompanies.optJSONObject(index) ?: return@List emptyList()
        val company = involved.optJSONObject("company") ?: return@List emptyList()
        val name = company.optString("name").takeIf { it.isNotBlank() } ?: return@List emptyList()
        val logo = company.optJSONObject("logo")?.toCompanyLogo()
        buildList {
            if (involved.optBoolean("developer", false)) {
                add(companyCredit(name, MediaCreditRole.Developer, logo))
            }
            if (involved.optBoolean("publisher", false)) {
                add(companyCredit(name, MediaCreditRole.Publisher, logo))
            }
        }
    }.flatten().distinctBy { credit ->
        "${credit.roleType}:${credit.personName.trim().lowercase()}"
    }
}

internal fun mergeGameCompanyCredits(
    primary: List<MediaCredit>,
    igdb: List<MediaCredit>,
): List<MediaCredit> {
    val igdbByRoleAndName = igdb.associateBy { credit ->
        "${credit.roleType}:${credit.personName.trim().lowercase()}"
    }
    val mergedPrimary = primary.map { credit ->
        igdbByRoleAndName["${credit.roleType}:${credit.personName.trim().lowercase()}"]
            ?.let { match ->
                credit.copy(
                    personImageUrl = match.personImageUrl ?: credit.personImageUrl,
                    personImageAspectRatio = match.personImageAspectRatio ?: credit.personImageAspectRatio,
                )
            }
            ?: credit
    }
    val existingKeys = mergedPrimary.mapTo(mutableSetOf()) { credit ->
        "${credit.roleType}:${credit.personName.trim().lowercase()}"
    }
    return mergedPrimary + igdb
        .filter { credit -> existingKeys.add("${credit.roleType}:${credit.personName.trim().lowercase()}") }
        .mapIndexed { index, credit -> credit.copy(sortOrder = mergedPrimary.size + index) }
}

private fun companyCredit(
    name: String,
    role: MediaCreditRole,
    logo: CompanyLogo?,
): MediaCredit = MediaCredit(
    personName = name,
    roleType = role,
    personImageUrl = logo?.url,
    personImageAspectRatio = logo?.aspectRatio,
    metadataSource = MetadataSource.Rawg,
)

/**
 * IGDB's resized logo deliveries can crop artwork. Retain the dimensions reported with the
 * original company-logo record for layout, and request IGDB's uncropped original asset.
 */
private data class CompanyLogo(
    val url: String,
    val aspectRatio: Float?,
)

private fun JSONObject.toCompanyLogo(): CompanyLogo? {
    val url = optString("url").toAbsoluteIgdbImageUrl() ?: return null
    return CompanyLogo(
        url = url,
        aspectRatio = providerLogoAspectRatio(optInt("width"), optInt("height")),
    )
}

internal fun providerLogoAspectRatio(width: Int, height: Int): Float? =
    if (width > 0 && height > 0) width.toFloat() / height else null

internal fun String.toAbsoluteIgdbImageUrl(): String? {
    return trim().takeIf { it.isNotBlank() }?.let { url ->
        when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            else -> url
        }
    }?.withIgdbLogoSize()
}

/**
 * IGDB returns the `t_thumb` URL on company-logo records. Its resized logo transformations may
 * crop the source artwork, so the size path is substituted for the uncropped original asset.
 */
internal fun String.withIgdbLogoSize(): String = replace("/$IgdbThumbSize/", "/$IgdbLogoSize/")

private fun String.normalizedIgdbTitle(): String =
    lowercase().filter(Char::isLetterOrDigit)

private fun String.escapeIgdbQueryText(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

private fun postJson(
    url: String,
    body: String,
    contentType: String,
    headers: Map<String, String> = emptyMap(),
): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "$contentType; charset=utf-8")
        connection.setRequestProperty("User-Agent", "Omnilog/1.0 (Android)")
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        connection.outputStream.bufferedWriter().use { it.write(body) }
        val statusCode = connection.responseCode
        if (statusCode !in 200..299) {
            val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw MetadataProviderHttpException(
                statusCode = statusCode,
                requestUrl = url,
                retryAfterMillis = retryAfterDelayMillis(connection.getHeaderField("Retry-After")),
                responseDetail = detail,
            )
        }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

internal const val IgdbThumbSize = "t_thumb"
internal const val IgdbLogoSize = "t_original"

private const val TwitchTokenUrl = "https://id.twitch.tv/oauth2/token"
private const val IgdbGamesUrl = "https://api.igdb.com/v4/games"
private const val DefaultTokenLifetimeSeconds = 3_600L
private const val TokenRefreshBufferMillis = 60_000L
