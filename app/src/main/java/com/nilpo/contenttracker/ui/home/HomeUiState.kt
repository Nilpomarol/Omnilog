package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.TrackedMedia

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedSection: MediaSection = MediaSection.Anime,
    val allTrackedItems: List<TrackedMedia> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val trackedItems: List<TrackedMedia> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: TrackingStatus? = null,
    val browseMode: HomeBrowseMode = HomeBrowseMode.Items,
    val sortMode: HomeSortMode = HomeSortMode.Recent,
    val sortDirection: HomeSortDirection = HomeSortDirection.Descending,
    val advancedFilters: HomeAdvancedFilters = HomeAdvancedFilters(),
    val refreshingMetadataItemId: Long? = null,
)

data class RecommendationUiState(
    val mediaItemId: Long? = null,
    val recommendations: List<ExternalRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

data class HomeAdvancedFilters(
    val authors: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val minimumExternalRating: Int? = null,
    val minimumUserRating: Int? = null,
) {
    val activeCount: Int
        get() = authors.size.coerceAtMost(1) +
            genres.size.coerceAtMost(1) +
            (if (minimumExternalRating != null) 1 else 0) +
            (if (minimumUserRating != null) 1 else 0)

    val isActive: Boolean
        get() = authors.isNotEmpty() || genres.isNotEmpty() ||
            minimumExternalRating != null || minimumUserRating != null
}

enum class HomeBrowseMode {
    Items,
    Collections,
    Authors,
}

internal enum class HomeGroupMode {
    None,
    Status,
    Collection,
    Author,
}

enum class HomeSortMode {
    Title,
    Progress,
    Rating,
    Recent,
}

enum class HomeSortDirection {
    Ascending,
    Descending,
}
