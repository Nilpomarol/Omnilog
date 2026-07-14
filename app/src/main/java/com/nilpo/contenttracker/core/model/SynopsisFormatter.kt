package com.nilpo.contenttracker.core.model

/** Supported inline styles for provider synopsis content. */
enum class SynopsisStyle {
    Bold,
    Italic,
}

data class SynopsisRun(
    val text: String,
    val styles: Set<SynopsisStyle> = emptySet(),
)

/**
 * A safe, display-ready synopsis. The original provider markup is intentionally not retained.
 * Only the text and the small set of supported inline styles survive normalization.
 */
data class SynopsisDocument(
    val runs: List<SynopsisRun>,
) {
    val plainText: String
        get() = runs.joinToString(separator = "", transform = SynopsisRun::text)

    fun toSafeHtml(): String {
        return buildString {
            runs.forEach { run ->
                val text = run.text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("\n", "<br>")
                val isBold = SynopsisStyle.Bold in run.styles
                val isItalic = SynopsisStyle.Italic in run.styles
                if (isBold) append("<strong>")
                if (isItalic) append("<em>")
                append(text)
                if (isItalic) append("</em>")
                if (isBold) append("</strong>")
            }
        }
    }
}

/**
 * Parses the small HTML/Markdown subset used by metadata providers and returns a safe document.
 * Unsupported tags are discarded while their text content is retained.
 */
fun parseSynopsis(raw: String?): SynopsisDocument? {
    val source = raw?.takeIf { it.isNotBlank() } ?: return null
    val markdownSource = htmlToMarkdown(source)
    val characters = normalizeCharacters(parseMarkdown(markdownSource))
    if (characters.isEmpty()) return null

    val runs = buildList {
        var currentStyles: Set<SynopsisStyle>? = null
        val currentText = StringBuilder()

        fun flush() {
            if (currentText.isNotEmpty()) {
                add(SynopsisRun(currentText.toString(), currentStyles.orEmpty()))
                currentText.clear()
            }
        }

        characters.forEach { character ->
            if (currentStyles != character.styles) {
                flush()
                currentStyles = character.styles
            }
            currentText.append(character.value)
        }
        flush()
    }

    return SynopsisDocument(runs)
}

/** Returns the canonical safe representation used when synopsis content is stored. */
fun normalizeSynopsis(raw: String?): String? = parseSynopsis(raw)?.toSafeHtml()

/** Returns a plain fallback suitable for non-rich UI such as metadata change summaries. */
fun plainSynopsis(raw: String?): String? = parseSynopsis(raw)?.plainText

private data class StyledCharacter(
    val value: Char,
    val styles: Set<SynopsisStyle>,
)

private data class MarkdownMarker(
    val token: String,
    val style: SynopsisStyle,
)

private fun htmlToMarkdown(source: String): String {
    val output = StringBuilder(source.length)
    var index = 0

    while (index < source.length) {
        when {
            source.startsWith("<!--", index) -> {
                val commentEnd = source.indexOf("-->", index + 4)
                index = if (commentEnd >= 0) commentEnd + 3 else source.length
            }

            source[index] == '<' -> {
                val tagEnd = findTagEnd(source, index)
                if (tagEnd < 0) {
                    output.append(source[index])
                    index++
                    continue
                }

                val tag = source.substring(index, tagEnd + 1)
                val name = htmlTagName(tag)
                if (name == null) {
                    output.append(source[index])
                    index++
                    continue
                }

                if (!isClosingHtmlTag(tag) && name in setOf("script", "style", "noscript")) {
                    val closingStart = source.indexOf("</$name", tagEnd + 1, ignoreCase = true)
                    index = if (closingStart < 0) {
                        source.length
                    } else {
                        val closingEnd = source.indexOf('>', closingStart)
                        if (closingEnd < 0) source.length else closingEnd + 1
                    }
                    continue
                }

                if (isClosingHtmlTag(tag)) {
                    when (name) {
                        "b", "strong" -> output.append("**")
                        "i", "em" -> output.append('*')
                        "p", "div", "section", "article", "header", "footer",
                        "h1", "h2", "h3", "h4", "h5", "h6", "li" -> output.appendParagraphBreak()
                    }
                } else {
                    when (name) {
                        "b", "strong" -> output.append("**")
                        "i", "em" -> output.append('*')
                        "br" -> output.appendLineBreak()
                        "p", "div", "section", "article", "header", "footer",
                        "h1", "h2", "h3", "h4", "h5", "h6" -> output.appendParagraphBreakIfNeeded()
                        "li" -> {
                            output.appendLineBreak()
                            output.append("- ")
                        }
                    }
                }
                index = tagEnd + 1
            }

            else -> {
                output.append(source[index])
                index++
            }
        }
    }

    return decodeHtmlEntities(output.toString())
}

