package com.kirox.app.ui.v3.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ChangeCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.DeleteSweep
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
/** The saved chat look (Вид чатов): bubble corners, text size, spacing, wallpaper. */
private data class ChatLook(val big: androidx.compose.ui.unit.Dp, val small: androidx.compose.ui.unit.Dp, val textSp: Float, val gap: androidx.compose.ui.unit.Dp, val wallpaper: Int) {
    val out get() = RoundedCornerShape(big, big, small, big)
    val inn get() = RoundedCornerShape(big, big, big, small)
}
private val LocalChatLook = androidx.compose.runtime.staticCompositionLocalOf { ChatLook(20.dp, 6.dp, 15f, 4.dp, 0) }
private fun chatLook(store: KxStore): ChatLook {
    val (big, small) = when (store.pref(KxPrefs.BUBBLES)) { 0 -> 10.dp to 4.dp; 2 -> 26.dp to 12.dp; else -> 20.dp to 6.dp }
    val text = listOf(13f, 15f, 17f, 19f)[store.pref(KxPrefs.TEXT_SIZE).coerceIn(0, 3)]
    val gap = when (store.pref(KxPrefs.DENSITY)) { 0 -> 2.dp; 2 -> 8.dp; else -> 4.dp }
    return ChatLook(big, small, text, gap, store.pref(KxPrefs.CHAT_WALLPAPER))
}
private val OutShape @Composable get() = LocalChatLook.current.out
private val InShape @Composable get() = LocalChatLook.current.inn

/** Outgoing bubble: CSS linear-gradient(160deg, #2E6B4B, #1F4D36). */
private fun Modifier.outBubble() = this.drawBehind {
    drawRect(Brush.linearGradient(listOf(Kx.accentDark, Kx.bubbleTo), cssAngleStart(160f, size), cssAngleEnd(160f, size)))
}
private fun Modifier.bubble(out: Boolean) = if (out) outBubble() else background(InBubble)

/** Lets bubbles ask the store to download their media (set by ChatScreen). */
private var LocalStoreDownload: ((Int) -> Unit)? = null
/** wave3-media: a tap on a photo/video bubble opens the Viewer / VideoPlayer board (set by ChatScreen). */
private var LocalOpenMedia: ((KxMessage) -> Unit)? = null
/** wave5: a tap on a document bubble downloads it, then opens it through «Проверка файлов» (KxFileOpener). */
private var LocalOpenFile: ((KxMessage) -> Unit)? = null
/** wave5: a tap on a reply quote jumps to the original message (set by ChatScreen). */
private var LocalJumpTo: ((Long) -> Unit)? = null
/** wave5 (fixer-media): store + chat of the open conversation for the media bubbles (voice, round video, GIF, music, file progress). */
private class BubbleMedia(val store: KxStore, val chatId: Long)
private var LocalBubbleMedia: BubbleMedia? = null

private data class PendingDelete(val message: KxMessage, val forAll: Boolean, val id: Long = System.nanoTime())

/** Conversation (board «Чат»): flat header + pinned bar, bubbles, glass composer, round mic/send button. */
@Composable
fun ChatScreen(store: KxStore, chatId: Long, onBack: () -> Unit, onCall: (Boolean) -> Unit = {}, onMenu: () -> Unit = {}, onProfile: () -> Unit = {}, onOpen: (String) -> Unit = {}, onOpenChat: (Long) -> Unit = {}) {
    val header = store.header(chatId)
    // Restored after activity recreation (saved back stack): the history is not open yet → open it.
    LaunchedEffect(chatId) { if (store !is com.kirox.app.ui.v3.demo.KxDemoStore && store.historyChatId != chatId) store.openChat(chatId) }
    LocalStoreDownload = store::download
    LocalBubbleMedia = BubbleMedia(store, chatId)
    LocalOpenMedia = { m -> onOpen((if (m.kind == KxMsgKind.Video) "VideoPlayer:" else "Viewer:") + "$chatId:${m.id}") }
    val look = chatLook(store)
    androidx.compose.runtime.CompositionLocalProvider(LocalChatLook provides look) { ChatScreenBody(store, chatId, header, onBack, onCall, onMenu, onProfile, onOpen, onOpenChat) }
}

