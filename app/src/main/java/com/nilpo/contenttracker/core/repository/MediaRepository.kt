package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    suspend fun seedSampleDataIfEmpty()

    suspend fun startNewSession(mediaItemId: Long)
}
