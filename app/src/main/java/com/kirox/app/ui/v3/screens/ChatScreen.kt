package com.kirox.app.ui.v3.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OutText = Color(0xFFE6F6EC)
private val OutMeta = Color(0xFFA6E3C1)
private val InBubble = Color(0xFF161B18)
private val OutShape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
private val InShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)

/** Outgoing bubble: CSS linear-gradient(160deg, #2E6B4B, #1F4D36). */
private fun Modifier.outBubble() = this.drawBehind {
    drawRect(Brush.linearGradient(listOf(Color(0xFF2E6B4B), Color(0xFF1F4D36)), cssAngleStart(160f, size), cssAngleEnd(160f, size)))
}
private fun Modifier.bubble(out: Boolean) = if (out) outBubble() else background(InBubble)

private data class PendingDelete(val message: KxMessage, val forAll: Boolean, val id: Long = System.nanoTime())

/** Conversation (board «Чат»): flat header + pinned bar, bubbles, glass composer, round mic/send button. */
@Composable
fun ChatScreen(store: KxStore, chatId: Long, onBack: () -> Unit, onCall: (Boolean) -> Unit = {}, onMenu: () -> Unit = {}, onProfile: () -> Unit = {}) {
    val header = store.header(chatId)
    val toast = LocalKxToast.current
    var text by rememberSaveable(chatId) { mutableStateOf("") }
    var replyTo by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    var menuFor by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    var editing by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    var pending by remember(chatId) { mutableStateOf<PendingDelete?>(null) }
    var attach by remember { mutableStateOf(false) }
    var circleMode by rememberSaveable { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Boolean?>(null) } // null = no, false = voice, true = circle
    var recSecs by remember { mutableIntStateOf(0) }
    val messages = store.messages(chatId).filter { it.id != pending?.message?.id }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hint) { if (hint != null) { delay(1800); hint = null } }
    LaunchedEffect(recording) { recSecs = 0; while (recording != null) { delay(1000); recSecs++ } }
    LaunchedEffect(pending?.id) { val p = pending ?: return@LaunchedEffect; delay(5000); store.delete(chatId, listOf(p.message.id), p.forAll); if (pending?.id == p.id) pending = null }
    val latestPending by rememberUpdatedState(pending)
    DisposableEffect(chatId) { onDispose { latestPending?.let { store.delete(chatId, listOf(it.message.id), it.forAll) } } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        // Two soft glows: radial-gradient(70% 40% at 100% 20%, .16) + (60% 35% at 0% 75%, .10).
        Canvas(Modifier.fillMaxSize()) {
            fun glow(cx: Float, cy: Float, rx: Float, ry: Float, a: Float) {
                scale(1f, ry / rx, Offset(cx, cy)) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF3F9A6C).copy(alpha = a), Color.Transparent), Offset(cx, cy), rx), rx, Offset(cx, cy))
                }
            }
            glow(size.width, size.height * .2f, size.width * .7f, size.height * .4f, .16f)
            glow(0f, size.height * .75f, size.width * .6f, size.height * .35f, .10f)
        }
        val top = if (header.pinned != null) 184.dp else 130.dp
        LazyColumn(
            Modifier.fillMaxSize().then(if (menuFor != null) Modifier.blur(10.dp) else Modifier), state = listState, reverseLayout = true,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = top, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
        ) {
            items(messages.asReversed(), key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth().animateItem(fadeInSpec = tween(250), fadeOutSpec = tween(350), placementSpec = tween(350, easing = KxMotion.rise))) {
                    m.dayHeader?.let { DayPill(it) }
                    MessageBubble(m, isGroup = header.kind == KxChatKind.Group, onLongPress = { menuFor = m }, onReact = { store.react(chatId, m.id, it) })
                }
            }
        }
        if (messages.isEmpty() && header.kind != KxChatKind.Saved && header.kind != KxChatKind.Channel)
            EmptyChatCard(if (header.kind == KxChatKind.Private) store.peer(chatId) else null, onSend = { store.send(chatId, it) },
                Modifier.align(Alignment.Center).padding(top = 40.dp))
        val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
        AnimatedVisibility(!atBottom, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 112.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Box(Modifier.size(44.dp).shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = .5f)).clip(CircleShape)
                .background(Color(0xCC161B18)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.16f), CircleShape)
                .kxClick("Вниз") { scope.launch { listState.animateScrollToItem(0) } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Kx.text)
            }
        }
        ChatHeader(header.copy(typing = if (recording != null) (if (recording == true) "записывает видео" else "записывает голосовое") else header.typing),
            onBack, onCall, onMenu, Modifier.align(Alignment.TopCenter), onProfile) { header.pinned?.let { store.pin(chatId, it.messageId, false) } }

        if (recording == true) CircleRecording(recSecs)
        when (header.composer) {
            KxComposer.Write -> Composer(
                text = text, onText = { text = it; store.setDraft(chatId, it) },
                replyTo = replyTo ?: editing, editing = editing != null,
                onCancelReply = { if (editing != null) text = ""; replyTo = null; editing = null },
                onSend = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        val e = editing
                        if (e != null) { store.edit(chatId, e.id, t); toast("Сообщение изменено") } else store.send(chatId, t, replyTo?.id)
                        text = ""; replyTo = null; editing = null
                    }
                },
                attachOpen = attach, onAttach = { attach = !attach },
                circleMode = circleMode, onToggleMode = { circleMode = !circleMode; hint = if (circleMode) "Удерживайте, чтобы записать кружок" else "Удерживайте, чтобы записать голосовое" },
                recording = recording, recSecs = recSecs,
                onRecStart = { recording = circleMode; store.recordStart(chatId, circleMode) },
                onRecEnd = { send ->
                    val r = recording; recording = null
                    if (r != null) store.recordStop(chatId, send, recSecs.coerceAtLeast(1))
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            else -> ChannelBar(header.composer, Modifier.align(Alignment.BottomCenter)) {
                if (header.composer == KxComposer.Mute || header.composer == KxComposer.Unmute) store.setMuted(chatId, header.composer == KxComposer.Mute)
            }
        }
        pending?.let { p -> UndoBar(p, Modifier.align(Alignment.BottomCenter)) { pending = null } }
        // Mic hint: small bubble above the button (right 12, bottom 90), not the generic toast.
        AnimatedVisibility(hint != null, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 90.dp),
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }, exit = fadeOut(tween(200))) {
            Text(hint ?: "", style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xF21E2823))
                .border(1.dp, Kx.w(.12f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
        }
        if (attach) AttachSheet(onClose = { attach = false }, onSend = { n -> attach = false; toast(if (n == 1) "Фото отправлено" else "Отправлено фото: $n") })
        menuFor?.let { m ->
            MessageMenuSheet(
                m, peerName = header.title, onDismiss = { menuFor = null },
                onReply = { replyTo = m; editing = null; menuFor = null },
                onEdit = { editing = m; replyTo = null; text = m.text; menuFor = null },
                onReact = { store.react(chatId, m.id, it); menuFor = null },
                onDelete = { forAll -> pending = PendingDelete(m, forAll); menuFor = null },
                onForward = { menuFor = null; toast("Выберите чат для пересылки") },
                onPin = { store.pin(chatId, m.id, !m.pinned); menuFor = null },
                onSave = { store.saveToFavorites(chatId, listOf(m.id)); menuFor = null; toast("Сохранено в Избранное") },
                onSelect = { menuFor = null; toast("Выбрано: 1") },
                onTranslate = { store.translate(chatId, m.id, m.translation == null); menuFor = null },
                onCopied = { menuFor = null; toast("Текст скопирован") },
                canDeleteForAll = m.canDeleteForAll && header.kind != KxChatKind.Saved,
            )
        }
    }
}

