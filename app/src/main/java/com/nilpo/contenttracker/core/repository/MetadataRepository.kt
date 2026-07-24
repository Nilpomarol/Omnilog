package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import java.io.IOException

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

internal data class MetadataFailureDetails(
    val retryable: Boolean,
    val diagnostic: String,
    val statusCode: Int? = null,
    val retryAfterMillis: Long? = null,
)

/** Only transient transport and provider responses are retryable. */
internal fun Throwable.toMetadataFailureDetails(
    providerName: String = "Provider",
): MetadataFailureDetails {
    val httpError = this as? MetadataProviderHttpException
    if (httpError != null) {
        val diagnostic = when (httpError.statusCode) {
            401, 403 -> "$providerName authorization failed (HTTP ${httpError.statusCode})"
            404 -> "$providerName item not found (HTTP 404)"
            408 -> "$providerName request timed out (HTTP 408)"
            429 -> "$providerName rate limit (HTTP 429)"
            else -> "$providerName request failed (HTTP ${httpError.statusCode})"
        }
        return MetadataFailureDetails(
            retryable = httpError.statusCode == 408 ||
                httpError.statusCode == 429 ||
                httpError.statusCode >= 500,
            diagnostic = diagnostic,
            statusCode = httpError.statusCode,
            retryAfterMillis = httpError.retryAfterMillis,
        )
    }
    if (this is IOException) {
        return MetadataFailureDetails(
            retryable = true,
            diagnostic = message?.takeIf(String::isNotBlank)
                ?.let { "Network request failed: $it" }
                ?: "Network request failed",
        )
    }
    val type = this::class.simpleName.orEmpty().ifBlank { "Unexpected error" }
    return MetadataFailureDetails(
        retryable = false,
        diagnostic = message?.takeIf(String::isNotBlank)?.let { "$type: $it" } ?: type,
    )
}
