package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

data class StoryGraphCsvPreview(
    val totalRows: Int,
    val importableRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
)

data class StoryGraphCsvImportResult(
    val importedRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
)

internal data class StoryGraphCsvItem(
    val title: String,
    val authors: List<String>,
    val isbnOrUid: String?,
    val format: String?,
    val readStatus: TrackingStatus,
    val dateAdded: LocalDate?,
    val lastDateRead: LocalDate?,
    val readCount: Int,
    val rating: Int?,
    val review: String?,
    val tags: List<String>,
    val isOwned: Boolean,
)

internal fun parseStoryGraphCsv(csv: String): List<StoryGraphCsvItem> {
    val table = parseStoryGraphCsvTable(csv)
    if (table.isEmpty()) {
        return emptyList()
    }

    val headers = table.first().map { it.normalizedStoryGraphHeader() }
    if (headers.none { it == "title" } || headers.none { it == "authors" } || headers.none { it == "read status" }) {
        throw IllegalArgumentException("Not a StoryGraph CSV export")
    }

    return table.drop(1).mapNotNull { row ->
        val values = headers.mapIndexed { index, header ->
            header to row.getOrNull(index).orEmpty().trim()
        }.toMap()
        val title = values.value("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        StoryGraphCsvItem(
            title = title,
            authors = values.value("authors").splitStoryGraphList(),
            isbnOrUid = values.value("isbn/uid").takeIf { it.isNotBlank() },
            format = values.value("format").takeIf { it.isNotBlank() },
            readStatus = values.value("read status").toStoryGraphStatus(),
            dateAdded = values.value("date added").toStoryGraphDateOrNull(),
            lastDateRead = values.value("last date read").toStoryGraphDateOrNull()
                ?: values.value("dates read").lastStoryGraphDateOrNull(),
            readCount = values.value("read count").toIntOrNull() ?: 0,
            rating = values.value("star rating").toOmnilogRatingOrNull(),
            review = values.value("review").takeIf { it.isNotBlank() },
            tags = values.value("tags").splitStoryGraphList(),
            isOwned = values.value("owned?").equals("yes", ignoreCase = true),
        )
    }
}

internal fun StoryGraphCsvItem.toAddTrackedMediaRequest(): AddTrackedMediaRequest {
    return AddTrackedMediaRequest(
        type = MediaType.Book,
        title = title,
        progressTotal = null,
        initialStatus = readStatus,
        initialProgress = 0,
        initialRating = rating,
        initialNotes = review,
        initialStartedAt = dateAdded.takeIf { readStatus == TrackingStatus.InProgress },
        initialFinishedAt = lastDateRead,
        isOwned = isOwned,
        platformName = format,
        platformType = format.toPlatformType(),
        genres = tags,
        creators = authors,
        metadataSource = MetadataSource.StoryGraph,
        metadataExternalId = isbnOrUid,
    )
}

private fun parseStoryGraphCsvTable(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var index = 0
    var inQuotes = false

    while (index < csv.length) {
        val char = csv[index]
        when {
            char == '"' && inQuotes && csv.getOrNull(index + 1) == '"' -> {
                cell.append('"')
                index++
            }
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                row += cell.toString()
                cell.clear()
            }
            (char == '\n' || char == '\r') && !inQuotes -> {
                if (char == '\r' && csv.getOrNull(index + 1) == '\n') {
                    index++
                }
                row += cell.toString()
                cell.clear()
                if (row.any { it.isNotBlank() }) {
                    rows += row.toList()
                }
                row.clear()
            }
            else -> cell.append(char)
        }
        index++
    }

    row += cell.toString()
    if (row.any { it.isNotBlank() }) {
        rows += row.toList()
    }
    return rows
}

private fun String.normalizedStoryGraphHeader(): String = trim()
    .removePrefix("\uFEFF")
    .lowercase()

private fun Map<String, String>.value(header: String): String = this[header].orEmpty()

private fun String.toStoryGraphStatus(): TrackingStatus {
    return when (lowercase()) {
        "read" -> TrackingStatus.Completed
        "currently-reading" -> TrackingStatus.InProgress
        "paused" -> TrackingStatus.Paused
        "did-not-finish", "dnf" -> TrackingStatus.Dropped
        else -> TrackingStatus.Planned
    }
}

private fun String.toStoryGraphDateOrNull(): LocalDate? {
    if (isBlank()) {
        return null
    }
    return listOf(
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ISO_LOCAL_DATE,
    ).firstNotNullOfOrNull { formatter ->
        try {
            LocalDate.parse(this, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun String.lastStoryGraphDateOrNull(): LocalDate? {
    return split(',', ';', '\n')
        .mapNotNull { it.trim().toStoryGraphDateOrNull() }
        .maxOrNull()
}

private fun String.toOmnilogRatingOrNull(): Int? {
    val rating = toDoubleOrNull() ?: return null
    return (rating * 2.0).roundToInt().coerceIn(1, 10)
}

private fun String?.toPlatformType(): ConsumptionPlatformType {
    return when (this?.lowercase()) {
        "hardcover", "paperback" -> ConsumptionPlatformType.Physical
        "ebook" -> ConsumptionPlatformType.Ebook
        "audiobook", "digital" -> ConsumptionPlatformType.DigitalStore
        else -> ConsumptionPlatformType.Other
    }
}

private fun String.splitStoryGraphList(): List<String> {
    return split(',', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
