package com.kirox.app.ui.v3.screens.chat

import com.kirox.app.ui.v3.components.kxHitClick
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val LbChats = listOf(listOf("Анна", "А", "#C0443C", "Звонок идёт…", "сейчас"), listOf("Семья", "С", "#2F6E9E", "Папа: музыка огонь 🔥", "18:20"),
    listOf("Мама", "М", "#B07A2E", "Купи хлеб по дороге 🙂", "18:02"), listOf("Kotlin-клуб", "K", "#3F9A6C", "Дмитрий: PR готов", "17:40"),
    listOf("Лиза", "Л", "#6A4C9C", "Сюрприз готов 🎁", "15:30"), listOf("Работа", "Р", "#5C6661", "Созвон в понедельник", "вчера"), listOf("Олег", "О", "#9AA59F", "Ок", "вчера"))

/**
 * Board «Живые плашки»: ongoing call, music player and upload bars stacked above the chat list.
 * The call/music/upload are simulated → only with KxDemoStore; the real list shows no fake bars.
 */
@Composable
fun LiveBarsScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is KxDemoStore
    // demo: needs store (active call, now playing track with progress, upload queue progress)
    var call by remember { mutableStateOf(demo) }
    var music by remember { mutableStateOf(demo) }
    var upload by remember { mutableStateOf(demo) }
    var sec by remember { mutableIntStateOf(312) }
    var mute by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var m by remember { mutableIntStateOf(74) }
    var up by remember { mutableFloatStateOf(12f) }
    var rk by remember { mutableIntStateOf(0) }
    LaunchedEffect(demo) { if (demo) while (true) { delay(1000); sec++; if (playing) m = (m + 1) % 214 } }
    LaunchedEffect(demo, rk) { if (demo) while (up < 100f) { delay(250); up = minOf(100f, up + 3f + (Math.random() * 4).toFloat()) } }
    val reduced = LocalKxReducedMotion.current
    val inf = rememberInfiniteTransition(label = "live")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "pulse")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "spin")
    val eq by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "eq")

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (call) key(rk) {
                Row(Modifier.chIn(dy = (-20).dp, durationMs = 300).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(Kx.accentDeep, hex("#2F7A55")))).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pa = if (reduced) 1f else 1f - .5f * ((1 - kotlin.math.cos(2 * Math.PI * pulse)) / 2).toFloat()
                    Box(Modifier.size(8.dp).graphicsLayer { alpha = pa }.clip(CircleShape).background(Color.White))
                    Text("Анна · звонок · ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", style = ts(13, W800, Color.White), modifier = Modifier.weight(1f).kxClick { })
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = .2f)).kxClick(if (mute) "Включить микрофон" else "Выключить микрофон") { mute = !mute },
                        contentAlignment = Alignment.Center) { Icon(if (mute) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(18.dp), tint = Color.White) }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Kx.dangerSolid).kxClick("Завершить звонок") { call = false }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CallEnd, null, Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
            if (music) key(rk) {
                Box(Modifier.chIn(50, (-20).dp, 300).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp))) {
                    val pct by animateFloatAsState(m / 214f, tween(1000, easing = LinearEasing), label = "m")
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(pct).height(2.dp).background(Kx.accent))
                    Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).cssGradient(135f, hex("#F2B866"), hex("#C0443C")), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, Modifier.size(20.dp), tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Летний вечер", style = ts(13, W800), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Лунный кот · из «Семья»", style = ts(11, color = Kx.text3), maxLines = 1)
                        }
                        Row(Modifier.height(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { i ->
                                val ph = eq - i * .25f
                                val sy = if (playing && !reduced) .3f + .7f * ((1 - kotlin.math.cos(2 * Math.PI * ph)) / 2).toFloat() else .3f
                                Box(Modifier.size(3.dp, 16.dp).graphicsLayer { scaleY = sy; transformOrigin = TransformOrigin(.5f, 1f) }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                            }
                        }
                        Box(Modifier.size(38.dp).clip(CircleShape).kxClick(if (playing) "Пауза" else "Играть") { playing = !playing }, contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Kx.text)
                        }
                        Box(Modifier.kxHitClick("Закрыть плеер") { music = false }.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                        }
                    }
                }
            }
            if (upload) key(rk) {
                Box(Modifier.chIn(100, (-20).dp, 300).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f))) {
                    val pct by animateFloatAsState(up / 100f, tween(300, easing = LinearEasing), label = "up")
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct).background(Kx.accent.copy(alpha = .12f)))
                    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (up < 100f) Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = spin }) {
                            val w = 2.dp.toPx(); val r = size.minDimension / 2 - w / 2
                            drawCircle(Kx.accent.copy(alpha = .25f), r, style = Stroke(w))
                            drawArc(Kx.accent, -135f, 90f, false, androidx.compose.ui.geometry.Offset(w / 2, w / 2), androidx.compose.ui.geometry.Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Butt))
                        } else Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
                        Text(if (up < 100f) "Отправляем 12 фото в «Семья»" else "Фото отправлены в «Семья»", style = ts(12, W700), modifier = Modifier.weight(1f))
                        Text("${up.toInt()}%", style = ts(12, color = Kx.text3))
                    }
                }
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Чаты", style = ts(26, W800), modifier = Modifier.weight(1f)); Icon(Icons.Outlined.Search, null, tint = Kx.text3)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            if (demo) LbChats.forEach { (name, l, c, msg, t) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LetterAv(l, hex(c), 50.dp, 18)
                    Column(Modifier.weight(1f)) {
                        Row { Text(name, style = ts(15, W700), modifier = Modifier.weight(1f)); Text(t, style = ts(12, color = Kx.text4)) }
                        Text(msg, style = ts(13, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else store.chats.filter { it.kind != KxChatKind.Saved }.take(7).forEach { c ->
                Row(Modifier.fillMaxWidth().kxClick { ctx.openChat(c.id) }.padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChatAvatar(c, 50.dp)
                    Column(Modifier.weight(1f)) {
                        Row { Text(c.title, style = ts(15, W700), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(c.time, style = ts(12, color = Kx.text4)) }
                        Text(c.previewSender?.let { "$it: ${c.preview}" } ?: c.preview, style = ts(13, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (demo) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp), horizontalArrangement = Arrangement.Center) {
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick {
                call = true; music = true; upload = true; up = 12f; sec = 312; playing = true; rk++
            }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("Вернуть все плашки", style = ts(12, W700, Kx.accent)) }
        }
    }
}
