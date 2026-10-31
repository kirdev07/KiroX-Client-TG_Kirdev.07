package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** Supergroup and channel settings an admin changes on the «Управление» page. */
data class GroupSettings(
    val supergroupId: Long,
    val isChannel: Boolean,
    val isPublic: Boolean,
    val slowModeSeconds: Int,
    val joinByRequest: Boolean,
    val stickerSetId: Long,
    val canSetStickerSet: Boolean,
    /** null: every reaction allowed; empty: none; otherwise only these emoji. */
    val reactions: List<String>?
)

/** One line of «Недавние действия». */
data class ChatLogEntry(val id: Long, val date: Int, val who: String, val text: String)

/** Boost level of a channel or group. */
data class BoostInfo(val level: Int, val boosts: Int, val levelStart: Int, val nextLevel: Int, val url: String, val premiumMembers: Int)

/** Numbers of one post. */
data class PostStats(val views: Int, val forwards: Int, val reactions: Int, val comments: Int)

/** The admin tools for supergroups and channels beyond members and permissions. */
class TelegramChatAdminExtras(private val client: TelegramClient) {

    suspend fun settings(chatId: Long): GroupSettings? = runCatching {
        val chat = client.query(TdApi.GetChat(chatId))
        val type = chat.type as? TdApi.ChatTypeSupergroup ?: return null
        val group = client.query(TdApi.GetSupergroup(type.supergroupId))
        val full = client.query(TdApi.GetSupergroupFullInfo(type.supergroupId))
        val reactions = when (val r = chat.availableReactions) {
            is TdApi.ChatAvailableReactionsSome -> r.reactions.mapNotNull { (it as? TdApi.ReactionTypeEmoji)?.emoji }
            else -> null
        }
        GroupSettings(
            type.supergroupId, type.isChannel, group.usernames?.activeUsernames?.isNotEmpty() == true,
            full.slowModeDelay, group.joinByRequest, full.stickerSetId, full.canSetStickerSet, reactions
        )
    }.getOrNull()

    /** Seconds between two messages of one member; 0 turns slow mode off. */
    suspend fun setSlowMode(chatId: Long, seconds: Int) { client.query(TdApi.SetChatSlowModeDelay(chatId, seconds)) }

    /** New members (by the public link or the join button) wait for an admin's approval. */
    suspend fun setJoinByRequest(supergroupId: Long, on: Boolean) { client.query(TdApi.ToggleSupergroupJoinByRequest(supergroupId, on, 0, false)) }

    /** [emoji] null allows all reactions; an empty list turns reactions off. */
    suspend fun setReactions(chatId: Long, emoji: List<String>?) {
        val value: TdApi.ChatAvailableReactions =
            if (emoji == null) TdApi.ChatAvailableReactionsAll(11)
            else TdApi.ChatAvailableReactionsSome(emoji.map { TdApi.ReactionTypeEmoji(it) }.toTypedArray(), 11)
        client.query(TdApi.SetChatAvailableReactions(chatId, value))
    }

    /** The group's own sticker set (members see it in the panel); 0 removes it. */
    suspend fun setStickerSet(supergroupId: Long, setId: Long) { client.query(TdApi.SetSupergroupStickerSet(supergroupId, setId)) }

    private suspend fun nameOf(sender: TdApi.MessageSender?): String = runCatching {
        when (sender) {
            is TdApi.MessageSenderUser -> client.query(TdApi.GetUser(sender.userId)).let { listOf(it.firstName, it.lastName).filter(String::isNotBlank).joinToString(" ") }
            is TdApi.MessageSenderChat -> client.query(TdApi.GetChat(sender.chatId)).title
            else -> ""
        }
    }.getOrDefault("").ifBlank { "Кто-то" }

    private suspend fun userName(id: Long) = nameOf(TdApi.MessageSenderUser(id))

    private fun short(m: TdApi.Message?) = messagePreview(m).take(80)

