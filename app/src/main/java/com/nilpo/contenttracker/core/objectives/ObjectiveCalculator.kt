package com.nilpo.contenttracker.core.objectives

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.completionDates
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
    ): Int = items.count { trackedMedia ->
        matchesMediaType(trackedMedia.item.type, objective.mediaType) &&
            trackedMedia.latestContributionTo(objective) != null
    }

    private fun TrackedMedia.latestContributionTo(objective: Objective): LocalDate? = when (objective.metric) {
        ObjectiveMetric.CompletedTitles -> sessions
            .flatMap { session -> session.completionDates() }
            .filterNotNull()
            .filter { date -> objective.contains(date) }
            .maxOrNull()
        ObjectiveMetric.ProgressUnits -> sessions
            .flatMap { session -> session.progressUpdates }
            .filter { update -> update.hasKnownDate && update.amount > 0 && objective.contains(update.loggedAt) }
            .maxOfOrNull { update -> update.loggedAt }
    }

    private fun progressUnits(
        items: List<TrackedMedia>,
        objective: Objective,
    ): Int {
        return items
            .asSequence()
            .filter { trackedMedia -> matchesMediaType(trackedMedia.item.type, objective.mediaType) }
            .flatMap { trackedMedia -> trackedMedia.sessions.asSequence() }
            // Entries are already increments, so there is nothing to derive: an entry counts in
            // full or not at all. Undated entries are skipped because an objective is a claim about
            // a period, and an entry with no date cannot be placed in one.
            .sumOf { session ->
                session.progressUpdates.sumOf { update ->
                    if (update.hasKnownDate && objective.contains(update.loggedAt)) update.amount else 0
                }
            }
    }

    private fun matchesMediaType(actual: MediaType, selected: MediaType?): Boolean =
        selected == null || actual == selected

    private fun Objective.contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)
}
