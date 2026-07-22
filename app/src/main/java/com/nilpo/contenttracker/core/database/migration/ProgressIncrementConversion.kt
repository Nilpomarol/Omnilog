package com.nilpo.contenttracker.core.database.migration

/**
 * Converts one session's cumulative progress rows into the increment model described in
 * `docs/omnilog-activity-concept.md`.
 *
 * Kept as a pure function rather than written as SQL inside the migration so it can be tested
 * without a device: the migration reads rows, hands them here, and writes back what it gets.
 *
 * The invariant callers depend on is that `baselineProgress + sum(entries)` equals what the
 * cumulative model reported as the session's progress — the chronologically last row's value.
 */

/** A row of `progress_updates` as it exists before migration 19→20, where values are cumulative. */
data class LegacyProgressRow(
    val id: Long,
    val progressValue: Int,
    val loggedAtEpochDay: Long,
    val createdAtEpochMillis: Long,
    val hasKnownDate: Boolean,
    val countsTowardObjectives: Boolean,
)

/**
 * A surviving row, rewritten as an increment.
 *
 * [loggedAtEpochDay] is carried here rather than read back from the source row, because the
 * conversion can move it: a row whose date was the day it was typed is re-dated to the session's
 * finish day. Writers must use this value, not the original.
 */
data class ConvertedEntry(
    val id: Long,
    val amount: Int,
    val loggedAtEpochDay: Long,
    val hasKnownDate: Boolean,
)

data class ConvertedSession(
    val baselineProgress: Int,
    val entries: List<ConvertedEntry>,
    val deletedRowIds: List<Long>,
)

/**
 * Repairs the dates of rows the old model flagged as not counting.
 *
 * That flag never meant "this progress is not real". It was set whenever a row was written after the
 * session was already marked complete, which happened both when adding a book finished years ago and
 * when simply logging a day's reading a day late. What the flag actually recorded is that the row's
 * date is the day it was typed rather than the day it was read.
 *
 * So the date is what gets fixed. A session with a finish date has an honest date available: a row
 * stamped after the finish belongs on the finish day, and a row stamped at or before it already has
 * a date worth trusting — those are real logged sittings that happened to be entered late. Either
 * way the row becomes an ordinary entry.
 *
 * Only a session with no finish date has nothing to date the progress by. That is the genuine
 * baseline case: something added part-way through, where the reading really does predate tracking.
 */
private fun List<LegacyProgressRow>.repairFlaggedDates(
    finishedAtEpochDay: Long?,
): List<LegacyProgressRow> {
    if (finishedAtEpochDay == null) return this
    return map { row ->
        when {
            row.countsTowardObjectives -> row
            row.loggedAtEpochDay > finishedAtEpochDay -> row.copy(
                loggedAtEpochDay = finishedAtEpochDay,
                countsTowardObjectives = true,
            )
            else -> row.copy(countsTowardObjectives = true)
        }
    }
}

private val RowOrder = compareBy<LegacyProgressRow>(
    { it.loggedAtEpochDay },
    { it.createdAtEpochMillis },
    { it.id },
)

fun convertSessionToIncrements(
    rows: List<LegacyProgressRow>,
    finishedAtEpochDay: Long? = null,
): ConvertedSession {
    if (rows.isEmpty()) return ConvertedSession(0, emptyList(), emptyList())

    val ordered = rows.repairFlaggedDates(finishedAtEpochDay).sortedWith(RowOrder)

    // Catch-up rows written before any real logging are where the user already was when tracking
    // started. That is a position, not a sitting, so it moves to the session and the rows go.
    // Catch-up rows appearing later are not baselines — logging had already begun — and fall
    // through to the walk below.
    val leadingCatchUp = ordered.takeWhile { !it.countsTowardObjectives }
    val baseline = leadingCatchUp.lastOrNull()?.progressValue ?: 0

    val deleted = leadingCatchUp.map { it.id }.toMutableList()
    val entries = mutableListOf<ConvertedEntry>()

    var previous = baseline
    ordered.drop(leadingCatchUp.size).forEach { row ->
        val amount = row.progressValue - previous
        previous = row.progressValue
        if (amount <= 0) {
            // A correction, not a sitting. The concept says corrections leave no trace.
            deleted += row.id
            return@forEach
        }
        entries += ConvertedEntry(
            id = row.id,
            amount = amount,
            loggedAtEpochDay = row.loggedAtEpochDay,
            // A catch-up row is stamped with the day it was written rather than the day the
            // consumption happened, so its date is fiction and must not survive as fact. Undated
            // rows are already excluded from objectives, which is the exclusion the
            // `countsTowardObjectives` flag was providing.
            hasKnownDate = row.hasKnownDate && row.countsTowardObjectives,
        )
    }

    val target = (ordered.last().progressValue - baseline).coerceAtLeast(0)
    return ConvertedSession(
        baselineProgress = baseline,
        entries = entries.trimmedTo(target, deleted),
        deletedRowIds = deleted,
    )
}

/**
 * Reduces entries from the end until they sum to [target].
 *
 * Needed when the session's last row corrected the total downwards. Dropping that row alone would
 * leave the earlier increments claiming progress the user has since revoked, so the surplus is taken
 * off the most recent entries first — the correction supersedes what came immediately before it.
 */
private fun List<ConvertedEntry>.trimmedTo(
    target: Int,
    deleted: MutableList<Long>,
): List<ConvertedEntry> {
    var surplus = sumOf { it.amount } - target
    if (surplus <= 0) return this

    val trimmed = ArrayDeque<ConvertedEntry>()
    for (entry in asReversed()) {
        when {
            surplus <= 0 -> trimmed.addFirst(entry)
            surplus >= entry.amount -> {
                surplus -= entry.amount
                deleted += entry.id
            }
            else -> {
                trimmed.addFirst(entry.copy(amount = entry.amount - surplus))
                surplus = 0
            }
        }
    }
    return trimmed.toList()
}
