package com.nilpo.contenttracker.core.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration36To37Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion36Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(36) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE tracking_sessions (" +
                                "id INTEGER NOT NULL PRIMARY KEY, status TEXT NOT NULL, finishedAtEpochDay INTEGER)",
                        )
                        db.execSQL(
                            "CREATE TABLE session_status_events (" +
                                "id INTEGER NOT NULL PRIMARY KEY, sessionId INTEGER NOT NULL, status TEXT NOT NULL, " +
                                "createdAtEpochMillis INTEGER NOT NULL)",
                        )
                        db.execSQL(
                            "CREATE TABLE progress_updates (" +
                                "id INTEGER NOT NULL PRIMARY KEY, sessionId INTEGER NOT NULL, " +
                                "createdAtEpochMillis INTEGER NOT NULL, hasKnownDate INTEGER NOT NULL)",
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
    }

    @After
    fun closeDatabase() {
        helper.close()
    }

    @Test
    fun repairsOnlyTheLatestUndatedTerminalEventAndItsFinalIncrement() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO tracking_sessions VALUES (1, 'Completed', NULL), (2, 'Completed', 20500)")
        db.execSQL(
            "INSERT INTO session_status_events VALUES " +
                "(1, 1, 'Paused', 10), (2, 1, 'Completed', 20), (3, 2, 'Completed', 30)",
        )
        db.execSQL("INSERT INTO progress_updates VALUES (1, 1, 20, 1), (2, 2, 30, 1)")

        MIGRATION_36_37.migrate(db)

        db.query("SELECT id, hasKnownDate FROM session_status_events ORDER BY id").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(1))
            check(cursor.moveToNext())
            assertEquals(0, cursor.getInt(1))
            check(cursor.moveToNext())
            assertEquals(1, cursor.getInt(1))
        }
        db.query("SELECT id, hasKnownDate FROM progress_updates ORDER BY id").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(1))
            check(cursor.moveToNext())
            assertEquals(1, cursor.getInt(1))
        }
    }
}
