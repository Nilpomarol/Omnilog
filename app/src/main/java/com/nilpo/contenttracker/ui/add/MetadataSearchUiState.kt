package com.nilpo.contenttracker.ui.add

import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion

data class MetadataSearchUiState(
    val query: String = "",
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val selectedSuggestion: MetadataSuggestion? = null,
    val isLoading: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val hasSearched: Boolean = false,
    val hasError: Boolean = false,
    /**
     * Providers that failed while others returned results. Empty when the search fully
     * succeeded, and also when it fully failed — that is [hasError], which offers a retry
     * for the whole search rather than naming sources the user got nothing from.
     */
    val failedSources: Set<MetadataSource> = emptySet(),
    val hasDetailsError: Boolean = false,
) {
    val hasPartialError: Boolean get() = failedSources.isNotEmpty()
}
