package com.nilpo.contenttracker

import android.app.Application
import androidx.room.Room
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
            .build()
    }

    val mediaRepository: OfflineMediaRepository by lazy {
        OfflineMediaRepository(database.mediaDao())
    }

    val metadataRepository: MetadataRepository by lazy {
        CompositeMetadataRepository(
            tmdb = TmdbMetadataRepository(BuildConfig.TMDB_API_KEY),
            aniList = AniListMetadataRepository(),
            openLibrary = OpenLibraryMetadataRepository(),
            googleBooks = GoogleBooksMetadataRepository(BuildConfig.GOOGLE_BOOKS_API_KEY),
            rawg = RawgMetadataRepository(BuildConfig.RAWG_API_KEY),
        )
    }
}
