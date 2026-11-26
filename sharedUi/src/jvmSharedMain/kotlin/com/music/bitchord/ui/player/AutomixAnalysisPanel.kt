package com.music.bitchord.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import kotlin.math.roundToInt

/*
 * The Automix analysis card: the handoff, overlap and fades drawn from the same
 * plan the crossfade controller is about to play. It replaces the old two-deck
 * popup with the compact Apple/Spotify-style editor — a beat strip above, a
 * waveform lane below, and the controls that fix the overlap length.
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

    val outgoing = view?.outgoing
    val incoming = view?.incoming
    val bars = if (phraseBars > 0) {
        phraseBars
    } else {
        (view?.beats ?: 0) / 4
    }
    val barsLabel = if (bars > 0) "$bars bars" else "Auto"
    val badge = when {
        !automixOn -> "Fade"
        (view?.bassSwapAt ?: -1f) >= 0f -> "DJ blend"
        else -> "DJ filter"
    }
    val preset = when {
        !automixOn -> "Fade"
        phraseBars > 0 -> "Custom"
        else -> "Auto"
    }

    fun setBars(value: Int) = PlayerSettings.setAutomixPhraseBars(value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AnalysisBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The sheet's grabber.
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f)),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Automix analysis",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

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
        Spacer(Modifier.height(14.dp))

        if (view != null) {
            TransitionChart(view, progress, live, top = true, onResize = {
                val index = BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it }
                setBars(BarChoices[(index + 1) % BarChoices.size])
            })
            Spacer(Modifier.height(10.dp))
            TransitionChart(view, progress, live, top = false, onResize = {
                val index = BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it }
                setBars(BarChoices[(index + 1) % BarChoices.size])
            })
        } else {
            Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(14.dp)).background(AnalysisPanel))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(14.dp)).background(AnalysisPanel))
        }
        Spacer(Modifier.height(12.dp))

        LegendRow()
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AnalysisChip)
                    .clickable {
                        val index = BarChoices.indexOf(phraseBars).let { if (it < 0) 0 else it }
                        setBars(BarChoices[(index + 1) % BarChoices.size])
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(barsLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("›", color = AnalysisSecondary, fontSize = 19.sp)
            }
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF12351F))
                    .clickable {
                        PlayerSettings.setSmartFadeEnabled(false)
                        onDismiss()
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(HandoffColor))
                Spacer(Modifier.width(9.dp))
                Text("Stop", color = HandoffColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Drag an edge of the overlap to change its length · use the bars control for a fixed phrase",
            color = AnalysisSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Text(
            text = "Presets",
            color = AnalysisSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .background(AnalysisBackground)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetChip("✦  Auto", preset == "Auto", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(true)
                setBars(0)
            }
            PresetChip("Custom", preset == "Custom", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(true)
                setBars(12)
            }
            PresetChip("Fade", preset == "Fade", Modifier.weight(1f)) {
                PlayerSettings.setSmartFadeEnabled(false)
                setBars(0)
            }
        }
        Spacer(Modifier.height(18.dp))

        SettingRow(HandoffColor, "Blend", badge)
        SettingRow(AirColor, "Overlap", barsLabel)
        SettingRow(AirColor, "EQ", "Air")
        SettingRow(OverlapColor, "Filter", "Auto")
        Spacer(Modifier.height(8.dp))
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
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
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

/**
 * One analysis lane. The top one carries the beat grid and the outgoing gain;
 * the lower one carries the waveform and the fade/eq curves. Both share the
 * same handoff (green) and end (orange) markers.
 */
