package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    private val refreshingMetadataItemId = MutableStateFlow<Long?>(null)
    private val mutableEvents = MutableSharedFlow<HomeUiEvent>()

    val metadataUiState = metadataSearchState.asStateFlow()
    val events = mutableEvents.asSharedFlow()

    private val filters = combine(
        searchQuery,
        statusFilter,
        sortMode,
        sortDirection,
    ) { query, status, sort, direction ->
        HomeFilters(
            query = query,
            status = status,
            sort = sort,
            direction = direction,
        )
    }

    val uiState = combine(
        selectedSection,
        mediaRepository.observeTrackedMedia(MediaType.entries.toSet()),
        filters,
        refreshingMetadataItemId,
    ) { section, allTrackedItems, filters, refreshingItemId ->
        val visibleItems = allTrackedItems
            .filter { it.item.type in section.types }
            .filterBySearch(filters.query)
            .filterByStatus(filters.status)
            .sortByMode(filters.sort, filters.direction)

        HomeUiState(
            selectedSection = section,
            allTrackedItems = allTrackedItems,
            trackedItems = visibleItems,
            searchQuery = filters.query,
            statusFilter = filters.status,
            sortMode = filters.sort,
            sortDirection = filters.direction,
            refreshingMetadataItemId = refreshingItemId,
        )
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
                hasError = false,
            )
            val result = runCatching {
                metadataRepository.searchSuggestions(
                    MetadataSearchRequest(
                        query = query,
                        mediaTypes = selectedSection.value.types,
                    ),
                )
            }
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = result.getOrElse { emptyList() },
                selectedSuggestion = null,
                isLoading = false,
                hasError = result.isFailure,
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
            hasDetailsError = false,
        )

        viewModelScope.launch {
            val result = runCatching {
                metadataRepository.getSuggestionDetails(suggestion)
            }
            val detailedSuggestion = result.getOrDefault(suggestion)
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
                hasDetailsError = result.isFailure,
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

    fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        rating: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ) {
        viewModelScope.launch {
            mediaRepository.updateSessionDetails(
                sessionId = sessionId,
                status = status,
                progressCurrent = progressCurrent,
                rating = rating,
                notes = notes,
                startedAt = startedAt,
                finishedAt = finishedAt,
            )
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

    fun updateExternalTracking(
        externalTrackingId: Long,
        source: ExternalTrackingSource,
        externalItemId: String?,
        url: String?,
    ) {
        viewModelScope.launch {
            mediaRepository.updateExternalTracking(
                externalTrackingId = externalTrackingId,
                source = source,
                externalItemId = externalItemId,
                url = url,
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

    fun updateMediaItemMetadata(
        mediaItemId: Long,
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        progressTotal: Int?,
        genres: List<String>,
        creators: List<String>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    ) {
        viewModelScope.launch {
            mediaRepository.updateMediaItemMetadata(
                mediaItemId = mediaItemId,
                title = title,
                originalTitle = originalTitle,
                releaseYear = releaseYear,
                progressTotal = progressTotal,
                genres = genres,
                creators = creators,
                coverUrl = coverUrl,
                synopsis = synopsis,
                sourceUrl = sourceUrl,
            )
        }
    }

    fun refreshMediaItemMetadata(mediaItemId: Long) {
        if (refreshingMetadataItemId.value != null) return

        viewModelScope.launch {
            refreshingMetadataItemId.value = mediaItemId
            val result = runCatching {
                mediaRepository.refreshMediaItemMetadata(mediaItemId, metadataRepository)
            }
            refreshingMetadataItemId.value = null

            mutableEvents.emit(
                when {
                    result.isFailure -> HomeUiEvent.MetadataRefreshFailed
                    result.getOrDefault(false) -> HomeUiEvent.MetadataRefreshSucceeded
                    else -> HomeUiEvent.MetadataRefreshUnavailable
                },
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

sealed interface HomeUiEvent {
    data object MetadataRefreshSucceeded : HomeUiEvent
    data object MetadataRefreshUnavailable : HomeUiEvent
    data object MetadataRefreshFailed : HomeUiEvent
}

private data class HomeFilters(
    val query: String,
    val status: TrackingStatus?,
    val sort: HomeSortMode,
    val direction: HomeSortDirection,
)

private fun List<TrackedMedia>.filterBySearch(query: String): List<TrackedMedia> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return this
    }

    return filter { trackedMedia ->
        trackedMedia.item.title.contains(normalizedQuery, ignoreCase = true) ||
            trackedMedia.collection?.name?.contains(normalizedQuery, ignoreCase = true) == true ||
            trackedMedia.item.creators.any { it.contains(normalizedQuery, ignoreCase = true) }
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
