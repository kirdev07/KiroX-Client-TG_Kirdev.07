package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class AState(val e: String, val t: String, val k: String, val text: String, val emo: String = "", val group: Boolean = false)
private val AStates = listOf(
    AState("⌨️", "Печатает", "dots", "печатает"), AState("🎙", "Записывает голосовое", "rec", "записывает голосовое"),
    AState("⭕", "Записывает кружок", "circ", "записывает видеосообщение"), AState("🖼", "Отправляет фото", "up", "отправляет фото"),
    AState("🐱", "Выбирает стикер", "emo", "выбирает стикер", emo = "🐱"), AState("🎲", "Играет", "emo", "бросает кубик", emo = "🎲"),
    AState("👥", "Несколько в группе", "dots", "Дмитрий и Сергей печатают", group = true), AState("💤", "Ничего не делает", "none", "в сети"),
)
private class ARow(val n: String, val l: String, val c: String, val t: String, val live: List<String>)
// demo: simulated activity in the chat list — only with KxDemoStore; the real list shows store typing statuses.
private val ARows = listOf(
    ARow("Анна", "А", "#C0443C", "19:40", listOf("печатает…", "записывает голосовое…", "До завтра 👋")),
    ARow("Kotlin-клуб", "K", "#3F9A6C", "17:40", listOf("Сергей печатает…", "Дмитрий: PR готов", "Айгерим отправляет файл…")),
    ARow("Лиза", "Л", "#6A4C9C", "15:30", listOf("Сюрприз готов 🎁", "выбирает стикер…", "Сюрприз готов 🎁")),
    ARow("Олег", "О", "#9AA59F", "вчера", listOf("Ок", "Ок", "записывает кружок…")),
)

/** Board «Что делает собеседник»: header preview of every activity indicator, state picker, live chat list, hide-my-typing switch. */
@Composable
fun ActivityScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { com.kirox.app.ui.v3.screens.more.ActivityLive(ctx); return }
    val store = ctx.store
    val demo = store is KxDemoStore
    var i by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var hide by remember { mutableStateOf(false) }
    LaunchedEffect(demo) { if (demo) while (true) { delay(2200); tick++ } }
    val st = AStates[i]
    val g = st.group

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.05f))))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", Modifier.kxClick("Назад", onClick = ctx.back), tint = Kx.text2)
            LetterAv(if (g) "K" else "А", hex(if (g) "#3F9A6C" else "#C0443C"), 42.dp, 16)
            Column(Modifier.weight(1f)) {
                Text(if (g) "Kotlin-клуб" else "Анна", style = ts(16, W800))
                key(k) {
                    Row(Modifier.height(18.dp).chIn(dy = 4.dp, durationMs = 250), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (st.k) {
                            "dots" -> Dots()
                            "rec" -> RecPulse { Box(Modifier.size(9.dp).clip(CircleShape).background(hex("#FF5A4E"))) }
                            "circ" -> RecPulse { Box(Modifier.size(12.dp).border(2.dp, hex("#FF5A4E"), CircleShape)) }
                            "up" -> UploadBar()
                            "emo" -> EmoAnim(st.emo, st.emo == "🎲")
                        }
                        Text(st.text, style = ts(13, color = Kx.accent), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Icon(Icons.Outlined.Call, null, tint = Kx.text2)
        }
        Text("Нажмите, чтобы увидеть состояние", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp))
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AStates.chunked(2).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, x ->
                        val idx = r * 2 + c; val on = i == idx
                        Row(Modifier.weight(1f).heightIn(min = 54.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f))
                            .border(1.5.dp, if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(18.dp)).kxClick(x.t) { i = idx; k++ }
                            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(x.e, style = ts(20)); Text(x.t, style = ts(13, W700, lh = 16.25.sp))
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("В списке чатов", style = ts(13, W700, Kx.accent), modifier = Modifier.weight(1f))
            Text("меняется само", style = ts(11, color = Kx.text4))
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
            if (demo) ARows.forEach { r ->
                val m = r.live[tick % r.live.size]
                ListRow(LetterAvData(r.l, hex(r.c)), r.n, r.t, m, m.endsWith("…"), tick)
            } else store.chats.take(4).forEach { c ->
                val m = c.typing?.let { "$it…" } ?: (c.previewSender?.let { "$it: ${c.preview}" } ?: c.preview)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChatAvatar(c, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Row { Text(c.title, style = ts(15, W700), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(c.time, style = ts(12, color = Kx.text4)) }
                        Text(m, style = ts(13, color = if (c.typing != null) Kx.accent else Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f))
            .kxClick("Не показывать, что я печатаю", role = androidx.compose.ui.semantics.Role.Switch) {
                // demo: needs store (hide my typing status setting)
                if (demo) hide = !hide else ctx.toast("Эта настройка пока недоступна")
            }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EyeIcon()
            Column(Modifier.weight(1f)) {
                Text("Не показывать, что я печатаю", style = ts(14, W700))
                Text("Собеседники не увидят «печатает…» от вас", style = ts(12, color = Kx.text3))
            }
            BSwitch(hide)
        }
    }
}

private class LetterAvData(val l: String, val c: Color)

@Composable
private fun ListRow(av: LetterAvData, n: String, t: String, msg: String, live: Boolean, tick: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LetterAv(av.l, av.c, 48.dp, 16)
        Column(Modifier.weight(1f)) {
            Row { Text(n, style = ts(15, W700), modifier = Modifier.weight(1f)); Text(t, style = ts(12, color = Kx.text4)) }
            key(tick) { Text(msg, style = ts(13, color = if (live) Kx.accent else Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.chIn(dy = 4.dp, durationMs = 300)) }
        }
    }
}

/** kxDot ×3: bounce −3px + fade, 1.2s, .15s apart (process — loops). */
@Composable
private fun Dots() {
    val t = rememberInfiniteTransition(label = "dots")
    val reduced = LocalKxReducedMotion.current
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { n ->
            val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), initialStartOffset = StartOffset(n * 150)), label = "d$n")
            val up = if (reduced) 0f else if (p < .3f) p / .3f else if (p < .6f) (.6f - p) / .3f else 0f
            Box(Modifier.size(5.dp).graphicsLayer { translationY = -3.dp.toPx() * up; alpha = .4f + .6f * up }.clip(CircleShape).background(Kx.accent))
        }
    }
}

