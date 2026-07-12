package com.nilpo.contenttracker.ui

import com.nilpo.contenttracker.ui.home.MediaSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailNavigationHistoryTest {
    @Test
    fun popReturnsMostRecentDetailAndPreservesEarlierEntries() {
        val history = emptyList<DetailHistoryEntry>()
            .pushDetail(
                mediaItemId = 101L,
                section = MediaSection.Anime,
                returnTarget = DetailReturnTarget.Section,
            )
            .pushDetail(
                mediaItemId = 202L,
                section = MediaSection.Books,
                returnTarget = DetailReturnTarget.Section,
            )

        val firstPop = history.popDetail()
        assertEquals(202L, firstPop.previous?.mediaItemId)
        assertEquals(MediaSection.Books, firstPop.previous?.section)
        assertEquals(101L, firstPop.remaining.single().mediaItemId)

        val secondPop = firstPop.remaining.popDetail()
        assertEquals(101L, secondPop.previous?.mediaItemId)
        assertNull(secondPop.remaining.singleOrNull())

        val emptyPop = emptyList<DetailHistoryEntry>().popDetail()
        assertNull(emptyPop.previous)
        assertEquals(emptyList<DetailHistoryEntry>(), emptyPop.remaining)
    }
}
