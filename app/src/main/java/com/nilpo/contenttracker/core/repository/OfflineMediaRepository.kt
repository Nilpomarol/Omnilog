package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ObjectiveEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.database.mapper.toEntity
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.normalizeSynopsis
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.canonicalUnit
import com.nilpo.contenttracker.core.model.definition
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.SampleTrackedMedia
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
                    sessions = relation.sessions.map { session ->
                        session.toDomain(relation.progressUpdates)
                    },
                    credits = relation.credits.map { it.toDomain() },
                    externalRatings = relation.externalRatings.map { it.toDomain() },
                )
            }
        }
    }

    override fun observeObjectives(): Flow<List<Objective>> = mediaDao.observeObjectives().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addObjective(objective: Objective): Long {
        require(objective.name.isNotBlank())
        require(objective.definition().isValid) { "Objective definitions must be compatible" }
        require(objective.unit == objective.canonicalUnit()) { "Objective units must match their definitions" }
        require(objective.targetValue > 0)
        require(!objective.endDate.isBefore(objective.startDate))
        return mediaDao.insertObjective(objective.toEntity())
    }

    override suspend fun updateObjective(objective: Objective) {
        require(objective.id > 0)
        require(objective.name.isNotBlank())
        require(objective.definition().isValid) { "Objective definitions must be compatible" }
        require(objective.unit == objective.canonicalUnit()) { "Objective units must match their definitions" }
        require(objective.targetValue > 0)
        require(!objective.endDate.isBefore(objective.startDate))
        mediaDao.insertObjective(objective.toEntity())
    }

    override suspend fun deleteObjective(objectiveId: Long) {
        mediaDao.deleteObjective(objectiveId)
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
        }
    }

    override suspend fun exportBackupJson(): String {
        return JSONObject()
            .put("schemaVersion", 5)
            .put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("collections", JSONArray(mediaDao.getMediaCollections().map { it.toJson() }))
            .put("mediaItems", JSONArray(mediaDao.getMediaItems().map { it.toJson() }))
            .put("mediaCredits", JSONArray(mediaDao.getMediaCredits().map { it.toJson() }))
            .put("trackingSessions", JSONArray(mediaDao.getAllTrackingSessions().map { it.toJson() }))
            .put("progressUpdates", JSONArray(mediaDao.getProgressUpdates().map { it.toJson() }))
            .put("externalRatings", JSONArray(mediaDao.getExternalRatings().map { it.toJson() }))
.put("objectives", JSONArray(mediaDao.getObjectives().map { it.toJson() }))
            .toString(2)
    }

    override suspend fun previewBackupJson(json: String): BackupPreview {
        val backup = parseBackupData(json)

        return BackupPreview(
            schemaVersion = backup.schemaVersion,
            exportedAtEpochMillis = backup.exportedAtEpochMillis,
            collectionCount = backup.collections.size,
            mediaItemCount = backup.mediaItems.size,
            mediaCreditCount = backup.mediaCredits.size,
            trackingSessionCount = backup.sessions.size,
            progressUpdateCount = backup.progressUpdates.size,
            externalRatingCount = backup.externalRatings.size,
            objectiveCount = backup.objectives.size,
        )
    }

    override suspend fun importBackupJson(json: String) {
        val backup = parseBackupData(json)
        mediaDao.replaceAllData(
            collections = backup.collections,
            mediaItems = backup.mediaItems,
            mediaCredits = backup.mediaCredits,
            sessions = backup.sessions,
            progressUpdates = backup.progressUpdates,
            externalRatings = backup.externalRatings,
            objectives = backup.objectives,
        )
    }

    override suspend fun previewImdbCsv(csv: String): ImdbCsvPreview {
        val rows = parseImdbCsv(csv)
        val existingKeys = mediaDao.getMediaItems().map { it.importDuplicateKey() }.toMutableSet()
        var importableRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.importDuplicateKey()
            when {
                item.type == null -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    existingKeys += duplicateKey
                    importableRows++
                }
            }
        }

        return ImdbCsvPreview(
            totalRows = rows.size,
            importableRows = importableRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun importImdbCsv(csv: String): ImdbCsvImportResult {
        val rows = parseImdbCsv(csv)
        val existingKeys = mediaDao.getMediaItems().map { it.importDuplicateKey() }.toMutableSet()
        var importedRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.importDuplicateKey()
            when {
                item.type == null -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    addTrackedMedia(item.toAddTrackedMediaRequest())
                    existingKeys += duplicateKey
                    importedRows++
                }
            }
        }

        return ImdbCsvImportResult(
            importedRows = importedRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun previewStoryGraphCsv(csv: String): StoryGraphCsvPreview {
        val rows = parseStoryGraphCsv(csv)
        val existingKeys = mediaDao.getMediaItems().map { it.storyGraphDuplicateKey() }.toMutableSet()
        var importableRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.storyGraphDuplicateKey()
            when {
                item.title.isBlank() -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    existingKeys += duplicateKey
                    importableRows++
                }
            }
        }

        return StoryGraphCsvPreview(
            totalRows = rows.size,
            importableRows = importableRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun importStoryGraphCsv(csv: String): StoryGraphCsvImportResult {
        val rows = parseStoryGraphCsv(csv)
        val existingKeys = mediaDao.getMediaItems().map { it.storyGraphDuplicateKey() }.toMutableSet()
        var importedRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.storyGraphDuplicateKey()
            when {
                item.title.isBlank() -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    addTrackedMedia(item.toAddTrackedMediaRequest())
                    existingKeys += duplicateKey
                    importedRows++
                }
            }
        }

        return StoryGraphCsvImportResult(
            importedRows = importedRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun previewMyAnimeListXml(xml: String): MyAnimeListXmlPreview {
        val rows = parseMyAnimeListXml(xml)
        val existingKeys = mediaDao.getMediaItems().map { it.myAnimeListDuplicateKey() }.toMutableSet()
        var importableRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.myAnimeListDuplicateKey()
            when {
                item.title.isBlank() -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    existingKeys += duplicateKey
                    importableRows++
                }
            }
        }

        return MyAnimeListXmlPreview(
            totalRows = rows.size,
            importableRows = importableRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun importMyAnimeListXml(xml: String): MyAnimeListXmlImportResult {
        val rows = parseMyAnimeListXml(xml)
        val existingKeys = mediaDao.getMediaItems().map { it.myAnimeListDuplicateKey() }.toMutableSet()
        var importedRows = 0
        var skippedDuplicateRows = 0
        var unsupportedRows = 0

        rows.forEach { item ->
            val duplicateKey = item.myAnimeListDuplicateKey()
            when {
                item.title.isBlank() -> unsupportedRows++
                duplicateKey in existingKeys -> skippedDuplicateRows++
                else -> {
                    addTrackedMedia(item.toAddTrackedMediaRequest())
                    existingKeys += duplicateKey
                    importedRows++
                }
            }
        }

        return MyAnimeListXmlImportResult(
            importedRows = importedRows,
            skippedDuplicateRows = skippedDuplicateRows,
            unsupportedRows = unsupportedRows,
        )
    }

    override suspend fun startNewSession(request: AddTrackingSessionRequest) {
        val sessions = mediaDao.getTrackingSessions(request.mediaItemId)
        val latestSession = sessions.maxByOrNull { it.sessionNumber }
        val mediaItem = mediaDao.getMediaItem(request.mediaItemId) ?: return
        val newSessionNumber = (latestSession?.sessionNumber ?: 0) + 1
        val updatedAtEpochMillis = System.currentTimeMillis()
        val validProgressTotal = mediaItem.progressTotal
            ?.takeUnless { mediaItem.type == MediaType.Game.name }
        val validProgress = validProgressTotal?.let { maxProgress ->
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

        val sessionId = mediaDao.insertTrackingSession(newSession)
        mediaDao.insertProgressUpdateIfNeeded(
            mediaItemId = request.mediaItemId,
            sessionId = sessionId,
            progressValue = validProgress,
            createdAtEpochMillis = updatedAtEpochMillis,
            countsTowardObjectives = request.status != TrackingStatus.Completed || request.finishedAt == LocalDate.now(),
        )
    }

    override suspend fun addTrackedMedia(request: AddTrackedMediaRequest): Long {
        val validTotal = request.progressTotal
            ?.takeUnless { request.type == MediaType.Game }
            ?.coerceAtLeast(0)
        val validNewCollectionName = request.newCollectionName?.trim()?.takeIf { it.isNotBlank() }
        val validCollectionId = when {
            validNewCollectionName != null -> mediaDao.insertMediaCollection(
                MediaCollectionEntity(name = validNewCollectionName),
            )
            request.collectionId != null && mediaDao.getMediaCollection(request.collectionId) != null -> request.collectionId
            else -> null
        }
        val validCollectionSortOrder = validCollectionId?.let { collectionId ->
            request.collectionSortOrder?.coerceAtLeast(0.0)
                ?: (mediaDao.getMaxCollectionSortOrder(collectionId) + 1.0)
        }
        val primaryExternalRating = request.type.preferredPrimaryExternalRating(
            ratings = request.externalRatings,
            fallback = request.externalRating,
        )
        val mediaItemId = mediaDao.insertMediaItem(
            MediaItemEntity(
                type = request.type.name,
                title = request.title.trim(),
                collectionId = validCollectionId,
                collectionSortOrder = validCollectionSortOrder,
                progressTotal = validTotal,
                originalTitle = request.originalTitle?.trim()?.takeIf { it.isNotBlank() },
                releaseYear = request.releaseYear,
                language = ItemLanguage.normalize(request.language),
                genresJson = request.genres.toJsonArrayString(),
                creatorsJson = request.creators.toJsonArrayString(),
                coverUrl = request.coverUrl,
                synopsis = normalizeSynopsis(request.synopsis),
                sourceUrl = request.sourceUrl,
                externalRatingScore = primaryExternalRating?.score,
                externalRatingMax = primaryExternalRating?.maxScore,
                externalRatingVoteCount = primaryExternalRating?.voteCount,
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

        var initialPrimaryRatingId: Long? = null
        if (request.externalRatings.isNotEmpty()) {
            request.externalRatings
                .filter { it.score > 0.0 && it.maxScore > 0.0 }
                .forEach { rating ->
                    val ratingId = mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = mediaItemId,
                            source = rating.source.name,
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                            origin = "Provider",
                        ),
                    )
                    if (initialPrimaryRatingId == null && primaryExternalRating != null &&
                        rating.score.closeTo(primaryExternalRating.score) &&
                        rating.maxScore.closeTo(primaryExternalRating.maxScore)
                    ) {
                        initialPrimaryRatingId = ratingId
                    }
                }
        }
        initialPrimaryRatingId?.let { ratingId -> mediaDao.setPrimaryRatingId(mediaItemId, ratingId) }

        val initialUpdatedAtEpochMillis = System.currentTimeMillis()
        val initialProgress = validTotal?.let { total ->
            request.initialProgress.coerceIn(0, total)
        } ?: request.initialProgress.coerceAtLeast(0)
        val initialSessionId = mediaDao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = 1,
                status = request.initialStatus.name,
                progressCurrent = initialProgress,
                rating = request.initialRating?.coerceIn(1, 10),
                notes = request.initialNotes?.trim()?.takeIf { it.isNotBlank() },
                platformName = request.platformName?.trim()?.takeIf { it.isNotBlank() },
                platformType = request.platformType.name,
                startedAtEpochDay = request.initialStartedAt?.toEpochDay(),
                finishedAtEpochDay = request.initialFinishedAt?.toEpochDay(),
                updatedAtEpochMillis = initialUpdatedAtEpochMillis,
            ),
        )
        mediaDao.insertProgressUpdateIfNeeded(
            mediaItemId = mediaItemId,
            sessionId = initialSessionId,
            progressValue = initialProgress,
            createdAtEpochMillis = initialUpdatedAtEpochMillis,
            countsTowardObjectives = request.initialStatus != TrackingStatus.Completed || request.initialFinishedAt == LocalDate.now(),
        )
        return mediaItemId
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
        val validProgressTotal = mediaItem.progressTotal
            ?.takeUnless { mediaItem.type == MediaType.Game.name }
        val validProgress = validProgressTotal?.let { maxProgress ->
            progressCurrent.coerceIn(0, maxProgress)
        } ?: progressCurrent.coerceAtLeast(0)

        val updatedAtEpochMillis = System.currentTimeMillis()
        mediaDao.updateSessionDetails(
            sessionId = sessionId,
            status = status.name,
            progressCurrent = validProgress,
            rating = rating?.coerceIn(1, 10),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            startedAtEpochDay = startedAt?.toEpochDay(),
            finishedAtEpochDay = finishedAt?.toEpochDay(),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        if (validProgress != session.progressCurrent) {
            mediaDao.insertProgressUpdateIfNeeded(
                mediaItemId = session.mediaItemId,
                sessionId = sessionId,
                progressValue = validProgress,
                createdAtEpochMillis = updatedAtEpochMillis,
                countsTowardObjectives = status != TrackingStatus.Completed || finishedAt == LocalDate.now(),
            )
        }
    }

    override suspend fun deletePastSession(sessionId: Long): DeletionRecovery? {
        val session = mediaDao.getTrackingSession(sessionId) ?: return null
        val sessions = mediaDao.getTrackingSessions(session.mediaItemId)
        val latestSessionNumber = sessions.maxOfOrNull { it.sessionNumber } ?: return null
        if (sessions.size <= 1 || session.sessionNumber == latestSessionNumber) {
            return null
        }
        val progressUpdates = mediaDao.getProgressUpdatesForSession(sessionId)
        mediaDao.deleteProgressUpdatesForSession(sessionId)
        mediaDao.deleteTrackingSession(sessionId)
        return DeletionRecovery.PastSession(
            session = session,
            progressUpdates = progressUpdates,
        )
    }

    override suspend fun updateProgressUpdateDate(
        progressUpdateId: Long,
        loggedAt: LocalDate?,
    ) {
        val update = mediaDao.getProgressUpdate(progressUpdateId) ?: return
        mediaDao.updateProgressUpdateDate(
            progressUpdateId = progressUpdateId,
            loggedAtEpochDay = loggedAt?.toEpochDay() ?: update.loggedAtEpochDay,
            hasKnownDate = loggedAt != null,
        )
    }

    override suspend fun deleteProgressUpdate(progressUpdateId: Long): DeletionRecovery? {
        val update = mediaDao.getProgressUpdate(progressUpdateId) ?: return null
        val sessionBeforeDeletion = mediaDao.getTrackingSession(update.sessionId) ?: return null
        val remainingProgress = mediaDao.getProgressUpdatesForSession(update.sessionId)
            .filterNot { it.id == progressUpdateId }
        val progressAfterDeletion = remainingProgress
            .maxWithOrNull(
                compareBy<ProgressUpdateEntity> { it.loggedAtEpochDay }
                    .thenBy { it.createdAtEpochMillis }
                    .thenBy { it.id },
            )
            ?.progressValue
            ?: 0
        val updatedAtEpochMillis = System.currentTimeMillis()
        val sessionAfterDeletion = sessionBeforeDeletion.copy(
            progressCurrent = progressAfterDeletion,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        mediaDao.deleteProgressUpdate(progressUpdateId)
        mediaDao.updateSessionProgress(
            sessionId = update.sessionId,
            progressCurrent = progressAfterDeletion,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        return DeletionRecovery.ProgressUpdate(
            update = update,
            sessionBeforeDeletion = sessionBeforeDeletion,
            sessionAfterDeletion = sessionAfterDeletion,
        )
    }

    override suspend fun deleteMediaItem(mediaItemId: Long): DeletionRecovery? {
        val item = mediaDao.getMediaItem(mediaItemId) ?: return null
        val collection = item.collectionId?.let { mediaDao.getMediaCollection(it) }
        val credits = mediaDao.getMediaCreditsForItem(mediaItemId)
        val sessions = mediaDao.getTrackingSessions(mediaItemId)
        val sessionIds = sessions.map { it.id }.toSet()
        val progressUpdates = mediaDao.getProgressUpdates()
            .filter { it.mediaItemId == mediaItemId && it.sessionId in sessionIds }
        val externalRatings = mediaDao.getExternalRatingsForItem(mediaItemId)

        mediaDao.deleteMediaItem(mediaItemId)
        mediaDao.deleteEmptyMediaCollections()
        return DeletionRecovery.MediaItem(
            item = item,
            collection = collection,
            credits = credits,
            sessions = sessions,
            progressUpdates = progressUpdates,
            externalRatings = externalRatings,
        )
    }

    override suspend fun restoreDeletion(recovery: DeletionRecovery): Boolean = when (recovery) {
        is DeletionRecovery.MediaItem -> restoreMediaItemDeletion(recovery)
        is DeletionRecovery.PastSession -> restorePastSessionDeletion(recovery)
        is DeletionRecovery.ProgressUpdate -> restoreProgressUpdateDeletion(recovery)
    }

    private suspend fun restoreMediaItemDeletion(recovery: DeletionRecovery.MediaItem): Boolean {
        val item = recovery.item
        if (mediaDao.getMediaItem(item.id) != null) return false
        if (item.collectionId != recovery.collection?.id && item.collectionId != null) return false
        if (recovery.credits.any { it.mediaItemId != item.id }) return false
        if (recovery.sessions.any { it.mediaItemId != item.id }) return false
        val sessionIds = recovery.sessions.map { it.id }.toSet()
        if (recovery.progressUpdates.any {
                it.mediaItemId != item.id || it.sessionId !in sessionIds
            }
        ) return false
        if (recovery.externalRatings.any { it.mediaItemId != item.id }) return false

        val existingCreditIds = mediaDao.getMediaCredits().map { it.id }.toSet()
        if (recovery.credits.any { it.id != 0L && it.id in existingCreditIds }) return false
        if (recovery.sessions.any { mediaDao.getTrackingSession(it.id) != null }) return false
        if (recovery.progressUpdates.any { mediaDao.getProgressUpdate(it.id) != null }) return false
        if (recovery.externalRatings.any { mediaDao.getExternalRating(it.id) != null }) return false

        val existingCollection = item.collectionId?.let { mediaDao.getMediaCollection(it) }
        if (item.collectionId != null && existingCollection == null && recovery.collection == null) {
            return false
        }
        if (recovery.collection != null && recovery.collection.id != item.collectionId) return false

        if (recovery.collection != null && existingCollection == null) {
            mediaDao.insertMediaCollection(recovery.collection)
        }
        mediaDao.insertMediaItem(item)
        if (recovery.credits.isNotEmpty()) {
            mediaDao.insertMediaCredits(recovery.credits)
        }
        recovery.sessions.forEach { mediaDao.insertTrackingSession(it) }
        recovery.progressUpdates.forEach { mediaDao.insertProgressUpdate(it) }
        recovery.externalRatings.forEach { mediaDao.insertExternalRating(it) }
        return true
    }

    private suspend fun restorePastSessionDeletion(recovery: DeletionRecovery.PastSession): Boolean {
        val session = recovery.session
        if (mediaDao.getMediaItem(session.mediaItemId) == null) return false
        if (mediaDao.getTrackingSession(session.id) != null) return false
        if (recovery.progressUpdates.any {
                it.mediaItemId != session.mediaItemId || it.sessionId != session.id
            }
        ) return false
        if (recovery.progressUpdates.any { mediaDao.getProgressUpdate(it.id) != null }) return false
        if (mediaDao.getTrackingSessions(session.mediaItemId)
                .any { it.sessionNumber == session.sessionNumber }
        ) return false

        mediaDao.insertTrackingSession(session)
        recovery.progressUpdates.forEach { mediaDao.insertProgressUpdate(it) }
        return true
    }

    private suspend fun restoreProgressUpdateDeletion(recovery: DeletionRecovery.ProgressUpdate): Boolean {
        val update = recovery.update
        if (mediaDao.getMediaItem(update.mediaItemId) == null) return false
        if (update.sessionId != recovery.sessionBeforeDeletion.id) return false
        if (update.mediaItemId != recovery.sessionBeforeDeletion.mediaItemId) return false
        if (recovery.sessionBeforeDeletion.id != recovery.sessionAfterDeletion.id) return false
        if (mediaDao.getProgressUpdate(update.id) != null) return false
        val currentSession = mediaDao.getTrackingSession(update.sessionId) ?: return false
        if (currentSession != recovery.sessionAfterDeletion) return false

        mediaDao.updateSessionProgress(
            sessionId = recovery.sessionBeforeDeletion.id,
            progressCurrent = recovery.sessionBeforeDeletion.progressCurrent,
            updatedAtEpochMillis = recovery.sessionBeforeDeletion.updatedAtEpochMillis,
        )
        mediaDao.insertProgressUpdate(update)
        return true
    }
    override suspend fun addExternalRating(
        mediaItemId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    ) {
        val validScore = score.takeIf { it in 0.0..maxScore } ?: return
        val validMax = maxScore.takeIf { it > 0.0 } ?: return
        val existing = mediaDao.getExternalRatingsForItem(mediaItemId)
            .firstOrNull { it.source == source.name && it.origin == "Manual" }
        if (existing != null) {
            updateExternalRating(existing.id, source, validScore, validMax, voteCount, makePrimary)
            return
        }
        val ratingId = mediaDao.insertExternalRating(
            ExternalRatingEntity(
                mediaItemId = mediaItemId,
                source = source.name,
                score = validScore,
                maxScore = validMax,
                voteCount = voteCount?.takeIf { it >= 0 },
                origin = "Manual",
            ),
        )
        if (makePrimary) {
            setPrimaryExternalRating(ratingId)
        }
    }

    override suspend fun updateExternalRating(
        externalRatingId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    ) {
        val existing = mediaDao.getExternalRating(externalRatingId) ?: return
        if (mediaDao.getExternalRatingsForItem(existing.mediaItemId).any { rating ->
                rating.id != externalRatingId && rating.source == source.name && rating.origin == "Manual"
            }
        ) return
        val validScore = score.takeIf { it in 0.0..maxScore } ?: return
        val validMax = maxScore.takeIf { it > 0.0 } ?: return
        val validVoteCount = voteCount?.takeIf { it >= 0 }
        mediaDao.updateExternalRating(
            externalRatingId = externalRatingId,
            source = source.name,
            score = validScore,
            maxScore = validMax,
            voteCount = validVoteCount,
            origin = "Manual",
        )
        val mediaItem = mediaDao.getMediaItem(existing.mediaItemId)
        if (makePrimary || existing.isPrimaryExternalRating(mediaItem)) {
            setPrimaryExternalRating(externalRatingId)
        }
    }

    override suspend fun setPrimaryExternalRating(externalRatingId: Long) {
        val rating = mediaDao.getExternalRating(externalRatingId) ?: return
        mediaDao.updatePrimaryExternalRating(
            mediaItemId = rating.mediaItemId,
            score = rating.score,
            maxScore = rating.maxScore,
            voteCount = rating.voteCount,
            primaryExternalRatingId = rating.id,
        )
    }

    override suspend fun deleteExternalRating(externalRatingId: Long) {
        val rating = mediaDao.getExternalRating(externalRatingId) ?: return
        mediaDao.deleteExternalRating(externalRatingId)
        val mediaItem = mediaDao.getMediaItem(rating.mediaItemId)
        if (rating.isPrimaryExternalRating(mediaItem)) {
            val replacement = mediaDao.getExternalRatingsForItem(rating.mediaItemId)
                .preferredPrimaryReplacement(mediaItem)
            mediaDao.updatePrimaryExternalRating(
                mediaItemId = rating.mediaItemId,
                score = replacement?.score,
                maxScore = replacement?.maxScore,
                voteCount = replacement?.voteCount,
                primaryExternalRatingId = replacement?.id,
            )
        }
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

    override suspend fun updateCollectionItemOrder(collectionId: Long, itemOrders: List<CollectionItemOrder>) {
        val validItemOrders = itemOrders
            .filter { itemOrder -> itemOrder.sortOrder >= 0.0 }
            .distinctBy { itemOrder -> itemOrder.mediaItemId }
        if (mediaDao.getMediaCollection(collectionId) == null || validItemOrders.isEmpty()) {
            return
        }

        mediaDao.updateCollectionSortOrders(
            collectionId = collectionId,
            itemOrders = validItemOrders,
        )
    }

    override suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        collectionSortOrder: Double?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val validTotal = progressTotal
            ?.takeUnless { currentItem.type == MediaType.Game.name }
            ?.coerceAtLeast(0)
        val validNewCollectionName = newCollectionName?.trim()?.takeIf { it.isNotBlank() }
        val validCollectionId = when {
            validNewCollectionName != null -> mediaDao.insertMediaCollection(
                MediaCollectionEntity(name = validNewCollectionName),
            )
            collectionId != null && mediaDao.getMediaCollection(collectionId) != null -> collectionId
            else -> null
        }
        val validCollectionSortOrder = validCollectionId?.let { targetCollectionId ->
            collectionSortOrder?.coerceAtLeast(0.0)
                ?: currentItem.collectionSortOrder.takeIf { currentItem.collectionId == targetCollectionId }
                ?: (mediaDao.getMaxCollectionSortOrder(targetCollectionId) + 1.0)
        }

        mediaDao.updateMediaItemDetails(
            mediaItemId = mediaItemId,
            title = validTitle,
            collectionId = validCollectionId,
            collectionSortOrder = validCollectionSortOrder,
            progressTotal = validTotal,
            isOwned = ownershipType != OwnershipType.None,
            ownershipType = ownershipType.name,
        )
        mediaDao.deleteEmptyMediaCollections()

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
        language: String?,
        progressTotal: Int?,
        genres: List<String>,
        creators: List<String>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return
        val validTotal = progressTotal
            ?.takeUnless { currentItem.type == MediaType.Game.name }
            ?.coerceAtLeast(0)
        val validOriginalTitle = originalTitle?.trim()?.takeIf { it.isNotBlank() }
        val validReleaseYear = releaseYear?.coerceAtLeast(0)
        val validLanguage = ItemLanguage.normalize(language)
        val validGenres = genres.cleanMetadataList()
        val validCreators = creators.cleanMetadataList()
        val validCoverUrl = coverUrl?.trim()?.takeIf { it.isNotBlank() }
        val validSynopsis = normalizeSynopsis(synopsis)
        val validSourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() }
        val updatedOverrideFields = currentItem.metadataOverrideFields() + buildSet {
            if (currentItem.title != validTitle) add(MetadataRefreshField.Title)
            if (currentItem.originalTitle != validOriginalTitle) add(MetadataRefreshField.OriginalTitle)
            if (currentItem.releaseYear != validReleaseYear) add(MetadataRefreshField.ReleaseYear)
            if (currentItem.language != validLanguage) add(MetadataRefreshField.Language)
            if (currentItem.progressTotal != validTotal) add(MetadataRefreshField.ProgressTotal)
            if (currentItem.genresJson.toStringList() != validGenres) add(MetadataRefreshField.Genres)
            if (currentItem.creatorsJson.toStringList() != validCreators) add(MetadataRefreshField.Creators)
            if (currentItem.coverUrl != validCoverUrl) add(MetadataRefreshField.Cover)
            if (currentItem.synopsis != validSynopsis) add(MetadataRefreshField.Synopsis)
            if (currentItem.sourceUrl != validSourceUrl) add(MetadataRefreshField.SourceUrl)
        }

        mediaDao.updateMediaItemMetadata(
            mediaItemId = mediaItemId,
            title = validTitle,
            originalTitle = validOriginalTitle,
            releaseYear = validReleaseYear,
            language = validLanguage,
            progressTotal = validTotal,
            genresJson = validGenres.toJsonArrayString(),
            creatorsJson = validCreators.toJsonArrayString(),
            coverUrl = validCoverUrl,
            synopsis = validSynopsis,
            sourceUrl = validSourceUrl,
        )
        mediaDao.updateMetadataOverrideFields(
            mediaItemId = mediaItemId,
            metadataOverrideFieldsCsv = updatedOverrideFields.toMetadataOverrideFieldsCsv(),
        )

        validTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun previewMediaItemMetadataRefresh(
        mediaItemId: Long,
        metadataRepository: MetadataRepository,
    ): MetadataRefreshPreview? {
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return null
        val metadataSource = currentItem.metadataSource?.let { source ->
            runCatching { MetadataSource.valueOf(source) }.getOrNull()
        } ?: return null
        val metadataExternalId = currentItem.metadataExternalId?.takeIf { it.isNotBlank() } ?: return null
        val mediaType = runCatching { MediaType.valueOf(currentItem.type) }.getOrNull() ?: return null
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
        val existingCredits = mediaDao.getMediaCreditsForItem(mediaItemId).map { it.toDomain() }
        val localOverrides = currentItem.metadataOverrideFields()
        val refreshed = metadataRepository.getSuggestionDetails(
            MetadataSuggestion(
                source = metadataSource,
                externalId = metadataExternalId,
                mediaType = mediaType,
                title = currentItem.title,
                originalTitle = currentItem.originalTitle,
                releaseYear = currentItem.releaseYear,
                language = currentItem.language,
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
        val refreshedTotal = refreshed.progressTotal
            ?.takeUnless { mediaType == MediaType.Game }
            ?.coerceAtLeast(0)
        val refreshedRatings = refreshed.externalRatings
            .ifEmpty { existingRatings }
        val normalizedRefreshed = refreshed.copy(
            title = refreshed.title.trim().takeIf { it.isNotBlank() } ?: currentItem.title,
            originalTitle = refreshed.originalTitle?.trim()?.takeIf { it.isNotBlank() },
            language = ItemLanguage.normalize(refreshed.language),
            genres = refreshed.genres.cleanMetadataList(),
            creators = refreshed.creators.cleanMetadataList(),
            progressTotal = refreshedTotal,
            coverUrl = refreshed.coverUrl?.trim()?.takeIf { it.isNotBlank() },
            synopsis = normalizeSynopsis(refreshed.synopsis),
            sourceUrl = refreshed.sourceUrl?.trim()?.takeIf { it.isNotBlank() },
            externalRatings = refreshedRatings,
        )

        return MetadataRefreshPreview(
            mediaItemId = mediaItemId,
            refreshed = normalizedRefreshed,
            changes = buildMetadataRefreshChanges(
                currentItem = currentItem,
                currentCredits = existingCredits,
                currentRatings = existingRatings,
                refreshed = normalizedRefreshed,
                localOverrides = localOverrides,
            ),
        )
    }

    override suspend fun applyMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Boolean {
        val currentItem = mediaDao.getMediaItem(preview.mediaItemId) ?: return false
        val localOverrides = currentItem.metadataOverrideFields()
        val mediaType = runCatching { MediaType.valueOf(currentItem.type) }.getOrNull() ?: return false
        val refreshed = preview.refreshed
        val currentPrimaryManualRating = currentItem.primaryExternalRatingId
            ?.let { primaryId -> mediaDao.getExternalRating(primaryId) }
            ?.takeIf { it.origin == "Manual" }
            ?.toPrimaryRating()
        val existingRatings = mediaDao.getExternalRatingsForItem(preview.mediaItemId)
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
        val refreshedTotal = refreshed.progressTotal
            ?.takeUnless { mediaType == MediaType.Game }
            ?.coerceAtLeast(0)
        val selectedTotal = if (MetadataRefreshField.ProgressTotal in selectedFields) {
            refreshedTotal
        } else {
            currentItem.progressTotal
        }
        val selectedRatings = if (MetadataRefreshField.ExternalRatings in selectedFields) {
            refreshed.externalRatings
        } else {
            existingRatings
        }
        val selectedExternalRating = currentPrimaryManualRating ?: if (
            MetadataRefreshField.ExternalRating in selectedFields ||
            MetadataRefreshField.ExternalRatings in selectedFields
        ) {
            mediaType.preferredPrimaryExternalRating(
                ratings = selectedRatings,
                fallback = if (MetadataRefreshField.ExternalRating in selectedFields) {
                    refreshed.externalRating
                } else {
                    existingPrimaryRating
                },
            )
        } else {
            existingPrimaryRating
        }

        mediaDao.refreshMediaItemMetadata(
            mediaItemId = preview.mediaItemId,
            title = refreshed.title.takeIfSelected(MetadataRefreshField.Title, selectedFields) ?: currentItem.title,
            originalTitle = refreshed.originalTitle.takeIfSelected(
                MetadataRefreshField.OriginalTitle,
                selectedFields,
            ) ?: currentItem.originalTitle,
            releaseYear = refreshed.releaseYear.takeIfSelected(
                MetadataRefreshField.ReleaseYear,
                selectedFields,
            ) ?: currentItem.releaseYear,
            language = ItemLanguage.normalize(refreshed.language).takeIfSelected(MetadataRefreshField.Language, selectedFields)
                ?: currentItem.language,
            progressTotal = selectedTotal,
            genresJson = if (MetadataRefreshField.Genres in selectedFields) {
                refreshed.genres.cleanMetadataList().toJsonArrayString()
            } else {
                currentItem.genresJson
            },
            creatorsJson = if (MetadataRefreshField.Creators in selectedFields) {
                refreshed.creators.cleanMetadataList().toJsonArrayString()
            } else {
                currentItem.creatorsJson
            },
            coverUrl = refreshed.coverUrl.takeIfSelected(MetadataRefreshField.Cover, selectedFields) ?: currentItem.coverUrl,
            synopsis = refreshed.synopsis.takeIfSelected(MetadataRefreshField.Synopsis, selectedFields) ?: currentItem.synopsis,
            sourceUrl = refreshed.sourceUrl.takeIfSelected(MetadataRefreshField.SourceUrl, selectedFields) ?: currentItem.sourceUrl,
            externalRatingScore = selectedExternalRating?.score,
            externalRatingMax = selectedExternalRating?.maxScore,
            externalRatingVoteCount = selectedExternalRating?.voteCount,
            popularityScore = refreshed.popularityScore.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.popularityScore,
            rankingPosition = refreshed.rankingPosition.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.rankingPosition,
            rankingLabel = refreshed.rankingLabel.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.rankingLabel,
            providerCollectionTitle = refreshed.collectionTitle.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.providerCollectionTitle,
            ratingDistributionJson = refreshed.ratingDistributionJson.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.ratingDistributionJson,
            popularityJson = refreshed.popularityJson.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.popularityJson,
            rankingJson = refreshed.rankingJson.takeIfSelected(
                MetadataRefreshField.ProviderStats,
                selectedFields,
            ) ?: currentItem.rankingJson,
            metadataSource = refreshed.source.name,
            metadataExternalId = refreshed.externalId,
            metadataLastFetchedAtEpochMillis = System.currentTimeMillis(),
        )

        if (MetadataRefreshField.Credits in selectedFields) {
            mediaDao.deleteMediaCreditsForItem(preview.mediaItemId)
            refreshed.credits
                .filter { it.personName.isNotBlank() }
                .mapIndexed { index, credit ->
                    credit.copy(
                        id = 0,
                        mediaItemId = preview.mediaItemId,
                        personName = credit.personName.trim(),
                        characterName = credit.characterName?.trim()?.takeIf { it.isNotBlank() },
                        sortOrder = credit.sortOrder.takeIf { it > 0 } ?: index,
                        metadataSource = credit.metadataSource ?: refreshed.source,
                    ).toEntity()
                }
                .takeIf { it.isNotEmpty() }
                ?.let { credits -> mediaDao.insertMediaCredits(credits) }
        }

        if (MetadataRefreshField.ExternalRatings in selectedFields) {
            mediaDao.deleteProviderExternalRatingsForItem(preview.mediaItemId)
            refreshed.externalRatings
                .filter { it.score > 0.0 && it.maxScore > 0.0 }
                .forEach { rating ->
                    mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = preview.mediaItemId,
                            source = rating.source.name,
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                            origin = "Provider",
                        ),
                    )
                }
        } else if (MetadataRefreshField.ExternalRating in selectedFields && existingRatings.isEmpty()) {
            refreshed.externalRating
                ?.takeIf { it.score > 0.0 && it.maxScore > 0.0 }
                ?.let { rating ->
                    mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = preview.mediaItemId,
                            source = refreshed.source.defaultExternalRatingSource(),
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                            origin = "Provider",
                        ),
                    )
                }
        }

        if (MetadataRefreshField.ProgressTotal in selectedFields) {
            refreshedTotal?.let { total ->
                mediaDao.clampSessionsToMediaTotal(
                    mediaItemId = preview.mediaItemId,
                    progressTotal = total,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            }
        }

        val remainingOverrides = localOverrides - selectedFields
        if (remainingOverrides != localOverrides) {
            mediaDao.updateMetadataOverrideFields(
                mediaItemId = preview.mediaItemId,
                metadataOverrideFieldsCsv = remainingOverrides.toMetadataOverrideFieldsCsv(),
            )
        }

        return true
    }

    override suspend fun refreshMediaItemMetadata(
        mediaItemId: Long,
        metadataRepository: MetadataRepository,
    ): Boolean {
        val preview = previewMediaItemMetadataRefresh(mediaItemId, metadataRepository) ?: return false
        return applyMediaItemMetadataRefresh(
            preview = preview,
            selectedFields = preview.changes
                .filterNot { change -> change.isLocallyOverridden }
                .map { change -> change.field }
                .toSet(),
        )
    }

    override suspend fun linkMediaItemMetadata(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
        metadataRepository: MetadataRepository,
    ): Boolean {
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return false
        val currentMediaType = runCatching { MediaType.valueOf(currentItem.type) }.getOrNull() ?: return false
        if (currentMediaType != suggestion.mediaType) {
            return false
        }

        val currentPrimaryManualRating = currentItem.primaryExternalRatingId
            ?.let { primaryId -> mediaDao.getExternalRating(primaryId) }
            ?.takeIf { it.origin == "Manual" }
            ?.toPrimaryRating()
        val existingRatings = mediaDao.getExternalRatingsForItem(mediaItemId)
            .filter { it.origin == "Provider" }
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
        val linked = metadataRepository.getSuggestionDetails(suggestion)
            .withPreservedMyAnimeListId(currentItem)
        val linkedTotal = linked.progressTotal
            ?.takeUnless { currentMediaType == MediaType.Game }
            ?.coerceAtLeast(0)
        val linkedRatings = (linked.externalRatings + existingRatings)
            .distinctBy { it.source }
        val linkedPrimaryRating = currentPrimaryManualRating ?: currentMediaType.preferredPrimaryExternalRating(
            ratings = linkedRatings,
            fallback = linked.externalRating ?: existingPrimaryRating,
        )

        mediaDao.refreshMediaItemMetadata(
            mediaItemId = mediaItemId,
            title = linked.title.trim().takeIf { it.isNotBlank() } ?: currentItem.title,
            originalTitle = linked.originalTitle?.trim()?.takeIf { it.isNotBlank() },
            releaseYear = linked.releaseYear,
            language = ItemLanguage.normalize(linked.language),
            progressTotal = linkedTotal,
            genresJson = linked.genres.cleanMetadataList().toJsonArrayString(),
            creatorsJson = linked.creators.cleanMetadataList().toJsonArrayString(),
            coverUrl = linked.coverUrl?.trim()?.takeIf { it.isNotBlank() },
            synopsis = normalizeSynopsis(linked.synopsis),
            sourceUrl = linked.sourceUrl?.trim()?.takeIf { it.isNotBlank() },
            externalRatingScore = linkedPrimaryRating?.score,
            externalRatingMax = linkedPrimaryRating?.maxScore,
            externalRatingVoteCount = linkedPrimaryRating?.voteCount,
            popularityScore = linked.popularityScore,
            rankingPosition = linked.rankingPosition,
            rankingLabel = linked.rankingLabel,
            providerCollectionTitle = linked.collectionTitle,
            ratingDistributionJson = linked.ratingDistributionJson,
            popularityJson = linked.popularityJson,
            rankingJson = linked.rankingJson,
            metadataSource = linked.source.name,
            metadataExternalId = linked.externalId,
            metadataLastFetchedAtEpochMillis = System.currentTimeMillis(),
        )

        mediaDao.deleteMediaCreditsForItem(mediaItemId)
        linked.credits
            .filter { it.personName.isNotBlank() }
            .mapIndexed { index, credit ->
                credit.copy(
                    id = 0,
                    mediaItemId = mediaItemId,
                    personName = credit.personName.trim(),
                    characterName = credit.characterName?.trim()?.takeIf { it.isNotBlank() },
                    sortOrder = credit.sortOrder.takeIf { it > 0 } ?: index,
                    metadataSource = credit.metadataSource ?: linked.source,
                ).toEntity()
            }
            .takeIf { it.isNotEmpty() }
            ?.let { credits -> mediaDao.insertMediaCredits(credits) }

        mediaDao.deleteProviderExternalRatingsForItem(mediaItemId)
        linkedRatings
            .filter { it.score > 0.0 && it.maxScore > 0.0 }
            .forEach { rating ->
                mediaDao.insertExternalRating(
                    ExternalRatingEntity(
                        mediaItemId = mediaItemId,
                        source = rating.source.name,
                        score = rating.score,
                        maxScore = rating.maxScore,
                        voteCount = rating.voteCount,
                        origin = "Provider",
                    ),
                )
            }

        linkedTotal?.let { total ->
            mediaDao.clampSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            mediaDao.fillCompletedSessionsToMediaTotal(
                mediaItemId = mediaItemId,
                progressTotal = total,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }

        return true
    }

}

private fun buildMetadataRefreshChanges(
    currentItem: MediaItemEntity,
    currentCredits: List<MediaCredit>,
    currentRatings: List<MetadataExternalRatingSuggestion>,
    refreshed: MetadataSuggestion,
    localOverrides: Set<MetadataRefreshField>,
): List<MetadataRefreshChange> = buildList {
    addChange(
        field = MetadataRefreshField.Title,
        currentValue = currentItem.title,
        newValue = refreshed.title,
    )
    addChange(
        field = MetadataRefreshField.OriginalTitle,
        currentValue = currentItem.originalTitle,
        newValue = refreshed.originalTitle,
    )
    addChange(
        field = MetadataRefreshField.ReleaseYear,
        currentValue = currentItem.releaseYear,
        newValue = refreshed.releaseYear,
    )
    addChange(
        field = MetadataRefreshField.Language,
        currentValue = currentItem.language,
        newValue = refreshed.language,
    )
    addChange(
        field = MetadataRefreshField.ProgressTotal,
        currentValue = currentItem.progressTotal,
        newValue = refreshed.progressTotal,
    )
    addChange(
        field = MetadataRefreshField.Genres,
        currentValue = currentItem.genresJson.toStringList(),
        newValue = refreshed.genres,
    )
    addChange(
        field = MetadataRefreshField.Creators,
        currentValue = currentItem.creatorsJson.toStringList(),
        newValue = refreshed.creators,
    )
    addChange(
        field = MetadataRefreshField.Credits,
        currentValue = currentCredits.creditSummary(),
        newValue = refreshed.credits.creditSummary(),
    )
    addChange(
        field = MetadataRefreshField.Cover,
        currentValue = currentItem.coverUrl,
        newValue = refreshed.coverUrl,
    )
    addChange(
        field = MetadataRefreshField.Synopsis,
        currentValue = plainSynopsis(currentItem.synopsis),
        newValue = plainSynopsis(refreshed.synopsis),
    )
    addChange(
        field = MetadataRefreshField.SourceUrl,
        currentValue = currentItem.sourceUrl,
        newValue = refreshed.sourceUrl,
    )
    addChange(
        field = MetadataRefreshField.ExternalRating,
        currentValue = currentItem.externalRatingScore?.let { score ->
            val maxScore = currentItem.externalRatingMax ?: return@let null
            "${score.cleanNumber()}/${maxScore.cleanNumber()}"
        },
        newValue = refreshed.externalRating?.let { rating ->
            "${rating.score.cleanNumber()}/${rating.maxScore.cleanNumber()}"
        },
    )
    addChange(
        field = MetadataRefreshField.ExternalRatings,
        currentValue = currentRatings.ratingSummary(),
        newValue = refreshed.externalRatings.ratingSummary(),
    )
    addChange(
        field = MetadataRefreshField.ProviderStats,
        currentValue = providerStatsSummary(
            popularityScore = currentItem.popularityScore,
            rankingPosition = currentItem.rankingPosition,
            rankingLabel = currentItem.rankingLabel,
            collectionTitle = currentItem.providerCollectionTitle,
        ),
        newValue = providerStatsSummary(
            popularityScore = refreshed.popularityScore,
            rankingPosition = refreshed.rankingPosition,
            rankingLabel = refreshed.rankingLabel,
            collectionTitle = refreshed.collectionTitle,
        ),
    )
}.distinctBy { it.field }.map { change ->
    change.copy(isLocallyOverridden = change.field in localOverrides)
}

private fun MutableList<MetadataRefreshChange>.addChange(
    field: MetadataRefreshField,
    currentValue: Any?,
    newValue: Any?,
) {
    val currentDisplay = currentValue.toMetadataDisplayValue()
    val newDisplay = newValue.toMetadataDisplayValue()
    if (newDisplay == "Empty" || currentDisplay == newDisplay) return

    add(
        MetadataRefreshChange(
            field = field,
            currentValue = currentDisplay,
            newValue = newDisplay,
            overwritesExistingValue = currentDisplay != "Empty",
        ),
    )
}

private fun Any?.toMetadataDisplayValue(): String {
    val value = when (this) {
        null -> ""
        is String -> this.trim()
        is Int -> takeIf { it > 0 }?.toString().orEmpty()
        is Double -> takeIf { it > 0.0 }?.cleanNumber().orEmpty()
        is List<*> -> filterIsInstance<String>().map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
        else -> toString().trim()
    }
    return value.takeIf { it.isNotBlank() }?.ellipsizeMetadataValue() ?: "Empty"
}

private fun String.ellipsizeMetadataValue(): String {
    return if (length <= 160) this else take(157).trimEnd() + "..."
}

private fun List<MediaCredit>.creditSummary(): String {
    return filter { it.personName.isNotBlank() }
        .take(8)
        .joinToString(", ") { credit ->
            credit.characterName?.takeIf { it.isNotBlank() }
                ?.let { "${credit.personName.trim()} as ${it.trim()}" }
                ?: credit.personName.trim()
        }
}

private fun List<MetadataExternalRatingSuggestion>.ratingSummary(): String {
    return filter { it.score > 0.0 && it.maxScore > 0.0 }
        .joinToString(", ") { rating ->
            "${rating.source.name} ${rating.score.cleanNumber()}/${rating.maxScore.cleanNumber()}"
        }
}

private fun providerStatsSummary(
    popularityScore: Double?,
    rankingPosition: Int?,
    rankingLabel: String?,
    collectionTitle: String?,
): String = listOfNotNull(
    popularityScore?.takeIf { it > 0.0 }?.let { "Popularity ${it.cleanNumber()}" },
    rankingPosition?.takeIf { it > 0 }?.let { "${rankingLabel ?: "Rank"} #$it" },
    collectionTitle?.takeIf { it.isNotBlank() }?.let { "Collection $it" },
).joinToString(", ")

private fun Double.cleanNumber(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
}

private fun <T> T?.takeIfSelected(field: MetadataRefreshField, selectedFields: Set<MetadataRefreshField>): T? {
    return if (field in selectedFields) this else null
}

private fun MediaItemEntity.metadataOverrideFields(): Set<MetadataRefreshField> {
    return metadataOverrideFieldsCsv
        ?.split(',')
        ?.mapNotNull { fieldName ->
            runCatching { MetadataRefreshField.valueOf(fieldName) }.getOrNull()
        }
        ?.toSet()
        .orEmpty()
}

private fun Set<MetadataRefreshField>.toMetadataOverrideFieldsCsv(): String? {
    return takeIf { it.isNotEmpty() }
        ?.sortedBy { field -> field.name }
        ?.joinToString(",") { field -> field.name }
}

private fun MetadataSource.defaultExternalRatingSource(): String {
    return when (this) {
        MetadataSource.AniList -> ExternalRatingSource.AniList
        MetadataSource.Jikan -> ExternalRatingSource.Mal
        MetadataSource.OpenLibrary -> ExternalRatingSource.OpenLibrary
        MetadataSource.GoogleBooks -> ExternalRatingSource.GoogleBooks
        MetadataSource.Tmdb -> ExternalRatingSource.Tmdb
        MetadataSource.Rawg -> ExternalRatingSource.Rawg
        MetadataSource.Imdb -> ExternalRatingSource.Imdb
        MetadataSource.StoryGraph -> ExternalRatingSource.StoryGraph
    }.name
}

private fun MediaType.preferredPrimaryExternalRating(
    ratings: List<MetadataExternalRatingSuggestion>,
    fallback: MetadataRatingSuggestion?,
): MetadataRatingSuggestion? {
    val preferredRating = when (this) {
        MediaType.Book -> ratings.firstOrNull {
            it.source == ExternalRatingSource.Goodreads && it.score > 0.0 && it.maxScore > 0.0
        }
        else -> null
    }
    return preferredRating?.toPrimaryRating() ?: fallback
}

private fun MetadataExternalRatingSuggestion.toPrimaryRating(): MetadataRatingSuggestion {
    return MetadataRatingSuggestion(
        score = score,
        maxScore = maxScore,
        voteCount = voteCount,
    )
}

private fun MetadataSuggestion.withPreservedMyAnimeListId(currentItem: MediaItemEntity): MetadataSuggestion {
    if (mediaType != MediaType.Anime || source != MetadataSource.AniList) return this
    val malId = popularityJson.myAnimeListIdFromJson()
        ?: currentItem.myAnimeListId()
        ?: return this
    val mergedPopularityJson = runCatching {
        JSONObject(popularityJson ?: "{}")
            .put("malId", malId)
            .toString()
    }.getOrDefault(popularityJson)
    return copy(popularityJson = mergedPopularityJson)
}

private fun ExternalRatingEntity.isPrimaryExternalRating(mediaItem: MediaItemEntity?): Boolean {
    mediaItem?.primaryExternalRatingId?.let { return id == it }
    val primaryScore = mediaItem?.externalRatingScore ?: return false
    val primaryMax = mediaItem.externalRatingMax ?: return false
    return score.closeTo(primaryScore) && maxScore.closeTo(primaryMax)
}

private fun List<ExternalRatingEntity>.preferredPrimaryReplacement(mediaItem: MediaItemEntity?): ExternalRatingEntity? {
    val mediaType = mediaItem?.type?.let { type ->
        runCatching { MediaType.valueOf(type) }.getOrNull()
    }
    return when (mediaType) {
        MediaType.Book -> firstOrNull {
            it.source == ExternalRatingSource.Goodreads.name && it.score > 0.0 && it.maxScore > 0.0
        } ?: firstOrNull()
        else -> firstOrNull()
    }
}

private fun Double.closeTo(other: Double): Boolean = kotlin.math.abs(this - other) < 0.001

private fun MediaItemEntity.myAnimeListId(): String? {
    if (metadataSource == MetadataSource.Jikan.name && !metadataExternalId.isNullOrBlank()) {
        return metadataExternalId.trim()
    }
    return popularityJson.myAnimeListIdFromJson()
}

private fun String?.myAnimeListIdFromJson(): String? {
    return runCatching {
        JSONObject(this ?: "{}").optInt("malId", 0).takeIf { it > 0 }?.toString()
    }.getOrNull()
}

private suspend fun MediaDao.insertProgressUpdateIfNeeded(
    mediaItemId: Long,
    sessionId: Long,
    progressValue: Int,
    createdAtEpochMillis: Long,
    countsTowardObjectives: Boolean = true,
) {
    if (progressValue <= 0) return

    insertProgressUpdate(
        ProgressUpdateEntity(
            mediaItemId = mediaItemId,
            sessionId = sessionId,
            progressValue = progressValue,
            loggedAtEpochDay = LocalDate.now().toEpochDay(),
            createdAtEpochMillis = createdAtEpochMillis,
            countsTowardObjectives = countsTowardObjectives,
        ),
    )
}

private fun MediaItemEntity.importDuplicateKey(): String {
    return if (metadataSource == MetadataSource.Imdb.name && !metadataExternalId.isNullOrBlank()) {
        "imdb:${metadataExternalId.trim().lowercase()}"
    } else {
        "${type.lowercase()}:${title.normalizedImportTitle()}:${releaseYear ?: ""}"
    }
}

private fun ImdbCsvItem.importDuplicateKey(): String {
    return if (!imdbId.isNullOrBlank()) {
        "imdb:${imdbId.trim().lowercase()}"
    } else {
        "${type?.name?.lowercase().orEmpty()}:${title.normalizedImportTitle()}:${releaseYear ?: ""}"
    }
}

private fun MediaItemEntity.storyGraphDuplicateKey(): String {
    return if (metadataSource == MetadataSource.StoryGraph.name && !metadataExternalId.isNullOrBlank()) {
        "storygraph:${metadataExternalId.trim().lowercase()}"
    } else {
        "book:${title.normalizedImportTitle()}:${creatorsJson.toStringList().firstOrNull()?.normalizedImportTitle().orEmpty()}"
    }
}

private fun StoryGraphCsvItem.storyGraphDuplicateKey(): String {
    return if (!isbnOrUid.isNullOrBlank()) {
        "storygraph:${isbnOrUid.trim().lowercase()}"
    } else {
        "book:${title.normalizedImportTitle()}:${authors.firstOrNull()?.normalizedImportTitle().orEmpty()}"
    }
}

private fun MediaItemEntity.myAnimeListDuplicateKey(): String {
    val malId = myAnimeListId()
    return if (!malId.isNullOrBlank()) {
        "mal:${malId.trim().lowercase()}"
    } else {
        "anime:${title.normalizedImportTitle()}:${progressTotal ?: ""}"
    }
}

private fun MyAnimeListXmlItem.myAnimeListDuplicateKey(): String {
    return if (!malId.isNullOrBlank()) {
        "mal:${malId.trim().lowercase()}"
    } else {
        "anime:${title.normalizedImportTitle()}:${episodeTotal ?: ""}"
    }
}

private fun String.normalizedImportTitle(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun parseBackupRoot(json: String): JSONObject {
    val root = JSONObject(json)
    val schemaVersion = root.optInt("schemaVersion", -1)
    if (schemaVersion !in 1..5) {
        throw UnsupportedBackupSchemaException(schemaVersion)
    }

    return root
}

private data class ParsedBackup(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long?,
    val collections: List<MediaCollectionEntity>,
    val mediaItems: List<MediaItemEntity>,
    val mediaCredits: List<MediaCreditEntity>,
    val sessions: List<TrackingSessionEntity>,
    val progressUpdates: List<ProgressUpdateEntity>,
    val externalRatings: List<ExternalRatingEntity>,
    val objectives: List<ObjectiveEntity>,
)

private fun parseBackupData(json: String): ParsedBackup {
    val root = parseBackupRoot(json)
    return ParsedBackup(
        schemaVersion = root.getInt("schemaVersion"),
        exportedAtEpochMillis = root.optNullableLong("exportedAtEpochMillis"),
        collections = root.optJSONArray("collections").orEmptyArray()
            .mapObjects { it.toMediaCollectionEntity() },
        mediaItems = root.getJSONArray("mediaItems")
            .mapObjects { it.toMediaItemEntity() },
        mediaCredits = root.optJSONArray("mediaCredits").orEmptyArray()
            .mapObjects { it.toMediaCreditEntity() },
        sessions = root.getJSONArray("trackingSessions")
            .mapObjects { it.toTrackingSessionEntity() },
        progressUpdates = root.optJSONArray("progressUpdates").orEmptyArray()
            .mapObjects { it.toProgressUpdateEntity() },
        externalRatings = root.optJSONArray("externalRatings").orEmptyArray()
            .mapObjects { it.toExternalRatingEntity() },
        objectives = root.optJSONArray("objectives").orEmptyArray()
            .mapObjects { it.toObjectiveEntity() },
    ).also { it.validate() }
}

private fun ParsedBackup.validate() {
    collections.requireUniquePositiveIds("collections") { it.id }
    mediaItems.requireUniquePositiveIds("media items") { it.id }
    mediaCredits.requireUniquePositiveIds("media credits") { it.id }
    sessions.requireUniquePositiveIds("tracking sessions") { it.id }
    progressUpdates.requireUniquePositiveIds("progress updates") { it.id }
    externalRatings.requireUniquePositiveIds("external ratings") { it.id }
    objectives.requireUniquePositiveIds("objectives") { it.id }

    val collectionIds = collections.map { it.id }.toSet()
    val mediaItemIds = mediaItems.map { it.id }.toSet()
    val sessionIds = sessions.map { it.id }.toSet()

    collections.forEach { collection ->
        require(collection.name.isNotBlank()) { "Collection names cannot be blank" }
    }

    mediaItems.forEach { item ->
        require(item.title.isNotBlank()) { "Media item titles cannot be blank" }
        requireEnum<MediaType>(item.type) { "Unknown media type: ${item.type}" }
        requireEnum<OwnershipType>(item.ownershipType) { "Unknown ownership type: ${item.ownershipType}" }
        item.metadataSource?.let { source ->
            requireEnum<MetadataSource>(source) { "Unknown metadata source: $source" }
        }
        item.collectionId?.let { collectionId ->
            require(collectionId in collectionIds) { "Media item references a missing collection" }
        }
        item.collectionSortOrder?.let { sortOrder ->
            require(sortOrder >= 0.0) { "Collection order cannot be negative" }
            require(item.collectionId != null) { "Collection order requires a collection" }
        }
        item.progressTotal?.let { total ->
            require(total >= 0) { "Progress totals cannot be negative" }
        }
        item.externalRatingScore?.let { score ->
            require(score > 0.0) { "External rating scores must be positive" }
        }
        item.externalRatingMax?.let { maxScore ->
            require(maxScore > 0.0) { "External rating max scores must be positive" }
        }
        item.externalRatingVoteCount?.let { count ->
            require(count >= 0) { "External rating vote counts cannot be negative" }
        }
    }

    mediaCredits.forEach { credit ->
        require(credit.mediaItemId in mediaItemIds) { "Credit references a missing media item" }
        require(credit.personName.isNotBlank()) { "Credit names cannot be blank" }
        requireEnum<MediaCreditRole>(credit.roleType) { "Unknown credit role: ${credit.roleType}" }
        credit.metadataSource?.let { source ->
            requireEnum<MetadataSource>(source) { "Unknown metadata source: $source" }
        }
    }

    sessions.forEach { session ->
        require(session.mediaItemId in mediaItemIds) { "Session references a missing media item" }
        require(session.sessionNumber > 0) { "Session numbers must be positive" }
        require(session.progressCurrent >= 0) { "Session progress cannot be negative" }
        requireEnum<TrackingStatus>(session.status) { "Unknown tracking status: ${session.status}" }
        session.platformType?.let { platformType ->
            requireEnum<ConsumptionPlatformType>(platformType) { "Unknown platform type: $platformType" }
        }
        session.rating?.let { rating ->
            require(rating in 1..10) { "Session ratings must be between 1 and 10" }
        }
    }

    progressUpdates.forEach { update ->
        require(update.mediaItemId in mediaItemIds) { "Progress update references a missing media item" }
        require(update.sessionId in sessionIds) { "Progress update references a missing session" }
        require(update.progressValue >= 0) { "Progress update values cannot be negative" }
    }

    externalRatings.forEach { rating ->
        require(rating.mediaItemId in mediaItemIds) { "External rating references a missing media item" }
        requireEnum<ExternalRatingSource>(rating.source) { "Unknown external rating source: ${rating.source}" }
        require(rating.score > 0.0) { "External rating scores must be positive" }
        require(rating.maxScore > 0.0) { "External rating max scores must be positive" }
        rating.voteCount?.let { count ->
            require(count >= 0) { "External rating vote counts cannot be negative" }
        }
    }

    objectives.forEach { objective ->
        requireEnum<ObjectiveMetric>(objective.metric) { "Unknown objective metric: ${objective.metric}" }
        requireEnum<ObjectiveUnit>(objective.unit) { "Unknown objective unit: ${objective.unit}" }
        objective.mediaType?.let { mediaType ->
            requireEnum<MediaType>(mediaType) { "Unknown objective media type: $mediaType" }
        }
        val domainObjective = objective.toDomain()
        require(domainObjective.definition().isValid) { "Objective definitions must be compatible" }
        require(domainObjective.unit == domainObjective.canonicalUnit()) { "Objective units must match their definitions" }
        require(objective.name.isNotBlank()) { "Objective names cannot be blank" }
        require(objective.targetValue > 0) { "Objective targets must be positive" }
        require(objective.endDateEpochDay >= objective.startDateEpochDay) { "Objective end date cannot precede start date" }
    }
}

private fun ExternalRatingEntity.toPrimaryRating(): MetadataRatingSuggestion {
    return MetadataRatingSuggestion(score = score, maxScore = maxScore, voteCount = voteCount)
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
        .putNullable("collectionSortOrder", collectionSortOrder)
        .putNullable("progressTotal", progressTotal)
        .putNullable("originalTitle", originalTitle)
        .putNullable("releaseYear", releaseYear)
        .putNullable("language", language)
        .put("genres", JSONArray(genresJson.toStringList()))
        .put("creators", JSONArray(creatorsJson.toStringList()))
        .putNullable("coverUrl", coverUrl)
        .putNullable("synopsis", synopsis)
        .putNullable("sourceUrl", sourceUrl)
        .putNullable("externalRatingScore", externalRatingScore)
        .putNullable("externalRatingMax", externalRatingMax)
        .putNullable("externalRatingVoteCount", externalRatingVoteCount)
        .putNullable("primaryExternalRatingId", primaryExternalRatingId)
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
        .putNullable("metadataOverrideFieldsCsv", metadataOverrideFieldsCsv)
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

private fun ProgressUpdateEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("sessionId", sessionId)
        .put("progressValue", progressValue)
        .put("loggedAtEpochDay", loggedAtEpochDay)
        .put("hasKnownDate", hasKnownDate)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("countsTowardObjectives", countsTowardObjectives)
}

private fun ExternalRatingEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("source", source)
        .put("score", score)
        .put("maxScore", maxScore)
        .putNullable("voteCount", voteCount)
        .put("origin", origin)
}

private fun ObjectiveEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("metric", metric)
        .put("unit", unit)
        .putNullable("mediaType", mediaType)
        .put("targetValue", targetValue)
        .put("startDateEpochDay", startDateEpochDay)
        .put("endDateEpochDay", endDateEpochDay)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .putNullable("archivedAtEpochMillis", archivedAtEpochMillis)
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
        collectionSortOrder = optNullableDouble("collectionSortOrder"),
        progressTotal = optNullableInt("progressTotal"),
        originalTitle = optNullableString("originalTitle"),
        releaseYear = optNullableInt("releaseYear"),
        language = optNullableString("language"),
        genresJson = optStringArray("genres").toJsonArrayString(),
        creatorsJson = optStringArray("creators").toJsonArrayString(),
        coverUrl = optNullableString("coverUrl"),
        synopsis = normalizeSynopsis(optNullableString("synopsis")),
        sourceUrl = optNullableString("sourceUrl"),
        externalRatingScore = optNullableDouble("externalRatingScore"),
        externalRatingMax = optNullableDouble("externalRatingMax"),
        externalRatingVoteCount = optNullableInt("externalRatingVoteCount"),
        primaryExternalRatingId = optNullableLong("primaryExternalRatingId"),
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
        metadataOverrideFieldsCsv = optNullableString("metadataOverrideFieldsCsv"),
        isOwned = optBoolean("isOwned", false),
        ownershipType = optString("ownershipType", "None"),
    )
}

