package com.nilpo.contenttracker

import android.app.Application
import androidx.room.Room
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.repository.OfflineMediaRepository

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
}
