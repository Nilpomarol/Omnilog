package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Keeps an unknown terminal date out of period calculations without discarding the transition.
 *
 * Earlier versions used the save day for a terminal transition and its final progress increment
 * when the finish date was blank. The latest event on an undated terminal session is exactly that
 * manufactured date, so it is safely repaired to an unknown-date fact here.
 */
val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE session_status_events ADD COLUMN hasKnownDate INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            """
            UPDATE session_status_events
            SET hasKnownDate = 0
            WHERE status IN ('Completed', 'Dropped')
              AND EXISTS (
                  SELECT 1
                  FROM tracking_sessions session
                  WHERE session.id = session_status_events.sessionId
                    AND session.status = session_status_events.status
                    AND session.finishedAtEpochDay IS NULL
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM session_status_events later
                  WHERE later.sessionId = session_status_events.sessionId
                    AND (
                        later.createdAtEpochMillis > session_status_events.createdAtEpochMillis
                        OR (
                            later.createdAtEpochMillis = session_status_events.createdAtEpochMillis
                            AND later.id > session_status_events.id
                        )
                    )
              )
            """.trimIndent(),
        )
        db.execSQL(
            """
            UPDATE progress_updates
            SET hasKnownDate = 0
            WHERE EXISTS (
                SELECT 1
                FROM session_status_events event
                WHERE event.sessionId = progress_updates.sessionId
                  AND event.hasKnownDate = 0
                  AND event.createdAtEpochMillis = progress_updates.createdAtEpochMillis
            )
            """.trimIndent(),
        )
    }
}
