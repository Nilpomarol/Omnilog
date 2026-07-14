package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SynopsisFormatterTest {
    @Test
    fun htmlEmphasisBecomesStyledRunsWithoutChangingText() {
        val document = parseSynopsis("A <strong>bold</strong> and <em>italic</em> sentence.")

        assertEquals("A bold and italic sentence.", document?.plainText)
        assertEquals(
            listOf(
                SynopsisRun("A "),
                SynopsisRun("bold", setOf(SynopsisStyle.Bold)),
                SynopsisRun(" and "),
                SynopsisRun("italic", setOf(SynopsisStyle.Italic)),
                SynopsisRun(" sentence."),
            ),
            document?.runs,
        )
    }

    @Test
    fun markdownEmphasisSupportsBoldItalicAndEscapedMarkers() {
        val document = parseSynopsis("**Bold** *italic* \\*literal\\*")

        assertEquals("Bold italic *literal*", document?.plainText)
        assertTrue(document?.runs?.any { it.text == "Bold" && SynopsisStyle.Bold in it.styles } == true)
        assertTrue(document?.runs?.any { it.text == "italic" && SynopsisStyle.Italic in it.styles } == true)
    }

    @Test
    fun entitiesAndParagraphBreaksAreNormalizedSafely() {
        val document = parseSynopsis("<p>Tom &amp; Jerry</p><p>Next line</p>")

        assertEquals("Tom & Jerry\n\nNext line", document?.plainText)
        assertEquals("Tom &amp; Jerry<br><br>Next line", document?.toSafeHtml())
    }

    @Test
    fun unsupportedTagsAndScriptContentDoNotReachTheOutput() {
        val document = parseSynopsis("<script>alert('x')</script><a href='bad'>Keep this</a>")

        assertEquals("Keep this", document?.plainText)
        assertEquals("Keep this", document?.toSafeHtml())
    }

    @Test
    fun blankAndUnmatchedMarkupUseReadablePlainFallback() {
        assertEquals(null, parseSynopsis("   \n  "))
        assertEquals("This is **not closed", plainSynopsis("This is **not closed"))
    }
}
