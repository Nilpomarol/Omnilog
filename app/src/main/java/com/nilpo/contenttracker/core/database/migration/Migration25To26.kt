package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE import_batch_items " +
                "ADD COLUMN coverageDismissed INTEGER NOT NULL DEFAULT 0",
        )
        // Do not surface historical imports as new work immediately after upgrading. Items
        // enriched again by this version reset the flag and receive a fresh coverage assessment.
        db.execSQL("UPDATE import_batch_items SET coverageDismissed = 1")
    }
}
