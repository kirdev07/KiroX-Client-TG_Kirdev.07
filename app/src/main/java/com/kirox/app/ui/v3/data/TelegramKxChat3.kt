package com.kirox.app.ui.v3.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramServices
import com.kirox.app.telegram.formattedTextOf
import com.kirox.app.telegram.kindOf
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.util.Calendar
import java.util.Locale

/*
 * wave3-chat (binder-chat): the real account behind the chat / more / extra chat boards. TelegramKxStore
 * delegates its wave3-chat members here one line each. Calls name their chat (never "the open chat"),
 * so a board opened over another chat cannot act on the wrong one. Pure pieces live in [KxChat3Mapping].
 */

internal object KxChat3Mapping {
    fun entity(e: KxEntity): TdApi.TextEntity? {
        val type: TdApi.TextEntityType = when (e.type) {
            KxEntityType.Bold -> TdApi.TextEntityTypeBold()
            KxEntityType.Italic -> TdApi.TextEntityTypeItalic()
            KxEntityType.Underline -> TdApi.TextEntityTypeUnderline()
            KxEntityType.Strike -> TdApi.TextEntityTypeStrikethrough()
            KxEntityType.Code -> TdApi.TextEntityTypeCode()
            KxEntityType.Pre -> TdApi.TextEntityTypePre()
            KxEntityType.Spoiler -> TdApi.TextEntityTypeSpoiler()
            KxEntityType.TextLink -> TdApi.TextEntityTypeTextUrl(e.url?.takeIf { it.isNotBlank() } ?: return null)
            // compat: a custom emoji keeps its id in url (TelegramKxStore mapping), so a Premium user's emoji survive copy / resend.
            KxEntityType.CustomEmoji -> TdApi.TextEntityTypeCustomEmoji(e.url?.toLongOrNull()?.takeIf { it != 0L } ?: return null)
            // Links, mentions and hashtags are found by Telegram itself.
            KxEntityType.Link, KxEntityType.Mention, KxEntityType.Hashtag -> return null
        }
        if (e.length <= 0 || e.offset < 0) return null
        return TdApi.TextEntity(e.offset, e.length, type)
    }

    fun formatted(text: String, entities: List<KxEntity> = emptyList()): TdApi.FormattedText =
        TdApi.FormattedText(text, entities.filter { it.offset + it.length <= text.length }.mapNotNull { entity(it) }.toTypedArray())

    /** Telegram's detected entities → KxEntity; e-mail and phone links carry mailto: / tel: urls. */
    fun detected(text: String, e: TdApi.TextEntity): KxEntity? {
        val part = runCatching { text.substring(e.offset, e.offset + e.length) }.getOrNull() ?: return null
        return when (val t = e.type) {
            is TdApi.TextEntityTypeUrl -> KxEntity(e.offset, e.length, KxEntityType.Link, if (part.contains("://")) part else "https://$part")
            is TdApi.TextEntityTypeTextUrl -> KxEntity(e.offset, e.length, KxEntityType.TextLink, t.url)
            is TdApi.TextEntityTypeEmailAddress -> KxEntity(e.offset, e.length, KxEntityType.Link, "mailto:$part")
            is TdApi.TextEntityTypePhoneNumber -> KxEntity(e.offset, e.length, KxEntityType.Link, "tel:" + part.filter { it.isDigit() || it == '+' })
            is TdApi.TextEntityTypeMention, is TdApi.TextEntityTypeMentionName, is TdApi.TextEntityTypeBotCommand -> KxEntity(e.offset, e.length, KxEntityType.Mention)
            is TdApi.TextEntityTypeHashtag, is TdApi.TextEntityTypeCashtag -> KxEntity(e.offset, e.length, KxEntityType.Hashtag)
            else -> null
        }
    }

    fun kind(k: TelegramMessageKind, content: TdApi.MessageContent?): KxMsgKind = when {
        content is TdApi.MessagePoll -> KxMsgKind.Poll
        content is TdApi.MessageLocation || content is TdApi.MessageVenue -> KxMsgKind.Location
        content is TdApi.MessageContact -> KxMsgKind.Contact
        content is TdApi.MessageDice -> KxMsgKind.Dice
        k == TelegramMessageKind.Photo -> KxMsgKind.Photo
        k == TelegramMessageKind.Video -> KxMsgKind.Video
        k == TelegramMessageKind.Animation -> KxMsgKind.Gif
        k == TelegramMessageKind.Voice -> KxMsgKind.Voice
        k == TelegramMessageKind.VideoNote -> KxMsgKind.VideoNote
        k == TelegramMessageKind.Sticker -> KxMsgKind.Sticker
        k == TelegramMessageKind.Audio -> KxMsgKind.Music
        k == TelegramMessageKind.Document -> KxMsgKind.File
        k == TelegramMessageKind.Text -> KxMsgKind.Text
        else -> KxMsgKind.Unsupported
    }

    /** Short label of a message for lists (text, else «📷 Фото»…). */
    fun preview(content: TdApi.MessageContent?): String {
        val text = formattedTextOf(content)?.text.orEmpty()
        if (content is TdApi.MessageDice) return content.emoji
        return KxFormat.preview(text, kindOf(content), 0, (content as? TdApi.MessagePoll)?.poll?.question?.text)
    }

