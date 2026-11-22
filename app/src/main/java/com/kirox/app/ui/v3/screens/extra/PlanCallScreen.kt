package com.kirox.app.ui.v3.screens.extra

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val PcWd = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
private val PcWdl = listOf("воскресенье", "понедельник", "вторник", "среду", "четверг", "пятницу", "субботу")
private val PcQuick = listOf("10:00" to 600, "13:00" to 780, "19:00" to 1140, "21:00" to 1260)
private fun p2(n: Int) = n.toString().padStart(2, '0')

/** Board «Запланировать звонок»: topic, day strip, time stepper + quick times, call / stream format → planned card with countdown. */
@Composable
fun PlanCallScreen(ctx: KxScreenCtx) {
    // demo: needs store (scheduled group call: createVideoChat(start_date, is_rtmp_stream), toggleVideoChatEnabledStartNotification); «now» = Sun 22 Nov 18:00 as on the board
    var title by remember { mutableStateOf("Разбор Compose 1.9") }
    var day by remember { mutableIntStateOf(1) }
    var min by remember { mutableIntStateOf(1140) }
    var kind by remember { mutableIntStateOf(0) }
    var planned by remember { mutableStateOf(false) }
    var remind by remember { mutableStateOf(true) }
    var left by remember { mutableIntStateOf(0) }
    var ticking by remember { mutableStateOf(false) }
    val toast = rememberExToast()
    LaunchedEffect(ticking) { while (ticking) { delay(1000); left = (left - 1).coerceAtLeast(0) } }
    fun dayName(d: Int) = when (d) { 0 -> "Сегодня"; 1 -> "Завтра"; else -> "В " + PcWdl[d % 7] }
    val timeText = p2(min / 60) + ":" + p2(min % 60)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) {
                    Text(if (planned) "Групповой звонок" else "Запланировать звонок", style = ts(20.sp, FontWeight.ExtraBold), maxLines = 1)
                    Text("Kotlin-клуб", style = ts(12.sp, color = Kx.text3))
                }
            }
            if (planned) key("planned") {
                Column(Modifier.weight(1f).exIn(300, dy = 8.dp).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF111614))
                        .ellipseBg(.9f, .8f, 0f, 1f, 0f to Color(0x592F6E9E), .6f to Color.Transparent)
                        .ellipseBg(1.2f, .9f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .45f), .6f to Color.Transparent)
                        .border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(28.dp)).padding(horizontal = 18.dp, vertical = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.EventAvailable, null, Modifier.size(18.dp), tint = Kx.accent)
                            Text("Звонок запланирован", style = ts(12.sp, FontWeight.Bold, Kx.accent))
                        }
                        Text(title, style = ts(22.sp, FontWeight.ExtraBold, lh = 26.4.sp), modifier = Modifier.padding(top = 10.dp))
                        val date = if (22 + day > 30) "${22 + day - 30} декабря" else "${22 + day} ноября"
                        Text("${dayName(day)}, $date в $timeText · " + if (kind == 1) "трансляция" else "звонок", style = ts(14.sp, color = Kx.text2), modifier = Modifier.padding(top = 4.dp))
                        val t = left.coerceAtLeast(0)
                        val parts = listOf(t / 86400 to "дн", t % 86400 / 3600 to "ч", t % 3600 / 60 to "мин", t % 60 to "сек")
                        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            parts.forEachIndexed { i, (v, l) ->
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .28f)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    val tick = if (i == 3) enter(300) else 1f
                                    Text(p2(v), style = ts(26.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.graphicsLayer {
                                        alpha = tick; translationY = -6.dp.toPx() * (1f - tick)
                                    })
                                    Text(l, style = ts(11.sp, color = Kx.text3))
                                }
                            }
                        }
                        Text("Напомнить себе: " + if (remind) "включено" else "выключено", style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 16.dp))
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bg by animateColorAsState(if (remind) Kx.accent.copy(alpha = .14f) else Kx.w(.06f), tween(200), label = "rb")
                        val bd by animateColorAsState(if (remind) Kx.accent.copy(alpha = .45f) else Kx.w(.1f), tween(200), label = "rd")
                        val ink = if (remind) Kx.accent else Kx.text
                        Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(20.dp)).kxClick(if (remind) "Напомню" else "Напомнить мне") { remind = !remind },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (remind) Icons.Rounded.NotificationsActive else Icons.Outlined.Notifications, null, Modifier.size(24.dp), tint = ink)
                            Text(if (remind) "Напомню" else "Напомнить мне", style = ts(14.sp, FontWeight.Bold, ink))
                        }
                        Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(20.dp))
                            .kxClick("Ссылка") { toast.show("Ссылка на звонок скопирована", Icons.Rounded.ContentCopy) },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Link, null, Modifier.size(24.dp), tint = Kx.text); Text("Ссылка", style = ts(14.sp, FontWeight.Bold))
                        }
                    }
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep)
                        .kxClick("Начать сейчас") { ticking = false; toast.show("Звонок начался", Icons.Rounded.Call) },
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { i ->
                                val w = kfLoop(900, i * 150, "wave$i")
                                Box(Modifier.size(3.dp, 18.dp).graphicsLayer { scaleY = .35f + .65f * tri(w) }.clip(RoundedCornerShape(2.dp)).background(Kx.onAccent))
                            }
                        }
                        Text("Начать сейчас", style = ts(16.sp, FontWeight.ExtraBold, Kx.onAccent))
                    }
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).kxClick("Изменить время или отменить") { ticking = false; planned = false },
                        contentAlignment = Alignment.Center) { Text("Изменить время или отменить", style = ts(14.sp, FontWeight.Bold, Kx.text3)) }
                }
            } else key("form") {
                Column(Modifier.weight(1f).exIn(300, dy = 8.dp).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp)) {
                    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Title, null, Modifier.size(24.dp), tint = Kx.text3)
                        ExField(title, { title = it }, "Тема звонка", Modifier.weight(1f), ts(16.sp))
                    }
                    PcLabel("День")
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { d ->
                            val on = day == d
                            Column(Modifier.width(50.dp).clip(RoundedCornerShape(18.dp)).then(if (on) Modifier.lin2(160f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)))
                                .kxClick("День $d") { day = d }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val ink = if (on) Kx.onAccent else Kx.text
                                Text(if (d == 0) "Сег" else PcWd[d % 7], style = ts(11.sp, FontWeight.SemiBold, ink.copy(alpha = .8f)))
                                Text("${if (22 + d > 30) 22 + d - 30 else 22 + d}", style = ts(18.sp, FontWeight.ExtraBold, ink), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                    PcLabel("Время")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                        ExRoundBtn(Icons.Outlined.Remove, "На час раньше", size = 40.dp, bg = Kx.w(.07f), border = null) { min = (min + 1410) % 1440 }
                        Text(timeText, style = ts(40.sp, FontWeight.ExtraBold, ls = 1.sp).copy(fontFeatureSettings = "tnum"), textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 130.dp))
                        ExRoundBtn(Icons.Outlined.Add, "На полчаса позже", size = 40.dp, bg = Kx.w(.07f), border = null) { min = (min + 30) % 1440 }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PcQuick.forEach { (label, m) ->
                            val bg by animateColorAsState(if (min == m) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "qb")
                            Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick(label) { min = m }, contentAlignment = Alignment.Center) {
                                Text(label, style = ts(13.sp, FontWeight.Bold, if (min == m) Kx.accent else Kx.text2))
                            }
                        }
                    }
                    PcLabel("Формат")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(Triple("Звонок", Icons.Outlined.Groups, 0), Triple("Трансляция", Icons.Outlined.Podcasts, 1)).forEach { (label, icon, i) ->
                            val bg by animateColorAsState(if (kind == i) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "kb")
                            val ink = if (kind == i) Kx.accent else Kx.text2
                            Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(label) { kind = i },
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, Modifier.size(20.dp), tint = ink); Text(label, style = ts(14.sp, FontWeight.Bold, ink))
                            }
                        }
                    }
                    Text(if (kind == 0) "Говорить может любой участник группы." else "Говорят только админы, остальные слушают и поднимают руку.",
                        style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
                }
            }
        }
        if (!planned) ExBottom {
            Row(Modifier.fillMaxWidth().height(54.dp).boxShadow(Kx.accentDeep.copy(alpha = .35f), 30.dp, 10.dp, 27.dp).clip(RoundedCornerShape(27.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep)
                .kxClick("Запланировать") {
                    if (title.isBlank()) { toast.show("Добавьте тему звонка", Icons.Rounded.Info); return@kxClick }
                    val l = day * 86400 + (min - 1080) * 60
                    if (l <= 0) { toast.show("Это время уже прошло", Icons.Rounded.Schedule); return@kxClick }
                    left = l; planned = true; ticking = true
                    toast.show("Участники получат уведомление", Icons.Rounded.NotificationsActive)
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, null, Modifier.size(24.dp), tint = Kx.onAccent)
                Text("Запланировать · ${dayName(day).lowercase()} в $timeText", style = ts(16.sp, FontWeight.ExtraBold, Kx.onAccent), maxLines = 1)
            }
        }
        ExToast(toast)
    }
}

@Composable
private fun PcLabel(text: String) = Text(text, style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
