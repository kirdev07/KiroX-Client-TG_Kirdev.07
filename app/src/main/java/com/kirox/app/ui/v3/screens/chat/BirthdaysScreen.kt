package com.kirox.app.ui.v3.screens.chat

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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val BdPink = Color(0xFFF07AA0)
// demo: needs store (contacts' birthdays: today + upcoming, per-person reminder, send greeting to that chat)
private val BdSoon = listOf(listOf("Анна", "А", "#C0443C", "25 ноября · среда", "3"), listOf("Дмитрий", "Д", "#2F6E9E", "2 декабря · среда", "10"),
    listOf("Папа", "П", "#3F9A6C", "14 декабря · понедельник", "22"), listOf("Лиза", "Л", "#6A4C9C", "31 декабря · четверг", "39"))
private val BdWish = listOf("С днём рождения, бабуля! Здоровья, тепла и радости каждый день ❤️", "Бабушка, с юбилеем! 80 — это только начало. Обнимаю крепко!", "С праздником! Приеду в выходные с тортом 🎂")
private val BdSt = listOf("🎂", "🎉", "💐", "🥳", "❤️")
private val BdConf = listOf(BdPink, Kx.accent, Kx.warning, hex("#6FA9D8"), hex("#B39DDB"))

private fun bdDays(n: Int) = if (n == 1) "день" else if (n < 5) "дня" else if (n % 10 == 1 && n != 11) "день" else if (n % 10 in 2..4 && n !in 12..14) "дня" else "дней"

/** Board «Дни рождения»: today hero with confetti + balloon (wish / call / video), upcoming list with reminders, wish sheet. */
@Composable
fun BirthdaysScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var sheet by remember { mutableStateOf(false) }
    var wish by remember { mutableIntStateOf(0) }
    var st by remember { mutableIntStateOf(0) }
    var sent by remember { mutableStateOf(false) }
    val remind = remember { mutableStateListOf(0) }
    var toast by remember { mutableStateOf<Pair<String, ImageVector>?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { delay(2400); toast = null } }
    fun say(t: String, i: ImageVector) { toast = t to i; tk++ }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, onClick = ctx.back)
                Text("Дни рождения", style = ts(20, W800), modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                if (demo) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f))
                        .ellipseGlow(1f, 0f, 1f, .9f, BdPink.copy(alpha = .3f)).ellipseGlow(0f, 1f, .9f, .8f, Kx.accentDeep.copy(alpha = .3f))
                        .border(1.dp, Kx.w(.12f), RoundedCornerShape(26.dp))) {
                        Confetti()
                        Balloon(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 18.dp))
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                            Text("СЕГОДНЯ", style = ts(12, W800, BdPink).copy(letterSpacing = .5.sp))
                            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(58.dp).clip(CircleShape).cssGradient(135f, hex("#B07A2E"), hex("#6A4C9C")).border(3.dp, BdPink, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("Б", style = ts(22, W800))
                                }
                                Column { Text("Бабушка", style = ts(18, W800)); Text("исполняется 80 🎂", style = ts(13, color = Kx.text2)) }
                            }
                            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick { if (!sent) sheet = true },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Celebration, null, Modifier.size(18.dp), tint = Kx.onAccent); Text(if (sent) "Поздравлено ✓" else "Поздравить", style = ts(13, W800, Kx.onAccent))
                                }
                                HeroBtn(Icons.Rounded.Call, "Позвонить") { say("Звонок · Бабушка…", Icons.Rounded.Call) }
                                HeroBtn(Icons.Rounded.Videocam, "Видео") { say("Видеозвонок · Бабушка…", Icons.Rounded.Videocam) }
                            }
                        }
                    }
                    Text("Скоро", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 8.dp))
                    BdSoon.forEachIndexed { i, (name, l, c, date, leftS) ->
                        val left = leftS.toInt(); val on = i in remind
                        Row(Modifier.chIn(i * 50, 8.dp, 250).fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LetterAv(l, hex(c), 46.dp, 16)
                            Column(Modifier.weight(1f)) { Text(name, style = ts(15, W700)); Text(date, style = ts(12, color = Kx.text4)) }
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (left <= 3) BdPink.copy(alpha = .16f) else Kx.w(.06f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("через $left ${bdDays(left)}", style = ts(12, W800, if (left <= 3) BdPink else Kx.text3))
                            }
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick(if (on) "Не напоминать" else "Напомнить за день") {
                                if (on) remind.remove(i) else remind.add(i); say(if (on) "Напоминание выключено" else "Напомним за день", Icons.Rounded.Notifications)
                            }, contentAlignment = Alignment.Center) {
                                Icon(if (on) Icons.Rounded.Notifications else Icons.Outlined.Notifications, null, Modifier.size(19.dp), tint = if (on) Kx.accent else Kx.text3)
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp, vertical = 18.dp)) {
                        Text("СЕГОДНЯ", style = ts(12, W800, BdPink).copy(letterSpacing = .5.sp))
                        Text("Сегодня дней рождения нет", style = ts(16, W800), modifier = Modifier.padding(top = 10.dp))
                    }
                }
                Text("Даты берутся из профилей Telegram, если человек их показывает, и из ваших контактов.", style = ts(12, color = Kx.text4, lh = 17.4.sp),
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
            }
        }

        BSheet(sheet, { sheet = false }, top = Color(0xF728302C), bottom = Color(0xFC121614), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp)) {
            Text("Поздравить", style = ts(19, W800))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BdWish.forEachIndexed { i, t ->
                    val on = wish == i
                    Text(t, style = ts(14, lh = 19.6.sp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick { wish = i }.padding(horizontal = 14.dp, vertical = 12.dp))
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BdSt.forEachIndexed { i, e ->
                    val on = st == i
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.04f))
                        .border(1.5.dp, if (on) Kx.accent else Kx.w(.06f), RoundedCornerShape(14.dp)).kxClick(e) { st = i }, contentAlignment = Alignment.Center) { Text(e, style = ts(26)) }
                }
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                sheet = false; sent = true; say("Поздравление отправлено ${BdSt[st]}", Icons.Rounded.Celebration)
            }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent); Text("Отправить поздравление", style = ts(15, W800, Kx.onAccent))
            }
        }
        val t = toast
        if (t != null) key(tk) { MixToast(t.first, t.second, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), 30) }
    }
}

