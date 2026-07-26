package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

internal data class ImportedTrackingSession(
    val status: TrackingStatus,
    val progressCurrent: Int = 0,
    val startedAt: LocalDate? = null,
    val finishedAt: LocalDate? = null,
    val rating: Int? = null,
    val notes: String? = null,
)

/** Counts shared by every additive provider-import preview. */
data class ProviderImportPreview(
    val totalRows: Int,
    val importableRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
    val invalidRows: Int = 0,
    val rejectedRows: List<ProviderRejectedRow> = emptyList(),
)

enum class ProviderRejectedReason {
    Duplicate,
    UnsupportedType,
    MissingTitle,
}

data class ProviderRejectedRow(
    val rowNumber: Int,
    val label: String?,
    val reason: ProviderRejectedReason,
)

internal data class ProviderRejectedGroup(
    val reason: ProviderRejectedReason,
    val count: Int,
    val samples: List<ProviderRejectedRow>,
)

internal fun ProviderImportPreview.rejectedGroups(): List<ProviderRejectedGroup> =
    listOf(
        ProviderRejectedReason.MissingTitle to invalidRows,
        ProviderRejectedReason.UnsupportedType to unsupportedRows,
        ProviderRejectedReason.Duplicate to skippedDuplicateRows,
    ).mapNotNull { (reason, count) ->
        count.takeIf { it > 0 }?.let {
            ProviderRejectedGroup(
                reason = reason,
                count = count,
                samples = rejectedRows.filter { row -> row.reason == reason },
            )
        }
    }

internal data class ProviderCsvParseResult<T>(
    val rows: List<T>,
    val totalRows: Int,
    val invalidRows: Int,
    val rejectedRows: List<ProviderRejectedRow>,
)

internal enum class ProviderNoImportableReason {
    DuplicatesOnly,
    UnsupportedOnly,
    Mixed,
}

internal fun ProviderImportPreview.noImportableReason(): ProviderNoImportableReason? = when {
    importableRows > 0 || totalRows <= 0 -> null
    skippedDuplicateRows == totalRows -> ProviderNoImportableReason.DuplicatesOnly
    unsupportedRows == totalRows -> ProviderNoImportableReason.UnsupportedOnly
    else -> ProviderNoImportableReason.Mixed
}

/** Result shared by provider imports; inserted ids become the input to batch enrichment. */
data class ProviderImportResult(
    val importedRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
    val importedMediaItemIds: List<Long> = emptyList(),
    val importBatchId: Long? = null,
    val invalidRows: Int = 0,
)

data class MyAnimeListAccountImportPreview(
    val items: List<MyAnimeListImportItem>,
    val preview: ProviderImportPreview,
)

class PreparedImdbCsvImport internal constructor(
    internal val parsed: ProviderCsvParseResult<ImdbCsvItem>,
    val preview: ImdbCsvPreview,
)

class PreparedStoryGraphCsvImport internal constructor(
    internal val parsed: ProviderCsvParseResult<StoryGraphCsvItem>,
    val preview: StoryGraphCsvPreview,
)

class PreparedMyAnimeListXmlImport internal constructor(
    internal val parsed: MyAnimeListXmlParseResult,
    val preview: MyAnimeListXmlPreview,
)

/**
 * Why a media item is being written.
 *
 * Provider imports are inbound snapshots. They must not immediately enter the outbound MAL queue
 * and send the same state back to MAL. Keeping this context as a call argument avoids a global
 * mutable "currently importing" flag that could suppress unrelated user edits.
 */
internal enum class MediaWriteOrigin(
    val queuesOutboundMalSync: Boolean,
) {
    UserAction(queuesOutboundMalSync = true),
    ProviderImport(queuesOutboundMalSync = false),
    AutomaticMetadataRefresh(queuesOutboundMalSync = false),
}

internal suspend fun MediaWriteOrigin.notifyOutboundMalSync(
    mediaItemId: Long,
    notify: suspend (Long) -> Unit,
) {
    if (queuesOutboundMalSync) notify(mediaItemId)
}

internal data class ProviderImportPlan<T>(
    val totalRows: Int,
    val importable: List<T>,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
    val invalidRows: Int = 0,
    val rejectedRows: List<ProviderRejectedRow> = emptyList(),
) {
    fun toPreview(): ProviderImportPreview = ProviderImportPreview(
        totalRows = totalRows,
        importableRows = importable.size,
        skippedDuplicateRows = skippedDuplicateRows,
        unsupportedRows = unsupportedRows,
        invalidRows = invalidRows,
        rejectedRows = rejectedRows,
    )

    fun toResult(
        importedMediaItemIds: List<Long>,
        importBatchId: Long? = null,
    ): ProviderImportResult = ProviderImportResult(
        importedRows = importedMediaItemIds.size,
        skippedDuplicateRows = skippedDuplicateRows,
        unsupportedRows = unsupportedRows,
        importedMediaItemIds = importedMediaItemIds,
        importBatchId = importBatchId,
        invalidRows = invalidRows,
    )
}

/**
 * Applies the same additive/duplicate rules to preview and persistence.
 *
 * Keys are added as rows are accepted, so duplicates within one export are skipped just like rows
 * already present in Room. Unsupported rows take precedence over duplicate classification.
 */
internal fun <T> planProviderImport(
    rows: List<T>,
    existingKeys: Collection<String>,
    duplicateKey: (T) -> String,
    isSupported: (T) -> Boolean,
    totalRows: Int = rows.size,
    invalidRows: Int = 0,
    initialRejectedRows: List<ProviderRejectedRow> = emptyList(),
    sourceRowNumber: (T) -> Int? = { null },
    displayLabel: (T) -> String? = { null },
): ProviderImportPlan<T> {
    val seenKeys = existingKeys.toMutableSet()
    val importable = mutableListOf<T>()
    var skippedDuplicateRows = 0
    var unsupportedRows = 0
    val rejectedRows = initialRejectedRows.toMutableList()

    fun recordRejection(row: T, reason: ProviderRejectedReason) {
        val rowNumber = sourceRowNumber(row) ?: return
        if (rejectedRows.count { it.reason == reason } >= MaxRejectedSamplesPerReason) return
        rejectedRows += ProviderRejectedRow(
            rowNumber = rowNumber,
            label = displayLabel(row)?.toSafeRejectedRowLabel(),
            reason = reason,
        )
    }

    rows.forEach { row ->
        when {
            !isSupported(row) -> {
                unsupportedRows++
                recordRejection(row, ProviderRejectedReason.UnsupportedType)
            }
            !seenKeys.add(duplicateKey(row)) -> {
                skippedDuplicateRows++
                recordRejection(row, ProviderRejectedReason.Duplicate)
            }
            else -> importable += row
        }
    }

    return ProviderImportPlan(
        totalRows = totalRows,
        importable = importable,
        skippedDuplicateRows = skippedDuplicateRows,
        unsupportedRows = unsupportedRows,
        invalidRows = invalidRows,
        rejectedRows = rejectedRows,
    )
}

private fun String.toSafeRejectedRowLabel(): String? =
    replace(Regex("\\s+"), " ")
        .trim()
        .take(MaxRejectedLabelLength)
        .takeIf(String::isNotBlank)

internal const val MaxRejectedSamplesPerReason = 2
private const val MaxRejectedLabelLength = 80
