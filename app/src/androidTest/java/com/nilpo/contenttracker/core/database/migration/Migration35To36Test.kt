package com.nilpo.contenttracker.core.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration35To36Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion35Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(35) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE tracking_sessions (" +
                                "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, rating INTEGER)",
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
    fun doublesEveryRatingIntoHalfPointsAndLeavesUnratedSessionsAlone() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO tracking_sessions VALUES (1, 8), (2, 10), (3, 1), (4, NULL)")

        MIGRATION_35_36.migrate(db)

        db.query("SELECT rating FROM tracking_sessions ORDER BY id").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(16, cursor.getInt(0))
            check(cursor.moveToNext())
            assertEquals(20, cursor.getInt(0))
            check(cursor.moveToNext())
            assertEquals(2, cursor.getInt(0))
            check(cursor.moveToNext())
            assertTrue(cursor.isNull(0))
        }
    }
}
