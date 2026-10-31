package com.kirox.app.ui.chat

import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.TelegramChatHistory
import com.kirox.app.telegram.TelegramChatItem
import com.kirox.app.telegram.TelegramDelivery
import com.kirox.app.telegram.TelegramMessage
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramPeerKind
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Message rows, mapping to the UI model and small helpers of the chat screen.

/** One list row: a message, a date separator, or an album ([album] oldest first, [message] carries its caption). */
internal data class ChatRow(val key: String, val message: MessageUi, val album: List<MessageUi> = emptyList())

/** [newestFirst] messages → rows with a date separator after the last message of each day. */
internal fun toRows(newestFirst: List<TelegramMessage>): List<ChatRow> {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))
    val rows = ArrayList<ChatRow>(newestFirst.size + 8)
    newestFirst.forEachIndexed { index, message ->
        val ui = message.toUi(timeFormat.format(Date(message.date * 1000L)))
        val last = rows.lastOrNull()
        val lastAlbum = last?.key?.takeIf { it.startsWith("a") }?.removePrefix("a")?.toLongOrNull()
        val newer = newestFirst.getOrNull(index - 1)
        // The newer part of the same album is the row just added (not a date separator).
        val newerSameAlbum = newer != null && newer.albumId == message.albumId && last?.key == "m${newer.id}"
        if (message.albumId != 0L && last != null && (lastAlbum == message.albumId || newerSameAlbum)) {
            // Album: newestFirst runs newest → oldest, so each older part goes to the front.
            val album = listOf(ui) + last.album.ifEmpty { listOf(last.message) }
            rows[rows.lastIndex] = ChatRow("a${message.albumId}", album.firstOrNull { it.text.isNotBlank() } ?: album.last(), album)
        } else rows += ChatRow("m${message.id}", ui)
        val older = newestFirst.getOrNull(index + 1)
        val day = dayFormat.format(Date(message.date * 1000L))
        if (older == null || dayFormat.format(Date(older.date * 1000L)) != day) {
            // Keyed by message: the same day can appear twice when dates are not in id order.
            rows += ChatRow("d${message.id}", MessageUi(-message.id, "", MessageDirection.System, text = day, time = ""))
        }
    }
    return rows
}

internal fun TelegramMessage.toUi(time: String) = MessageUi(
    id = id,
    author = senderName,
    direction = if (isOutgoing) MessageDirection.Outgoing else MessageDirection.Incoming,
    kind = when (kind) {
        TelegramMessageKind.Photo -> MessageKind.Photo
        TelegramMessageKind.Video -> MessageKind.Video
        TelegramMessageKind.Animation -> MessageKind.Animation
        TelegramMessageKind.VideoNote -> MessageKind.VideoNote
        TelegramMessageKind.Voice -> MessageKind.Voice
        TelegramMessageKind.Audio -> MessageKind.Audio
        TelegramMessageKind.Document -> MessageKind.Document
        TelegramMessageKind.Sticker -> MessageKind.Sticker
        TelegramMessageKind.Text, TelegramMessageKind.Other -> MessageKind.Text
    },
    text = text,
    time = time,
    delivery = when (delivery) {
        TelegramDelivery.Sending -> DeliveryState.Sending
        TelegramDelivery.Failed -> DeliveryState.Failed
        TelegramDelivery.Read -> DeliveryState.Read
        TelegramDelivery.Sent -> DeliveryState.Sent
        TelegramDelivery.None -> DeliveryState.None
    },
    quote = replyText?.let { QuoteUi(replyAuthor.orEmpty(), it, replyToMessageId) },
    forwardedFrom = forwardedFrom,
    reactions = reactions.map { (emoji, count, chosen) -> ReactionUi(emoji, count, chosen) },
    canEdit = canEdit,
    photo = photo,
    photoAspect = photoAspect,
    edited = edited,
    deleted = deleted,
    media = media,
    thumbnail = thumbnail,
    entities = entities,
    commentCount = commentCount,
    viewCount = viewCount,
    poll = poll,
    buttons = buttons
)

/** Copies a picked content URI into the app cache so TDLib can upload it from a plain path. */
internal fun copyToCache(context: Context, uri: Uri): String? = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }?.replace(Regex("[\\\\/:*?\"<>|]"), "_")?.takeIf { it.isNotBlank() } ?: "file"
    val dir = File(context.cacheDir, "outgoing/${System.currentTimeMillis()}").apply { mkdirs() }
    val target = File(dir, name)
    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: return null
    target.absolutePath
}.getOrNull()

/** Duration (s), width and height of a local video; zeros when unreadable. */
internal fun videoMeta(path: String): Triple<Int, Int, Int> = runCatching {
    android.media.MediaMetadataRetriever().use { r ->
        r.setDataSource(path)
        val ms = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        var w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        var h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) { val t = w; w = h; h = t }
        Triple((ms / 1000).toInt(), w, h)
    }
}.getOrDefault(Triple(0, 0, 0))

/** Telegram allows up to 10 photos and videos in one album. */
internal const val ALBUM_LIMIT = 10

/** Unsent text of the chat on screen, as of the last composition; not state, so writes cause no recomposition. */
internal class DraftHolder {
    var chatId = 0L
    /** Comment thread or forum topic the text was typed in; the draft is saved there, not in the chat. */
    var threadId = 0L
    var forumTopicId = 0
    var text: String? = null

    fun save(actions: TelegramChatActions) {
        val value = text ?: return
        if (chatId != 0L) actions.saveDraft(chatId, value, threadId, forumTopicId)
    }
}

internal fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

/** Album rows select or unselect all their messages together. */
internal fun Set<Long>.toggleAll(ids: List<Long>): Set<Long> = if (ids.first() in this) this - ids.toSet() else this + ids

/** "сообщение", "сообщения", "сообщений" for Russian plural rules. */
internal fun messageWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "сообщений"
        mod10 == 1 -> "сообщение"
        mod10 in 2..4 -> "сообщения"
        else -> "сообщений"
    }
}
