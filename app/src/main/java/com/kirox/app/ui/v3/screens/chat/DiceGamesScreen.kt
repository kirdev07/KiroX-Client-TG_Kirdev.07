package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

private class DGame(val e: String, val name: String, val max: Int, val say: (Int) -> String)
private val DGames = listOf(
    DGame("🎲", "Кубик", 6) { "Выпало $it" },
    DGame("🎯", "Дартс", 6) { if (it == 6) "В яблочко!" else "$it ${plural(it, "очко", "очка", "очков")}" },
    DGame("🏀", "Баскетбол", 5) { if (it >= 4) "Попадание!" else "Мимо" },
    DGame("⚽", "Футбол", 5) { if (it >= 3) "Гол!" else "Штанга" },
    DGame("🎳", "Боулинг", 6) { if (it == 6) "Страйк!" else "Сбито ${it + 3}" },
    DGame("🎰", "Слоты", 64) { if (it == 64) "Джекпот! 7️⃣7️⃣7️⃣" else "Почти…" },
)
private data class DMsg(val id: Long, val mine: Boolean, val g: Int, val v: Int, val done: Boolean = false)

/**
 * Board «Игры с кубиком»: best-of-3 match with animated throws and a scoreboard.
 * The opponent's throws are simulated — shown only with KxDemoStore (honesty rule).
 */
@Composable
fun DiceGamesScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    // Real chat (arg = chat id): «Бросить» sends a real Telegram dice (store.sendDice); Telegram picks the value and it shows in the chat.
    val chatId = if (demo) null else ctx.arg?.substringBefore(':')?.toLongOrNull()
    val header = chatId?.let { ctx.store.header(it) }
    var g by remember { mutableIntStateOf(0) }
    val msgs = remember { mutableStateListOf<DMsg>() }
    var me by remember { mutableIntStateOf(0) }
    var op by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    var mk by remember { mutableIntStateOf(0) }
    var ok by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val game = DGames[g]
    val over = me >= 3 || op >= 3
    fun roll(mine: Boolean): DMsg { val m = DMsg(System.nanoTime(), mine, g, 1 + (0 until DGames[g].max).random()); msgs += m; return m }
    fun land(id: Long) { val i = msgs.indexOfFirst { it.id == id }; if (i >= 0) msgs[i] = msgs[i].copy(done = true) }
    fun throwIt() {
        if (busy || over) return
        if (!demo) {
            if (chatId == null) { ctx.toast("Откройте игру из чата"); return }
            ctx.store.sendDice(chatId, game.e); ctx.toast("${game.e} брошен — результат появится в чате"); return
        }
        busy = true
        scope.launch {
            val a = roll(true); delay(1100); land(a.id); typing = true
            delay(1100); typing = false; val b = roll(false); delay(1100); land(b.id)
            if (a.v > b.v) { me++; mk++ } else if (b.v > a.v) { op++; ok++ }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
            LetterAv((header?.title ?: "О").take(1).uppercase(), hex("#5C6661"), 40.dp, 16)
            Column(Modifier.weight(1f)) {
                Text(header?.title ?: "Олег", style = ts(16, W800), maxLines = 1)
                Text(if (typing) "бросает ${game.e}…" else header?.status ?: "в сети", style = ts(12, color = if (typing) Kx.warning else Kx.accent), maxLines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.05f))))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(game.e, style = ts(22))
            Text("${game.name} · до 3 побед", style = ts(13, W700, Kx.text2), modifier = Modifier.weight(1f))
            Text("Вы", style = ts(12, color = Kx.text3))
            Score(me, mk)
            Text(":", style = ts(15, color = Kx.text4))
            Score(op, ok)
            Text("Олег", style = ts(12, color = Kx.text3))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Кто проиграет — тот платит за кофе ☕", style = ts(15), modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
            msgs.takeLast(4).forEach { m ->
                key(m.id) {
                    val gg = DGames[m.g]
                    Column(Modifier.align(if (m.mine) Alignment.End else Alignment.Start).chIn(dy = 12.dp, durationMs = 300),
                        horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(96.dp)) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(if (m.mine) Kx.accent.copy(alpha = .12f) else Kx.w(.06f))
                                .border(1.dp, Kx.w(.1f), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) { ThrowEmoji(gg.e, m.done) }
                            if (m.done) Box(Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).heightIn(min = 28.dp).widthIn(min = 28.dp).clip(CircleShape)
                                .then(if (m.v == 6) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Color(0xFA28302C))).padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center) { Text("${m.v}", style = ts(14, W800, if (m.v == 6) Kx.onAccent else Kx.text)) }
                        }
                        Text(if (m.done) (if (m.mine) "Вы · " else "Олег · ") + gg.say(m.v) else "…", style = ts(12, W700, if (m.done && m.v == 6) Kx.accent else Kx.text3))
                    }
                }
            }
            if (typing) Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val t = rememberInfiniteTransition(label = "dots")
                repeat(3) { i ->
                    val a by t.animateFloat(.3f, .3f, infiniteRepeatable(keyframes { durationMillis = 1000; .3f at 0; 1f at 500; .3f at 1000 }, initialStartOffset = StartOffset(i * 200)), label = "d$i")
                    Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Kx.text3))
                }
            }
            if (over) Column(Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp).chIn(dy = 12.dp).clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .12f))
                .border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (me >= 3) "Вы победили! Кофе за соперником ☕" else "Победа соперника. Кофе за вами ☕", style = ts(15, W800), textAlign = TextAlign.Center)
                Box(Modifier.padding(top = 8.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep)
                    .kxClick { me = 0; op = 0; msgs.clear(); mk = 0; ok = 0 }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("Реванш", style = ts(13, W800, Kx.onAccent)) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DGames.forEachIndexed { i, x ->
                    val on = g == i
                    Box(Modifier.weight(1f).height(50.dp).graphicsLayer { alpha = if (busy && !on) .4f else 1f }.clip(RoundedCornerShape(16.dp))
                        .background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f)).border(1.5.dp, if (on) Kx.accent else Color.Transparent, RoundedCornerShape(16.dp))
                        .kxClick(x.name) { if (!busy) { g = i; me = 0; op = 0; msgs.clear() } }, contentAlignment = Alignment.Center) { Text(x.e, style = ts(26)) }
                }
            }
            val off = over || busy
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                .then(if (off) Modifier.background(Kx.w(.07f)) else Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep)).kxClick { throwIt() }, contentAlignment = Alignment.Center) {
                Text(if (over) "Партия окончена" else if (busy) "Ждём результат…" else "Бросить ${game.e}", style = ts(16, W800, if (off) Kx.text4 else Kx.onAccent))
            }
        }
    }
}

