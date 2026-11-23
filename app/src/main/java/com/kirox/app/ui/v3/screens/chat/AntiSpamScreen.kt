package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private class Caught(val id: Int, val n: String, val l: String, val t: String, val msg: String)

// demo list. Real group (ctx.arg = chat id, wave3): aggressive anti-spam (store.antiSpam / switchAntiSpam) and slow mode (groupInfo / setSlowMode);
// the filtered messages live in the admin log (the list, «links only from admins» and the weekly count stay demo).
private val CaughtDemo = listOf(
    Caught(1, "Гость_7741", "Г", "14:02", "Заработок от 5000 в день без вложений, пиши в лс"),
    Caught(2, "crypto_signal", "C", "12:40", "Бесплатные сигналы, вход по ссылке в профиле 🚀"),
    Caught(3, "Аружан", "А", "10:15", "Ребята, ссылка на запись митапа: youtu.be/…"),
)

/** Board «Защита от спама» (group admin): shield card, stats, three switches, today's filtered messages with «Не спам». */
@Composable
fun AntiSpamScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    var available by remember { mutableStateOf<Boolean?>(if (demo) true else null) }
    var groupLine by remember { mutableStateOf(if (demo) "Android Dev KZ · 4 820 участников" else "") }
    var aggr by remember { mutableStateOf(true) }
    var newbies by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) {
        if (demo) return@LaunchedEffect
        if (chatId == null) { available = false; return@LaunchedEffect }
        val a = store.antiSpam(chatId)
        val info = store.groupInfo(chatId)
        available = a != null
        aggr = a == true
        newbies = (info?.slowModeSeconds ?: 0) > 0
        if (info != null) groupLine = info.title + if (info.members > 0) " · ${com.kirox.app.ui.v3.data.KxFormat.count(info.members)} " + com.kirox.app.ui.v3.data.KxFormat.plural(info.members, "участник", "участника", "участников") else ""
    }
    var links by remember { mutableStateOf(false) }
    val back = remember { mutableStateListOf<Int>() }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { kotlinx.coroutines.delay(2000); toast = null } }
    val list = CaughtDemo.filter { it.id !in back }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Защита от спама", style = ts(20, W800))
                    Text(groupLine, style = ts(12, color = Kx.text3))
                }
            }
            if (!demo && available != true) Column(Modifier.weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ShieldTile(false)
                Text(if (available == null) "Проверяем группу…" else "Здесь защиту не настроить", style = ts(17, W800), modifier = Modifier.padding(top = 12.dp))
                if (available == false) Text("Усиленный антиспам Telegram включают админы больших супергрупп. Откройте экран из управления такой группой.",
                    style = ts(13, color = Kx.text3, lh = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                val glow by animateColorAsState(if (aggr) Kx.accent.copy(alpha = .22f) else Kx.warning.copy(alpha = .16f), label = "glow")
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f))
                    .drawBehind {
                        val c = Offset(size.width / 2, 0f); val r = kotlin.math.hypot(size.width / 2, size.height)
                        drawRect(Brush.radialGradient(0f to glow, .7f to glow.copy(alpha = 0f), center = c, radius = r))
                    }
                    .border(1.dp, Kx.w(.08f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ShieldTile(aggr)
                    Column(Modifier.weight(1f)) {
                        Text(if (aggr) "Усиленная защита включена" else "Обычная защита", style = ts(17, W800))
                        Text(if (aggr) "Telegram сам удаляет похожие на спам сообщения от новичков и подозрительных аккаунтов." else "Спам удаляют только админы и жалобы участников.",
                            style = ts(13, color = Kx.text2, lh = 18.2.sp), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (demo) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple(if (aggr) "27" else "—", "удалено за неделю", Kx.danger), Triple("${list.size}", "сегодня", Kx.text), Triple("${back.size}", "отмечено «не спам»", Kx.accent)).forEach { (v, t, c) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v, style = ts(22, W800, c)); Text(t, style = ts(11, color = Kx.text3), textAlign = TextAlign.Center)
                        }
                    }
                }
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).padding(horizontal = 14.dp)) {
                    val opts = listOf(
                        Triple(Icons.Outlined.HealthAndSafety, "Усиленный антиспам" to "Автоматически, для больших групп", aggr),
                        Triple(Icons.Outlined.LinkOff, "Ссылки только от админов" to "Участники не смогут отправлять ссылки", links),
                        Triple(Icons.Outlined.Speed, "Медленный режим" to "Одно сообщение в минуту на участника", newbies),
                    ).let { if (demo) it else listOf(it[0], it[2]) }
                    opts.forEachIndexed { i, (ic, ts2, on) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).kxClick(ts2.first, role = androidx.compose.ui.semantics.Role.Switch) {
                            if (demo) when (i) { 0 -> aggr = !aggr; 1 -> links = !links; else -> newbies = !newbies }
                            else if (chatId != null) when (i) {
                                0 -> { aggr = !aggr; store.switchAntiSpam(chatId, aggr) }
                                else -> { newbies = !newbies; store.setSlowMode(chatId, if (newbies) 60 else 0) }
                            }
                        }.drawBehind { if (i < opts.lastIndex) drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx()) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(ic, null, tint = Kx.text3)
                            Column(Modifier.weight(1f)) { Text(ts2.first, style = ts(15, W700)); Text(ts2.second, style = ts(12, color = Kx.text3, lh = 16.2.sp)) }
                            BSwitch(on)
                        }
                    }
                }
                if (!demo && chatId != null) Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f))
                    .kxClick("Журнал действий") { ctx.open("AdminLog:$chatId") }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Что удалил фильтр, видно в «Недавних действиях» группы", style = ts(13, color = Kx.text2, lh = 18.sp), modifier = Modifier.weight(1f))
                    Text("Открыть", style = ts(12, W800, Kx.accent))
                }
                if (demo) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Удалено сегодня · ${list.size}", style = ts(13, W700, Kx.accent), modifier = Modifier.weight(1f))
                    Text("отметьте ошибки", style = ts(11, color = Kx.text4))
                }
                if (demo) Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    CaughtDemo.forEach { c ->
                        AnimatedVisibility(c.id !in back, exit = shrinkVertically(tween(300)) + fadeOut(tween(300))) {
                            Row(Modifier.padding(bottom = 6.dp).chIn(dy = 0.dp, dx = (-10).dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.danger.copy(alpha = .05f))
                                .border(1.dp, Kx.danger.copy(alpha = .15f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LetterAv(c.l, hex("#5C6661"), 36.dp, 13)
                                Column(Modifier.weight(1f)) {
                                    Text(buildAnnotatedString { append(c.n); append(" · "); withStyle(SpanStyle(color = Kx.text3, fontWeight = W400)) { append(c.t) } }, style = ts(13, W700), maxLines = 1)
                                    Text(c.msg, style = ts(13, color = Kx.text2).copy(textDecoration = TextDecoration.LineThrough), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.08f)).kxClick {
                                    back += c.id; toast = "Отмечено как ошибка — фильтр это учтёт"; tk++
                                }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("Не спам", style = ts(12, W800, Kx.accent)) }
                            }
                        }
                    }
                }
            }
        }
        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle)
    }
}

