package com.nilpo.contenttracker.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.timeline.TimelineSummary
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Streak, a three-week heatmap and per-unit totals for whatever the filters currently select.
 *
 * Everything here is derived from real dated entries. There is deliberately no "total time" figure:
 * no media item carries a runtime, so any duration would be invented.
 */
@Composable
fun TimelineSummaryPanel(
    summary: TimelineSummary,
    modifier: Modifier = Modifier,
) {
    val accent = OmnilogTheme.accents.Dashboard

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (summary.currentStreak > 0) {
                    Text(
                        text = summary.currentStreak.toString(),
                        color = OmnilogTheme.colors.appInk,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = stringResource(R.string.timeline_streak_days),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 2.dp),
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(R.string.timeline_streak_best, summary.bestStreak),
                        modifier = Modifier.padding(bottom = 2.dp),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.timeline_streak_none),
                        modifier = Modifier.weight(1f),
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            TimelineHeatmap(summary = summary, accent = accent)

            val totals = summary.totalsByUnit.entries.toList()
            if (totals.isNotEmpty() || summary.completedCount > 0) {
                HorizontalDivider(color = OmnilogTheme.colors.appLine)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    totals.forEach { (unit, total) ->
                        TimelineTotal(value = total, label = unit.label(total), accent = accent)
                    }
                    if (summary.completedCount > 0) {
                        TimelineTotal(
                            value = summary.completedCount,
                            label = stringResource(R.string.timeline_completed).lowercase(),
                            accent = OmnilogTheme.accents.Completed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTotal(value: Int, label: String, accent: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value.toString(),
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Intensity is scaled against the busiest day in view rather than a fixed threshold, so the grid
 * still reads on a light week instead of showing one lit cell and six empty ones.
 */
@Composable
private fun TimelineHeatmap(summary: TimelineSummary, accent: androidx.compose.ui.graphics.Color) {
    val weekdayLabels = listOf(
        R.string.timeline_weekday_monday,
        R.string.timeline_weekday_tuesday,
        R.string.timeline_weekday_wednesday,
        R.string.timeline_weekday_thursday,
        R.string.timeline_weekday_friday,
        R.string.timeline_weekday_saturday,
        R.string.timeline_weekday_sunday,
    )

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            weekdayLabels.forEach { labelRes ->
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        summary.heatmapWeeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val alpha = when {
                        day.isInFuture -> 0f
                        day.entryCount == 0 -> 0.10f
                        summary.busiestDayCount <= 0 -> 0.10f
                        else -> 0.28f + 0.72f * (day.entryCount.toFloat() / summary.busiestDayCount)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (day.isInFuture) {
                                    OmnilogTheme.colors.appLine.copy(alpha = 0.25f)
                                } else {
                                    accent.copy(alpha = alpha)
                                },
                            ),
                    )
                }
            }
        }
    }
}
