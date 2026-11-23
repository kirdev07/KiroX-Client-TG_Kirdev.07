package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import androidx.compose.material.icons.automirrored.outlined.Chat
import kotlinx.coroutines.launch

private class VoiceMsg(val mine: Boolean, val name: String, val dur: Int, val sent: String, val text: String)

// demo: needs store (voice messages: waveform, listened flag, transcription via TDLib recognizeSpeech)
private val VoiceMsgs = listOf(
    VoiceMsg(false, "Анна", 14, "15:02", "Привет! Слушай, я тут подумала, может в субботу съездим за город? Погоду обещают хорошую."),
    VoiceMsg(true, "Вы", 6, "15:04", "Давай! Я как раз свободен, возьму термос."),
    VoiceMsg(false, "Анна", 21, "15:06", "Отлично. Тогда выезжаем в девять утра, я заеду за тобой. Купи, пожалуйста, хлеб и сыр по дороге, остальное у меня."),
)

/** The board's pseudo-random waveform (same generator as the mockup), heights in dp. */
private fun waveform(i: Int): List<Int> {
    var x = ((i + 3) * 7919).toLong()
    return List(34) { k ->
        x = (x * 9301 + 49297) % 233280
        val h = 6 + Math.round((x / 233280.0) * 22 * (0.5 + kotlin.math.sin(k / 3.0) * 0.5 + 0.3)).toInt()
        minOf(28, h)
    }
}

private fun fmtT(t: Float) = "${(t / 60).toInt()}:${(t % 60).toInt().toString().padStart(2, '0')}"

/** Board «Голосовые»: voice bubbles with waveform seek, auto-play next unheard, mini player, speed, transcription. */
@Composable
fun VoiceScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → voice messages of chat ctx.arg (listened flag, Telegram speech recognition)
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoVoiceScreen(ctx) else RealVoiceScreen(ctx)
}

@Composable
private fun DemoVoiceScreen(ctx: KxScreenCtx) {
    val V = VoiceMsgs
    var cur by remember { mutableIntStateOf(-1) }
    val pos = remember { mutableStateListOf(0f, 0f, 0f) }
    var playing by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    val heard = remember { mutableStateListOf(false, true, false) }
    val tr = remember { mutableStateListOf(0, 0, 0) }
    LaunchedEffect(playing, cur) {
        while (playing && cur >= 0) {
            delay(250)
            val c = cur
            pos[c] = pos[c] + .25f * speed
            if (pos[c] >= V[c].dur) {
                pos[c] = 0f
                val nxt = V.indices.firstOrNull { it > c && !V[it].mine && !heard[it] }
                if (nxt != null) { heard[nxt] = true; cur = nxt } else { playing = false; cur = -1 }
            }
        }
    }
    val waves = remember { V.indices.map { waveform(it) } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 42.dp) { ctx.back() }
                MLetter("А", 38.dp, 15.sp, 0)
                Column(Modifier.weight(1f)) {
                    T("Анна", 16.sp, FontWeight.ExtraBold)
                    T(if (playing) "вы слушаете голосовое" else "в сети", 12.sp, color = Kx.accent)
                }
            }
            AnimatedVisibility(cur >= 0, enter = slideInVertically(tween(300, easing = Ease)) { -it } + fadeIn(tween(300))) {
                val c = cur.coerceAtLeast(0)
                val pct by animateFloatAsState(pos[c] / V[c].dur, tween(250, easing = LinearEasing), label = "mp")
                Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 4.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(18.dp)).background(Kx.surface)
                    .background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.14f), RoundedCornerShape(18.dp))) {
                    Row(Modifier.fillMaxSize().padding(start = 4.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).kxClick(if (playing) "Пауза" else "Продолжить") { playing = !playing }, contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Kx.accent)
                        }
                        Column(Modifier.weight(1f)) {
                            T(V[c].name, 13.sp, FontWeight.ExtraBold)
                            T("Голосовое · ${fmtT(pos[c])} / ${fmtT(V[c].dur.toFloat())}", 11.sp, color = Kx.text3)
                        }
                        Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.1f)).kxClick { speed = if (speed == 1f) 1.5f else if (speed == 1.5f) 2f else 1f }.padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center) { T((if (speed == 1.5f) "1.5" else speed.toInt().toString()) + "×", 12.sp, FontWeight.ExtraBold) }
                        Box(Modifier.size(34.dp).clip(CircleShape).kxClick("Закрыть плеер") { playing = false; cur = -1 }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = Kx.text3)
                        }
                    }
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(pct.coerceIn(0f, 1f)).height(2.dp).background(Kx.accent))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .14f), 1f, 0f, 1.2f, .6f, .6f)
                .verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                V.forEachIndexed { i, v ->
                    VoiceBubble(v, waves[i], pos[i], cur == i, cur == i && playing, !v.mine && !heard[i], tr[i], Modifier.align(if (v.mine) Alignment.End else Alignment.Start),
                        onPlay = { if (cur == i) playing = !playing else { heard[i] = true; cur = i; playing = true } },
                        onSeek = { f -> pos[i] = v.dur * f; heard[i] = true; cur = i; playing = true },
                        onTr = { if (tr[i] == 2) tr[i] = 0 else tr[i] = 1 })
                }
            }
            Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart) { T("Сообщение", 15.sp, color = Kx.text4) }
                Box(Modifier.size(46.dp).clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep).kxClick("Записать голосовое") { }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, null, tint = Kx.onAccent)
                }
            }
        }
    }
    tr.forEachIndexed { i, t -> if (t == 1) LaunchedEffect(i, t) { delay(1100); if (tr[i] == 1) tr[i] = 2 } }
}

