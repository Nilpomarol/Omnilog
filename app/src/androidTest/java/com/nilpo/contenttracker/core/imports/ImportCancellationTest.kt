package com.nilpo.contenttracker.core.imports

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nilpo.contenttracker.core.database.ContentTrackerDatabase
import com.nilpo.contenttracker.core.database.entity.ImportBatchEntity
import com.nilpo.contenttracker.core.database.entity.ImportBatchItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportCancellationTest {
    private lateinit var database: ContentTrackerDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ContentTrackerDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun cancellationIsTerminalAndPreservesAppliedItems() = runBlocking {
        val dao = database.importDao()
        val batchId = dao.insertBatch(
            ImportBatchEntity(
                source = ImportSource.ImdbCsv.name,
                state = ImportBatchState.Enriching.name,
                totalCount = 5,
                createdAtEpochMillis = 100,
                updatedAtEpochMillis = 100,
            ),
        )
        val itemIds = dao.insertItems(
            listOf(
                item(batchId, "pending", ImportItemState.Pending),
                item(batchId, "resolving", ImportItemState.Resolving),
                item(batchId, "review", ImportItemState.NeedsReview),
                item(batchId, "failed", ImportItemState.Failed),
                item(batchId, "applied", ImportItemState.Applied),
            ),
        )

        dao.cancelBatch(batchId, now = 500)

        assertEquals(ImportBatchState.Cancelled.name, dao.getBatch(batchId)?.state)
        assertEquals(500, dao.getBatch(batchId)?.completedAtEpochMillis)
        itemIds.take(4).forEach { itemId ->
            assertEquals(ImportItemState.Cancelled.name, dao.getItem(itemId)?.state)
        }
        assertEquals(ImportItemState.Applied.name, dao.getItem(itemIds.last())?.state)

        // A late provider response and a stale worker finalization cannot revive a cancelled batch.
        dao.finishItem(
            itemId = itemIds.first(),
            state = ImportItemState.Applied.name,
            providerSource = null,
            providerExternalId = null,
            matchKind = null,
            candidateReferencesJson = null,
            retryable = false,
            lastError = null,
            now = 600,
            completedAtEpochMillis = 600,
        )
        dao.updateBatchState(
            batchId = batchId,
            state = ImportBatchState.Completed.name,
            diagnostic = null,
            now = 600,
            completedAtEpochMillis = 600,
        )
        assertEquals(ImportItemState.Cancelled.name, dao.getItem(itemIds.first())?.state)
        assertEquals(ImportBatchState.Cancelled.name, dao.getBatch(batchId)?.state)
    }

    private fun item(
        batchId: Long,
        sourceKey: String,
        state: ImportItemState,
    ) = ImportBatchItemEntity(
        batchId = batchId,
        sourceKey = sourceKey,
        state = state.name,
        updatedAtEpochMillis = 100,
    )
}
