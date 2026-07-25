package com.nilpo.contenttracker.ui.detail

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCardPartsTest {
    private val now = LocalDate.of(2026, 7, 25)

    @Test
    fun currentYearDateOmitsYear() {
        val currentYearDate = LocalDate.of(2026, 7, 13)
        assertEquals("13 jul.", currentYearDate.formatSessionDate(now))
    }

    @Test
    fun previousYearDateIncludesYear() {
        val previousYearDate = LocalDate.of(2025, 7, 13)
        assertEquals("13 jul. 2025", previousYearDate.formatSessionDate(now))
    }
}
