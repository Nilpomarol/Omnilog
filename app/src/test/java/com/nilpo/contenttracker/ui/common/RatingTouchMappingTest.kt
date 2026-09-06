package com.nilpo.contenttracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The star row is rated by position, so this mapping is the control: get it wrong by one slice and
 * every rating the user sets is half a point off what they touched.
 */
class RatingTouchMappingTest {
    private val width = 400f
    private val slice = width / 20f

    @Test
    fun `the left half of a star is the half point and the right half is the whole one`() {
        // Star 8 spans 140f..160f: its left half is 7,5 and its right half is 8.
        assertEquals(15, ratingHalfPointsAt(slice * 14.5f, width))
        assertEquals(16, ratingHalfPointsAt(slice * 15.5f, width))
    }

    @Test
    fun `every position gets an equal slice of the row`() {
        (1..20).forEach { position ->
            val middleOfSlice = slice * (position - 0.5f)
            assertEquals(position, ratingHalfPointsAt(middleOfSlice, width))
        }
    }

    @Test
    fun `the ends of the row are half a point and ten`() {
        assertEquals(1, ratingHalfPointsAt(0f, width))
        assertEquals(1, ratingHalfPointsAt(0.1f, width))
        assertEquals(20, ratingHalfPointsAt(width, width))
    }

    @Test
    fun `a drag past either end holds at the nearest rating`() {
        assertEquals(1, ratingHalfPointsAt(-80f, width))
        assertEquals(20, ratingHalfPointsAt(width + 80f, width))
    }

    @Test
    fun `a row with no width yet cannot report a rating above the lowest`() {
        assertEquals(1, ratingHalfPointsAt(50f, 0f))
    }
}
