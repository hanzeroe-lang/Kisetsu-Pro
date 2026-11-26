package com.music.bitchord.ui.components.convxnav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The small slice of Convx's iOS foundation that IosTabBar needs. The tab bar
 * itself is the vendored Convx source; these are its material tokens expressed
 * in the app's Compose theme instead of Convx's own iOS compat layer.
 */

/** SwiftUI's Capsule. */
val IosCapsule: Shape = RoundedCornerShape(percent = 50)

enum class IosGlassStyle { Regular, Thin }
enum class IosGlassSpec { Chrome, Clear }

/** A light glass capsule: the real Convx material is a full backdrop effect. */
fun Modifier.iosGlass(shape: Shape, style: IosGlassStyle, spec: IosGlassSpec): Modifier = this
    .background(
        if (spec == IosGlassSpec.Clear) Color.White.copy(alpha = 0.07f)
        else Color.White.copy(alpha = 0.12f),
        shape,
    )
    .border(0.5.dp, Color.White.copy(alpha = 0.16f), shape)

object IosColors {
    val label: Color get() = Color.White
    val quaternarySystemFill: Color get() = Color.White.copy(alpha = 0.16f)
    val systemRed: Color get() = Color(0xFFFF453A)
}

object IosTypography {
    val body: TextStyle get() = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val caption2: TextStyle get() = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium)
}

object IosTheme {
    val tint: Color get() = Color(0xFF0A84FF)
    val colors: IosColors get() = IosColors
    val typography: IosTypography get() = IosTypography
}

val LocalIosContentColor = compositionLocalOf<Color> { Color.White }
val LocalIosTextStyle = compositionLocalOf<TextStyle> { IosTypography.body }

@Composable
fun IosText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text = text, color = color, style = style, maxLines = 1, modifier = modifier)
}