@Composable
private fun ChatScreenBody(store: KxStore, chatId: Long, header: KxChatHeader, onBack: () -> Unit, onCall: (Boolean) -> Unit, onMenu: () -> Unit, onProfile: () -> Unit, onOpen: (String) -> Unit, onOpenChat: (Long) -> Unit) {
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    var chatMenu by remember { mutableStateOf(false) }
    // Undo send (Чат → «Отмена отправки»): the text waits this many seconds before store.send; «Отменить» returns it.
    val undoSecs = store.pref(KxChatPrefs.UNDO_SEND)
    var held by remember(chatId) { mutableStateOf<Pair<String, Long?>?>(null) }
    var heldLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(held) { val h = held ?: return@LaunchedEffect; heldLeft = undoSecs; while (heldLeft > 0) { delay(1000); heldLeft-- }; store.send(chatId, h.first, h.second); if (held === h) held = null }
    DisposableEffect(chatId) { onDispose { held?.let { store.send(chatId, it.first, it.second) } } }
    val look = LocalChatLook.current
    val toast = LocalKxToast.current
    val fileOpener = com.kirox.app.ui.v3.components.rememberKxFileOpener(store)
    LocalOpenFile = { m ->
        when {
            demo || m.fileId == 0 -> {}
            // Not on the phone yet → the download starts and openFile answers «Файл ещё не скачан»; tap again when done.
            // wave5 (fixer-media): a tap while it downloads cancels; the download goes into Telegram's list (Downloads / Files boards).
            store.fileState(m.fileId).let { it.path == null && it.downloading } -> store.cancelDownload(m.fileId)
            store.fileState(m.fileId).path == null -> store.downloadMessageFile(chatId, m.id, m.fileId)
            else -> fileOpener.open(m.fileId, m.fileName)
        }
    }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var text by rememberSaveable(chatId) { mutableStateOf("") }
    var replyTo by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    var menuFor by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    var editing by remember(chatId) { mutableStateOf<KxMessage?>(null) }
    // Text in the composer before an edit started: put back when the edit ends (the edited text is not a draft).
    var beforeEdit by remember(chatId) { mutableStateOf("") }
    var pending by remember(chatId) { mutableStateOf<PendingDelete?>(null) }
    androidx.activity.compose.BackHandler(enabled = editing != null || replyTo != null) { if (editing != null) text = beforeEdit; editing = null; replyTo = null }
    var attach by remember { mutableStateOf(false) }
    var circleMode by rememberSaveable { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Boolean?>(null) } // null = no, false = voice, true = circle
    var recSecs by remember { mutableIntStateOf(0) }
    val messages = store.messages(chatId).filter { it.id != pending?.message?.id }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // wave5: the chat's Telegram draft fills an empty composer once (old UI did the same); setDraft is not called for it.
    var draftLoaded by rememberSaveable(chatId) { mutableStateOf(false) }
    val serverDraft = store.chatDraft(chatId)
    LaunchedEffect(serverDraft) { if (!draftLoaded && serverDraft != null) { if (text.isEmpty() && editing == null && serverDraft.isNotBlank()) text = serverDraft; draftLoaded = true } }
    // wave5: scroll to a jump target (search hit, JumpDate, pinned bar, reply quote) once the store has loaded it.
    val jump = store.jumpTarget(chatId)
    LaunchedEffect(jump, messages.size) {
        if (jump == 0L) return@LaunchedEffect
        val idx = messages.asReversed().indexOfFirst { it.id == jump }
        if (idx >= 0) { listState.animateScrollToItem(idx); store.jumpDone() }
    }
    LocalJumpTo = { id -> if (id != 0L) store.jumpTo(chatId, id) }
    // wave5: the chat on screen gets no in-app banner or sound (old conversation does the same).
    DisposableEffect(chatId) {
        if (!demo) com.kirox.app.notify.InAppAlerts.openChatId = chatId
        onDispose { if (com.kirox.app.notify.InAppAlerts.openChatId == chatId) com.kirox.app.notify.InAppAlerts.openChatId = 0L }
    }
    // Follow the newest message only when it is ours or the list already sits at the bottom (older pages and
    // incoming messages while reading further up must not yank the list).
    val newest = messages.lastOrNull()
    LaunchedEffect(newest?.id) { if (newest != null && (newest.out || listState.firstVisibleItemIndex <= 1)) listState.animateScrollToItem(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hint) { if (hint != null) { delay(1800); hint = null } }
    LaunchedEffect(recording) { recSecs = 0; while (recording != null) { delay(1000); recSecs++ } }
    LaunchedEffect(pending?.id) { val p = pending ?: return@LaunchedEffect; delay(5000); store.delete(chatId, listOf(p.message.id), p.forAll); if (pending?.id == p.id) pending = null }
    val latestPending by rememberUpdatedState(pending)
    DisposableEffect(chatId) { onDispose { latestPending?.let { store.delete(chatId, listOf(it.message.id), it.forAll) } } }
    // Links in bubbles: web → in-app browser («Встроенный браузер») or the phone's browser; @name / t.me → the chat in kiroX;
    // #tag → Hashtag board; /command → sent. KxChatPrefs.LINK_WARN on: a link whose text is not its address asks first.
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var linkAsk by remember { mutableStateOf<KxConfirm?>(null) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    val openTarget: (KxLinkTarget) -> Unit = { t ->
        when (t) {
            is KxLinkTarget.Web -> if (!demo && store.tweaks["inAppBrowser"] == true) browserUrl = t.url
                else if (runCatching { uriHandler.openUri(t.url) }.isFailure) toast("Не удалось открыть ссылку")
            is KxLinkTarget.External -> if (runCatching { uriHandler.openUri(t.uri) }.isFailure) toast("Нет приложения для этой ссылки")
            is KxLinkTarget.Username -> if (demo) toast("@${t.name}") else scope.launch {
                val id = runCatching { store.publicChat(t.name) }.getOrNull()
                if (id != null) onOpenChat(id) else toast("Не найдено: @${t.name}")
            }
            is KxLinkTarget.User -> if (demo) toast("Профиль недоступен в демо") else scope.launch {
                val id = runCatching { store.privateChat(t.userId) }.getOrNull()
                if (id != null) onOpenChat(id) else toast("Не удалось открыть чат")
            }
            is KxLinkTarget.Invite -> onOpen("JoinRequest:${t.link}")
            is KxLinkTarget.Hashtag -> onOpen("Hashtag:#${t.tag}")
            is KxLinkTarget.Command -> if (header.composer == KxComposer.Write) store.send(chatId, t.command) else toast(t.command)
        }
    }
    val onLinkTap: (KxMessage, KxEntity) -> Unit = { msg, e ->
        val t = KxChatLinks.target(msg.text, e)
        val why = if ((store.prefs[KxChatPrefs.LINK_WARN] ?: 1) != 0) KxChatLinks.warning(msg.text, e) else null
        if (t != null) {
            if (why == null) openTarget(t)
            else {
                val label = KxChatLinks.part(msg.text, e).orEmpty()
                val address = (t as? KxLinkTarget.Web)?.url ?: e.url.orEmpty()
                val forged = com.kirox.app.ui.v3.data.KxChat3Mapping.linkWarning(label, address) != null
                linkAsk = KxConfirm(
                    title = if (forged) "Осторожно: подозрительная ссылка" else "Открыть ссылку?",
                    text = "Текст ссылки: «$label»\n$address\n\n" + (if (forged) why + ". Не вводите там пароли и коды." else why),
                    yes = if (forged) "Всё равно открыть" else "Открыть", danger = forged,
                ) { openTarget(t) }
            }
        }
    }

    val msgActions = remember(chatId) { MsgActions(
        vote = { msg, opts -> store.votePoll(chatId, msg.id, opts) },
        button = { msg, r, c -> scope.launch {
            val answer = runCatching { store.pressButton(chatId, msg.id, r, c) }.getOrNull()
            when {
                answer == null -> {}
                answer.startsWith("http://") || answer.startsWith("https://") || answer.startsWith("tg://") -> KxChatLinks.url(answer)?.let { t ->
                    if (t is KxLinkTarget.Web && (store.prefs[KxChatPrefs.LINK_WARN] ?: 1) != 0)
                        linkAsk = KxConfirm("Открыть ссылку?", "Бот предлагает открыть\n$answer", "Открыть", danger = false) { openTarget(t) }
                    else openTarget(t)
                }
                else -> toast(answer)
            }
        } },
        retry = { msg -> store.retrySend(chatId, msg.id) },
    ) }

    androidx.compose.runtime.CompositionLocalProvider(LocalLinkTap provides onLinkTap, LocalMsgActions provides msgActions) {
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        // Two soft glows: radial-gradient(70% 40% at 100% 20%, .16) + (60% 35% at 0% 75%, .10).
        if (look.wallpaper == 2) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F2A1D), Kx.bg, Color(0xFF0B1A13)))))
        if (look.wallpaper == 1) Canvas(Modifier.fillMaxSize()) {
            val step = 22.dp.toPx(); var y = 0f
            while (y < size.height) { var x = if ((y / step).toInt() % 2 == 0) 0f else step / 2; while (x < size.width) { drawCircle(Kx.accent.copy(alpha = .06f), 1.6.dp.toPx(), Offset(x, y)); x += step }; y += step }
        }
        if (look.wallpaper == 0) Canvas(Modifier.fillMaxSize()) {
            fun glow(cx: Float, cy: Float, rx: Float, ry: Float, a: Float) {
                scale(1f, ry / rx, Offset(cx, cy)) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF3F9A6C).copy(alpha = a), Color.Transparent), Offset(cx, cy), rx), rx, Offset(cx, cy))
                }
            }
            glow(size.width, size.height * .2f, size.width * .7f, size.height * .4f, .16f)
            glow(0f, size.height * .75f, size.width * .6f, size.height * .35f, .10f)
        }
        val strangerPeer = if (!demo && header.kind == KxChatKind.Private && messages.any { !it.out && it.kind != KxMsgKind.Service }) store.peer(chatId) else null
        val stranger = strangerPeer != null && !strangerPeer.isContact && !strangerPeer.blocked && !header.status.contains("бот")
        val top = (if (header.pinned != null) 184.dp else 130.dp) + (if (stranger) 54.dp else 0.dp)
        LazyColumn(
            Modifier.fillMaxSize().imePadding().then(if (menuFor != null) Modifier.blur(10.dp) else Modifier), state = listState, reverseLayout = true,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = top, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap, Alignment.Bottom)
        ) {
            val reversed = messages.asReversed()
            itemsIndexed(reversed, key = { _, it -> it.id }) { index, m ->
                // Older history: ask for the next page when the top 10 come into view (real store; demo has everything).
                if (index >= reversed.size - 10 && !store.historyComplete(chatId)) LaunchedEffect(reversed.size) { store.loadOlder(chatId) }
                Column(Modifier.fillMaxWidth().animateItem(fadeInSpec = tween(250), fadeOutSpec = tween(350), placementSpec = tween(350, easing = KxMotion.rise))) {
                    m.dayHeader?.let { DayPill(it) }
                    // wave3-account: gestures (ChatGestures board) and the word filter (KxChatGestures.kt)
                    val service = m.kind == KxMsgKind.Service
                    KxMessageSwipe(
                        onLeft = if (service) null else when (KxGestureActions.left(store)) {
                            1 -> { { if (demo) toast("Выберите чат для пересылки") else onOpen("Forward:$chatId:${m.id}") } }
                            2 -> { { store.saveToFavorites(chatId, listOf(m.id)); toast("Сохранено в Избранное") } }
                            else -> { { replyTo = m; editing = null } }
                        },
                        onRight = if (service) null else when (KxGestureActions.right(store)) { 0 -> onBack; 1 -> { { replyTo = m; editing = null } }; else -> null },
                    ) {
                        KxFilteredMessage(store, header.kind, m) {
                            MessageBubble(m, isGroup = header.kind == KxChatKind.Group,
                                onLongPress = { when (KxGestureActions.hold(store)) { 1 -> if (demo) toast("Выбрано: 1") else onOpen("MultiSelect:$chatId:${m.id}"); 2 -> store.translate(chatId, m.id, m.translation == null); else -> menuFor = m } },
                                onReact = { store.react(chatId, m.id, it) },
                                onReply = { replyTo = m; editing = null }, onMenu = { menuFor = m },
                                onDoubleTap = when (KxGestureActions.double(store)) {
                                    1 -> { { store.react(chatId, m.id, "❤") } }
                                    2 -> { { replyTo = m; editing = null } }
                                    3 -> { { clipboard.setText(androidx.compose.ui.text.AnnotatedString(m.text)); toast("Текст скопирован") } }
                                    else -> null
                                })
                        }
                    }
                }
            }
        }
        // Real store: the history is empty until TDLib returns the first page — no greeting card (its 👋 sends!) before that.
        val historyKnownEmpty = demo || (!store.historyLoading(chatId) && store.historyComplete(chatId))
        if (messages.isEmpty() && !historyKnownEmpty) KxSpinnerOrNothing(Modifier.align(Alignment.Center))
        if (messages.isEmpty() && historyKnownEmpty && header.kind != KxChatKind.Saved && header.kind != KxChatKind.Channel)
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
        // wave5: unread @mention / reaction jump buttons above «Вниз» (old chat had them); each jumps to the oldest unread one.
        val listRow = if (demo) null else store.chats.firstOrNull { it.id == chatId }
        Column(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 164.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to (listRow?.unreadReaction == true), false to (listRow?.unreadMention == true)).forEach { (reactions, on) ->
                if (on) Box(Modifier.size(44.dp).shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = .5f)).clip(CircleShape)
                    .background(Color(0xCC161B18)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.16f), CircleShape)
                    .kxClick(if (reactions) "Непрочитанная реакция" else "Непрочитанное упоминание") {
                        scope.launch { store.unreadMentions(chatId, reactions).lastOrNull()?.let { store.jumpTo(chatId, it.messageId) } }
                    }, contentAlignment = Alignment.Center) {
                    if (reactions) Icon(Icons.Rounded.Favorite, null, Modifier.size(20.dp), tint = Kx.accent)
                    else Text("@", style = KxType.style(18.sp, FontWeight.ExtraBold, Kx.accent))
                }
            }
        }
        ChatHeader(header.copy(typing = if (recording != null) (if (recording == true) "записывает видео" else "записывает голосовое") else header.typing),
            onBack, onCall, { chatMenu = true }, Modifier.align(Alignment.TopCenter), onProfile, stranger = stranger,
            onStranger = { block -> if (block) linkAsk = KxConfirm("Заблокировать ${header.title}?", "Пользователь не сможет писать и звонить вам.", "Заблокировать") { store.block(chatId, true) } else onOpen("ContactBar:$chatId") }) { header.pinned?.let { store.pin(chatId, it.messageId, false) } }

        if (recording == true) CircleRecording(recSecs)
        when (header.composer) {
            KxComposer.Write -> Composer(
                text = text, onText = { text = it; if (editing == null) store.setDraft(chatId, it) },
                replyTo = replyTo ?: editing, editing = editing != null,
                onCancelReply = { if (editing != null) text = beforeEdit; replyTo = null; editing = null },
                onSend = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        val e = editing
                        if (e != null) { store.edit(chatId, e.id, t); toast("Сообщение изменено") }
                        else if (undoSecs > 0 && !demo) { held?.let { store.send(chatId, it.first, it.second) }; held = t to replyTo?.id }
                        else store.send(chatId, t, replyTo?.id)
                        text = if (e != null) beforeEdit else ""; replyTo = null; editing = null
                    }
                },
                attachOpen = attach, onAttach = { attach = !attach },
                circleMode = circleMode, onToggleMode = {
                    // Real account: round videos have no camera recorder yet — stay on voice and say so instead of recording nothing.
                    if (!demo) hint = "Кружки пока не поддерживаются — удерживайте для голосового"
                    else { circleMode = !circleMode; hint = if (circleMode) "Удерживайте, чтобы записать кружок" else "Удерживайте, чтобы записать голосовое" }
                },
                recording = recording, recSecs = recSecs,
                onRecStart = { recording = circleMode; store.recordStart(chatId, circleMode) },
                onRecEnd = { send ->
                    val r = recording; recording = null
                    if (r != null) store.recordStop(chatId, send, recSecs.coerceAtLeast(1))
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                // wave5 (fixer-media): the Stickers board (stickers / GIFs / emoji of this chat; sends through the store).
                onStickers = { attach = false; onOpen(if (demo) "Stickers" else "Stickers:$chatId") },
            )
            else -> ChannelBar(header.composer, Modifier.align(Alignment.BottomCenter)) {
                if (header.composer == KxComposer.Mute || header.composer == KxComposer.Unmute) store.setMuted(chatId, header.composer == KxComposer.Mute)
                else if (header.composer == KxComposer.Join) store.joinChat(chatId)
            }
        }
        pending?.let { p -> UndoBar(p, Modifier.align(Alignment.BottomCenter)) { pending = null } }
        // Mic hint: small bubble above the button (right 12, bottom 90), not the generic toast.
        AnimatedVisibility(hint != null, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 90.dp),
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }, exit = fadeOut(tween(200))) {
            Text(hint ?: "", style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xF21E2823))
                .border(1.dp, Kx.w(.12f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
        }
        if (attach) { if (demo) AttachSheet(onClose = { attach = false }, onSend = { n -> attach = false; toast(if (n == 1) "Фото отправлено" else "Отправлено фото: $n") }, onOpen = onOpen)
            else RealAttachSheet(store, chatId, onClose = { attach = false }, onOpen = onOpen, toast = { toast(it) }) }
        held?.let { h ->
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 14.dp, end = 14.dp, bottom = 84.dp).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(Color(0xF21E2823)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Отправка через $heldLeft с", style = KxType.style(14.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                Text("Отменить", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .kxClick("Отменить отправку") { text = h.first; held = null }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        // wave5: kiroX «Сохранять удалённые» — what was kept for this chat (old UI ⋮ «Удалённые сообщения»).
        val keptExtra = if (!demo && store.tweaks["keepDeleted"] == true) listOf(ChatMenuExtra(Icons.Outlined.DeleteSweep, "Удалённые сообщения") {
            scope.launch {
                val list = store.keptDeleted(chatId)
                linkAsk = KxConfirm("Удалённые сообщения", if (list.isEmpty()) "Здесь появятся сообщения, которые удалят после включения сохранения."
                    else list.take(15).joinToString("\n\n") { "${it.first}\n${it.second}" } + (if (list.size > 15) "\n\n…и ещё ${list.size - 15}" else ""), "Готово", danger = false) {}
            }
        }) else emptyList()
        if (chatMenu) ChatMenuSheet(header, onDismiss = { chatMenu = false }, onOpen = onOpen, extra = keptExtra, forum = !demo && store.chats.firstOrNull { it.id == chatId }?.forum == true)
        menuFor?.let { m ->
            MessageMenuSheet(
                m, peerName = header.title, onDismiss = { menuFor = null },
                onReply = { replyTo = m; editing = null; menuFor = null },
                onEdit = { if (editing == null) beforeEdit = text; editing = m; replyTo = null; text = store.editableText(chatId, m.id) ?: m.text; menuFor = null },
                onReact = { store.react(chatId, m.id, it); menuFor = null },
                onDelete = { forAll -> pending = PendingDelete(m, forAll); menuFor = null },
                onForward = { menuFor = null; if (demo) toast("Выберите чат для пересылки") else onOpen("Forward:$chatId:${m.id}") },
                onPin = { store.pin(chatId, m.id, !m.pinned); menuFor = null },
                onSave = { store.saveToFavorites(chatId, listOf(m.id)); menuFor = null; toast("Сохранено в Избранное") },
                onSelect = { menuFor = null; if (demo) toast("Выбрано: 1") else onOpen("MultiSelect:$chatId:${m.id}") },
                onAllReactions = { menuFor = null; onOpen(if (demo) "ReactPicker" else "ReactPicker:$chatId:${m.id}") },
                more = if (demo) emptyList() else buildList {
                    val group = header.kind == KxChatKind.Group
                    add(Triple(Icons.Outlined.Info, "Сведения") { menuFor = null; onOpen("MsgInfo:$chatId:${m.id}") })
                    // wave5: quote a part of the message (Quote board, store.sendQuote).
                    if (m.text.isNotBlank() && header.composer == KxComposer.Write) add(Triple(Icons.Outlined.FormatQuote, "Цитировать") { menuFor = null; onOpen("Quote:$chatId:${m.id}") })
                    if (m.out && group) add(Triple(Icons.Outlined.DoneAll, "Кто прочитал") { menuFor = null; onOpen("Seen:$chatId:${m.id}") })
                    // wave5: old-UI message menu items.
                    else if (m.reactions.isNotEmpty() && header.kind != KxChatKind.Private) add(Triple(Icons.Outlined.FavoriteBorder, "Кто отреагировал") { menuFor = null; onOpen("Seen:$chatId:${m.id}") })
                    if (m.edited && store.tweaks["keepEdits"] == true) add(Triple(Icons.Outlined.History, "История изменений") {
                        menuFor = null
                        scope.launch {
                            val versions = store.editHistory(chatId, m.id)
                            linkAsk = KxConfirm("История изменений", if (versions.isEmpty()) "Прежний текст не сохранён: сообщение изменили до включения сохранения."
                                else versions.joinToString("\n\n") { "${it.first}\n${it.second}" } + "\n\nСейчас\n" + m.text, "Готово", danger = false) {}
                        }
                    })
                    if (m.out && header.composer == KxComposer.Write && m.kind != KxMsgKind.Service) add(Triple(Icons.Outlined.Replay, "Отправить ещё раз") {
                        menuFor = null; store.forward(chatId, listOf(m.id), chatId, hideSender = true, dropCaption = false); toast("Отправлено ещё раз") })
                    if (m.out && m.canEdit && (m.kind == KxMsgKind.Photo || m.kind == KxMsgKind.Video)) add(Triple(Icons.Outlined.ChangeCircle, "Заменить медиа") { menuFor = null; onOpen("ReplaceMedia:$chatId:${m.id}") })
                    if (!m.out && header.kind != KxChatKind.Saved) add(Triple(Icons.Outlined.Flag, "Пожаловаться") { menuFor = null; onOpen("Report:$chatId:${m.id}") })
                },
                onTranslate = { store.translate(chatId, m.id, m.translation == null); menuFor = null },
                onCopied = { menuFor = null; toast("Текст скопирован") },
                canDeleteForAll = m.canDeleteForAll && header.kind != KxChatKind.Saved,
                isGroup = header.kind == KxChatKind.Group || header.kind == KxChatKind.Channel,
            )
        }
        linkAsk?.let { c -> KxConfirmDialog(c) { linkAsk = null } }
        fileOpener.Dialog()
    }
    }
    browserUrl?.let { url ->
        com.kirox.app.ui.chat.InAppBrowser(url, onTelegramLink = { link -> browserUrl = null; KxChatLinks.url(link)?.let { if (it !is KxLinkTarget.Web) openTarget(it) else runCatching { uriHandler.openUri(link) } } }) { browserUrl = null }
    }
}

