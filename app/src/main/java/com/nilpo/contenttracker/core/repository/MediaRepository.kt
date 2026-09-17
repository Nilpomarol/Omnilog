package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class UnsupportedBackupSchemaException(
    val schemaVersion: Int,
) : IllegalArgumentException("Unsupported backup schema version: $schemaVersion")

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long?,
    val collectionCount: Int,
    val mediaItemCount: Int,
    val mediaCreditCount: Int,
    val trackingSessionCount: Int,
    val progressUpdateCount: Int,
    val externalRatingCount: Int,
    val objectiveCount: Int = 0,
)

/** Complete persistence state for one session, used to make undo an exact inverse. */
data class SessionHistoryState(
    val session: TrackingSessionEntity,
    val progressUpdates: List<ProgressUpdateEntity>,
    val statusEvents: List<SessionStatusEventEntity>,
)

sealed interface DeletionRecovery {
    data class MediaItem(
        val item: MediaItemEntity,
        val collection: MediaCollectionEntity?,
        val credits: List<MediaCreditEntity>,
        val sessions: List<TrackingSessionEntity>,
        val progressUpdates: List<ProgressUpdateEntity>,
        val statusEvents: List<SessionStatusEventEntity> = emptyList(),
        val externalRatings: List<ExternalRatingEntity>,
    ) : DeletionRecovery

    data class PastSession(
        val session: TrackingSessionEntity,
        val progressUpdates: List<ProgressUpdateEntity>,
        val statusEvents: List<SessionStatusEventEntity> = emptyList(),
        /**
         * Which time through the title the deleted session was, captured before deletion. Not the
         * same as its [TrackingSessionEntity.sessionNumber], which can be sparse.
         */
        val visitNumber: Int,
    ) : DeletionRecovery

    data class ProgressUpdate(
        val update: ProgressUpdateEntity,
        val sessionBeforeDeletion: TrackingSessionEntity,
        val sessionAfterDeletion: TrackingSessionEntity,
        /** The reopening the deletion caused by taking a completed session below its total. */
        val reopeningEventId: Long? = null,
    ) : DeletionRecovery

    /** A deleted transition plus exact session snapshots for conflict-safe restoration. */
    data class SessionStatusEvents(
        val events: List<SessionStatusEventEntity>,
        val statusEventsBeforeDeletion: List<SessionStatusEventEntity>,
        val statusEventsAfterDeletion: List<SessionStatusEventEntity>,
        val sessionBeforeDeletion: TrackingSessionEntity,
        val sessionAfterDeletion: TrackingSessionEntity,
    ) : DeletionRecovery

    /** A complete before/after mutation. Restoring is allowed only while [after] still matches. */
    data class SessionMutation(
        val before: SessionHistoryState,
        val after: SessionHistoryState,
    ) : DeletionRecovery
}

data class MetadataRefreshPreview(
    val mediaItemId: Long,
    val refreshed: MetadataSuggestion,
    val changes: List<MetadataRefreshChange>,
)

data class MetadataRefreshChange(
    val field: MetadataRefreshField,
    val currentValue: String,
    val newValue: String,
    val overwritesExistingValue: Boolean,
    val isLocallyOverridden: Boolean = false,
)

enum class MetadataRefreshField {
    Title,
    OriginalTitle,
    ReleaseYear,
    Language,
    ProgressTotal,
    Genres,
    Creators,
    Credits,
    Cover,
    Synopsis,
    SourceUrl,
    ExternalRating,
    ExternalRatings,
    ProviderStats,
}

internal fun MetadataRefreshPreview.requiresMetadataConfirmation(): Boolean {
    return changes.any { change -> change.overwritesExistingValue || change.isLocallyOverridden }
}

internal fun MetadataRefreshPreview.defaultSelectedMetadataFields(): Set<MetadataRefreshField> {
    return changes
        .filterNot { change -> change.isLocallyOverridden }
        .map { change -> change.field }
        .toSet()
}

interface MediaRepository {
    fun observeTrackedMedia(types: Set<MediaType>): Flow<List<TrackedMedia>>

    fun observeObjectives(): Flow<List<Objective>>

    suspend fun addObjective(objective: Objective): Long

    suspend fun updateObjective(objective: Objective)

    suspend fun deleteObjective(objectiveId: Long)

    suspend fun exportBackupJson(): String

    suspend fun previewBackupJson(json: String): BackupPreview

    suspend fun importBackupJson(json: String)

    suspend fun previewImdbCsv(csv: String): ImdbCsvPreview

    suspend fun prepareImdbCsv(csv: String): PreparedImdbCsvImport

    suspend fun importPreparedImdbCsv(prepared: PreparedImdbCsvImport): ImdbCsvImportResult

    suspend fun importImdbCsv(csv: String): ImdbCsvImportResult

