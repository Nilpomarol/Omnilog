package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorImageUrl
import com.nilpo.contenttracker.core.model.creatorImageAspectRatio
import com.nilpo.contenttracker.core.model.creatorNames

internal enum class HomeGroupType {
    Status,
    Collection,
    Author,
}

internal data class HomeDisplayGroup(
    val key: String,
    val type: HomeGroupType,
    val title: String,
    val items: List<TrackedMedia>,
    val collection: MediaCollection? = null,
    val status: TrackingStatus? = null,
    val imageUrl: String? = null,
    val imageAspectRatio: Float? = null,
    val imageIsLogo: Boolean = false,
)

internal data class CollectionProgressSummary(
    val label: String,
    val completedCount: Int,
    val inProgressCount: Int,
    val progressFraction: Float,
)

internal fun buildHomeGroups(
    items: List<TrackedMedia>,
    groupMode: HomeGroupMode,
    sortMode: HomeSortMode = HomeSortMode.Recent,
    sortDirection: HomeSortDirection = HomeSortDirection.Descending,
    selectedCreators: Set<String> = emptySet(),
): List<HomeDisplayGroup> {
    val normalizedSelectedCreators = selectedCreators
        .map(String::normalizedGroupValue)
        .filter(String::isNotBlank)
        .toSet()

    return when (groupMode) {
        HomeGroupMode.None -> emptyList()
        HomeGroupMode.Status -> TrackingStatus.entries.mapNotNull { status ->
            val groupItems = items.filter { it.currentSession?.status == status }
            if (groupItems.isEmpty()) null
            else HomeDisplayGroup(
                key = "status:${status.name}",
                type = HomeGroupType.Status,
                title = status.name,
                items = groupItems,
                status = status,
            )
        }
        HomeGroupMode.Collection -> items
            .groupBy { it.collection }
            .map { (collection, groupItems) ->
                HomeDisplayGroup(
                    key = collection?.let { "collection:${it.id}" } ?: "collection:none",
                    type = HomeGroupType.Collection,
                    title = collection?.name ?: "",
                    items = groupItems,
                    collection = collection,
                )
            }
            .sortGroupedLibrary(sortMode, sortDirection)
        HomeGroupMode.Author -> items
            .flatMap { trackedMedia ->
                val creators = trackedMedia.creatorNames()
                    .filter { creator ->
                        normalizedSelectedCreators.isEmpty() ||
                            creator.normalizedGroupValue() in normalizedSelectedCreators
                    }
                when {
                    creators.isNotEmpty() -> creators.map { author -> author to trackedMedia }
                    normalizedSelectedCreators.isEmpty() -> listOf("" to trackedMedia)
                    else -> emptyList()
                }
            }
            .groupBy({ it.first }, { it.second })
            .map { (author, groupItems) ->
                HomeDisplayGroup(
                    key = "author:${author.lowercase()}",
                    type = HomeGroupType.Author,
                    title = author,
                    items = groupItems,
                    imageUrl = items.creatorImageUrl(author),
                    imageAspectRatio = items.creatorImageAspectRatio(author),
                    imageIsLogo = groupItems.creditsACompany(),
                )
            }
            .sortGroupedLibrary(sortMode, sortDirection)
    }
}

/**
 * Grouped views sort the groups themselves, not whichever flat item happened to be first.
 * Empty/unknown buckets remain last because they are fallbacks rather than real library entities.
 */
private fun List<HomeDisplayGroup>.sortGroupedLibrary(
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
): List<HomeDisplayGroup> {
    val named = filter { it.title.isNotBlank() }
    val unknown = filter { it.title.isBlank() }

    val comparator: Comparator<HomeDisplayGroup> = when (sortMode) {
        HomeSortMode.Title -> compareBy { it.title.lowercase() }
        HomeSortMode.Recent -> compareBy<HomeDisplayGroup> { group ->
            group.items.maxOfOrNull { it.currentSession?.updatedAtEpochMillis ?: 0L } ?: 0L
        }.thenBy { it.title.lowercase() }
        HomeSortMode.Rating -> compareBy<HomeDisplayGroup> { group ->
            val average = group.items.mapNotNull { it.currentSession?.ratingHalfPoints }.average()
            if (average.isNaN()) -1.0 else average
        }.thenBy { it.title.lowercase() }
        HomeSortMode.Progress -> compareBy<HomeDisplayGroup> { group ->
            if (group.items.isEmpty()) {
                -1.0
            } else {
                group.items.count { it.currentSession?.status == TrackingStatus.Completed }
                    .toDouble() / group.items.size
            }
        }.thenBy { it.title.lowercase() }
    }

    val sorted = when (sortDirection) {
        HomeSortDirection.Ascending -> named.sortedWith(comparator)
        HomeSortDirection.Descending -> named.sortedWith(comparator.reversed())
    }
    return sorted + unknown
}

/**
 * Whether this group's contributor is a company, and so drawn whole rather than cropped.
 *
 * Every item has to agree: a group spanning both a studio's anime and a person's books has no one
 * right answer, and a portrait cropped from a logo is the worse of the two failures. Reading the
 * type off whichever item happened to sort first made the choice depend on list order.
 */
private fun List<TrackedMedia>.creditsACompany(): Boolean =
    isNotEmpty() && all { it.item.type in CompanyCreditedTypes }

private val CompanyCreditedTypes = setOf(MediaType.Anime, MediaType.Game)

private fun String.normalizedGroupValue(): String = trim().lowercase()

internal fun List<TrackedMedia>.collectionCoverStack(limit: Int = 3): List<String> =
    sortedWith(
        compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
            .thenBy { it.item.title.lowercase() },
    ).mapNotNull { it.item.coverUrl }.take(limit)
