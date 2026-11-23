package com.kirox.app.ui.v3.screens.extra

import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class RmItem(val id: Int, val title: String, val from: String, val c: Color, val quote: String, val day: Int, val time: String, val rep: String, val done: Boolean)

// demo list. Real store (wave3): reminders are Telegram's own — scheduled messages in Saved Messages (store.scheduled / sendScheduled /
// deleteScheduled), so they ring on every device. Snooze and repeat stay demo (no rescheduling call in the store).
private val RmSeed = listOf(
    RmItem(1, "Скинуть Анне фото с дачи", "Анна", Color(0xFFC0443C), "Пришлёшь фотки, когда вернёшься?", 0, "19:00", "", false),
    RmItem(2, "Оплатить интернет", "Избранное", Kx.accent, "Счёт до 25-го, лицевой 4471…", 0, "21:30", "Каждый месяц", false),
    RmItem(3, "Посмотреть лекцию 8", "Kotlin-клуб", Color(0xFF6FA9D8), "Запись лекции про корутины здесь", 1, "10:00", "", false),
    RmItem(4, "Ответить Дмитрию про митап", "Дмитрий", Color(0xFF2F6E9E), "Ты с нами в субботу?", 1, "12:00", "", false),
    RmItem(5, "Позвонить в сервис", "Сервис-центр", Color(0xFFB07A2E), "Ваш ноутбук готов к выдаче", -1, "15:00", "", true),
)
private class RmWhen(val label: String, val sub: String, val off: ImageVector, val on: ImageVector, val day: Int, val time: String)
private val RmWhens = listOf(
    RmWhen("Через час", "20:15", Icons.Outlined.Schedule, Icons.Rounded.Schedule, 0, "20:15"),
    RmWhen("Вечером", "Сегодня в 21:00", Icons.Outlined.NightsStay, Icons.Rounded.NightsStay, 0, "21:00"),
    RmWhen("Завтра утром", "В 9:00", Icons.Outlined.WbSunny, Icons.Rounded.WbSunny, 1, "09:00"),
    RmWhen("В выходные", "Суббота в 10:00", Icons.Outlined.Weekend, Icons.Rounded.Weekend, 2, "10:00"),
)
private val RmReps = listOf("Один раз", "Каждый день", "Каждую неделю")
private fun rmDay(d: Int) = when (d) { 0 -> "Сегодня"; 1 -> "Завтра"; 2 -> "В субботу"; else -> "Вчера" }

