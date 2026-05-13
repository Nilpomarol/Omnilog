package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    suspend fun seedSampleDataIfEmpty()

    suspend fun startNewSession(mediaItemId: Long)

    suspend fun addTrackedMedia(request: AddTrackedMediaRequest)

    suspend fun updateSessionProgress(sessionId: Long, progressCurrent: Int)

    suspend fun updateSessionStatus(sessionId: Long, status: TrackingStatus)

    suspend fun updateSessionRating(sessionId: Long, rating: Int?)

    suspend fun updateSessionNotes(sessionId: Long, notes: String?)
}
