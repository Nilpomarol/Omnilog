package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Makes activity ownership unambiguous and repairs cached session totals left inconsistent by older
 * metadata-total clamping. Activity is canonical: missing history becomes baseline, while a stale
 * cache below recorded activity is rebuilt from the baseline and entries without deleting either.
 */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_tracking_sessions_id_mediaItemId " +
                "ON tracking_sessions(id, mediaItemId)",
        )

        db.execSQL(
            """
            CREATE TABLE progress_updates_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mediaItemId INTEGER NOT NULL,
                sessionId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                loggedAtEpochDay INTEGER NOT NULL,
                hasKnownDate INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                coversPeriod INTEGER NOT NULL,
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(sessionId, mediaItemId) REFERENCES tracking_sessions(id, mediaItemId)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        // The session owns the media id. Taking it from there also repairs any legacy mismatch.
        db.execSQL(
            """
            INSERT INTO progress_updates_new (
                id, mediaItemId, sessionId, amount, loggedAtEpochDay,
                hasKnownDate, createdAtEpochMillis, coversPeriod
            )
            SELECT p.id, s.mediaItemId, p.sessionId, p.amount, p.loggedAtEpochDay,
                   p.hasKnownDate, p.createdAtEpochMillis, p.coversPeriod
            FROM progress_updates p
            INNER JOIN tracking_sessions s ON s.id = p.sessionId
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE progress_updates")
        db.execSQL("ALTER TABLE progress_updates_new RENAME TO progress_updates")
        db.execSQL("CREATE INDEX index_progress_updates_mediaItemId ON progress_updates(mediaItemId)")
        db.execSQL("CREATE INDEX index_progress_updates_sessionId ON progress_updates(sessionId)")
        db.execSQL(
            "CREATE INDEX index_progress_updates_sessionId_mediaItemId " +
                "ON progress_updates(sessionId, mediaItemId)",
        )

        db.execSQL(
            """
            CREATE TABLE session_status_events_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mediaItemId INTEGER NOT NULL,
                sessionId INTEGER NOT NULL,
                previousStatus TEXT,
                status TEXT NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                occurredOnEpochDay INTEGER,
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(sessionId, mediaItemId) REFERENCES tracking_sessions(id, mediaItemId)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO session_status_events_new (
                id, mediaItemId, sessionId, previousStatus, status,
                createdAtEpochMillis, occurredOnEpochDay
            )
            SELECT e.id, s.mediaItemId, e.sessionId, NULL, e.status,
                   e.createdAtEpochMillis, e.occurredOnEpochDay
            FROM session_status_events e
            INNER JOIN tracking_sessions s ON s.id = e.sessionId
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE session_status_events")
        db.execSQL("ALTER TABLE session_status_events_new RENAME TO session_status_events")
        db.execSQL("CREATE INDEX index_session_status_events_mediaItemId ON session_status_events(mediaItemId)")
        db.execSQL("CREATE INDEX index_session_status_events_sessionId ON session_status_events(sessionId)")
        db.execSQL(
            "CREATE INDEX index_session_status_events_sessionId_mediaItemId " +
                "ON session_status_events(sessionId, mediaItemId)",
        )

        db.repairSessionProgressInvariants()
    }
}

private data class StoredSessionProgress(
    val id: Long,
    val progressCurrent: Int,
    val baselineProgress: Int,
)

private fun SupportSQLiteDatabase.repairSessionProgressInvariants() {
    val sessions = mutableListOf<StoredSessionProgress>()
    query("SELECT id, progressCurrent, baselineProgress FROM tracking_sessions").use { cursor ->
        while (cursor.moveToNext()) {
            sessions += StoredSessionProgress(
                id = cursor.getLong(0),
                progressCurrent = cursor.getInt(1).coerceAtLeast(0),
                baselineProgress = cursor.getInt(2).coerceAtLeast(0),
            )
        }
    }

    sessions.forEach { session ->
        val entryTotal = query(
            "SELECT COALESCE(SUM(amount), 0) FROM progress_updates WHERE sessionId = ?",
            arrayOf(session.id),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        var baseline = session.baselineProgress
        val derived = baseline + entryTotal
        if (derived < session.progressCurrent) baseline += session.progressCurrent - derived
        val repairedCurrent = maxOf(session.progressCurrent, baseline + entryTotal)
        execSQL(
            "UPDATE tracking_sessions SET baselineProgress = ?, progressCurrent = ? WHERE id = ?",
            arrayOf<Any>(baseline, repairedCurrent, session.id),
        )
    }
}
