package com.nilpo.contenttracker.ui.detail

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nilpo.contenttracker.ContentTrackerApplication
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.MediaMetadataUi
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.formatExternalRatingCompact
import com.nilpo.contenttracker.ui.common.logoRes
import com.nilpo.contenttracker.ui.theme.DarkAccents
import com.nilpo.contenttracker.ui.theme.DarkPalette
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.OnCoverInk
import com.nilpo.contenttracker.ui.theme.OnCoverMuted

/**
 * How far the title block reaches back up into the app bar's own bottom padding.
 *
 * The bar is a 64dp box around a 24dp glyph, so a seam measured from the box arrives on screen with
 * some 20dp of the bar's padding already in it. Measuring from the box is what made the gap look
 * twice what it was asked for; this claws part of that padding back, so what is left below the
 * arrow is a real gap rather than an accounting one. It stops well short of the glyphs themselves —
 * the back arrow and the kebab keep the row to themselves.
 */
private val BarPaddingReclaim = 8.dp

/**
 * Clearance below the header's last row, on top of whatever the session card overlaps by.
 *
 * This strip is also exactly where the artwork fades into the page, so it is doing two jobs: it
 * keeps the session card off the header, and it gives the fade somewhere to happen that is not on
 * top of any text.
 */
private val CardClearance = 28.dp

/** Seam between the title block and the genres, which belong to it. */
private val GenreGap = 14.dp

/** Ink on the collection pill. Fixed, because the pill is an accent fill in both themes. */
private val PillInk = Color(0xFF15120F)

/** Height of the provider mark above the score. */
private val LogoHeight = 22.dp

/** The sharp cover that sits on the blurred one. */
private val CoverWidth = 160.dp
private val CoverHeight = 240.dp

/**
 * The source is decoded at this width and stretched to fill the screen.
 *
 * This is the blur. `Modifier.blur` is API 31+ and `minSdk` is 26, so resampling — not the
 * modifier — has to carry the effect on the devices that lack it. Decoding at 56px and letting
 * [ContentScale.Crop] scale it up gives a smooth wash everywhere, costs a fraction of the memory a
 * full-size backdrop would, and leaves [BlurRadius] with nothing to do but take the edge off the
 * interpolation on the devices that can.
 */
private const val BackdropSampleWidth = 56

private val BlurRadius = 18.dp

/**
 * The item's own artwork, as the page's ground rather than as a picture on it.
 *
 * The cover is drawn twice: once sampled down and stretched across the whole header, once sharp at
 * [CoverWidth] with a shadow, because the near copy and the far copy are the same picture and
 * nothing else would separate them.
 *
 * The title sits *on* the artwork rather than below it, which is what the scrim is for: it stays
 * dark in both themes, because it exists to darken artwork and a pale one would not, and the text
 * on it is [OnCoverInk] for the same reason. Only the foot fades to `appBackground`, and that fade
 * is anchored to the bottom edge so it cannot ride up into the text when a long title wraps.
 *
 * An item with no cover gets no scrim and no fade — just the page's own background and ordinary
 * ink. A black band over nothing would be worse than no backdrop at all.
 *
 * [topInset] is the space the app bar and status bar occupy; the artwork fills it. [overlap] is how
 * far the session card will ride up into the foot, which the header reserves so the card lands
 * below the header rather than on it.
 */
@Composable
fun DetailBackdropHeader(
    metadata: MediaMetadataUi,
    topInset: Dp,
    modifier: Modifier = Modifier,
    overlap: Dp = 0.dp,
    onCollectionClick: (() -> Unit)? = null,
    onCreatorClick: ((String) -> Unit)? = null,
) {
    val background = OmnilogTheme.colors.appBackground
    val hasArt = !metadata.coverUrl.isNullOrBlank()

    Box(modifier = modifier.fillMaxWidth()) {
        if (hasArt) {
            // matchParentSize, not fillMaxSize: these take their size from the Box rather than
            // giving it one, so the column below is what decides how tall the header is.
            // fillMaxSize would also be unbounded here — a LazyColumn item is measured with
            // infinite max height.
            BackdropArt(
                coverUrl = metadata.coverUrl,
                modifier = Modifier.matchParentSize(),
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Black.copy(alpha = 0.44f),
                            0.34f to Color.Black.copy(alpha = 0.28f),
                            1.00f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Exactly the strip reserved below the title block, never a pixel more. Sized
                    // from the header's foot rather than as a fraction of it, because the header's
                    // height moves with the title's line count. An earlier pass ran this 138dp up
                    // from the bottom, which laid background over the figures row and washed it
                    // out — invisible on dark, obvious on light.
                    .height(overlap + CardClearance)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.55f to background.copy(alpha = 0.55f),
                            1.00f to background,
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(topInset - BarPaddingReclaim))
            TitleBlock(
                metadata = metadata,
                onArtwork = hasArt,
                onCollectionClick = onCollectionClick,
                onCreatorClick = onCreatorClick,
                modifier = Modifier.padding(horizontal = DetailGutter),
            )
            if (metadata.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(GenreGap))
                DetailGenreRow(
                    genres = metadata.genres,
                    accent = if (hasArt) {
                        metadata.mediaType.headerAccent()
                    } else {
                        metadata.mediaType.themeAccent()
                    },
                    // On artwork the pills are on the same dark scrim in both themes, so they take
                    // the dark theme's panel as their base whatever the theme — the reasoning
                    // [headerAccent] gives. Off artwork they are on the page and take the page's.
                    panel = if (hasArt) DarkPalette.appPanel else OmnilogTheme.colors.appPanel,
                    modifier = Modifier.padding(horizontal = DetailGutter),
                )
            }
            // Whatever the session card rides up by, plus enough that it lands below the genres
            // rather than on them.
            Spacer(modifier = Modifier.height(overlap + CardClearance))
        }
    }
}

