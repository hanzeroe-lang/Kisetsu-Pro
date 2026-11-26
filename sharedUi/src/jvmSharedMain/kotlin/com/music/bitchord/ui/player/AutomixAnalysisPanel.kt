package com.music.bitchord.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.MixTransitionView
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * The Automix analysis sheet, in the style of Apple's music mixers: a floating
 * card pinned above the bottom of the screen, with two analysis lanes drawn
 * from the plan the crossfade controller is about to play. The lanes can be
 * panned and zoomed, and their handles edit the overlap, bass handoff and
 * filter ride that the next blend will actually use.
 */

private val AnalysisBackground = Color(0xFF0D0D0D)
private val AnalysisPanel = Color(0xFF191919)
private val AnalysisChip = Color(0xFF2B2B2D)
private val AnalysisSecondary = Color.White.copy(alpha = 0.55f)
private val HandoffColor = Color(0xFF34C759)
private val OverlapColor = Color(0xFFFF9F0A)
private val AirColor = Color(0xFFBF5AF2)
private val FadeColor = Color(0xFFFF5A4F)
private val WaveCream = Color(0xFFF2E4D0)
private val BadgeBackground = Color(0xFF45330F)

/** Where the planned overlap sits in each chart, as a fraction of its width. */
private const val OverlapStart = 0.42f
private const val OverlapEnd = 0.84f

/** The phrase lengths the bars control can pick: Auto, then whole 4/4 phrases. */
private val BarChoices = intArrayOf(0, 4, 8, 12, 16)

/** How long a finger must rest on a lane before a drag edits it rather than moving the sheet. */
private const val LONG_PRESS_MS = 220L

