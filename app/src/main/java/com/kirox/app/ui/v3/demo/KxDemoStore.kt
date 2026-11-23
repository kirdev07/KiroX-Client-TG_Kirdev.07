package com.kirox.app.ui.v3.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.ui.v3.model.*

/**
 * Demo data for the v3 preview app and screenshot tests: the same people and texts as the
 * approved mockups. Actions change the in-memory lists so the preview behaves like the app.
 * Never talks to TDLib. Avatar colors follow the boards (colorIndex = KxAvatarPalette order).
 */
class KxDemoStore : KxStore {
    override val me = KxProfile(1, "Алекс Миронов", "alex_codes", "+7 701 123 45 67", "Пишу Android-приложения. Люблю тёмные темы.", colorIndex = 7)

    override val accounts get() = listOf(me, KxProfile(2, "Рабочий", phone = "+7 702 *** ** 11", colorIndex = 6))

    // Demo keeps the board's type tabs; the real store shows the user's Telegram folders.
    override val folders = listOf(KxFolder(0, "Все", 6), KxFolder(-1, "Личные"), KxFolder(-2, "Группы"), KxFolder(-3, "Каналы"), KxFolder(4, "Работа", 2))

    private val list = mutableStateListOf(
        KxChat(10, "Избранное", KxChatKind.Saved, "Список дел на завтра", time = "22:26", pinned = true),
        KxChat(11, "Анна", preview = "Беру билеты на 19:30", time = "21:58", unread = 2, pinned = true, online = true, typing = "печатает", hasStory = true, colorIndex = 0),
        KxChat(12, "Команда kiroX", KxChatKind.Group, "сборка прошла, проверяйте", previewSender = "Максим", time = "21:29", unread = 14, unreadMention = true, colorIndex = 2),
        KxChat(13, "Мама", preview = "Позвони, когда будешь…", time = "21:02", unread = 1, colorIndex = 3),
        KxChat(14, "Новости", KxChatKind.Channel, "Обновление Android 17 уже доступно", time = "20:10", unread = 37, muted = true, verified = true, colorIndex = 4, avatarIcon = "campaign"),
        KxChat(15, "Илья", preview = "Ок, тогда до завтра 👍", previewSender = "Вы", time = "19:36", out = KxOutState.Read, colorIndex = 5),
        KxChat(16, "Света", preview = "🎤 Голосовое сообщение · 0:24", time = "вчера", colorIndex = 6),
        KxChat(17, "Дмитрий", preview = "Скинь, что там за ошибка в сборке?", time = "вчера", online = true, colorIndex = 1),
        KxChat(18, "Book Club", KxChatKind.Group, "Собираемся по пятницам в 19:00", previewSender = "Мария", time = "пн", colorIndex = 7),
    )
    private val archive = mutableStateListOf(
        KxChat(30, "Скидки и акции", KxChatKind.Channel, "Только сегодня −30% на всё", time = "18:40", unread = 2, muted = true, archived = true, colorIndex = 5),
        KxChat(31, "Новости города", KxChatKind.Channel, "Перекрытие на проспекте до 20:00", time = "17:05", unread = 1, muted = true, archived = true, colorIndex = 1),
        KxChat(32, "Доставка 24", KxChatKind.Bot, "Ваш заказ передан курьеру", time = "вчера", unread = 1, muted = true, archived = true, colorIndex = 3),
    )
    override val chats: List<KxChat> get() = list
    override val archivedChats: List<KxChat> get() = archive

