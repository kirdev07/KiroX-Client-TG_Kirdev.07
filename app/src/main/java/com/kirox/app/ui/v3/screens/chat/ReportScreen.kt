package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val ReportReasons: List<Triple<ImageVector, String, Color>> = listOf(
    Triple(Icons.Outlined.Report, "Спам", Kx.warning),
    Triple(Icons.Outlined.Phishing, "Мошенничество", Kx.danger),
    Triple(Icons.Outlined.SentimentVeryDissatisfied, "Оскорбления", Kx.danger),
    Triple(Icons.Outlined.NoAdultContent, "Неприемлемое содержимое", Kx.warning),
    Triple(Icons.Outlined.Copyright, "Нарушение авторских прав", Kx.text3),
    Triple(Icons.Outlined.MoreHoriz, "Другое", Kx.text3),
)

/** Board «Пожаловаться»: reason list → details (what goes to moderators, comment, block/delete) → done. arg = chat id. */
@Composable
fun ReportScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is KxDemoStore
    val chatId = ctx.arg?.toLongOrNull()
    val head = chatId?.let { store.header(it) }
    var step by remember { mutableIntStateOf(1) }
    var r by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var block by remember { mutableStateOf(true) }
    var del by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    fun go(s: Int) { step = s; k++ }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        // Chat behind
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Kx.text)
                LetterAv(head?.title?.take(1)?.uppercase() ?: "?", hex("#5C6661"), 40.dp, 16)
                Column {
                    Text(head?.title ?: "+7 700 000 00 00", style = ts(16, W800), maxLines = 1)
                    Text(head?.status ?: "нет в контактах", style = ts(12, color = Kx.text3), maxLines = 1)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val msgs = if (chatId != null && !demo) store.messages(chatId).filter { !it.out }.takeLast(2).map { it.text }
                           else listOf("Поздравляем! Вы выиграли телефон 🎉", "Чтобы забрать приз, перейдите по ссылке и введите код из СМС")
                msgs.forEach {
                    Text(it, style = ts(15), modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }

        BSheet(true, {}, top = Color(0xFA28302C)) {
            when (step) {
                1 -> key(k) { Column(Modifier.chIn(dy = 0.dp, dx = 24.dp, durationMs = 300, easing = FastOutSlowInEasing)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Flag, null, tint = Kx.danger); Text("Пожаловаться", style = ts(20, W800))
                    }
                    Text("На 2 сообщения от этого номера. Что не так?", style = ts(13, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReportReasons.forEachIndexed { i, (ic, t, c) ->
                            Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick(t) { r = i; go(2) }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(ic, null, tint = c)
                                Text(t, style = ts(15, W700), modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = Kx.text4)
                            }
                        }
                    }
                } }
                2 -> key(k) { Column(Modifier.chIn(dy = 0.dp, dx = 24.dp, durationMs = 300, easing = FastOutSlowInEasing)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 40.dp, border = Color.Transparent) { go(1) }
                        Text(ReportReasons[r].second, style = ts(18, W800))
                    }
                    Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Text("Что уйдёт модераторам", style = ts(12, W700, Kx.text4))
                        Text("2 выбранных сообщения, номер отправителя и ваш комментарий. Остальная переписка не отправляется.", style = ts(13, color = Kx.text2, lh = 18.2.sp), modifier = Modifier.padding(top = 6.dp))
                    }
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f))
                        .border(1.dp, Kx.w(.1f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.TopStart) {
                        ChInput(note, { note = it }, "Комментарий (необязательно)", Modifier.fillMaxSize(), size = 14, singleLine = false)
                    }
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(Triple("Заблокировать номер", "Больше не сможет писать и звонить", block), Triple("Удалить чат", "Только у вас", del)).forEachIndexed { i, (t, sub, on) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).kxClick(t, role = androidx.compose.ui.semantics.Role.Switch) { if (i == 0) block = !block else del = !del }
                                .padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) { Text(t, style = ts(15, W700)); Text(sub, style = ts(12, color = Kx.text3)) }
                                KxSwitch(on, width = 46.dp, height = 28.dp, knob = 22.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
                            }
                        }
                    }
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.dangerSolid).kxClick {
                        if (demo) go(3)
                        else {
                            // demo: needs store (report(chatId, messageIds, reason, comment) is not in KxStore)
                            if (chatId != null) { if (block) store.block(chatId, true); if (del) store.deleteChat(chatId) }
                            ctx.toast("Отправка жалоб пока недоступна")
                        }
                    }, contentAlignment = Alignment.Center) { Text("Отправить жалобу", style = ts(16, W800, Color.White)) }
                } }
                else -> Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ShieldBadge()
                    Text("Жалоба отправлена", style = ts(20, W800), modifier = Modifier.padding(top = 14.dp))
                    Text("Модераторы проверят сообщения. Отправитель не узнает, кто пожаловался.", style = ts(14, color = Kx.text3, lh = 20.3.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    val rows = buildList { if (block) add("Номер заблокирован"); if (del) add("Чат удалён у вас"); if (isEmpty()) add("Чат остался как был") }
                    Column(Modifier.padding(top = 14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach {
                            Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Text(it, style = ts(14, W700))
                            }
                        }
                    }
                    Box(Modifier.padding(top = 14.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f))
                        .kxClick { step = 1; block = true; del = true; k++ }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                        Text("Начать заново", style = ts(14, W700, Kx.accent))
                    }
                }
            }
        }
    }
}

/** 84 shield: kxShield pop (.5 → 1.12 → 1, .5s) + kxRing pulse (scale .8→1.7, fade, 1.6s, twice). */
@Composable
private fun ShieldBadge() {
    val reduced = LocalKxReducedMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        pop.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        repeat(2) { ring.snapTo(0f); ring.animateTo(1f, tween(1600, easing = LinearOutSlowInEasing)) }
    }
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        if (!reduced && ring.value in 0.001f..0.999f) Box(Modifier.fillMaxSize().graphicsLayer {
            val s = .8f + .9f * ring.value; scaleX = s; scaleY = s; alpha = .7f * (1f - ring.value)
        }.border(2.dp, Kx.accent, CircleShape))
        val p = pop.value
        val s = if (p < .6f) .5f + (.62f) * (p / .6f) else 1.12f - .12f * ((p - .6f) / .4f)
        Box(Modifier.size(84.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (p / .6f).coerceIn(0f, 1f) }.clip(CircleShape)
            .cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(44.dp), tint = Kx.onAccent)
        }
    }
}