@Composable
private fun ChatHeader(h: KxChatHeader, onBack: () -> Unit, onCall: (Boolean) -> Unit, onMenu: () -> Unit, modifier: Modifier, onProfile: () -> Unit, onUnpin: () -> Unit) {
    Column(modifier.fillMaxWidth().background(Color(0xFF0D100E)).drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
        .statusBarsPadding().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", size = 48.dp, filled = false, tint = Kx.text, onClick = onBack)
            Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).kxClick("Профиль: ${h.title}", onClick = onProfile).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    KxAvatar(h.title, h.chatId, size = 42.dp, icon = if (h.kind == KxChatKind.Saved) Icons.Rounded.Bookmark else null, colorIndex = h.colorIndex)
                    if (h.online) Box(Modifier.align(Alignment.BottomEnd).offset(1.dp, 1.dp).size(16.dp).clip(CircleShape).background(Color(0xFF1B221E)).padding(2.dp).clip(CircleShape).background(Kx.accent))
                }
                Column(Modifier.weight(1f)) {
                    KxLine(h.title, KxType.style(16.sp, FontWeight.Bold))
                    val st = h.typing?.let { "$it…" } ?: h.status
                    if (st.isNotEmpty()) KxLine(st, KxType.style(12.sp, color = if (h.online || h.typing != null) Kx.accent else Kx.text4))
                }
                if (h.kind == KxChatKind.Private) Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .18f)).kxClick("Позвонить") { onCall(false) },
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Call, null, Modifier.size(21.dp), tint = Kx.accent) }
            }
            KxIconButton(Icons.Rounded.MoreVert, "Меню чата", size = 48.dp, filled = false, tint = Kx.text, onClick = onMenu)
        }
        h.pinned?.let { p ->
            Row(Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121614))
                .border(1.dp, Kx.w(.06f), RoundedCornerShape(16.dp)).kxClick("Закреплённое сообщение") {}.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(3.dp, 28.dp).clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                Column(Modifier.weight(1f)) {
                    Text("Закреплённое сообщение", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                    KxLine(p.text, KxType.style(13.sp, color = Kx.text2))
                }
                Box(Modifier.size(32.dp).kxClick("Открепить", onClick = onUnpin), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PushPin, null, Modifier.size(20.dp), tint = Kx.text4)
                }
            }
        }
    }
}