    override val stories = listOf(
        KxStoryPeer(1, "Вы", unseen = false, mine = true, colorIndex = 7),
        KxStoryPeer(11, "Анна", colorIndex = 0, time = "15 мин назад", slides = listOf(KxStorySlide(1, "Идём в кино 🎬", look = 0), KxStorySlide(2, "Сначала кофе ☕", look = 1), KxStorySlide(3, "Вечер удался", look = 2))),
        KxStoryPeer(20, "Максим", colorIndex = 1, time = "1 ч назад", slides = listOf(KxStorySlide(1, "Горы 🏔", look = 3), KxStorySlide(2, "Тропа к озеру", look = 4))),
        KxStoryPeer(12, "Команда kiroX", colorIndex = 2, time = "3 ч назад", slides = listOf(KxStorySlide(1, "Новая сборка уже здесь 🚀", look = 5))),
        KxStoryPeer(16, "Света", unseen = false, colorIndex = 6, time = "вчера", slides = listOf(KxStorySlide(1, "Море 🌊", look = 6))),
        KxStoryPeer(15, "Илья", unseen = false, colorIndex = 5, time = "вчера", slides = listOf(KxStorySlide(1, "Новый рекорд 🎮", look = 7))),
    )
    override val calls = listOf(
        KxCall(1, 11, "Анна", "21:40", KxCallDir.Incoming, duration = "12 мин", colorIndex = 0),
        KxCall(2, 20, "Максим", "20:15", KxCallDir.Missed, colorIndex = 1),
        KxCall(3, 12, "Команда kiroX", "18:02", KxCallDir.Outgoing, video = true, duration = "41 мин", colorIndex = 2, group = true),
        KxCall(4, 13, "Мама", "16:30", KxCallDir.Outgoing, duration = "3 мин", colorIndex = 3),
        KxCall(5, 16, "Света", "12:08", KxCallDir.Missed, video = true, colorIndex = 6),
        KxCall(6, 15, "Илья", "10:21", KxCallDir.Incoming, duration = "1 мин", colorIndex = 5),
    )
    override val contacts = listOf(
        KxContact(11, "Анна Ким", "в сети", online = true, hasStory = true, colorIndex = 0),
        KxContact(15, "Илья Петров", "был(а) 5 мин назад", colorIndex = 5),
        KxContact(20, "Максим Ли", "в сети", online = true, colorIndex = 1),
        KxContact(13, "Мама", "был(а) недавно", colorIndex = 3),
        KxContact(16, "Света Орлова", "в сети", online = true, colorIndex = 6),
        KxContact(17, "Дмитрий Ан", "был(а) на этой неделе", colorIndex = 2),
    )
    /** The board shows the sum of unread messages in unmuted chats. */
    override val unreadTotal: Int get() = list.filter { !it.muted }.sumOf { it.unread }
    override val missedCalls: Int get() = calls.count { it.dir == KxCallDir.Missed }
    override var connection: String? by mutableStateOf(null)
    override var error: String? by mutableStateOf(null)
    override fun clearError() { error = null }

    override fun chatsIn(folderId: Int): List<KxChat> = when (folderId) {
        -1 -> list.filter { it.kind == KxChatKind.Private || it.kind == KxChatKind.Saved || it.kind == KxChatKind.Secret }
        -2 -> list.filter { it.kind == KxChatKind.Group }
        -3 -> list.filter { it.kind == KxChatKind.Channel }
        4 -> list.filter { it.id == 12L || it.id == 17L }
        else -> list
    }.sortedByDescending { it.pinned } // wave5: pinned chats first (stable: the rest keep their order)

    private val pinnedMsg = mutableStateMapOf<Long, KxReply?>(11L to KxReply("Анна", "Встречаемся у кинотеатра в 19:15", 0))

    override fun header(chatId: Long): KxChatHeader {
        // wave5: a contact without a chat (Новое сообщение) gets its own empty private chat header, not «Анна».
        val c = (list + archive).firstOrNull { it.id == chatId }
            ?: contacts.firstOrNull { it.id == chatId }?.let { p -> KxChat(p.id, p.name, online = p.online, colorIndex = p.colorIndex) } ?: list[1]
        val status = when (c.kind) {
            KxChatKind.Saved -> ""
            KxChatKind.Group -> "8 участников, 3 в сети"
            KxChatKind.Channel -> "24 500 подписчиков"
            else -> if (c.online) "в сети" else "был(а) недавно"
        }
        return KxChatHeader(c.id, c.title, status, c.kind, c.online, muted = c.muted, pinned = pinnedMsg[chatId], colorIndex = c.colorIndex,
            composer = if (c.kind == KxChatKind.Channel) (if (c.muted) KxComposer.Unmute else KxComposer.Mute) else KxComposer.Write)
    }

    private val history = mutableStateMapOf<Long, List<KxMessage>>()
    override fun messages(chatId: Long): List<KxMessage> = history[chatId] ?: seed(chatId).also { history[chatId] = it }

