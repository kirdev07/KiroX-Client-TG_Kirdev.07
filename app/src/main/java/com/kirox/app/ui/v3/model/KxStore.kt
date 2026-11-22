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
    fun availableReactions(chatId: Long, messageId: Long): List<String> = listOf("❤", "🔥", "👍", "😂", "😮", "😢", "🎉")
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
    /** The story viewer closed: Telegram stops counting it as open. */
    fun closeStory() {}

    // Calls
    fun call(peerId: Long, video: Boolean) {}

    // ---- Round 5 (binder, additive). Lists load on first read and fill in; suspend funs answer
    // null on success / a human Russian error text, unless they return data. ----

    // Devices
    val sessions: List<KxSession> get() = emptyList()
    /** «Автоматически завершать сеансы» in days. */
    val sessionTtlDays: Int get() = 180
    fun refreshSessions() {}
    fun terminateSession(id: Long) {}
    /** TDLib refuses this within 24 h of signing in here: the error appears in [error]. */
    fun terminateOtherSessions() {}
    fun setSessionTtl(days: Int) {}
    /** QR on another device (tg://login?token=…): signs that device in. */
    suspend fun confirmQrLogin(link: String): String? = null

    // Privacy & security
    val privacy: List<KxPrivacyRule> get() = emptyList()
    /** Changes only the base rule; exceptions stay. */
    fun setPrivacy(key: String, level: Int) {}
    val showReadDate: Boolean get() = true
    fun setShowReadDate(on: Boolean) {}
    val archiveNewChats: Boolean get() = false
    fun setArchiveNewChats(on: Boolean) {}
    val blockedCount: Int get() = 0
    val blocked: List<KxContact> get() = emptyList()
    val cloudPassword: KxCloudPassword? get() = null
    /** Checks the cloud password (null = right). */
    suspend fun checkPassword(password: String): String? = null
    /** Sets, changes ([old] = current) or removes ([new] empty) the cloud password. */
    suspend fun setCloudPassword(old: String, new: String, hint: String, email: String?): String? = null
    suspend fun confirmRecoveryEmail(code: String): String? = null

    // Notifications
    val notifyScopes: List<KxNotifyScope> get() = emptyList()
    fun setNotifyScope(key: String, enabled: Boolean, preview: Boolean = true) {}

    // Storage
    val storage: KxStorage? get() = null
    fun refreshStorage() {}
    /** [types]: titles from KxStorage.byType; empty = everything. Files stay in Telegram. */
    fun clearCache(types: Set<String> = emptySet()) {}
    val keepMediaOptions: List<String> get() = listOf("3 дня", "1 неделя", "1 месяц", "Всегда")
    val keepMedia: Int get() = 3
    fun setKeepMedia(index: Int) {}

    // kiroX features (ghost mode etc.): keys are the KiroxTweaks names (ghost, ghostNoRead, keepDeleted…).
    val tweaks: Map<String, Boolean> get() = emptyMap()
    fun setTweak(key: String, on: Boolean) {}

    // Accounts ([accounts] above; the current one first)
    /** [index] in [accounts]. */
    fun switchAccount(index: Int) {}
    /** Opens a new account slot: the app goes to the sign-in screens. */
    fun addAccount() {}
    fun logout() {}

    // Premium
    val starsBalance: Long? get() = null
    val giftCatalog: List<KxGift> get() = emptyList()
    /** Spends stars — call only after the user confirmed the price. */
    suspend fun sendGift(userId: Long, giftId: Long, text: String, private: Boolean): String? = null

    // Folders
    fun createFolder(title: String, icon: String, chatIds: List<Long>) {}
    fun deleteFolder(id: Int) {}
    fun reorderFolders(ids: List<Int>) {}

    // Profile & contacts
    suspend fun checkUsername(name: String): KxUsernameState = KxUsernameState.Free
    /** Empty removes the username. */
    suspend fun setUsername(name: String): String? = null
    /** null when nobody uses Telegram with this number (or hides it). */
    suspend fun findByPhone(phone: String): KxContact? = null
    /** Returns the user id, or null when the number is not on Telegram. */
    suspend fun importContact(first: String, last: String, phone: String): Long? = null

    // New chats
    suspend fun createGroup(title: String, userIds: List<Long>, about: String = ""): Long? = null
    suspend fun createChannel(title: String, about: String, username: String? = null): Long? = null
    suspend fun createSecretChat(userId: Long): Long? = null

    // Search
    suspend fun searchChats(query: String): List<KxChat> = emptyList()
    suspend fun searchMessages(query: String): List<KxFound> = emptyList()
    /** Message ids in the open chat, newest first. */
    suspend fun searchInChat(chatId: Long, query: String): List<Long> = emptyList()
    /** Loads around [messageId] in the open chat; the history then contains it. */
    fun jumpTo(chatId: Long, messageId: Long) {}

    // Chat tools (open chat)
    fun sendQuote(chatId: Long, text: String, replyTo: Long, quote: String) {}
    fun sendSilent(chatId: Long, text: String) {}
    /** [at]: unix time, or -1 = when the other person comes online. */
    fun sendScheduled(chatId: Long, text: String, at: Int) {}
    suspend fun scheduled(chatId: Long): List<KxScheduled> = emptyList()
    fun sendScheduledNow(chatId: Long, messageId: Long) {}
    fun deleteScheduled(chatId: Long, messageId: Long) {}
    /** [quizCorrect]: index of the right answer for a quiz, null for a poll. */
    fun sendPoll(chatId: Long, question: String, options: List<String>, anonymous: Boolean, multiple: Boolean, quizCorrect: Int? = null) {}
    /** Empty [options] retracts the vote. */
    fun votePoll(chatId: Long, messageId: Long, options: List<Int>) {}
    fun sendLocation(chatId: Long, latitude: Double, longitude: Double) {}
    fun sendPhoto(chatId: Long, path: String, caption: String = "", spoiler: Boolean = false) {}
    fun sendFile(chatId: Long, path: String, caption: String = "") {}
    fun forward(fromChatId: Long, messageIds: List<Long>, toChatId: Long, hideSender: Boolean, dropCaption: Boolean) {}
    suspend fun messageLink(chatId: Long, messageId: Long): String? = null
    /** Inline button under a bot message: returns the bot's answer text or a URL to open, null when nothing to show. */
    suspend fun pressButton(chatId: Long, messageId: Long, row: Int, column: Int): String? = null
    /** Self-destruct / auto-delete timer in seconds (0 = off). */
    fun setAutoDelete(chatId: Long, seconds: Int) {}

    // Forums & comments
    suspend fun forumTopics(chatId: Long): List<KxTopic> = emptyList()
    /** Opens the topic as the conversation: messages(chatId) then holds the topic. */
    fun openTopic(chatId: Long, topicId: Int, title: String) {}
    suspend fun createTopic(chatId: Long, title: String): Int? = null
    /** Opens a channel post's comments; messages(discussionChatId) follow — use header/messages of [historyChatId]. */
    fun openComments(chatId: Long, postId: Long) {}
    /** Chat whose history is open right now (the discussion group after openComments), 0 = none. */
    val historyChatId: Long get() = 0

    // Stickers & GIFs
    suspend fun stickerSets(): List<KxStickerSet> = emptyList()
    suspend fun stickers(setId: Long): List<KxSticker> = emptyList()
    suspend fun recentStickers(): List<KxSticker> = emptyList()
    suspend fun favoriteStickers(): List<KxSticker> = emptyList()
    suspend fun savedGifs(): List<KxSticker> = emptyList()
    suspend fun searchGifs(chatId: Long, query: String): List<KxSticker> = emptyList()
    /** [stickerId]: KxSticker.id from the lists above. */
    fun sendSticker(chatId: Long, stickerId: String, replyTo: Long? = null) {}

    // Saved Messages
    suspend fun savedTags(): List<KxSavedTag> = emptyList()

    // ---- Round 5b: groups & channels (actions report failures in [error]) ----
    suspend fun groupInfo(chatId: Long): KxGroupInfo? = null
    fun setChatTitle(chatId: Long, title: String) {}
    fun setChatDescription(chatId: Long, text: String) {}
    /** 0 turns slow mode off. */
    fun setSlowMode(chatId: Long, seconds: Int) {}
    fun setJoinByRequest(chatId: Long, on: Boolean) {}
    /** null = every reaction, empty = none, else only these. */
    fun setAllowedReactions(chatId: Long, emoji: List<String>?) {}
    /** 0 clears the group sticker set. */
    fun setGroupStickerSet(chatId: Long, setId: Long) {}
    fun setProtectedContent(chatId: Long, on: Boolean) {}
    suspend fun members(chatId: Long): List<KxMember> = emptyList()
    fun promote(chatId: Long, userId: Long) {}
    fun demote(chatId: Long, userId: Long) {}
    /** Read-only member (cannot write). */
    fun restrict(chatId: Long, userId: Long, restricted: Boolean) {}
    fun ban(chatId: Long, userId: Long, banned: Boolean) {}
    fun removeMember(chatId: Long, userId: Long) {}
    suspend fun inviteLinks(chatId: Long): List<KxInviteLink> = emptyList()
    /** [days] 0 = no expiry, [limit] 0 = no limit. */
    suspend fun createInviteLink(chatId: Long, name: String, days: Int, limit: Int, byRequest: Boolean): KxInviteLink? = null
    fun revokeInviteLink(chatId: Long, link: String) {}
    suspend fun joinRequests(chatId: Long): List<KxJoinRequest> = emptyList()
    fun processJoinRequest(chatId: Long, userId: Long, approve: Boolean) {}
    suspend fun adminLog(chatId: Long, query: String = ""): List<KxLogEntry> = emptyList()
    suspend fun boostStatus(chatId: Long): KxBoostStatus? = null
    suspend fun chatStats(chatId: Long): KxStats? = null
    suspend fun postStats(chatId: Long, messageId: Long): KxPostStats? = null
    /** All pinned message ids of the open chat, newest last. */
    fun pinnedIds(chatId: Long): List<Long> = emptyList()

    // Proxy
    val proxies: List<KxProxy> get() = emptyList()
    fun addProxy(server: String, port: Int, type: String, secret: String) {}
    fun enableProxy(id: Int) {}
    fun disableProxy() {}
    fun removeProxy(id: Int) {}

    // Account & profile
    /** «Удалить аккаунт, если меня не было»: 30, 91, 182, 365, 548 or 730 days. */
    val accountTtlDays: Int get() = 365
    fun setAccountTtl(days: Int) {}
    /** Deletes the Telegram account for good — only after the user confirmed. */
    suspend fun deleteAccount(reason: String, password: String): String? = null
    suspend fun updateProfile(first: String, last: String, bio: String): String? = null
    /** [year] 0 = without year; [day] 0 removes the birthday. */
    suspend fun setBirthday(day: Int, month: Int, year: Int): String? = null
    /** Premium emoji status by custom emoji id; 0 removes it. */
    fun setEmojiStatus(customEmojiId: Long) {}
    /** Profile photo history, newest (current) first: id = photo id, sub = date («12 августа 2026»),
     *  media = path (null until downloaded), fileId to download, look = 1 for a video avatar. */
    suspend fun profilePhotos(userId: Long): List<KxSharedItem> = emptyList()

    // ---- Round 6 (binder 2, additive) ----

    // kiroX-local settings: see KxPrefs for the keys and value meanings.
    val prefs: Map<String, Int> get() = KxPrefs.defaults
    fun putPref(key: String, value: Int) {}
    /** Own names of the dock tabs by index (0 Контакты, 1 Звонки, 2 Чаты, 3 Истории, 4 Настройки). */
    val tabNames: Map<Int, String> get() = emptyMap()
    /** Blank brings the normal name back. */
    fun renameTab(index: Int, name: String) {}

    // Local passcode (kiroX lock screen)
    /** A code is set; false = nothing to unlock (the keypad stays off). */
    val passcodeSet: Boolean get() = false
    /** «Разблокировка отпечатком» is allowed. */
    val passcodeBiometric: Boolean get() = true
    /** True when [code] is right (unlocks the app). After 5 wrong codes the lock waits 30 s. */
    suspend fun checkPasscode(code: String): Boolean = false
    /** The system accepted the fingerprint. */
    fun passcodeBiometricOk() {}
    /** Seconds left of the pause after too many wrong codes (0 = codes accepted). */
    fun passcodeWait(): Int = 0

    // Own profile photos (ids from [profilePhotos])
    /** Makes an older photo the main one again. */
    suspend fun setMainPhoto(photoId: Long): String? = null
    /** Deletes a profile photo for good — only after the user confirmed. */
    suspend fun deletePhoto(photoId: Long): String? = null

    // Channels & groups
    /** Subscribes to a public channel / joins a public group (KxComposer.Join). Leaving is [deleteChat]. */
    fun joinChat(chatId: Long) {}
    /** Invite link preview (CheckChatInviteLink); null when the link is invalid or expired. */
    suspend fun inviteLinkInfo(link: String): KxInviteInfo? = null
    /** Joins by invite link: null = joined, "request" = join request sent (admins decide), else the error text. */
    suspend fun joinByInviteLink(link: String): String? = null

    // Message requests: chats a stranger started (main list + archive, newest first)
    suspend fun messageRequests(): List<KxRequest> = emptyList()
    /** Keeps the chat and drops the stranger bar. Delete = [deleteChat], block = [block] + [deleteChat]. */
    fun acceptRequest(chatId: Long) {}

    /** Message text translated by Telegram into [toLang] (ISO code); null when it failed (not in secret chats). */
    suspend fun translateMessage(chatId: Long, messageId: Long, toLang: String): String? = null

    // Own stories
    /** Who viewed own story [storyId] (newest first, up to 100) with their reactions. */
    suspend fun storyViewers(storyId: Int): List<KxStoryViewer> = emptyList()
    /** Own story totals from Telegram: KxPostStats.views and .reactions (forwards/comments 0); null when unknown. */
    suspend fun storyStats(storyId: Int): KxPostStats? = null
    /** Deletes an own story — only after the user confirmed. */
    suspend fun deleteStory(storyId: Int): String? = null
}
