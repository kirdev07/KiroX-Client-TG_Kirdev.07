package com.kirox.app.ui.v3.screens.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxAvatarPalette
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

/** Call-screen pieces shared by VideoCall, GroupCall, Incoming, CallEnd, VideoConf, Pip. */

/** Letter avatar on a 140° gradient with an exact letter size (the call boards size them by hand). */
@Composable
fun MLetter(letter: String, size: Dp, font: TextUnit, colorIndex: Int, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Bold,
            shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    val sw = KxAvatarPalette.all[colorIndex % KxAvatarPalette.all.size]
    Box(modifier.size(size).clip(shape).cssGrad(140f, sw.from, sw.to), contentAlignment = Alignment.Center) { T(letter, font, weight, sw.ink) }
}

/**
 * kxRing: a [width]-stroke circle the size of the parent growing 1 → [toScale] while fading
 * [fromAlpha] → 0, ease-out, every [periodMs]; [count] rings offset evenly. Loops (process animation).
 */
@Composable
fun CallRings(modifier: Modifier, color: Color = Kx.accent, periodMs: Int = 2000, count: Int = 2, toScale: Float = 1.9f, fromAlpha: Float = .55f,
              width: Dp = 2.dp, fill: Boolean = false, running: Boolean = true) {
    val clock by rememberLoopClock(running)
    if (LocalKxReducedMotion.current) return
    Canvas(modifier) {
        val r0 = size.minDimension / 2
        repeat(count) { i ->
            val t = clock - i * periodMs / count
            if (t < 0) return@repeat
            val p = androidx.compose.animation.core.CubicBezierEasing(0f, 0f, .58f, 1f).transform((t % periodMs).toFloat() / periodMs)
            val r = r0 * (1f + (toScale - 1f) * p)
            val a = fromAlpha * (1f - p)
            if (fill) drawCircle(color.copy(alpha = color.alpha * a), r)
            else drawCircle(color.copy(alpha = a), r - width.toPx() / 2 * (1f + (toScale - 1f) * p), style = Stroke(width.toPx() * (1f + (toScale - 1f) * p)))
        }
    }
}

/** kxBar: three 3dp bars 4 → 14dp, .9s ease-in-out, .2s apart (a "talking" indicator). */
@Composable
fun SpeakBars(color: Color = Kx.accent, height: Dp = 14.dp, min: Dp = 4.dp, max: Dp = 14.dp, periodMs: Int = 900, stepMs: Int = 200, bars: Int = 3,
              barWidth: Dp = 3.dp, gap: Dp = 2.dp, running: Boolean = true) {
    val clock by rememberLoopClock(running)
    val reduced = LocalKxReducedMotion.current
    Row(Modifier.height(height), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(bars) { i ->
            val h = if (reduced || !running) min else min + (max - min) * pingPong(clock, periodMs, i * stepMs, 0f, 1f)
            Box(Modifier.width(barWidth).height(h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

/** A head + shoulders silhouette placeholder of a camera picture (real video is wired later). */
fun Modifier.personPlaceholder(head: Color, headDark: Color, body: Color, bodyDark: Color, headTop: Float, headW: Float, headH: Float, bodyTop: Float, bodyW: Float, bodyH: Float) = drawBehind {
    val hw = headW.dp.toPx(); val hh = headH.dp.toPx(); val ht = headTop.dp.toPx()
    val hc = Offset(size.width / 2, ht + hh / 2)
    drawOval(Brush.radialGradient(0f to head, .7f to headDark, center = Offset(size.width / 2 - hw * .05f, ht + hh * .4f), radius = hw * .75f),
        topLeft = Offset(hc.x - hw / 2, ht), size = androidx.compose.ui.geometry.Size(hw, hh))
    val bw = bodyW.dp.toPx(); val bh = bodyH.dp.toPx(); val bt = bodyTop.dp.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        val l = size.width / 2 - bw / 2; val r = l + bw; val rad = bw / 2
        moveTo(l, bt + bh); lineTo(l, bt + rad)
        arcTo(androidx.compose.ui.geometry.Rect(l, bt, r, bt + bw), 180f, 180f, false)
        lineTo(r, bt + bh); close()
    }
    drawPath(path, Brush.radialGradient(0f to body, .8f to bodyDark, center = Offset(size.width / 2, bt + bh * .2f), radius = bw * .55f))
}

fun fmtClock(sec: Int): String = "${(sec / 60).toString().padStart(2, '0')}:${(sec % 60).toString().padStart(2, '0')}"
