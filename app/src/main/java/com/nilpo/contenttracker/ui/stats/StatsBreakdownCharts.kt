package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.ComparisonBasis
import com.nilpo.contenttracker.core.stats.CollectionRatingStat
import com.nilpo.contenttracker.core.stats.CreatorStat
import com.nilpo.contenttracker.core.stats.EstimatedTimeStat
import com.nilpo.contenttracker.core.stats.LanguageCoverage
import com.nilpo.contenttracker.core.stats.LanguageStat
import com.nilpo.contenttracker.core.stats.MediumStats
import com.nilpo.contenttracker.core.stats.ProgressTotalStats
import com.nilpo.contenttracker.core.stats.pictogramScale
import com.nilpo.contenttracker.core.stats.RankedStat
import com.nilpo.contenttracker.core.stats.connectedRatingTrendSegments
import com.nilpo.contenttracker.core.stats.estimatedTimeByMedium
import com.nilpo.contenttracker.core.stats.genrePieData
import com.nilpo.contenttracker.core.stats.RatingTrendPoint
import com.nilpo.contenttracker.core.stats.RevisitStats
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsObservation
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSegment
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.core.stats.StatusStatsBucket
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.compactProgressUnitLabel
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt



/**
 * The language split for the media that record one, with the rest named rather than drawn.
 *
 * Untagged titles inside an included medium get no block: a "Desconegut" tile would be back to
 * measuring metadata coverage on an axis that claims to measure language. The caption carries that
 * information instead, as a denominator the eye cannot misread as a category.
 */
@Composable
internal fun LanguageMosaicChart(
    coverage: LanguageCoverage,
    emptyText: String,
) {
    val visibleStats = coverage.stats.filter { stat -> stat.value > 0 }
    val excludedLabel = coverage.excludedMediaTypes
        .map { mediaType -> mediaType.pluralLabel() }
        .joinToString(separator = ", ")
    val topStats = visibleStats.take(2)
    val remainingStats = visibleStats.drop(2)

    StatsPanel {
        if (visibleStats.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (remainingStats.isEmpty()) 116.dp else 150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (remainingStats.isEmpty()) 116.dp else 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        topStats.forEachIndexed { index, stat ->
                            LanguageBlock(
                                label = languageStatLabel(stat),
                                value = stat.value,
                                color = languageChartColor(index),
                                prominent = true,
                                modifier = Modifier.weight(stat.value.toFloat()),
                            )
                        }
                    }
                    if (remainingStats.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            remainingStats.forEachIndexed { index, stat ->
                                LanguageBlock(
                                    label = languageStatLabel(stat),
                                    value = stat.value,
                                    color = languageChartColor(index + topStats.size),
                                    prominent = false,
                                    modifier = Modifier.weight(stat.value.toFloat()),
                                )
                            }
                        }
                    }
                }
                if (excludedLabel.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_languages_excluded, excludedLabel),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }
    }
}

/** Friendly display name from the shared language mapping, never a raw code. */
@Composable
internal fun languageStatLabel(stat: LanguageStat): String {
    val code = stat.code ?: return stringResource(R.string.stats_language_unknown)
    return languageLabel(code).ifBlank { code }
}

