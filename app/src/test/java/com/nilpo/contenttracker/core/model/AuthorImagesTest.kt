package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthorImagesTest {
    @Test
    fun reusesOneStoredPortraitForEveryMatchingAuthorName() {
        val library = listOf(
            trackedBook(
                id = 1,
                author = "Octavia E. Butler",
                imageUrl = null,
            ),
            trackedBook(
                id = 2,
                author = "Octavia E. Butler",
                imageUrl = "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false",
            ),
        )

        assertEquals(
            "https://covers.openlibrary.org/a/id/9285851-L.jpg?default=false",
            library.authorImageUrl("  octavia e. butler "),
        )
        assertNull(library.authorImageUrl("Another Author"))
    }

    @Test
    fun usesAuthorCreditsBeforeTheLegacyCreatorList() {
        val book = TrackedMedia(
            item = MediaItem(
                id = 1,
                type = MediaType.Book,
                title = "Book",
                creators = listOf("Stale author"),
            ),
            sessions = emptyList(),
            credits = listOf(
                MediaCredit(
                    personName = "Metadata author",
                    roleType = MediaCreditRole.Author,
                    personImageUrl = "https://example.com/author.jpg",
                ),
            ),
        )

        assertEquals(listOf("Metadata author"), book.creatorNames())
        assertEquals(
            "https://example.com/author.jpg",
            listOf(book).creatorImageUrl("metadata author"),
        )
        assertEquals(false, book.hasCreator("Stale author"))
    }

    @Test
    fun reusesACompanyLogoAcrossDeveloperAndPublisherCredits() {
        val library = listOf(
            trackedGame(
                id = 1,
                company = "Studio Example",
                role = MediaCreditRole.Publisher,
                imageUrl = "https://images.igdb.com/logo.png",
            ),
            trackedGame(
                id = 2,
                company = "Studio Example",
                role = MediaCreditRole.Developer,
                imageUrl = null,
            ),
        )

        assertEquals(
            "https://images.igdb.com/logo.png",
            library.contributorImageUrls(MediaCreditRole.Developer)["studio example"],
        )
        assertEquals(
            "https://images.igdb.com/logo.png",
            library.creatorImageUrl("Studio Example"),
        )
    }

    /**
     * The case that made a role-blind lookup visibly wrong: someone who both directs and acts was
     * liable to be drawn in a directors list with their cast headshot.
     */
    @Test
    fun neverIllustratesADirectorWithTheirCastHeadshot() {
        val library = listOf(
            TrackedMedia(
                item = MediaItem(id = 1, type = MediaType.Movie, title = "Acted in"),
                sessions = emptyList(),
                credits = listOf(
                    MediaCredit(
                        personName = "Clint Eastwood",
                        roleType = MediaCreditRole.Cast,
                        personImageUrl = "https://example.com/headshot.jpg",
                    ),
                ),
            ),
            TrackedMedia(
                item = MediaItem(id = 2, type = MediaType.Movie, title = "Directed"),
                sessions = emptyList(),
                credits = listOf(
                    MediaCredit(
                        personName = "Clint Eastwood",
                        roleType = MediaCreditRole.Director,
                        personImageUrl = "https://example.com/director.jpg",
                    ),
                ),
            ),
        )

        assertEquals(
            "https://example.com/director.jpg",
            library.contributorImageUrls(MediaCreditRole.Director)["clint eastwood"],
        )
        assertEquals("https://example.com/director.jpg", library.creatorImageUrl("Clint Eastwood"))
        assertEquals(
            "https://example.com/headshot.jpg",
            library.contributorImageUrls(MediaCreditRole.Cast)["clint eastwood"],
        )
    }

    @Test
    fun leavesADirectorUnillustratedRatherThanBorrowingACastPortrait() {
        val library = listOf(
            TrackedMedia(
                item = MediaItem(id = 1, type = MediaType.Movie, title = "Film"),
                sessions = emptyList(),
                credits = listOf(
                    MediaCredit(
                        personName = "Someone",
                        roleType = MediaCreditRole.Cast,
                        personImageUrl = "https://example.com/headshot.jpg",
                    ),
                    MediaCredit(personName = "Someone", roleType = MediaCreditRole.Director),
                ),
            ),
        )

        assertNull(library.contributorImageUrls(MediaCreditRole.Director)["someone"])
        assertNull(library.creatorImageUrl("Someone"))
    }

    /**
     * The library list is re-sorted constantly and its identity changes on every emission, so an
     * answer that depended on iteration order made the same person render differently from one
     * screen — or one recomposition — to the next.
     */
    @Test
    fun resolvesTheSameImageWhateverOrderTheLibraryIsIn() {
        val library = listOf(
            trackedBook(id = 3, author = "Ursula K. Le Guin", imageUrl = "https://example.com/c.jpg"),
            trackedBook(id = 1, author = "Ursula K. Le Guin", imageUrl = "https://example.com/a.jpg"),
            trackedBook(id = 2, author = "Ursula K. Le Guin", imageUrl = "https://example.com/b.jpg"),
        )

        val expected = library.creatorImageUrl("Ursula K. Le Guin")
        assertNotNull(expected)
        listOf(
            library.reversed(),
            library.sortedBy { it.item.title },
            library.shuffled(java.util.Random(7)),
        ).forEach { reordered ->
            assertEquals(expected, reordered.creatorImageUrl("Ursula K. Le Guin"))
            assertEquals(
                expected,
                reordered.contributorImageUrls(MediaCreditRole.Author)["ursula k. le guin"],
            )
        }
    }

    /** The detail page and the home group must not disagree about the same person. */
    @Test
    fun agreesBetweenTheCreditListAndTheHomeGroupLookup() {
        val library = listOf(
            trackedBook(id = 5, author = "Shared Author", imageUrl = "https://example.com/late.jpg"),
            trackedBook(id = 2, author = "Shared Author", imageUrl = "https://example.com/early.jpg"),
        )

        assertEquals(
            library.contributorImageUrls(MediaCreditRole.Author)["shared author"],
            library.creatorImageUrl("Shared Author"),
        )
    }

    @Test
    fun sharesAPerformerPortraitAcrossTitles() {
        val library = listOf(
            TrackedMedia(
                item = MediaItem(id = 1, type = MediaType.Movie, title = "With portrait"),
                sessions = emptyList(),
                credits = listOf(
                    MediaCredit(
                        personName = "A Performer",
                        roleType = MediaCreditRole.Cast,
                        personImageUrl = "https://example.com/performer.jpg",
                    ),
                ),
            ),
            TrackedMedia(
                item = MediaItem(id = 2, type = MediaType.Movie, title = "Without portrait"),
                sessions = emptyList(),
                credits = listOf(
                    MediaCredit(personName = "A Performer", roleType = MediaCreditRole.Cast),
                ),
            ),
        )

        assertEquals(
            "https://example.com/performer.jpg",
            library.contributorImageUrls(MediaCreditRole.Cast)["a performer"],
        )
    }

    @Test
    fun ignoresBlankImageUrls() {
        val library = listOf(
            trackedBook(id = 1, author = "Author", imageUrl = "   "),
            trackedBook(id = 2, author = "Author", imageUrl = "https://example.com/real.jpg"),
        )

        assertEquals("https://example.com/real.jpg", library.creatorImageUrl("Author"))
    }

    private fun trackedBook(id: Long, author: String, imageUrl: String?): TrackedMedia = TrackedMedia(
        item = MediaItem(id = id, type = MediaType.Book, title = "Book $id"),
        sessions = emptyList(),
        credits = listOf(
            MediaCredit(
                personName = author,
                roleType = MediaCreditRole.Author,
                personImageUrl = imageUrl,
            ),
        ),
    )

    private fun trackedGame(
        id: Long,
        company: String,
        role: MediaCreditRole,
        imageUrl: String?,
    ): TrackedMedia = TrackedMedia(
        item = MediaItem(id = id, type = MediaType.Game, title = "Game $id"),
        sessions = emptyList(),
        credits = listOf(
            MediaCredit(personName = company, roleType = role, personImageUrl = imageUrl),
        ),
    )
}
