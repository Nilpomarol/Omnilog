package com.nilpo.contenttracker.ui.home

import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

/** Collection continuity first; positions within each collection follow its reading/play order. */
internal fun prioritizeHomePlannedItems(
    items: List<TrackedMedia>,
    today: LocalDate = LocalDate.now(),
): List<TrackedMedia> {
    val recentStart = today.minusDays(29)
    val relevantCollections = items.mapNotNull { media ->
        val collectionId = media.item.collectionId ?: return@mapNotNull null
        val isActive = media.currentSession?.status == TrackingStatus.InProgress
        val recentlyCompleted = media.sessions.any { session ->
            session.status == TrackingStatus.Completed && session.finishedAt?.let { date ->
                !date.isBefore(recentStart) && !date.isAfter(today)
            } == true
        }
        collectionId.takeIf { isActive || recentlyCompleted }
    }.toSet()

    val ranked = items.filter { it.currentSession?.status == TrackingStatus.Planned }
        .sortedWith(
            compareByDescending<TrackedMedia> { it.item.collectionId in relevantCollections }
                .thenByDescending { media -> media.sessions.maxOfOrNull { it.updatedAtEpochMillis } ?: 0L },
        )

    // Keep each collection's existing slots in the ranking, but fill them in collection order.
    // A pairwise comparator mixing collection order and activity dates would not be transitive.
    val orderedCollections = ranked.filter { it.item.collectionId != null }
        .groupBy { it.item.collectionId }
        .mapValues { (_, members) ->
            members.sortedBy { it.item.collectionSortOrder?.takeIf(Double::isFinite) ?: Double.POSITIVE_INFINITY }
                .iterator()
        }
    return ranked.map { media -> orderedCollections[media.item.collectionId]?.next() ?: media }
}
