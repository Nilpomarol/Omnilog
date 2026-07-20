package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@Composable
internal fun StatsPeriod.label(): String {
    return when (this) {
        StatsPeriod.AllTime -> stringResource(R.string.stats_period_all_time)
        StatsPeriod.ThisYear -> stringResource(R.string.stats_period_this_year)
        StatsPeriod.Last12Months -> stringResource(R.string.stats_period_last_12_months)
        is StatsPeriod.Year -> year.toString()
    }
}

internal fun statsPeriodOptions(items: List<TrackedMedia>): List<StatsPeriod> {
    val yearOptions = items
        .flatMap { trackedMedia -> trackedMedia.sessions }
        .mapNotNull { session -> session.statsYear() }
        .filter { year -> year < LocalDate.now().year }
        .distinct()
        .sortedDescending()
        .map { year -> StatsPeriod.Year(year) }

    return listOf(
        StatsPeriod.ThisYear,
        StatsPeriod.Last12Months,
        StatsPeriod.AllTime,
    ) + yearOptions
}

internal fun TrackingSession.statsYear(): Int? {
    return finishedAt?.year
        ?: startedAt?.year
        ?: updatedAtEpochMillis.takeIf { millis -> millis > 0L }?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .year
        }
}

@Composable
internal fun TrackingStatus.label(): String {
    return when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }
}

@Composable
internal fun MediaType.label(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.media_type_anime)
        MediaType.Book -> stringResource(R.string.media_type_book)
        MediaType.Movie -> stringResource(R.string.media_type_movie)
        MediaType.TvShow -> stringResource(R.string.media_type_tv_show)
        MediaType.Game -> stringResource(R.string.media_type_game)
    }
}

/** Plural form, for captions that name a whole medium rather than one title. */
@Composable
internal fun MediaType.pluralLabel(): String {
    return when (this) {
        MediaType.Anime -> stringResource(R.string.stats_media_plural_anime)
        MediaType.Book -> stringResource(R.string.stats_media_plural_book)
        MediaType.Movie -> stringResource(R.string.stats_media_plural_movie)
        MediaType.TvShow -> stringResource(R.string.stats_media_plural_tv_show)
        MediaType.Game -> stringResource(R.string.stats_media_plural_game)
    }
}

internal fun MediaType.progressUnit(): String {
    return when (this) {
        MediaType.Anime,
        MediaType.TvShow,
            -> "ep"
        MediaType.Book -> "pag"
        MediaType.Movie -> "min"
        MediaType.Game -> "h"
    }
}

internal fun Double.roundedStatValue(): String {
    return if (this >= 10.0) {
        "%.0f".format(this)
    } else {
        "%.1f".format(this)
    }
}

internal fun Int.compactStatValue(): String {
    return when {
        this >= 100_000 -> "${this / 1_000}k"
        this >= 10_000 -> "%.1fk".format(this / 1_000.0)
        else -> this.toString()
    }
}

@Composable
@ReadOnlyComposable
internal fun genreChartColor(index: Int): Color {
    val colors = listOf(
        OmnilogTheme.accents.Anime,
        OmnilogTheme.accents.Books,
        OmnilogTheme.accents.Tv,
        OmnilogTheme.accents.Games,
        OmnilogTheme.accents.Dashboard,
        OmnilogTheme.accents.Paused,
    )
    return colors[index % colors.size]
}

@Composable
@ReadOnlyComposable
internal fun languageChartColor(index: Int): Color {
    val colors = listOf(
        OmnilogTheme.accents.Games,
        OmnilogTheme.accents.Tv,
        OmnilogTheme.accents.Books,
        OmnilogTheme.accents.Series,
        OmnilogTheme.accents.Dashboard,
        OmnilogTheme.accents.Anime,
        OmnilogTheme.accents.Completed,
        OmnilogTheme.accents.Paused,
    )
    return colors[index % colors.size]
}

internal val TrackingStatus.stateColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogTheme.accents.Planned
        TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
        TrackingStatus.Completed -> OmnilogTheme.accents.Completed
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
    }

@Composable
@ReadOnlyComposable
internal fun MediaType.statsColor(): Color {
    return when (this) {
        MediaType.Anime -> OmnilogTheme.accents.Anime
        MediaType.Book -> OmnilogTheme.accents.Books
        MediaType.Movie -> OmnilogTheme.accents.Movie
        MediaType.TvShow -> OmnilogTheme.accents.Series
        MediaType.Game -> OmnilogTheme.accents.Games
    }
}

/** Theme-resolved counterpart of [StatsMediaFilter.accent]; see `MediaSection.themedAccent`. */
@Composable
@ReadOnlyComposable
internal fun StatsMediaFilter.themedAccent(): Color = when (this) {
    StatsMediaFilter.All -> OmnilogTheme.accents.Dashboard
    StatsMediaFilter.Anime -> OmnilogTheme.accents.Anime
    StatsMediaFilter.Books -> OmnilogTheme.accents.Books
    StatsMediaFilter.Movies -> OmnilogTheme.accents.Tv
    StatsMediaFilter.Games -> OmnilogTheme.accents.Games
}

internal enum class StatsMediaFilter(
    val types: Set<MediaType>,
    val accent: Color,
) {
    All(MediaType.entries.toSet(), OmnilogColors.Dashboard),
    Anime(setOf(MediaType.Anime), MediaSection.Anime.accent),
    Books(setOf(MediaType.Book), MediaSection.Books.accent),
    Movies(setOf(MediaType.Movie, MediaType.TvShow), MediaSection.Movies.accent),
    Games(setOf(MediaType.Game), MediaSection.Games.accent),
}

@Composable
internal fun StatsMediaFilter.label(): String {
    return when (this) {
        StatsMediaFilter.All -> stringResource(R.string.stats_filter_all)
        StatsMediaFilter.Anime -> stringResource(R.string.nav_anime)
        StatsMediaFilter.Books -> stringResource(R.string.nav_books)
        StatsMediaFilter.Movies -> stringResource(R.string.nav_movies_tv)
        StatsMediaFilter.Games -> stringResource(R.string.nav_games)
    }
}
