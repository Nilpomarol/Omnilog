package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

data class MalAccount(val name: String)

data class MalApiResponse(
    val statusCode: Int,
    val json: JSONObject,
)

data class MalAnimeListPage(
    val items: List<MyAnimeListImportItem>,
    val nextPageUrl: String?,
)

class MalApiException(
    val statusCode: Int,
    message: String,
) : Exception(message) {
    val isRetryable: Boolean
        get() = statusCode == 408 || statusCode == 429 || statusCode >= 500
}

interface MalApiService {
    suspend fun exchangeAuthorizationCode(code: String, verifier: String): MalTokens

    suspend fun refreshTokens(tokens: MalTokens): MalTokens

    suspend fun getCurrentAccount(accessToken: String): MalAccount

    suspend fun getAnimeListPage(accessToken: String, nextPageUrl: String? = null): MalAnimeListPage

    suspend fun updateAnimeList(accessToken: String, malId: Int, payload: MalSyncPayload): Int
}

class MalApiClient(
    private val clientId: String,
    private val redirectUri: String,
) : MalApiService {
    override suspend fun exchangeAuthorizationCode(code: String, verifier: String): MalTokens = withContext(Dispatchers.IO) {
        tokenRequest(
            mapOf(
                "client_id" to clientId,
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to redirectUri,
            ),
        )
    }

    override suspend fun refreshTokens(tokens: MalTokens): MalTokens = withContext(Dispatchers.IO) {
        tokenRequest(
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to tokens.refreshToken,
            ),
            fallbackRefreshToken = tokens.refreshToken,
        ).copy(accountName = tokens.accountName)
    }

    override suspend fun getCurrentAccount(accessToken: String): MalAccount = withContext(Dispatchers.IO) {
        val json = requestJson(
            url = "$ApiBase/users/@me",
            method = "GET",
            accessToken = accessToken,
        ).json
        MalAccount(name = json.getString("name"))
    }

    override suspend fun getAnimeListPage(
        accessToken: String,
        nextPageUrl: String?,
    ): MalAnimeListPage = withContext(Dispatchers.IO) {
        val url = nextPageUrl?.validatedMalContinuationUrl() ?: buildString {
            append("$ApiBase/users/@me/animelist")
            append("?fields=")
            append(AnimeListFields.urlEncode())
            append("&limit=$AnimeListPageSize&sort=list_updated_at")
        }
        requestJson(
            url = url,
            method = "GET",
            accessToken = accessToken,
        ).json.toMalAnimeListPage()
    }

    override suspend fun updateAnimeList(accessToken: String, malId: Int, payload: MalSyncPayload): Int =
        withContext(Dispatchers.IO) {
            requestJson(
                url = "$ApiBase/anime/$malId/my_list_status",
                method = "PUT",
                accessToken = accessToken,
                form = payload.toFormFields(),
            ).statusCode
        }

    private fun tokenRequest(
        form: Map<String, String>,
        fallbackRefreshToken: String? = null,
    ): MalTokens {
        val json = requestJson(
            url = TokenUrl,
            method = "POST",
            form = form,
        ).json
        val expiresInSeconds = json.optLong("expires_in", 0L).coerceAtLeast(60L)
        return MalTokens(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                ?: checkNotNull(fallbackRefreshToken),
            expiresAtEpochMillis = System.currentTimeMillis() + expiresInSeconds * 1_000L,
        )
    }

    private fun requestJson(
        url: String,
        method: String,
        accessToken: String? = null,
        form: Map<String, String>? = null,
        redirectCount: Int = 0,
    ): MalApiResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = NetworkTimeoutMillis
        connection.readTimeout = NetworkTimeoutMillis
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        if (accessToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }
        if (URL(url).host == ApiHost) {
            connection.setRequestProperty("X-MAL-CLIENT-ID", clientId)
        }
        if (form != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(form.toFormBody())
            }
        }

        val statusCode = connection.responseCode
        if (statusCode == 307 || statusCode == 308) {
            val location = connection.getHeaderField("Location")
            if (!location.isNullOrBlank() && redirectCount < MaxRedirects) {
                val redirectedUrl = URL(URL(url), location)
                val isTrustedRedirect = redirectedUrl.protocol == "https" &&
                    redirectedUrl.host in TrustedMalHosts
                if (isTrustedRedirect) {
                    connection.disconnect()
                    return requestJson(
                        url = redirectedUrl.toString(),
                        method = method,
                        accessToken = accessToken,
                        form = form,
                        redirectCount = redirectCount + 1,
                    )
                }
            }
        }
        val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (statusCode !in 200..299) {
            val remoteMessage = runCatching { JSONObject(body).optString("message") }.getOrNull()
                ?.takeIf { it.isNotBlank() }
            val location = connection.getHeaderField("Location")?.take(MaxRedirectLocationLength)
            val detail = remoteMessage ?: "MyAnimeList request failed ($statusCode)"
            throw MalApiException(
                statusCode,
                if (location == null) detail else "$detail; redirect=$location",
            )
        }
        return MalApiResponse(
            statusCode = statusCode,
            json = if (body.isBlank()) JSONObject() else JSONObject(body),
        )
    }
}