    /** Days from [now] to the next [day].[month] (0 = today); 29.02 counts as 01.03 in other years. */
    fun daysUntil(day: Int, month: Int, now: Calendar = Calendar.getInstance()): Int {
        val today = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        fun on(year: Int) = (today.clone() as Calendar).apply {
            set(Calendar.YEAR, year); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            if (day == 29 && month == 2 && getActualMaximum(Calendar.DAY_OF_MONTH) < 29) { set(Calendar.MONTH, 2); set(Calendar.DAY_OF_MONTH, 1) }
        }
        var next = on(today.get(Calendar.YEAR))
        if (next.before(today)) next = on(today.get(Calendar.YEAR) + 1)
        return ((next.timeInMillis - today.timeInMillis + 12 * 3600_000L) / 86_400_000L).toInt()
    }

    fun readNote(r: TdApi.MessageReadDate): Pair<String, String> = when (r) {
        is TdApi.MessageReadDateRead -> "Прочитано " + KxFormat.dateTime(r.readDate) to ""
        is TdApi.MessageReadDateUnread -> "" to "Ещё не прочитано"
        is TdApi.MessageReadDateTooOld -> "" to "Прочитано, но время уже не хранится (больше 7 дней)"
        is TdApi.MessageReadDateUserPrivacyRestricted -> "" to "Собеседник скрывает время прочтения"
        is TdApi.MessageReadDateMyPrivacyRestricted -> "" to "Вы скрываете время прочтения — поэтому не видите и чужое"
        else -> "" to ""
    }

    /** Host of a link («https://Sub.Example.com/x» → «sub.example.com»), null when there is none. */
    fun host(link: String): String? = link.trim().removePrefix("http://").removePrefix("https://").removePrefix("www.")
        .substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':').lowercase().takeIf { it.contains('.') }

    /**
     * Why a link deserves a warning, null when it looks fine: punycode / mixed-script host (look-alike), or the visible
     * [text] names another site than the real [url].
     */
    fun linkWarning(text: String, url: String): String? {
        val real = host(url) ?: return null
        if (real.split('.').any { it.startsWith("xn--") }) return "Адрес записан кодом (punycode): $real может подделываться под другой сайт"
        val letters = real.filter { it.isLetter() }
        if (letters.any { it in 'а'..'я' || it == 'ё' } && letters.any { it in 'a'..'z' }) return "В адресе смешаны русские и латинские буквы: $real"
        val shown = host(text)
        if (shown != null && shown != real && !real.endsWith(".$shown")) return "Текст ссылки — $shown, а ведёт она на $real"
        return null
    }

    fun optionId(bytes: ByteArray): String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    fun optionBytes(id: String): ByteArray = if (id.isEmpty()) ByteArray(0) else runCatching { android.util.Base64.decode(id, android.util.Base64.NO_WRAP) }.getOrDefault(ByteArray(0))
}

internal class KxChat3(private val services: TelegramServices, private val app: Context, private val scope: CoroutineScope) {
    private val client get() = services.client
    private val names = java.util.concurrent.ConcurrentHashMap<Long, String>()
    private val titles = java.util.concurrent.ConcurrentHashMap<Long, String>()
    @Volatile private var myId = 0L

