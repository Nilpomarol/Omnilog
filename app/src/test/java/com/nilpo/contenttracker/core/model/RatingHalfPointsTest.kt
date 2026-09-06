package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingHalfPointsTest {
    @Test
    fun `a provider's whole points survive the round trip`() {
        (1..10).forEach { wholePoints ->
            val halfPoints = RatingHalfPoints.fromWholePoints(wholePoints)
            assertEquals(wholePoints, RatingHalfPoints.toWholePoints(halfPoints))
            assertEquals(wholePoints.toDouble(), RatingHalfPoints.toScore(halfPoints), 0.0)
        }
    }

    @Test
    fun `a half point rounds up on the way out to a provider`() {
        // 7,5 has to leave as 7 or 8; up, so a rating does not sink a little on every trip.
        assertEquals(8, RatingHalfPoints.toWholePoints(15))
        assertEquals(1, RatingHalfPoints.toWholePoints(1))
        assertEquals(7.5, RatingHalfPoints.toScore(15), 0.0)
    }

    @Test
    fun `the scale ends at half a point and at ten`() {
        assertEquals(1, RatingHalfPoints.coerce(0))
        assertEquals(1, RatingHalfPoints.coerce(-4))
        assertEquals(20, RatingHalfPoints.coerce(21))
        assertEquals(15, RatingHalfPoints.coerce(15))
    }
}