@Composable
private fun DayPill(text: String) {
    Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text3),
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Kx.fill).border(1.dp, Kx.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(m: KxMessage, isGroup: Boolean, onLongPress: () -> Unit, onReact: (String) -> Unit) {
    val align = if (m.out) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        val press = Modifier.combinedClickable(interactionSource = null, indication = null, onClick = {}, onLongClick = onLongPress)
        when (m.kind) {
            KxMsgKind.Service -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(m.text, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text3), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Kx.fill).padding(horizontal = 12.dp, vertical = 4.dp))
            }
            KxMsgKind.VideoNote -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = press.padding(vertical = 2.dp)) {
                if (m.out) MetaText(m, Kx.text4)
                VideoNoteCircle(m)
                if (!m.out) Text(m.time, style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.padding(bottom = 6.dp))
            }
            KxMsgKind.Voice -> if (m.out && m.played == 0f && m.status != KxOutState.Read) Row(
                press.width(220.dp).clip(OutShape).outBubble().padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(OutMeta).kxClick("Слушать") {}, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0B2216)) }
                Canvas(Modifier.weight(1f).height(22.dp)) {
                    val hs = listOf(.5f, .85f, .4f, .7f, .95f, .55f, .3f, .65f, .45f, .8f); val w = 3.dp.toPx(); val gap = 2.dp.toPx()
                    hs.forEachIndexed { i, v -> val h = size.height * v; val x = i * (w + gap) + w / 2
                        drawLine(OutMeta.copy(alpha = .6f), Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), w, StrokeCap.Round) }
                }
                Text("${m.meta} ✓", style = KxType.style(11.sp, color = OutMeta))
            } else Column(
                press.width(236.dp).clip(if (m.out) RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp) else InShape).bubble(m.out)
                    .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(OutMeta).kxClick("Слушать") {}, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0B2216))
                    }
                    Waveform(m.waveform.ifEmpty { listOf(.4f, .7f, .5f, .9f, .6f, .3f, .7f, .9f, .5f, .3f, .6f, .4f, .25f, .5f) }, m.played, Modifier.weight(1f).height(26.dp))
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .2f)).kxClick("Расшифровать голосовое") {}, contentAlignment = Alignment.Center) {
                        Text("→A", style = KxType.style(12.sp, FontWeight.ExtraBold, OutMeta))
                    }
                }
                Row { Text(m.meta, style = KxType.style(11.sp, color = OutMeta), modifier = Modifier.weight(1f)); MetaText(m, OutMeta) }
            }
            KxMsgKind.Photo, KxMsgKind.Video, KxMsgKind.Gif -> Column(press.width(230.dp).clip(if (m.out) OutShape else RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)).bubble(m.out).padding(4.dp)) {
                Box(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(18.dp))
                    .drawBehind { drawRect(Brush.linearGradient(0f to Color(0xFF355E49), .55f to Color(0xFF1A2C22), 1f to Color(0xFF0F1712), start = cssAngleStart(150f, size), end = cssAngleEnd(150f, size))) },
                    contentAlignment = Alignment.Center) {
                    Icon(if (m.kind == KxMsgKind.Photo) Icons.Rounded.Image else Icons.Rounded.PlayCircle, null, Modifier.size(36.dp), tint = Kx.accentLight.copy(alpha = .7f))
                }
                if (m.text.isNotEmpty()) TextWithMeta(m, Modifier.padding(start = 9.dp, end = 9.dp, top = 7.dp, bottom = 4.dp))
            }
            else -> Column(
                press.widthIn(max = 312.dp).width(IntrinsicSize.Max).clip(if (m.out) OutShape else InShape).bubble(m.out)
            ) {
                if (isGroup && !m.out && m.sender != null) {
                    val idx = if (m.senderColorIndex >= 0) m.senderColorIndex else ((m.senderId % 8 + 8) % 8).toInt()
                    Text(m.sender, style = KxType.style(13.sp, FontWeight.Bold, KxAvatarPalette.all[idx % 8].ink), modifier = Modifier.padding(start = 13.dp, top = 8.dp, end = 13.dp))
                }
                m.forwardedFrom?.let { Text("Переслано от $it", style = KxType.style(12.sp, FontWeight.SemiBold, if (m.out) OutMeta else Kx.accent), modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)) }
                m.reply?.let { r ->
                    Column(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp).clip(RoundedCornerShape(6.dp, 12.dp, 12.dp, 6.dp)).background(Color.Black.copy(alpha = .18f))
                        .drawBehind { drawRect(if (m.out) OutMeta else Kx.accent, size = Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)) {
                        Text(r.sender, style = KxType.style(13.sp, FontWeight.Bold, if (m.out) OutMeta else Kx.accent))
                        KxLine(r.text, KxType.style(13.sp, color = if (m.out) Color(0xFFBFDCCB) else Kx.text3))
                    }
                }
                if (m.kind == KxMsgKind.File || m.kind == KxMsgKind.Music) FileLine(m)
                val pad = when { m.reply != null -> PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 7.dp); m.out -> PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 7.dp); else -> PaddingValues(start = 13.dp, end = 13.dp, top = 9.dp, bottom = 7.dp) }
                TextWithMeta(m, Modifier.padding(pad))
            }
        }
        if (m.reactions.isNotEmpty()) Row(Modifier.padding(top = 1.dp, bottom = 2.dp, start = 6.dp, end = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            m.reactions.forEach { r ->
                Text("${r.emoji} ${r.count}", style = KxType.style(13.sp),
                    modifier = Modifier.clip(RoundedCornerShape(11.dp))
                        .then(if (r.mine) Modifier.background(Kx.accent.copy(alpha = .16f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(11.dp)) else Modifier.background(Kx.fill))
                        .kxClick("${r.emoji} ${r.count}") { onReact(r.emoji) }.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun TextWithMeta(m: KxMessage, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(m.translation ?: m.text, style = KxType.style(15.sp, color = if (m.out) OutText else Kx.text, lineHeight = 21.sp))
            if (m.translation != null) Text("Перевод · оригинал в меню", style = KxType.style(11.sp, color = if (m.out) OutMeta else Kx.text4))
        }
        MetaText(m, if (m.out) OutMeta else Kx.text4)
    }
}

@Composable
private fun MetaText(m: KxMessage, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 1.dp)) {
        Text((if (m.edited) "изм. " else "") + m.time, style = KxType.style(11.sp, color = color))
        if (m.out) when (m.status) {
            KxOutState.Read -> Icon(Icons.Rounded.DoneAll, "Прочитано", Modifier.size(14.dp), tint = color)
            KxOutState.Sent -> Icon(Icons.Rounded.Done, "Отправлено", Modifier.size(14.dp), tint = color)
            KxOutState.Sending -> Icon(Icons.Rounded.Schedule, "Отправляется", Modifier.size(13.dp), tint = color)
            KxOutState.Failed -> Icon(Icons.Rounded.ErrorOutline, "Не отправлено", Modifier.size(14.dp), tint = Kx.danger)
            KxOutState.None -> {}
        }
    }
}

@Composable
private fun FileLine(m: KxMessage) {
    Row(Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (m.out) OutMeta else Kx.accent), contentAlignment = Alignment.Center) {
            Icon(if (m.kind == KxMsgKind.Music) Icons.Rounded.MusicNote else Icons.Rounded.Description, null, tint = Kx.onAccent)
        }
        Column { KxLine(m.fileName, KxType.style(14.sp, FontWeight.SemiBold, if (m.out) OutText else Kx.text)); Text(m.meta, style = KxType.style(12.sp, color = if (m.out) OutMeta else Kx.text4)) }
    }
}