@Composable
private fun VoiceBubble(v: VoiceMsg, wave: List<Int>, pos: Float, isCur: Boolean, playing: Boolean, unheard: Boolean, tr: Int, modifier: Modifier,
                        onPlay: () -> Unit, onSeek: (Float) -> Unit, onTr: () -> Unit) {
    val shape = if (v.mine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    val frac by animateFloatAsState(pos / v.dur, tween(150), label = "f")
    Column(modifier.width(290.dp).clip(shape).then(if (v.mine) Modifier.cssGrad(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).then(if (v.mine) Modifier.background(Color(0xFFE6F6EC)) else Modifier.cssGrad(140f, Kx.accentMid, Kx.accentDeep))
                .kxClick(if (playing) "Пауза" else "Слушать", onClick = onPlay), contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Kx.onAccent)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().height(30.dp).pointerInput(Unit) { detectTapGestures { o -> onSeek((o.x / size.width * 10).toInt().coerceIn(0, 9) / 10f) } },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    wave.forEachIndexed { k, h ->
                        val done = k / 34f < frac
                        Box(Modifier.weight(1f).height(h.dp).clip(RoundedCornerShape(2.dp)).background(
                            if (done) (if (v.mine) Color(0xFFE6F6EC) else Kx.accent) else (if (v.mine) Color(0xFFE6F6EC).copy(alpha = .35f) else Kx.w(.22f))))
                    }
                }
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val meta = if (v.mine) Color(0xFFA9D9BF) else Kx.text4
                    T(if (isCur || pos > 0) "${fmtT(pos)} / ${fmtT(v.dur.toFloat())}" else fmtT(v.dur.toFloat()), 11.sp, color = meta)
                    if (unheard) Box(Modifier.size(6.dp).clip(CircleShape).background(Kx.accent))
                    Spacer(Modifier.weight(1f))
                    T(v.sent, 11.sp, color = meta)
                }
            }
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (tr == 2) Kx.accent.copy(alpha = .2f) else if (v.mine) Color.Black.copy(alpha = .18f) else Kx.w(.07f))
                .kxClick("Расшифровать", onClick = onTr), contentAlignment = Alignment.Center) {
                T("→A", 13.sp, FontWeight.ExtraBold, if (tr == 2) Kx.accent else if (v.mine) Color(0xFFE6F6EC) else Kx.text2)
            }
        }
        if (tr == 1) Column(Modifier.padding(top = 8.dp)) {
            val clock by rememberLoopClock()
            listOf(.9f, .6f).forEachIndexed { k, w ->
                Box(Modifier.padding(top = if (k == 1) 6.dp else 0.dp).fillMaxWidth(w).height(10.dp).clip(RoundedCornerShape(5.dp)).drawBehind {
                    val shift = (-200f + 400f * ((clock % 1000) / 1000f)).dp.toPx()
                    drawRect(Brush.horizontalGradient(0f to Kx.w(.06f), .15f to Kx.w(.18f), .3f to Kx.w(.06f), 1f to Kx.w(.06f), startX = shift, endX = shift + 400.dp.toPx(), tileMode = TileMode.Repeated))
                })
            }
        }
        if (tr == 2) {
            val a = remember { Animatable(0f) }
            LaunchedEffect(Unit) { a.animateTo(1f, tween(400, easing = Ease)) }
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(Kx.w(.08f)))
            Text(v.text, style = ts(14.sp, color = if (v.mine) Color(0xFFE6F6EC) else Kx.text2).copy(lineHeight = 19.6.sp),
                modifier = Modifier.padding(top = 8.dp).graphicsLayer { alpha = a.value }.blur((3f * (1f - a.value)).dp))
        }
    }
}


