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
class Migration34To35Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion34Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(34) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE media_credits (" +
                                "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, personImageUrl TEXT)",
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
    fun replacesCroppedIgdbLogoDeliveryWithOriginalWithoutTouchingOtherImages() {
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO media_credits VALUES " +
                "(1, 'https://images.igdb.com/igdb/image/upload/t_logo_med_2x/company.png'), " +
                "(2, 'https://image.tmdb.org/t/p/w500/company.png')",
        )

        MIGRATION_34_35.migrate(db)

        db.query("SELECT personImageUrl FROM media_credits ORDER BY id").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(
                "https://images.igdb.com/igdb/image/upload/t_original/company.png",
                cursor.getString(0),
            )
            check(cursor.moveToNext())
            assertEquals("https://image.tmdb.org/t/p/w500/company.png", cursor.getString(0))
        }
    }
}
