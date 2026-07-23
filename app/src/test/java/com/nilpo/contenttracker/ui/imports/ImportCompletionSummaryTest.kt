package com.nilpo.contenttracker.ui.imports

import com.nilpo.contenttracker.core.imports.ImportCompletionSummary
import com.nilpo.contenttracker.core.imports.ImportSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCompletionSummaryTest {
    @Test
    fun `clean completion needs no follow-up action`() {
        val summary = summary(applied = 10)

        assertFalse(summary.needsAttention)
        assertEquals(
            "Enriquiment de StoryGraph completat: 10/10 processats · 10 enriquits.",
            summary.userFacingMessage(),
        )
    }

    @Test
    fun `completion lists every follow-up category`() {
        val summary = summary(
            applied = 6,
            review = 1,
            issues = 1,
            coverage = 2,
        )

        assertTrue(summary.needsAttention)
        assertEquals(
            "Enriquiment de StoryGraph completat: 10/10 processats · 6 enriquits" +
                " · 1 per revisar · 1 incidència · 2 amb camps buits.",
            summary.userFacingMessage(),
        )
    }

    private fun summary(
        applied: Int,
        review: Int = 0,
        issues: Int = 0,
        coverage: Int = 0,
    ) = ImportCompletionSummary(
        batchId = 1,
        source = ImportSource.StoryGraphCsv,
        totalCount = 10,
        processedCount = 10,
        appliedCount = applied,
        needsReviewCount = review,
        issueCount = issues,
        coverageGapCount = coverage,
    )
}
