package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private class CwP(val name: String, val l: String, val a: Color, val b: Color)
private val CwPeople = mapOf("a" to CwP("Анна", "А", Color(0xFFC0443C), Color(0xFF6A4C9C)), "m" to CwP("Мама", "М", Color(0xFFB07A2E), Color(0xFF6A4C9C)))
private fun mmss(t: Int) = "${t / 60}:" + (t % 60).toString().padStart(2, '0')

/**
 * Board «Второй звонок»: active call (ring, waves, timer), incoming second call card (decline / end & answer / decline with message),
 * hold strip with swap, merge, hang-up returns to the held call. The incoming call is a simulation → only on the demo store.
 */
@Composable
fun CallWaitingScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → the live call; Telegram has no call waiting / hold
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoCallWaitingScreen(ctx) else com.kirox.app.ui.v3.screens.media.RealCallBoard(ctx, com.kirox.app.ui.v3.screens.media.CallBoard.Waiting)
}

@Composable
private fun DemoCallWaitingScreen(ctx: KxScreenCtx) {
    // demo: needs store (call state: active + held calls, incoming while in a call, merge into a group call)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var active by remember { mutableStateOf("a") }
    var hold by remember { mutableStateOf("") }
    var incoming by remember { mutableStateOf(demo) }
    val t = remember { mutableStateMapOf("a" to 312, "m" to 0) }
    var mute by remember { mutableStateOf(false) }
    var merged by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var noteN by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); if (active.isNotEmpty()) t[active] = (t[active] ?: 0) + 1; if (hold.isNotEmpty()) t[hold] = (t[hold] ?: 0) + 1 } }
    LaunchedEffect(noteN) { if (noteN > 0) { delay(2400); note = "" } }
    fun flash(s: String) { note = s; noteN++ }
    val a = CwPeople[active.ifEmpty { "a" }]!!
    val h = CwPeople[hold]
    Box(Modifier.fillMaxSize().background(Color(0xFF070908))) {
        val b1 = tri(kfRun(10000)); val b2 = tri(kfRun(12000))
        val blob by animateColorAsState(if (hold.isNotEmpty()) Kx.warning.copy(alpha = .25f) else Kx.accentDeep.copy(alpha = .4f), tween(600), label = "blob")
        Box(Modifier.offset((-80).dp, 80.dp).size(340.dp).graphicsLayer { translationX = 20.dp.toPx() * b1; translationY = -30.dp.toPx() * b1; val s = 1f + .15f * b1; scaleX = s; scaleY = s }.circleGlow(blob))
        Box(Modifier.align(Alignment.TopEnd).offset(90.dp, 300.dp).size(300.dp).graphicsLayer { translationX = 20.dp.toPx() * b2; translationY = -30.dp.toPx() * b2; val s = 1f + .15f * b2; scaleX = s; scaleY = s }
            .circleGlow(Color(0x4D2F6E9E)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (h != null && !incoming) Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).exIn(300, dy = (-20).dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Kx.warning.copy(alpha = .14f)).border(1.dp, Kx.warning.copy(alpha = .35f), RoundedCornerShape(18.dp)).kxClick("Переключить") { val x = active; active = hold; hold = x }
                .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExLetter(h.l, 34.dp, h.a, h.b, 135f, Kx.text, 14.sp)
                Column(Modifier.weight(1f)) {
                    Text("${h.name} · на удержании", style = ts(14.sp, FontWeight.Bold))
                    Text(mmss(t[hold] ?: 0), style = ts(12.sp, color = Color(0xFFE4D3B0)).copy(fontFeatureSettings = "tnum"))
                }
                Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.warning.copy(alpha = .22f)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.SwapCalls, null, Modifier.size(16.dp), tint = Kx.warning); Text("Переключить", style = ts(12.sp, FontWeight.ExtraBold, Kx.warning))
                }
            }
            // critique-B: while the incoming card is shown the current caller moves below it (the name was half hidden).
            val topPad by animateDpAsState(if (incoming) 236.dp else 50.dp, tween(350), label = "cwTop")
            val ava by animateDpAsState(if (incoming) 96.dp else 130.dp, tween(350), label = "cwAva")
            key(active) {
                val sw = enter(350)
                Column(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = sw; val s = .92f + .08f * sw; scaleX = s; scaleY = s }.padding(top = topPad), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(ava)) {
                        val r = kfLoop(2400, label = "ring")
                        Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .7f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .6f * (1f - r) }.border(2.dp, Kx.accent.copy(alpha = .45f), CircleShape))
                        ExLetter(if (merged) "2" else a.l, ava, a.a, a.b, 135f, Kx.text, if (incoming) 38.sp else 50.sp)
                    }
                    Text(if (merged) "Анна и Мама" else a.name, style = ts(26.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { i ->
                                val w = kfLoop(800, i * 150, "w$i")
                                Box(Modifier.size(3.dp, 14.dp).graphicsLayer { scaleY = .3f + .7f * tri(w) }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                            }
                        }
                        Text(if (active.isNotEmpty()) mmss(t[active] ?: 0) else "", style = ts(15.sp, color = Kx.text2).copy(fontFeatureSettings = "tnum"))
                    }
                }
            }
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 34.dp)) {
                // critique-C P1: the shared call control row (white = on, red end last, no separate big button).
                var speaker by remember { mutableStateOf(false) }
                KxCallControls(listOf(
                    KxCallButton(if (mute) Icons.Rounded.MicOff else Icons.Rounded.Mic, "Микрофон", !mute) { mute = !mute },
                    KxCallButton(Icons.Rounded.VolumeUp, "Динамик", speaker) { speaker = !speaker },
                    KxCallButton(Icons.Rounded.CallMerge, "Объединить", merged) { if (hold.isNotEmpty()) { merged = true; hold = ""; flash("Теперь это общий звонок на троих") } else flash("Объединить можно, когда второй звонок на удержании") },
                    KxCallButton(Icons.Rounded.AddIcCall, "Добавить") { if (demo) incoming = true else ctx.toast("Добавить участника пока нельзя") },
                    KxCallButton(Icons.Rounded.CallEnd, "Завершить", end = true) {
                        if (hold.isNotEmpty()) { val back = hold; active = back; hold = ""; merged = false; flash("Вернулись к звонку с " + if (back == "a") "Анной" else "Мамой") }
                        else { active = "a"; hold = ""; incoming = demo; merged = false; t["a"] = 312; t["m"] = 0 }
                    },
                ))
            }
        }
        if (incoming) Column(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 14.dp).exIn(400, dy = (-20).dp, easing = KxMotion.rise).fillMaxWidth()
            .boxShadow(Color.Black.copy(alpha = .5f), 50.dp, 20.dp, 26.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xF0161A18))
            .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.18f), RoundedCornerShape(26.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp)) {
                    val r = kfLoop(1200, label = "ir")
                    Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .7f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .6f * (1f - r) }.border(2.dp, Kx.accent, CircleShape))
                    ExLetter("М", 48.dp, Color(0xFFB07A2E), Color(0xFF6A4C9C), 135f, Kx.text, 19.sp)
                }
                Column(Modifier.weight(1f)) { Text("Мама", style = ts(16.sp, FontWeight.ExtraBold)); Text("звонит вам · kiroX", style = ts(13.sp, color = Kx.text2)) }
                val sh = kfRun(1200)
                Icon(Icons.Rounded.RingVolume, null, Modifier.size(24.dp).graphicsLayer {
                    rotationZ = if (sh < 0f) 0f else kfLerp(sh, floatArrayOf(0f, .1f, .2f, .3f, .4f, .5f, 1f), floatArrayOf(0f, -12f, 12f, -12f, 12f, 0f, 0f))
                }, tint = Kx.accent)
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CwBtn(Modifier.weight(1f), Icons.Rounded.CallEnd, "Отклонить", Kx.dangerSolid.copy(alpha = .2f), Kx.danger) { incoming = false; flash("Отклонён звонок · Мама") }
                CwBtn(Modifier.weight(1f), Icons.Rounded.PhoneForwarded, "Завершить и ответить", Kx.w(.1f), Kx.text) { incoming = false; active = "m"; hold = ""; flash("Звонок завершён · Анна") }
            }
            Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp)).kxClick("Отклонить и написать") { incoming = false; flash("Отправлено · Мама: «Перезвоню через 5 минут»") },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Sms, null, Modifier.size(16.dp), tint = Kx.text2)
                Text("Отклонить и написать «Перезвоню через 5 минут»", style = ts(12.sp, FontWeight.Bold, Kx.text2), maxLines = 1)
            }
        }
        if (note.isNotEmpty()) key(noteN) {
            Text(note, style = ts(13.sp, color = Kx.text2), textAlign = TextAlign.Center, modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp).exIn(300, dy = (-20).dp)
                .fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.08f)).padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun CwBtn(modifier: Modifier, icon: ImageVector, label: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Column(modifier.height(58.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(22.dp), tint = ink)
        Text(label, style = ts(11.sp, FontWeight.Bold, ink), textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}