/** 0..1 bars → the bubble's 34 bar heights (6..28 dp). */
private fun bars(w: List<Float>): List<Int> = if (w.isEmpty()) List(34) { 8 } else List(34) { k -> 6 + Math.round(22 * w[(k * w.size / 34).coerceAtMost(w.lastIndex)]) }

/** wave3-media: Голосовые on the real account. Plays with the app's VoicePlayer; «прослушано» is sent only when playback starts. */
@Composable
private fun RealVoiceScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<com.kirox.app.ui.v3.model.KxVoiceItem>?>(null) }
    LaunchedEffect(chatId) { items = if (chatId == null) emptyList() else store.voiceMessages(chatId).sortedBy { it.messageId } }
    var cur by remember { mutableLongStateOf(0L) }
    val heard = remember { mutableStateListOf<Long>() }
    val texts = remember { mutableStateMapOf<Long, String>() }
    val tr = remember { mutableStateMapOf<Long, Int>() }
    val toast = remember { MPillToastState() }
    val p = store.player
    val playing = cur != 0L && p.playing && p.track == null
    val list = items.orEmpty()
    fun play(v: com.kirox.app.ui.v3.model.KxVoiceItem) {
        if (cur != v.messageId) {
            cur = v.messageId
            if (!v.out && !v.listened && v.messageId !in heard) { heard += v.messageId; store.markListened(v.chatId, v.messageId) }
        }
        store.playVoice(v.fileId)
    }
    // critique-C P3: a one-time hint for the «→A» (transcribe) button.
    LaunchedEffect(Unit) {
        if ((store.prefs["hint.voice.transcribe"] ?: 0) == 0) { delay(800); toast.show("«→A» у голосового — расшифровать в текст", Icons.Rounded.Translate); store.putPref("hint.voice.transcribe", 1) }
    }
    fun transcribe(v: com.kirox.app.ui.v3.model.KxVoiceItem) {
        if ((tr[v.messageId] ?: 0) == 2) { tr[v.messageId] = 0; return }
        if (v.transcript != null || texts[v.messageId] != null) { texts.putIfAbsent(v.messageId, v.transcript.orEmpty()); tr[v.messageId] = 2; return }
        tr[v.messageId] = 1
        scope.launch {
            val r = store.transcribe(v.chatId, v.messageId) { part -> texts[v.messageId] = part; tr[v.messageId] = 2 }
            if (r.text != null) { texts[v.messageId] = r.text; tr[v.messageId] = 2 } else { tr[v.messageId] = 0; toast.show(r.error ?: "Не получилось расшифровать", Icons.Rounded.Translate) }
        }
    }
    val title = chatId?.let { store.header(it).title }.orEmpty()

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 42.dp) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(title.ifBlank { "Голосовые" }, 16.sp, FontWeight.ExtraBold, maxLines = 1)
                    T(if (playing) "вы слушаете голосовое" else "${list.size} голосовых", 12.sp, color = Kx.accent)
                }
                if (chatId != null) MRoundBtn(Icons.AutoMirrored.Outlined.Chat, "Открыть чат", size = 42.dp) { ctx.openChat(chatId) }
            }
            when {
                items == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Kx.accent) }
                list.isEmpty() -> Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                    Icon(Icons.Rounded.Mic, null, Modifier.size(48.dp), tint = Kx.text4)
                    T(if (chatId == null) "Откройте голосовые из чата" else "В этом чате нет голосовых", 15.sp, FontWeight.Bold)
                }
                else -> Column(Modifier.weight(1f).fillMaxWidth().ellipseGlow(Kx.accentDeep.copy(alpha = .14f), 1f, 0f, 1.2f, .6f, .6f)
                    .verticalScroll(rememberScrollState(Int.MAX_VALUE)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    list.forEach { v ->
                        val isCur = cur == v.messageId && p.track == null
                        val pos = if (isCur) p.positionMs / 1000f else 0f
                        val msg = VoiceMsg(v.out, v.sender, maxOf(1, v.seconds), v.time, texts[v.messageId] ?: v.transcript.orEmpty())
                        VoiceBubble(msg, bars(v.waveform), pos, isCur, isCur && p.playing, !v.out && !v.listened && v.messageId !in heard, tr[v.messageId] ?: 0,
                            Modifier.align(if (v.out) Alignment.End else Alignment.Start),
                            onPlay = { play(v) },
                            onSeek = { f -> if (isCur) store.playerSeek(f) else play(v) },
                            onTr = { transcribe(v) })
                    }
                }
            }
        }
        MPillToastHost(toast)
    }
}
