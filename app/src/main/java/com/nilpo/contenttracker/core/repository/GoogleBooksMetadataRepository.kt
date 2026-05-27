package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ItemLanguage
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

class GoogleBooksMetadataRepository(
    private val apiKey: String,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (apiKey.isBlank() || MediaType.Book !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query.toGoogleBooksQuery(), "UTF-8")
            val fields = "items(id,volumeInfo(title,authors,description,pageCount," +
                "averageRating,ratingsCount,publishedDate,categories,language,imageLinks/thumbnail,infoLink))"
            val response = getJson(
                "https://www.googleapis.com/books/v1/volumes" +
                    "?q=$encodedQuery&maxResults=20&printType=books&orderBy=relevance" +
                    "&hl=es&fields=$fields&key=$apiKey",
            )
            val items = response.optJSONArray("items") ?: return@withContext emptyList()
            List(items.length()) { items.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion() }
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (apiKey.isBlank() || suggestion.source != MetadataSource.GoogleBooks) return suggestion

        return withContext(Dispatchers.IO) {
            runCatching {
                val fields = "id,volumeInfo(title,authors,description,pageCount," +
                    "averageRating,ratingsCount,publishedDate,categories,language,imageLinks/thumbnail,infoLink)"
                val detailed = getJson(
                    "https://www.googleapis.com/books/v1/volumes/${suggestion.externalId}" +
                        "?fields=$fields&key=$apiKey",
                ).toMetadataSuggestion() ?: return@runCatching suggestion

                detailed.copy(
                    externalRating = detailed.externalRating ?: suggestion.externalRating,
                    externalRatings = (suggestion.externalRatings + detailed.externalRatings)
                        .distinctBy { it.source },
                )
            }.getOrElse { suggestion }
        }
    }

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val info = optJSONObject("volumeInfo") ?: return null
        val title = info.optString("title").takeIf { it.isNotBlank() } ?: return null

        val coverUrl = info.optJSONObject("imageLinks")
            ?.optString("thumbnail")
            ?.takeIf { it.isNotBlank() }
            ?.replace("http://", "https://")

        val releaseYear = info.optString("publishedDate")
            .take(4)
            .toIntOrNull()
            ?.takeIf { it > 0 }

        val authors = info.optJSONArray("authors")?.let { arr ->
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val categories = info.optJSONArray("categories")?.let { arr ->
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val averageRating = info.optDouble("averageRating", 0.0)
        val ratingsCount = info.optInt("ratingsCount", 0)
        val externalRating = if (averageRating > 0.0) {
            MetadataRatingSuggestion(
                score = averageRating,
                maxScore = 5.0,
                voteCount = ratingsCount.takeIf { it > 0 },
            )
        } else {
            null
        }

        return MetadataSuggestion(
            source = MetadataSource.GoogleBooks,
            externalId = id,
            mediaType = MediaType.Book,
            title = title,
            releaseYear = releaseYear,
            language = ItemLanguage.normalize(info.optString("language")),
            coverUrl = coverUrl,
            synopsis = info.optString("description").takeIf { it.isNotBlank() },
            progressTotal = info.optInt("pageCount", 0).takeIf { it > 0 },
            creators = authors,
            credits = authors.mapIndexed { index, author ->
                MediaCredit(
                    personName = author,
                    roleType = MediaCreditRole.Author,
                    sortOrder = index,
                    metadataSource = MetadataSource.GoogleBooks,
                )
            },
            genres = categories.standardBookGenres().ifEmpty { categories.take(3) },
            sourceUrl = info.optString("infoLink").takeIf { it.isNotBlank() },
            externalRating = externalRating,
            externalRatings = externalRating?.let {
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.GoogleBooks,
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    }
}

private fun String.toGoogleBooksQuery(): String {
    val compact = filter { it.isDigit() || it == 'X' || it == 'x' }
    return if (compact.length == 10 || compact.length == 13) {
        "isbn:$compact"
    } else {
        this
    }
}
