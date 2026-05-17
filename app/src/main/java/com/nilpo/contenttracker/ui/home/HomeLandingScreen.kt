package com.nilpo.contenttracker.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeLandingScreen(
    uiState: HomeUiState,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = uiState.allTrackedItems
    val activeItems = items
        .filter { it.currentSession?.status == TrackingStatus.InProgress }
        .sortedByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L }
        .take(8)
    val topRatedItems = items
        .filter { it.currentSession?.rating != null }
        .sortedWith(
            compareByDescending<TrackedMedia> { it.currentSession?.rating ?: 0 }
                .thenByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L },
        )
        .take(8)
    val recentItems = items
        .filter { (it.currentSession?.updatedAtEpochMillis ?: 0L) > 0L }
        .sortedByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L }
        .take(8)

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    DashboardSearch()
                }

                item {
                    HomeStats(items = items)
                }

                if (items.isEmpty()) {
                    item {
                        EmptyHomeState()
                    }
                } else {
                    if (activeItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_active_title),
                                items = activeItems,
                                onMediaClick = onMediaClick,
                            )
                        }
                    }

                    if (topRatedItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_top_rated_title),
                                items = topRatedItems,
                                onMediaClick = onMediaClick,
                            )
                        }
                    }

                    if (recentItems.isNotEmpty()) {
                        item {
                            HomeCarousel(
                                title = stringResource(R.string.home_recent_title),
                                items = recentItems,
                                onMediaClick = onMediaClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSearch() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = OmnilogColors.AppMuted,
            )
            Text(
                text = stringResource(R.string.search_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun HomeStats(items: List<TrackedMedia>) {
    val currentYear = LocalDate.now().year
    val ratedSessions = items.mapNotNull { it.currentSession?.rating }
    val averageRating = ratedSessions
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.let { "%.1f".format(it) }
        ?: "-"
    val watchedHours = items.watchedMovieHours()
    val titlesThisYear = items.count { trackedMedia ->
        trackedMedia.sessions.any { session ->
            session.finishedAt?.year == currentYear ||
                session.startedAt?.year == currentYear ||
                session.updatedDate()?.year == currentYear
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            label = stringResource(R.string.home_stat_total_titles),
            value = items.size.toString(),
            icon = painterResource(R.drawable.ic_kpi_titles),
            accent = OmnilogColors.Dashboard,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_hours_watched),
            value = watchedHours,
            icon = painterResource(R.drawable.ic_kpi_hourglass),
            accent = OmnilogColors.Tv,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_average_rating),
            value = averageRating,
            icon = painterResource(R.drawable.ic_kpi_rating),
            accent = OmnilogColors.Books,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_titles_this_year),
            value = titlesThisYear.toString(),
            icon = painterResource(R.drawable.ic_kpi_year),
            accent = OmnilogColors.Games,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: Painter,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(27.dp)
                    .padding(top = 8.dp, end = 10.dp),
                tint = accent.copy(alpha = 0.80f),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
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

@Composable
private fun HomeCarousel(
    title: String,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionTitle(title = title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { trackedMedia ->
                HomeMediaTile(
                    trackedMedia = trackedMedia,
                    accent = trackedMedia.item.type.sectionAccent(),
                    onClick = { onMediaClick(trackedMedia) },
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OmnilogColors.AppLine),
        )
    }
}

@Composable
private fun HomeMediaTile(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
) {
    val session = trackedMedia.currentSession

    Surface(
        modifier = Modifier
            .width(176.dp)
            .height(276.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xFF17110D).copy(alpha = 0.18f),
                                Color(0xFF15110E).copy(alpha = 0.86f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = displayMediaTitle(trackedMedia.item.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                session?.let {
                    StatePill(
                        status = it.status,
                    )
                    SessionStateInfo(
                        session = it,
                        progressTotal = trackedMedia.item.progressTotal,
                        color = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatePill(
    status: TrackingStatus,
) {
    val color = status.stateColor

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.92f),
        contentColor = Color.Black,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(status.iconResId),
                contentDescription = null,
                modifier = Modifier.height(13.dp),
            )
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SessionStateInfo(
    session: TrackingSession,
    progressTotal: Int?,
    color: Color,
) {
    when (session.status) {
        TrackingStatus.Planned -> Unit
        TrackingStatus.Completed -> {
            RatingDateLine(
                rating = session.rating,
                date = session.finishedAt?.formatDate(),
                accent = color,
            )
        }
        TrackingStatus.Dropped,
        TrackingStatus.Paused,
            -> {
            RatingDateLine(
                rating = session.rating,
                date = session.finishedAt?.formatDate(),
                accent = color,
            )
            ProgressBar(
                fraction = session.progressFraction(progressTotal),
                color = color.copy(alpha = 0.82f),
            )
        }
        TrackingStatus.InProgress -> {
            ProgressBar(
                fraction = session.progressFraction(progressTotal),
                color = color,
            )
            RatingDateLine(
                rating = session.rating,
                date = session.updatedDate()?.formatDate(),
                accent = color,
            )
        }
    }
}

@Composable
private fun RatingDateLine(
    rating: Int?,
    date: String?,
    accent: Color,
) {
    if (rating == null && date == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        rating?.let {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Text(
                text = "/10",
                modifier = Modifier.padding(bottom = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
        if (rating != null && date != null) {
            Text(
                text = "|",
                modifier = Modifier.padding(bottom = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.46f),
            )
        }
        date?.let {
            Text(
                text = it,
                modifier = Modifier.padding(bottom = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.24f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun EmptyHomeState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Text(
            text = stringResource(R.string.home_empty_state),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogColors.AppMuted,
        )
    }
}

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

@Composable
private fun TrackingStatus.label(): String =
    when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }

private fun MediaType.sectionAccent(): Color =
    when (this) {
        MediaType.Anime -> MediaSection.Anime.accent
        MediaType.Book -> MediaSection.Books.accent
        MediaType.Movie,
        MediaType.TvShow,
            -> MediaSection.Movies.accent
        MediaType.Game -> MediaSection.Games.accent
    }

private fun TrackingSession.progressFraction(progressTotal: Int?): Float {
    if (progressTotal == null || progressTotal <= 0) return 0f
    return progressCurrent.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
}

private fun TrackingSession.updatedDate(): LocalDate? {
    if (updatedAtEpochMillis <= 0L) return null
    return Instant.ofEpochMilli(updatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun List<TrackedMedia>.watchedMovieHours(): String {
    val watchedMinutes = sumOf { trackedMedia ->
        if (trackedMedia.item.type == MediaType.Movie) {
            trackedMedia.currentSession?.progressCurrent ?: 0
        } else {
            0
        }
    }
    val hours = watchedMinutes / 60.0

    return if (hours < 10.0 && watchedMinutes % 60 != 0) {
        "%.1f".format(hours)
    } else {
        hours.toInt().toString()
    }
}
