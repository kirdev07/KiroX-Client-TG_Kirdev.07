package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.components.topHighlight
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

/** Shared drawing helpers of the media section (Music, calls, players, stories editor…). */

/** CSS `linear-gradient(<deg>, c1, c2 <stop>…)` as a background. Stops: color to position (0..1). */
fun Modifier.cssGrad(deg: Float, vararg stops: Pair<Float, Color>): Modifier = drawBehind {
    drawRect(Brush.linearGradient(*stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

fun Modifier.cssGrad(deg: Float, from: Color, to: Color, toAt: Float = 1f): Modifier = cssGrad(deg, 0f to from, toAt to to)

/**
 * CSS `radial-gradient(rx% ry% at cx% cy%, color, transparent stop%)` over the element:
 * an ellipse centered at (cx, cy) fractions with radii (rx, ry) fractions of width/height.
 */
fun Modifier.ellipseGlow(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, stop: Float = 1f): Modifier = drawBehind {
    val c = Offset(size.width * cx, size.height * cy)
    val rX = size.width * rx; val rY = size.height * ry
    if (rX <= 0f || rY <= 0f) return@drawBehind
    scale(1f, rY / rX, pivot = c) {
        drawRect(Brush.radialGradient(0f to color, stop to color.copy(alpha = 0f), center = c, radius = rX),
            topLeft = Offset(0f, c.y - (c.y) * rX / rY), size = androidx.compose.ui.geometry.Size(size.width, size.height * rX / rY + 1f))
    }
}

/** Round glass button of the boards: rgba(255,255,255,a) fill, 1px border, optional inset highlight. */
@Composable
fun MRoundBtn(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 24.dp,
              bg: Color = Kx.w(.06f), border: Color? = Kx.w(.09f), highlight: Color? = null, tint: Color = Kx.text,
              shape: Shape = CircleShape, onClick: () -> Unit = {}) {
    Box(
        modifier.size(size).clip(shape).background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (highlight != null) Modifier.topHighlight(highlight) else Modifier)
            .kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/** Text style shortcut: size, weight, color. */
fun ts(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text): TextStyle = KxType.style(size, weight, color)

@Composable
fun T(text: String, size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(text, style = ts(size, weight, color), modifier = modifier, maxLines = maxLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)

/**
 * A clock (ms) that advances only while [running] — CSS `animation-play-state: paused` works the
 * same way: the keyframes freeze where they are and resume later. Stops at [limitMs] (finite
 * iterations) and stays at 0 under reduced motion.
 */
@Composable
fun rememberPlayClock(running: Boolean, limitMs: Long = Long.MAX_VALUE): State<Long> {
    val t = remember { mutableLongStateOf(0L) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(running, reduced) {
        if (!running || reduced) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (t.longValue < limitMs) {
            withFrameMillis { now -> t.longValue = (t.longValue + (now - last)).coerceAtMost(limitMs); last = now }
        }
    }
    return t
}

/** A forever-running clock (ms) for process animations (rings, spinners, waveforms). */
@Composable
fun rememberLoopClock(running: Boolean = true): State<Long> = rememberPlayClock(running)

val EaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)
val Ease = CubicBezierEasing(.25f, .1f, .25f, 1f)
val EaseOutBack = CubicBezierEasing(.34f, 1.4f, .5f, 1f)
@Suppress("unused") private val keep = FastOutSlowInEasing

/** Value of a 0%→a, 50%→b, 100%→a keyframe with ease-in-out per half, for a cycle of [periodMs]. */
fun pingPong(tMs: Long, periodMs: Int, delayMs: Int, a: Float, b: Float, easing: CubicBezierEasing = EaseInOut): Float {
    val t = tMs - delayMs
    if (t < 0) return a
    val p = (t % periodMs).toFloat() / periodMs
    return if (p < .5f) a + (b - a) * easing.transform(p * 2f) else b + (a - b) * easing.transform((p - .5f) * 2f)
}

/** Formats seconds as m:ss. */
fun fmtSec(s: Int): String = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
