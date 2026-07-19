package com.nilpo.contenttracker.ui.home

import androidx.compose.ui.graphics.Color
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.serialization.Serializable

@Serializable
enum class MediaSection(
    val titleResId: Int,
    val emptyMessageResId: Int,
    val accent: Color,
    val navMark: String,
    val types: Set<MediaType>,
    val defaultType: MediaType,
) {
    Anime(
        R.string.nav_anime,
        R.string.empty_anime,
        OmnilogColors.Anime,
        "A",
        setOf(MediaType.Anime),
        MediaType.Anime
    ),
    Books(
        R.string.nav_books,
        R.string.empty_books,
        OmnilogColors.Books,
        "B",
        setOf(MediaType.Book),
        MediaType.Book
    ),
    Movies(
        R.string.nav_movies_tv,
        R.string.empty_movies,
        OmnilogColors.Tv,
        "C/TV",
        setOf(MediaType.Movie, MediaType.TvShow),
        MediaType.Movie
    ),
    Games(
        R.string.nav_games,
        R.string.empty_games,
        OmnilogColors.Games,
        "G",
        setOf(MediaType.Game),
        MediaType.Game
    ),
}

internal val MediaSection.navIconResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.drawable.ic_nav_anime
        MediaSection.Books -> R.drawable.ic_nav_books
        // The camera stands for the whole "Cinema i TV" section. It is the film half of a section
        // that also holds series, but a single confident mark beats a composite, and this one at
        // least matches the glyph the rest of the app now uses for films.
        MediaSection.Movies -> R.drawable.ic_media_movie
        MediaSection.Games -> R.drawable.ic_nav_games
    }

internal val MediaSection.emptyTitleResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.empty_anime_title
        MediaSection.Books -> R.string.empty_books_title
        MediaSection.Movies -> R.string.empty_movies_title
        MediaSection.Games -> R.string.empty_games_title
    }

/** Label for the action that starts the add flow, matching the title of the screen it opens. */
internal val MediaSection.addActionResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.add_title_anime
        MediaSection.Books -> R.string.add_title_book
        MediaSection.Movies -> R.string.add_title_movie_tv
        MediaSection.Games -> R.string.add_title_game
    }

/** Null where no provider offers a bulk export for this section. */
internal val MediaSection.importActionResId: Int?
    get() = when (this) {
        MediaSection.Anime -> R.string.empty_import_anime
        MediaSection.Books -> R.string.empty_import_books
        MediaSection.Movies -> R.string.empty_import_movies
        MediaSection.Games -> null
    }

internal val MediaSection.creatorBrowseLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.browse_studios
        MediaSection.Books -> R.string.browse_authors
        MediaSection.Movies -> R.string.browse_directors
        MediaSection.Games -> R.string.browse_developers
    }

internal val MediaSection.creatorFilterLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.filter_studios
        MediaSection.Books -> R.string.filter_authors
        MediaSection.Movies -> R.string.filter_directors
        MediaSection.Games -> R.string.filter_developers
    }

internal val MediaSection.creatorUnknownLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.group_creator_unknown_studio
        MediaSection.Books -> R.string.group_author_unknown
        MediaSection.Movies -> R.string.group_creator_unknown_director
        MediaSection.Games -> R.string.group_creator_unknown_developer
    }

internal val MediaSection.creatorDetailLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.creator_page_subtitle_studio
        MediaSection.Books -> R.string.author_page_subtitle
        MediaSection.Movies -> R.string.creator_page_subtitle_director
        MediaSection.Games -> R.string.creator_page_subtitle_developer
    }

internal val MediaSection.creatorExpandLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.creator_expand_studio
        MediaSection.Books -> R.string.author_expand
        MediaSection.Movies -> R.string.creator_expand_director
        MediaSection.Games -> R.string.creator_expand_developer
    }

internal val MediaSection.creatorCollapseLabelResId: Int
    get() = when (this) {
        MediaSection.Anime -> R.string.creator_collapse_studio
        MediaSection.Books -> R.string.author_collapse
        MediaSection.Movies -> R.string.creator_collapse_director
        MediaSection.Games -> R.string.creator_collapse_developer
    }
