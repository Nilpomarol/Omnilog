package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.RecommendationReason
import com.nilpo.contenttracker.core.model.TrackedMedia
import java.text.Normalizer

fun interface RecommendationProvider {
    suspend fun getRecommendations(item: MediaItem): List<MetadataSuggestion>
}

interface RecommendationRepository {
    suspend fun getRecommendations(
        current: TrackedMedia,
        library: List<TrackedMedia>,
        limit: Int = 8,
    ): List<ExternalRecommendation>
}

class CompositeRecommendationRepository(
    private val tmdb: RecommendationProvider,
    private val aniList: RecommendationProvider,
    private val rawg: RecommendationProvider,
    private val books: RecommendationProvider,
) : RecommendationRepository {
    override suspend fun getRecommendations(
        current: TrackedMedia,
        library: List<TrackedMedia>,
        limit: Int,
    ): List<ExternalRecommendation> {
        if (limit <= 0) return emptyList()

        val suggestions = when (current.item.metadataSource) {
            MetadataSource.Tmdb -> tmdb.getRecommendations(current.item)
            MetadataSource.AniList -> aniList.getRecommendations(current.item)
            MetadataSource.Rawg -> rawg.getRecommendations(current.item)
            else -> when (current.item.type) {
                MediaType.Book -> books.getRecommendations(current.item)
                else -> emptyList()
            }
        }

        val libraryKeys = library
            .mapNotNull { trackedMedia ->
                val source = trackedMedia.item.metadataSource ?: return@mapNotNull null
                val externalId = trackedMedia.item.metadataExternalId ?: return@mapNotNull null
                ProviderItemKey(source, externalId)
            }
            .toSet()

        val seenKeys = mutableSetOf<ProviderItemKey>()
        val seenTitleYears = mutableSetOf<TitleYearKey>()

        return suggestions
            .asSequence()
            .mapIndexed { index, suggestion ->
                ExternalRecommendation(
                    suggestion = suggestion,
                    reason = RecommendationReason.Similar,
                    providerRank = index + 1,
                )
            }
            .filter { recommendation ->
                val suggestion = recommendation.suggestion
                val providerKey = ProviderItemKey(suggestion.source, suggestion.externalId)
                val titleYearKey = TitleYearKey(
                    title = suggestion.title.normalizedRecommendationTitle(),
                    releaseYear = suggestion.releaseYear,
                )
                val alreadyTracked = providerKey in libraryKeys ||
                    library.any { trackedMedia -> trackedMedia.matchesRecommendation(suggestion) }
                val duplicateRecommendation = !seenKeys.add(providerKey) ||
                    !seenTitleYears.add(titleYearKey)

                !alreadyTracked && !duplicateRecommendation
            }
            .take(limit)
            .toList()
    }
}

private data class ProviderItemKey(
    val source: MetadataSource,
    val externalId: String,
)

private data class TitleYearKey(
    val title: String,
    val releaseYear: Int?,
)

private fun TrackedMedia.matchesRecommendation(
    suggestion: com.nilpo.contenttracker.core.model.MetadataSuggestion,
): Boolean {
    if (item.type != suggestion.mediaType) return false
    if (item.title.normalizedRecommendationTitle() != suggestion.title.normalizedRecommendationTitle()) {
        return false
    }
    return item.releaseYear == null || suggestion.releaseYear == null || item.releaseYear == suggestion.releaseYear
}

private fun String.normalizedRecommendationTitle(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .filter { character -> character.isLetterOrDigit() || character.isWhitespace() }
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
}
