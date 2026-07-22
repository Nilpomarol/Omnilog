package com.nilpo.contenttracker.core.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryGraphOwnershipImportTest {
    @Test
    fun `owned flag is imported without deriving a subtype from format`() {
        val items = parseStoryGraphCsv(
            """
            Title,Authors,Read Status,Format,Owned?
            Owned ebook,Author,read,ebook,yes
            Owned paperback,Author,read,paperback,yes
            Borrowed ebook,Author,read,ebook,no
            """.trimIndent(),
        ).map { it.toAddTrackedMediaRequest() }

        assertTrue(items[0].isOwned)
        assertTrue(items[1].isOwned)
        assertFalse(items[2].isOwned)
    }
}
