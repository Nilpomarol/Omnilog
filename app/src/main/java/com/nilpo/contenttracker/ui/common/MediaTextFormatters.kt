package com.nilpo.contenttracker.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus

@Composable
fun MetadataSummary(
    trackedMedia: TrackedMedia,
    session: TrackingSession,
) {
    val seasonText = seasonSummary(trackedMedia.seasonsFor(session))
    val platform = session.platform?.let { stringResource(R.string.platform_label, it.name) }
    val ownership = stringResource(R.string.owned_label).takeIf { trackedMedia.item.ownership.isOwned }
    val externalRating = trackedMedia.externalRatings.firstOrNull()?.let {
        stringResource(
            R.string.external_rating,
            it.source.name,
            it.score,
            it.maxScore,
        )
    }
    val externalTracking = trackedMedia.externalTracking.takeIf { it.isNotEmpty() }?.let { tracking ->
        stringResource(
            R.string.external_tracking_label,
            tracking.joinToString(", ") { it.source.name },
        )
    }

    val details = listOfNotNull(seasonText, platform, ownership, externalRating, externalTracking)

    if (details.isNotEmpty()) {
        Text(
            text = details.joinToString(" | "),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun sessionLabel(session: TrackingSession): String {
    val visitLabel = when {
        session.sessionNumber == 1 -> stringResource(R.string.session_first_time)
        else -> stringResource(R.string.session_number, session.sessionNumber)
    }

    return "$visitLabel - ${session.status.statusDisplayName()}"
}

@Composable
fun sessionSummary(session: TrackingSession): String {
    val progress = when (val total = session.progressTotal) {
        null -> stringResource(R.string.progress_value, session.progressCurrent)
        else -> stringResource(R.string.progress_with_total, session.progressCurrent, total)
    }

    val rating = session.rating?.let { stringResource(R.string.rating_value, it) }

    return listOfNotNull(progress, rating).joinToString(" | ")
}

@Composable
fun seasonSummary(seasons: List<SeasonProgress>): String? {
    if (seasons.isEmpty()) {
        return null
    }

    val seasonLabels = seasons.map { season ->
        val total = season.progressTotal
        if (total == null) {
            stringResource(
                R.string.season_progress_without_total,
                season.seasonNumber,
                season.progressCurrent,
            )
        } else {
            stringResource(
                R.string.season_progress,
                season.seasonNumber,
                season.progressCurrent,
                total,
            )
        }
    }

    return seasonLabels.joinToString(" | ")
}

@Composable
private fun TrackingStatus.statusDisplayName(): String {
    return when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }
}
