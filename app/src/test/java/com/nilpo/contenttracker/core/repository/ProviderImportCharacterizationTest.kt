package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProviderImportCharacterizationTest {
    @Test
    fun `MAL XML fixture normalizes every supported tracking field`() {
        val rows = parseMyAnimeListXml(fixture("myanimelist-export.xml"))

        assertEquals(5, rows.size)

        val completed = rows[0]
        assertEquals(5114, completed.malId)
        assertEquals("Fullmetal Alchemist & Brotherhood", completed.title)
        assertEquals("TV", completed.seriesType)
        assertEquals(64, completed.episodeTotal)
        assertEquals(64, completed.watchedEpisodes)
        assertEquals(LocalDate.of(2020, 1, 2), completed.startedAt)
        assertEquals(LocalDate.of(2020, 3, 4), completed.finishedAt)
        assertEquals(10, completed.rating)
        assertEquals(TrackingStatus.Completed, completed.status)
        assertEquals("First line\nSecond line", completed.notes)
        assertEquals(listOf("favorites", "adventure", "rewatch"), completed.tags)

        val completedRequest = completed.toAddTrackedMediaRequest()
        assertEquals(MediaType.Anime, completedRequest.type)
        assertEquals(64, completedRequest.initialProgress)
        assertEquals(MetadataSource.Jikan, completedRequest.metadataSource)
        assertEquals("5114", completedRequest.metadataExternalId)
        assertEquals(5114, completedRequest.malId)

        val watching = rows[1]
        assertEquals("Sōsō no Frieren", watching.title)
        assertEquals(TrackingStatus.InProgress, watching.status)
        assertEquals(12, watching.toAddTrackedMediaRequest().initialProgress)
        assertNull(watching.rating)
        assertNull(watching.finishedAt)

        val archiveOnly = rows[2]
        assertNull(archiveOnly.malId)
        assertNull(archiveOnly.episodeTotal)
        assertEquals(0, archiveOnly.watchedEpisodes)
        assertEquals(10, archiveOnly.rating)
        assertEquals(TrackingStatus.Paused, archiveOnly.status)
        assertNull(archiveOnly.startedAt)
        assertNull(archiveOnly.toAddTrackedMediaRequest().metadataSource)

        assertEquals(TrackingStatus.Dropped, rows[3].status)
        assertEquals(TrackingStatus.Planned, rows[4].status)
    }

    @Test
    fun `MAL XML rejects a doctype instead of expanding an external entity`() {
        val maliciousXml = """
            <?xml version="1.0"?>
            <!DOCTYPE myanimelist [<!ENTITY xxe SYSTEM "file:///should-not-be-read">]>
            <myanimelist><anime><series_title>&xxe;</series_title></anime></myanimelist>
        """.trimIndent()

        assertThrows(Exception::class.java) { parseMyAnimeListXml(maliciousXml) }
    }

    @Test
    fun `IMDb fixture handles BOM CRLF quoted fields multiline cells and unsupported types`() {
        val csv = "\uFEFF" + fixture("imdb-export.csv").replace("\n", "\r\n")
        val rows = parseImdbCsv(csv)

        assertEquals(4, rows.size)

        val movie = rows[0]
        assertEquals(MediaType.Movie, movie.type)
        assertEquals("Inception, The Dream", movie.title)
        assertEquals("Inception", movie.originalTitle)
        assertEquals("tt1375666", movie.imdbId)
        assertEquals(148, movie.runtimeMinutes)
        assertEquals(2_700_001, movie.imdbVoteCount)
        assertEquals(listOf("Action", "Sci-Fi"), movie.genres)

        val movieRequest = movie.toAddTrackedMediaRequest()
        assertEquals(TrackingStatus.Completed, movieRequest.initialStatus)
        assertEquals(148, movieRequest.initialProgress)
        assertEquals(9, movieRequest.initialRating)
        assertEquals(LocalDate.of(2024, 1, 31), movieRequest.initialFinishedAt)
        assertEquals(MetadataSource.Imdb, movieRequest.metadataSource)
        assertEquals(ExternalRatingSource.Imdb, movieRequest.externalRatings.single().source)

        val miniSeries = rows[1]
        assertEquals(MediaType.TvShow, miniSeries.type)
        assertEquals(TrackingStatus.Planned, miniSeries.toAddTrackedMediaRequest().initialStatus)
        assertEquals("https://www.imdb.com/title/tt10048342/", miniSeries.sourceUrl)

        val episode = rows[2]
        assertEquals("An \"Escaped\" Episode", episode.title)
        assertEquals(MediaType.TvShow, episode.type)
        // This ambiguous fixture pins the parser's documented month/day-first order.
        assertEquals(LocalDate.of(2023, 12, 5), episode.dateRated)

        assertNull(rows[3].type)
    }

    @Test
    fun `StoryGraph fixture preserves reviews ownership dates formats and rating conversion`() {
        val rows = parseStoryGraphCsv(fixture("storygraph-export.csv"))

        assertEquals(4, rows.size)

        val completed = rows[0]
        assertEquals(listOf("Ursula K. Le Guin"), completed.authors)
        assertEquals("9780441478125", completed.isbnOrUid)
        assertEquals(TrackingStatus.Completed, completed.readStatus)
        assertEquals(LocalDate.of(2023, 2, 4), completed.lastDateRead)
        assertEquals(9, completed.rating)
        assertTrue(completed.isOwned)

        val completedRequest = completed.toAddTrackedMediaRequest()
        assertEquals(MediaType.Book, completedRequest.type)
        assertEquals(ConsumptionPlatformType.Physical, completedRequest.platformType)
        assertEquals(LocalDate.of(2023, 2, 4), completedRequest.initialFinishedAt)
        assertEquals(MetadataSource.StoryGraph, completedRequest.metadataSource)

        val reading = rows[1]
        assertEquals(TrackingStatus.InProgress, reading.readStatus)
        assertEquals("First line\nSecond line with \"quotes\".", reading.review)
        assertEquals(LocalDate.of(2024, 3, 1), reading.toAddTrackedMediaRequest().initialStartedAt)
        assertFalse(reading.isOwned)

        val dropped = rows[2]
        assertEquals(TrackingStatus.Dropped, dropped.readStatus)
        assertEquals(LocalDate.of(2022, 3, 5), dropped.lastDateRead)
        assertEquals(ConsumptionPlatformType.DigitalStore, dropped.toAddTrackedMediaRequest().platformType)

        val planned = rows[3]
        assertEquals(TrackingStatus.Planned, planned.readStatus)
        assertNull(planned.dateAdded)
        assertNull(planned.rating)
    }

    @Test
    fun `provider import planning is idempotent by stable source identity`() {
        val mal = parseMyAnimeListXml(fixture("myanimelist-export.xml")).first()
        assertIdempotent(listOf(mal, mal.copy(title = "Provider title changed")), MyAnimeListImportItem::myAnimeListDuplicateKey)

        val imdb = parseImdbCsv(fixture("imdb-export.csv")).first()
        assertIdempotent(listOf(imdb, imdb.copy(title = "Provider title changed")), ImdbCsvItem::importDuplicateKey)

        val storyGraph = parseStoryGraphCsv(fixture("storygraph-export.csv")).first()
        assertIdempotent(
            listOf(storyGraph, storyGraph.copy(title = "Provider title changed")),
            StoryGraphCsvItem::storyGraphDuplicateKey,
        )
    }

    @Test
    fun `shared import plan keeps preview counts and inserted media ids aligned`() {
        val plan = planProviderImport(
            rows = listOf("existing", "unsupported", "new", "new"),
            existingKeys = setOf("existing"),
            duplicateKey = { row -> row },
            isSupported = { row -> row != "unsupported" },
        )

        assertEquals(
            ProviderImportPreview(
                totalRows = 4,
                importableRows = 1,
                skippedDuplicateRows = 2,
                unsupportedRows = 1,
            ),
            plan.toPreview(),
        )
        assertEquals(
            ProviderImportResult(
                importedRows = 1,
                skippedDuplicateRows = 2,
                unsupportedRows = 1,
                importedMediaItemIds = listOf(91L),
            ),
            plan.toResult(listOf(91L)),
        )
    }

    @Test
    fun `provider import origin cannot notify outbound MAL sync`() = runBlocking {
        val notifiedIds = mutableListOf<Long>()
        val callback: suspend (Long) -> Unit = { mediaItemId -> notifiedIds += mediaItemId }

        MediaWriteOrigin.ProviderImport.notifyOutboundMalSync(41, callback)
        MediaWriteOrigin.UserAction.notifyOutboundMalSync(42, callback)

        assertEquals(listOf(42L), notifiedIds)
    }

    @Test
    fun `exact ISBN match prefers the provider that supplies a page count`() {
        val openLibrary = MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = "/works/OL1W",
            mediaType = MediaType.Book,
            title = "Book",
            progressTotal = null,
        )
        val googleBooks = MetadataSuggestion(
            source = MetadataSource.GoogleBooks,
            externalId = "volume-1",
            mediaType = MediaType.Book,
            title = "Book",
            progressTotal = 432,
        )

        assertEquals(googleBooks, chooseExactBookMatch(openLibrary, googleBooks))
        assertEquals(
            openLibrary.copy(progressTotal = 410),
            chooseExactBookMatch(openLibrary.copy(progressTotal = 410), googleBooks),
        )
        assertEquals(openLibrary, chooseExactBookMatch(openLibrary, null))
    }

    private fun <T> assertIdempotent(rows: List<T>, duplicateKey: (T) -> String) {
        val first = planProviderImport(
            rows = rows,
            existingKeys = emptySet(),
            duplicateKey = duplicateKey,
            isSupported = { true },
        )
        assertEquals(1, first.importable.size)
        assertEquals(1, first.skippedDuplicateRows)

        val second = planProviderImport(
            rows = rows,
            existingKeys = first.importable.map(duplicateKey),
            duplicateKey = duplicateKey,
            isSupported = { true },
        )
        assertTrue(second.importable.isEmpty())
        assertEquals(2, second.skippedDuplicateRows)
    }

    private fun fixture(name: String): String {
        val resource = requireNotNull(javaClass.classLoader?.getResource("imports/$name")) {
            "Missing import fixture: $name"
        }
        return resource.readText(Charsets.UTF_8)
    }
}
