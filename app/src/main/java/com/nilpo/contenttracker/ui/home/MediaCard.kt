package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.PartialStar
import com.nilpo.contenttracker.ui.common.compactProgressUnitLabel
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import com.nilpo.contenttracker.ui.common.progressLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlin.math.roundToInt

internal const val CoverAspectRatio = 2f / 3f
// Every row is pinned to the cover's height so the list reads as an even stack of posters.
internal val BaseRowHeight = 132.dp
// Matches the 10dp every list leaves between rows.
internal val RowDividerGap = 10.dp
private const val StarCount = 5

/**
 * Cover-led list row: the title, its collection, who made it and its year, length and genre, over one line saying where you
 * are with it (progress while under way, your stars otherwise) and a status chip.
 */
@Composable
fun MediaCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    /** Off inside a collection's own page, where the name would only repeat the header. */
    showCollection: Boolean = true,
    /** Off on a creator's own page, where every row would repeat the header's name. */
    showCreator: Boolean = true,
) {
    val item = trackedMedia.item
    val session = trackedMedia.currentSession
    val creator = trackedMedia.creatorNames().firstOrNull()?.takeIf { showCreator }
    val collection = formatCollectionDisplayName(trackedMedia.collection?.name, item.collectionSortOrder)
        .takeIf { showCollection }
    val fraction = trackedMedia.coverProgressFraction()
    val rating = session?.ratingHalfPoints
    val facts = listOfNotNull(
        item.releaseYear?.toString(),
        item.progressTotal?.takeIf { it > 0 }?.let { "$it ${compactProgressUnitLabel(item.type)}" },
        item.genres.firstOrNull(),
    ).joinToString(" · ")
    // Text is measured in sp and the row in dp, so the pin tracks the system font setting.
    val rowHeight = BaseRowHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val dividerColor = OmnilogTheme.colors.appLine
    val coverShape = RoundedCornerShape(6.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The divider hangs under each row, RowDividerGap below the cover, so it sits centred
            // in the lists' matching gap between rows.
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), stroke)
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(bottom = RowDividerGap)
            .height(rowHeight),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MetadataCoverImage(
            coverUrl = item.coverUrl,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(CoverAspectRatio)
                .shadow(elevation = 4.dp, shape = coverShape),
            shape = coverShape,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = displayMediaTitle(item.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (collection != null) {
                        Text(
                            text = collection,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (creator != null) {
                        Text(
                            text = creator,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (facts.isNotEmpty()) {
                        Text(
                            text = facts,
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailingAction?.invoke()
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        session == null -> Unit
                        fraction != null -> CardProgress(fraction = fraction, color = session.status.stateColor)
                        rating != null -> CardStars(halfPoints = rating, accent = accent)
                        // Games under way have no total to bar against, so their count stands alone.
                        session.status == TrackingStatus.InProgress && session.progressCurrent > 0 -> Text(
                            text = session.progressLabel(null, item.type),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
                if (session != null) {
                    CardStatusChip(status = session.status)
                }
            }
        }
    }
}

/**
 * Cover-first tile for scanning a large library, titled underneath since many posters don't show
 * their name. The status badge sits in the cover's corner and titles under way get a progress strip
 * along its bottom edge.
 */
@Composable
fun MediaGridCard(
    trackedMedia: TrackedMedia,
    onClick: () -> Unit,
) {
    val item = trackedMedia.item
    val session = trackedMedia.currentSession
    val fraction = trackedMedia.coverProgressFraction()
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CoverAspectRatio)
                .clip(shape),
        ) {
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
            )
            if (session != null) {
                CardStateIconBadge(
                    status = session.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp),
                )
            }
            if (session != null && fraction != null) {
                CoverProgressStrip(fraction = fraction, color = session.status.stateColor)
            }
        }
        Text(
            text = displayMediaTitle(item.title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appInk,
            // Two lines reserved even for a short title, so every tile — and every carousel of
            // them — is the same height.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BoxScope.CoverProgressStrip(fraction: Float, color: Color) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(color),
        )
    }
}

@Composable
internal fun CardProgress(fraction: Float, color: Color) {
    val pill = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(5.dp)
                .background(OmnilogTheme.colors.appLine, pill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color, pill),
            )
        }
        Text(
            text = "${(fraction * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
    }
}

/** Your rating as five stars over the ten-point scale, so each star holds four half points. */
@Composable
internal fun CardStars(halfPoints: Int, accent: Color) {
    val figure = formatRatingHalfPoints(halfPoints)
    val description = stringResource(R.string.library_row_personal_rating, figure)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(StarCount) { index ->
            PartialStar(
                fill = ((halfPoints - index * 4) / 4f).coerceIn(0f, 1f),
                starSize = 22.dp,
                accent = accent,
            )
        }
        Text(
            text = figure,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

/** The status on a chip tinted with its colour, text in ink so it reads on either theme. */
@Composable
private fun CardStatusChip(status: TrackingStatus) {
    val color = status.stateColor
    Text(
        text = status.label(),
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogTheme.colors.appInk,
        maxLines = 1,
    )
}

@Composable
private fun CardStateIconBadge(
    status: TrackingStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.stateColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color,
        contentColor = Color.Black,
    ) {
        Icon(
            painter = painterResource(status.iconResId),
            contentDescription = status.label(),
            modifier = Modifier
                .padding(4.dp)
                .size(13.dp),
        )
    }
}

private val TrackingStatus.stateColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogTheme.accents.Planned
        TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
        TrackingStatus.Completed -> OmnilogTheme.accents.Completed
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
    }

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

@Composable
private fun TrackingStatus.label(): String = when (this) {
    TrackingStatus.Planned -> stringResource(R.string.status_planned)
    TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
    TrackingStatus.Completed -> stringResource(R.string.status_completed)
    TrackingStatus.Paused -> stringResource(R.string.status_paused)
    TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
}

/** How far along a title under way is; null when there's no total to measure against. */
private fun TrackedMedia.coverProgressFraction(): Float? {
    val session = currentSession?.takeIf { it.status == TrackingStatus.InProgress } ?: return null
    val total = item.progressTotal?.takeIf { it > 0 && item.type != MediaType.Game } ?: return null
    return (session.progressCurrent.toFloat() / total).coerceIn(0f, 1f)
}
