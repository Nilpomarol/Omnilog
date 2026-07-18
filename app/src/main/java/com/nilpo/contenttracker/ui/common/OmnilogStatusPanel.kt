package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Panel for a transient loading or error state: what is happening, and where the state is
 * recoverable, the action that leaves it.
 *
 * Set [showProgressIndicator] only where the surrounding surface shows no other indicator —
 * the metadata search bar already spins while a query runs, so a second one there reads as
 * two separate waits. [action] wraps below the text rather than sitting beside it, so it stays
 * reachable at large font scales; keep its label to a short verb phrase.
 */
@Composable
fun OmnilogStatusPanel(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    textColor: Color = OmnilogTheme.colors.appMuted,
    showProgressIndicator: Boolean = false,
    action: EmptyStateAction? = null,
    actionEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showProgressIndicator) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                    trackColor = OmnilogTheme.colors.appLine,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
            if (action != null) {
                Button(
                    onClick = action.onClick,
                    enabled = actionEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = OmnilogTheme.colors.appBackground,
                    ),
                ) {
                    Text(text = action.label, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
