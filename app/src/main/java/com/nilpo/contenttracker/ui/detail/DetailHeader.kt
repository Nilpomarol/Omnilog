package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.MediaMetadataUi
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/**
 * How far the header reaches back up into the app bar's own bottom padding.
 *
 * The bar is a 64dp box around a 24dp glyph, so a seam measured from the box arrives on screen with
 * some 20dp of the bar's padding already in it. This claws part of that back, stopping well short of
 * the glyphs themselves.
 */
internal val BarPaddingReclaim = 8.dp

private val CoverWidth = 128.dp
private val CoverHeight = 192.dp
private val CoverShape = RoundedCornerShape(6.dp)

/**
 * The item's identity: the cover on the left and, beside it, the collection, a serif title, the
 * creator, one line of facts and the genres.
 *
 * Scores are deliberately not here. They sit beside the user's own rating further down, so the header
 * describes the work and nothing in it is a verdict.
 *
 * [topInset] is the space the app bar and status bar occupy.
 */
@Composable
fun DetailHeader(
    metadata: MediaMetadataUi,
    topInset: Dp,
    modifier: Modifier = Modifier,
    onCollectionClick: (() -> Unit)? = null,
    onCreatorClick: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DetailGutter,
                end = DetailGutter,
                top = topInset - BarPaddingReclaim,
            ),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MetadataCoverImage(
            coverUrl = metadata.coverUrl,
            modifier = Modifier
                .shadow(elevation = 12.dp, shape = CoverShape)
                .size(width = CoverWidth, height = CoverHeight),
            shape = CoverShape,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            formatCollectionDisplayName(
                metadata.collectionName,
                metadata.collectionSortOrder,
            )?.let { collectionName ->
                // A small letter-spaced overline in the media accent, the way a spine names its series.
                Text(
                    text = collectionName.uppercase(),
                    modifier = if (onCollectionClick != null) {
                        Modifier.clickable(onClick = onCollectionClick)
                    } else {
                        Modifier
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = metadata.mediaType.themeAccent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // The title steps its size down to fit three lines rather than running to five at full
            // size, which pushed a long title's creator and facts far below the cover. Line height is
            // relative so the leading shrinks with it.
            BasicText(
                text = displayMediaTitle(metadata.title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 1.22.em,
                    color = OmnilogTheme.colors.appInk,
                ),
                maxLines = TitleMaxLines,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 18.sp,
                    maxFontSize = 26.sp,
                    stepSize = 1.sp,
                ),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Games skip length: their total is hours played, a fact about the session, not the work.
            val length = metadata.progressTotal
                ?.takeUnless { metadata.mediaType == MediaType.Game }
                ?.let { total -> "$total ${progressUnitLabel(metadata.mediaType, total)}" }
            val facts = listOfNotNull(metadata.releaseYear?.toString(), length)
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (metadata.genres.isNotEmpty()) {
                // One line that scrolls sideways, so a long genre list never grows the header.
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    metadata.genres.forEach { genre ->
                        // Soft paper tabs rather than outlined chips: they describe, they do not toggle.
                        Text(
                            text = genre,
                            modifier = Modifier
                                .background(OmnilogTheme.colors.appPanel, RoundedCornerShape(50))
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private const val TitleMaxLines = 3

@Composable
private fun MediaType.themeAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogTheme.accents.Anime
    MediaType.Book -> OmnilogTheme.accents.Books
    MediaType.Movie -> OmnilogTheme.accents.Movie
    MediaType.TvShow -> OmnilogTheme.accents.Series
    MediaType.Game -> OmnilogTheme.accents.Games
}
