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
        KxContact(11, "Анна", "в сети", online = true, hasStory = true, colorIndex = 0),
        KxContact(17, "Дмитрий", "в сети", online = true, colorIndex = 1),
        KxContact(15, "Илья", "был(а) 5 минут назад", colorIndex = 5),
        KxContact(21, "Лиза", "был(а) вчера в 21:14", colorIndex = 4),
        KxContact(13, "Мама", "был(а) недавно", colorIndex = 3),
        KxContact(20, "Максим", "был(а) в 12:40", colorIndex = 1),
        KxContact(16, "Света", "был(а) недавно", colorIndex = 6),
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
    }

    private val pinnedMsg = mutableStateMapOf<Long, KxReply?>(11L to KxReply("Анна", "Встречаемся у кинотеатра в 19:15", 0))

    override fun header(chatId: Long): KxChatHeader {
        val c = (list + archive).firstOrNull { it.id == chatId } ?: list[1]
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
}

/** A store for screens shown before sign-in (they only use ctx.auth). */
object KxDemoStoreHolder { val empty: KxStore by lazy { KxDemoStore() } }
