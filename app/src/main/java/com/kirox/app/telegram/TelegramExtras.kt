package com.kirox.app.telegram

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/** Saved Messages split by where the messages were forwarded from. */
data class SavedTopic(val id: Long, val title: String, val chatId: Long, val preview: String, val order: Long)

/** Speech to text, Saved Messages by source chat and close friends. */
class TelegramExtras(private val client: TelegramClient, private val chats: TelegramChatRepository) {
    private val _topics = MutableStateFlow<List<SavedTopic>>(emptyList())
    val savedTopics: StateFlow<List<SavedTopic>> = _topics.asStateFlow()
    private val topics = HashMap<Long, SavedTopic>()

    init {
        client.addUpdateHandler { update ->
            if (update is TdApi.UpdateSavedMessagesTopic) {
                val t = update.topic
                val from = (t.type as? TdApi.SavedMessagesTopicTypeSavedFromChat)?.chatId ?: 0L
                val title = when (t.type) {
                    is TdApi.SavedMessagesTopicTypeMyNotes -> "Мои заметки"
                    is TdApi.SavedMessagesTopicTypeAuthorHidden -> "Автор скрыт"
                    else -> chats.chat(from)?.title.orEmpty().ifBlank { "Чат" }
                }
                if (t.order == 0L) topics.remove(t.id) else topics[t.id] = SavedTopic(t.id, title, from, messagePreview(t.lastMessage), t.order)
                _topics.value = topics.values.sortedByDescending { it.order }
            }
        }
    }

    // ---------- Voice and video notes to text ----------

    private suspend fun recognition(chatId: Long, messageId: Long): TdApi.SpeechRecognitionResult? =
        when (val content = client.query(TdApi.GetMessage(chatId, messageId)).content) {
            is TdApi.MessageVoiceNote -> content.voiceNote.speechRecognitionResult
            is TdApi.MessageVideoNote -> content.videoNote.speechRecognitionResult
            else -> null
        }

    /**
     * The text of a voice note or video note; Telegram recognizes it on its side (Premium, or a few
     * free tries a week). [onPartial] gets the text as it grows. Throws with Telegram's reason.
     */
    suspend fun transcribe(chatId: Long, messageId: Long, onPartial: (String) -> Unit = {}): String {
        when (val done = recognition(chatId, messageId)) {
            is TdApi.SpeechRecognitionResultText -> return done.text
            is TdApi.SpeechRecognitionResultPending -> Unit
            else -> client.query(TdApi.RecognizeSpeech(chatId, messageId))
        }
        repeat(120) {
            when (val r = recognition(chatId, messageId)) {
                is TdApi.SpeechRecognitionResultText -> return r.text
                is TdApi.SpeechRecognitionResultError -> throw TdException(r.error ?: TdApi.Error(400, "Не удалось расшифровать"))
                is TdApi.SpeechRecognitionResultPending -> if (r.partialText.isNotBlank()) onPartial(r.partialText)
                else -> Unit
            }
            delay(500)
        }
        throw TdException(TdApi.Error(408, "Не удалось расшифровать"))
    }

    // ---------- Saved Messages ----------

    /** Asks Telegram for the sources of Saved Messages; they arrive in [savedTopics]. */
    fun loadSavedTopics() { client.send(TdApi.LoadSavedMessagesTopics(100)) }


    // ---------- Stories ----------

    /** People who see your «Близкие друзья» stories. */
    suspend fun closeFriends(): Set<Long> = runCatching { client.query(TdApi.GetCloseFriends()).userIds.toSet() }.getOrDefault(emptySet())

    suspend fun setCloseFriends(ids: Collection<Long>) { client.query(TdApi.SetCloseFriends(ids.toLongArray())) }
}