@Composable
private fun ChatHeader(h: KxChatHeader, onBack: () -> Unit, onCall: (Boolean) -> Unit, onMenu: () -> Unit, modifier: Modifier, onProfile: () -> Unit,
                       stranger: Boolean = false, onStranger: (Boolean) -> Unit = {}, onUnpin: () -> Unit) {
    Column(modifier.fillMaxWidth().background(Color(0xFF0D100E)).drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
        .statusBarsPadding().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", size = 48.dp, filled = false, tint = Kx.text, onClick = onBack)
            Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).kxClick("Профиль: ${h.title}", onClick = onProfile).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    KxAvatar(h.title, h.chatId, size = 42.dp, icon = if (h.kind == KxChatKind.Saved) Icons.Rounded.Bookmark else null, colorIndex = h.colorIndex, photo = h.avatar)
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
                .border(1.dp, Kx.w(.06f), RoundedCornerShape(16.dp)).kxClick("Закреплённое сообщение") { h.pinned?.let { LocalJumpTo?.invoke(it.messageId) } }.padding(horizontal = 12.dp),
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
        // wave5: a stranger wrote → add / block, as the old chat and Telegram (full flow on the ContactBar board).
        if (stranger) Row(Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121614))
            .border(1.dp, Kx.w(.06f), RoundedCornerShape(16.dp)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1.4f).fillMaxHeight().kxClick("Добавить в контакты") { onStranger(false) }, contentAlignment = Alignment.Center) {
                Text("Добавить в контакты", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), maxLines = 1)
            }
            Box(Modifier.size(1.dp, 24.dp).background(Kx.w(.08f)))
            Box(Modifier.weight(1f).fillMaxHeight().kxClick("Заблокировать") { onStranger(true) }, contentAlignment = Alignment.Center) {
                Text("Заблокировать", style = KxType.style(13.sp, FontWeight.Bold, Kx.danger), maxLines = 1)
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
fun MessageBubble(m: KxMessage, isGroup: Boolean, onLongPress: () -> Unit, onReact: (String) -> Unit, onDoubleTap: (() -> Unit)? = null,
                  /** a11y (P1-4): TalkBack custom actions «Ответить» / «Меню» (gestures may be set to something else). */
                  onReply: (() -> Unit)? = null, onMenu: (() -> Unit)? = null) {
    val align = if (m.out) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        val bm = LocalBubbleMedia
        // wave5 (fixer-media): round video = tap plays it in the circle (downloads first); GIF = tap plays/pauses the loop.
        var roundPlay by remember(m.id) { mutableStateOf(false) }
        var gifTapped by remember(m.id) { mutableStateOf(false) }
        val onTap: () -> Unit = {
            when (m.kind) {
                KxMsgKind.File -> LocalOpenFile?.invoke(m)
                KxMsgKind.Music -> if (bm != null && m.fileId != 0) bm.store.playTracks(listOf(KxTrack(bm.chatId, m.id, m.fileName.substringBeforeLast('.').ifBlank { "Аудио" }, fileId = m.fileId)), 0)
                KxMsgKind.VideoNote -> if (bm != null && m.fileId != 0) {
                    val st = bm.store.fileState(m.fileId)
                    when {
                        st.path == null && st.downloading -> { bm.store.cancelDownload(m.fileId); roundPlay = false }
                        st.path == null && m.media == null -> { bm.store.download(m.fileId); roundPlay = true }
                        else -> roundPlay = !roundPlay
                    }
                    if (roundPlay && !m.out) bm.store.markListened(bm.chatId, m.id)
                }
                KxMsgKind.Gif -> if (bm != null && m.fileId != 0) {
                    if (m.media == null && bm.store.fileState(m.fileId).path == null) bm.store.download(m.fileId)
                    gifTapped = !gifTapped
                }
                else -> {}
            }
        }
        // a11y (P1-4): reply / react / menu reachable without gestures (TalkBack «Действия»).
        val a11yActs = buildList {
            if (onReply != null) add(CustomAccessibilityAction("Ответить") { onReply(); true })
            add(CustomAccessibilityAction("Реакция ❤") { onReact("❤"); true })
            if (onMenu != null) add(CustomAccessibilityAction("Меню сообщения") { onMenu(); true })
        }
        val press = Modifier.semantics { customActions = a11yActs }
            .combinedClickable(interactionSource = null, indication = null, onClick = onTap, onLongClick = onLongPress, onDoubleClick = onDoubleTap)
        when (m.kind) {
            KxMsgKind.Service -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(m.text, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text3), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Kx.fill).padding(horizontal = 12.dp, vertical = 4.dp))
            }
            KxMsgKind.VideoNote -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = press.padding(vertical = 2.dp)) {
                if (m.out) MetaText(m, Kx.text4)
                VideoNoteCircle(m, roundPlay) { roundPlay = false }
                if (!m.out) Text(m.time, style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.padding(bottom = 6.dp))
            }
            KxMsgKind.Voice -> if (m.out && m.played == 0f && m.status != KxOutState.Read) Row(
                press.width(220.dp).clip(OutShape).outBubble().padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VoicePlayButton(m)
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
                // wave5 (fixer-media): play/pause through the app's player, progress + tap-to-seek on the bars, «→A» = Telegram's speech recognition.
                val vp = bm?.let { voiceProgress(it.store, m) }
                var transcript by remember(m.id) { mutableStateOf<String?>(null) }
                var transcribing by remember(m.id) { mutableStateOf(false) }
                val trScope = rememberCoroutineScope()
                val trToast = LocalKxToast.current
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoicePlayButton(m)
                    Waveform(m.waveform.ifEmpty { listOf(.4f, .7f, .5f, .9f, .6f, .3f, .7f, .9f, .5f, .3f, .6f, .4f, .25f, .5f) }, vp ?: m.played,
                        Modifier.weight(1f).height(26.dp).then(if (vp != null && bm != null) Modifier.pointerInput(m.id) {
                            detectTapGestures { o -> bm.store.playerSeek((o.x / size.width).coerceIn(0f, 1f)) }
                        } else Modifier))
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .2f)).kxClick(if (transcript != null) "Скрыть расшифровку" else "Расшифровать голосовое") {
                        when {
                            transcript != null -> transcript = null
                            bm == null || transcribing -> {}
                            else -> { transcribing = true; trScope.launch {
                                val r = bm.store.transcribe(bm.chatId, m.id) { part -> transcript = part }
                                transcribing = false
                                if (r.error != null) { transcript = null; trToast(r.error) } else transcript = r.text ?: transcript
                            } }
                        }
                    }, contentAlignment = Alignment.Center) {
                        Text(if (transcribing) "…" else "→A", style = KxType.style(12.sp, FontWeight.ExtraBold, OutMeta))
                    }
                }
                transcript?.let { Text(it.ifBlank { "…" }, style = KxType.style(14.sp, color = if (m.out) OutText else Kx.text, lineHeight = 19.sp)) }
                val time = vp?.let { bm?.store?.player?.positionMs?.let { ms -> "${ms / 60000}:${((ms / 1000) % 60).toString().padStart(2, '0')}" } } ?: m.meta
                Row { Text(time, style = KxType.style(11.sp, color = OutMeta), modifier = Modifier.weight(1f)); MetaText(m, OutMeta) }
            }
            KxMsgKind.Photo, KxMsgKind.Video, KxMsgKind.Gif -> {
              // wave5 (fixer-media): spoiler = blurred until a tap; view-once = a cover, opened once in a protected view (OpenMessageContent, FLAG_SECURE).
              var unveiled by remember(m.id) { mutableStateOf(false) }
              var onceOpen by remember(m.id) { mutableStateOf(false) }
              val hidden = m.viewOnce || (m.spoiler && !unveiled)
              if (onceOpen && bm != null) ViewOnceView(bm, m) { onceOpen = false }
              val mediaTap: () -> Unit = when {
                  m.viewOnce -> { { if (!m.out && bm != null) onceOpen = true } }
                  m.spoiler && !unveiled -> { { unveiled = true } }
                  m.kind == KxMsgKind.Gif -> onTap
                  else -> { { LocalOpenMedia?.invoke(m) } }
              }
              Column(Modifier.combinedClickable(interactionSource = null, indication = null, onClick = mediaTap, onLongClick = onLongPress, onDoubleClick = if (m.kind == KxMsgKind.Gif) onDoubleTap else null).width(230.dp).clip(if (m.out) OutShape else RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)).bubble(m.out).padding(4.dp)) {
                // wave5 (fixer-media): no whole-video download just to draw the bubble; photos/GIFs follow «Автозагрузка медиа» in the store.
                // Demo / older stores without a path still ask for photos here.
                if (m.media == null && m.fileId != 0 && m.kind == KxMsgKind.Photo && bm == null) LaunchedEffect(m.fileId) { LocalStoreDownload?.invoke(m.fileId) }
                val gifPath = if (m.kind == KxMsgKind.Gif) m.media ?: bm?.store?.fileState(m.fileId)?.path?.takeIf { gifTapped } else null
                // Auto-downloaded GIF: plays by itself when autoplay is on (a tap pauses); otherwise a tap downloads and plays it.
                val gifPlays = gifPath != null && (if (m.media != null) (bm?.store?.autoplayGifs ?: true) != gifTapped else gifTapped)
                Box(Modifier.fillMaxWidth().height(if (m.aspect > 0f) (222f / m.aspect).coerceIn(118f, 300f).dp else 118.dp).clip(RoundedCornerShape(18.dp))
                    .drawBehind { drawRect(Brush.linearGradient(0f to Color(0xFF355E49), .55f to Color(0xFF1A2C22), 1f to Color(0xFF0F1712), start = cssAngleStart(150f, size), end = cssAngleEnd(150f, size))) },
                    contentAlignment = Alignment.Center) {
                    val pic = m.media?.takeIf { m.kind == KxMsgKind.Photo } ?: m.thumb
                    // Spoiler: the picture only blurred (Android 12+; older phones cannot blur → not drawn). View-once: never drawn in the chat.
                    if (pic != null && !m.viewOnce && (!hidden || android.os.Build.VERSION.SDK_INT >= 31)) KxImage(pic, Modifier.matchParentSize().then(if (hidden) Modifier.blur(28.dp) else Modifier))
                    if (!hidden && gifPlays && gifPath != null) com.kirox.app.ui.chat.LoopingVideo(gifPath, Modifier.matchParentSize())
                    if (hidden) Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (m.viewOnce) Icons.Rounded.LocalFireDepartment else Icons.Rounded.VisibilityOff, null, Modifier.size(18.dp), tint = Kx.text)
                        Text(when {
                            !m.viewOnce -> "Спойлер"
                            m.out -> if (m.kind == KxMsgKind.Video) "Одноразовое видео" else "Одноразовое фото"
                            else -> "Открыть один раз"
                        }, style = KxType.style(13.sp, FontWeight.Bold))
                    }
                    else if (m.kind == KxMsgKind.Gif) {
                        val st = if (m.fileId != 0 && gifPath == null) bm?.store?.fileState(m.fileId) else null
                        if (st?.downloading == true) androidx.compose.material3.CircularProgressIndicator(progress = { st.fraction.coerceAtLeast(.02f) }, modifier = Modifier.size(40.dp), color = Kx.accentLight, strokeWidth = 3.dp, trackColor = Kx.w(.15f))
                        else if (!gifPlays) Icon(Icons.Rounded.Gif, "GIF", Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)).padding(4.dp), tint = Kx.text)
                    } else if (pic == null || m.kind != KxMsgKind.Photo) Icon(if (m.kind == KxMsgKind.Photo) Icons.Rounded.Image else Icons.Rounded.PlayCircle, null, Modifier.size(36.dp), tint = Kx.accentLight.copy(alpha = .7f))
                }
                if (m.text.isNotEmpty()) TextWithMeta(m, Modifier.padding(start = 9.dp, end = 9.dp, top = 7.dp, bottom = 4.dp))
              }
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
                        .kxClick("Перейти к сообщению") { LocalJumpTo?.invoke(r.messageId) }
                        .drawBehind { drawRect(if (m.out) OutMeta else Kx.accent, size = Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)) {
                        Text(r.sender, style = KxType.style(13.sp, FontWeight.Bold, if (m.out) OutMeta else Kx.accent))
                        KxLine(r.text, KxType.style(13.sp, color = if (m.out) Color(0xFFBFDCCB) else Kx.text3))
                    }
                }
                if (m.kind == KxMsgKind.File || m.kind == KxMsgKind.Music) FileLine(m)
                val poll = m.poll
                val pad = when { m.reply != null -> PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 7.dp); m.out -> PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 7.dp); else -> PaddingValues(start = 13.dp, end = 13.dp, top = 9.dp, bottom = 7.dp) }
                if (m.kind == KxMsgKind.Poll && poll != null) PollBlock(m, poll) else TextWithMeta(m, Modifier.padding(pad))
            }
        }
        if (m.buttons.isNotEmpty()) InlineButtons(m)
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
            val lk = LocalChatLook.current
            val onLink = LocalLinkTap.current
            val body = if (m.translation != null || m.entities.isEmpty()) androidx.compose.ui.text.AnnotatedString(m.translation ?: m.text)
                else remember(m.text, m.entities, m.out, onLink) { bubbleText(m, onLink) }
            Text(body, style = KxType.style(lk.textSp.sp, color = if (m.out) OutText else Kx.text, lineHeight = (lk.textSp + 6f).sp))
            if (m.translation != null) Text("Перевод · оригинал в меню", style = KxType.style(11.sp, color = if (m.out) OutMeta else Kx.text4))
        }
        MetaText(m, if (m.out) OutMeta else Kx.text4)
    }
}

