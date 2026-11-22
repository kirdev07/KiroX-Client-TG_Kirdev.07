package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Snd(val name: String, val icon: ImageVector, val sub: String, val grp: String)
private val SOUNDS = listOf(
    Snd("Без звука", Icons.Rounded.NotificationsOff, "Только вибрация", "kiroX"),
    Snd("Капля", Icons.Rounded.WaterDrop, "kiroX · 0,4 с", "kiroX"), Snd("Стекло", Icons.Rounded.WineBar, "kiroX · 0,6 с", "kiroX"),
    Snd("Росток", Icons.Rounded.Eco, "kiroX · 0,8 с", "kiroX"), Snd("Колокольчик", Icons.Rounded.Notifications, "kiroX · 1,0 с", "kiroX"),
    Snd("Аккорд", Icons.Rounded.Piano, "Системный · 1,2 с", "Системные"), Snd("Пинг", Icons.Rounded.Radar, "Системный · 0,5 с", "Системные"),
    Snd("Смех Лизы", Icons.Rounded.Mic, "Из голосового · 2,1 с", "Мои звуки"))
private val SCOPES = listOf("Личные чаты", "Группы", "Каналы")

/** Board «Звук уведомлений»: scope chips, now-playing card (ripples, swinging bell, progress waveform), sound list by group, custom sound. */
@Composable
fun SoundsScreen(ctx: KxScreenCtx) {
    // demo: needs store (notification sound per scope; saved custom sounds; real playback)
    var scope by rememberSaveable { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf(listOf(1, 3, 0)) }
    var playing by remember { mutableIntStateOf(-1) }
    var playId by remember { mutableIntStateOf(0) }
    var t by remember { mutableIntStateOf(0) }
    val toast = remember { TToast() }
    val cur = sel[scope]
    LaunchedEffect(playId) {
        if (playing < 0) return@LaunchedEffect
        t = 0
        while (t < 28) { delay(45); t++ }
        playing = -1; t = 0
    }
    val reduced = LocalKxReducedMotion.current

    TScreen("Звук уведомлений", ctx.back, sub = "Для: " + SCOPES[scope].lowercase(), bottomPad = 30.dp, overlay = { TToastHost(toast, 34.dp) }, above = {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SCOPES.forEachIndexed { i, l -> TChip(l, scope == i) { scope = i } }
        }
        // Now playing
        val nShape = RoundedCornerShape(26.dp)
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().clip(nShape).background(Kx.w(.04f))
            .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .3f), .6f to Color.Transparent, center = Offset(size.width, 0f), radius = size.width)) }
            .border(1.dp, Kx.accent.copy(alpha = .22f), nShape).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(64.dp)) {
                if (playing >= 0 && !reduced) {
                    listOf(0, 400).forEach { d ->
                        val r = remember(playId) { Animatable(0f) }
                        LaunchedEffect(playId) { delay(d.toLong()); repeat(2) { r.snapTo(0f); r.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing)) } }
                        if (r.value > 0f && r.value < 1f) Box(Modifier.matchParentSize().graphicsLayer { val s = .6f + 1.6f * r.value; scaleX = s; scaleY = s; alpha = .7f * (1f - r.value) }
                            .border(2.dp, Kx.accent, CircleShape))
                    }
                }
                val bell = remember { Animatable(0f) }
                LaunchedEffect(playId) {
                    if (playing < 0 || reduced) return@LaunchedEffect
                    repeat(2) { bell.snapTo(0f); bell.animateTo(1f, tween(800, easing = TEaseInOut)) }
                    bell.snapTo(0f)
                }
                Box(Modifier.matchParentSize().clip(CircleShape).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Notifications, null, Modifier.size(32.dp).graphicsLayer { rotationZ = bellAngle(bell.value); transformOrigin = TransformOrigin(.5f, .15f) }, tint = Kx.onAccent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Сейчас выбран", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                Text(SOUNDS[cur].name, style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 8.dp).height(26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val seed = cur * 7 + 3
                    repeat(28) { j ->
                        val h = if (cur == 0) 3 else Math.round(4 + kotlin.math.abs(kotlin.math.sin(j * .7 + seed)) * 18 * (1 - j / 40.0)).toInt()
                        val c by animateColorAsState(if (playing >= 0 && j < t) Kx.accent else Kx.w(.18f), tween(100), label = "w")
                        Box(Modifier.weight(1f).height(h.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    }
                }
            }
        }
    }) {
        Spacer(Modifier.height(8.dp))
        var last = ""
        SOUNDS.forEachIndexed { i, s ->
            if (s.grp != last) {
                last = s.grp
                Text(s.grp.uppercase(), style = KxType.style(12.sp, FontWeight.Bold, Kx.text4).copy(letterSpacing = .6.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp))
            }
            val on = cur == i
            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else Color.Transparent, tween(200), label = "bg")
            val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .4f) else Color.Transparent, tween(200), label = "bd")
            val shape = RoundedCornerShape(18.dp)
            Row(Modifier.padding(bottom = 4.dp).fillMaxWidth().clip(shape).background(bg).border(1.dp, bd, shape)
                .kxClick(role = Role.RadioButton) {
                    sel = sel.mapIndexed { k, v -> if (k == scope) i else v }
                    if (i == 0) { playing = -1; t = 0 } else { playing = i; playId++ }
                }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TIconTile(s.icon, if (on) Color.White else Kx.text3, if (on) Kx.accentDeep else Kx.w(.06f), 38.dp)
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = KxType.style(15.sp, FontWeight.Bold))
                    Text(s.sub, style = KxType.style(12.sp, color = Kx.text4))
                }
                TRadio(on, off = Kx.w(.3f))
            }
        }
        Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).dashedBorder(Kx.accent.copy(alpha = .4f), 18.dp)
            .kxClick { toast.show("Откроется выбор файла", Icons.Outlined.Upload) }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Upload, null, Modifier.size(24.dp), tint = Kx.accent)
            Text("Свой звук из файла", style = KxType.style(15.sp, FontWeight.Bold, Kx.accent))
        }
        THint("Подойдёт MP3 или OGG до 5 секунд и 300 КБ. Можно взять голосовое из любого чата: нажмите на него и выберите «Сделать звуком».", Modifier.padding(start = 4.dp, end = 4.dp), top = 10.dp)
    }
}

/** kxBell keyframes: 0/100 → 0°, 15% 16°, 30% -14°, 45% 10°, 60% -6°, 75% 0°. */
private fun bellAngle(p: Float): Float {
    val k = floatArrayOf(0f, .15f, .3f, .45f, .6f, .75f, 1f); val v = floatArrayOf(0f, 16f, -14f, 10f, -6f, 0f, 0f)
    for (i in 1 until k.size) if (p <= k[i]) { val f = (p - k[i - 1]) / (k[i] - k[i - 1]); return v[i - 1] + (v[i] - v[i - 1]) * f }
    return 0f
}