/** Voice bars drawn 1:1 from the data: 3dp wide, 2dp gap; played part bright, rest .45. */
@Composable
private fun Waveform(bars: List<Float>, played: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = 3.dp.toPx(); val gap = 2.dp.toPx()
        val playedBars = (bars.size * played).toInt()
        bars.forEachIndexed { i, v ->
            val h = size.height * v.coerceIn(.1f, 1f); val x = i * (w + gap) + w / 2
            if (x <= size.width) drawLine(if (i < playedBars) OutMeta else OutMeta.copy(alpha = .45f), Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun VideoNoteCircle(m: KxMessage) {
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = 3.dp.toPx()
            if (m.out) drawCircle(Kx.accent.copy(alpha = .5f), radius = size.minDimension / 2 - sw / 2, style = Stroke(sw))
            else {
                drawCircle(Color.White.copy(alpha = .12f), radius = size.minDimension / 2 - sw / 2, style = Stroke(sw))
                drawArc(Kx.accent, -90f, 360f * m.played, false, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw))
            }
        }
        val grad = if (m.out) listOf(Color(0xFF2F6E4D), Color(0xFF173A28), Color(0xFF0B1510)) else listOf(Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFF141019))
        Box(Modifier.padding(3.dp).fillMaxSize().clip(CircleShape)
            .drawBehind { drawRect(Brush.radialGradient(0f to grad[0], .45f to grad[1], 1f to grad[2], center = Offset(size.width / 2, size.height * .38f), radius = size.width * .75f)) },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, Modifier.size(64.dp).offset(y = 13.dp), tint = if (m.out) Color(0x4DC8F0D8) else Color(0x59DCD0FF))
            Text(if (m.out) "${m.meta} ✓" else "${m.meta} · 🔇", style = KxType.style(11.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

/** Glass composer pill (left 10, right 78, bottom 22, 58dp) + the 58dp mic/circle/send button with hold-to-record. */
@Composable
private fun Composer(
    text: String, onText: (String) -> Unit, replyTo: KxMessage?, editing: Boolean, onCancelReply: () -> Unit, onSend: () -> Unit,
    attachOpen: Boolean, onAttach: () -> Unit, circleMode: Boolean, onToggleMode: () -> Unit,
    recording: Boolean?, recSecs: Int, onRecStart: () -> Unit, onRecEnd: (Boolean) -> Unit, modifier: Modifier,
) {
    val hasText = text.isNotBlank()
    val rec = recording != null
    val scale by animateFloatAsState(if (rec) 1.35f else 1f, spring(dampingRatio = .45f, stiffness = 400f), label = "act")
    val plusTurn by animateFloatAsState(if (attachOpen) 45f else 0f, spring(dampingRatio = .5f, stiffness = 500f), label = "plus")
    Column(modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 10.dp, end = 10.dp, bottom = 22.dp)) {
        if (replyTo != null) Row(
            Modifier.padding(bottom = 8.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .10f))
                .border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(18.dp)).padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(if (editing) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.Reply, null, Modifier.size(20.dp), tint = Kx.accent)
            Column(Modifier.weight(1f)) {
                Text(if (editing) "Редактирование" else "Ответ", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                KxLine(replyTo.text.ifEmpty { "Медиа" }, KxType.style(13.sp, color = Kx.text2))
            }
            Box(Modifier.size(34.dp).clip(CircleShape).kxClick(if (editing) "Отменить изменение" else "Отменить ответ", onClick = onCancelReply), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text4)
            }
        }
        Box {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val shape = RoundedCornerShape(29.dp)
                Box(Modifier.weight(1f).heightIn(min = 58.dp).shadow(34.dp, shape, spotColor = Color.Black.copy(alpha = .55f)).clip(shape)
                    .background(Color(0xFF101311)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                    .border(1.dp, Kx.w(.16f), shape).topHighlight(Kx.w(.35f))) {
                    if (recording == false) VoiceRecBar(recSecs) { onRecEnd(false) }
                    else Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(CircleShape).kxClick(if (attachOpen) "Закрыть вложения" else "Вложения", onClick = onAttach), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AddCircleOutline, null, Modifier.rotate(plusTurn), tint = Kx.text2)
                        }
                        Box(Modifier.weight(1f).padding(vertical = 17.dp)) {
                            if (text.isEmpty()) Text("Сообщение", style = KxType.style(16.sp, color = Kx.text4))
                            BasicTextField(text, onText, textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent), maxLines = 6, modifier = Modifier.fillMaxWidth())
                        }
                        KxIconButton(Icons.Outlined.Mood, "Стикеры и эмодзи", size = 46.dp, filled = false, tint = Kx.text2) {}
                    }
                }
                ActionButton(hasText, circleMode, rec, scale, onSend, onToggleMode, onRecStart, onRecEnd)
            }
            if (recording == false) Column(Modifier.align(Alignment.TopEnd).offset(y = (-84).dp).padding(end = 8.dp).size(42.dp, 72.dp).clip(RoundedCornerShape(21.dp))
                .background(Color(0xF21E2823)).border(1.dp, Kx.w(.12f), RoundedCornerShape(21.dp)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = Kx.text3)
                Icon(Icons.Rounded.KeyboardArrowUp, null, Modifier.size(18.dp), tint = Kx.text4)
            }
        }
    }
}

