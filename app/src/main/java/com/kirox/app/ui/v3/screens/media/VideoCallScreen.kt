package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat

import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val GlassBtn = Color.White.copy(alpha = .12f)
private val OnBtn = Color(0xFFEEF3EF)
private val EndRed = Color(0xFFE5534B)

/** Out of the call: speaker / phone / bluetooth. */
private enum class AudioOut(val icon: ImageVector, val label: String, val sheetLabel: String) {
    Speaker(Icons.AutoMirrored.Rounded.VolumeUp, "Динамик", "Динамик"),
    Phone(Icons.Rounded.PhoneInTalk, "Телефон", "Телефон"),
    Bt(Icons.Rounded.BluetoothAudio, "Наушники", "Наушники Bluetooth"),
}

/** Board «Видеозвонок»: calling → live (remote video placeholder, own PiP, controls, key) → ended. */
@Composable
fun VideoCallScreen(ctx: KxScreenCtx) {
    // demo: needs store (call state: phase, duration, remote camera, emoji key) — peer «Анна»
    var phase by remember { mutableStateOf("calling") }
    var sec by remember { mutableIntStateOf(0) }
    var dots by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var cam by remember { mutableStateOf(true) }
    var front by remember { mutableStateOf(true) }
    var out by remember { mutableStateOf(AudioOut.Speaker) }
    var ui by remember { mutableStateOf(true) }
    var pipLow by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf(false) }
    var outSheet by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    val remoteCam = true
    var run by remember { mutableIntStateOf(0) }
    LaunchedEffect(run) {
        var t = 0
        while (true) {
            delay(1000); t += 1000
            when (phase) { "calling" -> dots = (dots + 1) % 4; "live" -> sec += 1; else -> return@LaunchedEffect }
            if (phase == "calling" && t >= 3000) { delay(200); phase = "live"; sec = 0 }
        }
    }
    val live = phase == "live"
    val uiShown = !live || ui
    val uiA by animateFloatAsState(if (uiShown) 1f else 0f, tween(300, easing = Ease), label = "ui")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        AnimatedVisibility(live, enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 1.04f), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize()) {
                if (remoteCam) RemoteVideo() else RemoteOff()
                Box(Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f)))))
            }
        }
        if (phase == "calling") Box(Modifier.fillMaxSize().ellipseGlow(Kx.accentDeep.copy(alpha = .28f), .5f, .35f, .8f, .5f, .7f).statusBarsPadding()) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 200.dp).size(132.dp)) {
                CallRings(Modifier.matchParentSize())
                MLetter("А", 132.dp, 48.sp, 0)
            }
            Column(Modifier.padding(top = 368.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                T("Анна", 28.sp, FontWeight.ExtraBold)
                T("Видеозвонок" + ".".repeat(dots), 14.sp, color = Kx.text3, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Tap area to hide controls.
        if (live) Box(Modifier.fillMaxSize().padding(top = 70.dp, bottom = 210.dp).kxClick("Показать или скрыть кнопки") { ui = !ui; key = false })

        if (phase != "ended") {
            val top by animateDpAsState(if (pipLow) 470.dp else if (ui) 74.dp else 24.dp, tween(450, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "pip")
            val mirror by animateFloatAsState(if (front) 1f else -1f, tween(300), label = "mirror")
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = top, end = 16.dp).size(104.dp, 150.dp)
                .shadow(36.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
                .clip(RoundedCornerShape(22.dp)).cssGrad(170f, Color(0xFF2A2F3A), Color(0xFF15181E)).border(1.dp, Kx.w(.22f), RoundedCornerShape(22.dp))
                .kxClick("Переместить своё видео") { pipLow = !pipLow }) {
                Box(Modifier.matchParentSize().graphicsLayer { scaleX = mirror }
                    .personPlaceholder(Color(0xFF7C8496), Color(0xFF3E4452), Color(0xFF353B48), Color(0xFF353B48), 34f, 50f, 56f, 92f, 92f, 80f))
                if (!cam) Box(Modifier.matchParentSize().background(Color(0xFF121614)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.VideocamOff, null, tint = Kx.text4) }
                if (muted) Box(Modifier.align(Alignment.BottomStart).padding(6.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MicOff, null, Modifier.size(15.dp), tint = Kx.danger)
                }
            }
        }

        // Top bar.
        Row(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth()
            .graphicsLayer { alpha = uiA; translationY = (1f - uiA) * -20.dp.toPx() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", size = 42.dp, bg = Kx.w(.1f), border = Kx.w(.14f)) { ctx.back() }
            if (live) Column {
                T("Анна", 17.sp, FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpeakBars()
                    T(fmtClock(sec), 12.sp, color = Kx.text2)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.1f)).border(1.dp, Kx.w(.14f), RoundedCornerShape(19.dp))
                .kxClick("Ключ шифрования") { key = !key }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("🐼🌵🎈🔑", style = ts(16.sp).copy(letterSpacing = 2.sp))
            }
        }

        if (live && share) Row(Modifier.statusBarsPadding().padding(start = 16.dp, top = 66.dp).height(32.dp).kxRise(0, 250).clip(RoundedCornerShape(16.dp))
            .background(Kx.accent.copy(alpha = .18f)).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.ScreenShare, null, Modifier.size(16.dp), tint = Kx.accent)
            T("Вы показываете экран", 12.sp, FontWeight.Bold, Kx.accent)
        }

        AnimatedVisibility(key, Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 70.dp),
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.04f), exit = fadeOut(tween(150))) {
            Column(Modifier.fillMaxWidth().shadow(50.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
                .clip(RoundedCornerShape(24.dp)).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(24.dp)).topHighlight(Kx.w(.18f)).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🐼🌵🎈🔑", style = ts(34.sp).copy(letterSpacing = 6.sp))
                T("Звонок защищён", 15.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                Text("Если у собеседника те же эмодзи, звонок зашифрован сквозным шифрованием", style = ts(13.sp, color = Kx.text2).copy(lineHeight = 18.sp),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 12.dp).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.accent).kxClick { key = false }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    T("Понятно", 14.sp, FontWeight.Bold, Kx.onAccent)
                }
            }
        }

        // Controls.
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 26.dp)
            .graphicsLayer { alpha = uiA; translationY = (1f - uiA) * 30.dp.toPx() }) {
            if (live) Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                CallChip(Icons.Outlined.PersonAdd, "Добавить") { }
                CallChip(if (share) Icons.Outlined.StopScreenShare else Icons.Outlined.ScreenShare, if (share) "Остановить показ" else "Показать экран") { share = !share }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(Kx.surface.copy(alpha = .7f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(34.dp)).topHighlight(Kx.w(.18f)).padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Top) {
                CallBtn(out.icon, out.label, if (out == AudioOut.Speaker) OnBtn else GlassBtn, if (out == AudioOut.Speaker) Kx.bg else OnBtn) { outSheet = true }
                CallBtn(if (cam) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, if (cam) "Камера" else "Камера выкл.", if (cam) GlassBtn else OnBtn, if (cam) OnBtn else Kx.bg) { cam = !cam }
                val rot by animateFloatAsState(if (front) 0f else 180f, tween(300), label = "rot")
                CallBtn(Icons.Rounded.Cameraswitch, "Повернуть", GlassBtn, if (cam) OnBtn else Color(0xFF5C6661), rot) { if (cam) front = !front }
                CallBtn(if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, if (muted) "Без звука" else "Микрофон", if (muted) OnBtn else GlassBtn, if (muted) Kx.bg else OnBtn) { muted = !muted }
                CallBtn(Icons.Rounded.CallEnd, "Завершить", EndRed, Color.White) { phase = "ended"; outSheet = false; key = false }
            }
        }

        MSheet(outSheet, { outSheet = false }, dim = .45f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp)) {
            T("Вывод звука", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            AudioOut.entries.forEach { o ->
                val on = o == out
                Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
                    .kxClick { out = o; outSheet = false }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(o.icon, null, tint = Kx.accent)
                    T(o.sheetLabel, 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Rounded.CheckCircle, null, tint = Kx.accent)
                }
            }
        }

        AnimatedVisibility(phase == "ended", enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 1.04f), exit = fadeOut(tween(200))) {
            Column(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .18f), .5f, .42f, .7f, .45f, .7f).kxClick { },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                MLetter("А", 96.dp, 38.sp, 0)
                T("Звонок завершён", 22.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
                T("Видеозвонок · ${fmtClock(sec)}", 14.sp, color = Kx.text3)
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillBtn(Icons.Rounded.Videocam, "Позвонить снова", Kx.accent, Kx.onAccent) { phase = "calling"; sec = 0; dots = 0; ui = true; run++ }
                    PillBtn(Icons.AutoMirrored.Outlined.Chat, "Написать", Kx.w(.08f), Kx.text) { ctx.back() }
                }
            }
        }
    }
}

