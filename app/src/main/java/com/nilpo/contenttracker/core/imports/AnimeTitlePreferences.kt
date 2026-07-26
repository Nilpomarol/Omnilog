package com.nilpo.contenttracker.core.imports

import android.content.Context

enum class AnimeTitlePreference {
    EnglishWithRomajiOriginal,
    KeepMalTitle,
}

object AnimeTitlePreferences {
    private const val PreferencesName = "omnilog_import_preferences"
    private const val TitlePreferenceKey = "anime_title_preference"

    fun read(context: Context): AnimeTitlePreference {
        val stored = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getString(TitlePreferenceKey, null)
        return when (stored) {
            // Preserve the setting used by builds that stored Japanese script as the original title.
            "EnglishWithJapaneseOriginal" -> AnimeTitlePreference.EnglishWithRomajiOriginal
            else -> stored?.let { runCatching { AnimeTitlePreference.valueOf(it) }.getOrNull() }
        } ?: AnimeTitlePreference.EnglishWithRomajiOriginal
    }

    fun write(context: Context, preference: AnimeTitlePreference) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(TitlePreferenceKey, preference.name)
            .apply()
    }
}
