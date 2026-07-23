package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialMalMetadataTest {
    @Test
    fun `official MAL details supply English title Japanese original and core metadata`() {
        val base = MetadataSuggestion(
            source = MetadataSource.Jikan,
            externalId = "5114",
            mediaType = MediaType.Anime,
            title = "Hagane no Renkinjutsushi",
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
        assertEquals("鋼の錬金術師 FULLMETAL ALCHEMIST", result.originalTitle)
        assertEquals(2009, result.releaseYear)
        assertEquals("large.jpg", result.coverUrl)
        assertEquals(64, result.progressTotal)
        assertEquals(listOf("Action"), result.genres)
        assertEquals(listOf("Bones"), result.creators)
        assertEquals("https://myanimelist.net/anime/5114", result.sourceUrl)
    }
}
