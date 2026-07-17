package com.nilpo.contenttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import com.nilpo.contenttracker.core.repository.DeletionRecovery
import com.nilpo.contenttracker.core.repository.DeletionRecoveryStore
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.ImdbCsvImportResult
import com.nilpo.contenttracker.core.repository.ImdbCsvPreview
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.RecommendationRepository
import com.nilpo.contenttracker.core.repository.MyAnimeListXmlImportResult
import com.nilpo.contenttracker.core.repository.MyAnimeListXmlPreview
import com.nilpo.contenttracker.core.repository.StoryGraphCsvImportResult
import com.nilpo.contenttracker.core.repository.StoryGraphCsvPreview
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val metadataRepository: MetadataRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(MediaSection.Anime)
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<TrackingStatus?>(null)
    private val browseMode = MutableStateFlow(HomeBrowseMode.Items)
    private val sortMode = MutableStateFlow(HomeSortMode.Recent)
    private val sortDirection = MutableStateFlow(HomeSortDirection.Descending)
    private val advancedFilters = MutableStateFlow(HomeAdvancedFilters())
    private val metadataSearchState = MutableStateFlow(MetadataSearchUiState())
    private var metadataSearchJob: Job? = null
    private val metadataSearchCache = LinkedHashMap<MetadataSearchCacheKey, CachedMetadataSearch>()
    private val recommendationState = MutableStateFlow(RecommendationUiState())
    private var recommendationJob: Job? = null
    private val recommendationCache = LinkedHashMap<RecommendationCacheKey, CachedRecommendations>()
    private val refreshingMetadataItemId = MutableStateFlow<Long?>(null)
    private val mutableEvents = MutableSharedFlow<HomeUiEvent>()
    private val deletionRecoveryStore = DeletionRecoveryStore()

    val metadataUiState = metadataSearchState.asStateFlow()
    val recommendationUiState = recommendationState.asStateFlow()
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

    private val baseUiState = combine(
        selectedSection,
        mediaRepository.observeTrackedMedia(MediaType.entries.toSet()),
        filters,
        refreshingMetadataItemId,
        advancedFilters,
    ) { section, allTrackedItems, filters, refreshingItemId, advanced ->
        val visibleItems = allTrackedItems
            .filter { it.item.type in section.types }
            .filterBySearch(filters.query)
            .filterByStatus(filters.status)
            .filterByAdvancedFilters(advanced)
            .sortByMode(filters.sort, filters.direction)

        HomeUiState(
            selectedSection = section,
            allTrackedItems = allTrackedItems,
            trackedItems = visibleItems,
            searchQuery = filters.query,
            statusFilter = filters.status,
            browseMode = HomeBrowseMode.Items,
            sortMode = filters.sort,
            sortDirection = filters.direction,
            advancedFilters = advanced,
            refreshingMetadataItemId = refreshingItemId,
        )
    }

    val uiState = baseUiState
        .combine(mediaRepository.observeObjectives()) { state, objectives -> state.copy(objectives = objectives) }
        .combine(browseMode) { state, selectedBrowseMode ->
            state.copy(browseMode = selectedBrowseMode)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    suspend fun exportBackupJson(): String {
        return mediaRepository.exportBackupJson()
    }

    suspend fun previewBackupJson(json: String): BackupPreview {
        return mediaRepository.previewBackupJson(json)
    }

    suspend fun importBackupJson(json: String) {
        mediaRepository.importBackupJson(json)
    }

    suspend fun previewImdbCsv(csv: String): ImdbCsvPreview {
        return mediaRepository.previewImdbCsv(csv)
    }

    suspend fun importImdbCsv(csv: String): ImdbCsvImportResult {
        return mediaRepository.importImdbCsv(csv)
    }

    suspend fun previewStoryGraphCsv(csv: String): StoryGraphCsvPreview {
        return mediaRepository.previewStoryGraphCsv(csv)
    }

    suspend fun importStoryGraphCsv(csv: String): StoryGraphCsvImportResult {
        return mediaRepository.importStoryGraphCsv(csv)
    }

    suspend fun previewMyAnimeListXml(xml: String): MyAnimeListXmlPreview {
        return mediaRepository.previewMyAnimeListXml(xml)
    }

    suspend fun importMyAnimeListXml(xml: String): MyAnimeListXmlImportResult {
        return mediaRepository.importMyAnimeListXml(xml)
    }

    fun selectSection(section: MediaSection) {
        cancelMetadataSearch()
        selectedSection.value = section
        searchQuery.value = ""
        browseMode.value = HomeBrowseMode.Items
        metadataSearchState.value = MetadataSearchUiState()
    }

    fun selectSectionWithSearch(section: MediaSection, query: String) {
        cancelMetadataSearch()
        selectedSection.value = section
        searchQuery.value = query
        browseMode.value = HomeBrowseMode.Items
        metadataSearchState.value = MetadataSearchUiState(query = query)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateMetadataSearchQuery(query: String) {
        cancelMetadataSearch()
        metadataSearchState.value = MetadataSearchUiState(query = query)
    }

    fun searchMetadataSuggestions(forceShortQuery: Boolean = false) {
        val query = metadataSearchState.value.query.trim()
        if (query.isBlank() || (!forceShortQuery && query.length < MINIMUM_AUTOMATIC_SEARCH_LENGTH)) {
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = emptyList(),
                isLoading = false,
                hasSearched = false,
                hasError = false,
            )
            return
        }

        cancelMetadataSearch()
        val section = selectedSection.value
        val request = MetadataSearchRequest(query = query, mediaTypes = section.types)
        val cacheKey = MetadataSearchCacheKey(request)
        cachedMetadataSearch(cacheKey)?.let { cached ->
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = cached.suggestions,
                isLoading = false,
                hasSearched = true,
                hasError = false,
                hasPartialError = false,
            )
            return
        }

        val queryAtRequestStart = metadataSearchState.value.query
        metadataSearchState.value = metadataSearchState.value.copy(
            suggestions = emptyList(),
            isLoading = true,
            hasSearched = true,
            hasError = false,
        )
        metadataSearchJob = viewModelScope.launch {
            val result = runCatching { metadataRepository.searchSuggestionsWithDiagnostics(request) }
            if (!isActive || selectedSection.value != section || metadataSearchState.value.query != queryAtRequestStart) {
                return@launch
            }
            val searchResult = result.getOrNull()
            val hasProviderFailure = searchResult?.failedSources?.isNotEmpty() == true
            val hasResults = searchResult?.suggestions?.isNotEmpty() == true
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = searchResult?.suggestions.orEmpty(),
                selectedSuggestion = null,
                isLoading = false,
                hasError = result.isFailure || (hasProviderFailure && !hasResults),
                hasPartialError = hasProviderFailure && hasResults,
            )
            searchResult?.takeIf { it.failedSources.isEmpty() }?.let { successfulSearch ->
                cacheMetadataSearch(cacheKey, successfulSearch.suggestions)
            }
        }
    }

    fun loadRecommendations(
        current: TrackedMedia,
        library: List<TrackedMedia>,
        forceRefresh: Boolean = false,
    ) {
        recommendationJob?.cancel()
        val source = current.item.metadataSource
        val externalId = current.item.metadataExternalId
        val cacheKey = when {
            current.item.type == MediaType.Book -> {
                RecommendationCacheKey(
                    source = source ?: MetadataSource.OpenLibrary,
                    externalId = externalId?.takeIf { it.isNotBlank() }
                        ?: "local:" + current.item.id.toString(),
                )
            }
            source != null && !externalId.isNullOrBlank() -> {
                RecommendationCacheKey(source = source, externalId = externalId)
            }
            else -> null
        }
        if (cacheKey == null) {
            recommendationState.value = RecommendationUiState(mediaItemId = current.item.id)
            return
        }

        if (!forceRefresh) {
            cachedRecommendations(cacheKey)?.let { cached ->
                recommendationState.value = RecommendationUiState(
                    mediaItemId = current.item.id,
                    recommendations = cached.recommendations,
                )
                return
            }
        }

        recommendationState.value = RecommendationUiState(
            mediaItemId = current.item.id,
            isLoading = true,
        )
        val librarySnapshot = library.toList()
        recommendationJob = viewModelScope.launch {
            val result = runCatching {
                recommendationRepository.getRecommendations(
                    current = current,
                    library = librarySnapshot,
                )
            }
            if (!isActive || recommendationState.value.mediaItemId != current.item.id) {
                return@launch
            }

            val recommendations = result.getOrDefault(emptyList())
            recommendationState.value = RecommendationUiState(
                mediaItemId = current.item.id,
                recommendations = recommendations,
                hasError = result.isFailure,
            )
            if (result.isSuccess) {
                cacheRecommendations(cacheKey, recommendations)
            }
        }
    }
    fun clearMetadataSearch() {
        cancelMetadataSearch()
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

    fun updateBrowseMode(mode: HomeBrowseMode) {
        browseMode.value = mode
    }

    fun updateSortMode(mode: HomeSortMode) {
        sortMode.value = mode
        sortDirection.value = mode.defaultDirection()
    }

    fun updateSortDirection(direction: HomeSortDirection) {
        sortDirection.value = direction
    }

    fun updateAdvancedFilters(filters: HomeAdvancedFilters) {
        advancedFilters.value = filters
    }

    fun addObjective(objective: Objective) { viewModelScope.launch { mediaRepository.addObjective(objective) } }

    fun updateObjective(objective: Objective) { viewModelScope.launch { mediaRepository.updateObjective(objective) } }

    fun deleteObjective(objectiveId: Long) { viewModelScope.launch { mediaRepository.deleteObjective(objectiveId) } }

    fun startNewSession(request: AddTrackingSessionRequest) {
        viewModelScope.launch {
            mediaRepository.startNewSession(request)
        }
    }

    fun addTrackedMedia(request: AddTrackedMediaRequest) {
        viewModelScope.launch {
            val mediaItemId = mediaRepository.addTrackedMedia(request)
            mutableEvents.emit(HomeUiEvent.MediaItemCreated(mediaItemId))
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

    /**
     * UX-13 quick action: set the active session's progress to [newProgress].
     * Promotes a Planned/Paused session to In progress, and auto-completes (with an
     * undo affordance) when the value reaches the total. Plain increments give
     * immediate visual feedback via the list flow, so they don't raise a snackbar.
     */
    fun quickSetProgress(media: TrackedMedia, newProgress: Int) {
        val session = media.currentSession ?: return
        val item = media.item
        val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }
        val clamped = total?.let { newProgress.coerceIn(0, it) } ?: newProgress.coerceAtLeast(0)
        if (clamped == session.progressCurrent) return

        if (total != null && clamped >= total) {
            completeSession(session, progress = total, offerUndoFrom = session)
            return
        }

        val status = when (session.status) {
            TrackingStatus.Planned, TrackingStatus.Paused -> TrackingStatus.InProgress
            else -> session.status
        }
        val startedAt = session.startedAt
            ?: LocalDate.now().takeIf { status == TrackingStatus.InProgress }
        viewModelScope.launch {
            mediaRepository.updateSessionDetails(
                sessionId = session.id,
                status = status,
                progressCurrent = clamped,
                rating = session.rating,
                notes = session.notes,
                startedAt = startedAt,
                finishedAt = session.finishedAt,
            )
        }
    }

    /**
     * UX-17 quick action: start a planned title, or resume a paused one, from the dashboard.
     * Progress is untouched — this only moves the session to In progress, so the title moves into
     * `Ara mateix`.
     *
     * Only a Planned session gets today stamped as its start date, and only if it has none. A
     * paused session was started at some point in the past; stamping today would both misreport
     * the history and, because StatsCalculator buckets an unfinished session by its start date,
     * file a months-old title into the current period. If it has no start date, it keeps none.
     *
     * The tile is a single unguarded tap and the card leaves its section the moment it lands, so
     * this raises an undo snackbar rather than a confirmation: an accidental tap costs one tap to
     * reverse, and a deliberate one keeps its no-friction path.
     */
    fun quickStart(media: TrackedMedia) {
        val session = media.currentSession ?: return
        val startedAt = when (session.status) {
            TrackingStatus.Planned -> session.startedAt ?: LocalDate.now()
            TrackingStatus.Paused -> session.startedAt
            else -> return
        }
        viewModelScope.launch {
            mediaRepository.updateSessionDetails(
                sessionId = session.id,
                status = TrackingStatus.InProgress,
                progressCurrent = session.progressCurrent,
                rating = session.rating,
                notes = session.notes,
                startedAt = startedAt,
                finishedAt = session.finishedAt,
            )
            mutableEvents.emit(HomeUiEvent.SessionStartedReversible(session))
        }
    }

    /** UX-13 quick action: mark the active session completed, with undo. */
    fun quickComplete(media: TrackedMedia) {
        val session = media.currentSession ?: return
        val item = media.item
        val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }
        completeSession(session, progress = total ?: session.progressCurrent, offerUndoFrom = session)
    }

    /** Re-applies the pre-action session snapshot captured for the undo snackbar. */
    fun undoQuickProgress(previous: TrackingSession) {
        viewModelScope.launch {
            mediaRepository.updateSessionDetails(
                sessionId = previous.id,
                status = previous.status,
                progressCurrent = previous.progressCurrent,
                rating = previous.rating,
                notes = previous.notes,
                startedAt = previous.startedAt,
                finishedAt = previous.finishedAt,
            )
        }
    }

    private fun completeSession(
        session: TrackingSession,
        progress: Int,
        offerUndoFrom: TrackingSession,
    ) {
        viewModelScope.launch {
            mediaRepository.updateSessionDetails(
                sessionId = session.id,
                status = TrackingStatus.Completed,
                progressCurrent = progress,
                rating = session.rating,
                notes = session.notes,
                startedAt = session.startedAt,
                finishedAt = session.finishedAt ?: LocalDate.now(),
            )
            mutableEvents.emit(HomeUiEvent.SessionCompletedReversible(offerUndoFrom))
        }
    }

    fun deletePastSession(sessionId: Long) {
        viewModelScope.launch {
            mediaRepository.deletePastSession(sessionId)?.let { publishDeletionRecovery(it) }
        }
    }

    fun updateProgressUpdateDate(progressUpdateId: Long, loggedAt: LocalDate?) {
        viewModelScope.launch {
            mediaRepository.updateProgressUpdateDate(progressUpdateId, loggedAt)
        }
    }

    fun deleteProgressUpdate(progressUpdateId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteProgressUpdate(progressUpdateId)?.let { publishDeletionRecovery(it) }
        }
    }

    fun deleteMediaItem(mediaItemId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteMediaItem(mediaItemId)?.let { publishDeletionRecovery(it) }
        }
    }

    suspend fun restoreDeletion(token: Long): Result<Boolean> {
        val recovery = deletionRecoveryStore.take(token) ?: return Result.success(false)
        return runCatching { mediaRepository.restoreDeletion(recovery) }
    }

    fun expireDeletion(token: Long) {
        deletionRecoveryStore.discard(token)
    }

    fun addExternalRating(
        mediaItemId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    ) {
        viewModelScope.launch {
            mediaRepository.addExternalRating(mediaItemId, source, score, maxScore, voteCount, makePrimary)
        }
    }

    fun updateExternalRating(
        externalRatingId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
    ) {
        viewModelScope.launch {
            mediaRepository.updateExternalRating(externalRatingId, source, score, maxScore, voteCount, makePrimary)
        }
    }

    fun setPrimaryExternalRating(externalRatingId: Long) {
        viewModelScope.launch {
            mediaRepository.setPrimaryExternalRating(externalRatingId)
        }
    }

    fun deleteExternalRating(externalRatingId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteExternalRating(externalRatingId)
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

    fun updateCollectionItemOrder(collectionId: Long, itemOrders: List<CollectionItemOrder>) {
        viewModelScope.launch {
            mediaRepository.updateCollectionItemOrder(
                collectionId = collectionId,
                itemOrders = itemOrders,
            )
        }
    }

    fun updateMediaItemDetails(
        mediaItemId: Long,
        title: String,
        collectionId: Long?,
        newCollectionName: String?,
        collectionSortOrder: Double?,
        progressTotal: Int?,
        ownershipType: OwnershipType,
    ) {
        viewModelScope.launch {
            mediaRepository.updateMediaItemDetails(
                mediaItemId = mediaItemId,
                title = title,
                collectionId = collectionId,
                newCollectionName = newCollectionName,
                collectionSortOrder = collectionSortOrder,
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
        language: String?,
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
                language = language,
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

    suspend fun previewMediaItemMetadataRefresh(mediaItemId: Long): Result<MetadataRefreshPreview?> {
        if (refreshingMetadataItemId.value != null) return Result.success(null)
        refreshingMetadataItemId.value = mediaItemId
        val result = runCatching {
            mediaRepository.previewMediaItemMetadataRefresh(mediaItemId, metadataRepository)
        }
        refreshingMetadataItemId.value = null
        return result
    }

    suspend fun applyMediaItemMetadataRefresh(
        preview: MetadataRefreshPreview,
        selectedFields: Set<MetadataRefreshField>,
    ): Result<Boolean> {
        if (refreshingMetadataItemId.value != null) return Result.success(false)
        refreshingMetadataItemId.value = preview.mediaItemId
        val result = runCatching {
            mediaRepository.applyMediaItemMetadataRefresh(preview, selectedFields)
        }
        refreshingMetadataItemId.value = null
        return result
    }

    suspend fun searchMetadataLinkSuggestions(
        title: String,
        type: MediaType,
    ): List<MetadataSuggestion> {
        return metadataRepository.searchSuggestions(
            MetadataSearchRequest(
                query = title,
                mediaTypes = setOf(type),
            ),
        ).take(8)
    }

    suspend fun linkMediaItemMetadata(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
    ): Boolean {
        return mediaRepository.linkMediaItemMetadata(mediaItemId, suggestion, metadataRepository)
    }

    private fun cancelMetadataSearch() {
        metadataSearchJob?.cancel()
        metadataSearchJob = null
    }

    private suspend fun publishDeletionRecovery(recovery: DeletionRecovery) {
        val token = deletionRecoveryStore.put(recovery)
        when (recovery) {
            is DeletionRecovery.MediaItem -> mutableEvents.emit(
                HomeUiEvent.MediaItemDeletionAvailable(token, recovery.item.title),
            )
            is DeletionRecovery.PastSession -> mutableEvents.emit(
                HomeUiEvent.PastSessionDeletionAvailable(token, recovery.session.sessionNumber),
            )
            is DeletionRecovery.ProgressUpdate -> mutableEvents.emit(
                HomeUiEvent.ProgressUpdateDeletionAvailable(token),
            )
        }
    }
    private fun cachedRecommendations(key: RecommendationCacheKey): CachedRecommendations? {
        val cached = recommendationCache[key] ?: return null
        return if (
            System.currentTimeMillis() - cached.cachedAtEpochMillis <= RECOMMENDATION_CACHE_TTL_MILLIS
        ) {
            cached
        } else {
            recommendationCache.remove(key)
            null
        }
    }

    private fun cacheRecommendations(
        key: RecommendationCacheKey,
        recommendations: List<ExternalRecommendation>,
    ) {
        recommendationCache.remove(key)
        recommendationCache[key] = CachedRecommendations(
            recommendations = recommendations,
            cachedAtEpochMillis = System.currentTimeMillis(),
        )
        while (recommendationCache.size > MAX_RECOMMENDATION_CACHE_ENTRIES) {
            recommendationCache.entries.iterator().next().let { recommendationCache.remove(it.key) }
        }
    }
    private fun cachedMetadataSearch(key: MetadataSearchCacheKey): CachedMetadataSearch? {
        val cached = metadataSearchCache[key] ?: return null
        return if (System.currentTimeMillis() - cached.cachedAtEpochMillis <= METADATA_SEARCH_CACHE_TTL_MILLIS) {
            cached
        } else {
            metadataSearchCache.remove(key)
            null
        }
    }

    private fun cacheMetadataSearch(
        key: MetadataSearchCacheKey,
        suggestions: List<MetadataSuggestion>,
    ) {
        metadataSearchCache.remove(key)
        metadataSearchCache[key] = CachedMetadataSearch(
            suggestions = suggestions,
            cachedAtEpochMillis = System.currentTimeMillis(),
        )
        while (metadataSearchCache.size > MAX_METADATA_SEARCH_CACHE_ENTRIES) {
            metadataSearchCache.entries.iterator().next().let { metadataSearchCache.remove(it.key) }
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val metadataRepository: MetadataRepository,
        private val recommendationRepository: RecommendationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                mediaRepository = mediaRepository,
                metadataRepository = metadataRepository,
                recommendationRepository = recommendationRepository,
            ) as T
        }
    }
}

private data class MetadataSearchCacheKey(
    val query: String,
    val mediaTypes: Set<MediaType>,
) {
    constructor(request: MetadataSearchRequest) : this(
        query = request.query.trim().lowercase(),
        mediaTypes = request.mediaTypes,
    )
}

private data class CachedMetadataSearch(
    val suggestions: List<MetadataSuggestion>,
    val cachedAtEpochMillis: Long,
)

private data class RecommendationCacheKey(
    val source: MetadataSource,
    val externalId: String,
)

private data class CachedRecommendations(
    val recommendations: List<ExternalRecommendation>,
    val cachedAtEpochMillis: Long,
)

private const val MINIMUM_AUTOMATIC_SEARCH_LENGTH = 3
private const val METADATA_SEARCH_CACHE_TTL_MILLIS = 5 * 60 * 1_000L
private const val MAX_METADATA_SEARCH_CACHE_ENTRIES = 20
private const val RECOMMENDATION_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1_000L
private const val MAX_RECOMMENDATION_CACHE_ENTRIES = 20

sealed interface HomeUiEvent {
    data class MediaItemCreated(val mediaItemId: Long) : HomeUiEvent
    data class MediaItemDeletionAvailable(val deletionToken: Long, val title: String) : HomeUiEvent
    data class PastSessionDeletionAvailable(val deletionToken: Long, val sessionNumber: Int) : HomeUiEvent
    data class ProgressUpdateDeletionAvailable(val deletionToken: Long) : HomeUiEvent
    data class SessionCompletedReversible(val previous: TrackingSession) : HomeUiEvent

    /** [previous] carries the pre-start status, which decides the started/resumed wording. */
    data class SessionStartedReversible(val previous: TrackingSession) : HomeUiEvent
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

private fun List<TrackedMedia>.filterByAdvancedFilters(filters: HomeAdvancedFilters): List<TrackedMedia> {
    if (!filters.isActive) return this

    val selectedAuthors = filters.authors.map { it.normalizedFilterValue() }.toSet()
    val selectedGenres = filters.genres.map { it.normalizedFilterValue() }.toSet()

    return filter { trackedMedia ->
        val creators = trackedMedia.item.creators.map { it.normalizedFilterValue() }
        val genres = trackedMedia.item.genres.map { it.normalizedFilterValue() }
        val matchesAuthor = selectedAuthors.isEmpty() || creators.any { it in selectedAuthors }
        val matchesGenre = selectedGenres.isEmpty() || genres.any { it in selectedGenres }
        val matchesExternalRating = filters.minimumExternalRating?.let { minimum ->
            trackedMedia.item.externalRatingOnTen()?.let { it >= minimum } == true
        } ?: true
        val matchesUserRating = filters.minimumUserRating?.let { minimum ->
            (trackedMedia.currentSession?.rating ?: 0) >= minimum
        } ?: true

        matchesAuthor && matchesGenre && matchesExternalRating && matchesUserRating
    }
}

private fun String.normalizedFilterValue(): String = trim().lowercase()

private fun com.nilpo.contenttracker.core.model.MediaItem.externalRatingOnTen(): Double? {
    val score = externalRatingScore ?: return null
    val max = externalRatingMax ?: return null
    if (max <= 0.0) return null
    return score / max * 10.0
}

private fun List<TrackedMedia>.sortByMode(
    mode: HomeSortMode,
    direction: HomeSortDirection,
): List<TrackedMedia> {
    val comparator = when (mode) {
        HomeSortMode.Title -> compareBy<TrackedMedia> { it.item.title.lowercase() }
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
    val progressTotal = item.progressTotal.takeUnless { item.type == MediaType.Game }

    return if (progressTotal != null && progressTotal > 0) {
        progressCurrent.toDouble() / progressTotal.toDouble()
    } else {
        progressCurrent.toDouble()
    }
}

private fun HomeSortMode.defaultDirection(): HomeSortDirection {
    return when (this) {
        HomeSortMode.Title -> HomeSortDirection.Ascending
        HomeSortMode.Progress,
        HomeSortMode.Rating,
        HomeSortMode.Recent,
        -> HomeSortDirection.Descending
    }
}
