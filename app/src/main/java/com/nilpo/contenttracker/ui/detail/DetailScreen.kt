package com.nilpo.contenttracker.ui.detail

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.ContributorDirectory
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.DetailHeaderActions
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.localizedSteamScoreDescriptor
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

/** How far the session card rides up over the tail of the backdrop's fade. */
private val SessionOverlap = 34.dp

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
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
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
    val currentSession = trackedMedia.currentSession
    val pastSessions = trackedMedia.sessions
        .filter { session -> session.id != currentSession?.id }
        .sortedBy { it.sessionNumber }
    // The ratings card shows the user's own verdict beside the providers'. Ratings belong to
    // sessions, and a re-read can be scored differently from the first read, so the latest one
    // that carries a score is the one that stands as "la teva nota".
    val userRating = trackedMedia.orderedSessions
        .lastOrNull { it.ratingHalfPoints != null }
        ?.ratingHalfPoints
    var showDeleteConfirmation by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var showExternalRatingsManager by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var showQuickProgress by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var dismissGoodreadsPrompt by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val skippedGoodreadsPromptIds = remember(context) {
        context.getSharedPreferences("omnilog_preferences", Context.MODE_PRIVATE)
    }.getStringSet("skipped_goodreads_rating_prompt_ids", emptySet()).orEmpty()
    val showGoodreadsPrompt = askForGoodreadsRating &&
            !dismissGoodreadsPrompt &&
            trackedMedia.item.type == MediaType.Book &&
            trackedMedia.externalRatings.none { it.source == ExternalRatingSource.Goodreads } &&
            trackedMedia.item.id.toString() !in skippedGoodreadsPromptIds
    val primaryExternalRating = trackedMedia.primaryExternalRating
    val metadata = trackedMedia.item.toMediaMetadataUi(trackedMedia.credits).copy(
        creators = trackedMedia.creatorNames(),
        collectionName = trackedMedia.collection?.name,
        collectionSortOrder = trackedMedia.item.collectionSortOrder,
        progressTotal = trackedMedia.item.effectiveProgressTotal(),
        isOwned = trackedMedia.item.isOwned,
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

    headerActions.onDeleteRequested = {
        showDeleteConfirmation = true
    }
    headerActions.onManageExternalRatingsRequested = {
        showExternalRatingsManager = true
    }
    headerActions.isManagingExternalRatings = showExternalRatingsManager
    headerActions.onCloseExternalRatings = { showExternalRatingsManager = false }
    headerActions.onRefreshMetadataRequested = {
        onRefreshMediaItemMetadata(trackedMedia.item.id)
    }
    // The top bar is transparent over the backdrop, so it has to earn its surface back as the page
    // scrolls out from under it — otherwise the sections below run into the bar and the status bar.
    // Once the header has left the viewport entirely the bar is simply solid.
    val barFadeDistancePx = with(LocalDensity.current) { BarFadeDistance.toPx() }
    headerActions.barOpacity = if (detailListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (detailListState.firstVisibleItemScrollOffset / barFadeDistancePx).coerceIn(0f, 1f)
    }

    // The header puts a dark scrim behind the status bar in both themes, so on light the app-wide
    // rule — dark icons on a pale background — points the wrong way and the clock disappears into
    // the artwork. While the backdrop is up there the icons are forced pale; once the bar has taken
    // its own surface back, or when the page is left, they go back to whatever the theme wants.
    val view = LocalView.current
    val statusBarOverArtwork = metadata.coverUrl.isNullOrBlank().not() &&
            headerActions.barOpacity < 0.5f
    val themeWantsLightIcons = OmnilogTheme.colors.appBackground.luminance() < 0.5f
    DisposableEffect(view, statusBarOverArtwork, themeWantsLightIcons) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !(statusBarOverArtwork || themeWantsLightIcons)
        onDispose {
            controller?.isAppearanceLightStatusBars = !themeWantsLightIcons
        }
    }

    headerActions.onLinkMetadataRequested = onLinkMediaMetadata
    headerActions.showLinkMetadata = trackedMedia.item.type in setOf(
        MediaType.Anime,
        MediaType.Book,
        MediaType.Movie,
        MediaType.TvShow,
    )
    headerActions.linkMetadataLabelResId = when (trackedMedia.item.type) {
        MediaType.Anime -> R.string.link_metadata_anime
        MediaType.Book -> R.string.link_metadata_book
        else -> R.string.link_metadata_movie
    }

    if (showExternalRatingsManager) {
        ExternalRatingsPage(
            title = displayMediaTitle(trackedMedia.item.title),
            mediaType = trackedMedia.item.type,
            ratings = trackedMedia.externalRatings,
            primaryRatingId = trackedMedia.item.primaryExternalRatingId,
            accent = accent,
            onAddExternalRating = { source, score, maxScore, voteCount, makePrimary ->
                onAddExternalRating(
                    trackedMedia.item.id,
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
            // This page has no backdrop, so it takes the app bar's inset back as ordinary padding.
            modifier = modifier.padding(contentPadding),
        )
        return
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        // The 24dp gutter used to live on the LazyColumn, which meant nothing on this page could
        // reach the screen edge. It now belongs to each item, so the backdrop can bleed.
        val gutter = Modifier.padding(horizontal = 24.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = detailListState,
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                // The header and the live card are one composition, not two rows: the negative
                // spacing is what lets the card sit over the tail of the artwork instead of below
                // a hard edge. The header reserves the same distance under its title block, so the
                // card rides up into empty artwork rather than onto the title. Only the live card
                // overlaps — a title with nothing but history leaves the header standing alone.
                val overlap = if (currentSession != null) SessionOverlap else 0.dp

                Column(verticalArrangement = Arrangement.spacedBy(-overlap)) {
                    DetailBackdropHeader(
                        metadata = metadata,
                        topInset = contentPadding.calculateTopPadding(),
                        overlap = overlap,
                        onCollectionClick = trackedMedia.collection?.let { { onCollectionClick() } },
                        onCreatorClick = { creator ->
                            onAuthorClick(creator, trackedMedia.item.type.primaryContributorRole())
                        },
                    )

                    // The live session, at the page's full width. It used to hang off a rail that it
                    // shared with the history, which cost it 22dp for a line it did not need; the
                    // re-reads now live in their own collapsible `Historial` section below.
                    currentSession?.let { session ->
                        CurrentSessionSection(
                            modifier = gutter,
                            session = session,
                            progressTotal = trackedMedia.item.effectiveProgressTotal(),
                            mediaType = trackedMedia.item.type,
                            accent = accent,
                            // Completed and Dropped sessions have nothing left to log, and the
                            // sheet behind this button refuses them anyway.
                            onLogProgress = if (session.status.endsSession) {
                                null
                            } else {
                                { showQuickProgress = true }
                            },
                            onUpdateSessionDetails = onUpdateSessionDetails,
                            // Only offered when a previous session survives to become live again;
                            // deleting the sole session is untracking, which this is not.
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
                }
            }

            item {
                DetailQuickActionsSection(
                    // These controls belong to the live session, so they sit closer to its card than
                    // the page-wide section rhythm. The offset only tightens that single hand-off;
                    // later sections retain the standard 20dp separation.
                    modifier = gutter.offset(y = (-8).dp),
                    item = trackedMedia.item,
                    collection = trackedMedia.collection,
                    library = allTrackedMedia,
                    currentSession = currentSession,
                    accent = accent,
                    onSaveItemDetails = { title, collectionId, newCollectionName, collectionSortOrder, progressTotal, isOwned ->
                        onUpdateMediaItemDetails(
                            trackedMedia.item.id,
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

            if (pastSessions.isNotEmpty()) {
                item {
                    SessionHistorySection(
                        modifier = gutter,
                        sessions = pastSessions.reversed().map { session ->
                            {
                                PastSessionSection(
                                    session = session,
                                    visitNumber = trackedMedia.visitNumber(session),
                                    progressTotal = trackedMedia.item.effectiveProgressTotal(),
                                    mediaType = trackedMedia.item.type,
                                    accent = accent,
                                    onUpdateSessionDetails = onUpdateSessionDetails,
                                    onDeleteProgressUpdate = onDeleteProgressUpdate,
                                    onDeleteStatusEvent = onDeleteStatusEvent,
                                    onUpdateStatusEventDate = onUpdateStatusEventDate,
                                    onUpdateProgressUpdate = onUpdateProgressUpdate,
                                    onDeleteSession = { onDeletePastSession(session.id) },
                                )
                            }
                        },
                    )
                }
            }

            item {
                ItemDetailsSection(
                    item = trackedMedia.item,
                    credits = trackedMedia.credits,
                    contributors = contributors,
                    accent = accent,
                    onAuthorClick = onAuthorClick,
                    modifier = gutter,
                )
            }

            if (trackedMedia.externalRatings.isNotEmpty() || userRating != null) {
                item {
                    RatingsSection(
                        ratings = trackedMedia.externalRatings,
                        userRating = userRating,
                        mediaType = trackedMedia.item.type,
                        primaryRatingId = trackedMedia.item.primaryExternalRatingId,
                        rankingPosition = trackedMedia.item.rankingPosition,
                        rankingLabel = trackedMedia.item.rankingLabel,
                        accent = accent,
                        modifier = gutter,
                    )
                }
            }

            if (relatedMedia.collection.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = collectionSectionTitle,
                        relatedMedia = relatedMedia.collection,
                        onMediaClick = onRelatedMediaClick,
                    )
                }
            }

            if (relatedMedia.generic.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = stringResource(R.string.detail_related_title),
                        relatedMedia = relatedMedia.generic,
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
                    trackedMedia.item.id,
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
                        skippedGoodreadsPromptIds + trackedMedia.item.id.toString(),
                    )
                    .apply()
                dismissGoodreadsPrompt = true
            },
        )
    }

    // The same sheet the Home tiles open, reached from the card's own button. Logging progress was
    // previously impossible from this page without going through the full session editor, which is
    // what made the card feel like a read-out rather than somewhere to do anything.
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
                        displayMediaTitle(trackedMedia.item.title),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMediaItem(trackedMedia.item.id)
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
                item = trackedMedia.item,
                credits = trackedMedia.credits,
                accent = accent,
                onDismiss = { headerActions.isEditingItemDetails = false },
                onSaveMetadata = { title, originalTitle, releaseYear, language, progressTotal, genres, creators, credits, coverUrl, synopsis, sourceUrl, steamAppId ->
                    onUpdateMediaItemMetadata(
                        trackedMedia.item.id,
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
