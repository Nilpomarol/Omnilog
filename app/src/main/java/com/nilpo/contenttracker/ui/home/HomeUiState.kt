package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.TrackedMedia

data class HomeUiState(
    val selectedSection: MediaSection = MediaSection.Anime,
    val allTrackedItems: List<TrackedMedia> = emptyList(),
    val trackedItems: List<TrackedMedia> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: TrackingStatus? = null,
    val sortMode: HomeSortMode = HomeSortMode.Title,
    val sortDirection: HomeSortDirection = HomeSortDirection.Ascending,
    val refreshingMetadataItemId: Long? = null,
)

enum class HomeSortMode {
    Title,
    Collection,
    Progress,
    Rating,
    Recent,
}

enum class HomeSortDirection {
    Ascending,
    Descending,
}
