package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: the real recorder lives in ChatScreen (store.recordStart/recordStop); this board shows the gestures with demo buttons.
private val VR_WAVE = listOf(8, 16, 22, 12, 18, 10, 20, 14, 8, 16)
private val VR_DELAYS = listOf(0, 100, 200, 300, 150, 250, 50, 350, 120, 220)

/** VoiceRecord board: hold to record, slide left to cancel (mic into the bin), slide up to lock, pause / delete / send. */
@Composable
fun VoiceRecordScreen(ctx: KxScreenCtx) {
    // critique-A: the board opens in its key state — recording (timer, «Влево — отмена», lock ↑).
    var mode by remember { mutableStateOf("hold") }
    var sec by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val sent = remember { mutableStateListOf<Pair<Int, String>>() }
    var nid by remember { mutableIntStateOf(0) }
    var trashK by remember { mutableIntStateOf(0) }
    val rec = mode == "hold" || mode == "lock"
    val t = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

    LaunchedEffect(mode, paused) { while ((mode == "hold" || mode == "lock") && !paused) { delay(1000); sec++ } }
    LaunchedEffect(trashK) { if (trashK > 0) { delay(1100); if (mode == "trash") mode = "idle" } }

    fun start() { mode = "hold"; sec = 0; paused = false }
    fun send() { sent.add(nid++ to (if (t == "0:00") "0:01" else t)); while (sent.size > 3) sent.removeAt(0); mode = "idle" }
    fun cancel() { mode = "trash"; trashK++ }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "А", Color(0xFFC0443C), null, "Анна", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    MoIncoming("Расскажи голосом, как прошло?", modifier = Modifier.widthIn(max = w * .78f))
                    sent.forEach { (id, d) ->
                        key(id) {
                            val p = moEnter(300, easing = KxMotion.rise)
                            Row(Modifier.align(Alignment.End).width(250.dp).graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
                                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).moAccent().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x2E06150D)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(24.dp), tint = MoInk)
                                }
                                Row(Modifier.weight(1f).height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    VR_WAVE.forEach { h -> Box(Modifier.weight(1f).height(h.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x7306150D))) }
                                }
                                Text(d, style = mts(12, W7, MoInk))
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().moTopLine().heightIn(min = 66.dp).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (mode) {
                    "idle" -> {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AddCircle, null, Modifier.size(24.dp), tint = Kx.text2) }
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Сообщение", style = mts(15, color = MoMuted))
                        }
                    }
                    "trash" -> {
                        key(trashK) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                val mp = moEnter(700, easing = androidx.compose.animation.core.CubicBezierEasing(.42f, 0f, 1f, 1f))
                                Icon(Icons.Rounded.Mic, null, Modifier.size(22.dp).graphicsLayer {
                                    translationY = moKf(mp, 0f to 0f, .4f to -40f, 1f to 20f, easing = androidx.compose.animation.core.LinearEasing) * density
                                    val s = moKf(mp, 0f to 1f, .4f to .6f, 1f to .2f, easing = androidx.compose.animation.core.LinearEasing); scaleX = s; scaleY = s
                                    rotationZ = moKf(mp, 0f to 0f, .4f to -20f, 1f to 0f, easing = androidx.compose.animation.core.LinearEasing)
                                    alpha = if (mp < .4f) 1f else 1f - (mp - .4f) / .6f
                                }, tint = Color(0xFFFF5A4E))
                                val lp = moEnter(900, easing = androidx.compose.animation.core.LinearEasing)
                                Icon(Icons.Outlined.Delete, null, Modifier.size(28.dp).graphicsLayer {
                                    transformOrigin = TransformOrigin(.2f, .3f)
                                    rotationZ = moKf(lp, 0f to 0f, .3f to 0f, .5f to -40f, .8f to -40f, 1f to 0f, easing = MoEase)
                                }, tint = MoRedInk)
                            }
                        }
                        Text("Запись удалена", style = mts(14, W7, MoRedInk), modifier = Modifier.weight(1f))
                    }
                    else -> {
                        Row(Modifier.widthIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val b = moLoop(1000, label = "rec")
                            Box(Modifier.size(10.dp).graphicsLayer { alpha = 1f - .7f * MoEaseInOut.transform(moTri(b)) }.clip(CircleShape).background(Color(0xFFFF5A4E)))
                            Text(t, style = mts(15, W8).copy(fontFeatureSettings = "tnum"))
                        }
                        if (mode == "hold") {
                            val h = moRun(1400, 2)
                            val v = if (h < 0f) 0f else MoEaseInOut.transform(moTri(h))
                            Row(Modifier.weight(1f).graphicsLayer { translationX = -8.dp.toPx() * v; alpha = .6f + .4f * v }, verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                                Icon(Icons.Outlined.ChevronLeft, null, Modifier.size(18.dp), tint = MoSub)
                                Text("Влево — отмена", style = mts(13, color = MoSub))
                            }
                        } else {
                            Row(Modifier.weight(1f).height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                VR_DELAYS.forEach { d -> VrBar(d, paused) }
                            }
                            Box(Modifier.size(42.dp).clip(CircleShape).background(MoRed.copy(alpha = .12f)).kxClick("Удалить запись") { cancel() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Delete, null, Modifier.size(24.dp), tint = MoRedInk)
                            }
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick(if (paused) "Продолжить запись" else "Пауза") { paused = !paused },
                                contentAlignment = Alignment.Center) { Icon(if (paused) Icons.Rounded.Mic else Icons.Rounded.Pause, null, Modifier.size(24.dp), tint = Kx.text) }
                        }
                    }
                }
                val big = mode == "hold"
                val bs by animateDpAsState(if (big) 76.dp else 52.dp, tween(250, easing = KxMotion.rise), label = "bs")
                val ow by animateDpAsState(if (big) 68.dp else 52.dp, tween(250, easing = KxMotion.rise), label = "ow")
                val oh by animateDpAsState(if (big) 48.dp else 52.dp, tween(250, easing = KxMotion.rise), label = "oh")
                Box(Modifier.size(ow, oh), contentAlignment = Alignment.Center) {
                    Box(Modifier.offset(x = (bs - ow) / 2).requiredSize(bs), contentAlignment = Alignment.Center) {
                        if (big) {
                            val bl = moRun(1000, 2)
                            Box(Modifier.requiredSize(bs + 20.dp).graphicsLayer { val s = 1f + .12f * (if (bl < 0f) 0f else MoEaseInOut.transform(moTri(bl))); scaleX = s; scaleY = s }
                                .clip(CircleShape).background(Kx.accent.copy(alpha = .22f)))
                        }
                        Box(Modifier.fillMaxSize().clip(CircleShape).then(if (rec) Modifier.moAccent() else Modifier.background(Kx.w(.07f)))
                            .kxClick(if (mode == "lock") "Отправить голосовое" else "Удерживайте для записи") {
                                when (mode) { "idle" -> start(); "lock", "hold" -> send() }
                            }, contentAlignment = Alignment.Center) {
                            Icon(if (mode == "lock") Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic, null, Modifier.size(if (big) 34.dp else 24.dp), tint = if (rec) MoInk else MoSub)
                        }
                    }
                }
            }

            // critique-A S: gesture switchers are demo controls (dashed «демо» tray, demo store only).
            com.kirox.app.ui.v3.components.KxDemoControls(ctx.store, Modifier.padding(bottom = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VrGesture(Icons.Outlined.TouchApp, "Удержать", mode == "idle") { start() }
                    VrGesture(Icons.Outlined.West, "Влево", mode == "hold") { cancel() }
                    VrGesture(Icons.Outlined.North, "Вверх", mode == "hold") { mode = "lock" }
                    VrGesture(Icons.AutoMirrored.Outlined.Send, if (mode == "lock") "Отправить" else "Отпустить", rec) { send() }
                }
            }
        }

        if (mode == "hold") {
            val p = moEnter(200)
            Column(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 170.dp).width(44.dp)
                .graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx() }.clip(RoundedCornerShape(22.dp)).background(Kx.w(.1f))
                .border(1.dp, Kx.w(.14f), RoundedCornerShape(22.dp)).padding(top = 10.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LockOpen, null, Modifier.size(20.dp), tint = Kx.text)
                val u = moRun(1200, 2)
                Icon(Icons.Outlined.KeyboardArrowUp, null, Modifier.size(18.dp).graphicsLayer { translationY = -6.dp.toPx() * (if (u < 0f) 0f else MoEaseInOut.transform(moTri(u))) }, tint = MoSub)
            }
        }
    }
}

/** kxBar: scaleY .4 ↔ 1, .6 s, offset by [delayMs]; holds still while paused. */
@Composable
private fun RowScope.VrBar(delayMs: Int, paused: Boolean) {
    val v = moLoop(600, delayMs = -delayMs, label = "bar")
    val frozen = remember { floatArrayOf(0f) }
    if (!paused) frozen[0] = v
    val s = .4f + .6f * MoEaseInOut.transform(moTri(frozen[0]))
    Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleY = s }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
}

@Composable
private fun RowScope.VrGesture(icon: ImageVector, label: String, can: Boolean, onClick: () -> Unit) {
    val ink = if (can) Kx.accent else MoMuted
    Column(Modifier.weight(1f).height(44.dp).graphicsLayer { alpha = if (can) 1f else .35f }.clip(RoundedCornerShape(14.dp))
        .background(if (can) Kx.accent.copy(alpha = .12f) else Kx.w(.04f)).kxClick(label) { if (can) onClick() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(18.dp), tint = ink)
        Text(label, style = mts(11, W8, ink))
    }
}
