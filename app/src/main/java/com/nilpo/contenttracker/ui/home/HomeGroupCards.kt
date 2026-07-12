package com.nilpo.contenttracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeGroupHeader(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    when {
        group.type == HomeGroupType.Collection && group.collection != null -> CollectionGroupCard(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
            onCollectionClick = onCollectionClick,
        )
        group.type == HomeGroupType.Author -> AuthorGroupCard(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
            onAuthorClick = onAuthorClick,
        )
        else -> SimpleGroupHeader(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
        )
    }
}

@Composable
internal fun SimpleGroupHeader(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (group.status != null) {
                Icon(
                    painter = painterResource(group.status.iconResId),
                    contentDescription = group.status.label(),
                    modifier = Modifier.size(18.dp),
                    tint = group.status.stateColor,
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.resolvedTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.collection_item_count, group.items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
        }
    }
}

@Composable
private fun AuthorGroupCard(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    val summary = group.items.collectionProgressSummary()
    val averageRating = group.items.collectionAverageRating()
    val topItem = group.items.authorTopRatedItem()
    val lastUpdatedMillis = group.items.collectionLastUpdatedMillis()
    val mediaType = group.items.firstOrNull()?.item?.type
    val unitLabel = mediaType.collectionItemUnitLabel()
    val metaLine = listOfNotNull(
        "${group.items.size} $unitLabel",
        stringResource(R.string.group_completed_count, summary.completedCount),
        stringResource(R.string.group_in_progress_count, summary.inProgressCount),
    ).joinToString(" · ")
    val transition = updateTransition(
        targetState = isCollapsed,
        label = "author_card_transition",
    )
    val coverWidth = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "author_cover_width",
    ) { collapsed -> if (collapsed) 72.dp else 0.dp }
    val coverHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "author_cover_height",
    ) { collapsed -> if (collapsed) 108.dp else 0.dp }
    val coverGap = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "author_cover_gap",
    ) { collapsed -> if (collapsed) 8.dp else 0.dp }
    val contentHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "author_content_height",
    ) { collapsed -> if (collapsed) 108.dp else 28.dp }
    val coverAlpha = transition.animateFloat(
        transitionSpec = { tween(140) },
        label = "author_cover_alpha",
    ) { collapsed -> if (collapsed) 1f else 0f }
    val arrowRotation = transition.animateFloat(
        transitionSpec = { tween(180) },
        label = "author_arrow_rotation",
    ) { collapsed -> if (collapsed) 0f else 180f }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (group.title.isBlank()) onClick() else onAuthorClick(group.title)
        },
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(coverWidth.value)
                    .height(coverHeight.value)
                    .alpha(coverAlpha.value)
                    .clipToBounds(),
            ) {
                MetadataCoverImage(
                    coverUrl = topItem?.item?.coverUrl ?: group.items.collectionCoverUrl(),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp)),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            Spacer(modifier = Modifier.width(coverGap.value))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(contentHeight.value)
                    .clipToBounds(),
                verticalArrangement = if (isCollapsed) Arrangement.SpaceBetween else Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = if (isCollapsed) Alignment.Top else Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = group.resolvedTitle(),
                            style = if (isCollapsed) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isCollapsed) OmnilogColors.AppInk else accent,
                            maxLines = if (isCollapsed) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(
                            visible = isCollapsed,
                            enter = fadeIn(tween(150, delayMillis = 50)),
                            exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
                        ) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !isCollapsed,
                        enter = fadeIn(tween(120, delayMillis = 60)),
                        exit = fadeOut(tween(60)),
                    ) {
                        Text(
                            text = stringResource(R.string.collection_item_count, group.items.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(
                                if (isCollapsed) R.string.author_expand else R.string.author_collapse,
                            ),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = arrowRotation.value },
                            tint = accent,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(tween(150, delayMillis = 60)),
                    exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.group_progress_prefix, summary.label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OmnilogColors.AppMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                topItem?.item?.title?.let { title ->
                                    Text(
                                        text = stringResource(R.string.group_top_rated, title),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OmnilogColors.AppMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            averageRating?.let { rating ->
                                CollectionAverageRatingSlot(
                                    rating = rating,
                                    accent = accent,
                                )
                            }
                        }
                        GroupProgressBar(
                            fraction = summary.progressFraction,
                            color = accent,
                        )
                        if (lastUpdatedMillis != null) {
                            val date = Instant.ofEpochMilli(lastUpdatedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                            Text(
                                text = stringResource(R.string.session_updated_at, date),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionGroupCard(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    val summary = group.items.collectionProgressSummary()
    val averageRating = group.items.collectionAverageRating()
    val mediaType = group.items.firstOrNull()?.item?.type
    val lastUpdatedMillis = group.items.collectionLastUpdatedMillis()
    val unitLabel = mediaType.collectionItemUnitLabel()
    val completedStr = stringResource(R.string.group_completed_count, summary.completedCount)
    val inProgressStr = stringResource(R.string.group_in_progress_count, summary.inProgressCount)
    val metaLine = listOfNotNull(
        "${group.items.size} $unitLabel",
        completedStr,
        inProgressStr,
    ).joinToString(" · ")
    val transition = updateTransition(
        targetState = isCollapsed,
        label = "collection_card_transition",
    )
    val coverWidth = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "collection_cover_width",
    ) { collapsed -> if (collapsed) 100.dp else 0.dp }
    val coverHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "collection_cover_height",
    ) { collapsed -> if (collapsed) 150.dp else 0.dp }
    val coverGap = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "collection_cover_gap",
    ) { collapsed -> if (collapsed) 8.dp else 0.dp }
    val contentHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "collection_content_height",
    ) { collapsed -> if (collapsed) 150.dp else 28.dp }
    val coverAlpha = transition.animateFloat(
        transitionSpec = { tween(140) },
        label = "collection_cover_alpha",
    ) { collapsed -> if (collapsed) 1f else 0f }
    val arrowRotation = transition.animateFloat(
        transitionSpec = { tween(180) },
        label = "collection_arrow_rotation",
    ) { collapsed -> if (collapsed) 0f else 180f }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { group.collection?.let(onCollectionClick) },
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(coverWidth.value)
                    .height(coverHeight.value)
                    .alpha(coverAlpha.value)
                    .clipToBounds(),
            ) {
                MetadataCoverImage(
                    coverUrl = group.items.collectionCoverUrl(),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp)),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            Spacer(modifier = Modifier.width(coverGap.value))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(contentHeight.value)
                    .clipToBounds(),
                verticalArrangement = if (isCollapsed) Arrangement.SpaceBetween else Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = if (isCollapsed) Alignment.Top else Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = group.resolvedTitle(),
                            style = if (isCollapsed) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isCollapsed) OmnilogColors.AppInk else accent,
                            maxLines = if (isCollapsed) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(
                            visible = isCollapsed,
                            enter = fadeIn(tween(150, delayMillis = 50)),
                            exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
                        ) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !isCollapsed,
                        enter = fadeIn(tween(120, delayMillis = 60)),
                        exit = fadeOut(tween(60)),
                    ) {
                        Text(
                            text = stringResource(R.string.collection_item_count, group.items.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(
                                if (isCollapsed) R.string.collection_expand else R.string.collection_collapse,
                            ),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = arrowRotation.value },
                            tint = accent,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(tween(150, delayMillis = 60)),
                    exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = stringResource(R.string.group_progress_prefix, summary.label),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            averageRating?.let { rating ->
                                CollectionAverageRatingSlot(
                                    rating = rating,
                                    accent = accent,
                                )
                            }
                        }
                        GroupProgressBar(
                            fraction = summary.progressFraction,
                            color = accent,
                        )
                        if (lastUpdatedMillis != null) {
                            val date = Instant.ofEpochMilli(lastUpdatedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                            Text(
                                text = stringResource(R.string.session_updated_at, date),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogColors.AppMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GroupProgressBar(
    fraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogColors.AppLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun CollectionAverageRatingSlot(
    rating: Double,
    accent: Color,
) {
    Text(
        text = "%.1f".format(rating),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
    )
}

// â”€â”€ Composable helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun HomeDisplayGroup.resolvedTitle(): String {
    return when {
        status != null -> status.label()
        title.isNotBlank() -> title
        type == HomeGroupType.Collection -> stringResource(R.string.collection_none)
        type == HomeGroupType.Author -> stringResource(R.string.group_author_unknown)
        else -> title
    }
}

@Composable
internal fun List<TrackedMedia>.collectionProgressSummary(): CollectionProgressSummary {
    val completed = count { it.currentSession?.status == TrackingStatus.Completed }
    val inProgress = count { it.currentSession?.status == TrackingStatus.InProgress }
    val mediaType = firstOrNull()?.item?.type
    var fraction = if (isNotEmpty()) completed.toFloat() / size.toFloat() else 0f
    val label = if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
        val current = sumOf { it.currentSession?.progressCurrent ?: 0 }
        val total = sumOf { it.item.progressTotal ?: 0 }
        if (total > 0) {
            fraction = current.toFloat() / total.toFloat()
            stringResource(R.string.group_progress_units, current, total, stringResource(R.string.progress_unit_episode_many))
        } else {
            stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
        }
    } else {
        stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
    }
    return CollectionProgressSummary(
        label = label,
        completedCount = completed,
        inProgressCount = inProgress,
        progressFraction = fraction,
    )
}

internal fun List<TrackedMedia>.collectionAverageRating(): Double? {
    val ratings = mapNotNull { it.currentSession?.rating }
    if (ratings.isEmpty()) return null
    return ratings.average()
}

@Composable
private fun MediaType?.itemUnitLabel(): String = when (this) {
    MediaType.Book -> stringResource(R.string.group_unit_books)
    MediaType.Game -> stringResource(R.string.group_unit_games)
    MediaType.Movie -> stringResource(R.string.group_unit_movies)
    MediaType.TvShow -> stringResource(R.string.group_unit_tv_shows)
    MediaType.Anime -> stringResource(R.string.group_unit_anime)
    null -> stringResource(R.string.group_unit_items)
}

@Composable
private fun MediaType?.collectionItemUnitLabel(): String = when (this) {
    MediaType.Anime, MediaType.TvShow -> stringResource(R.string.group_unit_seasons)
    MediaType.Book -> stringResource(R.string.group_unit_books)
    MediaType.Movie -> stringResource(R.string.group_unit_movies)
    MediaType.Game -> stringResource(R.string.group_unit_games)
    null -> stringResource(R.string.group_unit_items)
}

// â”€â”€ TrackingStatus extensions (private to this file) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

@Composable
private fun TrackingStatus.label(): String = when (this) {
    TrackingStatus.Planned -> stringResource(R.string.status_planned)
    TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
    TrackingStatus.Completed -> stringResource(R.string.status_completed)
    TrackingStatus.Paused -> stringResource(R.string.status_paused)
    TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
}

