package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.database.mapper.toEntity
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.SampleTrackedMedia
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMediaRepository(
    private val mediaDao: MediaDao,
) : MediaRepository {
    override fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>> {
        val typeNames = types.map { it.name }

        return mediaDao.observeTrackedMedia(typeNames).let { flow ->
            flow.map { relations ->
                relations.map { relation ->
                    TrackedMedia(
                        item = relation.item.toDomain(),
                        sessions = relation.sessions.map { it.toDomain() },
                        externalRatings = relation.externalRatings.map { it.toDomain() },
                        externalTracking = relation.externalTracking.map { it.toDomain() },
                    )
                }
            }
        }
    }

    override suspend fun seedSampleDataIfEmpty() {
        if (mediaDao.countMediaItems() > 0) {
            return
        }

        SampleTrackedMedia.items.forEach { trackedMedia ->
            mediaDao.insertMediaItem(trackedMedia.item.toEntity())
            trackedMedia.sessions.forEach { session ->
                mediaDao.insertTrackingSession(session.toEntity())
            }
            trackedMedia.externalRatings.forEach { rating ->
                mediaDao.insertExternalRating(rating.toEntity())
            }
            trackedMedia.externalTracking.forEach { tracking ->
                mediaDao.insertExternalTracking(tracking.toEntity())
            }
        }
    }

    override suspend fun startNewSession(request: AddTrackingSessionRequest) {
        val sessions = mediaDao.getTrackingSessions(request.mediaItemId)
        val latestSession = sessions.maxByOrNull { it.sessionNumber }
        val mediaItem = mediaDao.getMediaItem(request.mediaItemId) ?: return
        val newSessionNumber = (latestSession?.sessionNumber ?: 0) + 1
        val validProgress = mediaItem.progressTotal?.let { maxProgress ->
            request.progressCurrent.coerceIn(0, maxProgress)
        } ?: request.progressCurrent.coerceAtLeast(0)
        val validPlatformName = request.platformName?.trim()?.takeIf { it.isNotBlank() }

        val newSession = if (latestSession == null) {
            TrackingSessionEntity(
                mediaItemId = request.mediaItemId,
                sessionNumber = newSessionNumber,
                status = request.status.name,
                progressCurrent = validProgress,
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
            )
        } else {
            latestSession.copy(
                id = 0,
                sessionNumber = newSessionNumber,
                status = request.status.name,
                progressCurrent = validProgress,
                rating = null,
                notes = null,
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
                startedAtEpochDay = null,
                finishedAtEpochDay = null,
            )
        }

        mediaDao.insertTrackingSession(newSession)
    }

    override suspend fun addTrackedMedia(request: AddTrackedMediaRequest) {
        val mediaItemId = mediaDao.insertMediaItem(
            MediaItemEntity(
                type = request.type.name,
                title = request.title.trim(),
                progressTotal = request.progressTotal?.coerceAtLeast(0),
                isOwned = request.isOwned,
                ownershipType = request.ownershipType.name,
            ),
        )

        mediaDao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = 1,
                status = request.initialStatus.name,
                progressCurrent = 0,
                platformName = request.platformName?.trim()?.takeIf { it.isNotBlank() },
                platformType = request.platformType.name,
            ),
        )
    }

    override suspend fun updateSessionProgress(sessionId: Long, progressCurrent: Int) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val mediaItem = mediaDao.getMediaItem(session.mediaItemId) ?: return
        val validProgress = mediaItem.progressTotal?.let { maxProgress ->
            progressCurrent.coerceIn(0, maxProgress)
        } ?: progressCurrent.coerceAtLeast(0)

        mediaDao.updateSessionProgress(
            sessionId = sessionId,
            progressCurrent = validProgress,
        )
    }

    override suspend fun updateSessionStatus(sessionId: Long, status: TrackingStatus) {
        mediaDao.updateSessionStatus(
            sessionId = sessionId,
            status = status.name,
        )
    }

    override suspend fun updateSessionRating(sessionId: Long, rating: Int?) {
        val validRating = rating?.coerceIn(1, 10)
        mediaDao.updateSessionRating(
            sessionId = sessionId,
            rating = validRating,
        )
    }

    override suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        mediaDao.updateSessionNotes(
            sessionId = sessionId,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun deletePastSession(sessionId: Long) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val sessions = mediaDao.getTrackingSessions(session.mediaItemId)
        val latestSessionNumber = sessions.maxOfOrNull { it.sessionNumber } ?: return

        if (sessions.size <= 1 || session.sessionNumber == latestSessionNumber) {
            return
        }

        mediaDao.deleteTrackingSession(sessionId)
    }

    override suspend fun addExternalTracking(
        mediaItemId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    ) {
        mediaDao.insertExternalTracking(
            ExternalTrackingEntity(
                mediaItemId = mediaItemId,
                source = source.name,
                externalItemId = externalItemId?.trim()?.takeIf { it.isNotBlank() },
                url = url?.trim()?.takeIf { it.isNotBlank() },
                isSynced = false,
            ),
        )
    }

    override suspend fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean) {
        mediaDao.updateExternalTrackingSynced(
            externalTrackingId = externalTrackingId,
            isSynced = isSynced,
        )
    }

    override suspend fun deleteExternalTracking(externalTrackingId: Long) {
        mediaDao.deleteExternalTracking(externalTrackingId)
    }

    override suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val validTotal = progressTotal?.coerceAtLeast(0)

        mediaDao.updateMediaItemDetails(
            mediaItemId = mediaItemId,
            title = validTitle,
            progressTotal = validTotal,
            isOwned = ownershipType != OwnershipType.None,
            ownershipType = ownershipType.name,
        )

        validTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(mediaItemId, total)
        }
    }

    override suspend fun updateSessionPlatform(
        sessionId: Long,
        platformName: String?,
        platformType: ConsumptionPlatformType,
    ) {
        val validPlatformName = platformName?.trim()?.takeIf { it.isNotBlank() }

        mediaDao.updateSessionPlatform(
            sessionId = sessionId,
            platformName = validPlatformName,
            platformType = validPlatformName?.let { platformType.name },
        )
    }
}
