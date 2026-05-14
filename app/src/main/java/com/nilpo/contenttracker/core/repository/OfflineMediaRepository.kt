package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.SeasonProgressEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
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
import kotlinx.coroutines.flow.combine

class OfflineMediaRepository(
    private val mediaDao: MediaDao,
) : MediaRepository {
    override fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>> {
        val typeNames = types.map { it.name }

        return combine(
            mediaDao.observeTrackedMedia(typeNames),
            mediaDao.observeSeasonProgress(typeNames),
            mediaDao.observeExternalRatings(typeNames),
            mediaDao.observeExternalTracking(typeNames),
        ) { relations, seasonProgress, externalRatings, externalTracking ->
                relations.map { relation ->
                    val sessionIds = relation.sessions.map { it.id }.toSet()
                    TrackedMedia(
                        item = relation.item.toDomain(),
                        sessions = relation.sessions.map { it.toDomain() },
                        seasonProgress = seasonProgress
                            .filter { it.trackingSessionId in sessionIds }
                            .map { it.toDomain() },
                        externalRatings = externalRatings
                            .filter { it.mediaItemId == relation.item.id }
                            .map { it.toDomain() },
                        externalTracking = externalTracking
                            .filter { it.mediaItemId == relation.item.id }
                            .map { it.toDomain() },
                    )
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
            trackedMedia.seasonProgress.forEach { season ->
                mediaDao.insertSeasonProgress(season.toEntity())
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
        val newSessionNumber = (latestSession?.sessionNumber ?: 0) + 1
        val validTotal = request.progressTotal?.coerceAtLeast(0)
        val validProgress = validTotal?.let { maxProgress ->
            request.progressCurrent.coerceIn(0, maxProgress)
        } ?: request.progressCurrent.coerceAtLeast(0)
        val validPlatformName = request.platformName?.trim()?.takeIf { it.isNotBlank() }

        val newSession = if (latestSession == null) {
            TrackingSessionEntity(
                mediaItemId = request.mediaItemId,
                sessionNumber = newSessionNumber,
                status = request.status.name,
                progressCurrent = validProgress,
                progressTotal = validTotal,
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
            )
        } else {
            latestSession.copy(
                id = 0,
                sessionNumber = newSessionNumber,
                status = request.status.name,
                progressCurrent = validProgress,
                progressTotal = validTotal,
                rating = null,
                notes = null,
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
                startedAtEpochDay = null,
                finishedAtEpochDay = null,
            )
        }

        val newSessionId = mediaDao.insertTrackingSession(newSession)
        var copiedSeasonCount = 0

        if (latestSession != null) {
            val previousSeasons = mediaDao.getSeasonProgressForSession(latestSession.id)
            copiedSeasonCount = previousSeasons.size
            previousSeasons.forEach { season ->
                mediaDao.insertSeasonProgress(
                    season.copy(
                        id = 0,
                        trackingSessionId = newSessionId,
                        progressCurrent = 0,
                    ),
                )
            }
        }

        if (copiedSeasonCount > 0) {
            updateSessionProgressFromSeasons(newSessionId)
        }
    }

    override suspend fun addTrackedMedia(request: AddTrackedMediaRequest) {
        val mediaItemId = mediaDao.insertMediaItem(
            MediaItemEntity(
                type = request.type.name,
                title = request.title.trim(),
                isOwned = request.isOwned,
                ownershipType = request.ownershipType.name,
            ),
        )

        val sessionId = mediaDao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = 1,
                status = request.initialStatus.name,
                progressCurrent = 0,
                progressTotal = request.progressTotal,
                platformName = request.platformName?.trim()?.takeIf { it.isNotBlank() },
                platformType = request.platformType.name,
            ),
        )

        if (request.type == MediaType.Anime || request.type == MediaType.TvShow) {
            mediaDao.insertSeasonProgress(
                SeasonProgressEntity(
                    trackingSessionId = sessionId,
                    seasonNumber = 1,
                    progressCurrent = 0,
                    progressTotal = request.progressTotal,
                ),
            )
        }
    }

    override suspend fun updateSessionProgress(sessionId: Long, progressCurrent: Int) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val validProgress = session.progressTotal?.let { maxProgress ->
            progressCurrent.coerceIn(0, maxProgress)
        } ?: progressCurrent.coerceAtLeast(0)

        mediaDao.updateSessionProgress(
            sessionId = sessionId,
            progressCurrent = validProgress,
        )
    }

    override suspend fun updateSessionProgressTotal(sessionId: Long, progressTotal: Int?) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val validTotal = progressTotal?.coerceAtLeast(0)
        val validProgress = validTotal?.let { maxProgress ->
            session.progressCurrent.coerceIn(0, maxProgress)
        } ?: session.progressCurrent.coerceAtLeast(0)

        mediaDao.updateSessionProgressTotal(
            sessionId = sessionId,
            progressCurrent = validProgress,
            progressTotal = validTotal,
        )

        val seasons = mediaDao.getSeasonProgressForSession(sessionId)
        if (seasons.size == 1) {
            val season = seasons.first()
            val validSeasonProgress = validTotal?.let { maxProgress ->
                season.progressCurrent.coerceIn(0, maxProgress)
            } ?: season.progressCurrent.coerceAtLeast(0)

            mediaDao.updateSeasonProgressTotal(
                seasonProgressId = season.id,
                progressCurrent = validSeasonProgress,
                progressTotal = validTotal,
            )
        }
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

    override suspend fun updateSeasonProgress(seasonProgressId: Long, progressCurrent: Int) {
        val seasonProgress = mediaDao.getSeasonProgress(seasonProgressId) ?: return
        val validProgress = seasonProgress.progressTotal?.let { maxProgress ->
            progressCurrent.coerceIn(0, maxProgress)
        } ?: progressCurrent.coerceAtLeast(0)

        mediaDao.updateSeasonProgress(
            seasonProgressId = seasonProgressId,
            progressCurrent = validProgress,
        )

        updateSessionProgressFromSeasons(seasonProgress.trackingSessionId)
    }

    override suspend fun updateSeasonProgressTotal(seasonProgressId: Long, progressTotal: Int?) {
        val seasonProgress = mediaDao.getSeasonProgress(seasonProgressId) ?: return
        val validTotal = progressTotal?.coerceAtLeast(0)
        val validProgress = validTotal?.let { maxProgress ->
            seasonProgress.progressCurrent.coerceIn(0, maxProgress)
        } ?: seasonProgress.progressCurrent.coerceAtLeast(0)

        mediaDao.updateSeasonProgressTotal(
            seasonProgressId = seasonProgressId,
            progressCurrent = validProgress,
            progressTotal = validTotal,
        )

        updateSessionProgressFromSeasons(seasonProgress.trackingSessionId)
    }

    override suspend fun deleteSeasonProgress(seasonProgressId: Long) {
        val seasonProgress = mediaDao.getSeasonProgress(seasonProgressId) ?: return
        mediaDao.deleteSeasonProgress(seasonProgressId)
        updateSessionProgressFromSeasons(seasonProgress.trackingSessionId)
    }

    override suspend fun addSeasonProgress(
        sessionId: Long,
        seasonNumber: Int,
        progressTotal: Int?,
    ) {
        if (seasonNumber <= 0) {
            return
        }

        val existingSeasons = mediaDao.getSeasonProgressForSession(sessionId)
        if (existingSeasons.any { it.seasonNumber == seasonNumber }) {
            return
        }

        mediaDao.insertSeasonProgress(
            SeasonProgressEntity(
                trackingSessionId = sessionId,
                seasonNumber = seasonNumber,
                progressCurrent = 0,
                progressTotal = progressTotal?.coerceAtLeast(0),
            ),
        )

        updateSessionProgressFromSeasons(sessionId)
    }

    private suspend fun updateSessionProgressFromSeasons(sessionId: Long) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val seasons = mediaDao.getSeasonProgressForSession(sessionId)
        if (seasons.isEmpty()) {
            mediaDao.updateSessionProgressTotal(
                sessionId = session.id,
                progressCurrent = 0,
                progressTotal = null,
            )
            return
        }

        val progressCurrent = seasons.sumOf { it.progressCurrent }
        val progressTotal = if (seasons.all { it.progressTotal != null }) {
            seasons.sumOf { it.progressTotal ?: 0 }
        } else {
            null
        }
        val validProgressCurrent = progressTotal?.let { total ->
            progressCurrent.coerceIn(0, total)
        } ?: progressCurrent.coerceAtLeast(0)

        mediaDao.updateSessionProgressTotal(
            sessionId = session.id,
            progressCurrent = validProgressCurrent,
            progressTotal = progressTotal,
        )
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
        ownershipType: OwnershipType,
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return

        mediaDao.updateMediaItemDetails(
            mediaItemId = mediaItemId,
            title = validTitle,
            isOwned = ownershipType != OwnershipType.None,
            ownershipType = ownershipType.name,
        )
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
