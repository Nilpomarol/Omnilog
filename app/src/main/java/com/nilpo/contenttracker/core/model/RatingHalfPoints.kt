package com.nilpo.contenttracker.core.model

/**
 * Personal ratings are stored in half points: 15 means 7,5 out of 10.
 *
 * The scale lives in one place because it has two edges. Inside the app a rating is always half
 * points, from [Min] (0,5) to [Max] (10). At a provider boundary it is always whole points, because
 * MyAnimeList, IMDb and StoryGraph all score out of ten without halves — [fromWholePoints] and
 * [toWholePoints] are the only sanctioned crossings.
 */
object RatingHalfPoints {
    const val Min: Int = 1
    const val Max: Int = 20

    /** A provider's whole-point score as half points. */
    fun fromWholePoints(wholePoints: Int): Int = wholePoints * 2

    /**
     * Half points as a whole-point score for a provider, rounding halves up.
     *
     * A provider cannot hold 7,5, so sending one loses the half either way. Rounding up keeps the
     * rating from drifting down every time it makes the trip.
     */
    fun toWholePoints(halfPoints: Int): Int = (halfPoints + 1) / 2

    /** Half points as a score out of ten, for display and for averages. */
    fun toScore(halfPoints: Int): Double = halfPoints / 2.0

    fun coerce(halfPoints: Int): Int = halfPoints.coerceIn(Min, Max)
}
