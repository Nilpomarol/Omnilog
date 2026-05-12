package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.TrackedMedia

data class HomeUiState(
    val selectedSection: MediaSection = MediaSection.Anime,
    val trackedItems: List<TrackedMedia> = emptyList(),
)
