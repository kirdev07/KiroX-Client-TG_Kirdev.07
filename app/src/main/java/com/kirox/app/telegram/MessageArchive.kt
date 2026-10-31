package com.kirox.app.telegram

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kirox.app.settings.KiroxTweaks
import org.drinkless.tdlib.TdApi
import java.util.concurrent.Executors

/** A message kept after it was deleted: who, when, what, and when it disappeared. */
data class KeptMessage(val chatId: Long, val id: Long, val sender: String, val date: Int, val text: String, val deletedAt: Int)

/** One earlier text of an edited message; [editedAt] is when it was replaced. */
data class MessageVersion(val text: String, val editedAt: Int)

/**
 * «Сохранять удалённые и изменённые сообщения» (kiroX settings, off by default): while it is on,
 * the text of new messages is copied into a database on this phone, so a message deleted by its
 * author can still be read and an edited one shows what it said before. Media keep only their label
 * («Фото», «Видео») and caption. Secret chats and self-destructing messages are never copied.
 * Copies older than [KEEP_DAYS] are removed; nothing leaves the phone.
 */
class MessageArchive(context: Context, private val client: TelegramClient, private val chats: TelegramChatRepository, private val users: TelegramUserRepository) {
    private val db = Helper(context.applicationContext).writableDatabase
    /** Writes stay off TDLib's thread. */
    private val io = Executors.newSingleThreadExecutor()

    init {
        client.addUpdateHandler(::handle)
        io.execute { prune() }
    }

    private val account get() = client.activeSession

    private fun handle(update: TdApi.Object) {
        val tweaks = KiroxTweaks.current
        if (!tweaks.keepDeleted && !tweaks.keepEdits) return
        when (update) {
            is TdApi.UpdateNewMessage -> save(update.message)
            is TdApi.UpdateMessageSendSucceeded -> save(update.message)
            is TdApi.UpdateMessageContent -> if (tweaks.keepEdits) {
                val text = textOf(update.newContent)
                val account = account
                io.execute { recordEdit(account, update.chatId, update.messageId, text) }
            }
            is TdApi.UpdateDeleteMessages -> if (tweaks.keepDeleted && update.isPermanent && !update.fromCache) {
                val account = account
                val now = (System.currentTimeMillis() / 1000).toInt()
                io.execute {
                    update.messageIds.forEach { id ->
                        db.update(TABLE, ContentValues().apply { put("deleted_at", now) }, "account = ? AND chat_id = ? AND id = ? AND deleted_at = 0", arrayOf(account, update.chatId.toString(), id.toString()))
                    }
                }
            }
        }
    }

    private fun save(message: TdApi.Message) {
        if (message.schedulingState != null || message.selfDestructType != null || message.sendingState != null) return
        if (chats.chat(message.chatId)?.type is TdApi.ChatTypeSecret) return
        val text = textOf(message.content)
        if (text.isBlank()) return
        val sender = when (val s = message.senderId) {
            is TdApi.MessageSenderUser -> users.user(s.userId)?.displayName().orEmpty()
            is TdApi.MessageSenderChat -> chats.chat(s.chatId)?.title.orEmpty()
            else -> ""
        }
        val values = ContentValues().apply {
            put("account", account); put("chat_id", message.chatId); put("id", message.id)
            put("sender", sender); put("date", message.date); put("text", text); put("deleted_at", 0)
        }
        io.execute { db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE) }
    }

    /** The text before an edit becomes a version; the kept copy takes the new text. */
    private fun recordEdit(account: String, chatId: Long, messageId: Long, newText: String) {
        val args = arrayOf(account, chatId.toString(), messageId.toString())
        val old = db.rawQuery("SELECT text FROM $TABLE WHERE account = ? AND chat_id = ? AND id = ?", args).use { if (it.moveToFirst()) it.getString(0) else null } ?: return
        if (old == newText) return
        db.insert(EDITS, null, ContentValues().apply {
            put("account", account); put("chat_id", chatId); put("message_id", messageId)
            put("text", old); put("edited_at", (System.currentTimeMillis() / 1000).toInt())
        })
        db.update(TABLE, ContentValues().apply { put("text", newText) }, "account = ? AND chat_id = ? AND id = ?", args)
    }

    /** Deleted messages of [chatId], newest first. */
    fun deleted(chatId: Long): List<KeptMessage> = db.rawQuery(
        "SELECT chat_id, id, sender, date, text, deleted_at FROM $TABLE WHERE account = ? AND chat_id = ? AND deleted_at > 0 ORDER BY date DESC LIMIT 500",
        arrayOf(account, chatId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(KeptMessage(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getString(4), c.getInt(5))) } }

    /** Earlier texts of a message, oldest first; empty when it was never edited while kept. */
    fun versions(chatId: Long, messageId: Long): List<MessageVersion> = db.rawQuery(
        "SELECT text, edited_at FROM $EDITS WHERE account = ? AND chat_id = ? AND message_id = ? ORDER BY edited_at",
        arrayOf(account, chatId.toString(), messageId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(MessageVersion(c.getString(0), c.getInt(1))) } }

    /** How many messages are kept now (all accounts). */
    fun count(): Int = db.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** «Очистить сохранённое». */
    fun clear() { db.delete(TABLE, null, null); db.delete(EDITS, null, null) }

    private fun prune() {
        val cutoff = (System.currentTimeMillis() / 1000 - KEEP_DAYS * 86_400L).toInt()
        db.delete(TABLE, "date < ?", arrayOf(cutoff.toString()))
        db.delete(EDITS, "edited_at < ?", arrayOf(cutoff.toString()))
    }

    private fun textOf(content: TdApi.MessageContent): String = when (content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> listOf("Фото", content.caption.text).filter { it.isNotBlank() }.joinToString(": ")
        is TdApi.MessageVideo -> listOf("Видео", content.caption.text).filter { it.isNotBlank() }.joinToString(": ")
        is TdApi.MessageDocument -> listOf("Файл ${content.document.fileName}", content.caption.text).filter { it.isNotBlank() }.joinToString(": ")
        is TdApi.MessageAnimation -> listOf("GIF", content.caption.text).filter { it.isNotBlank() }.joinToString(": ")
        is TdApi.MessageAudio -> "Аудио"
        is TdApi.MessageVoiceNote -> "Голосовое сообщение"
        is TdApi.MessageVideoNote -> "Видеосообщение"
        is TdApi.MessageSticker -> "${content.sticker.emoji} Стикер".trim()
        is TdApi.MessageLocation -> "Местоположение"
        is TdApi.MessageContact -> "Контакт"
        is TdApi.MessagePoll -> "Опрос: ${content.poll.question.text}"
        else -> ""
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "kept_messages.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE (account TEXT NOT NULL, chat_id INTEGER NOT NULL, id INTEGER NOT NULL, sender TEXT NOT NULL, date INTEGER NOT NULL, text TEXT NOT NULL, deleted_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (account, chat_id, id))")
            db.execSQL("CREATE TABLE $EDITS (account TEXT NOT NULL, chat_id INTEGER NOT NULL, message_id INTEGER NOT NULL, text TEXT NOT NULL, edited_at INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX edits_message ON $EDITS (account, chat_id, message_id)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    private companion object {
        const val TABLE = "kept"
        const val EDITS = "edits"
        const val KEEP_DAYS = 30
    }
}
