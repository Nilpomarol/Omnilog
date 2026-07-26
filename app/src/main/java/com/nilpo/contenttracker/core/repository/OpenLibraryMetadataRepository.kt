package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.BookEditionMetadata
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class OpenLibraryMetadataRepository : MetadataRepository {
    /**
     * Author OLID to portrait URL, with a blank value recording "this author has no photo".
     *
     * Authors recur constantly across a book library, and the answer never changes within a
     * session, so the verification below costs one request per distinct author rather than one
     * per book.
     */
    private val authorPortraits = ConcurrentHashMap<String, String>()

    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Book !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val response = getJson(
                "https://openlibrary.org/search.json?q=$encodedQuery" +
                    "&limit=20&fields=$OpenLibrarySearchFields",
            )
            val docs = response.optJSONArray("docs") ?: return@withContext emptyList()
            // A results page is twenty books' worth of authors; portraits are resolved on the
            // detail path instead, where the cost is bounded to the one book being added.
            List(docs.length()) { docs.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion(resolveAuthorPortraits = false) }
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (suggestion.source != MetadataSource.OpenLibrary) return suggestion

        // Failures propagate: the caller reports them and offers a retry, rather than silently
        // handing back the un-enriched suggestion as if the details had loaded.
        return withContext(Dispatchers.IO) {
            if (suggestion.externalId.startsWith("/books/")) {
                val editionJson = getJson("https://openlibrary.org${suggestion.externalId}.json")
                val expectedIsbn = suggestion.identifiers
                    .map(String::normalizedOpenLibraryIsbn)
                    .firstOrNull { it.length == 10 || it.length == 13 }
                val exactEdition = editionJson.toExactEditionSuggestion(expectedIsbn)
                    ?: return@withContext suggestion
                return@withContext mergeExactBookMatches(exactEdition, suggestion) ?: exactEdition
            }
            val work = getJson("https://openlibrary.org${suggestion.externalId}.json")
            val editions = getJson(
                "https://openlibrary.org${suggestion.externalId}/editions.json" +
                "?limit=20&fields=entries(key,title,number_of_pages,covers,isbn_10,isbn_13,languages,publish_date,publishers,physical_format)",
            )
            suggestion.enrichWith(work, editions)
        }
    }

    internal suspend fun findByIsbn(isbn: String): MetadataSuggestion? = withContext(Dispatchers.IO) {
        val normalized = isbn.normalizedOpenLibraryIsbn()
        val editionJson = getJson("https://openlibrary.org/isbn/$normalized.json")
        editionJson.toExactEditionSuggestion(normalized)
    }

    private suspend fun JSONObject.toExactEditionSuggestion(expectedIsbn: String?): MetadataSuggestion? {
        val returnedIsbns = listOf("isbn_10", "isbn_13")
            .flatMap { key -> optJSONArray(key).toStringList() }
            .map(String::normalizedOpenLibraryIsbn)
            .filter { it.isNotBlank() }
        val normalizedExpected = expectedIsbn?.normalizedOpenLibraryIsbn()
        if (normalizedExpected != null && normalizedExpected !in returnedIsbns) return null
        val selectedIsbn = normalizedExpected ?: returnedIsbns.firstOrNull()
        val edition = toEditionMetadata()?.copy(isbn = selectedIsbn) ?: return null
        val workKey = optJSONArray("works")
            ?.optJSONObject(0)
            ?.optString("key")
            ?.takeIf { it.startsWith("/works/") }
        val work = selectedIsbn?.let { findWorkByIsbn(it, workKey) }
        val base = work ?: MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = workKey ?: edition.externalId,
            mediaType = MediaType.Book,
            title = edition.title ?: optString("title").takeIf { it.isNotBlank() } ?: return null,
        )
        return edition.toMetadataSuggestion(base)
    }

    private suspend fun findWorkByIsbn(isbn: String, expectedWorkKey: String?): MetadataSuggestion? {
        return try {
            val encodedIsbnQuery = URLEncoder.encode("isbn:$isbn", "UTF-8")
            val response = getJson(
                "https://openlibrary.org/search.json?q=$encodedIsbnQuery" +
                    "&limit=5&fields=$OpenLibrarySearchFields",
            )
            val docs = response.optJSONArray("docs") ?: return null
            // The matching doc is chosen before portraits are resolved, so the five candidates
            // cost author lookups for the one book that is actually kept.
            List(docs.length()) { index -> docs.optJSONObject(index) }
                .filterNotNull()
                .firstOrNull { doc ->
                    val candidate = doc.toMetadataSuggestion(resolveAuthorPortraits = false)
                    candidate != null &&
                        (expectedWorkKey == null || candidate.externalId == expectedWorkKey) &&
                        candidate.identifiers.any { it.normalizedOpenLibraryIsbn() == isbn }
                }
                ?.toMetadataSuggestion(resolveAuthorPortraits = true)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Edition identity is still useful when optional work-level enrichment is unavailable.
            null
        }
    }

    private fun JSONObject.toMetadataSuggestion(resolveAuthorPortraits: Boolean): MetadataSuggestion? {
        val key = optString("key").takeIf { it.startsWith("/works/") } ?: return null
        val title = optString("title").takeIf { it.isNotBlank() } ?: return null
        val authorEntries = optJSONArray("author_name")
            .toOpenLibraryAuthorCredits(optJSONArray("author_key"))
        val authorCredits = if (resolveAuthorPortraits) {
            authorEntries.withVerifiedPortraits()
        } else {
            authorEntries.map { it.credit }
        }
        val authors = authorCredits.map { it.personName }
        val coverId = optLong("cover_i", 0L).takeIf { it > 0L }
        val rating = optDouble("ratings_average", 0.0)
        val ratingCount = optInt("ratings_count", 0)
        val pages = optInt("number_of_pages_median", 0).takeIf { it > 0 }
        val identifiers = optJSONArray("isbn").toStringList(limit = 12)
        val publishers = optJSONArray("publisher").toStringList(limit = 5)
        val externalRating = if (rating > 0.0) {
            MetadataRatingSuggestion(
                score = rating,
                maxScore = 5.0,
                voteCount = ratingCount.takeIf { it > 0 },
            )
        } else {
            null
        }

        return MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = key,
            mediaType = MediaType.Book,
            title = title,
            releaseYear = optInt("first_publish_year", 0).takeIf { it > 0 },
            language = ItemLanguage.normalize(optJSONArray("language").toStringList(limit = 1).firstOrNull()),
            coverUrl = coverId?.let { coverUrl(it) },
            synopsis = null,
            progressTotal = pages,
            creators = authors,
            credits = authorCredits,
            genres = optJSONArray("subject").toStringList().standardBookGenres(),
            sourceUrl = "https://openlibrary.org$key",
            publishers = publishers,
            identifiers = identifiers,
            popularityScore = optJSONArray("edition_key")?.length()?.toDouble(),
            externalRating = externalRating,
            externalRatings = externalRating?.let {
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.OpenLibrary,
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private fun MetadataSuggestion.enrichWith(
        work: JSONObject,
        editions: JSONObject,
    ): MetadataSuggestion {
        val description = work.descriptionText()
        val remoteTitle = work.optString("title").takeIf { it.isNotBlank() }
        val subjects = work.optJSONArray("subjects").toStringList().standardBookGenres()
        val editionSuggestions = editions.optJSONArray("entries").toBookEditionMetadata()
        // Work details contain author IDs but not their display names. The item already retains
        // the ordered author names from search/import, so pair those with the work's canonical IDs
        // to make a refresh capable of adding author portraits.
        val refreshedAuthorCredits = work.toOpenLibraryWorkAuthorCredits(creators).withVerifiedPortraits()

        return copy(
            title = remoteTitle ?: title,
            synopsis = description ?: synopsis,
            genres = subjects.ifEmpty { genres },
            credits = refreshedAuthorCredits.ifEmpty { credits },
            bookEditionSuggestions = editionSuggestions,
        )
    }

    private fun JSONObject.toEditionMetadata(): BookEditionMetadata? {
        val externalId = optString("key").takeIf { it.startsWith("/books/") } ?: return null
        return toBookEditionMetadata(externalId)
    }

    /**
     * Turns each author's OLID into a portrait only once the author record confirms one exists.
     *
     * Neither the search nor the work response says whether an author has a photo, so a URL built
     * from the OLID alone is a guess — and because most authors have none, that guess is usually a
     * permanent 404 stored as if it were an image. Everything downstream reads a non-blank
     * `personImageUrl` as "this author is illustrated", so a guess also shadows the real portrait
     * the same author may have on another book.
     *
     * Best-effort like the other image enrichers: an unreachable author record leaves the credit
     * without a portrait rather than failing the book.
     */
    private fun List<OpenLibraryAuthorCredit>.withVerifiedPortraits(): List<MediaCredit> = map { entry ->
        val authorKey = entry.authorKey ?: return@map entry.credit
        entry.credit.copy(personImageUrl = authorPortrait(authorKey))
    }

    private fun authorPortrait(authorKey: String): String? {
        authorPortraits[authorKey]?.let { cached -> return cached.takeIf { it.isNotBlank() } }
        val portrait = try {
            getJson("https://openlibrary.org/authors/$authorKey.json").openLibraryAuthorPortraitUrl()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Deliberately uncached: a reachability problem is not evidence about the author, and
            // caching it would make one blocked request suppress the portrait for the whole session.
            return null
        }
        authorPortraits[authorKey] = portrait.orEmpty()
        return portrait
    }
}

/** An author credit paired with the OLID needed to look its portrait up. */
internal data class OpenLibraryAuthorCredit(
    val credit: MediaCredit,
    val authorKey: String?,
)

private fun JSONArray?.toStringList(limit: Int = Int.MAX_VALUE): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)
}

