package com.nilpo.contenttracker.core.model

enum class TrackingStatus {
    Planned,
    InProgress,
    Completed,
    Paused,
    Dropped,
}

/**
 * Whether reaching this status stops the session for good, and so gives `finishedAt` a meaning.
 *
 * Pausing is deliberately absent: it interrupts a session rather than ending one, so it must not
 * carry a finish date — the date would claim the session was over, and would sit there stale the
 * moment it was resumed.
 *
 * This decides where the session editor offers today's date, not whether one is required. A finished
 * session with no date is a legitimate thing to record: you can know you read something without
 * knowing when.
 */
val TrackingStatus.endsSession: Boolean
    get() = this == TrackingStatus.Completed || this == TrackingStatus.Dropped
