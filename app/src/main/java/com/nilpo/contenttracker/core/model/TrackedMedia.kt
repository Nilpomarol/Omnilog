package com.nilpo.contenttracker.core.model

data class TrackedMedia(
    val item: MediaItem,
    val collection: MediaCollection? = null,
    val availableCollections: List<MediaCollection> = emptyList(),
    val sessions: List<TrackingSession>,
    val credits: List<MediaCredit> = emptyList(),
    val externalRatings: List<ExternalRating> = emptyList(),
    val externalTracking: List<ExternalTracking> = emptyList(),
) {
    val currentSession: TrackingSession?
        get() = sessions.maxByOrNull { it.sessionNumber }

    val revisitCount: Int
        get() = sessions.count { it.isRevisit }
}
