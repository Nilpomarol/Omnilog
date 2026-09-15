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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.formatExternalRatingCompact
import com.nilpo.contenttracker.ui.common.localizedSteamScoreDescriptor
import com.nilpo.contenttracker.ui.common.logoRes
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * How far the title block reaches back up into the app bar's own bottom padding.
 *
 * The bar is a 64dp box around a 24dp glyph, so a seam measured from the box arrives on screen with
 * some 20dp of the bar's padding already in it. This claws part of that back, stopping well short of
 * the glyphs themselves.
 */
private val BarPaddingReclaim = 8.dp

/**
 * The header's rhythm: title block to genres, and genres to whatever follows the header.
 *
 * One value rather than two, so the session card sits as far below the genres as the genres sit
 * below the cover.
 */
private val HeaderSeam = 14.dp

/** Height of the provider mark above the score. */
private val LogoHeight = 22.dp

private val CoverWidth = 136.dp
private val CoverHeight = 204.dp
private val CoverShape = RoundedCornerShape(8.dp)

/**
 * How much of the page's own background lies over the artwork at the top of the header.
 *
 * The wash is the page colour in both themes — ivory on light, charcoal on dark — so the title can use
 * ordinary theme ink rather than forcing pale text onto a dark scrim. The artwork still tints the
 * header, and the wash reaches the full background at the foot so the page carries on seamlessly.
 */
private const val WashTop = 0.78f

/**
 * The source is decoded at this width and stretched to fill the header.
 *
 * This is the blur. `Modifier.blur` is API 31+ and `minSdk` is 26, so resampling carries the effect
 * on the devices that lack it; [BlurRadius] only takes the edge off the interpolation where it can.
 */
private const val BackdropSampleWidth = 56

private val BlurRadius = 18.dp

/**
 * The item's identity: its cover and title over a soft wash of its own artwork.
 *
 * An item with no cover gets no wash — just the page's own background.
 *
 * [topInset] is the space the app bar and status bar occupy; the artwork fills it. [overlap] is how
 * far the session card will ride up into the foot, which the header reserves so the card lands below
 * the header rather than on it.
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

    Box(modifier = modifier.fillMaxWidth()) {
        if (!metadata.coverUrl.isNullOrBlank()) {
            // matchParentSize, not fillMaxSize: these take their size from the Box rather than giving
            // it one, so the column below decides how tall the header is.
            BackdropArt(
                coverUrl = metadata.coverUrl,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = WashTop),
                            1f to background,
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(topInset - BarPaddingReclaim))
            TitleBlock(
                metadata = metadata,
                onCollectionClick = onCollectionClick,
                onCreatorClick = onCreatorClick,
                modifier = Modifier.padding(horizontal = DetailGutter),
            )
            if (metadata.genres.isNotEmpty()) {
                // One quiet line rather than a row of pills: genres describe the work, they are not
                // controls.
                Text(
                    text = metadata.genres.joinToString(" · "),
                    modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = HeaderSeam),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Whatever the session card rides up by, plus enough that it lands below the genres.
            Spacer(modifier = Modifier.height(overlap + HeaderSeam))
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
    } ?: return

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
 * Cover on the left, everything else on the right, and the right column exactly as tall as the cover.
 *
 * The identity — collection, title, original title, creator — is packed at the top and the figures
 * sit on the cover's baseline, so both columns line up at both ends whatever the title's length.
 */
@Composable
private fun TitleBlock(
    metadata: MediaMetadataUi,
    onCollectionClick: (() -> Unit)?,
    onCreatorClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MetadataCoverImage(
            coverUrl = metadata.coverUrl,
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = CoverShape)
                .size(width = CoverWidth, height = CoverHeight),
            shape = CoverShape,
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
                    // Accent text, the way the library rows name a collection.
                    Text(
                        text = collectionName,
                        modifier = if (onCollectionClick != null) {
                            Modifier.clickable(onClick = onCollectionClick)
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = metadata.mediaType.themeAccent(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = displayMediaTitle(metadata.title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                metadata.originalTitle?.let { originalTitle ->
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HeaderFigures(metadata = metadata)
        }
    }
}

/**
 * Score, audience, length — the figures that sit on the header's baseline.
 *
 * Only what the item actually has is drawn. Games skip length because their total is tracked in
 * hours played, which is a fact about the session rather than about the work.
 */
@Composable
private fun HeaderFigures(metadata: MediaMetadataUi) {
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
    // Bare number against a unit label: the long form truncated in the narrow column.
    val length = metadata.progressTotal
        ?.takeUnless { metadata.mediaType == MediaType.Game }
        ?.toString()

    val minor = listOfNotNull(
        audience?.let { stringResource(R.string.metadata_users) to it },
        length?.let { stringResource(metadata.totalUnitLabelRes()) to it },
    )
    if (rating == null && minor.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (rating != null) {
            RatingFigure(
                value = rating,
                source = metadata.externalRatingSource,
                fallbackLabel = metadata.externalRatingSourceName
                    ?: metadata.sourceName
                    ?: stringResource(R.string.field_rating),
                verdict = localizedSteamScoreDescriptor(
                    mediaType = metadata.mediaType,
                    source = metadata.externalRatingSource,
                    descriptor = metadata.externalRatingScoreDescriptor,
                ),
                ink = ratingTint(ratingFraction) ?: OmnilogTheme.colors.appInk,
            )
        }
        minor.forEach { (label, value) ->
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The external score, the one figure in the header that is a judgement rather than a measurement.
 *
 * It carries the provider mark instead of the provider name. [verdict] is Steam's own words for its
 * percentage — the one source whose figure is a share of reviews rather than a mark out of ten.
 */
@Composable
private fun RatingFigure(
    value: String,
    source: ExternalRatingSource?,
    fallbackLabel: String,
    verdict: String?,
    ink: Color,
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
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        verdict?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The score's own colour, borrowed from the status accents: green a thing gone well, amber a thing
 * left hanging, red a thing abandoned. The bands are wide on purpose — most scores for things a person
 * chose to track land high, so amber and red carry real information when they appear.
 */
@Composable
private fun ratingTint(fraction: Double?): Color? {
    if (fraction == null) return null
    return when {
        fraction >= GoodScore -> OmnilogTheme.accents.Completed
        fraction >= FairScore -> OmnilogTheme.accents.Paused
        else -> OmnilogTheme.accents.Dropped
    }
}

private const val GoodScore = 0.75
private const val FairScore = 0.55

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
