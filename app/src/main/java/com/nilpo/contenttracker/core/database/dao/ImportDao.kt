package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportDao {
    @Query(
        "SELECT id AS mediaItemId, metadataSource, metadataExternalId, malId FROM media_items " +
            "WHERE metadataSource IN ('Jikan', 'Imdb', 'StoryGraph') " +
            "AND (coverUrl IS NULL OR synopsis IS NULL OR releaseYear IS NULL) " +
            "AND NOT EXISTS (SELECT 1 FROM import_batch_items WHERE mediaItemId = media_items.id)",
    )
    suspend fun getUnqueuedImportedMedia(): List<UnqueuedImportedMedia>

    @Insert
    suspend fun insertBatch(batch: ImportBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<ImportBatchItemEntity>): List<Long>

    @Query("SELECT * FROM import_batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatch(batchId: Long): ImportBatchEntity?

    @Query("SELECT * FROM import_batches ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeBatches(): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM import_batch_items ORDER BY batchId DESC, id")
    fun observeItems(): Flow<List<ImportBatchItemEntity>>

    @Query(
        "SELECT import_batch_items.id AS itemId, import_batch_items.batchId AS batchId, " +
            "import_batch_items.mediaItemId AS mediaItemId, media_items.title AS title, " +
            "media_items.type AS mediaType, import_batch_items.matchKind AS matchKind, " +
            "import_batch_items.providerSource AS providerSource, " +
            "import_batch_items.providerExternalId AS providerExternalId, " +
            "import_batch_items.candidateReferencesJson AS candidateReferencesJson " +
            "FROM import_batch_items INNER JOIN media_items " +
            "ON media_items.id = import_batch_items.mediaItemId " +
            "WHERE import_batch_items.state = 'NeedsReview' " +
            "ORDER BY import_batch_items.batchId DESC, media_items.title COLLATE NOCASE",
    )
    fun observeReviewItems(): Flow<List<ImportReviewRow>>

    @Query("SELECT * FROM import_batch_items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: Long): ImportBatchItemEntity?

    @Query(
        "SELECT import_batch_items.* FROM import_batch_items " +
            "INNER JOIN import_batches ON import_batches.id = import_batch_items.batchId " +
            "WHERE import_batch_items.state = 'NeedsReview' " +
            "AND import_batches.source IN ('MalApi', 'MalXml')",
    )
    suspend fun getMalReviewItems(): List<ImportBatchItemEntity>

    @Query(
        "SELECT import_batch_items.mediaItemId FROM import_batch_items " +
            "INNER JOIN import_batches ON import_batches.id = import_batch_items.batchId " +
            "WHERE import_batch_items.mediaItemId IS NOT NULL " +
            "AND import_batch_items.state IN ('Pending', 'Resolving') " +
            "AND import_batches.source IN ('MalApi', 'MalXml')",
    )
    suspend fun getActiveMalMediaIds(): List<Long>

    @Query(
        "SELECT * FROM import_batches WHERE state IN " +
            "('Enriching', 'Paused') ORDER BY createdAtEpochMillis, id",
    )
    suspend fun getResumableBatches(): List<ImportBatchEntity>

    @Query(
        "SELECT * FROM import_batch_items WHERE batchId = :batchId AND state = 'Pending' " +
            "ORDER BY id LIMIT :limit",
    )
    suspend fun getPendingItems(batchId: Long, limit: Int): List<ImportBatchItemEntity>

    @Query(
        "UPDATE import_batch_items SET state = 'Resolving', attemptCount = attemptCount + 1, " +
            "lastAttemptAtEpochMillis = :now, updatedAtEpochMillis = :now, lastError = NULL, retryable = 0 " +
            "WHERE id = :itemId AND state = 'Pending' AND EXISTS (" +
            "SELECT 1 FROM import_batches WHERE import_batches.id = import_batch_items.batchId " +
            "AND import_batches.state = 'Enriching')",
    )
    suspend fun claimPendingItem(itemId: Long, now: Long): Int

    @Query(
        "UPDATE import_batch_items SET state = :state, providerSource = :providerSource, " +
            "providerExternalId = :providerExternalId, matchKind = :matchKind, " +
            "candidateReferencesJson = :candidateReferencesJson, retryable = :retryable, " +
            "lastError = :lastError, updatedAtEpochMillis = :now, " +
            "completedAtEpochMillis = :completedAtEpochMillis WHERE id = :itemId " +
            "AND NOT EXISTS (SELECT 1 FROM import_batches " +
            "WHERE import_batches.id = import_batch_items.batchId " +
            "AND import_batches.state = 'Cancelled')",
    )
    suspend fun finishItem(
        itemId: Long,
        state: String,
        providerSource: String?,
        providerExternalId: String?,
        matchKind: String?,
        candidateReferencesJson: String?,
        retryable: Boolean,
        lastError: String?,
        now: Long,
        completedAtEpochMillis: Long?,
    )

    @Query(
        "UPDATE import_batch_items SET providerSource = :providerSource, " +
            "providerExternalId = :providerExternalId, updatedAtEpochMillis = :now " +
            "WHERE id = :itemId AND state = 'NeedsReview'",
    )
    suspend fun selectReviewReference(
        itemId: Long,
        providerSource: String,
        providerExternalId: String,
        now: Long,
    ): Int

    @Query(
        "UPDATE import_batch_items SET state = 'Pending', retryable = 0, lastError = NULL, " +
            "completedAtEpochMillis = NULL, updatedAtEpochMillis = :now " +
            "WHERE id IN (:itemIds) AND state = 'NeedsReview'",
    )
    suspend fun resetReviewItems(itemIds: List<Long>, now: Long): Int

    @Query(
        "UPDATE import_batch_items SET state = 'Pending', retryable = 0, completedAtEpochMillis = NULL, " +
            "updatedAtEpochMillis = :now " +
            "WHERE batchId = :batchId AND state = 'Resolving'",
    )
    suspend fun resetInterruptedItems(batchId: Long, now: Long)

    @Query(
        "UPDATE import_batch_items SET state = 'Pending', retryable = 0, completedAtEpochMillis = NULL, " +
            "updatedAtEpochMillis = :now " +
            "WHERE batchId = :batchId AND state = 'Failed' AND retryable = 1 AND attemptCount < :maxAttempts",
    )
    suspend fun resetRetryableFailures(batchId: Long, maxAttempts: Int, now: Long)

    @Query("SELECT COUNT(*) FROM import_batch_items WHERE batchId = :batchId AND state = 'Pending'")
    suspend fun pendingCount(batchId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM import_batch_items WHERE batchId = :batchId " +
            "AND state = 'Failed' AND retryable = 1 AND attemptCount < :maxAttempts",
    )
    suspend fun retryableFailureCount(batchId: Long, maxAttempts: Int): Int

    @Query(
        "SELECT COUNT(*) FROM import_batch_items WHERE batchId = :batchId " +
            "AND state IN ('Failed', 'Unavailable', 'NoMatch', 'NeedsReview')",
    )
    suspend fun issueCount(batchId: Long): Int

    @Query(
        "UPDATE import_batches SET state = :state, diagnostic = :diagnostic, " +
            "updatedAtEpochMillis = :now, completedAtEpochMillis = :completedAtEpochMillis " +
            "WHERE id = :batchId AND (state != 'Cancelled' OR :state = 'Cancelled')",
    )
    suspend fun updateBatchState(
        batchId: Long,
        state: String,
        diagnostic: String?,
        now: Long,
        completedAtEpochMillis: Long?,
    )

    @Query(
        "UPDATE import_batch_items SET state = 'Cancelled', retryable = 0, lastError = NULL, " +
            "candidateReferencesJson = NULL, updatedAtEpochMillis = :now, completedAtEpochMillis = :now " +
            "WHERE batchId = :batchId AND state IN " +
            "('Pending', 'Resolving', 'NeedsReview', 'Failed', 'Unavailable', 'NoMatch')",
    )
    suspend fun cancelUnfinishedItems(batchId: Long, now: Long)

    @Transaction
    suspend fun cancelBatch(batchId: Long, now: Long) {
        cancelUnfinishedItems(batchId, now)
        updateBatchState(
            batchId = batchId,
            state = "Cancelled",
            diagnostic = "Enrichment cancelled by user",
            now = now,
            completedAtEpochMillis = now,
        )
    }

    @Query(
        "UPDATE import_batch_items SET state = 'Pending', retryable = 0, lastError = NULL, " +
            "completedAtEpochMillis = NULL, " +
            "updatedAtEpochMillis = :now WHERE batchId = :batchId AND (" +
            "state IN ('Failed', 'Unavailable', 'NoMatch') OR (" +
            "state = 'Applied' AND EXISTS (" +
            "SELECT 1 FROM import_batches b WHERE b.id = batchId AND b.source = 'StoryGraphCsv'" +
            ") AND EXISTS (" +
            "SELECT 1 FROM media_items m WHERE m.id = mediaItemId " +
            "AND (m.progressTotal IS NULL OR m.progressTotal <= 0)" +
            ")))",
    )
    suspend fun retryEligibleIssues(batchId: Long, now: Long)
}

data class UnqueuedImportedMedia(
    val mediaItemId: Long,
    val metadataSource: String,
    val metadataExternalId: String?,
    val malId: Int?,
)

data class ImportReviewRow(
    val itemId: Long,
    val batchId: Long,
    val mediaItemId: Long,
    val title: String,
    val mediaType: String,
    val matchKind: String?,
    val providerSource: String?,
    val providerExternalId: String?,
    val candidateReferencesJson: String?,
)
