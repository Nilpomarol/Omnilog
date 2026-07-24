package com.nilpo.contenttracker.core.repository

import org.json.JSONArray

/** AniList popularity is a list count; rating votes are the score-distribution amounts. */
internal fun JSONArray.scoreDistributionVoteCount(): Int? {
    var total = 0L
    for (index in 0 until length()) {
        val amount = optJSONObject(index)?.optLong("amount", 0L)?.coerceAtLeast(0L) ?: 0L
        total = (total + amount).coerceAtMost(Int.MAX_VALUE.toLong())
    }
    return total.takeIf { it > 0L }?.toInt()
}