@Composable
private fun RowScope.HeroBtn(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).kxClick(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Kx.text); Text(text, style = ts(13, W700))
    }
}

/** 🎈 kxBalloon (−12px, ±4°, 3s, twice). */
@Composable
private fun Balloon(modifier: Modifier) {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(3000, easing = LinearEasing)) } }
    val w = ((1 - kotlin.math.cos(2 * Math.PI * p.value)) / 2).toFloat()
    Text("🎈", style = ts(44), modifier = modifier.graphicsLayer { translationY = -12.dp.toPx() * w; rotationZ = if (reduced) 0f else -4f + 8f * w })
}

/** 12 confetti strips falling 240px with 540° spin, staggered .3s, twice. */
@Composable
private fun BoxScope.Confetti() {
    if (LocalKxReducedMotion.current) return
    var wpx by remember { mutableIntStateOf(0) }
    Box(Modifier.matchParentSize().onSizeChanged { wpx = it.width }) {
        repeat(12) { i ->
            val p = remember { Animatable(0f) }
            val dur = (2400 + (i % 4) * 500)
            LaunchedEffect(Unit) { delay(i * 300L); repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(dur, easing = LinearEasing)) } }
            val v = p.value
            if (v > 0f && v < 1f) Box(Modifier.graphicsLayer {
                translationX = wpx * (5 + i * 8) / 100f; translationY = (-20 + 260 * v).dp.toPx(); rotationZ = 540f * v
                alpha = if (v < .1f) v / .1f else 1f - (v - .1f) / .9f
            }.size(6.dp, 10.dp).clip(RoundedCornerShape(2.dp)).background(BdConf[i % 5]))
        }
    }
}
