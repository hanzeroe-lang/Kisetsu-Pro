package com.music.bitchord.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Convx tokens used by the vendored floating nav bar. */
val LocalForcedDark = compositionLocalOf<Boolean?> { null }

object GlassTintTokens {
    val IconOpaqueBlackishLight: Color get() = Color(0xFF3C3C43)

    fun pluckColor(dark: Boolean, ambient: Color, highlight: Color = Color.Unspecified): Color =
        if (highlight.isSpecifiedColor()) highlight else if (dark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)

    fun pluckOpacity(dark: Boolean, hasAmbient: Boolean): Float = if (dark) 0.22f else 0.14f

    fun neutralTintColor(dark: Boolean): Color = if (dark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)

    private fun Color.isSpecifiedColor(): Boolean = this != Color.Unspecified
}
