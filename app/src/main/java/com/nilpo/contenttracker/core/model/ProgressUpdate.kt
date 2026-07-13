package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class ProgressUpdate(
    val id: Long,
    val mediaItemId: Long,
    val sessionId: Long,
    val progressValue: Int,
    val loggedAt: LocalDate,
    val hasKnownDate: Boolean = true,
    val createdAtEpochMillis: Long = 0,
    val countsTowardObjectives: Boolean = true,
)
