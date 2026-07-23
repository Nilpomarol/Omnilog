package com.nilpo.contenttracker.core.model

import java.time.LocalDate

/** Normalized inbound MAL row shared by XML exports and the authenticated account API. */
data class MyAnimeListImportItem(
    val malId: Int?,
    val title: String,
    val seriesType: String?,
    val episodeTotal: Int?,
    val watchedEpisodes: Int,
    val startedAt: LocalDate?,
    val finishedAt: LocalDate?,
    val rating: Int?,
    val status: TrackingStatus,
    val notes: String?,
    val tags: List<String>,
    val completedRewatches: Int = 0,
    val isRewatching: Boolean = false,
)
