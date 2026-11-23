package com.kirox.app.ui.v3.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.kirox.app.ui.v3.model.KxBookmark
import com.kirox.app.ui.v3.model.KxMemoryFeatures
import com.kirox.app.ui.v3.model.KxNote
import com.kirox.app.ui.v3.model.KxReminder
import com.kirox.app.ui.v3.model.KxTemplate
import org.json.JSONArray
import org.json.JSONObject

/*
 * features: the phone-only storage of templates, bookmarks, notes and reminders, per Telegram account
 * (user id), in shared preferences like the rest of the kiroX settings.
 *  - kirox_templates and kirox_features_settings are in «Резервная копия настроек» (the user's own texts and switches);
 *  - kirox_features (bookmarks, notes, reminders) never leaves the phone: they quote other people's messages.
 * Reminders are AlarmManager alarms → [KxReminderReceiver] → a notification that opens the message.
 */

/** JSON form of the feature lists (unit-tested). */
internal object KxFeatureJson {
    fun templates(list: List<KxTemplate>): String = JSONArray(list.map { JSONObject().put("id", it.id).put("k", it.key).put("t", it.text).put("u", it.uses) }).toString()
    fun templates(s: String?): List<KxTemplate> = arr(s) { KxTemplate(it.optLong("id"), it.optString("k"), it.optString("t"), it.optInt("u")) }
        .filter { it.id != 0L && it.key.isNotBlank() }

    fun bookmarks(list: List<KxBookmark>): String = JSONArray(list.map {
        JSONObject().put("c", it.chatId).put("m", it.messageId).put("ti", it.chatTitle).put("s", it.sender).put("t", it.text).put("d", it.date).put("a", it.added)
    }).toString()
    fun bookmarks(s: String?): List<KxBookmark> = arr(s) { KxBookmark(it.optLong("c"), it.optLong("m"), it.optString("ti"), it.optString("s"), it.optString("t"), it.optInt("d"), it.optLong("a")) }
        .filter { it.chatId != 0L && it.messageId != 0L }

    fun notes(map: Map<Long, KxNote>): String = JSONArray(map.values.map { JSONObject().put("c", it.chatId).put("ti", it.title).put("t", it.text).put("u", it.updated) }).toString()
    fun notes(s: String?): Map<Long, KxNote> = arr(s) { KxNote(it.optLong("c"), it.optString("ti"), it.optString("t"), it.optLong("u")) }
        .filter { it.chatId != 0L && it.text.isNotBlank() }.associateBy { it.chatId }

    fun reminders(list: List<KxReminder>): String = JSONArray(list.map {
        JSONObject().put("id", it.id).put("c", it.chatId).put("m", it.messageId).put("ti", it.chatTitle).put("t", it.text).put("at", it.at)
    }).toString()
    fun reminders(s: String?): List<KxReminder> = arr(s) { KxReminder(it.optInt("id"), it.optLong("c"), it.optLong("m"), it.optString("ti"), it.optString("t"), it.optLong("at")) }
        .filter { it.id != 0 && it.chatId != 0L }

    fun settings(map: Map<String, Int>): String = JSONObject(map as Map<*, *>).toString()
    fun settings(s: String?): Map<String, Int> = runCatching { JSONObject(s ?: return emptyMap()) }.getOrNull()?.let { o -> o.keys().asSequence().associateWith { o.optInt(it) } }.orEmpty()

    private inline fun <T> arr(s: String?, item: (JSONObject) -> T): List<T> {
        val a = runCatching { JSONArray(s ?: return emptyList()) }.getOrNull() ?: return emptyList()
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { runCatching { item(it) }.getOrNull() } }
    }
}

