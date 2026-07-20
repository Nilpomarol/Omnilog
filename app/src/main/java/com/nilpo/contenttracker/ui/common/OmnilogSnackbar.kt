package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Snackbar styled to match the Omnilog aesthetic: a warm dark panel with a
 * hairline border and cream ink, instead of the stock Material light pill.
 */
@Composable
fun OmnilogSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = OmnilogTheme.accents.Dashboard,
) {
    val actionLabel = snackbarData.visuals.actionLabel

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = OmnilogTheme.colors.appPanelTranslucent,
        contentColor = OmnilogTheme.colors.appInk,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 18.dp,
                end = if (actionLabel != null) 8.dp else 18.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = snackbarData.visuals.message,
                modifier = Modifier.weight(1f),
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (actionLabel != null) {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(
                        text = actionLabel,
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
