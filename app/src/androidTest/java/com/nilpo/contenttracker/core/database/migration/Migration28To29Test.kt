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
class Migration28To29Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion28Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE mal_sync_queue (" +
                                "mediaItemId INTEGER NOT NULL PRIMARY KEY, malId INTEGER NOT NULL, " +
                                "state TEXT NOT NULL, attemptCount INTEGER NOT NULL, lastError TEXT, " +
                                "updatedAtEpochMillis INTEGER NOT NULL, lastAttemptAtEpochMillis INTEGER, " +
                                "lastSuccessAtEpochMillis INTEGER)",
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
    fun preservesQueueRowsAndAddsNullablePayloadFingerprint() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO mal_sync_queue VALUES (1, 20, 'Synced', 0, NULL, 100, NULL, 100)")

        MIGRATION_28_29.migrate(db)

        db.query("SELECT lastSyncedPayloadHash FROM mal_sync_queue WHERE mediaItemId = 1").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(null, cursor.getString(0))
        }
    }
}
