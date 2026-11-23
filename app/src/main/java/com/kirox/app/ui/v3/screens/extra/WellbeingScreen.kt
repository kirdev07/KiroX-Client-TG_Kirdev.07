package com.kirox.app.ui.v3.screens.extra

import com.kirox.app.ui.v3.model.togglePref
import com.kirox.app.ui.v3.model.prefOn
import com.kirox.app.ui.v3.model.pref
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val WbMinutes = listOf(82, 64, 95, 51, 118, 74, 47)
/** critique-B: the 7 bars are the last 7 days ending today, named from the real date (was a fixed Пн…Вс with «Вс» as today). */
private val WbDays: List<Triple<String, Int, String>> get() = (6 downTo 0).mapIndexed { i, back ->
    val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -back) }
    val ru = java.util.Locale("ru")
    Triple(java.text.SimpleDateFormat("EE", ru).format(c.time).replaceFirstChar { it.uppercase() }.take(2), WbMinutes[i],
        if (back == 0) "сегодня" else if (back == 1) "вчера" else java.text.SimpleDateFormat("EEEE", ru).format(c.time))
}
private class WbSplit(val name: String, val l: String, val c: Color, val f: Float)
private val WbSplits = listOf(WbSplit("Kotlin-клуб", "K", Kx.accentDeep, .34f), WbSplit("Мама", "М", Color(0xFFB07A2E), .22f), WbSplit("Дмитрий", "Д", Color(0xFF2F6E9E), .26f), WbSplit("Анна", "А", Color(0xFFC0443C), .18f))
private val WbLimits = listOf("Нет" to 0, "45 мин" to 45, "1 час" to 60, "1,5 ч" to 90)
private fun hm(m: Int) = if (m >= 60) (if (m % 60 == 0) "${m / 60} ч" else "${m / 60} ч ${m % 60} мин") else "$m мин" // «1 ч», not «1 ч 0 мин»

/** Board «Время в kiroX»: today card (leaf), 7 day bars growing in with the dashed limit line, time per chat, daily limit chips, two switches. */
@Composable
fun WellbeingScreen(ctx: KxScreenCtx) {
    // wave3: the real store counts minutes kiroX is on screen, per day, on this phone (KxPrefsAccount.USAGE_DAYS); no per-chat split.
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { WellbeingReal(ctx); return }
    // demo: needs store (local usage tracking per day/chat; limit + break reminder + quiet after 23:00 prefs)
    var day by remember { mutableIntStateOf(6) }
    var limit by remember { mutableIntStateOf(60) }
    var breakR by remember { mutableStateOf(true) }
    var night by remember { mutableStateOf(false) }
    val max = 130f
    val days = remember { WbDays }
    val d = days[day]
    val avg = Math.round(days.sumOf { it.second } / 7.0).toInt()
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Время в kiroX", { ctx.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f)).ellipseBg(1f, .9f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .28f), .6f to Color.Transparent)
                    .border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(24.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(if (day == 6) "Сегодня" else d.third.replaceFirstChar { it.uppercase() }, style = ts(12.sp, color = Kx.text3))
                            Text(hm(d.second), style = ts(34.sp, FontWeight.ExtraBold, ls = (-1).sp), modifier = Modifier.padding(top = 2.dp))
                        }
                        val lf = kfRun(3000)
                        Icon(Icons.Rounded.Eco, null, Modifier.size(40.dp).graphicsLayer {
                            transformOrigin = TransformOrigin(.5f, 1f); rotationZ = if (lf < 0f) 0f else kfLerp(lf, floatArrayOf(0f, .5f, 1f), floatArrayOf(-8f, 8f, -8f))
                        }, tint = Kx.accent)
                    }
                    val less = d.second < avg
                    Text(if (less) "На ${avg - d.second} мин меньше, чем обычно 🌿" else "На ${d.second - avg} мин больше, чем обычно", style = ts(13.sp, color = if (less) Kx.accent else Kx.warning),
                        modifier = Modifier.padding(top = 4.dp))
                    Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(130.dp)) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            days.forEachIndexed { i, (wd, m, name) ->
                                val over = limit > 0 && m > limit; val on = day == i
                                val c by animateColorAsState(if (on) (if (over) Kx.warning else Kx.accent) else if (over) Kx.warning.copy(alpha = .62f) else Kx.accent.copy(alpha = .35f), tween(200), label = "bc")
                                val g = enter(600, i * 50, KxMotion.rise)
                                Column(Modifier.weight(1f).fillMaxHeight().kxClick("$name: ${hm(m)}") { day = i }, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.fillMaxWidth().height((m / max * 106).dp).graphicsLayer { transformOrigin = TransformOrigin(.5f, 1f); scaleY = g }
                                        .clip(RoundedCornerShape(4.dp, 4.dp, 2.dp, 2.dp)).background(c))
                                    Text(wd, style = ts(11.sp, if (on) FontWeight.ExtraBold else FontWeight.Medium, if (on) Kx.text else Kx.text4))
                                }
                            }
                        }
                        if (limit > 0) {
                            val y = (limit / max * 106 + 18).dp
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().offset(y = -y).height(1.5.dp).drawBehind {
                                drawLine(Kx.warning.copy(alpha = .7f), Offset(0f, 0f), Offset(size.width, 0f), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                            })
                            Text("лимит ${hm(limit)}", style = ts(11.sp, color = Kx.warning), modifier = Modifier.align(Alignment.BottomEnd).offset(y = -y).padding(bottom = 2.dp))
                        }
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (limit > 0) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Kx.warning)); Text("выше лимита", style = ts(11.sp, color = Kx.text3)) }
                        Text((if (limit > 0) "· " else "") + "нажмите на день, чтобы посмотреть подробнее", style = ts(11.sp, color = Kx.text4))
                    }
                }
                Text("Где проводили время · ${d.third}", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // The rows add up to the day total (the last row takes the rounding rest).
                    val parts = WbSplits.dropLast(1).map { Math.round(d.second * it.f) }.let { it + (d.second - it.sum()) }
                    WbSplits.forEachIndexed { wi, w ->
                        Row(Modifier.exIn(250, dy = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExLetter(w.l, 34.dp, w.c, font = 13.sp, ink = Kx.text)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(w.name, style = ts(14.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                                    Text(hm(parts[wi]), style = ts(13.sp, color = Kx.text2).copy(fontFeatureSettings = "tnum"))
                                }
                                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.06f))) {
                                    Box(Modifier.fillMaxWidth(w.f / .34f).fillMaxHeight().clip(RoundedCornerShape(0.dp, 3.dp, 3.dp, 0.dp)).background(Kx.accent))
                                }
                            }
                        }
                    }
                }
                Text("Дневной лимит", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WbLimits.forEach { (label, v) ->
                        val on = limit == v
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(14.dp)).kxClick(label) { limit = v }, contentAlignment = Alignment.Center) {
                            Text(label, style = ts(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    ExSwitchRow(Icons.Outlined.SelfImprovement, "Напоминать о перерыве", "Мягкое уведомление после 30 минут подряд", breakR, 13) { breakR = !breakR }
                    ExSwitchRow(Icons.Outlined.Bedtime, "Тише после 23:00", "Уведомления без звука, экран чатов приглушён", night, 13) { night = !night }
                }
                Text("Считается только на этом телефоне. kiroX никуда не отправляет эти данные.", style = ts(12.sp, color = Kx.text4, lh = 17.4.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
            }
        }
    }
}

