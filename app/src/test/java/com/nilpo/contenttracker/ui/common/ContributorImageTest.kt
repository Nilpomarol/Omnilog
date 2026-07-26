package com.nilpo.contenttracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ContributorImageTest {
    @Test
    fun sizesALogoBoxToTheLoadedImagesProportions() {
        assertEquals(2f, contributorLogoWidthRatio(400f, 200f), Tolerance)
        assertEquals(1.5f, contributorLogoWidthRatio(300f, 200f), Tolerance)
    }

    @Test
    fun neverLetsOneExtremeLogoSqueezeARowOrRunOffIt() {
        assertEquals(MaxLogoWidthRatio, contributorLogoWidthRatio(2000f, 100f), Tolerance)
        assertEquals(MinLogoWidthRatio, contributorLogoWidthRatio(50f, 400f), Tolerance)
    }

    @Test
    fun fallsBackToSquareWhenTheDimensionsAreUnusable() {
        assertEquals(DefaultLogoWidthRatio, contributorLogoWidthRatio(0f, 0f), Tolerance)
        assertEquals(DefaultLogoWidthRatio, contributorLogoWidthRatio(100f, 0f), Tolerance)
        assertEquals(DefaultLogoWidthRatio, contributorLogoWidthRatio(-10f, 100f), Tolerance)
    }

    /** Coil reports an unbounded intrinsic size for some painters. */
    @Test
    fun fallsBackToSquareForNonFiniteDimensions() {
        assertEquals(
            DefaultLogoWidthRatio,
            contributorLogoWidthRatio(Float.POSITIVE_INFINITY, 100f),
            Tolerance,
        )
        assertEquals(DefaultLogoWidthRatio, contributorLogoWidthRatio(100f, Float.NaN), Tolerance)
    }

    @Test
    fun trustsTheProvidersOriginalLogoRatioOverItsResizedDeliveryCanvas() {
        // IGDB's delivery canvas reports ~1.775 for every logo, including this 3:1 source logo.
        assertEquals(MaxLogoWidthRatio, resolvedLogoWidthRatio(3f, 1.775f), Tolerance)
    }

    @Test
    fun fallsBackToTheLoadedImageWhenTheProviderDidNotSupplyUsableDimensions() {
        assertEquals(1.775f, resolvedLogoWidthRatio(null, 1.775f), Tolerance)
        assertEquals(1.775f, resolvedLogoWidthRatio(0f, 1.775f), Tolerance)
    }
}

private const val Tolerance = 0.0001f
