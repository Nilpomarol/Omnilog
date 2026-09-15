package com.nilpo.contenttracker.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.ContributorImageBox
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlin.math.roundToInt

@Composable
internal fun HomeGroupHeader(
    group: HomeDisplayGroup,
    section: MediaSection,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    onCollectionClick: (MediaCollection) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    when {
        group.type == HomeGroupType.Collection && group.collection != null -> LibraryGroupCard(
            group = group,
            section = section,
            accent = accent,
            isCollapsed = isCollapsed,
            onToggle = onClick,
            onOpen = { onCollectionClick(group.collection) },
            expandLabelResId = R.string.collection_expand,
            collapseLabelResId = R.string.collection_collapse,
        ) {
            CollectionCoverStack(
                coverStack = group.items.collectionCoverStack(),
                itemCount = group.items.size,
                modifier = Modifier.fillMaxSize(),
            )
        }
        group.type == HomeGroupType.Author -> LibraryGroupCard(
            group = group,
            section = section,
            accent = accent,
            isCollapsed = isCollapsed,
            onToggle = onClick,
            onOpen = { if (group.title.isBlank()) onClick() else onAuthorClick(group.title) },
            expandLabelResId = section.creatorExpandLabelResId,
            collapseLabelResId = section.creatorCollapseLabelResId,
        ) {
            CreatorImage(group = group, accent = accent)
        }
        else -> SimpleGroupHeader(
            group = group,
            section = section,
            accent = accent,
            isCollapsed = isCollapsed,
            onClick = onClick,
        )
    }
}

