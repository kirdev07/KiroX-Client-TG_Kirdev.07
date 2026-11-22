package com.kirox.app.ui.v3.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*

/** Calls tab (board "Звонки"): segmented All/Missed, call-link card, call rows. */
@Composable
fun CallsScreen(store: KxStore, onCall: (KxCall, Boolean) -> Unit, onOpen: (String) -> Unit, bottomPadding: Dp = 110.dp) {
    var missed by rememberSaveable { mutableStateOf(false) }
    val calls = if (missed) store.calls.filter { it.dir == KxCallDir.Missed } else store.calls
    Box(Modifier.fillMaxSize()) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = .18f, alignEnd = true)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Звонки", style = KxType.style(30.sp, FontWeight.ExtraBold).copy(letterSpacing = (-.4).sp), modifier = Modifier.weight(1f))
                KxIconButton(androidx.compose.material.icons.Icons.Outlined.Search, "Поиск", size = 42.dp, glass = true) { onOpen("Search") }
                Box(Modifier.size(42.dp).shadow(18.dp, CircleShape, spotColor = Color(0x593F9A6C)).clip(CircleShape).background(Kx.accentGradient).kxClick("Новый звонок") { onOpen("NewCall") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AddIcCall, null, Modifier.size(22.dp), tint = Kx.onAccent)
                }
            }
            Segmented(missed, store.missedCalls) { missed = it }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0x333F9A6C), Kx.w(.03f)))).border(1.dp, Kx.accent.copy(alpha = .16f), RoundedCornerShape(22.dp))
                .kxClick { onOpen("CallLink") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Link, null, tint = Kx.accent) }
                Column(Modifier.weight(1f)) {
                    Text("Ссылка на звонок", style = KxType.style(15.sp, FontWeight.Bold))
                    Text("Позовите друзей в групповой звонок", style = KxType.style(12.sp, color = Kx.text3))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Kx.accent)
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 10.dp), contentPadding = PaddingValues(bottom = bottomPadding)) {
                item { Text("Сегодня", style = KxType.style(13.sp, FontWeight.Bold, Kx.text4), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) }
                itemsIndexed(calls, key = { _, c -> c.id }) { i, c -> CallRow(c, Modifier.kxRise(i * 50, 450)) { onCall(c, c.video) } }
                if (calls.isEmpty()) item { KxEmpty(if (missed) "Пропущенных звонков нет" else "Звонков пока не было") }
            }
        }
    }
}

@Composable
private fun Segmented(missed: Boolean, missedCount: Int, onPick: (Boolean) -> Unit) {
    val x by animateFloatAsState(if (missed) 1f else 0f, tween(450, easing = CubicBezierEasing(.34f, 1.4f, .5f, 1f)), label = "seg")
    BoxWithConstraints(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
        .background(Kx.w(.05f)).border(1.dp, Kx.line, RoundedCornerShape(20.dp)).padding(3.dp)) {
        val half = maxWidth / 2
        Box(Modifier.graphicsLayer { translationX = x * half.toPx() }.size(half, 32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .18f)).topHighlight(Kx.w(.14f)))
        Row(Modifier.fillMaxSize()) {
            listOf(false, true).forEach { m ->
                val c by animateColorAsState(if (m == missed) Kx.text else Kx.text4, label = "segc")
                Box(Modifier.weight(1f).fillMaxHeight().kxClick(role = androidx.compose.ui.semantics.Role.Tab) { onPick(m) }, contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (m) "Пропущенные" else "Все", style = KxType.style(14.sp, FontWeight.SemiBold, c))
                        if (m && missedCount > 0) Text(missedCount.toString(), style = KxType.style(14.sp, FontWeight.SemiBold, Kx.danger))
                    }
                }
            }
        }
    }
}

fun callInfo(c: KxCall): String {
    if (c.group) return listOf("Видеочат", c.time, c.duration).filter { it.isNotEmpty() }.joinToString(" · ")
    val kind = when (c.dir) {
        KxCallDir.Incoming -> if (c.video) "Входящий видеозвонок" else "Входящий"
        KxCallDir.Outgoing -> if (c.video) "Исходящий видеозвонок" else "Исходящий"
        KxCallDir.Missed -> if (c.video) "Пропущенный видеозвонок" else "Пропущенный"
        KxCallDir.Declined -> "Отклонённый"
    }
    return listOf(kind + if (c.count > 1) " (${c.count})" else "", c.time, c.duration).filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun CallRow(c: KxCall, modifier: Modifier, onCall: () -> Unit) {
    val missed = c.dir == KxCallDir.Missed
    Row(modifier.fillMaxWidth().height(68.dp).padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        KxAvatar(c.name, c.peerId, size = 50.dp, colorIndex = c.colorIndex)
        Column(Modifier.weight(1f)) {
            KxLine(c.name, KxType.style(16.sp, FontWeight.SemiBold, if (missed) Kx.danger else Kx.text))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(when (c.dir) { KxCallDir.Incoming -> Icons.AutoMirrored.Rounded.CallReceived; KxCallDir.Outgoing -> Icons.AutoMirrored.Rounded.CallMade; else -> Icons.AutoMirrored.Rounded.CallMissed },
                    null, Modifier.size(16.dp), tint = if (missed || c.dir == KxCallDir.Declined) Kx.danger else Kx.accent)
                KxLine(callInfo(c), KxType.style(13.sp, color = Kx.text4))
            }
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .12f)).kxClick((if (c.video) "Видеозвонок: " else "Позвонить: ") + c.name, onClick = onCall),
            contentAlignment = Alignment.Center) { Icon(if (c.video) Icons.Rounded.Videocam else Icons.Rounded.Call, null, Modifier.size(22.dp), tint = Kx.accent) }
    }
}

