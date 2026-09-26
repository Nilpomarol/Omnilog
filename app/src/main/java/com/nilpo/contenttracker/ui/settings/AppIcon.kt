package com.nilpo.contenttracker.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.nilpo.contenttracker.R

/**
 * The launcher icons the user can pick, each backed by an `activity-alias` in the manifest: the
 * classic mark, and the shelf logo in one of several colours. The package manager's enabled alias
 * is the only record of the choice, so there is no preference to fall out of sync with it.
 */
enum class AppIcon(
    val label: String,
    private val alias: String,
    @DrawableRes val mipmap: Int,
    @ColorRes val color: Int?,
) {
    Classic("Clàssica", "LauncherClassic", R.mipmap.ic_launcher, null),
    Green("Verd", "LauncherGreen", R.mipmap.ic_launcher_green, R.color.ic_launcher_green),
    Violet("Violeta", "LauncherViolet", R.mipmap.ic_launcher_violet, R.color.ic_launcher_violet),
    Olive("Oliva", "LauncherOlive", R.mipmap.ic_launcher_olive, R.color.ic_launcher_olive),
    Blue("Blau", "LauncherBlue", R.mipmap.ic_launcher_blue, R.color.ic_launcher_blue),
    Magenta("Magenta", "LauncherMagenta", R.mipmap.ic_launcher_magenta, R.color.ic_launcher_magenta),
    Teal("Turquesa", "LauncherTeal", R.mipmap.ic_launcher_teal, R.color.ic_launcher_teal);

    // Aliases resolve against the namespace, not the applicationId, which gains a suffix in debug.
    fun component(context: Context) = ComponentName(context, "com.nilpo.contenttracker.$alias")
}

// Disabling the alias the app was launched through makes the system close the app, so a choice
// waits here until MainActivity stops. ponytail: in-memory, a choice is lost if the process dies
// while still in the foreground; persist it if that ever matters.
private var pendingAppIcon: AppIcon? = null

fun currentAppIcon(context: Context): AppIcon {
    pendingAppIcon?.let { return it }
    val packageManager = context.packageManager
    // Classic is the manifest default, so it wins while no alias has been explicitly enabled.
    return AppIcon.entries.firstOrNull {
        packageManager.getComponentEnabledSetting(it.component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } ?: AppIcon.Classic
}

fun selectAppIcon(icon: AppIcon) {
    pendingAppIcon = icon
}

/** Called once the app is in the background, where switching the launcher entry can't close it. */
fun applyPendingAppIcon(context: Context) {
    val icon = pendingAppIcon ?: return
    pendingAppIcon = null
    val packageManager = context.packageManager
    // Enable the new entry before disabling the rest, so the launcher never has zero entries for the app.
    (listOf(icon) + (AppIcon.entries - icon)).forEach {
        packageManager.setComponentEnabledSetting(
            it.component(context),
            if (it == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
