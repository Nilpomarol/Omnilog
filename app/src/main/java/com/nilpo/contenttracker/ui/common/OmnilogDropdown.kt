package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Themed replacement for [DropdownMenu] that matches the Omnilog dark surface
 * (panel background, hairline border, rounded corners, no tonal tint) instead of
 * the stock Material popup. Use together with [OmnilogDropdownItem].
 */
@Composable
fun OmnilogDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(14.dp),
        containerColor = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        content = content,
    )
}

/**
 * Themed [DropdownMenuItem]. When [selected] is true the label adopts [accent]
 * and a trailing check is shown, giving the menu a clear current-value cue.
 */
@Composable
fun OmnilogDropdownItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = OmnilogTheme.colors.appInk,
    labelColor: Color = OmnilogTheme.colors.appInk,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (selected) accent else labelColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = OmnilogTheme.colors.appInk,
            leadingIconColor = OmnilogTheme.colors.appMuted,
            trailingIconColor = accent,
        ),
    )
}