/** One account's saved features; created by [KxFeatureFiles.of]. */
internal class KxLocalFeatures(private val app: Context, val account: Long, templates: List<KxTemplate>, bookmarks: List<KxBookmark>,
                               notes: Map<Long, KxNote>, reminders: List<KxReminder>, settings: Map<String, Int>)
    : KxMemoryFeatures(templates, bookmarks, notes, reminders, settings) {
    override fun changed(what: Char) {
        val f = KxFeatureFiles
        when (what) {
            't' -> f.prefs(app, f.TEMPLATES).edit { putString("t.$account", KxFeatureJson.templates(templates)) }
            's' -> f.prefs(app, f.SETTINGS).edit { putString("s", KxFeatureJson.settings(settings.filterKeys { it in com.kirox.app.ui.v3.model.KxFeatureKeys.defaults })) }
            'b' -> f.prefs(app, f.PRIVATE).edit { putString("b.$account", KxFeatureJson.bookmarks(bookmarks)) }
            'n' -> f.prefs(app, f.PRIVATE).edit { putString("n.$account", KxFeatureJson.notes(notes)) }
            'r' -> f.prefs(app, f.PRIVATE).edit { putString("r.$account", KxFeatureJson.reminders(reminders)) }
        }
    }
    override fun scheduled(r: KxReminder) = KxReminders.schedule(app, account, r)
    override fun cancelled(id: Int) = KxReminders.cancel(app, id)
}

internal object KxFeatureFiles {
    const val TEMPLATES = "kirox_templates"
    const val SETTINGS = "kirox_features_settings"
    const val PRIVATE = "kirox_features"
    private val open = HashMap<Long, KxLocalFeatures>()

    fun prefs(context: Context, name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    /** The features of account [account] (user id); 0 (not signed in yet) gets an empty, unsaved set. */
    @Synchronized fun of(context: Context, account: Long): KxLocalFeatures = open.getOrPut(account) {
        val app = context.applicationContext
        val p = prefs(app, PRIVATE)
        KxLocalFeatures(app, account,
            KxFeatureJson.templates(prefs(app, TEMPLATES).getString("t.$account", null)),
            KxFeatureJson.bookmarks(p.getString("b.$account", null)),
            KxFeatureJson.notes(p.getString("n.$account", null)),
            KxFeatureJson.reminders(p.getString("r.$account", null)),
            KxFeatureJson.settings(prefs(app, SETTINGS).getString("s", null)))
    }

    @Synchronized fun opened(account: Long): KxLocalFeatures? = open[account]

    /** A reminder fired: drop it from the saved list (and from the open copy). */
    @Synchronized fun fired(context: Context, account: Long, id: Int) {
        open[account]?.let { it.fired(id); return }
        val p = prefs(context, PRIVATE)
        val left = KxFeatureJson.reminders(p.getString("r.$account", null)).filterNot { it.id == id }
        p.edit { putString("r.$account", KxFeatureJson.reminders(left)) }
    }

    /** Every saved reminder of every account: (account, reminder). */
    fun allReminders(context: Context): List<Pair<Long, KxReminder>> = prefs(context, PRIVATE).all.entries
        .filter { it.key.startsWith("r.") }
        .flatMap { (k, v) -> val acc = k.removePrefix("r.").toLongOrNull() ?: return@flatMap emptyList(); KxFeatureJson.reminders(v as? String).map { acc to it } }
}

/** Alarms and the notification of «Напомнить прочитать позже». */
internal object KxReminders {
    const val CHANNEL = "kirox_reminders"
    private const val ACTION_FIRE = "com.kirox.app.REMIND"
    private const val EXTRA_ACCOUNT = "acc"
    private const val EXTRA_ID = "id"

    private fun intent(context: Context, account: Long, r: KxReminder) = Intent(context, KxReminderReceiver::class.java).setAction(ACTION_FIRE)
        .putExtra(EXTRA_ACCOUNT, account).putExtra(EXTRA_ID, r.id)
        .putExtra("c", r.chatId).putExtra("m", r.messageId).putExtra("ti", r.chatTitle).putExtra("t", r.text)

