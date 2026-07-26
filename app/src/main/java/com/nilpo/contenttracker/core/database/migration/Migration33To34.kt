package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds durable, user-started provider metadata refresh runs. */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS metadata_refresh_runs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "state TEXT NOT NULL, totalCount INTEGER NOT NULL, " +
                "createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, " +
                "completedAtEpochMillis INTEGER)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_refresh_runs_state ON metadata_refresh_runs (state)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_metadata_refresh_runs_updatedAtEpochMillis " +
                "ON metadata_refresh_runs (updatedAtEpochMillis)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS metadata_refresh_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, runId INTEGER NOT NULL, " +
                "mediaItemId INTEGER, state TEXT NOT NULL, attemptCount INTEGER NOT NULL, " +
                "retryable INTEGER NOT NULL, lastError TEXT, updatedAtEpochMillis INTEGER NOT NULL, " +
                "completedAtEpochMillis INTEGER, " +
                "FOREIGN KEY(runId) REFERENCES metadata_refresh_runs(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE SET NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_metadata_refresh_items_runId_mediaItemId " +
                "ON metadata_refresh_items (runId, mediaItemId)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_refresh_items_runId ON metadata_refresh_items (runId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_refresh_items_mediaItemId ON metadata_refresh_items (mediaItemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_refresh_items_state ON metadata_refresh_items (state)")
    }
}
