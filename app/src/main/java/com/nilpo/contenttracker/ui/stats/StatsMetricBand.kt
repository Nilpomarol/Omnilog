package com.nilpo.contenttracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.stats.ComparisonBasis
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlin.math.abs

/**
 * A KPI's change against the previous comparison window, paired with the arrow text the chip
 * shows. Absent when there is no previous window, or no prior data for that KPI.
 */
internal data class MetricDelta(
    val value: Number,
    val text: String,
)

internal fun intMetricDelta(delta: Int?): MetricDelta? =
    delta?.let { value -> MetricDelta(value = value, text = intDeltaText(value)) }

internal fun ratingMetricDelta(delta: Double?): MetricDelta? =
    delta?.let { value -> MetricDelta(value = value, text = ratingDeltaText(value)) }

/**
 * One lead figure and a stack of supporting values, split by a rule.
 *
 * Shared by the statistics hero and the dashboard preview so both read the same way; the two
 * differ only in [leadValueStyle], since the dashboard card is the more compact of the pair.
 */
@Composable
internal fun MetricBand(
    modifier: Modifier = Modifier,
    lead: @Composable RowScope.() -> Unit,
    supporting: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lead()
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = OmnilogTheme.colors.appLine,
        )
        Column(
            modifier = Modifier.weight(0.9f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = supporting,
        )
    }
}

@Composable
internal fun MetricBandLead(
    value: String,
    label: String,
    accent: Color,
    delta: MetricDelta?,
    basisLabel: String?,
    leadValueStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val description = metricDescription(
        label = label,
        value = value,
        delta = delta,
        basisLabel = basisLabel,
    )

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = leadValueStyle,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
            )
            delta?.let { metricDelta ->
                DeltaChip(text = metricDelta.text, color = deltaChipColor(metricDelta.value))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            // Two lines, not one: the label sits under a large figure in roughly half the card's
            // width, so at larger font scales it needs room to wrap rather than be truncated.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A supporting KPI on one line: short label left, value and delta right. The visible [label] is
 * abbreviated to keep the row from wrapping; [accessibleLabel] keeps the full wording for
 * screen readers.
 */
@Composable
internal fun MetricBandSupporting(
    label: String,
    accessibleLabel: String,
    value: String,
    accent: Color,
    delta: MetricDelta?,
    basisLabel: String?,
    valueStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val description = metricDescription(
        label = accessibleLabel,
        value = value,
        delta = delta,
        basisLabel = basisLabel,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            maxLines = 1,
        )
        delta?.let { metricDelta ->
            DeltaChip(text = metricDelta.text, color = deltaChipColor(metricDelta.value))
        }
    }
}

/**
 * A condensed basis for headers that cannot spare a full line, still saying enough to be true:
 * a year-to-date comparison reads `vs. 2025 fins avui`, not the bare `vs. 2025` that would imply
 * the whole calendar year. Pair it with [comparisonBasisLabel] for the spoken form.
 */
@Composable
internal fun comparisonBasisLabelShort(basis: ComparisonBasis): String {
    return when (basis) {
        is ComparisonBasis.SamePeriodOfYear ->
            stringResource(R.string.stats_delta_basis_same_period_short, basis.year)
        is ComparisonBasis.FullYear -> stringResource(R.string.stats_delta_basis_year, basis.year)
        ComparisonBasis.Previous12Months ->
            stringResource(R.string.stats_delta_basis_previous_12_months_short)
    }
}

/** Names the window a delta was measured against, so the chips are not bare arrows. */
@Composable
internal fun comparisonBasisLabel(basis: ComparisonBasis): String {
    return when (basis) {
        is ComparisonBasis.SamePeriodOfYear ->
            stringResource(R.string.stats_delta_basis_same_period, basis.year)
        is ComparisonBasis.FullYear -> stringResource(R.string.stats_delta_basis_year, basis.year)
        ComparisonBasis.Previous12Months ->
            stringResource(R.string.stats_delta_basis_previous_12_months)
    }
}

/**
 * Spoken form of a KPI. The delta chips read as bare arrows on their own, so each metric
 * announces its label, value, and — when there is a comparison window — the change and the
 * basis it was measured against.
 */
@Composable
internal fun metricDescription(
    label: String,
    value: String,
    delta: MetricDelta?,
    basisLabel: String?,
): String {
    if (delta == null || basisLabel == null) {
        return stringResource(R.string.stats_metric_description, label, value)
    }
    val magnitude = when (val deltaValue = delta.value) {
        is Double -> "%.1f".format(abs(deltaValue))
        else -> abs(deltaValue.toInt()).toString()
    }
    val deltaDescription = when {
        delta.value.toDouble() > 0.0 ->
            stringResource(R.string.stats_delta_more_description, magnitude, basisLabel)
        delta.value.toDouble() < 0.0 ->
            stringResource(R.string.stats_delta_less_description, magnitude, basisLabel)
        else -> stringResource(R.string.stats_delta_same_description, basisLabel)
    }
    return stringResource(R.string.stats_metric_description_delta, label, value, deltaDescription)
}
