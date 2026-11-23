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
    /** wave5: pin inside the folder shown in the list ([folderId] > 0 = a TDLib folder; else the chat's own main/archive list). */
    fun setPinnedIn(chatId: Long, pinned: Boolean, folderId: Int) = setPinned(chatId, pinned)
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
    /** fixer-final: the Calls tab is shown / scrolled near its end — load (or retry) the next page of the call log. */
    fun loadMoreCalls() {}
    /** lead walk-through: the call log is being read (the Calls tab shows a spinner instead of «Звонков пока не было»). */
    val callsLoading: Boolean get() = false

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
    /** wave5: clears the cache, waits for it, then logs out (clearCache alone is async and raced the logout). */
    fun logoutClearingCache() { clearCache(); logout() }

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
    /** wave5: global search limited to [kind] 0 photos/videos, 1 links, 2 files (Search tabs «Медиа», «Ссылки», «Файлы»). */
    suspend fun searchMessagesOf(query: String, kind: Int): List<KxFound> = emptyList()
    /** Message ids in the open chat, newest first. */
    suspend fun searchInChat(chatId: Long, query: String): List<Long> = emptyList()
    /** Loads around [messageId] in the open chat; the history then contains it. */
    fun jumpTo(chatId: Long, messageId: Long) {}
    /** wave5: message the open chat [chatId] should scroll to now (loaded), 0 = none; the screen calls [jumpDone] after scrolling. */
    fun jumpTarget(chatId: Long): Long = 0
    fun jumpDone() {}
    /** wave5: the chat's synced Telegram draft once its history is loaded (null = not known yet); the composer restores it once. */
    fun chatDraft(chatId: Long): String? = null
    /** wave5: text of an own message for editing with its formatting written back as markdown (null = use the plain text). */
    fun editableText(chatId: Long, messageId: Long): String? = null
    /** wave5 (kiroX «Сохранять удалённые»): messages of [chatId] deleted after the switch was on, newest first: (title line, text). */
    suspend fun keptDeleted(chatId: Long): List<Pair<String, String>> = emptyList()
    /** wave5: «Экспорт переписки» as text into Downloads/kiroX (old UI's export); [onProgress] = messages read. null = saved. */
    suspend fun exportChat(chatId: Long, onProgress: (Int) -> Unit = {}): String? = "Недоступно"
    /** Share sheet for the last exported file; null = shown. */
    suspend fun shareLastExport(): String? = "Недоступно"
    /** wave5 (kiroX «Сохранять историю правок»): earlier texts of a message, oldest first: («до <время>», text). */
    suspend fun editHistory(chatId: Long, messageId: Long): List<Pair<String, String>> = emptyList()

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
    // wave5: the AdminRights / Restrict boards send what they show.
    /** Admin with the board's rows (0 info, 1 delete, 2 ban, 3 invite, 4 pin, 5 topics, 6 calls, 7 anonymous, 8 add admins) and title (0–16). */
    fun promoteWith(chatId: Long, userId: Long, rights: Set<Int>, title: String) = promote(chatId, userId)
    /** The admin's current rows (same numbering), null when not an admin / unknown. */
    suspend fun adminRights(chatId: Long, userId: Long): Set<Int>? = null
    /** Restricted until [untilSec] (unix, 0 = forever); [allow] rows stay allowed (0 text, 1 media, 2 stickers/GIF, 3 links, 4 polls, 5 invite). */
    fun restrictWith(chatId: Long, userId: Long, allow: Set<Int>, untilSec: Int) = restrict(chatId, userId, true)
    /** Banned until [untilSec] (0 = forever); [deleteMessages] deletes all their messages in the chat. */
    fun banFor(chatId: Long, userId: Long, untilSec: Int, deleteMessages: Boolean) = ban(chatId, userId, true)
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
    /** wave5: the code is a graphic key (dots joined), not 4 digits — boards show a pattern pad then. */
    val passcodePattern: Boolean get() = false
    /** True when [code] is right (unlocks the app). After 5 wrong codes the lock waits 30 s. */
    suspend fun checkPasscode(code: String): Boolean = false
    /** The system accepted the fingerprint. */
    fun passcodeBiometricOk() {}
    /** Seconds left of the pause after too many wrong codes (0 = codes accepted). */
    fun passcodeWait(): Int = 0

    // ---- wave6 port-security (additive) ----
    /** «Автоблокировка» delay in seconds: 0, 60, 300, 3600 or 18000. */
    val passcodeAutoLock: Int get() = 60
    /** Blank in recents and no screenshots while the code is on. */
    val passcodeHideContent: Boolean get() = false
    /** Checks the code without unlocking or counting tries (to change / turn it off). */
    suspend fun passcodeMatches(code: String): Boolean = false
    /** Turns the code on or changes it ([pattern] = graphic key). */
    suspend fun setPasscode(code: String, pattern: Boolean) {}
    fun disablePasscode() {}
    fun setPasscodeAutoLock(seconds: Int) {}
    fun setPasscodeBiometric(on: Boolean) {}
    fun setPasscodeHideContent(on: Boolean) {}
    fun lockNow() {}
    /** Cloud password: new / changed recovery e-mail (waits for its code, see [confirmRecoveryEmail]). */
    suspend fun setRecoveryEmail(password: String, email: String): String? = null
    suspend fun resendRecoveryEmailCode(): String? = null
    /** «Забыли пароль?»: sends a code to the recovery e-mail; the address pattern lands in [passwordRecoveryPattern]. */
    suspend fun requestPasswordRecovery(): String? = null
    val passwordRecoveryPattern: String get() = ""
    /** Code from the e-mail + new password ([newPassword] empty removes it). */
    suspend fun recoverPassword(code: String, newPassword: String, hint: String): String? = null
    /** Reset without e-mail: null = asked (see [KxCloudPassword.pendingResetDate]) or done; else the reason. */
    suspend fun resetPassword(): String? = null
    suspend fun cancelPasswordReset(): String? = null
    /** «Всегда разрешать» / «Никогда не разрешать» people of a privacy rule (KxPrivacyRule.key); null while it can't be read. */
    suspend fun privacyExceptions(key: String): Pair<List<KxContact>, List<KxContact>>? = null
    suspend fun setPrivacyExceptions(key: String, allow: List<Long>, never: List<Long>): String? = null
    /** «Новые чаты от всех» (off = only contacts, Telegram Premium) and «Частые собеседники». */
    val newChatsFromAll: Boolean get() = true
    fun setNewChatsFromAll(on: Boolean) {}
    val suggestFrequent: Boolean get() = true
    fun setSuggestFrequent(on: Boolean) {}

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

    // wave3-media ---------------------------------------------------------------------------
    // Viewer / Album / VideoPlayer
    /** Photos and videos of [chatId] around [aroundMessageId] (0 = newest), oldest first. */
    suspend fun chatMedia(chatId: Long, aroundMessageId: Long): List<KxMediaItem> = emptyList()
    /** The album (same media album id) that holds [messageId], oldest first; one item when it is not an album. */
    suspend fun albumOf(chatId: Long, messageId: Long): List<KxMediaItem> = emptyList()
    /** Download progress of a file (snapshot state, starts watching on first read). */
    fun fileState(fileId: Int): KxFileState = KxFileState()
    fun cancelDownload(fileId: Int) {}
    /** Copies a downloaded photo/video into the phone gallery (Pictures/kiroX); null = saved. */
    suspend fun saveToGallery(fileId: Int, video: Boolean): String? = "Недоступно"
    /** Opens the system share sheet for a downloaded file; null = shown. */
    suspend fun shareFile(fileId: Int): String? = "Недоступно"
    /** Opens a downloaded file in another app (copy via FileProvider); null = opened. */
    suspend fun openFile(fileId: Int): String? = "Недоступно"
    /** wave5: FileGuard check of a downloaded file before [openFile]; [name] = the name shown in the chat. null = not checked (check off, not downloaded, demo). */
    suspend fun fileCheck(fileId: Int, name: String = ""): KxFileCheck? = null

    // Music (old music library + VoicePlayer)
    /** Music sent in all chats (newest first); [query] filters by title / performer on Telegram's side. */
    suspend fun musicLibrary(query: String = ""): List<KxTrack> = emptyList()
    val player: KxPlayerState get() = KxPlayerState()
    fun playTracks(tracks: List<KxTrack>, index: Int) {}
    fun playerToggle() {}
    fun playerNext() {}
    fun playerPrevious() {}
    fun playerSeek(fraction: Float) {}
    fun playerShuffle() {}
    fun playerRepeat() {}
    /** Plays (or pauses) one voice message. */
    fun playVoice(fileId: Int) {}
    /** wave5: downloads the file of a chat message and puts it into Telegram's download list (AddFileToDownloads → Downloads / Files boards). */
    fun downloadMessageFile(chatId: Long, messageId: Long, fileId: Int) = download(fileId)
    /** wave5: GIFs play by themselves in chats («Автовоспроизведение GIF» in Данные и память). */
    val autoplayGifs: Boolean get() = true

    // Downloads / files
    /** Telegram's download list, newest first. */
    suspend fun downloads(query: String = ""): List<KxDownload> = emptyList()
    fun pauseDownload(fileId: Int, paused: Boolean) {}
    /** Removes files from the list and from the phone (they stay in Telegram) — after a confirm. */
    fun removeDownloads(fileIds: List<Int>) {}

    // Voice + Transcribe
    /** Voice and round video messages of a chat, newest first. */
    suspend fun voiceMessages(chatId: Long): List<KxVoiceItem> = emptyList()
    /** Marks a voice message listened (OpenMessageContent). */
    fun markListened(chatId: Long, messageId: Long) {}
    /** Telegram's speech recognition (Premium or a few free tries a week); [onPartial] gets the growing text. */
    suspend fun transcribe(chatId: Long, messageId: Long, onPartial: (String) -> Unit = {}): KxTextResult = KxTextResult(error = "Недоступно")

    // Own stories
    suspend fun storyArchive(): List<KxStoryItem> = emptyList()
    /** Stories shown on the profile page. */
    suspend fun profileStories(): List<KxStoryItem> = emptyList()
    suspend fun setStoryOnProfile(storyId: Int, on: Boolean): String? = null
    /** Up to 3 own stories pinned to the top of the profile. */
    suspend fun pinnedStoryIds(): List<Int> = emptyList()
    /** How many stories Telegram lets pin (option pinned_story_count_max). */
    suspend fun pinnedStoryMax(): Int = 3
    /** Pins exactly [ids] (≤ 3; they are put on the profile first). */
    suspend fun pinStories(ids: List<Int>): String? = null
    /** Recent photos and videos of the phone; empty without gallery access. */
    suspend fun deviceMedia(limit: Int = 60): List<KxDeviceMedia> = emptyList()
    /** Posts a 24 h story from a picked item; [audience] 0 everyone, 1 contacts, 2 close friends. */
    suspend fun postStory(media: KxDeviceMedia, caption: String, audience: Int): String? = null
    /** Sends a picked photo/video to a chat (VideoTrim / Editor «Отправить»). */
    suspend fun sendDeviceMedia(chatId: Long, media: KxDeviceMedia, caption: String = ""): String? = null
    /** Attach sheet: a picked photo/video under a spoiler, or [asFile] as a document (original quality, real name). */
    suspend fun sendDeviceMediaAs(chatId: Long, media: KxDeviceMedia, caption: String = "", spoiler: Boolean = false, asFile: Boolean = false): String? =
        sendDeviceMedia(chatId, media, caption)
    /** qa-real: several picked items as one album (caption on the first); default = one by one. Null = sent, else the error. */
    suspend fun sendDeviceAlbum(chatId: Long, items: List<KxDeviceMedia>, caption: String = "", spoiler: Boolean = false, asFile: Boolean = false): String? {
        var error: String? = null
        items.forEachIndexed { i, m -> sendDeviceMediaAs(chatId, m, if (i == 0) caption else "", spoiler, asFile)?.let { error = it } }
        return error
    }
    /** Attach «Музыка»: an audio file picked through the system ([uri] content uri) sent as music with its title/performer. */
    suspend fun sendDeviceAudio(chatId: Long, uri: String, caption: String = ""): String? = null
    /** «записывает видеосообщение» in [chatId] while the round-video recorder is open ([on] false clears it). */
    fun roundActivity(chatId: Long, on: Boolean) {}
    /** User ids of the «Близкие друзья» list. */
    suspend fun closeFriends(): Set<Long> = emptySet()
    /** Private chat with a user (created if TDLib does not know it yet); null on failure. */
    suspend fun privateChat(userId: Long): Long? = userId

    // Calls (old call service state)
    val activeCall: KxCallState? get() = null
    val groupCall: KxGroupCallState? get() = null
    /** Last finished one-to-one call (CallEnd board); null when none in this session. */
    val lastCall: KxCallState? get() = null
    fun answerCall() {}
    fun hangUp() {}
    fun muteCallMic(muted: Boolean) {}
    fun turnCallCamera(on: Boolean) {}
    fun flipCallCamera() {}
    /** WebRTC video sinks (org.webrtc.VideoSink, typed Any to keep the model free of WebRTC) for the call video; nulls detach. */
    fun attachCallVideo(remote: Any?, local: Any?) {}
    fun joinGroupCall(chatId: Long) {}
    fun leaveGroupCall() {}
    fun muteGroupMic(muted: Boolean) {}
    fun turnGroupCamera(on: Boolean) {}
    // wave6 port-calls: the v3 call UI owns calls (KxCallHost) — what the old CallScreen/GroupCallScreen did.
    /** Whether the call engine is loaded (after a cold start from a push it may still be loading; answer then). */
    val callEngineReady: Boolean get() = true
    /** Hides a finished/failed one-to-one call at once (it also goes by itself after a moment). */
    fun dismissCall() {}
    /** Closes a failed video chat (kept until dismissed, or every later join does nothing). */
    fun dismissGroupCall() {}
    fun flipGroupCamera() {}
    /** Video chat renderer (org.webrtc.VideoSink as Any): [endpoint] null = own camera; [sink] null detaches. */
    fun attachGroupVideo(endpoint: String?, sink: Any?) {}
    /** [consent]: the android.content.Intent returned by the system screen-capture dialog. */
    fun startGroupScreenShare(consent: Any) {}
    fun stopGroupScreenShare() {}
    fun raiseGroupHand(raised: Boolean) {}
    /** Video chat problem to show once (screen sharing refused…); cleared by [clearGroupCallError]. */
    val groupCallError: String? get() = null
    fun clearGroupCallError() {}
    /** [problems]: echo, noise, interruptions, distorted, silentLocal, silentRemote, dropped, video. */
    suspend fun rateCall(callId: Int, rating: Int, comment: String, problems: List<String>): String? = null
    /** Schedules a video chat in a group/channel at [startAt] (unix); 0 starts now. */
    suspend fun planVideoChat(chatId: Long, title: String, startAt: Int, rtmp: Boolean = false): String? = null

    // Traffic / space / network / sounds
    suspend fun networkStats(): KxTraffic? = null
    suspend fun resetNetworkStats(): String? = null
    /** (free, total) bytes of the phone storage. */
    suspend fun deviceSpace(): Pair<Long, Long>? = null
    suspend fun netCheck(): KxNetCheck? = null
    /** 0 = no sound, 1 = default, then Telegram's saved notification sounds. */
    suspend fun notificationSounds(): List<KxSound> = emptyList()
    /** Sound id of a scope (private / groups / channels), -1 unknown. */
    suspend fun scopeSound(key: String): Long = -1
    suspend fun changeScopeSound(key: String, soundId: Long): String? = null
    fun previewSound(soundId: Long) {}

    // ---- wave3-account (binder-account, additive). Errors: suspend funs answer null = ok / the human error text. ----

    /** kiroX-local text settings (lists joined by '\n'), keys in [KxPrefsAccount]. */
    val prefTexts: Map<String, String> get() = emptyMap()
    fun putPrefText(key: String, value: String) {}

    // Folders: [id] 0 = a new folder.
    suspend fun folderInfo(id: Int): KxFolderInfo? = null
    suspend fun saveFolder(id: Int, info: KxFolderInfo): String? = null
    /** Shareable chats of the folder (ids) for an invite link. */
    suspend fun folderShareableChats(id: Int): List<Long> = emptyList()
    suspend fun folderLinks(id: Int): List<KxInviteLink> = emptyList()
    /** Creates a t.me/addlist link; null when it failed (the reason in [error]). */
    suspend fun createFolderLink(id: Int, chatIds: List<Long>): String? = null
    fun deleteFolderLink(id: Int, link: String) {}

    // Personal channel in the own profile
    suspend fun personalChannels(): KxPersonalChannels = KxPersonalChannels()
    /** 0 removes it from the profile. */
    suspend fun choosePersonalChannel(chatId: Long): String? = null

    // Change number (sendPhoneNumberCode for a change → checkPhoneNumberCode)
    /** Sends the code to [phone]: the answer is where it went («Код отправлен по SMS») or an error starting with "!". */
    suspend fun changeNumberSendCode(phone: String): String = "!Недоступно"
    suspend fun changeNumberResend(): String = "!Недоступно"
    suspend fun changeNumberCheck(code: String): String? = null

    // Login e-mail
    /** Masked login e-mail («a***@gmail.com»), empty when none. */
    suspend fun loginEmailPattern(): String = ""
    suspend fun loginEmailSendCode(email: String): String? = null
    suspend fun loginEmailResend(): String? = null
    suspend fun loginEmailCheck(code: String): String? = null

    /** «Это я» on a new sign-in alert (ConfirmSession). Terminating is [terminateSession]. */
    fun confirmSession(id: Long) {}

    /** New Telegram terms waiting for acceptance (updateTermsOfService), null when none. */
    val termsUpdate: KxTerms? get() = null
    suspend fun acceptTerms(id: String): String? = null

    // Group anti-spam (supergroups where the user may toggle it); null = not available here.
    suspend fun antiSpam(chatId: Long): Boolean? = null
    fun switchAntiSpam(chatId: Long, on: Boolean) {}
    fun reportAntiSpamMistake(chatId: Long, messageId: Long) {}
    /** Group: new members see the earlier history. */
    fun switchHistoryForNew(chatId: Long, on: Boolean) {}

    /** Chat id of a public username (@SpamBot…), null when not found. */
    suspend fun publicChat(username: String): Long? = null

    // Contacts uploaded from this phone's address book (Telegram keeps them on the server).
    suspend fun importedContactCount(): Int? = null
    /** Deletes them from Telegram — only after the user confirmed. */
    suspend fun clearImportedContacts(): String? = null

    // Name / profile colours (TDLib accent colours)
    val accentColors: List<KxAccent> get() = emptyList()
    val profileColors: List<KxAccent> get() = emptyList()
    /** Own current colour ids: name colour and profile colour (-1 = none). */
    val myColors: Pair<Int, Int> get() = 0 to -1
    suspend fun applyNameColor(accentId: Int): String? = null
    suspend fun applyProfileColor(accentId: Int): String? = null

    // Emoji status (Premium)
    suspend fun emojiStatuses(): List<KxEmojiStatus> = emptyList()
    /** [until] unix seconds, 0 = forever; [customEmojiId] 0 removes the status. */
    suspend fun applyEmojiStatus(customEmojiId: Long, until: Int): String? = null

    // Countries for phone input
    suspend fun countries(): List<KxCountry> = emptyList()
    /** Two-letter code by IP (GetCountryCode), empty when unknown. */
    suspend fun myCountry(): String = ""
    /** Turns a basic group into a supergroup (needed for slow mode, join requests, history switch); the new chat id or null. */
    suspend fun upgradeGroup(chatId: Long): Long? = null

    // ---- wave3-chat (binder-chat, additive). suspend funs answer null = ok / the human error text, unless they return data. ----

    /** Text with options (silent, scheduled, effect, formatting, link preview). */
    fun sendWith(chatId: Long, text: String, options: KxSendOptions) { send(chatId, text, options.replyTo) }
    /** Who the user may write as in [chatId] (the current one has selected = true); empty when there is no choice. */
    suspend fun sendAsOptions(chatId: Long): List<KxSender> = emptyList()
    suspend fun chooseSendAs(chatId: Long, senderId: Long): String? = null
    /** Effects Telegram offers for messages in private chats. */
    suspend fun messageEffects(): List<KxEffect> = emptyList()
    /** Telegram chat themes by name (the name is an emoji). */
    val chatThemes: List<String> get() = emptyList()
    /** Sets the chat theme for both sides; empty name removes it. */
    suspend fun applyChatTheme(chatId: Long, name: String): String? = null
    /** Results of «@bot query» in [chatId]; null when the bot is not found or not inline. */
    suspend fun inlineQuery(chatId: Long, bot: String, query: String, offset: String = ""): KxInlineResults? = null
    fun sendInlineResult(chatId: Long, queryId: Long, resultId: String) {}
    /** Pinned chats of the main list in this order. */
    suspend fun reorderPinned(chatIds: List<Long>): String? = null
    /** How many chats may be pinned in the main list (5, Premium 10). */
    val pinnedLimit: Int get() = 5
    suspend fun messageInfo(chatId: Long, messageId: Long): KxMsgInfo? = null
    /** Who read an own message in a small group (Telegram keeps it 7 days), with reactions; newest first. */
    suspend fun messageViewers(chatId: Long, messageId: Long): List<KxViewer> = emptyList()
    suspend fun reactionPanel(chatId: Long, messageId: Long): KxReactionPanel? = null
    /** The double-tap reaction; null while unknown. */
    val quickReaction: String? get() = null
    fun changeQuickReaction(emoji: String) {}
    /** Replaces the photo/video of an own message with the local file [path]. */
    suspend fun replaceMedia(chatId: Long, messageId: Long, path: String, video: Boolean): String? = null
    /** Unread mentions ([reactions] = unread reactions) of [chatId], newest first. */
    suspend fun unreadMentions(chatId: Long, reactions: Boolean = false): List<KxFound> = emptyList()
    /** Last messages of a chat without opening it (nothing is marked read); oldest first. */
    suspend fun peekHistory(chatId: Long, limit: Int = 20): List<KxMessage> = emptyList()
    suspend fun linkPreview(text: String): KxLinkPreview? = null
    /** The last message of the local day starting at [unix]; null when nothing was sent that day. */
    suspend fun messageAtDate(chatId: Long, unix: Int): Long? = null
    /** Sticker set by its short name (t.me/addstickers/<name>) or id. */
    suspend fun stickerSetByName(name: String): KxStickerSetInfo? = null
    suspend fun stickerSetInfo(setId: Long): KxStickerSetInfo? = null
    suspend fun installedStickerSets(): List<KxStickerSetInfo> = emptyList()
    suspend fun archivedStickerSets(): List<KxStickerSetInfo> = emptyList()
    suspend fun changeStickerSet(setId: Long, installed: Boolean, archived: Boolean): String? = null
    suspend fun reorderStickerSets(ids: List<Long>): String? = null
    /** [stickerId]: KxSticker.id. */
    suspend fun favoriteSticker(stickerId: String, favorite: Boolean): String? = null
    fun sendDice(chatId: Long, emoji: String) {}
    /** Gifts on the own profile, newest first. */
    suspend fun receivedGifts(): List<KxReceivedGift> = emptyList()
    suspend fun keepGift(giftId: String, saved: Boolean): String? = null
    /** Converts a gift to stars for good — only after the user confirmed. */
    suspend fun sellGift(giftId: String): String? = null
    /** Sends a contact card ([userId] 0 = not on Telegram). */
    fun sendContact(chatId: Long, userId: Long, phone: String, first: String, last: String) {}
    /** Private note about a user (only you see it); null when unknown. */
    suspend fun userNote(userId: Long): String? = null
    suspend fun saveUserNote(userId: Long, text: String): String? = null
    /** Adds a Telegram user to contacts under your own name for them. */
    suspend fun addContactById(userId: Long, first: String, last: String, sharePhone: Boolean): String? = null
    suspend fun sharePhone(userId: Long): String? = null
    /** Telegram's report flow: call with empty [optionId] first, then with the picked option / the text. */
    suspend fun reportChat(chatId: Long, messageIds: List<Long>, optionId: String = "", text: String = ""): KxReportStep = KxReportStep(error = "Недоступно")
    /** Leaves a group or channel (the chat stays only if it is public) — only after the user confirmed. */
    suspend fun leaveChat(chatId: Long): String? = null
    /** Current auto-delete timer of [chatId] in seconds (0 = off); null when unknown. */
    suspend fun autoDeleteOf(chatId: Long): Int? = null
    /** Timer for new chats you start (0 = off); null when unknown. */
    suspend fun defaultAutoDelete(): Int? = null
    suspend fun changeDefaultAutoDelete(seconds: Int): String? = null
    suspend fun loadChatNotify(chatId: Long): KxChatNotify? = null
    suspend fun saveChatNotify(chatId: Long, value: KxChatNotify): String? = null
    suspend fun checklist(chatId: Long, messageId: Long): KxChecklist? = null
    /** Needs Premium: TDLib answers an error otherwise. */
    suspend fun sendChecklist(chatId: Long, title: String, tasks: List<String>, othersCanAdd: Boolean, othersCanMark: Boolean): String? = null
    suspend fun markChecklist(chatId: Long, messageId: Long, done: List<Int>, notDone: List<Int>): String? = null
    /** Photos and videos sent on this day in earlier years (newest year first). */
    suspend fun memories(): List<KxFound> = emptyList()
    suspend fun recommendedChats(): List<KxPublicChat> = emptyList()
    suspend fun searchPublicChats(query: String): List<KxPublicChat> = emptyList()
    suspend fun similarChats(chatId: Long): List<KxPublicChat> = emptyList()
    /** wave4: gifts shown on another user's profile (Profile «Подарки · N»). */
    suspend fun userGifts(userId: Long): List<KxReceivedGift> = emptyList()
    /** wave4: chats whose stories you hid (HiddenStories board); null while loading. */
    val hiddenStoryChats: List<KxPublicChat>? get() = emptyList()
    fun loadHiddenStories() {}
    /** wave4: hide a chat's stories from the Stories tab, or show them again. */
    fun hideStories(chatId: Long, hidden: Boolean) {}
    /** wave4: groups the user [userId] shares with you (Profile «Общие группы»). */
    suspend fun commonGroups(userId: Long): List<KxPublicChat> = emptyList()
    /** Contacts' birthdays, the nearest first. */
    suspend fun birthdays(): List<KxBirthday> = emptyList()
    /** Tap on a big animated emoji: Telegram plays the effect on both sides. */
    fun clickBigEmoji(chatId: Long, messageId: Long) {}
    /** «Показывать чувствительное» (18+): null when Telegram does not let this account change it. */
    val sensitiveAllowed: Boolean? get() = null
    fun allowSensitive(on: Boolean) {}
    /** Any text translated into [toLang]; null when it failed. */
    suspend fun translateText(text: String, toLang: String): String? = null
    /** Links, mentions, hashtags, e-mails, phone numbers… found in [text] by Telegram. */
    suspend fun textEntities(text: String): List<KxEntity> = emptyList()
    suspend fun chatFacts(chatId: Long): KxChatFacts? = null
    /** qa-real: may I post in this chat (channels: creator / admin with canPostMessages)? Used for forward targets. */
    suspend fun canPost(chatId: Long): Boolean = true
    /** Sends a photo that can be opened once (private chats only: elsewhere the answer is «Только в личных чатах»). */
    suspend fun sendViewOnce(chatId: Long, path: String): String? = null
}
