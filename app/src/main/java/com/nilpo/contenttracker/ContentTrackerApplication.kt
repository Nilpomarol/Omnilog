package com.nilpo.contenttracker

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nilpo.contenttracker.BuildConfig
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.repository.AniListMetadataRepository
import com.nilpo.contenttracker.core.repository.CompositeMetadataRepository
import com.nilpo.contenttracker.core.repository.GoogleBooksMetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.OfflineMediaRepository
import com.nilpo.contenttracker.core.repository.OpenLibraryMetadataRepository
import com.nilpo.contenttracker.core.repository.RawgMetadataRepository
import com.nilpo.contenttracker.core.repository.TmdbMetadataRepository

class ContentTrackerApplication : Application() {
    val database: ContentTrackerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ContentTrackerDatabase::class.java,
            "content-tracker.db",
        )
            .fallbackToDestructiveMigration(false)
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()
    }

    val mediaRepository: OfflineMediaRepository by lazy {
        OfflineMediaRepository(database.mediaDao())
    }

    val metadataRepository: MetadataRepository by lazy {
        CompositeMetadataRepository(
            tmdb = TmdbMetadataRepository(
                apiKey = BuildConfig.TMDB_API_KEY,
                omdbApiKey = BuildConfig.OMDB_API_KEY,
            ),
            aniList = AniListMetadataRepository(),
            openLibrary = OpenLibraryMetadataRepository(),
            googleBooks = GoogleBooksMetadataRepository(BuildConfig.GOOGLE_BOOKS_API_KEY),
            rawg = RawgMetadataRepository(BuildConfig.RAWG_API_KEY),
        )
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN collectionSortOrder REAL")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_items_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                collectionId INTEGER,
                collectionSortOrder REAL,
                progressTotal INTEGER,
                originalTitle TEXT,
                releaseYear INTEGER,
                genresJson TEXT,
                creatorsJson TEXT,
                coverUrl TEXT,
                synopsis TEXT,
                sourceUrl TEXT,
                externalRatingScore REAL,
                externalRatingMax REAL,
                externalRatingVoteCount INTEGER,
                popularityScore REAL,
                rankingPosition INTEGER,
                rankingLabel TEXT,
                providerCollectionTitle TEXT,
                ratingDistributionJson TEXT,
                popularityJson TEXT,
                rankingJson TEXT,
                metadataLastFetchedAtEpochMillis INTEGER,
                metadataExternalId TEXT,
                metadataSource TEXT,
                isOwned INTEGER NOT NULL,
                ownershipType TEXT NOT NULL,
                FOREIGN KEY(collectionId) REFERENCES media_collections(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO media_items_new (
                id,
                type,
                title,
                collectionId,
                collectionSortOrder,
                progressTotal,
                originalTitle,
                releaseYear,
                genresJson,
                creatorsJson,
                coverUrl,
                synopsis,
                sourceUrl,
                externalRatingScore,
                externalRatingMax,
                externalRatingVoteCount,
                popularityScore,
                rankingPosition,
                rankingLabel,
                providerCollectionTitle,
                ratingDistributionJson,
                popularityJson,
                rankingJson,
                metadataLastFetchedAtEpochMillis,
                metadataExternalId,
                metadataSource,
                isOwned,
                ownershipType
            )
            SELECT
                id,
                type,
                title,
                collectionId,
                CAST(collectionSortOrder AS REAL),
                progressTotal,
                originalTitle,
                releaseYear,
                genresJson,
                creatorsJson,
                coverUrl,
                synopsis,
                sourceUrl,
                externalRatingScore,
                externalRatingMax,
                externalRatingVoteCount,
                popularityScore,
                rankingPosition,
                rankingLabel,
                providerCollectionTitle,
                ratingDistributionJson,
                popularityJson,
                rankingJson,
                metadataLastFetchedAtEpochMillis,
                metadataExternalId,
                metadataSource,
                isOwned,
                ownershipType
            FROM media_items
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE media_items")
        db.execSQL("ALTER TABLE media_items_new RENAME TO media_items")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_collectionId ON media_items(collectionId)")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS progress_updates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mediaItemId INTEGER NOT NULL,
                sessionId INTEGER NOT NULL,
                progressValue INTEGER NOT NULL,
                loggedAtEpochDay INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                FOREIGN KEY(mediaItemId) REFERENCES media_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(sessionId) REFERENCES tracking_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_progress_updates_mediaItemId ON progress_updates(mediaItemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_progress_updates_sessionId ON progress_updates(sessionId)")
    }
}
