package com.nilpo.contenttracker.core.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ratings changed unit between backup formats, and a backup carries no unit of its own — only the
 * schema version says which one it is. Reading an older backup as if it already held half points
 * would halve every score in the library on restore.
 */
class BackupRatingUnitTest {
    @Test
    fun `an older backup's whole points are read as half points`() {
        assertEquals(16, session(rating = 8, schemaVersion = 12).ratingHalfPoints)
        assertEquals(2, session(rating = 1, schemaVersion = 1).ratingHalfPoints)
    }

    @Test
    fun `a half-point backup is read as it was written`() {
        assertEquals(15, session(rating = 15, schemaVersion = FirstHalfPointBackupSchemaVersion).ratingHalfPoints)
        assertEquals(16, session(rating = 16, schemaVersion = FirstHalfPointBackupSchemaVersion).ratingHalfPoints)
    }

    @Test
    fun `an unrated session stays unrated in either format`() {
        assertNull(session(rating = null, schemaVersion = 12).ratingHalfPoints)
        assertNull(session(rating = null, schemaVersion = FirstHalfPointBackupSchemaVersion).ratingHalfPoints)
    }

    private fun session(rating: Int?, schemaVersion: Int) = JSONObject()
        .put("id", 1)
        .put("mediaItemId", 1)
        .put("sessionNumber", 1)
        .put("status", "Completed")
        .apply { rating?.let { put("rating", it) } }
        .toTrackingSessionEntity(schemaVersion)
}
