package com.nilpo.contenttracker.core.model

data class TrackedMedia(
    val item: MediaItem,
    val sessions: List<TrackingSession>,
    val seasonProgress: List<SeasonProgress> = emptyList(),
    val externalRatings: List<ExternalRating> = emptyList(),
    val externalTracking: List<ExternalTracking> = emptyList(),
) {
    val currentSession: TrackingSession?
        get() = sessions.maxByOrNull { it.sessionNumber }

    val revisitCount: Int
        get() = sessions.count { it.isRevisit }

    fun seasonsFor(session: TrackingSession): List<SeasonProgress> {
        return seasonProgress
            .filter { it.trackingSessionId == session.id }
            .sortedBy { it.seasonNumber }
    }
}
