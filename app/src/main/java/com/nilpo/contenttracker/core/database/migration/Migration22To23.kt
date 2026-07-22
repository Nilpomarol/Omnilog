package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the stable MAL identity and the durable app-to-MAL synchronization queue. */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN malId INTEGER")
        db.execSQL(
            """
            UPDATE media_items
            SET malId = CAST(metadataExternalId AS INTEGER)
            WHERE type = 'Anime'
              AND metadataSource = 'Jikan'
              AND metadataExternalId IS NOT NULL
              AND metadataExternalId != ''
              AND metadataExternalId NOT GLOB '*[^0-9]*'
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mal_sync_queue (
                mediaItemId INTEGER NOT NULL,
                malId INTEGER NOT NULL,
                state TEXT NOT NULL,
                attemptCount INTEGER NOT NULL,
                lastError TEXT,
                updatedAtEpochMillis INTEGER NOT NULL,
                lastAttemptAtEpochMillis INTEGER,
                lastSuccessAtEpochMillis INTEGER,
                PRIMARY KEY(mediaItemId),
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mal_sync_queue_state ON mal_sync_queue(state)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_mal_sync_queue_updatedAtEpochMillis " +
                "ON mal_sync_queue(updatedAtEpochMillis)",
        )
    }
}
