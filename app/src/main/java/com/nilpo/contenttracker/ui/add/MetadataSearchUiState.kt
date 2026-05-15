package com.nilpo.contenttracker.ui.add

import com.nilpo.contenttracker.core.model.MetadataSuggestion

data class MetadataSearchUiState(
    val query: String = "",
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
)
