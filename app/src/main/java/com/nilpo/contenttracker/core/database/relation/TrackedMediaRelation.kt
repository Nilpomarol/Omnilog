package com.nilpo.contenttracker.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity

data class TrackedMediaRelation(
    @Embedded
    val item: MediaItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val sessions: List<TrackingSessionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val externalRatings: List<ExternalRatingEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val externalTracking: List<ExternalTrackingEntity>,
)
