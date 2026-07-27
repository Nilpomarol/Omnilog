package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SteamMetadataRepositoryTest {
    @Test
    fun turnsStoreSearchGamesIntoSteamSuggestions() {
        val suggestion = steamSearchSuggestion(
            JSONObject(
                """
                {
                  "id": 1145360,
                  "name": "Hades",
                  "type": "game",
                  "tiny_image": "https://cdn.example/hades.jpg"
                }
                """.trimIndent(),
            ),
        )

        requireNotNull(suggestion)
        assertEquals(MetadataSource.Steam, suggestion.source)
        assertEquals(MediaType.Game, suggestion.mediaType)
        assertEquals("1145360", suggestion.externalId)
        assertEquals("Hades", suggestion.title)
        assertEquals("https://store.steampowered.com/app/1145360/", suggestion.sourceUrl)
    }

    @Test
    fun ignoresNonGameStoreSearchResults() {
        assertNull(
            steamSearchSuggestion(
                JSONObject("{\"id\": 1145360, \"name\": \"Hades Soundtrack\", \"type\": \"dlc\"}"),
            ),
        )
    }

    @Test
    fun keepsSteamMatchesWhenRawgAlsoReturnsUnrelatedGames() {
        val results = mergeGameSearchSuggestions(
            rawgSuggestions = listOf(game(MetadataSource.Rawg, "Unrelated RAWG Match")),
            steamSuggestions = listOf(game(MetadataSource.Steam, "The Actual Steam Game")),
        )

        assertEquals(listOf("Unrelated RAWG Match", "The Actual Steam Game"), results.map { it.title })
    }

    @Test
    fun hidesSteamMatchesThatDuplicateRawgTitles() {
        val results = mergeGameSearchSuggestions(
            rawgSuggestions = listOf(game(MetadataSource.Rawg, "Hades II")),
            steamSuggestions = listOf(game(MetadataSource.Steam, "Hades II")),
        )

        assertEquals(1, results.size)
        assertEquals(MetadataSource.Rawg, results.single().source)
    }

    @Test
    fun recognizesExplicitSteamAppIdQueries() {
        assertEquals("1145360", steamAppIdFromSearchQuery("steam:1145360"))
        assertEquals("1145360", steamAppIdFromSearchQuery(" Steam ID: 1145360 "))
        assertNull(steamAppIdFromSearchQuery("1145360"))
        assertNull(steamAppIdFromSearchQuery("Hades"))
    }

    private fun game(source: MetadataSource, title: String) = MetadataSuggestion(
        source = source,
        externalId = title.hashCode().toString(),
        mediaType = MediaType.Game,
        title = title,
    )
}