@Composable
private fun ActionButton(hasText: Boolean, circleMode: Boolean, rec: Boolean, scale: Float, onSend: () -> Unit, onToggleMode: () -> Unit,
                         onRecStart: () -> Unit, onRecEnd: (Boolean) -> Unit) {
    val t = rememberInfiniteTransition(label = "rec")
    val r1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)), label = "r1")
    val r2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, delayMillis = 600, easing = FastOutSlowInEasing)), label = "r2")
    val latestHasText by rememberUpdatedState(hasText)
    val send by rememberUpdatedState(onSend); val toggle by rememberUpdatedState(onToggleMode)
    val start by rememberUpdatedState(onRecStart); val end by rememberUpdatedState(onRecEnd)
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        if (rec) listOf(r1 to .45f, r2 to .35f).forEach { (p, a) ->
            Box(Modifier.size(58.dp).graphicsLayer { val s = 1f + p * .9f; scaleX = s; scaleY = s; alpha = (1f - p) * a }.clip(CircleShape).background(Kx.accent))
        }
        Box(Modifier.size(58.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(26.dp, CircleShape, spotColor = Kx.accentDeep.copy(alpha = .4f)).clip(CircleShape).background(Kx.accent).topHighlight(Kx.w(.5f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (latestHasText) { if (waitForUpOrCancellation() != null) send(); return@awaitEachGesture }
                    // A 260ms hold starts recording (release sends); a short tap switches mic <-> circle.
                    var timedOut = false
                    val quick = withTimeoutOrNull(260) { waitForUpOrCancellation() }
                    if (quick == null) timedOut = true
                    if (!timedOut) { toggle(); return@awaitEachGesture }
                    start()
                    val up = waitForUpOrCancellation()
                    end(up != null)
                }
            },
            contentAlignment = Alignment.Center) {
            AnimatedContent(when { hasText -> 0; circleMode -> 2; else -> 1 }, transitionSpec = { (scaleIn(spring(dampingRatio = .5f)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "icon") { k ->
                Icon(when (k) { 0 -> Icons.AutoMirrored.Rounded.Send; 2 -> Icons.Rounded.RadioButtonChecked; else -> Icons.Rounded.Mic },
                    when (k) { 0 -> "Отправить"; 2 -> "Записать кружок"; else -> "Записать голосовое" }, tint = Kx.onAccent)
            }
        }
    }
}

@Composable
private fun VoiceRecBar(secs: Int, onCancel: () -> Unit) {
    val t = rememberInfiniteTransition(label = "vr")
    val blink by t.animateFloat(1f, .2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "b")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "w")
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(10.dp).graphicsLayer { alpha = blink }.clip(CircleShape).background(Color(0xFFFF5A5A)))
        Text("${secs / 60}:${(secs % 60).toString().padStart(2, '0')}", style = KxType.style(15.sp, FontWeight.SemiBold))
        Canvas(Modifier.width(40.dp).height(20.dp)) {
            repeat(8) { i ->
                val h = size.height * (.3f + .7f * kotlin.math.abs(kotlin.math.sin((wave + i * .15f) * Math.PI.toFloat())))
                val x = i * 5.dp.toPx() + 1.5.dp.toPx()
                drawLine(Kx.accent, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(16.dp)).kxClick("Отмена", onClick = onCancel).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ChevronLeft, null, Modifier.size(18.dp), tint = Kx.text3)
            Text("Отмена", style = KxType.style(14.sp, color = Kx.text3))
        }
    }
}

