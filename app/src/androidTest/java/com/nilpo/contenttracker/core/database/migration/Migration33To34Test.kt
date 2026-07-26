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
class Migration33To34Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion33Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE media_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
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
    fun createsDurableRunAndItemTables() {
        val db = helper.writableDatabase
        MIGRATION_33_34.migrate(db)
        db.execSQL("INSERT INTO media_items VALUES (7)")
        db.execSQL(
            "INSERT INTO metadata_refresh_runs VALUES (1, 'Refreshing', 1, 10, 10, NULL)",
        )
        db.execSQL(
            "INSERT INTO metadata_refresh_items VALUES (1, 1, 7, 'Pending', 0, 0, NULL, 10, NULL)",
        )

        db.query("SELECT state, totalCount FROM metadata_refresh_runs WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("Refreshing", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        db.query("SELECT mediaItemId, state FROM metadata_refresh_items WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(7, cursor.getLong(0))
            assertEquals("Pending", cursor.getString(1))
        }
    }
}
