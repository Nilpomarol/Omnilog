package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.relation.TrackedMediaRelation
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
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

    @Query("SELECT * FROM media_items ORDER BY id")
    suspend fun getMediaItems(): List<MediaItemEntity>

    @Query("SELECT * FROM media_credits ORDER BY mediaItemId, sortOrder, id")
    suspend fun getMediaCredits(): List<MediaCreditEntity>

    @Query("SELECT * FROM media_collections ORDER BY name")
    fun observeMediaCollections(): Flow<List<MediaCollectionEntity>>

    @Query("SELECT * FROM media_collections ORDER BY name")
    suspend fun getMediaCollections(): List<MediaCollectionEntity>

    @Query("SELECT * FROM tracking_sessions ORDER BY id")
    suspend fun getAllTrackingSessions(): List<TrackingSessionEntity>

    @Query("SELECT * FROM progress_updates ORDER BY id")
    suspend fun getProgressUpdates(): List<ProgressUpdateEntity>

    @Query("SELECT * FROM progress_updates WHERE id = :progressUpdateId LIMIT 1")
    suspend fun getProgressUpdate(progressUpdateId: Long): ProgressUpdateEntity?

    @Query("SELECT * FROM progress_updates WHERE sessionId = :sessionId ORDER BY loggedAtEpochDay, createdAtEpochMillis, id")
    suspend fun getProgressUpdatesForSession(sessionId: Long): List<ProgressUpdateEntity>

    @Query("SELECT * FROM external_ratings ORDER BY id")
    suspend fun getExternalRatings(): List<ExternalRatingEntity>

    @Query("SELECT * FROM external_ratings WHERE mediaItemId = :mediaItemId ORDER BY id")
    suspend fun getExternalRatingsForItem(mediaItemId: Long): List<ExternalRatingEntity>

    @Query("SELECT * FROM external_tracking ORDER BY id")
    suspend fun getExternalTracking(): List<ExternalTrackingEntity>

    @Query("SELECT * FROM media_collections WHERE id = :collectionId LIMIT 1")
    suspend fun getMediaCollection(collectionId: Long): MediaCollectionEntity?

    @Query("SELECT COALESCE(MAX(collectionSortOrder), 0.0) FROM media_items WHERE collectionId = :collectionId")
    suspend fun getMaxCollectionSortOrder(collectionId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCollection(collection: MediaCollectionEntity): Long

    @Query("UPDATE media_collections SET name = :name WHERE id = :collectionId")
    suspend fun updateMediaCollectionName(collectionId: Long, name: String)

    @Query("DELETE FROM media_collections WHERE id = :collectionId")
    suspend fun deleteMediaCollection(collectionId: Long)

    @Query(
        """
        DELETE FROM media_collections
        WHERE id NOT IN (
            SELECT DISTINCT collectionId FROM media_items WHERE collectionId IS NOT NULL
        )
        """,
    )
    suspend fun deleteEmptyMediaCollections()

    @Query(
        """
        UPDATE media_items
        SET collectionSortOrder = :collectionSortOrder
        WHERE id = :mediaItemId AND collectionId = :collectionId
        """,
    )
    suspend fun updateCollectionSortOrder(
        mediaItemId: Long,
        collectionId: Long,
        collectionSortOrder: Double,
    )

    @Transaction
    suspend fun updateCollectionSortOrders(collectionId: Long, itemOrders: List<CollectionItemOrder>) {
        itemOrders.forEach { itemOrder ->
            updateCollectionSortOrder(
                mediaItemId = itemOrder.mediaItemId,
                collectionId = collectionId,
                collectionSortOrder = itemOrder.sortOrder,
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCredit(credit: MediaCreditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCredits(credits: List<MediaCreditEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingSession(session: TrackingSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressUpdate(update: ProgressUpdateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalRating(externalRating: ExternalRatingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalTracking(externalTracking: ExternalTrackingEntity): Long

    @Query("UPDATE external_tracking SET isSynced = :isSynced WHERE id = :externalTrackingId")
    suspend fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean)

    @Query("UPDATE external_tracking SET isSynced = :isSynced WHERE mediaItemId = :mediaItemId")
    suspend fun updateExternalTrackingSyncedForMedia(mediaItemId: Long, isSynced: Boolean)

    @Query(
        """
        UPDATE external_tracking
        SET source = :source,
            externalItemId = :externalItemId,
            url = :url,
            isSynced = :isSynced
        WHERE id = :externalTrackingId
        """,
    )
    suspend fun updateExternalTracking(
        externalTrackingId: Long,
        source: String,
        externalItemId: String?,
        url: String?,
        isSynced: Boolean,
    )

    @Query("DELETE FROM external_tracking WHERE id = :externalTrackingId")
    suspend fun deleteExternalTracking(externalTrackingId: Long)

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            collectionId = :collectionId,
            collectionSortOrder = :collectionSortOrder,
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
        collectionSortOrder: Double?,
        progressTotal: Int?,
        isOwned: Boolean,
        ownershipType: String,
    )

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            originalTitle = :originalTitle,
            releaseYear = :releaseYear,
            progressTotal = :progressTotal,
            genresJson = :genresJson,
            creatorsJson = :creatorsJson,
            coverUrl = :coverUrl,
            synopsis = :synopsis,
            sourceUrl = :sourceUrl
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updateMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        progressTotal: Int?,
        genresJson: String?,
        creatorsJson: String?,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    )

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            originalTitle = :originalTitle,
            releaseYear = :releaseYear,
            progressTotal = :progressTotal,
            genresJson = :genresJson,
            creatorsJson = :creatorsJson,
            coverUrl = :coverUrl,
            synopsis = :synopsis,
            sourceUrl = :sourceUrl,
            externalRatingScore = :externalRatingScore,
            externalRatingMax = :externalRatingMax,
            externalRatingVoteCount = :externalRatingVoteCount,
            popularityScore = :popularityScore,
            rankingPosition = :rankingPosition,
            rankingLabel = :rankingLabel,
            providerCollectionTitle = :providerCollectionTitle,
            ratingDistributionJson = :ratingDistributionJson,
            popularityJson = :popularityJson,
            rankingJson = :rankingJson,
            metadataLastFetchedAtEpochMillis = :metadataLastFetchedAtEpochMillis
        WHERE id = :mediaItemId
        """,
    )
    suspend fun refreshMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        progressTotal: Int?,
        genresJson: String?,
        creatorsJson: String?,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
        externalRatingScore: Double?,
        externalRatingMax: Double?,
        externalRatingVoteCount: Int?,
        popularityScore: Double?,
        rankingPosition: Int?,
        rankingLabel: String?,
        providerCollectionTitle: String?,
        ratingDistributionJson: String?,
        popularityJson: String?,
        rankingJson: String?,
        metadataLastFetchedAtEpochMillis: Long,
    )

    @Query(
        """
        UPDATE tracking_sessions
        SET status = :status,
            progressCurrent = :progressCurrent,
            rating = :rating,
            notes = :notes,
            startedAtEpochDay = :startedAtEpochDay,
            finishedAtEpochDay = :finishedAtEpochDay,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionDetails(
        sessionId: Long,
        status: String,
        progressCurrent: Int,
        rating: Int?,
        notes: String?,
        startedAtEpochDay: Long?,
        finishedAtEpochDay: Long?,
        updatedAtEpochMillis: Long,
    )

    @Query(
        """
        UPDATE tracking_sessions
        SET progressCurrent = :progressCurrent,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionProgress(
        sessionId: Long,
        progressCurrent: Int,
        updatedAtEpochMillis: Long,
    )

    @Query(
        """
        UPDATE tracking_sessions
        SET progressCurrent = :progressTotal,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE mediaItemId = :mediaItemId AND progressCurrent > :progressTotal
        """,
    )
    suspend fun clampSessionsToMediaTotal(
        mediaItemId: Long,
        progressTotal: Int,
        updatedAtEpochMillis: Long,
    )

    @Query("DELETE FROM tracking_sessions WHERE id = :sessionId")
    suspend fun deleteTrackingSession(sessionId: Long)

    @Query("DELETE FROM progress_updates WHERE sessionId = :sessionId")
    suspend fun deleteProgressUpdatesForSession(sessionId: Long)

    @Query("DELETE FROM progress_updates WHERE id = :progressUpdateId")
    suspend fun deleteProgressUpdate(progressUpdateId: Long)

    @Query("DELETE FROM media_items WHERE id = :mediaItemId")
    suspend fun deleteMediaItem(mediaItemId: Long)

    @Query("DELETE FROM media_credits WHERE mediaItemId = :mediaItemId")
    suspend fun deleteMediaCreditsForItem(mediaItemId: Long)

    @Query("DELETE FROM external_ratings WHERE mediaItemId = :mediaItemId")
    suspend fun deleteExternalRatingsForItem(mediaItemId: Long)

    @Query("DELETE FROM external_tracking")
    suspend fun deleteAllExternalTracking()

    @Query("DELETE FROM external_ratings")
    suspend fun deleteAllExternalRatings()

    @Query("DELETE FROM tracking_sessions")
    suspend fun deleteAllTrackingSessions()

    @Query("DELETE FROM progress_updates")
    suspend fun deleteAllProgressUpdates()

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMediaItems()

    @Query("DELETE FROM media_credits")
    suspend fun deleteAllMediaCredits()

    @Query("DELETE FROM media_collections")
    suspend fun deleteAllMediaCollections()

    @Transaction
    suspend fun replaceAllData(
        collections: List<MediaCollectionEntity>,
        mediaItems: List<MediaItemEntity>,
        mediaCredits: List<MediaCreditEntity>,
        sessions: List<TrackingSessionEntity>,
        progressUpdates: List<ProgressUpdateEntity>,
        externalRatings: List<ExternalRatingEntity>,
        externalTracking: List<ExternalTrackingEntity>,
    ) {
        deleteAllExternalTracking()
        deleteAllExternalRatings()
        deleteAllProgressUpdates()
        deleteAllTrackingSessions()
        deleteAllMediaCredits()
        deleteAllMediaItems()
        deleteAllMediaCollections()

        collections.forEach { insertMediaCollection(it) }
        mediaItems.forEach { insertMediaItem(it) }
        mediaCredits.forEach { insertMediaCredit(it) }
        sessions.forEach { insertTrackingSession(it) }
        progressUpdates.forEach { insertProgressUpdate(it) }
        externalRatings.forEach { insertExternalRating(it) }
        externalTracking.forEach { insertExternalTracking(it) }
        deleteEmptyMediaCollections()
    }
}
