package com.kirox.app.ui.chat

import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.TelegramChatHistory
import com.kirox.app.telegram.TelegramChatItem
import com.kirox.app.telegram.TelegramDelivery
import com.kirox.app.telegram.TelegramMessage
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramPeerKind
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Actions the real chat screen can perform; implemented by the message repository. */
/** Chat-level actions behind the ⋮ menu. */
interface TelegramChatMenuActions {
    suspend fun isMuted(chatId: Long): Boolean
    suspend fun setMuted(chatId: Long, muted: Boolean)
    suspend fun clearHistory(chatId: Long)
    suspend fun setAutoDelete(chatId: Long, seconds: Int)
    suspend fun deleteChat(chatId: Long)
}

interface TelegramChatActions {
    fun loadOlder()
    fun sendText(text: String, replyToMessageId: Long?, silent: Boolean = false, scheduleAt: Int = 0)
    suspend fun scheduledMessages(): List<com.kirox.app.telegram.ScheduledMessage> = emptyList()
    suspend fun sendScheduledNow(messageId: Long) {}
    suspend fun deleteScheduled(messageId: Long) {}
    fun sendPhoto(path: String, caption: String)
    fun sendDocument(path: String, caption: String)
    fun sendLocation(latitude: Double, longitude: Double) {}
    fun sendContact(userId: Long, firstName: String, lastName: String, phone: String) {}
    fun sendVoice(path: String, durationSeconds: Int, waveform: ByteArray)
    fun toggleReaction(messageId: Long, emoji: String)
    fun sendVideo(path: String, caption: String, durationSeconds: Int, width: Int, height: Int)
    fun edit(messageId: Long, text: String)
    fun delete(messageIds: List<Long>, forEveryone: Boolean)
    fun forward(toChatId: Long, messageIds: List<Long>, asCopy: Boolean = false, removeCaption: Boolean = false)
    /** Stores the unsent text as the chat's Telegram draft; blank text clears it. */
    fun saveDraft(chatId: Long, text: String, threadId: Long = 0, forumTopicId: Int = 0) {}
    /** Our activity in the chat (typing, recording); null cancels it. */
    fun setActivity(chatId: Long, activity: com.kirox.app.telegram.ChatActivity?) {}
    /** 2–10 photos and videos as one album; a null meta marks a photo. */
    fun sendAlbum(items: List<Pair<String, com.kirox.app.telegram.VideoMeta?>>, caption: String) {}
    fun sendPanelItem(item: com.kirox.app.telegram.TelegramPanelItem, replyToMessageId: Long?) {}
    fun sendVideoNote(path: String, durationSeconds: Int, length: Int) {}
    fun pin(messageId: Long) {}
    fun unpin(messageId: Long) {}
    /** Loads history up to [messageId] and scrolls there (TelegramChatHistory.jumpTo). */
    fun jumpTo(messageId: Long) {}
    fun jumpDone() {}
    /** Server search in the chat: results newest first and the id to continue from (0 at the end). */
    suspend fun search(query: String, fromMessageId: Long): Pair<List<TelegramMessage>, Long> = emptyList<TelegramMessage>() to 0L
    suspend fun availableReactions(messageId: Long): List<String>? = null
    suspend fun forumTopics(chatId: Long): List<com.kirox.app.telegram.TelegramForumTopic> = emptyList()
    suspend fun createForumTopic(chatId: Long, name: String): Int = 0
    fun vote(messageId: Long, indices: List<Int>) {}
    fun sendPoll(question: String, options: List<String>, anonymous: Boolean, multipleAnswers: Boolean, correctOption: Int?) {}
    /** Bot's answer to an inline button: text to show and URL to open. */
    suspend fun pressButton(messageId: Long, button: com.kirox.app.telegram.TelegramButton): Pair<String, String?>? = null
    suspend fun translate(messageId: Long, language: String): String? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramChatScreen(
    history: TelegramChatHistory,
    error: String?,
    onErrorShown: () -> Unit,
    forwardTargets: List<TelegramChatItem>,
    actions: TelegramChatActions,
    onBack: () -> Unit,
    /** Starts a call with the peer of a private chat (its chat id is the user id). */
    onCall: ((isVideo: Boolean) -> Unit)? = null,
    menu: TelegramChatMenuActions? = null,
    /** What someone in this chat is doing now ("печатает…"); replaces the status line while set. */
    activityLine: String? = null,
    /** Stickers and GIFs for the panel; emoji only when null. */
    stickers: StickerPanelSource? = null,
    /** Opens the comments of a channel post (by its message id). */
    onOpenComments: ((Long) -> Unit)? = null,
    /** Opens a topic of the forum shown in this screen. */
    onOpenTopic: ((com.kirox.app.telegram.TelegramForumTopic) -> Unit)? = null,
    /** Joins (or starts) the video chat of a group or channel. */
    onGroupCall: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tweaks by com.kirox.app.settings.KiroxTweaks.state.collectAsState()
    // «Подтверждения» from kiroX settings: a question before a sticker, GIF, voice message or call.
    var confirmAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    fun confirmed(question: String, ask: Boolean, action: () -> Unit) { if (ask) confirmAction = question to action else action() }
    // «Отложенные»: the picker for «Отправить позже» and this chat's waiting messages.
    var schedulePicking by remember { mutableStateOf(false) }
    var scheduled by remember { mutableStateOf<List<com.kirox.app.telegram.ScheduledMessage>>(emptyList()) }
    var scheduledOpen by remember { mutableStateOf(false) }
    var scheduledReload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val style = com.kirox.app.ui.theme.LocalChatStyle.current
    val fusion = com.kirox.app.ui.theme.Fusion.colors
    val appearance = remember(style, fusion) {
        ChatAppearance(
            incomingColor = fusion.incomingBubble.argb(), outgoingColor = fusion.outgoingBubble.argb(), inputColor = fusion.inputBar.argb(),
            bubbleRadiusDp = style.bubbleRadiusDp, textScale = style.textScale, messageSpacingDp = style.density.spacingDp, bubblePaddingDp = style.density.paddingDp
        )
    }
    var input by rememberSaveable(history.chatId, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var replyTo by remember(history.chatId) { mutableStateOf<MessageUi?>(null) }
    var editing by remember(history.chatId) { mutableStateOf<MessageUi?>(null) }
    var selected by remember { mutableStateOf<MessageUi?>(null) }
    var deleting by remember { mutableStateOf<MessageUi?>(null) }
    var forwarding by remember { mutableStateOf<List<Long>?>(null) }
    /** Multi-select: ids of selected messages; empty when not selecting. */
    var selectedIds by remember(history.chatId) { mutableStateOf(setOf<Long>()) }
    var deletingMany by remember { mutableStateOf<List<Long>?>(null) }
    var highlighted by remember(history.chatId) { mutableStateOf(0L) }
    var reactionChoices by remember { mutableStateOf(DEFAULT_REACTIONS) }
    var attachments by rememberSaveable { mutableStateOf(false) }
    var stickerPanel by rememberSaveable { mutableStateOf(false) }
    var showBotKeyboard by remember(history.chatId) { mutableStateOf(true) }
    var creatingPoll by remember { mutableStateOf(false) }
    var translation by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    /** Sends the typed text at [at] (unix seconds) or, with SEND_WHEN_ONLINE, when the person comes online. */
    fun sendScheduled(at: Int) {
        val text = input.text.trim()
        if (text.isEmpty()) return
        actions.sendText(text, replyTo?.id, scheduleAt = at)
        input = TextFieldValue(); replyTo = null; editing = null
        notice = if (at == com.kirox.app.telegram.SEND_WHEN_ONLINE) "Отправится, когда собеседник будет в сети" else "Отправится ${scheduledTime(at)}"
        scheduledReload++
    }
    LaunchedEffect(history.chatId, scheduledReload) {
        // Telegram needs a moment to register a just scheduled message.
        if (scheduledReload > 0) kotlinx.coroutines.delay(700.milliseconds)
        scheduled = if (history.chatId == 0L) emptyList() else runCatching { actions.scheduledMessages() }.getOrDefault(emptyList())
    }
    var videoNote by remember { mutableStateOf(false) }
    val noteCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) videoNote = true else notice = "Для видеосообщения нужны камера и микрофон"
    }
    val listState = rememberLazyListState()

