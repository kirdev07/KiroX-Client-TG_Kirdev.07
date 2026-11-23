package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class R(val icon: ImageVector, val title: String, val sub: String, val bg: Color?, val ink: Color)

/** A drifting soft glow blob (kxDrift: translate(dx,dy) scale(s) at the middle), runs ×2. */
@Composable
fun DriftGlow(color: Color, diameter: Dp, modifier: Modifier, periodMs: Int, dx: Float, dy: Float, scale: Float, startOffsetMs: Int = 0) {
    val clock by rememberPlayClock(true, periodMs * 2L)
    val v = if (clock >= periodMs * 2L) pingPong(0, periodMs, 0, 0f, 1f) else pingPong(clock + startOffsetMs, periodMs, 0, 0f, 1f)
    Canvas(modifier.size(diameter).graphicsLayer { translationX = dx.dp.toPx() * v; translationY = dy.dp.toPx() * v; scaleX = 1f + (scale - 1f) * v; scaleY = scaleX }) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(0f to color, .65f to color.copy(alpha = 0f), radius = r * 1.414f, center = center), r)
    }
}

/** Board «Входящий звонок»: ringing (rings, slide-to-answer, decline, text reply, remind) → result. */
@Composable
fun IncomingScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → incoming call of the call service (answer / decline)
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoIncomingScreen(ctx) else RealCallBoard(ctx, CallBoard.Incoming)
}

