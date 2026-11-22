package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private const val VTotal = 760f
// demo: needs store (video message with chapter timecodes from the caption)
private val Chapters = listOf(Triple(0f, "Вступление", "👋"), Triple(95f, "Зачем нужны корутины", "🤔"), Triple(240f, "launch и async", "🚀"),
    Triple(430f, "Отмена и таймауты", "⏱"), Triple(610f, "Тесты и итоги", "✅"))
private val Speeds = listOf(.5f, 1f, 1.25f, 1.5f, 2f)
private fun mm2(n: Float) = "${(n / 60).toInt().toString().padStart(2, '0')}:${(n % 60).toInt().toString().padStart(2, '0')}"

/** Board «Видеоплеер»: video placeholder (slow pan), double-tap skip zones, chapter segments, speed, chapter list. */
@Composable
fun VideoPlayerScreen(ctx: KxScreenCtx) {
    var t by remember { mutableFloatStateOf(262f) }
    var play by remember { mutableStateOf(true) }
    var sp by remember { mutableIntStateOf(1) }
    var bk by remember { mutableIntStateOf(0) }
    var fk by remember { mutableIntStateOf(0) }
    LaunchedEffect(play, sp) { while (play) { delay(250); t = minOf(VTotal, t + Speeds[sp] * .25f) } }
    val ci = Chapters.indexOfLast { t >= it.first }.coerceAtLeast(0)
    fun seek(v: Float) { t = v.coerceIn(0f, VTotal) }
    val pan by rememberPlayClock(play)

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { ctx.back() }
            Column(Modifier.weight(1f)) { T("Kotlin по-русски", 15.sp, FontWeight.ExtraBold); T("21 ноября в 19:00", 12.sp, color = Kx.text3) }
            MRoundBtn(Icons.Outlined.PictureInPictureAlt, "Картинка в картинке", bg = Kx.w(.08f), border = null) { ctx.open("Pip") }
        }
        Box(Modifier.padding(top = 30.dp).fillMaxWidth().height(230.dp).drawBehind {
            // kxPan 18s linear infinite alternate over a 400%-wide gradient.
            val cyc = (pan % 36000) / 18000f
            val f = if (cyc <= 1f) cyc else 2f - cyc
            val w = size.width * 4; val x0 = -(w - size.width) * f
            val dx = 0.364f * size.height // 110deg slant
            drawRect(Brush.linearGradient(listOf(Color(0xFF0F3D2A), Color(0xFF1B2A3A), Kx.accentDeep, Color(0xFF0B1F17), Color(0xFF2F6E9E)),
                start = Offset(x0 - dx, 0f), end = Offset(x0 + w + dx, size.height)))
        }.clip(androidx.compose.ui.graphics.RectangleShape)) {
            Column(Modifier.align(Alignment.Center).graphicsLayer { alpha = .85f }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Chapters[ci].third, style = ts(54.sp))
                T(Chapters[ci].second, 13.sp, FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Row(Modifier.matchParentSize()) {
                SkipZone(Icons.Outlined.FastRewind, "Назад на 10 секунд", bk, Modifier.weight(.35f)) { seek(t - 10); bk++ }
                Spacer(Modifier.weight(.30f))
                SkipZone(Icons.Outlined.FastForward, "Вперёд на 10 секунд", fk, Modifier.weight(.35f)) { seek(t + 10); fk++ }
            }
            if (!play) Box(Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)).kxClick("Смотреть") { play = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(36.dp), tint = Color.White)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("Глава ${ci + 1} · ${Chapters[ci].second}", 13.sp, FontWeight.Bold, Kx.accent, modifier = Modifier.weight(1f), maxLines = 1)
                T("${mm2(t)} / 12:40", 13.sp, FontWeight.Bold, Kx.text2)
            }
            BoxWithConstraints(Modifier.padding(top = 10.dp).fillMaxWidth().height(18.dp)) {
                Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Chapters.forEachIndexed { i, c ->
                        val start = c.first; val end = Chapters.getOrNull(i + 1)?.first ?: VTotal
                        val f = if (t >= end) 1f else if (t <= start) 0f else (t - start) / (end - start)
                        val ff by animateFloatAsState(f, tween(250, easing = LinearEasing), label = "seg")
                        val h by animateDpAsState(if (i == ci) 7.dp else 4.dp, tween(150), label = "h")
                        Box(Modifier.weight(end - start).fillMaxHeight().kxClick("Глава ${i + 1}: ${c.second}") { seek(start) }, contentAlignment = Alignment.CenterStart) {
                            Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(3.dp)).background(Kx.w(.18f))) { Box(Modifier.fillMaxHeight().fillMaxWidth(ff).background(Kx.accent)) }
                        }
                    }
                }
                val k by animateFloatAsState(t / VTotal, tween(250, easing = LinearEasing), label = "k")
                Box(Modifier.offset(x = maxWidth * k - 7.dp, y = 2.dp).size(14.dp).drawBehind { drawCircle(Kx.accent.copy(alpha = .25f), size.minDimension / 2 + 4.dp.toPx()) }
                    .clip(CircleShape).background(Kx.text))
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PBtn(Icons.Rounded.SkipPrevious, "Предыдущая глава") { seek(if (t - Chapters[ci].first > 3) Chapters[ci].first else Chapters[maxOf(0, ci - 1)].first) }
                PBtn(Icons.Outlined.Replay10, "Назад на 10 секунд") { seek(t - 10); bk++ }
                Box(Modifier.size(64.dp).clip(CircleShape).cssGrad(135f, Kx.accentMid, Kx.accentDeep).kxClick(if (play) "Пауза" else "Смотреть") { play = !play }, contentAlignment = Alignment.Center) {
                    Icon(if (play) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(34.dp), tint = Kx.onAccent)
                }
                PBtn(Icons.Outlined.Forward10, "Вперёд на 10 секунд") { seek(t + 10); fk++ }
                PBtn(Icons.Rounded.SkipNext, "Следующая глава") { seek(if (ci < Chapters.lastIndex) Chapters[ci + 1].first else VTotal) }
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Speeds.forEachIndexed { i, v ->
                    val on = sp == i
                    Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .2f) else Color.Transparent).kxClick { sp = i }, contentAlignment = Alignment.Center) {
                        T((if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()).replace('.', ',') + "×", 13.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.text3)
                    }
                }
            }
        }
        Column(Modifier.padding(top = 14.dp).weight(1f).fillMaxWidth().background(Kx.bg)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.08f)))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
                Text("Разбираем корутины на живом проекте. Главы:", style = ts(14.sp, color = Color(0xFFDDE5E0)).copy(lineHeight = 20.3.sp))
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Chapters.forEachIndexed { i, c ->
                        val on = i == ci
                        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent)
                            .kxClick { seek(c.first) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(mm2(c.first), style = ts(13.sp, FontWeight.ExtraBold, Kx.accent).copy(textDecoration = TextDecoration.Underline))
                            T(c.second, 14.sp, if (on) FontWeight.ExtraBold else FontWeight.Medium, if (on) Kx.text else Kx.text2, modifier = Modifier.weight(1f))
                            if (on) Icon(Icons.Rounded.GraphicEq, null, Modifier.size(16.dp), tint = Kx.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Kx.text) }
}

/** Invisible 35% zone; on tap a 64dp bubble flashes (kxSkip .6s: fade in 30%, out with scale 1.1). */
@Composable
private fun SkipZone(icon: ImageVector, label: String, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val a = remember { Animatable(1f) }
    LaunchedEffect(count) { if (count > 0) { a.snapTo(0f); a.animateTo(1f, tween(600, easing = Ease)) } }
    val v = a.value
    val al = if (count == 0) 0f else if (v < .3f) v / .3f else 1f - (v - .3f) / .7f
    val sc = if (v < .3f) .7f + .3f * v / .3f else 1f + .1f * (v - .3f) / .7f
    Box(modifier.fillMaxHeight().clickableNoRipple(label, onClick), contentAlignment = Alignment.Center) {
        Column(Modifier.size(64.dp).graphicsLayer { alpha = al; scaleX = sc; scaleY = sc }.clip(CircleShape).background(Kx.w(.18f)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Kx.text); T("10 с", 11.sp, FontWeight.ExtraBold)
        }
    }
}

/** Invisible tap zone (the mockup's transparent buttons show no ripple). */
fun Modifier.clickableNoRipple(label: String, onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = null, indication = null, onClickLabel = label, onClick = onClick)
