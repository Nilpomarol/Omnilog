package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyAnimeListIdTest {
    @Test
    fun explicitIdWinsOverLegacySources() {
        assertEquals(
            42,
            resolveMyAnimeListId(
                explicitMalId = 42,
                metadataSource = MetadataSource.Jikan.name,
                metadataExternalId = "99",
                popularityJson = "{\"malId\":100}",
                sourceUrl = "https://myanimelist.net/anime/101",
            ),
        )
    }

    @Test
    fun resolvesJikanAndAniListLegacyIds() {
        assertEquals(99, resolveMyAnimeListId(null, "Jikan", "99", null, null))
        assertEquals(100, resolveMyAnimeListId(null, "AniList", "1", "{\"malId\":100}", null))
    }

    @Test
    fun resolvesMalAnimeUrlButRejectsInvalidIds() {
        assertEquals(
            101,
            resolveMyAnimeListId(null, null, null, null, "https://myanimelist.net/anime/101/Title"),
        )
        assertNull(resolveMyAnimeListId(null, "Jikan", "not-a-number", "{}", "https://example.com"))
    }
}
