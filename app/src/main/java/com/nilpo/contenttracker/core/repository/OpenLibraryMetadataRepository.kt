package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OpenLibraryMetadataRepository : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Book !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            runCatching {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val fields = "key,title,author_name,first_publish_year,cover_i,subject," +
                    "number_of_pages_median,ratings_average,ratings_count,edition_key,isbn"
                val response = getJson(
                    "https://openlibrary.org/search.json?q=$encodedQuery" +
                        "&limit=20&lang=es&fields=$fields",
                )
                val docs = response.optJSONArray("docs") ?: return@withContext emptyList()
                List(docs.length()) { docs.getJSONObject(it) }
                    .mapNotNull { it.toMetadataSuggestion() }
            }.getOrDefault(emptyList())
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (suggestion.source != MetadataSource.OpenLibrary) return suggestion

        return withContext(Dispatchers.IO) {
            runCatching {
                val work = getJson("https://openlibrary.org${suggestion.externalId}.json")
                val editions = getJson(
                    "https://openlibrary.org${suggestion.externalId}/editions.json" +
                        "?limit=10&fields=entries(title,number_of_pages,covers,isbn_10,isbn_13)",
                )
                suggestion.enrichWith(work, editions)
            }.getOrDefault(suggestion)
        }
    }

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val key = optString("key").takeIf { it.startsWith("/works/") } ?: return null
        val title = optString("title").takeIf { it.isNotBlank() } ?: return null
        val authors = optJSONArray("author_name").toStringList()
        val coverId = optLong("cover_i", 0L).takeIf { it > 0L }
        val rating = optDouble("ratings_average", 0.0)
        val ratingCount = optInt("ratings_count", 0)
        val pages = optInt("number_of_pages_median", 0).takeIf { it > 0 }

        return MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = key,
            mediaType = MediaType.Book,
            title = title,
            releaseYear = optInt("first_publish_year", 0).takeIf { it > 0 },
            coverUrl = coverId?.let { coverUrl(it) },
            synopsis = null,
            progressTotal = pages,
            creators = authors,
            credits = authors.toAuthorCredits(),
            genres = optJSONArray("subject").toStringList().standardBookGenres(),
            sourceUrl = "https://openlibrary.org$key",
            popularityScore = optJSONArray("edition_key")?.length()?.toDouble(),
            externalRating = if (rating > 0.0) {
                MetadataRatingSuggestion(
                    score = rating,
                    maxScore = 5.0,
                    voteCount = ratingCount.takeIf { it > 0 },
                )
            } else {
                null
            },
        )
    }

    private fun MetadataSuggestion.enrichWith(
        work: JSONObject,
        editions: JSONObject,
    ): MetadataSuggestion {
        val description = work.descriptionText()
        val subjects = work.optJSONArray("subjects").toStringList().standardBookGenres()
        val entries = editions.optJSONArray("entries")
        val editionWithPages = entries.firstObjectWithPositiveInt("number_of_pages")
        val editionWithCover = entries.firstObjectWithArray("covers")
        val pageCount = editionWithPages?.optInt("number_of_pages", 0)?.takeIf { it > 0 }
        val editionCoverId = editionWithCover
            ?.optJSONArray("covers")
            ?.firstLong()
            ?.takeIf { it > 0L }

        return copy(
            synopsis = description ?: synopsis,
            genres = subjects.ifEmpty { genres },
            progressTotal = progressTotal ?: pageCount,
            coverUrl = coverUrl ?: editionCoverId?.let { coverUrl(it) },
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

private fun JSONArray?.toStringList(limit: Int = Int.MAX_VALUE): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)
}

private fun List<String>.toAuthorCredits(): List<MediaCredit> {
    return mapIndexed { index, author ->
        MediaCredit(
            personName = author,
            roleType = MediaCreditRole.Author,
            sortOrder = index,
            metadataSource = MetadataSource.OpenLibrary,
        )
    }
}

private fun JSONObject.descriptionText(): String? {
    val rawDescription = opt("description") ?: return null
    return when (rawDescription) {
        is String -> rawDescription
        is JSONObject -> rawDescription.optString("value")
        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun JSONArray?.firstObjectWithPositiveInt(fieldName: String): JSONObject? {
    if (this == null) return null
    return List(length()) { index -> optJSONObject(index) }
        .firstOrNull { it?.optInt(fieldName, 0)?.let { value -> value > 0 } == true }
}

private fun JSONArray?.firstObjectWithArray(fieldName: String): JSONObject? {
    if (this == null) return null
    return List(length()) { index -> optJSONObject(index) }
        .firstOrNull { it?.optJSONArray(fieldName)?.length()?.let { length -> length > 0 } == true }
}

private fun JSONArray.firstLong(): Long? {
    if (length() == 0) return null
    return optLong(0, 0L)
}

private fun coverUrl(coverId: Long): String {
    return "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
}
