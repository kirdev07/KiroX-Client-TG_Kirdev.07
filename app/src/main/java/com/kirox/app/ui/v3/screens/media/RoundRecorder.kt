package com.kirox.app.ui.v3.screens.media

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.ui.chat.RecordedVideoNote
import com.kirox.app.ui.chat.rememberVideoNoteCapture
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxTheme
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

/**
 * wave6 port-calls: the round-video recorder in the v3 look (Chat board «recCircle»: dark veil, 272dp circle with a
 * green ring filling over the minute, camera switch inside, blinking red dot + time, hint; VoiceRecord board buttons).
 * The camera side is the old recorder's CameraX code (ui/chat rememberVideoNoteCapture): tap to record, tap to send,
 * one minute at most, cropped to a square before sending.
 */
@Composable
fun KxRoundRecorder(onDone: (RecordedVideoNote) -> Unit, onCancel: () -> Unit, onError: (String) -> Unit) {
    val reduced = LocalKxReducedMotion.current
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        KxTheme(reduced) { RoundRecorderBody(onDone, onCancel, onError) }
    }
}

@Composable
private fun RoundRecorderBody(onDone: (RecordedVideoNote) -> Unit, onCancel: () -> Unit, onError: (String) -> Unit) {
    val note = rememberVideoNoteCapture(onDone, onCancel, onError)
    val recording = note.isRecording
    val processing = note.processing
    val elapsed = note.elapsedMs
    fun cancel() { if (recording) note.finish(canceled = true) else if (!processing) onCancel() }
    BackHandler { cancel() }
    // kxPop .4s: 0 → 1.04 → 1.
    val noMotion = LocalKxReducedMotion.current
    val pop = remember { Animatable(if (noMotion) 1f else 0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(400, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
    val blink by rememberLoopClock(recording)

    Column(Modifier.fillMaxSize().background(Color(0xF0050706)).statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(140.dp))
        Box(Modifier.size(272.dp).graphicsLayer {
            val v = pop.value
            val s = if (v < .7f) .6f + .44f * v / .7f else 1.04f - .04f * (v - .7f) / .3f
            scaleX = s; scaleY = s; alpha = (v / .5f).coerceAtMost(1f)
        }) {
            // conic-gradient(#8FD3AC 0 p%, rgba(255,255,255,.14) p% 100%) as a 5dp ring.
            val accent = Kx.accent; val track = Kx.w(.14f)
            Canvas(Modifier.matchParentSize()) {
                val w = 5.dp.toPx()
                val inset = w / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(track, 0f, 360f, false, tl, arcSize, style = Stroke(w))
                val p = (elapsed.toFloat() / note.maxMs).coerceIn(0f, 1f)
                if (p > 0f) drawArc(accent, -90f, 360f * p, false, tl, arcSize, style = Stroke(w, cap = StrokeCap.Butt))
            }
            Box(Modifier.padding(5.dp).fillMaxSize().clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2F6E4D), Color(0xFF173A28), Color(0xFF070B09))))) {
                AndroidView({ note.preview }, Modifier.matchParentSize())
                if (processing) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(36.dp), color = Kx.accent, strokeWidth = 3.dp)
                }
                if (!recording && !processing) Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 34.dp).size(40.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = .4f)).kxClick("Сменить камеру") { note.flip() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Cameraswitch, null, Modifier.size(22.dp), tint = Kx.text)
                }
            }
        }
        Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recording) {
                // kxBlink 1s: opacity 1 → .2 → 1.
                val a = if (noMotion) 1f else pingPong(blink, 1000, 0, 1f, .2f)
                Box(Modifier.size(10.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Color(0xFFFF5A5A)))
            }
            T(when { processing -> "Готовим кружок…"; recording -> "%d:%02d".format(elapsed / 60_000, elapsed / 1000 % 60); else -> "0:00" }, 16.sp, FontWeight.SemiBold)
        }
        T(when {
            processing -> "Ещё секунду — обрезаем видео в кружок"
            recording -> "Нажмите на кнопку — кружок отправится"
            else -> "Нажмите на кнопку, чтобы записать кружок · до 1 минуты"
        }, 13.sp, color = Color(0xFF9AA59F), modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE5534B).copy(alpha = .12f)).border(1.dp, Color(0xFFE5534B).copy(alpha = .3f), CircleShape)
                .kxClick(if (recording) "Удалить запись" else "Закрыть", enabled = !processing) { cancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF8A80))
            }
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                if (recording) CallRings(Modifier.matchParentSize(), color = Kx.accent.copy(alpha = .45f), periodMs = 1200, toScale = 1.5f, fromAlpha = .55f, fill = true)
                Box(Modifier.size(72.dp).clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep)
                    .kxClick(if (recording) "Отправить кружок" else "Записать кружок", enabled = !processing) { if (recording) note.finish(canceled = false) else note.start() },
                    contentAlignment = Alignment.Center) {
                    Icon(if (recording) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Videocam, null, Modifier.size(32.dp), tint = Kx.onAccent)
                }
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(if (recording || processing) .04f else .08f)).border(1.dp, Kx.w(.12f), CircleShape)
                .kxClick("Сменить камеру", enabled = !recording && !processing) { note.flip() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Cameraswitch, null, tint = if (recording || processing) Kx.text4 else Kx.text)
            }
        }
    }
}
