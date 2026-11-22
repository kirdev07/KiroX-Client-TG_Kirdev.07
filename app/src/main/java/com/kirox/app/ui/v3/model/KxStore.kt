package com.kirox.app.ui.v3.model

/**
 * Everything the v3 screens read and do. Values are Compose snapshot state in both
 * implementations, so screens recompose when they change. Getters are called during composition:
 * they must be cheap and never block (no TDLib queries inside).
 *
 * - KxDemoStore: demo data for the preview app and tests (never touches TDLib).
 * - TDLib store (ui/v3/data): the real account; reads TelegramViewModel flows into snapshot state.
 *
 * Screens never call TDLib directly: a new action goes here first, then into both stores.
 */
interface KxStore {
    val me: KxProfile
    /** Accounts on this phone (Telegram allows several); the first is the current one. */
    val accounts: List<KxProfile> get() = listOf(me)
    /** First item is the main list (id 0, «Все»); the rest are the user's Telegram folders. */
    val folders: List<KxFolder>
    val chats: List<KxChat>
    val archivedChats: List<KxChat>
    val stories: List<KxStoryPeer>
    val calls: List<KxCall>
    val contacts: List<KxContact>
    /** Chats-tab badge: Telegram's unread counter for the main list (unmuted). */
    val unreadTotal: Int
    val missedCalls: Int
    /** "Соединение…" / "Обновление…" or null when online. */
    val connection: String?
    /** First page of the chat list still loading. */
    val chatsLoading: Boolean get() = false
    /** Last failed action in human Russian («Нельзя закрепить больше 5 чатов»), null when none. */
    val error: String? get() = null
    fun clearError() {}

    fun chatsIn(folderId: Int): List<KxChat>
    /** Called when the list is scrolled near its end. */
    fun loadMoreChats(folderId: Int) {}
    fun header(chatId: Long): KxChatHeader
    /** Oldest first. */
    fun messages(chatId: Long): List<KxMessage>
    fun historyLoading(chatId: Long): Boolean = false
    /** True when the very first message of the chat is loaded. */
    fun historyComplete(chatId: Long): Boolean = true

    // Chat list
    fun openChat(chatId: Long) {}
    fun closeChat(chatId: Long) {}
    fun setPinned(chatId: Long, pinned: Boolean)
    fun setMuted(chatId: Long, muted: Boolean)
    fun setArchived(chatId: Long, archived: Boolean)
    /** Also clears unread mentions and reactions, like Telegram's «Прочитать». */
    fun markRead(chatId: Long)
    fun markUnread(chatId: Long) {}
    fun clearHistory(chatId: Long, forEveryone: Boolean = false) {}
    /** Private chat: forEveryone deletes for both; group/channel: leave, or delete for all when allowed. */
    fun deleteChat(chatId: Long, forEveryone: Boolean = false)

    // Messages
    fun send(chatId: Long, text: String, replyTo: Long? = null)
    /** Hold-to-record started (voice, or circle when [video]). The TDLib store starts the real recorder. */
    fun recordStart(chatId: Long, video: Boolean) {}
    /** Released ([send]) or cancelled. The store sends what was recorded. */
    fun recordStop(chatId: Long, send: Boolean, seconds: Int) {}
    /** [path] is the recorded file (null in the demo, which only simulates recording). */
    fun sendVoice(chatId: Long, seconds: Int, path: String? = null, waveform: ByteArray? = null) {}
    fun sendVideoNote(chatId: Long, seconds: Int, path: String? = null, length: Int = 0) {}
    fun edit(chatId: Long, messageId: Long, text: String)
    fun delete(chatId: Long, messageIds: List<Long>, forEveryone: Boolean)
    /** Toggles: tapping your own reaction again removes it. */
    fun react(chatId: Long, messageId: Long, emoji: String)
    fun availableReactions(chatId: Long, messageId: Long): List<String> = listOf("❤️", "🔥", "👍", "😂", "😮", "😢", "🎉")
    fun pin(chatId: Long, messageId: Long, pinned: Boolean) {}
    fun forward(fromChatId: Long, messageIds: List<Long>, toChatId: Long) {}
    fun saveToFavorites(chatId: Long, messageIds: List<Long>) {}
    fun translate(chatId: Long, messageId: Long, show: Boolean) {}
    fun retrySend(chatId: Long, messageId: Long) {}
    fun loadOlder(chatId: Long) {}
    fun loadNewer(chatId: Long) {}
    fun setDraft(chatId: Long, text: String) {}
    /** Starts downloading a file (avatar/media); the path appears in the models when ready. */
    fun download(fileId: Int) {}

    // Profiles
    /** Peer of a private chat (or the user behind a contact). */
    fun peer(chatId: Long): KxPeer = header(chatId).let { KxPeer(chatId, chatId, it.title, it.status, it.online, muted = it.muted, avatar = it.avatar, colorIndex = it.colorIndex) }
    fun shared(chatId: Long, tab: KxSharedTab): List<KxSharedItem> = emptyList()
    fun block(userId: Long, blocked: Boolean) {}

    // Stories
    fun viewStory(peerId: Long, storyId: Int) {}
    fun reactStory(peerId: Long, storyId: Int, emoji: String) {}
    fun replyStory(peerId: Long, storyId: Int, text: String) {}

    // Calls
    fun call(peerId: Long, video: Boolean) {}
}
