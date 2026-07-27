package com.nilpo.contenttracker.ui.common

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContributorImageBitmapTest {
    @Test
    fun identifiesATransparentLogoCanvas() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.TRANSPARENT)
            for (x in 20 until 80) {
                for (y in 35 until 55) {
                    bitmap.setPixel(x, y, Color.MAGENTA)
                }
            }

            val metrics = bitmap.logoContentMetrics()!!

            assertEquals(3f, metrics.widthRatio, 0.0001f)
            assertTrue(metrics.hasTransparentPixels)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun identifiesAnOpaqueLogoTile() {
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(20, 40, 80))

            val metrics = bitmap.logoContentMetrics()!!

            assertEquals(2f, metrics.widthRatio, 0.0001f)
            assertFalse(metrics.hasTransparentPixels)
        } finally {
            bitmap.recycle()
        }
    }
}
