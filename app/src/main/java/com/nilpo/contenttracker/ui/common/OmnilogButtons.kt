package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

private val ButtonShape = RoundedCornerShape(12.dp)

/** The one strong action of a group or sheet: filled in the accent. */
@Composable
fun OmnilogPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = OmnilogTheme.accents.Dashboard,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = OmnilogTheme.colors.appBackground,
            disabledContainerColor = OmnilogTheme.colors.appLine,
            disabledContentColor = OmnilogTheme.colors.appMuted,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** A quieter action beside or instead of the primary one: a soft tint of the same accent. */
@Composable
fun OmnilogTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = OmnilogTheme.accents.Dashboard,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
            disabledContainerColor = OmnilogTheme.colors.appLine.copy(alpha = 0.5f),
            disabledContentColor = OmnilogTheme.colors.appMuted,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
