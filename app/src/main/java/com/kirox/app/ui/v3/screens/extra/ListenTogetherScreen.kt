package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class LtTrack(val t: String, val a: String, val d: Int, val c1: Color, val c2: Color, val icon: ImageVector, val glow: Color, val by: String)
private val LtTracks = listOf(
    LtTrack("Летний вечер", "Лунный кот", 214, Color(0xFFF2B866), Color(0xFFC0443C), Icons.Rounded.WbTwilight, Color(0x40F2B866), "Лиза"),
    LtTrack("Горы зовут", "Эхо Медеу", 186, Color(0xFF6FA9D8), Color(0xFF2F6E9E), Icons.Rounded.Landscape, Color(0x406FA9D8), "Папа"),
    LtTrack("Дачный джаз", "Трио «Сад»", 245, Kx.accent, Kx.accentDeep, Icons.Rounded.Piano, Color(0x478FD3AC), "Мама"),
    LtTrack("Ночной трамвай", "Город спит", 198, Color(0xFFB39DDB), Color(0xFF6A4C9C), Icons.Rounded.Tram, Color(0x40B39DDB), "Вы"),
)
private val LtL = listOf("Я" to Kx.accentDeep, "М" to Color(0xFFB07A2E), "П" to Color(0xFF2F6E9E), "Л" to Color(0xFFC0443C))
private val LtR = listOf("❤️" to "Нравится", "🔥" to "Огонь", "💃" to "Танцую", "😂" to "Смешно", "🎶" to "Подпеваю")
private class LtFloat(val k: Int, val e: String, val x: Float)

/**
 * Board «Слушаем вместе» (idea, marked on the board): spinning vinyl with cover, progress, prev/play/next, listeners + eq, reactions floating up, queue.
 * Other listeners' random reactions are a simulation → only on the demo store.
 */
