package com.kirox.app.notify

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.kirox.app.KiroXApplication
import com.kirox.app.MainActivity
import com.kirox.app.telegram.TelegramChatRepository
import com.kirox.app.telegram.TelegramClient
import com.kirox.app.telegram.TelegramUserRepository
import com.kirox.app.telegram.displayName
import org.drinkless.tdlib.TdApi
import kotlin.time.Duration.Companion.seconds

/**
 * New-message notifications from TDLib's notification API. TDLib decides what to notify about
 * (it skips muted chats and drops notifications once messages are read on any device); this class
 * only mirrors its notification groups as Android notifications, one per chat, in a channel per
 * chat type (own sound and vibration in the system settings). While the app is on screen a new
 * message gives an in-app sound, vibration and banner instead, as set in «Уведомления и звуки».
 */
class MessageNotifications(
    private val context: Context,
    private val client: TelegramClient,
    private val chats: TelegramChatRepository,
    private val users: TelegramUserRepository,
    private val prefs: NotificationPreferences
) {
    /** [topic]: forum topic or comment thread of the message; the quick reply goes there. */
    private class Item(val id: Int, val date: Int, val sender: String, val text: String, val topic: TdApi.MessageTopic? = null)
    private class Group(val chatId: Long, val items: MutableList<Item> = mutableListOf())

    // Dispatcher-confined, like the repositories.
    private val groups = HashMap<Int, Group>()

    init {
        ensureChannel(context)
        // Telegram must know when the app goes to the background: it then marks the user offline
        // and TDLib starts notifying about new messages.
        AppVisibility.listener = { refreshOnline() }
        client.addUpdateHandler { update ->
            when (update) {
                is TdApi.UpdateMessageSendSucceeded -> sending.remove(update.oldMessageId)?.complete(true)
                is TdApi.UpdateMessageSendFailed -> sending.remove(update.oldMessageId)?.complete(false)
                is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                    is TdApi.AuthorizationStateReady -> enable()
                    // A background pause keeps what is already shown; logout and account switch clear it.
                    is TdApi.AuthorizationStateClosed -> { if (!client.paused) groups.keys.forEach(::cancel); groups.clear() }
                }
                is TdApi.UpdateActiveNotifications -> update.groups.forEach { group ->
                    apply(group.id, group.chatId, group.notifications, IntArray(0))
                }
                is TdApi.UpdateNotificationGroup -> apply(update.notificationGroupId, update.chatId, update.addedNotifications, update.removedNotificationIds)
            }
        }
    }

    private fun enable() {
        client.send(TdApi.SetOption("notification_group_count_max", TdApi.OptionValueInteger(MAX_GROUPS)))
        client.send(TdApi.SetOption("notification_group_size_max", TdApi.OptionValueInteger(MAX_PER_GROUP)))
        // A process started by a push has no visible screen: stay offline so Telegram keeps notifying.
        refreshOnline()
    }

    /** Tells Telegram whether you are online: while kiroX is on screen, unless ghost mode hides it. */
    fun refreshOnline() {
        val online = AppVisibility.visible && !com.kirox.app.settings.KiroxTweaks.current.ghostOnline
        client.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(online)))
    }

    private fun apply(groupId: Int, chatId: Long, added: Array<TdApi.Notification>, removed: IntArray) {
        val group = groups.getOrPut(groupId) { Group(chatId) }
        val removedSet = removed.toHashSet()
        group.items.removeAll { it.id in removedSet }
        var fresh = false
        for (notification in added) {
            val type = notification.type as? TdApi.NotificationTypeNewMessage ?: continue
            group.items.removeAll { it.id == notification.id }
            val topic = type.message.topicId?.takeIf { it is TdApi.MessageTopicForum || it is TdApi.MessageTopicThread }
            group.items += Item(notification.id, notification.date, senderName(type.message), if (type.showPreview) preview(type.message) else com.kirox.app.i18n.I18n.t("Новое сообщение"), topic)
            if (!notification.isSilent) fresh = true
        }
        group.items.sortBy { it.date }
        while (group.items.size > MAX_PER_GROUP) group.items.removeAt(0)
        if (group.items.isEmpty()) {
            groups.remove(groupId)
            cancel(groupId)
        } else if (!AppVisibility.visible) {
            show(groupId, group, alert = fresh)
        } else if (fresh && chatId != InAppAlerts.openChatId) {
            alertInApp(group)
        }
    }

    /** New message while kiroX is open: sound, vibration and banner as chosen; the open chat gets none. */
    private fun alertInApp(group: Group) {
        val local = prefs.current
        val last = group.items.lastOrNull() ?: return
        if (local.inAppPreview) {
            val title = chats.chat(group.chatId)?.title.orEmpty().ifBlank { last.sender }
            InAppAlerts.emit(InAppAlert(group.chatId, title, if (last.sender.isNotBlank() && last.sender != title) "${last.sender}: ${last.text}" else last.text))
        }
        if (local.inAppSound) runCatching {
            android.media.RingtoneManager.getRingtone(context, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
        if (local.inAppVibrate) runCatching {
            context.getSystemService(android.os.Vibrator::class.java)?.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Channel of the chat's type: private chats, groups or channels. */
    private fun channelFor(chatId: Long): String = when (val type = chats.chat(chatId)?.type) {
        is TdApi.ChatTypeSupergroup -> if (type.isChannel) CHANNEL_CHANNELS else CHANNEL_GROUPS
        is TdApi.ChatTypeBasicGroup -> CHANNEL_GROUPS
        else -> CHANNEL
    }

    private fun show(groupId: Int, group: Group, alert: Boolean) {
        val title = chats.chat(group.chatId)?.title.orEmpty().ifBlank { "Telegram" }
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName(com.kirox.app.i18n.I18n.t("Вы")).build())
            // In a private chat the sender already names it; a title there shows up as "Name: Name".
            .setConversationTitle(title.takeIf { group.items.any { it.sender.isNotBlank() && it.sender != title } })
            .setGroupConversation(group.items.map { it.sender }.distinct().size > 1)
        group.items.forEach { item ->
            style.addMessage(item.text, item.date * 1000L, Person.Builder().setName(item.sender.ifBlank { title }).build())
        }
        val local = prefs.current
        val notification = NotificationCompat.Builder(context, channelFor(group.chatId))
            .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox)
            .setStyle(style)
            // «Значок на иконке»: launchers add these up; off hides the counter and the dot.
            .setNumber(if (!local.badge) 0 else if (local.badgeCountsMessages) group.items.size else 1)
            .setBadgeIconType(if (local.badge) NotificationCompat.BADGE_ICON_SMALL else NotificationCompat.BADGE_ICON_NONE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(!alert)
            // On a secure lock screen only "new message" shows, not the text.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox)
                    .setContentTitle("kiroX")
                    .setContentText(com.kirox.app.i18n.I18n.t("Новое сообщение"))
                    .build()
            )
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, groupId, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(EXTRA_OPEN_CHAT, group.chatId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                NotificationCompat.Action.Builder(0, com.kirox.app.i18n.I18n.t("Ответить"), PendingIntent.getBroadcast(
                    context, groupId + REPLY_REQUEST_OFFSET,
                    Intent(context, MessageActionReceiver::class.java).setAction(ACTION_REPLY)
                        .putExtra(EXTRA_GROUP, groupId).putExtra(EXTRA_CHAT, group.chatId)
                        .putExtra(EXTRA_MAX, group.items.maxOf { it.id })
                        .apply { putTopic(this, group.items.maxByOrNull { it.date }?.topic) },
                    // The system writes the typed text into the intent, so it must be mutable.
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                ))
                    .addRemoteInput(androidx.core.app.RemoteInput.Builder(KEY_REPLY).setLabel(com.kirox.app.i18n.I18n.t("Сообщение")).build())
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setShowsUserInterface(false)
                    .build()
            )
            .addAction(
                0, com.kirox.app.i18n.I18n.t("Прочитано"),
                PendingIntent.getBroadcast(
                    context, groupId,
                    Intent(context, MessageActionReceiver::class.java).setAction(ACTION_READ)
                        .putExtra(EXTRA_GROUP, groupId).putExtra(EXTRA_CHAT, group.chatId)
                        .putExtra(EXTRA_MESSAGES, group.items.map { it.id }.toIntArray())
                        .putExtra(EXTRA_MAX, group.items.maxOf { it.id }),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        // Without the notification permission (Android 13+) messages simply stay in the app.
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33) return
        try { NotificationManagerCompat.from(context).notify(TAG, groupId, notification) } catch (_: SecurityException) {}
    }

    private fun cancel(groupId: Int) = NotificationManagerCompat.from(context).cancel(TAG, groupId)

    /**
     * Sends [text] to [chatId] from the notification and marks the chat read. Starts TDLib if it was
     * paused in the background and waits for it; returns false when the message could not be sent.
     */
    suspend fun reply(groupId: Int, chatId: Long, maxNotificationId: Int, text: String, topic: TdApi.MessageTopic? = null): Boolean {
        client.start()
        try {
            if (kotlinx.coroutines.withTimeoutOrNull(REPLY_WAIT) { client.ready.first { it } } == null) return false
            // SendMessage answers with a local, still pending message; only UpdateMessageSendSucceeded
            // means Telegram has it, so the process is kept until then.
            val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
            val pending = runCatching {
                // In a forum topic or a comment thread the answer stays there, not in the general chat.
                client.query(TdApi.SendMessage(chatId, topic, null, null, null, TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, true)))
            }.getOrNull() ?: return false
            if (pending.sendingState == null) done.complete(true) else sending[pending.id] = done
            val sent = kotlinx.coroutines.withTimeoutOrNull(SEND_WAIT) { done.await() } ?: false
            sending.remove(pending.id)
            if (sent) markRead(groupId, chatId, maxNotificationId)
            return sent
        } finally {
            // Started for this reply while in the background: close TDLib again so pushes keep coming.
            client.post(PAUSE_DELAY_MS) { client.pauseIfIdle() }
        }
    }

    /** Messages sent from notifications, by their temporary id, waiting for Telegram's answer. */
    private val sending = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.CompletableDeferred<Boolean>>()

    /** Removes the notification of [groupId] at once (TDLib also removes it after markRead). */
    fun dismiss(groupId: Int) = cancel(groupId)

    /** Marks the chat read: TDLib then removes the group everywhere. */
    fun markRead(groupId: Int, chatId: Long, maxNotificationId: Int) = client.post {
        client.send(TdApi.RemoveNotificationGroup(groupId, maxNotificationId))
        chats.chat(chatId)?.lastMessage?.id?.let { last -> client.send(TdApi.ViewMessages(chatId, longArrayOf(last), null, true)) }
    }

    private fun senderName(message: TdApi.Message): String = when (val sender = message.senderId) {
        is TdApi.MessageSenderUser -> users.user(sender.userId)?.displayName().orEmpty()
        is TdApi.MessageSenderChat -> chats.chat(sender.chatId)?.title.orEmpty()
        else -> ""
    }

    private fun preview(message: TdApi.Message): String = when (val content = message.content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> content.caption.text.ifBlank { com.kirox.app.i18n.I18n.t("Фото") }
        is TdApi.MessageVideo -> content.caption.text.ifBlank { com.kirox.app.i18n.I18n.t("Видео") }
        is TdApi.MessageDocument -> content.caption.text.ifBlank { com.kirox.app.i18n.I18n.t("Файл") }
        is TdApi.MessageVoiceNote -> com.kirox.app.i18n.I18n.t("Голосовое сообщение")
        is TdApi.MessageVideoNote -> com.kirox.app.i18n.I18n.t("Видеосообщение")
        is TdApi.MessageSticker -> "${content.sticker.emoji} Стикер".trim()
        is TdApi.MessageAnimation -> "GIF"
        is TdApi.MessageAudio -> com.kirox.app.i18n.I18n.t("Аудио")
        is TdApi.MessageContact -> com.kirox.app.i18n.I18n.t("Контакт")
        is TdApi.MessageLocation -> com.kirox.app.i18n.I18n.t("Местоположение")
        is TdApi.MessagePoll -> com.kirox.app.i18n.I18n.t("Опрос")
        is TdApi.MessageCall -> com.kirox.app.i18n.I18n.t("Звонок")
        else -> com.kirox.app.i18n.I18n.t("Сообщение")
    }

    companion object {
        /** Private chats; other accounts' messages (AccountPeek) use it too. */
        internal const val CHANNEL = "messages_private"
        internal const val CHANNEL_GROUPS = "messages_groups"
        internal const val CHANNEL_CHANNELS = "messages_channels"
        /** The single channel of earlier versions; removed so the system shows the three new ones. */
        private const val OLD_CHANNEL = "messages"
        private const val TAG = "kirox-messages"
        private const val MAX_GROUPS = 10L
        private const val MAX_PER_GROUP = 6L
        internal const val ACTION_READ = "com.kirox.app.notify.READ"
        internal const val ACTION_REPLY = "com.kirox.app.notify.REPLY"
        internal const val KEY_REPLY = "reply_text"
        /** Keeps the reply and read PendingIntents of one group apart. */
        private const val REPLY_REQUEST_OFFSET = 1 shl 20
        private val REPLY_WAIT = 15.seconds
        private val SEND_WAIT = 10.seconds
        /** Lets markRead reach Telegram before TDLib is closed. */
        private const val PAUSE_DELAY_MS = 1_500L
        internal const val EXTRA_GROUP = "group"
        internal const val EXTRA_CHAT = "chat"
        /** MainActivity extra: the chat to open when a message notification is tapped. */
        const val EXTRA_OPEN_CHAT = "kirox.open_chat"
        internal const val EXTRA_MESSAGES = "messages"
        internal const val EXTRA_MAX = "max"
        private const val EXTRA_FORUM_TOPIC = "forum_topic"
        private const val EXTRA_THREAD = "thread"

        private fun putTopic(intent: Intent, topic: TdApi.MessageTopic?) {
            when (topic) {
                is TdApi.MessageTopicForum -> intent.putExtra(EXTRA_FORUM_TOPIC, topic.forumTopicId)
                is TdApi.MessageTopicThread -> intent.putExtra(EXTRA_THREAD, topic.messageThreadId)
                else -> Unit
            }
        }

        /** The topic a reply from this notification belongs to, or null for the chat itself. */
        internal fun topicOf(intent: Intent): TdApi.MessageTopic? = when {
            intent.hasExtra(EXTRA_FORUM_TOPIC) -> TdApi.MessageTopicForum(intent.getIntExtra(EXTRA_FORUM_TOPIC, 0))
            intent.hasExtra(EXTRA_THREAD) -> TdApi.MessageTopicThread(intent.getLongExtra(EXTRA_THREAD, 0))
            else -> null
        }

        /** Channel names follow the interface language; called again when it changes. */
        fun refreshChannels(context: Context) = ensureChannel(context)

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(OLD_CHANNEL)
            listOf(
                Triple(CHANNEL, com.kirox.app.i18n.I18n.t("Личные чаты"), com.kirox.app.i18n.I18n.t("Новые сообщения в личных чатах")),
                Triple(CHANNEL_GROUPS, com.kirox.app.i18n.I18n.t("Группы"), com.kirox.app.i18n.I18n.t("Новые сообщения в группах")),
                Triple(CHANNEL_CHANNELS, com.kirox.app.i18n.I18n.t("Каналы"), com.kirox.app.i18n.I18n.t("Новые публикации в каналах"))
            ).forEach { (id, name, text) ->
                manager.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = text
                    enableVibration(true)
                })
            }
        }
    }
}

