package com.kirox.app.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/** One live location being shared: the message in [chatId] that is moved along, until [until] (unix seconds). */
data class LiveShare(val chatId: Long, val messageId: Long, val until: Long)

/**
 * «Транслировать геопозицию»: sends a live location message and keeps moving it while the period
 * lasts, from a foreground service so it goes on with kiroX in the background. «Остановить» in the
 * chat or in the notification ends it for everyone.
 */
class LiveLocationService : Service(), LocationListener {
    private val manager by lazy { getSystemService(LocationManager::class.java) }
    private var listening = false
    /** Shares end by time even when the phone does not move and no new location arrives. */
    private val timer = android.os.Handler(android.os.Looper.getMainLooper())
    private val expiry = object : Runnable {
        override fun run() { if (dropEnded().isNotEmpty()) timer.postDelayed(this, EXPIRY_CHECK_MS) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { intent.getLongExtra(EXTRA_CHAT, 0).let { if (it != 0L) stop(this, it) else stopAll(this) }; return START_NOT_STICKY }
        ensureChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox_v3)
            .setContentTitle(I18n.t("Вы транслируете геопозицию"))
            .setContentText(I18n.t("kiroX обновляет её, пока трансляция не закончится"))
            .setOngoing(true)
            .addAction(0, I18n.t("Остановить"), PendingIntent.getService(this, 1, Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE))
            .build()
        // Android 14+ refuses the location type without the permission: no crash, just no sharing.
        val promoted = runCatching { androidx.core.app.ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) }.isSuccess
        // A sticky restart after the process died has no shares left (they live in memory): stop.
        if (!promoted || _active.value.isEmpty()) {
            if (promoted) stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        listen()
        timer.removeCallbacks(expiry)
        timer.postDelayed(expiry, EXPIRY_CHECK_MS)
        return START_STICKY
    }

    /** Forgets shares whose period is over; stops the service when none is left. Returns the live ones. */
    private fun dropEnded(): List<LiveShare> {
        val now = System.currentTimeMillis() / 1000
        val (live, ended) = _active.value.values.partition { it.until > now }
        ended.forEach { _active.value = _active.value - it.chatId }
        if (live.isEmpty()) stopSelf()
        return live
    }

    @SuppressLint("MissingPermission")
    private fun listen() {
        if (listening) return
        listening = runCatching {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }.forEach {
                manager.requestLocationUpdates(it, UPDATE_MS, UPDATE_METERS, this)
            }
            true
        }.getOrDefault(false)
    }

    override fun onLocationChanged(location: Location) {
        val live = dropEnded()
        if (live.isEmpty()) return
        val client = (application as KiroXApplication).telegram.client
        live.filter { it.messageId > 0 }.forEach { share ->
            client.send(TdApi.EditMessageLiveLocation(share.chatId, share.messageId, null, TdApi.LiveLocation(location.toTd(), 0, 0, 0)))
        }
    }

    override fun onDestroy() {
        timer.removeCallbacks(expiry)
        runCatching { manager.removeUpdates(this) }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "live_location"
        private const val NOTIFICATION_ID = 7101
        private const val ACTION_STOP = "com.kirox.app.location.STOP"
        private const val EXTRA_CHAT = "chat"
        private const val UPDATE_MS = 20_000L
        private const val UPDATE_METERS = 15f
        private const val EXPIRY_CHECK_MS = 30_000L
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val _active = MutableStateFlow<Map<Long, LiveShare>>(emptyMap())
        /** Chats where your location is being shared now. */
        val active: StateFlow<Map<Long, LiveShare>> = _active.asStateFlow()
        @Volatile private var handlerAdded = false
        /** review: SendSucceeded can arrive before the send result — temporary id → real id, read when the result comes. */
        private val sentEarly = java.util.concurrent.ConcurrentHashMap<Long, Long>()

        private fun Location.toTd() = TdApi.Location(latitude, longitude, accuracy.toDouble())

        /** Sends a live location to [chatId] for [seconds] from [location] and starts moving it. */
        fun start(context: Context, chatId: Long, seconds: Int, location: Location) {
            val app = context.applicationContext as KiroXApplication
            val client = app.telegram.client
            if (!handlerAdded) {
                handlerAdded = true
                // The sent message gets its real id from Telegram; the share follows it.
                client.addUpdateHandler { update ->
                    if (update is TdApi.UpdateMessageSendSucceeded && update.message.content is TdApi.MessageLocation) {
                        val share = _active.value.values.firstOrNull { it.messageId == update.oldMessageId }
                        if (share != null) _active.value = _active.value + (share.chatId to share.copy(messageId = update.message.id))
                        else sentEarly[update.oldMessageId] = update.message.id
                    }
                }
            }
            // wave5 (fixer-media): the foreground service starts now, while the app is on screen — started after the async
            // send, Android 12+ may already count the app as background and refuse it (the share would never move or end).
            val until = System.currentTimeMillis() / 1000 + seconds
            _active.value = _active.value + (chatId to LiveShare(chatId, 0, until))
            runCatching { androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, LiveLocationService::class.java)) }
            scope.launch {
                runCatching {
                    client.query(TdApi.SendMessage(chatId, null, null, null, null, TdApi.InputMessageLiveLocation(TdApi.LiveLocation(location.toTd(), seconds, 0, 0))))
                }.onSuccess { message ->
                    val id = sentEarly.remove(message.id) ?: message.id
                    if (_active.value.containsKey(chatId)) _active.value = _active.value + (chatId to LiveShare(chatId, id, until))
                }.onFailure {
                    if (_active.value[chatId]?.messageId == 0L) stop(context, chatId)
                }
            }
        }

        /** Ends sharing in [chatId]: Telegram shows the last point as the final one. */
        fun stop(context: Context, chatId: Long) {
            val share = _active.value[chatId] ?: return
            _active.value = _active.value - chatId
            val client = (context.applicationContext as KiroXApplication).telegram.client
            if (share.messageId > 0) client.send(TdApi.EditMessageLiveLocation(share.chatId, share.messageId, null, null))
            if (_active.value.isEmpty()) context.stopService(Intent(context, LiveLocationService::class.java))
        }

        fun stopAll(context: Context) = _active.value.keys.toList().forEach { stop(context, it) }

        private fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < 26) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, I18n.t("Трансляция геопозиции"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            )
        }
    }
}
