package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import kotlinx.coroutines.delay

private val DcPh = mapOf(
    "ru" to listOf("Привет,", "мам!", "Приеду", "в", "субботу", "к", "обеду,", "часам", "к", "двум.", "Испеки", "свой", "пирог,", "пожалуйста", "🙂"),
    "kk" to listOf("Сәлем,", "апа!", "Сенбі", "күні", "түсте", "келемін.", "Бәлішіңді", "пісірші", "🙂"),
)

/**
 * Board «Диктовка»: dictation panel (state, RU/KZ, live words with blur-in + caret, waveform, mic orb with ring, erase / send).
 * The recognised words are a simulation, so they only play on the demo store; a real account shows the idle panel.
 */
@Composable
fun DictateScreen(ctx: KxScreenCtx) {
    // demo: needs store (on-device speech recognition → words + level; send as text)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var lang by remember { mutableStateOf("ru") }
    var n by remember { mutableIntStateOf(0) }
    var on by remember { mutableStateOf(demo) }
    var t by remember { mutableIntStateOf(0) }
    val sent = remember { mutableStateListOf<String>() }
    val ph = DcPh.getValue(lang)
    LaunchedEffect(on, lang) { if (demo) while (on) { delay(520); if (n < ph.size) n++ else on = false } }
    LaunchedEffect(on) { while (on) { delay(120); t++ } }
    val words = ph.take(n)
    val can = words.isNotEmpty()
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter("М", 40.dp, Color(0xFFB07A2E), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Мама", style = ts(16.sp, FontWeight.ExtraBold)); Text("в сети", style = ts(12.sp, color = Kx.accent)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(androidx.compose.ui.text.buildAnnotatedString {
                            append("Ты когда приедешь в субботу? ")
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 11.sp, color = Kx.text4)); append("17:52"); pop()
                        }, style = ts(15.sp))
                    }
                    sent.forEachIndexed { i, m ->
                        key(i) {
                            val p = enter(350)
                            Box(Modifier.align(Alignment.End).fillMaxWidth(.82f).wrapContentWidth(Alignment.End).graphicsLayer { alpha = p; translationY = (1f - p) * 30.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
                                .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(androidx.compose.ui.text.buildAnnotatedString {
                                    append("$m ")
                                    pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 11.sp, color = Kx.onAccent.copy(alpha = .7f))); append("18:01 ✓✓"); pop()
                                }, style = ts(15.sp, color = Kx.onAccent))
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(Color(0xE628302C), Color(0xFA121614))))
                .insetTop(.14f).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ink = if (on) Kx.accent else Kx.text3
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ink))
                        Text(if (on) "Слушаю" else if (can) "Готово — можно отправить" else "Пауза", style = ts(12.sp, FontWeight.Bold, ink))
                    }
                    Spacer(Modifier.weight(1f))
                    listOf("Русский" to "ru", "Қазақша" to "kk").forEach { (label, k) ->
                        Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(if (lang == k) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .kxClick(label) { lang = k; n = 0; on = demo }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = ts(12.sp, FontWeight.Bold, if (lang == k) Kx.accent else Kx.text3))
                        }
                    }
                }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = .25f))
                    .border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    FlowRowWords(words, on)
                    if (!can && !on) Text("Говорите — текст появится здесь", style = ts(17.sp, color = Kx.text4, lh = 24.65.sp))
                }
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    repeat(30) { i ->
                        val target = if (on && demo) maxOf(4, Math.round(6 + Math.abs(Math.sin((t + i) * 0.7) * Math.cos((t - i) * 0.31)) * 28).toInt()) else 4
                        val h by animateDpAsState(target.dp, tween(120), label = "w$i")
                        Box(Modifier.size(4.dp, h).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(Kx.accent, Kx.accentDeep))))
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // critique-B: labelled controls — erase the last word, cancel the whole dictation.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExRoundBtn(Icons.AutoMirrored.Outlined.Backspace, "Стереть слово", size = 52.dp, bg = Kx.w(.07f), border = null, tint = Kx.text2) { n = (n - 1).coerceAtLeast(0) }
                        Text("Стереть слово", style = ts(11.sp, color = Kx.text3))
                    }
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        if (on) {
                            val r = kfLoop(1400, label = "ring")
                            Box(Modifier.matchParentSize().graphicsLayer { val s = .9f + 1f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .6f * (1f - r) }.border(2.dp, Kx.accent, CircleShape))
                        }
                        val orb = if (on) tri(kfRun(1200, key = on)) else 0f
                        val a by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "mic")
                        Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .08f * orb; scaleX = s; scaleY = s }.boxShadow(Kx.accentDeep.copy(alpha = .4f), 30.dp, 10.dp, 42.dp)
                            .clip(CircleShape).background(Kx.accent.copy(alpha = .2f)).drawBehind {
                                drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = a)
                            }.kxClick(if (on) "Пауза" else "Продолжить диктовку") {
                                if (!demo) { ctx.toast("Распознавание речи пока недоступно"); return@kxClick }
                                if (!on && n >= ph.size) n = 0
                                on = !on
                            }, contentAlignment = Alignment.Center) {
                            Icon(if (on) Icons.Rounded.Pause else Icons.Rounded.Mic, null, Modifier.size(38.dp), tint = if (on) Kx.onAccent else Kx.accent)
                        }
                    }
                    val sa by animateFloatAsState(if (can) 1f else 0f, tween(200), label = "send")
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(.07f)).drawBehind {
                        drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = sa)
                    }.kxClick("Отправить текстом") { if (can) { sent.add(words.joinToString(" ")); n = 0; on = false } }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = if (can) Kx.onAccent else Kx.text4)
                    }
                }
                Text("Распознаётся на телефоне · скажите «точка» или «вопрос»", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Text("Отмена", style = ts(14.sp, FontWeight.Bold, Kx.text2), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).clip(RoundedCornerShape(14.dp))
                    .kxClick("Отменить диктовку") { n = 0; on = false; ctx.back() }.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }
}

/** Words fading in from blur(4px) (kxWord .3s), the last two lighter while listening, then a 2×20 blinking caret. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowWords(words: List<String>, on: Boolean) {
    FlowRow(verticalArrangement = Arrangement.Center) {
        words.forEachIndexed { i, w ->
            key(i, w) {
                val p = enter(300)
                Text("$w ", style = ts(17.sp, color = if (i >= words.size - 2 && on) Color(0xFFC9E8D5) else Kx.text, lh = 24.65.sp),
                    modifier = Modifier.graphicsLayer { alpha = p }.blur((4f * (1f - p)).dp))
            }
        }
        if (on) {
            val c = kfLoop(1000, label = "caret")
            Box(Modifier.padding(top = 2.dp).size(2.dp, 20.dp).graphicsLayer { alpha = if (c < .5f) 1f else 0f }.background(Kx.accent))
        }
    }
}
