package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialMalMetadataTest {
    @Test
    fun `official MAL details preserve a Romanized original title and supply core metadata`() {
        val base = MetadataSuggestion(
            source = MetadataSource.Jikan,
            externalId = "5114",
            mediaType = MediaType.Anime,
            title = "Hagane no Renkinjutsushi",
            originalTitle = "Hagane no Renkinjutsushi: FULLMETAL ALCHEMIST",
        )
        val details = JSONObject(
            """
            {
              "id": 5114,
              "title": "Fullmetal Alchemist: Brotherhood",
              "main_picture": {"medium": "medium.jpg", "large": "large.jpg"},
              "alternative_titles": {
                "en": "Fullmetal Alchemist: Brotherhood",
                "ja": "鋼の錬金術師 FULLMETAL ALCHEMIST"
              },
              "start_date": "2009-04-05",
              "synopsis": "Two brothers search for the Philosopher's Stone.",
              "rank": 1,
              "num_list_users": 3500000,
              "num_episodes": 64,
              "genres": [{"id": 1, "name": "Action"}],
              "studios": [{"id": 4, "name": "Bones"}]
            }
            """.trimIndent(),
        )

        val result = details.toOfficialMalMetadataSuggestion(base)

        assertEquals("Fullmetal Alchemist: Brotherhood", result.title)
        assertEquals("Hagane no Renkinjutsushi: FULLMETAL ALCHEMIST", result.originalTitle)
        assertEquals(2009, result.releaseYear)
        assertEquals("large.jpg", result.coverUrl)
        assertEquals(64, result.progressTotal)
        assertEquals(listOf("Action"), result.genres)
        assertEquals(listOf("Bones"), result.creators)
        assertEquals("https://myanimelist.net/anime/5114", result.sourceUrl)
    }

    @Test
    fun `official MAL details fall back to MAL's Romanized title without a previous original title`() {
        val base = MetadataSuggestion(
            source = MetadataSource.Jikan,
            externalId = "16498",
            mediaType = MediaType.Anime,
            title = "Attack on Titan",
        )
        val details = JSONObject(
            """
            {
              "id": 16498,
              "title": "Shingeki no Kyojin",
              "alternative_titles": {"en": "Attack on Titan", "ja": "進撃の巨人"}
            }
            """.trimIndent(),
        )
        val noEnglish = JSONObject("""{"id": 16498, "title": "Shingeki no Kyojin"}""")

        assertEquals("Shingeki no Kyojin", details.toOfficialMalMetadataSuggestion(base).originalTitle)
        assertEquals(null, noEnglish.toOfficialMalMetadataSuggestion(base).originalTitle)
    }

    @Test
    fun `Jikan uses its Latin-script title as the original title`() {
        val result = JSONObject(
            """
            {
              "mal_id": 16498,
              "title": "Shingeki no Kyojin",
              "title_english": "Attack on Titan",
              "title_japanese": "進撃の巨人"
            }
            """.trimIndent(),
        ).toJikanMetadataSuggestion(
            MetadataSuggestion(
                source = MetadataSource.Jikan,
                externalId = "16498",
                mediaType = MediaType.Anime,
                title = "Attack on Titan",
            ),
        )

        assertEquals("Attack on Titan", result.title)
        assertEquals("Shingeki no Kyojin", result.originalTitle)
    }
}
