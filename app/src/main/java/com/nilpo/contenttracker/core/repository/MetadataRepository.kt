package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion

interface MetadataRepository {
    suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion>

    suspend fun searchSuggestionsWithDiagnostics(request: MetadataSearchRequest): MetadataSearchResult {
        return MetadataSearchResult(suggestions = searchSuggestions(request))
    }

    suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion
}

data class MetadataSearchResult(
    val suggestions: List<MetadataSuggestion>,
    val failedSources: Set<MetadataSource> = emptySet(),
)