/** Score number with kxScore (scale 1.35 + green at 40 %, .5s) on every change of [k]. */
@Composable
private fun Score(v: Int, k: Int) {
    val p = remember { Animatable(1f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(k) { if (k > 0 && !reduced) { p.snapTo(0f); p.animateTo(1f, tween(500)) } }
    val x = p.value
    val up = if (x < .4f) x / .4f else (1f - x) / .6f
    Text("$v", style = ts(22, W800, androidx.compose.ui.graphics.lerp(Kx.text, Kx.accent, up)), modifier = Modifier.graphicsLayer { val s = 1f + .35f * up; scaleX = s; scaleY = s })
}

/** 52sp emoji: kxRoll (2 turns, .6 → 1.1 → 1, 1.1s) while rolling, kxLand (1.25 → .92 → 1, .35s) when landed. */
@Composable
private fun ThrowEmoji(e: String, done: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val roll = remember { Animatable(0f) }
    val land = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) roll.animateTo(1f, tween(1100, easing = CubicBezierEasing(.3f, .7f, .3f, 1f))) else roll.snapTo(1f) }
    LaunchedEffect(done) { if (done && !reduced) land.animateTo(1f, tween(350)) else if (done) land.snapTo(1f) }
    val (rot, sc) = if (!done) {
        val r = roll.value
        720f * r to (if (r < .5f) .6f + .5f * (r / .5f) else 1.1f - .1f * ((r - .5f) / .5f))
    } else {
        val l = land.value
        0f to (if (l < .6f) 1.25f - .33f * (l / .6f) else .92f + .08f * ((l - .6f) / .4f))
    }
    Text(e, style = ts(52), modifier = Modifier.graphicsLayer { rotationZ = rot; scaleX = sc; scaleY = sc })
}
