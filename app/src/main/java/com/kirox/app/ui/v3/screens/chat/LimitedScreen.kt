package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private val LimitedReasons = listOf("Пишу покупателям и продавцам по объявлениям", "Пишу коллегам и клиентам по работе", "Не знаю, почему так вышло")

/** Board «Ограничение» (spam-limited account): chat with banner → explainer → appeal → done. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LimitedScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var st by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var r by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    fun go(s: Int) { st = s; k++ }
    val back = { if (st == 0) ctx.back() else go(if (st == 3) 0 else st - 1) }
    BackHandler(st != 0) { back() }

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = back)
            Text(listOf("Новый чат", "Ограничение", "Апелляция", "Готово")[st], style = ts(20, W800), modifier = Modifier.weight(1f))
        }
        key(k) {
            val anim = Modifier.chIn(dy = 10.dp, durationMs = 300, easing = FastOutSlowInEasing)
            when (st) {
                0 -> Column(Modifier.weight(1f).then(anim)) {
                    Row(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.warning.copy(alpha = .08f))
                        .border(1.dp, Kx.warning.copy(alpha = .3f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.FrontHand, null, tint = Kx.warning)
                        Column(Modifier.weight(1f)) {
                            Text("Пока можно писать только взаимным контактам", style = ts(14, W800))
                            Text("Telegram временно ограничил сообщения незнакомым людям. Группы и каналы работают как обычно.", style = ts(12, color = Kx.text2, lh = 16.8.sp), modifier = Modifier.padding(top = 2.dp))
                            Box(Modifier.padding(top = 8.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.warning.copy(alpha = .16f)).kxClick { go(1) }.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center) { Text("Почему и надолго ли?", style = ts(12, W800, hex("#F2C66B"))) }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        LetterAv("М", hex("#5C6661"), 60.dp, 22)
                        Text("Мария · объявление о велосипеде", style = ts(16, W800), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                        Text("не в контактах", style = ts(13, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp), tint = Kx.text4); Text("Отправка ограничена", style = ts(14, color = Kx.text4))
                    }
                }
                1 -> Column(Modifier.weight(1f).then(anim).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).background(Kx.warning.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Hourglass() }
                        Text("Снимется 29 ноября", style = ts(19, W800), modifier = Modifier.padding(top = 10.dp))
                        Text("Дата — из ответа @SpamBot", style = ts(13, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    }
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.08f)))
                    Row(Modifier.padding(top = 6.dp)) { Text("22 ноября", style = ts(11, color = Kx.text4), modifier = Modifier.weight(1f)); Text("29 ноября", style = ts(11, color = Kx.text4)) }
                    Text("Почему так бывает", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Icons.Outlined.Report to "Несколько человек пожаловались на сообщения от вас как на спам.",
                            Icons.Outlined.Speed to "Много сообщений незнакомым людям за короткое время.",
                            Icons.Outlined.NewReleases to "Новые аккаунты иногда ограничивают автоматически.").forEach { (ic, t) ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(ic, null, tint = Kx.text3); Text(t, style = ts(13, color = Kx.text2, lh = 18.2.sp), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Text("Что можно делать", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Писать контактам", "Отвечать, если написали вам", "Группы и каналы", "Звонки контактам").forEach {
                            Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .1f)).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = Kx.accent); Text(it, style = ts(12, W700, Kx.accent))
                            }
                        }
                    }
                    Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Kx.w(.08f)).kxClick { go(2) }, contentAlignment = Alignment.Center) {
                        Text("Это ошибка — подать апелляцию", style = ts(15, W800))
                    }
                }
                2 -> Column(Modifier.weight(1f).then(anim).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Text("Коротко расскажите, как вы пользуетесь Telegram. Апелляцию прочитают модераторы Telegram, не kiroX.", style = ts(14, color = Kx.text2, lh = 20.3.sp))
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LimitedReasons.forEachIndexed { i, t ->
                            val on = r == i
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
                                .border(1.5.dp, if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .kxClick(t, role = androidx.compose.ui.semantics.Role.RadioButton) { r = i }.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BRadio(on, size = 20.dp, dot = 8.dp); Text(t, style = ts(14, W700))
                            }
                        }
                    }
                    BTextArea(note, { note = it }, "Подробности (необязательно)", 90.dp, Modifier.padding(top = 10.dp))
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                        // demo: needs store (spam appeal: send via @SpamBot / account.reportSpamLimit is not in KxStore)
                        if (demo) go(3) else ctx.toast("Апелляцию пока можно подать только в чате @SpamBot")
                    }, contentAlignment = Alignment.Center) { Text("Отправить апелляцию", style = ts(16, W800, Kx.onAccent)) }
                }
                else -> Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    PopBadge(Icons.Rounded.MarkEmailRead, 1.6f, .6f, 1800)
                    Text("Апелляция отправлена", style = ts(20, W800), modifier = Modifier.padding(top = 14.dp))
                    Text("Ответ придёт в чат @SpamBot, обычно в течение нескольких дней.", style = ts(14, color = Kx.text3, lh = 20.3.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Box(Modifier.padding(top = 20.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).kxClick { go(0) }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                        Text("К чату", style = ts(14, W700, Kx.accent))
                    }
                }
            }
        }
    }
}

/** hourglass_bottom 40 with kxSway (±6°, 2.4s, twice). */
@Composable
private fun Hourglass() {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(2400, easing = LinearEasing)) } }
    val rot = -6f * kotlin.math.cos(2 * Math.PI * p.value).toFloat()
    Icon(Icons.Rounded.HourglassBottom, null, Modifier.size(40.dp).graphicsLayer { rotationZ = if (reduced || p.value >= 1f) 0f else rot }, tint = Kx.warning)
}
