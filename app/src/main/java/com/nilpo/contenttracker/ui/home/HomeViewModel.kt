package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val mediaRepository: MediaRepository,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(MediaSection.Anime)
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<TrackingStatus?>(null)
    private val sortMode = MutableStateFlow(HomeSortMode.Title)

    val uiState = selectedSection
        .flatMapLatest { section ->
            combine(
                mediaRepository.observeTrackedMedia(section.types),
                searchQuery,
                statusFilter,
                sortMode,
            ) { trackedItems, query, status, sort ->
                val visibleItems = trackedItems
                    .filterBySearch(query)
                    .filterByStatus(status)
                    .sortByMode(sort)

                    HomeUiState(
                        selectedSection = section,
                        trackedItems = visibleItems,
                        searchQuery = query,
                        statusFilter = status,
                        sortMode = sort,
                    )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        viewModelScope.launch {
            mediaRepository.seedSampleDataIfEmpty()
        }
    }

    fun selectSection(section: MediaSection) {
        selectedSection.value = section
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateStatusFilter(status: TrackingStatus?) {
        statusFilter.value = status
    }

    fun updateSortMode(mode: HomeSortMode) {
        sortMode.value = mode
    }

    fun startNewSession(request: AddTrackingSessionRequest) {
        viewModelScope.launch {
            mediaRepository.startNewSession(request)
        }
    }

    fun addTrackedMedia(request: AddTrackedMediaRequest) {
        viewModelScope.launch {
            mediaRepository.addTrackedMedia(request)
        }
    }

    fun updateSessionProgress(sessionId: Long, progressCurrent: Int) {
        viewModelScope.launch {
            mediaRepository.updateSessionProgress(sessionId, progressCurrent)
        }
    }

    fun updateSessionStatus(sessionId: Long, status: TrackingStatus) {
        viewModelScope.launch {
            mediaRepository.updateSessionStatus(sessionId, status)
        }
    }

    fun updateSessionRating(sessionId: Long, rating: Int?) {
        viewModelScope.launch {
            mediaRepository.updateSessionRating(sessionId, rating)
        }
    }

    fun updateSessionNotes(sessionId: Long, notes: String?) {
        viewModelScope.launch {
            mediaRepository.updateSessionNotes(sessionId, notes)
        }
    }

    fun deletePastSession(sessionId: Long) {
        viewModelScope.launch {
            mediaRepository.deletePastSession(sessionId)
        }
    }

    fun addExternalTracking(
        mediaItemId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    ) {
        viewModelScope.launch {
            mediaRepository.addExternalTracking(
                mediaItemId = mediaItemId,
                source = source,
                externalItemId = externalItemId,
                url = url,
            )
        }
    }

    fun updateExternalTrackingSynced(externalTrackingId: Long, isSynced: Boolean) {
        viewModelScope.launch {
            mediaRepository.updateExternalTrackingSynced(
                externalTrackingId = externalTrackingId,
                isSynced = isSynced,
            )
        }
    }

    fun deleteExternalTracking(externalTrackingId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteExternalTracking(externalTrackingId)
        }
    }

    fun updateMediaCollectionName(collectionId: Long, name: String) {
        viewModelScope.launch {
            mediaRepository.updateMediaCollectionName(collectionId, name)
        }
    }

    fun deleteMediaCollection(collectionId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteMediaCollection(collectionId)
        }
    }

    fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        viewModelScope.launch {
            mediaRepository.updateMediaItemDetails(
                mediaItemId = mediaItemId,
                title = title,
                collectionId = collectionId,
                newCollectionName = newCollectionName,
                progressTotal = progressTotal,
                ownershipType = ownershipType,
            )
        }
    }

    fun updateSessionPlatform(
        sessionId: Long,
        platformName: String?,
        platformType: ConsumptionPlatformType,
    ) {
        viewModelScope.launch {
            mediaRepository.updateSessionPlatform(
                sessionId = sessionId,
                platformName = platformName,
                platformType = platformType,
            )
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(mediaRepository) as T
        }
    }
}

private fun List<TrackedMedia>.filterBySearch(query: String): List<TrackedMedia> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return this
    }

    return filter { trackedMedia ->
        trackedMedia.item.title.contains(normalizedQuery, ignoreCase = true) ||
            trackedMedia.collection?.name?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

private fun List<TrackedMedia>.filterByStatus(status: TrackingStatus?): List<TrackedMedia> {
    return status?.let { selectedStatus ->
        filter { trackedMedia -> trackedMedia.currentSession?.status == selectedStatus }
    } ?: this
}

private fun List<TrackedMedia>.sortByMode(mode: HomeSortMode): List<TrackedMedia> {
    return when (mode) {
        HomeSortMode.Title -> sortedBy { it.item.title.lowercase() }
        HomeSortMode.Collection -> sortedWith(
            compareBy<TrackedMedia> { it.collection?.name?.lowercase().orEmpty() }
                .thenBy { it.item.title.lowercase() },
        )
    }
}
