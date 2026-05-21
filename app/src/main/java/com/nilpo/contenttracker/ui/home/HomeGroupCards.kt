package com.nilpo.contenttracker.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
) {
    when {
        group.type == HomeGroupType.Collection && group.collection != null -> CollectionGroupCard(
            group = group,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
            onCollectionClick = onCollectionClick,
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
private fun CollectionGroupCard(
    group: HomeDisplayGroup,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    AnimatedContent(
        targetState = isCollapsed,
        transitionSpec = {
            if (targetState) {
                (fadeIn(tween(240, delayMillis = 180)) + expandVertically(tween(320, delayMillis = 180), expandFrom = Alignment.Top)) togetherWith
                    fadeOut(tween(130))
            } else {
                fadeIn(tween(160, delayMillis = 60)) togetherWith
                    (fadeOut(tween(140)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top))
            }
        },
        label = "collection_card",
    ) { collapsed ->
        if (!collapsed) {
            CollectionGroupCompactHeader(group, accent, onClick, onCollectionClick)
        } else {
            CollectionGroupFullCard(group, accent, onClick, onCollectionClick)
        }
    }
}

@Composable
private fun CollectionGroupFullCard(
    group: HomeDisplayGroup,
    accent: Color,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
) {
    val summary = group.items.collectionProgressSummary()
    val mediaType = group.items.firstOrNull()?.item?.type
    val lastUpdatedMillis = group.items.collectionLastUpdatedMillis()
    val unitLabel = mediaType.collectionItemUnitLabel()
    val completedStr = stringResource(R.string.group_completed_count, summary.completedCount)
    val inProgressStr = stringResource(R.string.group_in_progress_count, summary.inProgressCount)
    val metaLine = "${group.items.size} $unitLabel · $completedStr · $inProgressStr"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.AppInk,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetadataCoverImage(
                coverUrl = group.items.collectionCoverUrl(),
                modifier = Modifier
                    .size(width = 100.dp, height = 150.dp)
                    .clip(RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .clipToBounds(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = group.resolvedTitle(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogColors.AppMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = accent,
                        )
                        if (group.collection != null) {
                            Text(
                                text = stringResource(R.string.edit),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                                modifier = Modifier.clickable { onCollectionClick(group.collection) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.group_progress_prefix, summary.label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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

@Composable
private fun CollectionGroupCompactHeader(
    group: HomeDisplayGroup,
    accent: Color,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
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
            Text(
                text = group.resolvedTitle(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
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
            if (group.collection != null) {
                Text(
                    text = stringResource(R.string.edit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.clickable { onCollectionClick(group.collection) },
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
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

// ── Composable helpers ──────────────────────────────────────────────────────

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

// ── TrackingStatus extensions (private to this file) ────────────────────────

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
