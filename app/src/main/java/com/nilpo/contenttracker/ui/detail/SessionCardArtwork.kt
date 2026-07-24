package com.nilpo.contenttracker.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The medium's own artwork, behind the session card.
 *
 * It says what kind of thing this is without spending a word or a row on saying it, and it is the
 * one part of the card that is not the same shape on every item.
 *
 * Drawn in the *type's* accent rather than the session's: the state colour already carries the
 * chip, the graphic and the button, and tying the artwork to it too would mean pausing a series
 * repainted its branches. The type is a fact about the work and does not move.
 *
 * A single flat silhouette, tinted at draw time — which is why the asset carries no colour of its
 * own. One file per medium, all five drawn in the same hand.
 */
@Composable
fun SessionCardArtwork(
    mediaType: MediaType,
    modifier: Modifier = Modifier,
) {
    val art = mediaType.artworkRes()
    // The light theme's accents are darker and read roughly twice as strong at the same alpha, the
    // same asymmetry the accent sets themselves exist for.
    val onDark = OmnilogTheme.colors.appBackground.luminance() < 0.5f
    val alpha = if (onDark) ArtAlphaOnDark else ArtAlphaOnLight

    Image(
        painter = painterResource(art),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth(ArtWidthFraction)
            // The fade has to be a mask rather than per-shape alpha: this is a bitmap, so there are
            // no shapes to fade. Offscreen compositing is what gives DstIn something to cut into.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * FadeRadiusFactor,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        alignment = Alignment.TopEnd,
        contentScale = ContentScale.FillWidth,
        alpha = alpha,
        colorFilter = ColorFilter.tint(mediaType.typeAccent().saturated()),
    )
}

/**
 * The accent, pushed further from grey before it is laid down at a tenth of its strength.
 *
 * Most of an accent's chroma is spent on the panel underneath at these alphas, and what came back
 * was closer to neutral than to the colour the medium is supposed to have. Saturating first costs
 * nothing in legibility — the alpha is what keeps the drawing behind the text, not the hue — and
 * hands the artwork back the colour it is meant to be read in.
 */
private fun Color.saturated(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[1] = (hsv[1] * SaturationBoost).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private const val SaturationBoost = 1.5f

/** How much of the card's width the artwork spans, anchored to the top-right corner. */
private const val ArtWidthFraction = 0.82f

private const val ArtAlphaOnDark = 0.16f
private const val ArtAlphaOnLight = 0.10f

/**
 * How far the artwork reaches from its corner before it has faded out entirely, as a fraction of
 * its own width.
 *
 * Has to be well under 1: the diagonal of the drawn area is longer than its width, so a radius of
 * 1.15 covered every pixel of it and faded nothing. The point of the mask is that the far corner —
 * where the dates sit — receives none of it.
 */
private const val FadeRadiusFactor = 0.78f

@DrawableRes
private fun MediaType.artworkRes(): Int = when (this) {
    MediaType.Anime -> R.drawable.art_anime
    MediaType.Book -> R.drawable.art_books
    MediaType.Movie -> R.drawable.art_movie
    MediaType.TvShow -> R.drawable.art_series
    MediaType.Game -> R.drawable.art_games
}

@Composable
private fun MediaType.typeAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
}
