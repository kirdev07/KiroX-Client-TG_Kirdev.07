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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val MtDays = listOf("Пт" to "27", "Сб" to "28", "Вс" to "29", "Пт" to "4 дек")
private val MtTimes = listOf("12:00", "15:00", "18:00", "20:00")
private val MtBase = listOf(listOf(0, 4, 3, 0), listOf(1, 6, 5, 1), listOf(3, 8, 4, 2), listOf(5, 5, 2, 4))
private val MtPeople = listOf("О" to Kx.text3, "М" to Kx.warning, "А" to Color(0xFFF07AA0), "Д" to Color(0xFF6FA9D8), "Е" to Color(0xFFB39DDB), "Л" to Color(0xFFC0443C))
private val MtLong = listOf("пятницу, 27 ноября", "субботу, 28 ноября", "воскресенье, 29 ноября", "пятницу, 4 декабря")

/** Board «Удобное время»: day × time availability grid (heat by votes, your cells outlined, crown on the best), best slot card, voters, fix the meeting. */
@Composable
fun MeetTimeScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Выбрать время") else com.kirox.app.ui.v3.screens.more.MeetTimeLive(ctx, c); return }
    // demo: needs store (multi-answer poll as a time grid: votes per option, voters, schedule the event)
    val mine = remember { mutableStateMapOf("2-1" to true) }
    var fixed by remember { mutableStateOf(false) }
    var pop by remember { mutableStateOf("") }
    var popN by remember { mutableIntStateOf(0) }
    fun cnt(t: Int, d: Int) = MtBase[t][d] + if (mine["$t-$d"] == true) 1 else 0
    var best = 0 to 0; var bv = -1
    MtTimes.indices.forEach { t -> MtDays.indices.forEach { d -> val v = cnt(t, d); if (v > bv) { bv = v; best = t to d } } }
    val myN = mine.values.count { it }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) { Text("Выпускники 2018", style = ts(16.sp, FontWeight.ExtraBold)); Text("12 участников", style = ts(12.sp, color = Kx.text3)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.03f)))).border(1.dp, Kx.w(.1f), RoundedCornerShape(24.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EventAvailable, null, Modifier.size(24.dp), tint = Kx.accent)
                        Text("Встреча выпускников", style = ts(16.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    }
                    Text("Опрос kiroX · Олег · отметьте все удобные дни", style = ts(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(52.dp))
                            MtDays.forEach { (w, n) ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(n, style = ts(13.sp, FontWeight.Bold, lh = 15.6.sp)); Text(w, style = ts(11.sp, color = Kx.text3, lh = 13.2.sp))
                                }
                            }
                        }
                        MtTimes.forEachIndexed { t, time ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(time, style = ts(12.sp, FontWeight.Medium, Kx.text3), modifier = Modifier.width(52.dp))
                                MtDays.indices.forEach { d ->
                                    val k = "$t-$d"; val v = cnt(t, d); val my = mine[k] == true
                                    val a = if (v == 0) 0f else minOf(.85f, .12f + v * .08f)
                                    val bg by animateColorAsState(if (v > 0) Kx.accent.copy(alpha = a) else Color.Transparent, tween(200), label = "c")
                                    // critique-B: an empty cell is outlined (looked disabled when it was a flat grey fill).
                                    val bd by animateColorAsState(if (my) Kx.accent else if (v == 0) Kx.w(.14f) else Color.Transparent, tween(200), label = "b")
                                    key(if (pop == k) popN else 0) {
                                        val p = if (pop == k && popN > 0) enter(250) else 1f
                                        Box(Modifier.weight(1f).height(46.dp).graphicsLayer { val s = if (p < .6f) .6f + .52f * (p / .6f) else 1.12f - .12f * ((p - .6f) / .4f); scaleX = s; scaleY = s }) {
                                            Box(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).background(bg).border(if (my) 2.dp else 1.dp, bd, RoundedCornerShape(12.dp))
                                                .kxClick("${MtDays[d].first} ${MtDays[d].second}, $time: $v" + if (my) ", вы отметили" else "") { mine[k] = !my; pop = k; popN++; fixed = false },
                                                contentAlignment = Alignment.Center) { if (v > 0) Text("$v", style = ts(12.sp, FontWeight.ExtraBold, if (a > .5f) Kx.onAccent else Kx.text)) }
                                            if (t == best.first && d == best.second && bv > 0) {
                                                val cr = tri(kfRun(2000))
                                                Icon(Icons.Rounded.WorkspacePremium, null, Modifier.align(Alignment.TopEnd).offset(5.dp, (-9).dp).size(16.dp).graphicsLayer {
                                                    translationY = -3.dp.toPx() * cr; rotationZ = -8f + 16f * cr
                                                }, tint = Kx.warning)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(14.dp).border(2.dp, Kx.accent, RoundedCornerShape(4.dp))); Text("Вы", style = ts(11.sp, color = Kx.text4))
                        Spacer(Modifier.width(0.dp))
                        listOf(.15f, .45f, .8f).forEach { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(Kx.accent.copy(alpha = it))) }
                        Text("больше людей", style = ts(11.sp, color = Kx.text4))
                    }
                }
                Row(Modifier.padding(top = 12.dp).exIn(300, dy = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).lin2(150f, Kx.warning.copy(alpha = .16f), Kx.w(.03f))
                    .border(1.dp, Kx.warning.copy(alpha = .3f), RoundedCornerShape(22.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(32.dp), tint = Kx.warning)
                    Column(Modifier.weight(1f)) {
                        Text("Удобнее всего", style = ts(12.sp, color = Color(0xFFE4D3B0)))
                        Text("В ${MtLong[best.second]}, ${MtTimes[best.first]}", style = ts(16.sp, FontWeight.ExtraBold))
                        Text("Могут $bv из 12" + if (mine["${best.first}-${best.second}"] == true) ", и вы тоже" else "", style = ts(12.sp, color = Kx.text3))
                    }
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        MtPeople.forEach { (l, c) -> Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Kx.bg).padding(2.dp)) { ExLetter(l, 28.dp, c, font = 11.sp, ink = Kx.text) } }
                    }
                    Text("${if (myN > 0) 9 else 8} из 12 уже ответили", style = ts(13.sp, color = Kx.text2))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExPill("Сбросить мои отметки", height = 52.dp, bg = Kx.w(.07f), padH = 16.dp) { mine.clear(); fixed = false }
                Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Назначить") { fixed = true },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Event, null, Modifier.size(20.dp), tint = Kx.onAccent)
                    Text(if (fixed) "Встреча назначена ✓" else "Назначить на ${MtDays[best.second].first.lowercase()} ${MtDays[best.second].second}, ${MtTimes[best.first]}",
                        style = ts(15.sp, FontWeight.ExtraBold, Kx.onAccent), maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
