package com.nilpo.contenttracker.core.repository

import androidx.room.withTransaction
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.database.dao.MediaDao
import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ObjectiveEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.database.migration.LegacyProgressRow
import com.nilpo.contenttracker.core.database.migration.convertSessionToIncrements
import com.nilpo.contenttracker.core.database.mapper.toDomain
import com.nilpo.contenttracker.core.database.mapper.toEntity
import com.nilpo.contenttracker.core.mal.MalSyncPayload
import com.nilpo.contenttracker.core.mal.buildMalSyncPayload
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.storedName
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.core.model.metadataJsonWithSteamAppId
import com.nilpo.contenttracker.core.model.normalizeSteamAppId
import com.nilpo.contenttracker.core.model.steamAppIdFromMetadataJson
import com.nilpo.contenttracker.core.model.normalizeSynopsis
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.model.resolveMyAnimeListId
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.canonicalUnit
import com.nilpo.contenttracker.core.model.definition
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportItemState
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.imports.isValidIsbn
import com.nilpo.contenttracker.core.imports.normalizeIsbn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.Normalizer
import java.util.Locale

class OfflineMediaRepository(
    private val database: ContentTrackerDatabase,
    private val onMalRelevantChange: suspend (Long) -> Unit = {},
) : MediaRepository {
    private val mediaDao: MediaDao = database.mediaDao()
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
                        session.toDomain(relation.progressUpdates, relation.statusEvents)
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

    override suspend fun exportBackupJson(): String = database.withTransaction {
        JSONObject()
            .put("schemaVersion", BackupSchemaVersion)
            .put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("collections", JSONArray(mediaDao.getMediaCollections().map { it.toJson() }))
            .put("mediaItems", JSONArray(mediaDao.getMediaItems().map { it.toJson() }))
            .put("mediaCredits", JSONArray(mediaDao.getMediaCredits().map { it.toJson() }))
            .put("trackingSessions", JSONArray(mediaDao.getAllTrackingSessions().map { it.toJson() }))
            .put("progressUpdates", JSONArray(mediaDao.getProgressUpdates().map { it.toJson() }))
            .put("sessionStatusEvents", JSONArray(mediaDao.getSessionStatusEvents().map { it.toJson() }))
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
            sessionStatusEvents = backup.sessionStatusEvents,
            externalRatings = backup.externalRatings,
            objectives = backup.objectives,
        )
    }

    override suspend fun previewImdbCsv(csv: String): ImdbCsvPreview {
        return prepareImdbCsv(csv).preview
    }

    override suspend fun prepareImdbCsv(csv: String): PreparedImdbCsvImport {
        val parsed = parseImdbCsvWithReport(csv)
        val rows = parsed.rows
        val existingKeys = imdbExistingKeys()
        val preview = planProviderImport(
            rows = rows,
            existingKeys = existingKeys,
            duplicateKey = ImdbCsvItem::importDuplicateKey,
            isSupported = { item -> item.type != null },
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
            sourceRowNumber = ImdbCsvItem::sourceRowNumber,
            displayLabel = ImdbCsvItem::title,
        ).toPreview()
        return PreparedImdbCsvImport(parsed, preview)
    }

    private suspend fun imdbExistingKeys(): Set<String> = buildSet {
        mediaDao.getMediaItems().mapTo(this) { item -> item.importDuplicateKey() }
        database.importDao()
            .getActiveSourceKeys(ImportSource.ImdbCsv.name)
            .mapTo(this) { sourceKey -> sourceKey.canonicalImdbSourceKey() }
    }

    override suspend fun importImdbCsv(csv: String): ImdbCsvImportResult {
        return importPreparedImdbCsv(prepareImdbCsv(csv))
    }

    override suspend fun importPreparedImdbCsv(prepared: PreparedImdbCsvImport): ImdbCsvImportResult {
        val parsed = prepared.parsed
        val rows = parsed.rows
        return importProviderRows(
            rows = rows,
            source = ImportSource.ImdbCsv,
            existingKey = MediaItemEntity::importDuplicateKey,
            duplicateKey = ImdbCsvItem::importDuplicateKey,
            isSupported = { item -> item.type != null },
            persist = { item ->
                persistTrackedMedia(item.toAddTrackedMediaRequest(), MediaWriteOrigin.ProviderImport)
            },
            sourceExternalId = ImdbCsvItem::imdbId,
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
            sourceRowNumber = ImdbCsvItem::sourceRowNumber,
            displayLabel = ImdbCsvItem::title,
            additionalExistingKeys = {
                database.importDao()
                    .getActiveSourceKeys(ImportSource.ImdbCsv.name)
                    .map(String::canonicalImdbSourceKey)
            },
        )
    }

    private suspend fun persistImportedSessions(
        request: AddTrackedMediaRequest,
        importedSessions: List<ImportedTrackingSession>,
    ): Long = database.withTransaction {
        require(importedSessions.isNotEmpty()) { "An imported media item must have a session" }
        val mediaItemId = insertTrackedMedia(
            request = request,
            origin = MediaWriteOrigin.ProviderImport,
        )
        val firstSession = requireNotNull(mediaDao.getTrackingSessions(mediaItemId).singleOrNull())
        importedSessions.drop(1).forEachIndexed { index, session ->
            val validProgress = request.progressTotal?.let { total ->
                session.progressCurrent.coerceIn(0, total.coerceAtLeast(0))
            } ?: session.progressCurrent.coerceAtLeast(0)
            mediaDao.insertTrackingSession(
                firstSession.copy(
                    id = 0,
                    sessionNumber = index + 2,
                    status = session.status.name,
                    progressCurrent = validProgress,
                    baselineProgress = validProgress,
                    // The provider scored in whole points; the column holds half points.
                    ratingHalfPoints = session.rating?.let(RatingHalfPoints::fromWholePoints),
                    notes = session.notes?.trim()?.takeIf { it.isNotBlank() },
                    startedAtEpochDay = session.startedAt?.toEpochDay(),
                    finishedAtEpochDay = session.finishedAt?.toEpochDay(),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
        mediaItemId
    }

    override suspend fun previewStoryGraphCsv(csv: String): StoryGraphCsvPreview {
        return prepareStoryGraphCsv(csv).preview
    }

    override suspend fun prepareStoryGraphCsv(csv: String): PreparedStoryGraphCsvImport {
        val parsed = parseStoryGraphCsvWithReport(csv)
        val rows = parsed.rows
        val existingKeys = storyGraphExistingKeys()
        val preview = planProviderImport(
            rows = rows,
            existingKeys = existingKeys,
            duplicateKey = StoryGraphCsvItem::storyGraphDuplicateKey,
            isSupported = { item -> item.title.isNotBlank() },
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
            sourceRowNumber = StoryGraphCsvItem::sourceRowNumber,
            displayLabel = StoryGraphCsvItem::title,
        ).toPreview()
        return PreparedStoryGraphCsvImport(parsed, preview)
    }

    private suspend fun storyGraphExistingKeys(): Set<String> = buildSet {
        mediaDao.getMediaItems().mapTo(this) { item -> item.storyGraphDuplicateKey() }
        database.importDao()
            .getActiveSourceKeys(ImportSource.StoryGraphCsv.name)
            .mapTo(this) { sourceKey -> sourceKey.canonicalStoryGraphSourceKey() }
    }

    override suspend fun importStoryGraphCsv(csv: String): StoryGraphCsvImportResult {
        return importPreparedStoryGraphCsv(prepareStoryGraphCsv(csv))
    }

    override suspend fun importPreparedStoryGraphCsv(
        prepared: PreparedStoryGraphCsvImport,
    ): StoryGraphCsvImportResult {
        val parsed = prepared.parsed
        val rows = parsed.rows
        return importProviderRows(
            rows = rows,
            source = ImportSource.StoryGraphCsv,
            existingKey = MediaItemEntity::storyGraphDuplicateKey,
            duplicateKey = StoryGraphCsvItem::storyGraphDuplicateKey,
            isSupported = { item -> item.title.isNotBlank() },
            persist = { item ->
                val sessions = item.importedSessions()
                persistImportedSessions(item.toAddTrackedMediaRequest(sessions.first()), sessions)
            },
            sourceExternalId = StoryGraphCsvItem::isbnOrUid,
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
            sourceRowNumber = StoryGraphCsvItem::sourceRowNumber,
            displayLabel = StoryGraphCsvItem::title,
            additionalExistingKeys = {
                database.importDao()
                    .getActiveSourceKeys(ImportSource.StoryGraphCsv.name)
                    .map(String::canonicalStoryGraphSourceKey)
            },
        )
    }

    override suspend fun previewMyAnimeListXml(xml: String): MyAnimeListXmlPreview {
        return prepareMyAnimeListXml(xml).preview
    }

    override suspend fun prepareMyAnimeListXml(xml: String): PreparedMyAnimeListXmlImport {
        val parsed = parseMyAnimeListXmlWithReport(xml)
        val existingKeys = mediaDao.getMediaItems().map { it.myAnimeListDuplicateKey() }
        val preview = planProviderImport(
            rows = parsed.rows,
            existingKeys = existingKeys,
            duplicateKey = MyAnimeListImportItem::myAnimeListDuplicateKey,
            isSupported = { item -> item.title.isNotBlank() },
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
        ).toPreview()
        return PreparedMyAnimeListXmlImport(parsed, preview)
    }

    override suspend fun previewMyAnimeListAccount(
        items: List<MyAnimeListImportItem>,
    ): ProviderImportPreview {
        val existingKeys = mediaDao.getMediaItems().map { it.myAnimeListDuplicateKey() }.toMutableSet()
        return planProviderImport(
            rows = items,
            existingKeys = existingKeys,
            duplicateKey = MyAnimeListImportItem::myAnimeListDuplicateKey,
            isSupported = { item -> item.title.isNotBlank() },
        ).toPreview()
    }

    override suspend fun importMyAnimeListXml(xml: String): MyAnimeListXmlImportResult {
        return importPreparedMyAnimeListXml(prepareMyAnimeListXml(xml))
    }

    override suspend fun importPreparedMyAnimeListXml(
        prepared: PreparedMyAnimeListXmlImport,
    ): MyAnimeListXmlImportResult {
        val parsed = prepared.parsed
        return importMyAnimeListRows(
            items = parsed.rows,
            source = ImportSource.MalXml,
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            rejectedRows = parsed.rejectedRows,
        )
    }

    override suspend fun importMyAnimeListAccount(
        items: List<MyAnimeListImportItem>,
    ): ProviderImportResult {
        return importMyAnimeListRows(items, ImportSource.MalApi)
    }

    private suspend fun importMyAnimeListRows(
        items: List<MyAnimeListImportItem>,
        source: ImportSource,
        totalRows: Int = items.size,
        invalidRows: Int = 0,
        rejectedRows: List<ProviderRejectedRow> = emptyList(),
    ): ProviderImportResult {
        return importProviderRows(
            rows = items,
            source = source,
            existingKey = MediaItemEntity::myAnimeListDuplicateKey,
            duplicateKey = MyAnimeListImportItem::myAnimeListDuplicateKey,
            isSupported = { item -> item.title.isNotBlank() },
            persist = { item ->
                val sessions = item.importedSessions()
                persistImportedSessions(item.toAddTrackedMediaRequest(sessions.first()), sessions)
            },
            sourceExternalId = { item -> item.malId?.toString() },
            totalRows = totalRows,
            invalidRows = invalidRows,
            initialRejectedRows = rejectedRows,
        )
    }

    /** Inserts media, sessions, and their enrichment queue as one all-or-nothing operation. */
    private suspend fun <T> importProviderRows(
        rows: List<T>,
        source: ImportSource,
        existingKey: (MediaItemEntity) -> String,
        duplicateKey: (T) -> String,
        isSupported: (T) -> Boolean,
        persist: suspend (T) -> Long,
        sourceExternalId: (T) -> String?,
        additionalExistingKeys: suspend () -> Collection<String> = { emptyList() },
        totalRows: Int = rows.size,
        invalidRows: Int = 0,
        initialRejectedRows: List<ProviderRejectedRow> = emptyList(),
        sourceRowNumber: (T) -> Int? = { null },
        displayLabel: (T) -> String? = { null },
    ): ProviderImportResult = database.withTransaction {
        // Planning inside the write transaction prevents concurrent imports from accepting the
        // same source row between duplicate detection and insertion.
        val plan = planProviderImport(
            rows = rows,
            existingKeys = mediaDao.getMediaItems().map(existingKey) + additionalExistingKeys(),
            duplicateKey = duplicateKey,
            isSupported = isSupported,
            totalRows = totalRows,
            invalidRows = invalidRows,
            initialRejectedRows = initialRejectedRows,
            sourceRowNumber = sourceRowNumber,
            displayLabel = displayLabel,
        )
        val importedIds = plan.importable.map { item -> persist(item) }
        val batchId = recordImportBatch(
            source = source,
            rows = plan.importable.zip(importedIds),
            sourceKey = duplicateKey,
            sourceExternalId = sourceExternalId,
        )
        plan.toResult(importedIds, batchId)
    }

    private suspend fun <T> recordImportBatch(
        source: ImportSource,
        rows: List<Pair<T, Long>>,
        sourceKey: (T) -> String,
        sourceExternalId: (T) -> String?,
    ): Long? {
        if (rows.isEmpty()) return null
        val now = System.currentTimeMillis()
        val importDao = database.importDao()
        val batchId = importDao.insertBatch(
            ImportBatchEntity(
                source = source.name,
                state = ImportBatchState.Enriching.name,
                totalCount = rows.size,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        importDao.insertItems(
            rows.map { (row, mediaItemId) ->
                ImportBatchItemEntity(
                    batchId = batchId,
                    sourceKey = sourceKey(row),
                    sourceExternalId = sourceExternalId(row),
                    mediaItemId = mediaItemId,
                    state = ImportItemState.Pending.name,
                    updatedAtEpochMillis = now,
                )
            },
        )
        return batchId
    }

    override suspend fun startNewSession(request: AddTrackingSessionRequest) = database.withTransaction {
        val sessions = mediaDao.getTrackingSessions(request.mediaItemId)
        val latestSession = sessions.maxByOrNull { it.sessionNumber }
        val mediaItem = mediaDao.getMediaItem(request.mediaItemId) ?: return@withTransaction
        val malPayloadBefore = currentMalSyncPayload(request.mediaItemId)
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
                ratingHalfPoints = request.ratingHalfPoints?.let(RatingHalfPoints::coerce),
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
                ratingHalfPoints = request.ratingHalfPoints?.let(RatingHalfPoints::coerce),
                notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
                platformName = validPlatformName,
                platformType = validPlatformName?.let { request.platformType.name },
                startedAtEpochDay = request.startedAt?.toEpochDay(),
                finishedAtEpochDay = request.finishedAt?.toEpochDay(),
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        }

        // Progress supplied when a session is created is where the user already was, not a sitting
        // they logged, so it becomes the session's baseline and produces no entry.
        mediaDao.insertTrackingSession(newSession.copy(baselineProgress = validProgress))
        notifyMalIfPayloadChanged(request.mediaItemId, malPayloadBefore)
    }

    override suspend fun addTrackedMedia(request: AddTrackedMediaRequest): Long =
        persistTrackedMedia(request, MediaWriteOrigin.UserAction)

    private suspend fun persistTrackedMedia(
        request: AddTrackedMediaRequest,
        origin: MediaWriteOrigin,
    ): Long {
        val mediaItemId = insertTrackedMedia(request, origin)
        if (origin.queuesOutboundMalSync) {
            notifyMalIfPayloadChanged(mediaItemId, before = null)
        }
        return mediaItemId
    }

    private suspend fun insertTrackedMedia(
        request: AddTrackedMediaRequest,
        origin: MediaWriteOrigin,
    ): Long = database.withTransaction {
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
                language = ItemLanguage.normalize(request.language).takeUnless { request.type == MediaType.Game },
                genresJson = request.genres.toJsonArrayString(),
                tagsJson = request.tags.cleanMetadataList().toJsonArrayString(),
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
                metadataLastFetchedAtEpochMillis = request.metadataSource
                    ?.takeIf { origin == MediaWriteOrigin.UserAction }
                    ?.let { System.currentTimeMillis() },
                metadataExternalId = request.metadataExternalId,
                metadataSource = request.metadataSource?.name,
                malId = resolveMyAnimeListId(
                    explicitMalId = request.malId,
                    metadataSource = request.metadataSource?.name,
                    metadataExternalId = request.metadataExternalId,
                    popularityJson = request.popularityJson,
                    sourceUrl = request.sourceUrl,
                ),
                imdbId = request.metadataExternalId
                    ?.takeIf { request.metadataSource == MetadataSource.Imdb }
                    ?.canonicalImdbIdentifier(),
                storyGraphId = request.metadataExternalId
                    ?.takeIf { request.metadataSource == MetadataSource.StoryGraph }
                    ?.canonicalStoryGraphIdentifier(),
                isOwned = request.isOwned,
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
                .distinctBy { it.source }
                .forEach { rating ->
                    val ratingId = mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = mediaItemId,
                            source = rating.source.name,
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                            scoreDescriptor = rating.scoreDescriptor,
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
        mediaDao.insertTrackingSession(
            TrackingSessionEntity(
                mediaItemId = mediaItemId,
                sessionNumber = 1,
                status = request.initialStatus.name,
                progressCurrent = initialProgress,
                // Where the user already was when they added this. See addSession.
                baselineProgress = initialProgress,
                ratingHalfPoints = request.initialRatingHalfPoints?.let(RatingHalfPoints::coerce),
                notes = request.initialNotes?.trim()?.takeIf { it.isNotBlank() },
                platformName = request.platformName?.trim()?.takeIf { it.isNotBlank() },
                platformType = request.platformType.name,
                startedAtEpochDay = request.initialStartedAt?.toEpochDay(),
                finishedAtEpochDay = request.initialFinishedAt?.toEpochDay(),
                updatedAtEpochMillis = initialUpdatedAtEpochMillis,
            ),
        )
        mediaItemId
    }

    override suspend fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        ratingHalfPoints: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ): DeletionRecovery.SessionMutation? = database.withTransaction {
        val before = captureSessionHistory(sessionId) ?: return@withTransaction null
        val session = before.session
        val mediaItem = mediaDao.getMediaItem(session.mediaItemId) ?: return@withTransaction null
        val malPayloadBefore = currentMalSyncPayload(session.mediaItemId)
        // Provider metadata is not allowed to rewrite user history. A corrected total can be below
        // already-recorded progress, so only the natural lower bound is enforced here.
        val validProgress = progressCurrent.coerceAtLeast(0)

        // A finish date belongs only to a terminal state. Keeping one while reopening a session made
        // the snapshot contradict its own status and caused stats/timeline to disagree.
        val validFinishedAt = finishedAt.takeIf {
            status == TrackingStatus.Completed || status == TrackingStatus.Dropped
        }
        if (startedAt != null && validFinishedAt != null && validFinishedAt.isBefore(startedAt)) {
            return@withTransaction null
        }

        val updatedAtEpochMillis = System.currentTimeMillis()
        // Compared as the stored string rather than as a parsed enum: an unrecognised value on the
        // row should read as "different from whatever we are writing", not silently become Planned.
        val previousStatus = session.status

        // Ending a session on a past date means everything this save records happened on that date,
        // not today. Without this, finishing a book you read last month logged the remaining pages
        // as today's reading — it counted towards this month's goals and showed up in today's
        // timeline, neither of which you did.
        val endedOn = validFinishedAt?.takeIf {
            status == TrackingStatus.Completed || status == TrackingStatus.Dropped
        }
        val lastTransitionDay = before.statusEvents.lastOrNull()?.resolvedOccurredOn()
        // Only a claimed day can contradict an ending. An undated transition's day is a placeholder,
        // and letting it block an earlier end date refused saves the user had no way to understand.
        // Mirrors `TrackingSession.earliestEndingDate`, which the status sheets use to say no up front.
        val lastDatedTransitionDay = before.statusEvents.lastOrNull { it.hasKnownDate }?.resolvedOccurredOn()
        if (status.name != previousStatus && endedOn != null && lastDatedTransitionDay?.isAfter(endedOn) == true) {
            return@withTransaction null
        }
        if (status.name == previousStatus && endedOn != null) {
            val terminalIndex = before.statusEvents.indexOfLast { it.status == status.name }
            val precedingDay = before.statusEvents
                .take(terminalIndex.coerceAtLeast(0))
                .lastOrNull { it.hasKnownDate }
                ?.resolvedOccurredOn()
            if (precedingDay?.isAfter(endedOn) == true) return@withTransaction null
        }
        // Starting a session happened on its start date, which may be earlier than today.
        val opensSession = status == TrackingStatus.InProgress && previousStatus == TrackingStatus.Planned.name
        val transitionDay = endedOn
            ?: startedAt?.takeIf { opensSession && lastTransitionDay?.isAfter(it) != true }
            ?: listOfNotNull(LocalDate.now(), startedAt, lastTransitionDay).maxOrNull()
            ?: LocalDate.now()

        if (validProgress != session.progressCurrent) {
            mediaDao.applyProgressTarget(
                session = session,
                target = validProgress,
                loggedAt = if (status.endsSession) endedOn else LocalDate.now(),
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        }

        mediaDao.updateSessionDetails(
            sessionId = sessionId,
            status = status.name,
            progressCurrent = validProgress,
            ratingHalfPoints = ratingHalfPoints?.let(RatingHalfPoints::coerce),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            startedAtEpochDay = startedAt?.toEpochDay(),
            finishedAtEpochDay = validFinishedAt?.toEpochDay(),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )

        // Log the transition, not the state. This is the only record that a session was ever paused:
        // the status column will be overwritten by the next change, and a pause followed by a resume
        // would otherwise leave nothing at all behind. Written only when the status actually moves,
        // so saving a session's notes does not manufacture an event.
        if (status.name != previousStatus) {
            mediaDao.insertSessionStatusEvent(
                SessionStatusEventEntity(
                    mediaItemId = session.mediaItemId,
                    sessionId = sessionId,
                    previousStatus = previousStatus,
                    status = status.name,
                    createdAtEpochMillis = updatedAtEpochMillis,
                    occurredOnEpochDay = transitionDay.toEpochDay(),
                    hasKnownDate = !status.endsSession || endedOn != null,
                ),
            )
        } else if (startedAt != null && startedAt.toEpochDay() != session.startedAtEpochDay) {
            // The opening transition and the start date are one fact; a corrected start moves both.
            before.statusEvents.currentOpening()?.let { opening ->
                val index = before.statusEvents.indexOf(opening)
                val precedingDay = before.statusEvents.take(index).lastOrNull { it.hasKnownDate }?.resolvedOccurredOn()
                val followingDay = before.statusEvents.drop(index + 1).firstOrNull { it.hasKnownDate }?.resolvedOccurredOn()
                if (precedingDay?.isAfter(startedAt) != true && followingDay?.isBefore(startedAt) != true) {
                    mediaDao.updateSessionStatusEventDate(opening.id, startedAt.toEpochDay())
                }
            }
        }
        if (status.name == previousStatus && (status == TrackingStatus.Completed || status == TrackingStatus.Dropped)) {
            // The terminal event owns its historical date. Keep it aligned when the session editor
            // corrects the snapshot's finish date without changing status.
            before.statusEvents.lastOrNull { it.status == status.name }?.let { event ->
                endedOn?.let { date ->
                    mediaDao.updateSessionStatusEventDate(event.id, date.toEpochDay())
                }
            }
        }
        val after = captureSessionHistory(sessionId) ?: return@withTransaction null
        notifyMalIfPayloadChanged(session.mediaItemId, malPayloadBefore)
        DeletionRecovery.SessionMutation(before = before, after = after)
    }

    private suspend fun captureSessionHistory(sessionId: Long): SessionHistoryState? {
        val session = mediaDao.getTrackingSession(sessionId) ?: return null
        return SessionHistoryState(
            session = session,
            progressUpdates = mediaDao.getProgressUpdatesForSession(sessionId),
            statusEvents = mediaDao.getSessionStatusEventsForSession(sessionId),
        )
    }

    private suspend fun restoreSessionMutation(
        recovery: DeletionRecovery.SessionMutation,
    ): Boolean {
        val current = captureSessionHistory(recovery.after.session.id) ?: return false
        if (current != recovery.after) return false

        mediaDao.deleteProgressUpdatesForSession(current.session.id)
        mediaDao.deleteSessionStatusEventsForSession(current.session.id)
        if (mediaDao.updateTrackingSession(recovery.before.session) != 1) return false
        recovery.before.progressUpdates.forEach { mediaDao.insertProgressUpdate(it) }
        recovery.before.statusEvents.forEach { mediaDao.insertSessionStatusEvent(it) }
        return true
    }

    override suspend fun deletePastSession(sessionId: Long): DeletionRecovery? = database.withTransaction {
        val session = mediaDao.getTrackingSession(sessionId) ?: return@withTransaction null
        val sessions = mediaDao.getTrackingSessions(session.mediaItemId)
        val malPayloadBefore = currentMalSyncPayload(session.mediaItemId)
        val latestSessionNumber = sessions.maxOfOrNull { it.sessionNumber } ?: return@withTransaction null
        if (sessions.size <= 1 || session.sessionNumber == latestSessionNumber) {
            return@withTransaction null
        }
        // Captured before the row goes away: the survivors are deliberately not renumbered, so
        // afterwards there is no way to recover which visit this was.
        val visitNumber = sessions
            .sortedBy { it.sessionNumber }
            .indexOfFirst { it.id == sessionId } + 1
        val progressUpdates = mediaDao.getProgressUpdatesForSession(sessionId)
        val statusEvents = mediaDao.getSessionStatusEventsForSession(sessionId)
        mediaDao.deleteTrackingSession(sessionId)
        notifyMalIfPayloadChanged(session.mediaItemId, malPayloadBefore)
        DeletionRecovery.PastSession(
            session = session,
            progressUpdates = progressUpdates,
            statusEvents = statusEvents,
            visitNumber = visitNumber,
        )
    }

    override suspend fun deleteCurrentSession(sessionId: Long): DeletionRecovery? = database.withTransaction {
        val session = mediaDao.getTrackingSession(sessionId) ?: return@withTransaction null
        val sessions = mediaDao.getTrackingSessions(session.mediaItemId)
        val malPayloadBefore = currentMalSyncPayload(session.mediaItemId)
        val latestSessionNumber = sessions.maxOfOrNull { it.sessionNumber } ?: return@withTransaction null
        // The mirror image of deletePastSession's guard: only the latest session, and only when a
        // previous one survives to become live again. Deleting the sole session is untracking.
        if (sessions.size <= 1 || session.sessionNumber != latestSessionNumber) {
            return@withTransaction null
        }
        val visitNumber = sessions
            .sortedBy { it.sessionNumber }
            .indexOfFirst { it.id == sessionId } + 1
        val progressUpdates = mediaDao.getProgressUpdatesForSession(sessionId)
        val statusEvents = mediaDao.getSessionStatusEventsForSession(sessionId)
        mediaDao.deleteTrackingSession(sessionId)
        notifyMalIfPayloadChanged(session.mediaItemId, malPayloadBefore)
        DeletionRecovery.PastSession(
            session = session,
            progressUpdates = progressUpdates,
            statusEvents = statusEvents,
            visitNumber = visitNumber,
        )
    }

    override suspend fun updateProgressUpdate(
        progressUpdateId: Long,
        amount: Int,
        loggedAt: LocalDate?,
        coversPeriod: Boolean?,
    ): Boolean = database.withTransaction {
        if (amount <= 0) return@withTransaction false
        val update = mediaDao.getProgressUpdate(progressUpdateId) ?: return@withTransaction false
        val malPayloadBefore = currentMalSyncPayload(update.mediaItemId)
        mediaDao.updateProgressUpdateAndRecalculateSession(
            progressUpdateId = progressUpdateId,
            amount = amount,
            loggedAtEpochDay = loggedAt?.toEpochDay() ?: update.loggedAtEpochDay,
            hasKnownDate = loggedAt != null,
            coversPeriod = coversPeriod ?: update.coversPeriod,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        reopenIfBelowTotal(update.sessionId, System.currentTimeMillis())
        notifyMalIfPayloadChanged(update.mediaItemId, malPayloadBefore)
        true
    }

    /**
     * A completed session whose recorded progress no longer reaches the item's total is not finished,
     * so a correction that takes it below reopens it. Logged as a reopening rather than by removing
     * the completion: the completion is still something the user recorded, and stays correctable.
     *
     * Returns the reopening's id, or null when nothing changed. Items without a fixed total (games)
     * never reopen.
     */
    private suspend fun reopenIfBelowTotal(sessionId: Long, updatedAtEpochMillis: Long): Long? {
        val session = mediaDao.getTrackingSession(sessionId) ?: return null
        if (session.status != TrackingStatus.Completed.name) return null
        val item = mediaDao.getMediaItem(session.mediaItemId) ?: return null
        val total = item.progressTotal?.takeUnless { item.type == MediaType.Game.name }?.takeIf { it > 0 }
            ?: return null
        if (session.progressCurrent >= total) return null

        mediaDao.updateSessionStatus(sessionId, TrackingStatus.InProgress.name, updatedAtEpochMillis)
        mediaDao.updateSessionFinishedDate(
            sessionId = sessionId,
            finishedAtEpochDay = null,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        // Today, unless the log already claims a later day: transition days never run backwards.
        val day = listOfNotNull(
            LocalDate.now(),
            mediaDao.getSessionStatusEventsForSession(sessionId).lastOrNull()?.resolvedOccurredOn(),
        ).max()
        return mediaDao.insertSessionStatusEvent(
            SessionStatusEventEntity(
                mediaItemId = session.mediaItemId,
                sessionId = sessionId,
                previousStatus = TrackingStatus.Completed.name,
                status = TrackingStatus.InProgress.name,
                createdAtEpochMillis = updatedAtEpochMillis,
                occurredOnEpochDay = day.toEpochDay(),
            ),
        )
    }

    /** Deletes one mistaken transition and restores the exact state it left when it is the latest. */
    override suspend fun deleteSessionStatusEvent(eventId: Long): DeletionRecovery? = database.withTransaction {
        val event = mediaDao.getSessionStatusEvent(eventId) ?: return@withTransaction null
        val session = mediaDao.getTrackingSession(event.sessionId) ?: return@withTransaction null
        val malPayloadBefore = currentMalSyncPayload(event.mediaItemId)
        val sessionEvents = mediaDao.getSessionStatusEventsForSession(event.sessionId)
        val eventIndex = sessionEvents.indexOfFirst { it.id == eventId }
        if (eventIndex < 0) return@withTransaction null

        // Only the newest transition decides where the session stands now. Removing an older one
        // edits the record without touching the present. Later rows that moved into the state they
        // left change nothing and are not shown, so they must not make this one historical either.
        val isLatest = (eventIndex + 1..sessionEvents.lastIndex).all { index ->
            val later = sessionEvents[index]
            (later.previousStatus ?: sessionEvents[index - 1].status) == later.status
        }
        val statusBefore = if (isLatest) {
            // The state this transition recorded leaving. The neighbouring row is only a fallback
            // for rows written before that was stored, and InProgress the last resort — you have to
            // have been going to have stopped.
            event.previousStatus
                ?: sessionEvents.getOrNull(eventIndex - 1)?.status
                ?: TrackingStatus.InProgress.name
        } else {
            null
        }

        mediaDao.deleteSessionStatusEvent(eventId)
        if (isLatest) {
            // Anything after it recorded no change. Reconnected to the restored state those rows
            // would suddenly claim a transition that never happened, so they go with it.
            sessionEvents.drop(eventIndex + 1).forEach { mediaDao.deleteSessionStatusEvent(it.id) }
        } else {
            // Removing a historical link also reconnects the transition after it to the state that
            // preceded the deleted row. Otherwise a later delete could resurrect a state that no
            // longer exists in the log.
            sessionEvents.getOrNull(eventIndex + 1)?.let { next ->
                mediaDao.updateSessionStatusEventPreviousStatus(next.id, event.previousStatus)
            }
        }
        statusBefore?.let { status ->
            mediaDao.updateSessionStatus(
                sessionId = event.sessionId,
                status = status,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            // Undoing a start also undoes the start date it stamped. A start date that disagrees with
            // the transition's day was set by the user and stays.
            if (status == TrackingStatus.Planned.name && event.status == TrackingStatus.InProgress.name &&
                event.hasKnownDate && session.startedAtEpochDay == event.resolvedOccurredOn().toEpochDay()
            ) {
                mediaDao.updateSessionStartedDate(
                    sessionId = event.sessionId,
                    startedAtEpochDay = null,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            }
            val terminalStatuses = setOf(TrackingStatus.Completed.name, TrackingStatus.Dropped.name)
            if (status in terminalStatuses) {
                val terminalDate = sessionEvents.take(eventIndex)
                    .lastOrNull { it.status == status }
                    ?.resolvedOccurredOn()
                mediaDao.updateSessionFinishedDate(
                    sessionId = event.sessionId,
                    finishedAtEpochDay = terminalDate?.toEpochDay(),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            } else if (event.status in terminalStatuses) {
                mediaDao.updateSessionFinishedDate(
                    sessionId = event.sessionId,
                    finishedAtEpochDay = null,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            }
        }

        val sessionAfterDeletion = mediaDao.getTrackingSession(event.sessionId)
            ?: return@withTransaction null
        val statusEventsAfterDeletion = mediaDao.getSessionStatusEventsForSession(event.sessionId)
        notifyMalIfPayloadChanged(event.mediaItemId, malPayloadBefore)
        DeletionRecovery.SessionStatusEvents(
            events = listOf(event),
            statusEventsBeforeDeletion = sessionEvents,
            statusEventsAfterDeletion = statusEventsAfterDeletion,
            sessionBeforeDeletion = session,
            sessionAfterDeletion = sessionAfterDeletion,
        )
    }

    override suspend fun updateSessionStatusEventDate(eventId: Long, occurredOn: LocalDate?): Boolean =
        database.withTransaction {
        val event = mediaDao.getSessionStatusEvent(eventId) ?: return@withTransaction false
        val malPayloadBefore = currentMalSyncPayload(event.mediaItemId)
        val orderedEvents = mediaDao.getSessionStatusEventsForSession(event.sessionId)
        val eventIndex = orderedEvents.indexOfFirst { it.id == eventId }
        if (eventIndex < 0) return@withTransaction false
        val session = mediaDao.getTrackingSession(event.sessionId) ?: return@withTransaction false
        val opensRun = session.startedAtEpochDay != null && orderedEvents.currentOpening()?.id == eventId
        if (occurredOn != null) {
            // Only claimed days constrain: an undated neighbour's day is a placeholder.
            val previousDay = orderedEvents.take(eventIndex).lastOrNull { it.hasKnownDate }?.resolvedOccurredOn()
            val nextDay = orderedEvents.drop(eventIndex + 1).firstOrNull { it.hasKnownDate }?.resolvedOccurredOn()
            // The opening transition is the start date, so it is not held behind it.
            val startedAt = session.startedAtEpochDay?.let(LocalDate::ofEpochDay).takeUnless { opensRun }
            if (previousDay?.isAfter(occurredOn) == true || nextDay?.isBefore(occurredOn) == true ||
                startedAt?.isAfter(occurredOn) == true
            ) {
                return@withTransaction false
            }
            mediaDao.updateSessionStatusEventDate(eventId = eventId, occurredOnEpochDay = occurredOn.toEpochDay())
        } else {
            mediaDao.clearSessionStatusEventDate(eventId)
        }
        if (opensRun && occurredOn != null) {
            mediaDao.updateSessionStartedDate(
                sessionId = event.sessionId,
                startedAtEpochDay = occurredOn.toEpochDay(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
        val isCurrentTerminalEvent = event.status == session.status &&
            event.status in setOf(TrackingStatus.Completed.name, TrackingStatus.Dropped.name) &&
            orderedEvents.lastOrNull()?.id == eventId
        if (isCurrentTerminalEvent) {
            // The snapshot's finish date mirrors the ending's day, including not knowing it.
            mediaDao.updateSessionFinishedDate(
                sessionId = event.sessionId,
                finishedAtEpochDay = occurredOn?.toEpochDay(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
        notifyMalIfPayloadChanged(event.mediaItemId, malPayloadBefore)
        true
    }

    /** The transition that opened the current run; the derivation pairs the same one with the start. */
    private fun List<SessionStatusEventEntity>.currentOpening(): SessionStatusEventEntity? =
        withIndex().lastOrNull { (index, event) ->
            val from = event.previousStatus ?: getOrNull(index - 1)?.status
            event.status == TrackingStatus.InProgress.name &&
                (from == TrackingStatus.Planned.name || (from == null && index == 0))
        }?.value

    private fun SessionStatusEventEntity.resolvedOccurredOn(): LocalDate =
        occurredOnEpochDay?.let(LocalDate::ofEpochDay)
            ?: Instant.ofEpochMilli(createdAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

    override suspend fun deleteProgressUpdate(progressUpdateId: Long): DeletionRecovery? = database.withTransaction {
        val update = mediaDao.getProgressUpdate(progressUpdateId) ?: return@withTransaction null
        val sessionBeforeDeletion = mediaDao.getTrackingSession(update.sessionId) ?: return@withTransaction null
        val malPayloadBefore = currentMalSyncPayload(update.mediaItemId)
        val progressAfterDeletion = sessionBeforeDeletion.baselineProgress +
            mediaDao.getProgressUpdatesForSession(update.sessionId)
                .filterNot { it.id == progressUpdateId }
                .sumOf { it.amount }
        val updatedAtEpochMillis = System.currentTimeMillis()
        mediaDao.deleteProgressUpdate(progressUpdateId)
        mediaDao.updateSessionProgress(
            sessionId = update.sessionId,
            progressCurrent = progressAfterDeletion,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        val reopeningId = reopenIfBelowTotal(update.sessionId, updatedAtEpochMillis)
        val sessionAfterDeletion = mediaDao.getTrackingSession(update.sessionId) ?: return@withTransaction null
        notifyMalIfPayloadChanged(update.mediaItemId, malPayloadBefore)
        DeletionRecovery.ProgressUpdate(
            update = update,
            sessionBeforeDeletion = sessionBeforeDeletion,
            sessionAfterDeletion = sessionAfterDeletion,
            reopeningEventId = reopeningId,
        )
    }

    override suspend fun deleteMediaItem(mediaItemId: Long): DeletionRecovery? = database.withTransaction {
        val item = mediaDao.getMediaItem(mediaItemId) ?: return@withTransaction null
        val collection = item.collectionId?.let { mediaDao.getMediaCollection(it) }
        val credits = mediaDao.getMediaCreditsForItem(mediaItemId)
        val sessions = mediaDao.getTrackingSessions(mediaItemId)
        val sessionIds = sessions.map { it.id }.toSet()
        val progressUpdates = mediaDao.getProgressUpdates()
            .filter { it.mediaItemId == mediaItemId && it.sessionId in sessionIds }
        val statusEvents = mediaDao.getSessionStatusEvents()
            .filter { it.mediaItemId == mediaItemId && it.sessionId in sessionIds }
        val externalRatings = mediaDao.getExternalRatingsForItem(mediaItemId)

        mediaDao.deleteMediaItem(mediaItemId)
        mediaDao.deleteEmptyMediaCollections()
        DeletionRecovery.MediaItem(
            item = item,
            collection = collection,
            credits = credits,
            sessions = sessions,
            progressUpdates = progressUpdates,
            statusEvents = statusEvents,
            externalRatings = externalRatings,
        )
    }

    override suspend fun restoreDeletion(recovery: DeletionRecovery): Boolean = database.withTransaction {
        val mediaItemId = when (recovery) {
            is DeletionRecovery.MediaItem -> recovery.item.id
            is DeletionRecovery.PastSession -> recovery.session.mediaItemId
            is DeletionRecovery.ProgressUpdate -> recovery.update.mediaItemId
            is DeletionRecovery.SessionStatusEvents -> recovery.sessionBeforeDeletion.mediaItemId
            is DeletionRecovery.SessionMutation -> recovery.before.session.mediaItemId
        }
        val malPayloadBefore = currentMalSyncPayload(mediaItemId)
        val restored = when (recovery) {
            is DeletionRecovery.MediaItem -> restoreMediaItemDeletion(recovery)
            is DeletionRecovery.PastSession -> restorePastSessionDeletion(recovery)
            is DeletionRecovery.ProgressUpdate -> restoreProgressUpdateDeletion(recovery)
            is DeletionRecovery.SessionStatusEvents -> restoreSessionStatusEvents(recovery)
            is DeletionRecovery.SessionMutation -> restoreSessionMutation(recovery)
        }
        if (restored) {
            notifyMalIfPayloadChanged(mediaItemId, malPayloadBefore)
        }
        restored
    }

    private suspend fun currentMalSyncPayload(mediaItemId: Long): MalSyncPayload? {
        val item = mediaDao.getMediaItem(mediaItemId) ?: return null
        return buildMalSyncPayload(item, mediaDao.getTrackingSessions(mediaItemId))
    }

    private suspend fun notifyMalIfPayloadChanged(
        mediaItemId: Long,
        before: MalSyncPayload?,
    ) {
        if (before != currentMalSyncPayload(mediaItemId)) {
            onMalRelevantChange(mediaItemId)
        }
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
        if (recovery.statusEvents.any {
                it.mediaItemId != item.id || it.sessionId !in sessionIds
            }
        ) return false
        if (recovery.externalRatings.any { it.mediaItemId != item.id }) return false

        val existingCreditIds = mediaDao.getMediaCredits().map { it.id }.toSet()
        if (recovery.credits.any { it.id != 0L && it.id in existingCreditIds }) return false
        if (recovery.sessions.any { mediaDao.getTrackingSession(it.id) != null }) return false
        if (recovery.progressUpdates.any { mediaDao.getProgressUpdate(it.id) != null }) return false
        if (recovery.statusEvents.any { mediaDao.getSessionStatusEvent(it.id) != null }) return false
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
        recovery.statusEvents.forEach { mediaDao.insertSessionStatusEvent(it) }
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
        if (recovery.statusEvents.any {
                it.mediaItemId != session.mediaItemId || it.sessionId != session.id
            }
        ) return false
        if (recovery.progressUpdates.any { mediaDao.getProgressUpdate(it.id) != null }) return false
        if (recovery.statusEvents.any { mediaDao.getSessionStatusEvent(it.id) != null }) return false
        if (mediaDao.getTrackingSessions(session.mediaItemId)
                .any { it.sessionNumber == session.sessionNumber }
        ) return false

        mediaDao.insertTrackingSession(session)
        recovery.progressUpdates.forEach { mediaDao.insertProgressUpdate(it) }
        recovery.statusEvents.forEach { mediaDao.insertSessionStatusEvent(it) }
        return true
    }

    /**
     * Puts a deleted pause back, both halves together.
     *
     * Refuses if the session has gone, or if any of the rows is already present — restoring half a
     * pair on top of a surviving half would produce a sequence that never happened.
     */
    private suspend fun restoreSessionStatusEvents(
        recovery: DeletionRecovery.SessionStatusEvents,
    ): Boolean {
        if (recovery.events.isEmpty()) return false
        val sessionId = recovery.sessionAfterDeletion.id
        if (recovery.events.any { it.sessionId != sessionId }) return false
        val current = mediaDao.getTrackingSession(recovery.sessionAfterDeletion.id) ?: return false
        if (current != recovery.sessionAfterDeletion) return false
        if (mediaDao.getSessionStatusEventsForSession(sessionId) != recovery.statusEventsAfterDeletion) {
            return false
        }
        mediaDao.deleteSessionStatusEventsForSession(sessionId)
        if (mediaDao.updateTrackingSession(recovery.sessionBeforeDeletion) != 1) return false
        recovery.statusEventsBeforeDeletion.forEach { mediaDao.insertSessionStatusEvent(it) }
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

        recovery.reopeningEventId?.let { reopeningId ->
            // Only while the reopening is still the newest word on the session's state.
            if (mediaDao.getSessionStatusEventsForSession(update.sessionId).lastOrNull()?.id != reopeningId) return false
            mediaDao.deleteSessionStatusEvent(reopeningId)
        }
        if (mediaDao.updateTrackingSession(recovery.sessionBeforeDeletion) != 1) return false
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
        customSourceName: String?,
    ) {
        val storedSource = source.storedName(customSourceName) ?: return
        val validScore = score.takeIf { it in 0.0..maxScore } ?: return
        val validMax = maxScore.takeIf { it > 0.0 } ?: return
        val existing = mediaDao.getExternalRatingsForItem(mediaItemId)
            .firstOrNull { it.source.equals(storedSource, ignoreCase = true) }
        if (existing != null) {
            updateExternalRating(existing.id, source, validScore, validMax, voteCount, makePrimary, customSourceName)
            return
        }
        val ratingId = mediaDao.insertExternalRating(
            ExternalRatingEntity(
                mediaItemId = mediaItemId,
                source = storedSource,
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
        customSourceName: String?,
    ) {
        val storedSource = source.storedName(customSourceName) ?: return
        val existing = mediaDao.getExternalRating(externalRatingId) ?: return
        if (mediaDao.getExternalRatingsForItem(existing.mediaItemId).any { rating ->
                rating.id != externalRatingId && rating.source.equals(storedSource, ignoreCase = true)
            }
        ) return
        val validScore = score.takeIf { it in 0.0..maxScore } ?: return
        val validMax = maxScore.takeIf { it > 0.0 } ?: return
        val validVoteCount = voteCount?.takeIf { it >= 0 }
        mediaDao.updateExternalRating(
            externalRatingId = externalRatingId,
            source = storedSource,
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
        isOwned: Boolean,
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
            isOwned = isOwned,
        )
        mediaDao.deleteEmptyMediaCollections()
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
        credits: List<MediaCredit>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
        steamAppId: String?,
    ) {
        val validTitle = title.trim().takeIf { it.isNotBlank() } ?: return
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return
        val validTotal = progressTotal
            ?.takeUnless { currentItem.type == MediaType.Game.name }
            ?.coerceAtLeast(0)
        val validOriginalTitle = originalTitle?.trim()?.takeIf { it.isNotBlank() }
        val validReleaseYear = releaseYear?.coerceAtLeast(0)
        val validLanguage = ItemLanguage.normalize(language).takeUnless { currentItem.type == MediaType.Game.name }
        val validGenres = genres.cleanMetadataList()
        val validCreators = creators.cleanMetadataList()
        val currentCredits = mediaDao.getMediaCreditsForItem(mediaItemId).map { it.toDomain() }
        val validCredits = credits.cleanCredits()
        val validCoverUrl = coverUrl?.trim()?.takeIf { it.isNotBlank() }
        val validSynopsis = normalizeSynopsis(synopsis)
        val validSourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() }
        val validSteamAppId = if (currentItem.type == MediaType.Game.name) {
            normalizeSteamAppId(steamAppId)
        } else {
            null
        }
        val updatedPopularityJson = if (currentItem.type == MediaType.Game.name) {
            metadataJsonWithSteamAppId(currentItem.popularityJson, validSteamAppId)
        } else {
            currentItem.popularityJson
        }
        val updatedOverrideFields = currentItem.metadataOverrideFields() + buildSet {
            if (currentItem.title != validTitle) add(MetadataRefreshField.Title)
            if (currentItem.originalTitle != validOriginalTitle) add(MetadataRefreshField.OriginalTitle)
            if (currentItem.releaseYear != validReleaseYear) add(MetadataRefreshField.ReleaseYear)
            if (currentItem.language != validLanguage) add(MetadataRefreshField.Language)
            if (currentItem.progressTotal != validTotal) add(MetadataRefreshField.ProgressTotal)
            if (currentItem.genresJson.toStringList() != validGenres) add(MetadataRefreshField.Genres)
            if (currentItem.creatorsJson.toStringList() != validCreators) add(MetadataRefreshField.Creators)
            if (currentCredits.creditFingerprint() != validCredits.creditFingerprint()) {
                add(MetadataRefreshField.Credits)
            }
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
            popularityJson = updatedPopularityJson,
        )
        mediaDao.updateMetadataOverrideFields(
            mediaItemId = mediaItemId,
            metadataOverrideFieldsCsv = updatedOverrideFields.toMetadataOverrideFieldsCsv(),
        )

        if (currentCredits.creditFingerprint() != validCredits.creditFingerprint()) {
            mediaDao.deleteMediaCreditsForItem(mediaItemId)
            validCredits
                .map { credit -> credit.copy(id = 0, mediaItemId = mediaItemId).toEntity() }
                .takeIf { it.isNotEmpty() }
                ?.let { editedCredits -> mediaDao.insertMediaCredits(editedCredits) }
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
                    scoreDescriptor = rating.scoreDescriptor,
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
            language = ItemLanguage.normalize(refreshed.language).takeUnless { mediaType == MediaType.Game },
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
    ): Boolean = applyMediaItemMetadataRefresh(preview, selectedFields, MediaWriteOrigin.UserAction)

    override suspend fun applyImportedMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Boolean = applyMediaItemMetadataRefresh(preview, selectedFields, MediaWriteOrigin.ProviderImport)

    override suspend fun applyAutomaticMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
    ): Boolean = applyMediaItemMetadataRefresh(
        preview = preview,
        requestedFields = preview.changes
            .filterNot { change -> change.isLocallyOverridden }
            .map { change -> change.field }
            .toSet(),
        origin = MediaWriteOrigin.AutomaticMetadataRefresh,
    )

    private suspend fun applyMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        requestedFields: Set<MetadataRefreshField>,
        origin: MediaWriteOrigin,
    ): Boolean {
        val currentItem = mediaDao.getMediaItem(preview.mediaItemId) ?: return false
        val localOverrides = currentItem.metadataOverrideFields()
        // A bulk refresh may have fetched a value just before the user edits the same field.
        // Re-read the overrides here so that race can never replace the user's edit.
        val selectedFields = if (origin == MediaWriteOrigin.AutomaticMetadataRefresh) {
            requestedFields - localOverrides
        } else {
            requestedFields
        }
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
                    scoreDescriptor = rating.scoreDescriptor,
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
            malId = refreshed.malId ?: currentItem.toResolvedMalId(),
            metadataLastFetchedAtEpochMillis = System.currentTimeMillis(),
        )

        if (
            origin == MediaWriteOrigin.ProviderImport &&
            (mediaType == MediaType.Book || mediaType == MediaType.TvShow) &&
            MetadataRefreshField.ProgressTotal in selectedFields &&
            selectedTotal != null && selectedTotal > 0
        ) {
            // StoryGraph has no page-count column, while IMDb supplies runtime minutes rather than
            // episode counts for TV. Completed imports therefore start at 0/unknown. Once exact
            // metadata supplies the correct unit total, treat it as the imported baseline rather than
            // new activity. The DAO guard leaves real progress and history alone.
            mediaDao.reconcileCompletedImportedProgress(preview.mediaItemId, selectedTotal)
        }

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
            // A metadata refresh is authoritative for every source it returns. Remove a matching
            // manual value as well so the UI cannot show two slightly different ratings under the
            // same provider name.
            val refreshedRatings = refreshed.externalRatings
                .filter { it.score > 0.0 && it.maxScore > 0.0 }
                .distinctBy { it.source }
            mediaDao.deleteProviderExternalRatingsForItem(preview.mediaItemId)
            refreshedRatings
                .map { it.source.name }
                .takeIf { it.isNotEmpty() }
                ?.let { sources -> mediaDao.deleteExternalRatingsForSources(preview.mediaItemId, sources) }
            refreshedRatings.forEach { rating ->
                    mediaDao.insertExternalRating(
                        ExternalRatingEntity(
                            mediaItemId = preview.mediaItemId,
                            source = rating.source.name,
                            score = rating.score,
                            maxScore = rating.maxScore,
                            voteCount = rating.voteCount,
                            scoreDescriptor = rating.scoreDescriptor,
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
        val preview = previewMediaItemMetadataLink(mediaItemId, suggestion, metadataRepository) ?: return false
        return applyMediaItemMetadataRefresh(
            preview = preview,
            selectedFields = preview.changes.map { change -> change.field }.toSet(),
        )
    }

    override suspend fun previewMediaItemMetadataLink(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
        metadataRepository: MetadataRepository,
    ): MetadataRefreshPreview? {
        val currentItem = mediaDao.getMediaItem(mediaItemId) ?: return null
        val currentMediaType = runCatching { MediaType.valueOf(currentItem.type) }.getOrNull() ?: return null
        if (currentMediaType != suggestion.mediaType) {
            return null
        }

        val existingRatings = mediaDao.getExternalRatingsForItem(mediaItemId)
            .filter { it.origin == "Provider" }
            .map { it.toDomain() }
            .map { rating ->
                MetadataExternalRatingSuggestion(
                    source = rating.source,
                    score = rating.score,
                    maxScore = rating.maxScore,
                    voteCount = rating.voteCount,
                    scoreDescriptor = rating.scoreDescriptor,
                )
            }
        val existingCredits = mediaDao.getMediaCreditsForItem(mediaItemId).map { it.toDomain() }
        val suggestionWithSteamId = if (currentMediaType == MediaType.Game) {
            suggestion.copy(
                popularityJson = metadataJsonWithSteamAppId(
                    suggestion.popularityJson,
                    steamAppIdFromMetadataJson(currentItem.popularityJson),
                ),
            )
        } else {
            suggestion
        }
        val linked = metadataRepository.getSuggestionDetails(suggestionWithSteamId)
            .withPreservedMyAnimeListId(currentItem)
        val linkedTotal = linked.progressTotal
            ?.takeUnless { currentMediaType == MediaType.Game }
            ?.coerceAtLeast(0)
        val linkedRatings = (linked.externalRatings + existingRatings)
            .distinctBy { it.source }
        val normalizedLinked = linked.copy(
            title = linked.title.trim().takeIf { it.isNotBlank() } ?: currentItem.title,
            originalTitle = linked.originalTitle?.trim()?.takeIf { it.isNotBlank() },
            language = ItemLanguage.normalize(linked.language).takeUnless { currentMediaType == MediaType.Game },
            genres = linked.genres.cleanMetadataList(),
            creators = linked.creators.cleanMetadataList(),
            progressTotal = linkedTotal,
            coverUrl = linked.coverUrl?.trim()?.takeIf { it.isNotBlank() },
            synopsis = normalizeSynopsis(linked.synopsis),
            sourceUrl = linked.sourceUrl?.trim()?.takeIf { it.isNotBlank() },
            externalRatings = linkedRatings,
        )

        return MetadataRefreshPreview(
            mediaItemId = mediaItemId,
            refreshed = normalizedLinked,
            changes = buildMetadataRefreshChanges(
                currentItem = currentItem,
                currentCredits = existingCredits,
                currentRatings = existingRatings,
                refreshed = normalizedLinked,
                localOverrides = currentItem.metadataOverrideFields(),
            ),
        )
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
        currentComparisonValue = currentCredits.creditFingerprint(),
        newComparisonValue = refreshed.credits.creditFingerprint(),
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
    currentComparisonValue: Any? = currentValue,
    newComparisonValue: Any? = newValue,
) {
    val currentDisplay = currentValue.toMetadataDisplayValue()
    val newDisplay = newValue.toMetadataDisplayValue()
    val currentComparison = currentComparisonValue.toMetadataComparisonValue()
    val newComparison = newComparisonValue.toMetadataComparisonValue()
    if (newDisplay == "Empty" || currentComparison == newComparison) return

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

private fun Any?.toMetadataComparisonValue(): String = when (this) {
    null -> ""
    is String -> trim()
    is Int -> toString()
    is Double -> toString()
    is List<*> -> joinToString("|") { value -> value?.toString()?.trim().orEmpty() }
    else -> toString().trim()
}

/** Includes the portrait URLs so a metadata refresh can offer image-only credit updates. */
private fun List<MediaCredit>.creditFingerprint(): String {
    return filter { it.personName.isNotBlank() }
        .joinToString("|") { credit ->
            listOf(
                credit.personName.trim(),
                credit.roleType.name,
                credit.characterName?.trim().orEmpty(),
                credit.personImageUrl?.trim().orEmpty(),
                credit.characterImageUrl?.trim().orEmpty(),
            ).joinToString("~")
        }
}

internal fun List<MetadataExternalRatingSuggestion>.ratingSummary(): String {
    return filter { it.score > 0.0 && it.maxScore > 0.0 }
        .joinToString(", ") { rating ->
            buildString {
                append("${rating.source.name} ${rating.score.cleanNumber()}/${rating.maxScore.cleanNumber()}")
                rating.scoreDescriptor
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { descriptor -> append(" · $descriptor") }
            }
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
        MetadataSource.Rawg -> ExternalRatingSource.Steam
        MetadataSource.Steam -> ExternalRatingSource.Steam
        MetadataSource.Imdb -> ExternalRatingSource.Imdb
        MetadataSource.StoryGraph -> ExternalRatingSource.StoryGraph
    }.name
}

internal fun MediaType.preferredPrimaryExternalRating(
    ratings: List<MetadataExternalRatingSuggestion>,
    fallback: MetadataRatingSuggestion?,
): MetadataRatingSuggestion? {
    val preferredRating = when (this) {
        MediaType.Book -> ratings.firstOrNull {
            it.source == ExternalRatingSource.Goodreads && it.score > 0.0 && it.maxScore > 0.0
        }
        MediaType.Game -> ratings.firstOrNull {
            it.source == ExternalRatingSource.Steam && it.score > 0.0 && it.maxScore > 0.0
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
    val preservedMalId = this.malId
        ?: popularityJson.myAnimeListIdFromJson()?.toIntOrNull()
        ?: currentItem.toResolvedMalId()
        ?: return this
    val mergedPopularityJson = runCatching {
        JSONObject(popularityJson ?: "{}")
            .put("malId", preservedMalId)
            .toString()
    }.getOrDefault(popularityJson)
    return copy(malId = preservedMalId, popularityJson = mergedPopularityJson)
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

private fun MediaItemEntity.toResolvedMalId(): Int? = resolveMyAnimeListId(
    explicitMalId = malId,
    metadataSource = metadataSource,
    metadataExternalId = metadataExternalId,
    popularityJson = popularityJson,
    sourceUrl = sourceUrl,
)

private fun String?.myAnimeListIdFromJson(): String? {
    return runCatching {
        JSONObject(this ?: "{}").optInt("malId", 0).takeIf { it > 0 }?.toString()
    }.getOrNull()
}

/**
 * Moves a session's progress to [target], keeping `progressCurrent` equal to the baseline plus the
 * sum of its entries.
 *
 * Advancing logs an entry for the difference, which is what the user just did. Going backwards is a
 * correction rather than an event, so the surplus comes off the most recent entries first and only
 * lowers the baseline once there are no entries left to trim: a correction supersedes what came
 * immediately before it, and leaves no row of its own.
 *
 * [loggedAt] is the day the progress belongs to, which is today for an ordinary save but the finish
 * date when a session is being ended on a past one.
 */
private suspend fun MediaDao.applyProgressTarget(
    session: TrackingSessionEntity,
    target: Int,
    loggedAt: LocalDate?,
    updatedAtEpochMillis: Long,
) {
    val entries = getProgressUpdatesForSession(session.id)
    val difference = target - (session.baselineProgress + entries.sumOf { it.amount })

    when {
        difference > 0 -> insertProgressUpdate(
            ProgressUpdateEntity(
                mediaItemId = session.mediaItemId,
                sessionId = session.id,
                amount = difference,
                // An unknown end date needs an ordering placeholder, but it must never be read as
                // consumption that happened today.
                loggedAtEpochDay = (loggedAt ?: LocalDate.now()).toEpochDay(),
                hasKnownDate = loggedAt != null,
                createdAtEpochMillis = updatedAtEpochMillis,
            ),
        )

        difference < 0 -> {
            var surplus = -difference
            val newestFirst = entries.sortedWith(
                compareByDescending<ProgressUpdateEntity> { it.loggedAtEpochDay }
                    .thenByDescending { it.createdAtEpochMillis }
                    .thenByDescending { it.id },
            )
            for (entry in newestFirst) {
                if (surplus <= 0) break
                if (surplus >= entry.amount) {
                    surplus -= entry.amount
                    deleteProgressUpdate(entry.id)
                } else {
                    updateProgressUpdate(
                        progressUpdateId = entry.id,
                        amount = entry.amount - surplus,
                        loggedAtEpochDay = entry.loggedAtEpochDay,
                        hasKnownDate = entry.hasKnownDate,
                        coversPeriod = entry.coversPeriod,
                    )
                    surplus = 0
                }
            }
            if (surplus > 0) {
                updateSessionBaseline(
                    sessionId = session.id,
                    baselineProgress = (session.baselineProgress - surplus).coerceAtLeast(0),
                )
            }
        }
    }

    updateSessionProgress(
        sessionId = session.id,
        progressCurrent = target,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

private fun MediaItemEntity.importDuplicateKey(): String {
    val stableImdbId = imdbId?.canonicalImdbIdentifier()
        ?: metadataExternalId
            ?.takeIf { metadataSource == MetadataSource.Imdb.name }
            ?.canonicalImdbIdentifier()
    return if (stableImdbId != null) {
        "imdb:$stableImdbId"
    } else {
        "${type.lowercase()}:${title.normalizedImportTitle()}:${releaseYear ?: ""}"
    }
}

internal fun ImdbCsvItem.importDuplicateKey(): String {
    val canonicalImdbId = imdbId?.canonicalImdbIdentifier()
    return if (canonicalImdbId != null) {
        "imdb:$canonicalImdbId"
    } else {
        "${type?.name?.lowercase().orEmpty()}:${title.normalizedImportTitle()}:${releaseYear ?: ""}"
    }
}

private fun MediaItemEntity.storyGraphDuplicateKey(): String {
    val stableStoryGraphId = storyGraphId?.takeIf(String::isNotBlank)
        ?: metadataExternalId?.takeIf {
            metadataSource == MetadataSource.StoryGraph.name && it.isNotBlank()
        }
    return if (stableStoryGraphId != null) {
        "storygraph:${stableStoryGraphId.canonicalStoryGraphIdentifier()}"
    } else {
        "book:${title.normalizedImportTitle()}:${creatorsJson.toStringList().normalizedImportPeople()}"
    }
}

internal fun StoryGraphCsvItem.storyGraphDuplicateKey(): String {
    return if (!isbnOrUid.isNullOrBlank()) {
        "storygraph:${isbnOrUid.canonicalStoryGraphIdentifier()}"
    } else {
        "book:${title.normalizedImportTitle()}:${authors.normalizedImportPeople()}"
    }
}

private fun MediaItemEntity.myAnimeListDuplicateKey(): String {
    val malId = toResolvedMalId()
    return if (malId != null) {
        "mal:$malId"
    } else {
        "anime:${title.normalizedImportTitle()}:${progressTotal ?: ""}"
    }
}

internal fun MyAnimeListImportItem.myAnimeListDuplicateKey(): String {
    return if (malId != null) {
        "mal:$malId"
    } else {
        "anime:${title.normalizedImportTitle()}:${episodeTotal ?: ""}"
    }
}

private fun String.canonicalStoryGraphIdentifier(): String {
    val trimmed = trim()
    val isbn = trimmed
        .takeIf { value -> value.all { it.isDigit() || it == 'X' || it == 'x' || it == '-' || it.isWhitespace() } }
        ?.normalizeIsbn()
        ?.takeIf(String::isValidIsbn)
    return isbn ?: trimmed.lowercase(Locale.ROOT)
}

private fun String.canonicalImdbIdentifier(): String? =
    trim().lowercase(Locale.ROOT).takeIf(CanonicalImdbIdPattern::matches)

private fun String.canonicalImdbSourceKey(): String {
    if (!startsWith("imdb:")) return this
    val canonicalId = substringAfter(':').canonicalImdbIdentifier() ?: return this
    return "imdb:$canonicalId"
}

private fun String.canonicalStoryGraphSourceKey(): String {
    if (!startsWith("storygraph:")) return this
    return "storygraph:${substringAfter(':').canonicalStoryGraphIdentifier()}"
}

private fun Iterable<String>.normalizedImportPeople(): String =
    map(String::normalizedImportTitle)
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
        .joinToString("|")

private fun String.normalizedImportTitle(): String =
    Normalizer.normalize(this, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

private val CanonicalImdbIdPattern = Regex("tt[0-9]{7,10}")

/**
 * The backup format version.
 *
 * Bumped to 8 when progress moved from cumulative totals to increments, and to 9 when the status
 * log joined the backup at all — before that, restoring a backup silently dropped every pause and
 * resume the library had ever recorded.
 *
 * Older backups are converted on the way in rather than refused. Refusing them would mean a backup
 * taken before an upgrade could only be restored by an app version that no longer exists, which
 * turns the one safety net into a dead end exactly when it is needed.
 */
private const val BackupSchemaVersion = 13

/** The first version whose progress values are increments rather than cumulative totals. */
private const val FirstIncrementBackupSchemaVersion = 8

/** Ratings are half points from this backup version on; older backups carry whole points. */
internal const val FirstHalfPointBackupSchemaVersion = 13

private fun parseBackupRoot(json: String): JSONObject {
    val root = JSONObject(json)
    val schemaVersion = root.optInt("schemaVersion", -1)
    if (schemaVersion !in 1..BackupSchemaVersion) {
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
    val sessionStatusEvents: List<SessionStatusEventEntity>,
    val externalRatings: List<ExternalRatingEntity>,
    val objectives: List<ObjectiveEntity>,
)

private fun parseBackupData(json: String): ParsedBackup {
    val root = parseBackupRoot(json)
    val schemaVersion = root.getInt("schemaVersion")
    val rawSessions = root.getJSONArray("trackingSessions")
        .mapObjects { it.toTrackingSessionEntity(schemaVersion) }

    // Backups older than the increment format carry cumulative values and the catch-up flag, so
    // they go through the same conversion the database migration runs. Anything newer is already
    // in the right shape.
    val (sessions, progressUpdates) = if (schemaVersion < FirstIncrementBackupSchemaVersion) {
        convertLegacyProgress(
            sessions = rawSessions,
            legacyRows = root.optJSONArray("progressUpdates").orEmptyArray()
                .mapObjects { it.toLegacyBackupRow() },
        )
    } else {
        normalizeIncrementBackup(
            sessions = rawSessions,
            updates = root.optJSONArray("progressUpdates").orEmptyArray()
                .mapObjects { it.toProgressUpdateEntity() },
        )
    }

    return ParsedBackup(
        schemaVersion = root.getInt("schemaVersion"),
        exportedAtEpochMillis = root.optNullableLong("exportedAtEpochMillis"),
        collections = root.optJSONArray("collections").orEmptyArray()
            .mapObjects { it.toMediaCollectionEntity() },
        mediaItems = root.getJSONArray("mediaItems")
            .mapObjects { it.toMediaItemEntity() },
        mediaCredits = root.optJSONArray("mediaCredits").orEmptyArray()
            .mapObjects { it.toMediaCreditEntity() },
        sessions = sessions,
        progressUpdates = progressUpdates,
        // Absent from every backup written before version 9, which is why restoring one used to
        // silently drop the pause log.
        sessionStatusEvents = root.optJSONArray("sessionStatusEvents").orEmptyArray()
            .mapObjects { it.toSessionStatusEventEntity() },
        externalRatings = root.optJSONArray("externalRatings").orEmptyArray()
            .mapObjects { it.toExternalRatingEntity() },
        objectives = root.optJSONArray("objectives").orEmptyArray()
            .mapObjects { it.toObjectiveEntity() },
    ).repairUndatedTerminalHistory().also { it.validate() }
}

/** Applies the same correction as migration 36→37 to backups made by affected app versions. */
private fun ParsedBackup.repairUndatedTerminalHistory(): ParsedBackup {
    val sessionsById = sessions.associateBy { it.id }
    val latestBySession = sessionStatusEvents
        .groupBy { it.sessionId }
        .mapValues { (_, events) ->
            events.maxWithOrNull(
                compareBy<SessionStatusEventEntity> { it.createdAtEpochMillis }.thenBy { it.id },
            )
        }
    val unknownEventIds = latestBySession.values.filterNotNull().mapNotNull { event ->
        val session = sessionsById[event.sessionId]
        event.takeIf {
            session != null &&
                event.status in setOf(TrackingStatus.Completed.name, TrackingStatus.Dropped.name) &&
                session.status == event.status &&
                session.finishedAtEpochDay == null
        }?.id
    }.toSet()
    if (unknownEventIds.isEmpty()) return this

    val unknownEvents = sessionStatusEvents.filter { it.id in unknownEventIds }
    return copy(
        sessionStatusEvents = sessionStatusEvents.map { event ->
            event.copy(hasKnownDate = event.hasKnownDate && event.id !in unknownEventIds)
        },
        progressUpdates = progressUpdates.map { update ->
            update.copy(
                hasKnownDate = update.hasKnownDate && unknownEvents.none { event ->
                    event.sessionId == update.sessionId &&
                        event.createdAtEpochMillis == update.createdAtEpochMillis
                },
            )
        },
    )
}

/** Repairs pre-v10 increment backups without discarding canonical activity rows. */
internal fun normalizeIncrementBackup(
    sessions: List<TrackingSessionEntity>,
    updates: List<ProgressUpdateEntity>,
): Pair<List<TrackingSessionEntity>, List<ProgressUpdateEntity>> {
    val normalizedSessions = sessions.map { session ->
        val entryTotal = updates.filter { it.sessionId == session.id }.sumOf { it.amount }
        var baseline = session.baselineProgress.coerceAtLeast(0)
        val derived = baseline + entryTotal
        if (derived < session.progressCurrent) baseline += session.progressCurrent - derived
        session.copy(
            baselineProgress = baseline,
            progressCurrent = maxOf(session.progressCurrent, baseline + entryTotal),
        )
    }
    return normalizedSessions to updates
}

private fun ParsedBackup.validate() {
    collections.requireUniquePositiveIds("collections") { it.id }
    mediaItems.requireUniquePositiveIds("media items") { it.id }
    mediaCredits.requireUniquePositiveIds("media credits") { it.id }
    sessions.requireUniquePositiveIds("tracking sessions") { it.id }
    progressUpdates.requireUniquePositiveIds("progress updates") { it.id }
    sessionStatusEvents.requireUniquePositiveIds("session status events") { it.id }
    externalRatings.requireUniquePositiveIds("external ratings") { it.id }
    objectives.requireUniquePositiveIds("objectives") { it.id }

    val collectionIds = collections.map { it.id }.toSet()
    val mediaItemIds = mediaItems.map { it.id }.toSet()
    val mediaTypesById = mediaItems.associate { it.id to it.type }
    val sessionIds = sessions.map { it.id }.toSet()
    val sessionsById = sessions.associateBy { it.id }

    collections.forEach { collection ->
        require(collection.name.isNotBlank()) { "Collection names cannot be blank" }
    }

    mediaItems.forEach { item ->
        require(item.title.isNotBlank()) { "Media item titles cannot be blank" }
        requireEnum<MediaType>(item.type) { "Unknown media type: ${item.type}" }
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
        require(session.baselineProgress >= 0) { "Session baselines cannot be negative" }
        requireEnum<TrackingStatus>(session.status) { "Unknown tracking status: ${session.status}" }
        session.platformType?.let { platformType ->
            requireEnum<ConsumptionPlatformType>(platformType) { "Unknown platform type: $platformType" }
        }
        session.ratingHalfPoints?.let { halfPoints ->
            require(halfPoints in RatingHalfPoints.Min..RatingHalfPoints.Max) {
                "Session ratings must be between 0,5 and 10"
            }
        }
    }

    sessionStatusEvents.forEach { event ->
        val session = sessionsById[event.sessionId]
            ?: error("Status event references a missing session")
        require(event.mediaItemId == session.mediaItemId) {
            "Status event media item does not match its session"
        }
        requireEnum<TrackingStatus>(event.status) { "Unknown tracking status: ${event.status}" }
        event.previousStatus?.let { previous ->
            requireEnum<TrackingStatus>(previous) { "Unknown previous status: $previous" }
        }
    }

    progressUpdates.forEach { update ->
        val session = sessionsById[update.sessionId]
            ?: error("Progress update references a missing session")
        require(update.mediaItemId == session.mediaItemId) {
            "Progress update media item does not match its session"
        }
        require(update.amount > 0) { "Progress entries must be positive" }
    }

    val updatesBySession = progressUpdates.groupBy { it.sessionId }
    sessions.forEach { session ->
        require(
            session.progressCurrent == session.baselineProgress +
                updatesBySession[session.id].orEmpty().sumOf { it.amount },
        ) { "Session progress does not match its baseline and entries" }
        require(
            session.finishedAtEpochDay == null || session.startedAtEpochDay == null ||
                session.finishedAtEpochDay >= session.startedAtEpochDay,
        ) { "Session finishes before it starts" }
    }

    externalRatings.forEach { rating ->
        require(rating.mediaItemId in mediaItemIds) { "External rating references a missing media item" }
        requireEnum<ExternalRatingSource>(rating.source) { "Unknown external rating source: ${rating.source}" }
        require(rating.score > 0.0) { "External rating scores must be positive" }
        require(rating.maxScore > 0.0) { "External rating max scores must be positive" }
        rating.voteCount?.let { count ->
            require(count >= 0) { "External rating vote counts cannot be negative" }
        }
        rating.scoreDescriptor?.let { descriptor ->
            require(descriptor.isNotBlank()) { "External rating descriptors cannot be blank" }
            require(rating.source == ExternalRatingSource.Steam.name) {
                "Only Steam ratings can have a score descriptor"
            }
            require(mediaTypesById[rating.mediaItemId] == MediaType.Game.name) {
                "Steam score descriptors are only supported for games"
            }
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
        .put("tags", JSONArray(tagsJson.toStringList()))
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
        .putNullable("malId", malId)
        .putNullable("imdbId", imdbId)
        .putNullable("storyGraphId", storyGraphId)
        .putNullable("metadataOverrideFieldsCsv", metadataOverrideFieldsCsv)
        .put("isOwned", isOwned)
}

private fun MediaCreditEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("personName", personName)
        .put("roleType", roleType)
        .putNullable("characterName", characterName)
        .putNullable("personImageUrl", personImageUrl)
        .putNullable("personImageAspectRatio", personImageAspectRatio)
        .putNullable("characterImageUrl", characterImageUrl)
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
        .put("baselineProgress", baselineProgress)
        .putNullable("rating", ratingHalfPoints)
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
        .put("amount", amount)
        .put("loggedAtEpochDay", loggedAtEpochDay)
        .put("hasKnownDate", hasKnownDate)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("coversPeriod", coversPeriod)
}

private fun ExternalRatingEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("source", source)
        .put("score", score)
        .put("maxScore", maxScore)
        .putNullable("voteCount", voteCount)
        .putNullable("scoreDescriptor", scoreDescriptor)
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
        tagsJson = optStringArray("tags").toJsonArrayString(),
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
        malId = optNullableInt("malId"),
        imdbId = optNullableString("imdbId"),
        storyGraphId = optNullableString("storyGraphId"),
        metadataOverrideFieldsCsv = optNullableString("metadataOverrideFieldsCsv"),
        isOwned = if (has("isOwned")) {
            optBoolean("isOwned", false)
        } else {
            // Compatibility with any hand-edited legacy backup that retained only the old subtype.
            optString("ownershipType", "None") != "None"
        },
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
        personImageUrl = optNullableString("personImageUrl"),
        personImageAspectRatio = optNullableFloat("personImageAspectRatio"),
        characterImageUrl = optNullableString("characterImageUrl"),
        sortOrder = optInt("sortOrder", 0),
        metadataSource = optNullableString("metadataSource"),
    )
}

/** Visible for the backup-upgrade test: an older backup's whole points must not be read as halves. */
internal fun JSONObject.toTrackingSessionEntity(schemaVersion: Int): TrackingSessionEntity {
    return TrackingSessionEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        sessionNumber = getInt("sessionNumber"),
        status = getString("status"),
        progressCurrent = optInt("progressCurrent", 0),
        baselineProgress = optInt("baselineProgress", 0),
        // Ratings were whole points until the half-point format, so an older backup's 8 is
        // today's 16. Restoring one without this halves every score in the library.
        ratingHalfPoints = optNullableInt("rating")
            ?.let { if (schemaVersion < FirstHalfPointBackupSchemaVersion) RatingHalfPoints.fromWholePoints(it) else it },
        notes = optNullableString("notes"),
        platformName = optNullableString("platformName"),
        platformType = optNullableString("platformType"),
        startedAtEpochDay = optNullableLong("startedAtEpochDay"),
        finishedAtEpochDay = optNullableLong("finishedAtEpochDay"),
        updatedAtEpochMillis = optLong("updatedAtEpochMillis", 0),
    )
}

/** A cumulative progress row from a pre-increment backup, with the ids the conversion drops. */
private data class LegacyBackupRow(
    val row: LegacyProgressRow,
    val mediaItemId: Long,
    val sessionId: Long,
)

private fun JSONObject.toLegacyBackupRow(): LegacyBackupRow = LegacyBackupRow(
    row = LegacyProgressRow(
        id = getLong("id"),
        progressValue = optInt("progressValue", 0),
        loggedAtEpochDay = optLong("loggedAtEpochDay", LocalDate.now().toEpochDay()),
        createdAtEpochMillis = optLong("createdAtEpochMillis", 0),
        hasKnownDate = optBoolean("hasKnownDate", true),
        countsTowardObjectives = optBoolean("countsTowardObjectives", true),
    ),
    mediaItemId = getLong("mediaItemId"),
    sessionId = getLong("sessionId"),
)

/**
 * Rewrites a pre-increment backup's progress into entries and session baselines.
 *
 * Deliberately the same shape as migration 19→20, including where the baseline comes from: the
 * remainder of the session's own `progressCurrent` after the entries are accounted for. Restoring a
 * backup and upgrading a database have to agree, or the same data would land differently depending
 * on which route it took.
 */
private fun convertLegacyProgress(
    sessions: List<TrackingSessionEntity>,
    legacyRows: List<LegacyBackupRow>,
): Pair<List<TrackingSessionEntity>, List<ProgressUpdateEntity>> {
    val rowsBySession = legacyRows.groupBy { it.sessionId }
    val entries = mutableListOf<ProgressUpdateEntity>()

    val converted = sessions.map { session ->
        val rows = rowsBySession[session.id].orEmpty()
        val sourcesById = rows.associateBy { it.row.id }
        val result = convertSessionToIncrements(
            rows = rows.map { it.row },
            finishedAtEpochDay = session.finishedAtEpochDay,
        )

        result.entries.forEach { entry ->
            val source = sourcesById[entry.id] ?: return@forEach
            entries += ProgressUpdateEntity(
                id = entry.id,
                mediaItemId = source.mediaItemId,
                sessionId = session.id,
                amount = entry.amount,
                loggedAtEpochDay = entry.loggedAtEpochDay,
                hasKnownDate = entry.hasKnownDate,
                createdAtEpochMillis = source.row.createdAtEpochMillis,
            )
        }

        session.copy(
            baselineProgress = (session.progressCurrent - result.entries.sumOf { it.amount })
                .coerceAtLeast(0),
        )
    }

    return converted to entries
}

private fun SessionStatusEventEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mediaItemId", mediaItemId)
        .put("sessionId", sessionId)
        .putNullable("previousStatus", previousStatus)
        .put("status", status)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .putNullable("occurredOnEpochDay", occurredOnEpochDay)
        .put("hasKnownDate", hasKnownDate)
}

private fun JSONObject.toSessionStatusEventEntity(): SessionStatusEventEntity {
    return SessionStatusEventEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        sessionId = getLong("sessionId"),
        previousStatus = optNullableString("previousStatus"),
        status = getString("status"),
        createdAtEpochMillis = optLong("createdAtEpochMillis", 0),
        occurredOnEpochDay = optNullableLong("occurredOnEpochDay"),
        hasKnownDate = optBoolean("hasKnownDate", true),
    )
}

private fun JSONObject.toProgressUpdateEntity(): ProgressUpdateEntity {
    return ProgressUpdateEntity(
        id = getLong("id"),
        mediaItemId = getLong("mediaItemId"),
        sessionId = getLong("sessionId"),
        amount = optInt("amount", 0),
        loggedAtEpochDay = optLong("loggedAtEpochDay", LocalDate.now().toEpochDay()),
        hasKnownDate = optBoolean("hasKnownDate", true),
        createdAtEpochMillis = optLong("createdAtEpochMillis", 0),
        coversPeriod = optBoolean("coversPeriod", false),
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
        scoreDescriptor = optNullableString("scoreDescriptor"),
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

private fun JSONObject.optNullableFloat(name: String): Float? {
    return optNullableDouble(name)?.toFloat()
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

private fun List<MediaCredit>.cleanCredits(): List<MediaCredit> = asSequence()
    .map { credit ->
        credit.copy(
            personName = credit.personName.trim(),
            characterName = credit.characterName?.trim()?.takeIf { it.isNotBlank() },
            personImageUrl = credit.personImageUrl?.trim()?.takeIf { it.isNotBlank() },
            personImageAspectRatio = credit.personImageAspectRatio?.takeIf {
                it.isFinite() && it > 0f
            },
            characterImageUrl = credit.characterImageUrl?.trim()?.takeIf { it.isNotBlank() },
        )
    }
    .filter { it.personName.isNotBlank() }
    .distinctBy { credit ->
        listOf(credit.roleType, credit.personName.lowercase(), credit.characterName?.lowercase())
    }
    .toList()
