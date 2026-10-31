package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

/** A music file sent in some chat: where it is and what it is. */
data class TelegramAudio(
    val chatId: Long,
    val messageId: Long,
    val chatTitle: String,
    val date: Int,
    val file: TelegramFileRef,
    val title: String,
    val performer: String,
    val duration: Int,
    val size: Long,
    val cover: TelegramFileRef? = null
)

/**
 * Music from chats for the music library: everything across chats (Telegram's search with the audio
 * filter), one chat's music, and messages saved in playlists looked up again by chat and message id.
 */
class TelegramMusic(private val client: TelegramClient) {
    private val titles = ConcurrentHashMap<Long, String>()

    private suspend fun chatTitle(chatId: Long): String = titles[chatId]
        ?: runCatching { client.query(TdApi.GetChat(chatId)).title }.getOrDefault("").also { titles[chatId] = it }

    private suspend fun TdApi.Message.toAudio(): TelegramAudio? {
        val audio = (content as? TdApi.MessageAudio)?.audio ?: return null
        val file = audio.audio
        val name = audio.title.ifBlank { audio.fileName.substringBeforeLast('.') }.ifBlank { "Аудио" }
        return TelegramAudio(chatId, id, chatTitle(chatId), date, file.toRef(), name, audio.performer, audio.duration, if (file.size > 0) file.size else file.expectedSize, audio.albumCoverThumbnail?.file?.toRef())
    }

    /** One page of music from all chats; [offset] "" for the first page. The next offset is "" at the end. */
    suspend fun all(query: String, offset: String, limit: Int = 50): Pair<List<TelegramAudio>, String> {
        val found = client.query(TdApi.SearchMessages(null, query, offset, limit, TdApi.SearchMessagesFilterAudio(), null, 0, 0))
        return found.messages.mapNotNull { it.toAudio() } to found.nextOffset.orEmpty()
    }

    /**
     * When the server search gives nothing (it may want a query), the newest chats are looked
     * through one by one instead.
     */
    suspend fun fromRecentChats(chats: Int = 60, perChat: Int = 30): List<TelegramAudio> {
        val ids = runCatching { client.query(TdApi.GetChats(TdApi.ChatListMain(), chats)).chatIds.toList() }.getOrDefault(emptyList())
        return ids.flatMap { id -> runCatching { chat(id, 0, perChat).first }.getOrDefault(emptyList()) }.sortedByDescending { it.date }
    }

    /** One page of a chat's music, newest first; the next page starts from the returned message id (0 at the end). */
    suspend fun chat(chatId: Long, fromMessageId: Long, limit: Int = 50): Pair<List<TelegramAudio>, Long> {
        val found = client.query(TdApi.SearchChatMessages(chatId, null, "", null, fromMessageId, 0, limit, TdApi.SearchMessagesFilterAudio()))
        return found.messages.mapNotNull { it.toAudio() } to found.nextFromMessageId
    }

    /** The music message again (for a playlist); null when it was deleted or is not music any more. */
    suspend fun get(chatId: Long, messageId: Long): TelegramAudio? =
        runCatching { client.query(TdApi.GetMessage(chatId, messageId)).toAudio() }.getOrNull()
}
