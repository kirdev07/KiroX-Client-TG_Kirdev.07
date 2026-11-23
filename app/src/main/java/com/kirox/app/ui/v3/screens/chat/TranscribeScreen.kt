package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class TVoice(val mine: Boolean, val dur: String, val t: String, val ru: String, val en: String)

// demo: needs store (voice messages of a chat + transcribeAudio(chatId, msgId) with streaming text, rate transcription, auto-transcribe setting)
private val TVoices = listOf(
    TVoice(false, "0:24", "12:05", "Привет! Я на рынке, тут хорошая клубника. Взять тебе пару килограммов или уже есть? Ещё напомни, во сколько завтра приезжаешь.",
        "Hi! I am at the market, the strawberries are good. Should I get you a couple of kilos or do you have some? Also remind me what time you arrive tomorrow."),
    TVoice(true, "0:09", "12:07", "Возьми, спасибо! Приезжаю к шести, поезд в 17:40.", "Get some, thanks! I arrive around six, the train is at 17:40."),
    TVoice(false, "0:13", "12:08", "Хорошо, встречу на вокзале. Оденься потеплее, обещают дождь.", "Okay, I will meet you at the station. Dress warmer, rain is expected."),
)
private val TBars = listOf(8, 14, 20, 12, 24, 18, 10, 22, 26, 16, 12, 20, 24, 14, 8, 18, 22, 12, 16, 26, 20, 10, 14, 22, 18, 12)

/** Board «Расшифровка голосовых»: voice bubbles with play, →A transcribe (shimmer, streaming words), copy/translate/rate, auto switch. */
@Composable
fun TranscribeScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → voice/round messages of chat ctx.arg, Telegram speech recognition (streamed text)
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoTranscribeScreen(ctx) else RealTranscribeScreen(ctx)
}

