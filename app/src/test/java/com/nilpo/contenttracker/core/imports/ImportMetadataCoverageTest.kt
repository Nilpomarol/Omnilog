package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.database.dao.ImportCoverageRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportMetadataCoverageTest {
    @Test
    fun `coverage reports useful missing fields after a successful match`() {
        val item = row(
            mediaType = "Book",
            releaseYear = null,
            progressTotal = null,
            coverUrl = null,
            synopsis = "",
            creatorsJson = "[]",
            genresJson = "[]",
        ).toCoverageItem()

        assertEquals(
            setOf(
                ImportMetadataGap.Cover,
                ImportMetadataGap.Synopsis,
                ImportMetadataGap.ReleaseYear,
                ImportMetadataGap.Creators,
                ImportMetadataGap.Genres,
                ImportMetadataGap.ProgressTotal,
            ),
            item?.missingFields,
        )
    }

    @Test
    fun `complete metadata does not create a coverage item`() {
        assertNull(row().toCoverageItem())
    }

    @Test
    fun `games do not require a provider progress total`() {
        assertNull(row(mediaType = "Game", progressTotal = null).toCoverageItem())
    }

    @Test
    fun `optional metadata gaps do not create an action item`() {
        assertNull(
            row(
                releaseYear = null,
                coverUrl = null,
                synopsis = null,
                creatorsJson = "[]",
                genresJson = "[]",
            ).toCoverageItem(),
        )
    }

    private fun row(
        mediaType: String = "Book",
        releaseYear: Int? = 2020,
        progressTotal: Int? = 320,
        coverUrl: String? = "https://example.test/cover.jpg",
        synopsis: String? = "Synopsis",
        creatorsJson: String? = "[\"Author\"]",
        genresJson: String? = "[\"Fiction\"]",
    ) = ImportCoverageRow(
        itemId = 1,
        batchId = 2,
        mediaItemId = 3,
        title = "Imported title",
        mediaType = mediaType,
        importSource = "StoryGraphCsv",
        releaseYear = releaseYear,
        progressTotal = progressTotal,
        coverUrl = coverUrl,
        synopsis = synopsis,
        creatorsJson = creatorsJson,
        genresJson = genresJson,
    )
}
