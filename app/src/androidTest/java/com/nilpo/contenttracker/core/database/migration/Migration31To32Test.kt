package com.nilpo.contenttracker.core.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration31To32Test {
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion31Database() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(31) {
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
    fun clearsGuessedOpenLibraryPortraitsAndUpgradesUndersizedImages() {
        val db = helper.writableDatabase
        insertCredit(db, id = 1, "https://covers.openlibrary.org/a/olid/OL23919A-M.jpg?default=false")
        insertCredit(db, id = 2, "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false")
        insertCredit(db, id = 3, "https://images.igdb.com/igdb/image/upload/t_thumb/logo.png")
        insertCredit(db, id = 4, "https://image.tmdb.org/t/p/w185/profile.jpg")
        insertCredit(db, id = 5, "https://image.tmdb.org/t/p/w500/company.png")
        insertCredit(db, id = 6, null)

        MIGRATION_31_32.migrate(db)

        // The OLID URL was never confirmed to exist, so it goes; the verified photo-ID URL stays.
        assertNull(personImageUrl(db, id = 1))
        assertEquals(
            "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false",
            personImageUrl(db, id = 2),
        )
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_logo_med_2x/logo.png",
            personImageUrl(db, id = 3),
        )
        assertEquals("https://image.tmdb.org/t/p/h632/profile.jpg", personImageUrl(db, id = 4))
        // A company logo is already large enough and must not be rewritten to a profile size.
        assertEquals("https://image.tmdb.org/t/p/w500/company.png", personImageUrl(db, id = 5))
        assertNull(personImageUrl(db, id = 6))
    }

    @Test
    fun leavesAlreadyMigratedRowsUnchangedWhenRunAgain() {
        val db = helper.writableDatabase
        insertCredit(db, id = 1, "https://images.igdb.com/igdb/image/upload/t_thumb/logo.png")
        insertCredit(db, id = 2, "https://image.tmdb.org/t/p/w185/profile.jpg")

        MIGRATION_31_32.migrate(db)
        val afterFirstRun = listOf(personImageUrl(db, id = 1), personImageUrl(db, id = 2))
        MIGRATION_31_32.migrate(db)

        assertEquals(afterFirstRun[0], personImageUrl(db, id = 1))
        assertEquals(afterFirstRun[1], personImageUrl(db, id = 2))
    }

    private fun insertCredit(db: SupportSQLiteDatabase, id: Long, personImageUrl: String?) {
        val image = personImageUrl?.let { "'$it'" } ?: "NULL"
        db.execSQL(
            "INSERT INTO media_credits VALUES " +
                "($id, 1, 'Someone', 'Author', NULL, $image, NULL, 0, 'OpenLibrary')",
        )
    }

    private fun personImageUrl(db: SupportSQLiteDatabase, id: Long): String? {
        return db.query("SELECT personImageUrl FROM media_credits WHERE id = $id").use { cursor ->
            check(cursor.moveToFirst())
            if (cursor.isNull(0)) null else cursor.getString(0)
        }
    }
}
