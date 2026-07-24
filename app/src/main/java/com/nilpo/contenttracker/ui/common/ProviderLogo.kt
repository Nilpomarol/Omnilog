package com.nilpo.contenttracker.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource

/**
 * The provider's own mark, where the project has one.
 *
 * Null for the sources with no artwork. Callers fall back to the source's name, so a missing logo
 * costs a little polish rather than the information itself.
 */
@DrawableRes
fun ExternalRatingSource.logoRes(): Int? = when (this) {
    ExternalRatingSource.AniList -> R.drawable.anilist_logo
    ExternalRatingSource.Mal -> R.drawable.mal_logo
    ExternalRatingSource.Imdb -> R.drawable.imdb_logo
    ExternalRatingSource.Metacritic -> R.drawable.metacritic_logo
    ExternalRatingSource.Goodreads -> R.drawable.goodreads_logo_1
    ExternalRatingSource.GoogleBooks -> R.drawable.googlebooks_logo
    ExternalRatingSource.OpenLibrary -> R.drawable.openlibrary_logo
    ExternalRatingSource.Tmdb -> R.drawable.tmdb_logo
    ExternalRatingSource.RottenTomatoes -> R.drawable.rottentomatoes_logo
    ExternalRatingSource.Steam -> R.drawable.steam_logo
    ExternalRatingSource.StoryGraph -> R.drawable.storygraph_logo
    ExternalRatingSource.Rawg,
    ExternalRatingSource.FilmAffinity,
    -> null
}

/**
 * True for the marks supplied as a single dark silhouette.
 *
 * Those vanish on a dark ground, so they get tinted. Every other mark is full colour and is drawn
 * exactly as supplied — tinting a brand's own colours would destroy it.
 */
fun ExternalRatingSource.logoIsSilhouette(): Boolean = when (this) {
    ExternalRatingSource.Goodreads,
    ExternalRatingSource.StoryGraph,
    -> true

    else -> false
}

/** The pale chip a silhouette mark sits on. Warm rather than white, to match the app's ink. */
private val ChipPaper = Color(0xFFF2E9DD)

/**
 * A provider mark at a fixed height, keeping its own proportions.
 *
 * Height rather than size: these range from square app icons to wide wordmarks — Metacritic is
 * 176x40 and AniList is 512x512 — so constraining both axes would squash half of them. The painter's
 * intrinsic ratio supplies the width.
 *
 * Marks supplied as a bare dark silhouette get a pale chip to sit on, which is how their owners draw
 * them anyway. Tinting them pale instead was the first attempt and it turned Goodreads into a stray
 * lowercase "g" floating on the artwork — a letter, not a logo. Full-colour marks bring their own
 * background and are drawn untouched.
 *
 * Draws nothing when the source has no mark, so callers can place it unconditionally and let their
 * own fallback fill the space.
 */
@Composable
fun ProviderLogo(
    source: ExternalRatingSource,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val logo = source.logoRes() ?: return
    val mark = @Composable { markModifier: Modifier ->
        Image(
            painter = painterResource(logo),
            contentDescription = null,
            modifier = markModifier,
            contentScale = ContentScale.Fit,
        )
    }

    if (source.logoIsSilhouette()) {
        Surface(
            modifier = modifier.size(height),
            shape = RoundedCornerShape(4.dp),
            color = ChipPaper,
        ) {
            mark(Modifier.padding(2.dp))
        }
    } else {
        mark(modifier.height(height))
    }
}
