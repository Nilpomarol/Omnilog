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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TmdbMetadataRepository(
    private val apiKey: String,
    private val omdbApiKey: String = "",
) : MetadataRepository {
    override suspend fun searchSuggestions(
        request: MetadataSearchRequest,
    ): List<MetadataSuggestion> {
        val query = request.query.trim()
        if (apiKey.isBlank() || query.isBlank()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            buildList {
                if (MediaType.Movie in request.mediaTypes) {
                    addAll(searchEndpoint(query = query, endpoint = "movie", mediaType = MediaType.Movie))
                }
                if (MediaType.TvShow in request.mediaTypes) {
                    addAll(searchEndpoint(query = query, endpoint = "tv", mediaType = MediaType.TvShow))
                }
            }.sortedByDescending { it.externalRating?.score ?: 0.0 }
        }
    }

    override suspend fun getSuggestionDetails(
        suggestion: MetadataSuggestion,
    ): MetadataSuggestion {
        if (apiKey.isBlank() || suggestion.source != MetadataSource.Tmdb) {
            return suggestion
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val url = when (suggestion.mediaType) {
                    MediaType.Movie ->
                        "https://api.themoviedb.org/3/movie/${suggestion.externalId}" +
                            "?api_key=$apiKey&language=en-US&append_to_response=credits,external_ids"
                    MediaType.TvShow ->
                        "https://api.themoviedb.org/3/tv/${suggestion.externalId}" +
                            "?api_key=$apiKey&language=en-US&append_to_response=credits,external_ids"
                    else -> return@withContext suggestion
                }
                getJson(url).toDetailedSuggestion(suggestion)
            }.getOrElse { suggestion }
        }
    }

    private fun searchEndpoint(
        query: String,
        endpoint: String,
        mediaType: MediaType,
    ): List<MetadataSuggestion> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = getJson(
            "https://api.themoviedb.org/3/search/$endpoint" +
                "?api_key=$apiKey&query=$encodedQuery&include_adult=false&language=en-US",
        )
        val results = response.optJSONArray("results") ?: return emptyList()

        return List(results.length()) { index -> results.getJSONObject(index) }
            .mapNotNull { result -> result.toMetadataSuggestion(mediaType) }
    }

    private fun JSONObject.toMetadataSuggestion(mediaType: MediaType): MetadataSuggestion? {
        val id = optLong("id", 0L).takeIf { it > 0L } ?: return null
        val title = when (mediaType) {
            MediaType.Movie -> optString("title").takeIf { it.isNotBlank() }
            MediaType.TvShow -> optString("name").takeIf { it.isNotBlank() }
            else -> null
        } ?: return null
        val originalTitle = when (mediaType) {
            MediaType.Movie -> optString("original_title").takeIf { it.isNotBlank() && it != title }
            MediaType.TvShow -> optString("original_name").takeIf { it.isNotBlank() && it != title }
            else -> null
        }
        val date = when (mediaType) {
            MediaType.Movie -> optString("release_date")
            MediaType.TvShow -> optString("first_air_date")
            else -> ""
        }
        val posterPath = optString("poster_path").takeIf { it.isNotBlank() }
        val voteAverage = optDouble("vote_average", 0.0)
        val voteCount = optInt("vote_count", 0)
        val externalRating = if (voteAverage > 0.0) {
            MetadataRatingSuggestion(
                score = voteAverage,
                maxScore = 10.0,
                voteCount = voteCount.takeIf { it > 0 },
            )
        } else {
            null
        }

        return MetadataSuggestion(
            source = MetadataSource.Tmdb,
            externalId = id.toString(),
            mediaType = mediaType,
            title = title,
            originalTitle = originalTitle,
            releaseYear = date.take(4).toIntOrNull(),
            coverUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            synopsis = optString("overview").takeIf { it.isNotBlank() },
            popularityScore = voteCount.takeIf { it > 0 }?.toDouble(),
            sourceUrl = when (mediaType) {
                MediaType.Movie -> "https://www.themoviedb.org/movie/$id"
                MediaType.TvShow -> "https://www.themoviedb.org/tv/$id"
                else -> null
            },
            externalRating = externalRating,
            externalRatings = externalRating?.let {
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.Tmdb,
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private fun JSONObject.toDetailedSuggestion(base: MetadataSuggestion): MetadataSuggestion {
        val title = when (base.mediaType) {
            MediaType.Movie -> optString("title").takeIf { it.isNotBlank() }
            MediaType.TvShow -> optString("name").takeIf { it.isNotBlank() }
            else -> null
        }
        val originalTitle = when (base.mediaType) {
            MediaType.Movie -> optString("original_title").takeIf { it.isNotBlank() && it != title }
            MediaType.TvShow -> optString("original_name").takeIf { it.isNotBlank() && it != title }
            else -> null
        }
        val releaseDate = when (base.mediaType) {
            MediaType.Movie -> optString("release_date")
            MediaType.TvShow -> optString("first_air_date")
            else -> ""
        }
        val posterPath = optString("poster_path").takeIf { it.isNotBlank() }
        val collectionTitle = optJSONObject("belongs_to_collection")
            ?.optString("name")
            ?.takeIf { it.isNotBlank() }
        val progressTotal = when (base.mediaType) {
            MediaType.Movie -> optInt("runtime", 0).takeIf { it > 0 }
            MediaType.TvShow -> optInt("number_of_episodes", 0).takeIf { it > 0 }
            else -> base.progressTotal
        }
        val creators = when (base.mediaType) {
            MediaType.Movie -> optJSONObject("credits")
                ?.optJSONArray("crew")
                ?.toStringList("name") { optString("job") == "Director" }
                ?: emptyList()
            MediaType.TvShow -> optJSONArray("created_by").toStringList("name")
            else -> emptyList()
        }
        val creatorCredits = when (base.mediaType) {
            MediaType.Movie -> optJSONObject("credits")
                ?.optJSONArray("crew")
                .toCredits(MediaCreditRole.Director, MetadataSource.Tmdb) { optString("job") == "Director" }
            MediaType.TvShow -> optJSONArray("created_by")
                .toCredits(MediaCreditRole.Creator, MetadataSource.Tmdb)
            else -> emptyList()
        }
        val castCredits = optJSONObject("credits")
            ?.optJSONArray("cast")
            .toCredits(MediaCreditRole.Cast, MetadataSource.Tmdb, limit = 20)

        val imdbId = optJSONObject("external_ids")
            ?.optString("imdb_id")
            ?.takeIf { it.isNotBlank() }
        val omdbRatings = imdbId?.let(::fetchOmdbRatings).orEmpty()

        val preferredRating = omdbRatings
            .firstOrNull { it.source == ExternalRatingSource.Imdb }
            ?.toPrimaryRating()
            ?: base.externalRating

        return base.copy(
            title = title ?: base.title,
            originalTitle = originalTitle ?: base.originalTitle,
            collectionTitle = collectionTitle ?: base.collectionTitle,
            releaseYear = releaseDate.take(4).toIntOrNull() ?: base.releaseYear,
            genres = optJSONArray("genres").toStringList("name"),
            creators = creators.ifEmpty { base.creators },
            credits = (creatorCredits + castCredits).ifEmpty { base.credits },
            progressTotal = progressTotal ?: base.progressTotal,
            coverUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: base.coverUrl,
            synopsis = optString("overview").takeIf { it.isNotBlank() } ?: base.synopsis,
            popularityScore = optInt("vote_count", 0).takeIf { it > 0 }?.toDouble() ?: base.popularityScore,
            externalRating = preferredRating,
            externalRatings = (base.externalRatings + omdbRatings).distinctBy { it.source },
        )
    }

    private fun fetchOmdbRatings(imdbId: String): List<MetadataExternalRatingSuggestion> {
        if (omdbApiKey.isBlank()) return emptyList()

        return runCatching {
            val json = getJson(
                "https://www.omdbapi.com/?apikey=$omdbApiKey&i=$imdbId&r=json",
            )
            if (json.optString("Response") == "False") {
                return@runCatching emptyList()
            }
            buildList {
                json.optString("imdbRating")
                    .toDoubleOrNull()
                    ?.takeIf { it > 0.0 }
                    ?.let { score ->
                        add(
                            MetadataExternalRatingSuggestion(
                                source = ExternalRatingSource.Imdb,
                                score = score,
                                maxScore = 10.0,
                                voteCount = json.optString("imdbVotes").ratingVoteCount(),
                            ),
                        )
                    }
                json.optJSONArray("Ratings")?.let { ratings ->
                    List(ratings.length()) { ratings.getJSONObject(it) }.forEach { rating ->
                        val source = rating.optString("Source")
                        val value = rating.optString("Value")
                        when (source) {
                            "Rotten Tomatoes" -> value.removeSuffix("%")
                                .toDoubleOrNull()
                                ?.takeIf { it > 0.0 }
                                ?.let { score ->
                                    add(
                                        MetadataExternalRatingSuggestion(
                                            source = ExternalRatingSource.RottenTomatoes,
                                            score = score,
                                            maxScore = 100.0,
                                        ),
                                    )
                                }
                            "Metacritic" -> value.substringBefore("/")
                                .toDoubleOrNull()
                                ?.takeIf { it > 0.0 }
                                ?.let { score ->
                                    add(
                                        MetadataExternalRatingSuggestion(
                                            source = ExternalRatingSource.Metacritic,
                                            score = score,
                                            maxScore = 100.0,
                                        ),
                                    )
                                }
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"

        return connection.inputStream.bufferedReader().use { reader ->
            JSONObject(reader.readText())
        }
    }
}

private fun String.ratingVoteCount(): Int? {
    return replace(",", "")
        .trim()
        .toIntOrNull()
        ?.takeIf { it > 0 }
}

private fun MetadataExternalRatingSuggestion.toPrimaryRating(): MetadataRatingSuggestion {
    return MetadataRatingSuggestion(
        score = score,
        maxScore = maxScore,
        voteCount = voteCount,
    )
}

private fun org.json.JSONArray?.toStringList(
    fieldName: String,
    predicate: JSONObject.() -> Boolean = { true },
): List<String> {
    if (this == null) return emptyList()
    return List(length()) { getJSONObject(it) }
        .filter { it.predicate() }
        .map { it.optString(fieldName) }
        .filter { it.isNotBlank() }
}

private fun org.json.JSONArray?.toCredits(
    roleType: MediaCreditRole,
    source: MetadataSource,
    limit: Int = Int.MAX_VALUE,
    predicate: JSONObject.() -> Boolean = { true },
): List<MediaCredit> {
    if (this == null) return emptyList()
    return List(length()) { getJSONObject(it) }
        .filter { it.predicate() }
        .take(limit)
        .mapIndexedNotNull { index, obj ->
            val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            MediaCredit(
                personName = name,
                roleType = roleType,
                characterName = obj.optString("character").takeIf { it.isNotBlank() },
                sortOrder = index,
                metadataSource = source,
            )
        }
}
