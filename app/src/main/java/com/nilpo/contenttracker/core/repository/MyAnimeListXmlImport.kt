package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

typealias MyAnimeListXmlPreview = ProviderImportPreview

typealias MyAnimeListXmlImportResult = ProviderImportResult

internal data class MyAnimeListXmlParseResult(
    val rows: List<MyAnimeListImportItem>,
    val totalRows: Int,
    val invalidRows: Int,
    val rejectedRows: List<ProviderRejectedRow>,
)

internal fun parseMyAnimeListXml(xml: String): List<MyAnimeListImportItem> =
    parseMyAnimeListXmlWithReport(xml).rows

internal fun parseMyAnimeListXmlWithReport(xml: String): MyAnimeListXmlParseResult {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isExpandEntityReferences = false
        setFeatureIfSupported("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
        setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
    }
    val document = try {
        factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    } catch (error: SAXException) {
        throw MalformedProviderXmlException("Malformed MyAnimeList XML", error)
    }
    val root = document.documentElement
    if (root.nodeName != "myanimelist") {
        throw IllegalArgumentException("Not a MyAnimeList XML export")
    }

    val animeNodes = root.getElementsByTagName("anime")
    var invalidRows = 0
    val rejectedRows = mutableListOf<ProviderRejectedRow>()
    val rows = List(animeNodes.length) { index -> animeNodes.item(index) as Element }
        .mapIndexedNotNull { index, element ->
            val title = element.text("series_title").takeIf { it.isNotBlank() } ?: run {
                invalidRows++
                if (rejectedRows.size < MaxRejectedSamplesPerReason) {
                    rejectedRows += ProviderRejectedRow(
                        rowNumber = index + 1,
                        label = null,
                        reason = ProviderRejectedReason.MissingTitle,
                    )
                }
                return@mapIndexedNotNull null
            }
            MyAnimeListImportItem(
                malId = element.text("series_animedb_id").toIntOrNull()?.takeIf { it > 0 },
                title = title,
                seriesType = element.text("series_type").takeIf { it.isNotBlank() },
                episodeTotal = element.text("series_episodes").toIntOrNull()?.takeIf { it > 0 },
                watchedEpisodes = element.text("my_watched_episodes").toIntOrNull()?.coerceAtLeast(0) ?: 0,
                startedAt = element.text("my_start_date").toMalDateOrNull(),
                finishedAt = element.text("my_finish_date").toMalDateOrNull(),
                rating = element.text("my_score").toIntOrNull()?.takeIf { it in 1..10 },
                status = element.text("my_status").toMalStatus(),
                notes = element.text("my_comments").takeIf { it.isNotBlank() },
                tags = element.text("my_tags").splitMalList(),
                completedRewatches = element.textFirst("my_times_watched", "my_times_rewatched")
                    .toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0,
                isRewatching = element.textFirst("my_rewatching", "my_is_rewatching").toMalBoolean(),
            )
        }
    return MyAnimeListXmlParseResult(
        rows = rows,
        totalRows = animeNodes.length,
        invalidRows = invalidRows,
        rejectedRows = rejectedRows,
    )
}

private fun DocumentBuilderFactory.setFeatureIfSupported(name: String, value: Boolean) {
    try {
        setFeature(name, value)
    } catch (_: ParserConfigurationException) {
        // Android XML parser implementations do not support every hardening feature.
    }
}

internal fun MyAnimeListImportItem.toAddTrackedMediaRequest(): AddTrackedMediaRequest {
    return toAddTrackedMediaRequest(importedSessions().last())
}

internal fun MyAnimeListImportItem.toAddTrackedMediaRequest(
    session: ImportedTrackingSession,
): AddTrackedMediaRequest {
    return AddTrackedMediaRequest(
        type = MediaType.Anime,
        title = title,
        progressTotal = episodeTotal,
        initialStatus = session.status,
        initialProgress = session.progressCurrent,
        initialRatingHalfPoints = session.rating?.let(RatingHalfPoints::fromWholePoints),
        initialNotes = session.notes,
        initialStartedAt = session.startedAt,
        initialFinishedAt = session.finishedAt,
        isOwned = false,
        platformName = seriesType,
        platformType = ConsumptionPlatformType.Other,
        tags = tags,
        sourceUrl = malId?.let { "https://myanimelist.net/anime/$it" },
        metadataSource = malId?.let { MetadataSource.Jikan },
        metadataExternalId = malId?.toString(),
        malId = malId,
    )
}

/** Reconstructs the repeat count MAL stores even though it does not provide dates for older runs. */
internal fun MyAnimeListImportItem.importedSessions(): List<ImportedTrackingSession> {
    val completedProgress = episodeTotal ?: watchedEpisodes
    val historicalCompletedCount = when {
        isRewatching -> completedRewatches + 1
        status == TrackingStatus.Completed -> completedRewatches
        completedRewatches > 0 -> completedRewatches + 1
        else -> 0
    }
    val currentStatus = if (isRewatching && status == TrackingStatus.Completed) {
        TrackingStatus.InProgress
    } else {
        status
    }
    val currentProgress = if (currentStatus == TrackingStatus.Completed) {
        completedProgress
    } else {
        watchedEpisodes
    }

    return List(historicalCompletedCount) {
        ImportedTrackingSession(
            status = TrackingStatus.Completed,
            progressCurrent = completedProgress,
        )
    } + ImportedTrackingSession(
        status = currentStatus,
        progressCurrent = currentProgress,
        startedAt = startedAt,
        finishedAt = finishedAt,
        rating = rating,
        notes = notes,
    )
}

private fun Element.text(tagName: String): String {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) {
        return ""
    }
    return nodes.item(0)?.textContent?.trim().orEmpty()
}

private fun Element.textFirst(vararg tagNames: String): String {
    return tagNames.firstNotNullOfOrNull { tagName -> text(tagName).takeIf { it.isNotBlank() } }.orEmpty()
}

private fun String.toMalBoolean(): Boolean = when (trim().lowercase()) {
    "1", "true", "yes" -> true
    else -> false
}

private fun String.toMalStatus(): TrackingStatus {
    return when (lowercase()) {
        "completed" -> TrackingStatus.Completed
        "watching" -> TrackingStatus.InProgress
        "on-hold" -> TrackingStatus.Paused
        "dropped" -> TrackingStatus.Dropped
        else -> TrackingStatus.Planned
    }
}

private fun String.toMalDateOrNull(): LocalDate? {
    if (isBlank() || this == "0000-00-00") {
        return null
    }
    return runCatching { LocalDate.parse(this) }.getOrNull()
}

private fun String.splitMalList(): List<String> {
    return split(',', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