private fun JSONObject.toObjectiveEntity(): ObjectiveEntity {
    return ObjectiveEntity(
        id = getLong("id"),
        name = getString("name"),
        metric = getString("metric"),
        unit = getString("unit"),
        mediaType = optNullableString("mediaType"),
        targetValue = getInt("targetValue"),
        startDateEpochDay = getLong("startDateEpochDay"),
        endDateEpochDay = getLong("endDateEpochDay"),
        createdAtEpochMillis = optLong("createdAtEpochMillis", System.currentTimeMillis()),
        archivedAtEpochMillis = optNullableLong("archivedAtEpochMillis"),
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

private fun JSONObject.toProgressUpdateEntity(): ProgressUpdateEntity {
    return ProgressUpdateEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        sessionId = getLong("sessionId"),
        progressValue = optInt("progressValue", 0),
        loggedAtEpochDay = optLong("loggedAtEpochDay", LocalDate.now().toEpochDay()),
        hasKnownDate = optBoolean("hasKnownDate", true),
        createdAtEpochMillis = optLong("createdAtEpochMillis", 0),
        countsTowardObjectives = optBoolean("countsTowardObjectives", true),
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
        origin = optString("origin", "Manual"),
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

private fun JSONArray?.orEmptyArray(): JSONArray {
    return this ?: JSONArray()
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}

private fun <T> List<T>.requireUniquePositiveIds(
    label: String,
    idSelector: (T) -> Long,
) {
    val ids = map(idSelector)
    require(ids.all { it > 0L }) { "Backup $label contain invalid ids" }
    require(ids.size == ids.toSet().size) { "Backup $label contain duplicate ids" }
}

private inline fun <reified T : Enum<T>> requireEnum(
    value: String,
    lazyMessage: () -> String,
) {
    require(runCatching { enumValueOf<T>(value) }.isSuccess, lazyMessage)
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
