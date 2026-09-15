package com.nilpo.contenttracker.ui.home

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRhythmCompactCountTest {
    private val catalan = Locale("ca")

    @Test
    fun shortensThousandsAndRoundsDown() {
        assertEquals("999", compactCount(999, catalan))
        assertEquals("7k", compactCount(7000, catalan))
        assertEquals("8,5k", compactCount(8564, catalan))
        assertEquals("8,1k", compactCount(8100, catalan))
        assertEquals("9,9k", compactCount(9999, catalan))
        assertEquals("12k", compactCount(12_900, catalan))
    }
}
