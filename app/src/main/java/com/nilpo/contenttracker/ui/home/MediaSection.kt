package com.nilpo.contenttracker.ui.home

import androidx.compose.ui.graphics.Color
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType

enum class MediaSection(
    val titleResId: Int,
    val emptyMessageResId: Int,
    val accent: Color,
    val types: Set<MediaType>,
    val defaultType: MediaType,
) {
    Anime(R.string.nav_anime, R.string.empty_anime, Color(0xFF4FC3F7), setOf(MediaType.Anime), MediaType.Anime),
    Books(R.string.nav_books, R.string.empty_books, Color(0xFFFFB347), setOf(MediaType.Book), MediaType.Book),
    Movies(R.string.nav_movies, R.string.empty_movies, Color(0xFFEF5350), setOf(MediaType.Movie, MediaType.TvShow), MediaType.Movie),
    Games(R.string.nav_games, R.string.empty_games, Color(0xFF69F0AE), setOf(MediaType.Game), MediaType.Game),
}