/** "Прочитано" in a message notification. */
class MessageActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifications = (context.applicationContext as KiroXApplication).telegram.notifications
        if (intent.action == MessageNotifications.ACTION_REPLY) {
            val text = androidx.core.app.RemoteInput.getResultsFromIntent(intent)?.getCharSequence(MessageNotifications.KEY_REPLY)?.toString()?.trim()
            val groupId = intent.getIntExtra(MessageNotifications.EXTRA_GROUP, 0)
            if (text.isNullOrEmpty()) { notifications.dismiss(groupId); return }
            val pending = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val ok = notifications.reply(groupId, intent.getLongExtra(MessageNotifications.EXTRA_CHAT, 0), intent.getIntExtra(MessageNotifications.EXTRA_MAX, 0), text, MessageNotifications.topicOf(intent))
                    if (ok) notifications.dismiss(groupId)
                    else android.os.Handler(android.os.Looper.getMainLooper()).post { android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t("Не удалось отправить ответ"), android.widget.Toast.LENGTH_SHORT).show() }
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action != MessageNotifications.ACTION_READ) return
        notifications.markRead(
            intent.getIntExtra(MessageNotifications.EXTRA_GROUP, 0),
            intent.getLongExtra(MessageNotifications.EXTRA_CHAT, 0),
            intent.getIntExtra(MessageNotifications.EXTRA_MAX, 0)
        )
    }
}

/** Whether any activity is started; notifications are not shown over the open app. */
/** Chat a tapped notification asked to open; KiroXApp opens it once signed in and clears it. */
object NotificationTarget {
    val chat = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
}

object AppVisibility {
    @Volatile var visible = false
        internal set(value) {
            if (field == value) return
            field = value
            listener?.invoke(value)
            com.kirox.app.security.PasscodeLock.onVisibilityChanged(value)
        }
    @Volatile internal var listener: ((Boolean) -> Unit)? = null
}