/** Board «Напоминания»: next-up card with the bell, active / done tabs, reminder cards (done toggle, snooze +1h), new reminder sheet. */
@Composable
fun RemindScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    // critique-B: one reminders feature — on a real account this board is «Напоминания» of Settings (KxReminders).
    if (!demo) { com.kirox.app.ui.v3.features.RemindersScreen(ctx); return }
    val list = remember { mutableStateListOf<RmItem>().apply { addAll(RmSeed) } }
    var tab by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    var w by remember { mutableIntStateOf(2) }
    var rep by remember { mutableIntStateOf(0) }
    var pop by remember { mutableIntStateOf(0) }
    var popN by remember { mutableIntStateOf(0) }
    var next by remember { mutableIntStateOf(10) }
    val toast = rememberExToast()
    val active = list.filter { !it.done }.sortedWith(compareBy<RmItem> { it.day }.thenBy { it.time })
    val done = list.filter { it.done }
    val shown = if (tab == 0) active else done
    val nx = active.firstOrNull()
    fun upd(id: Int, f: (RmItem) -> RmItem) { val i = list.indexOfFirst { it.id == id }; if (i >= 0) list[i] = f(list[i]) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Напоминания", { ctx.back() }) {
                Box(Modifier.size(44.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Новое напоминание") { sheet = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddAlarm, null, Modifier.size(24.dp), tint = Kx.onAccent)
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).lin2(150f, Kx.accentDeep.copy(alpha = .24f), Kx.w(.03f))
                .border(1.dp, Kx.accent.copy(alpha = .24f), RoundedCornerShape(24.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val bell = kfRun(2400)
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(30.dp).graphicsLayer {
                        transformOrigin = TransformOrigin(.5f, .1f)
                        rotationZ = if (bell < 0f) 0f else kfLerp(bell, floatArrayOf(0f, .1f, .2f, .3f, .4f, .5f, 1f), floatArrayOf(0f, 14f, -12f, 9f, -6f, 0f, 0f))
                    }, tint = Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (nx != null) "Следующее · ${rmDay(nx.day).lowercase()} в ${nx.time}" else "Всё сделано", style = ts(12.sp, FontWeight.Bold, Kx.accent))
                    Text(nx?.title ?: "Напоминаний нет", style = ts(16.sp, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    Text(if (nx != null) "Из чата «${nx.from}»" else "Можно отдохнуть 🙂", style = ts(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Активные · ${active.size}", "Выполненные · ${done.size}").forEachIndexed { i, label ->
                    val bg by animateColorAsState(if (tab == i) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "tb")
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick(label) { tab = i }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = ts(13.sp, FontWeight.Bold, if (tab == i) Kx.accent else Kx.text2))
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                var k = 0
                key(tab) {
                    shown.map { it.day }.distinct().forEach { d ->
                        shown.filter { it.day == d }.forEachIndexed { j, r ->
                            k++
                            if (j == 0 && (tab == 0 || k == 1)) Text(if (tab == 1) "Выполнено" else rmDay(d), style = ts(13.sp, FontWeight.ExtraBold, Kx.accent),
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp))
                            key(r.id) { RmCard(r, k - 1, if (pop == r.id) popN else 0, {
                                upd(r.id) { it.copy(done = !it.done) }; pop = r.id; popN++
                            }) {
                                val h = (r.time.take(2).toInt() + 1).coerceAtMost(23)
                                upd(r.id) { it.copy(time = h.toString().padStart(2, '0') + it.time.drop(2)) }
                                toast.show("Отложено на час", Icons.Rounded.Snooze)
                            } }
                        }
                    }
                }
                if (shown.isEmpty()) key(tab) {
                    Column(Modifier.fillMaxWidth().exIn(300, dy = 8.dp).padding(start = 30.dp, end = 30.dp, top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AlarmOff, null, Modifier.size(54.dp), tint = Kx.dashed)
                        Text(if (tab == 0) "Активных напоминаний нет" else "Пока ничего не выполнено", style = ts(16.sp, FontWeight.Bold), modifier = Modifier.padding(top = 10.dp), textAlign = TextAlign.Center)
                        Text("Нажмите на сообщение и выберите «Напомнить»", style = ts(13.sp, color = Kx.text4), modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (sheet) ExGlassSheet({ sheet = false }) {
            Text("Напомнить о сообщении", style = ts(19.sp, FontWeight.ExtraBold))
            RmQuote("Мама", Kx.warning, "Не забудь записать бабушку к врачу на четверг", Modifier.padding(top = 10.dp), r = 12, padV = 8, padH = 12, bg = .05f, ellipsis = false)
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RmWhens.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { col, it ->
                            val i = row * 2 + col; val on = w == i
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "wb")
                            val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "wd")
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(it.label) { w = i }.padding(12.dp)) {
                                Icon(if (on) it.on else it.off, null, Modifier.size(24.dp), tint = if (on) Kx.accent else Kx.text3)
                                Text(it.label, style = ts(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
                                Text(it.sub, style = ts(12.sp, color = Kx.text4))
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RmReps.forEachIndexed { i, label ->
                    val bg by animateColorAsState(if (rep == i) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "rb")
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(label) { rep = i }, contentAlignment = Alignment.Center) {
                        Text(label, style = ts(13.sp, FontWeight.Bold, if (rep == i) Kx.accent else Kx.text2), maxLines = 1)
                    }
                }
            }
            val wh = RmWhens[w]
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Напомнить") {
                list.add(0, RmItem(next, "Записать бабушку к врачу", "Мама", Kx.warning, "Не забудь записать бабушку к врачу на четверг", wh.day, wh.time, if (rep > 0) RmReps[rep] else "", false))
                next++; sheet = false; tab = 0
                toast.show("Напомню " + (when (wh.day) { 0 -> "сегодня"; 1 -> "завтра"; else -> "в субботу" }) + " в " + wh.time, Icons.Rounded.AlarmOn)
            }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AlarmAdd, null, Modifier.size(24.dp), tint = Kx.onAccent)
                Text("Напомнить · " + wh.label.lowercase(), style = ts(16.sp, FontWeight.ExtraBold, Kx.onAccent))
            }
        }
        ExToast(toast)
    }
}

/** Piecewise-linear keyframes: [at] offsets 0..1 → [v] values. */
internal fun kfLerp(t: Float, at: FloatArray, v: FloatArray): Float {
    for (i in 1 until at.size) if (t <= at[i]) {
        val f = ((t - at[i - 1]) / (at[i] - at[i - 1])).coerceIn(0f, 1f)
        return v[i - 1] + (v[i] - v[i - 1]) * EaseInOut.transform(f)
    }
    return v.last()
}

@Composable
private fun RmQuote(from: String, c: Color, quote: String, modifier: Modifier = Modifier, r: Int = 10, padV: Int = 7, padH: Int = 10, bg: Float = .04f, ellipsis: Boolean = true) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(r.dp)).background(Kx.w(bg)).drawBehind { drawRect(c, size = Size(3.dp.toPx(), size.height)) }
        .padding(start = (padH + 3).dp, end = padH.dp, top = padV.dp, bottom = padV.dp)) {
        Text(from, style = ts(12.sp, FontWeight.Bold, c))
        Text(quote, style = ts(13.sp, color = Kx.text2), maxLines = if (ellipsis) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RmCard(r: RmItem, k: Int, pop: Int, onToggle: () -> Unit, onSnooze: () -> Unit) {
    val e = enter(250, k * 40)
    val op by animateFloatAsState(if (r.done) .6f else 1f, tween(300), label = "op")
    Row(Modifier.padding(bottom = 8.dp).graphicsLayer { alpha = e * op; translationY = (1f - e) * 8.dp.toPx() }.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f))
        .border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val ring by animateColorAsState(if (r.done) Kx.accent else Kx.w(.3f), tween(200), label = "rg")
        val fill by animateColorAsState(if (r.done) Kx.accent else Color.Transparent, tween(200), label = "fl")
        key(pop) {
            val p = if (pop > 0) enter(300) else 1f
            Box(Modifier.padding(top = 2.dp).size(28.dp).graphicsLayer { val s = 1f + .3f * tri(if (p >= 1f) 0f else p); scaleX = s; scaleY = s }
                .clip(CircleShape).background(fill).border(2.dp, ring, CircleShape).kxClick(if (r.done) "Вернуть напоминание" else "Отметить выполненным", onClick = onToggle),
                contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp).graphicsLayer { alpha = if (r.done) 1f else 0f }, tint = Kx.onAccent) }
        }
        Column(Modifier.weight(1f)) {
            Text(r.title, style = ts(15.sp, FontWeight.Bold).copy(textDecoration = if (r.done) TextDecoration.LineThrough else null))
            RmQuote(r.from, r.c, r.quote, Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val hot = r.day == 0 && !r.done
                Row(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(if (hot) Kx.accent.copy(alpha = .14f) else Kx.w(.05f)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val ink = if (hot) Kx.accent else Kx.text3
                    Icon(if (r.day == 0) Icons.Outlined.Alarm else Icons.Outlined.Event, null, Modifier.size(15.dp), tint = ink)
                    Text("${rmDay(r.day)}, ${r.time}", style = ts(12.sp, FontWeight.Bold, ink), maxLines = 1, softWrap = false)
                }
                if (r.rep.isNotEmpty()) Row(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(Kx.w(.05f)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Repeat, null, Modifier.size(15.dp), tint = Kx.text3); Text(r.rep, style = ts(12.sp, color = Kx.text3), maxLines = 1, softWrap = false)
                }
            }
        }
        if (!r.done) Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Отложить на час", onClick = onSnooze), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Snooze, null, Modifier.size(19.dp), tint = Kx.text2)
        }
    }
}


