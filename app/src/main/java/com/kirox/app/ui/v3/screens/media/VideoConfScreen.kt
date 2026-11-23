package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class ConfP(val name: String, val l: String, val light: Color?, val cx: Float, val c1: Color, val c2: Color, val av: Color, val cam: Boolean)

// demo: needs store (group video call participants with camera/mic/speaking state)
private val ConfPeople = listOf(
    ConfP("Вы", "Я", Color(0xFF6FC497), .4f, Color(0xFF1E4B36), Color(0xFF0E1C16), Color(0xFF3F9A6C), true),
    ConfP("Дмитрий", "Д", Color(0xFF6FA9D8), .6f, Color(0xFF1B3A55), Color(0xFF0D1620), Color(0xFF2F6E9E), true),
    ConfP("Мария", "М", Color(0xFFF2B866), .5f, Color(0xFF5A3C1A), Color(0xFF1E140A), Color(0xFFB07A2E), true),
    ConfP("Олег", "О", null, .5f, Color(0xFF1A201D), Color(0xFF0A0C0B), Color(0xFF5C6661), false),
    ConfP("Анна", "А", Color(0xFFF2A6A0), .5f, Color(0xFF5A1E1A), Color(0xFF200A08), Color(0xFFC0443C), true),
    ConfP("Алекс", "А", Color(0xFFD2B6F2), .5f, Color(0xFF35264F), Color(0xFF120E1C), Color(0xFF6A4C9C), true),
)

private fun Modifier.camBg(p: ConfP) = cssGrad(160f, p.c1, p.c2).then(if (p.light != null) Modifier.ellipseGlow(p.light, p.cx, .4f, .6f, .5f, .6f) else Modifier)

/** Board «Видеоконференция»: 2-column grid or speaker layout with strip, speaking borders + EQ, raised hand, controls. */
@Composable
fun VideoConfScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → the video chat this account is in (store.groupCall)
    // critique-C P1: one video-chat layout — the GroupCall board (⌄, «Говорит:», labelled controls, grid / speaker toggle).
    GroupCallScreen(ctx)
}

