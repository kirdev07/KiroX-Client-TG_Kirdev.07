package com.kirox.app.ui.v3.screens.extra

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

private val WbDays = listOf(Triple("Пн", 82, "понедельник"), Triple("Вт", 64, "вторник"), Triple("Ср", 95, "среда"), Triple("Чт", 51, "четверг"),
    Triple("Пт", 118, "пятница"), Triple("Сб", 74, "суббота"), Triple("Вс", 47, "сегодня"))
private class WbSplit(val name: String, val l: String, val c: Color, val f: Float)
private val WbSplits = listOf(WbSplit("Kotlin-клуб", "K", Kx.accentDeep, .34f), WbSplit("Мама", "М", Color(0xFFB07A2E), .22f), WbSplit("Каналы", "К", Color(0xFF2F6E9E), .26f), WbSplit("Анна", "А", Color(0xFFC0443C), .18f))
private val WbLimits = listOf("Нет" to 0, "45 мин" to 45, "1 час" to 60, "1,5 ч" to 90)
private fun hm(m: Int) = if (m >= 60) "${m / 60} ч ${m % 60} мин" else "$m мин"

/** Board «Время в kiroX»: today card (leaf), 7 day bars growing in with the dashed limit line, time per chat, daily limit chips, two switches. */
@Composable
fun WellbeingScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { com.kirox.app.ui.v3.screens.KxNotYet(ctx, "Цифровой баланс", "kiroX начнёт считать время в приложении на этом телефоне, когда функция будет подключена. Пока данных нет."); return }
    // demo: needs store (local usage tracking per day/chat; limit + break reminder + quiet after 23:00 prefs)
    var day by remember { mutableIntStateOf(6) }
    var limit by remember { mutableIntStateOf(60) }
    var breakR by remember { mutableStateOf(true) }
    var night by remember { mutableStateOf(false) }
    val max = 130f
    val d = WbDays[day]
    val avg = Math.round(WbDays.sumOf { it.second } / 7.0).toInt()
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
                            WbDays.forEachIndexed { i, (wd, m, name) ->
                                val over = limit > 0 && m > limit; val on = day == i
                                val c by animateColorAsState(if (on) (if (over) Kx.warning else Kx.accent) else if (over) Kx.warning.copy(alpha = .4f) else Kx.accent.copy(alpha = .35f), tween(200), label = "bc")
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
                    Text("Нажмите на день, чтобы посмотреть подробнее", style = ts(11.sp, color = Kx.text4), modifier = Modifier.padding(top = 6.dp))
                }
                Text("Где проводили время · ${d.third}", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WbSplits.forEach { w ->
                        Row(Modifier.exIn(250, dy = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExLetter(w.l, 34.dp, w.c, font = 13.sp, ink = Kx.text)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(w.name, style = ts(14.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                                    Text(hm(Math.round(d.second * w.f)), style = ts(13.sp, color = Kx.text2).copy(fontFeatureSettings = "tnum"))
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
