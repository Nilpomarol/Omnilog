package com.nilpo.contenttracker.core.model

data class SeasonProgress(
    val id: Long,
    val trackingSessionId: Long,
    val seasonNumber: Int,
    val progressCurrent: Int,
    val progressTotal: Int? = null,
)
