package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal fun MyAnimeListImportItem.toStagingJson(): String = JSONObject()
    .put("malId", malId)
    .put("title", title)
    .put("seriesType", seriesType)
    .put("episodeTotal", episodeTotal)
    .put("watchedEpisodes", watchedEpisodes)
    .put("startedAt", startedAt?.toString())
    .put("finishedAt", finishedAt?.toString())
    .put("rating", rating)
    .put("status", status.name)
    .put("notes", notes)
    .put("tags", JSONArray(tags))
    .put("completedRewatches", completedRewatches)
    .put("isRewatching", isRewatching)
    .toString()

internal fun String.toStagedMalImportItem(): MyAnimeListImportItem? = runCatching {
    val json = JSONObject(this)
    val malId = json.optInt("malId", 0).takeIf { it > 0 } ?: return@runCatching null
    val title = json.optString("title").trim().takeIf(String::isNotBlank) ?: return@runCatching null
    val tags = json.optJSONArray("tags")?.let { array ->
        List(array.length()) { index -> array.optString(index) }.filter(String::isNotBlank)
    }.orEmpty()
    MyAnimeListImportItem(
        malId = malId,
        title = title,
        seriesType = json.nullableString("seriesType"),
        episodeTotal = json.optInt("episodeTotal", 0).takeIf { it > 0 },
        watchedEpisodes = json.optInt("watchedEpisodes", 0).coerceAtLeast(0),
        startedAt = json.nullableString("startedAt")?.let(LocalDate::parse),
        finishedAt = json.nullableString("finishedAt")?.let(LocalDate::parse),
        rating = json.optInt("rating", 0).takeIf { it in 1..10 },
        status = runCatching { TrackingStatus.valueOf(json.optString("status")) }
            .getOrDefault(TrackingStatus.Planned),
        notes = json.nullableString("notes"),
        tags = tags,
        completedRewatches = json.optInt("completedRewatches", 0).coerceAtLeast(0),
        isRewatching = json.optBoolean("isRewatching", false),
    )
}.getOrNull()

private fun JSONObject.nullableString(key: String): String? =
    optString(key).trim().takeIf { !isNull(key) && it.isNotBlank() }
