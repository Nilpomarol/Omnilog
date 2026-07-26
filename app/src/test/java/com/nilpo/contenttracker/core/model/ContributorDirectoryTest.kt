package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributorDirectoryTest {
    @Test
    fun countsTheItemsAContributorRepresentsAndAveragesTheirRatings() {
        val directory = listOf(
            trackedBook(id = 1, author = "An Author", rating = 8),
            trackedBook(id = 2, author = "An Author", rating = 6),
            trackedBook(id = 3, author = "Someone Else", rating = 10),
        ).contributorDirectory()

        val stats = directory.stats(MediaCreditRole.Author, "  AN AUTHOR ")
        assertEquals(2, stats.itemCount)
        assertEquals(7.0, stats.averageRating!!, 0.0001)
    }

    @Test
    fun leavesTheAverageAbsentWhenNothingIsRated() {
        val directory = listOf(trackedBook(id = 1, author = "An Author", rating = null))
            .contributorDirectory()

        assertEquals(1, directory.stats(MediaCreditRole.Author, "An Author").itemCount)
        assertNull(directory.stats(MediaCreditRole.Author, "An Author").averageRating)
    }

    @Test
    fun reportsNothingForAContributorTheLibraryDoesNotCredit() {
        val directory = listOf(trackedBook(id = 1, author = "An Author", rating = 8))
            .contributorDirectory()

        assertEquals(ContributorStats(), directory.stats(MediaCreditRole.Author, "Nobody"))
        assertNull(directory.imageUrl(MediaCreditRole.Author, "Nobody"))
    }

    /** Counts are keyed by exact role, so an actor's filmography is not merged into a director's. */
    @Test
    fun keepsCountsSeparatePerRole() {
        val directory = listOf(
            movie(id = 1, name = "Clint Eastwood", role = MediaCreditRole.Cast),
            movie(id = 2, name = "Clint Eastwood", role = MediaCreditRole.Cast),
            movie(id = 3, name = "Clint Eastwood", role = MediaCreditRole.Director),
        ).contributorDirectory()

        assertEquals(2, directory.stats(MediaCreditRole.Cast, "Clint Eastwood").itemCount)
        assertEquals(1, directory.stats(MediaCreditRole.Director, "Clint Eastwood").itemCount)
    }

    @Test
    fun countsAnItemOnceEvenWhenItCreditsTheSamePersonTwiceInARole() {
        val film = TrackedMedia(
            item = MediaItem(id = 1, type = MediaType.Movie, title = "Film"),
            sessions = emptyList(),
            credits = listOf(
                MediaCredit(personName = "A Performer", roleType = MediaCreditRole.Cast, characterName = "One"),
                MediaCredit(personName = "A Performer", roleType = MediaCreditRole.Cast, characterName = "Two"),
            ),
        )

        assertEquals(
            1,
            listOf(film).contributorDirectory().stats(MediaCreditRole.Cast, "A Performer").itemCount,
        )
    }

    /** The directory must not disagree with the resolver every other surface uses. */
    @Test
    fun resolvesTheSameImageAsTheStandaloneLookups() {
        val library = listOf(
            trackedBook(id = 5, author = "An Author", rating = null, imageUrl = "https://example.com/late.jpg"),
            trackedBook(id = 2, author = "An Author", rating = null, imageUrl = "https://example.com/early.jpg"),
        )

        assertEquals(
            library.contributorImageUrls(MediaCreditRole.Author)["an author"],
            library.contributorDirectory().imageUrl(MediaCreditRole.Author, "An Author"),
        )
        assertEquals(
            library.creatorImageUrl("An Author"),
            library.contributorDirectory().imageUrl(MediaCreditRole.Author, "An Author"),
        )
    }

    @Test
    fun sharesOneLogoAcrossCompanyRolesButNotWithAPerson() {
        val directory = listOf(
            game(id = 1, name = "Studio Example", role = MediaCreditRole.Publisher, imageUrl = "https://example.com/logo.png"),
            game(id = 2, name = "Studio Example", role = MediaCreditRole.Developer, imageUrl = null),
            movie(id = 3, name = "Studio Example", role = MediaCreditRole.Cast, imageUrl = "https://example.com/face.jpg"),
        ).contributorDirectory()

        assertEquals(
            "https://example.com/logo.png",
            directory.imageUrl(MediaCreditRole.Developer, "Studio Example"),
        )
        assertEquals(
            "https://example.com/face.jpg",
            directory.imageUrl(MediaCreditRole.Cast, "Studio Example"),
        )
    }

    @Test
    fun resolvesTheSameAnswerWhateverOrderTheLibraryIsIn() {
        val library = listOf(
            trackedBook(id = 3, author = "An Author", rating = 9, imageUrl = "https://example.com/c.jpg"),
            trackedBook(id = 1, author = "An Author", rating = 7, imageUrl = "https://example.com/a.jpg"),
            trackedBook(id = 2, author = "An Author", rating = 8, imageUrl = "https://example.com/b.jpg"),
        )

        val expected = library.contributorDirectory()
        listOf(library.reversed(), library.shuffled(java.util.Random(3))).forEach { reordered ->
            val actual = reordered.contributorDirectory()
            assertEquals(
                expected.imageUrl(MediaCreditRole.Author, "An Author"),
                actual.imageUrl(MediaCreditRole.Author, "An Author"),
            )
            assertEquals(
                expected.stats(MediaCreditRole.Author, "An Author"),
                actual.stats(MediaCreditRole.Author, "An Author"),
            )
        }
    }

    /** One file per contributor, not one per item that credits them. */
    @Test
    fun listsEachContributorImageOnceForStorage() {
        val library = listOf(
            trackedBook(id = 1, author = "An Author", rating = null, imageUrl = "https://example.com/author.jpg"),
            trackedBook(id = 2, author = "An Author", rating = null, imageUrl = "https://example.com/author.jpg"),
            trackedBook(id = 3, author = "An Author", rating = null, imageUrl = "https://example.com/author.jpg"),
        )

        assertEquals(
            listOf("https://example.com/author.jpg"),
            library.contributorDirectory().imageUrls(),
        )
    }

    @Test
    fun persistsCoversAndContributorImagesTogether() {
        val library = listOf(
            trackedBook(
                id = 1,
                author = "An Author",
                rating = null,
                imageUrl = "https://example.com/author.jpg",
                coverUrl = "https://example.com/cover.jpg",
            ),
            trackedBook(id = 2, author = "An Author", rating = null, coverUrl = "   "),
        )

        val urls = library.persistableImageUrls()

        assertTrue("https://example.com/cover.jpg" in urls)
        assertTrue("https://example.com/author.jpg" in urls)
        assertEquals(urls.distinct(), urls)
        assertTrue(urls.none { it.isBlank() })
    }

    @Test
    fun theEmptyDirectoryAnswersWithoutFailing() {
        assertEquals(ContributorStats(), ContributorDirectory.Empty.stats(MediaCreditRole.Author, "Anyone"))
        assertNull(ContributorDirectory.Empty.imageUrl(MediaCreditRole.Author, "Anyone"))
        assertTrue(ContributorDirectory.Empty.imageUrls().isEmpty())
    }

    private fun trackedBook(
        id: Long,
        author: String,
        rating: Int?,
        imageUrl: String? = null,
        coverUrl: String? = null,
    ): TrackedMedia = TrackedMedia(
        item = MediaItem(id = id, type = MediaType.Book, title = "Book $id", coverUrl = coverUrl),
        sessions = rating?.let {
            listOf(
                TrackingSession(
                    id = id,
                    mediaItemId = id,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    rating = it,
                ),
            )
        } ?: emptyList(),
        credits = listOf(
            MediaCredit(
                personName = author,
                roleType = MediaCreditRole.Author,
                personImageUrl = imageUrl,
            ),
        ),
    )

    private fun movie(
        id: Long,
        name: String,
        role: MediaCreditRole,
        imageUrl: String? = null,
    ): TrackedMedia = credited(id, MediaType.Movie, name, role, imageUrl)

    private fun game(
        id: Long,
        name: String,
        role: MediaCreditRole,
        imageUrl: String? = null,
    ): TrackedMedia = credited(id, MediaType.Game, name, role, imageUrl)

    private fun credited(
        id: Long,
        type: MediaType,
        name: String,
        role: MediaCreditRole,
        imageUrl: String?,
    ): TrackedMedia = TrackedMedia(
        item = MediaItem(id = id, type = type, title = "Item $id"),
        sessions = emptyList(),
        credits = listOf(
            MediaCredit(personName = name, roleType = role, personImageUrl = imageUrl),
        ),
    )
}
