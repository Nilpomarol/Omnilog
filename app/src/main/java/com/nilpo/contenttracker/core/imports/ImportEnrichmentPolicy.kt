package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.model.MetadataSource

/**
 * Fields owned by an import snapshot rather than by the user.
 *
 * Exact provider enrichment may replace these values even when the import file supplied a slightly
 * different title, genre list, creator list, duration/page total, or rating. A field becomes protected
 * as soon as the user edits it, which is represented by [MetadataRefreshChange.isLocallyOverridden].
 */
internal fun MetadataRefreshPreview.autoApplicableImportedFields(
    importSource: ImportSource,
    animeTitlePreference: AnimeTitlePreference,
): Set<MetadataRefreshField> {
    val preserveImportedAnimeTitles =
        importSource in setOf(ImportSource.MalApi, ImportSource.MalXml) &&
            animeTitlePreference == AnimeTitlePreference.KeepMalTitle
    return changes
        .filter { change ->
            !change.isLocallyOverridden &&
                !(preserveImportedAnimeTitles &&
                    change.overwritesExistingValue &&
                    change.field in AnimeTitleFields)
        }
        .mapTo(mutableSetOf()) { change -> change.field }
}

internal fun MetadataRefreshPreview.hasReviewChangesAfter(
    appliedFields: Set<MetadataRefreshField>,
): Boolean = changes.any { change -> change.field !in appliedFields }

internal fun MetadataRefreshPreview.equivalentMalUrlFields(
    reference: ProviderReference,
): Set<MetadataRefreshField> {
    if (reference.source != MetadataSource.Jikan) return emptySet()
    val expectedId = reference.externalId.toIntOrNull() ?: return emptySet()
    return changes
        .filter { change ->
            change.field == MetadataRefreshField.SourceUrl &&
                change.currentValue.malAnimeId() == expectedId &&
                change.newValue.malAnimeId() == expectedId
        }
        .mapTo(mutableSetOf()) { it.field }
}

/** A review choice is valid only while the values shown for every selected field are still current. */
internal fun MetadataRefreshPreview.selectedChangesMatch(
    displayed: MetadataRefreshPreview,
    selectedFields: Set<MetadataRefreshField>,
): Boolean {
    val displayedChanges = displayed.changes.associateBy { it.field }
    return changes
        .filter { it.field in selectedFields }
        .all { fresh ->
            displayedChanges[fresh.field]?.let { shown ->
                shown.currentValue == fresh.currentValue && shown.newValue == fresh.newValue
            } == true
        }
}

private fun String.malAnimeId(): Int? = Regex("https?://(?:www\\.)?myanimelist\\.net/anime/(\\d+)(?:/.*)?")
    .matchEntire(trim())
    ?.groupValues
    ?.getOrNull(1)
    ?.toIntOrNull()

private val AnimeTitleFields = setOf(MetadataRefreshField.Title, MetadataRefreshField.OriginalTitle)
