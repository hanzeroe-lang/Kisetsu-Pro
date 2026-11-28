package com.music.bitchord.ui.components.convxfloating

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.components.shapes.ContinuousRoundedRectangle
import com.music.bitchord.ui.components.liquidGlass as appLiquidGlass

/*
 * Compatibility layer for the vendored Convx FloatingNavBar / FloatingTabBar
 * sources. Their own backdrop, iOS theme and SF-symbol modules are not part of
 * this app; these adapters map them onto Kisetsu's Compose theme and real glass.
 */

const val AppBackgroundColorKey = "app_background_color"
const val AppTextColorKey = "app_text_color"
const val NavBarMeltTransitionKey = "nav_bar_melt_transition"

enum class SearchSource { LOCAL, ONLINE }

class PrefState<T>(private val state: MutableState<T>) : State<T> by state {
    operator fun component1(): T = state.value
    operator fun component2(): (T) -> Unit = { state.value = it }
    override var value: T
        get() = state.value
        set(value) { state.value = value }
}

@Composable
fun <T> rememberPreference(key: String, defaultValue: T): PrefState<T> =
    remember(key) { PrefState(mutableStateOf(defaultValue)) }

class AmbientColor(val value: Color)

val LocalAmbientBackgroundColor = compositionLocalOf { AmbientColor(Color.Unspecified) }
val LocalAccentColor = compositionLocalOf<Color> { Color(0xFF007AFF) }
val LocalAppleMusicUi = compositionLocalOf { true }
val LocalAppBackdrop = compositionLocalOf<Any?> { null }
val LocalAppBackdropFrozen = compositionLocalOf<() -> Boolean> { { false } }
val LocalSampledChromeDark = compositionLocalOf<Boolean?> { null }
val LocalGlassEffectConfig = compositionLocalOf { GlassEffectConfig() }

data class GlassEffectConfig(val surfaceOpacity: Float = 0.12f) {
    val highlightColor: Color get() = Color.White.copy(alpha = 0.25f)
    val puckColor: Color get() = Color.Unspecified
    val puckOpacity: Float get() = 0.9f
    fun isEnabledFor(component: GlassComponent): Boolean = true
}

enum class GlassComponent { NAV_BAR, OTHER }

object AppTheme {
    val colorScheme: androidx.compose.material3.ColorScheme
        @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme
    val typography: androidx.compose.material3.Typography
        @Composable get() = androidx.compose.material3.MaterialTheme.typography
}

object IosColors {
    val label: Color get() = Color.White
    val quaternarySystemFill: Color get() = Color.White.copy(alpha = 0.16f)
    val systemRed: Color get() = Color(0xFFFF453A)
}

object IosTypography {
    val body: TextStyle get() = TextStyle(fontSize = 14.sp)
    val caption2: TextStyle get() = TextStyle(fontSize = 10.sp)
}

object IosTheme {
    val tint: Color get() = Color(0xFF0A84FF)
    val colors: IosColors get() = IosColors
    val typography: IosTypography get() = IosTypography
}

@Composable
fun IosText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text, color = color, style = style, maxLines = 1, modifier = modifier)
}


fun Color.isHueless(): Boolean = false

fun glassUsesDarkAppearance(): Boolean = true

fun Color.asSoftIconTint(): Color = copy(alpha = 0.72f)

/** Convx's capsule, mapped to the app's continuous rounded rectangle. */
val NavContinuousCapsule: CornerBasedShape = ContinuousRoundedRectangle(percent = 50)
val IosCapsule: Shape get() = NavContinuousCapsule
val Shape.shadowShape: Shape get() = this

/** Real glass, using the app's backdrop-backed material. */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig,
    shape: CornerBasedShape,
    highlightAlpha: Float = 0.2f,
    frozen: () -> Boolean = { false },
    rimTintColor: Color = Color.Unspecified,
    outerShadow: Boolean = false,
): Modifier = this.appLiquidGlass(
    shape = shape,
    surfaceTint = rimTintColor,
)

/** No-op backdrop: the glass material samples the app's own backdrop. */
open class Backdrop
class LayerBackdrop : Backdrop()

@Composable
fun rememberLayerBackdrop(): LayerBackdrop = remember { LayerBackdrop() }

@Composable
fun rememberCombinedBackdrop(first: Any?, second: Any?): Backdrop = remember { Backdrop() }

fun Modifier.layerBackdrop(backdrop: Any?): Modifier = this

object BackdropEffectsScope : androidx.compose.ui.unit.Density {
    override val density: Float get() = 1f
    override val fontScale: Float get() = 1f

    fun vibrancy() = Unit
    fun blur(radius: Float) = Unit
    fun lens(
        refractionHeight: Float,
        refractionAmount: Float,
        depthEffect: Boolean = true,
        chromaticAberration: Boolean = true,
        chromaticAberrationAmount: Float = 0.5f,
    ) = Unit
}

@Composable
fun Modifier.drawBackdrop(
    backdrop: Any?,
    shape: () -> Shape,
    effects: BackdropEffectsScope.() -> Unit,
    highlight: Any? = null,
    shadow: Any? = null,
    innerShadow: Any? = null,
    layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
): Modifier = this