@Composable
private fun DemoIncomingScreen(ctx: KxScreenCtx) {
    // demo: needs store (incoming call: peer, kind; answer/decline/remind/reply actions)
    var phase by remember { mutableStateOf("ring") }
    var video by remember { mutableStateOf(false) }
    var slide by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    fun finish(r: String) { phase = "done"; res = r; reply = false }
    LaunchedEffect(slide) { if (slide) { delay(450); finish("answered") } }

    Box(Modifier.fillMaxSize().background(Color(0xFF070908))) {
        DriftGlow(Color(120, 100, 200).copy(alpha = .35f), 420.dp, Modifier.offset((-80).dp, (-60).dp), 9000, -20f, 14f, 1.08f)
        DriftGlow(Kx.accentDeep.copy(alpha = .32f), 440.dp, Modifier.align(Alignment.BottomEnd).offset(120.dp, (-120).dp), 11000, -20f, 14f, 1.08f, startOffsetMs = 3000)

        if (phase == "ring") Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.align(Alignment.TopCenter).padding(top = 54.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.08f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(15.dp), tint = Kx.accent)
                T("${if (video) "Видеозвонок" else "Аудиозвонок"} · kiroX", 12.sp, color = Kx.text2)
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 150.dp).size(140.dp)) {
                CallRings(Modifier.matchParentSize(), color = Kx.accent.copy(alpha = .6f), periodMs = 2200, toScale = 2.1f, fromAlpha = .5f)
                MLetter("А", 140.dp, 52.sp, 0, Modifier.shadow(50.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f)))
            }
            Column(Modifier.padding(top = 320.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                T("Анна", 32.sp, FontWeight.ExtraBold)
                T(if (video) "Входящий видеозвонок…" else "Входящий звонок…", 15.sp, color = Kx.text3, modifier = Modifier.padding(top = 8.dp))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 230.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                SmallRound(Icons.Outlined.Sms, "Ответить текстом") { reply = true }
                SmallRound(Icons.Outlined.Alarm, "Напомнить") { finish("remind") }
            }
            // Slider.
            Box(Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 120.dp).fillMaxWidth().height(76.dp).clip(RoundedCornerShape(38.dp))
                .background(Kx.surface.copy(alpha = .6f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(38.dp)).topHighlight(Kx.w(.18f))) {
                val hintA by animateFloatAsState(if (slide) 0f else 1f, tween(200), label = "hint")
                val hc by rememberPlayClock(true, 2800)
                val hv = if (hc >= 2800) 0f else pingPong(hc, 1400, 0, 0f, 1f)
                Row(Modifier.fillMaxSize().padding(start = 92.dp, end = 20.dp).graphicsLayer { alpha = hintA }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)) {
                    T("Сдвиньте, чтобы ответить", 15.sp, FontWeight.SemiBold, Kx.text2, maxLines = 1)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp).graphicsLayer { translationX = 8.dp.toPx() * hv; alpha = .4f + .6f * hv }, tint = Kx.text2)
                }
                val kx by animateDpAsState(if (slide) 250.dp else 0.dp, tween(450, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "knob")
                val sc by rememberPlayClock(true, 3200)
                val sp = (sc % 1600) / 1600f
                val rot = if (sc >= 3200) 0f else when {
                    sp < .1f -> -16f * sp / .1f; sp < .2f -> -16f + 32f * (sp - .1f) / .1f; sp < .3f -> 16f - 32f * (sp - .2f) / .1f
                    sp < .4f -> -16f + 32f * (sp - .3f) / .1f; sp < .5f -> 16f - 16f * (sp - .4f) / .1f; else -> 0f
                }
                Box(Modifier.padding(6.dp).offset(x = kx).size(64.dp).shadow(22.dp, CircleShape, ambientColor = Kx.accentDeep.copy(alpha = .5f), spotColor = Kx.accentDeep.copy(alpha = .5f))
                    .clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep).kxClick("Ответить") { slide = true }, contentAlignment = Alignment.Center) {
                    Icon(if (video) Icons.Rounded.Videocam else Icons.Rounded.Call, null, Modifier.size(30.dp).graphicsLayer { rotationZ = rot }, tint = Kx.onAccent)
                }
            }
            // critique-C P3: Decline has the slider's 76dp height and full width, so the two read as a pair.
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 30.dp).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFFE5534B).copy(alpha = .16f))
                .border(1.dp, Color(0xFFE5534B).copy(alpha = .4f), RoundedCornerShape(32.dp)).kxClick("Отклонить") { finish("declined") },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CallEnd, null, tint = Kx.danger)
                T("Отклонить", 15.sp, FontWeight.Bold, Kx.danger)
            }
        }

        AnimatedVisibility(phase == "done", enter = fadeIn(tween(400)), exit = fadeOut(tween(150))) {
            val r = when (res) {
                "answered" -> R(Icons.Rounded.Call, "Звонок принят", if (video) "Открывается видеозвонок" else "Открывается экран звонка", null, Kx.onAccent)
                "declined" -> R(Icons.Rounded.CallEnd, "Звонок отклонён", "Анна увидит пропущенный звонок", Color(0xFFE5534B).copy(alpha = .16f), Kx.danger)
                "texted" -> R(Icons.Rounded.MarkChatRead, "Отправлено: «$text»", "Звонок отклонён", Kx.accent.copy(alpha = .14f), Kx.accent)
                else -> R(Icons.Rounded.Alarm, "Напомним через час", "Звонок отклонён, в 15:42 придёт напоминание", Kx.warning.copy(alpha = .16f), Kx.warning)
            }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                val pop = remember(res) { Animatable(0f) }
                LaunchedEffect(res) { pop.animateTo(1f, tween(500, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f))) }
                val v = pop.value
                val sc = if (v < .7f) .6f + .48f * v / .7f else 1.08f - .08f * (v - .7f) / .3f
                Box(Modifier.size(96.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = (v / .7f).coerceIn(0f, 1f) }.clip(RoundedCornerShape(30.dp))
                    .then(if (r.bg == null) Modifier.cssGrad(140f, Kx.accentMid, Kx.accentDeep) else Modifier.background(r.bg)), contentAlignment = Alignment.Center) {
                    Icon(r.icon, null, Modifier.size(50.dp), tint = r.ink)
                }
                Text(r.title, style = ts(22.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 30.dp, end = 30.dp))
                Text(r.sub, style = ts(14.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).kxClick { phase = "ring"; slide = false; res = "" }.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center) { T("Позвонить ещё раз", 14.sp, FontWeight.Bold) }
                    Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.accent).kxClick { phase = "ring"; slide = false; res = ""; video = !video }.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center) { T(if (video) "Аудиозвонок" else "Видеозвонок", 14.sp, FontWeight.Bold, Kx.onAccent) }
                }
            }
        }

        MSheet(reply, { reply = false }, dim = .6f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
            T("Отклонить и написать", 18.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            listOf("Не могу говорить, напишу позже", "Перезвоню через 10 минут", "Я за рулём", "Что случилось?").forEach { t ->
                Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.045f))
                    .kxClick { text = t; finish("texted") }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    T(t, 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.Send, null, tint = Kx.accent)
                }
            }
            Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).kxClick { }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Edit, null, tint = Kx.accent)
                T("Свой текст…", 15.sp, FontWeight.SemiBold, Kx.accent)
            }
        }
    }
}

@Composable
private fun SmallRound(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(.08f)).border(1.dp, Kx.w(.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Kx.text2) }
        T(label, 12.sp, color = Kx.text2)
    }
}
