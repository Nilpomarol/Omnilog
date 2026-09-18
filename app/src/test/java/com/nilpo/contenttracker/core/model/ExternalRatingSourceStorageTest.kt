package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalRatingSourceStorageTest {
    @Test
    fun knownSourcesRoundTripByEnumName() {
        assertEquals("Imdb", ExternalRatingSource.Imdb.storedName("ignored"))
        assertEquals(ExternalRatingSource.Imdb to null, ExternalRatingSource.fromStored("Imdb"))
    }

    @Test
    fun customSitesKeepTheirTypedName() {
        val stored = ExternalRatingSource.Other.storedName("  Letterboxd ")
        assertEquals("Letterboxd", stored)
        assertEquals(ExternalRatingSource.Other to "Letterboxd", ExternalRatingSource.fromStored(stored!!))
    }

    @Test
    fun customSiteNeedsAName() {
        assertNull(ExternalRatingSource.Other.storedName("   "))
        assertNull(ExternalRatingSource.Other.storedName(null))
    }

    @Test
    fun theStoredWordOtherIsACustomSiteNamedOther() {
        // Never written by a known source, so it can only be a site the user called that.
        assertEquals(ExternalRatingSource.Other to "Other", ExternalRatingSource.fromStored("Other"))
    }
}
