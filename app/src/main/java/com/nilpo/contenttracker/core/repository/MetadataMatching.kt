package com.nilpo.contenttracker.core.repository

import java.text.Normalizer
import java.util.Locale

/** Canonical text used only for provider matching; keeps letters and numbers from every script. */
internal fun String.normalizedMetadataMatchText(
    removeParenthetical: Boolean = false,
): String {
    val text = if (removeParenthetical) replace(Regex("""\([^)]*\)"""), " ") else this
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD)
    val folded = buildString(decomposed.length) {
        var baseScript: Character.UnicodeScript? = null
        decomposed.forEach { character ->
            val type = Character.getType(character)
            val isMark = type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt()
            if (!isMark) {
                baseScript = Character.UnicodeScript.of(character.code)
            }
            if (!isMark || baseScript != Character.UnicodeScript.LATIN) append(character)
        }
    }
    val normalized = folded
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}\\p{M}]+"), " ")
        .trim()
    return Normalizer.normalize(normalized, Normalizer.Form.NFC)
}
