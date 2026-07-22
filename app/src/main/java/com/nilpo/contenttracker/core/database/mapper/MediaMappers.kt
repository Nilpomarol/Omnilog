package com.nilpo.contenttracker.core.database.mapper

import com.nilpo.contenttracker.core.database.entity.ExternalRatingEntity
import com.nilpo.contenttracker.core.database.entity.MediaCollectionEntity
import com.nilpo.contenttracker.core.database.entity.MediaCreditEntity
import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.ProgressUpdateEntity
import com.nilpo.contenttracker.core.database.entity.SessionStatusEventEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.ConsumptionPlatform
import com.nilpo.contenttracker.core.model.ConsumptionPlatformType
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingOrigin
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.metadataJsonWithSteamAppId
import com.nilpo.contenttracker.core.model.steamAppIdFromMetadataJson
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun MediaItemEntity.toDomain(): MediaItem {
    return MediaItem(
        id = id,
        type = enumValueOrDefault(type, MediaType.Anime),
        title = title,
        collectionId = collectionId,
        collectionSortOrder = collectionSortOrder,
        progressTotal = progressTotal,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        language = language,
        genres = genresJson.toStringList(),
        creators = creatorsJson.toStringList(),
        coverUrl = coverUrl,
        synopsis = synopsis,
        sourceUrl = sourceUrl,
        steamAppId = steamAppIdFromMetadataJson(popularityJson),
        externalRatingScore = externalRatingScore,
        externalRatingMax = externalRatingMax,
        externalRatingVoteCount = externalRatingVoteCount,
        primaryExternalRatingId = primaryExternalRatingId,
        popularityScore = popularityScore,
        rankingPosition = rankingPosition,
        rankingLabel = rankingLabel,
        providerCollectionTitle = providerCollectionTitle,
        ratingDistributionJson = ratingDistributionJson,
        popularityJson = popularityJson,
        rankingJson = rankingJson,
        metadataLastFetchedAtEpochMillis = metadataLastFetchedAtEpochMillis,
        metadataExternalId = metadataExternalId,
        metadataSource = enumValueOrNull<MetadataSource>(metadataSource),
        isOwned = isOwned,
    )
}

fun MediaItem.toEntity(): MediaItemEntity {
    return MediaItemEntity(
        id = id,
        type = type.name,
        title = title,
        collectionId = collectionId,
        collectionSortOrder = collectionSortOrder,
        progressTotal = progressTotal,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        language = language,
        genresJson = genres.toJsonArrayString(),
        creatorsJson = creators.toJsonArrayString(),
        coverUrl = coverUrl,
        synopsis = synopsis,
        sourceUrl = sourceUrl,
        externalRatingScore = externalRatingScore,
        externalRatingMax = externalRatingMax,
        externalRatingVoteCount = externalRatingVoteCount,
        primaryExternalRatingId = primaryExternalRatingId,
        popularityScore = popularityScore,
        rankingPosition = rankingPosition,
        rankingLabel = rankingLabel,
        providerCollectionTitle = providerCollectionTitle,
        ratingDistributionJson = ratingDistributionJson,
        popularityJson = metadataJsonWithSteamAppId(popularityJson, steamAppId),
        rankingJson = rankingJson,
        metadataLastFetchedAtEpochMillis = metadataLastFetchedAtEpochMillis,
        metadataExternalId = metadataExternalId,
        metadataSource = metadataSource?.name,
        isOwned = isOwned,
    )
}

fun MediaCreditEntity.toDomain(): MediaCredit {
    return MediaCredit(
        id = id,
        mediaItemId = mediaItemId,
        personName = personName,
        roleType = enumValueOrDefault(roleType, MediaCreditRole.Cast),
        characterName = characterName,
        sortOrder = sortOrder,
        metadataSource = enumValueOrNull<MetadataSource>(metadataSource),
    )
}

fun MediaCredit.toEntity(mediaItemIdOverride: Long? = null): MediaCreditEntity {
    return MediaCreditEntity(
        id = id,
        mediaItemId = mediaItemIdOverride ?: mediaItemId,
        personName = personName,
        roleType = roleType.name,
        characterName = characterName,
        sortOrder = sortOrder,
        metadataSource = metadataSource?.name,
    )
}

