package com.kirox.app.notify

import androidx.core.content.edit
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kirox.app.telegram.tdlibParameters
import com.kirox.app.MainActivity
import com.kirox.app.telegram.TelegramAccount
import com.kirox.app.telegram.TelegramSessionManager
import com.kirox.app.telegram.messagePreview
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/**
 * Notifications of signed-in accounts that are not open right now. Telegram keeps pushing for every
 * account that registered this device; on a push KiroX briefly opens each inactive account's own
 * TDLib database (one client per account, never two for the same one), lets it sync, shows its new
 * messages labeled with the account, and closes it again. The open account is handled as before.
 */
class AccountPeek(context: Context) {
    private val appContext = context.applicationContext

    /** Peeks [accounts] (all but the open one) for [windowMs]; each closes itself afterward. */
    fun peek(accounts: List<TelegramAccount>, windowMs: Long) {
        // With KiroX on screen the user may switch accounts at any moment; peeks are for the background.
        if (AppVisibility.visible) return
        accounts.forEach { start(it, windowMs) }
    }

    private fun start(account: TelegramAccount, windowMs: Long) {
        val session = TelegramSessionManager(appContext, account.session)
        // Only encrypted, signed-in sessions are peeked; anything else waits for the user.
        val key = runCatching { session.keyStore.load() }.getOrNull() ?: return
        if (!session.hasDatabase) return
        val titles = HashMap<Long, String>()
        // TDLib may report its first state before Client.create returns, on its own thread: what has
        // to be sent then is remembered and sent as soon as the client exists.
        val client = java.util.concurrent.atomic.AtomicReference<Client?>(null)
        val wantsParameters = java.util.concurrent.atomic.AtomicBoolean(false)
        val closeRequested = java.util.concurrent.atomic.AtomicBoolean(false)
        val lock = java.util.concurrent.atomic.AtomicReference<Any?>(null)
        val handler = Client.ResultHandler { update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        wantsParameters.set(true)
                        client.get()?.let { if (wantsParameters.compareAndSet(true, false)) it.send(tdlibParameters(session, key), null) }
                    }
                    is TdApi.AuthorizationStateReady -> client.get()?.let { c ->
                        c.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(false)), null)
                        c.send(TdApi.SetOption("notification_group_count_max", TdApi.OptionValueInteger(GROUPS.toLong())), null)
                        c.send(TdApi.SetOption("notification_group_size_max", TdApi.OptionValueInteger(GROUP_SIZE.toLong())), null)
                    }
                    // Only our own hold: after the forced timeout the account may belong to the main client.
                    is TdApi.AuthorizationStateClosed -> lock.get()?.let { com.kirox.app.telegram.SessionLocks.release(account.session, it) }
                    is TdApi.AuthorizationStateWaitPhoneNumber, is TdApi.AuthorizationStateLoggingOut -> client.get()?.send(TdApi.Close(), null)
                    else -> Unit
                }
                is TdApi.UpdateNewChat -> titles[update.chat.id] = update.chat.title
                is TdApi.UpdateChatTitle -> titles[update.chatId] = update.title
                is TdApi.UpdateNotificationGroup -> {
                    update.addedNotifications.forEach { notification ->
                        val type = notification.type as? TdApi.NotificationTypeNewMessage ?: return@forEach
                        if (notification.isSilent) return@forEach
                        // wave5: a chat hidden in that account («Скрытые чаты») shows no name and no text.
                        val secret = runCatching { com.kirox.app.ui.v3.data.KxHidden.contains(appContext, account.session, update.chatId) }.getOrDefault(false)
                        show(account, update.notificationGroupId, if (secret) "kiroX" else titles[update.chatId].orEmpty(), if (type.showPreview && !secret) messagePreview(type.message) else com.kirox.app.i18n.I18n.t("Новое сообщение"))
                    }
                    // Read on another device: the group is empty, its notification goes away.
                    if (update.totalCount == 0) runCatching { NotificationManagerCompat.from(appContext).cancel(tagOf(account), update.notificationGroupId) }
                }
                else -> Unit
            }
        }
        val close = { if (closeRequested.compareAndSet(false, true)) client.get()?.send(TdApi.Close(), null) }
        // Held by the main client (the user opened this account) or another peek: leave it alone.
        // Freed by force after the window and a margin if this client never reaches Closed.
        val handle = com.kirox.app.telegram.SessionLocks.acquireHandle(account.session, windowMs + LOCK_MARGIN_MS) { close() } ?: return
        lock.set(handle)
        val created = runCatching { Client.create(handler, null, null) }.getOrNull()
        if (created == null) { com.kirox.app.telegram.SessionLocks.release(account.session, handle); return }
        client.set(created)
        if (wantsParameters.compareAndSet(true, false)) created.send(tdlibParameters(session, key), null)
        // A close asked for before the client existed could not be sent: send it now.
        if (closeRequested.get()) created.send(TdApi.Close(), null)
        else android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ close() }, windowMs)
    }

    // The POST_NOTIFICATIONS check is right before notify(); lint cannot follow the Android 13+ branch.
    @android.annotation.SuppressLint("MissingPermission")
    private fun show(account: TelegramAccount, groupId: Int, chatTitle: String, text: String) {
        val accountLabel = account.title.ifBlank { account.phone.ifBlank { com.kirox.app.i18n.I18n.t("Другой аккаунт") } }
        val tag = tagOf(account)
        val open = PendingIntent.getActivity(
            appContext, (account.session.hashCode() xor groupId),
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(EXTRA_SESSION, account.session).putExtra(EXTRA_KEY, secret(appContext)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, MessageNotifications.CHANNEL)
            .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox_v3)
            .setContentTitle(chatTitle.ifBlank { "Telegram" })
            .setContentText(text)
            .setSubText(accountLabel)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(open)
            // review: on a secure lock screen only «Новое сообщение», as for the open account.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(appContext, MessageNotifications.CHANNEL)
                    .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox_v3)
                    .setContentTitle("kiroX")
                    .setContentText(com.kirox.app.i18n.I18n.t("Новое сообщение"))
                    .build()
            )
            .build()
        // Without the notification permission (Android 13+) the other account's messages simply wait in the app.
        if (androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= 33) return
        try { NotificationManagerCompat.from(appContext).notify(tag, groupId, notification) } catch (_: SecurityException) {}
    }

    private fun tagOf(account: TelegramAccount) = "kirox-account-${account.session}"

    companion object {
        /** Intent extra: the account to open when the notification is tapped. */
        const val EXTRA_SESSION = "kirox.session"
        /** Intent extra proving the intent came from our own notification, not another app. */
        const val EXTRA_KEY = "kirox.session_key"

        /** Random per-install secret; MainActivity is exported, so the session extra alone is not trusted. */
        fun secret(context: Context): String {
            val prefs = context.getSharedPreferences("kirox_peek", Context.MODE_PRIVATE)
            return prefs.getString("secret", null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit { putString("secret", it) } }
        }
        private const val GROUPS = 10
        private const val GROUP_SIZE = 5
        private const val LOCK_MARGIN_MS = 20_000L
    }
}
