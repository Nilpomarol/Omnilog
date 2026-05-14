package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = MediaCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("collectionId")],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val collectionId: Long? = null,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val externalId: String? = null,
    val sourceApi: String? = null,
    val isOwned: Boolean = false,
    val ownershipType: String = "None",
)
