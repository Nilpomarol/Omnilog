package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.SeasonProgressEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.relation.TrackedMediaRelation
import com.nilpo.contenttracker.core.database.relation.TrackingSessionWithSeasonsRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun countMediaItems(): Int

    @Transaction
    @Query("SELECT * FROM media_items WHERE type IN (:types) ORDER BY title")
    fun observeTrackedMedia(types: List<String>): Flow<List<TrackedMediaRelation>>

    @Transaction
    @Query("SELECT * FROM tracking_sessions WHERE mediaItemId = :mediaItemId ORDER BY sessionNumber")
    fun observeSessionsWithSeasons(mediaItemId: Long): Flow<List<TrackingSessionWithSeasonsRelation>>

    @Query("SELECT * FROM tracking_sessions WHERE mediaItemId = :mediaItemId ORDER BY sessionNumber")
    suspend fun getTrackingSessions(mediaItemId: Long): List<TrackingSessionEntity>

    @Query("SELECT * FROM tracking_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getTrackingSession(sessionId: Long): TrackingSessionEntity?

    @Query("SELECT * FROM season_progress WHERE trackingSessionId = :trackingSessionId ORDER BY seasonNumber")
    suspend fun getSeasonProgressForSession(trackingSessionId: Long): List<SeasonProgressEntity>

    @Query("SELECT * FROM season_progress WHERE id = :seasonProgressId LIMIT 1")
    suspend fun getSeasonProgress(seasonProgressId: Long): SeasonProgressEntity?

    @Query(
        """
        SELECT season_progress.* FROM season_progress
        INNER JOIN tracking_sessions ON season_progress.trackingSessionId = tracking_sessions.id
        WHERE tracking_sessions.mediaItemId = :mediaItemId
        ORDER BY season_progress.seasonNumber
        """,
    )
    suspend fun getSeasonProgressForMedia(mediaItemId: Long): List<SeasonProgressEntity>

    @Query(
        """
        SELECT season_progress.* FROM season_progress
        INNER JOIN tracking_sessions ON season_progress.trackingSessionId = tracking_sessions.id
        INNER JOIN media_items ON tracking_sessions.mediaItemId = media_items.id
        WHERE media_items.type IN (:types)
        ORDER BY season_progress.seasonNumber
        """,
    )
    fun observeSeasonProgress(types: List<String>): Flow<List<SeasonProgressEntity>>

    @Query(
        """
        SELECT external_ratings.* FROM external_ratings
        INNER JOIN media_items ON external_ratings.mediaItemId = media_items.id
        WHERE media_items.type IN (:types)
        """,
    )
    fun observeExternalRatings(types: List<String>): Flow<List<ExternalRatingEntity>>

    @Query(
        """
        SELECT external_tracking.* FROM external_tracking
        INNER JOIN media_items ON external_tracking.mediaItemId = media_items.id
        WHERE media_items.type IN (:types)
        """,
    )
    fun observeExternalTracking(types: List<String>): Flow<List<ExternalTrackingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingSession(session: TrackingSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasonProgress(seasonProgress: SeasonProgressEntity): Long

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
        SET title = :title, isOwned = :isOwned, ownershipType = :ownershipType
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
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

    @Query(
        """
        UPDATE tracking_sessions
        SET progressCurrent = :progressCurrent, progressTotal = :progressTotal
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionProgressTotal(
        sessionId: Long,
        progressCurrent: Int,
        progressTotal: Int?,
    )

    @Query("UPDATE tracking_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: Long, status: String)

    @Query("UPDATE tracking_sessions SET rating = :rating WHERE id = :sessionId")
    suspend fun updateSessionRating(sessionId: Long, rating: Int?)

    @Query("UPDATE tracking_sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateSessionNotes(sessionId: Long, notes: String?)

    @Query("UPDATE season_progress SET progressCurrent = :progressCurrent WHERE id = :seasonProgressId")
    suspend fun updateSeasonProgress(seasonProgressId: Long, progressCurrent: Int)

    @Query(
        """
        UPDATE season_progress
        SET progressCurrent = :progressCurrent, progressTotal = :progressTotal
        WHERE id = :seasonProgressId
        """,
    )
    suspend fun updateSeasonProgressTotal(
        seasonProgressId: Long,
        progressCurrent: Int,
        progressTotal: Int?,
    )
}
