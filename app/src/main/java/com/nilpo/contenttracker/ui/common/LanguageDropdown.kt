package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val normalizedValue = ItemLanguage.normalize(value) ?: ItemLanguage.Original
    val options = remember(normalizedValue) { languageOptions(normalizedValue) }
    val selectedOption = options.firstOrNull { it.value == normalizedValue } ?: options.first()

    OmnilogDropdownField(
        selectedOption = selectedOption,
        options = options,
        optionLabel = { it.label() },
        onOptionSelected = { onValueChange(it.value) },
        modifier = modifier,
        label = label,
        fieldColor = accent,
    )
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
