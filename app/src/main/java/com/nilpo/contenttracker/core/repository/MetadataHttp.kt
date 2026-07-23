package com.nilpo.contenttracker.core.repository

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Shared JSON GET behavior so metadata providers report HTTP failures consistently. */
internal fun getJson(url: String): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Omnilog/1.0 (Android)")
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
        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

internal fun retryAfterDelayMillis(
    headerValue: String?,
    nowEpochMillis: Long = System.currentTimeMillis(),
): Long? {
    val value = headerValue?.trim()?.takeIf { it.isNotBlank() } ?: return null
    value.toLongOrNull()?.let { seconds ->
        return seconds.coerceAtLeast(1L)
            .coerceAtMost(MaxRetryAfterSeconds)
            .times(1_000L)
    }
    return runCatching {
        val retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli()
        (retryAt - nowEpochMillis).coerceAtLeast(1_000L).coerceAtMost(MaxRetryAfterMillis)
    }.getOrNull()
}

private const val MaxRetryAfterSeconds = 24 * 60 * 60L
private const val MaxRetryAfterMillis = MaxRetryAfterSeconds * 1_000L
