package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private const val TrimDur = 48f
private val Quals = listOf("480p" to .35f, "720p" to .8f, "1080p" to 1.6f)

private fun skyStops(p: Float): Array<Pair<Float, Color>> = arrayOf(
    0f to Color.hsl(210f - p * 180f, .55f, (60f - p * 15f) / 100f),
    .7f to Color.hsl(30f + p * 10f, .70f, (70f - p * 20f) / 100f),
    1f to Color.hsl(30f + p * 10f, .70f, (70f - p * 20f) / 100f))

private fun mmss(t: Float) = "${(t / 60).toInt()}:${(t % 60).toInt().toString().padStart(2, '0')}"

/** Board «Обрезка видео»: preview placeholder (sky changes with time), timeline with trim, quality, cover, mute, send. */
@Composable
fun VideoTrimScreen(ctx: KxScreenCtx) {
    // demo: needs store (picked video: duration, frames, sendVideo(start,end,quality,mute,cover))
    var start by remember { mutableFloatStateOf(6f) }
    var end by remember { mutableFloatStateOf(30f) }
    var pos by remember { mutableFloatStateOf(6f) }
    var playing by remember { mutableStateOf(false) }
    var mute by remember { mutableStateOf(false) }
    var q by remember { mutableIntStateOf(1) }
    var cover by remember { mutableFloatStateOf(12f) }
    var toastText by remember { mutableStateOf("") }
    var toastIcon by remember { mutableStateOf(Icons.Rounded.Done) }
    var toastStamp by remember { mutableLongStateOf(0L) }
    fun toast(t: String, i: ImageVector) { toastText = t; toastIcon = i; toastStamp = System.nanoTime() }
    LaunchedEffect(playing) { while (playing) { delay(100); var p = pos + .25f; if (p >= end) p = start; pos = p } }
    val p = pos / TrimDur
    val len = end - start
    val mb = len * Quals[q].second * (if (mute) .9f else 1f)
    fun clamp(v: Float, a: Float, b: Float) = v.coerceIn(a, b)

    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { ctx.back() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    T("Обрезка видео", 15.sp, FontWeight.ExtraBold)
                    T("Перетащите края полосы", 11.sp, color = Kx.text3) // critique-C P3: the cut length is shown once, in the centre
                }
                val mbg by animateColorAsState(if (mute) Color(0xFFE5534B).copy(alpha = .18f) else Kx.w(.08f), tween(200), label = "mbg")
                MRoundBtn(if (mute) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, if (mute) "Включить звук в видео" else "Отправить без звука",
                    bg = mbg, border = null, tint = if (mute) Kx.danger else Kx.text) { mute = !mute }
            }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(420.dp).clip(RoundedCornerShape(26.dp)).drawBehind { drawRect(Brush.verticalGradient(*skyStops(p))) }) {
                val clock by rememberPlayClock(true, 6000)
                val bob = if (clock >= 6000) 0f else pingPong(clock, 3000, 0, 0f, -6f)
                val sx by animateFloatAsState(20f + p * 260f, tween(300, easing = LinearEasing), label = "sx")
                val sy by animateFloatAsState(40f + kotlin.math.sin(p * Math.PI).toFloat() * -20f + 30f, tween(300, easing = LinearEasing), label = "sy")
                Canvas(Modifier.matchParentSize()) {
                    val c = Offset((sx + 30f).dp.toPx(), (sy + 30f + bob).dp.toPx()); val r = 30.dp.toPx()
                    drawCircle(Brush.radialGradient(0f to Color(0xFFFFE3A3), .6f to Color(0xFFF2B866), .7f to Color(0xFFF2B866).copy(alpha = 0f), center = c, radius = r * 1.414f), r, c)
                    val fx = size.width / 358f; val fy = size.height / 420f
                    fun poly(pts: List<Pair<Float, Float>>, col: Color) = drawPath(Path().apply {
                        pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * fx, y * fy) else lineTo(x * fx, y * fy) }; close()
                    }, col)
                    poly(listOf(0f to 300f, 70f to 220f, 120f to 260f, 190f to 170f, 250f to 240f, 300f to 200f, 358f to 250f, 358f to 420f, 0f to 420f), Color(0xFF2B3E35))
                    poly(listOf(0f to 340f, 90f to 290f, 160f to 320f, 240f to 280f, 358f to 330f, 358f to 420f, 0f to 420f), Color(0xFF1A2620))
                }
                val pa by animateFloatAsState(if (playing) .25f else 1f, tween(300), label = "pa")
                Box(Modifier.align(Alignment.Center).size(70.dp).graphicsLayer { alpha = pa }.clip(CircleShape).background(Color.Black.copy(alpha = .35f))
                    .border(1.dp, Kx.w(.25f), CircleShape).kxClick(if (playing) "Пауза" else "Смотреть") { if (!playing && pos >= end) pos = start; playing = !playing }, contentAlignment = Alignment.Center) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Color.White)
                }
                T("${mmss(pos)} / ${mmss(TrimDur)}", 12.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 10.dp, vertical = 4.dp))
                if (kotlin.math.abs(pos - cover) < .6f) T("Обложка", 12.sp, FontWeight.ExtraBold, Kx.onAccent, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).kxRise(0, 200)
                    .clip(RoundedCornerShape(10.dp)).background(Kx.accent).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
                    val W = maxWidth
                    Row(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp))) {
                        repeat(10) { i ->
                            Box(Modifier.weight(1f).fillMaxHeight().drawBehind { drawRect(Brush.verticalGradient(*skyStops((i + .5f) / 10f))) }) {
                                Box(Modifier.align(Alignment.CenterEnd).width(1.dp).fillMaxHeight().background(Color.Black.copy(alpha = .4f)))
                            }
                        }
                    }
                    val sl by animateDpAsState(W * (start / TrimDur), tween(150), label = "sl")
                    val sw by animateDpAsState(W * (len / TrimDur), tween(150), label = "sw")
                    Box(Modifier.clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).width(W * (start / TrimDur)).fillMaxHeight().background(Color.Black.copy(alpha = .65f)))
                    Box(Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)).width(W * ((TrimDur - end) / TrimDur)).fillMaxHeight().background(Color.Black.copy(alpha = .65f)))
                    Box(Modifier.offset(x = sl).width(sw).fillMaxHeight().border(3.dp, Kx.accent, RoundedCornerShape(10.dp)))
                    val hx by animateDpAsState(W * p - 1.dp, tween(100, easing = LinearEasing), label = "hx")
                    Box(Modifier.offset(x = hx, y = (-4).dp).width(3.dp).height(64.dp).shadow(6.dp, RoundedCornerShape(2.dp)).clip(RoundedCornerShape(2.dp)).background(Color.White))
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stepper(mmss(start), "Начало раньше", "Начало позже",
                        { val v = clamp(start - 1, 0f, end - 2); start = v; pos = v }, { val v = clamp(start + 1, 0f, end - 2); start = v; pos = v })
                    T(mmss(len), 13.sp, color = Kx.text2, modifier = Modifier.weight(1f).wrapContentWidth())
                    Stepper(mmss(end), "Конец раньше", "Конец позже",
                        { val v = clamp(end - 1, start + 2, TrimDur); end = v; pos = v - .5f }, { val v = clamp(end + 1, start + 2, TrimDur); end = v; pos = v - .5f })
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Quals.forEachIndexed { i, (label, _) ->
                    val on = q == i
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), tween(200), label = "q")
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(bg).kxClick { q = i }, contentAlignment = Alignment.Center) {
                        T(label, 13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2)
                    }
                }
            }
            // critique-C P3: «Обложка» on its own line, apart from the quality choice.
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.06f))
                .kxClick("Сделать обложкой текущий кадр") { cover = pos; toast("Обложка — кадр ${mmss(pos)}", Icons.Rounded.Image) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Image, null, Modifier.size(17.dp), tint = Kx.text)
                T("Обложка — текущий кадр", 13.sp, FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T("≈ " + String.format(java.util.Locale.US, "%.1f", mb).replace('.', ',') + " МБ · ${Quals[q].first}" + (if (mute) " · без звука" else ""), 12.sp, color = Kx.text4, modifier = Modifier.weight(1f))
                Row(Modifier.height(52.dp).shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Kx.accentDeep.copy(alpha = .4f), spotColor = Kx.accentDeep.copy(alpha = .4f))
                    .clip(RoundedCornerShape(26.dp)).cssGrad(135f, Kx.accentMid, Kx.accentDeep).kxClick { playing = false; toast("Видео ${mmss(len)} отправлено", Icons.Rounded.DoneAll) }
                    .padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent)
                    T("Отправить", 16.sp, FontWeight.ExtraBold, Kx.onAccent)
                }
            }
        }
        if (toastStamp != 0L) {
            val a = remember(toastStamp) { Animatable(0f) }
            LaunchedEffect(toastStamp) { a.animateTo(1f, tween(2400, easing = LinearEasing)); toastStamp = 0L }
            val v = a.value
            val al = when { v < .12f -> v / .12f; v < .85f -> 1f; else -> 1f - (v - .85f) / .15f }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp).graphicsLayer { alpha = al; translationY = if (v < .12f) 10.dp.toPx() * (1 - v / .12f) else 0f }
                .clip(RoundedCornerShape(18.dp)).background(Kx.surface3).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(toastIcon, null, Modifier.size(20.dp), tint = Kx.accent)
                T(toastText, 14.sp, FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Stepper(value: String, minusLabel: String, plusLabel: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).kxClick(minusLabel, onClick = onMinus), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ChevronLeft, null, Modifier.size(18.dp), tint = Kx.text) }
        T(value, 13.sp, FontWeight.ExtraBold, Kx.accent, modifier = Modifier.widthIn(min = 42.dp).wrapContentWidth())
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).kxClick(plusLabel, onClick = onPlus), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = Kx.text) }
    }
}
