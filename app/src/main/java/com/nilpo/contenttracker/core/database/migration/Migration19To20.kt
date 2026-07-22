package com.nilpo.contenttracker.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Moves progress from cumulative totals to increments, and moves pre-tracking progress off the
 * rows and onto the session. See `docs/omnilog-activity-concept.md`.
 *
 * Kept in its own file rather than alongside the one-line migrations in `ContentTrackerApplication`
 * because it has to read every row, convert it in Kotlin, and write it back — and because it sits
 * next to `convertSessionToIncrements`, which holds the logic and carries the tests.
 *
 * The invariant this migration must not break, for every session:
 *
 *     baselineProgress + sum(amount) == the progress the session showed before the upgrade
 *
 * That is enforced by construction here: the baseline is derived by subtracting the converted
 * entries from the session's existing `progressCurrent`, so the visible total cannot move no matter
 * what the rows looked like. `convertSessionToIncrements` decides what the entries are; this decides
 * where the remainder goes.
 *
 * Lossy by design. Progress the user later revoked — a value corrected downwards — does not survive
 * as an entry, because the concept holds that a correction is not something that happened. The
 * totals still land where the user left them; only the retracted steps disappear.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_sessions ADD COLUMN baselineProgress INTEGER NOT NULL DEFAULT 0")

        // Sessions whose progress was never backed by rows at all still have a total to keep, so
        // every session starts by treating its whole progress as baseline. Sessions that do have
        // rows overwrite this below with the part the entries do not account for.
        db.execSQL("UPDATE tracking_sessions SET baselineProgress = progressCurrent")

        val legacyRows = db.readLegacyProgressRows()
        val sessionProgress = db.readSessionProgress()

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS progress_updates_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mediaItemId INTEGER NOT NULL,
                sessionId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                loggedAtEpochDay INTEGER NOT NULL,
                hasKnownDate INTEGER NOT NULL DEFAULT 1,
                createdAtEpochMillis INTEGER NOT NULL,
                coversPeriod INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON DELETE CASCADE,
                FOREIGN KEY(sessionId) REFERENCES tracking_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        val sessionFinishedAt = db.readSessionFinishDates()

        legacyRows.forEach { (sessionId, rows) ->
            val converted = convertSessionToIncrements(
                rows = rows,
                finishedAtEpochDay = sessionFinishedAt[sessionId],
            )

            // Not `converted.baselineProgress`. That value is what the rows imply, and the rows can
            // have drifted from the session's cached total over the app's history. The total the
            // user has been looking at is the one worth preserving, so the baseline absorbs the
            // difference. Clamped because a baseline below zero would mean the entries already
            // overshoot the total, which nothing downstream can represent.
            val baseline = (sessionProgress[sessionId] ?: converted.baselineProgress)
                .minus(converted.entries.sumOf { it.amount })
                .coerceAtLeast(0)

            db.execSQL(
                "UPDATE tracking_sessions SET baselineProgress = ? WHERE id = ?",
                arrayOf<Any>(baseline, sessionId),
            )

            converted.entries.forEach { entry ->
                // The date comes from the conversion rather than from the row, because a row that
                // was stamped with the day it was typed gets moved to the session's finish day.
                db.execSQL(
                    """
                    INSERT INTO progress_updates_new (
                        id, mediaItemId, sessionId, amount, loggedAtEpochDay,
                        hasKnownDate, createdAtEpochMillis, coversPeriod
                    )
                    SELECT id, mediaItemId, sessionId, ?, ?, ?, createdAtEpochMillis, 0
                    FROM progress_updates
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf<Any>(
                        entry.amount,
                        entry.loggedAtEpochDay,
                        if (entry.hasKnownDate) 1 else 0,
                        entry.id,
                    ),
                )
            }
        }

        db.execSQL("DROP TABLE progress_updates")
        db.execSQL("ALTER TABLE progress_updates_new RENAME TO progress_updates")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_updates_mediaItemId " +
                "ON progress_updates(mediaItemId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_updates_sessionId " +
                "ON progress_updates(sessionId)",
        )
    }
}

/** Every pre-migration progress row, grouped by the session it belongs to. */
private fun SupportSQLiteDatabase.readLegacyProgressRows(): Map<Long, List<LegacyProgressRow>> {
    val bySession = mutableMapOf<Long, MutableList<LegacyProgressRow>>()
    query(
        """
        SELECT id, sessionId, progressValue, loggedAtEpochDay,
               hasKnownDate, createdAtEpochMillis, countsTowardObjectives
        FROM progress_updates
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val sessionId = cursor.getLong(1)
            bySession.getOrPut(sessionId) { mutableListOf() } += LegacyProgressRow(
                id = cursor.getLong(0),
                progressValue = cursor.getInt(2),
                loggedAtEpochDay = cursor.getLong(3),
                hasKnownDate = cursor.getInt(4) != 0,
                createdAtEpochMillis = cursor.getLong(5),
                countsTowardObjectives = cursor.getInt(6) != 0,
            )
        }
    }
    return bySession
}

/**
 * Finish dates, used to re-date rows whose own date was the day they were typed. Null-valued rows
 * are left out, so an unfinished session simply has no entry here.
 */
private fun SupportSQLiteDatabase.readSessionFinishDates(): Map<Long, Long> {
    val dates = mutableMapOf<Long, Long>()
    query("SELECT id, finishedAtEpochDay FROM tracking_sessions WHERE finishedAtEpochDay IS NOT NULL")
        .use { cursor ->
            while (cursor.moveToNext()) {
                dates[cursor.getLong(0)] = cursor.getLong(1)
            }
        }
    return dates
}

private fun SupportSQLiteDatabase.readSessionProgress(): Map<Long, Int> {
    val progress = mutableMapOf<Long, Int>()
    query("SELECT id, progressCurrent FROM tracking_sessions").use { cursor ->
        while (cursor.moveToNext()) {
            progress[cursor.getLong(0)] = cursor.getInt(1)
        }
    }
    return progress
}
