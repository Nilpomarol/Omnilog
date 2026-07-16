package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveStatus
import com.nilpo.contenttracker.ui.common.ObjectiveSummaryRow
import com.nilpo.contenttracker.ui.common.paceStatus
import com.nilpo.contenttracker.ui.theme.OmnilogColors

private const val VisibleObjectiveRows = 3

/**
 * UX-17: the dashboard's objectives preview is a single card, not one card per objective.
 *
 * The card carries a status roll-up in its header so the section reads at a glance, and one slim
 * row per objective below it. Rows keep the `24 de 40 llibres` value-and-target phrasing required
 * by UX-01/UX-02 — a percentage alone would not say what the objective measures. Renders nothing
 * when there are no objectives; the place to create one is Profile, which owns that action.
 */
@Composable
fun DashboardObjectivesPreview(
    objectives: List<ObjectiveProgress>,
    onClick: () -> Unit,
) {
    if (objectives.isEmpty()) return

    val visible = objectives.take(VisibleObjectiveRows)
    val overflow = objectives.size - visible.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_objectives_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = objectives.statusSummary(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = OmnilogColors.AppMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_objectives_open),
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogColors.Dashboard,
                )
            }

            visible.forEach { progress ->
                ObjectiveSummaryRow(progress = progress)
            }

            if (overflow > 0) {
                Text(
                    text = stringResource(R.string.home_objectives_more, overflow),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogColors.Dashboard,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

/**
 * Roll-up shown beside the card title, e.g. `2 al dia · 1 endarrerit`. Only non-zero groups appear,
 * so the line stays short enough to sit on one row next to the title.
 */
@Composable
private fun List<ObjectiveProgress>.statusSummary(): String {
    val byStatus = groupingBy { it.paceStatus() }.eachCount()
    val onTrack = (byStatus[ObjectiveStatus.OnTrack] ?: 0) + (byStatus[ObjectiveStatus.Ahead] ?: 0)
    val behind = byStatus[ObjectiveStatus.Behind] ?: 0
    val completed = byStatus[ObjectiveStatus.Completed] ?: 0

    return listOfNotNull(
        onTrack.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_on_track, it, it)
        },
        behind.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_behind, it, it)
        },
        completed.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.home_objectives_summary_completed, it, it)
        },
    ).joinToString(" · ")
}
