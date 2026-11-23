package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CuSub(val name: String, val icon: ImageVector, val c: Color, val kind: String, val days: Int, val unread: Int, val chatId: Long = 0)
// demo: needs store — subscriptions with "last opened" days and unread counts
private val CU_SUBS = listOf(
    CuSub("Скидки и акции KZ", Icons.Rounded.Sell, Color(0xFFB07A2E), "Канал", 210, 2140), CuSub("Мемы про котов", Icons.Rounded.Pets, Color(0xFFC0443C), "Канал", 140, 860),
    CuSub("Крипто-сигналы 24/7", Icons.Rounded.CurrencyBitcoin, Color(0xFF5C6661), "Канал", 95, 3420), CuSub("Выпускники 2018", Icons.Rounded.School, Color(0xFF2F6E9E), "Группа", 64, 412),
    CuSub("Погода Алматы", Icons.Rounded.WbCloudy, Color(0xFF3F9A6C), "Канал", 40, 120), CuSub("Барахолка района", Icons.Rounded.Storefront, Color(0xFF6A4C9C), "Группа", 33, 980),
    CuSub("Android Weekly", Icons.Rounded.Campaign, Color(0xFF3F9A6C), "Канал", 1, 0), CuSub("Kotlin-клуб", Icons.Rounded.Code, Color(0xFF2F6E9E), "Группа", 0, 3))
private val CU_FILTERS = listOf("Не читаю месяц" to 30, "Не читаю неделю" to 7, "Все" to 0, "Только каналы" to -1)
/** Real store: Telegram does not tell when a chat was last opened — the filters go by unread messages instead. */
private val CU_FILTERS_REAL = listOf("100+ непрочитанных" to 100, "Есть непрочитанные" to 1, "Все" to 0, "Только каналы" to -1)
private val CU_INKS = listOf(Color(0xFFB07A2E), Color(0xFFC0443C), Color(0xFF5C6661), Color(0xFF2F6E9E), Color(0xFF3F9A6C), Color(0xFF6A4C9C))
private fun cuAgo(d: Int) = if (d == 0) "сегодня" else if (d == 1) "вчера" else if (d < 30) "$d дн. назад" else if (d < 365) "${d / 30} мес. назад" else "больше года"
private fun cuK(n: Int) = String.format(java.util.Locale.US, "%.1f", n / 1000f).replace('.', ',')
private fun cuUn(n: Int) = if (n == 0) "всё прочитано" else (if (n > 999) cuK(n) + " тыс." else "$n") + " непрочитанных"

