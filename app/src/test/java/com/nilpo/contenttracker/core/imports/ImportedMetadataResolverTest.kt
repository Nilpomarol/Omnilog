package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.BookEditionMetadata
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataProviderHttpException
import com.nilpo.contenttracker.core.repository.MetadataSearchFailure
import com.nilpo.contenttracker.core.repository.MetadataSearchResult
import com.nilpo.contenttracker.core.repository.retryAfterDelayMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedMetadataResolverTest {
    @Test
    fun `MAL id becomes an exact Jikan reference without title matching`() = runBlocking {
        val repository = FakeMetadataRepository()

        val result = ImportedMetadataResolver(repository).resolve(
            input(ImportSource.MalApi, MediaType.Anime, externalId = "5114", malId = 5114),
        ) as ImportedResolution.Exact

        assertEquals(MetadataSource.Jikan, result.reference.source)
        assertEquals("5114", result.reference.externalId)
        assertEquals(0, repository.searchCalls)
        assertEquals(0, repository.identityCalls)
    }

    @Test
    fun `IMDb id uses the provider external-id resolver and remains exact`() = runBlocking {
        val repository = FakeMetadataRepository().apply {
            identityResult = suggestion(MetadataSource.Tmdb, "603", MediaType.Movie)
        }

        val result = ImportedMetadataResolver(repository).resolve(
            input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
        ) as ImportedResolution.Exact

        assertEquals(MetadataSource.Tmdb, result.reference.source)
        assertEquals("603", result.reference.externalId)
        assertEquals(1, repository.identityCalls)
    }

    @Test
    fun `missing TMDB credential is unavailable instead of silently title matching`() = runBlocking {
        val repository = FakeMetadataRepository().apply { imdbAvailable = false }

        val result = ImportedMetadataResolver(repository).resolve(
            input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
        )

        assertTrue(result is ImportedResolution.Unavailable)
        assertEquals(0, repository.searchCalls)
    }

    @Test
    fun `valid ISBN is resolved exactly while a StoryGraph UID only creates candidates`() = runBlocking {
        val exactRepository = FakeMetadataRepository().apply {
            identityResult = suggestion(MetadataSource.OpenLibrary, "/works/OL45804W", MediaType.Book)
        }
        val exact = ImportedMetadataResolver(exactRepository).resolve(
            input(ImportSource.StoryGraphCsv, MediaType.Book, externalId = "978-0-261-10357-3"),
        ) as ImportedResolution.Exact
        assertEquals(MetadataSource.OpenLibrary, exact.reference.source)

        val candidateRepository = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(
                        MetadataSource.GoogleBooks,
                        "volume-1",
                        MediaType.Book,
                        title = "Imported title",
                    ),
                ),
            )
        }
        val candidate = ImportedMetadataResolver(candidateRepository).resolve(
            input(ImportSource.StoryGraphCsv, MediaType.Book, externalId = "storygraph-uid-123"),
        ) as ImportedResolution.Candidates
        assertEquals("volume-1", candidate.references.single().externalId)
        assertEquals(0, candidateRepository.identityCalls)
    }

    @Test
    fun `StoryGraph fallback ranks matching authors and removes clear author conflicts`() = runBlocking {
        val repository = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(
                        MetadataSource.OpenLibrary,
                        "wrong-author",
                        MediaType.Book,
                        title = "Cent anys de solitud",
                        creators = listOf("Mario Vargas Llosa"),
                    ),
                    suggestion(
                        MetadataSource.OpenLibrary,
                        "missing-author",
                        MediaType.Book,
                        title = "Cent anys de solitud",
                    ),
                    suggestion(
                        MetadataSource.GoogleBooks,
                        "matching-author",
                        MediaType.Book,
                        title = "Cent anys de solitud",
                        creators = listOf("Márquez, Gabriel García"),
                    ),
                ),
            )
        }

        val result = ImportedMetadataResolver(repository).resolve(
            input(
                source = ImportSource.StoryGraphCsv,
                mediaType = MediaType.Book,
                externalId = "storygraph-uid-123",
                title = "Cent anys de solitud",
                creators = listOf("Gabriel Garcia Marquez"),
            ),
        ) as ImportedResolution.Candidates

        assertEquals(listOf("matching-author", "missing-author"), result.references.map { it.externalId })
        assertTrue(result.references.first().evidence.contains("autoria coincident"))
        assertTrue(result.references.last().evidence.contains("autoria no disponible"))
    }

    @Test
    fun `StoryGraph search applies only proven edition ISBNs automatically`() = runBlocking {
        val isbn = "9780261103573"
        val exactEditionRepository = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(
                        MetadataSource.GoogleBooks,
                        "exact-edition",
                        MediaType.Book,
                        title = "The Fellowship of the Ring",
                        creators = listOf("J. R. R. Tolkien"),
                        identifiers = listOf(isbn),
                        bookEdition = BookEditionMetadata(
                            externalId = "exact-edition",
                            isbn = isbn,
                            pageCount = 432,
                        ),
                    ),
                ),
            )
        }
        val exact = ImportedMetadataResolver(exactEditionRepository).resolve(
            input(
                source = ImportSource.StoryGraphCsv,
                mediaType = MediaType.Book,
                externalId = isbn,
                title = "The Fellowship of the Ring",
                creators = listOf("J.R.R. Tolkien"),
            ),
        ) as ImportedResolution.Exact
        assertEquals("exact-edition", exact.reference.externalId)

        val workOnlyRepository = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(
                        MetadataSource.OpenLibrary,
                        "/works/OL1W",
                        MediaType.Book,
                        title = "The Fellowship of the Ring",
                        creators = listOf("J. R. R. Tolkien"),
                        identifiers = listOf(isbn),
                    ),
                ),
            )
        }
        val workOnly = ImportedMetadataResolver(workOnlyRepository).resolve(
            input(
                source = ImportSource.StoryGraphCsv,
                mediaType = MediaType.Book,
                externalId = isbn,
                title = "The Fellowship of the Ring",
                creators = listOf("J.R.R. Tolkien"),
            ),
        )
        assertTrue(workOnly is ImportedResolution.Candidates)
    }

    @Test
    fun `StoryGraph exact edition with conflicting author still requires review`() = runBlocking {
        val isbn = "9780261103573"
        val repository = FakeMetadataRepository().apply {
            identityResult = suggestion(
                MetadataSource.GoogleBooks,
                "conflicting-edition",
                MediaType.Book,
                title = "The Fellowship of the Ring",
                creators = listOf("Different Writer"),
                identifiers = listOf(isbn),
                bookEdition = BookEditionMetadata(externalId = "conflicting-edition", isbn = isbn),
            )
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(
                        MetadataSource.GoogleBooks,
                        "conflicting-edition",
                        MediaType.Book,
                        title = "The Fellowship of the Ring",
                        creators = listOf("Different Writer"),
                        identifiers = listOf(isbn),
                        bookEdition = BookEditionMetadata(externalId = "conflicting-edition", isbn = isbn),
                    ),
                ),
            )
        }

        val result = ImportedMetadataResolver(repository).resolve(
            input(
                source = ImportSource.StoryGraphCsv,
                mediaType = MediaType.Book,
                externalId = isbn,
                title = "The Fellowship of the Ring",
                creators = listOf("J.R.R. Tolkien"),
            ),
        )

        assertTrue(result is ImportedResolution.Candidates)
    }

    @Test
    fun `title matches always require review and provider failures remain retryable`() = runBlocking {
        val candidates = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = listOf(
                    suggestion(MetadataSource.AniList, "1", MediaType.Anime, title = "Imported title"),
                ),
            )
        }
        val candidateResult = ImportedMetadataResolver(candidates).resolve(
            input(ImportSource.MalXml, MediaType.Anime, externalId = null, malId = null),
        )
        assertTrue(candidateResult is ImportedResolution.Candidates)

        val failing = FakeMetadataRepository().apply { identityFailure = IllegalStateException("offline") }
        val failure = ImportedMetadataResolver(failing).resolve(
            input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
        ) as ImportedResolution.Failed
        assertTrue(failure.retryable)
        assertTrue(failure.diagnostic.contains("offline"))
    }

    @Test
    fun `HTTP failures preserve retry timing and distinguish authorization from permanent errors`() = runBlocking {
        val rateLimited = FakeMetadataRepository().apply {
            identityFailure = MetadataProviderHttpException(
                statusCode = 429,
                requestUrl = "https://provider.test/item",
                retryAfterMillis = 240_000L,
            )
        }
        val rateLimitResult = ImportedMetadataResolver(rateLimited).resolve(
            input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
        ) as ImportedResolution.Failed
        assertTrue(rateLimitResult.retryable)
        assertEquals(240_000L, rateLimitResult.retryAfterMillis)

        val unauthorized = FakeMetadataRepository().apply {
            identityFailure = MetadataProviderHttpException(
                statusCode = 401,
                requestUrl = "https://provider.test/item",
            )
        }
        assertTrue(
            ImportedMetadataResolver(unauthorized).resolve(
                input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
            ) is ImportedResolution.Unavailable,
        )

        val invalidRequest = FakeMetadataRepository().apply {
            identityFailure = MetadataProviderHttpException(
                statusCode = 400,
                requestUrl = "https://provider.test/item",
            )
        }
        val permanent = ImportedMetadataResolver(invalidRequest).resolve(
            input(ImportSource.ImdbCsv, MediaType.Movie, externalId = "tt0133093"),
        ) as ImportedResolution.Failed
        assertFalse(permanent.retryable)
    }

    @Test
    fun `partial-search diagnostics retain the longest provider retry delay`() = runBlocking {
        val repository = FakeMetadataRepository().apply {
            searchResult = MetadataSearchResult(
                suggestions = emptyList(),
                failedSources = setOf(MetadataSource.OpenLibrary, MetadataSource.GoogleBooks),
                failures = listOf(
                    MetadataSearchFailure(
                        source = MetadataSource.OpenLibrary,
                        diagnostic = "OpenLibrary returned HTTP 503",
                        retryable = true,
                        statusCode = 503,
                        retryAfterMillis = 90_000L,
                    ),
                    MetadataSearchFailure(
                        source = MetadataSource.GoogleBooks,
                        diagnostic = "GoogleBooks returned HTTP 429",
                        retryable = true,
                        statusCode = 429,
                        retryAfterMillis = 300_000L,
                    ),
                ),
            )
        }

        val result = ImportedMetadataResolver(repository).resolve(
            input(ImportSource.StoryGraphCsv, MediaType.Book, externalId = "storygraph-uid"),
        ) as ImportedResolution.Failed

        assertTrue(result.retryable)
        assertEquals(300_000L, result.retryAfterMillis)
    }

    @Test
    fun `enrichment retry delay is exponential honors provider delay and is capped`() {
        assertEquals(60_000L, enrichmentRetryDelayMillis(attempt = 1))
        assertEquals(120_000L, enrichmentRetryDelayMillis(attempt = 2))
        assertEquals(300_000L, enrichmentRetryDelayMillis(attempt = 1, providerRetryAfterMillis = 300_000L))
        assertEquals(21_600_000L, enrichmentRetryDelayMillis(attempt = 20, providerRetryAfterMillis = Long.MAX_VALUE))
    }

    @Test
    fun `Retry-After accepts seconds and HTTP dates`() {
        val now = 1_700_000_000_000L

        assertEquals(120_000L, retryAfterDelayMillis("120", now))
        assertEquals(
            300_000L,
            retryAfterDelayMillis("Tue, 14 Nov 2023 22:18:20 GMT", now),
        )
        assertEquals(null, retryAfterDelayMillis("not-a-delay", now))
    }

    @Test
    fun `ISBN normalization validates checksums`() {
        assertEquals("9780261103573", "978-0-261-10357-3".normalizeIsbn())
        assertTrue("9780261103573".isValidIsbn())
        assertTrue("0306406152".isValidIsbn())
        assertFalse("9780261103574".isValidIsbn())
        assertFalse("storygraph123".normalizeIsbn().isValidIsbn())
    }

    @Test
    fun `candidate references preserve review labels through durable JSON`() {
        val references = listOf(
            ProviderReference(
                source = MetadataSource.Tmdb,
                externalId = "603",
                mediaType = MediaType.Movie,
                evidence = "Title candidate",
                title = "The Matrix",
                originalTitle = "The Matrix",
                releaseYear = 1999,
                coverUrl = "https://example.test/matrix.jpg",
                subtitle = "A novel",
                creators = listOf("Writer One", "Writer Two"),
                credits = listOf(
                    MediaCredit(
                        personName = "Writer One",
                        roleType = MediaCreditRole.Author,
                        metadataSource = MetadataSource.OpenLibrary,
                    ),
                ),
                language = "English",
                progressTotal = 432,
                genres = listOf("Science fiction", "Adventure"),
                synopsis = "Enough context to distinguish this edition.",
                sourceUrl = "https://example.test/book",
                publishers = listOf("Example Press"),
                identifiers = listOf("9780261103573", "0261103571"),
                format = "Hardcover",
                ratingScore = 4.25,
                ratingMaxScore = 5.0,
                ratingVoteCount = 1_234,
                externalRatings = listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.OpenLibrary,
                        score = 4.25,
                        maxScore = 5.0,
                        voteCount = 1_234,
                    ),
                ),
            ),
        )

        assertEquals(references, references.toJson().toProviderReferences())
    }

    @Test
    fun `candidate reference parser accepts phase 3 payloads without display metadata`() {
        val legacy = """[{"source":"Jikan","externalId":"5114","mediaType":"Anime","evidence":"candidate"}]"""

        val parsed = legacy.toProviderReferences().single()

        assertEquals("5114", parsed.externalId)
        assertEquals(null, parsed.title)
    }

    @Test
    fun `durable exact book reference reconstructs the selected edition`() {
        val reference = ProviderReference(
            source = MetadataSource.OpenLibrary,
            externalId = "/books/OL1M",
            mediaType = MediaType.Book,
            evidence = "Exact ISBN",
            title = "The Fellowship of the Ring",
            releaseYear = 2004,
            language = "English",
            progressTotal = 432,
            coverUrl = "https://example.test/cover.jpg",
            publishers = listOf("Example Press"),
            identifiers = listOf("9780261103573"),
            format = "Paperback",
        )

        val restored = listOf(reference).toJson().toProviderReferences().single()
            .toSuggestion("Fallback")

        assertEquals("/books/OL1M", restored.bookEdition?.externalId)
        assertEquals("9780261103573", restored.bookEdition?.isbn)
        assertEquals(432, restored.bookEdition?.pageCount)
        assertEquals("Paperback", restored.bookEdition?.format)
        assertEquals("Example Press", restored.bookEdition?.publisher)
    }

    private fun input(
        source: ImportSource,
        mediaType: MediaType,
        externalId: String?,
        malId: Int? = null,
        title: String = "Imported title",
        creators: List<String> = emptyList(),
    ) = ImportedMetadataInput(
        mediaItemId = 1,
        importSource = source,
        sourceExternalId = externalId,
        malId = malId,
        mediaType = mediaType,
        title = title,
        releaseYear = 1999,
        creators = creators,
    )

    private fun suggestion(
        source: MetadataSource,
        externalId: String,
        mediaType: MediaType,
        title: String = "Resolved title",
        creators: List<String> = emptyList(),
        identifiers: List<String> = emptyList(),
        bookEdition: BookEditionMetadata? = null,
    ) = MetadataSuggestion(
        source = source,
        externalId = externalId,
        mediaType = mediaType,
        title = title,
        creators = creators,
        identifiers = identifiers,
        bookEdition = bookEdition,
    )

    private class FakeMetadataRepository : MetadataRepository {
        var identityResult: MetadataSuggestion? = null
        var identityFailure: Throwable? = null
        var searchResult: MetadataSearchResult = MetadataSearchResult(emptyList())
        var imdbAvailable: Boolean = true
        var identityCalls: Int = 0
        var searchCalls: Int = 0

        override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> =
            searchSuggestionsWithDiagnostics(request).suggestions

        override suspend fun searchSuggestionsWithDiagnostics(request: MetadataSearchRequest): MetadataSearchResult {
            searchCalls++
            return searchResult
        }

        override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion = suggestion

        override suspend fun resolveImportedExternalId(
            source: MetadataSource,
            externalId: String,
            mediaType: MediaType,
        ): MetadataSuggestion? {
            identityCalls++
            identityFailure?.let { throw it }
            return identityResult
        }

        override fun isImportedSourceAvailable(source: MetadataSource): Boolean =
            source != MetadataSource.Imdb || imdbAvailable
    }
}
