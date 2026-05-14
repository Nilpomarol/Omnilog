package com.nilpo.contenttracker.core.model

data class TrackedMedia(
    val item: MediaItem,
    val sessions: List<TrackingSession>,
    val externalRatings: List<ExternalRating> = emptyList(),
    val externalTracking: List<ExternalTracking> = emptyList(),
) {
    val currentSession: TrackingSession?
        get() = sessions.maxByOrNull { it.sessionNumber }

    val revisitCount: Int
        get() = sessions.count { it.isRevisit }
}