    val isGroup = history.peerKind == TelegramPeerKind.Group
    // Newest first for reverseLayout; date separators are inserted as system rows.
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordLevel by remember { mutableStateOf(0f) }
    var recordMs by remember { mutableStateOf(0L) }
    var recordPaused by remember { mutableStateOf(false) }
    fun startRecording() {
        if (recorder.start()) { recording = true; recordMs = 0 } else notice = "Не удалось начать запись"
    }
    val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startRecording() else notice = "Без доступа к микрофону голосовое не записать" }
    LaunchedEffect(recording) {
        val chatId = history.chatId
        while (recording) {
            recordLevel = recorder.sampleLevel()
            recordMs = recorder.elapsedMs
            // Throttled by the repository to one request every few seconds.
            if (chatId != 0L) actions.setActivity(chatId, com.kirox.app.telegram.ChatActivity.RecordingVoice)
            kotlinx.coroutines.delay(100.milliseconds)
        }
        if (chatId != 0L) actions.setActivity(chatId, null)
    }
    DisposableEffect(Unit) { onDispose { recorder.stop(discard = true); VoicePlayer.stop() } }
    // Drafts: the chat's Telegram draft fills an empty input once; unsent text is saved back when
    // leaving the chat or the app. Text being edited is an existing message, not a draft.
    var draftLoaded by remember(history.chatId) { mutableStateOf(false) }
    LaunchedEffect(history.chatId, history.draft, history.loaded) {
        // A placeholder history has no draft yet: taking it would show an empty input and then
        // overwrite the real draft with "" on leaving.
        if (history.chatId == 0L || !history.loaded || draftLoaded) return@LaunchedEffect
        draftLoaded = true
        if (input.text.isEmpty() && history.draft.isNotEmpty()) input = TextFieldValue(history.draft, androidx.compose.ui.text.TextRange(history.draft.length))
    }
    // Last applied state; on a chat switch the old chat's onDispose runs before this is updated,
    // so it still holds that chat's text and not the next chat's input.
    val pendingDraft = remember { DraftHolder() }
    SideEffect {
        pendingDraft.chatId = history.chatId
        pendingDraft.threadId = history.threadId
        pendingDraft.forumTopicId = history.forumTopicId
        pendingDraft.text = if (editing == null && draftLoaded) input.text else null
    }
    // Another chat: the voice note of this one stops and the draft is saved.
    DisposableEffect(history.chatId) {
        onDispose { VoicePlayer.stop(); pendingDraft.save(actions) }
    }
    // Leaving the screen keeps a voice note playing (media notification); a recording is dropped.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
        if (recording) { recorder.stop(discard = true); recording = false; recordPaused = false }
        pendingDraft.save(actions)
    }
    var menuOpen by remember { mutableStateOf(false) }
    var searching by remember(history.chatId) { mutableStateOf(false) }
    var searchQuery by remember(history.chatId) { mutableStateOf("") }
    var muted by remember(history.chatId) { mutableStateOf<Boolean?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }

    var autoDeleteOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Search filters the loaded part of the conversation.
    val shownMessages = remember(history.messages, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) history.messages else history.messages.filter { it.text.contains(q, ignoreCase = true) }
    }
    // Server search: finds messages that are not loaded yet; tapping one scrolls the chat to it.
    var searchResults by remember(history.chatId) { mutableStateOf<List<TelegramMessage>?>(null) }
    var searchNext by remember(history.chatId) { mutableStateOf(0L) }
    /** Bumped per query: a page that arrives for an older query is dropped. */
    var searchGeneration by remember(history.chatId) { mutableStateOf(0) }
    LaunchedEffect(searchQuery, searching) {
        val q = searchQuery.trim()
        searchGeneration++
        searchResults = null
        searchNext = 0
        if (!searching || q.length < 2) return@LaunchedEffect
        kotlinx.coroutines.delay(400.milliseconds)
        runCatching { actions.search(q, 0) }
            .onSuccess { (found, next) -> searchResults = found; searchNext = next }
            .onFailure { searchResults = emptyList() }
    }
    val rows = remember(shownMessages) { toRows(shownMessages.asReversed()) }
    /** Every message of an album → all its ids: actions on an album act on all of it. */
    val albumIds = remember(rows) { rows.filter { it.album.size > 1 }.flatMap { row -> row.album.map { it.id to row.album.map { m -> m.id } } }.toMap() }
    fun idsOf(message: MessageUi): List<Long> = albumIds[message.id] ?: listOf(message.id)
    val byId = remember(history.messages) { history.messages.associateBy { it.id } }

    LaunchedEffect(error) { if (error != null) { notice = error; onErrorShown() } }
    LaunchedEffect(history.jumpTo, rows) {
        val target = history.jumpTo
        if (target == 0L) return@LaunchedEffect
        val index = rows.indexOfFirst { it.key == "m$target" || it.album.any { m -> m.id == target } }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            highlighted = target
            actions.jumpDone()
        }
    }
    // Own effect: jumpDone() restarts the one above, which would cancel this delay.
    LaunchedEffect(highlighted) {
        if (highlighted == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(1600.milliseconds)
        highlighted = 0
    }
    LaunchedEffect(listState, history.chatId) {
        snapshotFlow { listState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 15 } }
            .distinctUntilChanged().filter { it }.collect { actions.loadOlder() }
    }
    // Keep the newest message in view when it arrives while the user is at the bottom.
    LaunchedEffect(history.messages.lastOrNull()?.id) {
        val newest = history.messages.lastOrNull() ?: return@LaunchedEffect
        if (newest.isOutgoing || listState.firstVisibleItemIndex <= 3) listState.animateScrollToItem(0)
    }

    BackHandler(enabled = editing != null || replyTo != null) { editing = null; replyTo = null; input = TextFieldValue() }
    BackHandler(enabled = selectedIds.isNotEmpty()) { selectedIds = emptySet() }

    fun sendPicked(uri: Uri, asPhoto: Boolean) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { copyToCache(context, uri) }
            if (path == null) { notice = "Не удалось прочитать файл"; return@launch }
            val caption = input.text.trim()
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
            when {
                isVideo && asPhoto -> {
                    // Duration and size from the file itself; TDLib makes the preview.
                    val meta = withContext(Dispatchers.IO) { videoMeta(path) }
                    actions.sendVideo(path, caption, meta.first, meta.second, meta.third)
                }
                asPhoto -> actions.sendPhoto(path, caption)
                else -> actions.sendDocument(path, caption)
            }
            input = TextFieldValue()
        }
    }
    val files = com.kirox.app.ui.components.LocalTelegramFiles.current
    val linkOpener = LocalLinkOpener.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    /** Downloads the message's file if needed, then copies it to Downloads/KiroX. */
    fun saveMedia(message: MessageUi) {
        val file = message.media?.file ?: message.photo ?: return
        val name = message.media?.title?.takeIf { message.kind == MessageKind.Document || message.kind == MessageKind.Audio }.orEmpty()
        val mime = message.media?.mimeType?.ifBlank { null } ?: if (message.kind == MessageKind.Photo) "image/jpeg" else ""
        if (files == null) return
        scope.launch {
            notice = "Сохраняю…"
            files.download(file)
            // GetFile and DownloadFile answers can arrive in either order: wait for the file itself.
            val path = kotlinx.coroutines.withTimeoutOrNull(5.minutes) { files.state(file).first { it.path != null } }?.path
            notice = when {
                path == null -> "Не удалось скачать файл"
                saveToDownloads(context, path, name, mime) -> "Сохранено в «Загрузки/kiroX»"
                else -> "Не удалось сохранить файл"
            }
        }
    }
    fun sendAlbum(uris: List<Uri>) {
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    val path = copyToCache(context, uri) ?: return@mapNotNull null
                    val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                    path to if (isVideo) videoMeta(path).let { (d, w, h) -> com.kirox.app.telegram.VideoMeta(d, w, h) } else null
                }
            }
            when {
                items.isEmpty() -> { notice = "Не удалось прочитать файлы"; return@launch }
                items.size < uris.size -> notice = "Часть файлов не удалось прочитать"
            }
            actions.sendAlbum(items, input.text.trim())
            input = TextFieldValue()
        }
    }
    // Gallery: photos and videos; several are sent as one album, like in Telegram.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(ALBUM_LIMIT)) { uris ->
        when {
            uris.size == 1 -> sendPicked(uris[0], asPhoto = true)
            uris.size > 1 -> sendAlbum(uris)
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { sendPicked(it, asPhoto = false) } }
    // Camera tile: the photo goes to a cache file shared through the FileProvider, then is sent like a gallery pick.
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pickingContact by remember { mutableStateOf(false) }
    // «Локация»: the last known position from the system, asked once for permission; sent as a map point.
    fun sendCurrentLocation() {
        val manager = context.getSystemService(android.location.LocationManager::class.java)
        val last = runCatching {
            listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER, android.location.LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
        if (last != null) { actions.sendLocation(last.latitude, last.longitude); notice = "Геопозиция отправлена" }
        else notice = "Не удалось определить местоположение — включите геолокацию"
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) sendCurrentLocation() else notice = "Нет доступа к местоположению"
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> cameraUri?.takeIf { ok }?.let { sendPicked(it, asPhoto = true) } }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraUri?.let(takePhoto::launch) else notice = "Нет доступа к камере"
    }

    val peer = ChatPeerUi(
        id = history.chatId,
        name = history.threadTitle ?: if (history.peerKind == TelegramPeerKind.Saved) "Избранное" else history.title,
        status = activityLine ?: if (history.threadTitle != null) history.title else if (history.isForum) "темы" else when (history.peerKind) {
            TelegramPeerKind.User -> (if (history.isSecret) "🔒 секретный чат · " else "") + history.peerStatus?.let { TelegramFormat.status(it) }.orEmpty()
            TelegramPeerKind.Bot -> "бот"
            TelegramPeerKind.Group -> "группа"
            TelegramPeerKind.Channel -> "канал"
            else -> ""
        },
        initial = history.title.take(1).uppercase(),
        photo = history.photo,
        canVoiceCall = onCall != null && history.peerKind == TelegramPeerKind.User && !history.isSecret ||
            onGroupCall != null && history.threadTitle == null && (history.peerKind == TelegramPeerKind.Group || history.peerKind == TelegramPeerKind.Channel),
        canVideoCall = onCall != null && history.peerKind == TelegramPeerKind.User && !history.isSecret
    )
    if (profileOpen) androidx.compose.ui.window.Dialog(
        onDismissRequest = { profileOpen = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        PeerProfileScreen(
            chatId = history.chatId, title = peer.name, photo = peer.photo, status = peer.status,
            canCall = onCall != null && history.peerKind == TelegramPeerKind.User && !history.isSecret,
            onCall = { video -> profileOpen = false; confirmed(if (video) "Начать видеозвонок?" else "Позвонить?", tweaks.confirmCall) { onCall?.invoke(video) } },
            muted = muted,
            onToggleMute = menu?.let { m -> { target -> muted = target; scope.launch { runCatching { m.setMuted(history.chatId, target) }.onFailure { muted = !target } } } },
            onSearch = { profileOpen = false; searching = true },
            onOpenMessage = { id -> profileOpen = false; actions.jumpTo(id) },
            onBack = { profileOpen = false },
            onLeave = if (menu != null && (history.peerKind == TelegramPeerKind.Group || history.peerKind == TelegramPeerKind.Channel)) ({
                profileOpen = false
                scope.launch { runCatching { menu.deleteChat(history.chatId) }.onSuccess { onBack() }.onFailure { notice = "Не удалось выйти" } }
            }) else null
        )
    }

    // The wallpaper spans the whole screen, so a translucent Glass header and the floating composer sit over it.
    // «Сохранять в галерею» for this chat's type; Saved Messages are your own files and never copied.
    val galleryKinds by (context.applicationContext as com.kirox.app.KiroXApplication).telegram.media.saveToGallery.collectAsState()
    val galleryKind = when (history.peerKind) {
        TelegramPeerKind.User, TelegramPeerKind.Bot -> com.kirox.app.telegram.TelegramChatKind.Private
        TelegramPeerKind.Group -> com.kirox.app.telegram.TelegramChatKind.Group
        TelegramPeerKind.Channel -> com.kirox.app.telegram.TelegramChatKind.Channel
        else -> null
    }
    CompositionLocalProvider(LocalGalleryChatId provides history.chatId, LocalSaveToGallery provides (galleryKind in galleryKinds)) {
    Box(Modifier.fillMaxSize()) {
    com.kirox.app.ui.components.ChatWallpaperLayer(style.wallpaper, Modifier.matchParentSize())
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (selectedIds.isNotEmpty()) SelectionBar(
                count = selectedIds.size,
                onClose = { selectedIds = emptySet() },
                onCopy = {
                    val text = history.messages.filter { it.id in selectedIds && it.text.isNotBlank() }.joinToString("\n\n") { it.text }
                    if (text.isNotEmpty()) { clipboard.setText(AnnotatedString(text)); notice = "Скопировано" }
                    selectedIds = emptySet()
                },
                onForward = { forwarding = selectedIds.sorted(); },
                onDelete = { deletingMany = selectedIds.sorted() }
            ) else if (searching) Column(Modifier.statusBarsPadding()) {
                ChatSearchField(searchQuery, { searchQuery = it }, onClose = { searching = false; searchQuery = "" })
            } else Column {
                ChatHeader(peer, onBack, onProfile = {
                    profileOpen = true
                    if (menu != null) scope.launch { muted = runCatching { menu.isMuted(history.chatId) }.getOrNull() }
                }, onOverflow = {
                if (menu == null) notice = "Меню чата недоступно" else {
                    menuOpen = true
                    scope.launch { muted = runCatching { menu.isMuted(history.chatId) }.getOrNull() }
                }
            }, onCall = { video ->
                confirmed(if (video) "Начать видеозвонок?" else if (history.peerKind == TelegramPeerKind.User) "Позвонить?" else "Присоединиться к видеочату?", tweaks.confirmCall) {
                    if (history.peerKind == TelegramPeerKind.User) onCall?.invoke(video) else onGroupCall?.invoke()
                }
            })
                history.pinned?.let { pinned -> PinnedBar(pinned, count = history.pinnedIds.size) { actions.jumpTo(pinned.id) } }
            }
        },
        bottomBar = {
            if (recording) VoiceRecordingBar(
                elapsedMs = recordMs,
                level = recordLevel,
                paused = recordPaused,
                onPause = { if (recorder.isPaused) recorder.resume() else recorder.pause(); recordPaused = recorder.isPaused },
                onCancel = { recorder.stop(discard = true); recording = false; recordPaused = false },
                onSend = {
                    val note = recorder.stop()
                    recording = false
                    recordPaused = false
                    if (note != null) confirmed("Отправить голосовое?", tweaks.confirmVoice) { actions.sendVoice(note.file.absolutePath, note.durationSeconds, note.waveform) }
                    else notice = "Слишком короткое голосовое"
                }
            ) else if (history.peerKind == TelegramPeerKind.Bot && history.loaded && history.reachedStart && history.messages.isEmpty()) {
                // A bot nobody has written to yet: like Telegram, one «Запустить» button sends /start.
                Box(Modifier.fillMaxWidth().background(Fusion.colors.background).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    com.kirox.app.ui.components.FusionButton("Запустить", onClick = { actions.sendText("/start", null) })
                }
            } else if ((history.peerKind != TelegramPeerKind.Channel || history.threadTitle != null) && !history.isForum) Column {
                if (scheduled.isNotEmpty()) Row(
                    Modifier.fillMaxWidth().background(Fusion.colors.background).clickable { scheduledOpen = true }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KiroXIcon(KiroXIcon.Refresh, null, Modifier.size(18.dp), Fusion.colors.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Отложенные сообщения: ${scheduled.size}", color = Fusion.colors.accent, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(16.dp), Fusion.colors.textTertiary)
                }
                if (history.replyKeyboard.isNotEmpty() && showBotKeyboard) BotKeyboard(history.replyKeyboard) { text -> actions.sendText(text, null) }
                if (history.replyKeyboard.isNotEmpty()) Text(
                    if (showBotKeyboard) "Скрыть клавиатуру бота" else "Клавиатура бота",
                    color = Fusion.colors.textSecondary, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().background(Fusion.colors.background).clickable { showBotKeyboard = !showBotKeyboard }.padding(vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
                ChatComposer(
                value = input,
                reply = replyTo,
                editing = editing,
                onValueChange = { value ->
                    // Typing is reported only for new text, not for edits or cursor moves.
                    if (editing == null && value.text != input.text && history.chatId != 0L && history.peerKind != TelegramPeerKind.Saved) {
                        actions.setActivity(history.chatId, if (value.text.isEmpty()) null else com.kirox.app.telegram.ChatActivity.Typing)
                    }
                    input = value
                },
                onCancelContext = { replyTo = null; editing = null; input = TextFieldValue() },
                onAttach = { attachments = true },
                onEmoji = { stickerPanel = true },
                onSend = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        val edited = editing
                        if (edited != null) actions.edit(edited.id, text) else actions.sendText(text, replyTo?.id)
                        input = TextFieldValue(); replyTo = null; editing = null
                    }
                },
                onMic = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) startRecording()
                    else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                appearance = appearance,
                // Saved Messages make no sound anyway.
                onSendSilent = if (history.peerKind == TelegramPeerKind.Saved) null else ({
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        actions.sendText(text, replyTo?.id, silent = true)
                        input = TextFieldValue(); replyTo = null; editing = null
                        notice = "Отправлено без звука"
                    }
                }),
                onSchedule = { if (input.text.isNotBlank()) schedulePicking = true },
                // «Когда будет в сети» exists only for a person, not for groups, channels or yourself.
                onWhenOnline = if (history.peerKind == TelegramPeerKind.User) ({ sendScheduled(com.kirox.app.telegram.SEND_WHEN_ONLINE) }) else null
            )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val results = searchResults
            if (history.isForum) ForumTopicsList(history.chatId, actions, onOpen = { topic -> onOpenTopic?.invoke(topic) })
            else if (searching && results != null) SearchResults(
                results, canLoadMore = searchNext != 0L,
                onLoadMore = {
                    val q = searchQuery.trim()
                    val from = searchNext
                    val generation = searchGeneration
                    scope.launch {
                        val page = runCatching { actions.search(q, from) }.getOrNull()
                        if (generation != searchGeneration) return@launch
                        // A failed or empty page ends the list instead of spinning forever.
                        if (page == null || page.first.isEmpty()) { searchNext = 0; return@launch }
                        searchResults = (searchResults ?: emptyList()) + page.first
                        searchNext = page.second
                    }
                },
                onOpen = { message -> searching = false; searchQuery = ""; actions.jumpTo(message.id) }
            ) else if (history.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    if (history.reachedStart) Text("Сообщений пока нет", color = Fusion.colors.textSecondary)
                    else CircularProgressIndicator(Modifier.size(26.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp)
                }
            } else if (shownMessages.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ничего не найдено", color = Fusion.colors.textSecondary) }
            } else LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                // A read-only channel keeps room at the bottom for the notifications button.
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = if (history.peerKind == TelegramPeerKind.Channel && history.threadTitle == null && menu != null) 84.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(appearance.messageSpacingDp.dp)
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }, contentType = { _, row -> row.message.direction }) { index, row ->
                    // reverseLayout: the visually previous row is index + 1.
                    val previous = rows.getOrNull(index + 1)?.message
                    val grouped = previous != null && previous.direction == row.message.direction && previous.author == row.message.author && row.message.direction != MessageDirection.System
                    val next = rows.getOrNull(index - 1)?.message
                    val groupedBelow = next != null && next.direction == row.message.direction && next.author == row.message.author && row.message.direction != MessageDirection.System
                    // New messages slide in and the rest move smoothly instead of jumping.
                    Box(if (com.kirox.app.ui.theme.LocalMotionFactor.current > 0f) Modifier.animateItem() else Modifier) {
                    MessageRow(
                        row.message, grouped, appearance,
                        groupedBelow = groupedBelow,
                        album = row.album,
                        pinned = row.message.id in history.pinnedIds,
                        showAuthor = isGroup,
                        onLongClick = {
                            if (row.message.direction == MessageDirection.System) Unit
                            else if (selectedIds.isNotEmpty()) selectedIds = selectedIds.toggleAll(idsOf(row.message))
                            else selected = row.message
                        },
                        onReply = { replyTo = row.message; editing = null },
                        onNotice = { notice = it },
                        selected = row.message.id in selectedIds,
                        onClick = if (selectedIds.isNotEmpty() && row.message.direction != MessageDirection.System) ({ selectedIds = selectedIds.toggleAll(idsOf(row.message)) }) else null,
                        // «Двойной тап» from kiroX settings; off while selecting.
                        onDoubleClick = if (selectedIds.isNotEmpty() || row.message.direction == MessageDirection.System) null else when (tweaks.doubleTap) {
                            com.kirox.app.settings.DoubleTapAction.None -> null
                            com.kirox.app.settings.DoubleTapAction.Reaction -> ({ actions.toggleReaction(row.message.id, "\u2764") })
                            com.kirox.app.settings.DoubleTapAction.Reply -> ({ replyTo = row.message; editing = null })
                            com.kirox.app.settings.DoubleTapAction.Copy -> ({
                                if (row.message.text.isNotBlank()) {
                                    context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("kiroX", row.message.text))
                                    notice = "Скопировано"
                                }
                            })
                        },
                        onQuote = { id -> actions.jumpTo(id) },
                        highlighted = row.message.id == highlighted,
                        onVote = { indices -> actions.vote(row.message.id, indices) },
                        onButton = { button ->
                            val url = button.url
                            if (url != null) linkOpener?.invoke(url) ?: runCatching { uriHandler.openUri(url) }
                            else scope.launch {
                                val answer = runCatching { actions.pressButton(row.message.id, button) }.getOrNull()
                                answer?.first?.takeIf { it.isNotBlank() }?.let { notice = it }
                                answer?.second?.let { link -> linkOpener?.invoke(link) ?: runCatching { uriHandler.openUri(link) } }
                            }
                        },
                        onComments = if (history.peerKind == TelegramPeerKind.Channel && history.threadTitle == null && onOpenComments != null) ({ onOpenComments(row.message.id) }) else null
                    )
                    }
                }
                if (history.loadingOlder) item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp) }
                }
            }
            notice?.let { message ->
                Text(
                    message, color = Fusion.colors.textPrimary, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Fusion.colors.surface3).padding(horizontal = 16.dp, vertical = 11.dp)
                )
                LaunchedEffect(message) { kotlinx.coroutines.delay(2200.milliseconds); notice = null }
            }
                    // Read-only channel (approved board 6): a notifications switch where the composer would be.
            if (history.peerKind == TelegramPeerKind.Channel && history.threadTitle == null && menu != null && history.chatId != 0L) {
                LaunchedEffect(history.chatId) { muted = runCatching { menu.isMuted(history.chatId) }.getOrNull() }
                muted?.let { isMuted ->
                    Text(
                        if (isMuted) "ВКЛ. УВЕДОМЛЕНИЯ" else "ВЫКЛ. УВЕДОМЛЕНИЯ", color = Fusion.colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(Fusion.colors.surface1).clickable {
                                val target = !isMuted
                                muted = target
                                scope.launch { runCatching { menu.setMuted(history.chatId, target) }.onFailure { muted = isMuted } }
                            }.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
    }
    }

    if (pickingContact) ContactPickerSheet(onDismiss = { pickingContact = false }) { contact ->
        pickingContact = false
        val first = contact.name.substringBefore(' ')
        actions.sendContact(contact.id, first, contact.name.substringAfter(' ', ""), contact.phone)
    }
    if (attachments) AttachmentSheet(
        onDismiss = { attachments = false },
        onPick = { choice ->
            attachments = false
            when (choice) {
                "Фото и видео" -> pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                "Галерея" -> pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                "Видео" -> pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                "Музыка" -> pickFile.launch("audio/*")
                "Контакт" -> pickingContact = true
                "Локация" -> {
                    if (context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) sendCurrentLocation()
                    else locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                "Камера" -> {
                    val file = java.io.File(context.cacheDir, "camera").apply { mkdirs() }.let { java.io.File(it, "photo_${System.currentTimeMillis()}.jpg") }
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
                    cameraUri = uri
                    if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) takePhoto.launch(uri)
                    else cameraPermission.launch(android.Manifest.permission.CAMERA)
                }
                "Опрос" -> creatingPoll = true
                "Видеосообщение" -> {
                    val needed = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
                    if (needed.all { context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) videoNote = true
                    else noteCameraPermission.launch(needed)
                }
                else -> pickFile.launch("*/*")
            }
        },
        // Order of the approved media board: Галерея · Видео · Файлы / Камера … / Опрос · Музыка.
        actions = buildList {
            add(KiroXIcon.Media to "Галерея")
            add(KiroXIcon.Video to "Видео")
            add(KiroXIcon.File to "Файл")
            add(KiroXIcon.Camera to "Камера")
            add(KiroXIcon.Location to "Локация")
            add(KiroXIcon.Contacts to "Контакт")
            if (history.peerKind != TelegramPeerKind.Channel) add(KiroXIcon.Voice to "Видеосообщение")
            add(KiroXIcon.Music to "Музыка")
            if (!history.isSecret) add(KiroXIcon.Sliders to "Опрос")
        },
        footer = if (input.text.isNotBlank()) "Текст из поля ввода станет подписью" else null,
        top = if (history.isSecret) null else ({
            RecentMediaStrip(ALBUM_LIMIT) { uris ->
                attachments = false
                if (uris.size == 1) sendPicked(uris[0], asPhoto = true) else sendAlbum(uris)
            }
        })
    )
    selected?.let { message ->
        val source = byId[message.id]
        LaunchedEffect(message.id) {
            reactionChoices = DEFAULT_REACTIONS
            actions.availableReactions(message.id)?.takeIf { it.isNotEmpty() }?.let { reactionChoices = it }
        }
        MessageActionsSheet(
            message,
            onDismiss = { selected = null },
            onReply = { replyTo = message; editing = null; selected = null },
            onReaction = { emoji -> actions.toggleReaction(message.id, emoji); selected = null },
            onAction = { action ->
                selected = null
                when (action) {
                    "Копировать" -> { clipboard.setText(AnnotatedString(message.text)); notice = "Сообщение скопировано" }
                    "Редактировать" -> { editing = message; replyTo = null; input = TextFieldValue(message.text) }
                    "Удалить" -> if (albumIds.containsKey(message.id)) deletingMany = idsOf(message) else deleting = message
                    "Переслать" -> forwarding = idsOf(message)
                    "Сохранить" -> saveMedia(message)
                    "Закрепить" -> { actions.pin(message.id); notice = "Сообщение закреплено" }
                    "Открепить" -> actions.unpin(message.id)
                    "Выбрать" -> selectedIds = idsOf(message).toSet()
                    "Перевести" -> {
                        translation = message.text to null
                        scope.launch {
                            val language = Locale.getDefault().language.ifBlank { "ru" }
                            translation = message.text to (actions.translate(message.id, language) ?: "Перевод недоступен")
                        }
                    }
                }
            },
            reactions = history.peerKind != TelegramPeerKind.Saved,
            isPinned = message.id in history.pinnedIds,
            reactionChoices = reactionChoices,
            hidden = buildSet {
                if (history.isSecret) add("Закрепить")
                if (source?.canEdit != true) add("Редактировать")
                if (source?.delivery == TelegramDelivery.Sending) { add("Переслать"); add("Ответить") }
            }
        )
    }
    if (creatingPoll) PollCreateDialog(onDismiss = { creatingPoll = false }) { question, options, anonymous, multiple, correct ->
        creatingPoll = false
        actions.sendPoll(question, options, anonymous, multiple, correct)
    }
    translation?.let { (original, translated) ->
        AlertDialog(
            onDismissRequest = { translation = null },
            containerColor = Fusion.colors.surface2,
            title = { Text("Перевод", color = Fusion.colors.textPrimary) },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    if (translated == null) CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                    else Text(translated, color = Fusion.colors.textPrimary, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(original, color = Fusion.colors.textSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Row {
                    if (translated != null) TextButton(onClick = { clipboard.setText(AnnotatedString(translated)); notice = "Перевод скопирован"; translation = null }) { Text("Копировать", color = Fusion.colors.textPrimary) }
                    TextButton(onClick = { translation = null }) { Text("Закрыть", color = Fusion.colors.textSecondary) }
                }
            }
        )
    }
    if (videoNote) {
        val chatId = history.chatId
        LaunchedEffect(Unit) { actions.setActivity(chatId, com.kirox.app.telegram.ChatActivity.RecordingVideoNote) }
        VideoNoteRecorder(
            onDone = { note -> videoNote = false; actions.sendVideoNote(note.file.absolutePath, note.durationSeconds, note.length) },
            onCancel = { videoNote = false; actions.setActivity(chatId, null) },
            onError = { message -> videoNote = false; actions.setActivity(chatId, null); notice = message }
        )
    }
    if (stickerPanel) StickerPanel(
        chatId = history.chatId,
        source = stickers,
        onEmoji = { emoji ->
            // Inserted at the cursor, replacing a selection, like a keyboard would.
            val start = minOf(input.selection.start, input.selection.end)
            val end = maxOf(input.selection.start, input.selection.end)
            val text = input.text.replaceRange(start, end, emoji)
            input = TextFieldValue(text, androidx.compose.ui.text.TextRange(start + emoji.length))
        },
        onSend = { item ->
            val reply = replyTo?.id
            confirmed(if (item.isGif) "Отправить GIF?" else "Отправить стикер?", if (item.isGif) tweaks.confirmGif else tweaks.confirmSticker) { actions.sendPanelItem(item, reply) }
            replyTo = null
            stickerPanel = false
        },
        onDismiss = { stickerPanel = false }
    )
    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Fusion.colors.surface2,
            title = { Text("Удалить сообщение?", color = Fusion.colors.textPrimary) },
            text = {
                Text(
                    when {
                        history.peerKind == TelegramPeerKind.Saved -> "Сообщение будет удалено из «Избранного»."
                        message.direction == MessageDirection.Outgoing -> "Можно удалить только у себя или у всех участников."
                        else -> "Сообщение будет удалено у вас."
                    },
                    color = Fusion.colors.textSecondary
                )
            },
            confirmButton = {
                Row {
                    if (message.direction == MessageDirection.Outgoing && history.peerKind != TelegramPeerKind.Saved) {
                        TextButton(onClick = { actions.delete(listOf(message.id), forEveryone = true); deleting = null }) { Text("У всех", color = Fusion.colors.danger) }
                    }
                    TextButton(onClick = { actions.delete(listOf(message.id), forEveryone = false); deleting = null }) { Text("У меня", color = Fusion.colors.danger) }
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена", color = Fusion.colors.textPrimary) } }
        )
    }
    deletingMany?.let { ids ->
        val messages = history.messages.filter { it.id in ids }
        val allOwn = messages.isNotEmpty() && messages.all { it.isOutgoing } && history.peerKind != TelegramPeerKind.Saved
        AlertDialog(
            onDismissRequest = { deletingMany = null },
            containerColor = Fusion.colors.surface2,
            title = { Text("Удалить ${ids.size} ${messageWord(ids.size)}?", color = Fusion.colors.textPrimary) },
            text = { Text(if (allOwn) "Можно удалить только у себя или у всех участников." else "Сообщения будут удалены у вас.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                Row {
                    if (allOwn) TextButton(onClick = { actions.delete(ids, forEveryone = true); deletingMany = null; selectedIds = emptySet() }) { Text("У всех", color = Fusion.colors.danger) }
                    TextButton(onClick = { actions.delete(ids, forEveryone = false); deletingMany = null; selectedIds = emptySet() }) { Text("У меня", color = Fusion.colors.danger) }
                }
            },
            dismissButton = { TextButton(onClick = { deletingMany = null }) { Text("Отмена", color = Fusion.colors.textPrimary) } }
        )
    }
    forwarding?.let { forwardIds ->
        // «Без автора»: sent as your own message; «Без подписи» also drops captions of photos and videos.
        var asCopy by remember(forwardIds) { mutableStateOf(tweaks.forwardWithoutAuthor) }
        var noCaption by remember(forwardIds) { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = { forwarding = null }, containerColor = Fusion.colors.surface1) {
            Text("Переслать в…", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(asCopy, onClick = { asCopy = !asCopy }, label = { Text("Без автора") })
                FilterChip(noCaption && asCopy, onClick = { noCaption = !noCaption }, enabled = asCopy, label = { Text("Без подписи") })
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(forwardTargets, key = { it.id }) { target ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            actions.forward(target.id, forwardIds, asCopy, noCaption); forwarding = null; selectedIds = emptySet(); notice = "Переслано в «${target.title}»"
                        }.heightIn(min = 60.dp).padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelegramAvatar(target.title, target.photo, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(target.title, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (forwardTargets.isEmpty()) item { Text("Нет доступных чатов", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
            }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
    if (schedulePicking) ScheduleSendDialog(onDismiss = { schedulePicking = false }) { at -> schedulePicking = false; sendScheduled(at) }
    if (scheduledOpen) ModalBottomSheet(onDismissRequest = { scheduledOpen = false }, containerColor = Fusion.colors.surface1) {
        Text("Отложенные сообщения", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            items(scheduled, key = { it.id }) { item ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(if (item.sendAt == 0) "Когда собеседник будет в сети" else scheduledTime(item.sendAt), color = Fusion.colors.accent, fontSize = 13.sp)
                    Text(item.text, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            scope.launch { runCatching { actions.sendScheduledNow(item.id) }.onSuccess { notice = "Отправлено" }.onFailure { notice = "Не удалось отправить" }; scheduledReload++ }
                        }) { Text("Отправить сейчас", color = Fusion.colors.accent) }
                        TextButton(onClick = {
                            scope.launch { runCatching { actions.deleteScheduled(item.id) }.onFailure { notice = "Не удалось удалить" }; scheduledReload++ }
                        }) { Text("Удалить", color = Fusion.colors.danger) }
                    }
                }
            }
            if (scheduled.isEmpty()) item { Text("Отложенных сообщений нет", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
    confirmAction?.let { (question, action) ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            containerColor = Fusion.colors.surface1,
            title = { Text(question, color = Fusion.colors.textPrimary) },
            confirmButton = { TextButton(onClick = { confirmAction = null; action() }) { Text("Да", color = Fusion.colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (menuOpen && menu != null) {
        ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = Fusion.colors.surface1) {
            val items = buildList {
                add(Triple(KiroXIcon.Bell, if (muted == true) "Включить звук" else "Без звука", false))
                add(Triple(KiroXIcon.Search, "Поиск", false))
                if (history.peerKind == TelegramPeerKind.User) add(Triple(KiroXIcon.Refresh, "Автоудаление", false))
                add(Triple(KiroXIcon.Delete, "Очистить историю", true))
                add(Triple(KiroXIcon.Delete, if (history.peerKind == TelegramPeerKind.User || history.peerKind == TelegramPeerKind.Bot) "Удалить чат" else "Покинуть", true))
            }
            items.forEach { (icon, title, danger) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                        menuOpen = false
                        when (title) {
                            "Поиск" -> searching = true
                            "Очистить историю" -> confirmClear = true
                            "Автоудаление" -> autoDeleteOpen = true
                            "Удалить чат", "Покинуть" -> confirmDelete = true
                            else -> {
                                val target = muted != true
                                scope.launch {
                                    runCatching { menu.setMuted(history.chatId, target) }
                                        .onSuccess { notice = if (target) "Уведомления выключены" else "Уведомления включены" }
                                        .onFailure { notice = "Не удалось изменить уведомления" }
                                }
                            }
                        }
                    }.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.kirox.app.ui.icons.KiroXIcon(icon, null, Modifier.size(22.dp), if (danger) Fusion.colors.danger else Fusion.colors.textPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text(title, color = if (danger) Fusion.colors.danger else Fusion.colors.textPrimary, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
    if (autoDeleteOpen && menu != null) AlertDialog(
        onDismissRequest = { autoDeleteOpen = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Автоудаление сообщений", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                listOf(0 to "Выключено", 60 to "1 минута", 3600 to "1 час", 86400 to "1 день", 604800 to "1 неделя").forEach { (seconds, label) ->
                    Text(label, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable {
                        autoDeleteOpen = false
                        scope.launch {
                            runCatching { menu.setAutoDelete(history.chatId, seconds) }
                                .onSuccess { notice = if (seconds == 0) "Автоудаление выключено" else "Новые сообщения удалятся через: $label" }
                                .onFailure { notice = "Не удалось изменить автоудаление" }
                        }
                    }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { autoDeleteOpen = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (confirmClear && menu != null) AlertDialog(
        onDismissRequest = { confirmClear = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Очистить историю?", color = Fusion.colors.textPrimary) },
        text = { Text("Сообщения удалятся только у вас.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = {
                confirmClear = false
                scope.launch { runCatching { menu.clearHistory(history.chatId) }.onFailure { notice = "Не удалось очистить историю" } }
            }) { Text("Очистить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (confirmDelete && menu != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = Fusion.colors.surface1,
        title = { Text(if (history.peerKind == TelegramPeerKind.User || history.peerKind == TelegramPeerKind.Bot) "Удалить чат?" else "Покинуть «${history.title}»?", color = Fusion.colors.textPrimary) },
        text = { Text("Чат пропадёт из списка.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch {
                    runCatching { menu.deleteChat(history.chatId) }.onSuccess { onBack() }.onFailure { notice = "Не удалось удалить чат" }
                }
            }) { Text("Удалить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}


/** «12 нояб., 18:30» — when a scheduled message goes out. */
private fun scheduledTime(unixSeconds: Int): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(unixSeconds * 1000L))

/** «Отправить позже»: a date, then a time; [onPick] gets unix seconds, at least a minute ahead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSendDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val now = remember { java.util.Calendar.getInstance() }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        // Telegram keeps scheduled messages for up to a year.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= System.currentTimeMillis() - 24 * 3600_000L && utcTimeMillis <= System.currentTimeMillis() + 365L * 24 * 3600_000L
        }
    )
    val timeState = rememberTimePickerState(now.get(java.util.Calendar.HOUR_OF_DAY), (now.get(java.util.Calendar.MINUTE) + 5).coerceAtMost(59), is24Hour = true)
    var pickingTime by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if (!pickingTime) DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { pickingTime = true }, enabled = dateState.selectedDateMillis != null) { Text("Далее", color = Fusion.colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    ) { DatePicker(dateState, title = { Text("Когда отправить", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    else AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("Время отправки", color = Fusion.colors.textPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(timeState)
                error?.let { Text(it, color = Fusion.colors.danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // The date picker gives midnight UTC of the chosen day; the time is local.
                val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis() }
                val at = java.util.Calendar.getInstance().apply {
                    set(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH), utc.get(java.util.Calendar.DAY_OF_MONTH), timeState.hour, timeState.minute, 0)
                }.timeInMillis / 1000
                if (at < System.currentTimeMillis() / 1000 + 60) error = "Выберите время хотя бы через минуту"
                else onPick(at.toInt())
            }) { Text("Запланировать", color = Fusion.colors.accent) }
        },
        dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Назад", color = Fusion.colors.textSecondary) } }
    )
}
