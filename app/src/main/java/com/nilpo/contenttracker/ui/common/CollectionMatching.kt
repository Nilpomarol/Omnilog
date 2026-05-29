package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import java.text.Normalizer

data class CollectionQuickSuggestion(
    val name: String,
    @param:StringRes val labelResId: Int,
)

fun buildCollectionQuickSuggestions(
    availableCollections: List<MediaCollection>,
    providerCollectionTitle: String?,
    itemTitle: String,
): List<CollectionQuickSuggestion> {
    val candidates = listOfNotNull(
        providerCollectionTitle?.trim()?.takeIf { it.isNotBlank() }
            ?.let { it to R.string.collection_suggestion_provider },
        itemTitle.trim().takeIf { it.isNotBlank() }
            ?.let { it to R.string.collection_suggestion_title },
    )

    return candidates
        .distinctBy { (name, _) -> name.normalizedCollectionName() }
        .map { (name, labelResId) ->
            val existing = availableCollections.bestCollectionMatch(name)
            if (existing != null) {
                CollectionQuickSuggestion(
                    name = existing.name,
                    labelResId = R.string.collection_suggestion_existing,
                )
            } else {
                CollectionQuickSuggestion(
                    name = name,
                    labelResId = labelResId,
                )
            }
        }
        .distinctBy { suggestion -> suggestion.name.normalizedCollectionName() }
        .take(3)
}

fun List<MediaCollection>.bestCollectionMatch(candidateName: String): MediaCollection? {
    val normalizedCandidate = candidateName.normalizedCollectionName()
    if (normalizedCandidate.isBlank()) return null

    firstOrNull { collection ->
        collection.name.normalizedCollectionName() == normalizedCandidate
    }?.let { return it }

    val candidateKey = candidateName.collectionMatchKey()
    if (candidateKey.length < 4) return null

    return firstOrNull { collection ->
        val collectionKey = collection.name.collectionMatchKey()
        collectionKey.length >= 4 &&
            (candidateKey == collectionKey ||
                candidateKey.startsWith("$collectionKey ") ||
                collectionKey.startsWith("$candidateKey "))
    }
}

private fun String.normalizedCollectionName(): String {
    return Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("&", " and ")
        .replace(Regex("""['\u2019]"""), "")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
        .replace(Regex("""\s+"""), " ")
}

private fun String.collectionMatchKey(): String {
    return substringBeforeCollectionSeparator()
        .normalizedCollectionName()
        .replace(Regex("""\b(season|temporada|series|serie|book|libro|vol|volume|tome|part|parte|cour)\s+\d+(\.\d+)?\b.*$"""), "")
        .replace(Regex("""\b(s\d+|part\s*[ivx]+|parte\s*[ivx]+)\b.*$"""), "")
        .replace(Regex("""\b\d+(st|nd|rd|th)?\s+(season|temporada|book|libro|part|parte)\b.*$"""), "")
        .replace(Regex("""\b(sequel|prequel|ova|special|especial|movie|film)\b.*$"""), "")
        .trim()
        .replace(Regex("""\s+"""), " ")
}

private fun String.substringBeforeCollectionSeparator(): String {
    return split(Regex("""\s*[:;|/\\]\s*|\s+[\u2013\u2014-]\s+"""), limit = 2)
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: this
}