private fun JSONArray?.toOpenLibraryAuthorCredits(
    authorKeys: JSONArray?,
): List<OpenLibraryAuthorCredit> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val author = optString(index).takeIf { it.isNotBlank() } ?: return@List null
        OpenLibraryAuthorCredit(
            credit = MediaCredit(
                personName = author,
                roleType = MediaCreditRole.Author,
                sortOrder = index,
                metadataSource = MetadataSource.OpenLibrary,
            ),
            authorKey = authorKeys?.optString(index)?.toOpenLibraryAuthorKey(),
        )
    }.filterNotNull()
        .distinctBy { it.credit.personName.trim().lowercase() }
}

/**
 * A work response has `authors[].author.key` but no author-name field. The ordered names supplied
 * by the original search result remain the source of display text; this endpoint supplies the
 * stable IDs needed to look portraits up when the item is refreshed later.
 */
internal fun JSONObject.toOpenLibraryWorkAuthorCredits(
    authorNames: List<String>,
): List<OpenLibraryAuthorCredit> {
    val authorKeys = optJSONArray("authors")?.let { authors ->
        List(authors.length()) { index ->
            authors.optJSONObject(index)
                ?.optJSONObject("author")
                ?.optString("key")
                ?.substringAfterLast('/')
                ?.toOpenLibraryAuthorKey()
        }
    }.orEmpty()
    return authorNames.mapIndexed { index, authorName ->
        authorName.trim().takeIf { it.isNotBlank() }?.let { name ->
            OpenLibraryAuthorCredit(
                credit = MediaCredit(
                    personName = name,
                    roleType = MediaCreditRole.Author,
                    sortOrder = index,
                    metadataSource = MetadataSource.OpenLibrary,
                ),
                authorKey = authorKeys.getOrNull(index),
            )
        }
    }.filterNotNull()
        .distinctBy { it.credit.personName.trim().lowercase() }
}