/** Outgoing call screen (inline in the Calls board): pulsing rings, status dots, glass control bar. */
@Composable
fun CallingScreen(name: String, peerId: Long, video: Boolean = false, colorIndex: Int = -1, onEnd: () -> Unit) {
    var muted by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2200); connected = true }
    val t = rememberInfiniteTransition(label = "ring")
    val reduced = LocalKxReducedMotion.current
    val p1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)), label = "r1")
    val p2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, delayMillis = 900, easing = LinearOutSlowInEasing)), label = "r2")
    val up = remember { Animatable(0f) }
    LaunchedEffect(Unit) { up.animateTo(1f, tween(450, easing = KxMotion.rise)) }
    val upBar = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(100); upBar.animateTo(1f, tween(500, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize().drawBehind {
        // radial-gradient(90% 60% at 50% 30%, #17402C 0%, #0A0C0B 70%)
        drawRect(Kx.bg)
        val c = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .3f); val rx = size.width * .9f; val ry = size.height * .6f
        scale(1f, ry / rx, c) {
            drawCircle(Brush.radialGradient(0f to Color(0xFF17402C), .7f to Kx.bg, center = c, radius = rx), radius = rx, center = c)
        }
    }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(140.dp).graphicsLayer { translationY = (1 - up.value) * 40.dp.toPx(); val s = .96f + .04f * up.value; scaleX = s; scaleY = s; alpha = up.value }, contentAlignment = Alignment.Center) {
                if (!reduced) listOf(p1 to .35f, p2 to .25f).forEach { (p, a) ->
                    Box(Modifier.matchParentSize().graphicsLayer { scaleX = 1f + p * .9f; scaleY = scaleX; alpha = .5f * (1f - p) }.clip(CircleShape).background(Kx.accent.copy(alpha = a)))
                }
                KxAvatar(name, peerId, Modifier.shadow(50.dp, CircleShape, spotColor = Color.Black.copy(alpha = .5f)), size = 140.dp, colorIndex = colorIndex, letterWeight = FontWeight.ExtraBold, letterSize = 54.sp)
            }
            Text(name, style = KxType.style(28.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 28.dp))
            Row(Modifier.padding(top = 6.dp)) { KxTypingDots(Kx.accent, if (connected) "Идёт звонок" else if (video) "Видеовызов" else "Вызов", 15.sp) }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = Kx.text4)
                Text("Защищено сквозным шифрованием", style = KxType.caption)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.padding(bottom = 26.dp).graphicsLayer { translationY = (1 - upBar.value) * 40.dp.toPx(); val s = .96f + .04f * upBar.value; scaleX = s; scaleY = s; alpha = upBar.value }
                .clip(RoundedCornerShape(36.dp)).background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.04f))))
                .border(1.dp, Kx.stroke, RoundedCornerShape(36.dp)).topHighlight(Kx.w(.3f)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CallBtn(androidx.compose.material.icons.Icons.Outlined.VolumeUp, "Динамик", false) {}
                CallBtn(androidx.compose.material.icons.Icons.Outlined.VideocamOff, "Камера", false) {}
                CallBtn(if (muted) androidx.compose.material.icons.Icons.Outlined.MicOff else androidx.compose.material.icons.Icons.Outlined.Mic, "Микрофон", muted) { muted = !muted }
                Box(Modifier.size(56.dp).shadow(20.dp, CircleShape, spotColor = Color(0x66E5484D)).clip(CircleShape).background(Color(0xFFE5484D)).kxClick("Завершить", onClick = onEnd),
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.CallEnd, null, tint = Color.White) }
            }
        }
    }
}

@Composable
private fun CallBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.text else Kx.w(.10f), label = "cb")
    Box(Modifier.size(56.dp).clip(CircleShape).background(bg).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (on) Kx.bg else Kx.text)
    }
}
