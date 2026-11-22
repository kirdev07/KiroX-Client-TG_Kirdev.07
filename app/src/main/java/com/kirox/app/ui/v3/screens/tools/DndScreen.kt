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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.components.topHighlight
import com.kirox.app.ui.v3.theme.*

private val DND_DURS = listOf("1 час", "3 часа", "До утра", "Пока не выключу")
private val DND_PEOPLE = listOf(Triple("Мама", "М", Color(0xFFB07A2E)), Triple("Анна", "А", Color(0xFFC0443C)), Triple("Папа", "П", Color(0xFF2F6E9E)),
    Triple("Дмитрий", "Д", Color(0xFF6A4C9C)), Triple("Работа", "Р", Color(0xFF5C6661)), Triple("Алекс", "А", Color(0xFF3F9A6C)))
private val Violet = Color(0xFFB9A6FF)
private val VioletDeep = Color(0xFF6E5BC4)

/** Board «Не беспокоить»: big moon orb toggle (breathing glow, floating z), duration, exceptions (people + calls), nightly schedule bar. */
@Composable
fun DndScreen(ctx: KxScreenCtx) {
    // demo: needs store (do-not-disturb: on/until, exceptions list, calls/repeat flags, nightly schedule) — people are demo contacts
    var on by rememberSaveable { mutableStateOf(false) }
    var dur by rememberSaveable { mutableIntStateOf(2) }
    var exc by remember { mutableStateOf(setOf(0, 2)) }
    var calls by rememberSaveable { mutableStateOf(true) }
    var repeatC by rememberSaveable { mutableStateOf(true) }
    var sched by rememberSaveable { mutableStateOf(true) }
    var from by rememberSaveable { mutableIntStateOf(23) }
    var to by rememberSaveable { mutableIntStateOf(7) }
    val toast = remember { TToast() }
    fun hh(h: Int) = (((h % 24) + 24) % 24).toString().padStart(2, '0') + ":00"
    val acc by animateColorAsState(if (on) Violet else Kx.accent, tween(400), label = "acc")
    val until = when (dur) { 0 -> "18:15"; 1 -> "20:15"; 2 -> hh(to); else -> "" }
    val reduced = LocalKxReducedMotion.current
    val nightA by animateFloatAsState(if (on) 1f else 0f, tween(600), label = "night")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Box(Modifier.matchParentSize().alpha(nightA).background(Color(0xFF0A0A10)).drawBehind {
            drawRect(Brush.radialGradient(0f to Color(0xFF1D1838), .6f to Color(0xFF0A0A10), center = Offset(size.width / 2, 0f), radius = size.width * 1.2f))
        })
        TScreen("Не беспокоить", ctx.back, titleSize = 20, bg = Color.Transparent, overlay = { TToastHost(toast, 34.dp) }) {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(170.dp)) {
                    val br = if (on) tCycles(4000, 2, easing = TEaseInOut, key = on) else 0f
                    val bv = (1f - kotlin.math.cos(br * 2 * Math.PI).toFloat()) / 2f
                    val glow = if (on) Color(150, 120, 255).copy(alpha = .45f) else Kx.accent.copy(alpha = .25f)
                    Box(Modifier.requiredSize(198.dp).align(Alignment.Center).graphicsLayer { val s = 1f + .12f * bv; scaleX = s; scaleY = s; alpha = if (on) (.55f - .3f * bv) / .55f else 1f }
                        .background(Brush.radialGradient(0f to glow, .7f to Color.Transparent)))
                    val oShape = CircleShape
                    Column(Modifier.matchParentSize().then(if (on) Modifier.shadow(30.dp, oShape, spotColor = VioletDeep, ambientColor = VioletDeep) else Modifier)
                        .clip(oShape)
                        .drawBehind {
                            if (on) drawRect(Brush.radialGradient(0f to Color(0xFF4A3E8C), .7f to Color(0xFF1E1940), center = Offset(size.width * .35f, size.height * .3f), radius = size.width * .955f))
                            else drawRect(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.03f))))
                        }
                        .border(1.dp, if (on) Violet.copy(alpha = .5f) else Kx.w(.14f), oShape).topHighlight(Kx.w(if (on) .2f else .12f))
                        .kxClick(if (on) "Выключить «Не беспокоить»" else "Включить «Не беспокоить»", Role.Switch) {
                            toast.show(if (on) "Уведомления снова включены" else "Тишина" + (if (until.isNotEmpty()) " до $until" else ""),
                                if (on) Icons.Rounded.NotificationsActive else Icons.Rounded.Bedtime, if (on) Kx.accent else Violet)
                            on = !on
                        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        val ink by animateColorAsState(if (on) Color(0xFFE4DCFF) else Kx.text3, tween(400), label = "ink")
                        Icon(if (on) Icons.Rounded.Bedtime else Icons.Outlined.Bedtime, null, Modifier.size(64.dp), tint = ink)
                        Text(if (on) "Включено" else "Выключено", style = KxType.style(14.sp, FontWeight.ExtraBold, ink), modifier = Modifier.padding(top = 6.dp))
                    }
                    if (on && !reduced) {
                        listOf(Triple(24, 26, 16) to 0, Triple(14, 34, 12) to 1300).forEach { (pos, d) ->
                            val z = tCycles(2600, 2, delayMs = d, rest = 1f, easing = LinearOutSlowInEasing, key = on)
                            val a = if (z >= 1f) 0f else if (z < .3f) z / .3f else 1f - (z - .3f) / .7f
                            Text("z", style = KxType.style(pos.third.sp, FontWeight.ExtraBold, Violet), modifier = Modifier.align(Alignment.TopEnd).padding(end = pos.first.dp, top = pos.second.dp)
                                .graphicsLayer { translationX = 18.dp.toPx() * z; translationY = -40.dp.toPx() * z; val s = .6f + .5f * z; scaleX = s; scaleY = s; alpha = a })
                        }
                    }
                }
                Text(if (on) (if (until.isNotEmpty()) "Тишина до $until" else "Тишина, пока не выключите") else "Уведомления приходят как обычно",
                    style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.padding(top = 14.dp))
                Text(if (on) (if (exc.isNotEmpty()) "Пропускаем: " + DND_PEOPLE.filterIndexed { i, _ -> i in exc }.joinToString(", ") { it.first } else "Никого не пропускаем") else "Нажмите на луну, чтобы включить",
                    style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }

            Text("Включить на", style = KxType.style(13.sp, FontWeight.Bold, acc), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DND_DURS.forEachIndexed { i, label ->
                    val sel = dur == i
                    val bg by animateColorAsState(if (sel) (if (on) Violet.copy(alpha = .16f) else Kx.accent.copy(alpha = .14f)) else Kx.w(.05f), tween(200), label = "b")
                    val bd by animateColorAsState(if (sel) (if (on) Violet.copy(alpha = .45f) else Kx.accent.copy(alpha = .45f)) else Kx.w(.08f), tween(200), label = "d")
                    val shape = RoundedCornerShape(16.dp)
                    Box(Modifier.weight(1f).height(44.dp).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.RadioButton) { dur = i }, contentAlignment = Alignment.Center) {
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (sel) acc else Kx.text2), textAlign = TextAlign.Center)
                    }
                }
            }

            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Всё равно присылать от", style = KxType.style(13.sp, FontWeight.Bold, acc), modifier = Modifier.weight(1f))
                Text(if (exc.isNotEmpty()) "${exc.size} выбрано" else "никого", style = KxType.style(12.sp, color = Kx.text4))
            }
            val gShape = RoundedCornerShape(22.dp)
            Column(Modifier.fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape).padding(12.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DND_PEOPLE.forEachIndexed { i, (name, l, c) ->
                        val sel = i in exc
                        val op by animateFloatAsState(if (sel) 1f else .55f, tween(200), label = "op")
                        Column(Modifier.width(58.dp).alpha(op).kxClick(name + if (sel) ", пропускать" else "", Role.Checkbox) { exc = if (sel) exc - i else exc + i },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(50.dp)) {
                                Box(Modifier.matchParentSize().then(if (sel) Modifier.border(2.dp, acc, CircleShape).padding(2.dp).border(2.dp, Kx.bg, CircleShape) else Modifier)
                                    .clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                                    Text(l, style = KxType.style(18.sp, FontWeight.ExtraBold, Color.White))
                                }
                                if (sel) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(20.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(acc),
                                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                            }
                            Text(name, style = KxType.style(11.sp, color = Kx.text2), maxLines = 1)
                        }
                    }
                }
                Box(Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                listOf(Triple(Icons.Outlined.Call, "Звонки от избранных", "Звонок пройдёт, даже если сообщения молчат"),
                    Triple(Icons.Outlined.Replay, "Повторный звонок", "Если звонят второй раз за 3 минуты — пропустим")).forEachIndexed { i, (ic, label, sub) ->
                    val v = if (i == 0) calls else repeatC
                    Row(Modifier.fillMaxWidth().kxClick(role = Role.Switch) { if (i == 0) calls = !calls else repeatC = !repeatC }.padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ic, null, Modifier.size(24.dp), tint = Kx.text3)
                        Column(Modifier.weight(1f)) {
                            Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
                            Text(sub, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                        }
                        DndSwitch(v, on)
                    }
                }
            }

            Text("Каждую ночь", style = KxType.style(13.sp, FontWeight.Bold, acc), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth().kxClick(role = Role.Switch) { sched = !sched }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(24.dp), tint = Kx.text3)
                    Text("По расписанию", style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    DndSwitch(sched, on)
                }
                if (sched) Column(Modifier.padding(top = 14.dp).tRise(0, 200, 8.dp)) {
                    val wraps = from > to
                    val bar = if (on) Brush.horizontalGradient(listOf(VioletDeep, Color(0xFF9C88F0))) else Brush.horizontalGradient(listOf(Kx.accentDeep, Kx.accentMid))
                    BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.06f))) {
                        val w = maxWidth
                        Box(Modifier.offset(x = w * (from / 24f)).width(w * ((if (wraps) 24 - from else to - from).coerceAtLeast(0) / 24f)).fillMaxHeight().background(bar))
                        if (wraps) Box(Modifier.width(w * (to / 24f)).fillMaxHeight().background(bar))
                        Row(Modifier.matchParentSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            listOf("0", "6", "12", "18", "24").forEach { Text(it, style = KxType.style(11.sp, color = Kx.text4)) }
                        }
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("С" to 0, "До" to 1).forEach { (label, k) ->
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TRoundBtn(Icons.Rounded.Remove, "$label: на час раньше", 30.dp) { if (k == 0) from = (from + 23) % 24 else to = (to + 23) % 24 }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, style = KxType.style(11.sp, color = Kx.text4)); Text(hh(if (k == 0) from else to), style = KxType.style(17.sp, FontWeight.ExtraBold))
                                }
                                TRoundBtn(Icons.Rounded.Add, "$label: на час позже", 30.dp) { if (k == 0) from = (from + 1) % 24 else to = (to + 1) % 24 }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DndSwitch(v: Boolean, night: Boolean) = TSwitch(v, if (night) VioletDeep else Kx.accentDeep, Kx.w(.14f), 48.dp, 28.dp, 22.dp, Kx.text, Kx.text3, shadow = false)
