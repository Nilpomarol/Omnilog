package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

typealias ImdbCsvPreview = ProviderImportPreview

typealias ImdbCsvImportResult = ProviderImportResult

internal data class ImdbCsvItem(
    val type: MediaType?,
    val title: String,
    val originalTitle: String?,
    val releaseYear: Int?,
    val runtimeMinutes: Int?,
    val imdbId: String?,
    val sourceUrl: String?,
    val userRating: Int?,
    val dateRated: LocalDate?,
    val imdbRating: Double?,
    val imdbVoteCount: Int?,
    val genres: List<String>,
    val creators: List<String>,
)

internal fun parseImdbCsv(csv: String): List<ImdbCsvItem> {
    val table = parseCsvTable(csv)
    if (table.isEmpty()) {
        return emptyList()
    }

    val headers = table.first().map { it.normalizedHeader() }
    if (headers.none { it == "title" } || headers.none { it == "const" || it == "url" }) {
        throw IllegalArgumentException("Not an IMDb CSV export")
    }

    return table.drop(1).mapNotNull { row ->
        val values = headers.mapIndexedNotNull { index, header ->
            header to row.getOrNull(index).orEmpty().trim()
        }.toMap()
        val title = values.value("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val type = values.value("title type").toMediaType()
        val imdbId = values.value("const").takeIf { it.isNotBlank() }
        val sourceUrl = values.value("url").takeIf { it.isNotBlank() }
            ?: imdbId?.let { "https://www.imdb.com/title/$it/" }

        ImdbCsvItem(
            type = type,
            title = title,
            originalTitle = values.value("original title").takeIf { it.isNotBlank() && it != title },
            releaseYear = values.value("year").toIntOrNull(),
            runtimeMinutes = values.value("runtime (mins)").toIntOrNull()?.coerceAtLeast(0),
            imdbId = imdbId,
            sourceUrl = sourceUrl,
            userRating = values.value("your rating").toIntOrNull()?.coerceIn(1, 10),
            dateRated = values.value("date rated").toLocalDateOrNull(),
            imdbRating = values.value("imdb rating").toDoubleOrNull(),
            imdbVoteCount = values.value("num votes").toIntWithSeparatorsOrNull(),
            genres = values.value("genres").splitCsvList(),
            creators = values.value("directors").splitCsvList(),
        )
    }
}

internal fun ImdbCsvItem.toAddTrackedMediaRequest() =
    com.nilpo.contenttracker.core.model.AddTrackedMediaRequest(
        type = requireNotNull(type),
        title = title,
        progressTotal = runtimeMinutes,
        initialStatus = if (userRating != null || dateRated != null) {
            TrackingStatus.Completed
        } else {
            TrackingStatus.Planned
        },
        initialProgress = if (userRating != null || dateRated != null) {
            runtimeMinutes ?: 0
        } else {
            0
        },
        initialRating = userRating,
        initialFinishedAt = dateRated,
        isOwned = false,
        platformName = null,
        platformType = ConsumptionPlatformType.Other,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        genres = genres,
        creators = creators,
        sourceUrl = sourceUrl,
        externalRatings = imdbRating?.let { rating ->
            listOf(
                MetadataExternalRatingSuggestion(
                    source = ExternalRatingSource.Imdb,
                    score = rating,
                    maxScore = 10.0,
                    voteCount = imdbVoteCount,
                ),
            )
        } ?: emptyList(),
        metadataSource = imdbId?.let { MetadataSource.Imdb },
        metadataExternalId = imdbId,
    )

private fun parseCsvTable(csv: String): List<List<String>> {
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

private fun String.normalizedHeader(): String = trim()
    .removePrefix("\uFEFF")
    .lowercase()

private fun Map<String, String>.value(header: String): String = this[header].orEmpty()

private fun String.toMediaType(): MediaType? {
    return when (lowercase()) {
        "movie", "tvmovie", "tv movie", "tvspecial", "tv special", "video", "short" -> MediaType.Movie
        "tvseries", "tv series", "tvminiseries", "tv mini series", "tvepisode", "tv episode" -> MediaType.TvShow
        else -> null
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    if (isBlank()) {
        return null
    }
    return listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    ).firstNotNullOfOrNull { formatter ->
        try {
            LocalDate.parse(this, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun String.toIntWithSeparatorsOrNull(): Int? =
    replace(",", "")
        .trim()
        .toIntOrNull()

private fun String.splitCsvList(): List<String> {
    return split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
