package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ItemLanguage

@Composable
fun LanguageDropdown(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val normalizedValue = ItemLanguage.normalize(value) ?: ItemLanguage.Original
    val options = remember(normalizedValue) { languageOptions(normalizedValue) }
    val selectedOption = options.firstOrNull { it.value == normalizedValue } ?: options.first()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption.label(),
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                cursorColor = accent,
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.label()) },
                    onClick = {
                        onValueChange(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun languageLabel(value: String?): String {
    val normalizedValue = ItemLanguage.normalize(value) ?: return ""
    return languageOption(normalizedValue).label()
}

private fun languageOptions(currentValue: String): List<LanguageOption> {
    val defaults = ItemLanguage.Defaults.map(::languageOption)
    return if (defaults.any { it.value == currentValue }) {
        defaults
    } else {
        defaults + LanguageOption(value = currentValue, customLabel = currentValue)
    }
}

private fun languageOption(value: String): LanguageOption {
    return when (value) {
        ItemLanguage.Original -> LanguageOption(value, R.string.language_original)
        ItemLanguage.Catalan -> LanguageOption(value, R.string.language_catalan)
        ItemLanguage.Spanish -> LanguageOption(value, R.string.language_spanish)
        ItemLanguage.English -> LanguageOption(value, R.string.language_english)
        ItemLanguage.Japanese -> LanguageOption(value, R.string.language_japanese)
        else -> LanguageOption(value = value, customLabel = value)
    }
}

private data class LanguageOption(
    val value: String,
    @param:StringRes val labelResId: Int? = null,
    val customLabel: String? = null,
) {
    @Composable
    fun label(): String {
        return labelResId?.let { stringResource(it) } ?: customLabel.orEmpty()
    }
}
