package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxMsgKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private data class LogItem(val day: String? = null, val dayN: Int = 0, val by: Long = 0, val who: String = "", val color: Int = 0, val mine: Boolean = false,
                           val text: String = "", val time: String = "", val msgId: Long = 0)

// demo: used when the chat has no loaded messages (no arg / empty history)
private val DemoLog = listOf(
    LogItem(day = "18 ноября", dayN = 18),
    LogItem(by = 1, who = "Марина", color = 0, text = "Всем привет! Домашка на неделю в закрепе", time = "10:02"),
    LogItem(by = 2, who = "Олег", color = 6, text = "А когда следующий урок?", time = "10:15"),
    LogItem(by = 1, who = "Марина", color = 0, text = "Урок в четверг в 19:00, не опаздывайте", time = "10:16"),
    LogItem(day = "20 ноября", dayN = 20),
    LogItem(by = -1, who = "Вы", color = 7, mine = true, text = "Скиньте, пожалуйста, ссылку на урок", time = "18:50"),
    LogItem(by = 3, who = "Дана", color = 4, text = "Вот ссылка на урок, заходите", time = "18:52"),
    LogItem(by = 2, who = "Олег", color = 6, text = "Спасибо, отличный урок был!", time = "20:31"),
    LogItem(day = "Сегодня", dayN = 22),
    LogItem(by = 1, who = "Марина", color = 0, text = "Напоминаю: тест после урока в субботу", time = "09:40"),
    LogItem(by = -1, who = "Вы", color = 7, mine = true, text = "Понял, буду готов", time = "09:58"),
)
private val NameInks = listOf(Color(0xFFC7B5FF), Color(0xFF9FCBFF), Color(0xFFBDEDE8), Color(0xFFF2DDBF), Color(0xFFFFB0C4), Color(0xFFFFE9C9), Color(0xFF9FCBFF), Color(0xFFA9D9BF))

