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
): List<HomeDisplayGroup> {
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
        HomeGroupMode.Collection -> {
            val grouped = items.groupBy { it.collection }.toList()
            sortCollectionGroups(grouped, sortMode, sortDirection)
                .map { (collection, groupItems) ->
                    HomeDisplayGroup(
                        key = collection?.let { "collection:${it.id}" } ?: "collection:none",
                        type = HomeGroupType.Collection,
                        title = collection?.name ?: "",
                        items = groupItems,
                        collection = collection,
                    )
                }
        }
        HomeGroupMode.Author -> items
            .flatMap { trackedMedia ->
                trackedMedia.creatorNames()
                    .map { author -> author to trackedMedia }
                    .ifEmpty { listOf("" to trackedMedia) }
            }
            .groupBy({ it.first }, { it.second })
            .toList()
            .sortedBy { it.first.lowercase() }
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
    }
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

/**
 * Sorts named collection groups by [sortMode]/[sortDirection].
 * The "no collection" bucket is always placed last regardless of sort.
 */
private fun sortCollectionGroups(
    groups: List<Pair<MediaCollection?, List<TrackedMedia>>>,
    sortMode: HomeSortMode,
    sortDirection: HomeSortDirection,
): List<Pair<MediaCollection?, List<TrackedMedia>>> {
    val named = groups.filter { it.first != null }
    val noCollection = groups.filter { it.first == null }

    val comparator: Comparator<Pair<MediaCollection?, List<TrackedMedia>>> = when (sortMode) {
        HomeSortMode.Title -> compareBy { it.first!!.name.lowercase() }
        HomeSortMode.Recent -> compareBy {
            it.second.maxOfOrNull { tm -> tm.currentSession?.updatedAtEpochMillis ?: 0L } ?: 0L
        }
        HomeSortMode.Rating -> compareBy {
            val avg = it.second.mapNotNull { tm -> tm.currentSession?.rating }.average()
            if (avg.isNaN()) -1.0 else avg
        }
        HomeSortMode.Progress -> compareBy {
            val size = it.second.size
            if (size == 0) -1.0
            else it.second.count { tm -> tm.currentSession?.status == TrackingStatus.Completed }
                .toDouble() / size
        }
    }

    val sorted = if (sortDirection == HomeSortDirection.Descending) {
        named.sortedWith(comparator.reversed())
    } else {
        named.sortedWith(comparator)
    }

    return sorted + noCollection
}

internal fun List<TrackedMedia>.collectionCoverStack(limit: Int = 3): List<String> =
    sortedWith(
        compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
            .thenBy { it.item.title.lowercase() },
    ).mapNotNull { it.item.coverUrl }.take(limit)

internal fun List<TrackedMedia>.collectionLastUpdatedMillis(): Long? =
    mapNotNull { it.currentSession?.updatedAtEpochMillis?.takeIf { ms -> ms > 0 } }.maxOrNull()

internal fun List<TrackedMedia>.authorTopRatedItem(): TrackedMedia? =
    filter { it.item.coverUrl != null }
        .maxWithOrNull(
            compareBy<TrackedMedia> { it.currentSession?.rating ?: 0 }
                .thenBy { it.item.externalRatingScore ?: 0.0 }
                .thenBy { it.currentSession?.updatedAtEpochMillis ?: 0L },
        )
