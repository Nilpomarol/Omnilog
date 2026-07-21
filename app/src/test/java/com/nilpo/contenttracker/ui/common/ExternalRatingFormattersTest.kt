package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalRatingFormattersTest {
    @Test
    fun steamGameRatingsUseRoundedPercentages() {
        assertEquals(
            "98%",
            formatExternalRating(
                score = 98.019,
                maxScore = 100.0,
                mediaType = MediaType.Game,
                source = ExternalRatingSource.Steam,
            ),
        )
    }

    @Test
    fun nonSteamAndNonGameRatingsKeepTheTenPointScale() {
        assertEquals(
            "9,3/10",
            formatExternalRating(93.0, 100.0, MediaType.Game, ExternalRatingSource.Metacritic),
        )
        assertEquals(
            "9,8/10",
            formatExternalRating(98.0, 100.0, MediaType.Movie, ExternalRatingSource.Steam),
        )
    }

    @Test
    fun steamDescriptorsMapTheReturnedEnglishValueToCatalanResources() {
        assertEquals(
            R.string.steam_score_overwhelmingly_positive,
            steamScoreDescriptorResId("Overwhelmingly Positive"),
        )
        assertEquals(R.string.steam_score_mixed, steamScoreDescriptorResId("Mixed"))
        assertNull(steamScoreDescriptorResId("Unknown future value"))
    }
}