/** Board «Поиск в чате»: highlighted hits in the chat, ↑/↓ navigation with a flash, list view, «От кого» and «По дате» sheets. arg = chatId. */
@Composable
fun ChatSearchScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull()
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val real = chatId?.let { store.messages(it) }.orEmpty().filter { it.kind != KxMsgKind.Service }
    val log = remember(real) {
        if (demo) DemoLog else buildList {
            var n = 0
            real.forEach { m ->
                m.dayHeader?.let { add(LogItem(day = it, dayN = ++n)) }
                add(LogItem(by = if (m.out) -1 else m.senderId, who = if (m.out) "Вы" else m.sender ?: store.header(chatId!!).title,
                    color = if (m.out) 7 else m.senderColorIndex.takeIf { it >= 0 } ?: ((m.senderId % 7 + 7) % 7).toInt(), mine = m.out,
                    text = m.text.ifEmpty { m.meta }, time = m.time, msgId = m.id))
            }
        }
    }
    var q by remember { mutableStateOf(if (demo) "урок" else "") }
    var from by remember { mutableStateOf<Long?>(null) }
    var cur by remember { mutableIntStateOf(-1) }
    var listMode by remember { mutableStateOf(false) }
    var fromSheet by remember { mutableStateOf(false) }
    var calSheet by remember { mutableStateOf(false) }
    // qa-real: after «Перейти к этому дню» (JumpDate) the search steps aside so the chat shows the day.
    var dateJump by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val jumpTarget = chatId?.let { store.jumpTarget(it) } ?: 0L
    LaunchedEffect(jumpTarget, dateJump) { if (dateJump && jumpTarget != 0L) { dateJump = false; ctx.back() } }
    var jump by remember { mutableIntStateOf(0) }
    var flash by remember { mutableIntStateOf(0) }
    val qq = q.trim().lowercase()
    // Server search (store.searchInChat, newest first) adds hits that are loaded; ↑ past the oldest loaded hit loads older ones with store.jumpTo.
    val remoteIds by produceState(emptyList<Long>(), chatId, qq) {
        value = if (chatId == null || qq.isEmpty() || demo) emptyList() else { kotlinx.coroutines.delay(300); store.searchInChat(chatId, qq) }
    }
    val hits = log.indices.filter { i -> val m = log[i]; m.day == null && qq.isNotEmpty() && (m.text.lowercase().contains(qq) || (m.msgId != null && m.msgId in remoteIds)) && (from == null || m.by == from) }
    val loadedIds = log.mapNotNull { it.msgId }.toSet()
    val olderUnloaded = remoteIds.firstOrNull { it !in loadedIds && (log.firstOrNull { l -> l.msgId != null }?.msgId?.let { o -> it < o } ?: false) }
    val curPos = if (hits.isEmpty()) -1 else if (cur < 0 || cur >= hits.size) hits.size - 1 else cur
    val curIdx = if (curPos >= 0) hits[curPos] else -1
    val people = log.filter { it.day == null }.groupBy { it.by }
    val list = rememberLazyListState()
    LaunchedEffect(curIdx, flash) { if (curIdx >= 0 && !listMode) list.animateScrollToItem(maxOf(0, curIdx - 2)) }
    LaunchedEffect(jump) { if (jump > 0) log.indexOfFirst { it.dayN == jump }.takeIf { it >= 0 }?.let { list.animateScrollToItem(it) } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Закрыть поиск", 42.dp, bg = Color.Transparent, border = Color.Transparent, onClick = ctx.back)
                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(22.dp))
                    .padding(start = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                    from?.let { f -> people[f]?.first()?.let { p ->
                        Row(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .16f)).kxClick("Убрать фильтр") { from = null; cur = -1 }
                            .padding(start = 4.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            KxAvatar(p.who, p.by, size = 20.dp, colorIndex = p.color); Text(p.who, style = ts(12, W700, Kx.accent), maxLines = 1)
                            Icon(Icons.Outlined.Close, null, Modifier.size(14.dp), tint = Kx.accent)
                        }
                    } }
                    ChInput(q, { q = it; cur = -1 }, "Поиск в чате", Modifier.weight(1f))
                    if (q.isNotEmpty()) Box(Modifier.kxHitClick("Очистить") { q = ""; cur = -1 }.size(30.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Cancel, null, Modifier.size(18.dp), tint = Kx.text4)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SChip(Icons.Outlined.PersonSearch, "От кого", false) { fromSheet = true }
                SChip(Icons.Outlined.CalendarMonth, "По дате", false) {
                    // qa-real: the real chat uses the real calendar (JumpDate: real days, today, GetChatMessageByDate); the sheet is board demo.
                    if (!demo && chatId != null) { dateJump = true; ctx.open("JumpDate:$chatId") } else calSheet = true }
                SChip(Icons.AutoMirrored.Outlined.List, "Списком", listMode) { listMode = !listMode }
            }
            if (!listMode) LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(log) { i, m ->
                    if (m.day != null) Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        val on = jump == m.dayN
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .2f) else Kx.w(.06f), tween(300), label = "d")
                        val ink by animateColorAsState(if (on) Kx.accent else Kx.text3, tween(300), label = "di")
                        Text(m.day, style = ts(12, W600, ink), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 12.dp, vertical = 4.dp))
                    } else {
                        val hit = i in hits
                        val op by animateFloatAsState(if (qq.isNotEmpty() && !hit) .45f else 1f, tween(250), label = "op")
                        LogBubble(m, if (hit) splitAll(m.text, qq) else AnnotatedString(m.text), Modifier.alpha(op), if (i == curIdx) flash + 1 else 0)
                    }
                }
            } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                item { Text(if (hits.isNotEmpty()) "Найдено: ${hits.size}" else "Ничего не найдено", style = ts(13, W800, Kx.accent), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) }
                items(hits.reversed(), key = { it }) { i ->
                    val m = log[i]
                    val date = log.subList(0, i).lastOrNull { it.day != null }?.day
                    Row(Modifier.fillMaxWidth().chIn(dy = 8.dp, durationMs = 250).clip(RoundedCornerShape(18.dp)).kxClick { listMode = false; cur = hits.indexOf(i); flash++ }
                        .padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KxAvatar(m.who, m.by, size = 42.dp, colorIndex = m.color)
                        Column(Modifier.weight(1f)) {
                            Row { Text(m.who, style = ts(14, W700), modifier = Modifier.weight(1f), maxLines = 1); Text(listOfNotNull(date, m.time).joinToString(", "), style = ts(12, color = Kx.text4)) }
                            Text(splitHit(m.text, qq) ?: AnnotatedString(m.text), style = ts(14, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(when { qq.isEmpty() && from == null -> "Введите слово для поиска"; hits.isNotEmpty() -> "${curPos + 1} из ${hits.size}"; else -> "Нет совпадений" },
                    style = ts(14, color = if (hits.isNotEmpty()) Kx.text2 else Kx.text4), modifier = Modifier.weight(1f))
                ChRound(Icons.Outlined.KeyboardArrowUp, "Предыдущее совпадение", bg = Kx.w(.07f), border = Color.Transparent,
                    tint = if (curPos > 0 || olderUnloaded != null) Kx.text else Color(0xFF4A544F)) {
                    if (curPos > 0) { cur = curPos - 1; flash++ }
                    else if (chatId != null && olderUnloaded != null) { store.jumpTo(chatId, olderUnloaded); cur = 0; flash++ }
                }
                ChRound(Icons.Outlined.KeyboardArrowDown, "Следующее совпадение", bg = Kx.w(.07f), border = Color.Transparent,
                    tint = if (curPos >= 0 && curPos < hits.size - 1) Kx.text else Color(0xFF4A544F)) { if (curPos < hits.size - 1) { cur = curPos + 1; flash++ } }
            }
        }
        val sheetBg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))
        ChSheet(fromSheet, { fromSheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = sheetBg) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Сообщения от", style = ts(18, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
                people.forEach { (by, items) ->
                    val p = items.first()
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick(p.who) { from = by; fromSheet = false; cur = -1 }.padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KxAvatar(p.who, by, size = 40.dp, colorIndex = p.color)
                        Text(p.who, style = ts(15, W600), modifier = Modifier.weight(1f)); Text("${items.size} сообщ.", style = ts(12, color = Kx.text4))
                    }
                }
            }
        }
        ChSheet(calSheet, { calSheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = sheetBg) {
            // demo: needs store (per-day message counts for the calendar are not in the store yet)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Ноябрь 2026", style = ts(18, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
                MonthGrid(firstOffset = 6, days = 30, withMsgs = setOf(18, 20, 22), today = 22) { n -> calSheet = false; jump = if (log === DemoLog) n else log.firstOrNull { it.day != null }?.dayN ?: 0 }
                Text("Точкой отмечены дни с сообщениями", style = ts(12, color = Kx.text4), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
            }
        }
    }
}

