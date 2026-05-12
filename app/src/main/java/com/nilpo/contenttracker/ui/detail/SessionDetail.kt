package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.ui.common.seasonSummary
import com.nilpo.contenttracker.ui.common.sessionLabel
import com.nilpo.contenttracker.ui.common.sessionSummary

@Composable
fun SessionDetail(
    session: TrackingSession,
    seasons: List<SeasonProgress>,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = sessionLabel(session),
            color = accent,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = sessionSummary(session),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
        seasonSummary(seasons)?.let { seasonText ->
            Text(
                text = seasonText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        session.platform?.let { platform ->
            Text(
                text = stringResource(R.string.platform_label, platform.name),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
