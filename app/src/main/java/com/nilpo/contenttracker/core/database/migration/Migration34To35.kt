package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Replaces IGDB's cropped resized company-logo delivery with the original artwork. */
val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE media_credits " +
                "SET personImageUrl = REPLACE(personImageUrl, '/t_logo_med_2x/', '/t_original/') " +
                "WHERE personImageUrl LIKE '%images.igdb.com/%/t_logo_med_2x/%'",
        )
    }
}