private fun StringBuilder.appendLineBreak() {
    while (isNotEmpty() && last() == ' ') deleteCharAt(length - 1)
    if (isNotEmpty() && last() != '\n') append('\n')
}

private fun StringBuilder.appendParagraphBreakIfNeeded() {
    if (isEmpty()) return
    if (!endsWith("\n")) append('\n')
}

private fun StringBuilder.appendParagraphBreak() {
    while (isNotEmpty() && (last() == ' ' || last() == '\t')) deleteCharAt(length - 1)
    if (isEmpty()) return
    if (!endsWith("\n\n")) {
        if (!endsWith("\n")) append('\n')
        append('\n')
    }
}

private fun StringBuilder.endsWith(value: String): Boolean =
    length >= value.length && substring(length - value.length) == value

private fun findTagEnd(source: String, start: Int): Int {
    var quote: Char? = null
    for (index in start + 1 until source.length) {
        val character = source[index]
        if (quote != null) {
            if (character == quote) quote = null
        } else if (character == '\'' || character == '"') {
            quote = character
        } else if (character == '>') {
            return index
        }
    }
    return -1
}

private fun htmlTagName(tag: String): String? {
    var index = 1
    if (tag.getOrNull(index) == '/') index++
    while (index < tag.length && tag[index].isWhitespace()) index++
    val start = index
    while (index < tag.length && (tag[index].isLetterOrDigit() || tag[index] == '-')) index++
    return tag.substring(start, index).takeIf { it.isNotBlank() }?.lowercase()
}

private fun isClosingHtmlTag(tag: String): Boolean {
    var index = 1
    while (index < tag.length && tag[index].isWhitespace()) index++
    return tag.getOrNull(index) == '/'
}

private fun decodeHtmlEntities(source: String): String {
    val output = StringBuilder(source.length)
    var index = 0
    while (index < source.length) {
        if (source[index] != '&') {
            output.append(source[index])
            index++
            continue
        }

        val entityEnd = source.indexOf(';', index + 1)
        if (entityEnd < 0 || entityEnd - index > 12) {
            output.append('&')
            index++
            continue
        }

        val entity = source.substring(index + 1, entityEnd)
        val decoded = when {
            entity.equals("nbsp", ignoreCase = true) -> " "
            entity.equals("amp", ignoreCase = true) -> "&"
            entity.equals("lt", ignoreCase = true) -> "<"
            entity.equals("gt", ignoreCase = true) -> ">"
            entity.equals("quot", ignoreCase = true) -> "\""
            entity.equals("apos", ignoreCase = true) -> "'"
            entity.startsWith("#x", ignoreCase = true) -> entity.substring(2).toIntOrNull(16)?.toChar()?.toString()
            entity.startsWith('#') -> entity.substring(1).toIntOrNull()?.toChar()?.toString()
            else -> null
        }

        if (decoded == null) {
            output.append('&')
            index++
        } else {
            output.append(decoded)
            index = entityEnd + 1
        }
    }
    return output.toString()
}

