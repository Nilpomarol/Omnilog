package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "objectives")
data class ObjectiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val metric: String,
    val unit: String,
    val mediaType: String? = null,
    val targetValue: Int,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long? = null,
)
