package com.nilpo.contenttracker.core.imports

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportBatchVisibilityTest {
    @Test
    fun `terminal batches leave the import action list`() {
        assertFalse(progress(ImportBatchState.Completed).hasRemainingImportAction())
        assertFalse(progress(ImportBatchState.Cancelled).hasRemainingImportAction())
    }

    @Test
    fun `completed batch remains visible while metadata coverage needs acknowledgement`() {
        assertTrue(progress(ImportBatchState.Completed, coverageGapCount = 1).hasRemainingImportAction())
        assertFalse(progress(ImportBatchState.Cancelled, coverageGapCount = 1).hasRemainingImportAction())
    }

    @Test
    fun `batches needing work or a user decision remain visible`() {
        assertTrue(progress(ImportBatchState.Enriching).hasRemainingImportAction())
        assertTrue(progress(ImportBatchState.Paused).hasRemainingImportAction())
        assertTrue(progress(ImportBatchState.CompletedWithIssues).hasRemainingImportAction())
    }

    @Test
    fun `only resolved terminal batches can be removed from history`() {
        assertTrue(progress(ImportBatchState.Completed).canDeleteFromHistory())
        assertTrue(progress(ImportBatchState.Cancelled).canDeleteFromHistory())
        assertFalse(
            progress(
                ImportBatchState.Completed,
                coverageGapCount = 1,
            ).canDeleteFromHistory(),
        )
        assertFalse(progress(ImportBatchState.CompletedWithIssues).canDeleteFromHistory())
        assertFalse(progress(ImportBatchState.Paused).canDeleteFromHistory())
        assertFalse(progress(ImportBatchState.Enriching).canDeleteFromHistory())
    }

    private fun progress(
        state: ImportBatchState,
        coverageGapCount: Int = 0,
    ) = ImportBatchProgress(
        batchId = 1,
        source = ImportSource.StoryGraphCsv,
        state = state,
        totalCount = 10,
        appliedCount = 10,
        needsReviewCount = 0,
        issueCount = 0,
        retryableIssueCount = 0,
        coverageGapCount = coverageGapCount,
        pendingCount = 0,
        cancelledCount = 0,
    )
}
