package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AniListMetadataRepository(
    private val malClientId: String = "",
) : MetadataRepository {
    private val jikanRequestMutex = Mutex()
    private var lastJikanRequestAtEpochMillis: Long = 0L

    override suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (MediaType.Anime !in request.mediaTypes || query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val response = postGraphQL(
                SEARCH_QUERY,
                JSONObject().apply {
                    put("search", query)
                    put("perPage", 10)
                },
            )
            val mediaArray = response
                .getJSONObject("data")
                .getJSONObject("Page")
                .getJSONArray("media")
            List(mediaArray.length()) { mediaArray.getJSONObject(it) }
                .mapNotNull { it.toMetadataSuggestion() }
        }
    }

    override suspend fun getSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        if (suggestion.source == MetadataSource.Jikan) {
            return getJikanSuggestionDetails(suggestion)
        }
        if (suggestion.source != MetadataSource.AniList) return suggestion
        val suggestionMalId = suggestion.malId

        // Failures propagate: the caller reports them and offers a retry, rather than silently
        // handing back the un-enriched suggestion as if the details had loaded.
        return withContext(Dispatchers.IO) {
            val aniListId = suggestion.externalId.toLongOrNull() ?: return@withContext suggestion
            val detailed = postGraphQL(
                    DETAILS_QUERY,
                JSONObject().apply { put("id", aniListId) },
            )
                .getJSONObject("data")
                .getJSONObject("Media")
                .toMetadataSuggestion()
                ?: suggestion
            val malId = detailed.malId ?: suggestionMalId

            val officialMalDetails = malId?.let { id ->
                getOfficialMalAnimeDetails(id)
            }
            val officialMalRating = officialMalDetails?.toOfficialMalRating()
            val jikanDetails = if (officialMalRating == null) malId?.let { id ->
                getJikanJson("https://api.jikan.moe/v4/anime/$id")
                    .optJSONObject("data")
            } else {
                null
            }
            val malRating = officialMalRating ?: jikanDetails?.toJikanMalRating()
            val externalRatings = (listOfNotNull(malRating) + suggestion.externalRatings + detailed.externalRatings)
                .distinctBy { it.source }
            val malRank = officialMalDetails?.optInt("rank", 0)?.takeIf { it > 0 }

            detailed.copy(
                malId = malId,
                popularityJson = detailed.popularityJson.withMalId(malId),
                externalRating = malRating?.let {
                    MetadataRatingSuggestion(
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    )
                } ?: detailed.externalRating ?: suggestion.externalRating,
                externalRatings = externalRatings,
                rankingPosition = malRank ?: detailed.rankingPosition,
                rankingLabel = malRank?.let { "MAL rank" } ?: detailed.rankingLabel,
            )
        }
    }

    private suspend fun getJikanSuggestionDetails(suggestion: MetadataSuggestion): MetadataSuggestion {
        return withContext(Dispatchers.IO) {
            val malId = suggestion.externalId.toIntOrNull() ?: return@withContext suggestion
            val officialMalDetails = getOfficialMalAnimeDetails(malId)
            val baseWithOfficialMal = officialMalDetails
                ?.toOfficialMalMetadataSuggestion(suggestion)
                ?.withOfficialMalMetrics(officialMalDetails)
                ?: suggestion
            getAniListSuggestionByMalId(malId)
                ?.toMalBackedSuggestion(baseWithOfficialMal, malId)
                ?.withOfficialMalMetrics(officialMalDetails)
                ?: if (officialMalDetails != null) {
                    baseWithOfficialMal
                } else {
                    getJikanJson("https://api.jikan.moe/v4/anime/$malId")
                        .optJSONObject("data")
                        ?.toJikanMetadataSuggestion(baseWithOfficialMal)
                        ?: baseWithOfficialMal
                }
        }
    }

    private fun getAniListSuggestionByMalId(malId: Int): MetadataSuggestion? = runCatching {
        postGraphQL(
            MAL_DETAILS_QUERY,
            JSONObject().apply { put("malId", malId) },
        )
            .optJSONObject("data")
            ?.optJSONObject("Media")
            ?.toMetadataSuggestion()
    }.getOrNull()

    private fun getOfficialMalAnimeDetails(malId: Int): JSONObject? {
        val clientId = malClientId.trim().takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val connection = URL("https://api.myanimelist.net/v2/anime/$malId?fields=$MAL_DETAIL_FIELDS")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-MAL-CLIENT-ID", clientId)
            if (connection.responseCode !in 200..299) {
                return@runCatching null
            }
            JSONObject(connection.inputStream.bufferedReader().readText())
        }.getOrNull()
    }

    private suspend fun getJikanJson(url: String): JSONObject = jikanRequestMutex.withLock {
        val elapsed = System.currentTimeMillis() - lastJikanRequestAtEpochMillis
        if (elapsed < JIKAN_MIN_REQUEST_INTERVAL_MILLIS) {
            delay(JIKAN_MIN_REQUEST_INTERVAL_MILLIS - elapsed)
        }
        try {
            // Rate limits propagate to the import worker, which schedules durable delayed work.
            // Waiting here could hold one worker in memory for minutes or hours.
            getJson(url)
        } finally {
            lastJikanRequestAtEpochMillis = System.currentTimeMillis()
        }
    }

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val malId = optInt("idMal", 0).takeIf { it > 0 }
        val titleObj = optJSONObject("title") ?: return null
        val titleEnglish = titleObj.optString("english").takeIf { it.isNotBlank() }
        val titleRomaji = titleObj.optString("romaji").takeIf { it.isNotBlank() } ?: return null
        val titleNative = titleObj.optString("native").takeIf { it.isNotBlank() }
        val title = titleEnglish ?: titleRomaji
        val originalTitle = titleNative?.takeIf { it != title }
            ?: titleRomaji.takeIf { titleEnglish != null && it != titleEnglish }

        val coverUrl = optJSONObject("coverImage")?.optString("large")?.takeIf { it.isNotBlank() }
        val synopsis = optString("description")
            .takeIf { it.isNotBlank() }
            ?.replace(Regex("<[^>]+>"), " ")
            ?.trim()
        val averageScore = optInt("averageScore", 0)
        val popularity = optInt("popularity", 0)
        val rankings = optJSONArray("rankings")
        val ratingDistribution = optJSONObject("stats")?.optJSONArray("scoreDistribution")
        val releaseYear = optJSONObject("startDate")?.optInt("year", 0)?.takeIf { it > 0 }
        val genres = optJSONArray("genres")?.let { arr ->
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val creators = optJSONObject("studios")
            ?.optJSONArray("nodes")
            ?.let { arr -> List(arr.length()) { arr.getJSONObject(it).optString("name") } }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val studioCredits = creators.mapIndexed { index, studio ->
            MediaCredit(
                personName = studio,
                roleType = MediaCreditRole.Studio,
                sortOrder = index,
                metadataSource = MetadataSource.AniList,
            )
        }
        val voiceCredits = optJSONObject("characters")
            ?.optJSONArray("edges")
            .toVoiceActorCredits()
        val aniListRating = if (averageScore > 0) {
            MetadataRatingSuggestion(
                score = averageScore / 10.0,
                maxScore = 10.0,
                voteCount = popularity.takeIf { it > 0 },
            )
        } else {
            null
        }

        return MetadataSuggestion(
            source = MetadataSource.AniList,
            externalId = id.toString(),
            malId = malId,
            mediaType = MediaType.Anime,
            title = title,
            originalTitle = originalTitle,
            releaseYear = releaseYear,
            coverUrl = coverUrl,
            synopsis = synopsis,
            progressTotal = optInt("episodes", 0).takeIf { it > 0 },
            genres = genres,
            creators = creators,
            credits = studioCredits + voiceCredits,
            sourceUrl = optString("siteUrl").takeIf { it.isNotBlank() },
            popularityScore = popularity.takeIf { it > 0 }?.toDouble(),
            rankingPosition = rankings.firstRank(),
            rankingLabel = rankings.firstRankLabel(),
            ratingDistributionJson = ratingDistribution?.toString(),
            popularityJson = JSONObject()
                .put("popularity", popularity)
                .apply { malId?.let { put("malId", it) } }
                .toString(),
            rankingJson = rankings?.toString(),
            externalRating = aniListRating,
            externalRatings = aniListRating?.let { rating ->
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.AniList,
                        score = rating.score,
                        maxScore = rating.maxScore,
                        voteCount = rating.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private fun postGraphQL(query: String, variables: JSONObject): JSONObject {
        val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val body = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }
        connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
        return JSONObject(connection.inputStream.bufferedReader().readText())
    }

    companion object {
        private val SEARCH_QUERY = """
            query (${'$'}search: String, ${'$'}perPage: Int) {
              Page(perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME, sort: SEARCH_MATCH) {
                  id
                  idMal
                  title { english romaji native }
                  coverImage { large }
                  description(asHtml: false)
                  episodes
                  averageScore
                  popularity
                  rankings { rank type allTime context }
                  stats { scoreDistribution { score amount } }
                  startDate { year }
                  genres
                  studios(isMain: true) { nodes { name } }
                  characters(perPage: 12, sort: ROLE) {
                    edges {
                      node { name { full } }
                      voiceActors(language: JAPANESE, sort: RELEVANCE) { name { full } }
                    }
                  }
                  siteUrl
                }
              }
            }
        """.trimIndent()

        private val DETAILS_QUERY = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                idMal
                title { english romaji native }
                coverImage { large }
                description(asHtml: false)
                episodes
                averageScore
                popularity
                rankings { rank type allTime context }
                stats { scoreDistribution { score amount } }
                startDate { year }
                genres
                studios(isMain: true) { nodes { name } }
                characters(perPage: 12, sort: ROLE) {
                  edges {
                    node { name { full } }
                    voiceActors(language: JAPANESE, sort: RELEVANCE) { name { full } }
                  }
                }
                siteUrl
              }
            }
        """.trimIndent()

        private val MAL_DETAILS_QUERY = """
            query (${'$'}malId: Int) {
              Media(idMal: ${'$'}malId, type: ANIME) {
                id
                idMal
                title { english romaji native }
                coverImage { large }
                description(asHtml: false)
                episodes
                averageScore
                popularity
                rankings { rank type allTime context }
                stats { scoreDistribution { score amount } }
                startDate { year }
                genres
                studios(isMain: true) { nodes { name } }
                characters(perPage: 12, sort: ROLE) {
                  edges {
                    node { name { full } }
                    voiceActors(language: JAPANESE, sort: RELEVANCE) { name { full } }
                  }
                }
                siteUrl
              }
            }
        """.trimIndent()

        private const val MAL_DETAIL_FIELDS =
            "title,main_picture,alternative_titles,start_date,synopsis,mean,rank,popularity," +
                "num_list_users,num_scoring_users,genres,num_episodes,studios"
        // Jikan permits short bursts, but its sustained public quota is the limiting constraint for
        // a full-library import. Staying below one request per second prevents the tail of a large
        // batch from entering a rate-limit window.
        private const val JIKAN_MIN_REQUEST_INTERVAL_MILLIS = 1_100L
    }
}

private fun String?.withMalId(malId: Int?): String? {
    if (malId == null) return this
    return runCatching {
        JSONObject(this ?: "{}")
            .put("malId", malId)
            .toString()
    }.getOrDefault(this)
}

internal fun JSONObject.toOfficialMalMetadataSuggestion(base: MetadataSuggestion): MetadataSuggestion {
    val malId = optInt("id", 0).takeIf { it > 0 } ?: base.externalId.toIntOrNull()
    val alternativeTitles = optJSONObject("alternative_titles")
    val englishTitle = alternativeTitles
        ?.optString("en")
        ?.takeIf { it.isNotBlank() }
    val japaneseTitle = alternativeTitles
        ?.optString("ja")
        ?.takeIf { it.isNotBlank() }
    val mainPicture = optJSONObject("main_picture")
    val studios = optJSONArray("studios").toNamedList()
    val genres = optJSONArray("genres").toNamedList()
    val releaseYear = optString("start_date")
        .take(4)
        .toIntOrNull()
        ?.takeIf { it > 0 }
    val canonicalMalUrl = malId?.let { "https://myanimelist.net/anime/$it" }

    return base.copy(
        source = MetadataSource.Jikan,
        externalId = malId?.toString() ?: base.externalId,
        malId = malId,
        title = englishTitle
            ?: optString("title").takeIf { it.isNotBlank() }
            ?: base.title,
        originalTitle = japaneseTitle ?: base.originalTitle,
        releaseYear = releaseYear ?: base.releaseYear,
        coverUrl = mainPicture
            ?.optString("large")
            ?.takeIf { it.isNotBlank() }
            ?: mainPicture?.optString("medium")?.takeIf { it.isNotBlank() }
            ?: base.coverUrl,
        synopsis = optString("synopsis").takeIf { it.isNotBlank() } ?: base.synopsis,
        progressTotal = optInt("num_episodes", 0).takeIf { it > 0 } ?: base.progressTotal,
        genres = genres.ifEmpty { base.genres },
        creators = studios.ifEmpty { base.creators },
        credits = studios.mapIndexed { index, studio ->
            MediaCredit(
                personName = studio,
                roleType = MediaCreditRole.Studio,
                sortOrder = index,
                metadataSource = MetadataSource.Jikan,
            )
        }.ifEmpty { base.credits },
        sourceUrl = canonicalMalUrl ?: base.sourceUrl,
        popularityScore = optInt("num_list_users", 0)
            .takeIf { it > 0 }
            ?.toDouble()
            ?: base.popularityScore,
        rankingPosition = optInt("rank", 0).takeIf { it > 0 } ?: base.rankingPosition,
        rankingLabel = optInt("rank", 0).takeIf { it > 0 }?.let { "MAL rank" }
            ?: base.rankingLabel,
        popularityJson = base.popularityJson.withMalId(malId),
    )
}

private fun MetadataSuggestion.toMalBackedSuggestion(
    officialBase: MetadataSuggestion,
    malId: Int,
): MetadataSuggestion = copy(
    source = MetadataSource.Jikan,
    externalId = malId.toString(),
    malId = malId,
    originalTitle = originalTitle ?: officialBase.originalTitle,
    sourceUrl = "https://myanimelist.net/anime/$malId",
    externalRating = officialBase.externalRating ?: externalRating,
    externalRatings = (officialBase.externalRatings + externalRatings).distinctBy { it.source },
    rankingPosition = officialBase.rankingPosition ?: rankingPosition,
    rankingLabel = officialBase.rankingLabel ?: rankingLabel,
    popularityJson = popularityJson.withMalId(malId),
)

private fun MetadataSuggestion.withOfficialMalMetrics(malDetails: JSONObject?): MetadataSuggestion {
    val malRating = malDetails?.toOfficialMalRating() ?: return this
    val malRank = malDetails.optInt("rank", 0).takeIf { it > 0 }
    return copy(
        externalRating = MetadataRatingSuggestion(
            score = malRating.score,
            maxScore = malRating.maxScore,
            voteCount = malRating.voteCount,
        ),
        externalRatings = (listOf(malRating) + externalRatings).distinctBy { it.source },
        rankingPosition = malRank ?: rankingPosition,
        rankingLabel = malRank?.let { "MAL rank" } ?: rankingLabel,
    )
}

private fun JSONObject.toOfficialMalRating(): MetadataExternalRatingSuggestion? {
    val score = optDouble("mean", 0.0)
    if (score <= 0.0) return null
    return MetadataExternalRatingSuggestion(
        source = ExternalRatingSource.Mal,
        score = score,
        maxScore = 10.0,
        voteCount = optInt("num_scoring_users", 0).takeIf { it > 0 },
    )
}

private fun JSONObject.toJikanMalRating(): MetadataExternalRatingSuggestion? {
    val score = optDouble("score", 0.0)
    if (score <= 0.0) return null
    return MetadataExternalRatingSuggestion(
        source = ExternalRatingSource.Mal,
        score = score,
        maxScore = 10.0,
        voteCount = optInt("scored_by", 0).takeIf { it > 0 },
    )
}

private fun JSONObject.toJikanMetadataSuggestion(base: MetadataSuggestion): MetadataSuggestion {
    val malId = optInt("mal_id", 0).takeIf { it > 0 }?.toString() ?: base.externalId
    val score = optDouble("score", 0.0)
    val scoredBy = optInt("scored_by", 0)
    val rating = base.externalRating ?: if (score > 0.0) {
        MetadataRatingSuggestion(
            score = score,
            maxScore = 10.0,
            voteCount = scoredBy.takeIf { it > 0 },
        )
    } else {
        base.externalRating
    }
    val externalRatings = (
        listOfNotNull(
            rating?.let {
                MetadataExternalRatingSuggestion(
                    source = ExternalRatingSource.Mal,
                    score = it.score,
                    maxScore = it.maxScore,
                    voteCount = it.voteCount,
                )
            },
        ) + base.externalRatings
        ).distinctBy { it.source }
    val studios = optJSONArray("studios").toNamedList()

    return base.copy(
        source = MetadataSource.Jikan,
        externalId = malId,
        malId = malId.toIntOrNull(),
        title = optString("title_english").takeIf { it.isNotBlank() }
            ?: optString("title").takeIf { it.isNotBlank() }
            ?: base.title,
        originalTitle = optString("title_japanese").takeIf { it.isNotBlank() }
            ?: optString("title").takeIf { it.isNotBlank() && it != base.title },
        releaseYear = optJSONObject("aired")
            ?.optJSONObject("prop")
            ?.optJSONObject("from")
            ?.optInt("year", 0)
            ?.takeIf { it > 0 }
            ?: base.releaseYear,
        coverUrl = optJSONObject("images")
            ?.optJSONObject("jpg")
            ?.optString("large_image_url")
            ?.takeIf { it.isNotBlank() }
            ?: base.coverUrl,
        synopsis = optString("synopsis").takeIf { it.isNotBlank() } ?: base.synopsis,
        progressTotal = optInt("episodes", 0).takeIf { it > 0 } ?: base.progressTotal,
        genres = optJSONArray("genres").toNamedList().ifEmpty { base.genres },
        creators = studios.ifEmpty { base.creators },
        credits = studios.mapIndexed { index, studio ->
            MediaCredit(
                personName = studio,
                roleType = MediaCreditRole.Studio,
                sortOrder = index,
                metadataSource = MetadataSource.Jikan,
            )
        }.ifEmpty { base.credits },
        sourceUrl = optString("url").takeIf { it.isNotBlank() } ?: base.sourceUrl,
        externalRating = rating,
        externalRatings = externalRatings,
        popularityScore = optInt("members", 0).takeIf { it > 0 }?.toDouble() ?: base.popularityScore,
        rankingPosition = optInt("rank", 0).takeIf { it > 0 } ?: base.rankingPosition,
        rankingLabel = base.rankingLabel ?: "MAL rank",
    )
}

private fun org.json.JSONArray?.toNamedList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> getJSONObject(index).optString("name") }
        .filter { it.isNotBlank() }
}

private fun org.json.JSONArray?.firstRank(): Int? {
    if (this == null || length() == 0) return null
    return List(length()) { getJSONObject(it) }
        .firstOrNull { it.optBoolean("allTime", false) }
        ?.optInt("rank", 0)
        ?.takeIf { it > 0 }
}

private fun org.json.JSONArray?.firstRankLabel(): String? {
    if (this == null || length() == 0) return null
    val ranking = List(length()) { getJSONObject(it) }
        .firstOrNull { it.optBoolean("allTime", false) }
        ?: return null
    return ranking.optString("context").takeIf { it.isNotBlank() }
        ?: ranking.optString("type").takeIf { it.isNotBlank() }
}

private fun org.json.JSONArray?.toVoiceActorCredits(): List<MediaCredit> {
    if (this == null) return emptyList()
    return List(length()) { edgeIndex ->
        val edge = getJSONObject(edgeIndex)
        val characterName = edge
            .optJSONObject("node")
            ?.optJSONObject("name")
            ?.optString("full")
            ?.takeIf { it.isNotBlank() }
        val voiceActors = edge.optJSONArray("voiceActors") ?: return@List emptyList()
        List(voiceActors.length()) { actorIndex ->
            val actorName = voiceActors
                .getJSONObject(actorIndex)
                .optJSONObject("name")
                ?.optString("full")
                ?.takeIf { it.isNotBlank() }
                ?: return@List null
            MediaCredit(
                personName = actorName,
                roleType = MediaCreditRole.VoiceActor,
                characterName = characterName,
                sortOrder = edgeIndex * 10 + actorIndex,
                metadataSource = MetadataSource.AniList,
            )
        }.filterNotNull()
    }.flatten()
}
