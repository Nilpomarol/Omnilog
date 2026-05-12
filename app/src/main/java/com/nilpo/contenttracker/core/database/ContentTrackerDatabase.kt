package com.nilpo.contenttracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.SeasonProgressEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity

@Database(
    entities = [
        MediaItemEntity::class,
        TrackingSessionEntity::class,
        SeasonProgressEntity::class,
        ExternalRatingEntity::class,
        ExternalTrackingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ContentTrackerDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
