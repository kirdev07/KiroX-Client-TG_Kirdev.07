package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.more.c3Chat
import com.kirox.app.ui.v3.screens.more.c3Live
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import java.time.LocalDate
import kotlinx.coroutines.launch

private val Months = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
private val Gen = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val Wdl = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
// Demo heat-map. Real chat (arg = chat id): no per-day counts (TDLib's calendar needs a media filter), the picked day is
// checked with store.messageAtDate (GetChatMessageByDate) and «Перейти» opens the chat there (store.jumpTo).
private val First = LocalDate.of(2025, 9, 3)
private val Today = LocalDate.of(2026, 11, 22)
private val Samples = listOf("Ты дома?" to "Да, уже приехала", "Скинь фото с прогулки" to "Держи 📸", "Кино в 20:00?" to "Идём!", "Как прошёл день?" to "Устала, но всё хорошо", "Купить хлеб?" to "И молоко 🙂")
private val Hot = setOf(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 14), LocalDate.of(2025, 11, 22), LocalDate.of(2025, 9, 3))
private fun countDemo(d: LocalDate): Int {
    if (d < First || d > Today) return 0
    val k = d.year * 400 + (d.monthValue - 1) * 31 + d.dayOfMonth
    val v = kotlin.math.abs((kotlin.math.sin(k * 12.9898) * 43758.5453) % 1.0)
    val base = if (v < .28) 0 else Math.round(v * v * 90).toInt()
    return if (d in Hot) maxOf(base, 14) else base
}
private val Lvl = listOf(Color.Transparent, Kx.accent.copy(alpha = .1f), Kx.accent.copy(alpha = .25f), Kx.accent.copy(alpha = .45f), Kx.accent.copy(alpha = .7f))

