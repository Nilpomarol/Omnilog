package com.nilpo.contenttracker.core.activity

import com.nilpo.contenttracker.core.model.ProgressUpdate
import java.time.LocalDate

/**
 * The span an entry accumulated over, when it did not happen in one sitting.
 *
 * [from] is exclusive in spirit rather than in arithmetic: it is the date of the previous entry, so
 * the progress happened somewhere after that and up to [to]. It is the honest bound available, not
 * a claim that reading occurred on every day between.
 */
data class ActivityWindow(
    val from: LocalDate,
    val to: LocalDate,
)

/**
 * Derives the coverage window for each entry that claims to cover a period.
 *
 * Nothing is stored. A period entry's window runs back to the previous dated entry in the session,
 * or to the session's start when it is the first — which is exactly when the baseline was true. This
 * is why the concept never asks the user for a second date: the sequence already contains it.
 *
 * Only entries with [ProgressUpdate.coversPeriod] get a window, and only when there is a real span
 * to show. An entry dated the same day as the one before it covers no period worth printing, so it
 * is omitted rather than rendered as a window of zero length.
 *
 * Undated entries never get a window: with no end date there is nothing to measure back from.
 */
fun activityWindows(
    updates: List<ProgressUpdate>,
    sessionStartedAt: LocalDate?,
): Map<Long, ActivityWindow> {
    val dated = updates
        .filter { it.hasKnownDate }
        .sortedWith(compareBy({ it.loggedAt }, { it.createdAtEpochMillis }, { it.id }))

    val windows = mutableMapOf<Long, ActivityWindow>()
    dated.forEachIndexed { index, update ->
        if (!update.coversPeriod) return@forEachIndexed
        val from = dated.getOrNull(index - 1)?.loggedAt ?: sessionStartedAt ?: return@forEachIndexed
        if (!from.isBefore(update.loggedAt)) return@forEachIndexed
        windows[update.id] = ActivityWindow(from = from, to = update.loggedAt)
    }
    return windows
}
