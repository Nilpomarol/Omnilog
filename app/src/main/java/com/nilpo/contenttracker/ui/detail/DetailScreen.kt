package com.nilpo.contenttracker.ui.detail

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.DetailHeaderActions
import com.nilpo.contenttracker.ui.common.MediaMetadataHero
import com.nilpo.contenttracker.ui.common.MediaMetadataHeroGenres
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.localizedSteamScoreDescriptor
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate

@Composable
fun DetailScreen(
    trackedMedia: TrackedMedia,
    allTrackedMedia: List<TrackedMedia>,
    accent: Color,
    headerActions: DetailHeaderActions,
    onBack: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeletePastSession: (Long) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
    onAddExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimaryExternalRating: (Long) -> Unit,
    onDeleteExternalRating: (Long) -> Unit,
    onUpdateMediaItemDetails: (Long, String, Long?, String?, Double?, Int?, Boolean) -> Unit,
    onUpdateMediaItemMetadata: (Long, String, String?, Int?, String?, Int?, List<String>, List<String>, String?, String?, String?, String?) -> Unit,
    onRefreshMediaItemMetadata: (Long) -> Unit,
    onLinkMediaMetadata: () -> Unit,
    onDeleteMediaItem: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onRelatedMediaClick: (TrackedMedia) -> Unit,
    externalRecommendations: List<ExternalRecommendation> = emptyList(),
    isExternalRecommendationsLoading: Boolean = false,
    hasExternalRecommendationsError: Boolean = false,
    onRefreshExternalRecommendations: () -> Unit = {},
    onExternalRecommendationClick: (MetadataSuggestion) -> Unit = {},
    askForGoodreadsRating: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val currentSession = trackedMedia.currentSession
    val pastSessions = trackedMedia.sessions
        .filter { session -> session.id != currentSession?.id }
        .sortedBy { it.sessionNumber }
    var showDeleteConfirmation by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var showExternalRatingsManager by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
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
            modifier = modifier,
        )
        return
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
            state = detailListState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                MediaMetadataHero(
                    metadata = metadata,
                    onCollectionClick = trackedMedia.collection?.let { { onCollectionClick() } },
                    onCreatorClick = onAuthorClick,
                )
            }

            if (metadata.genres.isNotEmpty()) {
                item {
                    MediaMetadataHeroGenres(metadata = metadata)
                }
            }

            if (currentSession != null) {
                item {
                    CurrentSessionSection(
                        session = currentSession,
                        progressTotal = trackedMedia.item.effectiveProgressTotal(),
                        mediaType = trackedMedia.item.type,
                        accent = accent,
                        onUpdateSessionDetails = onUpdateSessionDetails,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                    )
                }
            }

            item {
                DetailQuickActionsSection(
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

            item {
                ItemDetailsSection(
                    item = trackedMedia.item,
                    credits = trackedMedia.credits,
                )
            }

            if (pastSessions.isNotEmpty()) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_history))
                }

                items(pastSessions) { session ->
                    PastSessionSection(
                        session = session,
                        visitNumber = trackedMedia.visitNumber(session),
                        progressTotal = trackedMedia.item.effectiveProgressTotal(),
                        mediaType = trackedMedia.item.type,
                        accent = accent,
                        onUpdateSessionDetails = onUpdateSessionDetails,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                        onDeleteSession = { onDeletePastSession(session.id) },
                    )
                }
            }

            if (trackedMedia.externalRatings.size > 1) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_external_scores))
                }
                item {
                    ExternalScoreTiles(
                        ratings = trackedMedia.externalRatings,
                        mediaType = trackedMedia.item.type,
                        accent = accent,
                    )
                }
            }

            if (relatedMedia.collection.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = collectionSectionTitle,
                        relatedMedia = relatedMedia.collection,
                        accent = accent,
                        onMediaClick = onRelatedMediaClick,
                    )
                }
            }

            if (relatedMedia.generic.isNotEmpty()) {
                item {
                    RelatedMediaSection(
                        title = stringResource(R.string.detail_related_title),
                        relatedMedia = relatedMedia.generic,
                        accent = accent,
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
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ItemDetailsEditor(
                item = trackedMedia.item,
                accent = accent,
                onDismiss = { headerActions.isEditingItemDetails = false },
                onSaveMetadata = { title, originalTitle, releaseYear, language, progressTotal, genres, creators, coverUrl, synopsis, sourceUrl, steamAppId ->
                    onUpdateMediaItemMetadata(
                        trackedMedia.item.id,
                        title,
                        originalTitle,
                        releaseYear,
                        language,
                        progressTotal,
                        genres,
                        creators,
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

@Composable
private fun ExternalScoreTiles(
    ratings: List<ExternalRating>,
    mediaType: MediaType,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ratings.chunked(3).forEach { rowRatings ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowRatings.forEach { rating ->
                    ExternalScoreTile(
                        rating = rating,
                        mediaType = mediaType,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowRatings.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun ExternalScoreTile(
    rating: ExternalRating,
    mediaType: MediaType,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = rating.source.displayName(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = formatExternalRating(
                        score = rating.score,
                        maxScore = rating.maxScore,
                        mediaType = mediaType,
                        source = rating.source,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                localizedSteamScoreDescriptor(
                    mediaType = mediaType,
                    source = rating.source,
                    descriptor = rating.scoreDescriptor,
                )?.let { descriptor ->
                    Text(
                        text = descriptor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            rating.voteCount?.let { voteCount ->
                Text(
                    text = stringResource(R.string.metadata_users) + " " + formatCompactCount(
                        voteCount.toDouble()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatScore(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}

private fun MediaItem.effectiveProgressTotal(): Int? {
    return progressTotal.takeUnless { type == MediaType.Game }
}

private fun formatCompactCount(value: Double): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1_000_000_000 -> "${formatScore(value / 1_000_000_000)}B"
        absValue >= 1_000_000 -> "${formatScore(value / 1_000_000)}M"
        absValue >= 1_000 -> "${formatScore(value / 1_000)}k"
        else -> formatScore(value)
    }
}