/** Board «Перейти к дате»: month heat-map calendar, day preview with jump, quick jumps. arg = chatId. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JumpDateScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val live = ctx.c3Live
    if (live && ctx.c3Chat == null) { com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Перейти к дате"); return }
    val chatId = ctx.c3Chat ?: ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val title = store.header(chatId).title
    val today = if (live) LocalDate.now() else Today
    val first = if (live) LocalDate.of(2013, 8, 1) else First
    var ym by remember { mutableStateOf(today.withDayOfMonth(1)) }
    // fixer-chat4: the real chat marks the days that have messages (store.messageDays for the shown month; null while loading).
    val liveDays = if (live) com.kirox.app.ui.v3.screens.more.c3Load(chatId, ym) { store.messageDays(chatId, ym.year, ym.monthValue) } else null
    fun count(d: LocalDate): Int = if (live) (if (d.year == ym.year && d.monthValue == ym.monthValue && liveDays?.contains(d.dayOfMonth) == true) 10 else 0) else countDemo(d)
    var sel by remember { mutableIntStateOf(if (live) today.dayOfMonth else 14) }
    var dir by remember { mutableIntStateOf(0) }
    var gen by remember { mutableIntStateOf(0) }
    var jumped by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val days = ym.lengthOfMonth()
    val lead = ym.dayOfWeek.value - 1
    val monthTotal = (1..days).sumOf { count(ym.withDayOfMonth(it)) }
    val canPrev = ym > first.withDayOfMonth(1)
    val canNext = ym < today.withDayOfMonth(1)
    fun go(d: LocalDate, day: Int, direction: Int) { ym = d.withDayOfMonth(1); sel = day; dir = direction; gen++ }
    val selDate = if (sel > 0) ym.withDayOfMonth(sel) else null
    val selCount = selDate?.let { count(it) } ?: 0
    val dayStart = selDate?.let { (it.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond()).toInt() }
    val foundId = if (live && dayStart != null) com.kirox.app.ui.v3.screens.more.c3Load(chatId, dayStart) { store.messageAtDate(chatId, dayStart) ?: -1L } else null
    val hasDay = if (live) (foundId ?: -1L) > 0 else selCount > 0
    val smp = Samples[(sel + ym.monthValue - 1).mod(Samples.size)]

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = ctx.back)
            Column(Modifier.weight(1f)) { Text("Перейти к дате", style = ts(20, W800)); Text("Чат: $title", style = ts(12, color = Kx.text3), maxLines = 1) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
            Row(Modifier.padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.Outlined.ChevronLeft, "Предыдущий месяц", 40.dp, border = Color.Transparent, modifier = Modifier.alpha(if (canPrev) 1f else .3f)) { if (canPrev) go(ym.minusMonths(1), -1, -1) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${Months[ym.monthValue - 1]} ${ym.year}", style = ts(18, W800))
                    if (!live) Text(if (monthTotal > 0) "$monthTotal ${plural(monthTotal, "сообщение", "сообщения", "сообщений")}" else "Сообщений не было", style = ts(12, color = Kx.text3))
                    else Text(when { liveDays == null -> "Ищем дни с сообщениями…"; liveDays.isEmpty() -> "В этом месяце сообщений не было"
                        else -> "Дней с сообщениями: ${liveDays.size}" }, style = ts(12, color = Kx.text3))
                }
                ChRound(Icons.Outlined.ChevronRight, "Следующий месяц", 40.dp, border = Color.Transparent, modifier = Modifier.alpha(if (canNext) 1f else .3f)) { if (canNext) go(ym.plusMonths(1), -1, 1) }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(24.dp)).padding(12.dp)) {
                Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { Text(it, style = ts(11, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                }
                key(gen) {
                    Column(Modifier.then(if (dir != 0) Modifier.chIn(dx = (30 * dir).dp, dy = 0.dp, durationMs = 250) else Modifier), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val cells = lead + days
                        (0 until (cells + 6) / 7).forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(7) { c ->
                                    val d = r * 7 + c - lead + 1
                                    if (d < 1 || d > days) Spacer(Modifier.weight(1f).aspectRatio(1f)) else {
                                        val date = ym.withDayOfMonth(d); val n = count(date); val on = sel == d
                                        val isToday = date == today; val off = date > today || date < first
                                        val lvl = when { n == 0 -> 0; n < 10 -> 1; n < 30 -> 2; n < 60 -> 3; else -> 4 }
                                        Column(Modifier.weight(1f).aspectRatio(1f).alpha(if (off) .25f else 1f).then(if (on) Modifier.chPop(.6f, 250, fade = false) else Modifier)
                                            .clip(RoundedCornerShape(14.dp)).then(if (on) Modifier.cssGradient(160f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Lvl[lvl]))
                                            .border(1.5.dp, if (isToday && !on) Kx.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                            .kxClick("$d ${Gen[ym.monthValue - 1]}: " + if (live) (if (n > 0) "есть сообщения" else "нет сообщений") else if (n > 0) "$n ${plural(n, "сообщение", "сообщения", "сообщений")}" else "нет сообщений") {
                                                if (!off) {
                                                    sel = d
                                                    // fixer-chat4: a highlighted day jumps right away (no second tap on «Перейти»).
                                                    if (live && n > 0) scope.launch {
                                                        val dayStart = date.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond().toInt()
                                                        val id = store.messageAtDate(chatId, dayStart)
                                                        if (id != null && id > 0) { if (store.historyChatId == chatId) ctx.back() else ctx.openChat(chatId); store.jumpTo(chatId, id) }
                                                    }
                                                }
                                            },
                                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                                            Text("$d", style = ts(14, if (on || isToday) W800 else W500, if (on || lvl >= 3) Kx.onAccent else if (lvl == 0) Kx.text4 else Kx.text)) // critique-A: empty days grey
                                            Box(Modifier.size(4.dp).clip(CircleShape).background(if (isToday && !on) Kx.accent else Color.Transparent))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!live) Row(Modifier.align(Alignment.End).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Меньше", style = ts(11, color = Kx.text4))
                    Lvl.drop(1).forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(it)) }
                    Text("Больше", style = ts(11, color = Kx.text4))
                }
            }
            if (selDate != null && hasDay) key(sel, ym) {
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().chIn(durationMs = 250).clip(RoundedCornerShape(22.dp)).cssGradient(150f, Kx.accentDeep.copy(alpha = .16f), Kx.w(.03f))
                    .border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$sel ${Gen[ym.monthValue - 1]}, ${Wdl[selDate.dayOfWeek.value - 1]}", style = ts(15, W800), modifier = Modifier.weight(1f))
                        if (!live) Text("$selCount фото и видео", style = ts(12, W700, Kx.accent))
                    }
                    if (!live) Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${smp.first}  10:1${sel % 10}", style = ts(13), modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Kx.w(.08f)).padding(horizontal = 11.dp, vertical = 7.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text("${smp.second}  10:2${sel % 10}", style = ts(13, color = Kx.onAccent), modifier = Modifier.clip(RoundedCornerShape(14.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).padding(horizontal = 11.dp, vertical = 7.dp))
                        }
                    }
                    val key = "${ym.year}-${ym.monthValue}-$sel"
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep)
                        .kxClick { jumped = key; if (chatId != 0L) { if (store.historyChatId == chatId) ctx.back() else ctx.openChat(chatId); foundId?.takeIf { it > 0 }?.let { store.jumpTo(chatId, it) } } }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MoveDown, null, tint = Kx.onAccent)
                        Text(if (jumped == key) "Открываем…" else if (live) "Перейти к этому дню" else "Перейти к первому сообщению", style = ts(15, W800, Kx.onAccent))
                    }
                }
            } else key(sel, ym) {
                Text(if (selDate == null) (if (live) "Выберите день — подсвечены дни с сообщениями" else "Выберите день — подсвечены дни, когда вы переписывались") else if (live && foundId == null) "Ищем…" else if (selDate == today) "Сегодня сообщений ещё не было" else "В этот день вы не переписывались", style = ts(13, color = Kx.text4), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp).fillMaxWidth().chIn(durationMs = 250).clip(RoundedCornerShape(22.dp)).background(Kx.w(.03f)).dashedBorder(Kx.w(.12f), 22.dp).padding(18.dp))
            }
            // critique-A: quick jumps are one horizontal row (no orphan «Самое начало» on a second line).
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (live) {
                    Quick(Icons.Outlined.Today, "Сегодня") { go(today, today.dayOfMonth, 0) }
                    today.minusWeeks(1).let { d -> Quick(Icons.Outlined.History, "Неделю назад") { go(d, d.dayOfMonth, -1) } }
                    today.minusYears(1).let { d -> Quick(Icons.Outlined.EventRepeat, "Год назад") { go(d, d.dayOfMonth, -1) } }
                } else {
                Quick(Icons.Outlined.Today, "Сегодня") { go(LocalDate.of(2026, 11, 1), 22, 0) }
                Quick(Icons.Outlined.History, "Неделю назад") { go(LocalDate.of(2026, 11, 1), 15, 0) }
                Quick(Icons.Outlined.EventRepeat, "Год назад") { go(LocalDate.of(2025, 11, 1), 22, -1) }
                Quick(Icons.Outlined.FirstPage, "Самое начало") { go(LocalDate.of(2025, 9, 1), 3, -1) }
                }
            }
        }
    }
}

@Composable
private fun Quick(icon: ImageVector, label: String, tap: () -> Unit) {
    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(17.dp)).kxClick(label, onClick = tap).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = Kx.text2); Text(label, style = ts(13, W700, Kx.text2))
    }
}

