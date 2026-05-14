package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val externalId: String? = null,
    val sourceApi: String? = null,
    val isOwned: Boolean = false,
    val ownershipType: String = "None",
)