/** wave3: reminders on the real account = scheduled messages in Saved Messages. */
@Composable
private fun RemindReal(ctx: KxScreenCtx) {
    val store = ctx.store
    val savedId = store.chats.firstOrNull { it.kind == com.kirox.app.ui.v3.model.KxChatKind.Saved }?.id ?: store.me.id
    var items by remember { mutableStateOf<List<com.kirox.app.ui.v3.model.KxScheduled>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    var w by remember { mutableIntStateOf(2) }
    var text by remember { mutableStateOf("") }
    val co = androidx.compose.runtime.rememberCoroutineScope()
    val toast = rememberExToast()
    LaunchedEffect(reload, savedId) { if (savedId != 0L) items = store.scheduled(savedId) }
    fun whenAt(i: Int): Long {
        val c = java.util.Calendar.getInstance()
        when (i) {
            0 -> c.add(java.util.Calendar.HOUR_OF_DAY, 1)
            1 -> { c.set(java.util.Calendar.HOUR_OF_DAY, 21); c.set(java.util.Calendar.MINUTE, 0); if (c.timeInMillis < System.currentTimeMillis() + 60_000) c.add(java.util.Calendar.DAY_OF_YEAR, 1) }
            2 -> { c.add(java.util.Calendar.DAY_OF_YEAR, 1); c.set(java.util.Calendar.HOUR_OF_DAY, 9); c.set(java.util.Calendar.MINUTE, 0) }
            else -> { do c.add(java.util.Calendar.DAY_OF_YEAR, 1) while (c.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY); c.set(java.util.Calendar.HOUR_OF_DAY, 10); c.set(java.util.Calendar.MINUTE, 0) }
        }
        c.set(java.util.Calendar.SECOND, 0)
        return c.timeInMillis
    }
    val fmt = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("ru"))
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Напоминания", { ctx.back() }) {
                Box(Modifier.size(44.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Новое напоминание") { sheet = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddAlarm, null, Modifier.size(24.dp), tint = Kx.onAccent)
                }
            }
            Text("Напоминание — это сообщение в «Избранном», которое Telegram отправит в выбранное время. Оно придёт на все ваши устройства.",
                style = ts(13.sp, color = Kx.text3, lh = 18.sp), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 30.dp)) {
                val list = items
                if (list == null) Text("Загружаем…", style = ts(14.sp, color = Kx.text3), modifier = Modifier.padding(8.dp))
                else if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AlarmOff, null, Modifier.size(54.dp), tint = Kx.dashed)
                    Text("Активных напоминаний нет", style = ts(16.sp, FontWeight.Bold), modifier = Modifier.padding(top = 10.dp))
                }
                list.orEmpty().forEach { r ->
                    Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Alarm, null, Modifier.size(24.dp), tint = Kx.accent)
                        Column(Modifier.weight(1f)) {
                            Text(r.text.ifBlank { "Сообщение" }, style = ts(15.sp, FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(r.time, style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                        }
                        ExPill("Готово", height = 34.dp, size = 12.sp, padH = 12.dp) { store.deleteScheduled(savedId, r.id); items = items.orEmpty().filter { it.id != r.id }; toast.show("Напоминание снято", Icons.Rounded.CheckCircle) }
                    }
                }
            }
        }
        if (sheet) ExGlassSheet({ sheet = false }) {
            Text("Новое напоминание", style = ts(19.sp, FontWeight.ExtraBold))
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(14.dp)) {
                if (text.isEmpty()) Text("О чём напомнить?", style = ts(15.sp, color = Kx.text4))
                androidx.compose.foundation.text.BasicTextField(text, { text = it.take(4000) }, Modifier.fillMaxWidth(), textStyle = ts(15.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent))
            }
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RmWhens.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { col, it ->
                            val i = row * 2 + col; val on = w == i
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f))
                                .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(18.dp)).kxClick(it.label) { w = i }.padding(12.dp)) {
                                Icon(if (on) it.on else it.off, null, Modifier.size(24.dp), tint = if (on) Kx.accent else Kx.text3)
                                Text(it.label, style = ts(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
                                Text(fmt.format(java.util.Date(whenAt(i))), style = ts(12.sp, color = Kx.text4))
                            }
                        }
                    }
                }
            }
            val ok = text.isNotBlank()
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).then(if (ok) Modifier.lin2(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                .kxClick("Напомнить") {
                    if (!ok) return@kxClick
                    val at = whenAt(w)
                    store.sendScheduled(savedId, text.trim(), (at / 1000).toInt())
                    text = ""; sheet = false
                    toast.show("Напомню " + fmt.format(java.util.Date(at)), Icons.Rounded.AlarmOn)
                    co.launch { kotlinx.coroutines.delay(800); reload++ }
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AlarmAdd, null, Modifier.size(24.dp), tint = if (ok) Kx.onAccent else Kx.text4)
                Text("Напомнить · " + RmWhens[w].label.lowercase(), style = ts(16.sp, FontWeight.ExtraBold, if (ok) Kx.onAccent else Kx.text4))
            }
        }
        ExToast(toast)
    }
}
