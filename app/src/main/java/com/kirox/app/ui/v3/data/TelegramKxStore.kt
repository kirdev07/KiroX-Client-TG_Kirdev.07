package com.kirox.app.ui.v3.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.telegram.ChatActivity
import com.kirox.app.telegram.LastMessageState
import com.kirox.app.telegram.TelegramCall
import com.kirox.app.telegram.TelegramCallKind
import com.kirox.app.telegram.TelegramChatHistory
import com.kirox.app.telegram.TelegramChatItem
import com.kirox.app.telegram.TelegramChatKind
import com.kirox.app.telegram.TelegramChatListKey
import com.kirox.app.telegram.TelegramChatLists
import com.kirox.app.telegram.TelegramConnection
import com.kirox.app.telegram.TelegramContact
import com.kirox.app.telegram.TelegramDelivery
import com.kirox.app.telegram.TelegramEntityType
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramMessage
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramMessageRights
import com.kirox.app.telegram.TelegramPanelItem
import com.kirox.app.telegram.TelegramPeerKind
import com.kirox.app.telegram.TelegramStoryChat
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.telegram.TelegramUserStatus
import com.kirox.app.telegram.TelegramViewModel
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * The real account behind the v3 screens: TelegramViewModel's StateFlows are mapped (on
 * Dispatchers.Default) into KxModels and written to Compose snapshot state on the main thread, so
 * screens recompose. Actions go to the existing repositories; nothing here talks to TDLib directly
 * and no getter blocks (getters run during composition).
 *
 * TDLib details that shape this class:
 * - TelegramMessageRepository holds the history of ONE open chat: [messages] is empty for other chats
 *   until [openChat]. Message actions name their chat (sendTextIn/editIn/…), so they never land in a
 *   different chat that happens to be open.
 * - Folder ids: 0 is the main list («Все»), others are TDLib chatFolderId values.
 * - Files: avatars, photos, stickers and thumbnails download by themselves (small, like the old UI);
 *   videos, voice, music and documents only on [download]. Paths come back through [paths].
 *
 * [startCall] lets the host ask for microphone/camera permissions first; [requestMicrophone] asks
 * for the microphone when a voice recording is refused for lack of it. Call [close] when done.
 */