private fun String.toOpenLibraryAuthorKey(): String? =
    trim().takeIf { it.startsWith("OL") && it.endsWith("A") }

/**
 * The portrait an author record actually has, or null when it has none.
 *
 * `photos` holds cover IDs, with deleted entries recorded as `-1`. Addressing the portrait by that
 * ID rather than by OLID is what makes the URL a fact instead of a guess.
 */
internal fun JSONObject.openLibraryAuthorPortraitUrl(): String? {
    val photos = optJSONArray("photos") ?: return null
    val photoId = List(photos.length()) { index -> photos.optLong(index, 0L) }
        .firstOrNull { it > 0L }
        ?: return null
    return openLibraryAuthorImageUrl(photoId)
}

/**
 * `default=false` keeps a portrait deleted after we stored it a normal image-load failure, so the
 * detail page falls back to the author's initial instead of Open Library's placeholder glyph.
 */
internal fun openLibraryAuthorImageUrl(photoId: Long): String {
    return "https://covers.openlibrary.org/a/id/$photoId-L.jpg?default=false"
}

private fun JSONObject.descriptionText(): String? {
    val rawDescription = opt("description") ?: return null
    return when (rawDescription) {
        is String -> rawDescription
        is JSONObject -> rawDescription.optString("value")
        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun JSONArray?.toBookEditionMetadata(): List<BookEditionMetadata> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index) }
        .mapNotNull { edition ->
            val editionObject = edition ?: return@mapNotNull null
            val externalId = editionObject.optString("key").takeIf { it.startsWith("/books/") }
                ?: return@mapNotNull null
            editionObject.toBookEditionMetadata(externalId)
        }
        .distinctBy { it.externalId }
}