@Composable
private fun RemoteVideo() {
    // kxDrift 9s ×2: translate(-8, 6) scale 1.03 at the middle.
    val clock by rememberPlayClock(true, 18000)
    val v = if (clock >= 18000) 0f else pingPong(clock, 9000, 0, 0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.requiredSize(maxWidth + 40.dp, maxHeight + 40.dp).graphicsLayer { translationX = -8.dp.toPx() * v; translationY = 6.dp.toPx() * v; scaleX = 1f + .03f * v; scaleY = scaleX }.background(Brush.verticalGradient(listOf(Color(0xFF16211C), Color(0xFF0D1411))))
            .ellipseGlow(Color(0xFF1B2E3A), .8f, .9f, .7f, .5f, .7f).ellipseGlow(Color(0xFF2C4A3B), .3f, .2f, .6f, .4f, .7f)
            .personPlaceholder(Color(0xFF6F8C7C), Color(0xFF34483D), Color(0xFF3F5C4C), Color(0xFF1E2C25), 250f, 150f, 170f, 400f, 300f, 360f))
    }
}


@Composable
private fun RemoteOff() {
    val clock by rememberPlayClock(true, 6000)
    val s = if (clock >= 6000) 1f else pingPong(clock, 3000, 0, 1f, 1.04f)
    Column(Modifier.fillMaxSize().ellipseGlow(Kx.accentDeep.copy(alpha = .22f), .5f, .4f, .7f, .45f, .7f), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        MLetter("А", 120.dp, 44.sp, 0, Modifier.graphicsLayer { scaleX = s; scaleY = s })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.VideocamOff, null, Modifier.size(18.dp), tint = Kx.text3)
            T("Камера собеседника выключена", 13.sp, color = Kx.text3)
        }
    }
}

@Composable
fun CallChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.1f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(17.dp)).kxClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = Kx.text)
        T(label, 13.sp, FontWeight.SemiBold)
    }
}

/** 56dp round call button with an 11sp label under it (62dp column). */
@Composable
fun CallBtn(icon: ImageVector, label: String, bg: Color, ink: Color, rotation: Float = 0f, size: androidx.compose.ui.unit.Dp = 56.dp, onClick: () -> Unit) {
    val b by animateColorAsState(bg, tween(200), label = "bg"); val i by animateColorAsState(ink, tween(200), label = "ink")
    Column(Modifier.width(62.dp).kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(size).clip(CircleShape).background(b), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(26.dp).graphicsLayer { rotationZ = rotation }, tint = i)
        }
        Text(label, style = ts(11.sp, color = Kx.text2), maxLines = 1, softWrap = false)
    }
}

@Composable
fun PillBtn(icon: ImageVector, label: String, bg: Color, ink: Color, height: androidx.compose.ui.unit.Dp = 46.dp, onClick: () -> Unit) {
    Row(Modifier.height(height).clip(RoundedCornerShape(height / 2)).background(bg).kxClick(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = ink)
        T(label, 14.sp, FontWeight.Bold, ink)
    }
}
