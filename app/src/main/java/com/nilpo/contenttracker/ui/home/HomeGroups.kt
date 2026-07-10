package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus

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
                trackedMedia.item.creators
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
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
                )
            }
    }
}

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

internal fun List<TrackedMedia>.collectionCoverUrl(): String? =
    sortedWith(
        compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
            .thenBy { it.item.title.lowercase() },
    ).firstOrNull { it.item.coverUrl != null }?.item?.coverUrl

internal fun List<TrackedMedia>.collectionLastUpdatedMillis(): Long? =
    mapNotNull { it.currentSession?.updatedAtEpochMillis?.takeIf { ms -> ms > 0 } }.maxOrNull()

internal fun List<TrackedMedia>.authorTopRatedItem(): TrackedMedia? =
    filter { it.item.coverUrl != null }
        .maxWithOrNull(
            compareBy<TrackedMedia> { it.currentSession?.rating ?: 0 }
                .thenBy { it.item.externalRatingScore ?: 0.0 }
                .thenBy { it.currentSession?.updatedAtEpochMillis ?: 0L },
        )
