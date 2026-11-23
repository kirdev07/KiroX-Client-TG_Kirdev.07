package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class ImApp(val label: String, val sub: String, val icon: ImageVector, val c: Color)
// critique-B P3: WhatsApp and «другое» only (LINE is not used by this audience); «архив чата», not file extensions.
private val IM_APPS = listOf(ImApp("WhatsApp", "Архив чата из «Экспорт чата»", Icons.Rounded.Chat, Color(0xFF2E8B57)),
    ImApp("Другое приложение", "Переписка, сохранённая в текстовый файл", Icons.Rounded.Description, Color(0xFF5C6661)))
private val IM_TARGETS = listOf(listOf("Мама", "М", "Последнее сообщение вчера") to Color(0xFFB07A2E), listOf("Семья", "С", "Группа · 5 участников") to Color(0xFF2F6E9E),
    listOf("Новый чат", "+", "Создать отдельный") to Color(0xFF3F9A6C))
private const val IM_TOTAL = 4812

internal fun ruNum(n: Int): String = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("ru-RU")).format(n).replace(' ', ' ').replace(' ', ' ')

/** Board «Перенос переписки»: 4 steps — source app, how to export, target chat, progress with flying messages. */
@Composable
fun ImportScreen(ctx: KxScreenCtx) {
    // wave3: stays demo on the real store (honest toast) — needs store (importMessages: checkChatsImport / importMessages from a shared file; real file comes via the share intent) — file and progress are demo
    var step by remember { mutableIntStateOf(0) }
    var app by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(0) }
    var prog by remember { mutableFloatStateOf(0f) }
    var runId by remember { mutableIntStateOf(0) }
    LaunchedEffect(runId) { if (runId == 0) return@LaunchedEffect; prog = 0f; while (prog < 100f) { delay(120); prog = minOf(100f, prog + 2f + (Math.random() * 5).toFloat()) } }
    val done = step == 3 && prog >= 100f
    val running = step == 3 && !done
    val back: () -> Unit = { if (step > 0 && !running) { if (done) { step = 0; prog = 0f } else step-- } else if (step == 0) ctx.back() }
    androidx.activity.compose.BackHandler(step > 0) { back() }
    val a = IM_APPS[app]

    Column(Modifier.fillMaxSize().background(Kx.bg)) {
        THeader("Перенос переписки", back, titleSize = 20)
        StepBars(step, 4)
        // Animated scene
        val sShape = RoundedCornerShape(24.dp)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth().height(130.dp).clip(sShape).background(Kx.w(.03f))
            .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .2f), .7f to Color.Transparent, center = Offset(size.width / 2, 0f), radius = size.width * .8f)) }
            .border(1.dp, Kx.w(.08f), sShape)) {
            val b1 = tCycles(3000, 2, easing = TEaseInOut)
            val bob = { p: Float -> -4f * (1f - kotlin.math.cos(p * 2 * Math.PI).toFloat()) / 2f }
            Box(Modifier.offset(34.dp, 33.dp).size(64.dp).graphicsLayer { translationY = bob(b1) * density }.clip(RoundedCornerShape(20.dp)).background(a.c), contentAlignment = Alignment.Center) {
                Icon(a.icon, null, Modifier.size(32.dp), tint = Color.White)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(end = 34.dp, top = 33.dp).size(64.dp).graphicsLayer { translationY = bob(if (b1 > 0f) (b1 + .5f) % 1f else 0f) * density }
                .clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                Text("kX", style = KxType.style(22.sp, FontWeight.Black, Kx.onAccent))
            }
            if (running) {
                listOf(Triple(Icons.Rounded.ChatBubble, Kx.accent, 30) to 0, Triple(Icons.Rounded.Image, Color(0xFF6FA9D8), 62) to 350,
                    Triple(Icons.Rounded.Mic, Kx.warning, 44) to 700, Triple(Icons.Rounded.ChatBubble, Kx.text, 78) to 1050).forEach { (t, d) ->
                    val p = tCycles(1400, 2, delayMs = d, rest = 1f, key = runId)
                    val al = if (p >= 1f) 0f else if (p < .15f) p / .15f else if (p > .85f) (1f - p) / .15f else 1f
                    Icon(t.first, null, Modifier.offset(110.dp, t.third.dp).size(20.dp).graphicsLayer { translationX = 150f * density * p; scaleX = .8f; scaleY = .8f; alpha = al }, tint = t.second)
                }
            } else Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.align(Alignment.Center).size(30.dp), tint = Kx.text4)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (step) {
                0 -> Column(Modifier.fillMaxSize().slideIn(0).padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
                    Text("Откуда переносим?", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text("Сообщения и фото окажутся в чате kiroX с той же датой", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IM_APPS.forEachIndexed { i, x ->
                            PickRow(app == i, { app = i }) {
                                TIconTile(x.icon, Color.White, x.c, 42.dp, 13.dp, 24.dp)
                                Column(Modifier.weight(1f)) { Text(x.label, style = KxType.style(15.sp, FontWeight.Bold)); Text(x.sub, style = KxType.style(12.sp, color = Kx.text4)) }
                                TRadio(app == i, off = Kx.w(.3f))
                            }
                        }
                    }
                }
                1 -> Column(Modifier.fillMaxSize().slideIn(1).padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
                    Text("Как выгрузить чат", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    val how = when (app) {
                        0 -> listOf("Откройте чат в WhatsApp и нажмите ⋮ → «Ещё»", "Выберите «Экспорт чата» и «С медиафайлами»", "В списке приложений выберите kiroX")
                        else -> listOf("Сохраните переписку в текстовый файл", "Положите его в «Загрузки»", "Вернитесь сюда и выберите файл")
                    }
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { how.forEachIndexed { i, t -> NumStep(i + 1, t) } }
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.accent)
                        Text("Можно и проще: просто нажмите «Поделиться» в том приложении и выберите kiroX.", style = KxType.style(12.sp, color = Kx.text2, lineHeight = 17.sp))
                    }
                }
                2 -> Column(Modifier.fillMaxSize().slideIn(2).padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
                    Text("Файл найден", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.FolderZip, null, Modifier.size(30.dp), tint = Kx.accent)
                        Column { Text("Чат с Мамой.zip", style = KxType.style(14.sp, FontWeight.Bold)); Text("4 812 сообщений · 312 фото · 2019–2026", style = KxType.style(12.sp, color = Kx.text4)) }
                    }
                    Text("В какой чат kiroX", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                    IM_TARGETS.forEachIndexed { i, (t, c) ->
                        Box(Modifier.padding(bottom = 6.dp)) {
                            PickRow(target == i, { target = i }, 16.dp, 10.dp, 12.dp) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) { Text(t[1], style = KxType.style(16.sp, FontWeight.ExtraBold)) }
                                Column(Modifier.weight(1f)) { Text(t[0], style = KxType.style(15.sp, FontWeight.Bold)); Text(t[2], style = KxType.style(12.sp, color = Kx.text4)) }
                                if (target == i) PopIcon(Icons.Rounded.CheckCircle, 24.dp)
                            }
                        }
                    }
                }
                else -> Column(Modifier.fillMaxSize().slideIn(3).padding(start = 24.dp, end = 24.dp, top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (done) DoneBadge(76.dp, 42.dp)
                    Text(if (done) "Готово!" else "Переносим…", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 14.dp))
                    Text(if (done) "Переписка теперь в чате «${IM_TARGETS[target].first[0]}»" else "Можно свернуть kiroX, перенос продолжится", style = KxType.style(14.sp, color = Kx.text3),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    val w by animateFloatAsState(prog / 100f, tween(300, easing = LinearEasing), label = "w")
                    Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.08f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                    }
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        val st = KxType.style(12.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum")
                        Text(ruNum(Math.round(IM_TOTAL * prog / 100f)) + " из " + ruNum(IM_TOTAL) + " сообщений", style = st, modifier = Modifier.weight(1f))
                        Text("${Math.round(prog)}%", style = st)
                    }
                    Text("Перенесённые сообщения помечены значком «импорт» и видны обоим", style = KxType.style(12.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                }
            }
        }
        Box(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            TPill(when (step) { 0 -> "Дальше"; 1 -> "Файл готов"; 2 -> "Перенести"; else -> if (done) "Открыть чат" else "Переносим…" }, Modifier.fillMaxWidth(), h = 54.dp,
                bg = if (running) solid(Kx.w(.07f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = if (running) Color(0xFF5C6661) else Kx.onAccent, size = 16) {
                when { step < 2 -> step += 1; step == 2 -> { if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) ctx.toast("Перенос чатов в kiroX v3 пока не готов — это предпросмотр") else { step = 3; runId += 1 } }; done -> { step = 0; prog = 0f } }
            }
        }
    }
}

