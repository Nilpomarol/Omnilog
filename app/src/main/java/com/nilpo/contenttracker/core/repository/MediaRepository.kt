package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaType
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
    val externalRatingCount: Int,
    val externalTrackingCount: Int,
)

data class CollectionItemOrder(
    val mediaItemId: Long,
    val sortOrder: Double,
)

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    suspend fun seedSampleDataIfEmpty()

    suspend fun exportBackupJson(): String

    suspend fun previewBackupJson(json: String): BackupPreview

    suspend fun importBackupJson(json: String)

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

    suspend fun deletePastSession(sessionId: Long)

    suspend fun deleteMediaItem(mediaItemId: Long)

    suspend fun addExternalTracking(
        mediaItemId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    )

    suspend fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean)

    suspend fun updateExternalTracking(
        externalTrackingId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    )

    suspend fun deleteExternalTracking(externalTrackingId: Long)

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
        progressTotal: Int?,
        genres: List<String>,
        creators: List<String>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    )

    suspend fun refreshMediaItemMetadata(mediaItemId: Long, metadataRepository: MetadataRepository): Boolean
}
