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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.OnCoverInk
import com.nilpo.contenttracker.ui.theme.OnCoverMuted

/**
 * How far the cover rises past the app bar's bottom edge, into the bar's own band.
 *
 * The cover is the one part of the header allowed up there. It buys its extra height from space the
 * bar was already occupying, so the text column beside it keeps the room it needs, and the back
 * arrow lands on the artwork rather than above it.
 */
private val CoverRise = 44.dp

/** Seam between the app bar's bottom edge and the top of the text column. */
private val TextGap = 4.dp

/** Never let the cover reach the status bar, however short the app bar turns out to be. */
private val MinCoverTop = 6.dp

/**
 * Clearance below the title block, on top of whatever the session card overlaps by.
 *
 * This strip is also exactly where the artwork fades into the page, so it is doing two jobs: it
 * keeps the session card off the title, and it gives the fade somewhere to happen that is not on
 * top of any text.
 */
private val CardClearance = 28.dp

/** Ink on the collection pill. Fixed, because the pill is an accent fill in both themes. */
private val PillInk = Color(0xFF15120F)

/** Height of the provider mark above the score. */
private val LogoHeight = 17.dp

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
 * below the title rather than on it.
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

        // The rise is what the cover actually gets, not what it asked for: on a device whose app
        // bar is shorter than CoverRise the cover would otherwise climb into the status bar.
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val coverTop = (topInset - CoverRise).coerceAtLeast(statusBar + MinCoverTop)
        val rise = topInset - coverTop

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(coverTop))
            TitleBlock(
                metadata = metadata,
                onArtwork = hasArt,
                coverRise = rise,
                onCollectionClick = onCollectionClick,
                onCreatorClick = onCreatorClick,
                modifier = Modifier.padding(horizontal = DetailGutter),
            )
            // Whatever the session card rides up by, plus enough that it lands below the title
            // rather than on it.
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
 * columns line up at the bottom whether the title runs to one line or two.
 */
@Composable
private fun TitleBlock(
    metadata: MediaMetadataUi,
    onArtwork: Boolean,
    coverRise: Dp,
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
        // Bottom, so the extra height the cover gains by rising is spent upwards. Both columns still
        // end on the same line.
        verticalAlignment = Alignment.Bottom,
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
                .height(CoverHeight - coverRise - TextGap),
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

            HeaderFigures(metadata = metadata, ink = ink, muted = muted)
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
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
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
                ink = ink,
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
            Spacer(modifier = Modifier.height(2.dp))
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
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
