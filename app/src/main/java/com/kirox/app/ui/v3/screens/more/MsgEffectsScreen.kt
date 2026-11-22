package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — message effects (sendMessage with effect_id; availableMessageEffects), Premium ones marked by the server.
private data class Fx(val e: String, val name: String, val up: Boolean, val parts: List<String>)

private val FX = listOf(
    Fx("🎉", "Конфетти", false, listOf("🎊", "🎉", "✨", "🟩", "🟨")),
    Fx("❤️", "Сердечки", true, listOf("❤️", "💚", "💖")),
    Fx("🔥", "Огонь", true, listOf("🔥", "🔥", "✨")),
    Fx("👍", "Класс", true, listOf("👍", "👍", "✨")),
    Fx("🎈", "Шарики", true, listOf("🎈", "🎈", "🎈")),
    Fx("❄️", "Снегопад", false, listOf("❄️", "❄️", "✨")),
    Fx("⭐", "Звездопад", false, listOf("⭐", "🌟", "✨")),
    Fx("😂", "Смех", true, listOf("😂", "🤣", "😆")),
)
private data class FxSent(val id: Int, val text: String, val e: String)

/** MsgEffects board: pick a full-screen effect for the message (confetti, hearts…), preview it, send with it. */
@Composable
fun MsgEffectsScreen(ctx: KxScreenCtx) {
    var menu by remember { mutableStateOf(true) }
    var fx by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(-1) }
    var rk by remember { mutableIntStateOf(0) }
    val sent = remember { mutableStateListOf<FxSent>() }
    fun play(i: Int) { run = i; rk++ }
    val f = FX[fx]

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "Л", Color(0xFF6A4C9C), Color(0xFFC0443C), "Лиза", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    MoIncoming("Экзамен сдан!!! 🎓", modifier = Modifier.widthIn(max = w * .78f))
                    sent.forEach { m ->
                        key(m.id) {
                            val p = moEnter(350, easing = KxMotion.rise)
                            Box(Modifier.align(Alignment.End).widthIn(max = w * .78f).graphicsLayer { alpha = p; translationY = (1f - p) * 14.dp.toPx(); val s = .9f + .1f * p; scaleX = s; scaleY = s }) {
                                Box(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(m.text, style = mts(15, color = MoInk))
                                }
                                Box(Modifier.align(Alignment.BottomStart).offset((-10).dp, 6.dp).size(24.dp).clip(CircleShape).background(Kx.surface2)
                                    .border(1.dp, Kx.w(.12f), CircleShape), contentAlignment = Alignment.Center) { Text(m.e, style = mts(13)) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) { Text(if (sent.isNotEmpty()) "Сообщение" else "Поздравляю!!! 🥳", style = mts(15)) }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Выбрать эффект") { menu = !menu }, contentAlignment = Alignment.Center) {
                    Text(f.e, style = mts(22))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).moAccent().kxClick("Отправить") {
                    sent.add(FxSent(sent.size, if (sent.isNotEmpty()) "Горжусь тобой! 💚" else "Поздравляю!!! 🥳", f.e)); menu = false; play(fx)
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = MoInk) }
            }
        }

        if (menu) MoPopup({ menu = false }, width = 250.dp, bottom = 80.dp, end = true, pad = 12.dp, scrim = .45f, dy = 16.dp) {
            // Title and the Premium note on two lines: side by side they wrapped into a 3-line column at 250dp.
            Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
                Text("Отправить с эффектом", style = mts(13, W8, Kx.text2))
                Text("часть эффектов — с Premium", style = mts(11, W6, MoMuted))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FX.indices.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { i ->
                            val x = FX[i]; val on = fx == i
                            Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Black.copy(alpha = .18f))
                                .border(1.5.dp, if (on) Kx.accent else Color.Transparent, RoundedCornerShape(16.dp)).kxClick(x.name) { fx = i; play(i) },
                                contentAlignment = Alignment.Center) {
                                val t = if (on) moRun(600, 2, key = i) else -1f
                                val rot = when { t < 0f -> 0f; t < .25f -> -10f * t / .25f; t < .75f -> -10f + 20f * (t - .25f) / .5f; else -> 10f - 10f * (t - .75f) / .25f }
                                Text(x.e, style = mts(26), modifier = Modifier.graphicsLayer { rotationZ = rot })
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp).heightIn(min = 36.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(f.name, style = mts(13, W7), modifier = Modifier.weight(1f))
                MoPill("Превью", height = 34.dp, bg = Kx.accent.copy(alpha = .14f), ink = Kx.accent, size = 12, weight = W8, icon = Icons.Rounded.PlayArrow) { play(fx) }
            }
        }

        if (run >= 0) key(rk) { FxParticles(FX[run]) { run = -1 } }
    }
}

/** 26 emoji particles: falling (linear, rotate 540°) or rising (ease-out, scale .4 → 1.3, fade). 3.4 s total. */
@Composable
private fun FxParticles(r: Fx, onDone: () -> Unit) {
    if (LocalKxReducedMotion.current) { LaunchedEffect(Unit) { onDone() }; return }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(3400f, tween(3400, easing = LinearEasing)); onDone() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val hPx = constraints.maxHeight.toFloat(); val k = hPx / 844f
        repeat(26) { i ->
            val dur = 2000f + (i % 5) * 250f; val del = (i % 9) * 120f
            val raw = ((t.value - del) / dur)
            if (raw <= 0f || raw >= 1f) return@repeat
            val x = maxWidth * (((i * 37) % 100) / 100f)
            val sz = 18 + (i * 7) % 16
            if (r.up) {
                val p = MoEaseOut.transform(raw)
                val y0 = (760 + (i % 4) * 20) * k
                Text(r.parts[i % r.parts.size], style = mts(sz), modifier = Modifier.offset(x = x).graphicsLayer {
                    translationY = y0 - 640f * k * p
                    val s = .4f + .9f * p; scaleX = s; scaleY = s
                    alpha = if (raw < .15f) raw / .15f else 1f - (raw - .15f) / .85f
                })
            } else {
                Text(r.parts[i % r.parts.size], style = mts(sz), modifier = Modifier.offset(x = x).graphicsLayer {
                    translationY = (-60f + 920f * raw) * k
                    rotationZ = 540f * raw
                    alpha = if (raw < .1f) raw / .1f else 1f - .1f * (raw - .1f) / .9f
                })
            }
        }
    }
}
