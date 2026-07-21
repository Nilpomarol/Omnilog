package com.nilpo.contenttracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "external_ratings",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaItemId")],
)
data class ExternalRatingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaItemId: Long,
    val source: String,
    val score: Double,
    val maxScore: Double,
    val voteCount: Int? = null,
    val scoreDescriptor: String? = null,
    val origin: String = "Provider",
)
