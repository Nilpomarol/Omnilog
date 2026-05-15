package com.nilpo.contenttracker.ui.add

import com.nilpo.contenttracker.core.model.MetadataSuggestion

data class MetadataSearchUiState(
    val query: String = "",
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val selectedSuggestion: MetadataSuggestion? = null,
    val isLoading: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val hasSearched: Boolean = false,
    val hasError: Boolean = false,
)
