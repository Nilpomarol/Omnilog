package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(MediaSection.Anime)
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<TrackingStatus?>(null)
    private val sortMode = MutableStateFlow(HomeSortMode.Title)
    private val sortDirection = MutableStateFlow(HomeSortDirection.Ascending)
    private val metadataSearchState = MutableStateFlow(MetadataSearchUiState())

    val metadataUiState = metadataSearchState.asStateFlow()

    val uiState = selectedSection
        .flatMapLatest { section ->
            combine(
                mediaRepository.observeTrackedMedia(section.types),
                searchQuery,
                statusFilter,
                sortMode,
                sortDirection,
            ) { trackedItems, query, status, sort, direction ->
                val visibleItems = trackedItems
                    .filterBySearch(query)
                    .filterByStatus(status)
                    .sortByMode(sort, direction)

                    HomeUiState(
                        selectedSection = section,
                        trackedItems = visibleItems,
                        searchQuery = query,
                        statusFilter = status,
                        sortMode = sort,
                        sortDirection = direction,
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

    suspend fun exportBackupJson(): String {
        return mediaRepository.exportBackupJson()
    }

    suspend fun previewBackupJson(json: String): BackupPreview {
        return mediaRepository.previewBackupJson(json)
    }

    suspend fun importBackupJson(json: String) {
        mediaRepository.importBackupJson(json)
    }

    fun selectSection(section: MediaSection) {
        selectedSection.value = section
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateMetadataSearchQuery(query: String) {
        metadataSearchState.value = metadataSearchState.value.copy(query = query)
    }

    fun searchMetadataSuggestions() {
        val query = metadataSearchState.value.query.trim()
        if (query.isBlank()) {
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = emptyList(),
                isLoading = false,
                hasSearched = false,
            )
            return
        }

        viewModelScope.launch {
            metadataSearchState.value = metadataSearchState.value.copy(
                isLoading = true,
                hasSearched = true,
            )
            val suggestions = metadataRepository.searchSuggestions(
                MetadataSearchRequest(
                    query = query,
                    mediaTypes = selectedSection.value.types,
                ),
            )
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = suggestions,
                selectedSuggestion = null,
                isLoading = false,
            )
        }
    }

    fun clearMetadataSearch() {
        metadataSearchState.value = MetadataSearchUiState()
    }

    fun selectMetadataSuggestion(suggestion: MetadataSuggestion) {
        metadataSearchState.value = metadataSearchState.value.copy(
            selectedSuggestion = suggestion,
            isLoadingDetails = true,
        )

        viewModelScope.launch {
            val detailedSuggestion = metadataRepository.getSuggestionDetails(suggestion)
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = metadataSearchState.value.suggestions.map { existingSuggestion ->
                    if (
                        existingSuggestion.source == detailedSuggestion.source &&
                        existingSuggestion.externalId == detailedSuggestion.externalId
                    ) {
                        detailedSuggestion
                    } else {
                        existingSuggestion
                    }
                },
                selectedSuggestion = detailedSuggestion,
                isLoadingDetails = false,
            )
        }
    }

    fun updateStatusFilter(status: TrackingStatus?) {
        statusFilter.value = status
    }

    fun updateSortMode(mode: HomeSortMode) {
        sortMode.value = mode
        sortDirection.value = mode.defaultDirection()
    }

    fun updateSortDirection(direction: HomeSortDirection) {
        sortDirection.value = direction
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

    fun deleteMediaItem(mediaItemId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteMediaItem(mediaItemId)
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
        private val metadataRepository: MetadataRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                mediaRepository = mediaRepository,
                metadataRepository = metadataRepository,
            ) as T
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

private fun List<TrackedMedia>.sortByMode(
    mode: HomeSortMode,
    direction: HomeSortDirection,
): List<TrackedMedia> {
    val comparator = when (mode) {
        HomeSortMode.Title -> compareBy<TrackedMedia> { it.item.title.lowercase() }
        HomeSortMode.Collection -> compareBy<TrackedMedia> { it.collection?.name?.lowercase().orEmpty() }
            .thenBy { it.item.title.lowercase() }
        HomeSortMode.Progress -> compareBy<TrackedMedia> { it.progressSortValue() }
            .thenBy { it.item.title.lowercase() }
        HomeSortMode.Rating -> compareBy<TrackedMedia> { it.currentSession?.rating ?: 0 }
            .thenBy { it.item.title.lowercase() }
        HomeSortMode.Recent -> compareBy<TrackedMedia> { it.currentSession?.updatedAtEpochMillis ?: 0L }
            .thenBy { it.item.title.lowercase() }
    }

    return when (direction) {
        HomeSortDirection.Ascending -> sortedWith(comparator)
        HomeSortDirection.Descending -> sortedWith(comparator.reversed())
    }
}

private fun TrackedMedia.progressSortValue(): Double {
    val progressCurrent = currentSession?.progressCurrent ?: 0
    val progressTotal = item.progressTotal

    return if (progressTotal != null && progressTotal > 0) {
        progressCurrent.toDouble() / progressTotal.toDouble()
    } else {
        progressCurrent.toDouble()
    }
}

private fun HomeSortMode.defaultDirection(): HomeSortDirection {
    return when (this) {
        HomeSortMode.Title,
        HomeSortMode.Collection,
        -> HomeSortDirection.Ascending
        HomeSortMode.Progress,
        HomeSortMode.Rating,
        HomeSortMode.Recent,
        -> HomeSortDirection.Descending
    }
}
