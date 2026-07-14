package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeletionRecoveryStoreTest {
    @Test
    fun takeReturnsRecoveryOnceAndTokensAreUnique() {
        val store = DeletionRecoveryStore()
        val first = recovery("First")
        val second = recovery("Second")

        val firstToken = store.put(first)
        val secondToken = store.put(second)

        assertEquals(first, store.take(firstToken))
        assertNull(store.take(firstToken))
        assertEquals(second, store.take(secondToken))
    }

    @Test
    fun discardRemovesPendingRecovery() {
        val store = DeletionRecoveryStore()
        val token = store.put(recovery("Discarded"))

        store.discard(token)

        assertNull(store.take(token))
    }

    private fun recovery(title: String): DeletionRecovery = DeletionRecovery.MediaItem(
        item = MediaItemEntity(type = "Movie", title = title),
        collection = null,
        credits = emptyList(),
        sessions = emptyList(),
        progressUpdates = emptyList(),
        externalRatings = emptyList(),
    )
}
