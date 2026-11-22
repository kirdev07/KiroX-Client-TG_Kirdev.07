package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PollBase = listOf(9, 5, 7, 3, 4, 2, 6, 1, 3, 2)
private val ConfColors = listOf(Color(0xFF8FD3AC), Color(0xFFE8B04A), Color(0xFFFF8A80), Color(0xFF9FB7FF), Color(0xFFF2A65A), Color(0xFFDCD0FF))

/** Board «Опрос»: poll / quiz creator, then the poll bubble with voting, results bars, shake and confetti. */
@Composable
fun PollScreen(ctx: KxScreenCtx) {
    var q by remember { mutableStateOf("Когда удобнее заниматься?") }
    val opts = remember { mutableStateListOf("Утром", "Днём", "Вечером") }
    var anon by remember { mutableStateOf(true) }
    var multi by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(-1) }
    var sent by remember { mutableStateOf(false) }
    val filled = opts.count { it.isNotBlank() }
    val canCreate = q.isNotBlank() && filled >= 2 && (!quiz || (correct >= 0 && opts.getOrNull(correct)?.isNotBlank() == true))
    BackHandler(sent) { sent = false }

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding()) {
        if (!sent) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.Outlined.Close, "Закрыть", onClick = ctx.back)
                Text(if (quiz) "Викторина" else "Опрос", style = ts(22, W800), modifier = Modifier.weight(1f))
                val bg by animateColorAsState(if (canCreate) Kx.accent else Kx.w(.07f), tween(200), label = "cb")
                val ink by animateColorAsState(if (canCreate) Kx.onAccent else Color(0xFF5C6661), tween(200), label = "ci")
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(bg).kxClick { if (canCreate) sent = true }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center) { Text("Создать", style = ts(14, W700, ink)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                ChCaption("Вопрос")
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    ChInput(q, { q = it }, "Задайте вопрос", Modifier.fillMaxWidth(), size = 16)
                }
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Варианты", style = ts(13, W800, Kx.accent), modifier = Modifier.weight(1f))
                    Text("${opts.size} из 10", style = ts(12, color = Kx.text4))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f))) {
                    opts.forEachIndexed { i, text ->
                        key(i) {
                            Row(Modifier.fillMaxWidth().height(54.dp).chIn(dy = 10.dp, durationMs = 250).topLine(i > 0).padding(start = 14.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (quiz) {
                                    val on = correct == i
                                    val ring by animateColorAsState(if (on) Kx.accent else Color(0xFF5C6661), tween(200), label = "r")
                                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent).border(2.dp, ring, CircleShape)
                                        .kxClick("Правильный ответ") { correct = i }, contentAlignment = Alignment.Center) {
                                        if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent)
                                    }
                                } else Icon(Icons.Outlined.DragIndicator, null, tint = Kx.text4)
                                ChInput(text, { opts[i] = it }, "Вариант ответа", Modifier.weight(1f))
                                Box(Modifier.size(36.dp).alpha(if (opts.size > 2) 1f else .3f).clip(CircleShape).kxClick("Удалить вариант") {
                                    if (opts.size > 2) { opts.removeAt(i); correct = if (correct == i) -1 else if (correct > i) correct - 1 else correct }
                                }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.RemoveCircleOutline, null, Modifier.size(20.dp), tint = Kx.text4) }
                            }
                        }
                    }
                    if (opts.size < 10) Row(Modifier.fillMaxWidth().height(52.dp).topLine(true).kxClick { opts += "" }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.AddCircle, null, tint = Kx.accent); Text("Добавить вариант", style = ts(15, W600, Kx.accent))
                    }
                }
                if (quiz && correct < 0) Text("Отметьте правильный ответ кружком слева", style = ts(12, color = Kx.warning), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp))
                ChCaption("Настройки", Modifier.padding(top = 16.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f))) {
                    PollToggle(Icons.Outlined.VisibilityOff, "Анонимный опрос", "Никто не увидит, кто как проголосовал", anon, false, true) { anon = !anon }
                    PollToggle(Icons.Outlined.Checklist, "Несколько ответов", "Можно выбрать больше одного варианта", multi, true, !quiz) { if (!quiz) multi = !multi }
                    PollToggle(Icons.Outlined.School, "Викторина", "Один правильный ответ, его увидят после голоса", quiz, true, true) { quiz = !quiz; multi = false }
                }
            }
        } else {
            // demo: needs store (sendPoll / votePoll are not in KxStore yet; the bubble is a local preview)
            val h = ctx.arg?.toLongOrNull()?.let { ctx.store.header(it) }
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp) { sent = false }
                KxAvatar(h?.title ?: "Курс английского", h?.chatId ?: 0, size = 38.dp, colorIndex = h?.colorIndex?.takeIf { it >= 0 } ?: 0)
                Column { Text(h?.title ?: "Курс английского", style = ts(16, W800)); Text(h?.status ?: "24 участника", style = ts(12, color = Kx.text3)) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .18f)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                PollBubble(q, opts.mapIndexedNotNull { i, t -> if (t.isNotBlank()) i to t.trim() else null }, anon, multi, quiz, correct)
            }
        }
    }
}