/** A small «Загрузка…» while the first page of the history comes (real store). */
@Composable
private fun KxSpinnerOrNothing(modifier: Modifier) {
    Text("Загрузка…", style = KxType.style(13.sp, color = Kx.text3), modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Kx.fill).padding(horizontal = 12.dp, vertical = 6.dp))
}

/** Poll votes, inline bot buttons and «send again» from the bubbles (set by ChatScreenBody). */
private class MsgActions(val vote: (KxMessage, List<Int>) -> Unit, val button: (KxMessage, Int, Int) -> Unit, val retry: (KxMessage) -> Unit)
private val LocalMsgActions = androidx.compose.runtime.staticCompositionLocalOf<MsgActions?> { null }

private fun votesWord(n: Int) = if (n % 10 == 1 && n % 100 != 11) "голос" else if (n % 10 in 2..4 && n % 100 !in 12..14) "голоса" else "голосов"

/** Poll / quiz in the bubble (board «Опрос», poll in chat): answers with result bars after voting, «Проголосовать» for multiple choice, «Отменить голос». */
@Composable
private fun PollBlock(m: KxMessage, p: KxPoll) {
    val act = LocalMsgActions.current
    val sub = if (m.out) Color(0xFFA9D9BF) else Kx.text3
    val ink = if (m.out) OutText else Kx.text
    var picked by remember(m.id, p.voted) { mutableStateOf(setOf<Int>()) }
    val reveal = p.voted || p.closed
    Column(Modifier.widthIn(min = 260.dp).padding(14.dp)) {
        Text(p.question, style = KxType.style(16.sp, FontWeight.ExtraBold, ink, lineHeight = 21.sp))
        Text((if (p.anonymous) "Анонимный " else "Открытый ") + (if (p.quiz) "тест" else "опрос") + (if (p.multiple) " · несколько ответов" else "") + (if (p.closed) " · завершён" else ""),
            style = KxType.style(12.sp, color = sub), modifier = Modifier.padding(top = 3.dp))
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            p.options.forEachIndexed { i, o ->
                val pick = if (reveal) o.chosen else i in picked
                val wrong = reveal && p.quiz && o.chosen && !o.correct
                val bar = when { !reveal -> Color.Transparent; p.quiz -> if (o.correct) Color(0x598FD3AC) else if (o.chosen) Color(0x4DFF8A80) else Kx.w(.08f); pick -> Color(0x598FD3AC); else -> Kx.w(.1f) }
                val mark = if (reveal) pick || (p.quiz && o.correct) else pick
                val ring = when { wrong -> Color(0xFFFF8A80); mark -> Kx.accent; else -> Color(0xFFA9D9BF) }
                val shape = RoundedCornerShape(if (p.multiple) 6.dp else 11.dp)
                Box(Modifier.fillMaxWidth().heightIn(min = 42.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .18f))
                    .then(if (!reveal && p.canVote && act != null) Modifier.kxClick(o.text) {
                        if (p.multiple) picked = if (i in picked) picked - i else picked + i else act.vote(m, listOf(i))
                    } else Modifier)) {
                    if (reveal) Box(Modifier.matchParentSize().fillMaxWidth(o.percent.coerceIn(0, 100) / 100f).background(bar))
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(22.dp).clip(shape).border(2.dp, ring, shape).background(if (mark) ring else Color.Transparent), contentAlignment = Alignment.Center) {
                            if (mark) Icon(if (wrong) Icons.Rounded.Close else Icons.Rounded.Check, null, Modifier.size(15.dp), tint = Color(0xFF06150D))
                        }
                        Text(o.text, style = KxType.style(14.sp, FontWeight.SemiBold, ink), modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                        if (reveal) Text("${o.percent}%", style = KxType.style(13.sp, FontWeight.ExtraBold, ink))
                    }
                }
            }
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (p.total > 0) "${p.total} ${votesWord(p.total)}" else "Пока нет голосов", style = KxType.style(12.sp, color = sub), modifier = Modifier.weight(1f))
            if (!reveal && p.multiple && picked.isNotEmpty() && act != null) Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent)
                .kxClick("Проголосовать") { act.vote(m, picked.sorted()) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("Проголосовать", style = KxType.style(12.sp, FontWeight.ExtraBold, Color(0xFF06150D)))
            }
            if (p.voted && !p.closed && !p.quiz && act != null) Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(alpha = .2f))
                .kxClick("Отменить голос") { act.vote(m, emptyList()) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Text("Отменить голос", style = KxType.style(12.sp, FontWeight.Bold, ink))
            }
            MetaText(m, if (m.out) OutMeta else Kx.text4)
        }
    }
}