@Suppress("unused")
@Composable
private fun DemoVideoConfScreen(ctx: KxScreenCtx) {
    var sec by remember { mutableIntStateOf(1284) }
    var speaking by remember { mutableIntStateOf(1) }
    var pin by remember { mutableIntStateOf(1) }
    var grid by remember { mutableStateOf(true) }
    var mic by remember { mutableStateOf(true) }
    var cam by remember { mutableStateOf(true) }
    var hand by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1000); sec++ } }
    LaunchedEffect(Unit) { while (true) { delay(2600); speaking = listOf(1, 2, 4, 1, 5).random() } }
    val P = ConfPeople
    val eq by rememberLoopClock()
    fun camOf(i: Int) = if (i == 0) cam else P[i].cam
    fun micOf(i: Int) = if (i == 0) mic else i != 3

    Column(Modifier.fillMaxSize().background(Color(0xFF050606)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                T("Kotlin-клуб · созвон", 15.sp, FontWeight.ExtraBold)
                T("${sec / 60}:${(sec % 60).toString().padStart(2, '0')} · ${P.size} ${ruPlural(P.size, "участник", "участника", "участников")}", 12.sp, color = Kx.accent)
            }
            Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.08f)).kxClick("Сменить вид") { grid = !grid }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (grid) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda, null, Modifier.size(18.dp), tint = Kx.text)
                T(if (grid) "Сетка" else "Докладчик", 12.sp, FontWeight.Bold)
            }
        }
        if (grid) Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 4.dp).kxRiseScale(0, 300), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            P.indices.chunked(2).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i ->
                        val p = P[i]; val sp = i != 0 && speaking == i; val c = camOf(i); val m = micOf(i)
                        val bd by animateColorAsState(if (sp) Kx.accent else Color.Transparent, tween(200), label = "bd")
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xFF111614)).border(2.5.dp, bd, RoundedCornerShape(20.dp))
                            .kxClick(p.name + (if (sp) ", говорит" else "") + " — закрепить") { pin = i; grid = false }) {
                            if (c) BlobBg(p, (5 + i) * 1000)
                            Box(Modifier.align(androidx.compose.ui.BiasAlignment(0f, -.08f)).size(54.dp).graphicsLayer { alpha = if (c) .9f else 1f }.clip(CircleShape)
                                .background(if (c) Color.Black.copy(alpha = .25f) else p.av), contentAlignment = Alignment.Center) { T(p.l, 22.sp, FontWeight.ExtraBold) }
                            Row(Modifier.align(Alignment.BottomStart).padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(if (m) Icons.Rounded.Mic else Icons.Rounded.MicOff, null, Modifier.size(13.dp), tint = if (m) Kx.text else Kx.danger)
                                    T(p.name, 11.sp, FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                if (sp) Row(Modifier.height(14.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(3) { k ->
                                        val s = pingPong(eq, 700, k * 150, .3f, 1f)
                                        Box(Modifier.size(3.dp, 14.dp).graphicsLayer { scaleY = s; transformOrigin = TransformOrigin(.5f, 1f) }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                                    }
                                }
                            }
                            if (i == 0 && hand) {
                                val hc by rememberPlayClock(true, 2000)
                                val r = if (hc >= 2000) -12f else pingPong(hc, 1000, 0, -12f, 12f)
                                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).graphicsLayer { rotationZ = r }.clip(CircleShape).background(Kx.warning), contentAlignment = Alignment.Center) {
                                    Text("✋", style = ts(16.sp))
                                }
                            }
                        }
                    }
                }
            }
        } else Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 4.dp).kxRiseScale(0, 300), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val pp = P[pin]
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF111614)).border(2.5.dp, Kx.accent, RoundedCornerShape(24.dp))) {
                BlobBg(pp, 6000)
                Box(Modifier.align(androidx.compose.ui.BiasAlignment(0f, -.1f)).size(96.dp).clip(CircleShape).background(Color.Black.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                    T(pp.l, 40.sp, FontWeight.ExtraBold)
                }
                Row(Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = Kx.accent); T(pp.name, 13.sp, FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                P.indices.filter { it != pin }.take(5).forEach { i ->
                    val p = P[i]; val sp = i != 0 && speaking == i; val c = camOf(i)
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111614)).border(2.dp, if (sp) Kx.accent else Color.Transparent, RoundedCornerShape(16.dp))
                        .then(if (c) Modifier.camBg(p) else Modifier).kxClick(p.name + (if (sp) ", говорит" else "") + " — закрепить") { pin = i; grid = false }) {
                        Box(Modifier.align(androidx.compose.ui.BiasAlignment(0f, -.25f)).size(34.dp).graphicsLayer { alpha = if (c) .9f else 1f }.clip(CircleShape)
                            .background(if (c) Color.Black.copy(alpha = .25f) else p.av), contentAlignment = Alignment.Center) { T(p.l, 14.sp, FontWeight.ExtraBold) }
                        T(p.name, 11.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ConfBtn(if (mic) Icons.Rounded.Mic else Icons.Rounded.MicOff, if (mic) "Выключить микрофон" else "Включить микрофон", !mic) { mic = !mic }
            ConfBtn(if (cam) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, if (cam) "Выключить камеру" else "Включить камеру", !cam) { cam = !cam }
            ConfBtn(Icons.Rounded.BackHand, if (hand) "Опустить руку" else "Поднять руку", hand, onColor = Kx.warning) { hand = !hand }
            ConfBtn(Icons.Rounded.ScreenShare, "Показать экран", false) { }
            ConfBtn(Icons.Rounded.CallEnd, "Выйти из звонка", false, end = true) { ctx.back() }
        }
    }
}

/** Camera placeholder: a soft colour blob drifting (kxBlob ×2: translate 8%,-6% scale 1.1), 10% larger than the tile. */
@Composable
private fun BoxScope.BlobBg(p: ConfP, periodMs: Int) {
    val clock by rememberPlayClock(true, periodMs * 2L)
    val v = if (clock >= periodMs * 2L) 0f else pingPong(clock, periodMs, 0, 0f, 1f)
    Box(Modifier.matchParentSize().graphicsLayer { scaleX = 1.2f + .12f * v; scaleY = scaleX; translationX = size.width * .08f * v; translationY = -size.height * .06f * v }.camBg(p))
}

@Composable
private fun ConfBtn(icon: ImageVector, label: String, on: Boolean, onColor: Color = Kx.text, end: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(if (end) Color(0xFFE5534B) else if (on) onColor else Kx.w(.1f), tween(200), label = "b")
    val ink = if (end) Color.White else if (on) Kx.onAccent else Kx.text
    Box(Modifier.size(56.dp).clip(CircleShape).background(bg).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ink) }
}
