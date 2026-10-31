package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** Unread mentions («@») and reactions to your messages in one chat, to jump to them like in Telegram. */
class TelegramMentions(private val client: TelegramClient) {
    /** Unread mentions and unread reactions in [chatId]. */
    suspend fun counts(chatId: Long): Pair<Int, Int> = runCatching {
        client.query(TdApi.GetChat(chatId)).let { it.unreadMentionCount to it.unreadReactionCount }
    }.getOrDefault(0 to 0)

    /** The oldest unread mention (or reaction); Telegram marks it read once it is on screen. */
    suspend fun firstUnread(chatId: Long, reactions: Boolean): Long? = runCatching {
        val filter = if (reactions) TdApi.SearchMessagesFilterUnreadReaction() else TdApi.SearchMessagesFilterUnreadMention()
        client.query(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 100, filter)).messages.lastOrNull()?.id
    }.getOrNull()

    /** Marks every mention (or reaction) in the chat as read. */
    suspend fun readAll(chatId: Long, reactions: Boolean) {
        runCatching { client.query(if (reactions) TdApi.ReadAllChatReactions(chatId) else TdApi.ReadAllChatMentions(chatId)) }
    }
}