@Composable
private fun CircleRecording(secs: Int) {
    Column(Modifier.fillMaxSize().background(Color(0xD9050706)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val sw = 4.dp.toPx()
                drawCircle(Kx.w(.12f), radius = size.minDimension / 2 - sw / 2, style = Stroke(sw))
                drawArc(Kx.accent, -90f, 360f * (secs % 60) / 60f, false, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
            }
            Box(Modifier.padding(8.dp).fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF2F6E4D), Color(0xFF0B1510)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, Modifier.size(110.dp), tint = Color(0x4DC8F0D8))
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .35f)).kxClick("Сменить камеру") {}, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Cameraswitch, null, Modifier.size(22.dp), tint = Kx.text)
                }
            }
        }
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5A5A)))
            Text("${secs / 60}:${(secs % 60).toString().padStart(2, '0')}", style = KxType.style(15.sp, FontWeight.SemiBold))
        }
        Text("Отпустите кнопку — кружок отправится", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 6.dp))
    }
}

/** Undo bar after «Удалить»: 5→1 countdown; the message is really deleted when it ends. */
@Composable
private fun UndoBar(p: PendingDelete, modifier: Modifier, onUndo: () -> Unit) {
    var left by remember(p.id) { mutableIntStateOf(5) }
    LaunchedEffect(p.id) { while (left > 1) { delay(1000); left-- } }
    Row(modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 104.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp))
        .background(Color(0xF7283229)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(left.toString(), style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent).copy(fontFeatureSettings = "tnum"))
        Text(if (p.forAll) "Удалено у вас и у собеседника" else "Удалено у вас", style = KxType.style(14.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).kxClick("Отменить удаление", onClick = onUndo).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text("Отменить", style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent))
        }
    }
}