/** Inline keyboard of a bot message: rows of buttons under the bubble; a tap asks the bot (store.pressButton). */
@Composable
private fun InlineButtons(m: KxMessage) {
    val act = LocalMsgActions.current
    Column(Modifier.widthIn(max = 312.dp).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        m.buttons.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEachIndexed { c, label ->
                    Box(Modifier.weight(1f).heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.08f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(12.dp))
                        .kxClick(label) { act?.button?.invoke(m, r, c) }.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = KxType.style(13.sp, FontWeight.SemiBold), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** A tap on a link/@mention/#hashtag/phone in a bubble (set by ChatScreenBody; null = links not tappable). */
private val LocalLinkTap = androidx.compose.runtime.staticCompositionLocalOf<((KxMessage, KxEntity) -> Unit)?> { null }

/** Message text with its entities: bold/italic/underline/strike/code, and links in the LinkWarn colour (#8FD3AC, underlined) that react to a tap. */
private fun bubbleText(m: KxMessage, onLink: ((KxMessage, KxEntity) -> Unit)?) = androidx.compose.ui.text.buildAnnotatedString {
    val text = m.text
    append(text)
    val linkInk = if (m.out) OutMeta else Color(0xFF8FD3AC)
    m.entities.forEach { e ->
        if (KxChatLinks.part(text, e) == null) return@forEach
        val start = e.offset; val end = e.offset + e.length
        val style = when (e.type) {
            KxEntityType.Bold -> androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)
            KxEntityType.Italic -> androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            KxEntityType.Underline -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            KxEntityType.Strike -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            KxEntityType.Code, KxEntityType.Pre -> androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            else -> null
        }
        if (style != null) addStyle(style, start, end)
        if (KxChatLinks.isTappable(e) && KxChatLinks.target(text, e) != null) {
            val web = e.type == KxEntityType.Link || e.type == KxEntityType.TextLink
            val ink = androidx.compose.ui.text.SpanStyle(color = linkInk,
                textDecoration = if (web) androidx.compose.ui.text.style.TextDecoration.Underline else null)
            if (onLink == null) addStyle(ink, start, end)
            else addLink(androidx.compose.ui.text.LinkAnnotation.Clickable(
                tag = "kx-link-$start", styles = androidx.compose.ui.text.TextLinkStyles(ink)) { onLink(m, e) }, start, end)
        }
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
            KxOutState.Failed -> { val retry = LocalMsgActions.current?.retry
                Icon(Icons.Rounded.ErrorOutline, "Не отправлено", Modifier.size(14.dp).then(if (retry != null) Modifier.kxClick("Отправить ещё раз") { retry(m) } else Modifier), tint = Kx.danger) }
            KxOutState.None -> {}
        }
    }
}

@Composable
private fun FileLine(m: KxMessage) {
    Row(Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // wave5 (fixer-media): download state on the tile — arrow (not on the phone), progress ring (tap cancels), the kind icon when ready.
        val bm = LocalBubbleMedia
        val st = if (bm != null && m.fileId != 0) bm.store.fileState(m.fileId) else null
        val onPhone = st == null || st.path != null || m.media != null
        val playing = m.kind == KxMsgKind.Music && bm != null && bm.store.player.let { p -> p.playing && p.track?.fileId == m.fileId }
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (m.out) OutMeta else Kx.accent), contentAlignment = Alignment.Center) {
            when {
                st != null && !onPhone && st.downloading -> {
                    androidx.compose.material3.CircularProgressIndicator(progress = { st.fraction.coerceAtLeast(.02f) }, modifier = Modifier.size(36.dp), color = Kx.onAccent, strokeWidth = 2.5.dp, trackColor = Kx.onAccent.copy(alpha = .25f))
                    Icon(Icons.Rounded.Close, "Отменить загрузку", Modifier.size(18.dp), tint = Kx.onAccent)
                }
                !onPhone && m.kind == KxMsgKind.File -> Icon(Icons.Rounded.Download, "Скачать", tint = Kx.onAccent)
                else -> Icon(if (m.kind == KxMsgKind.Music) (if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) else Icons.Rounded.Description, null, tint = Kx.onAccent)
            }
        }
        val sub = if (st != null && !onPhone && st.downloading && st.total > 0) "${com.kirox.app.ui.v3.data.KxFormat.fileSize(st.downloaded)} / ${com.kirox.app.ui.v3.data.KxFormat.fileSize(st.total)}" else m.meta
        Column { KxLine(m.fileName, KxType.style(14.sp, FontWeight.SemiBold, if (m.out) OutText else Kx.text)); Text(sub, style = KxType.style(12.sp, color = if (m.out) OutMeta else Kx.text4)) }
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

/** wave5 (fixer-media): is the app's player on this voice message → its 0..1 progress, else null. */
private fun voiceProgress(store: KxStore, m: KxMessage): Float? {
    if (m.fileId == 0) return null
    val p = store.player
    val path = store.fileState(m.fileId).path ?: m.media ?: return null
    if (p.path != path) return null
    return if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) else 0f
}

