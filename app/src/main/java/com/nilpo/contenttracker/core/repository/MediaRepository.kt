package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    suspend fun seedSampleDataIfEmpty()

    suspend fun startNewSession(request: AddTrackingSessionRequest)

    suspend fun addTrackedMedia(request: AddTrackedMediaRequest)

    suspend fun updateSessionProgress(sessionId: Long, progressCurrent: Int)

    suspend fun updateSessionStatus(sessionId: Long, status: TrackingStatus)

    suspend fun updateSessionRating(sessionId: Long, rating: Int?)

    suspend fun updateSessionNotes(sessionId: Long, notes: String?)

    suspend fun deletePastSession(sessionId: Long)

    suspend fun addExternalTracking(
        mediaItemId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    )

    suspend fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean)

    suspend fun deleteExternalTracking(externalTrackingId: Long)

    suspend fun updateMediaCollectionName(collectionId: Long, name: String)

    suspend fun deleteMediaCollection(collectionId: Long)

    suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    )

    suspend fun updateSessionPlatform(
        sessionId: Long,
        platformName: String?,
        platformType: ConsumptionPlatformType,
    )
}
