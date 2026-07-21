package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OmnilogAnchoredDropdown(
    selectedOption: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionColor: @Composable (T) -> Color = { OmnilogTheme.colors.appInk },
    optionIcon: (@Composable (option: T, tint: Color) -> Unit)? = null,
    anchorContent: @Composable (
        selectedOption: T,
        selectedColor: Color,
        expanded: Boolean,
        anchorModifier: Modifier,
    ) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedColor = optionColor(selectedOption)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            expanded = shouldExpand && enabled && options.isNotEmpty()
        },
        modifier = modifier,
    ) {
        anchorContent(
            selectedOption,
            selectedColor,
            expanded,
            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            matchAnchorWidth = true,
            shape = RoundedCornerShape(14.dp),
            containerColor = OmnilogTheme.colors.appPanel,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        ) {
            options.forEach { option ->
                val color = optionColor(option)
                OmnilogDropdownItem(
                    text = optionLabel(option),
                    selected = option == selectedOption,
                    accent = color,
                    labelColor = color,
                    leadingIcon = optionIcon?.let { icon ->
                        { icon(option, color) }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Full-width outlined selector built on [OmnilogAnchoredDropdown]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OmnilogDropdownField(
    selectedOption: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    fieldColor: Color? = null,
    optionColor: @Composable (T) -> Color = { OmnilogTheme.colors.appInk },
    optionIcon: (@Composable (option: T, tint: Color) -> Unit)? = null,
) {
    OmnilogAnchoredDropdown(
        selectedOption = selectedOption,
        options = options,
        optionLabel = optionLabel,
        onOptionSelected = onOptionSelected,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        optionColor = optionColor,
        optionIcon = optionIcon,
    ) { option, optionTint, expanded, anchorModifier ->
        val anchorTint = fieldColor ?: optionTint
        OutlinedTextField(
            value = optionLabel(option),
            onValueChange = {},
            modifier = anchorModifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            singleLine = true,
            label = label?.let { fieldLabel -> { Text(fieldLabel) } },
            leadingIcon = optionIcon?.let { icon ->
                { icon(option, anchorTint) }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = anchorTint,
                unfocusedTextColor = anchorTint,
                focusedBorderColor = anchorTint,
                unfocusedBorderColor = OmnilogTheme.colors.appLine,
                focusedLabelColor = anchorTint,
                unfocusedLabelColor = OmnilogTheme.colors.appMuted,
                focusedLeadingIconColor = anchorTint,
                unfocusedLeadingIconColor = anchorTint,
                focusedTrailingIconColor = anchorTint,
                unfocusedTrailingIconColor = OmnilogTheme.colors.appMuted,
            ),
        )
    }
}

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

@Composable
fun <T> OmnilogDropdownChip(
    selectedOption: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: (T) -> Boolean = { true },
    optionColor: @Composable (T) -> Color = { OmnilogTheme.colors.appInk },
    optionIcon: (@Composable (option: T, tint: Color) -> Unit)? = null,
) {
    OmnilogAnchoredDropdown(
        selectedOption = selectedOption,
        options = options,
        optionLabel = optionLabel,
        onOptionSelected = onOptionSelected,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        optionColor = optionColor,
        optionIcon = optionIcon,
    ) { option, color, _, anchorModifier ->
        val active = isActive(option)
        Surface(
            modifier = anchorModifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = if (active) color.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
            border = BorderStroke(
                1.dp,
                if (active) color.copy(alpha = 0.50f) else OmnilogTheme.colors.appLine,
            ),
            contentColor = if (active) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                optionIcon?.invoke(option, color)
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
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