/** Play / pause of a voice bubble (store.playVoice downloads first); a received one is marked listened (OpenMessageContent). */
@Composable
private fun VoicePlayButton(m: KxMessage) {
    val bm = LocalBubbleMedia
    val st = if (bm != null && m.fileId != 0) bm.store.fileState(m.fileId) else null
    val playing = bm != null && voiceProgress(bm.store, m) != null && bm.store.player.playing
    Box(Modifier.size(38.dp).clip(CircleShape).background(OutMeta).kxClick(if (playing) "Пауза" else "Слушать") {
        if (bm != null && m.fileId != 0) { bm.store.playVoice(m.fileId); if (!m.out && !playing) bm.store.markListened(bm.chatId, m.id) }
    }, contentAlignment = Alignment.Center) {
        if (st != null && st.path == null && st.downloading)
            androidx.compose.material3.CircularProgressIndicator(progress = { st.fraction.coerceAtLeast(.02f) }, modifier = Modifier.size(34.dp), color = Color(0xFF0B2216), strokeWidth = 2.dp, trackColor = Color(0x330B2216))
        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color(0xFF0B2216))
    }
}

/**
 * wave5 (fixer-media): a received view-once / self-destructing photo or video, full screen in a secure window
 * (no screenshots / recents preview). Opening sends OpenMessageContent (Telegram starts the timer and the sender
 * sees it opened); Telegram then deletes the message itself, so after closing it is gone from the chat.
 */