/** 7-column month grid (Пн first) used by «По дате». */
@Composable
internal fun MonthGrid(firstOffset: Int, days: Int, withMsgs: Set<Int>, today: Int, tap: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { Text(it, style = ts(11, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(bottom = 4.dp)) }
        }
        repeat(5) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { c ->
                    val n = r * 7 + c - firstOffset + 1
                    val valid = n in 1..days; val has = n in withMsgs
                    Box(Modifier.weight(1f).height(40.dp).alpha(if (!valid) 0f else if (n > today) .35f else 1f).clip(RoundedCornerShape(20.dp))
                        .background(if (n == today) Kx.accent.copy(alpha = .16f) else Color.Transparent).kxClick(if (valid) "$n ноября" else null, enabled = valid && has) { tap(n) },
                        contentAlignment = Alignment.Center) {
                        if (valid) Text("$n", style = ts(14, if (has) W800 else W500, if (has) Kx.text else Kx.text4))
                        if (has) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp).clip(CircleShape).background(Kx.accent))
                    }
                }
            }
        }
    }
}

private fun splitAll(text: String, q: String): AnnotatedString {
    val k = text.lowercase().indexOf(q); if (k < 0 || q.isEmpty()) return AnnotatedString(text)
    return marked(text.substring(0, k), text.substring(k, k + q.length), text.substring(k + q.length))
}

@Composable
private fun SChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, tap: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.06f), tween(200), label = "c")
    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(label, onClick = tap).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, Modifier.size(17.dp), tint = if (on) Kx.accent else Kx.text); Text(label, style = ts(13, W600, if (on) Kx.accent else Kx.text))
    }
}

@Composable
private fun LogBubble(m: LogItem, text: AnnotatedString, modifier: Modifier, flashKey: Int) {
    val reduced = LocalKxReducedMotion.current
    val f = remember { Animatable(0f) }
    LaunchedEffect(flashKey) {
        if (flashKey > 0 && !reduced) repeat(2) { f.animateTo(1f, tween(420)); f.animateTo(0f, tween(980)) }
    }
    val shape = if (m.mine) RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
    Box(modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.fillMaxWidth(.8f).wrapContentWidth(if (m.mine) Alignment.End else Alignment.Start)
            .then(if (f.value > 0f) Modifier.border(4.dp, Kx.accent.copy(alpha = .55f * f.value), shape) else Modifier)
            .clip(shape).then(if (m.mine) Modifier.cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (!m.mine) Text(m.who, style = ts(12, W800, NameInks[m.color.coerceIn(0, 7)]), modifier = Modifier.padding(bottom = 2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text, style = ts(15, color = if (m.mine) Color(0xFFE6F6EC) else Kx.text, lh = 20.25.sp), modifier = Modifier.weight(1f, fill = false))
                Text(m.time, style = ts(11, color = if (m.mine) Color(0xFFA9D9BF) else Kx.text4), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
