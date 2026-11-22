package com.kirox.app.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/**
 * History and actions for the one chat that is open on screen. Dispatcher-confined like the other
 * repositories; message objects are kept raw and mapped on (coalesced) publish.
 */
class TelegramMessageRepository(
    private val client: TelegramClient,
    private val chats: TelegramChatRepository,
    private val users: TelegramUserRepository
) {
    private var chatId = 0L
    private val messages = sortedMapOf<Long, TdApi.Message>()
    private var loadingOlder = false
    private var reachedStart = false
    private var lastReadOutbox = 0L
    private var publishScheduled = false
    private val viewed = HashSet<Long>()
    /** Messages of the open chat deleted by their author but kept on screen («Сохранять удалённые»). */
    private val deletedIds = HashSet<Long>()
    /** Deletions made from kiroX: they disappear at once and are never kept. */
    private val ownDeletes = HashSet<Long>()
    private var savedChat = false
    /** Comment thread or forum topic shown instead of the whole chat; null for the chat itself. */
    private var topic: TdApi.MessageTopic? = null
    private var threadTitle: String? = null
    /** Root of a comment thread in the discussion group, for GetMessageThreadHistory. */
    private var threadRoot = 0L

    /** Pinned message ids of the open chat, newest last. */
    private val pinnedIds = sortedSetOf<Long>()
    /** Pinned messages not in the loaded history. */
    private val pinnedCache = HashMap<Long, TdApi.Message>()
    /** Message to scroll to once it is loaded, see [jumpTo]. */
    private var jumpTarget = 0L
    private var jumpPagesLeft = 0

    private val _history = MutableStateFlow(TelegramChatHistory())
    val history: StateFlow<TelegramChatHistory> = _history.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    /** Last failed action, for a toast; cleared by [clearError]. */
    val error: StateFlow<String?> = _error.asStateFlow()

    init { client.addUpdateHandler(::handle) }

    private var resumeChatId = 0L
    /** Draft of the open thread or topic (the chat's own draft is in Chat.draftMessage). */
    private var threadDraft = ""
    /** Bumped on every open/close, so a late GetMessageThread/GetForumTopic answer is ignored. */
    private var threadRequest = 0

    fun open(id: Long) = client.post {
        if (chatId == id && topic == null) return@post
        threadRequest++
        topic = null; threadTitle = null; threadRoot = 0; threadDraft = ""
        openLoaded(id)
    }

    /**
     * Opens the comments of channel post [postId]: they live in the channel's discussion group as a
     * thread, which TDLib resolves with GetMessageThread. Errors (comments off) go to [error].
     */
    fun openComments(channelChatId: Long, postId: Long) = client.post {
        // No chat id until the thread is resolved, so nothing (a draft, a read mark) lands in the channel.
        _history.value = TelegramChatHistory(threadTitle = "Комментарии")
        val request = ++threadRequest
        client.send(TdApi.GetMessageThread(channelChatId, postId)) { result ->
            if (request != threadRequest) return@send
            when (result) {
                is TdApi.MessageThreadInfo -> {
                    topic = TdApi.MessageTopicThread(result.messageThreadId)
                    threadTitle = "Комментарии"
                    threadRoot = result.messageThreadId
                    threadDraft = draftText(result.draftMessage)
                    openLoaded(result.chatId)
                }
                is TdApi.Error -> _error.value = humanError(result)
            }
        }
    }

    /** Opens one topic of a forum supergroup as its own conversation. */
    fun openTopic(chatId: Long, topicId: Int, title: String) = client.post {
        _history.value = TelegramChatHistory(threadTitle = title)
        val request = ++threadRequest
        // The topic's own draft comes with the topic; load it before the history is shown.
        client.send(TdApi.GetForumTopic(chatId, topicId)) { result ->
            if (request != threadRequest) return@send
            topic = TdApi.MessageTopicForum(topicId)
            threadTitle = title
            threadRoot = 0
            threadDraft = draftText((result as? TdApi.ForumTopic)?.draftMessage)
            openLoaded(chatId)
        }
    }

    /** Votes for options at [indices] (empty retracts the vote). */
    fun vote(messageId: Long, indices: List<Int>) = act { id -> TdApi.SetPollAnswer(id, messageId, indices.toIntArray()) }

    fun stopPoll(messageId: Long) = act { id -> TdApi.StopPoll(id, messageId, null) }

    /** Sends a poll; [correctOption] makes it a quiz with that right answer. */
    fun sendPoll(question: String, options: List<String>, anonymous: Boolean, multipleAnswers: Boolean, correctOption: Int?) = act { id ->
        val type: TdApi.InputPollType = if (correctOption != null) TdApi.InputPollTypeQuiz(intArrayOf(correctOption), TdApi.FormattedText("", emptyArray()), null)
        else TdApi.InputPollTypeRegular(false)
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessagePoll(
            TdApi.FormattedText(question, emptyArray()),
            options.map { TdApi.InputPollOption(TdApi.FormattedText(it, emptyArray()), null) }.toTypedArray(),
            null, null, anonymous, multipleAnswers && correctOption == null, true, false, emptyArray(), false, false, type, 0, 0, false
        ))
    }

    /** Presses a bot's inline button; returns the bot's answer (text to show, URL to open) or null. */
    suspend fun pressButton(messageId: Long, button: TelegramButton): Pair<String, String?>? {
        val id = chatId.takeIf { it != 0L } ?: return null
        val data = button.data ?: return null
        val answer = client.query(TdApi.GetCallbackQueryAnswer(id, messageId, TdApi.CallbackQueryPayloadData(data)))
        return answer.text.orEmpty() to answer.url?.takeIf { it.isNotBlank() }
    }

    /**
     * Opens a bot's mini app: from a button of [messageId] (the bot is the sender or the «via» bot)
     * or, with [botUserId], from the bot's menu. Null when Telegram refuses.
     */
    suspend fun openWebApp(url: String, messageId: Long? = null, botUserId: Long? = null): WebAppLaunch? {
        val id = chatId.takeIf { it != 0L } ?: return null
        val bot = botUserId ?: onDispatcherMessage(messageId)?.let { message ->
            message.viaBotUserId.takeIf { it != 0L } ?: (message.senderId as? TdApi.MessageSenderUser)?.userId
        } ?: return null
        // Colors of the dark Fusion look, so the mini app opens in a matching theme.
        val theme = TdApi.ThemeParameters(
            0xFF101418.toInt(), 0xFF171C22.toInt(), 0xFF101418.toInt(), 0xFF171C22.toInt(), 0xFF171C22.toInt(), 0xFF2A313A.toInt(),
            0xFFFFFFFF.toInt(), 0xFF8E9BFF.toInt(), 0xFF8E9BFF.toInt(), 0xFF9AA3AD.toInt(), 0xFFFF6B6B.toInt(), 0xFF6B7380.toInt(),
            0xFF8E9BFF.toInt(), 0xFF6C4CE0.toInt(), 0xFFFFFFFF.toInt()
        )
        return runCatching {
            client.query(TdApi.OpenWebApp(id, bot, url, topic, null, TdApi.WebAppOpenParameters(theme, "kiroX", TdApi.WebAppOpenModeFullSize())))
        }.getOrNull()?.let { WebAppLaunch(it.launchId, it.url.url) }
    }

    fun closeWebApp(launchId: Long) { client.send(TdApi.CloseWebApp(launchId)) }

    /** The menu button of the open bot chat, when the bot has a mini app there. */
    suspend fun botMenu(): BotMenu? {
        val id = chatId.takeIf { it != 0L } ?: return null
        val userId = (chats.chat(id)?.type as? TdApi.ChatTypePrivate)?.userId ?: return null
        val button = runCatching { client.query(TdApi.GetUserFullInfo(userId)).botInfo?.menuButton }.getOrNull() ?: return null
        return button.url.takeIf { it.isNotBlank() }?.let { BotMenu(userId, button.text.ifBlank { "Меню" }, it) }
    }

    private suspend fun onDispatcherMessage(messageId: Long?): TdApi.Message? {
        val id = messageId ?: return null
        val deferred = kotlinx.coroutines.CompletableDeferred<TdApi.Message?>()
        client.post { deferred.complete(messages[id]) }
        return deferred.await()
    }

    /** Message text translated to [language] (ISO code, e.g. "ru") by Telegram. */
    suspend fun translate(messageId: Long, language: String): String? {
        val id = chatId.takeIf { it != 0L } ?: return null
        return runCatching { client.query(TdApi.TranslateMessageText(id, messageId, language, "")).text }.getOrNull()
    }

    /** Topics of a forum supergroup, pinned first as Telegram orders them. */
    /** New topic in forum [chatId]; returns its id. */
    suspend fun createForumTopic(chatId: Long, name: String): Int =
        client.query(TdApi.CreateForumTopic(chatId, name, false, null)).forumTopicId

    suspend fun forumTopics(chatId: Long): List<TelegramForumTopic> = runCatching {
        client.query(TdApi.GetForumTopics(chatId, "", 0, 0, 0, FORUM_TOPICS_LIMIT)).topics.map { t ->
            TelegramForumTopic(
                id = t.info.forumTopicId, title = t.info.name, isClosed = t.info.isClosed, isPinned = t.isPinned,
                unreadCount = t.unreadCount, preview = messagePreview(t.lastMessage), date = t.lastMessage?.date ?: t.info.creationDate,
                isGeneral = t.info.isGeneral
            )
        }
    }.getOrDefault(emptyList())

    private fun openLoaded(id: Long) {
        if (chatId != 0L) client.send(TdApi.CloseChat(chatId))
        chatId = id
        messages.clear(); viewed.clear(); deletedIds.clear()
        loadingOlder = false; reachedStart = false
        pinnedIds.clear(); pinnedCache.clear(); jumpTarget = 0; jumpPagesLeft = 0
        lastReadOutbox = chats.chat(id)?.lastReadOutboxMessageId ?: 0
        _history.value = TelegramChatHistory(chatId = id)
        client.send(TdApi.OpenChat(id))
        requestOlder()
        loadPinned(id)
        publish()
    }

    /** All pinned messages of the chat (Telegram allows many); the newest one is shown in the bar. */
    private fun loadPinned(id: Long) {
        client.send(TdApi.SearchChatMessages(id, topic, "", null, 0, 0, PINNED_LIMIT, TdApi.SearchMessagesFilterPinned())) { result ->
            if (id != chatId || result !is TdApi.FoundChatMessages) return@send
            result.messages.filterNotNull().forEach { message ->
                pinnedIds += message.id
                // Keep the pinned message itself for the bar even when it is far up in the history.
                if (message.id !in messages) pinnedCache[message.id] = message
            }
            schedulePublish()
        }
    }
    /** Emoji reactions this chat allows on [messageId], without Premium-only ones; null on failure. */
    suspend fun availableReactions(messageId: Long): List<String>? {
        val id = chatId.takeIf { it != 0L } ?: return null
        return runCatching {
            val available = client.query(TdApi.GetMessageAvailableReactions(id, messageId, 8))
            (available.topReactions + available.recentReactions + available.popularReactions)
                .filter { !it.needsPremium }
                .mapNotNull { reactionKey(it.type) }
                .distinct()
                .take(8)
        }.getOrNull()
    }

    fun pin(messageId: Long) = act { id -> TdApi.PinChatMessage(id, messageId, false, false) }
    fun unpin(messageId: Long) = act { id -> TdApi.UnpinChatMessage(id, messageId) }

    /**
     * Scrolls the open chat to [messageId]: loads older pages until it is in the list (up to
     * [JUMP_PAGES] pages), then reports it in [TelegramChatHistory.jumpTo] for the screen to scroll.
     */
    fun jumpTo(messageId: Long) = client.post {
        if (chatId == 0L) return@post
        jumpTarget = messageId
        jumpPagesLeft = JUMP_PAGES
        continueJump()
    }

    /** The screen has scrolled to the target. */
    fun jumpDone() = client.post { jumpTarget = 0; schedulePublish() }

    private fun continueJump() {
        if (jumpTarget == 0L) return
        when {
            jumpTarget in messages -> schedulePublish()
            reachedStart || jumpPagesLeft <= 0 -> { jumpTarget = 0; _error.value = "Сообщение слишком далеко в истории"; schedulePublish() }
            else -> { jumpPagesLeft--; requestOlder() }
        }
    }

    /** Server-side search in the open chat, newest first; [fromMessageId] 0 for the first page. */
    suspend fun search(query: String, fromMessageId: Long = 0): Pair<List<TelegramMessage>, Long> {
        val id = chatId.takeIf { it != 0L } ?: return emptyList<TelegramMessage>() to 0L
        val found = client.query(TdApi.SearchChatMessages(id, topic, query, null, fromMessageId, 0, SEARCH_PAGE, null))
        val list = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            // Mapping reads repository state, so it runs on the TDLib dispatcher.
            client.post { continuation.resumeWith(Result.success(found.messages.filterNotNull().map { it.toModel() })) }
        }
        return list to found.nextFromMessageId
    }

    fun close(id: Long) = client.post {
        if (resumeChatId == id) { resumeChatId = 0; _history.value = TelegramChatHistory() }
        // A thread or topic of this chat belongs to its own screen, which leaves it with closeThread;
        // the chat screen going away during the transition must not close it.
        if (chatId != id || topic != null) return@post
        client.send(TdApi.CloseChat(id))
        chatId = 0; messages.clear(); viewed.clear(); deletedIds.clear()
        _history.value = TelegramChatHistory()
    }

    /** Leaves a comment thread or forum topic (its screen closed); a plain open chat is left alone. */
    fun closeThread() = client.post {
        threadRequest++
        if (topic == null) return@post
        // After a background pause chatId is 0 and the thread waits in resumeChatId: drop that too.
        if (chatId != 0L) client.send(TdApi.CloseChat(chatId))
        chatId = 0; resumeChatId = 0; topic = null; threadTitle = null; threadRoot = 0; threadDraft = ""
        messages.clear(); viewed.clear(); deletedIds.clear(); pinnedIds.clear(); pinnedCache.clear()
        _history.value = TelegramChatHistory()
    }

    fun loadOlder() = client.post { requestOlder() }

    /**
     * [silent]: delivered without sound («Отправить без звука»). [scheduleAt]: unix time to send at,
     * [SEND_WHEN_ONLINE] — when the other person comes online, 0 — now.
     */
    fun sendText(text: String, replyToMessageId: Long?, silent: Boolean = false, scheduleAt: Int = 0, quote: String? = null, noLinkPreview: Boolean = false) = act { id ->
        val schedule = when {
            scheduleAt == SEND_WHEN_ONLINE -> TdApi.MessageSchedulingStateSendWhenOnline()
            scheduleAt > 0 -> TdApi.MessageSchedulingStateSendAtDate(scheduleAt, 0)
            else -> null
        }
        val options = if (silent || schedule != null) TdApi.MessageSendOptions(null, silent, false, false, false, 0, false, schedule, 0, 0, false) else null
        TdApi.SendMessage(id, topic, replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, quote?.let { q -> quoteOf(it, q) }, 0, null) }, options, null, inputText(text, noLinkPreview))
    }

    /** [spoiler]: hidden under a spoiler; [viewOnce]: disappears after it is opened (private chats). */
    fun sendPhoto(path: String, caption: String, spoiler: Boolean = false, viewOnce: Boolean = false) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessagePhoto().apply {
            photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = formatted(caption)
            hasSpoiler = spoiler
            if (viewOnce) selfDestructType = TdApi.MessageSelfDestructTypeImmediately()
        })
    }

    /** Puts another photo or video in place of the one in [messageId] (own messages). */
    fun replaceMedia(messageId: Long, path: String, video: Boolean) = act { id ->
        val caption = (messages[messageId]?.content as? TdApi.MessagePhoto)?.caption ?: (messages[messageId]?.content as? TdApi.MessageVideo)?.caption ?: TdApi.FormattedText("", emptyArray())
        val content: TdApi.InputMessageContent = if (video) TdApi.InputMessageVideo().apply {
            this.video = TdApi.InputVideo().apply { this.video = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = caption
        } else TdApi.InputMessagePhoto().apply {
            photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = caption
        }
        TdApi.EditMessageMedia(id, messageId, null, content)
    }

    /** Who read [messageId] (own messages in small groups, for 7 days): name and when. */
    suspend fun readBy(messageId: Long): List<Pair<String, Int>> {
        val id = chatId.takeIf { it != 0L } ?: return emptyList()
        return client.query(TdApi.GetMessageViewers(id, messageId)).viewers.map { userName(it.userId) to it.viewDate }
    }

    /** Who reacted to [messageId] and with what. */
    suspend fun reactedBy(messageId: Long): List<Pair<String, String>> {
        val id = chatId.takeIf { it != 0L } ?: return emptyList()
        return client.query(TdApi.GetMessageAddedReactions(id, messageId, null, "", 100)).reactions.mapNotNull { r ->
            val key = reactionKey(r.type) ?: return@mapNotNull null
            val name = when (val s = r.senderId) {
                is TdApi.MessageSenderUser -> userName(s.userId)
                is TdApi.MessageSenderChat -> runCatching { client.query(TdApi.GetChat(s.chatId)).title }.getOrDefault("")
                else -> ""
            }
            name to key
        }
    }

    private suspend fun userName(userId: Long): String =
        runCatching { client.query(TdApi.GetUser(userId)).let { "${it.firstName} ${it.lastName}".trim() } }.getOrDefault("").ifBlank { "Пользователь" }

    /** The quoted part of [messageId]: its text and where it is in the message. */
    private fun quoteOf(messageId: Long, quote: String): TdApi.InputTextQuote {
        val whole = (messages[messageId]?.content as? TdApi.MessageText)?.text?.text.orEmpty()
        return TdApi.InputTextQuote(TdApi.FormattedText(quote, emptyArray()), emptyArray(), whole.indexOf(quote).coerceAtLeast(0))
    }

    /** Reactions as strings: an emoji itself, or «ce:<id>» for a custom emoji. */
    private fun reactionKey(type: TdApi.ReactionType?): String? = when (type) {
        is TdApi.ReactionTypeEmoji -> type.emoji
        is TdApi.ReactionTypeCustomEmoji -> "ce:${type.customEmojiId}"
        else -> null
    }

    private fun reactionType(key: String): TdApi.ReactionType =
        key.removePrefix("ce:").toLongOrNull()?.takeIf { key.startsWith("ce:") }?.let { TdApi.ReactionTypeCustomEmoji(it) } ?: TdApi.ReactionTypeEmoji(key)

    fun sendVoice(path: String, durationSeconds: Int, waveform: ByteArray) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageVoiceNote(
            TdApi.InputVoiceNote(TdApi.InputFileLocal(path), durationSeconds, waveform), TdApi.FormattedText("", emptyArray()), null
        ))
    }

    fun sendVideo(path: String, caption: String, durationSeconds: Int, width: Int, height: Int) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageVideo(
            TdApi.InputVideo(TdApi.InputFileLocal(path), null, null, 0, IntArray(0), durationSeconds, width, height, true),
            formatted(caption), false, null, false
        ))
    }

    fun sendLocation(latitude: Double, longitude: Double) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageLocation(TdApi.Location(latitude, longitude, 0.0)))
    }

    /** Shares a Telegram user as a contact card. */
    fun sendContact(userId: Long, firstName: String, lastName: String, phone: String) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageContact(TdApi.Contact(phone, firstName, lastName, "", userId)))
    }

    fun sendDocument(path: String, caption: String) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageDocument().apply {
            document = TdApi.InputDocument().apply { document = TdApi.InputFileLocal(path) }
            this.caption = formatted(caption)
        })
    }

    /** Throws an animated 🎲 🎯 🏀 ⚽ 🎳 🎰: Telegram picks the result. */
    fun sendDice(emoji: String, replyToMessageId: Long?) = act { id ->
        TdApi.SendMessage(id, topic, replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null, 0, null) }, null, null, TdApi.InputMessageDice(emoji, false))
    }

    /** Sends a sticker or saved GIF from the panel by its file id (no upload). */
    fun sendPanelItem(item: TelegramPanelItem, replyToMessageId: Long?) = act { id ->
        val file = TdApi.InputFileId(item.fileId)
        val media = item.media
        val content: TdApi.InputMessageContent = if (item.isGif) TdApi.InputMessageAnimation(
            TdApi.InputAnimation(file, null, IntArray(0), media.duration, media.width, media.height), TdApi.FormattedText("", emptyArray()), false, false
        ) else TdApi.InputMessageSticker(TdApi.InputSticker(file, null, media.width, media.height), media.title)
        TdApi.SendMessage(id, topic, replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null, 0, null) }, null, null, content)
    }

    /** Sends a recorded video note (round video); [length] is the side of the square video. */
    fun sendVideoNote(path: String, durationSeconds: Int, length: Int) = act { id ->
        TdApi.SendMessage(id, topic, null, null, null, TdApi.InputMessageVideoNote(TdApi.InputVideoNote(TdApi.InputFileLocal(path), null, durationSeconds, length), null))
    }

    /** Edits a text message, or the caption of a media message. */
    fun edit(messageId: Long, text: String) = act { id ->
        if (messages[messageId]?.content is TdApi.MessageText) TdApi.EditMessageText(id, messageId, null, inputText(text))
        else TdApi.EditMessageCaption(id, messageId, null, formatted(text), false)
    }

    /** Sends 2–10 photos and videos as one album; [items] are local paths with video metadata or null for photos. */
    fun sendAlbum(items: List<Pair<String, VideoMeta?>>, caption: String) = act { id ->
        val contents = items.mapIndexed { index, (path, video) ->
            // Telegram shows the album caption from its first item.
            val text = formatted(if (index == 0) caption else "")
            if (video == null) TdApi.InputMessagePhoto().apply {
                photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
                this.caption = text
            } else TdApi.InputMessageVideo(
                TdApi.InputVideo(TdApi.InputFileLocal(path), null, null, 0, IntArray(0), video.durationSeconds, video.width, video.height, true),
                text, false, null, false
            )
        }
        TdApi.SendMessageAlbum(id, topic, null, null, contents.toTypedArray())
    }

    /** Adds the emoji reaction, or removes it when this account already chose it. */
    fun toggleReaction(messageId: Long, emoji: String) = act { id ->
        val chosen = messages[messageId]?.interactionInfo?.reactions?.reactions
            ?.any { reactionKey(it.type) == emoji && it.isChosen } == true
        if (chosen) TdApi.RemoveMessageReaction(id, messageId, reactionType(emoji))
        else TdApi.AddMessageReaction(id, messageId, reactionType(emoji), false, true)
    }

    fun delete(messageIds: List<Long>, forEveryone: Boolean) = act { id -> ownDeletes += messageIds; TdApi.DeleteMessages(id, messageIds.toLongArray(), forEveryone) }

    // Variants that name the chat (v3 UI): the open chat keeps its thread/topic and loaded state,
    // any other chat is addressed directly, so an action never lands in the wrong chat.

    fun sendTextIn(chatId: Long, text: String, replyToMessageId: Long?) = client.post {
        val inTopic = if (chatId == this.chatId) topic else null
        val reply = replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null, 0, null) }
        client.send(TdApi.SendMessage(chatId, inTopic, reply, null, null, inputText(text))) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    fun editIn(chatId: Long, messageId: Long, text: String) = client.post {
        val loaded = if (chatId == this.chatId) messages[messageId] else null
        val request = if (loaded != null && loaded.content !is TdApi.MessageText) TdApi.EditMessageCaption(chatId, messageId, null, formatted(text), false)
            else TdApi.EditMessageText(chatId, messageId, null, inputText(text))
        client.send(request) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    fun deleteIn(chatId: Long, messageIds: List<Long>, forEveryone: Boolean) = client.post {
        if (chatId == this.chatId) ownDeletes += messageIds
        client.send(TdApi.DeleteMessages(chatId, messageIds.toLongArray(), forEveryone)) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    /** «Повторить» for messages that failed to send. */
    fun resend(chatId: Long, messageIds: List<Long>) = client.post {
        client.send(TdApi.ResendMessages(chatId, messageIds.toLongArray(), null, 0)) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    /** What may be done with a message (edit, delete for all, forward...); null when TDLib does not know it. */
    suspend fun rights(chatId: Long, messageId: Long): TelegramMessageRights? = runCatching {
        val p = client.query(TdApi.GetMessageProperties(chatId, messageId))
                // canBeDeletedOnlyForSelf is false in supergroups/channels: there every delete is for everyone.
        TelegramMessageRights(p.canBeEdited, p.canBeDeletedForAllUsers, p.canBeDeletedOnlyForSelf, p.canBeForwarded, p.canBeCopied, p.canBePinned)
    }.getOrNull()

    /** Emoji reactions allowed on a message of any chat (see [availableReactions]); null on failure. */
    suspend fun availableReactionsIn(chatId: Long, messageId: Long): List<String>? = runCatching {
        val available = client.query(TdApi.GetMessageAvailableReactions(chatId, messageId, 8))
        (available.topReactions + available.recentReactions + available.popularReactions)
            .filter { !it.needsPremium }.mapNotNull { reactionKey(it.type) }.distinct().take(8)
    }.getOrNull()

    /** Toggles like [toggleReaction]; outside the open chat the current choice is read from TDLib first. */
    fun toggleReactionIn(chatId: Long, messageId: Long, emoji: String) = client.post {
        fun apply(message: TdApi.Message?) {
            val chosen = message?.interactionInfo?.reactions?.reactions?.any { reactionKey(it.type) == emoji && it.isChosen } == true
            val request = if (chosen) TdApi.RemoveMessageReaction(chatId, messageId, reactionType(emoji)) else TdApi.AddMessageReaction(chatId, messageId, reactionType(emoji), false, true)
            client.send(request) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
        }
        if (chatId == this.chatId && messageId in messages) apply(messages[messageId])
        else client.send(TdApi.GetMessage(chatId, messageId)) { result -> apply(result as? TdApi.Message) }
    }

    /** [asCopy]: sent as your own messages, without «Переслано от»; [removeCaption] also drops media captions. */
    fun forward(toChatId: Long, messageIds: List<Long>, asCopy: Boolean = false, removeCaption: Boolean = false) =
        act { id -> TdApi.ForwardMessages(toChatId, null, id, messageIds.toLongArray(), null, asCopy, asCopy && removeCaption) }

    /**
     * Saves [text] as the Telegram draft of [chatId] (any chat, not only the open one), or clears it
     * when blank. Skipped when the draft already holds this text, so leaving a chat is not a request.
     */
    fun saveDraft(chatId: Long, text: String, threadId: Long = 0, forumTopicId: Int = 0) = client.post {
        val target: TdApi.MessageTopic? = when {
            threadId != 0L -> TdApi.MessageTopicThread(threadId)
            forumTopicId != 0 -> TdApi.MessageTopicForum(forumTopicId)
            else -> null
        }
        if (target != null) {
            // The thread may already be closed (screen order on leaving): then its draft is not known
            // here, and the text is sent as is.
            val open = chatId == this.chatId && sameTopic(topic, target)
            if (open && threadDraft == text) return@post
            if (open) threadDraft = text
            client.send(TdApi.SetChatDraftMessage(chatId, target, draftOf(text))) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
            return@post
        }
        val chat = chats.chat(chatId) ?: return@post
        if (text.isBlank() && chat.draftMessage == null) return@post
        if (draftText(chat) == text) return@post
        client.send(TdApi.SetChatDraftMessage(chatId, null, draftOf(text))) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    private fun draftOf(text: String): TdApi.DraftMessage? = text.takeIf { it.isNotBlank() }?.let {
        TdApi.DraftMessage(null, (System.currentTimeMillis() / 1000).toInt(), TdApi.DraftMessageContentText(TdApi.FormattedText(it, emptyArray()), null), 0, null)
    }

    private fun sameTopic(a: TdApi.MessageTopic?, b: TdApi.MessageTopic): Boolean = when (a) {
        is TdApi.MessageTopicThread -> b is TdApi.MessageTopicThread && a.messageThreadId == b.messageThreadId
        is TdApi.MessageTopicForum -> b is TdApi.MessageTopicForum && a.forumTopicId == b.forumTopicId
        else -> false
    }

    fun clearError() { _error.value = null }

    private fun act(build: (Long) -> TdApi.Function<*>) = client.post {
        val id = chatId.takeIf { it != 0L } ?: return@post
        client.send(build(id)) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    /**
     * Inline bot results for «@bot запрос» typed in the composer; null when [username] is not an inline bot.
     * Results are shown as a list and sent with [sendInlineResult].
     */
    suspend fun inlineResults(username: String, query: String): InlineResults? {
        val id = chatId.takeIf { it != 0L } ?: return null
        val bot = runCatching { client.query(TdApi.SearchPublicChat(username)) }.getOrNull()?.type as? TdApi.ChatTypePrivate ?: return null
        val user = runCatching { client.query(TdApi.GetUser(bot.userId)) }.getOrNull() ?: return null
        val type = user.type as? TdApi.UserTypeBot ?: return null
        if (!type.isInline) return null
        val results = client.query(TdApi.GetInlineQueryResults(bot.userId, id, null, query, ""))
        return InlineResults(results.inlineQueryId, results.results.mapNotNull { result ->
            when (result) {
                is TdApi.InlineQueryResultArticle -> InlineResult(result.id, result.title, result.description)
                is TdApi.InlineQueryResultPhoto -> InlineResult(result.id, result.title.ifBlank { "Фото" }, result.description)
                is TdApi.InlineQueryResultVideo -> InlineResult(result.id, result.title.ifBlank { "Видео" }, result.description)
                is TdApi.InlineQueryResultAnimation -> InlineResult(result.id, result.title.ifBlank { "GIF" }, "")
                is TdApi.InlineQueryResultSticker -> InlineResult(result.id, "${result.sticker.emoji} Стикер".trim(), "")
                is TdApi.InlineQueryResultAudio -> InlineResult(result.id, result.audio.title.ifBlank { "Аудио" }, result.audio.performer)
                is TdApi.InlineQueryResultDocument -> InlineResult(result.id, result.title.ifBlank { "Файл" }, result.description)
                is TdApi.InlineQueryResultVoiceNote -> InlineResult(result.id, result.title.ifBlank { "Голосовое сообщение" }, "")
                is TdApi.InlineQueryResultLocation -> InlineResult(result.id, result.title.ifBlank { "Местоположение" }, "")
                is TdApi.InlineQueryResultVenue -> InlineResult(result.id, result.venue.title, result.venue.address)
                is TdApi.InlineQueryResultContact -> InlineResult(result.id, listOf(result.contact.firstName, result.contact.lastName).joinToString(" ").trim(), result.contact.phoneNumber)
                is TdApi.InlineQueryResultGame -> InlineResult(result.id, result.game.title, result.game.description)
                else -> null
            }
        })
    }

    fun sendInlineResult(queryId: Long, resultId: String, replyToMessageId: Long?) = act { id ->
        TdApi.SendInlineQueryResultMessage(id, topic, replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null, 0, null) }, null, queryId, resultId, false)
    }

    /** Tags of Saved Messages (Telegram Premium); empty without them. */
    suspend fun savedTags(): List<SavedTag> = runCatching {
        client.query(TdApi.GetSavedMessagesTags(0)).tags.mapNotNull { tag ->
            (tag.tag as? TdApi.ReactionTypeEmoji)?.let { SavedTag(it.emoji, tag.label.orEmpty(), tag.count) }
        }
    }.getOrDefault(emptyList())

    /** Saved messages marked with the tag [emoji], newest first. */
    suspend fun savedByTag(emoji: String): List<TelegramMessage> {
        val found = client.query(TdApi.SearchSavedMessages(0, TdApi.ReactionTypeEmoji(emoji), "", 0, 0, 100))
        val deferred = kotlinx.coroutines.CompletableDeferred<List<TelegramMessage>>()
        client.post { deferred.complete(found.messages.map { it.toModel() }) }
        return deferred.await()
    }

    /** Saved messages forwarded from one chat (a Saved Messages topic), newest first. */
    suspend fun savedFromTopic(topicId: Long): List<TelegramMessage> {
        val found = client.query(TdApi.GetSavedMessagesTopicHistory(topicId, 0, 0, 100))
        val deferred = kotlinx.coroutines.CompletableDeferred<List<TelegramMessage>>()
        client.post { deferred.complete(found.messages.map { it.toModel() }) }
        return deferred.await()
    }

    suspend fun setTagLabel(emoji: String, label: String) { client.query(TdApi.SetSavedMessagesTagLabel(TdApi.ReactionTypeEmoji(emoji), label)) }

    /** «Отложенные»: messages of the open chat waiting to be sent, soonest first. */
    suspend fun scheduledMessages(): List<ScheduledMessage> {
        val id = chatId.takeIf { it != 0L } ?: return emptyList()
        return client.query(TdApi.GetChatScheduledMessages(id)).messages.map { message ->
            val text = (message.content as? TdApi.MessageText)?.text?.text ?: "Медиа"
            val at = (message.schedulingState as? TdApi.MessageSchedulingStateSendAtDate)?.sendDate ?: 0
            ScheduledMessage(message.id, text, at)
        }.sortedBy { if (it.sendAt == 0) Int.MAX_VALUE else it.sendAt }
    }

    /** Sends a scheduled message right away. */
    suspend fun sendScheduledNow(messageId: Long) {
        val id = chatId.takeIf { it != 0L } ?: return
        client.query(TdApi.EditMessageSchedulingState(id, messageId, null))
    }

    suspend fun deleteScheduled(messageId: Long) {
        val id = chatId.takeIf { it != 0L } ?: return
        client.query(TdApi.DeleteMessages(id, longArrayOf(messageId), true))
    }

    private fun inputText(text: String, noLinkPreview: Boolean = false) =
        TdApi.InputMessageText(formatted(text), if (noLinkPreview) TdApi.LinkPreviewOptions(true, "", false, false, false) else null, true)

    /**
     * Text as Telegram's apps send it: **жирный**, __курсив__, ~~зачёркнутый~~, ||спойлер||, `код`,
     * ```блок кода``` and [текст](ссылка) become formatting; anything else stays as typed.
     */
    private fun formatted(text: String): TdApi.FormattedText =
        runCatching { org.drinkless.tdlib.Client.execute(TdApi.ParseMarkdown(TdApi.FormattedText(text, emptyArray()))) }.getOrNull()
            ?: TdApi.FormattedText(text, emptyArray())

    /** The text of [messageId] with its formatting written back as markdown, for editing. */
    fun editableText(messageId: Long): String? {
        val content = messages[messageId]?.content ?: return null
        val text = when (content) {
            is TdApi.MessageText -> content.text
            is TdApi.MessagePhoto -> content.caption
            is TdApi.MessageVideo -> content.caption
            is TdApi.MessageDocument -> content.caption
            is TdApi.MessageAnimation -> content.caption
            is TdApi.MessageAudio -> content.caption
            else -> return null
        }
        return runCatching { org.drinkless.tdlib.Client.execute(TdApi.GetMarkdownText(text)).text }.getOrNull() ?: text.text
    }

    private fun requestOlder() {
        val id = chatId
        if (id == 0L || loadingOlder || reachedStart) return
        loadingOlder = true
        schedulePublish()
        val from = messages.keys.firstOrNull() ?: 0L
        val request: TdApi.Function<TdApi.Messages> = when (val current = topic) {
            is TdApi.MessageTopicThread -> TdApi.GetMessageThreadHistory(id, threadRoot, from, 0, PAGE_SIZE)
            is TdApi.MessageTopicForum -> TdApi.GetForumTopicHistory(id, current.forumTopicId, from, 0, PAGE_SIZE)
            else -> TdApi.GetChatHistory(id, from, 0, PAGE_SIZE, false)
        }
        client.send(request) { result ->
            if (id != chatId) return@send
            loadingOlder = false
            when (result) {
                is TdApi.Messages -> {
                    val page = result.messages.filterNotNull()
                    if (page.isEmpty()) reachedStart = true
                    page.forEach { messages[it.id] = it; pinnedCache.remove(it.id) }
                }
                is TdApi.Error -> _error.value = humanError(result)
            }
            schedulePublish()
            continueJump()
        }
    }

    private fun handle(update: TdApi.Object) {
        when (update) {
            // Scheduled messages are not in the history until they are sent; they have their own list.
            is TdApi.UpdateNewMessage -> if (update.message.chatId == chatId && update.message.schedulingState == null && inTopic(update.message)) { messages[update.message.id] = update.message; schedulePublish() }
            // A scheduled message (date 0 until it goes out) stays in «Отложенные», not in the history.
            is TdApi.UpdateMessageSendSucceeded -> if (update.message.chatId == chatId && (update.oldMessageId in messages || inTopic(update.message))) {
                messages.remove(update.oldMessageId)
                if (update.message.schedulingState == null) messages[update.message.id] = update.message
                schedulePublish()
            }
            is TdApi.UpdateMessageSendFailed -> if (update.message.chatId == chatId) {
                messages.remove(update.oldMessageId)
                if (update.message.schedulingState == null) messages[update.message.id] = update.message
                _error.value = humanError(update.error)
                schedulePublish()
            }
            is TdApi.UpdateMessageContent -> if (update.chatId == chatId) messages[update.messageId]?.let { it.content = update.newContent; schedulePublish() }
            is TdApi.UpdateMessageEdited -> if (update.chatId == chatId) messages[update.messageId]?.let { it.editDate = update.editDate; it.replyMarkup = update.replyMarkup; schedulePublish() }
            is TdApi.UpdatePoll -> messages.values.firstOrNull { (it.content as? TdApi.MessagePoll)?.poll?.id == update.poll.id }?.let { message ->
                (message.content as TdApi.MessagePoll).poll = update.poll
                schedulePublish()
            }
            is TdApi.UpdateMessageInteractionInfo -> if (update.chatId == chatId) messages[update.messageId]?.let { it.interactionInfo = update.interactionInfo; schedulePublish() }
            is TdApi.UpdateDeleteMessages -> if (update.chatId == chatId && update.isPermanent) {
                // Kept with a mark when deleted by someone else and «Сохранять удалённые» is on; own deletions just go.
                val keep = com.kirox.app.settings.KiroxTweaks.current.keepDeleted && !update.fromCache
                update.messageIds.forEach { id ->
                    val message = messages[id]
                    if (keep && message != null && !message.isOutgoing && id !in ownDeletes) deletedIds += id else messages.remove(id)
                }
                ownDeletes.removeAll(update.messageIds.toSet())
                schedulePublish()
            }
            is TdApi.UpdateChatReadOutbox -> if (update.chatId == chatId) { lastReadOutbox = update.lastReadOutboxMessageId; schedulePublish() }
            // Keeps the open topic's draft current when it changes on another device.
            is TdApi.UpdateForumTopic -> if (update.chatId == chatId && (topic as? TdApi.MessageTopicForum)?.forumTopicId == update.forumTopicId) {
                threadDraft = draftText(update.draftMessage)
            }
            is TdApi.UpdateMessageIsPinned -> if (update.chatId == chatId) {
                if (update.isPinned) {
                    pinnedIds += update.messageId
                    if (update.messageId !in messages) client.send(TdApi.GetMessage(update.chatId, update.messageId)) { result ->
                        if (result is TdApi.Message && result.chatId == chatId) { pinnedCache[result.id] = result; schedulePublish() }
                    }
                } else { pinnedIds -= update.messageId; pinnedCache.remove(update.messageId) }
                schedulePublish()
            }
            is TdApi.UpdateUser, is TdApi.UpdateUserStatus -> if (chatId != 0L) schedulePublish()
            is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                // After a background pause the open chat loads again; logout and account switch drop it.
                // A thread or topic stays as it was (topic, title, root, draft) and reopens as itself.
                is TdApi.AuthorizationStateClosed -> {
                    resumeChatId = if (client.paused) chatId else 0
                    chatId = 0; messages.clear(); viewed.clear(); deletedIds.clear()
                    if (resumeChatId == 0L) {
                        topic = null; threadTitle = null; threadRoot = 0; threadDraft = ""
                        _history.value = TelegramChatHistory()
                    }
                }
                is TdApi.AuthorizationStateReady -> if (resumeChatId != 0L) {
                    val id = resumeChatId
                    resumeChatId = 0
                    if (topic != null) openLoaded(id) else open(id)
                }
            }
        }
    }

    /** Whether [message] belongs to the open thread or topic (always true for a whole chat). */
    private fun inTopic(message: TdApi.Message): Boolean = when (val current = topic) {
        null -> true
        is TdApi.MessageTopicThread -> (message.topicId as? TdApi.MessageTopicThread)?.messageThreadId == current.messageThreadId
        is TdApi.MessageTopicForum -> (message.topicId as? TdApi.MessageTopicForum)?.forumTopicId == current.forumTopicId
        else -> true
    }

    private fun schedulePublish() {
        if (publishScheduled) return
        publishScheduled = true
        client.post(PUBLISH_DELAY_MS) { publishScheduled = false; publish() }
    }

    private fun publish() {
        val id = chatId.takeIf { it != 0L } ?: return
        val chat = chats.chat(id)
        val type = chat?.type
        val peerUser = (type as? TdApi.ChatTypePrivate)?.let { users.user(it.userId) } ?: (type as? TdApi.ChatTypeSecret)?.let { users.user(it.userId) }
        savedChat = peerUser != null && peerUser.id == users.myId()
        val mapped = messages.values.map { it.toModel() }
        _history.value = TelegramChatHistory(
            loaded = true,
            chatId = id,
            title = chat?.title.orEmpty(),
            photo = chat?.photo?.small?.toRef(),
            peerKind = when {
                peerUser != null && peerUser.id == users.myId() -> TelegramPeerKind.Saved
                peerUser?.type is TdApi.UserTypeBot -> TelegramPeerKind.Bot
                type is TdApi.ChatTypePrivate || type is TdApi.ChatTypeSecret -> TelegramPeerKind.User
                type is TdApi.ChatTypeSupergroup && type.isChannel -> TelegramPeerKind.Channel
                type is TdApi.ChatTypeBasicGroup || type is TdApi.ChatTypeSupergroup -> TelegramPeerKind.Group
                else -> TelegramPeerKind.Unknown
            },
            peerStatus = peerUser?.status?.toStatus(),
            messages = mapped,
            loadingOlder = loadingOlder,
            reachedStart = reachedStart,
            isSecret = type is TdApi.ChatTypeSecret,
            draft = if (topic != null) threadDraft else chat?.let(::draftText).orEmpty(),
            threadId = (topic as? TdApi.MessageTopicThread)?.messageThreadId ?: 0L,
            forumTopicId = (topic as? TdApi.MessageTopicForum)?.forumTopicId ?: 0,
            pinned = pinnedIds.lastOrNull()?.let { messages[it] ?: pinnedCache[it] }?.toModel(),
            pinnedIds = pinnedIds.toSet(),
            jumpTo = jumpTarget.takeIf { it in messages } ?: 0L,
            threadTitle = threadTitle,
            // The newest keyboard a bot sent (or removed) in the open chat.
            replyKeyboard = messages.values.reversed().asSequence().filter { !it.isOutgoing }.mapNotNull { replyKeyboardOf(it.replyMarkup) }.firstOrNull().orEmpty(),
            isForum = topic == null && chats.isForum(id)
        )
        markViewed(id)
    }

    /** Opening a chat reads it: report newly shown incoming messages once. Not in ghost mode. */
    private fun markViewed(id: Long) {
        // Nothing is remembered as viewed either, so leaving ghost mode reads them on the next look.
        if (com.kirox.app.settings.KiroxTweaks.current.ghostRead) return
        val fresh = messages.values.filter { !it.isOutgoing && viewed.add(it.id) }.map { it.id }
        if (fresh.isNotEmpty()) client.send(TdApi.ViewMessages(id, fresh.toLongArray(), when (topic) {
            is TdApi.MessageTopicThread -> TdApi.MessageSourceMessageThreadHistory()
            is TdApi.MessageTopicForum -> TdApi.MessageSourceForumTopicHistory()
            else -> TdApi.MessageSourceChatHistory()
        }, true))
    }

    private fun TdApi.Message.toModel(): TelegramMessage {
        val content = content
        val reply = replyTo as? TdApi.MessageReplyToMessage
        val replied = reply?.takeIf { it.chatId == 0L || it.chatId == chatId }?.let { messages[it.messageId] }
        val photo = (content as? TdApi.MessagePhoto)?.photo?.let(::bestPhoto)
        val formatted = formattedTextOf(content).takeIf { content !is TdApi.MessageVoiceNote }
        val mediaInfo = mediaOf(content)
        return TelegramMessage(
            id = id,
            chatId = chatId,
            senderName = senderName(senderId),
            senderId = when (val s = senderId) { is TdApi.MessageSenderUser -> s.userId; is TdApi.MessageSenderChat -> s.chatId; else -> 0L },
            isOutgoing = isOutgoing,
            kind = kindOf(content),
            text = when (content) {
                is TdApi.MessageVoiceNote -> "%d:%02d".format(content.voiceNote.duration / 60, content.voiceNote.duration % 60)
                // Shared place and contact: readable text with a map link and the number.
                is TdApi.MessageLocation -> "📍 Геопозиция\nhttps://maps.google.com/?q=%.6f,%.6f".format(java.util.Locale.US, content.location.latitude, content.location.longitude)
                is TdApi.MessageContact -> "👤 " + listOf(content.contact.firstName, content.contact.lastName).filter { it.isNotBlank() }.joinToString(" ") +
                    (content.contact.phoneNumber.takeIf { it.isNotBlank() }?.let { "\n+" + it.trimStart('+') } ?: "")
                // Media without a caption shows nothing under it; other contents a text preview.
                is TdApi.MessageVideo, is TdApi.MessageAnimation, is TdApi.MessageAudio, is TdApi.MessageVideoNote, is TdApi.MessageSticker -> formatted?.text.orEmpty()
                else -> formatted?.text ?: messagePreview(this)
            },
            entities = entitiesOf(formatted),
            media = mediaInfo.first,
            thumbnail = mediaInfo.second,
            albumId = mediaAlbumId,
            poll = pollOf(content),
            buttons = inlineButtonsOf(replyMarkup),
            commentCount = interactionInfo?.replyInfo?.replyCount?.takeIf { isChannelPost },
            viewCount = interactionInfo?.viewCount?.takeIf { isChannelPost },
            replyToMessageId = reply?.takeIf { it.chatId == 0L || it.chatId == chatId }?.messageId ?: 0L,
            date = date,
            edited = editDate > 0,
            deleted = id in deletedIds,
            isProtected = !canBeSaved,
            delivery = when {
                !isOutgoing || savedChat -> TelegramDelivery.None
                sendingState is TdApi.MessageSendingStatePending -> TelegramDelivery.Sending
                sendingState is TdApi.MessageSendingStateFailed -> TelegramDelivery.Failed
                id <= lastReadOutbox -> TelegramDelivery.Read
                else -> TelegramDelivery.Sent
            },
            replyAuthor = replied?.let { senderName(it.senderId) } ?: reply?.let { "Ответ" },
            replyText = replied?.let(::messagePreview) ?: reply?.content?.let { "Сообщение" },
            forwardedFrom = forwardInfo?.origin?.let(::originName),
            photo = photo?.photo?.toRef() ?: (content as? TdApi.MessageVoiceNote)?.voiceNote?.voice?.toRef(),
            reactions = interactionInfo?.reactions?.reactions.orEmpty().mapNotNull { reaction ->
                reactionKey(reaction.type)?.let { Triple(it, reaction.totalCount, reaction.isChosen) }
            },
            photoAspect = photo?.let { if (it.height > 0) it.width.toFloat() / it.height else 0f } ?: 0f,
            fileName = (content as? TdApi.MessageDocument)?.document?.fileName ?: (content as? TdApi.MessageAudio)?.audio?.fileName,
            // Rough client-side hint; TDLib enforces the real rules and reports errors.
            canEdit = isOutgoing && sendingState == null && (content is TdApi.MessageText || content is TdApi.MessagePhoto || content is TdApi.MessageDocument || content is TdApi.MessageVideo || content is TdApi.MessageAnimation || content is TdApi.MessageAudio),
            serviceText = serviceTextOf(this)
        )
    }

    /** Line of a service message for the v3 UI; null for ordinary messages (see [serviceText]). */
    private fun serviceTextOf(message: TdApi.Message): String? {
        val type = chats.chat(message.chatId)?.type
        return serviceText(message, senderName(message.senderId), isChannel = (type as? TdApi.ChatTypeSupergroup)?.isChannel == true) { id ->
            if (id == users.myId()) "Вы" else users.user(id)?.displayName() ?: "Пользователь"
        }
    }

    private fun senderName(sender: TdApi.MessageSender?): String = when (sender) {
        is TdApi.MessageSenderUser -> if (sender.userId == users.myId()) "Вы" else users.user(sender.userId)?.displayName() ?: "Пользователь"
        is TdApi.MessageSenderChat -> chats.chat(sender.chatId)?.title ?: "Чат"
        else -> ""
    }

    private fun originName(origin: TdApi.MessageOrigin): String = when (origin) {
        is TdApi.MessageOriginUser -> users.user(origin.senderUserId)?.displayName() ?: "Пользователь"
        is TdApi.MessageOriginHiddenUser -> origin.senderName
        is TdApi.MessageOriginChat -> chats.chat(origin.senderChatId)?.title ?: "Чат"
        is TdApi.MessageOriginChannel -> chats.chat(origin.chatId)?.title ?: "Канал"
        else -> "—"
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val PUBLISH_DELAY_MS = 32L
        const val PINNED_LIMIT = 100
        /** Up to 1000 messages back; further is rare and better reached by search. */
        const val JUMP_PAGES = 20
        const val SEARCH_PAGE = 50
        const val FORUM_TOPICS_LIMIT = 100
    }
}

/** Duration and size of a picked video, needed to send it as a video. */
data class VideoMeta(val durationSeconds: Int, val width: Int, val height: Int)

/** Text of the chat's draft, empty when there is none. */
internal fun draftText(chat: TdApi.Chat): String = draftText(chat.draftMessage)

internal fun draftText(draft: TdApi.DraftMessage?): String =
    (draft?.content as? TdApi.DraftMessageContentText)?.text?.text.orEmpty()

/** Largest photo size not wider than ~1280 px, falling back to the smallest available. */
internal fun bestPhoto(photo: TdApi.Photo): TdApi.PhotoSize? =
    photo.sizes.filter { it.width <= 1280 }.maxByOrNull { it.width } ?: photo.sizes.minByOrNull { it.width }

/**
 * Russian line of a service message ("Анна вступил(а) в группу", "Канал переименован в «…»"), or
 * null for ordinary messages. Pure: [who] is the sender's name, [user] names other users. Used by the
 * v3 chat list (TelegramChatItem.lastServiceText) and history (TelegramMessage.serviceText).
 */
internal fun serviceText(message: TdApi.Message, who: String, isChannel: Boolean, user: (Long) -> String): String? {
    val senderId = (message.senderId as? TdApi.MessageSenderUser)?.userId
    val channel = isChannel || message.isChannelPost
    return when (val c = message.content) {
        is TdApi.MessageBasicGroupChatCreate -> "$who создал(а) группу «${c.title}»"
        is TdApi.MessageSupergroupChatCreate -> if (channel) "Канал создан" else "$who создал(а) группу «${c.title}»"
        is TdApi.MessageChatChangeTitle -> if (channel) "Канал переименован в «${c.title}»" else "$who изменил(а) название на «${c.title}»"
        is TdApi.MessageChatChangePhoto -> if (channel) "Фото канала изменено" else "$who изменил(а) фото группы"
        is TdApi.MessageChatDeletePhoto -> if (channel) "Фото канала удалено" else "$who удалил(а) фото группы"
        is TdApi.MessageChatAddMembers -> {
            val ids = c.memberUserIds.toList()
            when {
                ids == listOf(senderId) -> if (channel) "$who присоединился(-ась) к каналу" else "$who вступил(а) в группу"
                else -> "$who добавил(а) ${ids.joinToString(", ") { user(it) }}"
            }
        }
        is TdApi.MessageChatJoinByLink -> if (channel) "$who присоединился(-ась) к каналу" else "$who вступил(а) в группу по ссылке"
        is TdApi.MessageChatJoinByRequest -> if (channel) "$who присоединился(-ась) к каналу" else "$who вступил(а) в группу"
        is TdApi.MessageChatDeleteMember -> if (c.userId == senderId) (if (channel) "$who покинул(а) канал" else "$who покинул(а) группу") else "$who удалил(а) ${user(c.userId)}"
        is TdApi.MessagePinMessage -> if (channel) "Сообщение закреплено" else "$who закрепил(а) сообщение"
        is TdApi.MessageScreenshotTaken -> "$who сделал(а) снимок экрана"
        is TdApi.MessageContactRegistered -> "$who теперь в Telegram"
        is TdApi.MessageChatSetMessageAutoDeleteTime -> if (c.messageAutoDeleteTime == 0) "Автоудаление выключено" else "Включено автоудаление сообщений"
        is TdApi.MessageChatUpgradeTo, is TdApi.MessageChatUpgradeFrom -> "Группа стала супергруппой"
        is TdApi.MessageChatSetTheme -> "$who изменил(а) тему чата"
        is TdApi.MessageChatSetBackground -> "$who изменил(а) обои чата"
        is TdApi.MessageForumTopicCreated -> "Создана тема «${c.name}»"
        is TdApi.MessageVideoChatStarted -> if (channel) "Трансляция начата" else "$who начал(а) видеочат"
        is TdApi.MessageVideoChatEnded -> if (channel) "Трансляция завершена" else "Видеочат завершён"
        // A call is a message in TDLib; Telegram draws it as a line: «Исходящий звонок · 3 мин».
        is TdApi.MessageCall -> {
            val missed = c.discardReason is TdApi.CallDiscardReasonMissed || c.discardReason is TdApi.CallDiscardReasonDeclined
            val what = if (c.isVideo) "видеозвонок" else "звонок"
            val title = when {
                message.isOutgoing && missed -> "Отменённый $what"
                message.isOutgoing -> "Исходящий $what"
                missed -> "Пропущенный $what"
                else -> "Входящий $what"
            }
            val minutes = (c.duration + 30) / 60
            if (c.duration > 0) "$title · " + (if (c.duration < 60) "${c.duration} сек" else "$minutes мин") else title
        }
        is TdApi.MessageGameScore, is TdApi.MessagePaymentSuccessful, is TdApi.MessageGiftedPremium, is TdApi.MessageGift,
        is TdApi.MessageCustomServiceAction, is TdApi.MessageExpiredPhoto, is TdApi.MessageExpiredVideo -> messagePreview(message).takeIf { it != "Сообщение" } ?: "Служебное сообщение"
        else -> null
    }
}
