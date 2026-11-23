package com.kirox.app.ui.v3.screens.media

import androidx.compose.material.icons.rounded.Close
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
    // critique-C P2: a finished recording waits in a preview («Удалить» / «Отправить») instead of going out at once.
    var pending by remember { mutableStateOf<RecordedVideoNote?>(null) }
    val note = rememberVideoNoteCapture({ pending = it }, onCancel, onError)
    pending?.let { done ->
        BackHandler { runCatching { done.file.delete() }; pending = null }
        RoundPreview(done, onDelete = { runCatching { done.file.delete() }; pending = null }, onSend = { pending = null; onDone(done) })
        return
    }
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
        // qa-real2: the hint is centred under the circle / timer (it was left-aligned and wrapped).
        androidx.compose.material3.Text(when {
            processing -> "Ещё секунду — обрезаем видео в кружок"
            recording -> "Нажмите на кнопку, чтобы остановить — потом можно посмотреть"
            else -> "Нажмите на кнопку, чтобы записать кружок · до 1 минуты"
        }, style = ts(13.sp, FontWeight.Normal, Color(0xFF9AA59F)).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 24.dp, end = 24.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // critique-C P2: «Удалить запись» (red, while recording) and «Закрыть» (quiet) are different buttons.
            if (recording) Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE5534B).copy(alpha = .12f)).border(1.dp, Color(0xFFE5534B).copy(alpha = .3f), CircleShape)
                .kxClick("Удалить запись", enabled = !processing) { cancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF8A80))
            } else Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(.08f)).border(1.dp, Kx.w(.12f), CircleShape)
                .kxClick("Закрыть", enabled = !processing) { cancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = Kx.text)
            }
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                if (recording) CallRings(Modifier.matchParentSize(), color = Kx.accent.copy(alpha = .45f), periodMs = 1200, toScale = 1.5f, fromAlpha = .55f, fill = true)
                Box(Modifier.size(72.dp).clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep)
                    .kxClick(if (recording) "Остановить запись" else "Записать кружок", enabled = !processing) { if (recording) note.finish(canceled = false) else note.start() },
                    contentAlignment = Alignment.Center) {
                    if (recording) Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(Kx.onAccent))
                    else Icon(Icons.Rounded.Videocam, null, Modifier.size(32.dp), tint = Kx.onAccent)
                }
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(if (recording || processing) .04f else .08f)).border(1.dp, Kx.w(.12f), CircleShape)
                .kxClick("Сменить камеру", enabled = !recording && !processing) { note.flip() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Cameraswitch, null, tint = if (recording || processing) Kx.text4 else Kx.text)
            }
        }
    }
}

/** critique-C P2: the recorded round video loops in the circle; «Удалить» drops the file, «Отправить» sends it. */
@Composable
private fun RoundPreview(note: RecordedVideoNote, onDelete: () -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xF0050706)).statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(140.dp))
        Box(Modifier.size(272.dp).clip(CircleShape).background(Color(0xFF070B09)).border(5.dp, Kx.accent, CircleShape)) {
            AndroidView({ c ->
                android.widget.VideoView(c).apply {
                    setVideoPath(note.file.absolutePath)
                    setOnPreparedListener { mp -> mp.isLooping = true; start() }
                }
            }, Modifier.matchParentSize().padding(5.dp).clip(CircleShape), onRelease = { it.stopPlayback() })
        }
        T("%d:%02d".format(note.durationSeconds / 60, note.durationSeconds % 60), 16.sp, FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
        androidx.compose.material3.Text("Посмотрите кружок перед отправкой", style = ts(13.sp, FontWeight.Normal, Color(0xFF9AA59F)),
            modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE5534B).copy(alpha = .12f)).border(1.dp, Color(0xFFE5534B).copy(alpha = .3f), CircleShape)
                .kxClick("Удалить запись", onClick = onDelete), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF8A80)) }
            Box(Modifier.size(72.dp).clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить кружок", onClick = onSend), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(32.dp), tint = Kx.onAccent)
            }
            Spacer(Modifier.size(52.dp))
        }
    }
}
