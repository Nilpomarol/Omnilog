package com.nilpo.contenttracker.core.model

data class ExternalRecommendation(
    val suggestion: MetadataSuggestion,
    val reason: RecommendationReason,
    val providerRank: Int,
)

enum class RecommendationReason {
    Similar,
}
