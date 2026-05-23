package com.nilpo.contenttracker.ui.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.DetailHeaderActions
import com.nilpo.contenttracker.ui.common.MediaMetadataHero
import com.nilpo.contenttracker.ui.common.MediaMetadataHeroGenres
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatExternalRatingOnTen
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.LocalDate

@Composable
fun DetailScreen(
    trackedMedia: TrackedMedia,
    accent: Color,
    headerActions: DetailHeaderActions,
    onBack: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeletePastSession: (Long) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onAddExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onUpdateExternalRating: (Long, ExternalRatingSource, Double, Double, Int?, Boolean) -> Unit,
    onSetPrimaryExternalRating: (Long) -> Unit,
    onDeleteExternalRating: (Long) -> Unit,
    onAddExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTracking: (Long, ExternalTrackingSource, String?, String?) -> Unit,
    onUpdateExternalTrackingSynced: (Long, Boolean) -> Unit,
    onDeleteExternalTracking: (Long) -> Unit,
    onUpdateMediaItemDetails: (Long, String, Long?, String?, Double?, Int?, OwnershipType) -> Unit,
    onUpdateMediaItemMetadata: (Long, String, String?, Int?, Int?, List<String>, List<String>, String?, String?, String?) -> Unit,
    onRefreshMediaItemMetadata: (Long) -> Unit,
    onLinkMediaMetadata: () -> Unit,
    onDeleteMediaItem: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSession = trackedMedia.currentSession
    val pastSessions = trackedMedia.sessions
        .filter { session -> session.id != currentSession?.id }
        .sortedBy { it.sessionNumber }
    var showDeleteConfirmation by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var showExternalTrackingManager by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    var showExternalRatingsManager by rememberSaveable(trackedMedia.item.id) { mutableStateOf(false) }
    val isExternalTrackingUpdated = trackedMedia.externalTracking.isNotEmpty() &&
        trackedMedia.externalTracking.all { it.isSynced }
    val metadata = trackedMedia.item.toMediaMetadataUi(trackedMedia.credits).copy(
        collectionName = trackedMedia.collection?.name,
        collectionSortOrder = trackedMedia.item.collectionSortOrder,
        progressTotal = trackedMedia.item.effectiveProgressTotal(),
        isOwned = trackedMedia.item.ownership.isOwned,
        isExternalTrackingUpdated = isExternalTrackingUpdated,
        externalRatingSourceName = trackedMedia.primaryRatingSourceName(),
    )

    headerActions.onDeleteRequested = {
        showDeleteConfirmation = true
    }
    headerActions.onManageExternalTrackingRequested = {
        showExternalTrackingManager = true
    }
    headerActions.onManageExternalRatingsRequested = {
        showExternalRatingsManager = true
    }
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

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                MediaMetadataHero(
                    metadata = metadata,
                    onCollectionClick = trackedMedia.collection?.let { { onCollectionClick() } },
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
                    )
                }
            }

            item {
                DetailQuickActionsSection(
                    item = trackedMedia.item,
                    collection = trackedMedia.collection,
                    availableCollections = trackedMedia.availableCollections,
                    currentSession = currentSession,
                    externalTracking = trackedMedia.externalTracking,
                    accent = accent,
                    onSaveItemDetails = { title, collectionId, newCollectionName, collectionSortOrder, progressTotal, ownershipType ->
                        onUpdateMediaItemDetails(
                            trackedMedia.item.id,
                            title,
                            collectionId,
                            newCollectionName,
                            collectionSortOrder,
                            progressTotal,
                            ownershipType,
                        )
                    },
                    onStartNewSession = onStartNewSession,
                    onAddExternalTracking = { source, externalItemId, url ->
                        onAddExternalTracking(
                            trackedMedia.item.id,
                            source,
                            externalItemId,
                            url,
                        )
                    },
                    onUpdateExternalTrackingSynced = onUpdateExternalTrackingSynced,
                )
            }

            item {
                ItemDetailsSection(
                    item = trackedMedia.item,
                    credits = trackedMedia.credits,
                )
            }

            item {
                DetailSectionTitle(text = stringResource(R.string.detail_history))
            }

            if (pastSessions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                    )
                }
            }

            items(pastSessions) { session ->
                PastSessionSection(
                    session = session,
                    progressTotal = trackedMedia.item.effectiveProgressTotal(),
                    mediaType = trackedMedia.item.type,
                    accent = accent,
                    onUpdateSessionDetails = onUpdateSessionDetails,
                    onDeleteProgressUpdate = onDeleteProgressUpdate,
                    onDeleteSession = { onDeletePastSession(session.id) },
                )
            }

            if (trackedMedia.externalRatings.size > 1) {
                item {
                    DetailSectionTitle(text = stringResource(R.string.detail_external_scores))
                }
                item {
                    ExternalScoreTiles(
                        ratings = trackedMedia.externalRatings,
                        accent = accent,
                    )
                }
            }
        }
    }

    if (showExternalRatingsManager) {
        ExternalRatingsDialog(
            ratings = trackedMedia.externalRatings,
            primaryScore = trackedMedia.item.externalRatingScore,
            primaryMaxScore = trackedMedia.item.externalRatingMax,
            accent = accent,
            onDismiss = { showExternalRatingsManager = false },
            onAddExternalRating = { source, score, maxScore, voteCount, makePrimary ->
                onAddExternalRating(trackedMedia.item.id, source, score, maxScore, voteCount, makePrimary)
            },
            onUpdateExternalRating = onUpdateExternalRating,
            onSetPrimary = onSetPrimaryExternalRating,
            onDelete = onDeleteExternalRating,
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

    if (showExternalTrackingManager) {
        ExternalTrackingDialog(
            externalTracking = trackedMedia.externalTracking,
            accent = accent,
            onDismiss = { showExternalTrackingManager = false },
            onAddExternalTracking = { source, externalItemId, url ->
                onAddExternalTracking(trackedMedia.item.id, source, externalItemId, url)
                showExternalTrackingManager = false
            },
            onUpdateExternalTracking = onUpdateExternalTracking,
            onUpdateSynced = onUpdateExternalTrackingSynced,
            onDelete = onDeleteExternalTracking,
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
                onSaveMetadata = { title, originalTitle, releaseYear, progressTotal, genres, creators, coverUrl, synopsis, sourceUrl ->
                    onUpdateMediaItemMetadata(
                        trackedMedia.item.id,
                        title,
                        originalTitle,
                        releaseYear,
                        progressTotal,
                        genres,
                        creators,
                        coverUrl,
                        synopsis,
                        sourceUrl,
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
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = rating.source.displayName(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatExternalRatingOnTen(rating.score, rating.maxScore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
            }
            rating.voteCount?.let { voteCount ->
                Text(
                    text = stringResource(R.string.metadata_users) + " " + formatCompactCount(voteCount.toDouble()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogColors.AppMuted,
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

private fun TrackedMedia.primaryRatingSourceName(): String? {
    val score = item.externalRatingScore ?: return null
    val maxScore = item.externalRatingMax ?: return null
    return externalRatings.firstOrNull { rating ->
        rating.score.closeTo(score) && rating.maxScore.closeTo(maxScore)
    }?.source?.displayName()
}

private fun MediaItem.effectiveProgressTotal(): Int? {
    return progressTotal.takeUnless { type == MediaType.Game }
}

private fun Double.closeTo(other: Double): Boolean = kotlin.math.abs(this - other) < 0.001

private fun formatCompactCount(value: Double): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1_000_000_000 -> "${formatScore(value / 1_000_000_000)}B"
        absValue >= 1_000_000 -> "${formatScore(value / 1_000_000)}M"
        absValue >= 1_000 -> "${formatScore(value / 1_000)}k"
        else -> formatScore(value)
    }
}
