package com.nilpo.contenttracker.core.model

/**
 * Resolves one reusable portrait for each author from credits already saved in the library.
 *
 * Provider coverage is uneven, so this deliberately lets one successful Open Library portrait
 * benefit every other book credited to the same author without changing those book records.
 */
fun List<TrackedMedia>.authorImageUrls(): Map<String, String> {
    return contributorImageUrls(MediaCreditRole.Author)
}

/**
 * Resolves an image for every contributor with the same visual identity. Companies deliberately
 * share logos between studio, developer and publisher credits: providers do not label a company's
 * involvement consistently from one title to the next.
 *
 * The identity is always role-aware, so a person who both directs and acts cannot be represented by
 * their cast headshot in a directors list.
 */
fun List<TrackedMedia>.contributorImageUrls(role: MediaCreditRole): Map<String, String> {
    return contributorImageCandidates { _, credit -> credit.roleType.sharesImageWith(role) }
        .groupingBy { candidate -> candidate.key }
        .reduce { _, chosen, candidate -> minOf(chosen, candidate, ContributorImageOrder) }
        .mapValues { (_, candidate) -> candidate.imageUrl }
}

fun List<TrackedMedia>.authorImageUrl(authorName: String): String? =
    authorImageUrls()[authorName.contributorImageKey()]

fun String.contributorImageKey(): String = trim().lowercase()

/**
 * The contributor used to organise each media type in the library. Rich credits are authoritative;
 * the legacy [MediaItem.creators] names only keep older, unrefreshed records discoverable.
 */
fun TrackedMedia.creatorNames(): List<String> {
    val creditedNames = credits
        .asSequence()
        .filter { it.roleType == item.type.groupCreatorRole() }
        .map { it.personName.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.contributorImageKey() }
        .toList()
    return creditedNames.ifEmpty {
        item.creators
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.contributorImageKey() }
    }
}

fun TrackedMedia.hasCreator(name: String): Boolean =
    creatorNames().any { it.equals(name.trim(), ignoreCase = true) }

fun TrackedMedia.hasContributor(role: MediaCreditRole, name: String): Boolean =
    credits.any { credit ->
        credit.roleType == role && credit.personName.equals(name.trim(), ignoreCase = true)
    }

/**
 * Reuses a contributor portrait or company logo across every matching item in the library, matching
 * each item by the role that item's own media type is organised under.
 *
 * That role filter is the point: without it a director who also acts is liable to be drawn with a
 * cast headshot, and the answer depends on which title happened to be looked at first.
 */
fun List<TrackedMedia>.creatorImageUrl(creatorName: String): String? {
    val key = creatorName.contributorImageKey()
    return contributorImageCandidates { trackedMedia, credit ->
        credit.roleType.sharesImageWith(trackedMedia.item.type.groupCreatorRole())
    }
        .filter { candidate -> candidate.key == key }
        .minWithOrNull(ContributorImageOrder)
        ?.imageUrl
}

/** The provider-supplied logo ratio for [creatorName], resolved under the same ordering as its URL. */
fun List<TrackedMedia>.creatorImageAspectRatio(creatorName: String): Float? {
    val key = creatorName.contributorImageKey()
    return contributorImageCandidates { trackedMedia, credit ->
        credit.roleType.sharesImageWith(trackedMedia.item.type.groupCreatorRole())
    }
        .filter { candidate -> candidate.key == key }
        .minWithOrNull(ContributorImageOrder)
        ?.imageAspectRatio
}

/** A credit that could supply a contributor's image, with everything needed to rank it. */
internal data class ContributorImageCandidate(
    val key: String,
    val imageUrl: String,
    val imageAspectRatio: Float?,
    val mediaItemId: Long,
    val sortOrder: Int,
)

/**
 * A total order over candidates, so a name shared by several items always resolves to the same
 * image.
 *
 * Every term is drawn from the records themselves rather than from list position. Resolving by
 * iteration order — which is what "first match wins" amounted to — meant the same author could be
 * drawn differently on the detail page and the home group, and could change again whenever the
 * library was re-sorted.
 */
internal val ContributorImageOrder = compareBy<ContributorImageCandidate>(
    { it.mediaItemId },
    { it.sortOrder },
    { it.imageUrl },
)

private fun List<TrackedMedia>.contributorImageCandidates(
    matches: (TrackedMedia, MediaCredit) -> Boolean,
): Sequence<ContributorImageCandidate> = asSequence().flatMap { trackedMedia ->
    trackedMedia.credits
        .asSequence()
        .filter { credit -> matches(trackedMedia, credit) }
        .mapNotNull { credit ->
            credit.personImageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                ContributorImageCandidate(
                    key = credit.personName.contributorImageKey(),
                    imageUrl = imageUrl,
                    imageAspectRatio = credit.personImageAspectRatio,
                    mediaItemId = trackedMedia.item.id,
                    sortOrder = credit.sortOrder,
                )
            }
        }
}

internal fun MediaType.groupCreatorRole(): MediaCreditRole = when (this) {
    MediaType.Anime -> MediaCreditRole.Studio
    MediaType.Book -> MediaCreditRole.Author
    MediaType.Movie -> MediaCreditRole.Director
    MediaType.TvShow -> MediaCreditRole.Creator
    MediaType.Game -> MediaCreditRole.Developer
}

/** Whether two roles are drawn with the same picture of the same contributor. */
internal fun MediaCreditRole.sharesImageWith(other: MediaCreditRole): Boolean =
    imageIdentity() == other.imageIdentity()

internal fun MediaCreditRole.imageIdentity(): String = when (this) {
    MediaCreditRole.Studio,
    MediaCreditRole.Developer,
    MediaCreditRole.Publisher,
    -> "company"
    MediaCreditRole.Director,
    MediaCreditRole.Creator,
    -> "creator"
    else -> name
}