@Composable
internal fun AutomixAnalysisPanel(
    view: MixTransitionView?,
    progress: State<Float>,
    live: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phraseBars by PlayerSettings.automixPhraseBars.collectAsStateWithLifecycle()
    val automixOn by PlayerSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val overlapScale by PlayerSettings.automixOverlapScale.collectAsStateWithLifecycle()
    val bassSwap by PlayerSettings.automixBassSwapFraction.collectAsStateWithLifecycle()
    val filterSweep by PlayerSettings.automixFilterSweep.collectAsStateWithLifecycle()

    val outgoing = view?.outgoing
    val incoming = view?.incoming
    val bars = if (phraseBars > 0) phraseBars else (view?.beats ?: 0) / 4
    val barsLabel = if (bars > 0) "$bars bars" else "Auto"
    val badge = when {
        !automixOn -> "Fade"
        (view?.bassSwapAt ?: -1f) >= 0f -> "DJ blend"
        else -> "DJ filter"
    }
    val preset = when {
        !automixOn -> "Fade"
        phraseBars == 4 -> "Smooth"
        phraseBars == 16 -> "Fade"
        else -> "Auto"
    }

    fun setBars(value: Int) = PlayerSettings.setAutomixPhraseBars(value)
    fun cycleBars() {
        val index = BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it }
        setBars(BarChoices[(index + 1) % BarChoices.size])
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AnalysisBackground)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The grabber is only a visual cue now: the whole sheet handles swipes.
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Automix analysis",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = outgoing?.title?.takeIf { it.isNotBlank() } ?: "Analysing track",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = buildString {
                        append("Next: ")
                        append(incoming?.title?.takeIf { it.isNotBlank() } ?: "waiting for the queue")
                        incoming?.artist?.takeIf { it.isNotBlank() }?.let { append(" featuring ").append(it) }
                    },
                    color = AnalysisSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = badge,
                color = OverlapColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BadgeBackground)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        if (view != null) {
            TransitionChart(
                view, progress, live, top = true,
                overlapScale = overlapScale, bassSwap = bassSwap, filterSweep = filterSweep,
                phraseBars = phraseBars,
                onBars = ::setBars,
                onResize = { scale -> PlayerSettings.setAutomixOverlapScale(scale) },
                onSwap = { value -> PlayerSettings.setAutomixBassSwapFraction(value) },
                onSweep = { value -> PlayerSettings.setAutomixFilterSweep(value) },
            )
            Spacer(Modifier.height(10.dp))
            TransitionChart(
                view, progress, live, top = false,
                overlapScale = overlapScale, bassSwap = bassSwap, filterSweep = filterSweep,
                phraseBars = phraseBars,
                onBars = ::setBars,
                onResize = { scale -> PlayerSettings.setAutomixOverlapScale(scale) },
                onSwap = { value -> PlayerSettings.setAutomixBassSwapFraction(value) },
                onSweep = { value -> PlayerSettings.setAutomixFilterSweep(value) },
            )
        } else {
            Box(Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(14.dp)).background(AnalysisPanel))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(14.dp)).background(AnalysisPanel))
        }
        Spacer(Modifier.height(10.dp))

        LegendRow()
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AnalysisChip)
                    .clickable(onClick = ::cycleBars)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(barsLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("›", color = AnalysisSecondary, fontSize = 19.sp)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF3A2A10))
                    .clickable { InstantMixRequest.fire() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Instant mix", color = OverlapColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF12351F))
                    .clickable {
                        PlayerSettings.setSmartFadeEnabled(false)
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(HandoffColor))
                Spacer(Modifier.width(7.dp))
                Text("Stop", color = HandoffColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pinch to zoom · drag to scroll · drag the green/orange edges and the purple/red handles to edit the blend",
            color = AnalysisSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        Text(
            text = "Presets",
            color = AnalysisSecondary,
            fontSize = 13.sp,
            modifier = Modifier.background(AnalysisBackground).padding(horizontal = 10.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetChip("✦  Auto", preset == "Auto", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(true)
                setBars(0)
                PlayerSettings.resetAutomixEdits()
            }
            PresetChip("Smooth cut", preset == "Smooth", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(true)
                setBars(4)
                PlayerSettings.setAutomixFilterSweep(0f)
            }
            PresetChip("Fade handoff", preset == "Fade", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(true)
                setBars(16)
                PlayerSettings.setAutomixBassSwapFraction(0.7f)
                PlayerSettings.setAutomixFilterSweep(1f)
            }
        }
        Spacer(Modifier.height(12.dp))

        SettingRow(HandoffColor, "Blend", badge)
        SettingRow(AirColor, "Overlap", barsLabel)
        SettingRow(AirColor, "Bass swap", if (bassSwap >= 0f) "${(bassSwap * 100).roundToInt()}%" else "Auto")
        SettingRow(OverlapColor, "Filter", if (filterSweep >= 0f) "${(filterSweep * 100).roundToInt()}%" else "Auto")
        Text(
            text = "Reset edits",
            color = AirColor,
            fontSize = 13.sp,
            modifier = Modifier
                .clickable { PlayerSettings.resetAutomixEdits() }
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color(0xFF111111) else Color.White,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White else AnalysisChip)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun SettingRow(color: Color, title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 3.dp, height = 20.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(value, color = AnalysisSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun LegendRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LegendDot(HandoffColor, "Handoff")
        Spacer(Modifier.width(14.dp))
        LegendDot(OverlapColor, "Overlap")
        Spacer(Modifier.width(14.dp))
        LegendDot(AirColor, "Air")
        Spacer(Modifier.width(14.dp))
        LegendDot(FadeColor, "Fade in")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = AnalysisSecondary, fontSize = 13.sp)
    }
}

/** Which part of the lane a one-finger drag grabbed. */
private enum class ChartGesture { PAN, RESIZE_START, RESIZE_END, SWAP, SWEEP, ZOOM }

/**
 * One analysis lane with pan/zoom and handle editing. A single finger drags a
 * handle when it starts on one, otherwise it scrolls the view; two fingers
 * pinch to zoom. Edits are committed when the finger lifts.
 */
@Composable
private fun TransitionChart(
    view: MixTransitionView,
    progress: State<Float>,
    live: Boolean,
    top: Boolean,
    overlapScale: Float,
    bassSwap: Float,
    filterSweep: Float,
    phraseBars: Int,
    onBars: (Int) -> Unit,
    onResize: (Float) -> Unit,
    onSwap: (Float) -> Unit,
    onSweep: (Float) -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(overlapScale) }
    var swap by remember { mutableFloatStateOf(if (bassSwap < 0f) 0.7f else bassSwap) }
    var sweep by remember { mutableFloatStateOf(if (filterSweep < 0f) 0.35f else filterSweep) }
    var barIndex by remember {
        mutableIntStateOf(BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it })
    }
    LaunchedEffect(phraseBars) {
        barIndex = BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it }
    }
    LaunchedEffect(overlapScale) { scale = overlapScale }
    LaunchedEffect(bassSwap) { if (bassSwap >= 0f) swap = bassSwap }
    LaunchedEffect(filterSweep) { if (filterSweep >= 0f) sweep = filterSweep }

    val height = if (top) 156.dp else 164.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(AnalysisPanel)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val width = size.width.toFloat()
                        val laneHeight = size.height.toFloat()
                        // A quick swipe belongs to the sheet. Only a hold-then-drag
                        // (or a two-finger pinch) arms editing, so scrolling the
                        // analysis never changes it by accident.
                        var armed = false
                        var mode = ChartGesture.PAN
                        var last = down.position
                        var startDistance = 0f
                        var startZoom = zoom
                        var startPan = pan
                        val startScale = scale
                        val startSwap = swap
                        val startSweep = sweep
                        val startBarIndex = barIndex
                        if (down.position.x withinHandlesOf width) {
                            mode = if (abs(down.position.x - width * OverlapStart) <
                                abs(down.position.x - width * OverlapEnd)
                            ) ChartGesture.RESIZE_START else ChartGesture.RESIZE_END
                        } else if (!top && down.position.y < laneHeight * 0.52f) {
                            mode = ChartGesture.SWAP
                        } else if (!top) {
                            mode = ChartGesture.SWEEP
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (!armed) {
                                val elapsed = pressed[0].uptimeMillis - down.uptimeMillis
                                val moved = distance(pressed[0].position, down.position)
                                when {
                                    pressed.size >= 2 -> armed = true
                                    elapsed >= LONG_PRESS_MS && moved < 12f -> armed = true
                                    moved > 12f -> break // leave the gesture to the sheet
                                    else -> continue
                                }
                            }
                            if (pressed.size >= 2 && mode != ChartGesture.ZOOM) {
                                mode = ChartGesture.ZOOM
                                startZoom = zoom
                                startPan = pan
                                startDistance = distance(pressed[0].position, pressed[1].position)
                                last = pressed[0].position
                            }
                            when (mode) {
                                ChartGesture.ZOOM -> {
                                    if (pressed.size >= 2) {
                                        val distance = distance(pressed[0].position, pressed[1].position)
                                        if (startDistance > 0f) {
                                            zoom = (startZoom * distance / startDistance).coerceIn(1f, 4f)
                                            pan = 0f.coerceIn(0f, 1f - 1f / zoom)
                                        }
                                    }
                                }
                                ChartGesture.RESIZE_START -> {
                                    scale = (startScale - (pressed[0].position.x - down.position.x) / width).coerceIn(0.5f, 2f)
                                }
                                ChartGesture.RESIZE_END -> {
                                    scale = (startScale + (pressed[0].position.x - down.position.x) / width).coerceIn(0.5f, 2f)
                                }
                                ChartGesture.SWAP -> {
                                    swap = (startSwap + (down.position.y - pressed[0].position.y) / laneHeight).coerceIn(0.05f, 0.95f)
                                }
                                ChartGesture.SWEEP -> {
                                    sweep = (startSweep + (down.position.y - pressed[0].position.y) / laneHeight).coerceIn(0f, 1f)
                                }
                                ChartGesture.PAN -> {
                                    if (top) {
                                        // The top lane's bars are themselves a control:
                                        // hold and slide sideways to change the phrase.
                                        barIndex = (startBarIndex +
                                            ((pressed[0].position.x - down.position.x) / 64f).roundToInt())
                                            .coerceIn(0, BarChoices.lastIndex)
                                    } else if (zoom > 1f) {
                                        pan = (startPan - (pressed[0].position.x - down.position.x) / (width * zoom))
                                            .coerceIn(0f, 1f - 1f / zoom)
                                    }
                                }
                            }
                            last = pressed[0].position
                            event.changes.forEach { it.consume() }
                        }
                        if (scale != overlapScale) onResize(scale)
                        if (top && barIndex != startBarIndex) onBars(BarChoices[barIndex])
                        if (!top && swap != bassSwap) onSwap(swap)
                        if (!top && sweep != filterSweep) onSweep(sweep)
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val visible = 1f / zoom
            val viewLeft = w * pan * visible
            val viewWidth = w * visible

            drawRect(
                color = Color(0xFF6B4A2A).copy(alpha = 0.42f),
                topLeft = Offset(w * OverlapStart, 0f),
                size = Size(w * (OverlapEnd - OverlapStart), h),
            )

            val marks = view.beatMarks
            val interval = if (marks.size >= 2 && marks[1] > marks[0]) {
                (marks[1] - marks[0]) * w * (OverlapEnd - OverlapStart)
            } else {
                w * 0.045f
            }
            var gridX = -interval * 8f
            var gridIndex = 0
            while (gridX < w) {
                val x = viewLeft + gridX * zoom
                if (x in 0f..w && zoom == 1f) {
                    val strong = gridIndex % 4 == 0
                    drawLine(
                        color = Color.White.copy(alpha = if (strong) 0.22f else 0.11f),
                        start = Offset(x, h * 0.06f),
                        end = Offset(x, h * 0.94f),
                        strokeWidth = if (strong) 1.2f else 0.8f,
                    )
                }
                gridX += interval
                gridIndex++
            }

            if (top) {
                drawGainCurve(view, w, h, scale)
                val barX = w * (0.06f + 0.88f * (barIndex / BarChoices.lastIndex.toFloat()))
                drawRoundRect(
                    HandoffColor.copy(alpha = 0.95f),
                    topLeft = Offset(barX - 9f, h - 10f),
                    size = Size(18f, 5f),
                    cornerRadius = CornerRadius(2f),
                )
            } else {
                drawWaveforms(view, w, h, zoom, pan)
                drawFadeCurves(view, w, h, scale, swap, sweep)
            }

            if (live && !top && zoom == 1f) {
                val x = w * (OverlapStart + (OverlapEnd - OverlapStart) * progress.value.coerceIn(0f, 1f))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, h), 1.2f)
            }

            drawLine(HandoffColor, Offset(w * OverlapStart, 0f), Offset(w * OverlapStart, h), 3f)
            drawLine(OverlapColor, Offset(w * OverlapEnd, 0f), Offset(w * OverlapEnd, h), 3f)
            drawRoundRect(
                HandoffColor,
                topLeft = Offset(w * OverlapStart - 6f, h - 5f),
                size = Size(12f, 5f),
                cornerRadius = CornerRadius(2f),
            )
            drawRoundRect(
                OverlapColor,
                topLeft = Offset(w * OverlapEnd - 6f, 0f),
                size = Size(12f, 5f),
                cornerRadius = CornerRadius(2f),
            )

            if (!top) {
                val swapX = w * (OverlapStart + (OverlapEnd - OverlapStart) * swap)
                val swapY = h * 0.62f - h * 0.48f * smoothstep(smoothstep(swap))
                drawCircle(AirColor, radius = 7f, center = Offset(swapX, swapY))
                val sweepX = w * (OverlapStart + (OverlapEnd - OverlapStart) * 0.72f)
                val sweepY = h * 0.95f - h * 0.70f * smoothstep(sweep)
                drawCircle(FadeColor, radius = 7f, center = Offset(sweepX, sweepY))
            }
        }
    }
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private infix fun Float.withinHandlesOf(width: Float): Boolean =
    abs(this - width * OverlapStart) < 44f || abs(this - width * OverlapEnd) < 44f

