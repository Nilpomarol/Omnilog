package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Records the last MAL payload accepted for each queue row, so unchanged state is not resent. */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE mal_sync_queue ADD COLUMN lastSyncedPayloadHash TEXT")
    }
}
