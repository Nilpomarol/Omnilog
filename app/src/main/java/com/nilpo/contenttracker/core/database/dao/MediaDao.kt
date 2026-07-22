package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.MalSyncQueueEntity
import com.nilpo.contenttracker.core.database.entity.ObjectiveEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.relation.TrackedMediaRelation
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
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

    @Query("UPDATE media_items SET malId = :malId WHERE id = :mediaItemId")
    suspend fun updateMalId(mediaItemId: Long, malId: Int)

    @Query("SELECT * FROM mal_sync_queue WHERE mediaItemId = :mediaItemId LIMIT 1")
    suspend fun getMalSyncQueueItem(mediaItemId: Long): MalSyncQueueEntity?

    @Query(
        "SELECT * FROM mal_sync_queue WHERE state = 'Pending' " +
            "ORDER BY updatedAtEpochMillis LIMIT :limit",
    )
    suspend fun getPendingMalSyncQueue(limit: Int): List<MalSyncQueueEntity>

    @Query("SELECT * FROM mal_sync_queue ORDER BY updatedAtEpochMillis")
    fun observeMalSyncQueue(): Flow<List<MalSyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMalSyncQueueItem(item: MalSyncQueueEntity)

    @Query("DELETE FROM mal_sync_queue WHERE mediaItemId = :mediaItemId")
    suspend fun deleteMalSyncQueueItem(mediaItemId: Long)

    @Query("SELECT * FROM media_credits ORDER BY mediaItemId, sortOrder, id")
    suspend fun getMediaCredits(): List<MediaCreditEntity>

    @Query("SELECT * FROM media_credits WHERE mediaItemId = :mediaItemId ORDER BY sortOrder, id")
    suspend fun getMediaCreditsForItem(mediaItemId: Long): List<MediaCreditEntity>

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

    @Query("SELECT * FROM session_status_events ORDER BY createdAtEpochMillis, id")
    suspend fun getSessionStatusEvents(): List<SessionStatusEventEntity>

    @Query(
        "SELECT * FROM session_status_events WHERE sessionId = :sessionId " +
            "ORDER BY createdAtEpochMillis, id",
    )
    suspend fun getSessionStatusEventsForSession(sessionId: Long): List<SessionStatusEventEntity>

    @Query("SELECT * FROM external_ratings ORDER BY id")
    suspend fun getExternalRatings(): List<ExternalRatingEntity>

    @Query("SELECT * FROM objectives ORDER BY startDateEpochDay, endDateEpochDay, id")
    fun observeObjectives(): Flow<List<ObjectiveEntity>>

    @Query("SELECT * FROM objectives ORDER BY id")
    suspend fun getObjectives(): List<ObjectiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: ObjectiveEntity): Long

    @Query("DELETE FROM objectives WHERE id = :objectiveId")
    suspend fun deleteObjective(objectiveId: Long)

    @Query("DELETE FROM objectives")
    suspend fun deleteAllObjectives()

    @Query("SELECT * FROM external_ratings WHERE mediaItemId = :mediaItemId ORDER BY id")
    suspend fun getExternalRatingsForItem(mediaItemId: Long): List<ExternalRatingEntity>

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
    suspend fun insertSessionStatusEvent(event: SessionStatusEventEntity): Long

    @Query("SELECT * FROM session_status_events WHERE id = :eventId LIMIT 1")
    suspend fun getSessionStatusEvent(eventId: Long): SessionStatusEventEntity?

    /**
     * Removes one logged transition. The log is append-only by design, so this exists purely to
     * correct a mistake — an accidental pause has no other way out, since the row cannot be edited
     * into being right.
     */
    @Query("DELETE FROM session_status_events WHERE id = :eventId")
    suspend fun deleteSessionStatusEvent(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalRating(externalRating: ExternalRatingEntity): Long

    @Query("SELECT * FROM external_ratings WHERE id = :externalRatingId LIMIT 1")
    suspend fun getExternalRating(externalRatingId: Long): ExternalRatingEntity?

    @Query(
        """
        UPDATE external_ratings
        SET source = :source,
            score = :score,
            maxScore = :maxScore,
            voteCount = :voteCount,
            scoreDescriptor = NULL,
            origin = :origin
        WHERE id = :externalRatingId
        """,
    )
    suspend fun updateExternalRating(
        externalRatingId: Long,
        source: String,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        origin: String,
    )

    @Query(
        """
        UPDATE media_items
        SET externalRatingScore = :score,
            externalRatingMax = :maxScore,
            externalRatingVoteCount = :voteCount,
            primaryExternalRatingId = :primaryExternalRatingId
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updatePrimaryExternalRating(
        mediaItemId: Long,
        score: Double?,
        maxScore: Double?,
        voteCount: Int?,
        primaryExternalRatingId: Long?,
    )

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            collectionId = :collectionId,
            collectionSortOrder = :collectionSortOrder,
            progressTotal = :progressTotal,
            isOwned = :isOwned
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
    )

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            originalTitle = :originalTitle,
            releaseYear = :releaseYear,
            language = :language,
            progressTotal = :progressTotal,
            genresJson = :genresJson,
            creatorsJson = :creatorsJson,
            coverUrl = :coverUrl,
            synopsis = :synopsis,
            sourceUrl = :sourceUrl,
            popularityJson = :popularityJson
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updateMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        language: String?,
        progressTotal: Int?,
        genresJson: String?,
        creatorsJson: String?,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
        popularityJson: String?,
    )

    @Query("UPDATE media_items SET primaryExternalRatingId = :primaryExternalRatingId WHERE id = :mediaItemId")
    suspend fun setPrimaryRatingId(mediaItemId: Long, primaryExternalRatingId: Long?)

    @Query(
        """
        UPDATE media_items
        SET metadataOverrideFieldsCsv = :metadataOverrideFieldsCsv
        WHERE id = :mediaItemId
        """,
    )
    suspend fun updateMetadataOverrideFields(
        mediaItemId: Long,
        metadataOverrideFieldsCsv: String?,
    )

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            originalTitle = :originalTitle,
            releaseYear = :releaseYear,
            language = :language,
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
            metadataSource = :metadataSource,
            metadataExternalId = :metadataExternalId,
            malId = :malId,
            metadataLastFetchedAtEpochMillis = :metadataLastFetchedAtEpochMillis
        WHERE id = :mediaItemId
        """,
    )
    suspend fun refreshMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        language: String?,
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
        metadataSource: String?,
        metadataExternalId: String?,
        malId: Int?,
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

    @Query(
        """
        UPDATE tracking_sessions
        SET progressCurrent = :progressTotal,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE mediaItemId = :mediaItemId AND status = 'Completed' AND progressCurrent = 0
        """,
    )
    suspend fun fillCompletedSessionsToMediaTotal(
        mediaItemId: Long,
        progressTotal: Int,
        updatedAtEpochMillis: Long,
    )

    @Query("DELETE FROM tracking_sessions WHERE id = :sessionId")
    suspend fun deleteTrackingSession(sessionId: Long)

    @Query("DELETE FROM progress_updates WHERE sessionId = :sessionId")
    suspend fun deleteProgressUpdatesForSession(sessionId: Long)

    @Query(
        """
        UPDATE progress_updates
        SET progressValue = :progressValue,
            loggedAtEpochDay = :loggedAtEpochDay,
            hasKnownDate = :hasKnownDate
        WHERE id = :progressUpdateId
        """,
    )
    suspend fun updateProgressUpdate(
        progressUpdateId: Long,
        progressValue: Int,
        loggedAtEpochDay: Long,
        hasKnownDate: Boolean,
    )

    /**
     * Progress updates are cumulative, so the session's current progress is the value of the
     * chronologically last update — the same rule [deleteProgressUpdate] callers apply. Editing a
     * date can therefore change which update is last, which is why value and date edits share this
     * transaction. [maxProgress] is the media total when one is meaningful; null leaves the value
     * unbounded above.
     */
    @Transaction
    suspend fun updateProgressUpdateAndRecalculateSession(
        progressUpdateId: Long,
        progressValue: Int,
        loggedAtEpochDay: Long,
        hasKnownDate: Boolean,
        updatedAtEpochMillis: Long,
        maxProgress: Int?,
    ) {
        val existing = getProgressUpdate(progressUpdateId) ?: return
        updateProgressUpdate(
            progressUpdateId = progressUpdateId,
            progressValue = progressValue.clampToProgressTotal(maxProgress),
            loggedAtEpochDay = loggedAtEpochDay,
            hasKnownDate = hasKnownDate,
        )
        val currentProgress = getProgressUpdatesForSession(existing.sessionId)
            .maxWithOrNull(
                compareBy<ProgressUpdateEntity> { it.loggedAtEpochDay }
                    .thenBy { it.createdAtEpochMillis }
                    .thenBy { it.id },
            )
            ?.progressValue
            ?: 0
        // Older rows may predate the current total, so the recalculated session value is clamped
        // too rather than trusting that the edited row is the one that decided it.
        updateSessionProgress(
            sessionId = existing.sessionId,
            progressCurrent = currentProgress.clampToProgressTotal(maxProgress),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    @Query("DELETE FROM progress_updates WHERE id = :progressUpdateId")
    suspend fun deleteProgressUpdate(progressUpdateId: Long)

    @Query("DELETE FROM media_items WHERE id = :mediaItemId")
    suspend fun deleteMediaItem(mediaItemId: Long)

    @Query("DELETE FROM media_credits WHERE mediaItemId = :mediaItemId")
    suspend fun deleteMediaCreditsForItem(mediaItemId: Long)

    @Query("DELETE FROM external_ratings WHERE mediaItemId = :mediaItemId")
    suspend fun deleteExternalRatingsForItem(mediaItemId: Long)

    @Query("DELETE FROM external_ratings WHERE mediaItemId = :mediaItemId AND origin = 'Provider'")
    suspend fun deleteProviderExternalRatingsForItem(mediaItemId: Long)

    @Query("DELETE FROM external_ratings WHERE id = :externalRatingId")
    suspend fun deleteExternalRating(externalRatingId: Long)
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
        objectives: List<ObjectiveEntity> = emptyList(),
    ) {
        deleteAllExternalRatings()
        deleteAllProgressUpdates()
        deleteAllTrackingSessions()
        deleteAllMediaCredits()
        deleteAllMediaItems()
        deleteAllMediaCollections()
        deleteAllObjectives()

        collections.forEach { insertMediaCollection(it) }
        mediaItems.forEach { insertMediaItem(it) }
        mediaCredits.forEach { insertMediaCredit(it) }
        sessions.forEach { insertTrackingSession(it) }
        progressUpdates.forEach { insertProgressUpdate(it) }
        externalRatings.forEach { insertExternalRating(it) }
        objectives.forEach { insertObjective(it) }
        deleteEmptyMediaCollections()
    }
}

private fun Int.clampToProgressTotal(maxProgress: Int?): Int =
    maxProgress?.let { coerceIn(0, it) } ?: coerceAtLeast(0)