/** The outgoing fader, stressed or relaxed by the listener's overlap edit. */
private fun DrawScope.drawGainCurve(view: MixTransitionView, w: Float, h: Float, scale: Float) {
    val fade = view.fadeOut
    val path = Path()
    for (i in 0..120) {
        val x = w * i / 120f
        val t = ((x - w * OverlapStart) / (w * (OverlapEnd - OverlapStart))).coerceIn(0f, 1f)
        val y = when {
            x <= w * OverlapStart -> h * 0.22f
            x <= w * OverlapEnd -> {
                val shaped = (sample(fade, t) * scale).coerceIn(0f, 1f)
                h * 0.22f + (h * 0.58f) * (1f - shaped)
            }
            else -> h * 0.80f
        }
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, FadeColor, style = Stroke(width = 2.4f, join = StrokeJoin.Round))
}

/** Both decks' waveforms, with the view panned and zoomed by the listener. */
private fun DrawScope.drawWaveforms(
    view: MixTransitionView,
    w: Float,
    h: Float,
    zoom: Float,
    pan: Float,
) {
    val centre = h * 0.62f
    val half = h * 0.32f
    fun draw(deckMid: FloatArray, deckHigh: FloatArray, dimBefore: Boolean) {
        val bins = deckMid.size
        if (bins == 0) {
            drawLine(WaveCream.copy(alpha = 0.35f), Offset(0f, centre), Offset(w, centre), 1f)
            return
        }
        val baseStep = w / bins
        val step = baseStep * zoom
        val shift = pan * w * zoom
        for (bin in 0 until bins) {
            val screenX = bin * step - shift
            if (screenX + step < 0f || screenX > w) continue
            val energy = (deckMid[bin] * 0.7f + deckHigh[bin] * 0.3f).coerceIn(0f, 1f)
            if (energy <= 0.02f) continue
            val extent = half * energy
            val inBlend = screenX in w * OverlapStart..w * OverlapEnd
            val alpha = when {
                dimBefore && screenX < w * OverlapStart -> 0.26f
                !dimBefore && screenX > w * OverlapEnd -> 0.26f
                inBlend -> 0.9f
                else -> 0.62f
            }
            drawRect(
                color = WaveCream.copy(alpha = alpha),
                topLeft = Offset(screenX, centre - extent),
                size = Size(step + 0.6f, extent * 2f),
            )
        }
    }
    draw(view.outgoing.mid, view.outgoing.high, dimBefore = false)
    draw(view.incoming.mid, view.incoming.high, dimBefore = true)
}

