package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CoverWidth = 84.dp
private val CoverHeight = 126.dp  // 2:3 ratio
private const val MaxVisibleGenres = 2

@Composable
fun MediaCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val item = trackedMedia.item
    val session = trackedMedia.currentSession
    val creator = item.creators.firstOrNull()
    val collection = formatCollectionDisplayName(
        trackedMedia.collection?.name,
        item.collectionSortOrder,
    )
    val genres = item.genres.take(MaxVisibleGenres)
    val additionalGenreCount = (item.genres.size - genres.size).coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier.size(width = CoverWidth, height = CoverHeight),
                shape = RoundedCornerShape(6.dp),
            )

            // Keep the cover compact at the default scale, but let the information
            // column grow when system text needs more room.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = CoverHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = displayMediaTitle(item.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogColors.AppInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (collection != null) {
                            Text(
                                text = collection,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (creator != null) {
                            Text(
                                text = creator,
                                style = MaterialTheme.typography.bodySmall,
                                color = OmnilogColors.AppMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (genres.isNotEmpty()) {
                            GenreChips(
                                genres = genres,
                                additionalGenreCount = additionalGenreCount,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (item.ownership.isOwned) {
                            OwnedBadge()
                        }
                        if (session != null) {
                            CardStateIconBadge(status = session.status)
                        }
                        trailingAction?.invoke()
                    }
                }

                CardProgressFooter(
                    session = session,
                    progressTotal = item.progressTotal.takeUnless { item.type == MediaType.Game },
                    mediaType = item.type,
                    progressColor = session?.status?.stateColor ?: accent,
                    accent = accent,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(
    genres: List<String>,
    additionalGenreCount: Int,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        maxItemsInEachRow = 2,
    ) {
        genres.forEach { genre ->
            GenreChip(text = genre)
        }
        if (additionalGenreCount > 0) {
            GenreChip(text = "+$additionalGenreCount")
        }
    }
}

@Composable
private fun GenreChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppLine,
        contentColor = OmnilogColors.AppMuted,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 10.sp,
            ),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CardProgressFooter(
    session: TrackingSession?,
    progressTotal: Int?,
    mediaType: MediaType,
    progressColor: Color,
    accent: Color,
) {
    val isGame = mediaType == MediaType.Game
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = session.progressLabel(progressTotal, mediaType),
                style = if (isGame) {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                fontWeight = if (isGame) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isGame) accent else OmnilogColors.AppMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session?.rating?.let { rating ->
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
            }
        }
        if (progressTotal != null && progressTotal > 0) {
            CardProgressBar(
                fraction = session.progressFraction(progressTotal),
                color = progressColor,
            )
        }
        CardDates(session = session)
    }
}

@Composable
private fun CardStateIconBadge(
    status: TrackingStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.stateColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color,
        contentColor = Color.Black,
    ) {
        Icon(
            painter = painterResource(status.iconResId),
            contentDescription = status.label(),
            modifier = Modifier
                .padding(4.dp)
                .size(13.dp),
        )
    }
}

@Composable
private fun CardProgressBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogColors.AppLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun CardDates(session: TrackingSession?) {
    val parts = buildList {
        session?.startedAt?.let { add(stringResource(R.string.session_started_at, it.formatDate())) }
        session?.finishedAt?.let { add(stringResource(R.string.session_finished_at, it.formatDate())) }
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = OmnilogColors.AppMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

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

private fun TrackingSession?.progressFraction(progressTotal: Int?): Float {
    val current = this?.progressCurrent ?: 0
    if (progressTotal == null || progressTotal <= 0) return 0f
    return current.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
}

private fun TrackingSession?.progressLabel(progressTotal: Int?, mediaType: MediaType): String {
    val current = this?.progressCurrent ?: 0
    val unit = mediaType.progressUnit()
    return if (progressTotal != null) "$current/$progressTotal $unit" else "$current $unit"
}

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yy"))

private fun MediaType.progressUnit(): String = when (this) {
    MediaType.Anime, MediaType.TvShow -> "episodis"
    MediaType.Book -> "pagines"
    MediaType.Movie -> "min"
    MediaType.Game -> "h"
}