    /** Chat «Анна» is exactly the board «Чат». */
    private fun seed(chatId: Long): List<KxMessage> = if (chatId == 11L) listOf(
        KxMessage(1, false, "Привет! Ты завтра свободна вечером?", "19:02", dayHeader = "Сегодня"),
        KxMessage(2, false, "Новый фильм в «Октябре» 🎬", "19:02", kind = KxMsgKind.Photo,
            reactions = listOf(KxReaction("🔥", 2, mine = true), KxReaction("❤", 1))),
        KxMessage(3, true, "Беру билеты на 19:30", "19:12", status = KxOutState.Read, edited = true, reply = KxReply("Анна", "Новый фильм в «Октябре» 🎬", 2)),
        KxMessage(4, false, "", "19:13", kind = KxMsgKind.VideoNote, meta = "0:05", played = .38f),
        KxMessage(5, true, "", "19:13", kind = KxMsgKind.Voice, meta = "0:18", status = KxOutState.Read, played = 4f / 14f,
            waveform = listOf(.40f, .75f, .55f, .90f, .60f, .35f, .70f, .95f, .50f, .30f, .65f, .45f, .25f, .55f)),
    ) else listOf(
        KxMessage(1, false, chats.firstOrNull { it.id == chatId }?.preview ?: "Привет!", "12:00", dayHeader = "Сегодня",
            sender = if (header(chatId).kind == KxChatKind.Group) "Максим" else null, senderColorIndex = 1),
    )

