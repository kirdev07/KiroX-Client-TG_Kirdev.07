package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private const val RaText = "Вышел Compose 1.9. Общие переходы между экранами стали стабильными, ленивые списки работают быстрее на треть, а превью теперь понимают тёмную тему. Обновляйтесь и делитесь впечатлениями в комментариях."
private val RaW = RaText.split(' ')
private val RaSp = listOf("0,8×" to .8f, "1×" to 1f, "1,5×" to 1.5f, "2×" to 2f)

/**
 * Board «Читать вслух»: channel post with the spoken word highlighted (tap a word to read from it), player (ring, progress, eq), speed and voice.
 * Speaks for real with the phone's TextToSpeech (word highlight from onRangeStart); falls back to the board's timer when no TTS engine is ready.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadAloudScreen(ctx: KxScreenCtx) {
    // demo: needs store (the post comes from the chat; here it is the board's sample post)
    var i by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var voice by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var gen by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val tts = remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    val base = remember { intArrayOf(0) }
    val offs = remember { mutableListOf<Int>() }
    DisposableEffect(Unit) {
        val holder = arrayOfNulls<android.speech.tts.TextToSpeech>(1)
        val t = android.speech.tts.TextToSpeech(context.applicationContext) { st ->
            if (st == android.speech.tts.TextToSpeech.SUCCESS) { holder[0]?.language = java.util.Locale("ru"); main.post { ready = true } }
        }
        holder[0] = t
        t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { main.post { if (id == "ra$gen") { i = RaW.size - 1; playing = false } } }
            @Deprecated("old api") override fun onError(id: String?) {}
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                main.post { if (id == "ra$gen") { val k = offs.indexOfLast { it <= start }.coerceAtLeast(0); i = base[0] + k } }
            }
        })
        tts.value = t
        onDispose { t.stop(); t.shutdown(); tts.value = null }
    }
    // (re)start speaking whenever play state / start word / speed / voice changes
    LaunchedEffect(playing, ready, speed, voice, gen) {
        val t = tts.value
        if (!ready || t == null) return@LaunchedEffect
        if (!playing) { t.stop(); return@LaunchedEffect }
        base[0] = i; offs.clear()
        var o = 0; RaW.drop(i).forEach { w -> offs += o; o += w.length + 1 }
        t.setSpeechRate(speed); t.setPitch(if (voice == 0) .92f else 1.15f)
        t.speak(RaW.drop(i).joinToString(" "), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "ra$gen")
    }
    // fallback timer (no TTS engine)
    LaunchedEffect(playing, ready, speed) { if (!ready) while (playing) { delay((380 / speed).toLong()); if (i < RaW.size - 1) i++ else playing = false } }
    fun mmss(t: Int) = "${t / 60}:" + (t % 60).toString().padStart(2, '0')
    fun secs(w: Int) = Math.round(w * .38f / speed)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter("A", 40.dp, Kx.accentDeep, Color(0xFF2F6E9E), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Android Weekly", style = ts(16.sp, FontWeight.ExtraBold)); Text("канал · 18,4 тыс.", style = ts(12.sp, color = Kx.text3)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true).padding(14.dp)) {
                    val bd by animateColorAsState(if (playing) Kx.accent.copy(alpha = .35f) else Color.Transparent, tween(300), label = "bd")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Kx.w(.07f)).border(1.dp, bd, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).padding(14.dp)) {
                        Text("Android Weekly", style = ts(12.sp, FontWeight.ExtraBold, Kx.accent))
                        FlowRow(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
                            RaW.forEachIndexed { k, w ->
                                val bg by animateColorAsState(if (k == i) Kx.accent else Color.Transparent, tween(150), label = "wb")
                                val ink by animateColorAsState(if (k == i) Kx.onAccent else if (k < i) Kx.text else Kx.text4, tween(150), label = "wi")
                                Text(w, style = ts(16.sp, color = ink, lh = 27.2.sp), modifier = Modifier.padding(end = 4.dp).clip(RoundedCornerShape(5.dp)).background(bg)
                                    .kxClick(w) { i = k; playing = true; gen++ }.padding(horizontal = 2.dp, vertical = 1.dp))
                            }
                        }
                        Text("09:30 · 12,4K просмотров", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                    Text("Нажмите на слово, чтобы читать с него", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp).exIn(350, dy = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xF528302C), Color(0xFA121614)))).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(26.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp)) {
                        if (playing) {
                            val r = kfLoop(1400, label = "ring")
                            Box(Modifier.matchParentSize().graphicsLayer { val s = .9f + .8f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .6f * (1f - r) }.border(2.dp, Kx.accent, CircleShape))
                        }
                        Box(Modifier.matchParentSize().clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick(if (playing) "Пауза" else "Читать") {
                            if (!playing && i >= RaW.size - 1) i = 0
                            playing = !playing; if (playing) gen++
                        }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(30.dp), tint = Kx.onAccent) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (playing) "Читаю вслух…" else if (i >= RaW.size - 1) "Дочитано" else "Пауза", style = ts(14.sp, FontWeight.ExtraBold))
                        val prog by animateFloatAsState((i + 1) / RaW.size.toFloat(), tween(200, easing = LinearEasing), label = "p")
                        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.12f))) {
                            Box(Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                        }
                        Row(Modifier.padding(top = 4.dp)) {
                            Text(mmss(secs(i)), style = ts(11.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f))
                            Text(mmss(secs(RaW.size)), style = ts(11.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum"))
                        }
                    }
                    Row(Modifier.height(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        repeat(4) { e ->
                            val w = if (playing) kfLoop(700, e * 150, "eq$e") else 0f
                            Box(Modifier.size(3.dp, 22.dp).graphicsLayer { transformOrigin = TransformOrigin(.5f, 1f); scaleY = .25f + .75f * tri(w) }.clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RaSp.forEach { (label, v) ->
                        val on = speed == v
                        Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f)).kxClick(label) { speed = v; gen++ },
                            contentAlignment = Alignment.Center) { Text(label, style = ts(12.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.text2)) }
                    }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Мягкий голос" to 0, "Бодрый голос" to 1).forEach { (label, v) ->
                        val on = voice == v; val ink = if (on) Kx.accent else Kx.text2
                        Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(14.dp)).kxClick(label) { voice = v; gen++ },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.RecordVoiceOver, null, Modifier.size(18.dp), tint = ink); Text(label, style = ts(13.sp, FontWeight.Bold, ink))
                        }
                    }
                }
                Text("Голос синтезируется на телефоне · работает без интернета", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}
