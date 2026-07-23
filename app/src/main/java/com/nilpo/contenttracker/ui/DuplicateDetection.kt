package com.nilpo.contenttracker.ui

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.resolveMyAnimeListId
import java.text.Normalizer

internal sealed interface DuplicateMatch {
    data object None : DuplicateMatch
    data class Exact(val trackedMedia: TrackedMedia) : DuplicateMatch
    data class Possible(val trackedMedia: TrackedMedia) : DuplicateMatch
}

internal fun List<TrackedMedia>.findDuplicateFor(suggestion: MetadataSuggestion): DuplicateMatch {
    val suggestionMalId = suggestion.resolvedMyAnimeListId()
    val exactMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            (
                trackedMedia.hasSameProviderIdentity(suggestion) ||
                    suggestionMalId != null &&
                    trackedMedia.item.type == MediaType.Anime &&
                    trackedMedia.item.resolvedMyAnimeListId() == suggestionMalId
            )
    }
    if (exactMatch != null) {
        return DuplicateMatch.Exact(exactMatch)
    }

    val possibleMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            trackedMedia.hasCompatibleReleaseYear(suggestion) &&
            trackedMedia.titleCandidates().intersect(suggestion.titleCandidates()).isNotEmpty()
    }

    return possibleMatch?.let(DuplicateMatch::Possible) ?: DuplicateMatch.None
}

private fun TrackedMedia.hasSameProviderIdentity(suggestion: MetadataSuggestion): Boolean {
    return item.metadataSource == suggestion.source && item.metadataExternalId == suggestion.externalId
}

private fun MediaItem.resolvedMyAnimeListId(): Int? = resolveMyAnimeListId(
    explicitMalId = malId,
    metadataSource = metadataSource?.name,
    metadataExternalId = metadataExternalId,
    popularityJson = popularityJson,
    sourceUrl = sourceUrl,
)

private fun MetadataSuggestion.resolvedMyAnimeListId(): Int? = resolveMyAnimeListId(
    explicitMalId = malId,
    metadataSource = source.name,
    metadataExternalId = externalId,
    popularityJson = popularityJson,
    sourceUrl = sourceUrl,
)

private fun TrackedMedia.hasCompatibleReleaseYear(suggestion: MetadataSuggestion): Boolean {
    val existingYear = item.releaseYear
    val suggestionYear = suggestion.releaseYear
    return existingYear == null || suggestionYear == null || existingYear == suggestionYear
}

private fun TrackedMedia.titleCandidates(): Set<String> {
    return listOf(item.title, item.originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun MetadataSuggestion.titleCandidates(): Set<String> {
    return listOf(title, originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun String.normalizedDuplicateTitle(): String {
    val withoutMarks = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutMarks
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