@Composable
fun ListenTogetherScreen(ctx: KxScreenCtx) {
    // demo: needs store (needs a kiroX sync server — see the note on the board)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var i by remember { mutableIntStateOf(0) }
    var t by remember { mutableIntStateOf(42) }
    var playing by remember { mutableStateOf(true) }
    val floats = remember { mutableStateListOf<LtFloat>() }
    var n by remember { mutableIntStateOf(0) }
    fun react(e: String) { n++; if (floats.size > 40) floats.clear(); floats.add(LtFloat(n, e, 30f + Math.random().toFloat() * 150f)) }
    LaunchedEffect(Unit) { while (true) { delay(1000); if (playing) { val tr = LtTracks[i]; if (t + 1 >= tr.d) { i = (i + 1) % LtTracks.size; t = 0 } else t++ } } }
    LaunchedEffect(demo) { if (demo) while (true) { delay(2600); if (playing && Math.random() < .5) react(LtR.random().first) } }
    val tr = LtTracks[i]
    val glow by animateColorAsState(tr.glow, tween(600), label = "glow")
    // spin keeps its angle while paused (animation-play-state)
    val angle = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(playing, reduced) { if (playing && !reduced) while (true) { angle.animateTo(angle.value + 360f, tween(4000, easing = LinearEasing)) } }
    Box(Modifier.fillMaxSize().background(Kx.bg).ellipseBg(1f, .5f, .5f, .2f, 0f to glow, .7f to Color.Transparent)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.Outlined.KeyboardArrowDown, "Свернуть") { ctx.back() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val g = kfRun(1400)
                        Box(Modifier.size(8.dp).graphicsLayer { alpha = if (g < 0f) 1f else .5f + .4f * tri(g) }.clip(CircleShape).background(Kx.accent))
                        Text("Слушаем вместе", style = ts(12.sp, FontWeight.Bold, Kx.accent))
                    }
                    Text("Семья", style = ts(14.sp, FontWeight.ExtraBold))
                }
                ExRoundBtn(Icons.Outlined.PersonAdd, "Пригласить", border = null) {}
            }
            // critique-B: the «idea» banner only on the demo store (a real account never reaches this board, see ExtraDevOnly).
            if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.warning.copy(alpha = .1f))
                .border(1.dp, Kx.warning.copy(alpha = .35f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Construction, null, Modifier.size(18.dp), tint = Kx.warning)
                Text("Идея, не решено. В Telegram нет протокола «слушать синхронно», нужен свой сервер kiroX. Без него — только «поделиться треком».", style = ts(12.sp, color = Color(0xFFF2D9A6), lh = 17.sp))
            }
            Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(230.dp)) {
                    Box(Modifier.matchParentSize().boxShadow(Color.Black.copy(alpha = .6f), 50.dp, 20.dp, 115.dp).graphicsLayer { rotationZ = angle.value }.clip(CircleShape).drawBehind {
                        drawCircle(Color(0xFF0B0E0C)); var r = size.minDimension / 2; val st = 2.dp.toPx()
                        while (r > 0) { drawCircle(Color(0xFF141816), r, style = androidx.compose.ui.graphics.drawscope.Stroke(st)); r -= 2 * st }
                    })
                    Box(Modifier.align(Alignment.Center).size(110.dp).graphicsLayer { rotationZ = angle.value }.clip(CircleShape).lin2(135f, tr.c1, tr.c2), contentAlignment = Alignment.Center) {
                        Icon(tr.icon, null, Modifier.size(40.dp), tint = Color.White.copy(alpha = .85f))
                    }
                    Box(Modifier.align(Alignment.Center).size(12.dp).clip(CircleShape).background(Kx.bg))
                    floats.forEach { f ->
                        key(f.k) {
                            val p = enter(2200, easing = EaseOutCss)
                            if (p < 1f) Text(f.e, style = ts(26.sp), modifier = Modifier.align(Alignment.BottomStart).offset(f.x.dp, (-10).dp).graphicsLayer {
                                translationY = -200.dp.toPx() * p; val s = .6f + .6f * p; scaleX = s; scaleY = s; alpha = if (p < .15f) p / .15f else 1f - (p - .15f) / .85f
                            })
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr.t, style = ts(20.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                Text(tr.a, style = ts(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
            }
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) {
                val prog by animateFloatAsState(t / tr.d.toFloat(), tween(1000, easing = LinearEasing), label = "prog")
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.15f))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                }
                Row(Modifier.padding(top = 6.dp)) {
                    Text(ltMmss(t), style = ts(11.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f))
                    Text(ltMmss(tr.d), style = ts(11.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum"))
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                ExRoundBtn(Icons.Rounded.SkipPrevious, "Предыдущий трек", size = 48.dp, bg = Color.Transparent, border = null, iconSize = 30.dp) { i = (i + LtTracks.size - 1) % LtTracks.size; t = 0 }
                Box(Modifier.size(68.dp).boxShadow(Kx.accentDeep.copy(alpha = .4f), 30.dp, 10.dp, 34.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep)
                    .kxClick(if (playing) "Пауза для всех" else "Играть для всех") { playing = !playing }, contentAlignment = Alignment.Center) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(38.dp), tint = Kx.onAccent)
                }
                ExRoundBtn(Icons.Rounded.SkipNext, "Следующий трек", size = 48.dp, bg = Color.Transparent, border = null, iconSize = 30.dp) { i = (i + 1) % LtTracks.size; t = 0 }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    LtL.forEach { (l, c) -> Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF121614)).padding(2.dp)) { ExLetter(l, 32.dp, c, font = 12.sp, ink = Kx.text) } }
                }
                Text(if (playing) "Слушают 4 · звук синхронно" else "Пауза — у всех сразу", style = ts(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                Row(Modifier.height(16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    repeat(3) { b ->
                        val w = if (playing) kfLoop(800, b * 200, "eq$b") else 0f
                        Box(Modifier.size(3.dp, 16.dp).graphicsLayer { transformOrigin = TransformOrigin(.5f, 1f); scaleY = .25f + .75f * tri(w) }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                LtR.forEach { (e, label) ->
                    Box(Modifier.size(52.dp, 44.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).kxClick(label) { react(e) }, contentAlignment = Alignment.Center) { Text(e, style = ts(22.sp)) }
                }
            }
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                Text("Дальше в очереди", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
                listOf(1, 2, 3).map { LtTracks[(i + it) % LtTracks.size] }.forEach { q ->
                    key(i, q.t) {
                        Row(Modifier.exIn(250, dy = 8.dp).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).lin2(135f, q.c1, q.c2), contentAlignment = Alignment.Center) {
                                Icon(q.icon, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .8f))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(q.t, style = ts(14.sp, FontWeight.Bold))
                                Text("${q.a} · " + if (q.by == "Вы") "добавили вы" else "в очередь: ${q.by}", style = ts(12.sp, color = Kx.text4))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ltMmss(t: Int) = "${t / 60}:" + (t % 60).toString().padStart(2, '0')