    private fun update(id: Long, f: (KxChat) -> KxChat) {
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i] = f(list[i]) else archive.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { archive[it] = f(archive[it]) }
    }
    override fun setPinned(chatId: Long, pinned: Boolean) = update(chatId) { it.copy(pinned = pinned) }
    override fun setMuted(chatId: Long, muted: Boolean) = update(chatId) { it.copy(muted = muted) }
    override fun markRead(chatId: Long) = update(chatId) { it.copy(unread = 0, unreadMention = false, unreadReaction = false, markedUnread = false) }
    override fun markUnread(chatId: Long) = update(chatId) { it.copy(markedUnread = true) }
    override fun setArchived(chatId: Long, archived: Boolean) {
        if (archived) list.firstOrNull { it.id == chatId }?.let { list.remove(it); archive.add(0, it.copy(archived = true)) }
        else archive.firstOrNull { it.id == chatId }?.let { archive.remove(it); list.add(it.copy(archived = false)) }
    }
    override fun clearHistory(chatId: Long, forEveryone: Boolean) { history[chatId] = emptyList() }
    override fun deleteChat(chatId: Long, forEveryone: Boolean) { list.removeAll { it.id == chatId }; archive.removeAll { it.id == chatId } }

    private var nextId = 1000L
    private fun now(): String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(java.util.Date())
    private fun append(chatId: Long, m: KxMessage, preview: String) {
        history[chatId] = messages(chatId) + m
        update(chatId) { it.copy(preview = preview, previewSender = "Вы", time = now(), out = KxOutState.Sent, typing = null) }
    }
    override fun send(chatId: Long, text: String, replyTo: Long?) {
        val msgs = messages(chatId)
        val reply = replyTo?.let { id -> msgs.firstOrNull { it.id == id } }?.let { KxReply(if (it.out) "Вы" else header(chatId).title, it.text.ifEmpty { "Медиа" }, it.id) }
        append(chatId, KxMessage(nextId++, true, text, now(), status = KxOutState.Sent, reply = reply), text)
    }
    private fun dur(s: Int) = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    override fun sendVoice(chatId: Long, seconds: Int, path: String?, waveform: ByteArray?) =
        append(chatId, KxMessage(nextId++, true, "", now(), KxMsgKind.Voice, KxOutState.Sent, meta = dur(seconds),
            waveform = listOf(.5f, .85f, .4f, .7f, .55f, .9f, .35f, .6f, .8f, .45f, .65f, .3f, .75f, .5f)), "🎤 Голосовое сообщение · ${dur(seconds)}")
    override fun sendVideoNote(chatId: Long, seconds: Int, path: String?, length: Int) =
        append(chatId, KxMessage(nextId++, true, "", now(), KxMsgKind.VideoNote, KxOutState.Sent, meta = dur(seconds)), "Видеосообщение")
    override fun peer(chatId: Long): KxPeer {
        val c = (list + archive).firstOrNull { it.id == chatId }
        return if (chatId == 11L) KxPeer(11, 11, "Анна Ким", "в сети", online = true, premium = true, bio = "Люблю кино и горы 🏔", username = "anna",
            phone = "+7 ··· ··· ·· ··", birthday = "14 марта", gifts = 3, commonGroups = 2, muted = c?.muted == true, colorIndex = 0,
            counts = mapOf(KxSharedTab.Media to 48, KxSharedTab.Files to 6, KxSharedTab.Music to 12, KxSharedTab.Links to 9, KxSharedTab.Voice to 21, KxSharedTab.Gif to 4))
        else KxPeer(chatId, chatId, c?.title ?: "Контакт", header(chatId).status, online = c?.online == true, muted = c?.muted == true, colorIndex = c?.colorIndex ?: -1)
    }
    override fun shared(chatId: Long, tab: KxSharedTab): List<KxSharedItem> = when (tab) {
        KxSharedTab.Media -> (0..5).map { KxSharedItem(it.toLong(), look = it, video = if (it == 1) "0:24" else if (it == 4) "0:08" else null) }
        KxSharedTab.Gif -> (0..2).map { KxSharedItem(it.toLong(), look = 10 + it) }
        KxSharedTab.Files -> listOf(KxSharedItem(1, "Билеты_Октябрь.pdf", "248 КБ · сегодня", fileExt = "pdf"), KxSharedItem(2, "Бюджет поездки.xlsx", "36 КБ · вчера", fileExt = "xlsx"),
            KxSharedItem(3, "Фото с гор.zip", "84 МБ · 12 нояб.", fileExt = "zip"))
        KxSharedTab.Music -> listOf(KxSharedItem(1, "Лето — Ночные огни", "3:42 · сейчас играет", playing = true, look = 0), KxSharedItem(2, "Северный ветер", "4:05", look = 1), KxSharedItem(3, "Город спит", "2:58", look = 2))
        KxSharedTab.Links -> listOf(KxSharedItem(1, "kinooktyabr.ru/afisha", "Афиша кинотеатра"), KxSharedItem(2, "maps.app/route", "Маршрут до озера"), KxSharedItem(3, "github.com/kirox", "kiroX на GitHub"))
        KxSharedTab.Voice -> listOf(KxSharedItem(1, "Голосовое · 0:18", "Анна · сегодня 19:13"), KxSharedItem(2, "Голосовое · 1:02", "Вы · вчера"), KxSharedItem(3, "Кружок · 0:05", "Анна · вчера", video = "circle"))
    }
    private var recVideo = false
    override fun recordStart(chatId: Long, video: Boolean) { recVideo = video }
    override fun recordStop(chatId: Long, send: Boolean, seconds: Int) {
        if (send) { if (recVideo) sendVideoNote(chatId, seconds) else sendVoice(chatId, seconds) }
    }
    override fun edit(chatId: Long, messageId: Long, text: String) {
        history[chatId] = messages(chatId).map { if (it.id == messageId) it.copy(text = text, edited = true) else it }
    }
    override fun delete(chatId: Long, messageIds: List<Long>, forEveryone: Boolean) {
        history[chatId] = messages(chatId).filterNot { it.id in messageIds }
    }
    override fun pin(chatId: Long, messageId: Long, pinned: Boolean) {
        val m = messages(chatId).firstOrNull { it.id == messageId }
        pinnedMsg[chatId] = if (pinned && m != null) KxReply(if (m.out) "Вы" else header(chatId).title, m.text.ifEmpty { "Медиа" }, m.id) else null
        history[chatId] = messages(chatId).map { if (it.id == messageId) it.copy(pinned = pinned) else it.copy(pinned = false) }
    }
    override fun translate(chatId: Long, messageId: Long, show: Boolean) {
        history[chatId] = messages(chatId).map { if (it.id == messageId) it.copy(translation = if (show) "[EN] " + it.text else null) else it }
    }
    override fun react(chatId: Long, messageId: Long, emoji: String) {
        history[chatId] = messages(chatId).map { m ->
            if (m.id != messageId) m else {
                val has = m.reactions.firstOrNull { it.emoji == emoji }
                val r = when {
                    has == null -> m.reactions + KxReaction(emoji, 1, true)
                    has.mine -> m.reactions.mapNotNull { if (it.emoji == emoji) (if (it.count > 1) it.copy(count = it.count - 1, mine = false) else null) else it }
                    else -> m.reactions.map { if (it.emoji == emoji) it.copy(count = it.count + 1, mine = true) else it }
                }
                m.copy(reactions = r)
            }
        }
    }

    // ---- Round 5 demo values (binder): what the boards show ----
    override val sessions get() = sessionList
    private val sessionList = mutableStateListOf(
        KxSession(1, "Xiaomi 14", "kiroX 3.0", "Алматы, Казахстан", "в сети", current = true),
        KxSession(2, "MacBook Air", "Telegram macOS 11.2", "Алматы, Казахстан", "сегодня 18:40"),
        KxSession(3, "Chrome на Windows", "Telegram Web A", "Астана, Казахстан", "вчера", official = true),
        KxSession(4, "iPad", "Telegram iOS 11.1", "Москва, Россия", "12 нояб.", passwordPending = true),
    )
    private var ttlDays by mutableStateOf(180)
    override val sessionTtlDays get() = ttlDays
    override fun terminateSession(id: Long) { sessionList.removeAll { it.id == id && !it.current } }
    override fun terminateOtherSessions() { sessionList.removeAll { !it.current } }
    override fun setSessionTtl(days: Int) { ttlDays = days }

    private val privacyRules = mutableStateListOf(
        KxPrivacyRule("PhoneNumber", "Номер телефона", 2),
        KxPrivacyRule("LastSeen", "Время захода", 1, exceptions = 3),
        KxPrivacyRule("ProfilePhoto", "Фото профиля", 0),
        KxPrivacyRule("Bio", "О себе", 0),
        KxPrivacyRule("Birthdate", "Дата рождения", 1),
        KxPrivacyRule("ForwardLink", "Ссылка при пересылке", 1),
        KxPrivacyRule("Calls", "Звонки", 1, exceptions = 1),
        KxPrivacyRule("PeerToPeer", "Peer-to-peer в звонках", 1),
        KxPrivacyRule("VoiceNotes", "Голосовые и видеосообщения", 0),
        KxPrivacyRule("ChatInvites", "Приглашения в группы", 1),
    )
    override val privacy: List<KxPrivacyRule> get() = privacyRules
    override fun setPrivacy(key: String, level: Int) { val i = privacyRules.indexOfFirst { it.key == key }; if (i >= 0) privacyRules[i] = privacyRules[i].copy(level = level) }
    // Private state + val overrides: a `var x` next to `fun setX` clashes on the JVM (setX(Z)V).
    private var readDate by mutableStateOf(true)
    override val showReadDate get() = readDate
    override fun setShowReadDate(on: Boolean) { readDate = on }
    private var archiveNew by mutableStateOf(false)
    override val archiveNewChats get() = archiveNew
    override fun setArchiveNewChats(on: Boolean) { archiveNew = on }
    override val blockedCount get() = blocked.size
    override val blocked get() = listOf(KxContact(40, "Спам-бот", "был(а) давно", colorIndex = 4), KxContact(41, "Неизвестный", "был(а) недавно", colorIndex = 2))

    private val scopes = mutableStateListOf(
        KxNotifyScope("private", "Личные чаты", true, exceptions = 2),
        KxNotifyScope("groups", "Группы", true, exceptions = 5),
        KxNotifyScope("channels", "Каналы", false, exceptions = 1),
    )
    override val notifyScopes: List<KxNotifyScope> get() = scopes
    override fun setNotifyScope(key: String, enabled: Boolean, preview: Boolean) { val i = scopes.indexOfFirst { it.key == key }; if (i >= 0) scopes[i] = scopes[i].copy(enabled = enabled, preview = preview) }

    override var storage by mutableStateOf<KxStorage?>(KxStorage(
        2_684_354_560, 157_286_400,
        listOf("Фото" to 943_718_400L, "Видео" to 1_181_116_006L, "Файлы" to 220_200_960L, "Музыка" to 125_829_120L, "Голосовые и кружки" to 52_428_800L, "Стикеры и GIF" to 104_857_600L),
        listOf("Команда kiroX" to 734_003_200L, "Анна" to 419_430_400L, "Новости" to 262_144_000L),
    ))
    override fun clearCache(types: Set<String>) {
        val st = storage ?: return
        val kept = st.byType.filter { types.isNotEmpty() && it.first !in types }
        storage = st.copy(totalBytes = kept.sumOf { it.second }, byType = kept, chats = if (types.isEmpty()) emptyList() else st.chats)
    }
    private var keepMediaIndex by mutableStateOf(1)
    override val keepMedia get() = keepMediaIndex
    override fun setKeepMedia(index: Int) { keepMediaIndex = index }

    private val tweakMap = mutableStateMapOf("ghost" to false, "ghostNoRead" to true, "ghostNoTyping" to true, "ghostOffline" to true, "ghostStories" to true,
        "keepDeleted" to true, "keepEdits" to false, "fileGuard" to true, "inAppBrowser" to true, "showIds" to false, "forwardWithoutAuthor" to false,
        "confirmSticker" to false, "confirmVoice" to true, "hideCallsTab" to false, "hideStoriesTab" to false)
    override val tweaks: Map<String, Boolean> get() = tweakMap
    override fun setTweak(key: String, on: Boolean) { tweakMap[key] = on }

    override val starsBalance: Long? get() = 250
    override val giftCatalog get() = listOf(KxGift(1, "🧸", 15), KxGift(2, "🌹", 25), KxGift(3, "🎂", 50), KxGift(4, "💎", 100), KxGift(5, "🚀", 500, soldOut = true))

    override suspend fun checkUsername(name: String): KxUsernameState = when {
        name.length < 5 || !name.matches(Regex("[A-Za-z][A-Za-z0-9_]*")) -> KxUsernameState.Invalid
        name.lowercase() in setOf("admin", "telegram", "kirox") -> KxUsernameState.Taken
        else -> KxUsernameState.Free
    }
    override suspend fun findByPhone(phone: String): KxContact? =
        if (phone.filter(Char::isDigit).endsWith("00")) null else KxContact(50, "Ольга", "был(а) недавно", colorIndex = 3)
    override suspend fun searchMessages(query: String): List<KxFound> = if (query.isBlank()) emptyList() else listOf(
        KxFound(11, 1, "Анна", "Беру билеты на 19:30, $query", "21:58"),
        KxFound(12, 2, "Команда kiroX", "$query — сборка прошла", "21:29"),
    )
    override suspend fun forumTopics(chatId: Long): List<KxTopic> = listOf(
        KxTopic(1, "Общее", general = true, preview = "Максим: всем привет", time = "21:10"),
        KxTopic(2, "Релизы", unread = 4, pinned = true, preview = "Сборка 3.0 готова", time = "20:40"),
        KxTopic(3, "Баги", unread = 1, preview = "Падает на Android 17", time = "вчера"),
        KxTopic(4, "Архив идей", closed = true, preview = "Тёмная тема 2.0", time = "пн"),
    )
    override suspend fun savedTags(): List<KxSavedTag> = listOf(KxSavedTag("❤", "Важное", 12), KxSavedTag("👍", "Работа", 7), KxSavedTag("🔥", "", 3))
    override suspend fun scheduled(chatId: Long): List<KxScheduled> = listOf(KxScheduled(1, "С днём рождения! 🎉", "завтра 09:00"), KxScheduled(2, "Не забудь про встречу", "когда будет в сети"))

    // ---- Round 5b demo values ----
    override suspend fun groupInfo(chatId: Long): KxGroupInfo? = KxGroupInfo(chatId, header(chatId).title, "Команда разработки kiroX: сборки, баги, идеи.",
        username = "kirox_team", isPublic = true, slowModeSeconds = 0, reactions = listOf("❤", "🔥", "👍"), members = 128, owner = true,
        canChangeInfo = true, canRestrict = true, canPromote = true, canInvite = true)
    override suspend fun members(chatId: Long): List<KxMember> = listOf(
        KxMember(1, "Алекс Миронов", "Создатель", "в сети"), KxMember(20, "Максим", "Администратор", "был(а) в 21:14", canEdit = true),
        KxMember(11, "Анна", "", "в сети", canEdit = true), KxMember(17, "Дмитрий", "Только чтение", "был(а) вчера", canEdit = true))
    override suspend fun inviteLinks(chatId: Long): List<KxInviteLink> = listOf(
        KxInviteLink("https://t.me/+AbCdEf123", primary = true, members = 42), KxInviteLink("https://t.me/+Xyz789", "Для друзей", members = 7, expires = "до 30 нояб.", limit = 20))
    override suspend fun joinRequests(chatId: Long): List<KxJoinRequest> = listOf(KxJoinRequest(60, "Ольга", "Дизайнер интерфейсов", "21:10"), KxJoinRequest(61, "Тимур", "", "вчера"))
    override suspend fun adminLog(chatId: Long, query: String): List<KxLogEntry> = listOf(
        KxLogEntry(1, "21:40", "Максим", "закрепил(а) сообщение"), KxLogEntry(2, "20:02", "Алекс Миронов", "изменил(а) описание группы"))
    override suspend fun boostStatus(chatId: Long): KxBoostStatus? = KxBoostStatus(2, 14, 10, 20, "https://t.me/boost/kirox_team", 9)
    override val proxies: List<KxProxy> get() = listOf(KxProxy(1, "proxy.example.org", 443, "MTProto", enabled = false))

    // Round 6: kiroX-local settings (board defaults), tab names, passcode «1234» as on the Lock board.
    private val prefMap = mutableStateMapOf<String, Int>().apply { putAll(KxPrefs.defaults) }
    override val prefs: Map<String, Int> get() = prefMap
    private val readOnlyPrefs = setOf(KxPrefs.OWN_THEMES, KxPrefs.CHAT_LOOKS, KxPrefs.FILTER_WORDS)
    override fun putPref(key: String, value: Int) { if (key !in readOnlyPrefs) prefMap[key] = value }
    private val tabNameMap = mutableStateMapOf<Int, String>()
    override val tabNames: Map<Int, String> get() = tabNameMap
    override fun renameTab(index: Int, name: String) { if (name.isBlank()) tabNameMap.remove(index) else tabNameMap[index] = name }
    // wave6 port-security: demo passcode «1234» that can be changed / turned off, demo cloud password «kirox», demo exceptions.
    private var demoCode by mutableStateOf<String?>("1234")
    private var demoPattern by mutableStateOf(false)
    private var demoAuto by mutableStateOf(60)
    private var demoBio by mutableStateOf(true)
    private var demoHide by mutableStateOf(true)
    override val passcodeSet: Boolean get() = demoCode != null
    override val passcodePattern: Boolean get() = demoPattern
    override val passcodeBiometric: Boolean get() = demoBio
    override val passcodeAutoLock: Int get() = demoAuto
    override val passcodeHideContent: Boolean get() = demoHide
    override suspend fun checkPasscode(code: String): Boolean = code == demoCode
    override suspend fun passcodeMatches(code: String): Boolean = code == demoCode
    override suspend fun setPasscode(code: String, pattern: Boolean) { demoCode = code; demoPattern = pattern }
    override fun disablePasscode() { demoCode = null; demoPattern = false }
    override fun setPasscodeAutoLock(seconds: Int) { demoAuto = seconds }
    override fun setPasscodeBiometric(on: Boolean) { demoBio = on }
    override fun setPasscodeHideContent(on: Boolean) { demoHide = on }
    private var demoCloud by mutableStateOf(KxCloudPassword(has = true, hint = "любимый фильм", hasRecoveryEmail = true))
    private var demoPw = "kirox"
    override val cloudPassword get() = demoCloud
    override suspend fun checkPassword(password: String): String? = if (password == demoPw) null else "Неверный пароль"
    override suspend fun setCloudPassword(old: String, new: String, hint: String, email: String?): String? {
        if (demoCloud.has && old != demoPw) return "Неверный текущий пароль"
        demoPw = new
        demoCloud = if (new.isEmpty()) KxCloudPassword(false) else demoCloud.copy(has = true, hint = hint,
            pendingEmailPattern = email?.let { Regex("^(.).*(@.*)$").replace(it, "$1•••$2") } ?: demoCloud.pendingEmailPattern)
        return null
    }
    override suspend fun confirmRecoveryEmail(code: String): String? =
        if (code.trim() == "12345") { demoCloud = demoCloud.copy(hasRecoveryEmail = true, pendingEmailPattern = null); null } else "Неверный код (макет: 12345)"
    override suspend fun setRecoveryEmail(password: String, email: String): String? =
        if (password != demoPw) "Неверный текущий пароль" else { demoCloud = demoCloud.copy(pendingEmailPattern = Regex("^(.).*(@.*)$").replace(email.trim(), "$1•••$2")); null }
    override suspend fun requestPasswordRecovery(): String? = if (demoCloud.hasRecoveryEmail) null else "Почта для восстановления не указана"
    override val passwordRecoveryPattern: String get() = "a•••@mail.com"
    override suspend fun recoverPassword(code: String, newPassword: String, hint: String): String? {
        if (code.trim() != "12345") return "Неверный код (макет: 12345)"
        demoPw = newPassword; demoCloud = if (newPassword.isEmpty()) KxCloudPassword(false) else demoCloud.copy(hint = hint); return null
    }
    override suspend fun resetPassword(): String? { demoCloud = demoCloud.copy(pendingResetDate = (System.currentTimeMillis() / 1000).toInt() + 7 * 86400); return null }
    override suspend fun cancelPasswordReset(): String? { demoCloud = demoCloud.copy(pendingResetDate = 0); return null }
    private val demoExceptions = mutableStateMapOf("Calls" to (listOf(contacts[0]) to emptyList<KxContact>()))
    override suspend fun privacyExceptions(key: String): Pair<List<KxContact>, List<KxContact>>? = demoExceptions[key] ?: (emptyList<KxContact>() to emptyList())
    override suspend fun setPrivacyExceptions(key: String, allow: List<Long>, never: List<Long>): String? {
        demoExceptions[key] = contacts.filter { it.id in allow } to contacts.filter { it.id in never }
        val i = privacyRules.indexOfFirst { it.key == key }
        if (i >= 0) privacyRules[i] = privacyRules[i].copy(exceptions = allow.size + never.size)
        return null
    }
    private var demoNewAll by mutableStateOf(true)
    override val newChatsFromAll: Boolean get() = demoNewAll
    override fun setNewChatsFromAll(on: Boolean) { demoNewAll = on }
    private var demoFrequent by mutableStateOf(true)
    override val suggestFrequent: Boolean get() = demoFrequent
    override fun setSuggestFrequent(on: Boolean) { demoFrequent = on }
    private var demoTtl by mutableStateOf(365)
    override val accountTtlDays: Int get() = demoTtl
    override fun setAccountTtl(days: Int) { demoTtl = days }

    // wave3-account: demo values for the account/tools boards.
    private val prefTextMap = mutableStateMapOf<String, String>().apply { putAll(KxPrefsAccount.textDefaults) }
    override val prefTexts: Map<String, String> get() = prefTextMap
    override fun putPrefText(key: String, value: String) { prefTextMap[key] = value }
    override suspend fun folderInfo(id: Int): KxFolderInfo? = folders.firstOrNull { it.id == id && id != 0 }?.let { KxFolderInfo(it.title, it.icon, 5, chats.take(3).map { c -> c.id }, unread = it.unread, groups = true) }
    override suspend fun folderShareableChats(id: Int): List<Long> = chats.filter { it.kind != KxChatKind.Private }.take(4).map { it.id }
    override suspend fun createFolderLink(id: Int, chatIds: List<Long>): String? = "https://t.me/addlist/kIr0xDem0"
    override suspend fun personalChannels(): KxPersonalChannels = KxPersonalChannels(0, chats.filter { it.kind == KxChatKind.Channel }.map { KxPersonalChannel(it.id, it.title, 1240, it.preview, colorIndex = it.colorIndex) })
    override suspend fun changeNumberSendCode(phone: String): String = "Код отправлен по SMS"
    override suspend fun changeNumberResend(): String = "Код отправлен по SMS"
    override suspend fun changeNumberCheck(code: String): String? = if (code == "24680") null else "Неверный код"
    override suspend fun loginEmailPattern(): String = "a***@gmail.com"
    override suspend fun loginEmailCheck(code: String): String? = if (code == "482915") null else "Неверный код"
    override val termsUpdate: KxTerms? get() = null
    override suspend fun antiSpam(chatId: Long): Boolean? = true
    override suspend fun importedContactCount(): Int? = 214
    override val accentColors: List<KxAccent> get() = listOf(0xFFEF7468, 0xFFF0975A, 0xFFA88BEB, 0xFF5FC08F, 0xFF3BB39A, 0xFF4A9FD8, 0xFFE57FB4).mapIndexed { i, c -> KxAccent(i, listOf(c.toInt())) }
    override val profileColors: List<KxAccent> get() = accentColors.map { it.copy(premium = true) }
    override suspend fun emojiStatuses(): List<KxEmojiStatus> = listOf("🎉", "💼", "🌴", "🎧", "🔥", "😴").mapIndexed { i, e -> KxEmojiStatus(i + 1L, e) }
    override suspend fun countries(): List<KxCountry> = listOf(KxCountry("KZ", "Казахстан", "7", "🇰🇿"), KxCountry("RU", "Россия", "7", "🇷🇺"), KxCountry("UZ", "Узбекистан", "998", "🇺🇿"))
    override suspend fun myCountry(): String = "KZ"
}

/** A store for screens shown before sign-in (they only use ctx.auth). */
object KxDemoStoreHolder { val empty: KxStore by lazy { KxDemoStore() } }
