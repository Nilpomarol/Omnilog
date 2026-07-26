package com.nilpo.contenttracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nilpo.contenttracker.core.database.entity.MetadataRefreshItemEntity
import com.nilpo.contenttracker.core.database.entity.MetadataRefreshRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataRefreshDao {
    @Query("SELECT * FROM metadata_refresh_runs ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeRuns(): Flow<List<MetadataRefreshRunEntity>>

    @Query("SELECT * FROM metadata_refresh_items ORDER BY runId DESC, id")
    fun observeItems(): Flow<List<MetadataRefreshItemEntity>>

    @Query("SELECT * FROM metadata_refresh_runs WHERE id = :runId LIMIT 1")
    suspend fun getRun(runId: Long): MetadataRefreshRunEntity?

    @Query("SELECT * FROM metadata_refresh_runs WHERE state IN ('Queued', 'Refreshing') ORDER BY id DESC LIMIT 1")
    suspend fun getActiveRun(): MetadataRefreshRunEntity?

    @Query("SELECT * FROM metadata_refresh_runs WHERE state = 'Refreshing' ORDER BY id")
    suspend fun getResumableRuns(): List<MetadataRefreshRunEntity>

    @Insert
    suspend fun insertRun(run: MetadataRefreshRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<MetadataRefreshItemEntity>): List<Long>

    @Query("SELECT * FROM metadata_refresh_items WHERE runId = :runId AND state = 'Pending' ORDER BY id LIMIT :limit")
    suspend fun getPendingItems(runId: Long, limit: Int): List<MetadataRefreshItemEntity>

    @Query("SELECT COUNT(*) FROM metadata_refresh_items WHERE runId = :runId AND state = 'Pending'")
    suspend fun pendingCount(runId: Long): Int

    @Query("SELECT COUNT(*) FROM metadata_refresh_items WHERE runId = :runId AND state = 'Failed'")
    suspend fun failureCount(runId: Long): Int

    @Query("UPDATE metadata_refresh_items SET state = 'Processing', attemptCount = attemptCount + 1, retryable = 0, updatedAtEpochMillis = :now WHERE id = :itemId AND state = 'Pending'")
    suspend fun claimPendingItem(itemId: Long, now: Long): Int

    @Query("UPDATE metadata_refresh_items SET state = 'Pending', updatedAtEpochMillis = :now WHERE runId = :runId AND state = 'Processing'")
    suspend fun resetInterruptedItems(runId: Long, now: Long): Int

    @Query("UPDATE metadata_refresh_items SET state = :state, retryable = :retryable, lastError = :error, updatedAtEpochMillis = :now, completedAtEpochMillis = :completedAt WHERE id = :itemId")
    suspend fun finishItem(
        itemId: Long,
        state: String,
        retryable: Boolean,
        error: String?,
        now: Long,
        completedAt: Long?,
    )

    @Query("UPDATE metadata_refresh_runs SET state = :state, updatedAtEpochMillis = :now, completedAtEpochMillis = :completedAt WHERE id = :runId")
    suspend fun updateRunState(runId: Long, state: String, now: Long, completedAt: Long?)

    @Query("UPDATE metadata_refresh_items SET state = 'Cancelled', retryable = 0, updatedAtEpochMillis = :now, completedAtEpochMillis = :now WHERE runId = :runId AND state IN ('Pending', 'Processing')")
    suspend fun cancelUnfinishedItems(runId: Long, now: Long): Int
}
