package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataSearchResult
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
                suggestions = listOf(suggestion(MetadataSource.GoogleBooks, "volume-1", MediaType.Book)),
            )
        }
        val candidate = ImportedMetadataResolver(candidateRepository).resolve(
            input(ImportSource.StoryGraphCsv, MediaType.Book, externalId = "storygraph-uid-123"),
        ) as ImportedResolution.Candidates
        assertEquals("volume-1", candidate.references.single().externalId)
        assertEquals(0, candidateRepository.identityCalls)
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

    private fun input(
        source: ImportSource,
        mediaType: MediaType,
        externalId: String?,
        malId: Int? = null,
    ) = ImportedMetadataInput(
        mediaItemId = 1,
        importSource = source,
        sourceExternalId = externalId,
        malId = malId,
        mediaType = mediaType,
        title = "Imported title",
        releaseYear = 1999,
    )

    private fun suggestion(
        source: MetadataSource,
        externalId: String,
        mediaType: MediaType,
        title: String = "Resolved title",
    ) = MetadataSuggestion(
        source = source,
        externalId = externalId,
        mediaType = mediaType,
        title = title,
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
