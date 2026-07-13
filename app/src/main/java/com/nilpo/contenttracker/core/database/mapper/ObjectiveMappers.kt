package com.nilpo.contenttracker.core.database.mapper

import com.nilpo.contenttracker.core.database.entity.ObjectiveEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import java.time.LocalDate

fun ObjectiveEntity.toDomain(): Objective {
    return Objective(
        id = id,
        name = name,
        metric = enumValueOrDefault(metric, ObjectiveMetric.CompletedTitles),
        unit = enumValueOrDefault(unit, ObjectiveUnit.Titles),
        mediaType = enumValueOrNull<MediaType>(mediaType),
        targetValue = targetValue,
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        endDate = LocalDate.ofEpochDay(endDateEpochDay),
        createdAtEpochMillis = createdAtEpochMillis,
        archivedAtEpochMillis = archivedAtEpochMillis,
    )
}

fun Objective.toEntity(): ObjectiveEntity {
    return ObjectiveEntity(
        id = id,
        name = name,
        metric = metric.name,
        unit = unit.name,
        mediaType = mediaType?.name,
        targetValue = targetValue,
        startDateEpochDay = startDate.toEpochDay(),
        endDateEpochDay = endDate.toEpochDay(),
        createdAtEpochMillis = createdAtEpochMillis,
        archivedAtEpochMillis = archivedAtEpochMillis,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
    enumValueOrNull<T>(value) ?: default

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
    value?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } }
