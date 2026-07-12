package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationRepositoryTest {
    @Test
    fun filtersLibraryDuplicatesAndKeepsProviderOrder() = runBlocking {
        val current = trackedMedia(id = 1, title = "Current", externalId = "10")
        val alreadyTrackedByProvider = trackedMedia(
            id = 2,
            title = "Already tracked",
            externalId = "20",
        )
        val alreadyTrackedByTitle = trackedMedia(
            id = 3,
            title = "Same title",
            releaseYear = 2020,
            externalId = "30",
        )
        val repository = CompositeRecommendationRepository(
            tmdb = RecommendationProvider {
                listOf(
                    suggestion(id = "20", title = "Already tracked"),
                    suggestion(id = "21", title = "Same title", releaseYear = 2020),
                    suggestion(id = "22", title = "First new item"),
                    suggestion(id = "23", title = "First new item"),
                    suggestion(id = "24", title = "Second new item"),
                )
            },
            aniList = RecommendationProvider { emptyList() },
            rawg = RecommendationProvider { emptyList() },
            books = RecommendationProvider { emptyList() },
        )

        val results = repository.getRecommendations(
            current = current,
            library = listOf(current, alreadyTrackedByProvider, alreadyTrackedByTitle),
        )

        assertEquals(
            listOf("First new item", "Second new item"),
            results.map { it.suggestion.title },
        )
        assertEquals(listOf(3, 5), results.map { it.providerRank })
    }

    @Test
    fun doesNotAskUnsupportedProvidersForRecommendations() = runBlocking {
        var providerCalls = 0
        val repository = CompositeRecommendationRepository(
            tmdb = RecommendationProvider {
                providerCalls += 1
                emptyList()
            },
            aniList = RecommendationProvider { emptyList() },
            rawg = RecommendationProvider { emptyList() },
            books = RecommendationProvider { emptyList() },
        )
        val current = TrackedMedia(
            item = MediaItem(
                id = 1,
                type = MediaType.Book,
                title = "Book",
                metadataSource = MetadataSource.OpenLibrary,
                metadataExternalId = "/works/OL1W",
            ),
            sessions = emptyList(),
        )

        val results = repository.getRecommendations(current, listOf(current))

        assertEquals(emptyList<Any>(), results)
        assertEquals(0, providerCalls)
    }

    @Test
    fun dispatchesAnimeItemsToAniListProvider() = runBlocking {
        var providerCalls = 0
        val current = TrackedMedia(
            item = MediaItem(
                id = 10,
                type = MediaType.Anime,
                title = "Anime",
                metadataSource = MetadataSource.AniList,
                metadataExternalId = "100",
            ),
            sessions = emptyList(),
        )
        val repository = CompositeRecommendationRepository(
            tmdb = RecommendationProvider { emptyList() },
            aniList = RecommendationProvider {
                providerCalls += 1
                listOf(
                    MetadataSuggestion(
                        source = MetadataSource.AniList,
                        externalId = "200",
                        mediaType = MediaType.Anime,
                        title = "Suggested anime",
                    ),
                )
            },
            rawg = RecommendationProvider { emptyList() },
            books = RecommendationProvider { emptyList() },
        )

        val results = repository.getRecommendations(current, listOf(current))

        assertEquals(1, providerCalls)
        assertEquals(listOf("Suggested anime"), results.map { it.suggestion.title })
    }
    @Test
    fun dispatchesGameItemsToRawgProvider() = runBlocking {
        var providerCalls = 0
        val current = TrackedMedia(
            item = MediaItem(
                id = 20,
                type = MediaType.Game,
                title = "Game",
                metadataSource = MetadataSource.Rawg,
                metadataExternalId = "300",
            ),
            sessions = emptyList(),
        )
        val repository = CompositeRecommendationRepository(
            tmdb = RecommendationProvider { emptyList() },
            aniList = RecommendationProvider { emptyList() },
            rawg = RecommendationProvider {
                providerCalls += 1
                listOf(
                    MetadataSuggestion(
                        source = MetadataSource.Rawg,
                        externalId = "400",
                        mediaType = MediaType.Game,
                        title = "Suggested game",
                    ),
                )
            },
            books = RecommendationProvider { emptyList() },
        )

        val results = repository.getRecommendations(current, listOf(current))

        assertEquals(1, providerCalls)
        assertEquals(listOf("Suggested game"), results.map { it.suggestion.title })
    }
    @Test
    fun dispatchesBookItemsToBookProviderEvenWithoutExternalId() = runBlocking {
        var providerCalls = 0
        val current = TrackedMedia(
            item = MediaItem(
                id = 30,
                type = MediaType.Book,
                title = "Book",
                creators = listOf("Author"),
                genres = listOf("Fantasy"),
            ),
            sessions = emptyList(),
        )
        val repository = CompositeRecommendationRepository(
            tmdb = RecommendationProvider { emptyList() },
            aniList = RecommendationProvider { emptyList() },
            rawg = RecommendationProvider { emptyList() },
            books = RecommendationProvider {
                providerCalls += 1
                listOf(
                    MetadataSuggestion(
                        source = MetadataSource.OpenLibrary,
                        externalId = "/works/OL2W",
                        mediaType = MediaType.Book,
                        title = "Suggested book",
                    ),
                )
            },
        )

        val results = repository.getRecommendations(current, listOf(current))

        assertEquals(1, providerCalls)
        assertEquals(listOf("Suggested book"), results.map { it.suggestion.title })
    }
    @Test
    fun booksRankAuthorThenCollectionThenGenreOverlap() = runBlocking {
        val queries = mutableListOf<String>()
        val metadataRepository = object : MetadataRepository {
            override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
                queries += request.query
                return when (request.query) {
                    "Author" -> listOf(bookSuggestion("author", "Author book"))
                    "Series" -> listOf(bookSuggestion("series", "Series book"))
                    "Fantasy" -> listOf(
                        bookSuggestion("multi", "Two genre book"),
                        bookSuggestion("one", "One genre book"),
                    )
                    "Mystery" -> listOf(
                        bookSuggestion("multi", "Two genre book"),
                        bookSuggestion("other", "Other genre book"),
                    )
                    else -> emptyList()
                }
            }

            override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
                return suggestion
            }
        }
        val repository = BookRecommendationRepository(metadataRepository)
        val item = MediaItem(
            id = 40,
            type = MediaType.Book,
            title = "Current book",
            creators = listOf("Author"),
            providerCollectionTitle = "Series",
            genres = listOf("Fantasy", "Mystery"),
        )

        val results = repository.getRecommendations(item)

        assertEquals(listOf("Author", "Series", "Fantasy", "Mystery"), queries)
        assertEquals(
            listOf("Author book", "Series book", "Two genre book", "One genre book", "Other genre book"),
            results.map { it.title },
        )
    }
    private fun suggestion(
        id: String,
        title: String,
        releaseYear: Int? = null,
    ): MetadataSuggestion {
        return MetadataSuggestion(
            source = MetadataSource.Tmdb,
            externalId = id,
            mediaType = MediaType.Movie,
            title = title,
            releaseYear = releaseYear,
        )
    }

    private fun bookSuggestion(id: String, title: String): MetadataSuggestion {
        return MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = "/works/OL$id",
            mediaType = MediaType.Book,
            title = title,
        )
    }
    private fun trackedMedia(
        id: Long,
        title: String,
        externalId: String,
        releaseYear: Int? = null,
    ): TrackedMedia {
        return TrackedMedia(
            item = MediaItem(
                id = id,
                type = MediaType.Movie,
                title = title,
                releaseYear = releaseYear,
                metadataSource = MetadataSource.Tmdb,
                metadataExternalId = externalId,
            ),
            sessions = emptyList(),
        )
    }
}