class Highlight(val width: Dp = 1.dp, val alpha: Float = 1f)
class InnerShadow(val radius: Dp = 0.dp, val color: Color = Color.Black, val alpha: Float = 1f)

class InteractiveHighlight(
    animationScope: Any,
    val position: ((androidx.compose.ui.unit.IntSize, Offset) -> Offset)? = null,
) {
    val modifier: Modifier = Modifier
    val gestureModifier: Modifier = Modifier
    val pressProgress: Float get() = 0f
}

class DampedDragAnimation(
    val animationScope: Any,
    initialValue: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    val visibilityThreshold: Float = 0.001f,
    val initialScale: Float = 1f,
    val pressedScale: Float = 1f,
    val canDrag: DampedDragAnimation.(Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.() -> Unit = {},
    val onDragStopped: DampedDragAnimation.() -> Unit = {},
    val onDrag: DampedDragAnimation.(Offset, Offset) -> Unit = { _, _ -> },
) {
    var value: Float = initialValue
    val targetValue: Float get() = value
    var currentIndex: Int = initialValue.toInt()
    var currentTabs: List<Any> = emptyList()
    val offsetAnimation = Animatable(0f)
    val velocity: Float get() = 0f
    val pressProgress: Float get() = 0f
    val scaleX: Float get() = 1f
    val scaleY: Float get() = 1f
    val modifier: Modifier = Modifier
    fun animateToValue(next: Float) { value = next.coerceIn(valueRange) }
    fun updateValue(next: Float) { value = next.coerceIn(valueRange) }
    fun snapValue(next: Float) { value = next.coerceIn(valueRange) }
}

fun Modifier.elasticStretchLayerBlock(highlight: InteractiveHighlight): androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {}

fun Modifier.gooey(amount: () -> Float): Modifier = this

@Composable
internal fun AccessoryHost(
    accessory: (@Composable (Modifier, Boolean) -> Unit)?,
    slots: AccessorySlots,
    targetVisual: () -> FloatingTabBarVisual,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    modifier: Modifier = Modifier,
) {
    if (accessory != null) {
        Box(modifier) { accessory(Modifier, targetVisual() != FloatingTabBarVisual.INLINE) }
    }
}

fun Modifier.bounceClick(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
fun Modifier.additiveChrome(enabled: Boolean): Modifier = this
fun Modifier.vibrantChrome(enabled: Boolean): Modifier = this
fun Color.asPuckTint(dark: Boolean): Color = this
val glassResolutionScale: Float get() = 1f

object Motion {
    val spring = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
}

/** SF-symbol stand-ins: the vendored bar uses these for its search affordances. */
fun sf(name: String): String = name

enum class SfRenderingMode { Monochrome, DualTone, Hierarchical }

@Composable
fun SymbolIcon(
    symbol: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    chrome: Float = 0f,
    mode: Any? = null,
    bounceOnEnter: Boolean = false,
    replayOn: Any? = null,
    selected: Boolean = false,
    size: Dp = 24.dp,
) {
    val glyph = when (symbol) {
        "arrow.left" -> "‹"
        "xmark" -> "×"
        "magnifyingglass" -> "⌕"
        else -> "●"
    }
    Text(glyph, color = tint, modifier = modifier, fontSize = size.value.sp)
}

class MenuIconChrome

class IosMenuSection
@Composable
fun IosContextMenuOverlay(
    sections: List<IosMenuSection>,
    anchor: androidx.compose.ui.geometry.Rect?,
    onDismissRequest: () -> Unit,
) = Unit

class NavSearchState(
    val keyboardActive: Boolean = false,
    val visualActive: Boolean = false,
    val query: TextFieldValue = TextFieldValue(""),
    val searchSource: SearchSource = SearchSource.ONLINE,
    val canToggleSource: Boolean = true,
    val focusRequester: androidx.compose.ui.focus.FocusRequester = androidx.compose.ui.focus.FocusRequester(),
    val onExit: () -> Unit = {},
    val onTapBar: () -> Unit = {},
    val onTapSearchIcon: () -> Unit = {},
    val onCloseKeyboard: () -> Unit = {},
    val onQueryChange: (TextFieldValue) -> Unit = {},
    val onSubmit: (String) -> Unit = {},
    val onToggleSource: () -> Unit = {},
)

val LocalNavSearchState = compositionLocalOf { NavSearchState() }

@Composable
fun DynamicSearchPlaceholder(
    searchSource: SearchSource = SearchSource.ONLINE,
    style: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize())
}

@Composable
fun FloatingMiniPlayer(
    isInline: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    onLyricsClick: (() -> Unit)?,
    onQueueClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().clickable(onClick = onClick))
}

object R {
    object string {
        const val dismiss = 1
    }
}

fun stringResource(id: Int): String = "Dismiss"

/** Safe stand-in for Convx drawable resources: no real IDs are shipped here. */
@Composable
fun painterResource(id: Int): androidx.compose.ui.graphics.painter.Painter =
    androidx.compose.ui.graphics.painter.ColorPainter(Color.White)

