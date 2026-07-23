package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MyAnimeListImportItem

/** Counts shared by every additive provider-import preview. */
data class ProviderImportPreview(
    val totalRows: Int,
    val importableRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
)

/** Result shared by provider imports; inserted ids become the input to batch enrichment. */
data class ProviderImportResult(
    val importedRows: Int,
    val skippedDuplicateRows: Int,
    val unsupportedRows: Int,
    val importedMediaItemIds: List<Long> = emptyList(),
    val importBatchId: Long? = null,
)

data class MyAnimeListAccountImportPreview(
    val items: List<MyAnimeListImportItem>,
    val preview: ProviderImportPreview,
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
) {
    fun toPreview(): ProviderImportPreview = ProviderImportPreview(
        totalRows = totalRows,
        importableRows = importable.size,
        skippedDuplicateRows = skippedDuplicateRows,
        unsupportedRows = unsupportedRows,
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
): ProviderImportPlan<T> {
    val seenKeys = existingKeys.toMutableSet()
    val importable = mutableListOf<T>()
    var skippedDuplicateRows = 0
    var unsupportedRows = 0

    rows.forEach { row ->
        when {
            !isSupported(row) -> unsupportedRows++
            !seenKeys.add(duplicateKey(row)) -> skippedDuplicateRows++
            else -> importable += row
        }
    }

    return ProviderImportPlan(
        totalRows = rows.size,
        importable = importable,
        skippedDuplicateRows = skippedDuplicateRows,
        unsupportedRows = unsupportedRows,
    )
}
