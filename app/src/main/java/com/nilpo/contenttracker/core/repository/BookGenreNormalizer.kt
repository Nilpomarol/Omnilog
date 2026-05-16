package com.nilpo.contenttracker.core.repository

fun List<String>.standardBookGenres(limit: Int = 6): List<String> {
    return mapNotNull { it.standardBookGenre() }
        .distinct()
        .take(limit)
}

private fun String.standardBookGenre(): String? {
    val normalized = lowercase()
        .replace("&", "and")
        .replace(Regex("""[^a-z0-9 ]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    return when {
        normalized.isNoiseSubject() -> null
        normalized.hasAny("fantasy") -> "Fantasy"
        normalized.hasAny("science fiction", "sci fi", "scifi") -> "Science Fiction"
        normalized.hasAny("mystery", "detective") -> "Mystery"
        normalized.hasAny("thriller", "suspense") -> "Thriller"
        normalized.hasAny("horror", "ghost stories") -> "Horror"
        normalized.hasAny("romance", "love stories") -> "Romance"
        normalized.hasAny("historical fiction") -> "Historical Fiction"
        normalized.hasAny("adventure stories", "adventure") -> "Adventure"
        normalized.hasAny("young adult", "juvenile fiction") -> "Young Adult"
        normalized.hasAny("children", "juvenile literature") -> "Children"
        normalized.hasAny("comics", "graphic novels", "manga") -> "Comics"
        normalized.hasAny("poetry") -> "Poetry"
        normalized.hasAny("drama", "plays") -> "Drama"
        normalized.hasAny("biography", "autobiography", "memoir") -> "Biography"
        normalized.hasAny("history") -> "History"
        normalized.hasAny("philosophy") -> "Philosophy"
        normalized.hasAny("psychology") -> "Psychology"
        normalized.hasAny("religion", "spirituality") -> "Religion"
        normalized.hasAny("science") -> "Science"
        normalized.hasAny("business", "economics") -> "Business"
        normalized.hasAny("self help", "self improvement") -> "Self-Help"
        normalized.hasAny("travel") -> "Travel"
        normalized.hasAny("cooking", "cookery") -> "Cooking"
        normalized.hasAny("art", "music", "performing arts") -> "Arts"
        normalized.hasAny("fiction") -> "Fiction"
        normalized.hasAny("nonfiction", "non fiction") -> "Nonfiction"
        else -> null
    }
}

private fun String.isNoiseSubject(): Boolean {
    return hasAny(
        "accessible book",
        "protected daisy",
        "in library",
        "overdrive",
        "internet archive",
        "large type books",
        "open library",
        "translations",
        "adaptations",
    )
}

private fun String.hasAny(vararg values: String): Boolean {
    return values.any { contains(it) }
}
