package com.nilpo.contenttracker.ui.imports

import com.nilpo.contenttracker.core.imports.ImportCompletionSummary
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.repository.ProviderImportResult
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
            "StoryGraph: metadades completades.",
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
            "StoryGraph: metadades completades · 1 per revisar · 1 incidència · 2 amb dades pendents.",
            summary.userFacingMessage(),
        )
    }

    @Test
    fun `import result leaves out every counter that is zero`() {
        val clean = ProviderImportResult(importedRows = 12, skippedDuplicateRows = 0, unsupportedRows = 0)
        val mixed = ProviderImportResult(importedRows = 1, skippedDuplicateRows = 3, unsupportedRows = 1, invalidRows = 1)

        assertEquals(
            "12 llibres importats. Les metadades es completen en segon pla.",
            clean.userFacingMessage(ImportSource.StoryGraphCsv),
        )
        assertEquals(
            "1 anime importat · 3 ja hi eren · 2 omesos. Les metadades es completen en segon pla.",
            mixed.userFacingMessage(ImportSource.MalXml),
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
