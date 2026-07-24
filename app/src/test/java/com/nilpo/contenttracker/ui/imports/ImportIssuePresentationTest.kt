package com.nilpo.contenttracker.ui.imports

import com.nilpo.contenttracker.core.imports.ImportIssueItem
import com.nilpo.contenttracker.core.imports.ImportItemState
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportIssuePresentationTest {
    @Test
    fun `only recoverable issue states offer a retry`() {
        assertTrue(issue(ImportItemState.NoMatch).canRetry)
        assertTrue(issue(ImportItemState.Unavailable).canRetry)
        assertTrue(issue(ImportItemState.Failed, retryable = true).canRetry)
        assertFalse(issue(ImportItemState.Failed, retryable = false).canRetry)
    }

    @Test
    fun `technical rate limit errors become a useful explanation`() {
        val issue = issue(
            state = ImportItemState.Failed,
            retryable = true,
            lastError = "Provider rate limit (HTTP 429)",
        )

        assertEquals(
            "El proveïdor ha limitat temporalment les consultes.",
            issue.userFacingReason(),
        )
    }

    @Test
    fun `permanent failures do not expose the raw provider error`() {
        val issue = issue(
            state = ImportItemState.Failed,
            retryable = false,
            lastError = "HTTP 403 https://provider.test/private-path",
        )

        assertEquals(
            "S'ha produït un error que no es pot reintentar automàticament.",
            issue.userFacingReason(),
        )
    }

    @Test
    fun `authorization failures explain the required user action`() {
        val issue = issue(
            state = ImportItemState.Unavailable,
            lastError = "Provider authorization failed (HTTP 401)",
        )

        assertEquals(
            "No s'ha pogut autenticar amb el proveïdor de metadades.",
            issue.userFacingReason(),
        )
    }

    private fun issue(
        state: ImportItemState,
        retryable: Boolean = false,
        lastError: String? = null,
    ) = ImportIssueItem(
        itemId = 1,
        batchId = 2,
        mediaItemId = 3,
        title = "Imported title",
        mediaType = MediaType.Book,
        importSource = ImportSource.StoryGraphCsv,
        state = state,
        retryable = retryable,
        attemptCount = 3,
        lastError = lastError,
    )
}
