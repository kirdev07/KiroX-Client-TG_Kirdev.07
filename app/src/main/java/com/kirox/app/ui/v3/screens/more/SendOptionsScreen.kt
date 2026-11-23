package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo drafts. Real chat (arg = chat id): the composer is a real field and the options call store.sendWith (silent / tomorrow 08:00 / when online).
private val SO_DRAFTS = listOf("Спокойной ночи! Завтра созвонимся 🌙", "Не забудь зонт, обещают дождь ☔", "Билеты уже в почте, глянь утром", "Удачи на собеседовании! 💚")
private data class SoMsg(val id: Int, val text: String, val kind: String, val done: Boolean)
private data class SoOpt(val icon: ImageVector, val t: String, val s: String, val c: Color, val kind: String)

/** SendOptions board: long-press send → silent / schedule / when online / now; sent bubbles show how they will arrive. */
@Composable
fun SendOptionsScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Варианты отправки"); return }
    val live = chatId != null
    val peek = c3Peek(ctx.store, chatId)
    val header = chatId?.let { ctx.store.header(it) }
    val name = header?.title?.substringBefore(' ') ?: "Анна"
    val canOnline = !live || header?.kind == com.kirox.app.ui.v3.model.KxChatKind.Private
    var menu by remember { mutableStateOf(!live) }
    val sent = remember { mutableStateListOf<SoMsg>() }
    var onlineDemo by remember { mutableStateOf(false) }
    val online = if (live) header?.online == true else onlineDemo
    var n by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    val text = if (live) draft else SO_DRAFTS[n % SO_DRAFTS.size]
    fun add(kind: String) {
        menu = false
        if (live) {
            if (text.isBlank()) { ctx.toast("Сначала напишите сообщение"); return }
            ctx.store.sendWith(chatId!!, text.trim(), com.kirox.app.ui.v3.model.KxSendOptions(silent = kind == "silent",
                at = when (kind) { "later" -> c3NextHour(8); "online" -> -1; else -> 0 }))
            draft = ""
        }
        sent.add(SoMsg(n, text, kind, kind != "online" || online)); n++
        while (sent.size > 4) sent.removeAt(0)
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().moBottomLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoBack(ctx.back)
                Box(Modifier.size(40.dp)) {
                    MoLetter(name.take(1).uppercase().ifEmpty { "?" }, Color(0xFFC0443C))
                    if (header?.avatar != null) com.kirox.app.ui.v3.components.KxImage(header.avatar, Modifier.matchParentSize().clip(CircleShape), maxPx = 120)
                    val dot by animateColorAsState(if (online) Kx.accent else Color(0xFF5C6661), tween(300), label = "dot")
                    Box(Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape).background(dot).border(2.dp, Kx.bg, CircleShape))
                }
                Column(Modifier.weight(1f)) {
                    Text(header?.title ?: "Анна", style = mts(16, W8), maxLines = 1)
                    Text(if (live) header!!.status else if (online) "в сети" else "была в 23:42", style = mts(12, color = if (online) Kx.accent else MoSub), maxLines = 1)
                }
            }
            // critique-A S: demo-only switch lives in the dashed «демо» tray (never on a real account).
            if (!live && !online && sent.any { it.kind == "online" && !it.done }) com.kirox.app.ui.v3.components.KxDemoControls(ctx.store) {
                MoPill("Анна заходит в сеть", height = 32.dp, size = 11, ink = MoSub, padH = 10.dp) {
                    onlineDemo = true
                    sent.indices.forEach { i -> if (sent[i].kind == "online") sent[i] = sent[i].copy(done = true) }
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp)).background(Kx.w(.06f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text("Сегодня", style = mts(11, W7, MoSub))
                    }
                    val last = if (live) peek.lastOrNull { !it.out && it.text.isNotBlank() } else null
                    if (!live) MoIncoming("Я уже сплю, завтра рано вставать 😴", "23:41", modifier = Modifier.widthIn(max = w * .78f))
                    else if (last != null) MoIncoming(last.text, last.time, modifier = Modifier.widthIn(max = w * .78f))
                    sent.forEach { m -> key(m.id) { SoBubble(m, Modifier.align(Alignment.End).widthIn(max = w * .82f), name, live) } }
                }
            }

            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (live) MoInput(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth(), singleLine = false) else Text(text, style = mts(15))
                }
                Box(Modifier.size(52.dp).moGlow(2200, 6.dp, .18f, 26.dp).clip(CircleShape).moAccent().kxClick("Варианты отправки") { menu = true },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = MoInk) }
            }
        }
        if (!menu) Text("удерживайте, чтобы выбрать →", style = mts(11, color = MoMuted),
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 74.dp, bottom = 30.dp))

        if (menu) {
            Box(Modifier.matchParentSize().moFade(200).background(Color.Black.copy(alpha = .5f)).kxClick("Закрыть") { menu = false })
            val p = moEnter(250, easing = KxMotion.rise)
            Column(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 84.dp).width(270.dp).graphicsLayer {
                alpha = p; translationY = (1f - p) * 14.dp.toPx(); val s = .9f + .1f * p; scaleX = s; scaleY = s; transformOrigin = TransformOrigin(1f, 1f)
            }.shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(24.dp)).padding(6.dp)) {
                listOf(
                    SoOpt(Icons.Rounded.NotificationsOff, "Без звука", "Придёт тихо, без уведомления", Kx.accent, "silent"),
                    SoOpt(Icons.Rounded.Event, "Отложить", if (live) "Уйдёт в 08:00" else "Выбрать дату и время", Color(0xFF7FB8E0), "later"),
                    SoOpt(Icons.Rounded.HowToReg, "Когда будет в сети", "Отправим, как только $name зайдёт", MoAmber, "online"),
                    // critique-A: no «Отправить сейчас» — the send button itself does that.
                ).filter { canOnline || it.kind != "online" }.forEach { o ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).kxClick { add(o.kind) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(if (o.kind == "normal") Kx.w(.08f) else o.c.copy(alpha = .16f)),
                            contentAlignment = Alignment.Center) { Icon(o.icon, null, Modifier.size(20.dp), tint = o.c) }
                        Column(Modifier.weight(1f)) {
                            Text(o.t, style = mts(15, W7))
                            Text(o.s, style = mts(12, color = MoSub))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoBubble(m: SoMsg, modifier: Modifier, name: String = "Анна", live: Boolean = false) {
    val wait = m.kind == "online" && !m.done
    val sched = m.kind == "later"
    val quiet = wait || sched
    val p = moEnter(300, easing = KxMotion.rise)
    val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    Column(modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s },
        horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.clip(shape).then(if (quiet) Modifier.background(Kx.accent.copy(alpha = .08f)).moDashed(shape, Kx.accent.copy(alpha = .5f)) else Modifier.moAccent())
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            MoBubbleText(m.text, if (sched) "08:00" else "23:52", if (quiet) Kx.text else MoInk, icon = if (quiet) Icons.Outlined.Schedule else Icons.Outlined.DoneAll, iconSize = 14)
        }
        val noteIcon = when (m.kind) { "silent" -> Icons.Rounded.NotificationsOff; "later" -> Icons.Rounded.Event; "online" -> if (wait) Icons.Rounded.HourglassTop else Icons.Rounded.CheckCircle; else -> Icons.Rounded.NotificationsActive }
        val note = when (m.kind) { "silent" -> if (live) "Без звука — уведомление придёт тихо" else "Без звука — Анну не разбудит"; "later" -> if (live) "Уйдёт в 08:00" else "Уйдёт завтра в 08:00"; "online" -> if (wait) "Уйдёт, когда $name появится в сети" else "Отправлено — $name в сети"; else -> "Со звуком" }
        val ink = if (m.kind == "normal") MoMuted else Kx.accent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val t = if (wait) moRun(1200, 2) else -1f
            Icon(noteIcon, null, Modifier.size(14.dp).graphicsLayer { rotationZ = if (t < 0f) 0f else 20f * MoEaseInOut.transform(moTri(t)) }, tint = ink)
            Text(note, style = mts(11, W7, ink))
        }
    }
}
