package com.nilpo.contenttracker.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity

data class TrackedMediaRelation(
    @Embedded
    val item: MediaItemEntity,
    @Relation(
        parentColumn = "collectionId",
        entityColumn = "id",
    )
    val collection: MediaCollectionEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val sessions: List<TrackingSessionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val progressUpdates: List<ProgressUpdateEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val statusEvents: List<SessionStatusEventEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val credits: List<MediaCreditEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaItemId",
    )
    val externalRatings: List<ExternalRatingEntity>,
)
