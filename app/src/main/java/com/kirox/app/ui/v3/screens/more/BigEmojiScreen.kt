package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// demo: needs store — animated emoji effects (clickAnimatedEmojiMessage) and the «смотрит эффект» chat action.
private val BE_LOOP = linkedMapOf("❤️" to ("beat" to 1400), "🎉" to ("shake" to 1600), "👍" to ("bounce" to 1400), "😂" to ("shake" to 1000), "🔥" to ("flame" to 800), "🥳" to ("bounce" to 1600))
private val BE_BURST = mapOf("❤️" to listOf("❤️", "💖", "💕"), "🎉" to listOf("🎉", "🎊", "✨"), "👍" to listOf("👍", "✨"), "😂" to listOf("😂", "🤣"), "🔥" to listOf("🔥", "✨"), "🥳" to listOf("🥳", "🎈", "🎊"))
private data class BeMsg(val id: Int, val e: String, val me: Boolean, val n: Int)
private val FlyEase = CubicBezierEasing(.2f, .7f, .3f, 1f)

/** BigEmoji board: one emoji = huge and animated; tap it for a full-screen burst the other side sees too. */
@Composable
fun BigEmojiScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) C3NeedChat(ctx, "Большой эмодзи") else BigEmojiLive(ctx, c, ctx.arg?.split(":")?.getOrNull(1)?.toLongOrNull()); return }
    val msgs = remember { mutableStateListOf(BeMsg(0, "🥳", false, 1), BeMsg(1, "🎉", true, 1)) }
    var n by remember { mutableIntStateOf(1) }
    var run by remember { mutableStateOf("") }
    var rk by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun fire(e: String) {
        job?.cancel(); run = e; rk++; hint = true; seen = false
        job = scope.launch {
            launch { delay(900); seen = true }
            launch { delay(2000); run = "" }
            delay(2400); hint = false
        }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "А", Color(0xFFC0443C), null, "Анна", if (run.isNotEmpty() && seen) "смотрит эффект ✨" else "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                    MoIncoming("Меня взяли на работу!!!", modifier = Modifier.widthIn(max = w * .78f))
                    msgs.takeLast(4).forEach { m -> key(m.id) { BeBig(m, Modifier.align(if (m.me) Alignment.End else Alignment.Start)) { fire(m.e) } } }
                }
            }
            Text("Нажмите на большой эмодзи — эффект увидит и собеседник", style = mts(12, color = MoMuted), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp))
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BE_LOOP.keys.forEach { e ->
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick("Отправить $e") {
                        msgs.add(BeMsg(msgs.size, e, true, n)); if (n == 1) fire(e)
                    }, contentAlignment = Alignment.Center) { Text(e, style = mts(28)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                listOf(1 to "Один — огромный", 2 to "Два", 3 to "Три").forEach { (k, t) ->
                    val on = n == k
                    MoPill(t, height = 32.dp, bg = if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f), ink = if (on) Kx.accent else MoSub, size = 12) { n = k }
                }
            }
        }

        if (run.isNotEmpty()) key(rk) { BeBurst(BE_BURST[run] ?: listOf(run)) }

        if (hint) key(rk) {
            val p = moEnter(2400, easing = LinearEasing)
            val a = when { p < .15f -> p / .15f; p < .8f -> 1f; else -> 1f - (p - .8f) / .2f }
            Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp).graphicsLayer { alpha = a; translationY = (1f - a) * 8.dp.toPx() }
                .clip(RoundedCornerShape(16.dp)).background(Color(0xF528302C)).border(1.dp, Kx.w(.12f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(if (seen) "Анна тоже видит эффект" else "Эффект на весь экран…", style = mts(12, W7), maxLines = 1)
            }
        }
    }
}

@Composable
private fun BeBig(m: BeMsg, modifier: Modifier, onTap: () -> Unit) {
    val pop = moEnter(400, easing = KxMotion.rise)
    val loop = BE_LOOP[m.e]
    val t = if (m.n == 1 && loop != null) moRun(loop.second, 2) else -1f
    val fs = when (m.n) { 1 -> 96; 2 -> 64; else -> 48 }
    Box(modifier.graphicsLayer {
        val s = if (pop < .6f) .3f + .85f * (pop / .6f) else 1.15f - .15f * ((pop - .6f) / .4f)
        scaleX = s; scaleY = s; alpha = (pop / .6f).coerceIn(0f, 1f)
    }.clip(RoundedCornerShape(8.dp)).kxClick("Эмодзи ${m.e}, нажмите для эффекта", onClick = onTap).padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(m.e.repeat(m.n), style = mts(fs, lh = 1.1f, ls = 2.sp), maxLines = 1, softWrap = false, modifier = Modifier.graphicsLayer {
            if (t < 0f || loop == null) return@graphicsLayer
            when (loop.first) {
                "beat" -> { val s = moKf(t, 0f to 1f, .15f to 1.25f, .3f to 1f, .45f to 1.18f, 1f to 1f); scaleX = s; scaleY = s }
                "bounce" -> {
                    translationY = moKf(t, 0f to 0f, .4f to -18f, .7f to 0f, 1f to 0f) * density
                    rotationZ = moKf(t, 0f to 0f, .4f to -8f, .7f to 4f, 1f to 0f)
                }
                "shake" -> rotationZ = moKf(t, 0f to 0f, .2f to -14f, .4f to 12f, .6f to -8f, .8f to 6f, 1f to 0f)
                "flame" -> { scaleY = moKf(t, 0f to 1f, .5f to 1.15f, 1f to 1f); translationY = moKf(t, 0f to 0f, .5f to -3f, 1f to 0f) * density; transformOrigin = TransformOrigin.Center }
            }
        })
    }
}

/** 22 emoji fly out from the screen point (50%, 45%), scale .4 → 1.3, fade in by 15% and out. */
@Composable
private fun BeBurst(set: List<String>) {
    if (LocalKxReducedMotion.current) return
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(2000f, tween(2000, easing = LinearEasing)) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cx = maxWidth / 2; val cy = maxHeight * .45f
        repeat(22) { i ->
            val dur = 1100f + (i % 4) * 200f; val del = (i % 5) * 50f
            val raw = (clock.value - del) / dur
            if (raw <= 0f || raw >= 1f) return@repeat
            val a = (i / 22.0) * Math.PI * 2 + (i % 2) * .2
            val r = 150 + (i * 37) % 120
            val dx = Math.round(kotlin.math.cos(a) * r).toFloat(); val dy = Math.round(kotlin.math.sin(a) * r - 60).toFloat()
            val p = FlyEase.transform(raw)
            Text(set[i % set.size], style = mts(26 + (i * 11) % 22), modifier = Modifier.offset(cx - 16.dp, cy - 16.dp).graphicsLayer {
                translationX = dx * p * density; translationY = dy * p * density
                val s = .4f + .9f * p; scaleX = s; scaleY = s
                alpha = if (p < .15f) p / .15f else 1f - (p - .15f) / .85f
            })
        }
    }
}