    suspend fun previewStoryGraphCsv(csv: String): StoryGraphCsvPreview

    suspend fun prepareStoryGraphCsv(csv: String): PreparedStoryGraphCsvImport

    suspend fun importPreparedStoryGraphCsv(prepared: PreparedStoryGraphCsvImport): StoryGraphCsvImportResult

    suspend fun importStoryGraphCsv(csv: String): StoryGraphCsvImportResult

    suspend fun previewMyAnimeListXml(xml: String): MyAnimeListXmlPreview

    suspend fun prepareMyAnimeListXml(xml: String): PreparedMyAnimeListXmlImport

    suspend fun importPreparedMyAnimeListXml(prepared: PreparedMyAnimeListXmlImport): MyAnimeListXmlImportResult

    suspend fun importMyAnimeListXml(xml: String): MyAnimeListXmlImportResult

    suspend fun previewMyAnimeListAccount(items: List<MyAnimeListImportItem>): ProviderImportPreview

    suspend fun importMyAnimeListAccount(items: List<MyAnimeListImportItem>): ProviderImportResult

    suspend fun startNewSession(request: AddTrackingSessionRequest)

    suspend fun addTrackedMedia(request: AddTrackedMediaRequest): Long

    suspend fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        ratingHalfPoints: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ): DeletionRecovery.SessionMutation?

    suspend fun deletePastSession(sessionId: Long): DeletionRecovery?

    /**
     * Deletes the live session and hands the title back to the one before it.
     *
     * The counterpart to [deletePastSession], which refuses the latest session precisely because
     * this exists. Refuses when there is nothing to fall back to: the only session is untracking,
     * not a session delete.
     */
    suspend fun deleteCurrentSession(sessionId: Long): DeletionRecovery?

    suspend fun deleteProgressUpdate(progressUpdateId: Long): DeletionRecovery?

    /**
     * Removes one logged status transition and reconnects the transition chain around it.
     *
     * Deleting a transition means it never happened, so the session falls back to the state it was
     * in before — delete a resume and the session is paused again, which is what it was. An earlier
     * version removed the pause and its resume together on the grounds that half a pair described
     * nothing real; but a lone pause describes something perfectly real, namely a session that is
     * still paused.
     */
    suspend fun deleteSessionStatusEvent(eventId: Long): DeletionRecovery?

    /**
     * Re-dates a logged status change to the day it actually happened, or marks the day unknown when
     * [occurredOn] is null. False when refused: a day before the session started or out of order with
     * the dated transitions around it.
     */
    suspend fun updateSessionStatusEventDate(eventId: Long, occurredOn: java.time.LocalDate?): Boolean

    /**
     * Edits one activity entry and re-derives the owning session's total in one transaction.
     *
     * [amount] is the increment the entry records, not a running total. A null [loggedAt] marks the
     * entry as having no known date; a null [coversPeriod] leaves the coverage flag as it was.
     */
    suspend fun updateProgressUpdate(
        progressUpdateId: Long,
        amount: Int,
        loggedAt: LocalDate?,
        coversPeriod: Boolean? = null,
    ): Boolean

    suspend fun deleteMediaItem(mediaItemId: Long): DeletionRecovery?
    suspend fun restoreDeletion(recovery: DeletionRecovery): Boolean

    suspend fun addExternalRating(
        mediaItemId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    )

    suspend fun updateExternalRating(
        externalRatingId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    )

    suspend fun setPrimaryExternalRating(externalRatingId: Long)

    suspend fun deleteExternalRating(externalRatingId: Long)

    suspend fun updateMediaCollectionName(collectionId: Long, name: String)

    suspend fun deleteMediaCollection(collectionId: Long)

    suspend fun updateCollectionItemOrder(collectionId: Long, itemOrders: List<CollectionItemOrder>)

    suspend fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        collectionSortOrder: Double?,
        progressTotal: Int?,
        isOwned: Boolean,
    )

    suspend fun updateMediaItemMetadata(
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
    )

    suspend fun previewMediaItemMetadataRefresh(
        mediaItemId: Long,
        metadataRepository: MetadataRepository,
    ): MetadataRefreshPreview?

    suspend fun applyMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Boolean

    /** Applies safe inbound enrichment without queueing the imported state back to MAL. */
    suspend fun applyImportedMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Boolean

    /** Refreshes provider-owned fields only, including a final override check immediately before writing. */
    suspend fun applyAutomaticMediaItemMetadataRefresh(preview: MetadataRefreshPreview): Boolean

    suspend fun refreshMediaItemMetadata(mediaItemId: Long, metadataRepository: MetadataRepository): Boolean

    suspend fun previewMediaItemMetadataLink(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
        metadataRepository: MetadataRepository,
    ): MetadataRefreshPreview?

    suspend fun linkMediaItemMetadata(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
        metadataRepository: MetadataRepository,
    ): Boolean
}
