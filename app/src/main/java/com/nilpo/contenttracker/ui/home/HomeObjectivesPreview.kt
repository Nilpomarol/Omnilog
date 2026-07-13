package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.ui.common.CompactObjectiveCard
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun DashboardObjectivesPreview(
    objectives: List<ObjectiveProgress>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Objectius personals",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Obre els objectius",
                tint = OmnilogColors.Dashboard,
            )
        }
        if (objectives.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                color = OmnilogColors.AppPanel,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Text(
                    "Afegeix un objectiu des del teu perfil.",
                    modifier = Modifier.padding(14.dp),
                    color = OmnilogColors.AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            objectives.take(3).forEach { progress ->
                CompactObjectiveCard(progress = progress)
            }
        }
    }
}
