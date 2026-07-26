package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Repairs contributor images that were stored wrong rather than waiting for a refresh to reach them.
 *
 * Two separate faults, both invisible until the credit is drawn. Open Library portraits were
 * addressed by OLID without ever confirming a photo existed, so most of them are permanent 404s
 * that still read as "this author is illustrated" and shadow the real portrait the same author has
 * on another book — those are cleared, and the provider now stores photo-ID URLs it has verified.
 * The rest are simply too small for the sizes they are drawn at, and the size is a path segment.
 */
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE media_credits SET personImageUrl = NULL " +
                "WHERE personImageUrl LIKE 'https://covers.openlibrary.org/a/olid/%'",
        )
        db.execSQL(
            "UPDATE media_credits " +
                "SET personImageUrl = REPLACE(personImageUrl, '/t_thumb/', '/t_logo_med_2x/') " +
                "WHERE personImageUrl LIKE '%images.igdb.com/%/t_thumb/%'",
        )
        db.execSQL(
            "UPDATE media_credits " +
                "SET personImageUrl = REPLACE(personImageUrl, '/t/p/w185/', '/t/p/h632/') " +
                "WHERE personImageUrl LIKE 'https://image.tmdb.org/t/p/w185/%'",
        )
    }
}
