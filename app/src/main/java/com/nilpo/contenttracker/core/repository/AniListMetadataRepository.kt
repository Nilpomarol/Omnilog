package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AniListMetadataRepository : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Anime !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val response = postGraphQL(
                SEARCH_QUERY,
                JSONObject().apply {
                    put("search", query)
                    put("perPage", 10)
                },
            )
            val mediaArray = response
                .getJSONObject("data")
                .getJSONObject("Page")
                .getJSONArray("media")
            List(mediaArray.length()) { mediaArray.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion() }
        }
    }

    // AniList search already returns all needed fields; no separate detail fetch required.
    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion = suggestion

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val titleObj = optJSONObject("title") ?: return null
        val titleEnglish = titleObj.optString("english").takeIf { it.isNotBlank() }
        val titleRomaji = titleObj.optString("romaji").takeIf { it.isNotBlank() } ?: return null
        val title = titleEnglish ?: titleRomaji
        val originalTitle = if (titleEnglish != null && titleRomaji != titleEnglish) titleRomaji else null

        val coverUrl = optJSONObject("coverImage")?.optString("large")?.takeIf { it.isNotBlank() }
        val synopsis = optString("description")
            .takeIf { it.isNotBlank() }
            ?.replace(Regex("<[^>]+>"), " ")
            ?.trim()
        val averageScore = optInt("averageScore", 0)
        val popularity = optInt("popularity", 0)
        val releaseYear = optJSONObject("startDate")?.optInt("year", 0)?.takeIf { it > 0 }
        val genres = optJSONArray("genres")?.let { arr ->
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        return MetadataSuggestion(
            source = MetadataSource.AniList,
            externalId = id.toString(),
            mediaType = MediaType.Anime,
            title = title,
            originalTitle = originalTitle,
            releaseYear = releaseYear,
            coverUrl = coverUrl,
            synopsis = synopsis,
            progressTotal = optInt("episodes", 0).takeIf { it > 0 },
            genres = genres,
            sourceUrl = optString("siteUrl").takeIf { it.isNotBlank() },
            externalRating = if (averageScore > 0) {
                MetadataRatingSuggestion(
                    score = averageScore / 10.0,
                    maxScore = 10.0,
                    voteCount = popularity.takeIf { it > 0 },
                )
            } else {
                null
            },
        )
    }

    private fun postGraphQL(query: String, variables: JSONObject): JSONObject {
        val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val body = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }
        connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
        return JSONObject(connection.inputStream.bufferedReader().readText())
    }

    companion object {
        private val SEARCH_QUERY = """
            query (${'$'}search: String, ${'$'}perPage: Int) {
              Page(perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME, sort: SEARCH_MATCH) {
                  id
                  title { english romaji }
                  coverImage { large }
                  description(asHtml: false)
                  episodes
                  averageScore
                  popularity
                  startDate { year }
                  genres
                  siteUrl
                }
              }
            }
        """.trimIndent()
    }
}
