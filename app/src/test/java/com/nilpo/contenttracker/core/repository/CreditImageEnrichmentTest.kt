package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditImageEnrichmentTest {
    @Test
    fun createsOpenLibraryAuthorPortraitUrlWithoutAPlaceholder() {
        assertEquals(
            "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false",
            openLibraryAuthorImageUrl(9285851L),
        )
    }

    @Test
    fun readsThePortraitAnAuthorRecordActuallyHas() {
        assertEquals(
            "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false",
            JSONObject("""{ "photos": [9285851, 1234] }""").openLibraryAuthorPortraitUrl(),
        )
    }

    @Test
    fun treatsAnAuthorWithNoUsablePhotoAsUnillustrated() {
        assertNull(JSONObject("""{ "photos": [] }""").openLibraryAuthorPortraitUrl())
        assertNull(JSONObject("""{ "photos": [-1] }""").openLibraryAuthorPortraitUrl())
        assertNull(JSONObject("""{ "name": "No photos field" }""").openLibraryAuthorPortraitUrl())
    }

    @Test
    fun skipsDeletedPhotoIdsInFavourOfTheFirstLivingOne() {
        assertEquals(
            "https://covers.openlibrary.org/a/id/777-L.jpg?default=false",
            JSONObject("""{ "photos": [-1, 777] }""").openLibraryAuthorPortraitUrl(),
        )
    }

    @Test
    fun pairsOpenLibraryWorkAuthorIdsWithStoredNamesDuringRefresh() {
        val credits = JSONObject(
            """
                {
                  "authors": [
                    { "author": { "key": "/authors/OL23919A" } },
                    { "author": { "key": "/authors/OL12345A" } }
                  ]
                }
            """.trimIndent(),
        ).toOpenLibraryWorkAuthorCredits(listOf("Ursula K. Le Guin", "Second Author"))

        assertEquals("Ursula K. Le Guin", credits[0].credit.personName)
        assertEquals("OL23919A", credits[0].authorKey)
        assertEquals("OL12345A", credits[1].authorKey)
    }

    /**
     * An OLID is not evidence of a photo, and most authors have none. Emitting a URL here is what
     * used to store a permanent 404 that then shadowed the author's real portrait elsewhere.
     */
    @Test
    fun neverGuessesAPortraitFromAnAuthorIdAlone() {
        val credits = JSONObject(
            """{ "authors": [ { "author": { "key": "/authors/OL23919A" } } ] }""",
        ).toOpenLibraryWorkAuthorCredits(listOf("Ursula K. Le Guin"))

        assertNull(credits.single().credit.personImageUrl)
    }

    @Test
    fun ignoresAuthorKeysThatAreNotOpenLibraryAuthorIds() {
        val credits = JSONObject(
            """
                {
                  "authors": [
                    { "author": { "key": "/works/OL123W" } },
                    { "author": { "key": "/authors/OL9A" } }
                  ]
                }
            """.trimIndent(),
        ).toOpenLibraryWorkAuthorCredits(listOf("Not An Author Key", "Real Author"))

        assertNull(credits[0].authorKey)
        assertEquals("OL9A", credits[1].authorKey)
    }

    @Test
    fun readsIgdbDeveloperAndPublisherLogos() {
        val credits = JSONObject(
            """
                {
                  "involved_companies": [
                    {
                      "developer": true,
                      "publisher": true,
                      "company": {
                        "name": "Supergiant Games",
                        "logo": {
                          "url": "//images.igdb.com/igdb/image/upload/t_thumb/logo.png",
                          "width": 600,
                          "height": 200
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
        ).toCompanyCredits()

        assertEquals(2, credits.size)
        assertEquals(MediaCreditRole.Developer, credits[0].roleType)
        assertEquals(MediaCreditRole.Publisher, credits[1].roleType)
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_original/logo.png",
            credits[0].personImageUrl,
        )
        assertEquals(3f, credits[0].personImageAspectRatio!!, 0.0001f)
    }

    /** IGDB returns `t_thumb` on a logo, so request the uncropped original asset. */
    @Test
    fun asksIgdbForALogoSizedForTheRowItIsDrawnIn() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_original/abc.jpg",
            "//images.igdb.com/igdb/image/upload/t_thumb/abc.jpg".toAbsoluteIgdbImageUrl(),
        )
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_original/abc.jpg",
            "http://images.igdb.com/igdb/image/upload/t_thumb/abc.jpg".toAbsoluteIgdbImageUrl(),
        )
    }

    @Test
    fun leavesAnIgdbUrlWithoutAThumbSegmentAlone() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_1080p/abc.jpg",
            "//images.igdb.com/igdb/image/upload/t_1080p/abc.jpg".toAbsoluteIgdbImageUrl(),
        )
    }

    @Test
    fun usesTheProvidersOriginalLogoDimensionsRatherThanItsResizedDeliveryCanvas() {
        assertEquals(3f, providerLogoAspectRatio(600, 200)!!, 0.0001f)
        assertEquals(0.5f, providerLogoAspectRatio(200, 400)!!, 0.0001f)
        assertNull(providerLogoAspectRatio(200, 0))
    }

    @Test
    fun preservesRawgCompanyNamesWhileAddingMatchingIgdbLogo() {
        val rawgCredits = listOf(
            MediaCredit(
                personName = "Supergiant Games",
                roleType = MediaCreditRole.Developer,
                sortOrder = 0,
            ),
        )
        val igdbCredits = listOf(
            MediaCredit(
                personName = "Supergiant Games",
                roleType = MediaCreditRole.Developer,
                personImageUrl = "https://example.com/logo.png",
                personImageAspectRatio = 3f,
            ),
            MediaCredit(
                personName = "Private Division",
                roleType = MediaCreditRole.Publisher,
                personImageUrl = "https://example.com/publisher.png",
            ),
        )

        val merged = mergeGameCompanyCredits(rawgCredits, igdbCredits)

        assertEquals(2, merged.size)
        assertEquals("https://example.com/logo.png", merged[0].personImageUrl)
        assertEquals(3f, merged[0].personImageAspectRatio!!, 0.0001f)
        assertEquals(MediaCreditRole.Publisher, merged[1].roleType)
        assertEquals(1, merged[1].sortOrder)
    }

    /** The cast carousel draws these at roughly 400x565px, so `w185` was a three-times upscale. */
    @Test
    fun asksTmdbForAProfileSizedForTheCastCarousel() {
        assertEquals(
            "https://image.tmdb.org/t/p/h632/profile.jpg",
            tmdbPersonImageUrl("/profile.jpg"),
        )
    }

    @Test
    fun leavesBlankIgdbImageUrlsAbsent() {
        assertNull(" ".toAbsoluteIgdbImageUrl())
        assertTrue("https://images.igdb.com/logo.png".toAbsoluteIgdbImageUrl()!!.startsWith("https://"))
    }

    @Test
    fun usesTmdbCompanyLogoOnlyForAnExactAniListStudioName() {
        val logos = JSONObject(
            """
                {
                  "production_companies": [
                    { "name": "Studio Bones", "logo_path": "/bones.png" },
                    { "name": "Bones Films", "logo_path": "/other.png" }
                  ]
                }
            """.trimIndent(),
        ).productionCompanyLogos().toMap()
        val credits = listOf(
            MediaCredit(personName = "Studio Bones", roleType = MediaCreditRole.Studio),
            MediaCredit(personName = "Bones", roleType = MediaCreditRole.Studio),
            MediaCredit(personName = "A Voice Actor", roleType = MediaCreditRole.VoiceActor),
        ).withMatchingCompanyLogos(logos)

        assertEquals("https://image.tmdb.org/t/p/w500/bones.png", credits[0].personImageUrl)
        assertNull(credits[1].personImageUrl)
        assertNull(credits[2].personImageUrl)
    }
}
