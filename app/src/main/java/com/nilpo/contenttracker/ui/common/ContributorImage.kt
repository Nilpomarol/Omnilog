package com.nilpo.contenttracker.ui.common

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.BitmapImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.nilpo.contenttracker.ContentTrackerApplication

/**
 * A contributor's visual identity: a cropped portrait for a person, a whole logo for a company.
 *
 * One implementation for the credit list, the home group card and the author header, which used to
 * hold three copies of this with different clamps — the same studio logo came out a different shape
 * on each screen.
 */
@Composable
fun ContributorImage(
    imageUrl: String?,
    name: String,
    isCompany: Boolean,
    accent: Color,
    height: Dp,
    logoAspectRatio: Float? = null,
    logoFrameWidth: Dp? = null,
    modifier: Modifier = Modifier,
) {
    var logoWidthRatio by remember(imageUrl) { mutableFloatStateOf(DefaultLogoWidthRatio) }
    val width by animateDpAsState(
        // The intrinsic ratio only arrives once the image has loaded, so the box would otherwise
        // snap from square to its real width and shove the text beside it sideways.
        targetValue = if (isCompany) {
            logoFrameWidth ?: height * resolvedLogoWidthRatio(logoAspectRatio, logoWidthRatio)
        } else {
            height * PortraitWidthRatio
        },
        animationSpec = tween(180),
        label = "contributor_image_width",
    )
    ContributorImageBox(
        imageUrl = imageUrl,
        name = name,
        isCompany = isCompany,
        accent = accent,
        modifier = modifier.width(width).height(height),
        onLogoWidthRatio = { ratio -> logoWidthRatio = ratio },
    )
}

/**
 * The same image sized entirely by [modifier], for callers that drive the width themselves.
 *
 * [onLogoWidthRatio] reports the loaded logo's proportions so such a caller can size to them.
 */
@Composable
fun ContributorImageBox(
    imageUrl: String?,
    name: String,
    isCompany: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onLogoWidthRatio: (Float) -> Unit = {},
) {
    var isLoaded by remember(imageUrl) { mutableStateOf(false) }
    var hasTransparentPixels by remember(imageUrl) { mutableStateOf(false) }
    // Resolved through the permanent store the same way covers are, so a portrait already
    // downloaded is drawn from disk rather than re-fetched — and still appears offline.
    val context = LocalContext.current
    val application = context.applicationContext as? ContentTrackerApplication
    val imageRequest = remember(context, application, imageUrl) {
        val model = application?.coverRepository?.displayModel(imageUrl) ?: imageUrl
        model?.let {
            ImageRequest.Builder(context)
                .data(it)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                // Company logos are inspected after decoding to distinguish a transparent mark
                // from an opaque logo tile. Software decoding makes that inspection dependable.
                .allowHardware(!isCompany)
                .build()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            // Company logos commonly have transparent artwork. A white card gives their marks a
            // reliable contrast surface, while portraits retain the contextual accent treatment.
            .background(if (isCompany) Color.White else accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        // Hidden once the image is up. A company logo is drawn whole, so it letterboxes against
        // this box and is usually transparent besides — leaving the initial underneath meant a
        // large accent-coloured letter showing through and around every logo.
        if (!isLoaded) {
            Text(
                text = name.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = accent,
            )
        }
        imageRequest?.let { request ->
            AsyncImage(
                model = request,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    // Transparent artwork gets deliberate white breathing room. Opaque logo tiles
                    // stay edge-to-edge, so their supplied colour reaches the edge of the frame.
                    .then(
                        if (isCompany && hasTransparentPixels) {
                            Modifier.padding(TransparentLogoInset)
                        } else {
                            Modifier
                        },
                    ),
                contentScale = if (isCompany) ContentScale.Fit else ContentScale.Crop,
                onSuccess = { result ->
                    isLoaded = true
                    if (isCompany) {
                        val size = result.painter.intrinsicSize
                        val visualMetrics = (result.result.image as? BitmapImage)
                            ?.bitmap
                            ?.logoContentMetrics()
                        hasTransparentPixels = visualMetrics?.hasTransparentPixels == true
                        onLogoWidthRatio(
                            visualMetrics?.widthRatio
                                ?: contributorLogoWidthRatio(size.width, size.height),
                        )
                    }
                },
            )
        }
    }
}

/**
 * How many times its own height a logo box should be, given the loaded image's dimensions.
 *
 * Clamped at both ends so one extreme logo cannot squeeze a row or run off it, and defaulting to
 * square whenever the dimensions are unusable rather than dividing by zero.
 */
internal fun contributorLogoWidthRatio(intrinsicWidth: Float, intrinsicHeight: Float): Float {
    if (!intrinsicWidth.isFinite() || !intrinsicHeight.isFinite()) return DefaultLogoWidthRatio
    if (intrinsicWidth <= 0f || intrinsicHeight <= 0f) return DefaultLogoWidthRatio
    return (intrinsicWidth / intrinsicHeight).coerceIn(MinLogoWidthRatio, MaxLogoWidthRatio)
}

/**
 * Providers can report a logo's original canvas separately from a normalized delivery URL. Prefer
 * that value, but retain the layout guardrails: a provider can report an extremely wide source and
 * a contributor row must always leave room for its name and metadata. The logo itself is still
 * drawn with [ContentScale.Fit], so this bounds the frame rather than distorting the artwork.
 */
internal fun resolvedLogoWidthRatio(providerAspectRatio: Float?, loadedAspectRatio: Float): Float {
    val ratio = providerAspectRatio?.takeIf { it.isFinite() && it > 0f } ?: loadedAspectRatio
    return ratio.coerceIn(MinLogoWidthRatio, MaxLogoWidthRatio)
}

/**
 * Returns the visible content's ratio for an alpha-backed logo, ignoring transparent provider
 * padding. This keeps already-saved IGDB logos correct even though their old records predate the
 * original-dimension field. Images without transparency deliberately fall back to their canvas.
 */
internal fun Bitmap.logoContentWidthRatio(): Float? = logoContentMetrics()?.widthRatio

/** The visible logo bounds and whether its canvas contains transparent pixels. */
internal fun Bitmap.logoContentMetrics(): LogoContentMetrics? {
    // Coil may retain a decoded cover as a GPU-only hardware bitmap. Android intentionally forbids
    // CPU pixel reads from those images, so use the normal canvas ratio instead of crashing while
    // trying to inspect transparent padding.
    if (config == Bitmap.Config.HARDWARE || !hasAlpha() || width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    var hasTransparentPixels = false
    pixels.forEachIndexed { index, pixel ->
        val alpha = (pixel ushr 24) and AlphaMask
        if (alpha <= AlphaThreshold) {
            hasTransparentPixels = true
        } else {
            val x = index % width
            val y = index / width
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
        }
    }
    if (right < left || bottom < top) return null
    return LogoContentMetrics(
        widthRatio = (right - left + 1).toFloat() / (bottom - top + 1),
        hasTransparentPixels = hasTransparentPixels,
    )
}

internal data class LogoContentMetrics(
    val widthRatio: Float,
    val hasTransparentPixels: Boolean,
)

internal const val DefaultLogoWidthRatio = 1f
internal const val MinLogoWidthRatio = 1f
internal const val MaxLogoWidthRatio = 2.5f
private const val AlphaMask = 0xFF
private const val AlphaThreshold = 16
private val TransparentLogoInset = 5.dp

/** A person is drawn in a consistent portrait crop rather than at their image's own proportions. */
private const val PortraitWidthRatio = 0.7f
