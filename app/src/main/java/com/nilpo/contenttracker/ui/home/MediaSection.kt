package com.nilpo.contenttracker.ui.home

import androidx.compose.ui.graphics.Color
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.theme.OmnilogColors

enum class MediaSection(
    val titleResId: Int,
    val emptyMessageResId: Int,
    val accent: Color,
    val navMark: String,
    val types: Set<MediaType>,
    val defaultType: MediaType,
) {
    Anime(R.string.nav_anime, R.string.empty_anime, OmnilogColors.Anime, "A", setOf(MediaType.Anime), MediaType.Anime),
    Books(R.string.nav_books, R.string.empty_books, OmnilogColors.Books, "B", setOf(MediaType.Book), MediaType.Book),
    Movies(R.string.nav_movies_tv, R.string.empty_movies, OmnilogColors.Tv, "C/TV", setOf(MediaType.Movie, MediaType.TvShow), MediaType.Movie),
    Games(R.string.nav_games, R.string.empty_games, OmnilogColors.Games, "G", setOf(MediaType.Game), MediaType.Game),
}