internal fun JSONObject.toMalAnimeListPage(): MalAnimeListPage {
    val data = optJSONArray("data") ?: JSONArray()
    val items = List(data.length()) { index -> data.optJSONObject(index) }
        .mapNotNull { entry -> entry?.toMyAnimeListImportItem() }
    val nextPageUrl = optJSONObject("paging")
        ?.optString("next")
        ?.takeIf { it.isNotBlank() }
        ?.validatedMalContinuationUrl()
    return MalAnimeListPage(items = items, nextPageUrl = nextPageUrl)
}

private fun JSONObject.toMyAnimeListImportItem(): MyAnimeListImportItem? {
    val node = optJSONObject("node") ?: return null
    val listStatus = optJSONObject("list_status") ?: return null
    val malId = node.optInt("id", 0).takeIf { it > 0 } ?: return null
    val title = node.optString("title").takeIf { it.isNotBlank() } ?: return null
    return MyAnimeListImportItem(
        malId = malId,
        title = title,
        seriesType = node.optString("media_type").toMalSeriesType(),
        episodeTotal = node.optInt("num_episodes", 0).takeIf { it > 0 },
        watchedEpisodes = listStatus.optInt("num_episodes_watched", 0).coerceAtLeast(0),
        startedAt = listStatus.optString("start_date").toMalLocalDateOrNull(),
        finishedAt = listStatus.optString("finish_date").toMalLocalDateOrNull(),
        rating = listStatus.optInt("score", 0).takeIf { it > 0 }?.coerceIn(1, 10),
        status = listStatus.optString("status").toMalTrackingStatus(),
        notes = listStatus.optString("comments").takeIf { it.isNotBlank() },
        tags = listStatus.optJSONArray("tags").toStringList(),
    )
}

private fun String.toMalSeriesType(): String? = when (lowercase()) {
    "tv" -> "TV"
    "movie" -> "Movie"
    "ova" -> "OVA"
    "ona" -> "ONA"
    "special" -> "Special"
    "music" -> "Music"
    else -> takeIf { it.isNotBlank() }
}

private fun String.toMalTrackingStatus(): TrackingStatus = when (lowercase()) {
    "completed" -> TrackingStatus.Completed
    "watching" -> TrackingStatus.InProgress
    "on_hold" -> TrackingStatus.Paused
    "dropped" -> TrackingStatus.Dropped
    else -> TrackingStatus.Planned
}

private fun String.toMalLocalDateOrNull(): LocalDate? =
    takeIf { it.isNotBlank() }?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun String.validatedMalContinuationUrl(): String {
    val parsed = URL(this)
    require(parsed.protocol == "https" && parsed.host == ApiHost) {
        "MyAnimeList returned an untrusted pagination URL"
    }
    return parsed.toString()
}

internal fun Map<String, String>.toFormBody(): String = entries.joinToString("&") { (key, value) ->
    "${key.urlEncode()}=${value.urlEncode()}"
}

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private const val ApiBase = "https://api.myanimelist.net/v2"
private const val ApiHost = "api.myanimelist.net"
private const val TokenUrl = "https://myanimelist.net/v1/oauth2/token"
private const val NetworkTimeoutMillis = 15_000
private const val MaxRedirects = 3
private const val MaxRedirectLocationLength = 160
private const val AnimeListPageSize = 100
private const val AnimeListFields = "list_status,num_episodes,media_type"
private val TrustedMalHosts = setOf("api.myanimelist.net", "myanimelist.net")
