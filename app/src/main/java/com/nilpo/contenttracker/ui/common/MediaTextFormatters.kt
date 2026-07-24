package com.nilpo.contenttracker.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The locale every formatted date goes through.
 *
 * The app ships one language: `res/values/` holds Catalan copy and there is no other qualifier, so
 * a device set to English would otherwise render `January` and `Monday` beside Catalan text.
 * `Locale.getDefault()` follows the device, not the app, which is the wrong authority here.
 */
val OmnilogLocale: Locale = Locale.forLanguageTag("ca")

/**
 * The unit a medium measures progress in, spelled out and agreeing in number with [value].
 *
 * Every surface that writes a progress unit goes through here or through
 * [compactProgressUnitLabel]. Earlier there were separate hardcoded copies per screen, which
 * silently drifted — the library rows read `pagines` while the Home card for the same book
 * read `pàgines`.
 *
 * `StatsScreen` deliberately keeps its own chart-axis abbreviations (`ep`, `pag`) and is not
 * a caller.
 */
@Composable
fun progressUnitLabel(mediaType: MediaType, value: Int): String =
    when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
            -> if (value == 1) {
            stringResource(R.string.progress_unit_episode_one)
        } else {
            stringResource(R.string.progress_unit_episode_many)
        }
        MediaType.Book -> if (value == 1) {
            stringResource(R.string.progress_unit_page_one)
        } else {
            stringResource(R.string.progress_unit_page_many)
        }
        MediaType.Movie -> if (value == 1) {
            stringResource(R.string.progress_unit_minute_one)
        } else {
            stringResource(R.string.progress_unit_minute_many)
        }
        MediaType.Game -> if (value == 1) {
            stringResource(R.string.progress_unit_hour_one)
        } else {
            stringResource(R.string.progress_unit_hour_many)
        }
    }

/**
 * The compact unit vocabulary for dense card and chip surfaces: full words where they are
 * short enough (`episodis`, `pàgines`), abbreviations where they are not (`min`, `h`).
 * Editors and sheets spell the unit out via [progressUnitLabel] instead.
 */
@Composable
fun compactProgressUnitLabel(mediaType: MediaType): String =
    when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
            -> stringResource(R.string.progress_unit_episode_many)
        MediaType.Book -> stringResource(R.string.progress_unit_page_many)
        MediaType.Movie -> stringResource(R.string.progress_unit_minute_compact)
        MediaType.Game -> stringResource(R.string.progress_unit_hour_compact)
    }

/** A session's progress as `450/752 pàgines`, or `450 pàgines` when the total is unknown. */
@Composable
fun TrackingSession?.progressLabel(progressTotal: Int?, mediaType: MediaType): String {
    val current = this?.progressCurrent ?: 0
    val unit = compactProgressUnitLabel(mediaType)
    return if (progressTotal != null) "$current/$progressTotal $unit" else "$current $unit"
}

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

fun formatExternalRating(
    score: Double,
    maxScore: Double,
    mediaType: MediaType,
    source: ExternalRatingSource?,
): String {
    if (mediaType == MediaType.Game && source == ExternalRatingSource.Steam) {
        val percentage = if (maxScore > 0.0) score / maxScore * 100.0 else score
        return "${percentage.roundToInt()}%"
    }
    return formatExternalRatingOnTen(score, maxScore)
}

/**
 * The same figure as [formatExternalRating], without the denominator.
 *
 * Every source is normalised onto ten before display, so "/10" repeats on every row and tells the
 * reader nothing they cannot already assume. Steam's game score keeps its percent sign, because
 * there the unit is the difference between 87% approval and 8.7 out of 10.
 */
fun formatExternalRatingCompact(
    score: Double,
    maxScore: Double,
    mediaType: MediaType,
    source: ExternalRatingSource?,
): String {
    if (mediaType == MediaType.Game && source == ExternalRatingSource.Steam) {
        val percentage = if (maxScore > 0.0) score / maxScore * 100.0 else score
        return "${percentage.roundToInt()}%"
    }
    val normalizedScore = if (maxScore > 0.0) score / maxScore * 10.0 else score
    return formatDecimal(normalizedScore)
}

@Composable
fun localizedSteamScoreDescriptor(
    mediaType: MediaType,
    source: ExternalRatingSource?,
    descriptor: String?,
): String? {
    if (mediaType != MediaType.Game || source != ExternalRatingSource.Steam) return null
    return steamScoreDescriptorResId(descriptor)?.let { stringResource(it) }
}

@androidx.annotation.StringRes
internal fun steamScoreDescriptorResId(descriptor: String?): Int? {
    return when (descriptor?.trim()?.lowercase(Locale.ROOT)) {
        "overwhelmingly positive" -> R.string.steam_score_overwhelmingly_positive
        "very positive" -> R.string.steam_score_very_positive
        "positive" -> R.string.steam_score_positive
        "mostly positive" -> R.string.steam_score_mostly_positive
        "mixed" -> R.string.steam_score_mixed
        "mostly negative" -> R.string.steam_score_mostly_negative
        "negative" -> R.string.steam_score_negative
        "very negative" -> R.string.steam_score_very_negative
        "overwhelmingly negative" -> R.string.steam_score_overwhelmingly_negative
        "no user reviews" -> R.string.steam_score_no_reviews
        else -> null
    }
}

@Composable
fun MetadataSummary(
    trackedMedia: TrackedMedia,
    session: TrackingSession,
) {
    val platform = session.platform?.let { stringResource(R.string.platform_label, it.name) }
    val ownership = stringResource(R.string.owned_label).takeIf { trackedMedia.item.isOwned }
    val externalRating = trackedMedia.primaryExternalRating?.let {
        "${it.source.displayName()} ${formatExternalRating(it.score, it.maxScore, trackedMedia.item.type, it.source)}"
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
        String.format(OmnilogLocale, "%.1f", value)
    }
}

@Composable
fun sessionLabel(session: TrackingSession, visitNumber: Int): String {
    val visitLabel = when {
        visitNumber <= 1 -> stringResource(R.string.session_first_time)
        else -> stringResource(R.string.session_number, visitNumber)
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
