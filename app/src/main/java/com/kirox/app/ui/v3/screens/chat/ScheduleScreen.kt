package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ScheduleSend
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Days = listOf("Сегодня", "Завтра", "Вт, 24 нояб", "Ср, 25 нояб", "Чт, 26 нояб", "Пт, 27 нояб", "Сб, 28 нояб")
private val Quick = listOf(Triple("Завтра в 8:00", 1, 8), Triple("Завтра в 9:00", 1, 9), Triple("Вечером в 19:00", 0, 19), Triple("В субботу в 10:00", 6, 10))
private data class Sched(val id: Int, val text: String, val whenDay: String, val time: String)
private data class SentMsg(val text: String, val time: String, val silent: Boolean)
private fun p2(n: Int) = n.toString().padStart(2, '0')
/** qa-real: real picker days from today: «Сегодня», «Завтра», «Ср, 25 нояб»… (two weeks). */
internal fun realDays(count: Int = 14): List<String> {
    val f = SimpleDateFormat("EE, d MMM", Locale("ru"))
    return List(count) { i ->
        when (i) { 0 -> "Сегодня"; 1 -> "Завтра"; else -> f.format(java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, i) }.time)
            .replace(".", "").replaceFirstChar { it.uppercase() } }
    }
}

/** Board «Отложенные»: long-press send menu (silent / schedule / when online), date-time wheels, scheduled list with send-now and delete. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(ctx: KxScreenCtx) {
    // Real chat ("Schedule:<chatId>"): store.send / sendSilent / sendScheduled(at | -1 = when online), the list is store.scheduled(chatId),
    // «Отправить сейчас» = sendScheduledNow, delete (after the confirm) = deleteScheduled. Without a chat id nothing is sent (board demo).
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val h = store.header(chatId)
    // qa-real: from a real chat («⋮ → Отложенные») the board opens on the real list, with no demo draft / incoming message.
    val fromChat = ctx.arg?.toLongOrNull() != null
    var draft by remember { mutableStateOf(if (fromChat) "" else "Доброе утро! Не забудь про документы 📄") }
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf(fromChat) }
    val days = if (fromChat) remember { realDays() } else Days
    var day by remember { mutableIntStateOf(1) }
    var hh by remember { mutableIntStateOf(8) }
    var mm by remember { mutableIntStateOf(0) }
    var quick by remember { mutableIntStateOf(0) }
    val sent = remember { mutableStateListOf<SentMsg>() }
    val sched = remember { mutableStateListOf<Sched>() }
    var nextId by remember { mutableIntStateOf(1) }
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var confirm by remember { mutableStateOf<Sched?>(null) }
    val text = draft.trim()
    val realChat = ctx.arg?.toLongOrNull() != null || store is com.kirox.app.ui.v3.demo.KxDemoStore
    val live = ctx.arg?.toLongOrNull() != null
    val realIds = remember { mutableStateMapOf<Int, Long>() }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(live, reload) {
        if (!live) return@LaunchedEffect
        if (reload > 0) kotlinx.coroutines.delay(800)
        val items = store.scheduled(chatId)
        sched.clear(); realIds.clear()
        // «завтра в 14:00» → chip «Завтра», bubble time «14:00»; «когда будет в сети» stays whole.
        items.forEach { s -> val id = nextId++; realIds[id] = s.id
            val timed = s.time.contains(" в ") && !s.time.endsWith("сети")
            sched += Sched(id, s.text, if (timed) s.time.substringBeforeLast(" в ").replaceFirstChar { it.uppercase() } else s.time.replaceFirstChar { it.uppercase() },
                if (timed) s.time.substringAfterLast(" в ") else "—") }
    }
    fun unixAt(dayOffset: Int, h: Int, m: Int): Int = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_MONTH, dayOffset); set(java.util.Calendar.HOUR_OF_DAY, h); set(java.util.Calendar.MINUTE, m); set(java.util.Calendar.SECOND, 0)
    }.timeInMillis.div(1000).toInt()
    fun now() = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date())
    fun say(t: String) { toast = t to System.nanoTime() }
    fun sendNow(silent: Boolean) {
        if (text.isEmpty()) return
        sent += SentMsg(text, now(), silent)
        if (realChat && chatId != 0L) { if (silent) store.sendSilent(chatId, text) else store.send(chatId, text) }
        draft = ""; menu = false; if (silent) say("Отправлено без звука")
    }
    fun addSched(whenDay: String, time: String, at: Int) {
        if (realChat && chatId != 0L) store.sendScheduled(chatId, text, at)
        if (live) reload++
        sched += Sched(nextId++, text, whenDay, time); draft = ""; menu = false; picker = false
        say("Отправится ${whenDay.lowercase()} в $time")
    }
    BackHandler(list) { list = false }
    val whenLabel = (if (day == 0) "сегодня" else if (day == 1) "завтра" else days[day].lowercase()) + " в $hh:${p2(mm)}"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp) { if (list) list = false else ctx.back() }
                if (!list) {
                    KxAvatar(h.title, chatId, size = 38.dp, colorIndex = h.colorIndex, photo = h.avatar)
                    Column(Modifier.weight(1f)) { Text(h.title, style = ts(16, W800), maxLines = 1); Text(h.status, style = ts(12, color = Kx.text3), maxLines = 1) }
                    if (sched.isNotEmpty()) Box(Modifier.size(42.dp).chIn().clip(CircleShape).background(Kx.accent.copy(alpha = .12f)).kxClick("Отложенные") { list = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.ScheduleSend, null, tint = Kx.accent)
                        Box(Modifier.align(Alignment.TopEnd).offset((-2).dp, 2.dp).heightIn(min = 16.dp).widthIn(min = 16.dp).clip(RoundedCornerShape(8.dp)).background(Kx.accent), contentAlignment = Alignment.Center) {
                            Text("${sched.size}", style = ts(11, W800, Kx.onAccent))
                        }
                    }
                } else Column(Modifier.weight(1f)) { Text("Отложенные", style = ts(18, W800)); Text("${h.title} · ${sched.size}", style = ts(12, color = Kx.text3)) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .14f)), reverseLayout = false,
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                if (!list) {
                    if (!fromChat) item {
                        Box(Modifier.fillMaxWidth(.78f)) {
                            ChatBubble(false, "Напомни утром про документы, я сейчас спать", "01:10", false, null)
                        }
                    }
                    items(sent) { m -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { Box(Modifier.fillMaxWidth(.78f), contentAlignment = Alignment.CenterEnd) {
                        ChatBubble(true, m.text, m.time, m.silent, null, Modifier.chIn()) } } }
                } else {
                    if (sched.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(bottom = 200.dp), contentAlignment = Alignment.Center) { Text("Отложенных сообщений нет", style = ts(14, color = Kx.text4)) } }
                    items(sched, key = { it.id }) { m ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(m.whenDay, style = ts(12, color = Kx.text3), modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 4.dp))
                            Column(Modifier.fillMaxWidth(.82f).chIn(dy = 12.dp), horizontalAlignment = Alignment.End) {
                                ChatBubble(true, m.text, m.time, false, Icons.Outlined.Schedule)
                                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick {
                                        sched.remove(m); sent += SentMsg(m.text, now(), false); realIds[m.id]?.let { store.sendScheduledNow(chatId, it) }
                                    }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(15.dp), tint = Kx.accent); Text("Отправить сейчас", style = ts(12, W700, Kx.accent))
                                    }
                                    Box(Modifier.size(30.dp).clip(CircleShape).background(Kx.danger.copy(alpha = .1f)).kxClick("Удалить") { confirm = m }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), tint = Kx.danger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!list) Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart) { ChInput(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth()) }
                Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).semantics { contentDescription = "Отправить; удерживайте — ещё варианты" }
                    .pointerInput(text, menu) { detectTapGestures(onTap = { if (menu) menu = false else sendNow(false) }, onLongPress = { if (text.isNotEmpty()) menu = true }) },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent) }
            }
        }
        AnimatedVisibility(menu, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable(remember { MutableInteractionSource() }, null) { menu = false })
        }
        AnimatedVisibility(menu, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 10.dp, bottom = 76.dp),
            enter = fadeIn(tween(220)) + scaleIn(tween(220, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), .85f, TransformOrigin(1f, 1f)), exit = fadeOut(tween(150))) {
            Column(Modifier.width(260.dp).shadow(24.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color(0xFF161B18)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(6.dp)) {
                MenuItem(Icons.AutoMirrored.Outlined.Send, "Отправить", null, Kx.text) { sendNow(false) }
                MenuItem(Icons.Outlined.NotificationsOff, "Без звука", "Придёт тихо, не разбудит", Kx.text) { sendNow(true) }
                MenuItem(Icons.AutoMirrored.Outlined.ScheduleSend, "Отложить", "Выбрать дату и время", Kx.accent) { menu = false; picker = true }
                MenuItem(Icons.Outlined.HowToReg, "Когда будет в сети", "${h.title} · ${h.status}", Kx.text) { addSched("Когда будет в сети", "—", -1) }
            }
        }
        ChSheet(picker, { picker = false }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Когда отправить?", style = ts(19, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (if (fromChat) Quick.take(3) else Quick).forEachIndexed { i, (label, d, hr) ->
                        val on = quick == i
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, RoundedCornerShape(18.dp))
                            .kxClick(label) { quick = i; day = d; hh = hr; mm = 0 }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = ts(13, W700, if (on) Kx.accent else Kx.text))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Wheel(Modifier.weight(2f), day, days.size, { days[it] }) { day = it; quick = -1 }
                    Wheel(Modifier.weight(1f), hh, 24, { p2(it) }) { hh = it; quick = -1 }
                    Wheel(Modifier.weight(1f), mm / 5, 12, { p2(it * 5) }) { mm = it * 5; quick = -1 }
                }
                ChPrimary("Отправить $whenLabel", modifier = Modifier.padding(top = 14.dp).fillMaxWidth(), height = 54.dp, radius = 16.dp) {
                    if (fromChat && unixAt(day, hh, mm) <= System.currentTimeMillis() / 1000 + 30) say("Выберите время в будущем")
                    else addSched(days[day], "$hh:${p2(mm)}", unixAt(day, hh, mm)) }
            }
        }
        toast?.let { (t, id) -> key(id) { FwdToast(t, 0, bottom = 86.dp, side = 20.dp, minH = 46.dp, radius = 16.dp) { toast = null } } }
        ChConfirm(if (confirm != null) "Удалить отложенное сообщение?" else null, "Сообщение не будет отправлено.", "Удалить", { confirm = null }) { confirm?.let { c -> sched.remove(c); realIds[c.id]?.let { store.deleteScheduled(chatId, it) } }; confirm = null }
    }
}

@Composable
private fun ChatBubble(mine: Boolean, text: String, time: String, silent: Boolean, icon: ImageVector?, modifier: Modifier = Modifier) {
    Row(modifier.clip(if (mine) RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
        .then(if (mine) Modifier.cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = ts(15, color = if (mine) Color(0xFFE6F6EC) else Kx.text, lh = 20.25.sp), modifier = Modifier.weight(1f, fill = false))
        Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val meta = if (mine) Color(0xFFA9D9BF) else Kx.text4
            if (silent) Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(13.dp), tint = meta)
            if (icon != null) Icon(icon, null, Modifier.size(13.dp), tint = meta)
            Text(time, style = ts(11, color = meta))
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, sub: String?, ink: Color, tap: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick(label, onClick = tap).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = ink)
        Column(Modifier.weight(1f)) {
            Text(label, style = ts(15, W600, ink))
            if (sub != null) Text(sub, style = ts(12, color = Kx.text4), modifier = Modifier.padding(top = 1.dp), maxLines = 1)
        }
    }
}

@Composable
private fun Wheel(modifier: Modifier, value: Int, len: Int, fmt: (Int) -> String, set: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(12.dp)).kxClick("Раньше") { set((value - 1 + len) % len) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ExpandLess, null, tint = Kx.text4)
        }
        Text(fmt((value - 1 + len) % len), style = ts(12, color = Kx.text4), maxLines = 1)
        Text(fmt(value), style = ts(20, W800, Kx.accent), modifier = Modifier.padding(vertical = 4.dp), maxLines = 1)
        Text(fmt((value + 1) % len), style = ts(12, color = Kx.text4), maxLines = 1)
        Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(12.dp)).kxClick("Позже") { set((value + 1) % len) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ExpandMore, null, tint = Kx.text4)
        }
    }
}
