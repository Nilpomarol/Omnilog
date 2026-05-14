package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    val uiState = selectedSection
        .flatMapLatest { section ->
            mediaRepository.observeTrackedMedia(section.types)
                .map { trackedItems ->
                    HomeUiState(
                        selectedSection = section,
                        trackedItems = trackedItems,
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

    fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        viewModelScope.launch {
            mediaRepository.updateMediaItemDetails(
                mediaItemId = mediaItemId,
                title = title,
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
