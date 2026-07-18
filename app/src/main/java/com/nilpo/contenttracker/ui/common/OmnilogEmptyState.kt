package com.nilpo.contenttracker.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Panel for a state with no content to show: a reason, and a way out of it.
 *
 * Actions wrap onto their own lines at large font scales rather than shrinking, so keep
 * [primaryAction] and [secondaryAction] to short verb phrases.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmnilogEmptyState(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int? = null,
    primaryAction: EmptyStateAction? = null,
    secondaryAction: EmptyStateAction? = null,
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
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = accent,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            if (primaryAction != null || secondaryAction != null) {
                FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (primaryAction != null) {
                        Button(
                            onClick = primaryAction.onClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = OmnilogTheme.colors.appBackground,
                            ),
                        ) {
                            Text(text = primaryAction.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (secondaryAction != null) {
                        OutlinedButton(
                            onClick = secondaryAction.onClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
                        ) {
                            Text(text = secondaryAction.label)
                        }
                    }
                }
            }
        }
    }
}

data class EmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
)
