package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Timers = listOf("Выкл." to 0, "5 с" to 5, "15 с" to 15, "30 с" to 30, "1 мин" to 60, "1 ч" to 3600, "1 д" to 86400, "1 нед" to 604800)
private val KeyColors = listOf(Color(0xFF8FD3AC), Color(0xFF3F9A6C), Color(0xFF1F4D36), Color(0xFFDCD0FF), Color(0xFF4B3F7A), Color(0xFFE8B04A))

private data class SMsg(val id: Int, val mine: Boolean, val text: String, val time: String, val at: Long, val ttl: Int, val longTtl: String = "", val service: Boolean = false)

/** Board «Секретный чат»: intro card, self-destruct timer sheet, burning messages, encryption key sheet. */
@Composable
fun SecretScreen(ctx: KxScreenCtx) {
    // Opened as "Secret:<userId>" with the real store: the first message creates the secret chat (store.createSecretChat), messages go
    // through store.send, the timer is store.setAutoDelete. The burning bubbles here stay the local preview.
    // demo: needs store (key fingerprint and per-message TTL are not in the store yet)
    val store = ctx.store
    val peerId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val h = store.header(peerId)
    val scope = rememberCoroutineScope()
    val realChat = store !is com.kirox.app.ui.v3.demo.KxDemoStore && ctx.arg?.toLongOrNull() != null
    var secretId by remember { mutableStateOf<Long?>(null) }
    suspend fun ensureSecret(): Long? {
        secretId?.let { return it }
        val id = store.createSecretChat(peerId)
        if (id == null) ctx.toast(store.error ?: "Не удалось начать секретный чат").also { store.clearError() }
        secretId = id
        return id
    }
    var timer by remember { mutableIntStateOf(2) }
    var draft by remember { mutableStateOf("") }
    var timerSheet by remember { mutableStateOf(false) }
    var keySheet by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nextId by remember { mutableIntStateOf(2) }
    val msgs = remember { mutableStateListOf(SMsg(1, false, "Привет! Тут можно писать спокойно 🙂", "15:40", System.currentTimeMillis(), 15)) }
    LaunchedEffect(Unit) {
        while (true) { delay(250); now = System.currentTimeMillis(); msgs.removeAll { it.ttl > 0 && now - it.at >= (it.ttl + .6) * 1000 } }
    }
    var replyAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(replyAt) {
        // Simulated reply only in the demo app: never fake activity on a real account.
        if (replyAt == 0L || ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) return@LaunchedEffect
        delay(1600)
        val ttl = Timers[timer].second
        msgs += SMsg(nextId++, false, "Получила, вижу таймер 👀", hhmm(), System.currentTimeMillis(), if (ttl in 1..60) ttl else 0, if (ttl > 60) Timers[timer].first else "")
    }
    fun send() {
        val t = draft.trim(); if (t.isEmpty()) return
        val ttl = Timers[timer].second
        msgs += SMsg(nextId++, true, t, hhmm(), System.currentTimeMillis(), if (ttl in 1..60) ttl else 0, if (ttl > 60) Timers[timer].first else "")
        draft = ""; replyAt = System.currentTimeMillis()
        if (realChat) scope.launch { ensureSecret()?.let { id -> store.send(id, t) } }
    }
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size) }
    val tShort = if (timer > 0) Timers[timer].first else "без таймера"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp, onClick = ctx.back)
                Box(Modifier.size(40.dp)) {
                    KxAvatar(h.title, peerId, size = 40.dp, colorIndex = h.colorIndex)
                    Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(18.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(Kx.accent),
                        contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, Modifier.size(11.dp), tint = Kx.onAccent) }
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = Kx.accent); KxLine(h.title, ts(16, W800, Kx.accent))
                    }
                    Text("секретный чат · $tShort", style = ts(12, color = Kx.text3), maxLines = 1)
                }
                val tbg by animateColorAsState(if (timer > 0) Kx.accent.copy(alpha = .14f) else Kx.w(.06f), label = "tb")
                Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(tbg).kxClick("Таймер удаления") { timerSheet = true }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ink = if (timer > 0) Kx.accent else Kx.text3
                    Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = ink); Text(tShort, style = ts(13, W700, ink), maxLines = 1)
                }
                Box(Modifier.size(42.dp).clip(CircleShape).kxClick("Ключ шифрования") { keySheet = true }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Key, null, tint = Kx.text) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().ellipseGlow(.5f, 0f, .6f, .3f, Kx.accentDeep.copy(alpha = .12f)), state = list,
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { IntroCard() }
                items(msgs, key = { it.id }) { m ->
                    if (m.service) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(m.text, style = ts(12, color = Kx.text3), modifier = Modifier.chIn().clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 5.dp))
                    } else SecretBubble(m, now)
                }
            }
            Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(23.dp))
                    .padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = Kx.accent)
                    ChInput(draft, { draft = it }, "Секретное сообщение", Modifier.weight(1f), keyboard = KeyboardOptions(imeAction = ImeAction.Send))
                }
                Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить") { send() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent)
                }
            }
        }
        val sheetBg = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))
        ChSheet(timerSheet, { timerSheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = sheetBg) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Автоудаление", style = ts(19, W800), modifier = Modifier.padding(horizontal = 4.dp))
                Text("Сообщения исчезнут у обоих после прочтения через выбранное время", style = ts(13, color = Kx.text3, lh = 19.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 14.dp))
                Timers.chunked(4).forEachIndexed { r, row ->
                    Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { c, (label, _) ->
                            val i = r * 4 + c; val on = i == timer
                            Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.045f))
                                .border(1.dp, if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(16.dp)).kxClick(label) {
                                    timer = i; timerSheet = false
                                    if (realChat) scope.launch { ensureSecret()?.let { id -> store.setAutoDelete(id, Timers[i].second) } }
                                    msgs += SMsg(nextId++, false, if (i > 0) "Вы включили автоудаление: $label" else "Вы выключили автоудаление", "", 0, 0, service = true)
                                }, contentAlignment = Alignment.Center) { Text(label, style = ts(14, W700, if (on) Kx.accent else Kx.text)) }
                        }
                    }
                }
            }
        }
        ChSheet(keySheet, { keySheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = sheetBg) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ключ шифрования", style = ts(19, W800))
                Column(Modifier.padding(top = 16.dp).size(176.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(8) { r -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(8) { c -> val i = r * 8 + c; Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(KeyColors[(i * 37 + (i shr 3) * 11) % 6])) }
                    } }
                }
                Text("4F2A 91C7 0BDE 6631\nA85E 27F0 DC14 9B3A", style = ts(13, color = Kx.text2, lh = 20.sp).copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text("Если у собеседника такая же картинка, переписка защищена сквозным шифрованием", style = ts(13, color = Kx.text3, lh = 19.sp), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp))
            }
        }
    }
}