    private suspend fun describe(action: TdApi.ChatEventAction): String = when (action) {
        is TdApi.ChatEventMessageEdited -> "изменил сообщение: «${short(action.newMessage)}»"
        is TdApi.ChatEventMessageDeleted -> "удалил сообщение: «${short(action.message)}»"
        is TdApi.ChatEventMessagePinned -> "закрепил: «${short(action.message)}»"
        is TdApi.ChatEventMessageUnpinned -> "открепил: «${short(action.message)}»"
        is TdApi.ChatEventPollStopped -> "остановил опрос"
        is TdApi.ChatEventMemberJoined -> "вступил"
        is TdApi.ChatEventMemberJoinedByInviteLink -> "вступил по ссылке"
        is TdApi.ChatEventMemberJoinedByRequest -> "вступил по заявке"
        is TdApi.ChatEventMemberInvited -> "пригласил ${userName(action.userId)}"
        is TdApi.ChatEventMemberLeft -> "вышел"
        is TdApi.ChatEventMemberPromoted ->
            if (action.newStatus is TdApi.ChatMemberStatusAdministrator || action.newStatus is TdApi.ChatMemberStatusCreator) "назначил администратором ${userName(action.userId)}"
            else "снял права администратора у ${userName(action.userId)}"
        is TdApi.ChatEventMemberRestricted -> when (action.newStatus) {
            is TdApi.ChatMemberStatusBanned -> "заблокировал ${nameOf(action.memberId)}"
            is TdApi.ChatMemberStatusRestricted -> "ограничил ${nameOf(action.memberId)}"
            else -> "снял ограничения с ${nameOf(action.memberId)}"
        }
        is TdApi.ChatEventTitleChanged -> "сменил название на «${action.newTitle}»"
        is TdApi.ChatEventDescriptionChanged -> "изменил описание"
        is TdApi.ChatEventPhotoChanged -> if (action.newPhoto == null) "удалил фото" else "сменил фото"
        is TdApi.ChatEventUsernameChanged -> if (action.newUsername.isBlank()) "сделал чат частным" else "сменил ссылку на @${action.newUsername}"
        is TdApi.ChatEventPermissionsChanged -> "изменил разрешения участников"
        is TdApi.ChatEventSlowModeDelayChanged ->
            if (action.newSlowModeDelay == 0) "выключил медленный режим" else "включил медленный режим: ${slowLabel(action.newSlowModeDelay)}"
        is TdApi.ChatEventAvailableReactionsChanged -> "изменил доступные реакции"
        is TdApi.ChatEventStickerSetChanged -> "сменил стикеры группы"
        is TdApi.ChatEventHasProtectedContentToggled -> if (action.hasProtectedContent) "запретил копирование" else "разрешил копирование"
        is TdApi.ChatEventInvitesToggled -> if (action.canInviteUsers) "разрешил приглашать участникам" else "запретил приглашать участникам"
        is TdApi.ChatEventSignMessagesToggled -> if (action.signMessages) "включил подписи" else "выключил подписи"
        is TdApi.ChatEventInviteLinkEdited -> "изменил ссылку-приглашение"
        is TdApi.ChatEventInviteLinkRevoked -> "отозвал ссылку-приглашение"
        is TdApi.ChatEventInviteLinkDeleted -> "удалил ссылку-приглашение"
        is TdApi.ChatEventVideoChatCreated -> "начал видеочат"
        is TdApi.ChatEventVideoChatEnded -> "завершил видеочат"
        is TdApi.ChatEventForumTopicCreated -> "создал тему «${action.topicInfo.name}»"
        is TdApi.ChatEventForumTopicDeleted -> "удалил тему «${action.topicInfo.name}»"
        is TdApi.ChatEventMessageAutoDeleteTimeChanged -> "изменил автоудаление"
        is TdApi.ChatEventLinkedChatChanged -> "сменил связанный чат"
        else -> "изменил настройки"
    }

    /** «Недавние действия»: what admins and members did in the last 48 hours, newest first. */
    suspend fun eventLog(chatId: Long, query: String = ""): List<ChatLogEntry> = runCatching {
        client.query(TdApi.GetChatEventLog(chatId, query, 0, 100, null, LongArray(0))).events.map { e ->
            ChatLogEntry(e.id, e.date, nameOf(e.memberId), describe(e.action))
        }
    }.getOrDefault(emptyList())

    suspend fun boosts(chatId: Long): BoostInfo? = runCatching {
        client.query(TdApi.GetChatBoostStatus(chatId)).let { BoostInfo(it.level, it.boostCount, it.currentLevelBoostCount, it.nextLevelBoostCount, it.boostUrl, it.premiumMemberCount) }
    }.getOrNull()

    suspend fun postStats(chatId: Long, messageId: Long): PostStats? = runCatching {
        val info = client.query(TdApi.GetMessage(chatId, messageId)).interactionInfo
        PostStats(info?.viewCount ?: 0, info?.forwardCount ?: 0, info?.reactions?.reactions?.sumOf { it.totalCount } ?: 0, info?.replyInfo?.replyCount ?: 0)
    }.getOrNull()

    companion object {
        /** Reactions offered in the picker, in Telegram's order. */
        val REACTIONS = listOf(
            "👍", "👎", "❤", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩", "🙏", "👌", "🕊", "🤡",
            "🥱", "🥴", "😍", "🐳", "🌚", "🌭", "💯", "🤣", "⚡", "🍌", "🏆", "💔", "🤨", "😐", "🍓", "🍾", "💋", "😈", "😴", "😭",
            "🤓", "👻", "👀", "🎃", "🙈", "😇", "😨", "🤝", "✍", "🤗", "🫡", "🎅", "🎄", "☃", "💅", "🤪", "🗿", "🆒", "💘", "🙉",
            "🦄", "😘", "💊", "🙊", "😎", "👾", "🤷", "😡"
        )

        /** «30 с», «1 мин», «1 ч». */
        fun slowLabel(seconds: Int) = when {
            seconds >= 3600 -> "${seconds / 3600} ч"
            seconds >= 60 -> "${seconds / 60} мин"
            else -> "$seconds с"
        }
    }
}
