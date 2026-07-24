package com.nilpo.contenttracker.ui.detail

import android.os.Build
import androidx.annotation.PluralsRes
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nilpo.contenttracker.ContentTrackerApplication
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.MediaMetadataUi
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Clear artwork between the app bar and the title block.
 *
 * Deliberately small. The header used to be a fixed 300dp with the title pushed to its bottom edge,
 * which left everything between the bar and the title as dead space. The artwork's real estate is
 * the band behind the transparent bar and status bar — this is only the seam below it.
 */
private val ArtworkBand = 36.dp

/**
 * Clearance below the title block, on top of whatever the session card overlaps by.
 *
 * Without it the card lands on the title rather than below it.
 */
private val CardClearance = 16.dp

/** The sharp cover that sits on the blurred one. */
private val CoverWidth = 104.dp
private val CoverHeight = 156.dp

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

/** How far the app bar's dark scrim reaches past the bar itself. */
private val BarScrimDepth = 92.dp

/**
 * The item's own artwork, as the page's ground rather than as a picture on it.
 *
 * The cover is drawn twice: once sampled down and stretched behind everything, once sharp at
 * [CoverWidth]. Between them sits a veil that starts as a thin dark scrim — enough for the app bar's
 * icons, which is why the bar switches to [com.nilpo.contenttracker.ui.theme.OnCoverInk] over this
 * header — and finishes as opaque `appBackground`. The title block therefore sits on the page's own
 * background colour, not on artwork, so it uses ordinary `appInk` and stays legible in both themes
 * whatever the cover happens to be.
 *
 * [topInset] is the space the app bar and status bar occupy. The artwork fills it; the text starts
 * below it.
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

    // The fade finishes exactly where the title block starts, so the title always has a flat ground
    // to sit on and can keep ordinary `appInk`. Measured in pixels rather than as a fraction of the
    // header because the header's height depends on how tall the title runs, and a fraction would
    // slide the fade around whenever the title wrapped to another line.
    val fadeEndPx = with(LocalDensity.current) { (topInset + ArtworkBand).toPx() }

    Box(modifier = modifier.fillMaxWidth()) {
        // matchParentSize, not fillMaxSize: these take their size from the Box rather than giving
        // it one, so the column below is what decides how tall the header is. fillMaxSize would
        // also be unbounded here, since a LazyColumn item is measured with infinite max height.
        BackdropArt(
            coverUrl = metadata.coverUrl,
            modifier = Modifier.matchParentSize(),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    // Weighted to stay out of the way. An earlier pass had this at 44% background
                    // by mid-band, which washed the artwork to mush and made the band read as
                    // empty space rather than as a backdrop. It now holds the cover almost clean
                    // through the bar and spends the whole fade in the last stretch.
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.52f to background.copy(alpha = 0.08f),
                        1.00f to background,
                        startY = 0f,
                        endY = fadeEndPx,
                    ),
                ),
        )

        // A separate scrim for the app bar only. The bar's icons are pale in both themes, so this
        // one stays dark in both themes — the same reasoning `OnCoverInk` is built on.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topInset + BarScrimDepth)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.38f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(topInset + ArtworkBand))
            TitleBlock(
                metadata = metadata,
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
        verticalAlignment = Alignment.Bottom,
    ) {
        MetadataCoverImage(
            coverUrl = metadata.coverUrl,
            modifier = Modifier.size(width = CoverWidth, height = CoverHeight),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            formatCollectionDisplayName(
                metadata.collectionName,
                metadata.collectionSortOrder,
            )?.let { collectionName ->
                Text(
                    text = collectionName,
                    modifier = if (onCollectionClick != null) {
                        Modifier.clickable(onClick = onCollectionClick)
                    } else {
                        Modifier
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = displayMediaTitle(metadata.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 3,
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
            metadata.creators.take(2).forEach { creator ->
                Text(
                    text = creator,
                    modifier = if (onCreatorClick != null) {
                        Modifier.clickable { onCreatorClick(creator) }
                    } else {
                        Modifier
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = OmnilogTheme.colors.appInk.copy(alpha = 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            metadata.subtitleLine()?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The one-line "year · studio · length" strip under the creators.
 *
 * Deliberately built from whatever is present rather than from a fixed set: an item with no release
 * year and no language should show a shorter line, not a line with gaps in it.
 */
@Composable
private fun MediaMetadataUi.subtitleLine(): String? {
    val parts = buildList {
        releaseYear?.let { add(it.toString()) }
        language
            ?.takeUnless { mediaType == MediaType.Game }
            ?.let { add(languageLabel(it)) }
        progressTotal
            ?.takeUnless { mediaType == MediaType.Game }
            ?.let { total ->
                add(pluralStringResource(totalUnitSummaryRes(), total, total))
            }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@PluralsRes
private fun MediaMetadataUi.totalUnitSummaryRes(): Int {
    return when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
        -> R.plurals.backdrop_total_episodes

        MediaType.Book -> R.plurals.backdrop_total_pages
        MediaType.Movie -> R.plurals.backdrop_total_minutes
        MediaType.Game -> R.plurals.backdrop_total_hours
    }
}