@Composable
private fun ViewOnceView(bm: BubbleMedia, m: KxMessage, onClose: () -> Unit) {
    val st = bm.store.fileState(m.fileId)
    LaunchedEffect(m.id) { if (st.path == null) bm.store.download(m.fileId); bm.store.markListened(bm.chatId, m.id) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)) {
        Box(Modifier.fillMaxSize().background(Color.Black).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) {
            val path = st.path
            when {
                path == null -> androidx.compose.material3.CircularProgressIndicator(progress = { st.fraction.coerceAtLeast(.02f) }, modifier = Modifier.size(48.dp), color = Kx.accent, strokeWidth = 3.dp, trackColor = Kx.w(.15f))
                m.kind == KxMsgKind.Video -> com.kirox.app.ui.chat.OneShotVideo(path, Modifier.fillMaxSize(), onClose)
                else -> KxImage(path, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            }
            Text("Исчезнет после закрытия", style = KxType.style(13.sp, FontWeight.SemiBold), modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun VideoNoteCircle(m: KxMessage, play: Boolean = false, onEnd: () -> Unit = {}) {
    val bm = LocalBubbleMedia
    val st = if (bm != null && m.fileId != 0) bm.store.fileState(m.fileId) else null
    val path = m.media ?: st?.path
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
            // wave5 (fixer-media): the round's own preview frame, inline playback with sound, download ring.
            val thumb = m.thumb
            if (play && path != null) com.kirox.app.ui.chat.OneShotVideo(path, Modifier.matchParentSize(), onEnd)
            else if (thumb != null) KxImage(thumb, Modifier.matchParentSize())
            else Icon(Icons.Rounded.Person, null, Modifier.size(64.dp).offset(y = 13.dp), tint = if (m.out) Color(0x4DC8F0D8) else Color(0x59DCD0FF))
            if (st != null && !(play && path != null)) Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                if (st != null && path == null && st.downloading)
                    androidx.compose.material3.CircularProgressIndicator(progress = { st.fraction.coerceAtLeast(.02f) }, modifier = Modifier.size(38.dp), color = Kx.text, strokeWidth = 2.dp, trackColor = Kx.w(.2f))
                Icon(if (st != null && path == null && st.downloading) Icons.Rounded.Close else Icons.Rounded.PlayArrow, null, Modifier.size(24.dp), tint = Kx.text)
            }
            if (!(play && path != null)) Text(if (m.out) "${m.meta} ✓" else "${m.meta} · 🔇", style = KxType.style(11.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
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
    onStickers: () -> Unit = {},
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
                        KxIconButton(Icons.Outlined.Mood, "Стикеры и эмодзи", size = 46.dp, filled = false, tint = Kx.text2, onClick = onStickers)
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
private fun AttachSheet(onClose: () -> Unit, onSend: (Int) -> Unit, onOpen: (String) -> Unit = {}) {
    val camToast = LocalKxToast.current
    val boardOf = mapOf("Галерея" to "Gallery", "Файл" to "FilePicker", "Место" to "Location", "Опрос" to "Poll", "Контакт" to "ShareContact", "Музыка" to "Music", "Игры" to "DiceGames", "Позже" to "Schedule")
    androidx.activity.compose.BackHandler(onBack = onClose)
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
                        if (i < 0) Column(Modifier.weight(1f).height(108.dp).background(Color(0xFF151A17)).kxClick("Снять фото") { camToast("Камера в демо не снимает — выберите фото ниже") },
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
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(label) { boardOf[label]?.let { onClose(); onOpen(it) } }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(colors.first), contentAlignment = Alignment.Center) { Icon(ic, null, tint = colors.second) }
                            Text(label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.fill).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Подпись…", style = KxType.style(15.sp, color = Kx.text4))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Редактировать фото") { onClose(); onOpen("Editor") }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tune, null, tint = Kx.text) }
                KxButton("Отправить ${picked.size}", height = 48.dp, icon = Icons.AutoMirrored.Rounded.Send) { onSend(picked.size) }
            }
        }
    }
}
