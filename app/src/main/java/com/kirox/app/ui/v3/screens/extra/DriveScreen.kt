package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class DrMsg(val id: Long, val name: String, val l: String, val a: Color, val b: Color, val text: String, val reply: String, val private: Boolean = false)
private val DrDemo = listOf(
    DrMsg(0, "Мама", "М", Color(0xFFB07A2E), Color(0xFF6A4C9C), "Позвони, как доедешь", "Хорошо, позвоню через полчаса"),
    DrMsg(0, "Kotlin-клуб", "K", Color(0xFF6FA9D8), Color(0xFF2F6E9E), "Дмитрий: митап переносится на 12:00", "Принято, спасибо"),
    DrMsg(0, "Анна", "А", Color(0xFFC0443C), Color(0xFF6A4C9C), "Купишь молоко по пути?", "Да, куплю"),
)

/**
 * Board «За рулём»: simplified screen with the road strip, one message at a time (read aloud with waves / reply / call / next) and the auto-reply switch.
 * Demo store: the board's queue, dictated reply and speed. Real account: unread unmuted chats, read aloud by TextToSpeech, reply opens the chat.
 */
@Composable
fun DriveScreen(ctx: KxScreenCtx) {
    // demo: needs store (driving mode: auto-reply «Я за рулём», speed from location); real queue = unread chats
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    val q = remember(demo) {
        if (demo) DrDemo else ctx.store.chats.filter { it.unread > 0 && !it.muted }.take(5).map { c ->
            DrMsg(c.id, c.title, c.title.take(1).uppercase(), Color.Unspecified, Color.Unspecified, (c.previewSender?.let { "$it: " } ?: "") + c.preview, "", c.kind == KxChatKind.Private)
        }
    }
    var i by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(if (q.isEmpty()) "idle" else "new") }
    var auto by remember { mutableStateOf(true) }
    var speed by remember { mutableIntStateOf(62) }
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tts = remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(demo) {
        if (!demo) tts.value = android.speech.tts.TextToSpeech(context.applicationContext) { st -> if (st == android.speech.tts.TextToSpeech.SUCCESS) tts.value?.language = java.util.Locale("ru") }
        onDispose { tts.value?.shutdown(); tts.value = null }
    }
    LaunchedEffect(demo) { if (demo) while (true) { delay(1500); speed = 58 + (0..10).random() } }
    fun next() { val n = i + 1; if (n >= q.size) { phase = "idle"; i = n } else { i = n; phase = "new" } }
    LaunchedEffect(step) {
        when (phase) {
            "read" -> { delay(3200); if (phase == "read") phase = "new" }
            "listen" -> { delay(1800); phase = "sent"; delay(1400); next() }
        }
    }
    val idle = phase == "idle"
    val m = q.getOrNull(i.coerceAtMost(q.size - 1))
    val left = q.size - i
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val road = kfRun(1200)
        Box(Modifier.align(Alignment.TopCenter).width(4.dp).fillMaxHeight().drawBehind {
            val p = 80.dp.toPx(); val off = if (road < 0f) 0f else p * road
            var y = -p + off; while (y < size.height) { drawRect(Kx.accent.copy(alpha = .12f), Offset(0f, y), Size(size.width, 40.dp.toPx())); y += p }
        })
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(28.dp), tint = Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text("За рулём", style = ts(20.sp, FontWeight.ExtraBold, Color(0xFFF2F5F3)))
                    Text(if (demo) "$speed км/ч · экран упрощён" else "Экран упрощён", style = ts(13.sp, color = Kx.text3))
                }
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.1f)).kxClick("Выйти") { tts.value?.stop(); ctx.back() }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Выйти", style = ts(15.sp, FontWeight.Bold, Color(0xFFF2F5F3)))
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().heightIn(min = 300.dp).clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF141A17), Color(0xFF0B0F0D)))).border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(30.dp))
                .padding(horizontal = 18.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (idle || m == null) Column(Modifier.exIn(300, dy = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(64.dp), tint = Kx.accentDeep)
                    Text("Новых нет", style = ts(24.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                    Text("Следите за дорогой 🙂", style = ts(16.sp, color = Kx.text3), modifier = Modifier.padding(top = 6.dp))
                } else key(i) {
                    Column(Modifier.fillMaxWidth().exIn(300, dy = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp)) {
                            if (phase == "read") {
                                val r = kfLoop(1400, label = "ring")
                                Box(Modifier.matchParentSize().graphicsLayer { val s = .9f + .9f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .6f * (1f - r) }.border(3.dp, Kx.accent, CircleShape))
                            }
                            if (m.a != Color.Unspecified) ExLetter(m.l, 96.dp, m.a, m.b, 135f, Kx.text, 40.sp)
                            else KxAvatar(m.name, m.id, size = 96.dp, letterWeight = FontWeight.ExtraBold, letterSize = 40.sp)
                        }
                        Text(m.name, style = ts(28.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                        Text(if (left > 1) "Новое сообщение · ещё ${left - 1} ждут" else "Новое сообщение", style = ts(16.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                        when (phase) {
                            "read" -> {
                                Row(Modifier.padding(top = 18.dp).height(40.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    repeat(16) { b ->
                                        val w = kfLoop(900, b * 70, "b$b")
                                        Box(Modifier.size(6.dp, 40.dp).graphicsLayer { scaleY = .25f + .75f * tri(w) }.clip(RoundedCornerShape(3.dp)).background(Kx.accent))
                                    }
                                }
                                Text("«${m.text}»", style = ts(18.sp, color = Color(0xFFE2E9E4), lh = 25.2.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                            }
                            "listen" -> {
                                Text("Говорите ответ…", style = ts(18.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 18.dp))
                                Text("«${m.reply}»", style = ts(22.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            }
                            "sent" -> Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.DoneAll, null, Modifier.size(28.dp), tint = Kx.accent); Text("Отправлено", style = ts(20.sp, FontWeight.ExtraBold, Kx.accent))
                            }
                        }
                    }
                }
            }
            val op = if (idle) .35f else 1f
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DrBtn(Modifier.weight(1f), 110.dp, 28.dp, Icons.Rounded.VolumeUp, 40.dp, "Прочитать", 18.sp, true, op, false) {
                        if (idle || m == null) return@DrBtn
                        phase = "read"; step++
                        if (!demo) tts.value?.speak("${m.name}. ${m.text}", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "drive")
                    }
                    DrBtn(Modifier.weight(1f), 110.dp, 28.dp, Icons.Rounded.Mic, 40.dp, "Ответить", 18.sp, true, op, true) {
                        if (idle || m == null) return@DrBtn
                        if (demo) { phase = "listen"; step++ } else ctx.openChat(m.id)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DrBtn(Modifier.weight(1f), 84.dp, 24.dp, Icons.Rounded.Call, 30.dp, "Позвонить", 17.sp, false, op, false) {
                        if (!demo && m != null && !idle) { if (m.private) ctx.store.call(m.id, false) else ctx.toast("Позвонить можно только человеку") }
                    }
                    DrBtn(Modifier.weight(1f), 84.dp, 24.dp, Icons.Rounded.SkipNext, 30.dp, "Дальше", 17.sp, false, op, false) { if (!idle) { tts.value?.stop(); next() } }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f)).kxClick("Отвечать «Я за рулём»") { auto = !auto }
                .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.SmartToy, null, Modifier.size(24.dp), tint = Kx.text3)
                Column(Modifier.weight(1f)) {
                    Text("Отвечать «Я за рулём»", style = ts(15.sp, FontWeight.Bold))
                    Text("Тем, кто пишет второй раз за 10 минут", style = ts(12.sp, color = Kx.text4))
                }
                KxSwitch(auto, width = 52.dp, height = 30.dp, knob = 24.dp, trackOff = Kx.w(.18f), knobOn = Color(0xFFF2F5F3))
            }
        }
    }
}

@Composable
private fun DrBtn(modifier: Modifier, h: Dp, r: Dp, icon: ImageVector, iconSize: Dp, label: String, size: TextUnit, column: Boolean, op: Float, primary: Boolean, onClick: () -> Unit) {
    val ink = if (primary) Kx.onAccent else Color(0xFFF2F5F3)
    val m = modifier.height(h).graphicsLayer { alpha = op }.clip(RoundedCornerShape(r)).then(if (primary) Modifier.lin2(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.08f)))
        .kxClick(label, onClick = onClick)
    if (column) Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(iconSize), tint = ink); Text(label, style = ts(size, FontWeight.ExtraBold, ink))
    } else Row(m, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(iconSize), tint = ink); Text(label, style = ts(size, FontWeight.ExtraBold, ink))
    }
}