/** Convx's Screens, mapped onto Kisetsu's tab routes. */
sealed class Screens(val titleId: Int, val route: String, val icon: Int, val iosIcon: Int = icon) {
    object Home : Screens(1, "home", 1)
    object Search : Screens(2, "search_input", 2)
    object Radio : Screens(3, "radio", 3)
    object ListenTogether : Screens(4, "together", 4)
    object Songs : Screens(5, "songs", 5)
    object Library : Screens(6, "library", 6)
    object Explore : Screens(7, "explore", 7)
    object Settings : Screens(8, "settings", 8)

    fun icon(appleMusicUi: Boolean): Int = if (appleMusicUi) iosIcon else icon

    companion object {
        val MainScreens = listOf(Home, Search, Radio, Library)
        fun mainScreens(localOnly: Boolean) = if (localOnly) listOf(Home, Songs, Search, Library) else MainScreens
        val MainRoutes = (MainScreens + Songs).map { it.route }.toSet()
    }
}

fun isGlassAllowed(): Boolean = true

fun rememberStickySelectedRoute(currentRoute: String?, screens: List<Screens>): Any? =
    screens.firstOrNull { it.route == currentRoute }?.route ?: screens.firstOrNull()?.route

fun isRouteSelected(currentRoute: String?, route: String, all: List<Screens>): Boolean = currentRoute == route

/** Unqualified fold helpers used by FoldingTabBar. */
fun elasticStretchLayerBlock(glow: InteractiveHighlight): androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {}

class SpringEasing(zeta: Float) : androidx.compose.animation.core.Easing {
    override fun transform(fraction: Float): Float =
        androidx.compose.animation.core.FastOutSlowInEasing.transform(fraction.coerceIn(0f, 1f))
}

class BarAccessoryTiming(val durationMs: Int)

const val CollapseYDelayMs = 40

object BarMotion {
    const val CollapseYDelayMs = 40
    val CollapseAccessory = BarAccessoryTiming(180)
    val ExpandAccessory = BarAccessoryTiming(220)
    val BarSurfaceExpandSpring: androidx.compose.animation.core.Easing =
        androidx.compose.animation.core.FastOutSlowInEasing
    val BarSurfaceCollapseSpring: androidx.compose.animation.core.Easing =
        androidx.compose.animation.core.FastOutSlowInEasing
    val BarSurfaceExpandWidthSpring: androidx.compose.animation.core.Easing =
        androidx.compose.animation.core.FastOutSlowInEasing
    val Travel: androidx.compose.animation.core.Easing =
        androidx.compose.animation.core.FastOutSlowInEasing

    fun surface(
        durationMs: Int,
        easing: androidx.compose.animation.core.Easing,
        widthEasing: androidx.compose.animation.core.Easing? = null,
    ): androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.geometry.Rect> =
        androidx.compose.animation.core.tween(durationMs, easing = easing)
}

@Composable
fun FoldingTabGroup(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Any?,
    surfaceBackdrop: Any?,
    tabsCount: Int,
    accentColor: Color,
    containerColor: Color,
    height: Dp,
    indicatorVisible: Boolean,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.height(height).background(containerColor),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun FoldingTabCell(
    onClick: () -> Unit,
    onLongClick: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.clickable(onClick = onClick)) { content() }
}

class AccessorySlots(val expandedHeightPx: Int = 0, val inlineHeightPx: Int = 0)

internal fun accessorySlot(
    slots: AccessorySlots,
    visual: FloatingTabBarVisual,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
): Modifier = Modifier

class FoldGeometry(
    val navWidth: Int,
    val searchWidth: Int,
    val height: Int,
    val rowY: Int,
    val selectedX: Int,
    val searchX: Int,
    val playerX: Int,
    val playerY: Int,
)

fun foldGeometry(
    width: Int,
    expandedRowHeight: Int,
    collapsedRowHeight: Int,
    spacing: Int,
    playerHeight: Int,
    hasPlayer: Boolean,
    fold: Float,
    search: Float,
    tabsCount: Int,
    selectedIndex: Int,
    inset: Float,
): FoldGeometry {
    val rowHeight = androidx.compose.ui.util.lerp(expandedRowHeight.toFloat(), collapsedRowHeight.toFloat(), fold).toInt().coerceAtLeast(1)
    val searchWidth = (rowHeight * (1f - fold) + collapsedRowHeight * fold).toInt().coerceAtLeast(1)
    val navWidth = (width - searchWidth - spacing).coerceAtLeast(1)
    val selectedX = ((selectedIndex.coerceAtLeast(0)) * (navWidth / tabsCount.coerceAtLeast(1))).coerceAtMost(navWidth)
    return FoldGeometry(
        navWidth = navWidth,
        searchWidth = searchWidth,
        height = rowHeight + if (hasPlayer) playerHeight + spacing else 0,
        rowY = 0,
        selectedX = selectedX,
        searchX = width - searchWidth,
        playerX = 0,
        playerY = rowHeight + spacing,
    )
}
