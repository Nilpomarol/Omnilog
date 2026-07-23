package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.database.entity.MediaItemEntity
import com.nilpo.contenttracker.core.database.entity.TrackingSessionEntity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingStatus
import org.json.JSONArray
import java.time.LocalDate

data class MalSyncPayload(
    val status: String,
    val watchedEpisodes: Int,
    val score: Int,
    val comments: String,
    val tags: List<String>,
    val startDate: String,
    val finishDate: String,
    val isRewatching: Boolean,
    val completedRewatches: Int,
) {
    fun toFormFields(): Map<String, String> = linkedMapOf(
        "status" to status,
        "num_watched_episodes" to watchedEpisodes.toString(),
        "score" to score.toString(),
        "comments" to comments,
        "tags" to tags.joinToString(","),
        "start_date" to startDate,
        "finish_date" to finishDate,
        "is_rewatching" to isRewatching.toString(),
        "num_times_rewatched" to completedRewatches.toString(),
    )
}

fun buildMalSyncPayload(
    item: MediaItemEntity,
    sessions: List<TrackingSessionEntity>,
): MalSyncPayload? {
    if (item.type != MediaType.Anime.name) return null
    val orderedSessions = sessions.sortedBy { it.sessionNumber }
    val current = orderedSessions.lastOrNull() ?: return null
    val currentStatus = current.status.toMalStatus() ?: return null
    val isRewatching = orderedSessions.size > 1 && current.status == TrackingStatus.InProgress.name
    val completedRewatches = orderedSessions.drop(1).count { it.status == TrackingStatus.Completed.name }

    return MalSyncPayload(
        status = currentStatus,
        watchedEpisodes = current.progressCurrent.coerceAtLeast(0),
        score = current.rating?.coerceIn(1, 10) ?: 0,
        comments = current.notes.orEmpty(),
        tags = item.tagsJson.toTagList(),
        startDate = current.startedAtEpochDay.toIsoDate(),
        finishDate = current.finishedAtEpochDay.toIsoDate(),
        isRewatching = isRewatching,
        completedRewatches = completedRewatches,
    )
}

private fun String.toMalStatus(): String? = when (this) {
    TrackingStatus.Planned.name -> "plan_to_watch"
    TrackingStatus.InProgress.name -> "watching"
    TrackingStatus.Completed.name -> "completed"
    TrackingStatus.Paused.name -> "on_hold"
    TrackingStatus.Dropped.name -> "dropped"
    else -> null
}

private fun Long?.toIsoDate(): String = this?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()

private fun String?.toTagList(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        List(array.length()) { index -> array.optString(index).trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }.getOrDefault(emptyList())
}
