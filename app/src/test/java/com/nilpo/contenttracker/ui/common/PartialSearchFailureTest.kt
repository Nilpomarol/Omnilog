package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.core.model.MetadataSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the provider enumeration in the partial-failure message. The plural agreement around it
 * lives in `metadata_search_partial_error` and needs a device to verify.
 */
class PartialSearchFailureTest {
    /** Must match `list_conjunction` in strings.xml. */
    private val conjunction = "%1\$s i %2\$s"

    @Test
    fun namesASingleFailedProvider() {
        assertEquals(
            "TMDb",
            formatProviderEnumeration(listOf(MetadataSource.Tmdb), conjunction),
        )
    }

    @Test
    fun joinsTwoFailedProvidersWithTheConjunction() {
        assertEquals(
            "RAWG i TMDb",
            formatProviderEnumeration(listOf(MetadataSource.Tmdb, MetadataSource.Rawg), conjunction),
        )
    }

    @Test
    fun separatesThreeOrMoreWithCommasBeforeTheConjunction() {
        assertEquals(
            "Google Books, Open Library i TMDb",
            formatProviderEnumeration(
                listOf(MetadataSource.Tmdb, MetadataSource.GoogleBooks, MetadataSource.OpenLibrary),
                conjunction,
            ),
        )
    }

    @Test
    fun ordersNamesIndependentlyOfTheOrderProvidersFailedIn() {
        assertEquals(
            formatProviderEnumeration(listOf(MetadataSource.Rawg, MetadataSource.AniList), conjunction),
            formatProviderEnumeration(listOf(MetadataSource.AniList, MetadataSource.Rawg), conjunction),
        )
    }

    @Test
    fun rendersNothingWhenNoProviderFailed() {
        assertEquals("", formatProviderEnumeration(emptyList(), conjunction))
    }
}