    fun schedule(context: Context, account: Long, r: KxReminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, r.id, intent(context, account, r), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Exact when the system lets us (no extra permission is asked); otherwise Android may shift it by a few minutes.
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi)
        }.onFailure { am.set(AlarmManager.RTC_WAKEUP, r.at, pi) }
    }

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, id, Intent(context, KxReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        am.cancel(pi); pi.cancel()
        NotificationManagerCompat.from(context).cancel(id)
    }

    /** After a reboot / update alarms are gone: set them again (missed ones show now). */
    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        KxFeatureFiles.allReminders(context).forEach { (acc, r) -> schedule(context, acc, if (r.at < now) r.copy(at = now + 5_000) else r) }
    }

    /** Chat ids hidden in «Скрытые чаты» on any account of this phone (their reminders stay neutral). */
    private fun hidden(context: Context, chatId: Long): Boolean = context.getSharedPreferences(KxDnd.PREFS, Context.MODE_PRIVATE).all
        .any { (k, v) -> k.startsWith(com.kirox.app.ui.v3.model.KxPrefsAccount.HIDDEN_CHATS) && (v as? String).orEmpty().split('\n').any { it.trim() == chatId.toString() } }

    fun fire(context: Context, i: Intent) {
        val account = i.getLongExtra(EXTRA_ACCOUNT, 0); val id = i.getIntExtra(EXTRA_ID, 0)
        val chatId = i.getLongExtra("c", 0); val messageId = i.getLongExtra("m", 0)
        if (id == 0 || chatId == 0L) return
        KxFeatureFiles.fired(context, account, id)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, com.kirox.app.i18n.I18n.t("Напоминания kiroX"), NotificationManager.IMPORTANCE_HIGH))
        val secret = hidden(context, chatId)
        val open = Intent(context, com.kirox.app.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (!secret) { putExtra(com.kirox.app.notify.MessageNotifications.EXTRA_OPEN_CHAT, chatId); putExtra(KxFeatureJump.EXTRA_CHAT, chatId); putExtra(KxFeatureJump.EXTRA_MESSAGE, messageId) } }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox_v3)
            .setContentTitle(if (secret) "kiroX" else "Прочитать позже · ${i.getStringExtra("ti").orEmpty()}")
            .setContentText(if (secret) "Напоминание" else i.getStringExtra("t").orEmpty().ifBlank { "Сообщение" })
            .setStyle(if (secret) null else NotificationCompat.BigTextStyle().bigText(i.getStringExtra("t").orEmpty()))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun isFire(i: Intent) = i.action == ACTION_FIRE
}

/** Reminder alarms, and boot / update (alarms are cleared then). */
class KxReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (KxReminders.isFire(intent)) KxReminders.fire(context, intent)
        else if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) KxReminders.rescheduleAll(context)
    }
}

/**
 * «Открыть сообщение» from a reminder or a bookmark: the chat opens (MainActivity → NotificationTarget / ctx.openChat),
 * then the chat layer (features/KxFeatureChat) scrolls to the message once it is on screen.
 */
object KxFeatureJump {
    const val EXTRA_CHAT = "kirox.jump_chat"
    const val EXTRA_MESSAGE = "kirox.jump_msg"
    @Volatile var pending: Pair<Long, Long>? = null

    fun request(chatId: Long, messageId: Long) { pending = chatId to messageId }

    /** MainActivity: a reminder notification was tapped. */
    fun take(intent: Intent?) {
        val c = intent?.getLongExtra(EXTRA_CHAT, 0L) ?: 0L
        val m = intent?.getLongExtra(EXTRA_MESSAGE, 0L) ?: 0L
        if (c != 0L && m != 0L) { pending = c to m; intent?.removeExtra(EXTRA_CHAT); intent?.removeExtra(EXTRA_MESSAGE) }
    }

    /** The message to scroll to in [chatId], once (null = none). */
    fun consume(chatId: Long): Long? = pending?.takeIf { it.first == chatId }?.second?.also { pending = null }
}

/** Composer «when» → TDLib scheduling state (unit-tested): -1 = «Отправить, когда будет в сети», > 0 = at that unix time, else now. */
internal object KxScheduleMapping {
    fun state(at: Int): org.drinkless.tdlib.TdApi.MessageSchedulingState? = when {
        at == com.kirox.app.ui.v3.model.KxSendWhen.WHEN_ONLINE -> org.drinkless.tdlib.TdApi.MessageSchedulingStateSendWhenOnline()
        at > 0 -> org.drinkless.tdlib.TdApi.MessageSchedulingStateSendAtDate(at, 0)
        else -> null
    }
}
