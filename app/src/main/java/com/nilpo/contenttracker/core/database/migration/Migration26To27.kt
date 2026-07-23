package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE import_batches " +
                "ADD COLUMN completionNotified INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "UPDATE import_batches SET completionNotified = 1 " +
                "WHERE state IN ('Completed', 'CompletedWithIssues', 'Cancelled')",
        )
    }
}