@Composable
private fun BackdropArt(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as? ContentTrackerApplication
    val coverModel = remember(application, coverUrl) {
        application?.coverRepository?.displayModel(coverUrl) ?: coverUrl
    }
    val request = remember(context, coverModel) {
        coverModel?.let { model ->
            ImageRequest.Builder(context)
                .data(model)
                .size(BackdropSampleWidth, BackdropSampleWidth * 3 / 2)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    if (request == null) {
        // No cover. The veil above renders the whole header as plain background, which is the
        // graceful outcome — no placeholder art, no empty frame.
        return
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modifier.blur(BlurRadius)
        } else {
            modifier
        },
        contentScale = ContentScale.Crop,
    )
}

/**
 * Cover on the left, everything else on the right, and the right column is exactly as tall as the
 * cover.
 *
 * The height is fixed rather than wrapped so the two sides always end on the same line. That budget
 * is what caps the title at two lines and the creators at one: an item whose title needs four lines
 * gets an ellipsis, not a column that outgrows the artwork beside it.
 *
 * Within that height the identity — collection, title, original title, creator — is packed at the
 * top, and the figures that describe the work rather than name it sit on the baseline. So the two
 * columns line up at both ends whether the title runs to one line or two.
 */
@Composable
private fun TitleBlock(
    metadata: MediaMetadataUi,
    onArtwork: Boolean,
    onCollectionClick: (() -> Unit)?,
    onCreatorClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // With a cover this text is on a dark scrim in both themes; without one it is on the page.
    val ink = if (onArtwork) OnCoverInk else OmnilogTheme.colors.appInk
    val muted = if (onArtwork) OnCoverMuted else OmnilogTheme.colors.appMuted

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        // Level with the cover, so the collection pill starts on the cover's own top edge.
        verticalAlignment = Alignment.Top,
    ) {
        MetadataCoverImage(
            coverUrl = metadata.coverUrl,
            // The sharp cover and the blurred one are the same picture, so without a shadow the
            // near one has nothing to separate it from the far one.
            modifier = Modifier
                .then(
                    if (onArtwork) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black,
                        )
                    } else {
                        Modifier
                    },
                )
                .size(width = CoverWidth, height = CoverHeight),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .height(CoverHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                formatCollectionDisplayName(
                    metadata.collectionName,
                    metadata.collectionSortOrder,
                )?.let { collectionName ->
                    CollectionPill(
                        text = collectionName,
                        accent = metadata.mediaType.headerAccent(),
                        onClick = onCollectionClick,
                    )
                }
                Text(
                    text = displayMediaTitle(metadata.title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                metadata.originalTitle?.let { originalTitle ->
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                metadata.creators.firstOrNull()?.let { creator ->
                    Text(
                        text = creator,
                        modifier = if (onCreatorClick != null) {
                            Modifier.clickable { onCreatorClick(creator) }
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = ink.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HeaderFigures(
                metadata = metadata,
                onArtwork = onArtwork,
                ink = ink,
                muted = muted,
            )
        }
    }
}

/**
 * The collection, as a pill rather than a line of small caps.
 *
 * It is the one piece of the header that is a link, and on artwork a bare label had nothing to say
 * so. The accent is the media type's, which is also what makes the pill legible as a category at a
 * glance rather than as another line of text.
 */
@Composable
private fun CollectionPill(
    text: String,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(999.dp),
        // Solid, not a wash. At 22% over artwork the fill picked up whatever was behind it and the
        // label went with it; a pill has to be its own surface to read as one.
        color = accent,
        contentColor = PillInk,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Score, audience, length — the three figures that sit on the header's baseline.
 *
 * Only what the item actually has is drawn, so a book with no vote count shows two columns rather
 * than a dash in the third. Games skip length because their total is tracked in hours played, which
 * is a fact about the session rather than about the work.
 */
@Composable
private fun HeaderFigures(
    metadata: MediaMetadataUi,
    onArtwork: Boolean,
    ink: Color,
    muted: Color,
) {
    val rating = metadata.externalRatingScore?.let { score ->
        metadata.externalRatingMax?.let { maxScore ->
            formatExternalRatingCompact(
                score = score,
                maxScore = maxScore,
                mediaType = metadata.mediaType,
                source = metadata.externalRatingSource,
            )
        }
    }
    // Every source arrives on its own scale, so the tint is decided on the fraction rather than on
    // the printed figure: 8,1 out of 10 and 4,3 out of 5 are the same verdict.
    val ratingFraction = metadata.externalRatingScore?.let { score ->
        metadata.externalRatingMax?.takeIf { it > 0.0 }?.let { maxScore -> score / maxScore }
    }
    val audience = (metadata.externalRatingVoteCount?.toDouble() ?: metadata.popularityScore)
        ?.let(::formatCompactCount)
    // Bare number against a unit label, not "752 pàgines" as one value: three equal columns cannot
    // hold the long form, and it truncated to "752 pàgi…" on a real title.
    val length = metadata.progressTotal
        ?.takeUnless { metadata.mediaType == MediaType.Game }
        ?.toString()

    // Length is deliberately not here. It is the most duplicated figure on the page — the session
    // card underneath already says "752 de 752 pàgines" — and dropping it is what buys the score
    // the width to be the size it now is.
    val minor = listOfNotNull(
        audience?.let { stringResource(R.string.metadata_users) to it },
    )
    if (rating == null && minor.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (rating != null) {
            RatingFigure(
                value = rating,
                source = metadata.externalRatingSource,
                fallbackLabel = metadata.externalRatingSourceName
                    ?: metadata.sourceName
                    ?: stringResource(R.string.field_rating),
                ink = ratingTint(ratingFraction, onArtwork) ?: ink,
                muted = muted,
            )
        }
        minor.forEach { (label, value) ->
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The external score, given the weight of the header baseline.
 *
 * It carries the provider mark instead of the provider name, which is both smaller and quicker to
 * recognise, and the figure runs a size larger than the counts beside it: this is the one number in
 * the header that is a judgement rather than a measurement.
 *
 * The denominator is gone. Every source is normalised onto ten before it reaches here, so "/10"
 * said the same thing on every item. Steam percent sign survives inside the formatter, where it
 * still distinguishes 87% approval from 8,7 out of 10.
 */
@Composable
private fun RatingFigure(
    value: String,
    source: ExternalRatingSource?,
    fallbackLabel: String,
    ink: Color,
    muted: Color,
) {
    Column {
        if (source?.logoRes() != null) {
            ProviderLogo(source = source, height = LogoHeight)
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = fallbackLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The score's own colour, or null to leave it in plain ink.
 *
 * Borrowed from the status accents rather than invented, because the app has already taught these
 * three: green is a thing gone well, amber a thing left hanging, red a thing abandoned. A score
 * reads on the same scale without a legend.
 *
 * The bands are deliberately wide and the top one deliberately generous — most external scores for
 * things a person chose to track land above 7,5, so amber and red carry real information when they
 * do appear. [GoodScore] and [FairScore] are fractions of the source's own maximum, so a 4,3 out of
 * 5 and an 8,6 out of 10 get the same colour.
 *
 * On artwork the dark-tuned accents are used whatever the theme, for the reason [headerAccent]
 * gives: the scrim under this text is dark in both. Off artwork the text is on the page, so the
 * theme's own set applies — the light values are the ones that clear 4.5:1 on paper.
 */
@Composable
private fun ratingTint(fraction: Double?, onArtwork: Boolean): Color? {
    if (fraction == null) return null
    val accents = if (onArtwork) DarkAccents else OmnilogTheme.accents
    return when {
        fraction >= GoodScore -> accents.Completed
        fraction >= FairScore -> accents.Paused
        else -> accents.Dropped
    }
}

private const val GoodScore = 0.75
private const val FairScore = 0.55

/**
 * The media type's accent, taken from the theme-invariant set on purpose.
 *
 * The header's scrim is dark in both themes, so the pill drawn on it needs the values tuned for
 * dark — `OmnilogTheme.accents` would hand back the light-tuned ones on a paper theme and darken a
 * pill that is still sitting on artwork.
 */
private fun MediaType.headerAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogColors.Anime
    MediaType.Book -> OmnilogColors.Books
    MediaType.Movie -> OmnilogColors.Movie
    MediaType.TvShow -> OmnilogColors.Series
    MediaType.Game -> OmnilogColors.Games
}

/** The same accent resolved for the active theme, for the parts of the header not on artwork. */
@Composable
private fun MediaType.themeAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
}

@StringRes
private fun MediaMetadataUi.totalUnitLabelRes(): Int {
    return when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
        -> R.string.metadata_total_episodes

        MediaType.Book -> R.string.metadata_total_pages
        MediaType.Movie -> R.string.metadata_total_minutes
        MediaType.Game -> R.string.metadata_total_hours
    }
}