fun MediaCollectionEntity.toDomain(): MediaCollection {
    return MediaCollection(
        id = id,
        name = name,
    )
}

fun MediaCollection.toEntity(): MediaCollectionEntity {
    return MediaCollectionEntity(
        id = id,
        name = name,
    )
}

fun ProgressUpdateEntity.toDomain(): ProgressUpdate {
    return ProgressUpdate(
        id = id,
        mediaItemId = mediaItemId,
        sessionId = sessionId,
        amount = amount,
        loggedAt = LocalDate.ofEpochDay(loggedAtEpochDay),
        hasKnownDate = hasKnownDate,
        createdAtEpochMillis = createdAtEpochMillis,
        coversPeriod = coversPeriod,
    )
}

/**
 * The stored day when there is one, otherwise the day the row was written.
 *
 * Rows predating the editable date have no day of their own, so the recorded instant stands in for
 * it — which is what the app showed for them before either way. Resolved at the mapper boundary so
 * the domain model keeps a plain [LocalDate] and nothing downstream has to know about zones.
 */
fun SessionStatusEventEntity.toDomain(): SessionStatusEvent = SessionStatusEvent(
    id = id,
    sessionId = sessionId,
    previousStatus = enumValueOrNull<TrackingStatus>(previousStatus),
    status = enumValueOrDefault(status, TrackingStatus.Planned),
    occurredOn = occurredOnEpochDay?.let(LocalDate::ofEpochDay)
        ?: Instant.ofEpochMilli(createdAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
    createdAtEpochMillis = createdAtEpochMillis,
)

fun TrackingSessionEntity.toDomain(
    progressUpdates: List<ProgressUpdateEntity> = emptyList(),
    statusEvents: List<SessionStatusEventEntity> = emptyList(),
): TrackingSession {
    return TrackingSession(
        id = id,
        mediaItemId = mediaItemId,
        sessionNumber = sessionNumber,
        status = enumValueOrDefault(status, TrackingStatus.Planned),
        progressCurrent = progressCurrent,
        baselineProgress = baselineProgress,
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
        updatedAtEpochMillis = updatedAtEpochMillis,
        progressUpdates = progressUpdates
            .filter { update -> update.sessionId == id }
            .sortedWith(compareBy<ProgressUpdateEntity> { it.loggedAtEpochDay }.thenBy { it.createdAtEpochMillis })
            .map { it.toDomain() },
        // Oldest first. Ordered by the instant rather than the day, so two transitions made minutes
        // apart keep their sequence — which is the whole point of a log.
        statusEvents = statusEvents
            .filter { event -> event.sessionId == id }
            .sortedWith(
                compareBy<SessionStatusEventEntity> { it.createdAtEpochMillis }.thenBy { it.id },
            )
            .map { it.toDomain() },
    )
}

fun TrackingSession.toEntity(): TrackingSessionEntity {
    return TrackingSessionEntity(
        id = id,
        mediaItemId = mediaItemId,
        sessionNumber = sessionNumber,
        status = status.name,
        progressCurrent = progressCurrent,
        baselineProgress = baselineProgress,
        rating = rating,
        notes = notes,
        platformName = platform?.name,
        platformType = platform?.type?.name,
        startedAtEpochDay = startedAt?.toEpochDay(),
        finishedAtEpochDay = finishedAt?.toEpochDay(),
        updatedAtEpochMillis = updatedAtEpochMillis,
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
        scoreDescriptor = scoreDescriptor,
        origin = enumValueOrDefault(origin, ExternalRatingOrigin.Provider),
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
        scoreDescriptor = scoreDescriptor,
        origin = origin.name,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T,
): T {
    return enumValueOrNull<T>(value) ?: default
}

private inline fun <reified T : Enum<T>> enumValueOrNull(
    value: String?,
): T? {
    return value?.let { enumValue ->
        enumValues<T>().firstOrNull { it.name == enumValue }
    }
}

private fun String?.toStringList(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}

private fun List<String>.toJsonArrayString(): String? {
    if (isEmpty()) return null
    return JSONArray(this).toString()
}