private fun hhmm() = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date())

@Composable
private fun IntroCard() {
    val reduced = LocalKxReducedMotion.current
    val bob = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { bob.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)); bob.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) } }
    Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.width(290.dp).chIn(dy = 12.dp, durationMs = 400).clip(RoundedCornerShape(24.dp)).background(Kx.w(.045f)).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(24.dp)).padding(16.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).graphicsLayer { translationY = -4.dp.toPx() * bob.value }.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .14f)),
                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, Modifier.size(28.dp), tint = Kx.accent) }
            Text("Вы создали секретный чат", style = ts(15, W800), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Pair<ImageVector, String>>(
                    Icons.Outlined.EnhancedEncryption to "Сквозное шифрование: прочитать можете только вы двое",
                    Icons.Outlined.CloudOff to "Сообщения не хранятся на серверах",
                    Icons.Outlined.Timer to "Можно включить автоудаление сообщений",
                    Icons.Outlined.Block to "Пересылать сообщения из этого чата нельзя",
                ).forEach { (icon, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, Modifier.size(18.dp), tint = Kx.accent); Text(text, style = ts(13, color = Kx.text2, lh = 18.sp)) }
                }
            }
        }
    }
}

@Composable
private fun SecretBubble(m: SMsg, now: Long) {
    val left = if (m.ttl > 0) maxOf(0f, m.ttl - (now - m.at) / 1000f) else 0f
    val burning = m.ttl > 0 && left <= 0f
    val burn by animateFloatAsState(if (burning) 1f else 0f, tween(600), label = "burn")
    val meta = if (m.mine) Color(0xFFA9D9BF) else Kx.text4
    val hole = if (m.mine) Color(0xFF24563E) else Color(0xFF161B18)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.fillMaxWidth(.78f).wrapContentWidth(if (m.mine) Alignment.End else Alignment.Start).chIn()
            .graphicsLayer { alpha = 1 - burn; val s = 1 - .15f * burn; scaleX = s; scaleY = s; translationY = -10.dp.toPx() * burn }
            .then(if (burn > 0f) Modifier.blur((8 * burn).dp) else Modifier)
            .clip(if (m.mine) RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
            .then(if (m.mine) Modifier.cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)) {
            Text(m.text, style = ts(15, color = if (m.mine) Color(0xFFE6F6EC) else Kx.text, lh = 20.25.sp))
            Row(Modifier.align(Alignment.End).padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (m.ttl > 0 || m.longTtl.isNotEmpty()) {
                    val frac = if (m.ttl > 0) left / m.ttl else 1f
                    Box(Modifier.size(16.dp).drawBehind {
                        drawCircle(Kx.w(.12f)); drawArc(meta, -90f, 360f * frac, true); drawCircle(hole, radius = size.minDimension / 2 - 2.dp.toPx())
                    })
                    Text(if (m.ttl > 0) "${kotlin.math.ceil(left).toInt()} с" else m.longTtl, style = ts(11, color = meta))
                }
                Text(m.time, style = ts(11, color = meta))
                if (m.mine) Icon(Icons.Outlined.DoneAll, null, Modifier.size(14.dp), tint = meta)
            }
        }
    }
}
