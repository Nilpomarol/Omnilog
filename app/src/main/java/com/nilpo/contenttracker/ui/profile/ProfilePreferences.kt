package com.nilpo.contenttracker.ui.profile

import android.content.Context
import android.content.SharedPreferences

/**
 * Local-only profile preferences. Omnilog is still a single-user, local-first
 * app, so profile information belongs in preferences until a real account
 * model is introduced.
 */
object ProfilePreferences {
    const val FILE_NAME = "omnilog_preferences"
    const val DISPLAY_NAME_KEY = "profile_display_name"
    const val BIO_KEY = "profile_bio"
    const val AVATAR_ACCENT_KEY = "profile_avatar_accent"
    const val AVATAR_IMAGE_PATH_KEY = "profile_avatar_image_path"
    /** Previous versions stored a provider URI. Kept only for one-time migration. */
    const val AVATAR_IMAGE_URI_KEY = "profile_avatar_image_uri"
    /** Ids of objectives whose completion has already been celebrated, so it only fires once. */
    const val CELEBRATED_OBJECTIVES_KEY = "celebrated_objective_ids"

    const val DEFAULT_DISPLAY_NAME = "El teu perfil"
    const val DEFAULT_BIO = "La meva biblioteca personal"
    const val DEFAULT_AVATAR_ACCENT = 0

    fun from(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
