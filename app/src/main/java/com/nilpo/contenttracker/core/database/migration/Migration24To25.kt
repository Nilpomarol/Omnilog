package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Keeps provider/user tags separate from factual media genres. */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN tagsJson TEXT")
        // Before this schema, raw MAL/StoryGraph tags were temporarily stored as genres. We can
        // safely recover only imports that have not yet received factual provider metadata.
        db.execSQL(
            """
            UPDATE media_items
            SET tagsJson = genresJson, genresJson = NULL
            WHERE metadataSource IN ('Jikan', 'StoryGraph')
              AND metadataLastFetchedAtEpochMillis IS NULL
              AND genresJson IS NOT NULL
            """.trimIndent(),
        )
    }
}