private fun JSONObject.toBookEditionMetadata(externalId: String): BookEditionMetadata {
    val coverId = optJSONArray("covers")?.firstLong()?.takeIf { it > 0L }
    val isbn = optJSONArray("isbn_13")?.firstString() ?: optJSONArray("isbn_10")?.firstString()
    val language = optJSONArray("languages").toEditionLanguage()
    val publisher = optJSONArray("publishers")?.firstString()
    return BookEditionMetadata(
        externalId = externalId,
        title = optString("title").takeIf { it.isNotBlank() },
        releaseYear = optString("publish_date").extractYear(),
        language = language,
        pageCount = optInt("number_of_pages", 0).takeIf { it > 0 },
        coverUrl = coverId?.let(::coverUrl),
        isbn = isbn,
        format = optString("physical_format").takeIf { it.isNotBlank() },
        publisher = publisher,
        sourceUrl = "https://openlibrary.org$externalId",
    )
}

private fun JSONArray?.toEditionLanguage(): String? {
    if (this == null || length() == 0) return null
    val first = opt(0)
    val languageCode = when (first) {
        is String -> first
        is JSONObject -> first.optString("key").substringAfterLast('/')
        else -> null
    }
    return ItemLanguage.normalize(languageCode)
}

private fun JSONArray.firstLong(): Long? {
    if (length() == 0) return null
    return optLong(0, 0L)
}

private fun JSONArray.firstString(): String? {
    return optString(0).takeIf { it.isNotBlank() }
}

private fun String.extractYear(): Int? {
    return Regex("""\b(\d{4})\b""")
        .find(this)
        ?.value
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
}

private fun String.normalizedOpenLibraryIsbn(): String =
    filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()

private const val OpenLibrarySearchFields =
    "key,title,author_name,author_key,first_publish_year,cover_i,subject,language," +
        "number_of_pages_median,ratings_average,ratings_count,edition_key,isbn,publisher"

private fun coverUrl(coverId: Long): String {
    return "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
}
