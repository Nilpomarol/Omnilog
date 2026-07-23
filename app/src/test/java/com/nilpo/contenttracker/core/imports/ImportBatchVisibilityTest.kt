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
    fun `batches needing work or a user decision remain visible`() {
        assertTrue(progress(ImportBatchState.Enriching).hasRemainingImportAction())
        assertTrue(progress(ImportBatchState.Paused).hasRemainingImportAction())
        assertTrue(progress(ImportBatchState.CompletedWithIssues).hasRemainingImportAction())
    }

    private fun progress(state: ImportBatchState) = ImportBatchProgress(
        batchId = 1,
        source = ImportSource.StoryGraphCsv,
        state = state,
        totalCount = 10,
        appliedCount = 10,
        needsReviewCount = 0,
        issueCount = 0,
        pendingCount = 0,
        cancelledCount = 0,
    )
}
