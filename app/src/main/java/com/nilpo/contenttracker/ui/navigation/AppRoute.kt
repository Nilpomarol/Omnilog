package com.nilpo.contenttracker.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.nilpo.contenttracker.ui.home.MediaSection
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class Section(val section: MediaSection) : AppRoute

    @Serializable
    data class MediaDetail(val mediaItemId: Long) : AppRoute

    @Serializable
    data class CollectionDetail(
        val collectionId: Long,
        val section: MediaSection,
    ) : AppRoute

    @Serializable
    data class AuthorDetail(
        val author: String,
        val section: MediaSection,
    ) : AppRoute

    @Serializable
    data object Stats : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data class AddMedia(
        val section: MediaSection,
        val collectionId: Long? = null,
        val collectionOrder: Double? = null,
    ) : AppRoute
}

internal fun MutableList<NavKey>.push(route: AppRoute) {
    if (lastOrNull() != route) add(route)
}

internal fun MutableList<NavKey>.goBack(): Boolean {
    if (size <= 1) return false
    removeAt(lastIndex)
    return true
}

internal fun MutableList<NavKey>.selectHome() {
    while (size > 1) removeAt(lastIndex)
}

internal fun MutableList<NavKey>.selectSection(section: MediaSection) {
    val target = AppRoute.Section(section)
    val existingIndex = indexOfLast { it == target }
    if (existingIndex >= 0) {
        while (lastIndex > existingIndex) removeAt(lastIndex)
        return
    }
    selectHome()
    add(target)
}
