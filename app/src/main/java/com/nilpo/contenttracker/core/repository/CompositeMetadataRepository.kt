package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion

class CompositeMetadataRepository(
    private val tmdb: TmdbMetadataRepository,
    private val jikan: JikanMetadataRepository,
) : MetadataRepository {
    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        return buildList {
            val tmdbTypes = request.mediaTypes.intersect(setOf(MediaType.Movie, MediaType.TvShow))
            if (tmdbTypes.isNotEmpty()) {
                addAll(tmdb.searchSuggestions(request.copy(mediaTypes = tmdbTypes)))
            }
            if (MediaType.Anime in request.mediaTypes) {
                addAll(jikan.searchSuggestions(request.copy(mediaTypes = setOf(MediaType.Anime))))
            }
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        return when (suggestion.source) {
            MetadataSource.Tmdb -> tmdb.getSuggestionDetails(suggestion)
            MetadataSource.Jikan -> jikan.getSuggestionDetails(suggestion)
            else -> suggestion
        }
    }
}
