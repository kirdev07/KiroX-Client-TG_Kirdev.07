package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Demo chat. Real chat (arg = chat id): the delay is the kiroX setting KxChatPrefs.UNDO_SEND; the text goes to store.send only when the countdown ends
// (leaving the board sends it at once), «Отменить» keeps it local. The main chat composer does not read this setting yet.
private val US_DELAYS = listOf("Выкл" to 0, "3 с" to 3, "5 с" to 5, "10 с" to 10)
private data class UsMsg(val id: Int, val t: String, val pending: Boolean, val min: String)

/** UndoSend board: messages wait a few seconds with a countdown ring and «Отменить»; the delay is set in the header. */
@Composable
fun UndoSendScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Отмена отправки"); return }
    val live = chatId != null
    val header = chatId?.let { ctx.store.header(it) }
    val peek = c3Peek(ctx.store, chatId)
    var draft by remember { mutableStateOf(if (live) "" else "Я смогу, но только до обеда") }
    val sent = remember { mutableStateListOf<UsMsg>() }
    var pend by remember { mutableIntStateOf(-1) }
    var left by remember { mutableIntStateOf(0) }
    var delayDemo by remember { mutableIntStateOf(5) }
    val delaySec = if (live) ctx.store.prefs[com.kirox.app.ui.v3.model.KxChatPrefs.UNDO_SEND] ?: 0 else delayDemo
    fun changeDelay(v: Int) { if (live) ctx.store.putPref(com.kirox.app.ui.v3.model.KxChatPrefs.UNDO_SEND, v) else delayDemo = v }
    fun deliver(id: Int) { if (live) sent.firstOrNull { it.id == id }?.let { ctx.store.send(chatId!!, it.t) } }
    val latestPend by rememberUpdatedState(pend)
    DisposableEffect(Unit) { onDispose { if (latestPend >= 0) deliver(latestPend) } }
    var settings by remember { mutableStateOf(false) }
    var n by remember { mutableIntStateOf(0) }
    val can = draft.trim().isNotEmpty()

    LaunchedEffect(pend) {
        if (pend < 0) return@LaunchedEffect
        val id = pend
        while (left > 0) { delay(100); left-- }
        deliver(id)
        sent.indices.forEach { i -> if (sent[i].id == id) sent[i] = sent[i].copy(pending = false) }
        pend = -1
    }
    fun send() {
        val t = draft.trim(); if (t.isEmpty()) return
        val id = n
        if (pend >= 0) deliver(pend)
        sent.indices.forEach { i -> if (sent[i].pending) sent[i] = sent[i].copy(pending = false) }
        sent.add(UsMsg(id, t, delaySec > 0, "${1 + id % 9}")); draft = ""; n++
        if (live && delaySec <= 0) ctx.store.send(chatId!!, t)
        if (delaySec > 0) { left = delaySec * 10; pend = id }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoChatHeader(ctx.back, (header?.title ?: "Р").take(1).uppercase(), Color(0xFF5C6661), null, header?.title ?: "Работа", header?.status ?: "8 участников") {
                MoRound(Icons.AutoMirrored.Outlined.Undo, "Настройки отмены", bg = if (settings) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), border = null,
                    tint = if (settings) Kx.accent else Kx.text) { settings = !settings }
            }
            if (settings) Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp).fillMaxWidth().moIn(250, dy = 10.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                Text("Сколько ждать перед отправкой", style = mts(14, W8))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    US_DELAYS.forEach { (label, v) ->
                        val on = delaySec == v
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(14.dp)).kxClick { changeDelay(v) },
                            contentAlignment = Alignment.Center) { Text(label, style = mts(13, W7, if (on) Kx.accent else Kx.text2)) }
                    }
                }
                Text(if (delaySec > 0) "Сообщение уйдёт через $delaySec с. Пока идёт отсчёт, его можно забрать." else "Сообщения уходят сразу, как обычно",
                    style = mts(12, color = MoMuted), modifier = Modifier.padding(top = 8.dp))
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    val lastIn = if (live) peek.lastOrNull { !it.out && it.text.isNotBlank() } else null
                    if (!live) MoIncoming("Кто сможет подменить Марию в понедельник?", author = "Начальник", authorColor = Color(0xFF6FA9D8), size = 14, modifier = Modifier.widthIn(max = w * .8f))
                    else if (lastIn != null) MoIncoming(lastIn.text, lastIn.time, author = lastIn.sender, authorColor = Color(0xFF6FA9D8), size = 14, modifier = Modifier.widthIn(max = w * .8f))
                    sent.forEach { m -> key(m.id) { UsBubble(m, Modifier.align(Alignment.End).widthIn(max = w * .82f)) } }
                }
            }
            if (pend >= 0) Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp).fillMaxWidth().moIn(200, dy = 10.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xF528302C), Color(0xFA121614)))).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(20.dp))
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    val f = 1f - left / (delaySec * 10f).coerceAtLeast(1f)
                    Canvas(Modifier.matchParentSize()) {
                        val r = 15.dp.toPx(); val sw = 3.dp.toPx()
                        drawCircle(Kx.w(.12f), r, style = Stroke(sw))
                        drawArc(Kx.accent, -90f, 360f * (1f - f), false, topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Text("${(left + 9) / 10}", style = mts(13, W8))
                }
                Text("Отправляем…", style = mts(14, color = Kx.text2), modifier = Modifier.weight(1f))
                MoPill("Отменить", height = 40.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 14, weight = W8, padH = 16.dp,
                    icon = Icons.AutoMirrored.Outlined.Undo, iconSize = 18.dp, gap = 6.dp) {
                    val m = sent.firstOrNull { it.id == pend }; sent.removeAll { it.id == pend }; pend = -1; left = 0; draft = m?.t ?: ""
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                    .border(1.dp, Kx.w(.12f), RoundedCornerShape(25.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    MoInput(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth())
                }
                Box(Modifier.size(50.dp).clip(CircleShape).then(if (can) Modifier.moAccent() else Modifier.background(Kx.w(.07f))).kxClick("Отправить") { send() },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = if (can) MoInk else Color(0xFF5C6661)) }
            }
        }
    }
}

@Composable
private fun UsBubble(m: UsMsg, modifier: Modifier) {
    val p = moEnter(300)
    val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    val bg by animateColorAsState(if (m.pending) Kx.accent.copy(alpha = .14f) else Color.Transparent, tween(300), label = "bg")
    val ink = if (m.pending) Color(0xFFC9E8D5) else MoInk
    Box(modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 30.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }.clip(shape)
        .then(if (m.pending) Modifier.background(bg).moDashed(shape, Kx.accent.copy(alpha = .5f)) else Modifier.moAccent()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        MoBubbleText(m.t, "18:4${m.min}", ink, icon = if (m.pending) Icons.Outlined.Schedule else Icons.Outlined.DoneAll, iconSize = 14)
    }
}
