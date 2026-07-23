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

    /** Resolves a stable identifier carried by an import into this app's metadata providers. */
    suspend fun resolveImportedExternalId(
        source: MetadataSource,
        externalId: String,
        mediaType: com.nilpo.contenttracker.core.model.MediaType,
    ): MetadataSuggestion? = null

    fun isImportedSourceAvailable(source: MetadataSource): Boolean = true
}

data class MetadataSearchResult(
    val suggestions: List<MetadataSuggestion>,
    val failedSources: Set<MetadataSource> = emptySet(),
    val failures: List<MetadataSearchFailure> = emptyList(),
)

data class MetadataSearchFailure(
    val source: MetadataSource,
    val diagnostic: String,
    val retryable: Boolean,
    val statusCode: Int? = null,
    val retryAfterMillis: Long? = null,
)

class MetadataProviderHttpException(
    val statusCode: Int,
    val requestUrl: String,
    val retryAfterMillis: Long? = null,
    responseDetail: String? = null,
) : Exception(
    buildString {
        append("HTTP $statusCode")
        responseDetail?.takeIf { it.isNotBlank() }?.let { append(": ${it.take(180)}") }
    },
)
