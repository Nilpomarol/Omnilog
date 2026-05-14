package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.relation.TrackedMediaRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun countMediaItems(): Int

    @Transaction
    @Query("SELECT * FROM media_items WHERE type IN (:types) ORDER BY title")
    fun observeTrackedMedia(types: List<String>): Flow<List<TrackedMediaRelation>>

    @Query("SELECT * FROM tracking_sessions WHERE mediaItemId = :mediaItemId ORDER BY sessionNumber")
    suspend fun getTrackingSessions(mediaItemId: Long): List<TrackingSessionEntity>

    @Query("SELECT * FROM tracking_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getTrackingSession(sessionId: Long): TrackingSessionEntity?

    @Query("SELECT * FROM media_items WHERE id = :mediaItemId LIMIT 1")
    suspend fun getMediaItem(mediaItemId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_collections ORDER BY name")
    fun observeMediaCollections(): Flow<List<MediaCollectionEntity>>

    @Query("SELECT * FROM media_collections ORDER BY name")
    suspend fun getMediaCollections(): List<MediaCollectionEntity>

    @Query("SELECT * FROM media_collections WHERE id = :collectionId LIMIT 1")
    suspend fun getMediaCollection(collectionId: Long): MediaCollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCollection(collection: MediaCollectionEntity): Long

    @Query("UPDATE media_collections SET name = :name WHERE id = :collectionId")
    suspend fun updateMediaCollectionName(collectionId: Long, name: String)

    @Query("DELETE FROM media_collections WHERE id = :collectionId")
    suspend fun deleteMediaCollection(collectionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingSession(session: TrackingSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalRating(externalRating: ExternalRatingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalTracking(externalTracking: ExternalTrackingEntity): Long

    @Query("UPDATE external_tracking SET isSynced = :isSynced WHERE id = :externalTrackingId")
    suspend fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean)

    @Query("DELETE FROM external_tracking WHERE id = :externalTrackingId")
    suspend fun deleteExternalTracking(externalTrackingId: Long)

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            collectionId = :collectionId,
            progressTotal = :progressTotal,
            isOwned = :isOwned,
            ownershipType = :ownershipType
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        progressTotal: Int?,
        isOwned: Boolean,
        ownershipType: String,
    )

    @Query(
        """
        UPDATE tracking_sessions
        SET platformName = :platformName, platformType = :platformType
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionPlatform(
        sessionId: Long,
        platformName: String?,
        platformType: String?,
    )

    @Query("UPDATE tracking_sessions SET progressCurrent = :progressCurrent WHERE id = :sessionId")
    suspend fun updateSessionProgress(sessionId: Long, progressCurrent: Int)

    @Query("UPDATE tracking_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: Long, status: String)

    @Query("UPDATE tracking_sessions SET rating = :rating WHERE id = :sessionId")
    suspend fun updateSessionRating(sessionId: Long, rating: Int?)

    @Query("UPDATE tracking_sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateSessionNotes(sessionId: Long, notes: String?)

    @Query(
        """
        UPDATE tracking_sessions
        SET progressCurrent = :progressTotal
        WHERE mediaItemId = :mediaItemId AND progressCurrent > :progressTotal
        """,
    )
    suspend fun clampSessionsToMediaTotal(mediaItemId: Long, progressTotal: Int)

    @Query("DELETE FROM tracking_sessions WHERE id = :sessionId")
    suspend fun deleteTrackingSession(sessionId: Long)
}