/** The editable air (purple) and fade (red) curves, sampled from the plan's fader. */
private fun DrawScope.drawFadeCurves(
    view: MixTransitionView,
    w: Float,
    h: Float,
    scale: Float,
    swap: Float,
    sweep: Float,
) {
    val fade = view.fadeIn
    val air = Path()
    val red = Path()
    val left = w * OverlapStart
    val right = w * OverlapEnd
    for (i in 0..120) {
        val x = w * i / 120f
        val t = ((x - left) / (right - left)).coerceIn(0f, 1f)
        val level = when {
            x < left -> 0f
            x > right -> (1f - (x - right) / (w - right).coerceAtLeast(1f)).coerceIn(0f, 1f)
            else -> (sample(fade, t) * scale).coerceIn(0f, 1f)
        }
        val airShape = smoothstep(level) * (0.55f + 0.45f * swap)
        val redShape = smoothstep(level) * (0.55f + 0.45f * sweep)
        val airY = h * 0.62f - (h * 0.48f) * airShape
        val redY = h * 0.95f - (h * 0.70f) * redShape
        if (i == 0) {
            air.moveTo(x, airY)
            red.moveTo(x, redY)
        } else {
            air.lineTo(x, airY)
            red.lineTo(x, redY)
        }
    }
    drawPath(air, AirColor, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
    drawPath(red, FadeColor, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
}

/** [curve] read at [at], linearly between its samples. */
private fun sample(curve: FloatArray, at: Float): Float {
    if (curve.isEmpty()) return 1f
    val position = at * (curve.size - 1)
    val index = position.toInt().coerceIn(0, curve.size - 1)
    val next = (index + 1).coerceAtMost(curve.size - 1)
    return curve[index] + (curve[next] - curve[index]) * (position - index)
}

private fun smoothstep(value: Float): Float {
    val v = value.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}