/** kxRec: fade to .4 + scale .8 at half, 1s loop. */
@Composable
private fun RecPulse(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "rec")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "p")
    val w = if (LocalKxReducedMotion.current) 0f else ((1 - kotlin.math.cos(2 * Math.PI * p)) / 2).toFloat()
    Box(Modifier.graphicsLayer { alpha = 1f - .6f * w; scaleX = 1f - .2f * w; scaleY = 1f - .2f * w }) { content() }
}

/** kxBar: 28×5 track, fill 10 % ↔ 95 %, 1.6s alternate. */
@Composable
private fun UploadBar() {
    val t = rememberInfiniteTransition(label = "up")
    val f by t.animateFloat(.1f, .95f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    Box(Modifier.size(28.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Kx.accent.copy(alpha = .2f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(if (LocalKxReducedMotion.current) .5f else f).background(Kx.accent))
    }
}

/** 🐱 kxWig (±14°, .8s, twice) or 🎲 kxRoll (360°, 1s, twice). */
@Composable
private fun EmoAnim(e: String, roll: Boolean) {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(if (roll) 1000 else 800, easing = if (roll) LinearEasing else FastOutSlowInEasing)) } }
    val v = p.value
    val rot = if (roll) 360f * v else when { v < .25f -> -14f * v / .25f; v < .75f -> -14f + 28f * (v - .25f) / .5f; else -> 14f - 14f * (v - .75f) / .25f }
    Text(e, style = ts(14), modifier = Modifier.graphicsLayer { rotationZ = if (v >= 1f) 0f else rot })
}

/** visibility_off with kxEye (±2px, 3s, twice). */
@Composable
private fun EyeIcon() {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(3000, easing = LinearEasing)) } }
    val x = if (p.value <= 0f || p.value >= 1f) 0f else -2f * kotlin.math.cos(2 * Math.PI * p.value).toFloat()
    Icon(Icons.Outlined.VisibilityOff, null, Modifier.graphicsLayer { translationX = x.dp.toPx() }, tint = Kx.text3)
}
