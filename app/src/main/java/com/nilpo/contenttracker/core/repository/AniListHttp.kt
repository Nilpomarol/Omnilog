package com.nilpo.contenttracker.core.repository

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal fun postAniListGraphQL(query: String, variables: JSONObject): JSONObject {
    val connection = URL(AniListGraphQlUrl).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.outputStream.bufferedWriter().use {
            it.write(JSONObject().put("query", query).put("variables", variables).toString())
        }

        val statusCode = connection.responseCode
        if (statusCode !in 200..299) {
            val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw MetadataProviderHttpException(
                statusCode = statusCode,
                requestUrl = AniListGraphQlUrl,
                retryAfterMillis = retryAfterDelayMillis(connection.getHeaderField("Retry-After")),
                responseDetail = detail,
            )
        }
        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

private const val AniListGraphQlUrl = "https://graphql.anilist.co"
