package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.RecommendationReason
import com.nilpo.contenttracker.core.model.MetadataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookRecommendationRepository(
    private val metadataRepository: MetadataRepository,
) : RecommendationProvider {
    override suspend fun getRecommendations(item: MediaItem): List<MetadataSuggestion> {
        if (item.type != MediaType.Book) return emptyList()

        val candidates = linkedMapOf<RecommendationCandidateKey, RecommendationCandidate>()
        withContext(Dispatchers.IO) {
            item.creators
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy(String::lowercase)
                .take(MAX_CREATORS)
                .forEach { creator ->
                    searchAndMerge(
                        query = creator,
                        priority = RecommendationPriority.Creator,
                        candidates = candidates,
                    )
                }

            item.providerCollectionTitle
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { collectionTitle ->
                    searchAndMerge(
                        query = collectionTitle,
                        priority = RecommendationPriority.Collection,
                        candidates = candidates,
                    )
                }

            item.genres
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy(String::lowercase)
                .take(MAX_GENRES)
                .forEach { genre ->
                    searchAndMerge(
                        query = genre,
                        priority = RecommendationPriority.Genre,
                        candidates = candidates,
                    )
                }
        }

        return candidates.values
            .sortedWith(
                compareBy<RecommendationCandidate> { it.priority.rank }
                    .thenByDescending { it.genreMatchCount }
                    .thenBy { it.providerRank }
                    .thenBy { it.suggestion.title.lowercase() },
            )
            .take(MAX_RECOMMENDATIONS)
            .map { it.suggestion }
    }

    private suspend fun searchAndMerge(
        query: String,
        priority: RecommendationPriority,
        candidates: MutableMap<RecommendationCandidateKey, RecommendationCandidate>,
    ) {
        val suggestions = runCatching {
            metadataRepository.searchSuggestions(
                MetadataSearchRequest(
                    query = query,
                    mediaTypes = setOf(MediaType.Book),
                ),
            )
        }.getOrDefault(emptyList())

        suggestions.forEachIndexed { index, suggestion ->
            val key = RecommendationCandidateKey(suggestion.source, suggestion.externalId)
            val existing = candidates[key]
            candidates[key] = RecommendationCandidate(
                suggestion = suggestion,
                priority = if (existing == null || priority.rank < existing.priority.rank) {
                    priority
                } else {
                    existing.priority
                },
                genreMatchCount = (existing?.genreMatchCount ?: 0) +
                    if (priority == RecommendationPriority.Genre) 1 else 0,
                providerRank = minOf(existing?.providerRank ?: Int.MAX_VALUE, index),
            )
        }
    }

    private data class RecommendationCandidateKey(
        val source: MetadataSource,
        val externalId: String,
    )

    private data class RecommendationCandidate(
        val suggestion: MetadataSuggestion,
        val priority: RecommendationPriority,
        val genreMatchCount: Int,
        val providerRank: Int,
    )

    private enum class RecommendationPriority(val rank: Int) {
        Creator(0),
        Collection(1),
        Genre(2),
    }

    private companion object {
        const val MAX_CREATORS = 3
        const val MAX_GENRES = 5
        const val MAX_RECOMMENDATIONS = 8
    }
}
