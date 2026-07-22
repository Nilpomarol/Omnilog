package com.nilpo.contenttracker.core.mal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MalAccount(val name: String)

data class MalApiResponse(
    val statusCode: Int,
    val json: JSONObject,
)

class MalApiException(
    val statusCode: Int,
    message: String,
) : Exception(message) {
    val isRetryable: Boolean
        get() = statusCode == 408 || statusCode == 429 || statusCode >= 500
}

class MalApiClient(
    private val clientId: String,
    private val redirectUri: String,
) {
    suspend fun exchangeAuthorizationCode(code: String, verifier: String): MalTokens = withContext(Dispatchers.IO) {
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

    suspend fun refreshTokens(tokens: MalTokens): MalTokens = withContext(Dispatchers.IO) {
        tokenRequest(
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to tokens.refreshToken,
            ),
            fallbackRefreshToken = tokens.refreshToken,
        ).copy(accountName = tokens.accountName)
    }

    suspend fun getCurrentAccount(accessToken: String): MalAccount = withContext(Dispatchers.IO) {
        val json = requestJson(
            url = "$ApiBase/users/@me",
            method = "GET",
            accessToken = accessToken,
        ).json
        MalAccount(name = json.getString("name"))
    }

    suspend fun updateAnimeList(accessToken: String, malId: Int, payload: MalSyncPayload): Int =
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
    ): MalApiResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = NetworkTimeoutMillis
        connection.readTimeout = NetworkTimeoutMillis
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        if (accessToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }
        if (form != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(form.toFormBody())
            }
        }

        val statusCode = connection.responseCode
        val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (statusCode !in 200..299) {
            val remoteMessage = runCatching { JSONObject(body).optString("message") }.getOrNull()
                ?.takeIf { it.isNotBlank() }
            throw MalApiException(statusCode, remoteMessage ?: "MyAnimeList request failed ($statusCode)")
        }
        return MalApiResponse(
            statusCode = statusCode,
            json = if (body.isBlank()) JSONObject() else JSONObject(body),
        )
    }
}

internal fun Map<String, String>.toFormBody(): String = entries.joinToString("&") { (key, value) ->
    "${key.urlEncode()}=${value.urlEncode()}"
}

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private const val ApiBase = "https://api.myanimelist.net/v2"
private const val TokenUrl = "https://myanimelist.net/v1/oauth2/token"
private const val NetworkTimeoutMillis = 15_000