@Composable
private fun DemoTranscribeScreen(ctx: KxScreenCtx) {
    val st = remember { mutableStateMapOf<Int, String>() }
    val words = remember { mutableStateMapOf<Int, Int>() }
    val en = remember { mutableStateMapOf<Int, Boolean>() }
    val rate = remember { mutableStateMapOf<Int, Int>() }
    var playing by remember { mutableIntStateOf(-1) }
    var pk by remember { mutableIntStateOf(0) }
    var auto by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    LaunchedEffect(tk) { if (toast != null) { delay(2000); toast = null } }
    LaunchedEffect(pk) { if (playing >= 0) { delay(3000); playing = -1 } }
    fun say(t: String) { toast = t; tk++ }
    fun start(i: Int) {
        st[i] = "load"; words[i] = 0
        scope.launch {
            delay(1100); st[i] = "show"
            val n = TVoices[i].ru.split(' ').size
            while ((words[i] ?: 0) < n) { delay(90); words[i] = minOf(n, (words[i] ?: 0) + 2) }
        }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                LetterAv("М", hex("#B07A2E"), 40.dp, 16)
                Column(Modifier.weight(1f)) { Text("Мама", style = ts(16, W800)); Text("в сети", style = ts(12, color = Kx.accent)) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Сегодня", style = ts(11, W700, Kx.text3), modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp)).background(Kx.w(.06f)).padding(horizontal = 10.dp, vertical = 3.dp))
                TVoices.forEachIndexed { i, v ->
                    val s = st[i]; val open = s == "load" || s == "show"
                    val src = if (en[i] == true) v.en else v.ru; val all = src.split(' ')
                    val n = if (en[i] == true) all.size else (words[i] ?: 0)
                    val full = s == "show" && n >= all.size
                    val me = v.mine; val pl = playing == i; val r = rate[i]
                    val ink = if (me) Kx.onAccent else Kx.text
                    val dark = Color(0xFF06150D)
                    Column(Modifier.align(if (me) Alignment.End else Alignment.Start).width(284.dp)
                        .clip(if (me) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                        .then(if (me) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f))).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (me) dark.copy(alpha = .18f) else Kx.accent.copy(alpha = .16f))
                                .kxClick(if (pl) "Пауза" else "Слушать") { if (pl) playing = -1 else { playing = i; pk++ } }, contentAlignment = Alignment.Center) {
                                Icon(if (pl) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = if (me) dark else Kx.accent)
                            }
                            Wave(Modifier.weight(1f), if (me) dark.copy(alpha = .45f) else Kx.accent.copy(alpha = .55f), pl)
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (open) (if (me) dark.copy(alpha = .25f) else Kx.accent.copy(alpha = .22f)) else (if (me) dark.copy(alpha = .12f) else Kx.w(.08f)))
                                .kxClick(if (open) "Скрыть текст" else "Расшифровать") {
                                    if (open) st[i] = "closed" else if ((words[i] ?: 0) >= v.ru.split(' ').size) st[i] = "show" else start(i)
                                }, contentAlignment = Alignment.Center) {
                                Text(if (open) "▴" else "→A", style = ts(13, androidx.compose.ui.text.font.FontWeight.Black, if (me) dark else Kx.accent))
                            }
                        }
                        Row(Modifier.padding(top = 4.dp).graphicsLayer { alpha = .75f }) {
                            Text(v.dur, style = ts(11, color = ink), modifier = Modifier.weight(1f)); Text(v.t, style = ts(11, color = ink))
                        }
                        if (s == "load") Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShimmerLine(.92f, 0); ShimmerLine(.70f, 150)
                        }
                        if (s == "show") {
                            val line = if (me) dark.copy(alpha = .15f) else Kx.w(.08f)
                            Text(buildAnnotatedString { append(all.take(n).joinToString(" ")); if (!full) withStyle(SpanStyle(color = ink.copy(alpha = .5f))) { append(" ▍") } },
                                style = ts(14, color = ink, lh = 20.3.sp), modifier = Modifier.padding(top = 8.dp).chIn(dy = 6.dp, durationMs = 250)
                                    .drawBehind { drawLine(line, Offset(0f, -8.dp.toPx()), Offset(size.width, -8.dp.toPx()), 1.dp.toPx()) }.padding(top = 8.dp))
                            if (full) Row(Modifier.padding(top = 8.dp).chIn(dy = 6.dp, durationMs = 250), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val chip = if (me) dark.copy(alpha = .12f) else Kx.w(.07f)
                                TChip(Icons.Outlined.ContentCopy, "Копировать", chip, ink, "Скопировать текст") { clip.setText(AnnotatedString(src)); say("Текст скопирован") }
                                TChip(Icons.Outlined.Translate, if (en[i] == true) "Оригинал" else "На английский", chip, ink) { en[i] = en[i] != true }
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.size(30.dp).clip(CircleShape).background(if (r == 1) (if (me) dark.copy(alpha = .25f) else Kx.accent.copy(alpha = .22f)) else Color.Transparent)
                                    .kxClick("Хорошая расшифровка") { rate[i] = 1; say("Спасибо за оценку") }, contentAlignment = Alignment.Center) {
                                    Icon(if (r == 1) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp, null, Modifier.size(16.dp), tint = ink)
                                }
                                Box(Modifier.size(30.dp).clip(CircleShape).background(if (r == -1) (if (me) dark.copy(alpha = .25f) else Kx.danger.copy(alpha = .2f)) else Color.Transparent)
                                    .kxClick("Плохая расшифровка") { rate[i] = -1; say("Учтём, чтобы расшифровка была точнее") }, contentAlignment = Alignment.Center) {
                                    Icon(if (r == -1) Icons.Rounded.ThumbDown else Icons.Outlined.ThumbDown, null, Modifier.size(16.dp), tint = ink)
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).kxClick("Расшифровывать сразу", role = androidx.compose.ui.semantics.Role.Switch) {
                    auto = !auto; if (auto) TVoices.indices.forEach { if (st[it] != "show") start(it) }
                }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Subtitles, null, tint = Kx.text3)
                    Column(Modifier.weight(1f)) {
                        Text("Расшифровывать сразу", style = ts(14, W700))
                        Text("Текст появится под каждым новым голосовым", style = ts(12, color = Kx.text3))
                    }
                    BSwitch(auto)
                }
                Text("Осталось 2 расшифровки на этой неделе", style = ts(12, color = Kx.text3), modifier = Modifier.padding(top = 4.dp).padding(horizontal = 4.dp))
            }
        }
        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle, bottom = 96.dp)
    }
}

@Composable
private fun TChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, ink: Color, label: String? = null, onClick: () -> Unit) {
    Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).kxClick(label, onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = ink); Text(text, style = ts(12, W700, ink))
    }
}

