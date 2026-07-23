package com.nilpo.contenttracker.core.repository

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class TmdbApiClient(
    private val credential: String,
) {
    fun getJson(url: String): JSONObject {
        val isAccessToken = credential.isTmdbAccessToken()
        val authenticatedUrl = if (isAccessToken) {
            url
        } else {
            val separator = if ('?' in url) "&" else "?"
            "$url${separator}api_key=${URLEncoder.encode(credential, "UTF-8")}"
        }
        val connection = URL(authenticatedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        if (isAccessToken) {
            connection.setRequestProperty("Authorization", "Bearer $credential")
        }

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw MetadataProviderHttpException(
                    statusCode = responseCode,
                    requestUrl = url,
                    retryAfterMillis = retryAfterDelayMillis(connection.getHeaderField("Retry-After")),
                    responseDetail = body,
                )
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}

private fun String.isTmdbAccessToken(): Boolean {
    return startsWith("eyJ") && count { it == '.' } == 2
}
