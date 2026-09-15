package com.nilpo.contenttracker.ui.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditionFiltersTest {

    @Test
    fun providerFormatsFallIntoTheirCategory() {
        assertEquals(EditionFormat.Paperback, editionFormatOf("Paperback"))
        assertEquals(EditionFormat.Paperback, editionFormatOf("Mass Market Paperback"))
        assertEquals(EditionFormat.Hardcover, editionFormatOf("Hardcover"))
        assertEquals(EditionFormat.Audiobook, editionFormatOf("Audio CD"))
        assertEquals(EditionFormat.Digital, editionFormatOf("Kindle Edition"))
        assertEquals(EditionFormat.Digital, editionFormatOf("ebook"))
    }

    @Test
    fun unknownOrMissingFormatsStayUncategorised() {
        assertNull(editionFormatOf(null))
        assertNull(editionFormatOf("Unknown Binding"))
    }

    @Test
    fun catalogueLanguageCodesBecomeLocaleTags() {
        assertEquals("fr", catalogLanguageTag("fre"))
        assertEquals("ro", catalogLanguageTag("RUM"))
        assertEquals("he", catalogLanguageTag("heb"))
        assertEquals("xyz", catalogLanguageTag("xyz"))
    }
}
