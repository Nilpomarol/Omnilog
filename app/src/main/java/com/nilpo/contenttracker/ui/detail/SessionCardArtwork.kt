package com.nilpo.contenttracker.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The medium's own artwork, as the session card's surface rather than as a watermark on it.
 *
 * Each drawing is a whole composition — a wave and blossoms, a stack of books and a cup, a reel and a
 * clapperboard — not a corner motif. That rules out cropping a fragment into a corner and dropping it
 * to a tenth of its strength: at that size and that alpha the drawing stops being a picture and turns
 * into texture, which is what the first attempt at this looked like. So it runs the full bleed of the
 * card at close to full force, and the text stays legible because [legibilityVeil] pulls the drawing
 * back only where words actually are.
 *
 * Drawn in the *type's* accent rather than the session's. The state colour already owns the chip, the
 * figure, the graphic and the button; tying the artwork to it too would mean pausing a series
 * repainted its branches, and Planejat's grey would drain every drawing to greyscale. The type is a
 * fact about the work and does not move — which also gives the card two colours instead of one.
 *
 * A modifier rather than a composable laid out behind the content, because the card is rendered
 * inside a row measured at [androidx.compose.foundation.layout.IntrinsicSize.Min] — the rail in
 * `SessionThread` takes its height from the card — and a `BoxWithConstraints` there would throw.
 * Drawing behind the content node reads the same size without a second layout pass and without
 * subcomposition. Apply it to the card's content, not to the `Surface`: `Surface` draws its own
 * colour over its children's background.
 *
 * The assets carry no colour of their own: one flat white silhouette per medium, tinted here.
 */
@Composable
fun Modifier.sessionCardArtwork(mediaType: MediaType): Modifier {
    val onDark = OmnilogTheme.colors.appBackground.luminance() < 0.5f
    val alpha = mediaType.artAlpha.let { if (onDark) it.dark else it.light }
    val painter = painterResource(mediaType.artworkRes())
    val tint = ColorFilter.tint(mediaType.typeAccent())
    val veil = legibilityVeil(ground = cardGround(mediaType), panel = OmnilogTheme.colors.appPanel)

    return clipToBounds().drawBehind {
        drawArtwork(painter = painter, alpha = alpha, tint = tint)
        drawRect(brush = veil)
    }
}

/**
 * The drawing, larger than the card and hanging off two of its edges.
 *
 * Oversized on purpose: at the card's own size the composition would sit centred in a box and read as
 * a sticker. Letting it run past the top and the trailing corner is what makes it read as a detail of
 * something larger that the card happens to be a window onto.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArtwork(
    painter: Painter,
    alpha: Float,
    tint: ColorFilter,
) {
    val height = size.height * ArtHeightFraction
    val width = height * painter.intrinsicSize.let { it.width / it.height }
    translate(
        left = size.width * (1f + ArtOverhangEnd) - width,
        top = -size.height * ArtOverhangTop,
    ) {
        with(painter) { draw(size = Size(width, height), alpha = alpha, colorFilter = tint) }
    }
}

/**
 * The card's own surface: the panel, carrying a little of the medium's colour.
 *
 * Lives here beside the artwork because the two are one decision. [legibilityVeil] fades into this
 * exact value, so a card drawn on plain `appPanel` would show the gradient as a visible seam down the
 * middle of itself.
 */
@Composable
fun cardGround(mediaType: MediaType): Color =
    lerp(OmnilogTheme.colors.appPanel, mediaType.typeAccent(), GroundTint)

private const val GroundTint = 0.17f

/**
 * An unreached progress cell on a card that has artwork behind it — opaque, unlike the translucent
 * accent every other surface uses.
 *
 * A see-through track picks up whatever the drawing is doing underneath, so the run of empty cells
 * came out mottled and, over the denser passages, invisible: the graphic stopped reading as a
 * count. Sitting the track on the card's own ground restores a flat, even bed for the filled cells
 * to be measured against, and the artwork simply passes behind it.
 */
@Composable
fun cardTrack(mediaType: MediaType): Color =
    lerp(cardGround(mediaType), OmnilogTheme.colors.appInk, TrackLift)

private const val TrackLift = 0.16f

/**
 * What keeps the text readable, and the only reason the artwork can run this strong.
 *
 * A flat scrim would have cost the drawing everywhere to protect the third of the card that needed
 * it. This holds the ground opaque across the leading edge — where the chip, the figure and the dates
 * begin — then opens up across the middle and lets the drawing through nearly undimmed at the
 * trailing edge, which is where the card has the least text.
 *
 * It never reaches fully transparent: the trailing edge still carries the percentage and the recency
 * label, and a quarter of the panel is what those need to stay legible over ink.
 */
private fun legibilityVeil(ground: Color, panel: Color): Brush = Brush.horizontalGradient(
    0.00f to ground,
    0.26f to ground,
    0.56f to ground.copy(alpha = 0.58f),
    1.00f to panel.copy(alpha = 0.26f),
)

/** How tall the drawing is drawn, as a multiple of the card's own height. */
private const val ArtHeightFraction = 1.32f

/** How far the drawing hangs past the card's top edge, as a fraction of the card's height. */
private const val ArtOverhangTop = 0.16f

/** How far it hangs past the trailing edge, as a fraction of the card's width. */
private const val ArtOverhangEnd = 0.07f

/**
 * How much each medium's artwork stands out — the one lever that actually controls that, and the
 * only numbers here worth touching.
 *
 * One entry per medium rather than a rule, because the drawings are not interchangeable: what the
 * value has to answer is how much ink this particular one puts on the card. The sakura and the games
 * spread are filled work covering about a fifth of the frame; books, series and film are open line
 * art covering an eighth, so they need roughly a fifth more to carry the same weight.
 *
 * Light runs lower throughout. Its accents are darker colours, so the same value lands at close to
 * twice the contrast against paper — the asymmetry the two accent sets exist for.
 */
private val MediaType.artAlpha: ArtAlpha
    get() = when (this) {
        MediaType.Anime -> ArtAlpha(dark = 0.60f, light = 0.40f)
        MediaType.Book -> ArtAlpha(dark = 0.72f, light = 0.48f)
        MediaType.Movie -> ArtAlpha(dark = 0.72f, light = 0.48f)
        MediaType.TvShow -> ArtAlpha(dark = 0.72f, light = 0.48f)
        MediaType.Game -> ArtAlpha(dark = 0.60f, light = 0.40f)
    }

/** A medium's artwork strength in each theme. 0 is invisible, 1 is the accent at full force. */
private data class ArtAlpha(val dark: Float, val light: Float)

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