@Composable
internal fun SimpleGroupHeader(
    group: HomeDisplayGroup,
    section: MediaSection,
    accent: Color,
    isCollapsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        contentColor = OmnilogTheme.colors.appInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (group.status != null) {
                Icon(
                    painter = painterResource(group.status.iconResId),
                    contentDescription = group.status.label(),
                    modifier = Modifier.size(18.dp),
                    tint = group.status.stateColor,
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.resolvedTitle(section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.collection_item_count, group.items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
        }
    }
}

/**
 * A collection or a creator in a grouped list, drawn as one more list row rather than a panel: [image]
 * in a cover-sized slot, a serif name and what's in the group, then your average and how far along it
 * is, with the rows' hairline underneath. Expanding folds the image away to a slim heading over the
 * group's items; tapping anywhere else opens the collection or creator.
 */
@Composable
private fun LibraryGroupCard(
    group: HomeDisplayGroup,
    section: MediaSection,
    accent: Color,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    @StringRes expandLabelResId: Int,
    @StringRes collapseLabelResId: Int,
    image: @Composable BoxScope.() -> Unit,
) {
    val summary = group.items.collectionProgressSummary()
    val averageRating = group.items.collectionAverageRating()
    val unitLabel = group.items.firstOrNull()?.item?.type.collectionItemUnitLabel()
    // Zero counts drop out: "0 en curs" on a finished saga is noise.
    val facts = listOfNotNull(
        "${group.items.size} $unitLabel",
        summary.completedCount.takeIf { it > 0 }?.let { stringResource(R.string.group_completed_count, it) },
        summary.inProgressCount.takeIf { it > 0 }?.let { stringResource(R.string.group_in_progress_count, it) },
    ).joinToString(" · ")
    // Pinned to the list rows' height, so a folded group reads as one more row in the stack.
    val rowHeight = BaseRowHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)
    // The cover stack insets its covers by up to two steps, so the slot widens by them to keep each
    // cover 2:3. Every group gets this one width, so the names beside the images line up.
    val slotWidth = (rowHeight - CoverStackStep * 2) * CoverAspectRatio + CoverStackStep * 2
    val dividerColor = OmnilogTheme.colors.appLine
    val transition = updateTransition(
        targetState = isCollapsed,
        label = "group_card_transition",
    )
    val slotAnimatedWidth = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "group_slot_width",
    ) { collapsed -> if (collapsed) slotWidth else 0.dp }
    val slotAnimatedHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "group_slot_height",
    ) { collapsed -> if (collapsed) rowHeight else 0.dp }
    val slotGap = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "group_slot_gap",
    ) { collapsed -> if (collapsed) 14.dp else 0.dp }
    val contentHeight = transition.animateDp(
        transitionSpec = { tween(220) },
        label = "group_content_height",
    ) { collapsed -> if (collapsed) rowHeight else 48.dp }
    val slotAlpha = transition.animateFloat(
        transitionSpec = { tween(140) },
        label = "group_slot_alpha",
    ) { collapsed -> if (collapsed) 1f else 0f }
    val arrowRotation = transition.animateFloat(
        transitionSpec = { tween(180) },
        label = "group_arrow_rotation",
    ) { collapsed -> if (collapsed) 0f else 180f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), stroke)
            }
            // No clip: it would slice the image's shadow flat along the row's left edge.
            .clickable(onClick = onOpen)
            .padding(bottom = RowDividerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(slotAnimatedWidth.value)
                .height(slotAnimatedHeight.value)
                .alpha(slotAlpha.value),
            contentAlignment = Alignment.Center,
            content = image,
        )
        Spacer(modifier = Modifier.width(slotGap.value))
        Column(
            modifier = Modifier
                .weight(1f)
                .height(contentHeight.value)
                .clipToBounds()
                .padding(vertical = if (isCollapsed) 2.dp else 0.dp),
            verticalArrangement = if (isCollapsed) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = if (isCollapsed) Alignment.Top else Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = group.resolvedTitle(section),
                        style = (if (isCollapsed) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
                            .copy(fontFamily = SerifFontFamily, fontWeight = FontWeight.Normal),
                        color = OmnilogTheme.colors.appInk,
                        maxLines = if (isCollapsed) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedVisibility(
                        visible = isCollapsed,
                        enter = fadeIn(tween(150, delayMillis = 50)),
                        exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
                    ) {
                        Text(
                            text = facts,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !isCollapsed,
                    enter = fadeIn(tween(120, delayMillis = 60)),
                    exit = fadeOut(tween(60)),
                ) {
                    Text(
                        text = stringResource(R.string.collection_item_count, group.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                    )
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(if (isCollapsed) 32.dp else 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(if (isCollapsed) expandLabelResId else collapseLabelResId),
                        modifier = Modifier.graphicsLayer { rotationZ = arrowRotation.value },
                        tint = OmnilogTheme.colors.appMuted,
                    )
                }
            }
            AnimatedVisibility(
                visible = isCollapsed,
                enter = fadeIn(tween(150, delayMillis = 60)),
                exit = fadeOut(tween(70)) + shrinkVertically(tween(110), shrinkTowards = Alignment.Top),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    averageRating?.let { rating ->
                        CardStars(halfPoints = (rating * 2).roundToInt(), accent = accent)
                    }
                    CardProgress(fraction = summary.progressFraction, color = accent)
                }
            }
        }
    }
}

/**
 * Who made a group's titles, in the slot a collection's covers take.
 *
 * A portrait is cropped to a cover's shape, so a person sits in the list like one more book. Logos are
 * anything from a square badge to a long wordmark, some transparent and some a solid tile of their own,
 * so rather than following each one's shape they all get the same frame: a white card the slot's width,
 * with the logo fitted inside a margin like a print in a mount. Every studio row then carries the same
 * weight, no logo runs into the corners, and the names beside them line up.
 * With no image at all, the titles' own covers stand in.
 */
