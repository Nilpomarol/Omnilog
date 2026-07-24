package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataExternalRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AniListRecommendationRepository : RecommendationProvider {
    override suspend fun getRecommendations(item: MediaItem): List<MetadataSuggestion> {
        val externalId = item.metadataExternalId?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (item.type != MediaType.Anime || item.metadataSource != MetadataSource.AniList) {
            return emptyList()
        }

        val aniListId = externalId.toIntOrNull() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = postAniListGraphQL(
                    RECOMMENDATIONS_QUERY,
                    JSONObject().apply {
                        put("id", aniListId)
                        put("perPage", 10)
                    },
                )
                val nodes = response
                    .optJSONObject("data")
                    ?.optJSONObject("Media")
                    ?.optJSONObject("recommendations")
                    ?.optJSONArray("nodes")
                    ?: return@runCatching emptyList()

                List(nodes.length()) { index -> nodes.getJSONObject(index) }
                    .mapNotNull { it.optJSONObject("mediaRecommendation")?.toMetadataSuggestion() }
            }.getOrDefault(emptyList())
        }
    }

    private fun JSONObject.toMetadataSuggestion(): MetadataSuggestion? {
        val id = optInt("id", 0).takeIf { it > 0 } ?: return null
        val titleObject = optJSONObject("title") ?: return null
        val englishTitle = titleObject.optString("english").takeIf { it.isNotBlank() }
        val romajiTitle = titleObject.optString("romaji").takeIf { it.isNotBlank() } ?: return null
        val title = englishTitle ?: romajiTitle
        val originalTitle = romajiTitle.takeIf { englishTitle != null && it != englishTitle }
        val averageScore = optInt("averageScore", 0)
        val popularity = optInt("popularity", 0)
        val ratingVoteCount = optJSONObject("stats")
            ?.optJSONArray("scoreDistribution")
            ?.scoreDistributionVoteCount()
        val rating = averageScore.takeIf { it > 0 }?.let {
            MetadataRatingSuggestion(
                score = it / 10.0,
                maxScore = 10.0,
                voteCount = ratingVoteCount,
            )
        }
        val genres = optJSONArray("genres")?.let { array ->
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.orEmpty()
        val sourceUrl = optString("siteUrl").takeIf { it.isNotBlank() }
            ?: "https://anilist.co/anime/$id"

        return MetadataSuggestion(
            source = MetadataSource.AniList,
            externalId = id.toString(),
            mediaType = MediaType.Anime,
            title = title,
            originalTitle = originalTitle,
            releaseYear = optJSONObject("startDate")?.optInt("year", 0)?.takeIf { it > 0 },
            coverUrl = optJSONObject("coverImage")?.optString("large")?.takeIf { it.isNotBlank() },
            synopsis = optString("description")
                .takeIf { it.isNotBlank() }
                ?.replace(Regex("<[^>]+>"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim(),
            progressTotal = optInt("episodes", 0).takeIf { it > 0 },
            genres = genres,
            sourceUrl = sourceUrl,
            popularityScore = popularity.takeIf { it > 0 }?.toDouble(),
            externalRating = rating,
            externalRatings = rating?.let {
                listOf(
                    MetadataExternalRatingSuggestion(
                        source = ExternalRatingSource.AniList,
                        score = it.score,
                        maxScore = it.maxScore,
                        voteCount = it.voteCount,
                    ),
                )
            }.orEmpty(),
        )
    }

    private companion object {
        val RECOMMENDATIONS_QUERY = """
            query (${ '$' }id: Int, ${ '$' }perPage: Int) {
              Media(id: ${ '$' }id, type: ANIME) {
                recommendations(perPage: ${ '$' }perPage) {
                  nodes {
                    mediaRecommendation {
                      id
                      title { english romaji }
                      coverImage { large }
                      description(asHtml: false)
                      episodes
                      averageScore
                      popularity
                      stats { scoreDistribution { amount } }
                      startDate { year }
                      genres
                      siteUrl
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
