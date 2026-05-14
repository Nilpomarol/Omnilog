package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.common.MetadataSummary
import com.nilpo.contenttracker.ui.common.sessionLabel
import com.nilpo.contenttracker.ui.common.sessionSummary

@Composable
fun MediaCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
) {
    val session = trackedMedia.currentSession

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = trackedMedia.item.title,
            style = MaterialTheme.typography.titleLarge,
        )

        if (session != null) {
            Text(
                text = sessionLabel(session),
                color = accent,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = sessionSummary(session, trackedMedia.item.progressTotal),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
            )
            MetadataSummary(
                trackedMedia = trackedMedia,
                session = session,
            )
        }
    }
}
