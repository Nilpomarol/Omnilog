package com.nilpo.contenttracker.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nilpo.contenttracker.core.cover.CoverRepository
import com.nilpo.contenttracker.core.model.AddTrackedMediaRequest
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.core.mal.MalSyncManager
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportCompletionSummary
import com.nilpo.contenttracker.core.imports.ImportEnrichmentManager
import com.nilpo.contenttracker.core.imports.ImportReviewApplyOutcome
import com.nilpo.contenttracker.core.imports.ImportReviewDraft
import com.nilpo.contenttracker.core.imports.ProviderReference
import com.nilpo.contenttracker.core.refresh.MetadataRefreshManager
import com.nilpo.contenttracker.core.refresh.MetadataRefreshState
import com.nilpo.contenttracker.core.repository.CollectionItemOrder
import com.nilpo.contenttracker.core.repository.DeletionRecovery
import com.nilpo.contenttracker.core.repository.DeletionRecoveryStore
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.ImdbCsvImportResult
import com.nilpo.contenttracker.core.repository.MediaRepository
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.MetadataRepository
import com.nilpo.contenttracker.core.repository.RecommendationRepository
import com.nilpo.contenttracker.core.repository.MyAnimeListXmlImportResult
import com.nilpo.contenttracker.core.repository.MyAnimeListAccountImportPreview
import com.nilpo.contenttracker.core.repository.ProviderImportResult
import com.nilpo.contenttracker.core.repository.PreparedImdbCsvImport
import com.nilpo.contenttracker.core.repository.PreparedMyAnimeListXmlImport
import com.nilpo.contenttracker.core.repository.PreparedStoryGraphCsvImport
import com.nilpo.contenttracker.core.repository.StoryGraphCsvImportResult
import com.nilpo.contenttracker.core.timeline.TimelineBuilder
import com.nilpo.contenttracker.core.timeline.TimelineEntry
import com.nilpo.contenttracker.ui.add.MetadataSearchUiState
import com.nilpo.contenttracker.ui.common.CompletionReaction
import com.nilpo.contenttracker.ui.common.ObjectiveStep
import com.nilpo.contenttracker.ui.common.objectiveSteps
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.StatusReaction
import com.nilpo.contenttracker.ui.common.completionReaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import com.nilpo.contenttracker.core.model.persistableImageUrls
import com.nilpo.contenttracker.core.model.contributorDirectory
import com.nilpo.contenttracker.core.model.ContributorDirectory
import com.nilpo.contenttracker.core.model.RatingHalfPoints

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val metadataRepository: MetadataRepository,
    private val recommendationRepository: RecommendationRepository,
    private val coverRepository: CoverRepository,
    private val malSyncManager: MalSyncManager,
    private val importEnrichmentManager: ImportEnrichmentManager,
    private val metadataRefreshManager: MetadataRefreshManager,
) : ViewModel() {
    private val selectedSection = MutableStateFlow(MediaSection.Anime)
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<TrackingStatus?>(null)
    private val browseMode = MutableStateFlow(HomeBrowseMode.Items)
    private val displayMode = MutableStateFlow(HomeDisplayMode.List)
    private val sortMode = MutableStateFlow(HomeSortMode.Recent)
    private val sortDirection = MutableStateFlow(HomeSortDirection.Descending)
    private val advancedFilters = MutableStateFlow(HomeAdvancedFilters())
    private val metadataSearchState = MutableStateFlow(MetadataSearchUiState())
    private var metadataSearchJob: Job? = null
    private var metadataDetailsJob: Job? = null
    private val metadataSearchCache = LinkedHashMap<MetadataSearchCacheKey, CachedMetadataSearch>()
    private val recommendationState = MutableStateFlow(RecommendationUiState())
    private var recommendationJob: Job? = null
    private val recommendationCache = LinkedHashMap<RecommendationCacheKey, CachedRecommendations>()
    private val refreshingMetadataItemId = MutableStateFlow<Long?>(null)
    private val mutableEvents = MutableSharedFlow<HomeUiEvent>()
    private val deletionRecoveryStore = DeletionRecoveryStore()

    val metadataUiState = metadataSearchState.asStateFlow()
    val recommendationUiState = recommendationState.asStateFlow()
    val malSyncState = malSyncManager.state
    val importEnrichmentState = importEnrichmentManager.state
    val metadataRefreshState: StateFlow<MetadataRefreshState> = metadataRefreshManager.state
    val events = mutableEvents.asSharedFlow()

    fun beginMalAuthorization(): String? = malSyncManager.beginAuthorization()

    fun handleMalAuthorizationRedirect(uri: Uri) {
        viewModelScope.launch { malSyncManager.handleAuthorizationRedirect(uri) }
    }

    fun enableAndSyncMyAnimeList() {
        viewModelScope.launch { malSyncManager.enableAndSyncAll() }
    }

    fun retryMyAnimeListChanges() {
        viewModelScope.launch { malSyncManager.retryUnfinishedChanges() }
    }

    fun cancelMyAnimeListChanges() {
        viewModelScope.launch { malSyncManager.cancelUnfinishedChanges() }
    }

    fun disconnectMyAnimeList() = malSyncManager.disconnect()

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
            isLoading = false,
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
        .combine(displayMode) { state, selectedDisplayMode ->
            state.copy(displayMode = selectedDisplayMode)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /**
     * Derived from [uiState] rather than its own repository query so both share one observation of
     * the library. [distinctUntilChanged] matters here: [uiState] re-emits on every keystroke and
     * filter change, none of which affect the timeline.
     */
    val timelineEntries: StateFlow<List<TimelineEntry>> = uiState
        .map { it.allTrackedItems }
        .distinctUntilChanged()
        .map { TimelineBuilder().buildEntries(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Contributor facts for the detail page's credit lists, derived on the same terms as
     * [timelineEntries].
     *
     * These answers depend on the whole library, so working them out inside composition meant a
     * scan per credit role on every emission — including the keystrokes and filter changes that
     * [distinctUntilChanged] absorbs here.
     */
    val contributorDirectory: StateFlow<ContributorDirectory> = uiState
        .map { it.allTrackedItems }
        .distinctUntilChanged()
        .map { it.contributorDirectory() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ContributorDirectory.Empty,
        )

    suspend fun exportBackupJson(): String {
        return mediaRepository.exportBackupJson()
    }

    suspend fun previewBackupJson(json: String): BackupPreview {
        return mediaRepository.previewBackupJson(json)
    }

    suspend fun importBackupJson(json: String) {
        mediaRepository.importBackupJson(json)
        synchronizeLibraryCoversInBackground()
    }

    suspend fun prepareImdbCsv(csv: String): PreparedImdbCsvImport = mediaRepository.prepareImdbCsv(csv)

    suspend fun importPreparedImdbCsv(prepared: PreparedImdbCsvImport): ImdbCsvImportResult =
        mediaRepository.importPreparedImdbCsv(prepared).startImportedMediaBackgroundWork()

    suspend fun prepareStoryGraphCsv(csv: String): PreparedStoryGraphCsvImport =
        mediaRepository.prepareStoryGraphCsv(csv)

    suspend fun importPreparedStoryGraphCsv(prepared: PreparedStoryGraphCsvImport): StoryGraphCsvImportResult =
        mediaRepository.importPreparedStoryGraphCsv(prepared).startImportedMediaBackgroundWork()

    suspend fun prepareMyAnimeListXml(xml: String): PreparedMyAnimeListXmlImport =
        mediaRepository.prepareMyAnimeListXml(xml)

    suspend fun importPreparedMyAnimeListXml(
        prepared: PreparedMyAnimeListXmlImport,
    ): MyAnimeListXmlImportResult =
        mediaRepository.importPreparedMyAnimeListXml(prepared).startImportedMediaBackgroundWork()

    suspend fun previewMyAnimeListAccount(): Result<MyAnimeListAccountImportPreview> {
        return malSyncManager.fetchAccountImportRows().mapCatching { loaded ->
            val preview = mediaRepository.previewMyAnimeListAccount(loaded.items)
            MyAnimeListAccountImportPreview(
                items = loaded.items,
                preview = preview.copy(
                    totalRows = loaded.totalRows,
                    invalidRows = loaded.invalidRows,
                ),
            )
        }
    }

    suspend fun importMyAnimeListAccount(
        preview: MyAnimeListAccountImportPreview,
    ): ProviderImportResult {
        val result = mediaRepository.importMyAnimeListAccount(preview.items).startImportedMediaBackgroundWork()
        malSyncManager.discardAccountImportStage()
        return result
    }

    suspend fun discardMyAnimeListAccountImport() {
        malSyncManager.discardAccountImportStage()
    }

    private fun ProviderImportResult.startImportedMediaBackgroundWork(): ProviderImportResult {
        // The import is already durable. If WorkManager is temporarily unavailable, startup recovery
        // will enqueue the persisted batch later; the UI must not misreport a completed import as lost.
        runCatching { importEnrichmentManager.enqueue(importBatchId) }
        synchronizeLibraryCoversInBackground()
        return this
    }

    fun toggleImportEnrichment() {
        val batch = importEnrichmentState.value.activeBatch ?: return
        toggleImportEnrichment(batch.batchId)
    }

    fun toggleImportEnrichment(batchId: Long) {
        val batch = importEnrichmentState.value.recentBatches.firstOrNull { it.batchId == batchId }
            ?: return
        viewModelScope.launch {
            if (batch.state == ImportBatchState.Paused) {
                importEnrichmentManager.resume(batch.batchId)
            } else {
                importEnrichmentManager.pause(batch.batchId)
            }
        }
    }

    fun retryImportEnrichmentIssues(batchId: Long) {
        viewModelScope.launch { importEnrichmentManager.retryIssues(batchId) }
    }

    suspend fun retryImportEnrichmentIssue(itemId: Long): Result<Unit> =
        importEnrichmentManager.retryIssue(itemId)

    suspend fun skipImportEnrichmentIssue(itemId: Long): Result<Unit> =
        importEnrichmentManager.skipIssue(itemId)

    suspend fun dismissImportMetadataCoverage(itemId: Long): Result<Unit> =
        importEnrichmentManager.dismissCoverage(itemId)

    suspend fun retryImportMetadataCoverage(itemId: Long): Result<Unit> =
        importEnrichmentManager.retryCoverage(itemId)

    fun currentImportCompletion(batchId: Long): ImportCompletionSummary? =
        importEnrichmentState.value.pendingCompletion?.takeIf { it.batchId == batchId }

    suspend fun acknowledgeImportCompletion(batchId: Long) {
        importEnrichmentManager.acknowledgeCompletion(batchId)
    }

    suspend fun deleteImportHistoryBatch(batchId: Long): Result<Unit> =
        importEnrichmentManager.deleteHistoryBatch(batchId)

    suspend fun clearFinishedImportHistory(): Result<Int> =
        importEnrichmentManager.clearFinishedHistory()

    suspend fun completeImportIssueAfterManualLink(
        itemId: Long,
        suggestion: MetadataSuggestion,
    ): Result<Unit> = importEnrichmentManager.completeIssueAfterManualLink(itemId, suggestion)

    fun cancelImportEnrichment(batchId: Long) {
        viewModelScope.launch { importEnrichmentManager.cancelEnrichment(batchId) }
    }

    suspend fun prepareImportReview(
        itemId: Long,
        reference: ProviderReference?,
    ): Result<ImportReviewDraft?> = importEnrichmentManager.prepareReview(itemId, reference)

    suspend fun applyImportReview(
        draft: ImportReviewDraft,
        selectedFields: Set<MetadataRefreshField>,
    ): Result<ImportReviewApplyOutcome> = importEnrichmentManager.applyReview(draft, selectedFields)

    suspend fun skipImportReview(itemId: Long): Result<Unit> =
        importEnrichmentManager.skipReview(itemId)

    suspend fun refreshAnimeTitleLanguage(): Result<Int> =
        importEnrichmentManager.enqueueAnimeTitleRefresh()

    suspend fun startBulkMetadataRefresh() = metadataRefreshManager.start()

    fun cancelBulkMetadataRefresh(runId: Long) {
        viewModelScope.launch { metadataRefreshManager.cancel(runId) }
    }

    fun selectSection(section: MediaSection) {
        cancelMetadataSearch()
        selectedSection.value = section
        searchQuery.value = ""
        browseMode.value = HomeBrowseMode.Items
        // Filter values belong to a section: an author, a page span or a year range picked in Books
        // would otherwise silently hide items in Anime.
        advancedFilters.value = HomeAdvancedFilters()
        metadataSearchState.value = MetadataSearchUiState()
    }

    fun selectSectionWithSearch(section: MediaSection, query: String) {
        cancelMetadataSearch()
        selectedSection.value = section
        searchQuery.value = query
        browseMode.value = HomeBrowseMode.Items
        advancedFilters.value = HomeAdvancedFilters()
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
            prefetchCovers(cached.suggestions.map { it.coverUrl })
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = cached.suggestions,
                isLoading = false,
                hasSearched = true,
                hasError = false,
                failedSources = emptySet(),
            )
            return
        }

        val queryAtRequestStart = metadataSearchState.value.query
        metadataSearchState.value = metadataSearchState.value.copy(
            suggestions = emptyList(),
            isLoading = true,
            hasSearched = true,
            hasError = false,
            failedSources = emptySet(),
        )
        metadataSearchJob = viewModelScope.launch {
            val result = runCatching { metadataRepository.searchSuggestionsWithDiagnostics(request) }
            if (!isActive || selectedSection.value != section || metadataSearchState.value.query != queryAtRequestStart) {
                return@launch
            }
            val searchResult = result.getOrNull()
            val hasProviderFailure = searchResult?.failedSources?.isNotEmpty() == true
            val hasResults = searchResult?.suggestions?.isNotEmpty() == true
            prefetchCovers(searchResult?.suggestions.orEmpty().map { it.coverUrl })
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = searchResult?.suggestions.orEmpty(),
                selectedSuggestion = null,
                isLoading = false,
                hasError = result.isFailure || (hasProviderFailure && !hasResults),
                failedSources = if (hasResults) searchResult?.failedSources.orEmpty() else emptySet(),
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
                prefetchCovers(cached.recommendations.map { it.suggestion.coverUrl })
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
            prefetchCovers(recommendations.map { it.suggestion.coverUrl })
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
        metadataDetailsJob?.cancel()
        metadataSearchState.value = metadataSearchState.value.copy(
            selectedSuggestion = suggestion,
            isLoadingDetails = true,
            hasDetailsError = false,
        )

        metadataDetailsJob = viewModelScope.launch {
            val result = runCatching {
                metadataRepository.getSuggestionDetails(suggestion)
            }
            // A slower response for a previously selected suggestion must not overwrite the
            // one the user is now waiting on, or they land on the review step for the wrong item.
            if (!isActive || !metadataSearchState.value.selectedSuggestion.isSameSuggestionAs(suggestion)) {
                return@launch
            }
            val detailedSuggestion = result.getOrDefault(suggestion)
            prefetchCovers(listOf(detailedSuggestion.coverUrl))
            metadataSearchState.value = metadataSearchState.value.copy(
                suggestions = metadataSearchState.value.suggestions.map { existingSuggestion ->
                    if (existingSuggestion.isSameSuggestionAs(detailedSuggestion)) {
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

    /** Retries the details load for the suggestion the user is currently on. */
    fun retryMetadataSuggestionDetails() {
        val suggestion = metadataSearchState.value.selectedSuggestion ?: return
        selectMetadataSuggestion(suggestion)
    }

    fun updateStatusFilter(status: TrackingStatus?) {
        statusFilter.value = status
    }

    fun updateBrowseMode(mode: HomeBrowseMode) {
        browseMode.value = mode
    }

    fun updateDisplayMode(mode: HomeDisplayMode) {
        displayMode.value = mode
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
            persistCover(request.coverUrl)
        }
    }

    fun updateSessionDetails(
        sessionId: Long,
        status: TrackingStatus,
        progressCurrent: Int,
        ratingHalfPoints: Int?,
        notes: String?,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ) {
        // Snapshot before the write: the reaction describes the change, and the objective steps
        // compare against the counts from before it.
        val before = uiState.value
        val media = before.allTrackedItems.firstOrNull { item -> item.sessions.any { it.id == sessionId } }
        val session = media?.sessions?.firstOrNull { it.id == sessionId }
        viewModelScope.launch {
            val recovery = mediaRepository.updateSessionDetails(
                sessionId = sessionId,
                status = status,
                progressCurrent = progressCurrent,
                ratingHalfPoints = ratingHalfPoints,
                notes = notes,
                startedAt = startedAt,
                finishedAt = finishedAt,
            ) ?: return@launch refuseChange()
            if (media == null || session == null) return@launch
            publishSessionReaction(
                before = before,
                media = media,
                session = session,
                recovery = recovery,
                status = status,
                progress = progressCurrent,
                startedAt = startedAt,
                finishedAt = finishedAt,
                canUndo = recovery.isStatusAndProgressOnly(),
            )
        }
    }

    /**
     * UX-13 quick action: commit the quick sheet's draft, setting the session's progress to
     * [newProgress]. This is the single write behind every state the sheet's primary button can
     * show — save, start, resume and complete all land here, because from the session's point of
     * view they differ only in the numbers involved.
     *
     * Promotes a Planned/Paused session to In progress, and completes (with an undo affordance)
     * when the value reaches the total.
     *
     * Only a Planned session gets today stamped as its start date, and only if it has none. A
     * paused session was started at some point in the past; stamping today would both misreport
     * the history and, because StatsCalculator buckets an unfinished session by its start date,
     * file a months-old title into the current period. If it has no start date, it keeps none.
     */
    fun quickCommitProgress(media: TrackedMedia, newProgress: Int) {
        val session = media.currentSession ?: return
        val item = media.item
        val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }
        val clamped = total?.let { newProgress.coerceIn(0, it) } ?: newProgress.coerceAtLeast(0)
        val promotes = session.status == TrackingStatus.Planned ||
            session.status == TrackingStatus.Paused
        // A promotion is itself a change worth writing, so only a genuinely idle commit bails out.
        if (!promotes && clamped == session.progressCurrent) return

        if (total != null && clamped >= total) {
            completeSession(
                media = media,
                session = session,
                progress = total,
                // Completing straight out of Planned would otherwise leave a finished session with
                // no start date at all.
                startedAt = session.startedAt
                    ?: LocalDate.now().takeIf { session.status == TrackingStatus.Planned },
            )
            return
        }

        val startedAt = when (session.status) {
            TrackingStatus.Planned -> session.startedAt ?: LocalDate.now()
            TrackingStatus.Paused -> session.startedAt
            else -> session.startedAt ?: LocalDate.now()
        }
        val before = uiState.value
        val status = if (promotes) TrackingStatus.InProgress else session.status
        viewModelScope.launch {
            val recovery = mediaRepository.updateSessionDetails(
                sessionId = session.id,
                status = status,
                progressCurrent = clamped,
                ratingHalfPoints = session.ratingHalfPoints,
                notes = session.notes,
                startedAt = startedAt,
                finishedAt = session.finishedAt,
            ) ?: return@launch refuseChange()
            publishSessionReaction(before, media, session, recovery, status, clamped, startedAt, session.finishedAt)
        }
    }

    /**
     * UX-13 quick action: mark the session completed at [progress], with undo.
     *
     * The value matters for the total-less types: a game finished at 42 hours has no total to fall
     * back on, so taking the sheet's draft is the only way those 42 hours survive the completion.
     * Anything with a total finishes at the total regardless of what the draft said.
     */
    fun quickComplete(media: TrackedMedia, completion: QuickCompletion) {
        val session = media.currentSession ?: return
        val item = media.item
        val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }
        completeSession(
            media = media,
            session = session,
            progress = total ?: completion.progress.coerceAtLeast(0),
            startedAt = session.startedAt
                ?: LocalDate.now().takeIf { session.status == TrackingStatus.Planned },
            ratingHalfPoints = completion.ratingHalfPoints,
            finishedAt = completion.finishedAt,
        )
    }

    private fun completeSession(
        media: TrackedMedia,
        session: TrackingSession,
        progress: Int,
        startedAt: LocalDate? = session.startedAt,
        ratingHalfPoints: Int? = session.ratingHalfPoints,
        finishedAt: LocalDate = session.finishedAt ?: LocalDate.now(),
    ) {
        val before = uiState.value
        viewModelScope.launch {
            val recovery = mediaRepository.updateSessionDetails(
                sessionId = session.id,
                status = TrackingStatus.Completed,
                progressCurrent = progress,
                ratingHalfPoints = ratingHalfPoints,
                notes = session.notes,
                startedAt = startedAt,
                finishedAt = finishedAt,
            ) ?: return@launch refuseChange()
            publishSessionReaction(before, media, session, recovery, TrackingStatus.Completed, progress, startedAt, finishedAt)
        }
    }

    /**
     * Picks the one reaction a session write earns and hands it the write's undo. Finishing gets the
     * completion page (which carries any goal it reached); a goal reached any other way gets its own
     * page; a change of status or of progress gets the card. A write that earns none — notes, a
     * rating, a date — drops its recovery.
     */
    private suspend fun publishSessionReaction(
        before: HomeUiState,
        media: TrackedMedia,
        session: TrackingSession,
        recovery: DeletionRecovery,
        status: TrackingStatus,
        progress: Int,
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
        canUndo: Boolean = true,
    ) {
        val steps = awaitObjectiveSteps(before)
        val reached = steps.filter { it.reached }
        val event: (Long?) -> HomeUiEvent = when {
            status == TrackingStatus.Completed && session.status != TrackingStatus.Completed -> { token ->
                HomeUiEvent.SessionCompletedReversible(
                    token,
                    completionReaction(media, session, startedAt, finishedAt ?: LocalDate.now(), steps),
                )
            }
            reached.isNotEmpty() -> { token -> HomeUiEvent.ObjectiveReached(reached, token) }
            status != session.status || progress != session.progressCurrent -> { token ->
                HomeUiEvent.SessionStatusChangedReversible(token, media.statusReaction(session, status, progress))
            }
            else -> return
        }
        mutableEvents.emit(event(recovery.takeIf { canUndo }?.let(deletionRecoveryStore::put)))
    }

    /**
     * The objectives a write just moved. The write has returned by now, but the library flow re-reads
     * it asynchronously, so this waits for that emission before comparing.
     */
    private suspend fun awaitObjectiveSteps(before: HomeUiState): List<ObjectiveStep> {
        val after = withTimeoutOrNull(2_000) {
            uiState.first { it.allTrackedItems !== before.allTrackedItems }
        } ?: return emptyList()
        return objectiveSteps(before.allTrackedItems, after.allTrackedItems, after.objectives)
    }

    fun deletePastSession(sessionId: Long) {
        viewModelScope.launch {
            mediaRepository.deletePastSession(sessionId)?.let { publishDeletionRecovery(it) }
        }
    }

    fun deleteCurrentSession(sessionId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteCurrentSession(sessionId)?.let { publishDeletionRecovery(it) }
        }
    }

    fun updateProgressUpdate(
        progressUpdateId: Long,
        amount: Int,
        loggedAt: LocalDate?,
        coversPeriod: Boolean,
    ) {
        val before = uiState.value
        viewModelScope.launch {
            if (!mediaRepository.updateProgressUpdate(progressUpdateId, amount, loggedAt, coversPeriod)) {
                return@launch refuseChange()
            }
            // Re-dating or enlarging an entry can carry a goal over its target too; there is no undo here.
            awaitObjectiveSteps(before).filter { it.reached }.takeIf { it.isNotEmpty() }
                ?.let { mutableEvents.emit(HomeUiEvent.ObjectiveReached(it, recoveryToken = null)) }
        }
    }

    fun updateSessionStatusEventDate(eventId: Long, occurredOn: LocalDate?) {
        val before = uiState.value
        viewModelScope.launch {
            if (!mediaRepository.updateSessionStatusEventDate(eventId, occurredOn)) return@launch refuseChange()
            awaitObjectiveSteps(before).filter { it.reached }.takeIf { it.isNotEmpty() }
                ?.let { mutableEvents.emit(HomeUiEvent.ObjectiveReached(it, recoveryToken = null)) }
        }
    }

    /** Corrects a mistaken pause. See `MediaRepository.deleteSessionStatusEvent`. */
    fun deleteSessionStatusEvent(eventId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteSessionStatusEvent(eventId)?.let { publishDeletionRecovery(it) } ?: refuseChange()
        }
    }

    fun deleteProgressUpdate(progressUpdateId: Long) {
        viewModelScope.launch {
            mediaRepository.deleteProgressUpdate(progressUpdateId)?.let { publishDeletionRecovery(it) } ?: refuseChange()
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
            .onSuccess { restored ->
                if (restored && recovery is DeletionRecovery.MediaItem) {
                    persistCoverInBackground(recovery.item.coverUrl)
                }
            }
    }

    fun expireDeletion(token: Long) {
        deletionRecoveryStore.discard(token)
        synchronizeLibraryCoversInBackground()
    }

    fun addExternalRating(
        mediaItemId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
        customSourceName: String?,
    ) {
        viewModelScope.launch {
            mediaRepository.addExternalRating(mediaItemId, source, score, maxScore, voteCount, makePrimary, customSourceName)
        }
    }

    fun updateExternalRating(
        externalRatingId: Long,
        source: ExternalRatingSource,
        score: Double,
        maxScore: Double,
        voteCount: Int?,
        makePrimary: Boolean,
        customSourceName: String?,
    ) {
        viewModelScope.launch {
            mediaRepository.updateExternalRating(externalRatingId, source, score, maxScore, voteCount, makePrimary, customSourceName)
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
        isOwned: Boolean,
    ) {
        viewModelScope.launch {
            mediaRepository.updateMediaItemDetails(
                mediaItemId = mediaItemId,
                title = title,
                collectionId = collectionId,
                newCollectionName = newCollectionName,
                collectionSortOrder = collectionSortOrder,
                progressTotal = progressTotal,
                isOwned = isOwned,
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
        credits: List<MediaCredit>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
        steamAppId: String?,
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
                credits = credits,
                coverUrl = coverUrl,
                synopsis = synopsis,
                sourceUrl = sourceUrl,
                steamAppId = steamAppId,
            )
            persistCover(coverUrl)
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

            if (result.getOrDefault(false)) {
                synchronizeLibraryCoversInBackground()
            }

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
        if (result.getOrDefault(false) && MetadataRefreshField.Cover in selectedFields) {
            persistCoverInBackground(preview.refreshed.coverUrl)
        }
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
        ).take(8).also { suggestions ->
            prefetchCovers(suggestions.map { it.coverUrl })
        }
    }

    suspend fun linkMediaItemMetadata(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
    ): Boolean {
        return mediaRepository.linkMediaItemMetadata(mediaItemId, suggestion, metadataRepository).also { linked ->
            if (linked) persistCoverInBackground(suggestion.coverUrl)
        }
    }

    suspend fun previewMediaItemMetadataLink(
        mediaItemId: Long,
        suggestion: MetadataSuggestion,
    ): Result<MetadataRefreshPreview?> {
        if (refreshingMetadataItemId.value != null) return Result.success(null)
        refreshingMetadataItemId.value = mediaItemId
        val result = runCatching {
            mediaRepository.previewMediaItemMetadataLink(mediaItemId, suggestion, metadataRepository)
        }
        refreshingMetadataItemId.value = null
        return result
    }

    private fun prefetchCovers(coverUrls: Iterable<String?>) {
        coverRepository.prefetch(coverUrls)
    }

    private suspend fun persistCover(coverUrl: String?) {
        coverUrl?.let { coverRepository.persist(it) }
    }

    private fun persistCoverInBackground(coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) return
        viewModelScope.launch { persistCover(coverUrl) }
    }

    private fun synchronizeLibraryCoversInBackground() {
        viewModelScope.launch {
            val imageUrls = mediaRepository
                .observeTrackedMedia(MediaType.entries.toSet())
                .first()
                .persistableImageUrls()
            coverRepository.persistAll(imageUrls)
            coverRepository.removeOrphans(imageUrls)
        }
    }

    private fun cancelMetadataSearch() {
        metadataSearchJob?.cancel()
        metadataSearchJob = null
        // A new or cleared search invalidates any details load started from the old results.
        metadataDetailsJob?.cancel()
        metadataDetailsJob = null
    }

    /** A write the repository declined. Saying so beats a save that silently did nothing. */
    private suspend fun refuseChange() {
        mutableEvents.emit(HomeUiEvent.ChangeRefused)
    }

    private suspend fun publishDeletionRecovery(recovery: DeletionRecovery) {
        require(recovery !is DeletionRecovery.SessionMutation) {
            "Session mutations use their contextual quick-action event"
        }
        val token = deletionRecoveryStore.put(recovery)
        when (recovery) {
            is DeletionRecovery.MediaItem -> mutableEvents.emit(
                HomeUiEvent.MediaItemDeletionAvailable(token, recovery.item.title),
            )
            is DeletionRecovery.PastSession -> mutableEvents.emit(
                HomeUiEvent.PastSessionDeletionAvailable(token, recovery.visitNumber),
            )
            is DeletionRecovery.ProgressUpdate -> mutableEvents.emit(
                HomeUiEvent.ProgressUpdateDeletionAvailable(token),
            )
            is DeletionRecovery.SessionStatusEvents -> mutableEvents.emit(
                HomeUiEvent.StatusEventDeletionAvailable(
                    deletionToken = token,
                    previousStatus = recovery.events.singleOrNull()
                        ?.previousStatus
                        .toTrackingStatusOrNull(),
                    status = recovery.events.singleOrNull()
                        ?.status
                        .toTrackingStatusOrNull(),
                ),
            )
            is DeletionRecovery.SessionMutation -> error("Unreachable")
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
        private val coverRepository: CoverRepository,
        private val malSyncManager: MalSyncManager,
        private val importEnrichmentManager: ImportEnrichmentManager,
        private val metadataRefreshManager: MetadataRefreshManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                mediaRepository = mediaRepository,
                metadataRepository = metadataRepository,
                recommendationRepository = recommendationRepository,
                coverRepository = coverRepository,
                malSyncManager = malSyncManager,
                importEnrichmentManager = importEnrichmentManager,
                metadataRefreshManager = metadataRefreshManager,
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
    data object ChangeRefused : HomeUiEvent
    data class MediaItemCreated(val mediaItemId: Long) : HomeUiEvent
    data class MediaItemDeletionAvailable(val deletionToken: Long, val title: String) : HomeUiEvent
    data class PastSessionDeletionAvailable(val deletionToken: Long, val visitNumber: Int) : HomeUiEvent
    data class ProgressUpdateDeletionAvailable(val deletionToken: Long) : HomeUiEvent
    data class StatusEventDeletionAvailable(
        val deletionToken: Long,
        val previousStatus: TrackingStatus?,
        val status: TrackingStatus?,
    ) : HomeUiEvent
    data class SessionCompletedReversible(
        /** Null when the editor save also changed independent personal fields. */
        val recoveryToken: Long?,
        val reaction: CompletionReaction,
    ) : HomeUiEvent

    /**
     * Every goal one write reached, most meaningful first; the UI celebrates the first it has not
     * celebrated before. [recoveryToken] is null when the write has no undo.
     */
    data class ObjectiveReached(val steps: List<ObjectiveStep>, val recoveryToken: Long?) : HomeUiEvent

    data class SessionStatusChangedReversible(
        /** Null when the editor save also changed independent personal fields. */
        val recoveryToken: Long?,
        val reaction: StatusReaction,
    ) : HomeUiEvent
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
            trackedMedia.creatorNames().any { it.contains(normalizedQuery, ignoreCase = true) }
    }
}

private fun List<TrackedMedia>.filterByStatus(status: TrackingStatus?): List<TrackedMedia> {
    return status?.let { selectedStatus ->
        filter { trackedMedia -> trackedMedia.currentSession?.status == selectedStatus }
    } ?: this
}

/** Suggestions are identified by the source and external id the providers round-trip. */
private fun MetadataSuggestion?.isSameSuggestionAs(other: MetadataSuggestion?): Boolean {
    if (this == null || other == null) return false
    return source == other.source && externalId == other.externalId
}

internal fun List<TrackedMedia>.filterByAdvancedFilters(filters: HomeAdvancedFilters): List<TrackedMedia> {
    if (!filters.isActive) return this

    val selectedAuthors = filters.authors.map { it.normalizedFilterValue() }.toSet()
    val selectedGenres = filters.genres.map { it.normalizedFilterValue() }.toSet()

    return filter { trackedMedia ->
        val item = trackedMedia.item
        val matchesType = filters.types.isEmpty() || item.type in filters.types
        val matchesPlatform = filters.platformTypes.isEmpty() ||
            trackedMedia.currentSession?.platform?.type?.let { it in filters.platformTypes } == true
        // A title without a year or a total cannot be placed in a span, so it never matches one.
        val matchesReleaseYear = filters.releaseYears?.let { years ->
            item.releaseYear?.let { it in years } == true
        } ?: true
        val matchesLength = filters.lengthRange?.let { range ->
            item.progressTotal?.let { it in range } == true
        } ?: true
        val matchesOwned = !filters.ownedOnly || item.isOwned
        if (!(matchesType && matchesPlatform && matchesReleaseYear && matchesLength && matchesOwned)) return@filter false

        val creators = trackedMedia.creatorNames().map { it.normalizedFilterValue() }
        val genres = trackedMedia.item.genres.map { it.normalizedFilterValue() }
        val matchesAuthor = selectedAuthors.isEmpty() || creators.any { it in selectedAuthors }
        val matchesGenre = selectedGenres.isEmpty() || genres.any { it in selectedGenres }
        val matchesExternalRating = filters.minimumExternalRating?.let { minimum ->
            trackedMedia.item.externalRatingOnTen()?.let { it >= minimum } == true
        } ?: true
        val matchesUserRating = filters.minimumUserRating?.let { minimum ->
            // The filter asks for whole points; the rating is half points, so compare scores.
            val score = trackedMedia.currentSession?.ratingHalfPoints
                ?.let(RatingHalfPoints::toScore)
                ?: 0.0
            score >= minimum
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

internal fun List<TrackedMedia>.sortByMode(
    mode: HomeSortMode,
    direction: HomeSortDirection,
): List<TrackedMedia> {
    val comparator = when (mode) {
        HomeSortMode.Title -> compareBy<TrackedMedia> { it.item.title.lowercase() }
        HomeSortMode.Progress -> compareBy<TrackedMedia> { it.progressSortValue() }
            .thenBy { it.item.title.lowercase() }
        HomeSortMode.Rating -> compareBy<TrackedMedia> { it.currentSession?.ratingHalfPoints ?: 0 }
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

internal fun HomeSortMode.defaultDirection(): HomeSortDirection {
    return when (this) {
        HomeSortMode.Title -> HomeSortDirection.Ascending
        HomeSortMode.Progress,
        HomeSortMode.Rating,
        HomeSortMode.Recent,
        -> HomeSortDirection.Descending
    }
}

private fun TrackedMedia.statusReaction(session: TrackingSession, status: TrackingStatus, progress: Int) = StatusReaction(
    title = item.title,
    coverUrl = item.coverUrl,
    previousStatus = session.status,
    status = status,
    mediaType = item.type,
    previousProgress = session.progressCurrent,
    progress = progress,
    // Games log hours against no fixed length, so they never draw a bar.
    progressTotal = item.progressTotal?.takeUnless { item.type == MediaType.Game },
)

private fun String?.toTrackingStatusOrNull(): TrackingStatus? =
    TrackingStatus.entries.firstOrNull { it.name == this }

/**
 * The repository's session-mutation recovery restores a whole snapshot. The full editor may save
 * personal fields alongside a status/progress change, so only hand that recovery to the UI when it
 * cannot erase a rating, note, or date from the same save.
 *
 * A date the transition itself fills in is part of the change, not a personal edit: completing
 * writes a finish date where there was none, reopening clears it, and leaving Planned stamps a start
 * where there was none. Undo puts those back to empty, so nothing the user had before is lost.
 */
internal fun DeletionRecovery.SessionMutation.isStatusAndProgressOnly(): Boolean {
    val old = before.session
    val new = after.session
    val statusChanged = old.status != new.status
    val finishWrittenOrCleared = statusChanged &&
        (old.finishedAtEpochDay == null || new.finishedAtEpochDay == null)
    val startWritten = statusChanged && old.startedAtEpochDay == null
    return old.copy(
        status = new.status,
        progressCurrent = new.progressCurrent,
        updatedAtEpochMillis = new.updatedAtEpochMillis,
        finishedAtEpochDay = if (finishWrittenOrCleared) new.finishedAtEpochDay else old.finishedAtEpochDay,
        startedAtEpochDay = if (startWritten) new.startedAtEpochDay else old.startedAtEpochDay,
    ) == new
}
