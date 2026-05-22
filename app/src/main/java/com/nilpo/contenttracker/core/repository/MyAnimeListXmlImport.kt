package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory

data class MyAnimeListXmlPreview(
    val totalRows: Int,
    val importableRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
)

data class MyAnimeListXmlImportResult(
    val importedRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
)

internal data class MyAnimeListXmlItem(
    val malId: String?,
    val title: String,
    val seriesType: String?,
    val episodeTotal: Int?,
    val watchedEpisodes: Int,
    val startedAt: LocalDate?,
    val finishedAt: LocalDate?,
    val rating: Int?,
    val status: TrackingStatus,
    val notes: String?,
    val tags: List<String>,
)

internal fun parseMyAnimeListXml(xml: String): List<MyAnimeListXmlItem> {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isExpandEntityReferences = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }
    val document = factory.newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    val root = document.documentElement
    if (root.nodeName != "myanimelist") {
        throw IllegalArgumentException("Not a MyAnimeList XML export")
    }

    val animeNodes = root.getElementsByTagName("anime")
    return List(animeNodes.length) { index -> animeNodes.item(index) as Element }
        .mapNotNull { element ->
            val title = element.text("series_title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MyAnimeListXmlItem(
                malId = element.text("series_animedb_id").takeIf { it.isNotBlank() && it != "0" },
                title = title,
                seriesType = element.text("series_type").takeIf { it.isNotBlank() },
                episodeTotal = element.text("series_episodes").toIntOrNull()?.takeIf { it > 0 },
                watchedEpisodes = element.text("my_watched_episodes").toIntOrNull()?.coerceAtLeast(0) ?: 0,
                startedAt = element.text("my_start_date").toMalDateOrNull(),
                finishedAt = element.text("my_finish_date").toMalDateOrNull(),
                rating = element.text("my_score").toIntOrNull()?.takeIf { it > 0 }?.coerceIn(1, 10),
                status = element.text("my_status").toMalStatus(),
                notes = element.text("my_comments").takeIf { it.isNotBlank() },
                tags = element.text("my_tags").splitMalList(),
            )
        }
}

internal fun MyAnimeListXmlItem.toAddTrackedMediaRequest(): AddTrackedMediaRequest {
    val completedProgress = if (status == TrackingStatus.Completed) {
        episodeTotal ?: watchedEpisodes
    } else {
        watchedEpisodes
    }
    return AddTrackedMediaRequest(
        type = MediaType.Anime,
        title = title,
        progressTotal = episodeTotal,
        initialStatus = status,
        initialProgress = completedProgress,
        initialRating = rating,
        initialNotes = notes,
        initialStartedAt = startedAt,
        initialFinishedAt = finishedAt,
        isOwned = false,
        ownershipType = OwnershipType.None,
        platformName = seriesType,
        platformType = ConsumptionPlatformType.Other,
        genres = tags,
        sourceUrl = malId?.let { "https://myanimelist.net/anime/$it" },
        metadataSource = malId?.let { MetadataSource.Jikan },
        metadataExternalId = malId,
    )
}

private fun Element.text(tagName: String): String {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) {
        return ""
    }
    return nodes.item(0)?.textContent?.trim().orEmpty()
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
