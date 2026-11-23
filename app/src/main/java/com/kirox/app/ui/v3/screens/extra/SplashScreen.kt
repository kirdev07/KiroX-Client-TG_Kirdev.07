package com.kirox.app.ui.v3.screens.extra

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** Board «Загрузка kiroX»: glowing X bubble with orbit and typing bubble, wordmark, loading bar. */
@Composable
fun SplashScreen(ctx: KxScreenCtx) {
    @Suppress("UNUSED_VARIABLE") val unused = ctx
    KxSplash()
}

/** The same board as the app's start screen (KiroXApp shows it while TDLib starts). */
@Composable
fun KxSplash() {
    val glowRaw = kfRun(3200); val glow = tri(glowRaw)
    val spin = kfLoop(18000, label = "spin")
    val orbit = kfRun(6000)
    val tilt = kfRun(4600, delayMs = 800)
    val float = tri(kfRun(4000))
    val load = kfLoop(1400, label = "load")
    val rise1 = enter(900, 300, KxMotion.rise); val rise2 = enter(900, 600, KxMotion.rise); val rise3 = enter(900, 900, EaseCss)
    Box(Modifier.fillMaxSize().ellipseBg(.9f, .55f, .5f, .42f, 0f to Color(0xFF0F2419), .62f to ExBg)) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Board is 844dp tall: on shorter screens the art/wordmark move up (and the art shrinks) so they
            // stay above the loading bar; k = 1 (board look) from ~770dp up.
            val k = ((maxHeight.value - 190f) / 580f).coerceIn(.5f, 1f)
            // Twinkles (kxTwinkle) and drifting sparks (kxDrift).
            listOf(Tw(60, 150, 4, 2600, 0), Tw(318, 220, 5, 3100, 800), Tw(290, 560, 3, 2200, 1300), Tw(92, 520, 4, 2900, 400)).forEach { w ->
                val raw = kfRun(w.period, delayMs = w.delay)
                val t = tri(raw)
                Box(Modifier.offset(w.x.dp, w.y.dp).size(w.s.dp).graphicsLayer {
                    if (raw >= 0f) { alpha = .15f + .85f * t; val s = .6f + .4f * t; scaleX = s; scaleY = s }
                }.clip(CircleShape).background(Kx.accentLight))
            }
            listOf(Tw(150, 640, 3, 4500, 0), Tw(238, 660, 3, 5200, 1600), Tw(196, 690, 2, 4000, 2800)).forEach { w ->
                val t = kfRun(w.period, delayMs = w.delay)
                Box(Modifier.offset(w.x.dp, w.y.dp).size(w.s.dp).graphicsLayer {
                    val e = EaseOutCss.transform(t.coerceAtLeast(0f))
                    translationY = -220.dp.toPx() * e
                    alpha = if (t < 0f) 0f else if (t < .15f) .8f * (t / .15f) else .8f * (1f - (t - .15f) / .85f)
                }.clip(CircleShape).background(Kx.accent))
            }
            // Art: glow, dashed ring, orbit dot, typing bubble, the X bubble.
            Box(Modifier.fillMaxWidth().offset(y = 230.dp * k).height(240.dp).graphicsLayer { scaleX = k; scaleY = k }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(260.dp).graphicsLayer { val s = 1f + .15f * glow; scaleX = s; scaleY = s; alpha = if (glowRaw < 0f) 1f else .7f + .3f * glow }.circleGlow(Color(0x5950AF78)))
                Canvas(Modifier.size(210.dp).graphicsLayer { rotationZ = 360f * spin }) {
                    drawCircle(Kx.accent.copy(alpha = .25f), radius = size.minDimension / 2 - .5f,
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
                }
                Box(Modifier.size(168.dp).graphicsLayer { rotationZ = -360f * orbit.coerceAtLeast(0f) }) {
                    Box(Modifier.align(Alignment.TopCenter).offset(y = (-5).dp).size(10.dp).boxShadow(Kx.accent, 12.dp, radius = 5.dp).clip(CircleShape).background(Color(0xFFCFF5DE)))
                }
                Box(Modifier.size(150.dp, 130.dp)) {
                    Box(Modifier.offset((-26).dp, 64.dp).size(96.dp, 56.dp).graphicsLayer {
                        if (tilt > 0f) { val k = tri(tilt); translationY = -8.dp.toPx() * k; rotationZ = -4f + 6f * k }
                    }.boxShadow(Color(0x80000000), 30.dp, 12.dp, 24.dp)
                        .clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)).background(Color(0xD91A241F))
                        .border(1.dp, Color(0xFF2E4A3A), RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp))) {
                        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(3) { i ->
                                val d = kfLoop(1200, i * 150, "dot$i")
                                val k = if (d <= .3f) d / .3f else if (d <= .6f) 1f - (d - .3f) / .3f else 0f
                                Box(Modifier.size(9.dp).graphicsLayer { translationY = -6.dp.toPx() * EaseInOut.transform(k); alpha = .45f + .55f * k }.clip(CircleShape).background(Kx.accent))
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).offset(10.dp).size(124.dp, 104.dp).graphicsLayer { translationY = -10.dp.toPx() * float }
                        .boxShadow(Color(0x736FC497), 60.dp, radius = 36.dp)
                        .clip(RoundedCornerShape(36.dp, 36.dp, 8.dp, 36.dp))
                        .lin(150f, 0f to Kx.accentLight, .35f to Kx.accentMid, 1f to Color(0xFF2E7A54)).insetTop(.35f, 2.dp),
                        contentAlignment = Alignment.Center) {
                        Text("X", style = ts(56.sp, FontWeight.ExtraBold, Kx.onAccent, ls = (-2).sp).copy(shadow = Shadow(Color.White.copy(alpha = .35f), Offset(0f, 2f), 0f)))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().offset(y = 500.dp * k).graphicsLayer { alpha = rise1; translationY = (1f - rise1) * 18.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(buildAnnotatedString { append("kiro"); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.ExtraBold)) { append("X") } },
                    style = ts(40.sp, FontWeight.Light, ls = .5.sp))
                Text("Telegram, только удобнее", style = ts(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 8.dp)
                    .graphicsLayer { alpha = rise2; translationY = (1f - rise2) * 18.dp.toPx() })
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp).size(120.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1A211D))
                .semantics { contentDescription = "Загрузка" }) {
                Box(Modifier.fillMaxHeight().width(48.dp).graphicsLayer { translationX = (-1f + 3.5f * EaseInOut.transform(load)) * size.width }
                    .clip(RoundedCornerShape(2.dp)).lin2(90f, Kx.accentDeep, Kx.accent))
            }
            Text("Подключаемся к Telegram…", style = ts(12.sp, color = Kx.text4), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp).graphicsLayer { alpha = rise3; translationY = (1f - rise3) * 18.dp.toPx() })
        }
    }
}

private data class Tw(val x: Int, val y: Int, val s: Int, val period: Int, val delay: Int)
