package com.nilpo.contenttracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity

@Database(
    entities = [
        MediaCollectionEntity::class,
        MediaItemEntity::class,
        MediaCreditEntity::class,
        TrackingSessionEntity::class,
        ExternalRatingEntity::class,
        ExternalTrackingEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class ContentTrackerDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
