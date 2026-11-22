package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class OffMsg(val id: Int, val text: String, val time: String, val st: String)

/** Board «Нет сети» (demo chat): offline banner, queued messages with spinning clocks, failed retry, reconnect flushes the queue one by one. */
@Composable
fun OfflineScreen(ctx: KxScreenCtx) {
    // demo: explainer chat — nothing is really sent; real queue state would come from TDLib message sending states
    var net by remember { mutableStateOf("off") }
    var draft by remember { mutableStateOf("") }
    var next by remember { mutableIntStateOf(4) }
    var msgs by remember { mutableStateOf(listOf(OffMsg(1, "Еду, пробка на Абая", "18:42", "wait"), OffMsg(2, "Буду минут через 15", "18:43", "wait"), OffMsg(3, "Возьмите мне чай 🙏", "18:43", "failed"))) }
    var flushId by remember { mutableIntStateOf(0) }
    LaunchedEffect(net) { if (net == "connecting") { delay(1200); net = "on"; flushId++ } }
    LaunchedEffect(flushId) {
        if (flushId == 0) return@LaunchedEffect
        while (net == "on") { delay(550); val m = msgs.firstOrNull { it.st == "wait" } ?: break; msgs = msgs.map { if (it.id == m.id) it.copy(st = "sent") else it } }
    }
    val off = net == "off"; val connecting = net == "connecting"; val on = net == "on"
    val waiting = msgs.count { it.st == "wait" }; val failed = msgs.count { it.st == "failed" }; val sentNow = msgs.count { it.st == "sent" }
    val can = draft.isNotBlank()
    fun send() {
        if (!can) return
        msgs = msgs + OffMsg(next, draft.trim(), "18:4" + minOf(9, next + 1), "wait"); draft = ""; next++
        if (on) flushId++
    }

    Column(Modifier.fillMaxSize().background(Kx.bg)) {
        Row(Modifier.fillMaxWidth().background(Kx.bg).statusBarsPadding().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFC0443C), Color(0xFF6A4C9C)))), contentAlignment = Alignment.Center) {
                Text("А", style = KxType.style(16.sp, FontWeight.ExtraBold))
            }
            Column(Modifier.weight(1f)) {
                Text("Анна", style = KxType.style(16.sp, FontWeight.ExtraBold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (off) "ожидание сети" else if (connecting) "подключение" else "в сети", style = KxType.style(12.sp, color = if (off) Kx.warning else if (connecting) Kx.text3 else Kx.accent))
                    if (connecting) {
                        val t by rememberInfiniteTransition(label = "dots").animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "d")
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { i -> val q = ((t - i * .1667f) % 1f + 1f) % 1f
                                Text("•", style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.graphicsLayer { alpha = kf(q, fa(0f, .4f, .8f, 1f), fa(.25f, 1f, .25f, .25f)) }) }
                        }
                    }
                }
            }
        }
        // Banner (kxBanner slide from -100%)
        key(off) {
            val sl = remember { Animatable(0f) }
            LaunchedEffect(Unit) { sl.animateTo(1f, tween(300, easing = TEase)) }
            Box(Modifier.graphicsLayer { translationY = -(1f - sl.value) * size.height }) {
                if (off) Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Kx.warning.copy(alpha = .18f), Kx.warning.copy(alpha = .06f))))
                    .drawBehind { drawLine(Kx.warning.copy(alpha = .25f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WifiOff, null, Modifier.size(22.dp), tint = Kx.warning) }
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = Kx.warning, fontWeight = FontWeight.Bold)) { append("Нет сети.") }
                        append(" " + if (waiting > 0) "$waiting " + (if (waiting == 1) "сообщение ждёт" else "сообщения ждут") + " отправки" else "Сообщения уйдут, когда сеть появится")
                    }, style = KxType.style(13.sp, color = Color(0xFFE4D3B0)), modifier = Modifier.weight(1f))
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.warning.copy(alpha = .18f)).kxClick { net = "connecting" }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Включить сеть", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.warning))
                    }
                }
                else Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Kx.accent.copy(alpha = .18f), Kx.accent.copy(alpha = .05f))))
                    .drawBehind { drawLine(Kx.accent.copy(alpha = .25f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        val wave by rememberInfiniteTransition(label = "wave").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)), label = "w")
                        val reduced = LocalKxReducedMotion.current
                        if (!reduced) Box(Modifier.matchParentSize().graphicsLayer { val s = .6f + 1.8f * wave; scaleX = s; scaleY = s; alpha = .8f * (1f - wave) }.border(2.dp, Kx.accent, CircleShape))
                        Icon(Icons.Outlined.Wifi, null, Modifier.size(20.dp), tint = Kx.accent)
                    }
                    Text(if (connecting) "Подключаемся…" else if (waiting > 0) "Отправляем очередь…" else "Сеть есть, всё отправлено", style = KxType.style(13.sp, color = Color(0xFFC9E8D5)), modifier = Modifier.weight(1f))
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.08f)).kxClick { net = "off" }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Выключить", style = KxType.style(12.sp, FontWeight.Bold))
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState(), reverseScrolling = false).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
            Box(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(buildAnnotatedString { append("Ты где? Мы уже на месте 🙂 "); withStyle(SpanStyle(fontSize = 11.sp, color = Kx.text4)) { append("18:40") } }, style = KxType.style(15.sp))
            }
            msgs.forEach { m ->
                key(m.id) {
                    Row(Modifier.align(Alignment.End).fillMaxWidth(.82f).wrapContentWidth(Alignment.End).tRise(0, 300, 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (m.st == "failed") Box(Modifier.size(28.dp).clip(CircleShape).background(Kx.dangerSolid.copy(alpha = .16f))
                            .kxClick("Отправить снова") { msgs = msgs.map { if (it.id == m.id) it.copy(st = "wait") else it }; if (on) flushId++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Error, null, Modifier.size(18.dp), tint = Kx.danger)
                        }
                        val sent = m.st == "sent"
                        val ink by animateColorAsState(if (sent) Kx.onAccent else Color(0xFFC9E8D5), tween(300), label = "ink")
                        val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
                        Column(Modifier.clip(shape).background(if (sent) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.accent.copy(alpha = .16f)))
                            .then(if (!sent) Modifier.dashedBorder(if (m.st == "failed") Kx.dangerSolid.copy(alpha = .5f) else Kx.accent.copy(alpha = .45f), 18.dp) else Modifier)
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                            Text(m.text, style = KxType.style(15.sp, color = ink))
                            Row(Modifier.align(Alignment.End).padding(top = 2.dp).graphicsLayer { alpha = .75f }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(m.time, style = KxType.style(11.sp, color = ink))
                                if (m.st == "wait") {
                                    val rot by rememberInfiniteTransition(label = "clk").animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "r")
                                    Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp).graphicsLayer { rotationZ = rot }, tint = ink)
                                }
                                if (sent) {
                                    val pop = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) { pop.animateTo(1f, tween(350, easing = TEase)) }
                                    val s = kf(pop.value, fa(0f, .6f, 1f), fa(.5f, 1.3f, 1f))
                                    Icon(Icons.Outlined.DoneAll, null, Modifier.size(15.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (pop.value / .6f).coerceAtMost(1f) }, tint = ink)
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().imePadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fShape = RoundedCornerShape(25.dp)
                Box(Modifier.weight(1f).height(50.dp).clip(fShape).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), fShape).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text(if (off) "Напишите — отправим позже" else "Сообщение", style = KxType.style(15.sp, color = Kx.text4))
                    BasicTextField(draft, { draft = it }, Modifier.fillMaxWidth().semantics { contentDescription = "Сообщение" }, singleLine = true, textStyle = KxType.style(15.sp),
                        cursorBrush = SolidColor(Kx.accent), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }))
                }
                Box(Modifier.size(50.dp).clip(CircleShape).background(if (!can) solid(Kx.w(.07f)) else if (off) solid(Kx.warning.copy(alpha = .25f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)))
                    .kxClick("Отправить") { send() }, contentAlignment = Alignment.Center) {
                    Icon(if (off && can) Icons.AutoMirrored.Rounded.ScheduleSend else Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp),
                        tint = if (!can) Color(0xFF5C6661) else if (off) Kx.warning else Kx.onAccent)
                }
            }
            val hint = if (off) (if (failed > 0) "Нажмите на красный значок, чтобы отправить снова" else "kiroX сам отправит всё, как только появится сеть") else if (sentNow > 0) "Отправлено: $sentNow" else ""
            Text(hint, style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp))
        }
    }
}