private fun Modifier.topLine(on: Boolean) = if (!on) this else drawBehind { drawLine(Color.White.copy(alpha = .06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }

@Composable
private fun PollToggle(icon: ImageVector, label: String, sub: String, on: Boolean, line: Boolean, enabled: Boolean, tap: () -> Unit) {
    Row(Modifier.fillMaxWidth().topLine(line).alpha(if (enabled) 1f else .4f).kxClick(label, onClick = tap).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Kx.accent)
        Column(Modifier.weight(1f)) { Text(label, style = ts(15, W600)); Text(sub, style = ts(12, color = Kx.text3), modifier = Modifier.padding(top = 2.dp)) }
        KxSwitch(on, width = 50.dp, height = 30.dp, knob = 24.dp, trackOn = Kx.accent, trackOff = Kx.w(.12f), knobOn = Kx.onAccent, knobOff = Kx.text4)
    }
}

@Composable
private fun PollBubble(q: String, shown: List<Pair<Int, String>>, anon: Boolean, multi: Boolean, quiz: Boolean, correct: Int) {
    var picked by remember { mutableStateOf(listOf<Int>()) }
    var voted by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val reduced = LocalKxReducedMotion.current
    val counts = shown.mapIndexed { k, (i, _) -> PollBase[k % PollBase.size] + if (voted && i in picked) 1 else 0 }
    val total = counts.sum()
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
    Box(Modifier.width(300.dp).graphicsLayer {
        alpha = appear.value; translationY = (1 - appear.value) * 40.dp.toPx(); val s = .95f + .05f * appear.value; scaleX = s; scaleY = s
        translationX = shake.value * 6.dp.toPx()
    }.clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).cssGradient(160f, Color(0xFF23523B), Color(0xFF183A2A)).padding(14.dp)) {
        Column {
            Text(q, style = ts(16, W800, lh = 20.8.sp))
            Text((if (anon) "Анонимный " else "Открытый ") + (if (quiz) "тест" else "опрос") + (if (multi) " · несколько ответов" else ""),
                style = ts(12, color = Color(0xFFA9D9BF)), modifier = Modifier.padding(top = 3.dp))
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shown.forEachIndexed { k, (i, text) ->
                    val pick = i in picked
                    val pct = if (total > 0) Math.round(counts[k] * 100f / total) else 0
                    val isCorrect = quiz && i == correct
                    var bar = Color.Transparent; var ring = Color(0xFFA9D9BF); var dot = Color.Transparent; var icon: ImageVector? = null
                    if (voted) {
                        bar = if (quiz) (if (isCorrect) Kx.accent.copy(alpha = .35f) else if (pick) Kx.danger.copy(alpha = .3f) else Kx.w(.08f)) else (if (pick) Kx.accent.copy(alpha = .35f) else Kx.w(.1f))
                        if (pick || isCorrect) { val wrong = quiz && pick && !isCorrect; dot = if (wrong) Kx.danger else Kx.accent; ring = dot; icon = if (wrong) Icons.Rounded.Close else Icons.Rounded.Check }
                    } else if (pick) { ring = Kx.accent; dot = Kx.accent; icon = Icons.Rounded.Check }
                    val w by animateFloatAsState(if (voted) pct / 100f else 0f, tween(700, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "w")
                    val ringA by animateColorAsState(ring, tween(200), label = "ra")
                    val dotA by animateColorAsState(dot, tween(200), label = "da")
                    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .18f))
                        .drawBehind { drawRect(bar, size = size.copy(width = size.width * w)) }
                        .kxClick(text) {
                            if (voted) return@kxClick
                            if (multi) { picked = if (pick) picked - i else picked + i; return@kxClick }
                            val wrong = quiz && i != correct
                            picked = listOf(i); voted = true
                            if (wrong && !reduced) scope.launch { delay(100); repeat(1) { for (x in listOf(-1f, 1f, 0f)) shake.animateTo(x, tween(100)) } }
                            if (quiz && !wrong) confetti++
                        }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(if (multi) 6.dp else 11.dp)).background(dotA).border(2.dp, ringA, RoundedCornerShape(if (multi) 6.dp else 11.dp)),
                            contentAlignment = Alignment.Center) { icon?.let { key(it) { Icon(it, null, Modifier.size(15.dp).chPop(.6f), tint = Kx.onAccent) } } }
                        Text(text, style = ts(14, W600), modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                        if (voted) Text("$pct%", style = ts(13, W800), modifier = Modifier.chIn(dy = 10.dp))
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (total > 0) "$total ${plural(total, "голос", "голоса", "голосов")}" else "Пока нет голосов", style = ts(12, color = Color(0xFFA9D9BF)), modifier = Modifier.weight(1f))
                if (multi && !voted && picked.isNotEmpty()) Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent).kxClick { voted = true }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) { Text("Проголосовать", style = ts(12, W800, Kx.onAccent)) }
                if (voted && !quiz) Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(alpha = .2f)).kxClick { voted = false; picked = emptyList() }
                    .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("Отменить голос", style = ts(12, W700)) }
            }
        }
        if (confetti > 0 && !reduced) key(confetti) { Confetti(Modifier.matchParentSize()) }
    }
}

/** kxConf: 12 bits fly up 120dp rotating 300°, 1.1s, staggered .06s. */
@Composable
private fun Confetti(modifier: Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(1100 + 180, easing = LinearEasing)) }
    Box(modifier.drawBehind {
        repeat(12) { i ->
            val d = (i % 4) * 60f / 1280f
            val t = ((p.value - d) / (1100f / 1280f)).coerceIn(0f, 1f)
            val e = 1 - (1 - t) * (1 - t)
            if (t <= 0f || t >= 1f) return@repeat
            val x = (20 + i * 22).dp.toPx() - 14.dp.toPx(); val y = size.height - 40.dp.toPx() - 8.dp.toPx() - e * 120.dp.toPx()
            val c = ConfColors[i % ConfColors.size].copy(alpha = 1 - e)
            rotate(300f * e, Offset(x + 4.dp.toPx(), y + 4.dp.toPx())) {
                drawRoundRect(c, Offset(x, y), androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
    })
}
