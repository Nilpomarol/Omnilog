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
    val collectionSortOrder: Double? = null,
    val progressTotal: Int? = null,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val language: String? = null,
    val genresJson: String? = null,
    val creatorsJson: String? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val sourceUrl: String? = null,
    val externalRatingScore: Double? = null,
    val externalRatingMax: Double? = null,
    val externalRatingVoteCount: Int? = null,
    val primaryExternalRatingId: Long? = null,
    val popularityScore: Double? = null,
    val rankingPosition: Int? = null,
    val rankingLabel: String? = null,
    val providerCollectionTitle: String? = null,
    val ratingDistributionJson: String? = null,
    val popularityJson: String? = null,
    val rankingJson: String? = null,
    val metadataLastFetchedAtEpochMillis: Long? = null,
    val metadataExternalId: String? = null,
    val metadataSource: String? = null,
    val metadataOverrideFieldsCsv: String? = null,
    val isOwned: Boolean = false,
    // Retained only for compatibility with existing on-device databases. Ownership is now
    // represented exclusively by [isOwned]; this legacy column is never read by the domain layer.
    val ownershipType: String = "None",
)