@Composable
private fun CreatorImage(group: HomeDisplayGroup, accent: Color) {
    val shape = RoundedCornerShape(6.dp)
    when {
        group.imageUrl == null -> CollectionCoverStack(
            coverStack = group.items.collectionCoverStack(),
            itemCount = group.items.size,
            modifier = Modifier.fillMaxSize(),
        )
        !group.imageIsLogo -> ContributorImageBox(
            imageUrl = group.imageUrl,
            name = group.title,
            isCompany = false,
            accent = accent,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(CoverAspectRatio, matchHeightConstraintsFirst = true)
                .shadow(elevation = 4.dp, shape = shape),
        )
        else -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(LogoMountAspectRatio)
                .shadow(elevation = 4.dp, shape = shape)
                .background(Color.White)
                .padding(LogoMountMargin),
        ) {
            ContributorImageBox(
                imageUrl = group.imageUrl,
                name = group.title,
                isCompany = true,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// Landscape, since most studio marks are wider than tall; a square badge simply sits with more air.
private const val LogoMountAspectRatio = 4f / 3f
private val LogoMountMargin = 10.dp

@Composable
private fun CollectionCoverStack(
    coverStack: List<String>,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val layers = itemCount.coerceIn(1, 3)
    val step = CoverStackStep
    Box(modifier = modifier) {
        // Draw the furthest sheet first; each layer peeks a step further at the
        // top-right corner so a multi-item group reads as a small stack of covers.
        for (layer in (layers - 1) downTo 0) {
            val inset = step * (layers - 1 - layer)
            val push = step * layer
            MetadataCoverImage(
                coverUrl = coverStack.getOrNull(layer),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = inset, end = inset, start = push, bottom = push)
                    .shadow(elevation = 4.dp, shape = shape),
                shape = shape,
            )
        }
    }
}

private val CoverStackStep = 5.dp

@Composable
internal fun GroupProgressBar(
    fraction: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogTheme.colors.appLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

// ── Composable helpers ──────────────────────────────────────────────────────

@Composable
internal fun HomeDisplayGroup.resolvedTitle(section: MediaSection): String {
    return when {
        status != null -> status.label()
        title.isNotBlank() -> title
        type == HomeGroupType.Collection -> stringResource(R.string.collection_none)
        type == HomeGroupType.Author -> stringResource(section.creatorUnknownLabelResId)
        else -> title
    }
}

@Composable
internal fun List<TrackedMedia>.collectionProgressSummary(): CollectionProgressSummary {
    val completed = count { it.currentSession?.status == TrackingStatus.Completed }
    val inProgress = count { it.currentSession?.status == TrackingStatus.InProgress }
    val mediaType = firstOrNull()?.item?.type
    var fraction = if (isNotEmpty()) completed.toFloat() / size.toFloat() else 0f
    val label = if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
        val current = sumOf { it.currentSession?.progressCurrent ?: 0 }
        val total = sumOf { it.item.progressTotal ?: 0 }
        if (total > 0) {
            fraction = current.toFloat() / total.toFloat()
            stringResource(R.string.group_progress_units, current, total, stringResource(R.string.progress_unit_episode_many))
        } else {
            stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
        }
    } else {
        stringResource(R.string.group_progress_units, completed, size, mediaType.itemUnitLabel())
    }
    return CollectionProgressSummary(
        label = label,
        completedCount = completed,
        inProgressCount = inProgress,
        progressFraction = fraction,
    )
}

internal fun List<TrackedMedia>.collectionAverageRating(): Double? {
    val ratings = mapNotNull { it.currentSession?.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
    if (ratings.isEmpty()) return null
    return ratings.average()
}

@Composable
private fun MediaType?.itemUnitLabel(): String = when (this) {
    MediaType.Book -> stringResource(R.string.group_unit_books)
    MediaType.Game -> stringResource(R.string.group_unit_games)
    MediaType.Movie -> stringResource(R.string.group_unit_movies)
    MediaType.TvShow -> stringResource(R.string.group_unit_tv_shows)
    MediaType.Anime -> stringResource(R.string.group_unit_anime)
    null -> stringResource(R.string.group_unit_items)
}

@Composable
private fun MediaType?.collectionItemUnitLabel(): String = when (this) {
    MediaType.Anime, MediaType.TvShow -> stringResource(R.string.group_unit_seasons)
    MediaType.Book -> stringResource(R.string.group_unit_books)
    MediaType.Movie -> stringResource(R.string.group_unit_movies)
    MediaType.Game -> stringResource(R.string.group_unit_games)
    null -> stringResource(R.string.group_unit_items)
}

// ── TrackingStatus extensions (private to this file) ────────────────────────

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
