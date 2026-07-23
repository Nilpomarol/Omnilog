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
    val sourceRowNumber: Int,
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

internal fun parseImdbCsv(csv: String): List<ImdbCsvItem> = parseImdbCsvWithReport(csv).rows

internal fun parseImdbCsvWithReport(csv: String): ProviderCsvParseResult<ImdbCsvItem> {
    val table = parseProviderCsvTable(csv)
    if (table.isEmpty()) {
        throw ProviderCsvValidationException(ProviderCsvValidationIssue.EmptyFile)
    }

    val headers = table.first().map { it.normalizedHeader() }
    val missingColumns = buildList {
        if ("title" !in headers) add("Title")
        if ("title type" !in headers) add("Title Type")
        if (headers.none { it == "const" || it == "url" }) add("Const o URL")
    }
    if (missingColumns.isNotEmpty()) {
        throw ProviderCsvValidationException(
            issue = ProviderCsvValidationIssue.MissingRequiredColumns,
            missingColumns = missingColumns,
        )
    }
    if (table.size == 1) throw ProviderCsvValidationException(ProviderCsvValidationIssue.NoDataRows)

    var invalidRows = 0
    val rejectedRows = mutableListOf<ProviderRejectedRow>()
    val items = table.drop(1).mapIndexedNotNull { rowIndex, row ->
        val values = headers.mapIndexedNotNull { index, header ->
            header to row.getOrNull(index).orEmpty().trim()
        }.toMap()
        val title = values.value("title").takeIf { it.isNotBlank() } ?: run {
            invalidRows++
            if (rejectedRows.size < MaxRejectedSamplesPerReason) {
                rejectedRows += ProviderRejectedRow(
                    rowNumber = rowIndex + 2,
                    label = null,
                    reason = ProviderRejectedReason.MissingTitle,
                )
            }
            return@mapIndexedNotNull null
        }
        val type = values.value("title type").toMediaType()
        val imdbId = values.value("const").takeIf { it.isNotBlank() }
        val sourceUrl = values.value("url").takeIf { it.isNotBlank() }
            ?: imdbId?.let { "https://www.imdb.com/title/$it/" }

        ImdbCsvItem(
            sourceRowNumber = rowIndex + 2,
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
    if (items.isEmpty()) throw ProviderCsvValidationException(ProviderCsvValidationIssue.NoUsableRows)
    return ProviderCsvParseResult(
        rows = items,
        totalRows = table.size - 1,
        invalidRows = invalidRows,
        rejectedRows = rejectedRows,
    )
}

internal fun ImdbCsvItem.toAddTrackedMediaRequest() =
    com.nilpo.contenttracker.core.model.AddTrackedMediaRequest(
        type = requireNotNull(type),
        title = title,
        // IMDb exports runtime minutes for both films and series. Omnilog tracks films in minutes,
        // but TV in episodes, so a series must wait for TMDB to supply the episode count.
        progressTotal = runtimeMinutes.takeIf { type == MediaType.Movie },
        initialStatus = if (userRating != null || dateRated != null) {
            TrackingStatus.Completed
        } else {
            TrackingStatus.Planned
        },
        initialProgress = if (
            type == MediaType.Movie && (userRating != null || dateRated != null)
        ) {
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

private fun String.normalizedHeader(): String = trim()
    .removePrefix("\uFEFF")
    .lowercase()

private fun Map<String, String>.value(header: String): String = this[header].orEmpty()

private fun String.toMediaType(): MediaType? {
    return when (lowercase()) {
        "movie", "tvmovie", "tv movie", "tvspecial", "tv special", "video", "short" -> MediaType.Movie
        "tvseries", "tv series", "tvminiseries", "tv mini series" -> MediaType.TvShow
        // An IMDb episode ID resolves to TMDB's episode result, not a TV-series result. Importing it
        // as a series would create the wrong kind of library item and mix runtime minutes with episodes.
        "tvepisode", "tv episode" -> null
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
