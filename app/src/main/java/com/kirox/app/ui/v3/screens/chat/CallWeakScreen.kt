package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularAlt2Bar
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.components.KxCallControls
import com.kirox.app.ui.v3.components.KxCallButton
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class CwBan(val icon: ImageVector, val ic: Color, val title: String, val sub: String, val btn: String, val bg: Color, val bd: Color, val spin: Boolean = false)

/**
 * Board «Слабая связь в звонке»: video call with signal bars, status banner (weak here / weak there / reconnecting), voice-only fallback,
 * and a network switcher. The whole call is simulated → shown only with KxDemoStore.
 */
@Composable
fun CallWeakScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → signal bars / reconnecting / voice-only of the live call
    if (ctx.store !is KxDemoStore) { com.kirox.app.ui.v3.screens.media.RealCallBoard(ctx, com.kirox.app.ui.v3.screens.media.CallBoard.Weak); return }
    var net by remember { mutableStateOf("weak") }
    var sec by remember { mutableIntStateOf(754) }
    var cam by remember { mutableStateOf(true) }
    var audioOnly by remember { mutableStateOf(false) }
    var micOn by remember { mutableStateOf(true) }
    var speakerOn by remember { mutableStateOf(true) }
    var k by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); if (net != "lost") sec++ } }
    val reduced = LocalKxReducedMotion.current
    val bars = mapOf("good" to 4, "weak" to 2, "theirs" to 3, "lost" to 0).getValue(net)
    fun bc(i: Int) = if (i <= bars) (if (bars >= 3) Kx.accent else if (bars == 2) Kx.warning else Kx.danger) else Kx.w(.25f)
    val video = net != "lost" && !audioOnly
    val vOp by animateFloatAsState(if (video) 1f else 0f, tween(400), label = "v")
    val amber = Color(0xB83C2C0E)
    val ban = when (net) {
        "weak" -> CwBan(Icons.Outlined.SignalCellularAlt2Bar, Kx.warning, "Слабая связь у вас", "Видео может подтормаживать", if (audioOnly) "Вернуть видео" else "Только голос", amber, Kx.warning.copy(alpha = .35f))
        "theirs" -> CwBan(Icons.Outlined.PersonOff, Kx.warning, "Плохая связь у собеседника", "Мы снизили качество видео, чтобы не прерывалось", "", amber, Kx.warning.copy(alpha = .35f))
        "lost" -> CwBan(Icons.Rounded.Sync, hex("#7FB8E0"), "Переподключаемся…", "Звонок продолжится сам, как только вернётся сеть", "", Color(0xBF0E2234), hex("#7FB8E0").copy(alpha = .35f), spin = true)
        else -> null
    }
    val inf = rememberInfiniteTransition(label = "call")
    val pan by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "pan")
    val pix by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "pix")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "spin")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing)), label = "ring")

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val blurPx = if (net == "weak" && video && !reduced) 6f * kotlin.math.sin(Math.PI * pix).toFloat() else 0f
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = vOp }.then(if (blurPx > .1f) Modifier.blur(blurPx.dp) else Modifier).drawBehind {
            val w = size.width * 3; val x0 = -(w - size.width) * (if (reduced) 0f else pan)
            val s = cssAngleStartLocal(110f, w, size.height)
            drawRect(Brush.linearGradient(listOf(hex("#2A3A33"), hex("#4A5A52"), hex("#1B2420"), Kx.accentDeep), Offset(x0 + s.x, s.y), Offset(x0 + w - s.x, size.height - s.y)))
        })
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - vOp }.drawBehind {
            val c = Offset(size.width / 2, size.height * .35f); val r = kotlin.math.hypot(size.width / 2, size.height * .65f)
            drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .25f), .7f to hex("#050706"), 1f to hex("#050706"), center = c, radius = r))
        })
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 200.dp).graphicsLayer { alpha = 1f - vOp }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                if (!reduced) Box(Modifier.fillMaxSize().graphicsLayer { val s = .9f + .6f * ring; scaleX = s; scaleY = s; alpha = .6f * (1f - ring) }.border(2.dp, Kx.accent.copy(alpha = .5f), CircleShape))
                LetterAv("А", hex("#C0443C"), 120.dp, 46)
            }
            Text(if (net == "lost") "Ждём сеть…" else "Только голос — бережём связь", style = ts(13, color = Kx.text2), modifier = Modifier.padding(top = 12.dp))
        }
        // Top
        Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))).statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Анна", style = ts(18, W800))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (net == "lost") "пауза" else "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}") + " · ", style = ts(13, color = Kx.text2))
                    Icon(Icons.Rounded.Lock, null, Modifier.size(13.dp), tint = Kx.text2); Text(" шифрование", style = ts(13, color = Kx.text2))
                }
            }
            Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .35f)).padding(start = 10.dp, end = 10.dp, bottom = 9.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(5, 9, 12, 15).forEachIndexed { i, h -> Box(Modifier.size(4.dp, h.dp).clip(RoundedCornerShape(1.dp)).background(bc(i + 1))) }
            }
        }
        if (ban != null) key(k) {
            Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 74.dp).chIn(dy = (-12).dp, durationMs = 300).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(ban.bg).border(1.dp, ban.bd, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(ban.icon, null, Modifier.graphicsLayer { rotationZ = if (ban.spin && !reduced) spin else 0f }, tint = ban.ic)
                Column(Modifier.weight(1f)) { Text(ban.title, style = ts(14, W800)); Text(ban.sub, style = ts(12, color = Kx.text2)) }
                if (ban.btn.isNotEmpty()) Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.12f)).kxClick { audioOnly = !audioOnly }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center) { Text(ban.btn, style = ts(12, W800)) }
            }
        }
        // Self preview
        val selfOn = cam && !audioOnly
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 150.dp, end = 16.dp).size(96.dp, 140.dp).clip(RoundedCornerShape(18.dp))
            .then(if (selfOn) Modifier.cssGradient(160f, hex("#3A403D"), hex("#1B201E")) else Modifier.background(hex("#111111"))).border(1.dp, Kx.w(.2f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center) {
            // critique-C P3: the self tile says what it is (letter + «Камера выключена» when off).
            if (selfOn) LetterAv("А", hex("#3F9A6C"), 52.dp, 20)
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.VideocamOff, null, Modifier.size(26.dp), tint = Kx.w(.6f))
                Text("Камера\nвыключена", style = ts(11, color = Kx.text2), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        // Controls + demo network
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 20.dp)) {
            // critique-C P1: the shared call control row.
            KxCallControls(listOf(
                KxCallButton(if (selfOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, "Камера", selfOn) { cam = !cam },
                KxCallButton(if (micOn) Icons.Rounded.Mic else Icons.Rounded.MicOff, "Микрофон", micOn) { micOn = !micOn },
                KxCallButton(Icons.AutoMirrored.Rounded.VolumeUp, "Динамик", speakerOn) { speakerOn = !speakerOn },
                KxCallButton(Icons.Rounded.CallEnd, "Завершить", end = true, onClick = ctx.back),
            ), Modifier.clip(RoundedCornerShape(30.dp)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(30.dp)).padding(vertical = 12.dp))
            // critique-C P3: the network switcher is a demo control — say so, like other boards.
            Text("Макет: качество связи", style = ts(12, color = Kx.text4), modifier = Modifier.padding(top = 14.dp, start = 4.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Triple("good", Icons.Outlined.SignalCellularAlt, "Хорошо"), Triple("weak", Icons.Outlined.SignalCellularAlt2Bar, "Слабо"),
                    Triple("theirs", Icons.Outlined.PersonOff, "У Анны"), Triple("lost", Icons.Outlined.SignalCellularConnectedNoInternet0Bar, "Пропала")).forEach { (key, ic, t) ->
                    val on = net == key
                    Column(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, RoundedCornerShape(16.dp))
                        .kxClick(t) { net = key; k++; if (key != "weak") audioOnly = false }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        Icon(ic, null, Modifier.size(20.dp), tint = if (on) Kx.accent else Kx.text2); Text(t, style = ts(11, W800, if (on) Kx.accent else Kx.text2))
                    }
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun CwBtn(icon: ImageVector, bg: Color, label: String?, onClick: (() -> Unit)? = null) {
    Box(Modifier.size(56.dp).clip(CircleShape).background(bg).then(if (onClick != null) Modifier.kxClick(label, onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Kx.text)
    }
}

/** CSS gradient-line start point for an angle on a w×h box. */
private fun cssAngleStartLocal(deg: Float, w: Float, h: Float): Offset {
    val r = Math.toRadians(deg.toDouble()); val dx = kotlin.math.sin(r).toFloat(); val dy = -kotlin.math.cos(r).toFloat()
    val half = (kotlin.math.abs(w * dx) + kotlin.math.abs(h * dy)) / 2
    return Offset(w / 2 - dx * half, h / 2 - dy * half)
}
