package com.nilpo.contenttracker.core.objectives

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

class ObjectiveCalculator(
    private val today: LocalDate = LocalDate.now(),
) {
    fun calculate(
        items: List<TrackedMedia>,
        objectives: List<Objective>,
    ): List<ObjectiveProgress> {
        return objectives.map { objective ->
            ObjectiveProgress(
                objective = objective,
                currentValue = when (objective.metric) {
                    ObjectiveMetric.CompletedTitles -> completedTitles(items, objective)
                    ObjectiveMetric.ProgressUnits -> progressUnits(items, objective)
                },
            )
        }
    }

    private fun completedTitles(
        items: List<TrackedMedia>,
        objective: Objective,
    ): Int {
        return items
            .asSequence()
            .filter { trackedMedia -> matchesMediaType(trackedMedia.item.type, objective.mediaType) }
            .filter { trackedMedia ->
                trackedMedia.sessions.any { session ->
                    session.status == TrackingStatus.Completed &&
                        session.finishedAt?.let { date -> !date.isBefore(objective.startDate) && !date.isAfter(objective.endDate) } == true
                }
            }
            .count()
    }

    private fun progressUnits(
        items: List<TrackedMedia>,
        objective: Objective,
    ): Int {
        return items
            .asSequence()
            .filter { trackedMedia -> matchesMediaType(trackedMedia.item.type, objective.mediaType) }
            .flatMap { trackedMedia -> trackedMedia.sessions.asSequence() }
            .sumOf { session ->
                var previousValue = 0
                session.progressUpdates
                    .sortedWith(
                        compareBy<com.nilpo.contenttracker.core.model.ProgressUpdate> { it.loggedAt }
                            .thenBy { it.createdAtEpochMillis }
                            .thenBy { it.id },
                    )
                    .sumOf { update ->
                        val delta = (update.progressValue - previousValue).coerceAtLeast(0)
                        previousValue = update.progressValue
                        if (update.countsTowardObjectives && update.hasKnownDate && objective.contains(update.loggedAt)) delta else 0
                    }
            }
    }

    private fun matchesMediaType(actual: MediaType, selected: MediaType?): Boolean =
        selected == null || actual == selected

    private fun Objective.contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)
}
