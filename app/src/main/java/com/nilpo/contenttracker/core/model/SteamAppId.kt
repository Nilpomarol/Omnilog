package com.nilpo.contenttracker.core.model

import org.json.JSONObject

fun normalizeSteamAppId(value: String?): String? {
    return value
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.any { digit -> digit != '0' } }
}

fun steamAppIdFromMetadataJson(json: String?): String? {
    return runCatching {
        normalizeSteamAppId(
            JSONObject(json ?: return@runCatching null)
                .optJSONObject("steam")
                ?.optString("appId"),
        )
    }.getOrNull()
}

fun metadataJsonWithSteamAppId(json: String?, steamAppId: String?): String? {
    val normalizedId = normalizeSteamAppId(steamAppId)
    val root = runCatching { JSONObject(json ?: "{}") }.getOrElse { JSONObject() }
    val steam = root.optJSONObject("steam") ?: JSONObject()

    if (normalizedId == null) {
        steam.remove("appId")
    } else {
        steam.put("appId", normalizedId)
    }

    if (steam.length() == 0) {
        root.remove("steam")
    } else {
        root.put("steam", steam)
    }
    return root.takeIf { it.length() > 0 }?.toString()
}