@Composable
private fun TransitionChart(
    view: MixTransitionView,
    progress: State<Float>,
    live: Boolean,
    top: Boolean,
    onResize: () -> Unit,
) {
    var draggingEdge by remember { mutableStateOf(false) }
    val height = if (top) 156.dp else 164.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(AnalysisPanel)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x
                        draggingEdge = abs(x - size.width * OverlapStart) < 48f ||
                            abs(x - size.width * OverlapEnd) < 48f
                    },
                    onDragEnd = { if (draggingEdge) onResize() },
                ) { _, _ -> Unit }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            val left = w * OverlapStart
            val right = w * OverlapEnd

            // The overlap window, shaded behind everything else.
            drawRect(
                color = Color(0xFF6B4A2A).copy(alpha = 0.42f),
                topLeft = Offset(left, 0f),
                size = Size(right - left, h),
            )

            val marks = view.beatMarks
            val interval = if (marks.size >= 2 && marks[1] > marks[0]) {
                (marks[1] - marks[0]) * (right - left)
            } else {
                w * 0.045f
            }
            val gridColor = Color.White.copy(alpha = 0.17f)
            val gridStart = left - interval * 8f
            var gridX = gridStart
            var gridIndex = 0
            while (gridX < w) {
                if (gridX >= 0f) {
                    val strong = gridIndex % 4 == 0
                    drawLine(
                        color = gridColor.copy(alpha = if (strong) 0.22f else 0.11f),
                        start = Offset(gridX, h * 0.06f),
                        end = Offset(gridX, h * 0.94f),
                        strokeWidth = (if (strong) 1.2f else 0.8f),
                    )
                }
                gridX += interval
                gridIndex++
            }

            if (top) {
                drawGainCurve(view, w, h, left, right)
            } else {
                drawWaveforms(view, w, h, left, right)
                drawFadeCurves(view, w, h, left, right)
            }

            if (live && !top) {
                val x = left + (right - left) * progress.value.coerceIn(0f, 1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.2f,
                )
            }

            // The two edges of the overlap.
            drawLine(HandoffColor, Offset(left, 0f), Offset(left, h), 3f)
            drawLine(OverlapColor, Offset(right, 0f), Offset(right, h), 3f)
            drawRoundRect(
                HandoffColor,
                topLeft = Offset(left - 6f, h - 5f),
                size = Size(12f, 5f),
                cornerRadius = CornerRadius(2f),
            )
            drawRoundRect(
                OverlapColor,
                topLeft = Offset(right - 6f, 0f),
                size = Size(12f, 5f),
                cornerRadius = CornerRadius(2f),
            )
        }
    }
}

/** The outgoing fader across the lane: flat before the handoff, falling through the blend. */
private fun DrawScope.drawGainCurve(view: MixTransitionView, w: Float, h: Float, left: Float, right: Float) {
    val fade = view.fadeOut
    val path = Path()
    for (i in 0..120) {
        val x = w * i / 120f
        val y = when {
            x <= left -> h * 0.22f
            x <= right -> {
                val t = ((x - left) / (right - left)).coerceIn(0f, 1f)
                h * 0.22f + (h * 0.58f) * (1f - sample(fade, t))
            }
            else -> h * 0.80f
        }
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, FadeColor, style = Stroke(width = 2.4f, join = StrokeJoin.Round))
}

/** Both decks' waveforms, with the silent stretches outside the overlap dimmed. */
private fun DrawScope.drawWaveforms(view: MixTransitionView, w: Float, h: Float, left: Float, right: Float) {
    val centre = h * 0.62f
    val half = h * 0.32f
    drawWave(view.outgoing.low, view.outgoing.mid, view.outgoing.high, w, centre, half) { x ->
        if (x > right) 0.26f else if (x > left) 0.9f else 0.62f
    }
    drawWave(view.incoming.low, view.incoming.mid, view.incoming.high, w, centre, half) { x ->
        if (x < left) 0.26f else if (x < right) 0.9f else 0.62f
    }
}

private fun DrawScope.drawWave(
    low: FloatArray,
    mid: FloatArray,
    high: FloatArray,
    w: Float,
    centre: Float,
    half: Float,
    alphaAt: (Float) -> Float,
) {
    val bins = mid.size
    if (bins == 0) {
        drawLine(WaveCream.copy(alpha = 0.35f), Offset(0f, centre), Offset(w, centre), 1f)
        return
    }
    val step = w / bins
    val bar = step + 0.6f
    for (bin in 0 until bins) {
        val x = step * bin
        val energy = (mid[bin] * 0.7f + high[bin] * 0.3f).coerceIn(0f, 1f)
        if (energy <= 0.02f) continue
        val extent = half * energy
        drawRect(
            color = WaveCream.copy(alpha = alphaAt(x + step / 2f)),
            topLeft = Offset(x, centre - extent),
            size = Size(bar, extent * 2f),
        )
    }
}

/** The purple EQ/air lift and the red fade-in, sampled from the plan's own fader. */
private fun DrawScope.drawFadeCurves(view: MixTransitionView, w: Float, h: Float, left: Float, right: Float) {
    val fade = view.fadeIn
    val air = Path()
    val red = Path()
    for (i in 0..120) {
        val x = w * i / 120f
        val t = ((x - left) / (right - left)).coerceIn(0f, 1f)
        val level = if (x < left) 0f else if (x > right) (1f - (x - right) / (w - right).coerceAtLeast(1f)).coerceIn(0f, 1f) else sample(fade, t)
        val airY = h * 0.62f - (h * 0.48f) * smoothstep(level)
        val redY = h * 0.95f - (h * 0.70f) * smoothstep(level)
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