/** Board «Чистка подписок»: idle hero with sweeping mop, filters, multi-select list, leave / archive with slide-out and undo toast. */
@Composable
fun CleanupScreen(ctx: KxScreenCtx) {
    // wave3: real store — channels and groups from store.chats with Telegram's unread counts; leave (deleteChat, after KxConfirm) /
    // archive (setArchived, undo). «Last opened» is not known to Telegram, so the real filters use unread counts.
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val subs = if (demo) CU_SUBS else store.chats.filter { it.kind == com.kirox.app.ui.v3.model.KxChatKind.Channel || it.kind == com.kirox.app.ui.v3.model.KxChatKind.Group }
        .mapIndexed { i, c -> CuSub(c.title, if (c.kind == com.kirox.app.ui.v3.model.KxChatKind.Channel) Icons.Rounded.Campaign else Icons.Rounded.Groups, CU_INKS[i % CU_INKS.size],
            if (c.kind == com.kirox.app.ui.v3.model.KxChatKind.Channel) "Канал" else "Группа", -1, c.unread, c.id) }
    val filters = if (demo) CU_FILTERS else CU_FILTERS_REAL
    var confirm by remember { mutableStateOf<com.kirox.app.ui.v3.components.KxConfirm?>(null) }
    var f by remember { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf(setOf<Int>()) }
    var gone by remember { mutableStateOf(setOf<Int>()) }
    var leaving by remember { mutableStateOf(setOf<Int>()) }
    val toast = remember { TToast() }
    val co = rememberCoroutineScope()
    val lim = filters[f].second
    // Keys: the board index in the demo, the chat id on the real account (the list reorders as messages arrive).
    val keyed = subs.mapIndexed { i, x -> (if (demo) i else x.chatId.hashCode()) to x }
    val list = keyed.map { IndexedValue(it.first, it.second) }.filter { (i, x) -> i !in gone && (if (lim == -1) x.kind == "Канал" else if (demo) x.days >= lim else x.unread >= lim) }
    val n = list.count { it.index in sel }
    val idleRule: (CuSub) -> Boolean = { x -> if (demo) x.days >= 30 else x.unread >= 100 }
    val idle = keyed.count { (i, x) -> i !in gone && idleRule(x) }
    val unreadIdle = keyed.sumOf { (i, x) -> if (i !in gone && idleRule(x)) x.unread else 0 }
    fun run(leave: Boolean) {
        val ids = sel.filter { it !in gone }
        if (ids.isEmpty()) return
        val chats = keyed.filter { it.first in ids }.map { it.second.chatId }
        leaving = ids.toSet()
        co.launch {
            delay(420)
            if (!demo) chats.forEachIndexed { k, id -> if (k > 0) delay(300); if (leave) store.deleteChat(id) else store.setArchived(id, true) } // spaced: no FLOOD_WAIT
            gone = gone + ids; leaving = emptySet(); sel = emptySet()
            if (leave && !demo) toast.show("Вы вышли: " + ids.size, Icons.AutoMirrored.Outlined.Logout)
            else toast.show((if (leave) "Вы отписались: " else "В архиве: ") + ids.size, if (leave) Icons.AutoMirrored.Outlined.Logout else Icons.Outlined.Archive, act = "Вернуть") {
                gone = gone - ids.toSet(); if (!demo && !leave) chats.forEach { store.setArchived(it, false) }
            }
        }
    }
    fun act(leave: Boolean) {
        val k = sel.count { it !in gone }
        if (k == 0) return
        if (leave && !demo) confirm = com.kirox.app.ui.v3.components.KxConfirm("Выйти из $k ${tPlural(k, "чата", "чатов", "чатов")}?",
            "Вы покинете выбранные каналы и группы. Вернуться можно будет только по ссылке или через поиск.", "Выйти") { run(true) }
        else run(leave)
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader("Чистка подписок", ctx.back, titleSize = 20)
            val hShape = RoundedCornerShape(24.dp)
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(hShape).background(Kx.w(.04f))
                .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .3f), .6f to Color.Transparent, center = Offset(size.width, 0f), radius = size.width)) }
                .border(1.dp, Kx.accent.copy(alpha = .22f), hShape).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val sw = tCycles(2400, 2, easing = TEaseInOut)
                val x = kf(sw, fa(0f, .5f, 1f), fa(-20f, 20f, -20f)); val r = kf(sw, fa(0f, .5f, 1f), fa(-20f, 20f, -20f))
                val moving = sw > 0f
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CleaningServices, null, Modifier.size(32.dp).graphicsLayer { translationX = if (moving) x * density else 0f; rotationZ = if (moving) r else 0f }, tint = Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (idle > 0) "$idle " + tPlural(idle, "подписку", "подписки", "подписок") + (if (demo) " вы не открывали месяц" else " с сотней непрочитанных") else "Все подписки в деле", style = KxType.style(17.sp, FontWeight.ExtraBold))
                    Text(if (idle > 0) "В них " + (if (unreadIdle > 999) cuK(unreadIdle) + " тыс." else "$unreadIdle") + " непрочитанных сообщений" else "Нечего чистить — отлично!",
                        style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEachIndexed { i, (l, _) -> TChip(l, f == i, h = 34.dp) { f = i } }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 110.dp)) {
                // critique-B: select all / clear.
                val ids = list.map { it.index }
                if (ids.isNotEmpty()) {
                    val allOn = ids.all { it in sel }
                    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).kxClick(role = Role.Checkbox) { sel = if (allOn) sel - ids.toSet() else sel + ids }
                        .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (allOn) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, Modifier.size(24.dp), tint = if (allOn) Kx.accent else Kx.text3)
                        Text(if (allOn) "Снять выбор" else "Выбрать все · ${ids.size}", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
                    }
                }
                list.forEachIndexed { k, (i, x) ->
                    key(i) {
                        val on = i in sel
                        val out = remember { Animatable(0f) }
                        LaunchedEffect(i in leaving) { if (i in leaving) out.animateTo(1f, tween(420, easing = TEase)) }
                        val o = out.value
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(200), label = "bg")
                        Row(Modifier.tRise(k * 30, 250, 8.dp).graphicsLayer { alpha = (1f - o / .6f).coerceAtLeast(0f); translationX = 60.dp.toPx() * (o / .6f).coerceAtMost(1f) }
                            .padding(bottom = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).kxClick(role = Role.Checkbox) { sel = if (on) sel - i else sel + i }
                            .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val ring by animateColorAsState(if (on) Kx.accent else Kx.w(.3f), tween(200), label = "r")
                            Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
                                if (on) {
                                    val ck = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) { ck.animateTo(1f, tween(250, easing = TEase)) }
                                    val s = kf(ck.value, fa(0f, .6f, 1f), fa(0f, 1.3f, 1f))
                                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp).graphicsLayer { scaleX = s; scaleY = s }, tint = Kx.onAccent)
                                }
                            }
                            Box(Modifier.size(44.dp).clip(CircleShape).background(x.c), contentAlignment = Alignment.Center) { Icon(x.icon, null, Modifier.size(22.dp), tint = Color.White) }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KxLine(x.name, KxType.style(15.sp, FontWeight.Bold), Modifier.weight(1f))
                                    Text(x.kind, style = KxType.style(11.sp, color = Kx.text4))
                                }
                                val lastInk = Kx.text4 // critique-B: neutral (every row was orange)
                                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (x.days >= 0) {
                                        Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = lastInk)
                                        Text("открывали " + cuAgo(x.days), style = KxType.style(12.sp, color = lastInk))
                                        Text("· " + cuUn(x.unread), style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
                                    } else Text(cuUn(x.unread), style = KxType.style(12.sp, color = lastInk), maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().tRise(0, 300, 8.dp).padding(start = 30.dp, end = 30.dp, top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(56.dp), tint = Kx.accent)
                    Text("Чисто!", style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                    Text("Здесь пусто — все подписки вы читаете", style = KxType.style(13.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg)).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TPill("В архив", Modifier.alpha(if (n > 0) 1f else .4f), Icons.Outlined.Archive, 54.dp, solid(Kx.w(.07f)), Kx.text, 14, FontWeight.Bold) { act(false) }
            val lbg by animateColorAsState(if (n > 0) Kx.dangerSolid else Kx.w(.07f), tween(250), label = "lb")
            TPill(if (n > 0) "Выйти · $n" else "Выберите подписки", Modifier.weight(1f), Icons.AutoMirrored.Outlined.Logout, 54.dp, solid(lbg), if (n > 0) Color.White else Kx.text3) { act(true) }
        }
        TToastHost(toast, 96.dp, 3000)
        confirm?.let { com.kirox.app.ui.v3.components.KxConfirmDialog(it) { confirm = null } }
    }
}
