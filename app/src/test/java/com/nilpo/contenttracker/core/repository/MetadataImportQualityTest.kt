package com.nilpo.contenttracker.core.repository

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MetadataImportQualityTest {
    @Test
    fun `matching normalization preserves non-Latin scripts and folds accents`() {
        assertEquals("amelie", "Amélie".normalizedMetadataMatchText())
        assertEquals("進撃の巨人", "進撃の巨人".normalizedMetadataMatchText())
        assertEquals("война и мир", "Война и мир".normalizedMetadataMatchText())
        assertEquals(
            "cent anys de solitud",
            "Cent anys de solitud (edició il·lustrada)"
                .normalizedMetadataMatchText(removeParenthetical = true),
        )
    }

    @Test
    fun `AniList rating votes sum score distribution instead of popularity`() {
        val distribution = JSONArray(
            """[{"score":10,"amount":120},{"score":20,"amount":80},{"score":30,"amount":5}]""",
        )

        assertEquals(205, distribution.scoreDistributionVoteCount())
        assertEquals(null, JSONArray().scoreDistributionVoteCount())
    }

    @Test
    fun `metadata retry policy separates transient transport from deterministic bugs`() {
        assertTrue(IOException("offline").toMetadataFailureDetails().retryable)
        assertTrue(
            MetadataProviderHttpException(429, "https://provider.test", 90_000L)
                .toMetadataFailureDetails()
                .retryable,
        )
        assertTrue(
            MetadataProviderHttpException(503, "https://provider.test")
                .toMetadataFailureDetails()
                .retryable,
        )
        assertFalse(
            MetadataProviderHttpException(400, "https://provider.test")
                .toMetadataFailureDetails()
                .retryable,
        )
        assertFalse(IllegalStateException("bad invariant").toMetadataFailureDetails().retryable)
    }
}
