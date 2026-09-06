package com.nilpo.contenttracker.core.model

/** How much of the library one contributor represents, and how the user has rated that work. */
data class ContributorStats(
    val itemCount: Int = 0,
    val averageRating: Double? = null,
)

/**
 * Everything the credit lists need to know about the library's contributors, resolved once.
 *
 * The detail page asks these questions for every credit role it draws, and the answers depend on
 * the whole library rather than on the item being viewed. Computing them per role inside
 * composition meant a full library scan for each role, repeated on every emission — so this is
 * built once, off the main thread, and read from.
 */
class ContributorDirectory internal constructor(
    private val statsByRole: Map<String, ContributorStats>,
    private val imagesByIdentity: Map<String, String>,
    private val imageAspectRatiosByIdentity: Map<String, Float?>,
) {
    /**
     * Matched by role as well as name, so a director's film count is not mixed with the work of an
     * identically named actor or author.
     */
    fun stats(role: MediaCreditRole, personName: String): ContributorStats =
        statsByRole[statsKey(role, personName)] ?: ContributorStats()

    /** The one image this contributor is drawn with everywhere, or null when none was found. */
    fun imageUrl(role: MediaCreditRole, personName: String): String? =
        imagesByIdentity[imageKey(role, personName)]

    /** The source canvas ratio for this contributor's resolved image, when the provider supplied it. */
    fun imageAspectRatio(role: MediaCreditRole, personName: String): Float? =
        imageAspectRatiosByIdentity[imageKey(role, personName)]

    /** The resolved images, deduplicated — one per contributor rather than one per credit. */
    fun imageUrls(): List<String> = imagesByIdentity.values.distinct()

    companion object {
        val Empty = ContributorDirectory(emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * Builds the directory in a single pass over the library.
 *
 * Images resolve by visual identity (companies share one logo across studio, developer and
 * publisher credits) under the same total order as [contributorImageUrls], so every surface agrees
 * on which image a contributor is drawn with. Counts resolve by exact role, which is a different
 * grouping — both are collected here rather than in a scan each.
 */
fun List<TrackedMedia>.contributorDirectory(): ContributorDirectory {
    val itemsByRole = mutableMapOf<String, MutableList<TrackedMedia>>()
    val bestImage = mutableMapOf<String, ContributorImageCandidate>()

    forEach { trackedMedia ->
        val countedRoles = mutableSetOf<String>()
        trackedMedia.credits.forEach { credit ->
            val name = credit.personName.contributorImageKey()
            if (name.isBlank()) return@forEach

            val roleKey = statsKey(credit.roleType, name)
            // An item that credits the same contributor twice in one role still counts once.
            if (countedRoles.add(roleKey)) {
                itemsByRole.getOrPut(roleKey, ::mutableListOf).add(trackedMedia)
            }

            val imageUrl = credit.personImageUrl?.takeIf { it.isNotBlank() } ?: return@forEach
            val candidate = ContributorImageCandidate(
                key = name,
                imageUrl = imageUrl,
                imageAspectRatio = credit.personImageAspectRatio,
                mediaItemId = trackedMedia.item.id,
                sortOrder = credit.sortOrder,
            )
            val identityKey = imageKey(credit.roleType, name)
            val chosen = bestImage[identityKey]
            if (chosen == null || ContributorImageOrder.compare(candidate, chosen) < 0) {
                bestImage[identityKey] = candidate
            }
        }
    }

    return ContributorDirectory(
        statsByRole = itemsByRole.mapValues { (_, items) ->
            ContributorStats(
                itemCount = items.size,
                averageRating = items
                    .mapNotNull { it.currentSession?.ratingHalfPoints?.let(RatingHalfPoints::toScore) }
                    .takeIf { it.isNotEmpty() }
                    ?.average(),
            )
        },
        imagesByIdentity = bestImage.mapValues { (_, candidate) -> candidate.imageUrl },
        imageAspectRatiosByIdentity = bestImage.mapValues { (_, candidate) -> candidate.imageAspectRatio },
    )
}

/**
 * Every image the library needs kept available offline: item covers plus contributor portraits.
 *
 * Portraits come from the directory rather than from the credits directly, so a contributor shared
 * by fifty items is stored once — as the same image those items are all drawn with.
 */
fun List<TrackedMedia>.persistableImageUrls(): List<String> {
    val coverUrls = mapNotNull { it.item.coverUrl?.trim()?.takeIf(String::isNotBlank) }
    return (coverUrls + contributorDirectory().imageUrls()).distinct()
}

private fun statsKey(role: MediaCreditRole, personName: String): String =
    "${role.name}:${personName.contributorImageKey()}"

private fun imageKey(role: MediaCreditRole, personName: String): String =
    "${role.imageIdentity()}:${personName.contributorImageKey()}"