/** Progress segments: 4dp, gap 6, accent up to [step], w.10 after; padding 4 20 0. */
@Composable
internal fun StepBars(step: Int, n: Int) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(n) { i ->
            val c by animateColorAsState(if (i <= step) Kx.accent else Kx.w(.1f), tween(300), label = "st")
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
        }
    }
}

/** kxIn of wizard steps: slide 24dp from the right + fade .3s. */
@Composable
internal fun Modifier.slideIn(key: Int): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(300, easing = TEase)) }
    return this.graphicsLayer { alpha = p.value; translationX = (1f - p.value) * 24.dp.toPx() }
}

/** Selectable row: accent .10 / w.04 fill, accent .45 / w.08 border. */
@Composable
internal fun PickRow(on: Boolean, onClick: () -> Unit, radius: Dp = 18.dp, vPad: Dp = 12.dp, hPad: Dp = 14.dp, content: @Composable RowScope.() -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "pb")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "pd")
    val sh = RoundedCornerShape(radius)
    Row(Modifier.fillMaxWidth().clip(sh).background(bg).border(1.dp, bd, sh).kxClick(role = Role.RadioButton, onClick = onClick).padding(horizontal = hPad, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Numbered how-to step: 30 circle accent .14, 14sp text #C9D2CC. */
@Composable
internal fun NumStep(n: Int, t: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Text("$n", style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent)) }
        Text(t, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp))
    }
}

/** kxPop icon (scale .5 → 1.12 → 1). */
@Composable
internal fun PopIcon(icon: ImageVector, size: Dp, tint: Color = Kx.accent, durMs: Int = 250) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durMs, easing = TEase)) }
    val s = kf(p.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
    Icon(icon, null, Modifier.size(size).graphicsLayer { scaleX = s; scaleY = s; alpha = (p.value / .6f).coerceAtMost(1f) }, tint = tint)
}

/** Gradient check disc that pops in. */
@Composable
internal fun DoneBadge(size: Dp, icon: Dp) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(450, easing = TEase)) }
    val s = kf(p.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
    Box(Modifier.size(size).graphicsLayer { scaleX = s; scaleY = s; alpha = (p.value / .6f).coerceAtMost(1f) }.clip(CircleShape)
        .background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, null, Modifier.size(icon), tint = Kx.onAccent)
    }
}