private fun parseMarkdown(source: String): List<StyledCharacter> {
    val output = mutableListOf<StyledCharacter>()
    val activeMarkers = mutableListOf<MarkdownMarker>()
    var index = 0

    fun appendLiteral(text: String) {
        val styles = activeMarkers.mapTo(mutableSetOf()) { it.style }.toSet()
        text.forEach { character -> output += StyledCharacter(character, styles) }
    }

    while (index < source.length) {
        if (source[index] == '\\' && source.getOrNull(index + 1)?.let { it == '*' || it == '_' || it == '\\' } == true) {
            appendLiteral(source[index + 1].toString())
            index += 2
            continue
        }

        val marker = when {
            source.startsWith("**", index) -> MarkdownMarker("**", SynopsisStyle.Bold)
            source.startsWith("__", index) -> MarkdownMarker("__", SynopsisStyle.Bold)
            source[index] == '*' -> MarkdownMarker("*", SynopsisStyle.Italic)
            source[index] == '_' -> MarkdownMarker("_", SynopsisStyle.Italic)
            else -> null
        }

        if (marker == null) {
            appendLiteral(source[index].toString())
            index++
            continue
        }

        val activeIndex = activeMarkers.indexOfLast { it.token == marker.token }
        val canClose = canCloseMarker(source, index, marker.token)
        val canOpen = canOpenMarker(source, index, marker.token)
        val hasMatchingMarker = hasMatchingMarker(source, index, marker.token)

        when {
            activeIndex >= 0 && canClose -> {
                activeMarkers.removeAt(activeIndex)
                index += marker.token.length
            }

            activeIndex < 0 && canOpen && hasMatchingMarker -> {
                activeMarkers += marker
                index += marker.token.length
            }

            else -> {
                appendLiteral(marker.token)
                index += marker.token.length
            }
        }
    }

    return output
}

private fun canOpenMarker(source: String, index: Int, token: String): Boolean {
    val next = source.getOrNull(index + token.length) ?: return false
    if (next.isWhitespace()) return false
    if (token == "_" && source.getOrNull(index - 1)?.isLetterOrDigit() == true && next.isLetterOrDigit()) {
        return false
    }
    return true
}

private fun canCloseMarker(source: String, index: Int, token: String): Boolean {
    val previous = source.getOrNull(index - 1) ?: return false
    if (previous.isWhitespace()) return false
    if (token == "_" && previous.isLetterOrDigit() && source.getOrNull(index + token.length)?.isLetterOrDigit() == true) {
        return false
    }
    return true
}

private fun hasMatchingMarker(source: String, index: Int, token: String): Boolean {
    var nextIndex = index + token.length
    while (nextIndex >= 0 && nextIndex < source.length) {
        nextIndex = source.indexOf(token, nextIndex)
        if (nextIndex < 0) return false
        if (!isEscaped(source, nextIndex) && canCloseMarker(source, nextIndex, token)) return true
        nextIndex += token.length
    }
    return false
}

private fun isEscaped(source: String, index: Int): Boolean {
    var slashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && source[cursor] == '\\') {
        slashCount++
        cursor--
    }
    return slashCount % 2 == 1
}

private fun normalizeCharacters(source: List<StyledCharacter>): List<StyledCharacter> {
    val normalized = mutableListOf<StyledCharacter>()
    var consecutiveNewlines = 0

    source.forEach { original ->
        val character = if (original.value == '\r') '\n' else original.value
        if (character == '\n') {
            while (normalized.lastOrNull()?.value == ' ' || normalized.lastOrNull()?.value == '\t') {
                normalized.removeAt(normalized.lastIndex)
            }
            consecutiveNewlines++
            if (consecutiveNewlines <= 2) {
                normalized += original.copy(value = '\n')
            }
        } else {
            if (consecutiveNewlines > 0 && (character == ' ' || character == '\t')) return@forEach
            consecutiveNewlines = 0
            normalized += original.copy(value = character)
        }
    }

    while (normalized.firstOrNull()?.value?.isWhitespace() == true) normalized.removeAt(0)
    while (normalized.lastOrNull()?.value?.isWhitespace() == true) normalized.removeAt(normalized.lastIndex)
    return normalized
}
