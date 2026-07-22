package com.nilpo.contenttracker.core.model

/** Resolves legacy MAL identities without making Jikan or AniList the canonical owner of the id. */
fun resolveMyAnimeListId(
    explicitMalId: Int?,
    metadataSource: String?,
    metadataExternalId: String?,
    popularityJson: String?,
    sourceUrl: String?,
): Int? {
    explicitMalId?.takeIf { it > 0 }?.let { return it }

    if (metadataSource == MetadataSource.Jikan.name) {
        metadataExternalId?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
    }

    popularityJson
        ?.let { MalIdJson.find(it)?.groupValues?.getOrNull(1) }
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?.let { return it }

    return sourceUrl
        ?.let { MalAnimeUrl.find(it)?.groupValues?.getOrNull(1) }
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
}

private val MalAnimeUrl = Regex("https?://(?:www\\.)?myanimelist\\.net/anime/(\\d+)")
private val MalIdJson = Regex("\"malId\"\\s*:\\s*(\\d+)")
