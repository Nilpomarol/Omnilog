package com.nilpo.contenttracker.core.model

data class TrackedMedia(
    val item: MediaItem,
    val collection: MediaCollection? = null,
    val availableCollections: List<MediaCollection> = emptyList(),
    val sessions: List<TrackingSession>,
    val credits: List<MediaCredit> = emptyList(),
    val externalRatings: List<ExternalRating> = emptyList(),
) {
    /**
     * Sessions in the order they were lived through. Deleting a past session leaves the
     * survivors' [TrackingSession.sessionNumber] untouched, so the numbers can be sparse and
     * need not start at 1 — position here is the only trustworthy ordering.
     */
    val orderedSessions: List<TrackingSession> by lazy {
        sessions.sortedBy { it.sessionNumber }
    }

    val currentSession: TrackingSession?
        get() = orderedSessions.lastOrNull()

    val primaryExternalRating: ExternalRating?
        get() {
            item.primaryExternalRatingId?.let { primaryId ->
                externalRatings.firstOrNull { it.id == primaryId }?.let { return it }
            }

            val score = item.externalRatingScore ?: return null
            val maxScore = item.externalRatingMax ?: return null
            return externalRatings.firstOrNull { rating ->
                kotlin.math.abs(rating.score - score) < 0.001 &&
                    kotlin.math.abs(rating.maxScore - maxScore) < 0.001
            }
        }

    private val revisitSessionIds: Set<Long> by lazy {
        orderedSessions.drop(1).map { it.id }.toSet()
    }

    val revisitCount: Int
        get() = orderedSessions.count(::isRevisit)

    /**
     * Which time through this title [session] was, counting from 1. Derived from position rather
     * than from the stored number so that deleting a mistaken first session demotes the survivor
     * back to a first visit instead of stranding it at "2a vegada".
     */
    fun visitNumber(session: TrackingSession): Int {
        val index = orderedSessions.indexOfFirst { it.id == session.id }
        return if (index < 0) session.sessionNumber else index + 1
    }

    /**
     * Whether [session] represents a revisit that has actually begun. A later planned session is
     * structurally the next visit, but planning it is not the same as having revisited the title.
     */
    fun isRevisit(session: TrackingSession): Boolean =
        session.id in revisitSessionIds && session.status != TrackingStatus.Planned
}
