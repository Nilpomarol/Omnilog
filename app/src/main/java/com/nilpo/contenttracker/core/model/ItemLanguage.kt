package com.nilpo.contenttracker.core.model

import java.util.Locale

object ItemLanguage {
    const val Original = "original"
    const val Catalan = "ca"
    const val Spanish = "es"
    const val English = "en"
    const val Japanese = "ja"

    val Defaults = listOf(Original, Catalan, Spanish, English, Japanese)

    fun normalize(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return when (trimmed.lowercase(Locale.ROOT).replace("_", "-")) {
            Original, "orig" -> Original
            Catalan, "cat", "catalan", "català", "catala" -> Catalan
            Spanish, "spa", "esl", "spanish", "castellà", "castella", "espanyol" -> Spanish
            English, "eng", "english", "anglès", "angles" -> English
            Japanese, "jp", "jpn", "japanese", "japonès", "japones" -> Japanese
            else -> trimmed
        }
    }
}