    // ---- kiroX-local chat prefs (KxChatPrefs) ----
    private val prefs by lazy { app.getSharedPreferences("kirox_v3_chat3", Context.MODE_PRIVATE) }
    var values by mutableStateOf(emptyMap<String, Int>())
        private set
    fun owns(key: String) = key.startsWith("c3.")
    fun put(key: String, value: Int) { values = values + (key to value); scope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs.edit().putInt(key, value).apply() } }

    // ---- state the getters read ----
    var pinnedLimit by mutableStateOf(5)
        private set
    var sensitive by mutableStateOf<Boolean?>(null)
        private set
    private var quickOverride by mutableStateOf<String?>(null)
    val quickReaction: String? get() = quickOverride ?: services.chat3Updates.defaultReaction
    val chatThemes: List<String> get() = services.chat3Updates.chatThemes

    fun start() {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val loaded = prefs.all.filter { (k, v) -> k.startsWith("c3.") && v is Int }.mapValues { it.value as Int }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { values = loaded + values }
        }
        scope.launch {
            // Options are known once TDLib is ready; read them then (and again after an account switch).
            client.ready.collect { ready ->
                if (!ready) return@collect
                myId = 0L
                intOption("pinned_chat_count_max")?.let { pinnedLimit = it }
                sensitive = if (boolOption("can_ignore_sensitive_content_restrictions") == true) boolOption("ignore_sensitive_content_restrictions") ?: false else null
            }
        }
    }

    private suspend fun intOption(name: String): Int? = orNull { (client.query(TdApi.GetOption(name)) as? TdApi.OptionValueInteger)?.value?.toInt() }
    private suspend fun boolOption(name: String): Boolean? = orNull { client.query(TdApi.GetOption(name)) }.let { (it as? TdApi.OptionValueBoolean)?.value ?: if (it is TdApi.OptionValueEmpty) false else null }

    // ---- small helpers ----
    private suspend fun me(): Long { if (myId == 0L) myId = client.query(TdApi.GetMe()).id; return myId }

    private suspend fun userName(id: Long): String = names[id] ?: orNull { client.query(TdApi.GetUser(id)) }?.let { u ->
        listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Удалённый аккаунт" }.also { names[id] = it }
    } ?: "Пользователь"

    private suspend fun chatTitle(id: Long): String = titles[id] ?: orNull { client.query(TdApi.GetChat(id)) }?.title?.also { titles[id] = it }.orEmpty()

    private suspend fun senderName(s: TdApi.MessageSender?): String = when (s) {
        is TdApi.MessageSenderUser -> userName(s.userId)
        is TdApi.MessageSenderChat -> chatTitle(s.chatId)
        else -> ""
    }

    /** Local path of a small file (avatar, thumbnail), downloading it first when needed (≤ 4 s). */
    private suspend fun small(file: TdApi.File?): String? {
        file ?: return null
        file.local?.path?.takeIf { file.local.isDownloadingCompleted && it.isNotBlank() }?.let { return it }
        return withTimeoutOrNull(4000) { orNull { client.query(TdApi.DownloadFile(file.id, 1, 0, 0, true)) }?.local?.path?.takeIf { it.isNotBlank() } }
    }

    private fun sender(id: Long): TdApi.MessageSender = if (id > 0) TdApi.MessageSenderUser(id) else TdApi.MessageSenderChat(id)

    private suspend fun found(chatId: Long, m: TdApi.Message) = KxFound(chatId, m.id, chatTitle(chatId), KxChat3Mapping.preview(m.content), KxFormat.chatTime(m.date))

    private fun options(o: KxSendOptions): TdApi.MessageSendOptions? {
        val schedule = KxScheduleMapping.state(o.at) // features: one mapping, unit-tested
        if (!o.silent && schedule == null && o.effectId == 0L) return null
        return TdApi.MessageSendOptions(null, o.silent, false, false, false, 0, false, schedule, o.effectId, 0, false)
    }

    private suspend fun sendContent(chatId: Long, content: TdApi.InputMessageContent, o: KxSendOptions = KxSendOptions()) {
        client.query(TdApi.SendMessage(chatId, null, o.replyTo?.let { TdApi.InputMessageReplyToMessage(it, null, 0, null) }, options(o), null, content))
    }

    // ---- sending ----
    suspend fun sendWith(chatId: Long, text: String, o: KxSendOptions) {
        val preview = TdApi.LinkPreviewOptions(o.noPreview, o.previewUrl, o.previewLarge == false, o.previewLarge == true, o.previewAbove)
        sendContent(chatId, TdApi.InputMessageText(KxChat3Mapping.formatted(text, o.entities), preview, true), o)
    }

    suspend fun sendDice(chatId: Long, emoji: String) = sendContent(chatId, TdApi.InputMessageDice(emoji, false))

    suspend fun sendContact(chatId: Long, userId: Long, phone: String, first: String, last: String) =
        sendContent(chatId, TdApi.InputMessageContact(TdApi.Contact(phone, first, last, "", userId)))

    /** View-once (self-destruct) media exists only in private chats. */
    suspend fun sendViewOnce(chatId: Long, path: String): String? {
        val type = orNull { client.query(TdApi.GetChat(chatId)) }?.type
        if (type !is TdApi.ChatTypePrivate) return "Только в личных чатах"
        return attempt { sendViewOncePhoto(chatId, path) }
    }

    private suspend fun sendViewOncePhoto(chatId: Long, path: String) = sendContent(chatId, TdApi.InputMessagePhoto().apply {
        photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
        caption = TdApi.FormattedText("", emptyArray())
        selfDestructType = TdApi.MessageSelfDestructTypeImmediately()
    })

    suspend fun sendChecklist(chatId: Long, title: String, tasks: List<String>, add: Boolean, mark: Boolean) = sendContent(chatId, TdApi.InputMessageChecklist(
        TdApi.InputChecklist(KxChat3Mapping.formatted(title), tasks.filter { it.isNotBlank() }.mapIndexed { i, t -> TdApi.InputChecklistTask(i + 1, KxChat3Mapping.formatted(t)) }.toTypedArray(), add, mark)))

    // ---- send as ----
    suspend fun sendAsOptions(chatId: Long): List<KxSender> {
        val current = orNull { client.query(TdApi.GetChat(chatId)) }?.messageSenderId
        val currentId = when (current) { is TdApi.MessageSenderUser -> current.userId; is TdApi.MessageSenderChat -> current.chatId; else -> 0L }
        val list = orNull { client.query(TdApi.GetChatAvailableMessageSenders(chatId)) }?.senders.orEmpty()
        return list.map { s ->
            when (val who = s.sender) {
                is TdApi.MessageSenderUser -> {
                    val u = orNull { client.query(TdApi.GetUser(who.userId)) }
                    KxSender(who.userId, userName(who.userId), "личный аккаунт", s.needsPremium, who.userId == currentId || (currentId == 0L && who.userId == me()),
                        small(u?.profilePhoto?.small))
                }
                is TdApi.MessageSenderChat -> {
                    val c = orNull { client.query(TdApi.GetChat(who.chatId)) }
                    val channel = (c?.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
                    KxSender(who.chatId, c?.title.orEmpty(), if (who.chatId == chatId) "анонимно от имени группы" else if (channel) "канал" else "группа",
                        s.needsPremium, who.chatId == currentId, small(c?.photo?.small), channel)
                }
                else -> KxSender(0, "")
            }
        }.filter { it.id != 0L }
    }

    suspend fun chooseSendAs(chatId: Long, senderId: Long) { client.query(TdApi.SetChatMessageSender(chatId, sender(senderId))) }

    // ---- effects, themes, inline bots ----
    suspend fun messageEffects(): List<KxEffect> = services.chat3Updates.effectIds.take(80).mapNotNull { id ->
        orNull { client.query(TdApi.GetMessageEffect(id)) }?.let { KxEffect(it.id, it.emoji, it.isPremium) }
    }

    suspend fun applyChatTheme(chatId: Long, name: String) { client.query(TdApi.SetChatTheme(chatId, if (name.isBlank()) null else TdApi.InputChatThemeEmoji(name))) }

    suspend fun inlineQuery(chatId: Long, bot: String, query: String, offset: String): KxInlineResults? {
        val name = bot.trim().removePrefix("@")
        if (name.isEmpty()) return null
        val chat = orNull { client.query(TdApi.SearchPublicChat(name)) } ?: return null
        val botId = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: return null
        val r = client.query(TdApi.GetInlineQueryResults(botId, chatId, null, query, offset))
        val items = coroutineScope {
            r.results.take(30).map { res ->
                async {
                    when (res) {
                        is TdApi.InlineQueryResultAnimation -> KxInlineResult(res.id, "gif", res.title, thumb = small(res.animation.thumbnail?.file))
                        is TdApi.InlineQueryResultSticker -> KxInlineResult(res.id, "sticker", res.sticker.emoji, thumb = small(res.sticker.thumbnail?.file ?: res.sticker.sticker.takeIf { res.sticker.format is TdApi.StickerFormatWebp }))
                        is TdApi.InlineQueryResultPhoto -> KxInlineResult(res.id, "photo", res.title, res.description, small(res.photo.sizes.minByOrNull { it.width }?.photo))
                        is TdApi.InlineQueryResultVideo -> KxInlineResult(res.id, "video", res.title, res.description, small(res.video.thumbnail?.file))
                        is TdApi.InlineQueryResultArticle -> KxInlineResult(res.id, "article", res.title, res.description, small(res.thumbnail?.file))
                        is TdApi.InlineQueryResultDocument -> KxInlineResult(res.id, "other", res.title, res.description)
                        is TdApi.InlineQueryResultAudio -> KxInlineResult(res.id, "other", res.audio.title.ifBlank { res.audio.fileName }, res.audio.performer)
                        is TdApi.InlineQueryResultVoiceNote -> KxInlineResult(res.id, "other", res.title.ifBlank { "Голосовое" })
                        is TdApi.InlineQueryResultContact -> KxInlineResult(res.id, "other", listOf(res.contact.firstName, res.contact.lastName).joinToString(" ").trim(), res.contact.phoneNumber)
                        is TdApi.InlineQueryResultLocation -> KxInlineResult(res.id, "other", res.title.ifBlank { "Место" })
                        is TdApi.InlineQueryResultVenue -> KxInlineResult(res.id, "other", res.venue.title, res.venue.address)
                        is TdApi.InlineQueryResultGame -> KxInlineResult(res.id, "other", res.game.title, res.game.description)
                        else -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        return KxInlineResults(r.inlineQueryId, name, items, r.nextOffset)
    }

    suspend fun sendInlineResult(chatId: Long, queryId: Long, resultId: String) {
        client.query(TdApi.SendInlineQueryResultMessage(chatId, null, null, null, queryId, resultId, false))
    }

    // ---- chat list ----
    suspend fun reorderPinned(chatIds: List<Long>) { client.query(TdApi.SetPinnedChats(TdApi.ChatListMain(), chatIds.toLongArray())) }

    // ---- one message ----
    suspend fun messageInfo(chatId: Long, messageId: Long): KxMsgInfo? {
        val m = orNull { client.query(TdApi.GetMessage(chatId, messageId)) } ?: return null
        val chat = orNull { client.query(TdApi.GetChat(chatId)) }
        val (read, note) = if (m.isOutgoing && chat?.type is TdApi.ChatTypePrivate)
            orNull { client.query(TdApi.GetMessageReadDate(chatId, messageId)) }?.let { KxChat3Mapping.readNote(it) } ?: ("" to "")
        else "" to ""
        val via = if (m.viaBotUserId != 0L) orNull { client.query(TdApi.GetUser(m.viaBotUserId)) }?.usernames?.editableUsername?.let { "@$it" }.orEmpty() else ""
        return KxMsgInfo(KxFormat.dateTime(m.date), if (m.editDate > 0) KxFormat.dateTime(m.editDate) else "", read, note,
            m.interactionInfo?.viewCount ?: 0, m.interactionInfo?.forwardCount ?: 0, via, m.autoDeleteIn.toInt())
    }

    suspend fun messageViewers(chatId: Long, messageId: Long): List<KxViewer> {
        val viewers = orNull { client.query(TdApi.GetMessageViewers(chatId, messageId)) }?.viewers.orEmpty()
        val reactions = orNull { client.query(TdApi.GetMessageAddedReactions(chatId, messageId, null, "", 100)) }?.reactions.orEmpty()
            .mapNotNull { r -> ((r.senderId as? TdApi.MessageSenderUser)?.userId ?: return@mapNotNull null) to ((r.type as? TdApi.ReactionTypeEmoji)?.emoji ?: "⭐") }.toMap()
        val seen = viewers.sortedByDescending { it.viewDate }.map { v ->
            val u = orNull { client.query(TdApi.GetUser(v.userId)) }
            KxViewer(v.userId, userName(v.userId), KxFormat.dateTime(v.viewDate), reactions[v.userId].orEmpty(), small(u?.profilePhoto?.small))
        }
        // wave5 («Кто отреагировал»): people who reacted but are not in the viewer list (big groups, channels, older messages).
        val seenIds = seen.map { it.userId }.toSet()
        return seen + reactions.filterKeys { it !in seenIds }.map { (uid, emoji) ->
            val u = orNull { client.query(TdApi.GetUser(uid)) }
            KxViewer(uid, userName(uid), "", emoji, small(u?.profilePhoto?.small))
        }
    }

    suspend fun reactionPanel(chatId: Long, messageId: Long): KxReactionPanel? {
        val r = orNull { client.query(TdApi.GetMessageAvailableReactions(chatId, messageId, 8)) } ?: return null
        fun emojis(a: Array<TdApi.AvailableReaction>) = a.mapNotNull { (it.type as? TdApi.ReactionTypeEmoji)?.emoji }
        val premium = (r.topReactions + r.recentReactions + r.popularReactions).filter { it.needsPremium }.mapNotNull { (it.type as? TdApi.ReactionTypeEmoji)?.emoji }.toSet()
        return KxReactionPanel(emojis(r.topReactions), emojis(r.recentReactions), emojis(r.popularReactions), premium, r.allowCustomEmoji)
    }

    fun changeQuickReaction(emoji: String) {
        quickOverride = emoji
        client.send(TdApi.SetDefaultReactionType(TdApi.ReactionTypeEmoji(emoji)))
    }

    suspend fun replaceMedia(chatId: Long, messageId: Long, path: String, video: Boolean) {
        val old = orNull { client.query(TdApi.GetMessage(chatId, messageId)) }
        val caption = formattedTextOf(old?.content) ?: TdApi.FormattedText("", emptyArray())
        val content: TdApi.InputMessageContent = if (video) TdApi.InputMessageVideo().apply {
            this.video = TdApi.InputVideo().apply { this.video = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = caption
        } else TdApi.InputMessagePhoto().apply {
            photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = caption
        }
        client.query(TdApi.EditMessageMedia(chatId, messageId, null, content))
    }

    suspend fun clickBigEmoji(chatId: Long, messageId: Long) { orNull { client.query(TdApi.ClickAnimatedEmojiMessage(chatId, messageId)) } }

    suspend fun checklist(chatId: Long, messageId: Long): KxChecklist? {
        val list = (orNull { client.query(TdApi.GetMessage(chatId, messageId)) }?.content as? TdApi.MessageChecklist)?.list ?: return null
        return KxChecklist(list.title.text, list.tasks.map { t -> KxChecklistTask(t.id, t.text.text, t.completedBy != null, if (t.completedBy != null) senderName(t.completedBy) else "") },
            list.canAddTasks, list.canMarkTasksAsDone)
    }

    suspend fun markChecklist(chatId: Long, messageId: Long, done: List<Int>, notDone: List<Int>) {
        client.query(TdApi.MarkChecklistTasksAsDone(chatId, messageId, done.toIntArray(), notDone.toIntArray()))
    }

    // ---- history without opening ----
    suspend fun unreadMentions(chatId: Long, reactions: Boolean): List<KxFound> {
        val filter: TdApi.SearchMessagesFilter = if (reactions) TdApi.SearchMessagesFilterUnreadReaction() else TdApi.SearchMessagesFilterUnreadMention()
        val r = orNull { client.query(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 50, filter)) } ?: return emptyList()
        return r.messages.map { found(chatId, it) }
    }

    suspend fun peekHistory(chatId: Long, limit: Int): List<KxMessage> {
        val got = LinkedHashMap<Long, TdApi.Message>()
        var from = 0L
        // TDLib often answers the first page with what it has cached (sometimes one message): ask a few times.
        repeat(4) {
            if (got.size >= limit) return@repeat
            val page = orNull { client.query(TdApi.GetChatHistory(chatId, from, 0, limit - got.size, false)) }?.messages.orEmpty()
            if (page.isEmpty()) return@repeat
            page.forEach { got[it.id] = it }
            from = page.last().id
        }
        val group = orNull { client.query(TdApi.GetChat(chatId)) }?.type.let { it is TdApi.ChatTypeBasicGroup || (it is TdApi.ChatTypeSupergroup && !it.isChannel) }
        return got.values.sortedBy { it.id }.map { m ->
            KxMessage(m.id, m.isOutgoing, formattedTextOf(m.content)?.text.orEmpty().ifEmpty { KxChat3Mapping.preview(m.content) }, KxFormat.messageTime(m.date),
                KxChat3Mapping.kind(kindOf(m.content), m.content), sender = if (group && !m.isOutgoing) senderName(m.senderId) else null)
        }
    }

    /**
     * fixer-chat4: days of [year]-[month] (1..31) with at least one message — walks back day by day with
     * GetChatMessageByDate (one request per day that has messages, not per calendar day).
     */
    suspend fun messageDays(chatId: Long, year: Int, month: Int): Set<Int> {
        val zone = java.time.ZoneId.systemDefault()
        val first = java.time.LocalDate.of(year, month, 1)
        val start = first.atStartOfDay(zone).toEpochSecond()
        var cursor = minOf(first.plusMonths(1).atStartOfDay(zone).toEpochSecond() - 1, System.currentTimeMillis() / 1000 + 60)
        val out = HashSet<Int>()
        repeat(32) {
            if (cursor < start) return out
            val m = orNull { client.query(TdApi.GetChatMessageByDate(chatId, cursor.toInt())) } ?: return out
            if (m.id == 0L || m.date < start) return out
            val d = java.time.Instant.ofEpochSecond(m.date.toLong()).atZone(zone).toLocalDate()
            out += d.dayOfMonth
            cursor = d.atStartOfDay(zone).toEpochSecond() - 1
        }
        return out
    }

    /** fixer-chat4: the full-screen animation of a message effect: the reaction effect, or the Premium sticker's animation. */
    suspend fun messageEffectFile(effectId: Long): Int? {
        val e = orNull { client.query(TdApi.GetMessageEffect(effectId)) } ?: return null
        return when (val t = e.type) {
            is TdApi.MessageEffectTypeEmojiReaction -> t.effectAnimation?.sticker?.id
            is TdApi.MessageEffectTypePremiumSticker -> (t.sticker?.fullType as? TdApi.StickerFullTypeRegular)?.premiumAnimation?.id ?: t.sticker?.sticker?.id
            else -> null
        }
    }

    suspend fun messageAtDate(chatId: Long, dayStart: Int): Long? {
        val m = orNull { client.query(TdApi.GetChatMessageByDate(chatId, dayStart + 86_399)) } ?: return null
        return m.id.takeIf { m.date >= dayStart }
    }

    suspend fun memories(): List<KxFound> {
        val out = ArrayList<KxFound>()
        val fmt = java.text.SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        for (years in 1..6) {
            val start = Calendar.getInstance().apply { add(Calendar.YEAR, -years); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val from = (start.timeInMillis / 1000).toInt()
            val r = orNull { client.query(TdApi.SearchMessages(TdApi.ChatListMain(), "", "", 30, TdApi.SearchMessagesFilterPhotoAndVideo(), null, from, from + 86_399)) } ?: continue
            r.messages.forEach { m -> out += KxFound(m.chatId, m.id, chatTitle(m.chatId), KxChat3Mapping.preview(m.content), fmt.format(java.util.Date(m.date * 1000L))) }
        }
        return out
    }

    // ---- link preview, translation, entities ----
    suspend fun linkPreview(text: String): KxLinkPreview? = orNull { client.query(TdApi.GetLinkPreview(KxChat3Mapping.formatted(text), null)) }?.let {
        KxLinkPreview(it.url, it.siteName.ifBlank { it.displayUrl }, it.title, it.description?.text.orEmpty(), it.hasLargeMedia)
    }

    suspend fun translateText(text: String, toLang: String): String? = orNull { client.query(TdApi.TranslateText(KxChat3Mapping.formatted(text), toLang, "")) }?.text

    suspend fun textEntities(text: String): List<KxEntity> = orNull { client.query(TdApi.GetTextEntities(text)) }?.entities.orEmpty().mapNotNull { KxChat3Mapping.detected(text, it) }

    // ---- stickers ----
    private suspend fun sticker(s: TdApi.Sticker, withFile: Boolean): KxSticker {
        val webp = s.format is TdApi.StickerFormatWebp
        val path = if (!withFile) null else small(if (webp) s.sticker else s.thumbnail?.file)
        return KxSticker("f${s.sticker.id}", s.emoji, path, s.sticker.id, animated = !webp)
    }

    private suspend fun info(set: TdApi.StickerSet) = KxStickerSetInfo(set.id, set.name, set.title, set.stickers.size,
        set.stickers.any { it.format !is TdApi.StickerFormatWebp }, set.isInstalled, set.isArchived, set.isOfficial,
        coroutineScope { set.stickers.mapIndexed { i, s -> async { sticker(s, i < 40) } }.awaitAll() })

    private suspend fun info(set: TdApi.StickerSetInfo) = KxStickerSetInfo(set.id, set.name, set.title, set.size,
        set.covers.any { it.format !is TdApi.StickerFormatWebp }, set.isInstalled, set.isArchived, set.isOfficial, set.covers.take(1).map { sticker(it, true) })

    suspend fun stickerSetByName(name: String): KxStickerSetInfo? {
        val clean = name.trim().substringAfterLast('/').substringBefore('?')
        clean.toLongOrNull()?.let { id -> return stickerSetInfo(id) }
        return orNull { client.query(TdApi.SearchStickerSet(clean, false)) }?.let { info(it) }
    }
    suspend fun stickerSetInfo(setId: Long): KxStickerSetInfo? = orNull { client.query(TdApi.GetStickerSet(setId)) }?.let { info(it) }
    suspend fun installedStickerSets(): List<KxStickerSetInfo> = orNull { client.query(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())) }?.sets.orEmpty().map { info(it) }
    suspend fun archivedStickerSets(): List<KxStickerSetInfo> = orNull { client.query(TdApi.GetArchivedStickerSets(TdApi.StickerTypeRegular(), 0, 100)) }?.sets.orEmpty().map { info(it) }
    suspend fun changeStickerSet(setId: Long, installed: Boolean, archived: Boolean) { client.query(TdApi.ChangeStickerSet(setId, installed, archived)) }
    suspend fun reorderStickerSets(ids: List<Long>) = services.stickers.reorder(ids)
    suspend fun favoriteSticker(stickerId: String, favorite: Boolean) {
        val file = TdApi.InputFileId(stickerId.removePrefix("f").toIntOrNull() ?: throw IllegalArgumentException("sticker"))
        client.query(if (favorite) TdApi.AddFavoriteSticker(file) else TdApi.RemoveFavoriteSticker(file))
    }

    // ---- gifts ----
    suspend fun receivedGifts(): List<KxReceivedGift> = giftsOf(me())

    suspend fun giftsOf(userId: Long): List<KxReceivedGift> {
        val r = orNull { client.query(TdApi.GetReceivedGifts("", TdApi.MessageSenderUser(userId), 0, false, false, false, false, false, false, false, false, false, "", 100)) }
            ?: return emptyList()
        return r.gifts.sortedByDescending { it.date }.map { g ->
            val (emoji, file) = when (val gift = g.gift) {
                is TdApi.SentGiftRegular -> gift.gift.sticker.emoji to (gift.gift.sticker.thumbnail?.file ?: gift.gift.sticker.sticker.takeIf { gift.gift.sticker.format is TdApi.StickerFormatWebp })
                is TdApi.SentGiftUpgraded -> "🎁" to null
                else -> "🎁" to null
            }
            KxReceivedGift(g.receivedGiftId, emoji.ifBlank { "🎁" }, if (g.senderId == null) "Скрытый отправитель" else senderName(g.senderId),
                g.text?.text.orEmpty(), KxFormat.dateTime(g.date), g.sellStarCount, g.isSaved, small(file))
        }
    }
    suspend fun keepGift(giftId: String, saved: Boolean) { client.query(TdApi.ToggleGiftIsSaved(giftId, saved)) }
    suspend fun sellGift(giftId: String) { client.query(TdApi.SellGift("", giftId)) }

    // ---- contacts ----
    suspend fun userNote(userId: Long): String? = orNull { client.query(TdApi.GetUserFullInfo(userId)) }?.note?.text
    suspend fun saveUserNote(userId: Long, text: String) { client.query(TdApi.SetUserNote(userId, KxChat3Mapping.formatted(text.trim()))) }
    suspend fun addContactById(userId: Long, first: String, last: String, sharePhone: Boolean) {
        client.query(TdApi.AddContact(userId, TdApi.ImportedContact("", first.trim(), last.trim(), null), sharePhone))
    }
    suspend fun sharePhone(userId: Long) { client.query(TdApi.SharePhoneNumber(userId)) }

    /** Birthdays found by the one wider scan of this session (user id → day, month, year). */
    private var scanned: Map<Long, Triple<Int, Int, Int>>? = null

    /**
     * Telegram's own «close birthdays» (UpdateContactCloseBirthdays, what the official apps show) plus, once per
     * session, a small scan of up to 50 contacts one by one — it stops at the first FLOOD_WAIT so the account is not limited.
     */
    suspend fun birthdays(): List<KxBirthday> {
        val known = LinkedHashMap<Long, Triple<Int, Int, Int>>()
        services.chat3Updates.closeBirthdays.forEach { (id, b) -> known[id] = b }
        val scan = scanned ?: run {
            val found = LinkedHashMap<Long, Triple<Int, Int, Int>>()
            val ids = orNull { client.query(TdApi.GetContacts()) }?.userIds.orEmpty().take(50)
            for (id in ids) {
                val info = try { client.query(TdApi.GetUserFullInfo(id)) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: com.kirox.app.telegram.TdException) {
                    if (e.error.code == 429) break else null
                } catch (e: Exception) { null }
                info?.birthdate?.let { found[id] = Triple(it.day, it.month, it.year) }
            }
            found.also { scanned = it }
        }
        scan.forEach { (id, b) -> known.putIfAbsent(id, b) }
        val sorted = known.map { (id, b) -> KxBirthday(id, userName(id), b.first, b.second, b.third, KxChat3Mapping.daysUntil(b.first, b.second)) }.sortedBy { it.daysLeft }
        // Avatars only for the first ones shown.
        return sorted.mapIndexed { i, b -> if (i < 12) b.copy(avatar = small(orNull { client.query(TdApi.GetUser(b.userId)) }?.profilePhoto?.small)) else b }
    }

    // ---- reports, leaving, timers, notifications ----
    /** Telegram's report flow (ReportChat), keeping TDLib's reason when it fails. */
    suspend fun reportChat(chatId: Long, messageIds: List<Long>, optionId: String, text: String): KxReportStep = try {
        when (val r = client.query(TdApi.ReportChat(chatId, KxChat3Mapping.optionBytes(optionId), messageIds.toLongArray(), text))) {
            is TdApi.ReportChatResultOk -> KxReportStep(done = true)
            is TdApi.ReportChatResultOptionRequired -> KxReportStep(title = r.title, options = r.options.map { KxChat3Mapping.optionId(it.id) to it.text })
            is TdApi.ReportChatResultTextRequired -> KxReportStep(textNeeded = true, textOptional = r.isOptional, optionId = KxChat3Mapping.optionId(r.optionId))
            is TdApi.ReportChatResultMessagesRequired -> KxReportStep(error = "Telegram просит выбрать сообщения: пожалуйтесь из меню сообщения")
            else -> KxReportStep(error = "Жалоба не отправилась")
        }
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { KxReportStep(error = KxExtrasMapping.errorText(e)) }

    suspend fun leaveChat(chatId: Long) {
        val type = client.query(TdApi.GetChat(chatId)).type
        if (type is TdApi.ChatTypePrivate || type is TdApi.ChatTypeSecret) throw IllegalStateException("not a group")
        client.query(TdApi.LeaveChat(chatId))
        if (type is TdApi.ChatTypeBasicGroup) orNull { client.query(TdApi.DeleteChatHistory(chatId, true, false)) }
    }

    suspend fun autoDeleteOf(chatId: Long): Int? = orNull { client.query(TdApi.GetChat(chatId)) }?.messageAutoDeleteTime
    suspend fun defaultAutoDelete(): Int? = orNull { client.query(TdApi.GetDefaultMessageAutoDeleteTime()) }?.time
    suspend fun changeDefaultAutoDelete(seconds: Int) { client.query(TdApi.SetDefaultMessageAutoDeleteTime(TdApi.MessageAutoDeleteTime(seconds))) }

    suspend fun loadChatNotify(chatId: Long): KxChatNotify? = orNull { client.query(TdApi.GetChat(chatId)) }?.notificationSettings?.let {
        KxChatNotify(it.muteFor, it.showPreview, !it.disableMentionNotifications, !it.disablePinnedMessageNotifications, it.useDefaultMuteFor)
    }

    suspend fun saveChatNotify(chatId: Long, v: KxChatNotify) {
        val s = client.query(TdApi.GetChat(chatId)).notificationSettings
        s.useDefaultMuteFor = false; s.muteFor = v.muteFor
        s.useDefaultShowPreview = false; s.showPreview = v.preview
        s.useDefaultDisableMentionNotifications = false; s.disableMentionNotifications = !v.mentions
        s.useDefaultDisablePinnedMessageNotifications = false; s.disablePinnedMessageNotifications = !v.pinned
        client.query(TdApi.SetChatNotificationSettings(chatId, s))
    }

    // ---- public chats ----
    private suspend fun publicChat(id: Long): KxPublicChat? {
        val c = orNull { client.query(TdApi.GetChat(id)) } ?: return null
        val sg = (c.type as? TdApi.ChatTypeSupergroup)?.let { orNull { client.query(TdApi.GetSupergroup(it.supergroupId)) } }
        val joined = when (sg?.status) { is TdApi.ChatMemberStatusLeft, is TdApi.ChatMemberStatusBanned, null -> false; else -> true }
        val username = sg?.usernames?.let { it.editableUsername.ifBlank { it.activeUsernames.firstOrNull().orEmpty() } }.orEmpty()
        return KxPublicChat(c.id, c.title, username, sg?.memberCount ?: 0, sg?.isChannel ?: false, joined, small(c.photo?.small), sg?.verificationStatus?.isVerified == true)
    }
    private suspend fun publicChats(ids: LongArray?): List<KxPublicChat> = coroutineScope { ids.orEmpty().take(40).map { async { publicChat(it) } }.awaitAll().filterNotNull() }
    private fun LongArray?.orEmpty() = this ?: LongArray(0)
    suspend fun recommendedChats() = publicChats(orNull { client.query(TdApi.GetRecommendedChats()) }?.chatIds)
    suspend fun searchPublicChats(query: String) = if (query.isBlank()) emptyList() else publicChats(orNull { client.query(TdApi.SearchPublicChats(query.trim(), null)) }?.chatIds)
    suspend fun similarChats(chatId: Long) = publicChats(orNull { client.query(TdApi.GetChatSimilarChats(chatId)) }?.chatIds)
    suspend fun commonGroups(userId: Long) = publicChats(orNull { client.query(TdApi.GetGroupsInCommon(userId, 0, 100)) }?.chatIds)

    suspend fun chatFacts(chatId: Long): KxChatFacts? {
        val c = orNull { client.query(TdApi.GetChat(chatId)) } ?: return null
        return when (val t = c.type) {
            is TdApi.ChatTypeSupergroup -> {
                val sg = orNull { client.query(TdApi.GetSupergroup(t.supergroupId)) } ?: return null
                val linked = if (sg.hasLinkedChat) orNull { client.query(TdApi.GetSupergroupFullInfo(t.supergroupId)) }?.linkedChatId ?: 0L else 0L
                KxChatFacts(sg.status is TdApi.ChatMemberStatusCreator, sg.status is TdApi.ChatMemberStatusCreator || sg.status is TdApi.ChatMemberStatusAdministrator,
                    sg.verificationStatus?.isVerified == true, sg.verificationStatus?.isScam == true, sg.verificationStatus?.isFake == true, linked)
            }
            is TdApi.ChatTypeBasicGroup -> orNull { client.query(TdApi.GetBasicGroup(t.basicGroupId)) }?.let {
                KxChatFacts(it.status is TdApi.ChatMemberStatusCreator, it.status is TdApi.ChatMemberStatusCreator || it.status is TdApi.ChatMemberStatusAdministrator)
            }
            is TdApi.ChatTypePrivate -> orNull { client.query(TdApi.GetUser(t.userId)) }?.verificationStatus.let {
                KxChatFacts(verified = it?.isVerified == true, scam = it?.isScam == true, fake = it?.isFake == true)
            }
            else -> KxChatFacts()
        }
    }

    // ---- account option ----
    fun allowSensitive(on: Boolean) {
        if (sensitive == null) return
        sensitive = on
        client.send(TdApi.SetOption("ignore_sensitive_content_restrictions", TdApi.OptionValueBoolean(on)))
    }
}
