package com.nilpo.contenttracker.ui.detail

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedMediaTest {
    @Test
    fun ranksCollectionBeforeCreatorBeforeGenreAndExcludesUnrelatedItems() {
        val current = trackedMedia(
            id = 1,
            collectionId = 10,
            creators = listOf("Ursula Le Guin"),
            genres = listOf("Ciencia ficción"),
        )
        val sameGenre = trackedMedia(
            id = 2,
            title = "Same genre",
            creators = listOf("Someone Else"),
            genres = listOf("ciencia ficcion"),
        )
        val sameCreator = trackedMedia(
            id = 3,
            title = "Same creator",
            creators = listOf("ursula le guin"),
            genres = listOf("Drama"),
        )
        val sameCollection = trackedMedia(
            id = 4,
            title = "Same collection",
            collectionId = 10,
        )
        val unrelated = trackedMedia(
            id = 5,
            title = "Unrelated",
            creators = listOf("Someone Else"),
            genres = listOf("Drama"),
        )

        val related = findRelatedMedia(
            current = current,
            library = listOf(current, sameGenre, sameCreator, sameCollection, unrelated),
        )

        assertEquals(listOf(4L), related.collection.map { it.trackedMedia.item.id })
        assertEquals(listOf(3L, 2L), related.generic.map { it.trackedMedia.item.id })
        assertTrue(related.collection.first().isSameCollection)
        assertEquals(listOf("ursula le guin"), related.generic[0].sharedCreators)
        assertEquals(listOf("ciencia ficcion"), related.generic[1].sharedGenres)
    }

    @Test
    fun supportsMixedMediaTypesWhenTheyShareARealRelationship() {
        val current = trackedMedia(id = 1, type = MediaType.Book, genres = listOf("Fantasy"))
        val related = trackedMedia(id = 2, type = MediaType.Movie, genres = listOf("fantasy"))

        val result = findRelatedMedia(current, listOf(current, related))

        assertEquals(listOf(2L), result.generic.map { it.trackedMedia.item.id })
    }

    private fun trackedMedia(
        id: Long,
        title: String = "Item $id",
        type: MediaType = MediaType.Book,
        collectionId: Long? = null,
        creators: List<String> = emptyList(),
        genres: List<String> = emptyList(),
    ): TrackedMedia {
        return TrackedMedia(
            item = MediaItem(
                id = id,
                type = type,
                title = title,
                collectionId = collectionId,
                creators = creators,
                genres = genres,
            ),
            sessions = emptyList(),
        )
    }
}
