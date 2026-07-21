package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.normalizeSteamAppId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SteamMetadataHelpersTest {
    @Test
    fun readsSteamAppIdFromCurrentRawgStoreShape() {
        assertEquals(
            "1145360",
            steamAppIdForStore(
                storeId = 1,
                nestedStoreId = null,
                url = "https://store.steampowered.com/app/1145360/",
            ),
        )
    }

    @Test
    fun retainsCompatibilityWithNestedRawgStoreShape() {
        assertEquals(
            "1145360",
            steamAppIdForStore(
                storeId = 0,
                nestedStoreId = 1,
                url = "https://store.steampowered.com/app/1145360/Hades/",
            ),
        )
    }

    @Test
    fun ignoresNonSteamStoresAndMalformedUrls() {
        assertNull(
            steamAppIdForStore(
                storeId = 6,
                nestedStoreId = null,
                url = "https://www.nintendo.com/games/detail/hades-switch/",
            ),
        )
        assertNull(steamAppIdForStore(storeId = 1, nestedStoreId = null, url = "not-an-app-url"))
    }

    @Test
    fun validatesManualSteamIds() {
        assertEquals("1145360", normalizeSteamAppId(" 1145360 "))
        assertNull(normalizeSteamAppId(""))
        assertNull(normalizeSteamAppId("not-an-id"))
        assertNull(normalizeSteamAppId("000"))
    }

    @Test
    fun constructsPortraitCoverAndParsesLocalizedReleaseYear() {
        assertEquals(
            "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1145360/library_600x900.jpg",
            steamPortraitCoverUrl("1145360"),
        )
        assertEquals(2020, steamReleaseYear("17 Sep, 2020"))
        assertEquals(2020, steamReleaseYear("17 de setembre de 2020"))
        assertNull(steamReleaseYear("Coming soon"))
    }

    @Test
    fun keepsSteamDescriptionOrFallsBackToItsReturnedScore() {
        assertEquals("Very Positive", steamReviewScoreDescriptor("Very Positive", 8))
        assertEquals("Overwhelmingly Positive", steamReviewScoreDescriptor("", 9))
    }

    @Test
    fun freshProviderRatingsReplaceStoredValuesWithoutDroppingOtherSources() {
        val existing = listOf(
            rating(ExternalRatingSource.Rawg, 3.0, 5.0),
            rating(ExternalRatingSource.Steam, 90.0, 100.0),
        )
        val fresh = listOf(
            rating(ExternalRatingSource.Steam, 98.0, 100.0),
            rating(ExternalRatingSource.Metacritic, 93.0, 100.0),
            rating(ExternalRatingSource.Rawg, 4.0, 5.0),
        )

        assertEquals(
            listOf(
                rating(ExternalRatingSource.Steam, 98.0, 100.0),
                rating(ExternalRatingSource.Metacritic, 93.0, 100.0),
                rating(ExternalRatingSource.Rawg, 4.0, 5.0),
            ),
            mergeFreshExternalRatings(fresh, existing),
        )
    }

    @Test
    fun gamesPreferSteamAsTheirPrimaryExternalRating() {
        val ratings = listOf(
            rating(ExternalRatingSource.Rawg, 4.0, 5.0),
            rating(ExternalRatingSource.Metacritic, 93.0, 100.0),
            rating(ExternalRatingSource.Steam, 98.0, 100.0),
        )

        assertEquals(
            MetadataRatingSuggestion(score = 98.0, maxScore = 100.0),
            MediaType.Game.preferredPrimaryExternalRating(
                ratings = ratings,
                fallback = MetadataRatingSuggestion(score = 93.0, maxScore = 100.0),
            ),
        )
    }

    @Test
    fun metadataRefreshDetectsANewSteamValuationEvenWhenThePercentageIsUnchanged() {
        val existing = listOf(rating(ExternalRatingSource.Steam, 98.0, 100.0))
        val refreshed = listOf(
            rating(
                source = ExternalRatingSource.Steam,
                score = 98.0,
                maxScore = 100.0,
                scoreDescriptor = "Overwhelmingly Positive",
            ),
        )

        assertNotEquals(existing.ratingSummary(), refreshed.ratingSummary())
    }

    private fun rating(
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        scoreDescriptor: String? = null,
    ) = MetadataExternalRatingSuggestion(
        source = source,
        score = score,
        maxScore = maxScore,
        scoreDescriptor = scoreDescriptor,
    )
}