/** 72 r24 tile: gradient + kxShield breathe (2.6s, twice) + kxScan line (1.6s, endless) when on; amber .16 otherwise. */
@Composable
private fun ShieldTile(on: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val br = remember { Animatable(0f) }
    LaunchedEffect(on) { br.snapTo(0f); if (on && !reduced) repeat(2) { br.snapTo(0f); br.animateTo(1f, tween(2600, easing = LinearEasing)) } }
    val scan = rememberInfiniteTransition(label = "scan")
    val sp by scan.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing)), label = "sp")
    val s = 1f + .06f * kotlin.math.sin(Math.PI * br.value).toFloat()
    Box(Modifier.size(72.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(24.dp))
        .then(if (on) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.warning.copy(alpha = .16f))),
        contentAlignment = Alignment.Center) {
        Icon(if (on) Icons.Rounded.HealthAndSafety else Icons.Rounded.Shield, null, Modifier.size(40.dp), tint = if (on) Kx.onAccent else Kx.warning)
        if (on && !reduced) {
            val a = when { sp < .2f -> sp / .2f; sp > .8f -> (1f - sp) / .2f; else -> 1f }
            Box(Modifier.fillMaxWidth().height(2.dp).graphicsLayer { translationY = (-40 + 80 * sp).dp.toPx(); alpha = a }
                .shadow(10.dp, spotColor = Color.White, ambientColor = Color.White).background(Kx.w(.8f)))
        }
    }
}