/** 26 bars, kxBar (scaleY .5 ↔ 1, .6s, endless) while playing. */
@Composable
private fun Wave(modifier: Modifier, color: Color, playing: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val t = rememberInfiniteTransition(label = "wave")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing)), label = "p")
    Row(modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TBars.forEachIndexed { i, h ->
            val ph = p - (i % 5) * .08f / .6f
            val sy = if (playing && !reduced) .5f + .5f * ((1 - kotlin.math.cos(2 * Math.PI * ph)) / 2).toFloat() else 1f
            Box(Modifier.weight(1f).height(h.dp).graphicsLayer { scaleY = sy }.clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

/** kxShimmer line: 10dp, 200px tile sweeping, 1.1s linear. */
@Composable
private fun ShimmerLine(frac: Float, delayMs: Int) {
    val t = rememberInfiniteTransition(label = "shim")
    val x by t.animateFloat(-200f, 200f, infiniteRepeatable(tween(1100, easing = LinearEasing), initialStartOffset = StartOffset(delayMs)), label = "x")
    Box(Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(5.dp)).drawBehind {
        val px = x.dp.toPx(); val w = 200.dp.toPx()
        drawRect(Brush.horizontalGradient(0f to Kx.w(.06f), .5f to Kx.w(.22f), 1f to Kx.w(.06f), startX = px, endX = px + w, tileMode = TileMode.Repeated))
    })
}


/** wave3-media: Расшифровка on the real account. Play / «прослушано» only on a real tap; copy the recognized text. */
@Composable
private fun RealTranscribeScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var items by remember { mutableStateOf<List<com.kirox.app.ui.v3.model.KxVoiceItem>?>(null) }
    LaunchedEffect(chatId) { items = if (chatId == null) emptyList() else store.voiceMessages(chatId).sortedBy { it.messageId } }
    val st = remember { mutableStateMapOf<Long, String>() }
    val text = remember { mutableStateMapOf<Long, String>() }
    val heard = remember { mutableStateListOf<Long>() }
    var cur by remember { mutableLongStateOf(0L) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { delay(2500); toast = null } }
    fun say(t: String) { toast = t; tk++ }
    val p = store.player
    fun start(v: com.kirox.app.ui.v3.model.KxVoiceItem) {
        v.transcript?.let { text[v.messageId] = it; st[v.messageId] = "show"; return }
        st[v.messageId] = "load"
        scope.launch {
            val r = store.transcribe(v.chatId, v.messageId) { part -> text[v.messageId] = part; st[v.messageId] = "show" }
            if (r.text != null) { text[v.messageId] = r.text; st[v.messageId] = "show" } else { st[v.messageId] = "closed"; say(r.error ?: "Не получилось расшифровать") }
        }
    }
    val title = chatId?.let { store.header(it).title }.orEmpty()
    val list = items.orEmpty()

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Column(Modifier.weight(1f)) { Text(title.ifBlank { "Расшифровка" }, style = ts(16, W800), maxLines = 1); Text("${list.size} голосовых", style = ts(12, color = Kx.accent)) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            if (items == null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Kx.accent) }
            else if (list.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(if (chatId == null) "Откройте расшифровку из чата" else "В этом чате нет голосовых", style = ts(15, W700, Kx.text3))
            }
            else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState(Int.MAX_VALUE)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEach { v ->
                    val s = st[v.messageId]; val open = s == "load" || s == "show"
                    val me = v.out; val pl = cur == v.messageId && p.playing && p.track == null
                    val ink = if (me) Kx.onAccent else Kx.text
                    val dark = Color(0xFF06150D)
                    Column(Modifier.align(if (me) Alignment.End else Alignment.Start).width(284.dp)
                        .clip(if (me) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                        .then(if (me) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f))).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (me) dark.copy(alpha = .18f) else Kx.accent.copy(alpha = .16f))
                                .kxClick(if (pl) "Пауза" else "Слушать") {
                                    if (cur != v.messageId) {
                                        cur = v.messageId
                                        if (!v.out && !v.listened && v.messageId !in heard) { heard += v.messageId; store.markListened(v.chatId, v.messageId) }
                                    }
                                    store.playVoice(v.fileId)
                                }, contentAlignment = Alignment.Center) {
                                Icon(if (pl) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = if (me) dark else Kx.accent)
                            }
                            Wave(Modifier.weight(1f), if (me) dark.copy(alpha = .45f) else Kx.accent.copy(alpha = .55f), pl)
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (open) (if (me) dark.copy(alpha = .25f) else Kx.accent.copy(alpha = .22f)) else (if (me) dark.copy(alpha = .12f) else Kx.w(.08f)))
                                .kxClick(if (open) "Скрыть текст" else "Расшифровать") { if (open) st[v.messageId] = "closed" else if (text[v.messageId] != null) st[v.messageId] = "show" else start(v) },
                                contentAlignment = Alignment.Center) {
                                Text(if (open) "▴" else "→A", style = ts(13, androidx.compose.ui.text.font.FontWeight.Black, if (me) dark else Kx.accent))
                            }
                        }
                        Row(Modifier.padding(top = 4.dp).graphicsLayer { alpha = .75f }) {
                            Text("${v.seconds / 60}:${(v.seconds % 60).toString().padStart(2, '0')}" + if (v.video) " · кружок" else "", style = ts(11, color = ink), modifier = Modifier.weight(1f))
                            Text(v.time, style = ts(11, color = ink))
                        }
                        if (s == "load") Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { ShimmerLine(.92f, 0); ShimmerLine(.70f, 150) }
                        if (s == "show") {
                            val t = text[v.messageId].orEmpty()
                            Text(t.ifBlank { "Речь не распознана" }, style = ts(14, color = ink, lh = 20.3.sp), modifier = Modifier.padding(top = 8.dp))
                            if (t.isNotBlank()) Row(Modifier.padding(top = 8.dp)) {
                                TChip(Icons.Outlined.ContentCopy, "Копировать", if (me) dark.copy(alpha = .12f) else Kx.w(.07f), ink, "Скопировать текст") { clip.setText(AnnotatedString(t)); say("Текст скопирован") }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Text(if (store.me.premium) "Расшифровка без ограничений" else "Без Premium — несколько расшифровок в неделю", style = ts(12, color = Kx.text3),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp))
        }
        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle, bottom = 40.dp)
    }
}
