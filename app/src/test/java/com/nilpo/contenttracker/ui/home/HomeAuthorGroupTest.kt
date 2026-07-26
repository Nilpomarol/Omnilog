package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAuthorGroupTest {
    @Test
    fun drawsACompanyGroupWholeAndAPersonGroupCropped() {
        val studioGroup = authorGroups(
            listOf(
                trackedItem(id = 1, type = MediaType.Anime, name = "Studio Example"),
                trackedItem(id = 2, type = MediaType.Anime, name = "Studio Example"),
            ),
        ).single()
        val authorGroup = authorGroups(
            listOf(trackedItem(id = 3, type = MediaType.Book, name = "An Author")),
        ).single()

        assertTrue(studioGroup.imageIsLogo)
        assertFalse(authorGroup.imageIsLogo)
    }

    /**
     * A group spanning a company's anime and a person's books has no right answer, and reading the
     * type off whichever item sorted first made the choice depend on list order.
     */
    @Test
    fun croppedIsTheAnswerWhenAGroupSpansCompaniesAndPeople() {
        val items = listOf(
            trackedItem(id = 1, type = MediaType.Anime, name = "Shared Name"),
            trackedItem(id = 2, type = MediaType.Book, name = "Shared Name"),
        )

        val group = authorGroups(items).single()
        val reordered = authorGroups(items.reversed()).single()

        assertFalse(group.imageIsLogo)
        assertEquals(group.imageIsLogo, reordered.imageIsLogo)
    }

    @Test
    fun resolvesTheSameGroupImageWhateverOrderTheLibraryIsIn() {
        val items = listOf(
            trackedItem(id = 3, type = MediaType.Book, name = "An Author", imageUrl = "https://example.com/c.jpg"),
            trackedItem(id = 1, type = MediaType.Book, name = "An Author", imageUrl = "https://example.com/a.jpg"),
            trackedItem(id = 2, type = MediaType.Book, name = "An Author", imageUrl = "https://example.com/b.jpg"),
        )

        assertEquals(
            authorGroups(items).single().imageUrl,
            authorGroups(items.reversed()).single().imageUrl,
        )
    }

    private fun authorGroups(items: List<TrackedMedia>): List<HomeDisplayGroup> =
        buildHomeGroups(items, HomeGroupMode.Author)

    private fun trackedItem(
        id: Long,
        type: MediaType,
        name: String,
        imageUrl: String? = null,
    ): TrackedMedia = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Item $id"),
        sessions = emptyList(),
        credits = listOf(
            MediaCredit(
                personName = name,
                roleType = when (type) {
                    MediaType.Anime -> MediaCreditRole.Studio
                    MediaType.Book -> MediaCreditRole.Author
                    MediaType.Movie -> MediaCreditRole.Director
                    MediaType.TvShow -> MediaCreditRole.Creator
                    MediaType.Game -> MediaCreditRole.Developer
                },
                personImageUrl = imageUrl,
            ),
        ),
    )
}
