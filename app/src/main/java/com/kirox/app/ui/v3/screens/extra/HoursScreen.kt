package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import java.util.Calendar

private val HWd = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val HWdl = listOf("понедельник", "вторник", "среду", "четверг", "пятницу", "субботу", "воскресенье")
private val HWdNow = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
private data class HDay(val open: Boolean, val from: Int, val to: Int)
private fun hm(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)

/** Board «Часы и адрес» (Premium / Business): live open/closed status, presets, week with ±30 min and day switches, map + address. */
@Composable
fun HoursScreen(ctx: KxScreenCtx) {
    // demo: needs store (business opening hours + location)
    var days by remember { mutableStateOf(HWd.indices.map { HDay(true, if (it < 5) 480 else 600, if (it < 5) 1260 else 1140) }) }
    val cal = remember { Calendar.getInstance() }
    val today = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val toast = rememberExToast()
    val t = days[today]
    val openNow = t.open && now >= t.from && now < t.to
    val status = if (openNow) (if (t.to - now <= 60) "Скоро закроется · до " else "Открыто до ") + (if (t.to == 1440) "24:00" else hm(t.to)) else {
        var k = 0; var nd: Pair<Int, HDay>? = if (t.open && now < t.from) today to t else null
        while (nd == null && k < 7) { k++; val i = (today + k) % 7; if (days[i].open) nd = i to days[i] }
        if (nd != null) "Закрыто · откроется " + (if (nd.first == today && k == 0) "сегодня" else if (k == 1) "завтра" else "в " + HWdl[nd.first]) + " в " + hm(nd.second.from) else "Закрыто"
    }
    fun upd(i: Int, f: (HDay) -> HDay) { days = days.mapIndexed { k, d -> if (k == i) f(d) else d } }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Часы и адрес", { ctx.back() }) { KxPremiumChip() }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f))
                    .ellipseBg(1f, .9f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .28f), .6f to Color.Transparent).border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(24.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(50.dp).clip(CircleShape).lin2(135f, Color(0xFFB07A2E), Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.LocalCafe, null, Modifier.size(24.dp), tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Кофейня «Зерно»", style = ts(16.sp, FontWeight.ExtraBold))
                            val ink = if (openNow) Kx.accent else Kx.danger
                            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val d = if (openNow) kfLoop(1600, label = "dot") else -1f
                                Box(Modifier.size(8.dp).drawBehind {
                                    if (d >= 0f) { val k = tri(d); drawCircle(Kx.accent.copy(alpha = .6f * (1f - k)), radius = size.minDimension / 2 + 6.dp.toPx() * k) }
                                }.clip(CircleShape).background(ink))
                                Text(status, style = ts(13.sp, FontWeight.Bold, ink))
                            }
                        }
                    }
                    Text("Так видят ваш профиль. Сейчас ${HWdNow[today]}, ${hm(now)}", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 10.dp))
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<Pair<String, () -> Unit>>(
                        "Будни 9–18" to { days = HWd.indices.map { HDay(it < 5, 540, 1080) } },
                        "Каждый день" to { days = HWd.indices.map { HDay(true, 540, 1260) } },
                        "Круглосуточно" to { days = HWd.indices.map { HDay(true, 0, 1440) } },
                    ).forEach { (label, act) ->
                        Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).kxClick(label, onClick = act),
                            contentAlignment = Alignment.Center) { Text(label, style = ts(12.sp, FontWeight.Bold, Kx.text2), maxLines = 1) }
                    }
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    days.forEachIndexed { i, d ->
                        val isToday = i == today
                        Row(Modifier.fillMaxWidth().background(if (isToday) Kx.accent.copy(alpha = .06f) else Color.Transparent).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(HWd[i], style = ts(14.sp, FontWeight.ExtraBold, if (isToday) Kx.accent else Kx.text), modifier = Modifier.width(30.dp))
                            if (d.open) Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ExRoundBtn(Icons.Outlined.Remove, "${HWd[i]}: открываться на полчаса раньше", size = 26.dp, border = null, iconSize = 16.dp) { upd(i) { it.copy(from = maxOf(0, it.from - 30)) } }
                                Text(hm(d.from), style = ts(14.sp, FontWeight.Bold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.widthIn(min = 46.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("—", style = ts(14.sp, color = Kx.text4))
                                Text(if (d.to == 1440) "24:00" else hm(d.to), style = ts(14.sp, FontWeight.Bold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.widthIn(min = 46.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                ExRoundBtn(Icons.Outlined.Add, "${HWd[i]}: закрываться на полчаса позже", size = 26.dp, border = null, iconSize = 16.dp) { upd(i) { it.copy(to = minOf(1440, it.to + 30)) } }
                            } else Text("Выходной", style = ts(14.sp, color = Kx.text4), modifier = Modifier.weight(1f))
                            Box(Modifier.clip(RoundedCornerShape(13.dp)).kxClick(HWd[i] + if (d.open) ": рабочий день" else ": выходной", role = androidx.compose.ui.semantics.Role.Switch) {
                                upd(i) { it.copy(open = !it.open) }
                            }) { KxSwitch(d.open, width = 44.dp, height = 26.dp, knob = 20.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3) }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.05f)))
                    }
                }
                AwTitle("Адрес", top = 16)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    val pin = kfRun(2000)
                    Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF142019))) {
                        Canvas(Modifier.matchParentSize()) {
                            val step = 22.dp.toPx(); val c = Kx.w(.05f)
                            var x = 0f; while (x < size.width) { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
                            var y = 0f; while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
                            drawRect(Kx.w(.08f), Offset(0f, 54.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 10.dp.toPx()))
                            drawRect(Kx.w(.08f), Offset(150.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(10.dp.toPx(), size.height))
                            drawRoundRect(Kx.accentDeep.copy(alpha = .25f), Offset(40.dp.toPx(), 14.dp.toPx()), androidx.compose.ui.geometry.Size(60.dp.toPx(), 30.dp.toPx()),
                                androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                        }
                        Icon(Icons.Rounded.LocationOn, null, Modifier.offset(160.dp, 22.dp).size(38.dp).graphicsLayer { translationY = -5.dp.toPx() * tri(pin) }, tint = Kx.accent)
                    }
                    Row(Modifier.fillMaxWidth().background(Kx.w(.04f)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("ул. Абая, 52", style = ts(15.sp, FontWeight.Bold))
                            Text("Алматы · вход со двора", style = ts(12.sp, color = Kx.text4))
                        }
                        ExPill("Изменить", height = 34.dp, bg = Kx.w(.07f), size = 12.sp, padH = 12.dp) { toast.show("Откроется карта", Icons.Rounded.Map) }
                    }
                }
            }
        }
        ExBottom { ExPillCta("Сохранить", true, Icons.Rounded.Storefront) { toast.show("Часы и адрес сохранены", Icons.Rounded.Storefront) } }
        ExToast(toast, bottom = 96.dp)
    }
}
