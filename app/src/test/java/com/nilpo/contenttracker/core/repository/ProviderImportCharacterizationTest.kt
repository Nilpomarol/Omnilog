package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.BookEditionMetadata
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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class ProviderImportCharacterizationTest {
    @Test
    fun `provider files decode UTF BOMs BOM-less UTF-16 and Windows-1252`() {
        val text = "Title,Authors\nAmélie,Jean-Pierre Jeunet"

        assertEquals(
            text,
            decodeProviderImportText(
                byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                    text.toByteArray(StandardCharsets.UTF_8),
            ),
        )
        assertEquals(
            text,
            decodeProviderImportText(
                byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
                    text.toByteArray(StandardCharsets.UTF_16LE),
            ),
        )
        assertEquals(
            text,
            decodeProviderImportText(
                byteArrayOf(0xFE.toByte(), 0xFF.toByte()) +
                    text.toByteArray(StandardCharsets.UTF_16BE),
            ),
        )
        assertEquals(text, decodeProviderImportText(text.toByteArray(StandardCharsets.UTF_16LE)))
        assertEquals(
            text,
            decodeProviderImportText(text.toByteArray(Charset.forName("windows-1252"))),
        )
    }

    @Test
    fun `provider files reject binary control characters`() {
        assertThrows(ProviderImportEncodingException::class.java) {
            decodeProviderImportText(byteArrayOf('A'.code.toByte(), 0, 1, 'B'.code.toByte()))
        }
    }

    @Test
    fun `shared CSV parser rejects unclosed quotes and inconsistent rows`() {
        val unclosed = assertThrows(MalformedProviderCsvException::class.java) {
            parseProviderCsvTable("Title,Authors\n\"Unfinished,Writer")
        }
        assertEquals(2, unclosed.rowNumber)
        val inconsistent = assertThrows(MalformedProviderCsvException::class.java) {
            parseProviderCsvTable("Title,Authors\nBook")
        }
        assertEquals(2, inconsistent.rowNumber)
    }

    @Test
    fun `IMDb validation distinguishes empty missing header-only and unusable exports`() {
        val empty = assertThrows(ProviderCsvValidationException::class.java) {
            parseImdbCsv("  \n")
        }
        assertEquals(ProviderCsvValidationIssue.EmptyFile, empty.issue)

        val missing = assertThrows(ProviderCsvValidationException::class.java) {
            parseImdbCsv("Title,Const\nAlien,tt0078748")
        }
        assertEquals(ProviderCsvValidationIssue.MissingRequiredColumns, missing.issue)
        assertEquals(listOf("Title Type"), missing.missingColumns)

        val headerOnly = assertThrows(ProviderCsvValidationException::class.java) {
            parseImdbCsv("Title,Title Type,Const")
        }
        assertEquals(ProviderCsvValidationIssue.NoDataRows, headerOnly.issue)

        val unusable = assertThrows(ProviderCsvValidationException::class.java) {
            parseImdbCsv("Title,Title Type,Const\n,Movie,tt0078748")
        }
        assertEquals(ProviderCsvValidationIssue.NoUsableRows, unusable.issue)
    }

    @Test
    fun `StoryGraph validation distinguishes wrong header header-only and unusable exports`() {
        val missing = assertThrows(ProviderCsvValidationException::class.java) {
            parseStoryGraphCsv("Title,Read Status\nA Book,read")
        }
        assertEquals(ProviderCsvValidationIssue.MissingRequiredColumns, missing.issue)
        assertEquals(listOf("Authors"), missing.missingColumns)

        val headerOnly = assertThrows(ProviderCsvValidationException::class.java) {
            parseStoryGraphCsv("Title,Authors,Read Status")
        }
        assertEquals(ProviderCsvValidationIssue.NoDataRows, headerOnly.issue)

        val unusable = assertThrows(ProviderCsvValidationException::class.java) {
            parseStoryGraphCsv("Title,Authors,Read Status\n,Writer,read")
        }
        assertEquals(ProviderCsvValidationIssue.NoUsableRows, unusable.issue)
    }

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
        assertEquals(2, completed.completedRewatches)
        assertFalse(completed.isRewatching)

        val completedSessions = completed.importedSessions()
        assertEquals(3, completedSessions.size)
        assertTrue(completedSessions.all { it.status == TrackingStatus.Completed })
        assertEquals(listOf(64, 64, 64), completedSessions.map { it.progressCurrent })
        assertNull(completedSessions[0].rating)
        assertEquals(10, completedSessions.last().rating)

        val completedRequest = completed.toAddTrackedMediaRequest()
        assertEquals(MediaType.Anime, completedRequest.type)
        assertEquals(64, completedRequest.initialProgress)
        assertEquals(MetadataSource.Jikan, completedRequest.metadataSource)
        assertEquals("5114", completedRequest.metadataExternalId)
        assertEquals(5114, completedRequest.malId)
        assertEquals(listOf("favorites", "adventure", "rewatch"), completedRequest.tags)
        assertTrue(completedRequest.genres.isEmpty())

        val watching = rows[1]
        assertEquals("Sōsō no Frieren", watching.title)
        assertEquals(TrackingStatus.InProgress, watching.status)
        assertEquals(12, watching.toAddTrackedMediaRequest().initialProgress)
        assertNull(watching.rating)
        assertNull(watching.finishedAt)
        assertEquals(1, watching.completedRewatches)
        assertTrue(watching.isRewatching)
        assertEquals(
            listOf(TrackingStatus.Completed, TrackingStatus.Completed, TrackingStatus.InProgress),
            watching.importedSessions().map { it.status },
        )

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

        assertThrows(MalformedProviderXmlException::class.java) { parseMyAnimeListXml(maliciousXml) }
    }

    @Test
    fun `MAL XML reports malformed documents and accepts decoded UTF-16 declarations`() {
        assertThrows(MalformedProviderXmlException::class.java) {
            parseMyAnimeListXml("<myanimelist><anime></myanimelist>")
        }

        val xml = """<?xml version="1.0" encoding="UTF-16"?>
            <myanimelist><anime><series_title>Frieren</series_title></anime></myanimelist>
        """.trimIndent()
        val decoded = decodeProviderImportText(
            byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + xml.toByteArray(StandardCharsets.UTF_16LE),
        )

        assertEquals("Frieren", parseMyAnimeListXml(decoded).single().title)
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
        assertNull(miniSeries.toAddTrackedMediaRequest().progressTotal)
        assertEquals("https://www.imdb.com/title/tt10048342/", miniSeries.sourceUrl)

        val completedMiniSeries = miniSeries.copy(userRating = 8)
            .toAddTrackedMediaRequest()
        assertEquals(TrackingStatus.Completed, completedMiniSeries.initialStatus)
        assertNull(completedMiniSeries.progressTotal)
        assertEquals(0, completedMiniSeries.initialProgress)

        val episode = rows[2]
        assertEquals("An \"Escaped\" Episode", episode.title)
        assertNull(episode.type)
        // This ambiguous fixture pins the parser's documented month/day-first order.
        assertEquals(LocalDate.of(2023, 12, 5), episode.dateRated)

        assertNull(rows[3].type)
    }

    @Test
    fun `IMDb preview report keeps source rows and every rejection category aligned`() {
        val parsed = parseImdbCsvWithReport(
            """Title,Title Type,Const
                Accepted movie,Movie,tt0000001
                ,Movie,tt0000002
                Unsupported episode,TV Episode,tt0000003
                Repeated movie,Movie,tt0000001
            """.trimIndent(),
        )
        val plan = planProviderImport(
            rows = parsed.rows,
            existingKeys = emptySet(),
            duplicateKey = ImdbCsvItem::importDuplicateKey,
            isSupported = { it.type != null },
            totalRows = parsed.totalRows,
            invalidRows = parsed.invalidRows,
            initialRejectedRows = parsed.rejectedRows,
            sourceRowNumber = ImdbCsvItem::sourceRowNumber,
            displayLabel = ImdbCsvItem::title,
        )
        val preview = plan.toPreview()

        assertEquals(4, preview.totalRows)
        assertEquals(1, preview.importableRows)
        assertEquals(1, preview.invalidRows)
        assertEquals(1, preview.unsupportedRows)
        assertEquals(1, preview.skippedDuplicateRows)
        assertEquals(
            listOf(
                ProviderRejectedRow(3, null, ProviderRejectedReason.MissingTitle),
                ProviderRejectedRow(4, "Unsupported episode", ProviderRejectedReason.UnsupportedType),
                ProviderRejectedRow(5, "Repeated movie", ProviderRejectedReason.Duplicate),
            ),
            preview.rejectedRows,
        )
        assertEquals(
            listOf(
                ProviderRejectedReason.MissingTitle,
                ProviderRejectedReason.UnsupportedType,
                ProviderRejectedReason.Duplicate,
            ),
            preview.rejectedGroups().map(ProviderRejectedGroup::reason),
        )
        val result = plan.toResult(importedMediaItemIds = listOf(91L))
        assertEquals(preview.importableRows, result.importedRows)
        assertEquals(preview.invalidRows, result.invalidRows)
        assertEquals(preview.unsupportedRows, result.unsupportedRows)
        assertEquals(preview.skippedDuplicateRows, result.skippedDuplicateRows)
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
        assertEquals(listOf("science fiction", "classics"), completedRequest.tags)
        assertTrue(completedRequest.genres.isEmpty())

        val reading = rows[1]
        assertEquals(TrackingStatus.InProgress, reading.readStatus)
        assertEquals("First line\nSecond line with \"quotes\".", reading.review)
        assertNull(reading.toAddTrackedMediaRequest().initialStartedAt)
        assertFalse(reading.isOwned)

        val dropped = rows[2]
        assertEquals(TrackingStatus.Dropped, dropped.readStatus)
        assertEquals(LocalDate.of(2022, 3, 5), dropped.lastDateRead)
        assertEquals(
            listOf(
                StoryGraphReadPeriod(
                    startedAt = LocalDate.of(2022, 2, 1),
                    finishedAt = LocalDate.of(2022, 3, 5),
                ),
            ),
            dropped.readPeriods,
        )
        assertEquals(1, dropped.importedSessions().size)
        assertEquals(TrackingStatus.Dropped, dropped.importedSessions().single().status)
        assertEquals(LocalDate.of(2022, 3, 5), dropped.importedSessions().single().finishedAt)
        assertEquals(ConsumptionPlatformType.DigitalStore, dropped.toAddTrackedMediaRequest().platformType)

        val planned = rows[3]
        assertEquals(TrackingStatus.Planned, planned.readStatus)
        assertNull(planned.dateAdded)
        assertNull(planned.rating)
    }

    @Test
    fun `StoryGraph rereads become distinct chronological sessions`() {
        val item = parseStoryGraphCsv(
            """Title,Authors,Read Status,Date Added,Last Date Read,Dates Read,Read Count,Star Rating,Review
                A Reread,Writer,read,2019/01/01,2024/03/12,"2020/02/01-2020/02/10, 2024/03/01-2024/03/12",2,4.5,Latest review
            """.trimIndent(),
        ).single()

        val sessions = item.importedSessions()

        assertEquals(2, sessions.size)
        assertEquals(listOf(TrackingStatus.Completed, TrackingStatus.Completed), sessions.map { it.status })
        assertEquals(LocalDate.of(2020, 2, 1), sessions[0].startedAt)
        assertEquals(LocalDate.of(2020, 2, 10), sessions[0].finishedAt)
        assertEquals(LocalDate.of(2024, 3, 1), sessions[1].startedAt)
        assertEquals(LocalDate.of(2024, 3, 12), sessions[1].finishedAt)
        assertNull(sessions[0].rating)
        assertEquals(9, sessions[1].rating)
        assertEquals("Latest review", sessions[1].notes)
        assertFalse(sessions.any { it.startedAt == item.dateAdded })
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
    fun `StoryGraph duplicate keys canonicalize ISBNs UIDs and author order`() {
        val original = parseStoryGraphCsv(fixture("storygraph-export.csv")).first()
        val formattedIsbn = original.copy(isbnOrUid = "978-0-441-47812-5")
        assertEquals(original.storyGraphDuplicateKey(), formattedIsbn.storyGraphDuplicateKey())

        val uid = original.copy(isbnOrUid = " SG-9780441478125 ")
        assertEquals("storygraph:sg-9780441478125", uid.storyGraphDuplicateKey())

        val withoutId = original.copy(
            isbnOrUid = null,
            title = "Cent anys de solitud",
            authors = listOf("Gabriel García Márquez", "Translator Name"),
        )
        val reordered = withoutId.copy(
            title = "CENT ANYS DE SOLITUD",
            authors = listOf("translator name", "Gabriel Garcia Marquez"),
        )
        assertEquals(withoutId.storyGraphDuplicateKey(), reordered.storyGraphDuplicateKey())

        val plan = planProviderImport(
            rows = listOf(original, formattedIsbn),
            existingKeys = emptySet(),
            duplicateKey = StoryGraphCsvItem::storyGraphDuplicateKey,
            isSupported = { true },
        )
        assertEquals(1, plan.importable.size)
        assertEquals(1, plan.skippedDuplicateRows)
    }

    @Test
    fun `IMDb duplicate keys canonicalize title ids`() {
        val original = parseImdbCsv(fixture("imdb-export.csv")).first()
        val differentlyFormatted = original.copy(imdbId = " TT1375666 ")

        assertEquals("imdb:tt1375666", original.importDuplicateKey())
        assertEquals(original.importDuplicateKey(), differentlyFormatted.importDuplicateKey())

        val plan = planProviderImport(
            rows = listOf(original, differentlyFormatted),
            existingKeys = emptySet(),
            duplicateKey = ImdbCsvItem::importDuplicateKey,
            isSupported = { true },
        )
        assertEquals(1, plan.importable.size)
        assertEquals(1, plan.skippedDuplicateRows)
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
    fun `empty preview reason distinguishes duplicates unsupported and mixed rows`() {
        assertEquals(
            ProviderNoImportableReason.DuplicatesOnly,
            ProviderImportPreview(2, 0, 2, 0).noImportableReason(),
        )
        assertEquals(
            ProviderNoImportableReason.UnsupportedOnly,
            ProviderImportPreview(2, 0, 0, 2).noImportableReason(),
        )
        assertEquals(
            ProviderNoImportableReason.Mixed,
            ProviderImportPreview(2, 0, 1, 1).noImportableReason(),
        )
        assertNull(ProviderImportPreview(2, 1, 1, 0).noImportableReason())
    }

    @Test
    fun `rejected row samples are bounded without changing aggregate counts`() {
        val plan = planProviderImport(
            rows = listOf("one", "two", "three", "four"),
            existingKeys = setOf("one", "two", "three", "four"),
            duplicateKey = { it },
            isSupported = { true },
            sourceRowNumber = { row -> listOf("one", "two", "three", "four").indexOf(row) + 2 },
            displayLabel = { it },
        )

        assertEquals(4, plan.skippedDuplicateRows)
        assertEquals(MaxRejectedSamplesPerReason, plan.rejectedRows.size)
        assertEquals(listOf("one", "two"), plan.rejectedRows.map(ProviderRejectedRow::label))
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
    fun `exact ISBN match keeps edition identity and merges complementary provider fields`() {
        val openLibrary = MetadataSuggestion(
            source = MetadataSource.OpenLibrary,
            externalId = "/books/OL1M",
            mediaType = MediaType.Book,
            title = "Book",
            progressTotal = null,
            creators = listOf("Author One"),
            identifiers = listOf("978-0-261-10357-3"),
            bookEdition = BookEditionMetadata(
                externalId = "/books/OL1M",
                isbn = "9780261103573",
                publisher = "Primary Press",
            ),
        )
        val googleBooks = MetadataSuggestion(
            source = MetadataSource.GoogleBooks,
            externalId = "volume-1",
            mediaType = MediaType.Book,
            title = "Book",
            progressTotal = 432,
            synopsis = "A detailed synopsis.",
            genres = listOf("Fantasy"),
            publishers = listOf("Secondary Press"),
            identifiers = listOf("9780261103573", "0261103571"),
            bookEdition = BookEditionMetadata(
                externalId = "volume-1",
                isbn = "9780261103573",
                pageCount = 432,
                format = "BOOK",
            ),
        )

        val merged = requireNotNull(mergeExactBookMatches(openLibrary, googleBooks))

        assertEquals(MetadataSource.OpenLibrary, merged.source)
        assertEquals("/books/OL1M", merged.externalId)
        assertEquals("/books/OL1M", merged.bookEdition?.externalId)
        assertEquals(432, merged.progressTotal)
        assertEquals(432, merged.bookEdition?.pageCount)
        assertEquals("BOOK", merged.bookEdition?.format)
        assertEquals("A detailed synopsis.", merged.synopsis)
        assertEquals(listOf("Fantasy"), merged.genres)
        assertEquals(listOf("Primary Press", "Secondary Press"), merged.publishers)
        assertEquals(listOf("9780261103573", "0261103571"), merged.identifiers)
        assertEquals(openLibrary, mergeExactBookMatches(openLibrary, null))
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
