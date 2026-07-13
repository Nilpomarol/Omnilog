package com.nilpo.contenttracker

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath
import com.nilpo.contenttracker.BuildConfig
import com.nilpo.contenttracker.core.backup.AutoBackupScheduler
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.repository.AniListMetadataRepository
import com.nilpo.contenttracker.core.repository.BookRecommendationRepository
import com.nilpo.contenttracker.core.repository.CompositeMetadataRepository
import com.nilpo.contenttracker.core.repository.GoogleBooksMetadataRepository
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.OfflineMediaRepository
import com.nilpo.contenttracker.core.repository.OpenLibraryMetadataRepository
import com.nilpo.contenttracker.core.repository.RawgMetadataRepository
import com.nilpo.contenttracker.core.repository.CompositeRecommendationRepository
import com.nilpo.contenttracker.core.repository.RecommendationRepository
import com.nilpo.contenttracker.core.repository.TmdbMetadataRepository
import com.nilpo.contenttracker.core.repository.AniListRecommendationRepository
import com.nilpo.contenttracker.core.repository.RawgRecommendationRepository
import com.nilpo.contenttracker.core.repository.TmdbRecommendationRepository

class ContentTrackerApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AutoBackupScheduler.ensureScheduled(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.toOkioPath().resolve("cover_cache"))
                    .maxSizeBytes(COVER_DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            .build()
    }

    val database: ContentTrackerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ContentTrackerDatabase::class.java,
            "content-tracker.db",
        )
            .fallbackToDestructiveMigration(false)
                        .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
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
            aniList = AniListMetadataRepository(malClientId = BuildConfig.MAL_CLIENT_ID),
            openLibrary = OpenLibraryMetadataRepository(),
            googleBooks = GoogleBooksMetadataRepository(BuildConfig.GOOGLE_BOOKS_API_KEY),
            rawg = RawgMetadataRepository(BuildConfig.RAWG_API_KEY),
        )
    }

    val recommendationRepository: RecommendationRepository by lazy {
        CompositeRecommendationRepository(
            tmdb = TmdbRecommendationRepository(BuildConfig.TMDB_API_KEY),
            aniList = AniListRecommendationRepository(),
            rawg = RawgRecommendationRepository(BuildConfig.RAWG_API_KEY),
            books = BookRecommendationRepository(metadataRepository),
        )
    }
}

private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS objectives (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                metric TEXT NOT NULL,
                unit TEXT NOT NULL,
                mediaType TEXT,
                targetValue INTEGER NOT NULL,
                startDateEpochDay INTEGER NOT NULL,
                endDateEpochDay INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                archivedAtEpochMillis INTEGER
            )
            """.trimIndent(),
        )
    }
}

private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE progress_updates ADD COLUMN countsTowardObjectives INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            """
            UPDATE progress_updates
            SET countsTowardObjectives = 0
            WHERE sessionId IN (
                SELECT id
                FROM tracking_sessions
                WHERE status = 'Completed'
                  AND (
                      finishedAtEpochDay IS NULL
                      OR loggedAtEpochDay > finishedAtEpochDay
                  )
            )
            """.trimIndent(),
        )
    }
}

private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE progress_updates ADD COLUMN hasKnownDate INTEGER NOT NULL DEFAULT 1")
    }
}

private const val COVER_DISK_CACHE_SIZE_BYTES = 200L * 1024L * 1024L

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

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN language TEXT")
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN metadataOverrideFieldsCsv TEXT")
    }
}

private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS external_tracking")
    }
}

private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN primaryExternalRatingId INTEGER")
        db.execSQL("ALTER TABLE external_ratings ADD COLUMN origin TEXT NOT NULL DEFAULT 'Manual'")
        db.execSQL(
            """
            UPDATE media_items
            SET primaryExternalRatingId = (
                SELECT id FROM external_ratings
                WHERE mediaItemId = media_items.id
                  AND ABS(score - media_items.externalRatingScore) < 0.001
                  AND ABS(maxScore - media_items.externalRatingMax) < 0.001
                ORDER BY id
                LIMIT 1
            )
            WHERE externalRatingScore IS NOT NULL AND externalRatingMax IS NOT NULL
            """.trimIndent(),
        )
    }
}
