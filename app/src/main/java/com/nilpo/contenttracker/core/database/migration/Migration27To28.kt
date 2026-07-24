package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN imdbId TEXT")
        db.execSQL("ALTER TABLE media_items ADD COLUMN storyGraphId TEXT")

        // Preserve the visible provider identity first, then recover identities whose imported
        // items have already been enriched to another metadata provider.
        db.execSQL(
            "UPDATE media_items SET imdbId = LOWER(TRIM(metadataExternalId)) " +
                "WHERE metadataSource = 'Imdb' AND metadataExternalId IS NOT NULL",
        )
        db.execSQL(
            "UPDATE media_items SET storyGraphId = TRIM(metadataExternalId) " +
                "WHERE metadataSource = 'StoryGraph' AND metadataExternalId IS NOT NULL",
        )
        db.execSQL(
            "UPDATE media_items SET imdbId = (" +
                "SELECT LOWER(TRIM(import_batch_items.sourceExternalId)) " +
                "FROM import_batch_items INNER JOIN import_batches " +
                "ON import_batches.id = import_batch_items.batchId " +
                "WHERE import_batch_items.mediaItemId = media_items.id " +
                "AND import_batches.source = 'ImdbCsv' " +
                "AND import_batch_items.sourceExternalId IS NOT NULL " +
                "ORDER BY import_batches.id DESC LIMIT 1" +
                ") WHERE imdbId IS NULL",
        )
        db.execSQL(
            "UPDATE media_items SET storyGraphId = (" +
                "SELECT TRIM(import_batch_items.sourceExternalId) " +
                "FROM import_batch_items INNER JOIN import_batches " +
                "ON import_batches.id = import_batch_items.batchId " +
                "WHERE import_batch_items.mediaItemId = media_items.id " +
                "AND import_batches.source = 'StoryGraphCsv' " +
                "AND import_batch_items.sourceExternalId IS NOT NULL " +
                "ORDER BY import_batches.id DESC LIMIT 1" +
                ") WHERE storyGraphId IS NULL",
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_imdbId ON media_items(imdbId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_media_items_storyGraphId ON media_items(storyGraphId)",
        )
    }
}
