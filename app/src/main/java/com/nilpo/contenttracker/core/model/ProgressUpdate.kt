package com.nilpo.contenttracker.core.model

import java.time.LocalDate

data class ProgressUpdate(
    val id: Long,
    val mediaItemId: Long,
    val sessionId: Long,
    val progressValue: Int,
    val loggedAt: LocalDate,
    val createdAtEpochMillis: Long = 0,
)
