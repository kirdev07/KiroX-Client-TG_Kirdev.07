package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val EmList = listOf("😺", "🔥", "🌵", "🍕", "😎", "🎉", "🥶", "👻", "🌈", "🐸", "☕", "🚀", "😴", "🍩", "⚡", "💚", "🤖", "🌚")
private val EmNames = mapOf("😺🔥" to "Огненный котик", "🐸☕" to "Лягушка на кофе", "👻🎉" to "Праздничный призрак", "🤖💚" to "Добрый робот", "🌚😴" to "Сонная луна")

/** Board «Микс эмодзи»: two slots merge into a sticker (fly together → born with sparks), 18-emoji picker, shuffle, send. */
@Composable
fun EmojiMixScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var a by remember { mutableStateOf("😺") }
    var b by remember { mutableStateOf("🔥") }
    var slot by remember { mutableStateOf("b") }
    var phase by remember { mutableStateOf("pair") }
    var k by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase) { if (phase == "merge") { delay(450); phase = "result"; k++ } }
    LaunchedEffect(tk) { if (toast != null) { delay(2400); toast = null } }
    val result = phase == "result"; val merging = phase == "merge"
    val sel = if (slot == "a") a else b
    val name = EmNames[a + b] ?: "Новый стикер"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) { Text("Микс эмодзи", style = ts(20, W800)); Text("Смешайте два — получите стикер", style = ts(12, color = Kx.text3)) }
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp)).background(Kx.w(.03f))
                .ellipseGlow(.5f, .4f, .7f, .6f, Kx.accent.copy(alpha = .18f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                if (!result) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Slot(a, slot == "a", merging, 1f, "Первый эмодзи") { slot = "a"; phase = "pair" }
                    Icon(Icons.Outlined.Add, null, Modifier.size(28.dp), tint = Kx.accent)
                    Slot(b, slot == "b", merging, -1f, "Второй эмодзи") { slot = "b"; phase = "pair" }
                } else key(k) { Born(a, b, name) }
                Text(if (result) "Готовый стикер можно отправить или сохранить" else "Нажмите на ячейку, затем выберите эмодзи ниже", style = ts(12, color = Kx.text4),
                    textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text(if (slot == "a") "Первый эмодзи" else "Второй эмодзи", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmList.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { ch ->
                                val on = ch == sel
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.04f))
                                    .border(1.5.dp, if (on) Kx.accent else Kx.w(.06f), RoundedCornerShape(14.dp)).kxClick(ch) { if (slot == "a") a = ch else b = ch; phase = "pair" },
                                    contentAlignment = Alignment.Center) { Text(ch, style = ts(26)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Случайная пара") { a = EmList.random(); b = EmList.random(); phase = "pair" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Shuffle, null, tint = Kx.text)
                }
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                    if (!result) { if (!merging) phase = "merge" }
                    // demo: needs store (send an emoji-mix sticker to a chat)
                    else if (demo) { toast = "Стикер «$name» отправлен"; tk++ } else ctx.toast("Отправка миксов пока недоступна")
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (result) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Science, null, tint = Kx.onAccent)
                    Text(if (result) "Отправить в чат" else "Смешать", style = ts(16, W800, Kx.onAccent))
                }
            }
        }
        val t = toast
        if (t != null) key(tk) {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(2400, easing = LinearEasing)) }
            val v = p.value; val al = when { v < .12f -> v / .12f; v > .85f -> (1f - v) / .15f; else -> 1f }
            val ty = if (v < .12f) 10f * (1f - v / .12f) else 0f
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp).graphicsLayer { alpha = al; translationY = ty.dp.toPx() }
                .clip(RoundedCornerShape(18.dp)).background(Kx.sheetTop).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.accent); Text(t, style = ts(14, W600), maxLines = 1)
            }
        }
    }
}

/** 92 slot; kxL/kxR (fly 62px to the centre, rotate ±20°, scale .6, fade, .45s ease-in) while merging. */
@Composable
private fun Slot(e: String, on: Boolean, merging: Boolean, dir: Float, label: String, onClick: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(merging) { if (merging) p.animateTo(1f, tween(450, easing = FastOutLinearInEasing)) else p.snapTo(0f) }
    val v = p.value
    Box(Modifier.size(92.dp).graphicsLayer { translationX = 62.dp.toPx() * v * dir; rotationZ = 20f * v * dir; val s = 1f - .4f * v; scaleX = s; scaleY = s; alpha = 1f - v }
        .clip(RoundedCornerShape(28.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f)).border(2.dp, if (on) Kx.accent else Kx.w(.1f), RoundedCornerShape(28.dp))
        .kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Text(e, style = ts(54)) }
}

/** Result sticker: kxBorn pop (.6s overshoot), big emoji bobbing twice, small one tilted 14°, 10 sparks. */
@Composable
private fun Born(a: String, b: String, name: String) {
    val reduced = LocalKxReducedMotion.current
    val born = remember { Animatable(if (reduced) 1f else 0f) }
    val bob = remember { Animatable(0f) }
    LaunchedEffect(Unit) { born.animateTo(1f, tween(600, easing = CubicBezierEasing(.3f, 1.4f, .5f, 1f))) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { bob.snapTo(0f); bob.animateTo(1f, tween(2400, easing = LinearEasing)) } }
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val p = born.value
            val s = if (p < .6f) 1.25f * p / .6f else 1.25f - .25f * (p - .6f) / .4f
            val r = if (p < .6f) -30f + 38f * p / .6f else 8f - 8f * (p - .6f) / .4f
            Box(Modifier.size(150.dp).graphicsLayer { scaleX = s; scaleY = s; rotationZ = r }) {
                val y = ((1 - cos(2 * Math.PI * bob.value)) / 2).toFloat()
                Text(a, style = ts(96), modifier = Modifier.align(Alignment.Center).graphicsLayer { translationX = -.1f * 96.dp.toPx(); translationY = -.05f * 96.dp.toPx() - 6.dp.toPx() * y }
                    .shadow(0.dp))
                Text(b, style = ts(52), modifier = Modifier.offset(93.dp - 26.dp, 93.dp - 30.dp).graphicsLayer { rotationZ = 14f })
            }
            Text(name, style = ts(15, W800), modifier = Modifier.padding(top = 4.dp))
        }
        repeat(10) { i ->
            val ang = i / 10.0 * Math.PI * 2
            MixSpark((cos(ang) * 90).toFloat(), (sin(ang) * 80).toFloat(), (i % 3) * 50)
        }
    }
}

@Composable
private fun MixSpark(dx: Float, dy: Float, delayMs: Int) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(800, easing = LinearOutSlowInEasing)) }
    val v = p.value
    if (v >= 1f) return
    Text("✨", style = ts(16), modifier = Modifier.offset(y = (-20).dp).graphicsLayer { translationX = dx.dp.toPx() * v; translationY = dy.dp.toPx() * v; scaleX = v; scaleY = v; alpha = 1f - v })
}
