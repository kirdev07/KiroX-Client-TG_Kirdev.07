package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Board demo (no chat id). With "SlowMode:<chatId>": the interval is KxGroupInfo.slowModeSeconds and the header is the chat's.
// demo: needs store (seconds left for the current member are not in the store yet; the chat and sending stay the board's local preview —
// nothing is sent to Telegram from here)
private data class SMsg(val who: String?, val nc: Color, val text: String, val t: String, val me: Boolean)
private val SBase = listOf(
    SMsg("Дмитрий", Kx.accent, "Кто разбирался с Compose Multiplatform на iOS?", "17:31", false),
    SMsg("Айгерим", Kx.warning, "Пробую сейчас: пока сыровато, но жить можно", "17:32", false),
    SMsg("Сергей", Color(0xFF7FB8E0), "Есть пример с навигацией?", "17:33", false),
)
private val SMine = listOf("Могу скинуть свой шаблон с навигацией 👇", "github.com/… там README с шагами", "Если что — пишите в личку")
private const val SInt = 60

private fun slowWord(s: Int): String = when {
    s == 60 -> "минуту"
    s < 60 -> "$s ${gxPlural(s, "секунду", "секунды", "секунд")}"
    s < 3600 -> (s / 60).let { "$it ${gxPlural(it, "минуту", "минуты", "минут")}" }
    else -> (s / 3600).let { if (it == 1) "час" else "$it ${gxPlural(it, "час", "часа", "часов")}" }
}