/** wave3: honest local counting — only minutes kiroX was open, from the day counting started. */
@Composable
private fun WellbeingReal(ctx: KxScreenCtx) {
    val store = ctx.store
    val P = com.kirox.app.ui.v3.model.KxPrefsAccount
    val mins = (store.prefTexts[P.USAGE_DAYS] ?: "").split(',').mapNotNull { it.trim().toIntOrNull() }.let { if (it.size == 7) it else List(7) { 0 } }
    val since = store.prefTexts[P.USAGE_SINCE].orEmpty()
    val names = (6 downTo 0).map { back -> java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -back) } }
        .map { c -> java.text.SimpleDateFormat("EE", java.util.Locale("ru")).format(c.time).replaceFirstChar { it.uppercase() }.take(2) }
    var day by remember { mutableIntStateOf(6) }
    val limit = store.pref(P.WELL_LIMIT)
    val quiet = store.prefOn(P.WELL_QUIET)
    val max = (mins.maxOrNull() ?: 0).coerceAtLeast(maxOf(limit, 30)).toFloat() * 1.1f
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Время в kiroX", { ctx.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f)).border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(24.dp)).padding(16.dp)) {
                    Text(if (day == 6) "Сегодня" else names[day], style = ts(12.sp, color = Kx.text3))
                    Text(hm(mins[day]), style = ts(34.sp, FontWeight.ExtraBold, ls = (-1).sp), modifier = Modifier.padding(top = 2.dp))
                    Text("За неделю: " + hm(mins.sum()), style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        mins.forEachIndexed { i, m ->
                            val over = limit > 0 && m > limit; val on = day == i
                            Column(Modifier.weight(1f).fillMaxHeight().kxClick("${names[i]}: ${hm(m)}") { day = i }, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height((m / max * 106).coerceAtLeast(2f).dp).clip(RoundedCornerShape(4.dp, 4.dp, 2.dp, 2.dp))
                                    .background(if (over) Kx.warning.copy(alpha = if (on) 1f else .4f) else Kx.accent.copy(alpha = if (on) 1f else .35f)))
                                Text(names[i], style = ts(11.sp, if (on) FontWeight.ExtraBold else FontWeight.Medium, if (on) Kx.text else Kx.text4))
                            }
                        }
                    }
                    Text(if (since.isEmpty()) "Считаем с этого дня: минута засчитывается, когда kiroX открыт на экране." else "Считаем с $since, только пока kiroX на экране.",
                        style = ts(11.sp, color = Kx.text4), modifier = Modifier.padding(top = 6.dp))
                }
                Text("Дневной лимит", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WbLimits.forEach { (label, v) ->
                        val on = limit == v
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(14.dp)).kxClick(label) { store.putPref(P.WELL_LIMIT, v) }, contentAlignment = Alignment.Center) {
                            Text(label, style = ts(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
                Text("Лимит отмечается на графике; kiroX ничего не блокирует.", style = ts(11.sp, color = Kx.text4), modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    ExSwitchRow(Icons.Outlined.Bedtime, "Тише после 23:00", "Уведомления kiroX без звука до 07:00 (как «Не беспокоить»)", quiet, 13) { store.togglePref(P.WELL_QUIET) }
                }
                Text("Считается только на этом телефоне. kiroX никуда не отправляет эти данные.", style = ts(12.sp, color = Kx.text4, lh = 17.4.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
            }
        }
    }
}
