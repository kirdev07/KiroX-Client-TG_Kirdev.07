package com.kirox.app.ui.v3.data

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kirox.app.ui.v3.model.KxChat

/**
 * wave3-account: launcher shortcuts of the Shortcuts board. Each one opens its chat through the same
 * intent extra the notifications use (MainActivity reads MessageNotifications.EXTRA_OPEN_CHAT).
 */
object KxShortcuts {
    private const val PREFIX = "kx-chat-"

    private fun intent(context: Context, chatId: Long) = Intent(context, com.kirox.app.MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(com.kirox.app.notify.MessageNotifications.EXTRA_OPEN_CHAT, chatId)

    private fun info(context: Context, chat: KxChat, rank: Int = 0) = ShortcutInfoCompat.Builder(context, PREFIX + chat.id)
        .setShortLabel(chat.title.ifBlank { "Чат" }.take(24))
        .setLongLabel(chat.title.ifBlank { "Чат" })
        .setIcon(IconCompat.createWithResource(context, com.kirox.app.R.mipmap.ic_launcher))
        .setIntent(intent(context, chat.id))
        .setRank(rank)
        .build()

    /** The long-press menu of the app icon shows these chats (up to 4); empty removes them. */
    fun setFrequent(context: Context, chats: List<KxChat>) = runCatching {
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtMost(4)
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        if (chats.isNotEmpty()) ShortcutManagerCompat.setDynamicShortcuts(context, chats.take(max).mapIndexed { i, c -> info(context, c, i) })
    }.isSuccess

    /** Asks the launcher to put [chat] on the home screen; false when the launcher can't. */
    fun pin(context: Context, chat: KxChat): Boolean = runCatching {
        ShortcutManagerCompat.isRequestPinShortcutSupported(context) && ShortcutManagerCompat.requestPinShortcut(context, info(context, chat), null)
    }.getOrDefault(false)

    fun hasFrequent(context: Context): Boolean = runCatching { ShortcutManagerCompat.getDynamicShortcuts(context).any { it.id.startsWith(PREFIX) } }.getOrDefault(false)
}

/**
 * wave3-account: Help / BugReport / RateApp on the real app. kiroX has no report server: the text (and, when
 * asked, the local crash journal of CrashLog) goes into Android's share sheet — the user picks where to send it.
 */
object KxBugReport {
    fun share(context: Context, subject: String, text: String, withLogs: Boolean) {
        val body = buildString {
            append(text.trim().ifEmpty { "(без описания)" })
            append("\n\n— kiroX ").append(com.kirox.app.BuildConfig.VERSION_NAME).append(" · Android ").append(android.os.Build.VERSION.RELEASE)
                .append(" · ").append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
            if (withLogs) {
                val log = runCatching { com.kirox.app.CrashLog.read(context) }.getOrDefault("")
                append("\n\nЖурнал сбоев:\n").append(log.ifBlank { "пусто" }.take(60_000))
            }
        }
        runCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body), subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
