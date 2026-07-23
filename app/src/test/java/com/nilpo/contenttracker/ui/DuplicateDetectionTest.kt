package com.nilpo.contenttracker.ui

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectionTest {
    @Test
    fun aniListLinkedAnimeMatchesJikanResultByPreservedMalId() {
        val existing = anime(
            title = "Fullmetal Alchemist: Brotherhood",
            source = MetadataSource.AniList,
            externalId = "5114-anilist",
            malId = 5114,
        )
        val result = listOf(existing).findDuplicateFor(
            suggestion(
                title = "Hagane no Renkinjutsushi: Fullmetal Alchemist",
                source = MetadataSource.Jikan,
                externalId = "5114",
            ),
        )

        assertTrue(result is DuplicateMatch.Exact)
        assertEquals(existing.item.id, (result as DuplicateMatch.Exact).trackedMedia.item.id)
    }

    @Test
    fun legacyAniListMalIdInProviderJsonStillProducesExactMatch() {
        val existing = anime(
            title = "Different localized title",
            source = MetadataSource.AniList,
            externalId = "21",
            popularityJson = "{\"malId\":5114}",
        )
        val result = listOf(existing).findDuplicateFor(
            suggestion(
                title = "No title overlap",
                source = MetadataSource.Jikan,
                externalId = "5114",
            ),
        )

        assertTrue(result is DuplicateMatch.Exact)
    }

    @Test
    fun differentMalIdsAreNotExactEvenWhenTitlesMatch() {
        val existing = anime(
            title = "Shared title",
            source = MetadataSource.AniList,
            externalId = "100",
            malId = 1000,
            releaseYear = 2020,
        )
        val result = listOf(existing).findDuplicateFor(
            suggestion(
                title = "Shared title",
                source = MetadataSource.Jikan,
                externalId = "2000",
                releaseYear = 2020,
            ),
        )

        assertTrue(result is DuplicateMatch.Possible)
    }

    private fun anime(
        title: String,
        source: MetadataSource,
        externalId: String,
        malId: Int? = null,
        popularityJson: String? = null,
        releaseYear: Int? = null,
    ) = TrackedMedia(
        item = MediaItem(
            id = 1,
            type = MediaType.Anime,
            title = title,
            releaseYear = releaseYear,
            metadataSource = source,
            metadataExternalId = externalId,
            malId = malId,
            popularityJson = popularityJson,
        ),
        sessions = emptyList(),
    )

    private fun suggestion(
        title: String,
        source: MetadataSource,
        externalId: String,
        releaseYear: Int? = null,
    ) = MetadataSuggestion(
        source = source,
        externalId = externalId,
        mediaType = MediaType.Anime,
        title = title,
        releaseYear = releaseYear,
    )
}
