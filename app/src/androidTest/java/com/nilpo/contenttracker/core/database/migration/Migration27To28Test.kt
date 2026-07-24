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
class Migration27To28Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion27Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(27) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE media_items (" +
                                "id INTEGER NOT NULL PRIMARY KEY, " +
                                "metadataSource TEXT, metadataExternalId TEXT)",
                        )
                        db.execSQL(
                            "CREATE TABLE import_batches (" +
                                "id INTEGER NOT NULL PRIMARY KEY, source TEXT NOT NULL)",
                        )
                        db.execSQL(
                            "CREATE TABLE import_batch_items (" +
                                "batchId INTEGER NOT NULL, mediaItemId INTEGER, sourceExternalId TEXT)",
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
    fun preservesProviderIdsFromMetadataAndPriorImportBatches() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO media_items VALUES (1, 'Imdb', ' TT1375666 ')")
        db.execSQL("INSERT INTO media_items VALUES (2, 'StoryGraph', '978-0-441-47812-5')")
        db.execSQL("INSERT INTO media_items VALUES (3, 'Tmdb', '27205')")
        db.execSQL("INSERT INTO import_batches VALUES (10, 'ImdbCsv')")
        db.execSQL("INSERT INTO import_batches VALUES (11, 'StoryGraphCsv')")
        db.execSQL("INSERT INTO import_batch_items VALUES (10, 3, 'TT7654321')")
        db.execSQL("INSERT INTO import_batch_items VALUES (11, 3, 'book-uid')")

        MIGRATION_27_28.migrate(db)

        assertEquals("tt1375666", db.value("SELECT imdbId FROM media_items WHERE id = 1"))
        assertEquals("978-0-441-47812-5", db.value("SELECT storyGraphId FROM media_items WHERE id = 2"))
        assertEquals("tt7654321", db.value("SELECT imdbId FROM media_items WHERE id = 3"))
        assertEquals("book-uid", db.value("SELECT storyGraphId FROM media_items WHERE id = 3"))
        assertTrue(db.hasIndex("media_items", "index_media_items_imdbId"))
        assertTrue(db.hasIndex("media_items", "index_media_items_storyGraphId"))
    }

    private fun SupportSQLiteDatabase.value(query: String): String? = query(query).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getString(0)
    }

    private fun SupportSQLiteDatabase.hasIndex(table: String, indexName: String): Boolean =
        query("PRAGMA index_list($table)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameColumn) else null }
                .any { it == indexName }
        }
}
