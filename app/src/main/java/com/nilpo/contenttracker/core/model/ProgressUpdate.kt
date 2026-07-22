package com.nilpo.contenttracker.core.model

import java.time.LocalDate

/**
 * One logged sitting on a session. See `docs/omnilog-activity-concept.md`.
 *
 * [amount] is what was consumed on this occasion, not a running total, and is always positive —
 * consuming less than recorded is a correction to an entry, not an entry of its own.
 */
data class ProgressUpdate(
    val id: Long,
    val mediaItemId: Long,
    val sessionId: Long,
    val amount: Int,
    /**
     * When this was logged. Not a claim about when the consumption happened: logging is irregular,
     * so the only thing this date can always support is the moment the user wrote it down.
     */
    val loggedAt: LocalDate,
    val hasKnownDate: Boolean = true,
    val createdAtEpochMillis: Long = 0,
    /**
     * Whether this entry accumulated over a span rather than in one sitting. When set, the span is
     * derived from the previous entry in the session rather than stored.
     */
    val coversPeriod: Boolean = false,
)
