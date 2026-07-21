package com.nilpo.contenttracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ObjectiveEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity

@Database(
    entities = [
        MediaCollectionEntity::class,
        MediaItemEntity::class,
        MediaCreditEntity::class,
        TrackingSessionEntity::class,
        ProgressUpdateEntity::class,
        SessionStatusEventEntity::class,
        ExternalRatingEntity::class,
        ObjectiveEntity::class,
    ],
    version = 19,
    exportSchema = false,
)
abstract class ContentTrackerDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