/** Channel subscribers see a mute/unmute bar instead of the composer. */
@Composable
private fun ChannelBar(mode: KxComposer, modifier: Modifier, onClick: () -> Unit) {
    val label = when (mode) { KxComposer.Mute -> "Выключить звук"; KxComposer.Unmute -> "Включить звук"; KxComposer.Join -> "Вступить"; KxComposer.Blocked -> "Разблокировать"; else -> "Вам запрещено писать в этот чат" }
    Box(modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 22.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(29.dp))
        .background(Color(0xFF101311)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(29.dp))
        .then(if (mode == KxComposer.Restricted) Modifier else Modifier.kxClick(onClick = onClick)), contentAlignment = Alignment.Center) {
        Text(label, style = KxType.style(15.sp, FontWeight.Bold, if (mode == KxComposer.Restricted) Kx.text4 else Kx.accent))
    }
}

/** «Недавние» attach sheet: camera + 5 photos with numbered marks, 8 kinds, caption + «Отправить N». */
@Composable
private fun AttachSheet(onClose: () -> Unit, onSend: (Int) -> Unit) {
    val picked = remember { mutableStateListOf<Int>() }
    val tiles = listOf(listOf(Color(0xFF3E7A5A), Color(0xFF173A28)), listOf(Color(0xFF4B3F7A), Color(0xFF1E1830)), listOf(Color(0xFFC08A4A), Color(0xFF4A3018)),
        listOf(Color(0xFF3C7BD0), Color(0xFF142A4A)), listOf(Color(0xFFA24B5B), Color(0xFF3A1820)))
    val kinds: List<Triple<ImageVector, String, Pair<Color, Color>>> = listOf(
        Triple(Icons.Rounded.PhotoLibrary, "Галерея", Color(0xFF2E5C45) to Color(0xFFC8F0D8)), Triple(Icons.Rounded.Description, "Файл", Color(0xFF2D4E73) to Color(0xFFC6DBF5)),
        Triple(Icons.Rounded.LocationOn, "Место", Color(0xFF6B4E2E) to Color(0xFFF2DDBF)), Triple(Icons.Rounded.Ballot, "Опрос", Color(0xFF4B3F7A) to Color(0xFFDCD0FF)),
        Triple(Icons.Rounded.Person, "Контакт", Color(0xFF2E6B66) to Color(0xFFBDEDE8)), Triple(Icons.Rounded.MusicNote, "Музыка", Color(0xFFA24B5B) to Color(0xFFFFD6DE)),
        Triple(Icons.Rounded.Casino, "Игры", Color(0xFF5A6B2E) to Color(0xFFE6F2BF)), Triple(Icons.Rounded.ScheduleSend, "Позже", Color(0xFF3A4540) to Color(0xFFC9D2CC)))
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть вложения", onClick = onClose))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xEB222C27), Color(0xF5101412))))
            .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            Row(Modifier.padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (picked.isEmpty()) "Недавние" else "Выбрано: ${picked.size}", style = KxType.style(17.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text) }
            }
            val cells = listOf(-1) + tiles.indices
            Column(Modifier.clip(RoundedCornerShape(18.dp)), verticalArrangement = Arrangement.spacedBy(4.dp)) { cells.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { i ->
                        if (i < 0) Column(Modifier.weight(1f).height(108.dp).background(Color(0xFF151A17)).kxClick("Снять фото") {},
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(30.dp), tint = Kx.accent); Text("Камера", style = KxType.style(12.sp, FontWeight.SemiBold, Kx.accent), modifier = Modifier.padding(top = 4.dp))
                        } else {
                            val order = picked.indexOf(i)
                            val s by animateFloatAsState(if (order >= 0) .92f else 1f, label = "tile")
                            Box(Modifier.weight(1f).height(108.dp).graphicsLayer { scaleX = s; scaleY = s }
                                .drawBehind { drawRect(Brush.linearGradient(tiles[i], cssAngleStart(150f, size), cssAngleEnd(150f, size))) }
                                .kxClick("Фото ${i + 1}") { if (order >= 0) picked.remove(i) else picked.add(i) }) {
                                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (order >= 0) Kx.accent else Color.Black.copy(alpha = .25f))
                                    .border(1.5.dp, Color.White.copy(alpha = .8f), CircleShape), contentAlignment = Alignment.Center) {
                                    if (order >= 0) Text("${order + 1}", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.onAccent))
                                }
                                if (i == 3) Text("0:12", style = KxType.style(11.sp, FontWeight.SemiBold), modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 6.dp, vertical = 1.dp))
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            } }
            if (picked.isEmpty()) kinds.chunked(4).forEach { row ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (ic, label, colors) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(label) {}.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(colors.first), contentAlignment = Alignment.Center) { Icon(ic, null, tint = colors.second) }
                            Text(label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.fill).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Подпись…", style = KxType.style(15.sp, color = Kx.text4))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Редактировать фото") {}, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tune, null, tint = Kx.text) }
                KxButton("Отправить ${picked.size}", height = 48.dp, icon = Icons.AutoMirrored.Rounded.Send) { onSend(picked.size) }
            }
        }
    }
}
