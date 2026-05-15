package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
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
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject

class OfflineMediaRepository(
    private val mediaDao: MediaDao,
) : MediaRepository {
    override fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>> {
        val typeNames = types.map { it.name }

        return combine(
            mediaDao.observeTrackedMedia(typeNames),
            mediaDao.observeMediaCollections(),
        ) { relations, collections ->
            val availableCollections = collections.map { it.toDomain() }
            relations.map { relation ->
                TrackedMedia(
                    item = relation.item.toDomain(),
                    collection = relation.collection?.toDomain(),
                    availableCollections = availableCollections,
                    sessions = relation.sessions.map { it.toDomain() },
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
            trackedMedia.externalRatings.forEach { rating ->
                mediaDao.insertExternalRating(rating.toEntity())
            }
            trackedMedia.externalTracking.forEach { tracking ->
                mediaDao.insertExternalTracking(tracking.toEntity())
            }
        }
    }

    override suspend fun exportBackupJson(): String {
        return JSONObject()
            .put("schemaVersion", 2)
            .put("collections", JSONArray(mediaDao.getMediaCollections().map { it.toJson() }))
            .put("mediaItems", JSONArray(mediaDao.getMediaItems().map { it.toJson() }))
            .put("trackingSessions", JSONArray(mediaDao.getAllTrackingSessions().map { it.toJson() }))
            .put("externalRatings", JSONArray(mediaDao.getExternalRatings().map { it.toJson() }))
            .put("externalTracking", JSONArray(mediaDao.getExternalTracking().map { it.toJson() }))
            .toString(2)
    }

    override suspend fun previewBackupJson(json: String): BackupPreview {
        val root = parseBackupRoot(json)

        return BackupPreview(
            collectionCount = root.getJSONArray("collections").length(),
            mediaItemCount = root.getJSONArray("mediaItems").length(),
            trackingSessionCount = root.getJSONArray("trackingSessions").length(),
            externalRatingCount = root.getJSONArray("externalRatings").length(),
            externalTrackingCount = root.getJSONArray("externalTracking").length(),
        )
    }

    override suspend fun importBackupJson(json: String) {
        val root = parseBackupRoot(json)
        mediaDao.replaceAllData(
            collections = root.getJSONArray("collections").mapObjects { it.toMediaCollectionEntity() },
            mediaItems = root.getJSONArray("mediaItems").mapObjects { it.toMediaItemEntity() },
            sessions = root.getJSONArray("trackingSessions").mapObjects { it.toTrackingSessionEntity() },
            externalRatings = root.getJSONArray("externalRatings").mapObjects { it.toExternalRatingEntity() },
            externalTracking = root.getJSONArray("externalTracking").mapObjects { it.toExternalTrackingEntity() },
        )
    }

    override suspend fun startNewSession(request: AddTrackingSessionRequest) {
        val sessions = mediaDao.getTrackingSessions(request.mediaItemId)
        val latestSession = sessions.maxByOrNull { it.sessionNumber }
        val mediaItem = mediaDao.getMediaItem(request.mediaItemId) ?: return
        val newSessionNumber = (latestSession?.sessionNumber ?: 0) + 1
        val updatedAtEpochMillis = System.currentTimeMillis()
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
                updatedAtEpochMillis = updatedAtEpochMillis,
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
                updatedAtEpochMillis = updatedAtEpochMillis,
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
                metadataExternalId = request.metadataExternalId,
                metadataSource = request.metadataSource?.name,
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
                updatedAtEpochMillis = System.currentTimeMillis(),
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
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun updateSessionStatus(sessionId: Long, status: TrackingStatus) {
        mediaDao.updateSessionStatus(
            sessionId = sessionId,
            status = status.name,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun updateSessionRating(sessionId: Long, rating: Int?) {
        val validRating = rating?.coerceIn(1, 10)
        mediaDao.updateSessionRating(
            sessionId = sessionId,
            rating = validRating,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        mediaDao.updateSessionNotes(
            sessionId = sessionId,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            updatedAtEpochMillis = System.currentTimeMillis(),
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

    override suspend fun deleteMediaItem(mediaItemId: Long) {
        if (mediaDao.getMediaItem(mediaItemId) == null) {
            return
        }

        mediaDao.deleteMediaItem(mediaItemId)
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

    override suspend fun updateMediaCollectionName(collectionId: Long, name: String) {
        val validName = name.trim().takeIf { it.isNotBlank() } ?: return
        if (mediaDao.getMediaCollection(collectionId) == null) {
            return
        }

        mediaDao.updateMediaCollectionName(collectionId, validName)
    }

    override suspend fun deleteMediaCollection(collectionId: Long) {
        if (mediaDao.getMediaCollection(collectionId) == null) {
            return
        }

        mediaDao.deleteMediaCollection(collectionId)
    }

    override suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val validTotal = progressTotal?.coerceAtLeast(0)
        val validNewCollectionName = newCollectionName?.trim()?.takeIf { it.isNotBlank() }
        val validCollectionId = when {
            validNewCollectionName != null -> mediaDao.insertMediaCollection(
                MediaCollectionEntity(name = validNewCollectionName),
            )
            collectionId != null && mediaDao.getMediaCollection(collectionId) != null -> collectionId
            else -> null
        }

        mediaDao.updateMediaItemDetails(
            mediaItemId = mediaItemId,
            title = validTitle,
            collectionId = validCollectionId,
            progressTotal = validTotal,
            isOwned = ownershipType != OwnershipType.None,
            ownershipType = ownershipType.name,
        )

        validTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
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
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}

private fun parseBackupRoot(json: String): JSONObject {
    val root = JSONObject(json)
    val schemaVersion = root.optInt("schemaVersion", -1)
    if (schemaVersion !in 1..2) {
        throw UnsupportedBackupSchemaException(schemaVersion)
    }

    return root
}

private fun MediaCollectionEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
}

private fun MediaItemEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("type", type)
        .put("title", title)
        .putNullable("collectionId", collectionId)
        .putNullable("progressTotal", progressTotal)
        .putNullable("coverUrl", coverUrl)
        .putNullable("synopsis", synopsis)
        .putNullable("metadataExternalId", metadataExternalId)
        .putNullable("metadataSource", metadataSource)
        .put("isOwned", isOwned)
        .put("ownershipType", ownershipType)
}

private fun TrackingSessionEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("sessionNumber", sessionNumber)
        .put("status", status)
        .put("progressCurrent", progressCurrent)
        .putNullable("rating", rating)
        .putNullable("notes", notes)
        .putNullable("platformName", platformName)
        .putNullable("platformType", platformType)
        .putNullable("startedAtEpochDay", startedAtEpochDay)
        .putNullable("finishedAtEpochDay", finishedAtEpochDay)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)
}

private fun ExternalRatingEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("source", source)
        .put("score", score)
        .put("maxScore", maxScore)
        .putNullable("voteCount", voteCount)
}

private fun ExternalTrackingEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("source", source)
        .putNullable("externalItemId", externalItemId)
        .putNullable("url", url)
        .put("isSynced", isSynced)
}

private fun JSONObject.toMediaCollectionEntity(): MediaCollectionEntity {
    return MediaCollectionEntity(
        id = getLong("id"),
        name = getString("name"),
    )
}

private fun JSONObject.toMediaItemEntity(): MediaItemEntity {
    return MediaItemEntity(
        id = getLong("id"),
        type = getString("type"),
        title = getString("title"),
        collectionId = optNullableLong("collectionId"),
        progressTotal = optNullableInt("progressTotal"),
        coverUrl = optNullableString("coverUrl"),
        synopsis = optNullableString("synopsis"),
        metadataExternalId = optNullableString("metadataExternalId") ?: optNullableString("externalId"),
        metadataSource = optNullableString("metadataSource") ?: optNullableString("sourceApi"),
        isOwned = optBoolean("isOwned", false),
        ownershipType = optString("ownershipType", "None"),
    )
}

private fun JSONObject.toTrackingSessionEntity(): TrackingSessionEntity {
    return TrackingSessionEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        sessionNumber = getInt("sessionNumber"),
        status = getString("status"),
        progressCurrent = optInt("progressCurrent", 0),
        rating = optNullableInt("rating"),
        notes = optNullableString("notes"),
        platformName = optNullableString("platformName"),
        platformType = optNullableString("platformType"),
        startedAtEpochDay = optNullableLong("startedAtEpochDay"),
        finishedAtEpochDay = optNullableLong("finishedAtEpochDay"),
        updatedAtEpochMillis = optLong("updatedAtEpochMillis", 0),
    )
}

private fun JSONObject.toExternalRatingEntity(): ExternalRatingEntity {
    return ExternalRatingEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        source = getString("source"),
        score = getDouble("score"),
        maxScore = getDouble("maxScore"),
        voteCount = optNullableInt("voteCount"),
    )
}

private fun JSONObject.toExternalTrackingEntity(): ExternalTrackingEntity {
    return ExternalTrackingEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        source = getString("source"),
        externalItemId = optNullableString("externalItemId"),
        url = optNullableString("url"),
        isSynced = optBoolean("isSynced", false),
    )
}

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
    return put(name, value ?: JSONObject.NULL)
}

private fun JSONObject.optNullableString(name: String): String? {
    return if (isNull(name)) null else optString(name)
}

private fun JSONObject.optNullableInt(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}

private fun JSONObject.optNullableLong(name: String): Long? {
    return if (isNull(name)) null else optLong(name)
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}
