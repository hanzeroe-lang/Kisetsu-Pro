package com.music.bitchord.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/** The search field's 46dp pill plus its 8dp lower gap. */
private val StickySearchHeight = 54.dp

/**
 * Blur-only backdrop for the pinned search field. The list scrolling beneath
 * it is frosted rather than hidden by a solid slab, so the field floats over
 * the content the same way the top and bottom bars do.
 *
 * The effect sits inside the page's own haze source, so [canDrawArea] lets it
 * sample that source rather than being limited to areas beneath its z-index.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun StickySearchBlur(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(StickySearchHeight)
            .hazeEffect(state = hazeState) {
                backgroundColor = Color.Transparent
                tints = emptyList()
                blurRadius = 18.dp
                noiseFactor = 0f
                inputScale = HazeInputScale.Fixed(0.33f)
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseInOut,
                    startIntensity = 1f,
                    endIntensity = 0f,
                )
                canDrawArea = { true }
            },
    )
}
