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
) : KxStore {
    private val app: android.app.Application = vm.getApplication()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override var me by mutableStateOf(KxProfile(0, ""))
        private set
    override var folders by mutableStateOf(listOf(KxFolder(0, "Все")))
        private set
    override var chats by mutableStateOf(emptyList<KxChat>())
        private set
    override var archivedChats by mutableStateOf(emptyList<KxChat>())
        private set
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
        // Chat lists + typing + stories + avatars → KxChat.
        scope.launch {
            combine(vm.chats.lists, vm.activity.linesV3, vm.stories.storyChats, pathsDaily) { lists, lines, storyChats, p -> mapLists(lists, lines, storyChats, p) }
                .flowOn(Dispatchers.Default)
                .collect { l ->
                    items = l.items; chats = l.main; archivedChats = l.archive; folders = l.folders; folderChats = l.byFolder; chatsLoading = l.loading
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

    override fun chatsIn(folderId: Int): List<KxChat> = if (folderId == 0) chats else folderChats[folderId].orEmpty()

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

    override fun setMuted(chatId: Long, muted: Boolean) {
        scope.launch { runCatching { vm.chats.setMuted(chatId, muted) } }
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
        if (!recorder.start()) { error = "Не удалось начать запись"; return }
        vm.activity.send(chatId, ChatActivity.RecordingVoice)
        // The waveform is built from levels sampled ~10 times a second.
        levelJob?.cancel()
        levelJob = scope.launch {
            while (recorder.isRecording) {
                recorder.sampleLevel()
                vm.activity.send(chatId, ChatActivity.RecordingVoice) // repeated; the repository throttles it
                delay(100)
            }
        }
    }

    override fun recordStop(chatId: Long, send: Boolean, seconds: Int) {
        levelJob?.cancel(); levelJob = null
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
        // The repository forwards from the open chat.
        if (history.chatId == fromChatId && messageIds.isNotEmpty()) vm.messages.forward(toChatId, messageIds)
    }

    override fun saveToFavorites(chatId: Long, messageIds: List<Long>) {
        if (history.chatId != chatId || messageIds.isEmpty()) return
        scope.launch { runCatching { vm.users.savedMessagesChatId() }.onSuccess { vm.messages.forward(it, messageIds) }.onFailure { showError(it) } }
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
        listOfNotNull(storyId, next).forEach { id ->
            fetchStory(peerId, id)
            storyCache.value[peerId to id]?.video?.let { ref -> vm.files.download(ref); watchFile(ref) }
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
        vm.voip.start(peerId, video)
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

    private fun mapLists(lists: TelegramChatLists, lines: Map<Long, String>, storyChats: List<TelegramStoryChat>, known: Map<Int, String>): Lists {
        val withStory = storyChats.filter { !it.isSelf && it.storyIds.isNotEmpty() }.map { it.chatId }.toHashSet()
        fun map(list: List<TelegramChatItem>) = list.map { it.toKx(lines[it.id], it.id in withStory, known) }
        val main = map(lists.main.chats)
        val byFolder = lists.folders.associate { it.id to map(lists.folderChats[it.id]?.chats.orEmpty()) }
        fun unread(list: List<KxChat>) = list.count { (it.unread > 0 || it.markedUnread) && !it.muted }
        val folders = listOf(KxFolder(0, "Все", unread(main))) + lists.folders.map { KxFolder(it.id, it.title, unread(byFolder[it.id].orEmpty())) }
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
        val autoDownload = msgKind == KxMsgKind.Photo || msgKind == KxMsgKind.Sticker
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
            media = if (autoDownload) autoPath(mainFile, known) else knownPath(mainFile, known),
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

    private fun fetchStory(chatId: Long, storyId: Int) {
        if (!storyAsked.add(chatId to storyId)) return
        scope.launch {
            vm.stories.story(chatId, storyId)?.let { st -> storyCache.update { it + ((chatId to storyId) to st) } }
                ?: storyAsked.remove(chatId to storyId) // failed (network): asked again on the next list update
        }
    }

    private fun TelegramUserProfile.toKx() = KxProfile(
        id = id, name = name, username = username.orEmpty(),
        phone = if (phoneNumber.isBlank()) "" else runCatching { com.kirox.app.phone.PhoneNumberEngine().formatInternational(phoneNumber) }.getOrDefault("+" + phoneNumber.trimStart('+')),
        bio = bio, status = "в сети", premium = isPremium, avatar = avatarOf(photo, paths.value),
    )

    private fun connectionText(state: TelegramConnection): String? = when (state) {
        TelegramConnection.WaitingForNetwork -> "Ожидание сети…"
        TelegramConnection.ConnectingToProxy -> "Подключение к прокси…"
        TelegramConnection.Connecting -> "Соединение…"
        TelegramConnection.Updating -> "Обновление…"
        TelegramConnection.Ready -> null
    }

    private companion object {
        const val DRAFT_DELAY_MS = 500L
        const val FIRST_PAGE = 20
        val DEFAULT_REACTIONS = listOf("❤", "🔥", "👍", "😂", "😮", "😢", "🎉")
    }
}
