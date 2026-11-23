package com.kirox.app.ui.v3.screens.chat

import com.kirox.app.ui.v3.components.kxHitClick
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class AgEv(val id: Int, val day: Int, val time: String, val dur: String, val title: String, val place: String, val icon: ImageVector,
                   val chat: String, val l: String, val c: Color, val quote: String, val now: Boolean = false)

// demo: needs store (events detected in chats: date/time/place/quote/source message) — real accounts show none until that exists
private val AgEvents = listOf(
    AgEv(1, 0, "19:00", "2 ч", "Кино · Анна", "Кинотеатр «Арман»", Icons.Outlined.Movie, "Анна", "А", hex("#C0443C"), "Давай в 7 на новый фильм?", now = true),
    AgEv(2, 1, "11:00", "1 ч", "Созвон по проекту", "Видеозвонок в «Работа»", Icons.Outlined.Videocam, "Работа", "Р", hex("#5C6661"), "Созвон в понедельник в 11, не забудьте"),
    AgEv(3, 3, "10:30", "30 мин", "Бабушка к врачу", "Поликлиника №4", Icons.Outlined.LocalHospital, "Мама", "М", hex("#B07A2E"), "Записала бабушку на 25-е в 10:30"),
    AgEv(4, 6, "11:00", "4 ч", "Митап Kotlin-клуба", "Абая 52, вход со двора", Icons.Outlined.LocationOn, "Kotlin-клуб", "K", Kx.accentDeep, "Начало в 11:00. Алекс ведёт второй доклад"),
    AgEv(5, 6, "18:00", "3 ч", "Шашлыки на даче", "Дача", Icons.Outlined.OutdoorGrill, "Семья", "С", hex("#2F6E9E"), "В субботу вечером все на дачу!"),
)
private val AgDays = listOf("Вс" to 22, "Пн" to 23, "Вт" to 24, "Ср" to 25, "Чт" to 26, "Пт" to 27, "Сб" to 28)
private val AgLong = listOf("Сегодня, 22 ноября", "Понедельник, 23 ноября", "Вторник, 24 ноября", "Среда, 25 ноября", "Четверг, 26 ноября", "Пятница, 27 ноября", "Суббота, 28 ноября")

/** Board «События из чатов»: week strip with dots, events found in chats (quote, add to calendar, open chat, not-an-event). */
@Composable
fun AgendaScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var day by remember { mutableIntStateOf(0) }
    val added = remember { mutableStateListOf<Int>() }
    val hidden = remember { mutableStateListOf<Int>() }
    var pop by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<Pair<String, ImageVector>?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { delay(2400); toast = null } }
    fun say(t: String, i: ImageVector) { toast = t to i; tk++ }
    val vis = if (demo) AgEvents.filter { it.id !in hidden } else emptyList()
    val list = vis.filter { it.day == day }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) { Text("События из чатов", style = ts(20, W800)); Text("kiroX нашёл договорённости в переписках", style = ts(12, color = Kx.text3)) }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AgDays.forEachIndexed { i, (wd, n) ->
                    val on = day == i; val has = vis.any { it.day == i }
                    val ink = if (on) Kx.onAccent else Kx.text
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).then(if (on) Modifier.cssGradient(160f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.04f)))
                        .border(1.dp, if (on) Color.Transparent else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick("$wd $n") { day = i }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (i == 0) "Сег" else wd, style = ts(11, color = ink), modifier = Modifier.graphicsLayer { alpha = .8f })
                        Text("$n", style = ts(17, W800, ink))
                        Box(Modifier.padding(top = 3.dp).height(6.dp), contentAlignment = Alignment.Center) {
                            if (has) Box(Modifier.size(5.dp).clip(CircleShape).background(if (on) Kx.onAccent else Kx.accent))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 30.dp)) {
                Text(AgLong[day] + if (list.isNotEmpty()) " · ${list.size}" else "", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
                list.forEachIndexed { k, e ->
                    key(day, e.id) {
                        val isAdded = e.id in added
                        Row(Modifier.padding(bottom = 10.dp).chIn(k * 60, 0.dp, 300, dx = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.width(50.dp).padding(top = 12.dp), horizontalAlignment = Alignment.End) {
                                Text(e.time, style = ts(15, W800)); Text(e.dur, style = ts(11, color = Kx.text4))
                            }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (e.now) Kx.accent.copy(alpha = .08f) else Kx.w(.04f))
                                .border(1.dp, if (e.now) Kx.accent.copy(alpha = .3f) else Kx.w(.08f), RoundedCornerShape(20.dp))
                                .drawBehind { drawRect(e.c, Offset.Zero, Size(4.dp.toPx(), size.height)) }
                                .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
                                Column {
                                    Text(e.title, style = ts(15, W800), modifier = Modifier.padding(end = 50.dp))
                                    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(e.icon, null, Modifier.size(14.dp), tint = Kx.text3); Text(e.place, style = ts(12, color = Kx.text3))
                                    }
                                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LetterAv(e.l, e.c, 18.dp, 11)
                                        Text("«${e.quote}»", style = ts(12, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (isAdded) Kx.accent.copy(alpha = .16f) else Kx.w(.06f)).kxClick {
                                            if (!isAdded) { added += e.id; pop = e.id; say("Добавлено: ${e.title}", Icons.Rounded.EventAvailable) }
                                        }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            key(isAdded) { Icon(if (isAdded) Icons.Outlined.EventAvailable else Icons.Outlined.EditCalendar, null,
                                                Modifier.size(15.dp).then(if (pop == e.id && isAdded) Modifier.checkPop() else Modifier), tint = if (isAdded) Kx.accent else Kx.text) }
                                            Text(if (isAdded) "В календаре" else "В календарь", style = ts(12, W700, if (isAdded) Kx.accent else Kx.text))
                                        }
                                        Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.06f)).kxClick { say("Откроется сообщение в «${e.chat}»", Icons.AutoMirrored.Rounded.Chat) }
                                            .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("В чат", style = ts(12, W700, Kx.text2)) }
                                        Spacer(Modifier.weight(1f))
                                        Box(Modifier.kxHitClick("Это не событие") { hidden += e.id; say("Больше не покажем", Icons.Rounded.VisibilityOff) }.size(30.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text4)
                                        }
                                    }
                                }
                                if (e.now) NowTag(Modifier.align(Alignment.TopEnd))
                            }
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EventBusy, null, Modifier.size(50.dp), tint = Kx.dashed)
                    Text("В этот день ничего", style = ts(15, W700, Kx.text2), modifier = Modifier.padding(top = 8.dp))
                    Text("Свободный день — тоже план 🙂", style = ts(12, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        val t = toast
        if (t != null) key(tk) { MixToast(t.first, t.second, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), 30) }
    }
}

/** «СКОРО» tag with kxNow (fade to .4, 1.4s, twice). */
@Composable
private fun NowTag(modifier: Modifier) {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(1400, easing = LinearEasing)) } }
    val a = 1f - .6f * kotlin.math.sin(Math.PI * p.value).toFloat()
    Box(modifier.graphicsLayer { alpha = a }.clip(RoundedCornerShape(8.dp)).background(Kx.accent).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("СКОРО", style = ts(11, androidx.compose.ui.text.font.FontWeight.Black, Kx.onAccent))
    }
}

/** kxCheck: 0 → 1.25 → 1 in .3s. */
@Composable
private fun Modifier.checkPop(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(300)) }
    return graphicsLayer { val v = p.value; val s = if (v < .6f) 1.25f * v / .6f else 1.25f - .25f * (v - .6f) / .4f; scaleX = s; scaleY = s }
}
