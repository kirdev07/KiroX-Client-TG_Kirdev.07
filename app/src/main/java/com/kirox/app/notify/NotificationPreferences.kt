package com.kirox.app.notify

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Notification settings that belong to this device, not to the Telegram account. */
data class LocalNotificationSettings(
    /** «В приложении»: a short sound for a new message while kiroX is open. */
    val inAppSound: Boolean = true,
    val inAppVibrate: Boolean = false,
    /** A banner at the top of the screen with the sender and text. */
    val inAppPreview: Boolean = true,
    /** Counter on the app icon (where the launcher shows one). */
    val badge: Boolean = true,
    /** The counter counts messages; off — one per chat. */
    val badgeCountsMessages: Boolean = true
)

class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(
        LocalNotificationSettings(
            prefs.getBoolean("in_app_sound", true),
            prefs.getBoolean("in_app_vibrate", false),
            prefs.getBoolean("in_app_preview", true),
            prefs.getBoolean("badge", true),
            prefs.getBoolean("badge_messages", true)
        )
    )
    val settings: StateFlow<LocalNotificationSettings> = _settings.asStateFlow()
    val current: LocalNotificationSettings get() = _settings.value

    fun update(value: LocalNotificationSettings) {
        _settings.value = value
        prefs.edit {
            putBoolean("in_app_sound", value.inAppSound)
            putBoolean("in_app_vibrate", value.inAppVibrate)
            putBoolean("in_app_preview", value.inAppPreview)
            putBoolean("badge", value.badge)
            putBoolean("badge_messages", value.badgeCountsMessages)
        }
    }

    /** Back to the defaults above (part of «Сбросить все уведомления»). */
    fun reset() = update(LocalNotificationSettings())
}

/** A new message that arrived while kiroX is open, for the in-app banner. */
data class InAppAlert(val chatId: Long, val title: String, val text: String)

/** In-app banners and the chat on screen, which gets neither a banner nor a sound. */
object InAppAlerts {
    private val _alerts = MutableSharedFlow<InAppAlert>(extraBufferCapacity = 4)
    val alerts: SharedFlow<InAppAlert> = _alerts.asSharedFlow()
    @Volatile var openChatId: Long = 0L

    internal fun emit(alert: InAppAlert) { _alerts.tryEmit(alert) }
}