/** Board «Медленный режим» (member side): snail banner, chat, locked composer with countdown ring, nudge + tip on early send. */
@Composable
fun SlowModeScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val header = chatId?.let { store.header(it) }
    val info = gxLoad(chatId) { chatId?.let { store.groupInfo(it) } }
    val interval = info?.slowModeSeconds?.takeIf { it > 0 } ?: SInt
    val mine = remember { mutableStateListOf<SMsg>() }
    var left by remember { mutableIntStateOf(0) }
    var nk by remember { mutableIntStateOf(0) }
    var tk by remember { mutableIntStateOf(0) }
    var tip by remember { mutableStateOf(false) }
    val locked = left > 0
    val nextText = SMine[mine.size % SMine.size]
    fun mmss(n: Int) = "${n / 60}:" + (n % 60).toString().padStart(2, '0')
    LaunchedEffect(mine.size) { if (mine.isNotEmpty()) { left = interval; while (left > 0) { delay(1000); left-- } } }
    LaunchedEffect(tk) { if (tk > 0) { tip = true; delay(2200); tip = false } }
    val reduced = LocalKxReducedMotion.current

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().hairline().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Box(Modifier.size(40.dp).clip(CircleShape).background(Kx.accentDeep), contentAlignment = Alignment.Center) { Text((header?.title ?: "K").take(1).uppercase(), style = KxType.style(16.sp, FontWeight.ExtraBold)) }
            Column(Modifier.weight(1f)) {
                Text(header?.title ?: "Kotlin-клуб", style = KxType.style(16.sp, FontWeight.ExtraBold))
                Text(header?.status ?: "1 284 участника, 96 в сети", style = KxType.style(12.sp, color = Kx.text3))
            }
        }
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x14E8B04A))
            .border(1.dp, Color(0x40E8B04A), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val snail = remember { Animatable(0f) }
            LaunchedEffect(Unit) { if (!reduced) repeat(2) { snail.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)); snail.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) } }
            Text("🐌", style = KxType.style(20.sp), modifier = Modifier.graphicsLayer { translationX = snail.value * 3.dp.toPx() })
            Text("Медленный режим: одно сообщение раз в " + slowWord(interval) + ". Так в чате спокойнее.", style = KxType.style(12.sp, color = Color(0xFFE8D2A0), lineHeight = 16.2.sp), modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val minH = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true)) {
                    Column(Modifier.fillMaxWidth().heightIn(min = minH).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                        (SBase + mine).takeLast(6).forEach { m ->
                            key(m.text + m.t) {
                                Column(Modifier.fillMaxWidth(.8f).align(if (m.me) Alignment.End else Alignment.Start).then(if (m.me) Modifier.gxBubbleIn() else Modifier),
                                    horizontalAlignment = if (m.me) Alignment.End else Alignment.Start) {
                                    if (!m.me && m.who != null) Text(m.who, style = KxType.style(12.sp, FontWeight.ExtraBold, m.nc), modifier = Modifier.padding(start = 10.dp, bottom = 2.dp))
                                    val shape = if (m.me) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                                    val ink = if (m.me) Kx.onAccent else Kx.text
                                    Text(buildAnnotatedString {
                                        append(m.text); append("  ")
                                        withStyle(SpanStyle(fontSize = 11.sp, color = ink.copy(alpha = .6f))) { append(m.t) }
                                    }, style = KxType.style(15.sp, color = ink, lineHeight = 21.sp), modifier = Modifier.clip(shape)
                                        .then(if (m.me) Modifier.cssLinear(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f))).padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
            SlowTip(tip, "Подождите ещё ${mmss(left)} — в группе медленный режим")
        }
        Row(Modifier.fillMaxWidth().hairline(top = true).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val nudge = remember { Animatable(0f) }
            LaunchedEffect(nk) { if (nk > 0 && !reduced) { nudge.snapTo(0f); nudge.animateTo(1f, tween(400)) } }
            val inBg by animateColorAsState(if (locked) Color(0x0FE8B04A) else Kx.w(.07f), tween(300), label = "ib")
            Box(Modifier.weight(1f).heightIn(min = 48.dp).graphicsLayer { translationX = nudgeX(nudge.value) * density }.clip(RoundedCornerShape(24.dp)).background(inBg)
                .border(1.dp, if (locked) Color(0x33E8B04A) else Color.Transparent, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Text(if (locked) "Следующее сообщение через ${mmss(left)}" else nextText, style = KxType.style(15.sp, color = if (locked) Kx.text4 else Kx.text), maxLines = 1)
            }
            val ready = remember { Animatable(1f) }
            LaunchedEffect(locked) { if (!locked && mine.isNotEmpty() && !reduced) { ready.snapTo(0f); ready.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing)) } }
            val ring by animateFloatAsState(if (locked) left.toFloat() / interval else 0f, tween(1000, easing = LinearEasing), label = "ring")
            Box(Modifier.size(52.dp).drawBehind {
                if (ready.value < 1f) drawCircle(Kx.accent.copy(alpha = .6f * (1f - ready.value)), size.minDimension / 2 + 14.dp.toPx() * ready.value)
            }.clip(CircleShape).then(if (locked) Modifier.background(Kx.w(.06f)) else Modifier.cssLinear(135f, Kx.accentMid, Kx.accentDeep))
                .kxClick(if (locked) "Медленный режим, осталось ${mmss(left)}" else "Отправить") {
                    if (locked) { nk++; tk++ } else mine.add(SMsg(null, Kx.accent, nextText, "17:" + (34 + mine.size).toString().padStart(2, '0'), true))
                }, contentAlignment = Alignment.Center) {
                if (locked) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 3.dp.toPx(); val r = 23.dp.toPx()
                        val tl = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r); val sz = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                        drawCircle(Color(0x2EE8B04A), r, style = Stroke(sw))
                        drawArc(Kx.warning, -90f, 360f * ring, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Text("$left", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.warning).copy(fontFeatureSettings = "tnum"))
                } else Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = Kx.onAccent)
            }
        }
    }
}


@Composable
private fun BoxScope.SlowTip(tip: Boolean, text: String) {
    AnimatedVisibility(tip, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { 8 }, exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { 8 }) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xF828302C)).border(1.dp, Color(0x4DE8B04A), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp), tint = Kx.warning)
            Text(text, style = KxType.style(13.sp, FontWeight.Bold))
        }
    }
}

/** kxNudge keyframes: 0 → -6 → 6 → -4 → 4 → 0 (dp). */
private fun nudgeX(v: Float): Float {
    val pts = floatArrayOf(0f, -6f, 6f, -4f, 4f, 0f)
    if (v <= 0f || v >= 1f) return 0f
    val seg = v / .2f; val i = seg.toInt().coerceAtMost(4); val f = seg - i
    return pts[i] + (pts[i + 1] - pts[i]) * f
}

/** kxIn of chat bubbles: fade + 12dp lift + scale .96, .3s rise. */
@Composable
private fun Modifier.gxBubbleIn(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(300, easing = KxMotion.rise)) }
    return graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 12.dp.toPx(); val s = .96f + .04f * p.value; scaleX = s; scaleY = s }
}

@Suppress("unused") private val keepBrush: Brush = SolidColor(Color.Transparent)