@Composable
internal fun LanguageBlock(
    label: String,
    value: Int,
    color: Color,
    prominent: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = if (prominent) Alignment.CenterStart else Alignment.Center,
    ) {
        Column(
            horizontalAlignment = if (prominent) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = if (prominent) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appBackground,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun RatingDistributionHistogram(
    buckets: List<StatsBucket>,
    emptyText: String,
) {
    val maxValue = buckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val legendMediaTypes = buckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }

    StatsPanel {
        if (maxValue == 0) {
            EmptyStatsText(text = emptyText)
        } else {
            // Every position of the fixed 1-10 scale stays visible, including
            // scores nobody has used, so the shape of the scale is honest.
            buckets.forEach { bucket ->
                RatingDistributionRow(
                    bucket = bucket,
                    maxValue = maxValue,
                )
            }
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
        }
    }
}

@Composable
internal fun RatingDistributionRow(
    bucket: StatsBucket,
    maxValue: Int,
) {
    val fillFraction = if (maxValue > 0) bucket.value.toFloat() / maxValue.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bucket.label,
                modifier = Modifier.width(18.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .background(OmnilogTheme.colors.appLine.copy(alpha = 0.36f), RoundedCornerShape(5.dp)),
            ) {
                if (bucket.segments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(18.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    ) {
                        bucket.segments.forEach { segment ->
                            Box(
                                modifier = Modifier
                                    .weight(segment.value.toFloat())
                                    .height(18.dp)
                                    .background(segment.mediaType.statsColor()),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(18.dp)
                            .background(OmnilogTheme.accents.Books, RoundedCornerShape(5.dp)),
                    )
                }
            }
            Text(
                text = bucket.value.compactStatValue(),
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun StatusBreakdown(buckets: List<StatusStatsBucket>) {
    val visibleBuckets = buckets.filter { bucket -> bucket.value > 0 }
    val maxValue = visibleBuckets.maxOfOrNull { bucket -> bucket.value } ?: 0

    StatsPanel {
        if (visibleBuckets.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                visibleBuckets.forEach { bucket ->
                    StatusMiniBar(
                        bucket = bucket,
                        maxValue = maxValue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusMiniBar(
    bucket: StatusStatsBucket,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    val scaledHeight = if (maxValue > 0) {
        (70 * sqrt(bucket.value.toFloat() / maxValue.toFloat())).toInt().coerceAtLeast(8)
    } else {
        0
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = bucket.value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = bucket.status.stateColor,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledHeight.dp)
                    .background(
                        color = bucket.status.stateColor,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    ),
            )
        }
        Text(
            text = bucket.status.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** How many covers a card fans out at most; the count in the caption carries the real total. */
private const val MaxCreatorCovers = 3
private val CreatorCoverWidth = 74.dp
private val CreatorCoverHeight = 106.dp
private val CreatorCoverOffset = 22.dp

/**
 * Creators as a strip of cards, each fanning out covers from the work of theirs you finished.
 *
 * A creator has no cover of their own, so rather than borrowing one title's artwork and implying
 * it represents them, the card shows several. The fan tops out at [MaxCreatorCovers], so it reads
 * as "a body of work" rather than as a count — the caption states the real number.
 */
@Composable
internal fun CreatorCarousel(
    stats: List<CreatorStat>,
    emptyText: String,
    onCreatorClick: (CreatorStat) -> Unit,
) {
    if (stats.isEmpty()) {
        StatsPanel { EmptyStatsText(text = emptyText) }
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stats, key = { stat -> stat.name }) { stat ->
            CreatorCard(stat = stat, onClick = { onCreatorClick(stat) })
        }
    }
}

@Composable
private fun CreatorCard(stat: CreatorStat, onClick: () -> Unit) {
    val accent = stat.mediaType.statsColor()
    val covers = stat.coverUrls.take(MaxCreatorCovers)
    // Keep the fan's footprint fixed whether a creator has one cover or three, so the cards in
    // the strip stay aligned instead of stepping in and out.
    val fanWidth = CreatorCoverWidth + CreatorCoverOffset * (MaxCreatorCovers - 1)
    val caption = if (stat.averageRating != null) {
        stringResource(
            R.string.stats_creator_titles_and_rating,
            stat.completedTitles,
            stat.averageRating.roundedStatValue(),
        )
    } else {
        stringResource(R.string.stats_creator_titles_only, stat.completedTitles)
    }

    Column(
        modifier = Modifier
            .width(fanWidth)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${stat.name}. $caption"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(fanWidth)
                .height(CreatorCoverHeight + 8.dp),
        ) {
            // Drawn back to front so the best-rated cover ends up on top of the fan.
            covers.indices.reversed().forEach { index ->
                MetadataCoverImage(
                    coverUrl = covers[index],
                    modifier = Modifier
                        .padding(start = CreatorCoverOffset * index, top = 4.dp * index)
                        .width(CreatorCoverWidth)
                        .height(CreatorCoverHeight),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            if (covers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .width(CreatorCoverWidth)
                        .height(CreatorCoverHeight)
                        .background(accent.copy(alpha = 0.20f), RoundedCornerShape(6.dp)),
                )
            }
        }
        Text(
            text = stat.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Collections as a strip of cards, ranked by the average rating of the titles inside them.
 *
 * Shares the creator card's fanned-cover shell, but inverts its emphasis: the name and the score
 * both lead, since the name is what identifies a collection while the score is what earned it the
 * position, and the count of rated titles drops to the supporting line. That inversion is what
 * keeps the two carousels distinguishable despite the shared shape.
 */
@Composable
internal fun CollectionRatingCarousel(
    stats: List<CollectionRatingStat>,
    emptyText: String,
    onCollectionClick: (CollectionRatingStat) -> Unit,
) {
    if (stats.isEmpty()) {
        StatsPanel { EmptyStatsText(text = emptyText) }
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stats, key = { stat -> stat.id }) { stat ->
            CollectionRatingCard(stat = stat, onClick = { onCollectionClick(stat) })
        }
    }
}

@Composable
private fun CollectionRatingCard(stat: CollectionRatingStat, onClick: () -> Unit) {
    val accent = stat.mediaType.statsColor()
    val covers = stat.coverUrls.take(MaxCreatorCovers)
    val fanWidth = CreatorCoverWidth + CreatorCoverOffset * (MaxCreatorCovers - 1)
    val rating = stat.averageRating.roundedStatValue()
    val caption = stringResource(R.string.stats_collection_rated_titles, stat.ratedTitles)

    Column(
        modifier = Modifier
            .width(fanWidth)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${stat.name}. ${stat.averageRating.roundedStatValue()}. $caption"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(fanWidth)
                .height(CreatorCoverHeight + 8.dp),
        ) {
            // Drawn back to front so the best-rated cover ends up on top of the fan.
            covers.indices.reversed().forEach { index ->
                MetadataCoverImage(
                    coverUrl = covers[index],
                    modifier = Modifier
                        .padding(start = CreatorCoverOffset * index, top = 4.dp * index)
                        .width(CreatorCoverWidth)
                        .height(CreatorCoverHeight),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            if (covers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .width(CreatorCoverWidth)
                        .height(CreatorCoverHeight)
                        .background(accent.copy(alpha = 0.20f), RoundedCornerShape(6.dp)),
                )
            }
        }
        Text(
            text = stat.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = rating,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            maxLines = 1,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * How much of what you finished was something you had already finished before.
 *
 * The bar is the share of completed titles you came back to, split by medium, so its parts sum to
 * the figure in the headline. The old form drew a bar per medium from a 22dp floor, which made one
 * repeat look a third as tall as seven rather than a seventh.
 */
@Composable
internal fun RevisitShareChart(
    revisitedTitles: Int,
    completedTitles: Int,
    breakdown: List<RevisitStats>,
    revisitSessions: Int,
) {
    val visible = breakdown.filter { stat -> stat.value > 0 }

    StatsPanel {
        if (revisitedTitles <= 0 || completedTitles <= 0) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = revisitedTitles.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.accents.Books,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        R.string.stats_revisit_share_caption,
                        completedTitles,
                        revisitSessions,
                    ),
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(OmnilogTheme.colors.appLine.copy(alpha = 0.40f), RoundedCornerShape(5.dp)),
            ) {
                val share = (revisitedTitles.toFloat() / completedTitles.toFloat()).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(share)
                        .height(22.dp)
                        .clip(RoundedCornerShape(5.dp)),
                ) {
                    visible.forEach { stat ->
                        Box(
                            modifier = Modifier
                                .weight(stat.value.toFloat())
                                .fillMaxHeight()
                                .background(stat.mediaType.statsColor()),
                        )
                    }
                }
            }
            if (visible.isNotEmpty()) {
                RevisitLegendRow(stats = visible)
            }
        }
    }
}

/**
 * The bar's key, on one line whatever the mix.
 *
 * Each entry takes an equal share of the row and leads with its count, so the number always
 * survives; only the longest medium names give ground at large font scales.
 */
@Composable
private fun RevisitLegendRow(stats: List<RevisitStats>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEach { stat ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(stat.mediaType.statsColor(), RoundedCornerShape(2.dp)),
                )
                Text(
                    text = stat.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = stat.mediaType.statsColor(),
                    maxLines = 1,
                )
                Text(
                    text = stat.mediaType.label(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Consumption totals as labelled values in each medium's own unit. No shared
 * visual scale across media types, because the units are not comparable.
 */
@Composable
internal fun ProgressTotals(totals: List<ProgressTotalStats>) {
    val visibleTotals = totals.filter { total -> total.value > 0 }

    StatsPanel {
        if (visibleTotals.isEmpty()) {
            EmptyStatsText(text = stringResource(R.string.stats_empty_activity))
        } else {
            visibleTotals.forEach { total ->
                ConsumptionPictogramRow(total = total)
            }
        }
    }
}

/**
 * One medium's total as a run of blocks, in that medium's own unit.
 *
 * The block size is chosen per row and named on the row, because a block means a
 * different thing in each: reading block counts across rows would compare pages
 * with episodes. Only the count within a row carries meaning.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsumptionPictogramRow(total: ProgressTotalStats) {
    val scale = pictogramScale(total.value)
    val accent = total.mediaType.statsColor()
    val unit = total.mediaType.progressUnit()
    val valueText = "${total.value.compactStatValue()} $unit"
    val rowDescription = stringResource(
        R.string.stats_consumption_row_description,
        total.mediaType.label(),
        total.value.compactStatValue(),
        unit,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = rowDescription },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, RoundedCornerShape(2.dp)),
            )
            Text(
                text = total.mediaType.label(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(scale.fullBlocks) {
                PictogramBlock(accent = accent, partial = false)
            }
            if (scale.hasPartialBlock) {
                PictogramBlock(accent = accent, partial = true)
            }
        }
        Text(
            text = stringResource(
                R.string.stats_pictogram_block_legend,
                scale.blockUnit.compactStatValue(),
                unit,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A partial block is dimmed, so a total is never rounded up into a block it has not reached. */
@Composable
private fun PictogramBlock(accent: Color, partial: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(
                color = accent.copy(alpha = if (partial) 0.40f else 0.92f),
                shape = RoundedCornerShape(3.dp),
            ),
    )
}



/**
 * Genre-mention share as a pie of the five strongest genres plus an `Altres`
 * remainder. The section subtitle states that slices are overlapping genre
 * mentions, not an exclusive split of titles.
 */
@Composable
internal fun GenrePieChart(
    stats: List<RankedStat>,
    emptyText: String,
) {
    val pieData = genrePieData(stats)
    val othersLabel = stringResource(R.string.stats_genre_others)
    val slices = pieData.top.map { stat -> stat.label to stat.value } +
        if (pieData.othersMentions > 0) listOf(othersLabel to pieData.othersMentions) else emptyList()
    val total = slices.sumOf { (_, value) -> value }

    StatsPanel {
        if (slices.isEmpty() || total == 0) {
            EmptyStatsText(text = emptyText)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.45f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val sliceColors = slices.indices.map { genreChartColor(it) }
                    Canvas(modifier = Modifier.size(168.dp)) {
                        var startAngle = -90f
                        slices.forEachIndexed { index, (_, value) ->
                            val sweep = 360f * value.toFloat() / total.toFloat()
                            drawArc(
                                color = sliceColors[index],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                            )
                            startAngle += sweep
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    slices.forEachIndexed { index, (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(genreChartColor(index), RoundedCornerShape(2.dp)),
                            )
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogTheme.colors.appInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = genreChartColor(index),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RankedList(
    stats: List<RankedStat>,
    accent: Color,
    emptyText: String,
) {
    val maxValue = stats.maxOfOrNull { stat -> stat.value } ?: 0

    StatsPanel {
        if (stats.isEmpty()) {
            EmptyStatsText(text = emptyText)
        } else {
            stats.forEachIndexed { index, stat ->
                RankedAuthorBar(
                    stat = stat,
                    maxValue = maxValue,
                    accent = accent,
                    rank = index + 1,
                )
            }
        }
    }
}

@Composable
internal fun RankedAuthorBar(
    stat: RankedStat,
    maxValue: Int,
    accent: Color,
    rank: Int,
) {
    val fillFraction = if (maxValue > 0) stat.value.toFloat() / maxValue.toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rank.toString().padStart(2, '0'),
                modifier = Modifier.width(22.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
            Text(
                text = stat.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stat.value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .height(18.dp)
                    .background(accent.copy(alpha = 0.92f), RoundedCornerShape(5.dp)),
            )
        }
    }
}
