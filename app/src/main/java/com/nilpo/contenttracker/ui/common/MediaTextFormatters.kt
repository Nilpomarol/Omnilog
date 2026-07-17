package com.nilpo.contenttracker.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus

fun displayMediaTitle(title: String): String {
    val cleanedTitle = title
        .replace(Regex("""\s*\([^)]*\)"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    return cleanedTitle.ifBlank { title.trim() }
}

/**
 * Names the providers that failed, sorted so the sentence does not reorder itself with whatever
 * order the providers happened to fail in. [conjunctionTemplate] is `list_conjunction`, taken as
 * a raw template so this stays testable without Android resources.
 */
internal fun formatProviderEnumeration(
    failedSources: List<MetadataSource>,
    conjunctionTemplate: String,
): String {
    val names = failedSources.map { it.displayName() }.sorted()
    return when (names.size) {
        0 -> ""
        1 -> names.single()
        else -> String.format(conjunctionTemplate, names.dropLast(1).joinToString(", "), names.last())
    }
}

/**
 * Renders the message naming the providers that failed while others returned results.
 *
 * The plural form agrees with how many providers failed, so the sentence reads correctly for
 * both `TMDb no ha respost` and `TMDb i RAWG no han respost`.
 */
@Composable
fun partialSearchFailureMessage(failedSources: List<MetadataSource>): String {
    val enumeration = formatProviderEnumeration(
        failedSources = failedSources,
        conjunctionTemplate = stringResource(R.string.list_conjunction),
    )
    return pluralStringResource(
        R.plurals.metadata_search_partial_error,
        failedSources.size,
        enumeration,
    )
}

fun formatExternalRatingOnTen(score: Double, maxScore: Double): String {
    val normalizedScore = if (maxScore > 0.0) {
        score / maxScore * 10.0
    } else {
        score
    }
    return "${formatDecimal(normalizedScore)}/10"
}

@Composable
fun MetadataSummary(
    trackedMedia: TrackedMedia,
    session: TrackingSession,
) {
    val platform = session.platform?.let { stringResource(R.string.platform_label, it.name) }
    val ownership = stringResource(R.string.owned_label).takeIf { trackedMedia.item.ownership.isOwned }
    val externalRating = trackedMedia.externalRatings.firstOrNull()?.let {
        "${it.source.name} ${formatExternalRatingOnTen(it.score, it.maxScore)}"
    }
    val details = listOfNotNull(platform, ownership, externalRating)

    if (details.isNotEmpty()) {
        Text(
            text = details.joinToString(" | "),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
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
fun sessionSummary(session: TrackingSession, progressTotal: Int?): String {
    val progress = when (progressTotal) {
        null -> stringResource(R.string.progress_value, session.progressCurrent)
        else -> stringResource(R.string.progress_with_total, session.progressCurrent, progressTotal)
    }

    val rating = session.rating?.let { stringResource(R.string.rating_value, it) }

    return listOfNotNull(progress, rating).joinToString(" | ")
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
