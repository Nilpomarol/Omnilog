package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
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

    fun startNewSession(mediaItemId: Long) {
        viewModelScope.launch {
            mediaRepository.startNewSession(mediaItemId)
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

    class Factory(
        private val mediaRepository: MediaRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(mediaRepository) as T
        }
    }
}
