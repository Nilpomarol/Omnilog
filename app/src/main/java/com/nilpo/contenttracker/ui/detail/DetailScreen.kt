package com.nilpo.contenttracker.ui.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ContributorDirectory
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.DetailHeaderActions
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

/** How far the page scrolls before the top bar has fully taken its own surface back. */
private val BarFadeDistance = 150.dp

@Composable
fun DetailScreen(
    trackedMedia: TrackedMedia,
    allTrackedMedia: List<TrackedMedia>,
    contributors: ContributorDirectory,
    accent: Color,
    headerActions: DetailHeaderActions,
    onBack: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onQuickCommitProgress: (Int) -> Unit,
    onQuickComplete: (QuickCompletion) -> Unit,
    onDeletePastSession: (Long) -> Unit,
    onDeleteCurrentSession: (Long) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate?) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onAddExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimaryExternalRating: (Long) -> Unit,
    onDeleteExternalRating: (Long) -> Unit,
    onUpdateMediaItemDetails: (Long, String, Long?, String?, Double?, Int?, Boolean) -> Unit,
    onUpdateMediaItemMetadata: (Long, String, String?, Int?, String?, Int?, List<String>, List<String>, List<MediaCredit>, String?, String?, String?, String?) -> Unit,
    onRefreshMediaItemMetadata: (Long) -> Unit,
    onLinkMediaMetadata: () -> Unit,
    onDeleteMediaItem: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    onAuthorClick: (String, MediaCreditRole) -> Unit,
    onRelatedMediaClick: (TrackedMedia) -> Unit,
    externalRecommendations: List<ExternalRecommendation> = emptyList(),
    isExternalRecommendationsLoading: Boolean = false,
    hasExternalRecommendationsError: Boolean = false,
    onRefreshExternalRecommendations: () -> Unit = {},
    onExternalRecommendationClick: (MetadataSuggestion) -> Unit = {},
    askForGoodreadsRating: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val media = trackedMedia.item
    val currentSession = trackedMedia.currentSession
    val pastSessions = trackedMedia.sessions
        .filter { session -> session.id != currentSession?.id }
        .sortedBy { it.sessionNumber }
    // The ratings row shows the user's own verdict beside the providers'. Ratings belong to
    // sessions, and a re-read can be scored differently from the first read, so the latest one
    // that carries a score is the one that stands as "la teva nota".
    val userRating = trackedMedia.orderedSessions
        .lastOrNull { it.ratingHalfPoints != null }
        ?.ratingHalfPoints
    var showDeleteConfirmation by rememberSaveable(media.id) { mutableStateOf(false) }
    var showExternalRatingsManager by rememberSaveable(media.id) { mutableStateOf(false) }
    var showQuickProgress by rememberSaveable(media.id) { mutableStateOf(false) }
    var dismissGoodreadsPrompt by rememberSaveable(media.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val skippedGoodreadsPromptIds = remember(context) {
        context.getSharedPreferences("omnilog_preferences", Context.MODE_PRIVATE)
    }.getStringSet("skipped_goodreads_rating_prompt_ids", emptySet()).orEmpty()
    val showGoodreadsPrompt = askForGoodreadsRating &&
            !dismissGoodreadsPrompt &&
            media.type == MediaType.Book &&
            trackedMedia.externalRatings.none { it.source == ExternalRatingSource.Goodreads } &&
            media.id.toString() !in skippedGoodreadsPromptIds
    val primaryExternalRating = trackedMedia.primaryExternalRating
    val metadata = media.toMediaMetadataUi(trackedMedia.credits).copy(
        creators = trackedMedia.creatorNames(),
        collectionName = trackedMedia.collection?.name,
        collectionSortOrder = media.collectionSortOrder,
        progressTotal = media.effectiveProgressTotal(),
        isOwned = media.isOwned,
        externalRatingSourceName = primaryExternalRating?.source?.displayName(),
        externalRatingSource = primaryExternalRating?.source,
        externalRatingScoreDescriptor = primaryExternalRating?.scoreDescriptor,
    )
    val relatedMedia = remember(trackedMedia, allTrackedMedia) {
        findRelatedMedia(
            current = trackedMedia,
            library = allTrackedMedia,
        )
    }
    val detailListState = rememberLazyListState()
    val collectionSectionTitle = trackedMedia.collection?.name?.let { collectionName ->
        stringResource(R.string.detail_related_collection_title, collectionName)
    } ?: stringResource(R.string.detail_related_collection_fallback)

    // What each folded row says about what is behind it, so most visits never need to open one.
    val factsSummary = listOfNotNull(
        media.language
            ?.takeUnless { media.type == MediaType.Game }
            ?.let { languageLabel(it) }
            ?.takeIf { it.isNotBlank() },
        media.releaseYear?.toString(),
    ).joinToString(" · ")
    val hasFacts = factsSummary.isNotEmpty() ||
            media.effectiveProgressTotal() != null ||
            media.tags.isNotEmpty()
    val hasCredits = trackedMedia.credits.any { it.personName.isNotBlank() }

    headerActions.onDeleteRequested = {
        showDeleteConfirmation = true
    }
    headerActions.onManageExternalRatingsRequested = {
        showExternalRatingsManager = true
    }
    headerActions.isManagingExternalRatings = showExternalRatingsManager
    headerActions.onCloseExternalRatings = { showExternalRatingsManager = false }
    headerActions.onRefreshMetadataRequested = {
        onRefreshMediaItemMetadata(media.id)
    }
    // The top bar is transparent over the header, so it has to earn its surface back as the page
    // scrolls out from under it — otherwise the sections below run into the bar and the status bar.
    // Once the header has left the viewport entirely the bar is simply solid.
    val barFadeDistancePx = with(LocalDensity.current) { BarFadeDistance.toPx() }
    headerActions.barOpacity = if (detailListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (detailListState.firstVisibleItemScrollOffset / barFadeDistancePx).coerceIn(0f, 1f)
    }

    headerActions.onLinkMetadataRequested = onLinkMediaMetadata
    headerActions.showLinkMetadata = media.type in setOf(
        MediaType.Anime,
        MediaType.Book,
        MediaType.Movie,
        MediaType.TvShow,
    )
    headerActions.linkMetadataLabelResId = when (media.type) {
        MediaType.Anime -> R.string.link_metadata_anime
        MediaType.Book -> R.string.link_metadata_book
        else -> R.string.link_metadata_movie
    }

    if (showExternalRatingsManager) {
        ExternalRatingsPage(
            title = displayMediaTitle(media.title),
            mediaType = media.type,
            ratings = trackedMedia.externalRatings,
            primaryRatingId = media.primaryExternalRatingId,
            accent = accent,
            onAddExternalRating = { source, score, maxScore, voteCount, makePrimary ->
                onAddExternalRating(
                    media.id,
                    source,
                    score,
                    maxScore,
                    voteCount,
                    makePrimary
                )
            },
            onUpdateExternalRating = onUpdateExternalRating,
            onSetPrimary = onSetPrimaryExternalRating,
            onDelete = onDeleteExternalRating,
            // This page has no header, so it takes the app bar's inset back as ordinary padding.
            modifier = modifier.padding(contentPadding),
        )
        return
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        // The gutter belongs to each item rather than to the LazyColumn, so hairlines' neighbours and
        // the cover carousels can reach the screen edge.
        val gutter = Modifier.padding(horizontal = DetailGutter)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = detailListState,
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                DetailHeader(
                    metadata = metadata,
                    topInset = contentPadding.calculateTopPadding(),
                    onCollectionClick = trackedMedia.collection?.let { { onCollectionClick() } },
                    onCreatorClick = { creator ->
                        onAuthorClick(creator, media.type.primaryContributorRole())
                    },
                )
            }

            item {
                // The session and the actions that act on it sit closer to each other than the
                // page's section rhythm.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentSession?.let { session ->
                            CurrentSessionSection(
                                // The section keeps a few dp of its own inside the gutter, so its
                                // press ripple does not end flush against the text.
                                modifier = Modifier.padding(horizontal = DetailGutter - 8.dp),
                                trackedMedia = trackedMedia,
                                session = session,
                                progressTotal = media.effectiveProgressTotal(),
                                mediaType = media.type,
                                accent = accent,
                                onUpdateSessionDetails = onUpdateSessionDetails,
                                onQuickComplete = onQuickComplete,
                                // Only offered when a previous session survives to become live
                                // again; deleting the sole session is untracking, which this is not.
                                onDeleteSession = if (pastSessions.isNotEmpty()) {
                                    { onDeleteCurrentSession(session.id) }
                                } else {
                                    null
                                },
                                onDeleteProgressUpdate = onDeleteProgressUpdate,
                                onDeleteStatusEvent = onDeleteStatusEvent,
                                onUpdateStatusEventDate = onUpdateStatusEventDate,
                                onUpdateProgressUpdate = onUpdateProgressUpdate,
                            )
                        }

                        DetailQuickActionsSection(
                            modifier = gutter,
                            item = media,
                            collection = trackedMedia.collection,
                            library = allTrackedMedia,
                            currentSession = currentSession,
                            accent = accent,
                            // Completed and Dropped sessions have nothing left to log, and the sheet
                            // behind this button refuses them anyway.
                            onLogProgress = currentSession
                                ?.takeUnless { it.status.endsSession }
                                ?.let { { showQuickProgress = true } },
                            onSaveItemDetails = { title, collectionId, newCollectionName, collectionSortOrder, progressTotal, isOwned ->
                                onUpdateMediaItemDetails(
                                    media.id,
                                    title,
                                    collectionId,
                                    newCollectionName,
                                    collectionSortOrder,
                                    progressTotal,
                                    isOwned,
                                )
                            },
                            onStartNewSession = onStartNewSession,
                        )
                }
            }

            media.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                item { DetailHairline() }
                item { DetailSynopsis(body = synopsis, modifier = gutter) }
            }

            if (trackedMedia.externalRatings.isNotEmpty() || userRating != null) {
                item { DetailHairline() }
                item {
                    RatingsSection(
                        ratings = trackedMedia.externalRatings,
                        userRating = userRating,
                        mediaType = media.type,
                        primaryRatingId = media.primaryExternalRatingId,
                        rankingPosition = media.rankingPosition,
                        accent = accent,
                        onManageExternalRatings = { showExternalRatingsManager = true },
                        modifier = gutter,
                    )
                }
            }

            if (hasFacts || hasCredits || pastSessions.isNotEmpty()) {
                item {
                    // The lower-frequency record, folded. Each row closes with its own hairline so
                    // the group reads as one ruled list.
                    Column {
                        DetailHairline()
                        if (hasFacts) {
                            DetailDisclosureRow(
                                icon = rememberVectorPainter(Icons.Outlined.Info),
                                title = stringResource(R.string.detail_item_details),
                                summary = factsSummary,
                            ) {
                                DetailFactsBlock(item = media, modifier = gutter)
                            }
                            DetailHairline()
                        }
                        if (hasCredits) {
                            DetailDisclosureRow(
                                icon = rememberVectorPainter(Icons.Outlined.Person),
                                title = stringResource(R.string.detail_credits),
                                summary = trackedMedia.creatorNames().firstOrNull(),
                            ) {
                                Box(modifier = gutter) {
                                    CreditGroups(
                                        credits = trackedMedia.credits,
                                        contributors = contributors,
                                        mediaType = media.type,
                                        accent = accent,
                                        onAuthorClick = onAuthorClick,
                                    )
                                }
                            }
                            DetailHairline()
                        }
                        if (pastSessions.isNotEmpty()) {
                            DetailDisclosureRow(
                                icon = painterResource(R.drawable.ic_history),
                                title = stringResource(R.string.detail_history),
                                summary = pluralStringResource(
                                    R.plurals.detail_history_sessions,
                                    pastSessions.size,
                                    pastSessions.size,
                                ),
                            ) {
                                // Most recent first: a re-read is usually the thing you came to check.
                                Column(modifier = gutter) {
                                    pastSessions.reversed().forEachIndexed { index, session ->
                                        if (index > 0) {
                                            HorizontalDivider(color = OmnilogTheme.colors.appLine)
                                        }
                                        PastSessionSection(
                                            session = session,
                                            visitNumber = trackedMedia.visitNumber(session),
                                            progressTotal = media.effectiveProgressTotal(),
                                            mediaType = media.type,
                                            accent = accent,
                                            onUpdateSessionDetails = onUpdateSessionDetails,
                                            onDeleteProgressUpdate = onDeleteProgressUpdate,
                                            onDeleteStatusEvent = onDeleteStatusEvent,
                                            onUpdateStatusEventDate = onUpdateStatusEventDate,
                                            onUpdateProgressUpdate = onUpdateProgressUpdate,
                                            onDeleteSession = { onDeletePastSession(session.id) },
                                        )
                                    }
                                }
                            }
                            DetailHairline()
                        }
                    }
                }
            }

            if (relatedMedia.collection.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = collectionSectionTitle,
                        relatedMedia = relatedMedia.collection.map { it.trackedMedia },
                        onMediaClick = onRelatedMediaClick,
                    )
                }
            }

            if (relatedMedia.generic.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = stringResource(R.string.detail_related_title),
                        relatedMedia = relatedMedia.generic.map { it.trackedMedia },
                        onMediaClick = onRelatedMediaClick,
                    )
                }
            }
            if (
                externalRecommendations.isNotEmpty() ||
                isExternalRecommendationsLoading ||
                hasExternalRecommendationsError
            ) {
                item {
                    ExternalRecommendationsSection(
                        recommendations = externalRecommendations,
                        accent = accent,
                        isLoading = isExternalRecommendationsLoading,
                        hasError = hasExternalRecommendationsError,
                        onRefresh = onRefreshExternalRecommendations,
                        onRecommendationClick = onExternalRecommendationClick,
                    )
                }
            }
        }
    }

    if (showGoodreadsPrompt) {
        GoodreadsRatingPrompt(
            accent = accent,
            onSave = { score, voteCount ->
                onAddExternalRating(
                    media.id,
                    ExternalRatingSource.Goodreads,
                    score,
                    5.0,
                    voteCount,
                    true,
                )
                dismissGoodreadsPrompt = true
            },
            onDismiss = { dismissGoodreadsPrompt = true },
            onSkipBook = {
                val preferences =
                    context.getSharedPreferences("omnilog_preferences", Context.MODE_PRIVATE)
                preferences.edit()
                    .putStringSet(
                        "skipped_goodreads_rating_prompt_ids",
                        skippedGoodreadsPromptIds + media.id.toString(),
                    )
                    .apply()
                dismissGoodreadsPrompt = true
            },
        )
    }

    // The same sheet the Home tiles open, reached from the action row's log button.
    if (showQuickProgress && currentSession != null) {
        QuickProgressSheet(
            trackedMedia = trackedMedia,
            accent = sessionStateVisual(currentSession.status).color,
            onCommit = {
                onQuickCommitProgress(it)
                showQuickProgress = false
            },
            onComplete = {
                onQuickComplete(it)
                showQuickProgress = false
            },
            onDismiss = { showQuickProgress = false },
        )
    }

    if (showDeleteConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = stringResource(R.string.delete_media_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_media_message,
                        displayMediaTitle(media.title),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMediaItem(media.id)
                        showDeleteConfirmation = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (headerActions.isEditingItemDetails) {
        Dialog(
            onDismissRequest = { headerActions.isEditingItemDetails = false },
            // The editor owns the discard confirmation, so back has to reach its own handler rather
            // than closing the window out from under an unsaved edit.
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            ItemDetailsEditor(
                item = media,
                credits = trackedMedia.credits,
                accent = accent,
                onDismiss = { headerActions.isEditingItemDetails = false },
                onSaveMetadata = { title, originalTitle, releaseYear, language, progressTotal, genres, creators, credits, coverUrl, synopsis, sourceUrl, steamAppId ->
                    onUpdateMediaItemMetadata(
                        media.id,
                        title,
                        originalTitle,
                        releaseYear,
                        language,
                        progressTotal,
                        genres,
                        creators,
                        credits,
                        coverUrl,
                        synopsis,
                        sourceUrl,
                        steamAppId,
                    )
                    headerActions.isEditingItemDetails = false
                },
            )
        }
    }
}

/** The page's section rule: a hairline inside the gutter, so the page reads as one ruled sheet. */
@Composable
private fun DetailHairline() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DetailGutter),
        color = OmnilogTheme.colors.appLine,
    )
}

private fun MediaItem.effectiveProgressTotal(): Int? {
    return progressTotal.takeUnless { type == MediaType.Game }
}

private fun MediaType.primaryContributorRole(): MediaCreditRole = when (this) {
    MediaType.Anime -> MediaCreditRole.Studio
    MediaType.Book -> MediaCreditRole.Author
    MediaType.Movie -> MediaCreditRole.Director
    MediaType.TvShow -> MediaCreditRole.Creator
    MediaType.Game -> MediaCreditRole.Developer
}
