package com.nilpo.contenttracker.core.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.graphics.Bitmap
import android.graphics.Color
import com.nilpo.contenttracker.ui.common.logoContentWidthRatio
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration32To33Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion32Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE media_credits (" +
                                "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                                "mediaItemId INTEGER NOT NULL, personName TEXT NOT NULL, " +
                                "roleType TEXT NOT NULL, characterName TEXT, personImageUrl TEXT, " +
                                "characterImageUrl TEXT, sortOrder INTEGER NOT NULL, " +
                                "metadataSource TEXT)",
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
    fun addsANullAspectRatioWithoutChangingExistingCredits() {
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO media_credits VALUES " +
                "(1, 1, 'Studio Example', 'Studio', NULL, 'https://example.com/logo.png', NULL, 0, NULL)",
        )

        MIGRATION_32_33.migrate(db)

        db.query("SELECT personImageUrl, personImageAspectRatio FROM media_credits WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("https://example.com/logo.png", cursor.getString(0))
            assertNull(cursor.getString(1))
        }
    }

    @Test
    fun measuresTheVisibleLogoInsteadOfTransparentProviderPadding() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.TRANSPARENT)
            for (x in 20 until 80) {
                for (y in 35 until 55) {
                    bitmap.setPixel(x, y, Color.MAGENTA)
                }
            }

            assertEquals(3f, bitmap.logoContentWidthRatio()!!, 0.0001f)
        } finally {
            bitmap.recycle()
        }
    }
}