class TelegramKxStore(
    private val vm: TelegramViewModel,
    private val startCall: (userId: Long, video: Boolean, start: () -> Unit) -> Unit = { _, _, start -> start() },
    private val requestMicrophone: () -> Unit = {},
    /** wave5: asks the user for one runtime permission (storage on Android 7–9 before saving to the gallery). */
    private val requestPermission: (String) -> Unit = {},
) : KxStore {
    private val app: android.app.Application = vm.getApplication()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override var me by mutableStateOf(KxProfile(0, ""))
        private set
    override var folders by mutableStateOf(listOf(KxFolder(0, "Все")))
        private set
    // wave5: hidden chats («Скрытые чаты», per account) leave every list the v3 UI reads — main list, folders, archive,
    // search, forward/share pickers — not only the main list. The Hidden board finds them by header(id).
    private var allChats by mutableStateOf(emptyList<KxChat>())
    private var allArchived by mutableStateOf(emptyList<KxChat>())
    private val hiddenIds: Set<Long> get() = prefTexts[com.kirox.app.ui.v3.model.KxPrefsAccount.HIDDEN_CHATS].orEmpty().split('\n').mapNotNull { it.trim().toLongOrNull() }.toSet()
    private fun List<KxChat>.visible(): List<KxChat> { val h = hiddenIds; return if (h.isEmpty()) this else filter { it.id !in h } }
    override val chats: List<KxChat> get() = allChats.visible()
    override val archivedChats: List<KxChat> get() = allArchived.visible()
    override var stories by mutableStateOf(emptyList<KxStoryPeer>())
        private set
    override var calls by mutableStateOf(emptyList<KxCall>())
        private set
    override var contacts by mutableStateOf(emptyList<KxContact>())
        private set
    override var unreadTotal by mutableStateOf(0)
        private set
    override var missedCalls by mutableStateOf(0)
        private set
    override var connection by mutableStateOf<String?>(null)
        private set
    override var chatsLoading by mutableStateOf(true)
        private set
    override var error by mutableStateOf<String?>(null)
        private set

    private var folderChats by mutableStateOf(emptyMap<Int, List<KxChat>>())
    /** Raw list items by id, for actions and for headers of chats that are not open. */
    @Volatile private var items: Map<Long, TelegramChatItem> = emptyMap() // also read by the off-main mapping
    private var history by mutableStateOf(TelegramChatHistory())
    private var historyMessages by mutableStateOf(emptyList<KxMessage>())
    private var typingLines by mutableStateOf(emptyMap<Long, String>())
    /** Members / subscribers and (isMember, canPost), fetched once per opened group or channel. */
    private val memberCounts = mutableStateMapOf<Long, Int>()
    private val writeAccess = mutableStateMapOf<Long, com.kirox.app.telegram.TelegramWriteAccess>()
    private var onlineMembers by mutableStateOf(emptyMap<Long, Int>())
    /** Reactions allowed per message, fetched on first ask. */
    private val reactionChoices = mutableStateMapOf<Long, List<String>>()
    private val reactionAsked = HashSet<Long>()

    // Inputs of the off-main mapping besides the repositories' own flows.
    /** fileId → local path of files that finished downloading. */
    private val paths = MutableStateFlow<Map<Int, String>>(emptyMap())
    /** Bumped at local midnight. */
    private val day = MutableStateFlow(0)
    /** [paths], re-emitted at midnight: every mapping that formats dates listens to this. */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val pathsDaily = combine(paths.debounce(150), day) { p, _ -> p }
    private val watched = ConcurrentHashMap.newKeySet<Int>()
    /** messageId → what the menu may offer, for the open chat. */
    private val rights = MutableStateFlow<Map<Long, TelegramMessageRights>>(emptyMap())
    private val rightsAsked = HashSet<Long>()
    private var rightsChat = 0L
    /** messageId → translated text shown instead of the original. */
    private val translations = MutableStateFlow<Map<Long, String>>(emptyMap())

    private val requestedFolders = HashSet<Int>()

    /** The old UI's OGG/Opus recorder (ui/chat/VoiceNotes.kt), reused for hold-to-record. */
    private val recorder = com.kirox.app.ui.chat.VoiceRecorder(app)
    private var levelJob: Job? = null
    private var recordingChat: Long? = null

    /** Story contents by (chat, story id), fetched once per story (GetStory). */
    private val storyCache = MutableStateFlow<Map<Pair<Long, Int>, com.kirox.app.telegram.TelegramStory>>(emptyMap())
    private val storyAsked = HashSet<Pair<Long, Int>>()
    private var openStory: Pair<Long, Int>? = null
    private var draftJob: Job? = null
    private val lastDraft = HashMap<Long, String>()
    /** History size when [fillFirstPage] last asked for more, so a failed request is not repeated forever. */
    private var fillAskedAt = -1
    private var chatErrorText: String? = null
    private var messageErrorText: String? = null

    private data class Lists(
        val main: List<KxChat>, val archive: List<KxChat>, val folders: List<KxFolder>,
        val byFolder: Map<Int, List<KxChat>>, val items: Map<Long, TelegramChatItem>, val loading: Boolean,
    )

    init {
        // wave5 (fixer-media): TDLib deleted a file from the phone (cache clean / storage optimizer) → drop the stale path,
        // so avatars/photos download again and save/open/play do not get a missing file.
        scope.launch {
            vm.files.removed.collect { id ->
                if (paths.value.containsKey(id)) paths.update { it - id }
                watched.remove(id)
                mediaBinderOrNull?.forget(id)
            }
        }
        // Chat lists + typing + stories + avatars → KxChat.
        scope.launch {
            combine(vm.chats.lists, vm.activity.linesV3, vm.stories.storyChats, pathsDaily, combine(vm.chats.folderUnread, vm.chats.unreadUnmutedChats) { f, t -> f to t }) { lists, lines, storyChats, p, unread ->
                mapLists(lists, lines, storyChats, p, unread.first, unread.second)
            }
                .flowOn(Dispatchers.Default)
                .collect { l ->
                    items = l.items; allChats = l.main; allArchived = l.archive; folders = l.folders; folderChats = l.byFolder; chatsLoading = l.loading
                    // Folder lists load only when asked: ask once for each folder that appears.
                    l.folders.filter { it.id != 0 && requestedFolders.add(it.id) }.forEach { vm.chats.loadMore(TelegramChatListKey.Folder(it.id)) }
                }
        }
        scope.launch { vm.activity.linesV3.collect { typingLines = it } }
        scope.launch { vm.chats.unreadUnmutedChats.collect { unreadTotal = it } }
        scope.launch { vm.chats.onlineMembers.collect { onlineMembers = it } }
        scope.launch { vm.connection.state.collect { connection = connectionText(it) } }
        scope.launch { vm.chats.error.collect { chatErrorText = it; error = chatErrorText ?: messageErrorText } }
        scope.launch { vm.messages.error.collect { messageErrorText = it; error = messageErrorText ?: chatErrorText } }
        scope.launch {
            combine(vm.users.profile, paths) { p, _ -> p?.toKx() }.flowOn(Dispatchers.Default).collect { if (it != null) me = it }
        }
        scope.launch {
            combine(vm.users.contacts, vm.stories.storyChats, paths) { list, storyChats, _ -> mapContacts(list, storyChats) }
                .flowOn(Dispatchers.Default).collect { contacts = it }
        }
        scope.launch {
            combine(vm.stories.storyChats, pathsDaily, storyCache) { list, _, cache -> list.sortedByDescending { it.isSelf }.map { it.toKx(cache) } }
                .flowOn(Dispatchers.Default).collect { stories = it }
        }
        scope.launch { vm.stories.storyChats.collect { list -> list.forEach { c -> c.storyIds.forEach { fetchStory(c.chatId, it) } } } }
        scope.launch {
            combine(vm.calls.state, pathsDaily) { state, _ -> mapCalls(state.calls) }.flowOn(Dispatchers.Default)
                .collect { (list, missed) -> calls = list; missedCalls = missed }
        }
        scope.launch {
            combine(vm.messages.history, pathsDaily, rights, translations) { h, _, r, t -> h to mapHistory(h, r, t) }
                .flowOn(Dispatchers.Default)
                .collect { (h, mapped) ->
                    history = h; historyMessages = mapped
                    pendingJump?.let { (c, m) -> if (h.chatId == c && h.loaded) { pendingJump = null; vm.messages.jumpTo(m) } }
                    fillFirstPage(h)
                    fetchRights(h)
                }
        }
        scope.launch {
            while (true) {
                val now = java.util.Calendar.getInstance()
                val next = (now.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 1); set(java.util.Calendar.MILLISECOND, 0)
                }
                delay(next.timeInMillis - now.timeInMillis)
                day.update { it + 1 }
            }
        }
        // initRound5() runs from the last init block: its collectors start at once on the main thread
        // (Main.immediate) and write state declared further down, which must be initialised first.
        // The call log is a search over call messages: start it (the old UI does this on its Calls tab).
        vm.calls.loadMore()
    }

    fun close() {
        openStory?.let { (c, id) -> vm.stories.close(c, id) }
        // A recording still running when the host goes away is discarded, never sent.
        if (recorder.isRecording) recorder.stop(discard = true)
        scope.cancel()
    }

    // ---- Reads -------------------------------------------------------------------------------

    override fun clearError() {
        vm.chats.clearError(); vm.messages.clearError()
    }

    override fun chatsIn(folderId: Int): List<KxChat> = if (folderId == 0) chats else folderChats[folderId].orEmpty().visible()

    override fun messages(chatId: Long): List<KxMessage> = if (history.chatId == chatId) historyMessages else emptyList()

    override fun historyLoading(chatId: Long): Boolean = history.chatId != chatId || !history.loaded || history.loadingOlder

    override fun historyComplete(chatId: Long): Boolean = history.chatId == chatId && history.reachedStart

    override fun header(chatId: Long): KxChatHeader {
        val item = items[chatId]
        val typing = typingLines[chatId]?.removeSuffix("…")
        val open = history.takeIf { it.chatId == chatId && it.loaded }
        val kind = when {
            open != null -> when {
                open.peerKind == TelegramPeerKind.Saved -> KxChatKind.Saved
                open.isSecret -> KxChatKind.Secret
                open.peerKind == TelegramPeerKind.Bot -> KxChatKind.Bot
                open.peerKind == TelegramPeerKind.Group -> KxChatKind.Group
                open.peerKind == TelegramPeerKind.Channel -> KxChatKind.Channel
                else -> KxChatKind.Private
            }
            item != null -> item.kxKind()
            else -> KxChatKind.Private
        }
        val members = memberCounts[chatId] ?: 0
        val title = when {
            kind == KxChatKind.Saved -> "Избранное"
            open != null && open.title.isNotEmpty() -> open.threadTitle ?: open.title
            else -> item?.title.orEmpty()
        }
        val online = open?.peerStatus == TelegramUserStatus.Online || item?.isOnline == true
        val muted = item?.isMutedResolved == true
        val status = when (kind) {
            KxChatKind.Saved -> ""
            KxChatKind.Bot -> "бот"
            // Telegram: «8 участников, 3 в сети» (the online part only when more than you).
            KxChatKind.Group -> if (members > 0) KxFormat.members(members) + (onlineMembers[chatId]?.takeIf { it > 1 }?.let { ", $it в сети" } ?: "") else "группа"
            KxChatKind.Channel -> if (members > 0) KxFormat.subscribers(members) else "канал"
            else -> KxFormat.status(open?.peerStatus).ifEmpty { if (online) "в сети" else "" }
        }
        val composer = when (writeAccess[chatId]) {
            null, com.kirox.app.telegram.TelegramWriteAccess.Write -> KxComposer.Write
            com.kirox.app.telegram.TelegramWriteAccess.Join -> KxComposer.Join
            // A channel reader: the bottom button switches notifications.
            com.kirox.app.telegram.TelegramWriteAccess.ReadOnly -> if (muted) KxComposer.Unmute else KxComposer.Mute
            com.kirox.app.telegram.TelegramWriteAccess.Restricted -> KxComposer.Restricted
            com.kirox.app.telegram.TelegramWriteAccess.Blocked -> KxComposer.Blocked
            // KxComposer has no «ЗАПУСТИТЬ» yet (proposal in tg-tasks.md): writing /start works meanwhile.
            com.kirox.app.telegram.TelegramWriteAccess.StartBot -> KxComposer.Write
        }
        val pinned = open?.pinned?.let { KxReply(it.senderName, it.serviceText ?: KxFormat.preview(it.text, it.kind, it.media?.duration ?: 0), it.id) }
        return KxChatHeader(
            chatId, title, status, kind, online = online, typing = typing, muted = muted, members = members,
            pinned = pinned, composer = composer,
            avatar = avatarOf(open?.photo ?: item?.photo, paths.value), colorIndex = item?.accentColorId ?: -1,
            canDeleteForAll = item?.canDeleteForAll == true,
        )
    }

    override fun availableReactions(chatId: Long, messageId: Long): List<String> {
        reactionChoices[messageId]?.let { return it }
        // Asked during composition: answer the default set now, the chat's own set when it arrives.
        if (reactionAsked.add(messageId)) scope.launch {
            vm.messages.availableReactionsIn(chatId, messageId)?.takeIf { it.isNotEmpty() }?.let { reactionChoices[messageId] = it }
        }
        return DEFAULT_REACTIONS
    }

    // ---- Chat list actions -------------------------------------------------------------------

    /** 0 = main list, a TDLib folder id, or a negative id for the archive (the contract has no archive id yet). */
    override fun loadMoreChats(folderId: Int) = vm.chats.loadMore(
        when {
            folderId == 0 -> TelegramChatListKey.Main
            folderId > 0 -> TelegramChatListKey.Folder(folderId)
            else -> TelegramChatListKey.Archive
        }
    )

    override fun openChat(chatId: Long) {
        vm.messages.open(chatId)
        // Shared media and the profile are read again for this chat (new messages since).
        reloadNext("shared:$chatId:"); reloadNext("peer:$chatId")
        fillAskedAt = -1
        // Always asked (0 for private chats): a group opened from search or a link is not in the loaded lists.
        scope.launch { vm.chats.memberCount(chatId).takeIf { it > 0 }?.let { memberCounts[chatId] = it } }
        scope.launch { writeAccess[chatId] = vm.chats.writeAccess(chatId) }
    }

    override fun closeChat(chatId: Long) {
        lastDraft.remove(chatId)
        vm.activity.send(chatId, null)
        vm.messages.close(chatId)
    }

    override fun setPinned(chatId: Long, pinned: Boolean) {
        // Pins are per list; KxStore has no list argument, so the chat's own list (main or archive) is used.
        val key = if (items[chatId]?.archived == true) TelegramChatListKey.Archive else TelegramChatListKey.Main
        vm.chats.setPinned(key, chatId, pinned)
    }

    override fun setPinnedIn(chatId: Long, pinned: Boolean, folderId: Int) {
        if (folderId > 0) vm.chats.setPinned(TelegramChatListKey.Folder(folderId), chatId, pinned) else setPinned(chatId, pinned)
    }

    override fun setMuted(chatId: Long, muted: Boolean) {
        scope.launch { runCatching { vm.chats.setMuted(chatId, muted) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; showError(it) } }
    }

    override fun setArchived(chatId: Long, archived: Boolean) = vm.chats.setArchived(chatId, archived)

    override fun markRead(chatId: Long) = vm.chats.markReadAll(chatId)

    override fun markUnread(chatId: Long) = vm.chats.markUnread(chatId)

    override fun clearHistory(chatId: Long, forEveryone: Boolean) {
        scope.launch { runCatching { vm.chats.clearHistory(chatId, forEveryone) }.onFailure { showError(it) } }
    }

    override fun deleteChat(chatId: Long, forEveryone: Boolean) {
        // Private: delete (forEveryone also for the other person); group/channel: leave, or delete for all as creator.
        scope.launch { runCatching { vm.chats.deleteChat(chatId, forEveryone) }.onFailure { showError(it) } }
    }

    // ---- Messages ----------------------------------------------------------------------------

    override fun send(chatId: Long, text: String, replyTo: Long?) {
        val body = text.trim()
        if (body.isEmpty()) return
        draftJob?.cancel()
        vm.activity.send(chatId, null)
        lastDraft[chatId] = ""
        vm.messages.sendTextIn(chatId, body, replyTo)
        // A sent message clears the draft on Telegram's side as well.
        vm.messages.saveDraft(chatId, "")
    }

    /**
     * Hold-to-record. Voice uses the old UI's recorder (OGG/Opus, as Telegram wants it); round video
     * needs a camera preview the v3 chat does not have yet, so [video] does nothing here.
     */
    override fun recordStart(chatId: Long, video: Boolean) {
        if (video) return
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) { error = "Нужен доступ к микрофону"; requestMicrophone(); return }
        // A recording still running (another chat, a missed release) is thrown away, never sent.
        if (recorder.isRecording) { levelJob?.cancel(); recordingChat?.let { vm.activity.send(it, null) }; recorder.stop(discard = true) }
        if (!recorder.start()) { error = "Не удалось начать запись"; return }
        recordingChat = chatId
        vm.activity.send(chatId, ChatActivity.RecordingVoice)
        // The waveform is built from levels sampled ~10 times a second.
        levelJob?.cancel()
        levelJob = scope.launch {
            var tick = 0
            while (recorder.isRecording) {
                recorder.sampleLevel()
                // Telegram shows a chat action for about 5 s: repeat it every 4 s.
                if (tick % 40 == 0) vm.activity.send(chatId, ChatActivity.RecordingVoice)
                tick++
                delay(100)
            }
        }
    }

    override fun recordStop(chatId: Long, send: Boolean, seconds: Int) {
        levelJob?.cancel(); levelJob = null
        recordingChat = null
        vm.activity.send(chatId, null)
        if (!recorder.isRecording) return
        // Too short (< MIN_DURATION) or cancelled: the recorder deletes the file and returns null.
        val recording = recorder.stop(discard = !send) ?: return
        vm.messages.sendVoiceIn(chatId, recording.file.absolutePath, recording.durationSeconds, recording.waveform)
    }

    override fun sendVoice(chatId: Long, seconds: Int, path: String?, waveform: ByteArray?) {
        if (path != null) vm.messages.sendVoiceIn(chatId, path, seconds, waveform ?: ByteArray(0))
    }

    override fun sendVideoNote(chatId: Long, seconds: Int, path: String?, length: Int) {
        if (path != null) vm.messages.sendVideoNoteIn(chatId, path, seconds, length)
    }

    override fun edit(chatId: Long, messageId: Long, text: String) {
        if (text.isNotBlank()) vm.messages.editIn(chatId, messageId, text.trim())
    }

    override fun delete(chatId: Long, messageIds: List<Long>, forEveryone: Boolean) {
        if (messageIds.isNotEmpty()) vm.messages.deleteIn(chatId, messageIds, forEveryone)
    }

    override fun react(chatId: Long, messageId: Long, emoji: String) = vm.messages.toggleReactionIn(chatId, messageId, reactionKey(emoji))

    /** Telegram's reaction for the red heart is "❤" without U+FE0F; "❤️" would fail with REACTION_INVALID. */
    private fun reactionKey(emoji: String) = if (emoji == "❤️") "❤" else emoji

    override fun pin(chatId: Long, messageId: Long, pinned: Boolean) {
        // TelegramMessageRepository pins in the open chat only.
        if (history.chatId != chatId) return
        if (pinned) vm.messages.pin(messageId) else vm.messages.unpin(messageId)
    }

    override fun forward(fromChatId: Long, messageIds: List<Long>, toChatId: Long) {
        // wave5: explicit source chat, so boards opened outside the chat (or after it closed) forward too.
        if (messageIds.isNotEmpty()) vm.messages.forwardFrom(fromChatId, toChatId, messageIds)
    }

    override fun saveToFavorites(chatId: Long, messageIds: List<Long>) {
        if (messageIds.isEmpty()) return
        scope.launch { runCatching { vm.users.savedMessagesChatId() }.onSuccess { vm.messages.forwardFrom(chatId, it, messageIds) }.onFailure { showError(it) } }
    }

    override fun translate(chatId: Long, messageId: Long, show: Boolean) {
        if (!show) { translations.update { it - messageId }; return }
        if (history.chatId != chatId) return
        scope.launch {
            val text = vm.messages.translate(messageId, "ru")
            if (text != null) translations.update { it + (messageId to text) } else error = "Не удалось перевести"
        }
    }

    override fun retrySend(chatId: Long, messageId: Long) = vm.messages.resend(chatId, listOf(messageId))

    override fun loadOlder(chatId: Long) {
        if (history.chatId == chatId) vm.messages.loadOlder()
    }

    // loadNewer: TelegramMessageRepository always holds the history down to the newest message
    // (jumps load older pages until the target), so there is nothing newer to load.

    /**
     * Saved to Telegram (synced draft) half a second after typing stops, so each keystroke is not a
     * request; also tells the chat «печатает…» (throttled by TelegramChatActionRepository, which also
     * respects kiroX's ghost-typing setting).
     */
    override fun setDraft(chatId: Long, text: String) {
        val previous = lastDraft[chatId]
        lastDraft[chatId] = text
        // First call after opening with the draft Telegram already has: the UI restoring it, not typing.
        if (previous == null && history.chatId == chatId && text == history.draft) return
        if (previous == text) return
        vm.activity.send(chatId, if (text.isNotBlank()) ChatActivity.Typing else null)
        draftJob?.cancel()
        draftJob = scope.launch { delay(DRAFT_DELAY_MS); vm.messages.saveDraft(chatId, text) }
    }

    override fun download(fileId: Int) {
        if (fileId == 0) return
        val ref = TelegramFileRef(fileId, null)
        vm.files.download(ref)
        watchFile(ref)
    }

    // ---- Stories -----------------------------------------------------------------------------

    /** Opens the story for TDLib (marks it viewed unless ghost mode) and downloads its video and the next one. */
    override fun viewStory(peerId: Long, storyId: Int) {
        val key = peerId to storyId
        if (openStory != key) {
            openStory?.let { (c, id) -> vm.stories.close(c, id) }
            openStory = key
            vm.stories.open(peerId, storyId)
        }
        val ids = vm.stories.storyChats.value.firstOrNull { it.chatId == peerId }?.storyIds.orEmpty()
        val next = ids.getOrNull(ids.indexOf(storyId) + 1)
        // wave5: the next story's video is fetched ahead only when «Автозагрузка медиа» allows story preload on this network.
        listOfNotNull(storyId, next).forEach { id ->
            fetchStory(peerId, id)
            if (id == storyId || vm.autoDownload.rule.value.preloadStories) storyCache.value[peerId to id]?.video?.let { ref -> vm.files.download(ref); watchFile(ref) }
        }
    }

    /** Toggles like message reactions: choosing the current one removes it. */
    override fun reactStory(peerId: Long, storyId: Int, emoji: String) {
        val key = peerId to storyId
        val chosen = reactionKey(emoji)
        val remove = storyCache.value[key]?.chosenReaction == chosen
        scope.launch {
            runCatching { vm.stories.react(peerId, storyId, if (remove) null else chosen) }
                .onSuccess { storyCache.update { m -> m[key]?.let { m + (key to it.copy(chosenReaction = if (remove) null else chosen)) } ?: m } }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; showError(it) }
        }
    }

    override fun replyStory(peerId: Long, storyId: Int, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        scope.launch {
            runCatching { vm.stories.reply(peerId, storyId, body) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; showError(it) }
        }
    }

    // ---- Calls -------------------------------------------------------------------------------

    /** [peerId] is a user id (a private chat's id equals its user's id in TDLib). */
    override fun call(peerId: Long, video: Boolean) = startCall(peerId, video) {
        vm.groupCalls.leave()
        vm.voip.start(peerId, video && granted(android.Manifest.permission.CAMERA)) // camera refused → voice call
    }

    // ---- Files -------------------------------------------------------------------------------

    /** Local path of a small file (avatar, photo, sticker, thumbnail), starting its download once. */
    private fun autoPath(ref: TelegramFileRef?, known: Map<Int, String>): String? {
        ref ?: return null
        ref.localPath?.let { return it }
        known[ref.id]?.let { return it }
        // One collector per file until its path is known, then it ends.
        if (watched.add(ref.id)) scope.launch { val p = vm.files.observe(ref).first { it != null }!!; paths.update { it + (ref.id to p) } }
        return null
    }

    private fun avatarOf(ref: TelegramFileRef?, known: Map<Int, String>) = autoPath(ref, known)

    /** Path of a big file once it is on disk, without downloading it (see [download]). */
    private fun knownPath(ref: TelegramFileRef?, known: Map<Int, String>): String? {
        ref ?: return null
        ref.localPath?.let { return it }
        known[ref.id]?.let { return it }
        watchFile(ref)
        return null
    }

    private fun watchFile(ref: TelegramFileRef) {
        if (watched.add(ref.id)) scope.launch { val p = vm.files.state(ref).first { it.path != null }.path!!; paths.update { it + (ref.id to p) } }
    }

    private fun showError(t: Throwable) {
        // TDLib errors in human Russian (FLOOD_WAIT, rights…), as the old UI shows them.
        error = (t as? com.kirox.app.telegram.TdException)?.let { com.kirox.app.telegram.humanError(it.error) } ?: "Не получилось"
    }

    // ---- Mapping -----------------------------------------------------------------------------

    /**
     * TDLib's first GetChatHistory page is often what is cached locally, sometimes a single message.
     * Ask for more until a screenful (or the start of the chat), once per history size.
     */
    private fun fillFirstPage(h: TelegramChatHistory) {
        if (!h.loaded || h.chatId == 0L || h.reachedStart || h.loadingOlder) return
        val size = h.messages.size
        if (size >= FIRST_PAGE || size == fillAskedAt) return
        fillAskedAt = size
        vm.messages.loadOlder()
    }

    /** GetMessageProperties for newly loaded messages of the open chat (local in TDLib, cheap). */
    private fun fetchRights(h: TelegramChatHistory) {
        if (h.chatId != rightsChat) { rightsChat = h.chatId; rightsAsked.clear(); rights.value = emptyMap() }
        if (h.chatId == 0L) return
        val ids = h.messages.filter { !it.deleted && rightsAsked.add(it.id) }.map { it.id }
        if (ids.isEmpty()) return
        val chatId = h.chatId
        scope.launch {
            val found = ids.mapNotNull { id -> vm.messages.rights(chatId, id)?.let { id to it } }
            if (chatId == rightsChat && found.isNotEmpty()) rights.update { it + found }
        }
    }

    private fun mapLists(lists: TelegramChatLists, lines: Map<Long, String>, storyChats: List<TelegramStoryChat>, known: Map<Int, String>, folderUnread: Map<Int, Int>, unreadTotal: Int): Lists {
        val withStory = storyChats.filter { !it.isSelf && it.storyIds.isNotEmpty() }.map { it.chatId }.toHashSet()
        fun map(list: List<TelegramChatItem>) = list.map { it.toKx(lines[it.id], it.id in withStory, known) }
        val main = map(lists.main.chats)
        val byFolder = lists.folders.associate { it.id to map(lists.folderChats[it.id]?.chats.orEmpty()) }
        fun unread(list: List<KxChat>) = list.count { (it.unread > 0 || it.markedUnread) && !it.muted }
        // Folder badges: Telegram's own counter (UpdateUnreadChatCount), not only the loaded rows.
        val folders = listOf(KxFolder(0, "Все", unreadTotal, chatCount = main.size)) + lists.folders.map {
            val rows = byFolder[it.id].orEmpty()
            KxFolder(it.id, it.title, folderUnread[it.id] ?: unread(rows), icon = it.icon, chatCount = rows.size)
        }
        val items = HashMap<Long, TelegramChatItem>()
        (lists.archive.chats + lists.folderChats.values.flatMap { it.chats } + lists.main.chats).forEach { items[it.id] = it }
        return Lists(main, map(lists.archive.chats), folders, byFolder, items, loading = lists.main.chats.isEmpty() && !lists.main.complete)
    }

    private fun TelegramChatItem.kxKind(): KxChatKind = when {
        isSaved -> KxChatKind.Saved
        isSecret -> KxChatKind.Secret
        isBot -> KxChatKind.Bot
        kind == TelegramChatKind.Group -> KxChatKind.Group
        kind == TelegramChatKind.Channel -> KxChatKind.Channel
        else -> KxChatKind.Private
    }

    private fun TelegramChatItem.toKx(typing: String?, hasStory: Boolean, known: Map<Int, String>) = KxChat(
        id = id,
        title = title,
        kind = kxKind(),
        preview = lastServiceText ?: KxFormat.preview(preview, lastKind, lastDuration, lastPollQuestion),
        previewSender = lastSender,
        time = KxFormat.chatTime(timestamp),
        unread = unreadCount,
        unreadMention = unreadMentionCount > 0,
        unreadReaction = unreadReactionCount > 0,
        markedUnread = isMarkedUnread && unreadCount == 0,
        muted = isMutedResolved,
        pinned = isPinned,
        online = isOnline,
        verified = isVerified,
        premium = isPremium,
        scam = isScam,
        fake = isFake,
        typing = typing?.removeSuffix("…"),
        out = when (lastDelivery) {
            TelegramDelivery.Sending -> KxOutState.Sending
            TelegramDelivery.Failed -> KxOutState.Failed
            TelegramDelivery.Read -> KxOutState.Read
            TelegramDelivery.Sent -> KxOutState.Sent
            // Items built without lastDelivery (search results) still carry outgoingState.
            TelegramDelivery.None -> when (outgoingState) { LastMessageState.Read -> KxOutState.Read; LastMessageState.Sent -> KxOutState.Sent; else -> KxOutState.None }
        },
        hasStory = hasStory,
        archived = archived,
        draft = draft,
        avatar = avatarOf(photo, known),
        colorIndex = accentColorId,
        hasScheduled = hasScheduled,
        forum = isForum,
        canDeleteForAll = canDeleteForAll,
    )

    private fun mapHistory(h: TelegramChatHistory, rights: Map<Long, TelegramMessageRights>, translations: Map<Long, String>): List<KxMessage> {
        if (!h.loaded || h.chatId == 0L) return emptyList()
        val known = paths.value
        val group = h.peerKind == TelegramPeerKind.Group
        val privateLike = h.peerKind == TelegramPeerKind.User || h.peerKind == TelegramPeerKind.Bot || h.peerKind == TelegramPeerKind.Saved
        var previousDate = 0
        return h.messages.map { m ->
            val header = if (previousDate == 0 || !KxFormat.sameDay(previousDate, m.date)) KxFormat.dayHeader(m.date) else null
            previousDate = m.date
            m.toKx(group, privateLike, header, rights[m.id], translations[m.id], m.id in h.pinnedIds, known)
        }
    }

    private fun TelegramMessage.toKx(
        group: Boolean, privateLike: Boolean, dayHeader: String?, r: TelegramMessageRights?, translation: String?, pinned: Boolean, known: Map<Int, String>,
    ): KxMessage {
        val service = serviceText
        val msgKind = when {
            service != null -> KxMsgKind.Service
            poll != null -> KxMsgKind.Poll
            kind == TelegramMessageKind.Photo -> KxMsgKind.Photo
            kind == TelegramMessageKind.Video -> KxMsgKind.Video
            kind == TelegramMessageKind.Animation -> KxMsgKind.Gif
            kind == TelegramMessageKind.Voice -> KxMsgKind.Voice
            kind == TelegramMessageKind.VideoNote -> KxMsgKind.VideoNote
            kind == TelegramMessageKind.Sticker -> KxMsgKind.Sticker
            kind == TelegramMessageKind.Audio -> KxMsgKind.Music
            kind == TelegramMessageKind.Document -> KxMsgKind.File
            // telegram renders these two as text with a marker (TelegramMessageRepository.toModel).
            text.startsWith("📍 Геопозиция") -> KxMsgKind.Location
            kind == TelegramMessageKind.Other && text.startsWith("👤 ") -> KxMsgKind.Contact
            // Slot machine dice are told in text by telegram; other unknown contents get messagePreview's «Сообщение».
            kind == TelegramMessageKind.Other && text == "Сообщение" -> KxMsgKind.Unsupported
            else -> KxMsgKind.Text
        }
        val media = media
        // Photos and voice notes keep their file in `photo` (TelegramMessage.photo), other contents in media.file.
        val mainFile = when (msgKind) { KxMsgKind.Photo, KxMsgKind.Voice -> photo; else -> media?.file }
        // wave5 (fixer-media): «Автозагрузка медиа» of the current network (AutoDownloadPolicy, as the old chat does):
        // photos only when allowed, GIFs up to 8 MB when autoplay is on, files/music up to the network's file limit.
        val rule = vm.autoDownload.rule.value
        val size = media?.size ?: 0L
        val autoDownload = when (msgKind) {
            KxMsgKind.Photo -> rule.photos
            KxMsgKind.Sticker -> true
            KxMsgKind.Gif -> rule.gifs && size in 1..GIF_AUTO_BYTES
            KxMsgKind.File, KxMsgKind.Music -> size in 1..rule.maxOtherBytes
            KxMsgKind.VideoNote -> size in 1..rule.maxVideoBytes
            else -> false
        }
        val aspect = when {
            photoAspect > 0f -> photoAspect
            media != null && media.width > 0 && media.height > 0 -> media.width.toFloat() / media.height
            else -> 0f
        }
        return KxMessage(
            id = id,
            out = isOutgoing,
            text = when {
                service != null -> service
                msgKind == KxMsgKind.Voice -> "" // telegram puts the length into text; it goes to meta here
                msgKind == KxMsgKind.Poll -> poll?.question.orEmpty()
                msgKind == KxMsgKind.Unsupported -> "Сообщение не поддерживается в kiroX"
                else -> text
            },
            time = KxFormat.messageTime(date),
            kind = msgKind,
            status = when (delivery) {
                TelegramDelivery.Sending -> KxOutState.Sending
                TelegramDelivery.Failed -> KxOutState.Failed
                TelegramDelivery.Read -> KxOutState.Read
                TelegramDelivery.Sent -> KxOutState.Sent
                TelegramDelivery.None -> KxOutState.None
            },
            sender = if (group && !isOutgoing && service == null) senderName else null,
            senderId = senderId,
            reply = replyText?.let { KxReply(replyAuthor.orEmpty(), it, replyToMessageId) },
            reactions = reactions.map { (emoji, count, mine) -> KxReaction(emoji, count, mine) },
            entities = if (service != null || msgKind == KxMsgKind.Voice) emptyList() else entities.mapNotNull { e ->
                val type = when (e.type) {
                    TelegramEntityType.Bold -> KxEntityType.Bold
                    TelegramEntityType.Italic -> KxEntityType.Italic
                    TelegramEntityType.Underline -> KxEntityType.Underline
                    TelegramEntityType.Strike -> KxEntityType.Strike
                    TelegramEntityType.Code -> KxEntityType.Code
                    TelegramEntityType.Pre -> KxEntityType.Pre
                    TelegramEntityType.Spoiler -> KxEntityType.Spoiler
                    TelegramEntityType.Url, TelegramEntityType.Email, TelegramEntityType.Phone -> KxEntityType.Link
                    TelegramEntityType.TextUrl -> KxEntityType.TextLink
                    TelegramEntityType.Mention, TelegramEntityType.MentionName, TelegramEntityType.BotCommand -> KxEntityType.Mention
                    TelegramEntityType.Hashtag -> KxEntityType.Hashtag
                    TelegramEntityType.CustomEmoji -> KxEntityType.CustomEmoji
                    TelegramEntityType.Quote -> null
                }
                type?.let { KxEntity(e.offset, e.length, it, e.url) }
            },
            edited = edited,
            forwardedFrom = forwardedFrom,
            meta = when (msgKind) {
                KxMsgKind.Voice, KxMsgKind.VideoNote, KxMsgKind.Video, KxMsgKind.Gif, KxMsgKind.Music -> media?.duration?.takeIf { it > 0 }?.let(KxFormat::duration).orEmpty()
                KxMsgKind.File -> KxFormat.fileSize(media?.size ?: 0)
                else -> ""
            },
            fileName = fileName ?: media?.title.orEmpty(),
            // Telegram sends voice loudness as 5-bit values (0..31).
            waveform = media?.waveform.orEmpty().map { it.coerceIn(0, 31) / 31f },
            dayHeader = dayHeader,
            // View-once media is not fetched for the bubble (only when the user opens it).
            media = if (selfDestruct) null else if (autoDownload) autoPath(mainFile, known) else knownPath(mainFile, known),
            thumb = autoPath(thumbnail, known),
            aspect = aspect,
            fileId = mainFile?.id ?: 0,
            albumId = albumId,
            views = viewCount?.let(KxFormat::compactCount).orEmpty(),
            comments = commentCount ?: 0,
            buttons = buttons.map { row -> row.map { it.text } },
            canEdit = r?.canEdit ?: canEdit,
            // Until GetMessageProperties answers (a moment): no «Удалить у всех», and «только у меня» only
            // where it exists (private chats; in supergroups and channels every delete is for all).
            canDeleteForAll = r?.canDeleteForAll ?: false,
            canDeleteForMe = r?.canDeleteForMe ?: privateLike,
            canForward = r?.canForward ?: !isProtected,
            canCopy = r?.canCopy ?: !isProtected,
            canPin = r?.canPin ?: false,
            pinned = pinned,
            translation = translation,
            poll = poll?.let { KxExtrasMapping.poll(it) },
            spoiler = hasSpoiler,
            viewOnce = selfDestruct,
        )
    }

    private fun mapCalls(list: List<TelegramCall>): Pair<List<KxCall>, Int> {
        val known = paths.value
        // Telegram groups consecutive calls with the same person, direction and day into one row «(3)».
        val rows = ArrayList<KxCall>()
        var lastDate = 0
        for (c in list) { // newest first
            val dir = when (c.kind) {
                TelegramCallKind.Incoming -> KxCallDir.Incoming
                TelegramCallKind.Outgoing -> KxCallDir.Outgoing
                TelegramCallKind.Missed -> KxCallDir.Missed
                TelegramCallKind.Unanswered -> KxCallDir.Declined
            }
            val prev = rows.lastOrNull()
            if (prev != null && prev.peerId == c.chatId && prev.dir == dir && prev.video == c.isVideo && KxFormat.sameDay(lastDate, c.date)) {
                rows[rows.lastIndex] = prev.copy(count = prev.count + 1)
            } else {
                rows += KxCall(c.messageId, c.chatId, c.peerName, KxFormat.chatTime(c.date), dir, c.isVideo, KxFormat.callDuration(c.durationSeconds),
                    avatar = avatarOf(c.photo, known), colorIndex = items[c.chatId]?.accentColorId ?: -1)
                lastDate = c.date
            }
        }
        // TDLib has no "seen" mark for calls: the dock badge counts missed calls of the last 24 hours.
        val since = (System.currentTimeMillis() / 1000).toInt() - 86_400
        return rows to list.count { it.kind == TelegramCallKind.Missed && it.date >= since }
    }

    private fun mapContacts(list: List<TelegramContact>, storyChats: List<TelegramStoryChat>): List<KxContact> {
        val known = paths.value
        val withStory = storyChats.filter { !it.isSelf }.map { it.chatId }.toHashSet()
        return list.sortedWith(compareByDescending<TelegramContact> { seenRank(it.status) }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .map { KxContact(it.id, it.name, KxFormat.status(it.status), it.status == TelegramUserStatus.Online, it.id in withStory, avatar = avatarOf(it.photo, known), colorIndex = items[it.id]?.accentColorId ?: -1) }
    }

    /** Higher = seen more recently: online, then exact last-seen time, then recently / week / month / long ago. */
    private fun seenRank(status: TelegramUserStatus): Long = when (status) {
        TelegramUserStatus.Online -> Long.MAX_VALUE
        is TelegramUserStatus.Offline -> 10L + status.wasOnline
        TelegramUserStatus.Recently -> 4
        TelegramUserStatus.LastWeek -> 3
        TelegramUserStatus.LastMonth -> 2
        TelegramUserStatus.LongAgo -> 1
    }

    private fun TelegramStoryChat.toKx(cache: Map<Pair<Long, Int>, com.kirox.app.telegram.TelegramStory>): KxStoryPeer {
        val known = paths.value
        return KxStoryPeer(chatId, if (isSelf) "Вы" else title, unseen = hasUnread, mine = isSelf,
            avatar = avatarOf(photo, known), colorIndex = items[chatId]?.accentColorId ?: -1,
            time = KxFormat.ago(newestDate),
            // Not fetched yet: an empty slide keeps the count (the viewer shows its placeholder look).
            slides = storyIds.map { id -> cache[chatId to id]?.let { st ->
                val image = autoPath(st.image, known)
                // A video plays once its file is on disk (viewStory downloads it); until then the cover is shown as a photo.
                val video = if (st.isVideo) knownPath(st.video, known) else null
                KxStorySlide(id, st.caption, media = video ?: image, video = video != null)
            } ?: KxStorySlide(id) })
    }

    private val storyFailures = HashMap<Pair<Long, Int>, Int>()
    private fun fetchStory(chatId: Long, storyId: Int) {
        val key = chatId to storyId
        if (!storyAsked.add(key)) return
        scope.launch {
            val story = vm.stories.story(chatId, storyId)
            if (story != null) { storyFailures.remove(key); storyCache.update { it + (key to story) }; return@launch }
            // Gone (expired / deleted) or offline: two more tries, 10 s and 60 s later, then the slide stays empty.
            val failures = (storyFailures[key] ?: 0) + 1
            storyFailures[key] = failures
            if (failures <= 2) { delay(if (failures == 1) 10_000L else 60_000L); storyAsked.remove(key); fetchStory(chatId, storyId) }
        }
    }

    override fun closeStory() {
        openStory?.let { (c, id) -> vm.stories.close(c, id) }
        openStory = null
    }

    private fun TelegramUserProfile.toKx() = KxProfile(
        id = id, name = name, username = username.orEmpty(),
        phone = if (phoneNumber.isBlank()) "" else runCatching { com.kirox.app.phone.PhoneNumberEngine().formatInternational(phoneNumber) }.getOrDefault("+" + phoneNumber.trimStart('+')),
        bio = bio, status = "в сети", premium = isPremium, avatar = avatarOf(photo, paths.value), birthday = birthday,
    )

    private fun connectionText(state: TelegramConnection): String? = when (state) {
        TelegramConnection.WaitingForNetwork -> "Ожидание сети…"
        TelegramConnection.ConnectingToProxy -> "Подключение к прокси…"
        TelegramConnection.Connecting -> "Соединение…"
        TelegramConnection.Updating -> "Обновление…"
        TelegramConnection.Ready -> null
    }


    // ---- Round 5: settings, accounts, search, chat tools ------------------------------------

    private val services get() = (app as com.kirox.app.KiroXApplication).telegram

    /** Runs a TDLib-backed action in the store scope; a failure goes to [error]. */
    private fun act(block: suspend () -> Unit) { scope.launch { attempt(block)?.let { error = it } } }

    /**
     * Loads [key] once, the first time a getter needs it (getters run during composition). A failed
     * load is retried no sooner than 5 s, then 30 s later — not on every recomposition while offline.
     */
    private val loaded = HashSet<String>()
    private val retryAt = HashMap<String, Pair<Long, Int>>() // key → (earliest retry ms, failures)
    private fun loadOnce(key: String, block: suspend () -> Unit) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (key in loaded || (retryAt[key]?.first ?: 0L) > now) return
        loaded += key
        scope.launch {
            if (attempt(block) == null) { retryAt.remove(key); return@launch }
            val failures = (retryAt[key]?.second ?: 0) + 1
            retryAt[key] = (android.os.SystemClock.elapsedRealtime() + if (failures == 1) 5_000L else 30_000L) to failures
            loaded.remove(key)
        }
    }
    /** Forgets a loaded key so the next read loads it again (no back-off). */
    private fun reloadNext(prefix: String) { loaded.removeAll { it.startsWith(prefix) }; retryAt.keys.removeAll { it.startsWith(prefix) } }

    // Devices
    private var sessionList by mutableStateOf(emptyList<KxSession>())
    private var ttlDays by mutableStateOf(180)
    override val sessions: List<KxSession> get() { loadOnce("sessions") { reloadSessions() }; return sessionList }
    override val sessionTtlDays: Int get() { loadOnce("sessions") { reloadSessions() }; return ttlDays }
    private suspend fun reloadSessions() {
        sessionList = services.sessions.sessions().sortedByDescending { it.isCurrent }.map { KxExtrasMapping.session(it) }
        ttlDays = services.account.inactiveSessionDays().takeIf { it > 0 } ?: ttlDays
    }
    override fun refreshSessions() { scope.launch { attempt { reloadSessions() }?.let { error = it } } }
    override fun terminateSession(id: Long) = act { services.sessions.terminate(id); reloadSessions() }
    override fun terminateOtherSessions() = act { services.sessions.terminateOthers(); reloadSessions() }
    override fun setSessionTtl(days: Int) = act { services.account.setInactiveSessionDays(days); ttlDays = days }
    override suspend fun confirmQrLogin(link: String): String? = attempt { services.account.confirmQrLogin(link); reloadSessions() }

    // Privacy & security
    private var privacyRules by mutableStateOf(emptyList<KxPrivacyRule>())
    override val privacy: List<KxPrivacyRule> get() { loadOnce("privacy") { reloadPrivacy() }; return privacyRules }
    private suspend fun reloadPrivacy() {
        privacyRules = com.kirox.app.telegram.PrivacyItem.entries.mapNotNull { item ->
            orNull { services.settings.privacy(item) }?.let { st ->
                KxPrivacyRule(item.name, item.title, KxExtrasMapping.level(st.level), st.exceptions, item.levels.map { KxExtrasMapping.level(it) })
            }
        }
    }
    override fun setPrivacy(key: String, level: Int) {
        val item = com.kirox.app.telegram.PrivacyItem.entries.firstOrNull { it.name == key } ?: return
        privacyRules = privacyRules.map { if (it.key == key) it.copy(level = level) else it }
        // Only the base rule changes; TelegramSettingsRepository keeps the exceptions in front.
        act { services.settings.setPrivacy(item, KxExtrasMapping.level(level)); reloadPrivacy() }
    }
    private var readDate by mutableStateOf(true)
    private var archiveNew by mutableStateOf(false)
    private suspend fun reloadMessagePrivacy() {
        val p = services.settings.messagePrivacy()
        p.showReadDate?.let { readDate = it }
        p.archiveNewChats?.let { archiveNew = it }
    }
    override val showReadDate: Boolean get() { loadOnce("msgPrivacy") { reloadMessagePrivacy() }; return readDate }
    override fun setShowReadDate(on: Boolean) { readDate = on; act { services.settings.setShowReadDate(on) } }
    override val archiveNewChats: Boolean get() { loadOnce("msgPrivacy") { reloadMessagePrivacy() }; return archiveNew }
    override fun setArchiveNewChats(on: Boolean) { archiveNew = on; act { services.settings.setArchiveNewChats(on) } }
    private var blockedList by mutableStateOf(emptyList<KxContact>())
    private var blockedTotal by mutableStateOf(0)
    private suspend fun reloadBlocked() {
        val list = services.settings.blocked()
        blockedList = list.map { KxContact(it.id, it.name, if (it.isChat) "чат" else "", avatar = avatarOf(it.photo, paths.value)) }
        blockedTotal = orNull { services.settings.blockedCount() } ?: list.size
    }
    override val blockedCount: Int get() { loadOnce("blocked") { reloadBlocked() }; return blockedTotal }
    override val blocked: List<KxContact> get() { loadOnce("blocked") { reloadBlocked() }; return blockedList }
    override fun block(userId: Long, blocked: Boolean) = act { vm.users.setBlocked(userId, blocked); reloadBlocked() }
    private var passwordState by mutableStateOf<KxCloudPassword?>(null)
    private suspend fun reloadPassword() {
        val st = services.settings.passwordState()
        passwordState = KxCloudPassword(st.hasPassword, st.hint, st.hasRecoveryEmail, st.pendingEmailPattern)
    }
    override val cloudPassword: KxCloudPassword? get() { loadOnce("password") { reloadPassword() }; return passwordState }
    override suspend fun checkPassword(password: String): String? =
        attempt { services.settings.verifyPassword(password) }?.let { if (it.contains("PASSWORD_HASH_INVALID", true)) "Неверный пароль" else it }
    override suspend fun setCloudPassword(old: String, new: String, hint: String, email: String?): String? =
        attempt { services.settings.setPassword(old, new, hint, email?.takeIf { it.isNotBlank() }); reloadPassword() }
    override suspend fun confirmRecoveryEmail(code: String): String? = attempt { services.settings.confirmRecoveryEmail(code.trim()); reloadPassword() }

    // Notifications
    private var scopes by mutableStateOf(emptyList<KxNotifyScope>())
    private suspend fun reloadScopes() {
        scopes = com.kirox.app.telegram.NotifyScope.entries.mapNotNull { sc ->
            orNull { services.settings.notifications(sc) }?.let { KxNotifyScope(KxExtrasMapping.scopeKey(sc), sc.title, it.enabled, it.showPreview, it.exceptions) }
        }
    }
    override val notifyScopes: List<KxNotifyScope> get() { loadOnce("notify") { reloadScopes() }; return scopes }
    override fun setNotifyScope(key: String, enabled: Boolean, preview: Boolean) {
        val sc = KxExtrasMapping.scope(key) ?: return
        scopes = scopes.map { if (it.key == key) it.copy(enabled = enabled, preview = preview) else it }
        act {
            val current = services.settings.notifications(sc)
            services.settings.setNotifications(sc, current.copy(enabled = enabled, showPreview = preview))
        }
    }

    // Storage: the fast totals first, then the slow breakdown by type and chat.
    private var storageState by mutableStateOf<KxStorage?>(null)
    override val storage: KxStorage? get() { loadOnce("storage") { reloadStorage() }; return storageState }
    private suspend fun reloadStorage() {
        val fast = services.settings.storage()
        storageState = KxStorage(fast.filesBytes, fast.databaseBytes, loadingDetails = true)
        val full = services.settings.storageBreakdown()
        storageState = KxStorage(
            full.filesBytes, full.databaseBytes,
            full.byCategory.entries.filter { it.value > 0 }.sortedByDescending { it.value }.map { it.key.title to it.value },
            full.chats.sortedByDescending { it.bytes }.map { it.title.ifBlank { "Без чата" } to it.bytes },
        )
    }
    override fun refreshStorage() { reloadNext("storage"); loadOnce("storage") { reloadStorage() } }
    override fun clearCache(types: Set<String>) = act {
        if (types.isEmpty()) services.settings.clearCache()
        else services.settings.clearStorage(types.mapNotNull { KxExtrasMapping.storageCategory(it) }.toSet())
        reloadStorage()
    }
    override val keepMediaOptions: List<String> get() = com.kirox.app.telegram.KeepMedia.entries.map { it.title }
    private var keepMediaIndex by mutableStateOf(-1)
    override val keepMedia: Int get() {
        if (keepMediaIndex < 0) keepMediaIndex = KxExtrasMapping.keepMediaIndex(services.storage.keepMedia.value)
        return keepMediaIndex
    }
    override fun setKeepMedia(index: Int) {
        val value = com.kirox.app.telegram.KeepMedia.entries.getOrNull(index) ?: return
        keepMediaIndex = index
        services.storage.setKeepMedia(value)
    }

    // kiroX tweaks: the same KiroxTweaks the repositories read (ghost mode keeps working).
    private var tweakState by mutableStateOf(KxExtrasMapping.tweaks(com.kirox.app.settings.KiroxTweaks.current))
    override val tweaks: Map<String, Boolean> get() = tweakState
    override fun setTweak(key: String, on: Boolean) {
        val setter = KxExtrasMapping.tweakKeys[key]?.second ?: return
        val updated = setter(com.kirox.app.settings.KiroxTweaks.current, on)
        com.kirox.app.settings.KiroxTweaks.update(updated)
        tweakState = KxExtrasMapping.tweaks(updated)
    }

    // Accounts: the slots kiroX keeps on this phone; the current one first.
    private var accountSlots by mutableStateOf(emptyList<com.kirox.app.telegram.TelegramAccount>())
    override val accounts: List<KxProfile> get() {
        val active = services.client.activeSession
        val ordered = accountSlots.sortedByDescending { it.session == active }
        if (ordered.isEmpty()) return listOf(me)
        // Other slots: a stable id from the stored phone (digits), else from the slot position.
        return ordered.mapIndexed { i, a -> if (a.session == active) me else KxProfile(a.phone.filter(Char::isDigit).toLongOrNull()?.let { -it } ?: -(i + 1L), a.title.ifBlank { a.phone }, phone = a.phone, status = "") }
    }
    override fun switchAccount(index: Int) {
        val active = services.client.activeSession
        val target = accountSlots.sortedByDescending { it.session == active }.getOrNull(index) ?: return
        if (target.session != active) services.client.switchTo(target.session)
    }
    override fun addAccount() = services.client.addAccount()
    override fun logout() = vm.auth.logOut()
    override fun logoutClearingCache() {
        scope.launch {
            try { services.settings.clearCache() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { /* logging out matters more */ }
            vm.auth.logOut()
        }
    }

    // Premium
    private var stars by mutableStateOf<Long?>(null)
    private var gifts by mutableStateOf(emptyList<KxGift>())
    override val starsBalance: Long? get() { loadOnce("stars") { stars = vm.users.stars() }; return stars }
    override val giftCatalog: List<KxGift> get() {
        loadOnce("gifts") { gifts = vm.users.giftCatalog().map { KxGift(it.id, it.emoji, it.stars, it.soldOut, autoPath(it.sticker, paths.value)) } }
        return gifts
    }
    override suspend fun sendGift(userId: Long, giftId: Long, text: String, private: Boolean): String? =
        attempt { vm.users.sendGift(giftId, userId, text, private); stars = vm.users.stars() }

    // Folders (icon on create: TelegramChatRepository.createFolder takes no icon yet, Telegram picks one)
    override fun createFolder(title: String, icon: String, chatIds: List<Long>) = act { vm.chats.createFolder(title.trim(), chatIds, icon) }
    override fun deleteFolder(id: Int) = act { vm.chats.deleteFolder(id) }
    override fun reorderFolders(ids: List<Int>) = act { vm.chats.reorderFolders(ids.filter { it > 0 }) }

    // Profile & contacts
    override suspend fun checkUsername(name: String): KxUsernameState =
        orNull { KxExtrasMapping.username(vm.users.checkUsername(name.trim())) } ?: KxUsernameState.Invalid
    override suspend fun setUsername(name: String): String? {
        val old = vm.users.profile.value ?: return "Профиль ещё не загружен"
        return attempt { vm.users.updateProfile(old.firstName, old.lastName, old.bio, name.trim(), old) }
    }
    override suspend fun findByPhone(phone: String): KxContact? = orNull { vm.users.findByPhone(phone) }?.let {
        KxContact(it.id, it.name, KxFormat.status(it.status), it.status == com.kirox.app.telegram.TelegramUserStatus.Online, avatar = avatarOf(it.photo, paths.value))
    }
    override suspend fun importContact(first: String, last: String, phone: String): Long? =
        orNull { vm.users.addContact(phone.filter { it.isDigit() || it == '+' }, first.trim(), last.trim()) }

    // New chats
    override suspend fun createGroup(title: String, userIds: List<Long>, about: String): Long? =
        try { vm.chats.createGroup(title.trim(), userIds, about) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }
    override suspend fun createChannel(title: String, about: String, username: String?): Long? =
        try { vm.chats.createChannel(title.trim(), about, username?.takeIf { it.isNotBlank() }) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }
    override suspend fun createSecretChat(userId: Long): Long? =
        try { vm.chats.createSecretChat(userId) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }

    // Search
    override suspend fun searchChats(query: String): List<KxChat> {
        if (query.isBlank()) return emptyList()
        val known = paths.value
        return orNull { vm.chats.search(query.trim()) }.orEmpty().map { it.toKx(null, false, known) }.visible()
    }
    override suspend fun searchMessages(query: String): List<KxFound> {
        if (query.isBlank()) return emptyList()
        val known = paths.value
        return orNull { vm.chats.searchMessages(query.trim()) }.orEmpty().filter { it.chatId !in hiddenIds }.map {
            KxFound(it.chatId, it.messageId, it.chatTitle, it.text.replace('\n', ' '), KxFormat.chatTime(it.date), autoPath(it.photo, known))
        }
    }
    override suspend fun searchMessagesOf(query: String, kind: Int): List<KxFound> {
        if (query.isBlank()) return emptyList()
        val filter: org.drinkless.tdlib.TdApi.SearchMessagesFilter = when (kind) { 0 -> org.drinkless.tdlib.TdApi.SearchMessagesFilterPhotoAndVideo(); 1 -> org.drinkless.tdlib.TdApi.SearchMessagesFilterUrl(); else -> org.drinkless.tdlib.TdApi.SearchMessagesFilterDocument() }
        val known = paths.value
        return orNull { vm.chats.searchMessages(query.trim(), filter) }.orEmpty().filter { it.chatId !in hiddenIds }.map {
            KxFound(it.chatId, it.messageId, it.chatTitle, it.text.replace('\n', ' '), KxFormat.chatTime(it.date), autoPath(it.photo, known))
        }
    }
    override suspend fun searchInChat(chatId: Long, query: String): List<Long> {
        if (history.chatId != chatId || query.isBlank()) return emptyList()
        return orNull { vm.messages.search(query.trim()).first }.orEmpty().map { it.id }
    }
    // wave5: a jump asked right after openChat (search hit, JumpDate, SharedMedia) waits until that chat's history is open.
    @Volatile private var pendingJump: Pair<Long, Long>? = null
    override fun jumpTo(chatId: Long, messageId: Long) {
        if (history.chatId == chatId && history.loaded) { pendingJump = null; vm.messages.jumpTo(messageId) } else pendingJump = chatId to messageId
    }
    override fun jumpTarget(chatId: Long): Long = if (history.chatId == chatId) history.jumpTo else 0L
    override fun jumpDone() { vm.messages.jumpDone() }
    override fun editableText(chatId: Long, messageId: Long): String? = if (history.chatId == chatId) runCatching { vm.messages.editableText(messageId) }.getOrNull() else null
    private var lastExport: Pair<String, String>? = null
    override suspend fun exportChat(chatId: Long, onProgress: (Int) -> Unit): String? = try {
        val text = services.chats.exportText(chatId) { onProgress(it) }
        val file = kotlinx.coroutines.withContext(Dispatchers.IO) { java.io.File(app.cacheDir, "export").apply { mkdirs() }.resolve("chat.txt").apply { writeText(text) } }
        val name = header(chatId).title.ifBlank { "chat" }.map { c -> if (c.code == 92 || c in "/:*?<>|" || c == '"') '_' else c }.joinToString("") + ".txt"
        lastExport = file.path to name
        if (com.kirox.app.ui.chat.saveToDownloads(app, file.path, name, "text/plain")) null else "Не удалось сохранить переписку"
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { "Не удалось сохранить переписку" }
    override suspend fun shareLastExport(): String? {
        val (path, name) = lastExport ?: return "Сначала экспортируйте чат"
        return if (com.kirox.app.ui.chat.shareFile(app, path, name, "text/plain")) null else "Нет приложения, чтобы поделиться"
    }
    override suspend fun keptDeleted(chatId: Long): List<Pair<String, String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching { services.archive.deleted(chatId) }.getOrDefault(emptyList()).map { k ->
            listOf(k.sender, KxFormat.dateTime(k.date)).filter { it.isNotBlank() }.joinToString(" · ") + " · удалено " + KxFormat.dateTime(k.deletedAt) to k.text
        }
    }
    override suspend fun editHistory(chatId: Long, messageId: Long): List<Pair<String, String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching { services.archive.versions(chatId, messageId) }.getOrDefault(emptyList()).map { "до " + KxFormat.dateTime(it.editedAt) to it.text }
    }
    override fun chatDraft(chatId: Long): String? = if (history.chatId == chatId && history.loaded) history.draft else null

    // Chat tools: TelegramMessageRepository sends these to its open chat only.
    private fun inOpen(chatId: Long, block: () -> Unit) {
        if (history.chatId == chatId) block() else error = "Откройте чат, чтобы отправить"
    }
    override fun sendQuote(chatId: Long, text: String, replyTo: Long, quote: String) = inOpen(chatId) { vm.messages.sendText(text.trim(), replyTo, quote = quote) }
    override fun sendSilent(chatId: Long, text: String) = inOpen(chatId) { vm.messages.sendText(text.trim(), null, silent = true) }
    override fun sendScheduled(chatId: Long, text: String, at: Int) =
        inOpen(chatId) { vm.messages.sendText(text.trim(), null, scheduleAt = if (at < 0) com.kirox.app.telegram.SEND_WHEN_ONLINE else at) }
    override suspend fun scheduled(chatId: Long): List<KxScheduled> {
        if (history.chatId != chatId) return emptyList()
        return orNull { vm.messages.scheduledMessages() }.orEmpty().map {
            KxScheduled(it.id, it.text, if (it.sendAt == 0) "когда будет в сети" else KxFormat.futureTime(it.sendAt))
        }
    }
    override fun sendScheduledNow(chatId: Long, messageId: Long) = inOpen(chatId) { act { vm.messages.sendScheduledNow(messageId) } }
    override fun deleteScheduled(chatId: Long, messageId: Long) = inOpen(chatId) { act { vm.messages.deleteScheduled(messageId) } }
    override fun sendPoll(chatId: Long, question: String, options: List<String>, anonymous: Boolean, multiple: Boolean, quizCorrect: Int?) =
        inOpen(chatId) {
            // Blank options are dropped: the quiz answer index must point into the kept list.
            val kept = options.mapIndexed { i, o -> i to o.trim() }.filter { it.second.isNotEmpty() }
            val correct = quizCorrect?.let { q -> kept.indexOfFirst { it.first == q }.takeIf { it >= 0 } }
            if (quizCorrect != null && correct == null) { error = "Отметьте правильный ответ"; return@inOpen }
            vm.messages.sendPoll(question.trim(), kept.map { it.second }, anonymous, multiple, correct)
        }
    override fun votePoll(chatId: Long, messageId: Long, options: List<Int>) = inOpen(chatId) { vm.messages.vote(messageId, options) }
    override fun sendLocation(chatId: Long, latitude: Double, longitude: Double) = inOpen(chatId) { vm.messages.sendLocation(latitude, longitude) }
    override fun sendPhoto(chatId: Long, path: String, caption: String, spoiler: Boolean) = inOpen(chatId) { vm.messages.sendPhoto(path, caption, spoiler) }
    override fun sendFile(chatId: Long, path: String, caption: String) = inOpen(chatId) { vm.messages.sendDocument(path, caption) }
    override fun forward(fromChatId: Long, messageIds: List<Long>, toChatId: Long, hideSender: Boolean, dropCaption: Boolean) {
        if (messageIds.isNotEmpty()) vm.messages.forwardFrom(fromChatId, toChatId, messageIds, asCopy = hideSender, removeCaption = hideSender && dropCaption)
    }
    override suspend fun messageLink(chatId: Long, messageId: Long): String? = orNull { services.admin.messageLink(chatId, messageId) }
    override suspend fun pressButton(chatId: Long, messageId: Long, row: Int, column: Int): String? {
        if (history.chatId != chatId) return null
        val button = history.messages.firstOrNull { it.id == messageId }?.buttons?.getOrNull(row)?.getOrNull(column) ?: return null
        // URL buttons need no request: the screen opens the link.
        if (button.url != null && button.kind == com.kirox.app.telegram.TelegramButton.Kind.Url) return button.url
        val answer = try { vm.messages.pressButton(messageId, button) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }
        return answer?.let { it.second ?: it.first.takeIf { t -> t.isNotBlank() } }
    }
    override fun setAutoDelete(chatId: Long, seconds: Int) = act { vm.chats.setAutoDelete(chatId, seconds) }

    // Forums & comments
    override suspend fun forumTopics(chatId: Long): List<KxTopic> = orNull { vm.messages.forumTopics(chatId) }.orEmpty().map {
        KxTopic(it.id, it.title, it.unreadCount, it.isPinned, it.isClosed, it.preview, KxFormat.chatTime(it.date), it.isGeneral)
    }
    override fun openTopic(chatId: Long, topicId: Int, title: String) { fillAskedAt = -1; vm.messages.openTopic(chatId, topicId, title) }
    override suspend fun createTopic(chatId: Long, title: String): Int? =
        try { vm.messages.createForumTopic(chatId, title.trim()) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }
    override fun openComments(chatId: Long, postId: Long) { fillAskedAt = -1; vm.messages.openComments(chatId, postId) }
    override val historyChatId: Long get() = history.chatId

    // Stickers & GIFs: items are remembered by id so sendSticker can send the same file again.
    private val panelItems = java.util.concurrent.ConcurrentHashMap<String, TelegramPanelItem>()
    private fun TelegramPanelItem.toKx(): KxSticker {
        val id = KxExtrasMapping.stickerId(this)
        panelItems[id] = this
        val known = paths.value
        // Animated stickers and GIFs show their still thumbnail until the screen plays them.
        val path = if (media.animatedSticker || isGif) autoPath(thumbnail, known) ?: knownPath(media.file, known) else autoPath(media.file, known)
        return KxSticker(id, media.title, path, media.file.id, animated = media.animatedSticker, gif = isGif)
    }
    override suspend fun stickerSets(): List<KxStickerSet> = vm.stickers.installedSets().map { KxStickerSet(it.id, it.title, it.cover?.toKx()) }
    override suspend fun stickers(setId: Long): List<KxSticker> = vm.stickers.set(setId).map { it.toKx() }
    override suspend fun recentStickers(): List<KxSticker> = vm.stickers.recent().map { it.toKx() }
    override suspend fun favoriteStickers(): List<KxSticker> = vm.stickers.favorites().map { it.toKx() }
    override suspend fun savedGifs(): List<KxSticker> = vm.stickers.savedGifs().map { it.toKx() }
    override suspend fun searchGifs(chatId: Long, query: String): List<KxSticker> = vm.stickers.searchGifs(chatId, query).map { it.toKx() }
    override fun sendSticker(chatId: Long, stickerId: String, replyTo: Long?) {
        val item = panelItems[stickerId] ?: return
        inOpen(chatId) { vm.messages.sendPanelItem(item, replyTo) }
    }

    // Saved Messages
    override suspend fun savedTags(): List<KxSavedTag> = orNull { vm.messages.savedTags() }.orEmpty().map { KxSavedTag(it.emoji, it.label, it.count) }

    // Profiles: shared media per chat and tab, loaded on first read.
    private val sharedItems = mutableStateMapOf<Pair<Long, KxSharedTab>, List<KxSharedItem>>()
    override fun shared(chatId: Long, tab: KxSharedTab): List<KxSharedItem> {
        val key = chatId to tab
        val kind = KxExtrasMapping.shared(tab)
        loadOnce("shared:$chatId:$tab") {
            val known = paths.value
            val months = java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale.forLanguageTag("ru"))
            val found = if (kind == null) vm.chats.sharedGifs(chatId) else vm.chats.sharedMedia(chatId, kind)
            sharedItems[key] = found.map { item ->
                KxSharedItem(
                    id = item.messageId, title = item.title, sub = item.subtitle,
                    media = autoPath(item.thumb, known), video = if (item.isVideo) "video" else null,
                    fileExt = if (tab == KxSharedTab.Files || tab == KxSharedTab.Music) item.title.substringAfterLast('.', "").take(4).uppercase() else "",
                    messageId = item.messageId,
                    month = months.format(java.util.Date(item.date * 1000L)).replaceFirstChar { it.uppercase() },
                    fileId = (item.file ?: item.full)?.id ?: 0,
                )
            }
        }
        return sharedItems[key].orEmpty()
    }

    // ---- Round 5b: groups & channels, proxies, account ------------------------------------

    /** Value of a request whose failure goes to [error] (null then). */
    private suspend fun <T> ask(block: suspend () -> T): T? = try { block() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = KxExtrasMapping.errorText(e); null }

    override suspend fun groupInfo(chatId: Long): KxGroupInfo? {
        val details = ask { services.admin.details(chatId) } ?: return null
        val settings = orNull { services.adminExtras.settings(chatId) }
        val rights = orNull { services.admin.rights(chatId) }
        val username = orNull { vm.chats.profileInfo(chatId).username }.orEmpty()
        return KxGroupInfo(
            chatId, details.title, details.description, username,
            isChannel = settings?.isChannel ?: (items[chatId]?.kind == TelegramChatKind.Channel),
            isPublic = settings?.isPublic ?: username.isNotEmpty(),
            slowModeSeconds = settings?.slowModeSeconds ?: 0, joinByRequest = settings?.joinByRequest == true,
            reactions = settings?.reactions, stickerSetId = settings?.stickerSetId ?: 0,
            protectedContent = orNull { services.admin.isProtected(chatId) } == true,
            members = memberCounts[chatId] ?: orNull { vm.chats.memberCount(chatId) } ?: 0,
            owner = rights?.isOwner == true, canChangeInfo = rights?.canChangeInfo == true, canRestrict = rights?.canRestrict == true,
            canPromote = rights?.canPromote == true, canInvite = rights?.canInvite == true,
        )
    }
    override fun setChatTitle(chatId: Long, title: String) = act { services.admin.setTitle(chatId, title.trim()) }
    override fun setChatDescription(chatId: Long, text: String) = act { services.admin.setDescription(chatId, text.trim()) }
    override fun setSlowMode(chatId: Long, seconds: Int) = act { services.adminExtras.setSlowMode(chatId, seconds) }
    override fun setJoinByRequest(chatId: Long, on: Boolean) = act {
        val sg = services.adminExtras.settings(chatId)?.supergroupId ?: run { error = "Доступно только в супергруппах"; return@act }
        services.adminExtras.setJoinByRequest(sg, on)
    }
    override fun setAllowedReactions(chatId: Long, emoji: List<String>?) = act { services.adminExtras.setReactions(chatId, emoji?.map { if (it == "❤️") "❤" else it }) }
    override fun setGroupStickerSet(chatId: Long, setId: Long) = act {
        val sg = services.adminExtras.settings(chatId)?.supergroupId ?: run { error = "Доступно только в супергруппах"; return@act }
        services.adminExtras.setStickerSet(sg, setId)
    }
    override fun setProtectedContent(chatId: Long, on: Boolean) = act { services.admin.setProtected(chatId, on) }
    override suspend fun members(chatId: Long): List<KxMember> {
        val known = paths.value
        return ask { services.admin.members(chatId) }.orEmpty().map { m ->
            KxMember(m.userId, m.name, m.role.title, avatar = autoPath(m.photo, known), canEdit = m.canBeEdited)
        }
    }
    private fun isChannel(chatId: Long) = items[chatId]?.kind == TelegramChatKind.Channel || history.takeIf { it.chatId == chatId }?.peerKind == TelegramPeerKind.Channel
    override fun promote(chatId: Long, userId: Long) = act { services.admin.promote(chatId, userId, isChannel(chatId)) }
    override fun demote(chatId: Long, userId: Long) = act { services.admin.demote(chatId, userId) }
    override fun restrict(chatId: Long, userId: Long, restricted: Boolean) = act { if (restricted) services.admin.restrict(chatId, userId) else services.admin.unrestrict(chatId, userId) }
    override fun ban(chatId: Long, userId: Long, banned: Boolean) = act { if (banned) services.admin.ban(chatId, userId) else services.admin.unban(chatId, userId) }
    override fun removeMember(chatId: Long, userId: Long) = act { services.admin.remove(chatId, userId) }
    override fun promoteWith(chatId: Long, userId: Long, rights: Set<Int>, title: String) = act { services.admin.promoteWith(chatId, userId, isChannel(chatId), rights, title) }
    override suspend fun adminRights(chatId: Long, userId: Long): Set<Int>? = ask { services.admin.adminRows(chatId, userId) }
    override fun restrictWith(chatId: Long, userId: Long, allow: Set<Int>, untilSec: Int) = act { services.admin.restrictWith(chatId, userId, allow, untilSec) }
    override fun banFor(chatId: Long, userId: Long, untilSec: Int, deleteMessages: Boolean) = act { services.admin.banFor(chatId, userId, untilSec, deleteMessages) }
    private fun com.kirox.app.telegram.InviteLink.toKx() = KxInviteLink(link, name, isPrimary, memberCount,
        if (expires > 0) "до " + KxFormat.futureTime(expires) else "", memberLimit)
    override suspend fun inviteLinks(chatId: Long): List<KxInviteLink> = ask { services.admin.inviteLinks(chatId) }.orEmpty().map { it.toKx() }
    override suspend fun createInviteLink(chatId: Long, name: String, days: Int, limit: Int, byRequest: Boolean): KxInviteLink? =
        ask { services.admin.createInviteLink(chatId, name.trim(), days, limit, byRequest) }?.toKx()
    override fun revokeInviteLink(chatId: Long, link: String) = act { services.admin.revokeInviteLink(chatId, link) }
    override suspend fun joinRequests(chatId: Long): List<KxJoinRequest> {
        val known = paths.value
        return ask { services.admin.joinRequests(chatId) }.orEmpty().map { KxJoinRequest(it.userId, it.name, it.bio, KxFormat.chatTime(it.date), autoPath(it.photo, known)) }
    }
    override fun processJoinRequest(chatId: Long, userId: Long, approve: Boolean) = act { services.admin.processJoinRequest(chatId, userId, approve) }
    override suspend fun adminLog(chatId: Long, query: String): List<KxLogEntry> =
        orNull { services.adminExtras.eventLog(chatId, query) }.orEmpty().map { KxLogEntry(it.id, KxFormat.dateTime(it.date), it.who, it.text) }
    override suspend fun boostStatus(chatId: Long): KxBoostStatus? = orNull { services.adminExtras.boosts(chatId) }?.let {
        KxBoostStatus(it.level, it.boosts, it.levelStart, it.nextLevel, it.url, it.premiumMembers)
    }
    override suspend fun chatStats(chatId: Long): KxStats? = orNull { services.admin.statistics(chatId) }?.let { KxStats(it.isChannel, it.period, it.values, it.notificationsPercent) }
    override suspend fun postStats(chatId: Long, messageId: Long): KxPostStats? = orNull { services.adminExtras.postStats(chatId, messageId) }?.let {
        KxPostStats(it.views, it.forwards, it.reactions, it.comments)
    }
    override fun pinnedIds(chatId: Long): List<Long> = if (history.chatId == chatId) history.pinnedIds.sorted() else emptyList()

    // Proxy
    private var proxyList by mutableStateOf(emptyList<KxProxy>())
    private suspend fun reloadProxies() {
        proxyList = services.settings.proxies().map { KxProxy(it.id, it.server, it.port, it.kind.title, it.enabled) }
    }
    override val proxies: List<KxProxy> get() { loadOnce("proxies") { reloadProxies() }; return proxyList }
    override fun addProxy(server: String, port: Int, type: String, secret: String) = act {
        val kind = com.kirox.app.telegram.ProxyKind.entries.firstOrNull { it.title.equals(type, true) || it.name.equals(type, true) } ?: com.kirox.app.telegram.ProxyKind.MtProto
        services.settings.addProxy(server.trim(), port, kind, secret.trim()); reloadProxies()
    }
    override fun enableProxy(id: Int) = act { services.settings.enableProxy(id); reloadProxies() }
    override fun disableProxy() = act { services.settings.disableProxy(); reloadProxies() }
    override fun removeProxy(id: Int) = act { services.settings.removeProxy(id); reloadProxies() }

    // Account & profile
    private var ttl by mutableStateOf(365)
    override val accountTtlDays: Int get() { loadOnce("accountTtl") { ttl = services.settings.accountTtl().days }; return ttl }
    override fun setAccountTtl(days: Int) {
        val value = com.kirox.app.telegram.AccountTtl.of(days)
        ttl = value.days
        act { services.settings.setAccountTtl(value) }
    }
    override suspend fun deleteAccount(reason: String, password: String): String? = attempt { services.account.deleteAccount(reason, password) }
    override suspend fun updateProfile(first: String, last: String, bio: String): String? {
        val old = vm.users.profile.value ?: return "Профиль ещё не загружен"
        if (first.isBlank()) return "Введите имя"
        return attempt { vm.users.updateProfile(first.trim(), last.trim(), bio.trim(), old.username.orEmpty(), old) }
    }
    override suspend fun setBirthday(day: Int, month: Int, year: Int): String? =
        attempt { services.account.setBirthday(if (day == 0) null else com.kirox.app.telegram.Birthday(day, month, year)) }
    override fun setEmojiStatus(customEmojiId: Long) = act { vm.users.setEmojiStatus(customEmojiId) }
    override suspend fun profilePhotos(userId: Long): List<KxSharedItem> {
        val photos = orNull { services.account.profilePhotoHistory(userId) }.orEmpty()
        photos.forEach { autoPath(it.file, paths.value) } // starts the downloads
        // Profile photos are small: wait a moment so the board opens with pictures, not placeholders.
        kotlinx.coroutines.withTimeoutOrNull(4000) {
            paths.first { known -> photos.all { val f = it.file; f == null || f.localPath != null || known.containsKey(f.id) } }
        }
        val known = paths.value
        return photos.map { KxSharedItem(it.id, sub = KxFormat.dayHeader(it.addedDate), media = autoPath(it.file, known), fileId = it.file?.id ?: 0, look = if (it.animated) 1 else 0) }
    }
    override fun joinChat(chatId: Long) = act { vm.chats.join(chatId) }
    override suspend fun inviteLinkInfo(link: String): KxInviteInfo? = orNull { vm.chats.inviteLinkInfo(link) }?.let { i ->
        KxInviteInfo(i.chatId, i.title, i.channel, i.public, i.members, i.knownNames, i.description, i.byRequest, autoPath(i.photo, paths.value), i.member)
    }
    override suspend fun messageRequests(): List<KxRequest> {
        // Chats from strangers are often auto-archived («Архивировать новые чаты»): load the archive's first
        // page too when nothing of it is known yet, and wait a moment for it.
        if (items.values.none { it.archived }) {
            loadMoreChats(-1)
            repeat(15) { if (items.values.any { it.archived }) return@repeat; delay(200) }
        }
        val contactIds = vm.users.contacts.value.map { it.id }.toSet()
        val candidates = items.values.filter {
            it.kind == com.kirox.app.telegram.TelegramChatKind.Private && it.peerUserId != 0L && !it.isBot && !it.isSaved && !it.isSecret &&
                it.peerUserId !in contactIds && it.peerUserId != me.id
        }.sortedByDescending { it.timestamp }.take(60)
        val known = paths.value
        return candidates.mapNotNull { c ->
            if (!vm.chats.fromStranger(c.id)) return@mapNotNull null
            val username = orNull { vm.chats.profileInfo(c.id) }?.username.orEmpty()
            val common = orNull { services.admin.commonGroups(c.peerUserId).size } ?: 0
            KxRequest(c.id, c.peerUserId, c.title, KxRequestMapping.meta(username, c.photo != null, common), c.preview, common, risky = c.isScam || c.isFake,
                media = KxRequestMapping.hiddenMedia(c.lastKind), avatar = autoPath(c.photo, known), colorIndex = c.accentColorId)
        }
    }
    override fun acceptRequest(chatId: Long) = act { vm.chats.removeActionBar(chatId) }
    override suspend fun storyViewers(storyId: Int): List<KxStoryViewer> {
        val known = paths.value
        return orNull { vm.stories.viewers(storyId) }.orEmpty().map { KxStoryViewer(it.userId, it.name, KxFormat.ago(it.date), it.reaction.orEmpty(), autoPath(it.photo, known)) }
    }
    override suspend fun storyStats(storyId: Int): KxPostStats? =
        vm.stories.story(me.id, storyId)?.let { KxPostStats(it.viewCount, 0, it.reactionCount, 0) }
    override suspend fun deleteStory(storyId: Int): String? = attempt { vm.stories.delete(me.id, storyId) }
    override suspend fun translateMessage(chatId: Long, messageId: Long, toLang: String): String? {
        // The board says why it failed; no store.error banner on top of that.
        return orNull { vm.messages.translateIn(chatId, messageId, toLang) }
    }
    override suspend fun joinByInviteLink(link: String): String? {
        var request = false
        return attempt { request = vm.chats.joinByInviteLink(link) } ?: if (request) "request" else null
    }
    override suspend fun setMainPhoto(photoId: Long): String? = attempt { services.account.setMainProfilePhoto(photoId) }
    override suspend fun deletePhoto(photoId: Long): String? = attempt { services.account.deleteProfilePhoto(photoId) }

    /** Profile extras per chat (username, phone, bio, birthday, common groups), loaded on first read. */
    private val peerExtras = mutableStateMapOf<Long, KxPeer>()
    override fun peer(chatId: Long): KxPeer {
        val h = header(chatId)
        val item = items[chatId]
        val userId = item?.peerUserId?.takeIf { it != 0L } ?: if (h.kind == KxChatKind.Private || h.kind == KxChatKind.Bot) chatId else 0L
        val base = KxPeer(userId.takeIf { it != 0L } ?: chatId, chatId, h.title, h.status, h.online, premium = item?.isPremium == true,
            muted = h.muted, avatar = h.avatar, colorIndex = h.colorIndex,
            isContact = userId != 0L && vm.users.contacts.value.any { it.id == userId })
        loadOnce("peer:$chatId") {
            val info = vm.chats.profileInfo(chatId)
            val extras = if (userId != 0L) orNull { services.account.extras(userId) } else null
            val common = if (userId != 0L) orNull { services.admin.commonGroups(userId).size } ?: 0 else 0
            peerExtras[chatId] = base.copy(
                username = info.username, phone = if (info.phone.isBlank()) "" else runCatching { com.kirox.app.phone.PhoneNumberEngine().formatInternational(info.phone) }.getOrDefault("+" + info.phone),
                bio = info.about, birthday = extras?.birthday?.text.orEmpty(), commonGroups = common,
            )
        }
        val extra = peerExtras[chatId] ?: return base
        // Live values (status, online, avatar) from the header; the loaded facts from the extras.
        return base.copy(username = extra.username, phone = extra.phone, bio = extra.bio, birthday = extra.birthday, commonGroups = extra.commonGroups)
    }

    private fun initRound5() {
        scope.launch { services.client.accounts.accounts.collect { accountSlots = it } }
        scope.launch { com.kirox.app.settings.KiroxTweaks.state.collect { tweakState = KxExtrasMapping.tweaks(it) } }
    }

    // ---- Round 6: kiroX-local settings (TelegramKxLocal.kt) ----
    private val local by lazy { KxLocalSettings(app, scope).also { it.start() } }
    private val mergedPrefs by androidx.compose.runtime.derivedStateOf { local.values + account.values + chat3.values }
    override val prefs: Map<String, Int> get() = mergedPrefs
    override fun putPref(key: String, value: Int) = if (chat3.owns(key)) chat3.put(key, value) else if (account.owns(key)) account.put(key, value) else local.put(key, value)
    override val tabNames: Map<Int, String> get() = local.tabNames
    override fun renameTab(index: Int, name: String) = local.renameTab(index, name)
    override val passcodeSet: Boolean get() = local.passcodeSet
    override val passcodeBiometric: Boolean get() = local.passcodeBiometric
    override val passcodePattern: Boolean get() = com.kirox.app.security.PasscodeLock.settings.value.pattern
    override suspend fun checkPasscode(code: String): Boolean = local.checkPasscode(code)
    override fun passcodeBiometricOk() = local.biometricOk()
    override fun passcodeWait(): Int = local.passcodeWait()

    init {
        initRound5() // last: see the note in the first init block
        // Warm the preferences behind [local] off the main thread (first reads load from disk).
        scope.launch(Dispatchers.IO) { app.getSharedPreferences("kirox_v3_prefs", android.content.Context.MODE_PRIVATE); com.kirox.app.settings.ChatWallpapers.count(app) }
    }

    // ---- wave3-media (TelegramKxMedia.kt) ----
    private val mediaBinderLazy = lazy { KxMediaBinder(app, vm, scope) { autoPath(it, paths.value) }.also { it.start() } }
    private val mediaBinder by mediaBinderLazy
    private val mediaBinderOrNull: KxMediaBinder? get() = if (mediaBinderLazy.isInitialized()) mediaBinder else null
    override suspend fun chatMedia(chatId: Long, aroundMessageId: Long) = mediaBinder.chatMedia(chatId, aroundMessageId)
    override suspend fun albumOf(chatId: Long, messageId: Long) = mediaBinder.albumOf(chatId, messageId)
    override fun fileState(fileId: Int) = mediaBinder.fileState(fileId)
    override fun cancelDownload(fileId: Int) = mediaBinder.cancelDownload(fileId)
    override suspend fun saveToGallery(fileId: Int, video: Boolean): String? {
        // wave5 (fixer-media): Android 7–9 write the gallery copy only with WRITE_EXTERNAL_STORAGE — ask instead of failing silently.
        if (android.os.Build.VERSION.SDK_INT < 29 && !granted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return "Разрешите доступ к памяти и сохраните ещё раз"
        }
        return mediaBinder.saveToGallery(fileId, video)
    }
    override suspend fun shareFile(fileId: Int) = mediaBinder.shareFile(fileId)
    override suspend fun openFile(fileId: Int) = mediaBinder.openFile(fileId)
    override suspend fun fileCheck(fileId: Int, name: String) = mediaBinder.fileCheck(fileId, name)
    override suspend fun musicLibrary(query: String) = mediaBinder.musicLibrary(query)
    override val player: KxPlayerState get() = mediaBinder.player
    override fun playTracks(tracks: List<KxTrack>, index: Int) = mediaBinder.playTracks(tracks, index)
    override fun playerToggle() = mediaBinder.playerToggle()
    override fun playerNext() = mediaBinder.playerNext()
    override fun playerPrevious() = mediaBinder.playerPrevious()
    override fun playerSeek(fraction: Float) = mediaBinder.playerSeek(fraction)
    override fun playerShuffle() = mediaBinder.playerShuffle()
    override fun playerRepeat() = mediaBinder.playerRepeat()
    override fun playVoice(fileId: Int) = mediaBinder.playVoice(fileId)
    override fun downloadMessageFile(chatId: Long, messageId: Long, fileId: Int) { mediaBinder.addToDownloads(fileId, chatId, messageId); if (fileId != 0) watchFile(TelegramFileRef(fileId, null)) }
    override val autoplayGifs: Boolean get() = vm.autoDownload.autoplayGifs.value
    override suspend fun downloads(query: String) = mediaBinder.downloads(query)
    override fun pauseDownload(fileId: Int, paused: Boolean) = mediaBinder.pauseDownload(fileId, paused)
    override fun removeDownloads(fileIds: List<Int>) = mediaBinder.removeDownloads(fileIds)
    override suspend fun voiceMessages(chatId: Long) = mediaBinder.voiceMessages(chatId)
    override fun markListened(chatId: Long, messageId: Long) = mediaBinder.markListened(chatId, messageId)
    override suspend fun transcribe(chatId: Long, messageId: Long, onPartial: (String) -> Unit) = mediaBinder.transcribe(chatId, messageId, onPartial)
    override suspend fun storyArchive() = mediaBinder.storyArchive()
    override suspend fun profileStories() = mediaBinder.profileStories()
    override suspend fun setStoryOnProfile(storyId: Int, on: Boolean) = mediaBinder.setStoryOnProfile(storyId, on)
    override suspend fun pinnedStoryIds() = mediaBinder.pinnedStoryIds()
    override suspend fun pinnedStoryMax() = mediaBinder.pinnedStoryMax()
    override suspend fun pinStories(ids: List<Int>) = mediaBinder.pinStories(ids)
    override suspend fun deviceMedia(limit: Int) = mediaBinder.deviceMedia(limit)
    override suspend fun postStory(media: KxDeviceMedia, caption: String, audience: Int) = mediaBinder.postStory(media, caption, audience)
    override suspend fun sendDeviceMedia(chatId: Long, media: KxDeviceMedia, caption: String) = mediaBinder.sendDeviceMedia(chatId, media, caption)
    override suspend fun sendDeviceMediaAs(chatId: Long, media: KxDeviceMedia, caption: String, spoiler: Boolean, asFile: Boolean) =
        mediaBinder.sendDeviceMedia(chatId, media, caption, spoiler, asFile)
    override suspend fun sendDeviceAudio(chatId: Long, uri: String, caption: String) = mediaBinder.sendDeviceAudio(chatId, uri, caption)
    override fun roundActivity(chatId: Long, on: Boolean) { vm.activity.send(chatId, if (on) ChatActivity.RecordingVideoNote else null) }
    override suspend fun closeFriends() = mediaBinder.closeFriends()
    override suspend fun privateChat(userId: Long) = mediaBinder.privateChat(userId)
    override val activeCall: KxCallState? get() = mediaBinder.activeCall
    override val groupCall: KxGroupCallState? get() = mediaBinder.groupCall
    override val lastCall: KxCallState? get() = mediaBinder.lastCall
    // wave5 (fixer-media): answer / camera / video chat go through the host's permission ask like an outgoing call;
    // without the camera a video call or camera switch stays voice only (no capture without permission).
    private fun granted(p: String) = androidx.core.content.ContextCompat.checkSelfPermission(app, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
    override fun answerCall() { val video = activeCall?.video == true; startCall(activeCall?.peerId ?: 0, video) { if (granted(android.Manifest.permission.RECORD_AUDIO)) { vm.groupCalls.leave(); mediaBinder.answerCall() } else error = "Нужен доступ к микрофону" } }
    override fun hangUp() = mediaBinder.hangUp()
    override fun muteCallMic(muted: Boolean) = mediaBinder.muteCallMic(muted)
    override fun turnCallCamera(on: Boolean) { if (!on) mediaBinder.turnCallCamera(false) else startCall(activeCall?.peerId ?: 0, true) { if (granted(android.Manifest.permission.CAMERA)) mediaBinder.turnCallCamera(true) else error = "Нужен доступ к камере" } }
    override fun flipCallCamera() = mediaBinder.flipCallCamera()
    override fun attachCallVideo(remote: Any?, local: Any?) = mediaBinder.attachCallVideo(remote, local)
    override fun joinGroupCall(chatId: Long) { startCall(0, false) { if (granted(android.Manifest.permission.RECORD_AUDIO)) mediaBinder.joinGroupCall(chatId) else error = "Нужен доступ к микрофону" } }
    override fun leaveGroupCall() = mediaBinder.leaveGroupCall()
    override fun muteGroupMic(muted: Boolean) = mediaBinder.muteGroupMic(muted)
    override fun turnGroupCamera(on: Boolean) { if (!on) mediaBinder.turnGroupCamera(false) else startCall(0, true) { if (granted(android.Manifest.permission.CAMERA)) mediaBinder.turnGroupCamera(true) else error = "Нужен доступ к камере" } }
    override suspend fun rateCall(callId: Int, rating: Int, comment: String, problems: List<String>) = mediaBinder.rateCall(callId, rating, comment, problems)
    override suspend fun planVideoChat(chatId: Long, title: String, startAt: Int, rtmp: Boolean) = mediaBinder.planVideoChat(chatId, title, startAt, rtmp)
    override suspend fun networkStats() = mediaBinder.networkStats()
    override suspend fun resetNetworkStats() = mediaBinder.resetNetworkStats()
    override suspend fun deviceSpace() = mediaBinder.deviceSpace()
    override suspend fun netCheck() = mediaBinder.netCheck()
    override suspend fun notificationSounds() = mediaBinder.notificationSounds()
    override suspend fun scopeSound(key: String) = mediaBinder.scopeSound(key)
    override suspend fun changeScopeSound(key: String, soundId: Long) = mediaBinder.changeScopeSound(key, soundId)
    override fun previewSound(soundId: Long) = mediaBinder.previewSound(soundId)

    // ---- wave3-account (TelegramKxAccount.kt) ----
    private val account by lazy { KxAccountBinder(app, services, scope) { error = it }.also { it.start() } }
    override val prefTexts: Map<String, String> get() = account.texts
    override fun putPrefText(key: String, value: String) = account.putText(key, value)
    override suspend fun folderInfo(id: Int): KxFolderInfo? = account.folderInfo(id)?.let { f -> f.copy(unread = folders.firstOrNull { it.id == id }?.unread ?: 0) }
    override suspend fun saveFolder(id: Int, info: KxFolderInfo): String? = account.saveFolder(id, info)
    override suspend fun folderShareableChats(id: Int): List<Long> = account.folderShareableChats(id)
    override suspend fun folderLinks(id: Int): List<KxInviteLink> = account.folderLinks(id)
    override suspend fun createFolderLink(id: Int, chatIds: List<Long>): String? = account.createFolderLink(id, chatIds)
    override fun deleteFolderLink(id: Int, link: String) = account.deleteFolderLink(id, link)
    override suspend fun personalChannels(): KxPersonalChannels = account.personalChannels(me.id).let { p ->
        p.copy(channels = p.channels.map { c -> c.copy(avatar = items[c.chatId]?.let { autoPath(it.photo, paths.value) }) })
    }
    override suspend fun choosePersonalChannel(chatId: Long): String? = account.choosePersonalChannel(chatId)
    override suspend fun changeNumberSendCode(phone: String): String = account.changeNumberSendCode(phone)
    override suspend fun changeNumberResend(): String = account.changeNumberResend()
    override suspend fun changeNumberCheck(code: String): String? = account.changeNumberCheck(code)
    override suspend fun loginEmailPattern(): String = account.loginEmailPattern()
    override suspend fun loginEmailSendCode(email: String): String? = account.loginEmailSendCode(email)
    override suspend fun loginEmailResend(): String? = account.loginEmailResend()
    override suspend fun loginEmailCheck(code: String): String? = account.loginEmailCheck(code)
    override fun confirmSession(id: Long) = account.confirmSession(id) { reloadSessions() }
    override val termsUpdate: KxTerms? get() = termsState
    private var termsState by mutableStateOf<KxTerms?>(null)
    override suspend fun acceptTerms(id: String): String? = account.acceptTerms(id).also { if (it == null) termsState = null }
    override suspend fun antiSpam(chatId: Long): Boolean? = account.antiSpam(chatId)
    override fun switchAntiSpam(chatId: Long, on: Boolean) = account.switchAntiSpam(chatId, on)
    override fun reportAntiSpamMistake(chatId: Long, messageId: Long) = account.reportAntiSpamMistake(chatId, messageId)
    override fun switchHistoryForNew(chatId: Long, on: Boolean) = account.switchHistoryForNew(chatId, on)
    override suspend fun publicChat(username: String): Long? = account.publicChat(username)
    override suspend fun importedContactCount(): Int? = account.importedContactCount()
    override suspend fun clearImportedContacts(): String? = account.clearImportedContacts()
    override val accentColors: List<KxAccent> get() = accentState
    override val profileColors: List<KxAccent> get() = profileAccentState
    private var accentState by mutableStateOf(emptyList<KxAccent>())
    private var profileAccentState by mutableStateOf(emptyList<KxAccent>())
    override val myColors: Pair<Int, Int> get() { account.loadMyColors(); return account.myColors }
    override suspend fun applyNameColor(accentId: Int): String? = account.applyNameColor(accentId)
    override suspend fun applyProfileColor(accentId: Int): String? = account.applyProfileColor(accentId)
    override suspend fun emojiStatuses(): List<KxEmojiStatus> = account.emojiStatuses()
    override suspend fun applyEmojiStatus(customEmojiId: Long, until: Int): String? = account.applyEmojiStatus(customEmojiId, until)
    override suspend fun countries(): List<KxCountry> = account.countries()
    override suspend fun myCountry(): String = account.myCountry()
    override suspend fun upgradeGroup(chatId: Long): Long? = account.upgradeGroup(chatId)
    private fun initAccount() {
        scope.launch { services.accountUpdates.terms.collect { termsState = account.termsUpdate } }
        scope.launch { services.accountUpdates.accentColors.collect { accentState = account.accentColors } }
        scope.launch { services.accountUpdates.profileColors.collect { profileAccentState = account.profileColors } }
    }
    init { initAccount() }

    // ---- wave3-chat (binder-chat): TelegramKxChat3.kt ----
    private val chat3 by lazy { KxChat3(services, app, scope).also { it.start() } }
    override fun sendWith(chatId: Long, text: String, options: KxSendOptions) = act { chat3.sendWith(chatId, text, options) }
    override suspend fun sendAsOptions(chatId: Long): List<KxSender> = chat3.sendAsOptions(chatId)
    override suspend fun chooseSendAs(chatId: Long, senderId: Long): String? = attempt { chat3.chooseSendAs(chatId, senderId) }
    override suspend fun messageEffects(): List<KxEffect> = chat3.messageEffects()
    override val chatThemes: List<String> get() = chat3.chatThemes
    override suspend fun applyChatTheme(chatId: Long, name: String): String? = attempt { chat3.applyChatTheme(chatId, name) }
    override suspend fun inlineQuery(chatId: Long, bot: String, query: String, offset: String): KxInlineResults? = orNull { chat3.inlineQuery(chatId, bot, query, offset) }
    override fun sendInlineResult(chatId: Long, queryId: Long, resultId: String) = act { chat3.sendInlineResult(chatId, queryId, resultId) }
    override suspend fun reorderPinned(chatIds: List<Long>): String? = attempt { chat3.reorderPinned(chatIds) }
    override val pinnedLimit: Int get() = chat3.pinnedLimit
    override suspend fun messageInfo(chatId: Long, messageId: Long): KxMsgInfo? = chat3.messageInfo(chatId, messageId)
    override suspend fun messageViewers(chatId: Long, messageId: Long): List<KxViewer> = chat3.messageViewers(chatId, messageId)
    override suspend fun reactionPanel(chatId: Long, messageId: Long): KxReactionPanel? = chat3.reactionPanel(chatId, messageId)
    override val quickReaction: String? get() = chat3.quickReaction
    override fun changeQuickReaction(emoji: String) = chat3.changeQuickReaction(emoji)
    override suspend fun replaceMedia(chatId: Long, messageId: Long, path: String, video: Boolean): String? = attempt { chat3.replaceMedia(chatId, messageId, path, video) }
    override suspend fun unreadMentions(chatId: Long, reactions: Boolean): List<KxFound> = chat3.unreadMentions(chatId, reactions)
    override suspend fun peekHistory(chatId: Long, limit: Int): List<KxMessage> = chat3.peekHistory(chatId, limit)
    override suspend fun linkPreview(text: String): KxLinkPreview? = chat3.linkPreview(text)
    override suspend fun messageAtDate(chatId: Long, unix: Int): Long? = chat3.messageAtDate(chatId, unix)
    override suspend fun stickerSetByName(name: String): KxStickerSetInfo? = chat3.stickerSetByName(name)
    override suspend fun stickerSetInfo(setId: Long): KxStickerSetInfo? = chat3.stickerSetInfo(setId)
    override suspend fun installedStickerSets(): List<KxStickerSetInfo> = chat3.installedStickerSets()
    override suspend fun archivedStickerSets(): List<KxStickerSetInfo> = chat3.archivedStickerSets()
    override suspend fun changeStickerSet(setId: Long, installed: Boolean, archived: Boolean): String? = attempt { chat3.changeStickerSet(setId, installed, archived) }
    override suspend fun reorderStickerSets(ids: List<Long>): String? = attempt { chat3.reorderStickerSets(ids) }
    override suspend fun favoriteSticker(stickerId: String, favorite: Boolean): String? = attempt { chat3.favoriteSticker(stickerId, favorite) }
    override fun sendDice(chatId: Long, emoji: String) = act { chat3.sendDice(chatId, emoji) }
    override suspend fun receivedGifts(): List<KxReceivedGift> = chat3.receivedGifts()
    override suspend fun keepGift(giftId: String, saved: Boolean): String? = attempt { chat3.keepGift(giftId, saved) }
    override suspend fun sellGift(giftId: String): String? = attempt { chat3.sellGift(giftId) }
    override fun sendContact(chatId: Long, userId: Long, phone: String, first: String, last: String) = act { chat3.sendContact(chatId, userId, phone, first, last) }
    override suspend fun userNote(userId: Long): String? = chat3.userNote(userId)
    override suspend fun saveUserNote(userId: Long, text: String): String? = attempt { chat3.saveUserNote(userId, text) }
    override suspend fun addContactById(userId: Long, first: String, last: String, sharePhone: Boolean): String? = attempt { chat3.addContactById(userId, first, last, sharePhone) }
    override suspend fun sharePhone(userId: Long): String? = attempt { chat3.sharePhone(userId) }
    override suspend fun reportChat(chatId: Long, messageIds: List<Long>, optionId: String, text: String): KxReportStep = chat3.reportChat(chatId, messageIds, optionId, text)
    override suspend fun leaveChat(chatId: Long): String? = attempt { chat3.leaveChat(chatId) }
    override suspend fun autoDeleteOf(chatId: Long): Int? = chat3.autoDeleteOf(chatId)
    override suspend fun defaultAutoDelete(): Int? = chat3.defaultAutoDelete()
    override suspend fun changeDefaultAutoDelete(seconds: Int): String? = attempt { chat3.changeDefaultAutoDelete(seconds) }
    override suspend fun loadChatNotify(chatId: Long): KxChatNotify? = chat3.loadChatNotify(chatId)
    override suspend fun saveChatNotify(chatId: Long, value: KxChatNotify): String? = attempt { chat3.saveChatNotify(chatId, value) }
    override suspend fun checklist(chatId: Long, messageId: Long): KxChecklist? = chat3.checklist(chatId, messageId)
    override suspend fun sendChecklist(chatId: Long, title: String, tasks: List<String>, othersCanAdd: Boolean, othersCanMark: Boolean): String? =
        attempt { chat3.sendChecklist(chatId, title, tasks, othersCanAdd, othersCanMark) }
    override suspend fun markChecklist(chatId: Long, messageId: Long, done: List<Int>, notDone: List<Int>): String? = attempt { chat3.markChecklist(chatId, messageId, done, notDone) }
    override suspend fun memories(): List<KxFound> = chat3.memories()
    override suspend fun recommendedChats(): List<KxPublicChat> = chat3.recommendedChats()
    override suspend fun searchPublicChats(query: String): List<KxPublicChat> = chat3.searchPublicChats(query)
    override suspend fun similarChats(chatId: Long): List<KxPublicChat> = chat3.similarChats(chatId)
    override suspend fun commonGroups(userId: Long): List<KxPublicChat> = chat3.commonGroups(userId)
    override suspend fun userGifts(userId: Long): List<KxReceivedGift> = chat3.giftsOf(userId)
    override val hiddenStoryChats: List<KxPublicChat>? get() = hiddenStories
    private var hiddenStories by mutableStateOf<List<KxPublicChat>?>(null)
    private var hiddenWatch = false
    override fun loadHiddenStories() {
        if (!hiddenWatch) { hiddenWatch = true; scope.launch { vm.stories.hiddenStoryChats.collect { list ->
            hiddenStories = list.map { KxPublicChat(it.chatId, it.title, channel = false, avatar = autoPath(it.photo, paths.value)) } } } }
        vm.stories.loadHidden()
    }
    override fun hideStories(chatId: Long, hidden: Boolean) = act { vm.stories.setHidden(chatId, hidden) }
    override suspend fun birthdays(): List<KxBirthday> = chat3.birthdays()
    override fun clickBigEmoji(chatId: Long, messageId: Long) { scope.launch { chat3.clickBigEmoji(chatId, messageId) } }
    override val sensitiveAllowed: Boolean? get() = chat3.sensitive
    override fun allowSensitive(on: Boolean) = chat3.allowSensitive(on)
    override suspend fun translateText(text: String, toLang: String): String? = chat3.translateText(text, toLang)
    override suspend fun textEntities(text: String): List<KxEntity> = chat3.textEntities(text)
    override suspend fun chatFacts(chatId: Long): KxChatFacts? = chat3.chatFacts(chatId)
    override suspend fun sendViewOnce(chatId: Long, path: String): String? = chat3.sendViewOnce(chatId, path)

    private companion object {
        const val DRAFT_DELAY_MS = 500L
        const val FIRST_PAGE = 20
        /** GIFs up to this size download and loop by themselves (old chat: AUTO_ANIMATION_BYTES). */
        const val GIF_AUTO_BYTES = 8L * 1024 * 1024
        val DEFAULT_REACTIONS = listOf("❤", "🔥", "👍", "😂", "😮", "😢", "🎉")
    }
}
