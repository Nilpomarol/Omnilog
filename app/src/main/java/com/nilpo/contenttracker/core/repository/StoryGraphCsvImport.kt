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

typealias StoryGraphCsvPreview = ProviderImportPreview

typealias StoryGraphCsvImportResult = ProviderImportResult

internal data class StoryGraphCsvItem(
    val sourceRowNumber: Int,
    val title: String,
    val authors: List<String>,
    val isbnOrUid: String?,
    val format: String?,
    val readStatus: TrackingStatus,
    val dateAdded: LocalDate?,
    val lastDateRead: LocalDate?,
    val readPeriods: List<StoryGraphReadPeriod>,
    val readCount: Int,
    val rating: Int?,
    val review: String?,
    val tags: List<String>,
    val isOwned: Boolean,
)

internal data class StoryGraphReadPeriod(
    val startedAt: LocalDate?,
    val finishedAt: LocalDate?,
)

internal fun parseStoryGraphCsv(csv: String): List<StoryGraphCsvItem> = parseStoryGraphCsvWithReport(csv).rows

internal fun parseStoryGraphCsvWithReport(csv: String): ProviderCsvParseResult<StoryGraphCsvItem> {
    val table = parseProviderCsvTable(csv)
    if (table.isEmpty()) {
        throw ProviderCsvValidationException(ProviderCsvValidationIssue.EmptyFile)
    }

    val headers = table.first().map { it.normalizedStoryGraphHeader() }
    val missingColumns = buildList {
        if ("title" !in headers) add("Title")
        if ("authors" !in headers) add("Authors")
        if ("read status" !in headers) add("Read Status")
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
        val values = headers.mapIndexed { index, header ->
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
        val readCount = values.value("read count").toIntOrNull()?.coerceAtLeast(0) ?: 0
        val readPeriods = values.value("dates read").toStoryGraphReadPeriods()
        StoryGraphCsvItem(
            sourceRowNumber = rowIndex + 2,
            title = title,
            authors = values.value("authors").splitStoryGraphList(),
            isbnOrUid = values.value("isbn/uid").takeIf { it.isNotBlank() },
            format = values.value("format").takeIf { it.isNotBlank() },
            readStatus = values.value("read status").toStoryGraphStatus(),
            dateAdded = values.value("date added").toStoryGraphDateOrNull(),
            lastDateRead = values.value("last date read").toStoryGraphDateOrNull()
                ?: readPeriods.mapNotNull(StoryGraphReadPeriod::finishedAt).maxOrNull(),
            readPeriods = readPeriods,
            readCount = readCount,
            rating = values.value("star rating").toOmnilogRatingOrNull(),
            review = values.value("review").takeIf { it.isNotBlank() },
            tags = values.value("tags").splitStoryGraphList(),
            isOwned = values.value("owned?").equals("yes", ignoreCase = true),
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

internal fun StoryGraphCsvItem.toAddTrackedMediaRequest(): AddTrackedMediaRequest {
    return toAddTrackedMediaRequest(importedSessions().first())
}

internal fun StoryGraphCsvItem.toAddTrackedMediaRequest(
    session: ImportedTrackingSession,
): AddTrackedMediaRequest {
    return AddTrackedMediaRequest(
        type = MediaType.Book,
        title = title,
        progressTotal = null,
        initialStatus = session.status,
        initialProgress = 0,
        initialRating = session.rating,
        initialNotes = session.notes,
        initialStartedAt = session.startedAt,
        initialFinishedAt = session.finishedAt,
        isOwned = isOwned,
        platformName = format,
        platformType = format.toPlatformType(),
        tags = tags,
        creators = authors,
        metadataSource = MetadataSource.StoryGraph,
        metadataExternalId = isbnOrUid,
    )
}

/** Builds reading sessions without treating StoryGraph's library-added date as reading activity. */
internal fun StoryGraphCsvItem.importedSessions(): List<ImportedTrackingSession> {
    val knownPeriods = readPeriods.sortedWith(
        compareBy<StoryGraphReadPeriod> { it.finishedAt ?: LocalDate.MAX }
            .thenBy { it.startedAt ?: LocalDate.MAX },
    )

    val completedPeriods = if (readStatus == TrackingStatus.Completed) {
        knownPeriods
    } else {
        // For an unfinished row, Read Count describes earlier completed reads. Any remaining
        // date range belongs to the current attempt (for example, a DNF with Read Count 0).
        knownPeriods.take(readCount)
    }
    val currentPeriod = knownPeriods.drop(completedPeriods.size).lastOrNull()
    val completedSessionCount = if (readStatus == TrackingStatus.Completed) {
        maxOf(readCount, completedPeriods.size, 1)
    } else {
        maxOf(readCount, completedPeriods.size)
    }
    val completedSessions = buildList {
        repeat((completedSessionCount - completedPeriods.size).coerceAtLeast(0)) {
            add(ImportedTrackingSession(status = TrackingStatus.Completed))
        }
        completedPeriods.forEach { period ->
            add(
                ImportedTrackingSession(
                    status = TrackingStatus.Completed,
                    startedAt = period.startedAt,
                    finishedAt = period.finishedAt,
                ),
            )
        }
    }.toMutableList()

    if (readStatus == TrackingStatus.Completed && lastDateRead != null) {
        val lastIndex = completedSessions.lastIndex
        val latest = completedSessions[lastIndex]
        if (latest.finishedAt == null || lastDateRead > latest.finishedAt) {
            completedSessions[lastIndex] = latest.copy(finishedAt = lastDateRead)
        }
    }

    if (readStatus == TrackingStatus.Completed) {
        val lastIndex = completedSessions.lastIndex
        completedSessions[lastIndex] = completedSessions[lastIndex].copy(
            rating = rating,
            notes = review,
        )
        return completedSessions
    }

    val currentSession = ImportedTrackingSession(
        status = readStatus,
        startedAt = currentPeriod?.startedAt.takeIf {
            readStatus == TrackingStatus.InProgress ||
                readStatus == TrackingStatus.Paused ||
                readStatus == TrackingStatus.Dropped
        },
        finishedAt = if (readStatus == TrackingStatus.Dropped) {
            currentPeriod?.finishedAt ?: lastDateRead
        } else {
            null
        },
        rating = rating,
        notes = review,
    )
    return completedSessions + currentSession
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

private fun String.toStoryGraphReadPeriods(): List<StoryGraphReadPeriod> {
    return split(',', ';', '\n')
        .mapNotNull { entry ->
            val dates = StoryGraphDatePattern.findAll(entry)
                .mapNotNull { match -> match.value.toStoryGraphDateOrNull() }
                .toList()
            when {
                dates.size >= 2 -> StoryGraphReadPeriod(
                    startedAt = minOf(dates.first(), dates.last()),
                    finishedAt = maxOf(dates.first(), dates.last()),
                )
                dates.size == 1 -> StoryGraphReadPeriod(startedAt = null, finishedAt = dates.single())
                else -> null
            }
        }
}

private fun String.toOmnilogRatingOrNull(): Int? {
    val rating = toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
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

private val StoryGraphDatePattern = Regex("""\d{4}[/-]\d{1,2}[/-]\d{1,2}""")
