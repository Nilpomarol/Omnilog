package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Moves personal ratings from whole points to half points, so a rating can hold 7,5.
 *
 * The column keeps its name and type; only the unit changes, which is why this migration exists at
 * all. Doubling is exact in integers, so every existing rating survives as the same score.
 */
val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE tracking_sessions SET rating = rating * 2 WHERE rating IS NOT NULL")
    }
}
