package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.stats.ComparisonBasis
import com.nilpo.contenttracker.core.stats.StatsBucket
import com.nilpo.contenttracker.core.stats.StatsObservation
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.StatsSnapshot
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme


@Composable
internal fun StatsActivityHero(snapshot: StatsSnapshot) {
    val buckets = snapshot.completionSessionsByMonth.takeLast(12)
    val maxValue = buckets.maxOfOrNull { bucket -> bucket.value } ?: 0
    val legendMediaTypes = buckets
        .flatMap { bucket -> bucket.segments.map { segment -> segment.mediaType } }
        .distinct()
        .sortedBy { mediaType -> mediaType.ordinal }
    var selectedMonthKey by remember(snapshot) { mutableStateOf<String?>(null) }
    val selectedBucket = buckets.firstOrNull { bucket -> bucket.key == selectedMonthKey }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.accents.Dashboard.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.stats_activity_hero_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                )
                // The selected period already sits in the sticky filter bar, so
                // the header carries the comparison basis instead. Spelling it
                // out in full matters: for a year-to-date period the basis is
                // the same date range of the previous year, not that whole year.
                snapshot.deltas.basis?.let { basis ->
                    Text(
                        text = basis.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatsPeriodMetrics(snapshot = snapshot)
            // For a bounded period the bars are self-evidently the lead metric
            // broken down by month, so the caption would only repeat the label
            // above. All-time is the exception: there the caption carries the
            // 12-month window, which nothing else on the card states.
            if (snapshot.filters.period == StatsPeriod.AllTime) {
                Text(
                    text = stringResource(R.string.stats_activity_bars_caption_all_time),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActivityHeroBars(
                buckets = buckets,
                maxValue = maxValue,
                selectedKey = selectedMonthKey,
                onBarClick = { bucket ->
                    selectedMonthKey = if (selectedMonthKey == bucket.key) null else bucket.key
                },
            )
            if (legendMediaTypes.isNotEmpty()) {
                MonthlyLegend(mediaTypes = legendMediaTypes)
            }
            // Sits below the bars because it answers a bar tap.
            Text(
                text = selectedBucket?.let { bucket ->
                    stringResource(R.string.stats_activity_selected_month, bucket.label, bucket.value)
                } ?: snapshot.topLevelSummary.observation?.label()
                ?: stringResource(R.string.stats_activity_hero_empty),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedBucket != null) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The period KPIs: completion sessions as the single lead figure, with the
 * average rating and revisit count as supporting values behind a rule. All
 * three carry their delta chip, and the comparison basis is named once in the
 * card header rather than repeated per chip.
 */
@Composable
private fun StatsPeriodMetrics(snapshot: StatsSnapshot) {
    val basisLabel = snapshot.deltas.basis?.let { basis -> comparisonBasisLabel(basis) }

    MetricBand(
        lead = {
            MetricBandLead(
                value = snapshot.completionSessions.toString(),
                label = stringResource(R.string.stats_completion_sessions),
                accent = OmnilogTheme.accents.Completed,
                delta = intMetricDelta(snapshot.deltas.completionSessions),
                basisLabel = basisLabel,
                leadValueStyle = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1.1f),
            )
        },
        supporting = {
            MetricBandSupporting(
                label = stringResource(R.string.stats_metric_rating_short),
                accessibleLabel = stringResource(R.string.stats_summary_average_rating),
                value = snapshot.averageRating
                    ?.let { rating -> "${rating.roundedStatValue()}/10" }
                    ?: "—",
                accent = OmnilogTheme.accents.Dashboard,
                delta = ratingMetricDelta(snapshot.deltas.averageRating),
                basisLabel = basisLabel,
            )
            MetricBandSupporting(
                label = stringResource(R.string.stats_summary_revisits),
                accessibleLabel = stringResource(R.string.stats_summary_revisits),
                value = snapshot.revisitCount.toString(),
                accent = OmnilogTheme.accents.Books,
                delta = intMetricDelta(snapshot.deltas.revisits),
                basisLabel = basisLabel,
            )
        },
    )
}



@Composable
internal fun StatsObservation.label(): String {
    return when (this) {
        is StatsObservation.BusiestMonth -> stringResource(
            R.string.stats_activity_hero_busiest_month,
            label,
            completionSessions,
        )
        is StatsObservation.HighestRatedMedium -> stringResource(
            R.string.stats_observation_highest_rated_medium,
            mediaType.label(),
            averageRating,
        )
    }
}


@Composable
internal fun ComparisonBasis.label(): String {
    return when (this) {
        is ComparisonBasis.SamePeriodOfYear -> stringResource(R.string.stats_delta_basis_same_period, year)
        is ComparisonBasis.FullYear -> stringResource(R.string.stats_delta_basis_year, year)
        ComparisonBasis.Previous12Months -> stringResource(R.string.stats_delta_basis_previous_12_months)
    }
}

@Composable
internal fun ActivityHeroBars(
    buckets: List<StatsBucket>,
    maxValue: Int,
    selectedKey: String? = null,
    onBarClick: ((StatsBucket) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 122.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { bucket ->
            val barDescription = stringResource(
                R.string.stats_activity_selected_month,
                bucket.label,
                bucket.value,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (selectedKey == null || selectedKey == bucket.key) 1f else 0.35f)
                    .then(
                        if (onBarClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .semantics { contentDescription = barDescription }
                                .clickable { onBarClick(bucket) }
                        } else {
                            Modifier
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val barHeight = if (maxValue == 0 || bucket.value == 0) {
                        2
                    } else {
                        (96 * bucket.value / maxValue).coerceAtLeast(3)
                    }
                    if (bucket.value > 0 && bucket.segments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            bucket.segments.asReversed().forEach { segment ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(segment.value.toFloat())
                                        .background(segment.mediaType.statsColor()),
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight.dp)
                                .background(
                                    color = if (bucket.value > 0) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appLine.copy(alpha = 0.58f),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                ),
                        )
                    }
                }
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun intDeltaText(delta: Int): String {
    return when {
        delta > 0 -> "↑$delta"
        delta < 0 -> "↓${-delta}"
        else -> "—"
    }
}

internal fun ratingDeltaText(delta: Double): String {
    return when {
        delta > 0.0 -> "↑%.1f".format(delta)
        delta < 0.0 -> "↓%.1f".format(-delta)
        else -> "—"
    }
}

@Composable
internal fun deltaChipColor(delta: Number): Color {
    val value = delta.toDouble()
    return when {
        value > 0.0 -> OmnilogTheme.accents.Completed
        value < 0.0 -> OmnilogTheme.accents.Dropped
        else -> OmnilogTheme.colors.appMuted
    }
}

@Composable
internal fun DeltaChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.18f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 1,
        )
    }
}
