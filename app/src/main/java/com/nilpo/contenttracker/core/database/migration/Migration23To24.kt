package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds durable provider-import batches and seeds unfinished enrichment for earlier imports. */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS import_batches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                source TEXT NOT NULL,
                state TEXT NOT NULL,
                totalCount INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                completedAtEpochMillis INTEGER,
                continuationUrl TEXT,
                diagnostic TEXT,
                reconnectRequired INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_import_batches_state ON import_batches(state)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_import_batches_updatedAtEpochMillis " +
                "ON import_batches(updatedAtEpochMillis)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS import_batch_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                batchId INTEGER NOT NULL,
                sourceKey TEXT NOT NULL,
                sourceExternalId TEXT,
                normalizedPayloadJson TEXT,
                mediaItemId INTEGER,
                state TEXT NOT NULL,
                providerSource TEXT,
                providerExternalId TEXT,
                matchKind TEXT,
                candidateReferencesJson TEXT,
                attemptCount INTEGER NOT NULL,
                retryable INTEGER NOT NULL,
                lastError TEXT,
                updatedAtEpochMillis INTEGER NOT NULL,
                lastAttemptAtEpochMillis INTEGER,
                completedAtEpochMillis INTEGER,
                FOREIGN KEY(batchId) REFERENCES import_batches(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_import_batch_items_batchId_sourceKey " +
                "ON import_batch_items(batchId, sourceKey)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_import_batch_items_batchId ON import_batch_items(batchId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_import_batch_items_mediaItemId ON import_batch_items(mediaItemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_import_batch_items_state ON import_batch_items(state)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_import_batch_items_updatedAtEpochMillis " +
                "ON import_batch_items(updatedAtEpochMillis)",
        )

        seedEarlierImports(db, source = "MalApi", metadataSource = "Jikan")
        seedEarlierImports(db, source = "ImdbCsv", metadataSource = "Imdb")
        seedEarlierImports(db, source = "StoryGraphCsv", metadataSource = "StoryGraph")
    }

    private fun seedEarlierImports(
        db: SupportSQLiteDatabase,
        source: String,
        metadataSource: String,
    ) {
        db.execSQL(
            """
            INSERT INTO import_batches(
                source, state, totalCount, createdAtEpochMillis, updatedAtEpochMillis, reconnectRequired
            )
            SELECT '$source', 'Enriching', COUNT(*),
                   CAST(strftime('%s','now') AS INTEGER) * 1000,
                   CAST(strftime('%s','now') AS INTEGER) * 1000,
                   0
            FROM media_items
            WHERE metadataSource = '$metadataSource'
            HAVING COUNT(*) > 0
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO import_batch_items(
                batchId, sourceKey, sourceExternalId, mediaItemId, state,
                attemptCount, retryable, updatedAtEpochMillis
            )
            SELECT b.id,
                   '$metadataSource:' || m.id,
                   CASE WHEN '$metadataSource' = 'Jikan' THEN CAST(m.malId AS TEXT) ELSE m.metadataExternalId END,
                   m.id,
                   'Pending',
                   0,
                   0,
                   CAST(strftime('%s','now') AS INTEGER) * 1000
            FROM media_items m
            JOIN import_batches b ON b.id = (
                SELECT id FROM import_batches WHERE source = '$source' ORDER BY id DESC LIMIT 1
            )
            WHERE m.metadataSource = '$metadataSource'
            """.trimIndent(),
        )
    }
}
