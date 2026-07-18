package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class UnsupportedBackupSchemaException(
    val schemaVersion: Int,
) : IllegalArgumentException("Unsupported backup schema version: $schemaVersion")

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long?,
    val collectionCount: Int,
    val mediaItemCount: Int,
    val mediaCreditCount: Int,
    val trackingSessionCount: Int,
    val progressUpdateCount: Int,
    val externalRatingCount: Int,
    val objectiveCount: Int = 0,
)

sealed interface DeletionRecovery {
    data class MediaItem(
        val item: MediaItemEntity,
        val collection: MediaCollectionEntity?,
        val credits: List<MediaCreditEntity>,
        val sessions: List<TrackingSessionEntity>,
        val progressUpdates: List<ProgressUpdateEntity>,
        val externalRatings: List<ExternalRatingEntity>,
    ) : DeletionRecovery

    data class PastSession(
        val session: TrackingSessionEntity,
        val progressUpdates: List<ProgressUpdateEntity>,
        /**
         * Which time through the title the deleted session was, captured before deletion. Not the
         * same as its [TrackingSessionEntity.sessionNumber], which can be sparse.
         */
        val visitNumber: Int,
    ) : DeletionRecovery

    data class ProgressUpdate(
        val update: ProgressUpdateEntity,
        val sessionBeforeDeletion: TrackingSessionEntity,
        val sessionAfterDeletion: TrackingSessionEntity,
    ) : DeletionRecovery
}

data class MetadataRefreshPreview(
    val mediaItemId: Long,
    val refreshed: MetadataSuggestion,
    val changes: List<MetadataRefreshChange>,
)

data class MetadataRefreshChange(
    val field: MetadataRefreshField,
    val currentValue: String,
    val newValue: String,
    val overwritesExistingValue: Boolean,
    val isLocallyOverridden: Boolean = false,
)

enum class MetadataRefreshField {
    Title,
    OriginalTitle,
    ReleaseYear,
    Language,
    ProgressTotal,
    Genres,
    Creators,
    Credits,
    Cover,
    Synopsis,
    SourceUrl,
    ExternalRating,
    ExternalRatings,
    ProviderStats,
}

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    fun observeObjectives(): Flow<List<Objective>>

    suspend fun addObjective(objective: Objective): Long

    suspend fun updateObjective(objective: Objective)

    suspend fun deleteObjective(objectiveId: Long)

    suspend fun exportBackupJson(): String

    suspend fun previewBackupJson(json: String): BackupPreview

    suspend fun importBackupJson(json: String)

    suspend fun previewImdbCsv(csv: String): ImdbCsvPreview

    suspend fun importImdbCsv(csv: String): ImdbCsvImportResult

    suspend fun previewStoryGraphCsv(csv: String): StoryGraphCsvPreview

    suspend fun importStoryGraphCsv(csv: String): StoryGraphCsvImportResult

    suspend fun previewMyAnimeListXml(xml: String): MyAnimeListXmlPreview

    suspend fun importMyAnimeListXml(xml: String): MyAnimeListXmlImportResult

    suspend fun startNewSession(request: AddTrackingSessionRequest)

    suspend fun addTrackedMedia(request: AddTrackedMediaRequest): Long

    suspend fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        rating: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    )

    suspend fun deletePastSession(sessionId: Long): DeletionRecovery?

    suspend fun deleteProgressUpdate(progressUpdateId: Long): DeletionRecovery?
    suspend fun updateProgressUpdateDate(progressUpdateId: Long, loggedAt: LocalDate?)

    suspend fun deleteMediaItem(mediaItemId: Long): DeletionRecovery?
    suspend fun restoreDeletion(recovery: DeletionRecovery): Boolean

    suspend fun addExternalRating(
        mediaItemId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    )

    suspend fun updateExternalRating(
        externalRatingId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    )

    suspend fun setPrimaryExternalRating(externalRatingId: Long)

    suspend fun deleteExternalRating(externalRatingId: Long)

    suspend fun updateMediaCollectionName(collectionId: Long, name: String)

    suspend fun deleteMediaCollection(collectionId: Long)

    suspend fun updateCollectionItemOrder(collectionId: Long, itemOrders: List<CollectionItemOrder>)

    suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        collectionSortOrder: Double?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    )

    suspend fun updateMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        language: String?,
        progressTotal: Int?,
        genres: List<String>,
        creators: List<String>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    )

    suspend fun previewMediaItemMetadataRefresh(
        mediaItemId: Long,
        metadataRepository: MetadataRepository,
    ): MetadataRefreshPreview?

    suspend fun applyMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Boolean

    suspend fun refreshMediaItemMetadata(mediaItemId: Long, metadataRepository: MetadataRepository): Boolean

    suspend fun linkMediaItemMetadata(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
        metadataRepository: MetadataRepository,
    ): Boolean
}
