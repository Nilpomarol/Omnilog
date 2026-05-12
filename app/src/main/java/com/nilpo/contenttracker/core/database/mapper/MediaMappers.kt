package com.nilpo.contenttracker.core.database.mapper

import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.ExternalTrackingEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.SeasonProgressEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.ConsumptionPlatform
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.ExternalTracking
import com.nilpo.contenttracker.core.model.ExternalTrackingSource
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Ownership
import com.nilpo.contenttracker.core.model.OwnershipType
import com.nilpo.contenttracker.core.model.SeasonProgress
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

fun MediaItemEntity.toDomain(): MediaItem {
    return MediaItem(
        id = id,
        type = enumValueOrDefault(type, MediaType.Anime),
        title = title,
        coverUrl = coverUrl,
        synopsis = synopsis,
        externalId = externalId,
        sourceApi = sourceApi,
        ownership = Ownership(
            isOwned = isOwned,
            type = enumValueOrDefault(ownershipType, OwnershipType.None),
        ),
    )
}

fun MediaItem.toEntity(): MediaItemEntity {
    return MediaItemEntity(
        id = id,
        type = type.name,
        title = title,
        coverUrl = coverUrl,
        synopsis = synopsis,
        externalId = externalId,
        sourceApi = sourceApi,
        isOwned = ownership.isOwned,
        ownershipType = ownership.type.name,
    )
}

fun TrackingSessionEntity.toDomain(): TrackingSession {
    return TrackingSession(
        id = id,
        mediaItemId = mediaItemId,
        sessionNumber = sessionNumber,
        status = enumValueOrDefault(status, TrackingStatus.Planned),
        progressCurrent = progressCurrent,
        progressTotal = progressTotal,
        rating = rating,
        notes = notes,
        platform = platformName?.let { name ->
            ConsumptionPlatform(
                name = name,
                type = enumValueOrDefault(platformType, ConsumptionPlatformType.Other),
            )
        },
        startedAt = startedAtEpochDay?.let(LocalDate::ofEpochDay),
        finishedAt = finishedAtEpochDay?.let(LocalDate::ofEpochDay),
    )
}

fun TrackingSession.toEntity(): TrackingSessionEntity {
    return TrackingSessionEntity(
        id = id,
        mediaItemId = mediaItemId,
        sessionNumber = sessionNumber,
        status = status.name,
        progressCurrent = progressCurrent,
        progressTotal = progressTotal,
        rating = rating,
        notes = notes,
        platformName = platform?.name,
        platformType = platform?.type?.name,
        startedAtEpochDay = startedAt?.toEpochDay(),
        finishedAtEpochDay = finishedAt?.toEpochDay(),
    )
}

fun SeasonProgressEntity.toDomain(): SeasonProgress {
    return SeasonProgress(
        id = id,
        trackingSessionId = trackingSessionId,
        seasonNumber = seasonNumber,
        progressCurrent = progressCurrent,
        progressTotal = progressTotal,
    )
}

fun SeasonProgress.toEntity(): SeasonProgressEntity {
    return SeasonProgressEntity(
        id = id,
        trackingSessionId = trackingSessionId,
        seasonNumber = seasonNumber,
        progressCurrent = progressCurrent,
        progressTotal = progressTotal,
    )
}

fun ExternalRatingEntity.toDomain(): ExternalRating {
    return ExternalRating(
        id = id,
        mediaItemId = mediaItemId,
        source = enumValueOrDefault(source, ExternalRatingSource.Tmdb),
        score = score,
        maxScore = maxScore,
        voteCount = voteCount,
    )
}

fun ExternalRating.toEntity(): ExternalRatingEntity {
    return ExternalRatingEntity(
        id = id,
        mediaItemId = mediaItemId,
        source = source.name,
        score = score,
        maxScore = maxScore,
        voteCount = voteCount,
    )
}

fun ExternalTrackingEntity.toDomain(): ExternalTracking {
    return ExternalTracking(
        id = id,
        mediaItemId = mediaItemId,
        source = enumValueOrDefault(source, ExternalTrackingSource.Other),
        externalItemId = externalItemId,
        url = url,
        isSynced = isSynced,
    )
}

fun ExternalTracking.toEntity(): ExternalTrackingEntity {
    return ExternalTrackingEntity(
        id = id,
        mediaItemId = mediaItemId,
        source = source.name,
        externalItemId = externalItemId,
        url = url,
        isSynced = isSynced,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T,
): T {
    return value?.let { enumValue ->
        enumValues<T>().firstOrNull { it.name == enumValue }
    } ?: default
}
