package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class YrRow(val n: String, val l: String, val c: Color, val name: String, val w: Float, val v: String, val fs: TextUnit)
private class YrSlide(val c0: Color, val c1: Color, val blob: Color, val icon: ImageVector, val kicker: String, val big: String, val size: TextUnit, val text: String, val list: List<YrRow> = emptyList())
// demo: needs store (year stats: messages sent, top chats, top emoji, peak hour, voice/call hours)
private val YrSlides = listOf(
    YrSlide(Kx.accentDeep, Color(0xFF14301F), Color(0x738FD3AC), Icons.Rounded.AutoAwesome, "Это был ваш год", "24 318", 64.sp, "сообщений вы отправили. Это примерно как книга на 600 страниц."),
    YrSlide(Color(0xFF2F6E9E), Color(0xFF12283A), Color(0x736FA9D8), Icons.Rounded.Favorite, "С кем вы были ближе всего", "Ваши люди", 44.sp, "", listOf(
        YrRow("1", "М", Color(0xFFB07A2E), "Мама", 1f, "4 812", 16.sp), YrRow("2", "А", Color(0xFFC0443C), "Анна", .82f, "3 960", 16.sp), YrRow("3", "K", Kx.accentDeep, "Kotlin-клуб", .61f, "2 940", 16.sp))),
    YrSlide(Color(0xFFB07A2E), Color(0xFF3A2410), Color(0x73F2B866), Icons.Rounded.Mood, "Ваш любимый эмодзи", "😂", 96.sp, "Вы поставили его 1 204 раза. Смеяться — полезно.", listOf(
        YrRow("2", "❤️", Kx.w(.15f), "Сердце", .64f, "772", 22.sp), YrRow("3", "🔥", Kx.w(.15f), "Огонь", .41f, "493", 22.sp))),
    YrSlide(Color(0xFF6A4C9C), Color(0xFF241938), Color(0x73B39DDB), Icons.Rounded.Nightlight, "Вы — сова", "23:40", 72.sp, "Чаще всего вы писали поздно вечером. Самый болтливый месяц — июль."),
    YrSlide(Color(0xFFC0443C), Color(0xFF3A1412), Color(0x73F2A6A0), Icons.Rounded.GraphicEq, "Вы наговорили", "41 ч", 72.sp, "голосовых сообщений и 18 часов звонков. Спасибо, что выбрали kiroX 💚"),
)

/** Board «Итоги года»: story slides with timed progress bars (5 s, auto-advance), tap zones, floating icon, blobs, top lists with growing bars, share on the last slide. */
@Composable
fun YearRecapScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { com.kirox.app.ui.v3.screens.KxNotYet(ctx, "Итоги года", "Итоги года kiroX посчитает по вашей переписке на этом телефоне. Эта функция ещё не подключена — выдуманных цифр не показываем."); return }
    var i by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val prog = remember { Animatable(0f) }
    LaunchedEffect(k) { prog.snapTo(0f); prog.animateTo(1f, tween(5000, easing = LinearEasing)); if (i < YrSlides.size - 1) { i++; k++ } }
    fun go(d: Int) { i = (i + d).coerceIn(0, YrSlides.size - 1); k++ }
    val sl = YrSlides[i]
    val last = i == YrSlides.size - 1
    val c0 by animateColorAsState(sl.c0, tween(600), label = "c0")
    val c1 by animateColorAsState(sl.c1, tween(600), label = "c1")
    val blob by animateColorAsState(sl.blob, tween(600), label = "blob")
    Box(Modifier.fillMaxSize().lin(170f, 0f to c0, .6f to c1, 1f to Kx.bg)) {
        val b1 = tri(kfRun(9000)); val b2 = tri(kfRun(11000))
        Box(Modifier.offset((-60).dp, 120.dp).size(300.dp).graphicsLayer { translationX = 30.dp.toPx() * b1; translationY = -40.dp.toPx() * b1; val s = 1f + .2f * b1; scaleX = s; scaleY = s }.circleGlow(blob))
        Box(Modifier.align(Alignment.BottomEnd).offset(80.dp, (-120).dp).size(280.dp).graphicsLayer { translationX = 30.dp.toPx() * b2; translationY = -40.dp.toPx() * b2; val s = 1f + .2f * b2; scaleX = s; scaleY = s }.circleGlow(Kx.w(.12f)))
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            key(k) {
                val p = enter(500, easing = KxMotion.rise)
                Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 110.dp, bottom = 120.dp).graphicsLayer { alpha = p; translationY = (1f - p) * 30.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s },
                    verticalArrangement = Arrangement.Center) {
                    val fl = tri(kfRun(4000))
                    Icon(sl.icon, null, Modifier.size(64.dp).graphicsLayer { translationY = -10.dp.toPx() * fl; rotationZ = -6f + 12f * fl }, tint = Color.White.copy(alpha = .9f))
                    Text(sl.kicker, style = ts(16.sp, FontWeight.Bold, Color.White.copy(alpha = .8f)), modifier = Modifier.padding(top = 18.dp))
                    Text(sl.big, style = ts(sl.size, FontWeight.Black, lh = (sl.size.value * 1.05f).sp, ls = (-1).sp), modifier = Modifier.padding(top = 6.dp))
                    if (sl.text.isNotEmpty()) Text(sl.text, style = ts(16.sp, color = Color.White.copy(alpha = .85f), lh = 23.2.sp), modifier = Modifier.padding(top = 10.dp))
                    if (sl.list.isNotEmpty()) Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sl.list.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(r.n, style = ts(18.sp, FontWeight.Black, Color.White.copy(alpha = .7f)), modifier = Modifier.width(30.dp))
                                Box(Modifier.size(40.dp).clip(CircleShape).background(r.c), contentAlignment = Alignment.Center) { Text(r.l, style = ts(r.fs, FontWeight.ExtraBold)) }
                                Column(Modifier.weight(1f)) {
                                    Text(r.name, style = ts(15.sp, FontWeight.ExtraBold))
                                    val bw = enter(800, easing = KxMotion.rise)
                                    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.15f))) {
                                        Box(Modifier.fillMaxWidth(r.w * bw).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Color.White))
                                    }
                                }
                                Text(r.v, style = ts(13.sp, FontWeight.Bold, Color.White.copy(alpha = .85f)))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxSize().padding(top = 70.dp, bottom = 110.dp)) {
                Box(Modifier.weight(.35f).fillMaxHeight().kxClick("Назад") { go(-1) })
                Box(Modifier.weight(.65f).fillMaxHeight().kxClick("Дальше") { go(1) })
            }
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                YrSlides.indices.forEach { j ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.3f))) {
                        val w = if (j < i) 1f else if (j == i) prog.value else 0f
                        Box(Modifier.fillMaxWidth(w).fillMaxHeight().background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) { Text("kX", style = ts(11.sp, FontWeight.Black, Kx.onAccent)) }
                Text("Ваш 2026 в kiroX", style = ts(13.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                ExRoundBtn(Icons.Outlined.Close, "Закрыть", size = 36.dp, bg = Color.Black.copy(alpha = .25f), border = null) { ctx.back() }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color.White.copy(alpha = .92f))
                .kxClick(if (last) "Поделиться итогами" else "Дальше") { if (last) exShare(context, "Мой 2026 в kiroX: 24 318 сообщений, любимый эмодзи 😂") else go(1) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (last) Icons.Rounded.IosShare else Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = Kx.onAccent)
                Text(if (last) "Поделиться итогами" else "Дальше", style = ts(15.sp, FontWeight.ExtraBold, Kx.onAccent))
            }
        }
    }
}
