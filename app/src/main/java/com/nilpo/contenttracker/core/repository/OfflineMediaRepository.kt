package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.SeasonProgressEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.database.mapper.toEntity
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.SampleTrackedMedia
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMediaRepository(
    private val mediaDao: MediaDao,
) : MediaRepository {
    override fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>> {
        return mediaDao.observeTrackedMedia(types.map { it.name })
            .map { relations ->
                relations.map { relation ->
                    TrackedMedia(
                        item = relation.item.toDomain(),
                        sessions = relation.sessions.map { it.toDomain() },
                        seasonProgress = mediaDao
                            .getSeasonProgressForMedia(relation.item.id)
                            .map { it.toDomain() },
                        externalRatings = relation.externalRatings.map { it.toDomain() },
                        externalTracking = relation.externalTracking.map { it.toDomain() },
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

    override suspend fun startNewSession(mediaItemId: Long) {
        val sessions = mediaDao.getTrackingSessions(mediaItemId)
        val latestSession = sessions.maxByOrNull { it.sessionNumber }
        val newSessionNumber = (latestSession?.sessionNumber ?: 0) + 1

        val newSession = if (latestSession == null) {
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = newSessionNumber,
                status = TrackingStatus.InProgress.name,
            )
        } else {
            latestSession.copy(
                id = 0,
                sessionNumber = newSessionNumber,
                status = TrackingStatus.InProgress.name,
                progressCurrent = 0,
                rating = null,
                notes = null,
                startedAtEpochDay = null,
                finishedAtEpochDay = null,
            )
        }

        val newSessionId = mediaDao.insertTrackingSession(newSession)

        if (latestSession != null) {
            val previousSeasons = mediaDao.getSeasonProgressForSession(latestSession.id)
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
                status = TrackingStatus.Planned.name,
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
        mediaDao.updateSessionProgress(
            sessionId = sessionId,
            progressCurrent = progressCurrent.coerceAtLeast(0),
        )
    }
}
