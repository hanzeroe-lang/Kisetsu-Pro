package com.music.bitchord.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

private val BOTTOM_FADE_HEIGHT = 180.dp
private val BOTTOM_FADE_HEIGHT_WITH_MINI_PLAYER = 254.dp
private const val BOTTOM_FOOT_FADE_STOPS = 8
private const val BOTTOM_FOOT_FADE_START = 0.6f

private val BottomFootFade = Brush.verticalGradient(
    colorStops = Array(BOTTOM_FOOT_FADE_STOPS + 1) { i ->
        val t = i / BOTTOM_FOOT_FADE_STOPS.toFloat()
        val at = BOTTOM_FOOT_FADE_START + (1f - BOTTOM_FOOT_FADE_START) * t
        // Fade in from top of strip to bottom: transparent at top, opaque toward bottom.
        at to Color.Black.copy(alpha = EaseInOut.transform(t))
    },
)

@OptIn(ExperimentalHazeApi::class)
@Composable
fun BottomFadeBlur(
    hazeState: HazeState,
    pageColor: Color,
    withMiniPlayer: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val inset = floatingBarInsets.asPaddingValues().calculateBottomPadding()
    val height by animateDpAsState(
        targetValue = inset + if (withMiniPlayer) BOTTOM_FADE_HEIGHT_WITH_MINI_PLAYER else BOTTOM_FADE_HEIGHT,
        animationSpec = tween(220),
        label = "bottomBlurHeight",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(BottomFootFade, blendMode = BlendMode.DstIn)
            }
            .hazeEffect(state = hazeState) {
                backgroundColor = pageColor
                tints = emptyList()
                blurRadius = 24.dp
                noiseFactor = 0f
                inputScale = HazeInputScale.Fixed(0.33f)
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseInOut,
                    startIntensity = 0f,
                    endIntensity = 1f,
                )
            },
    )
}
