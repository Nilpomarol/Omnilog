package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The wash between cover art and the text laid over it.
 *
 * Every cover carousel — Home, Stats, related media, external recommendations — drew its own copy
 * of this gradient, and the copies had drifted to four different middle alphas (0.10 to 0.16) and
 * two different stop counts. Nothing chose those differences; they are copy-paste noise, and at
 * these values no one can see them apart.
 *
 * The reason to share it is not tidiness. The scrim is deliberately dark in **both** themes, since
 * its whole job is to darken artwork and a pale scrim could not. That makes it a trap: text on top
 * of it must use [com.nilpo.contenttracker.ui.theme.OnCoverInk] rather than the theme's `appInk`,
 * which inverts to near-black on light and vanishes. All four carousels fell into that trap. Having
 * one scrim keeps the rule and the thing it applies to in the same place.
 */
@Composable
fun CoverScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ScrimTint.copy(alpha = 0.15f),
                        ScrimTint.copy(alpha = 0.62f),
                        ScrimFoot.copy(alpha = 0.98f),
                    ),
                ),
            ),
    )
}

private val ScrimTint = Color(0xFF17110D)
private val ScrimFoot = Color(0xFF15110E)
