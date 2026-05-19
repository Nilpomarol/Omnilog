package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.database.mapper.toEntity
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.SampleTrackedMedia
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class OfflineMediaRepository(
    private val mediaDao: MediaDao,
) : MediaRepository {
    override fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>> {
        val typeNames = types.map { it.name }

        return combine(
            mediaDao.observeTrackedMedia(typeNames),
            mediaDao.observeMediaCollections(),
        ) { relations, collections ->
            relations.map { relation ->
                val sameTypeCollectionIds = relations
                    .filter { otherRelation -> otherRelation.item.type == relation.item.type }
                    .mapNotNull { otherRelation -> otherRelation.item.collectionId }
                    .toSet()
                val availableCollections = collections
                    .filter { collection -> collection.id in sameTypeCollectionIds }
                    .map { it.toDomain() }
                TrackedMedia(
                    item = relation.item.toDomain(),
                    collection = relation.collection?.toDomain(),
                    availableCollections = availableCollections,
                    sessions = relation.sessions.map { it.toDomain() },
                    credits = relation.credits.map { it.toDomain() },
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
            trackedMedia.credits.forEach { credit ->
                mediaDao.insertMediaCredit(credit.toEntity())
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
            .put("schemaVersion", 3)
            .put("collections", JSONArray(mediaDao.getMediaCollections().map { it.toJson() }))
            .put("mediaItems", JSONArray(mediaDao.getMediaItems().map { it.toJson() }))
            .put("mediaCredits", JSONArray(mediaDao.getMediaCredits().map { it.toJson() }))
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
            mediaCreditCount = root.optJSONArray("mediaCredits")?.length() ?: 0,
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
            mediaCredits = root.optJSONArray("mediaCredits")?.mapObjects { it.toMediaCreditEntity() } ?: emptyList(),
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
                rating = request.rating?.coerceIn(1, 10),
                notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
                startedAtEpochDay = request.startedAt?.toEpochDay(),
                finishedAtEpochDay = request.finishedAt?.toEpochDay(),
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        } else {
            latestSession.copy(
                id = 0,
                sessionNumber = newSessionNumber,
                status = request.status.name,
                progressCurrent = validProgress,
                rating = request.rating?.coerceIn(1, 10),
                notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
                startedAtEpochDay = request.startedAt?.toEpochDay(),
                finishedAtEpochDay = request.finishedAt?.toEpochDay(),
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        }

        mediaDao.insertTrackingSession(newSession)
        mediaDao.updateExternalTrackingSyncedForMedia(request.mediaItemId, isSynced = false)
    }

    override suspend fun addTrackedMedia(request: AddTrackedMediaRequest) {
        val mediaItemId = mediaDao.insertMediaItem(
            MediaItemEntity(
                type = request.type.name,
                title = request.title.trim(),
                progressTotal = request.progressTotal?.coerceAtLeast(0),
                originalTitle = request.originalTitle?.trim()?.takeIf { it.isNotBlank() },
                releaseYear = request.releaseYear,
                genresJson = request.genres.toJsonArrayString(),
                creatorsJson = request.creators.toJsonArrayString(),
                coverUrl = request.coverUrl,
                synopsis = request.synopsis,
                sourceUrl = request.sourceUrl,
                externalRatingScore = request.externalRating?.score,
                externalRatingMax = request.externalRating?.maxScore,
                externalRatingVoteCount = request.externalRating?.voteCount,
                popularityScore = request.popularityScore,
                rankingPosition = request.rankingPosition,
                rankingLabel = request.rankingLabel,
                providerCollectionTitle = request.providerCollectionTitle,
                ratingDistributionJson = request.ratingDistributionJson,
                popularityJson = request.popularityJson,
                rankingJson = request.rankingJson,
                metadataLastFetchedAtEpochMillis = request.metadataSource?.let { System.currentTimeMillis() },
                metadataExternalId = request.metadataExternalId,
                metadataSource = request.metadataSource?.name,
                isOwned = request.isOwned,
                ownershipType = request.ownershipType.name,
            ),
        )

        if (request.credits.isNotEmpty()) {
            mediaDao.insertMediaCredits(
                request.credits
                    .filter { it.personName.isNotBlank() }
                    .mapIndexed { index, credit ->
                        credit.copy(
                            id = 0,
                            mediaItemId = mediaItemId,
                            personName = credit.personName.trim(),
                            characterName = credit.characterName?.trim()?.takeIf { it.isNotBlank() },
                            sortOrder = credit.sortOrder.takeIf { it > 0 } ?: index,
                            metadataSource = credit.metadataSource ?: request.metadataSource,
                        ).toEntity()
                    },
            )
        }

        if (request.externalRatings.isNotEmpty()) {
            request.externalRatings
                .filter { it.score > 0.0 && it.maxScore > 0.0 }
                .forEach { rating ->
                    mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = mediaItemId,
                            source = rating.source.name,
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                        ),
                    )
                }
        }

        mediaDao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = 1,
                status = request.initialStatus.name,
                progressCurrent = request.progressTotal?.let { total ->
                    request.initialProgress.coerceIn(0, total)
                } ?: request.initialProgress.coerceAtLeast(0),
                rating = request.initialRating?.coerceIn(1, 10),
                notes = request.initialNotes?.trim()?.takeIf { it.isNotBlank() },
                platformName = request.platformName?.trim()?.takeIf { it.isNotBlank() },
                platformType = request.platformType.name,
                startedAtEpochDay = request.initialStartedAt?.toEpochDay(),
                finishedAtEpochDay = request.initialFinishedAt?.toEpochDay(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        rating: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ) {
        val session = mediaDao.getTrackingSession(sessionId) ?: return
        val mediaItem = mediaDao.getMediaItem(session.mediaItemId) ?: return
        val validProgress = mediaItem.progressTotal?.let { maxProgress ->
            progressCurrent.coerceIn(0, maxProgress)
        } ?: progressCurrent.coerceAtLeast(0)

        mediaDao.updateSessionDetails(
            sessionId = sessionId,
            status = status.name,
            progressCurrent = validProgress,
            rating = rating?.coerceIn(1, 10),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            startedAtEpochDay = startedAt?.toEpochDay(),
            finishedAtEpochDay = finishedAt?.toEpochDay(),
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        mediaDao.updateExternalTrackingSyncedForMedia(session.mediaItemId, isSynced = false)
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

    override suspend fun updateExternalTracking(
        externalTrackingId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    ) {
        mediaDao.updateExternalTracking(
            externalTrackingId = externalTrackingId,
            source = source.name,
            externalItemId = externalItemId?.trim()?.takeIf { it.isNotBlank() },
            url = url?.trim()?.takeIf { it.isNotBlank() },
            isSynced = false,
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

    override suspend fun updateMediaItemMetadata(
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
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val validTotal = progressTotal?.coerceAtLeast(0)

        mediaDao.updateMediaItemMetadata(
            mediaItemId = mediaItemId,
            title = validTitle,
            originalTitle = originalTitle?.trim()?.takeIf { it.isNotBlank() },
            releaseYear = releaseYear?.coerceAtLeast(0),
            progressTotal = validTotal,
            genresJson = genres.cleanMetadataList().toJsonArrayString(),
            creatorsJson = creators.cleanMetadataList().toJsonArrayString(),
            coverUrl = coverUrl?.trim()?.takeIf { it.isNotBlank() },
            synopsis = synopsis?.trim()?.takeIf { it.isNotBlank() },
            sourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() },
        )

        validTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun refreshMediaItemMetadata(
        mediaItemId: Long,
        metadataRepository: MetadataRepository,
    ): Boolean {
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return false
        val metadataSource = currentItem.metadataSource?.let { source ->
            runCatching { MetadataSource.valueOf(source) }.getOrNull()
        } ?: return false
        val metadataExternalId = currentItem.metadataExternalId?.takeIf { it.isNotBlank() } ?: return false
        val mediaType = runCatching { MediaType.valueOf(currentItem.type) }.getOrNull() ?: return false
        val existingRatings = mediaDao.getExternalRatingsForItem(mediaItemId)
            .map { it.toDomain() }
            .map { rating ->
                MetadataExternalRatingSuggestion(
                    source = rating.source,
                    score = rating.score,
                    maxScore = rating.maxScore,
                    voteCount = rating.voteCount,
                )
            }
        val existingPrimaryRating = currentItem.externalRatingScore?.let { score ->
            val maxScore = currentItem.externalRatingMax ?: return@let null
            if (score > 0.0 && maxScore > 0.0) {
                MetadataRatingSuggestion(
                    score = score,
                    maxScore = maxScore,
                    voteCount = currentItem.externalRatingVoteCount,
                )
            } else {
                null
            }
        }
        val refreshed = metadataRepository.getSuggestionDetails(
            MetadataSuggestion(
                source = metadataSource,
                externalId = metadataExternalId,
                mediaType = mediaType,
                title = currentItem.title,
                originalTitle = currentItem.originalTitle,
                releaseYear = currentItem.releaseYear,
                genres = currentItem.genresJson.toStringList(),
                creators = currentItem.creatorsJson.toStringList(),
                progressTotal = currentItem.progressTotal,
                coverUrl = currentItem.coverUrl,
                synopsis = currentItem.synopsis,
                sourceUrl = currentItem.sourceUrl,
                popularityScore = currentItem.popularityScore,
                rankingPosition = currentItem.rankingPosition,
                rankingLabel = currentItem.rankingLabel,
                ratingDistributionJson = currentItem.ratingDistributionJson,
                popularityJson = currentItem.popularityJson,
                rankingJson = currentItem.rankingJson,
                externalRating = existingPrimaryRating,
                externalRatings = existingRatings,
            ),
        )
        val refreshedTotal = refreshed.progressTotal?.coerceAtLeast(0)
        val refreshedRatings = refreshed.externalRatings
            .ifEmpty { existingRatings }

        mediaDao.refreshMediaItemMetadata(
            mediaItemId = mediaItemId,
            title = refreshed.title.trim().takeIf { it.isNotBlank() } ?: currentItem.title,
            originalTitle = refreshed.originalTitle?.trim()?.takeIf { it.isNotBlank() },
            releaseYear = refreshed.releaseYear,
            progressTotal = refreshedTotal,
            genresJson = refreshed.genres.cleanMetadataList().toJsonArrayString(),
            creatorsJson = refreshed.creators.cleanMetadataList().toJsonArrayString(),
            coverUrl = refreshed.coverUrl?.trim()?.takeIf { it.isNotBlank() },
            synopsis = refreshed.synopsis?.trim()?.takeIf { it.isNotBlank() },
            sourceUrl = refreshed.sourceUrl?.trim()?.takeIf { it.isNotBlank() },
            externalRatingScore = refreshed.externalRating?.score,
            externalRatingMax = refreshed.externalRating?.maxScore,
            externalRatingVoteCount = refreshed.externalRating?.voteCount,
            popularityScore = refreshed.popularityScore,
            rankingPosition = refreshed.rankingPosition,
            rankingLabel = refreshed.rankingLabel,
            providerCollectionTitle = refreshed.collectionTitle,
            ratingDistributionJson = refreshed.ratingDistributionJson,
            popularityJson = refreshed.popularityJson,
            rankingJson = refreshed.rankingJson,
            metadataLastFetchedAtEpochMillis = System.currentTimeMillis(),
        )

        mediaDao.deleteMediaCreditsForItem(mediaItemId)
        refreshed.credits
            .filter { it.personName.isNotBlank() }
            .mapIndexed { index, credit ->
                credit.copy(
                    id = 0,
                    mediaItemId = mediaItemId,
                    personName = credit.personName.trim(),
                    characterName = credit.characterName?.trim()?.takeIf { it.isNotBlank() },
                    sortOrder = credit.sortOrder.takeIf { it > 0 } ?: index,
                    metadataSource = credit.metadataSource ?: metadataSource,
                ).toEntity()
            }
            .takeIf { it.isNotEmpty() }
            ?.let { credits -> mediaDao.insertMediaCredits(credits) }

        mediaDao.deleteExternalRatingsForItem(mediaItemId)
        refreshedRatings
            .filter { it.score > 0.0 && it.maxScore > 0.0 }
            .forEach { rating ->
                mediaDao.insertExternalRating(
                    ExternalRatingEntity(
                        mediaItemId = mediaItemId,
                        source = rating.source.name,
                        score = rating.score,
                        maxScore = rating.maxScore,
                        voteCount = rating.voteCount,
                    ),
                )
            }

        refreshedTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }

        return true
    }

}

private fun parseBackupRoot(json: String): JSONObject {
    val root = JSONObject(json)
    val schemaVersion = root.optInt("schemaVersion", -1)
    if (schemaVersion !in 1..3) {
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
        .putNullable("originalTitle", originalTitle)
        .putNullable("releaseYear", releaseYear)
        .put("genres", JSONArray(genresJson.toStringList()))
        .put("creators", JSONArray(creatorsJson.toStringList()))
        .putNullable("coverUrl", coverUrl)
        .putNullable("synopsis", synopsis)
        .putNullable("sourceUrl", sourceUrl)
        .putNullable("externalRatingScore", externalRatingScore)
        .putNullable("externalRatingMax", externalRatingMax)
        .putNullable("externalRatingVoteCount", externalRatingVoteCount)
        .putNullable("popularityScore", popularityScore)
        .putNullable("rankingPosition", rankingPosition)
        .putNullable("rankingLabel", rankingLabel)
        .putNullable("providerCollectionTitle", providerCollectionTitle)
        .putNullable("ratingDistributionJson", ratingDistributionJson)
        .putNullable("popularityJson", popularityJson)
        .putNullable("rankingJson", rankingJson)
        .putNullable("metadataLastFetchedAtEpochMillis", metadataLastFetchedAtEpochMillis)
        .putNullable("metadataExternalId", metadataExternalId)
        .putNullable("metadataSource", metadataSource)
        .put("isOwned", isOwned)
        .put("ownershipType", ownershipType)
}

private fun MediaCreditEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("personName", personName)
        .put("roleType", roleType)
        .putNullable("characterName", characterName)
        .put("sortOrder", sortOrder)
        .putNullable("metadataSource", metadataSource)
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
        originalTitle = optNullableString("originalTitle"),
        releaseYear = optNullableInt("releaseYear"),
        genresJson = optStringArray("genres").toJsonArrayString(),
        creatorsJson = optStringArray("creators").toJsonArrayString(),
        coverUrl = optNullableString("coverUrl"),
        synopsis = optNullableString("synopsis"),
        sourceUrl = optNullableString("sourceUrl"),
        externalRatingScore = optNullableDouble("externalRatingScore"),
        externalRatingMax = optNullableDouble("externalRatingMax"),
        externalRatingVoteCount = optNullableInt("externalRatingVoteCount"),
        popularityScore = optNullableDouble("popularityScore"),
        rankingPosition = optNullableInt("rankingPosition"),
        rankingLabel = optNullableString("rankingLabel"),
        providerCollectionTitle = optNullableString("providerCollectionTitle"),
        ratingDistributionJson = optNullableString("ratingDistributionJson"),
        popularityJson = optNullableString("popularityJson"),
        rankingJson = optNullableString("rankingJson"),
        metadataLastFetchedAtEpochMillis = optNullableLong("metadataLastFetchedAtEpochMillis"),
        metadataExternalId = optNullableString("metadataExternalId") ?: optNullableString("externalId"),
        metadataSource = optNullableString("metadataSource") ?: optNullableString("sourceApi"),
        isOwned = optBoolean("isOwned", false),
        ownershipType = optString("ownershipType", "None"),
    )
}

private fun JSONObject.toMediaCreditEntity(): MediaCreditEntity {
    return MediaCreditEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        personName = getString("personName"),
        roleType = optString("roleType", MediaCreditRole.Cast.name),
        characterName = optNullableString("characterName"),
        sortOrder = optInt("sortOrder", 0),
        metadataSource = optNullableString("metadataSource"),
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

private fun JSONObject.optNullableDouble(name: String): Double? {
    return if (isNull(name)) null else optDouble(name)
}

private fun JSONObject.optNullableLong(name: String): Long? {
    return if (isNull(name)) null else optLong(name)
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}

private fun JSONObject.optStringArray(name: String): List<String> {
    val array = optJSONArray(name) ?: return emptyList()
    return List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
}

private fun String?.toStringList(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}

private fun List<String>.toJsonArrayString(): String? {
    if (isEmpty()) return null
    return JSONArray(this).toString()
}

private fun List<String>.cleanMetadataList(): List<String> =
    map { it.trim() }.filter { it.isNotBlank() }.distinct()
