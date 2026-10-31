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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { intent.getLongExtra(EXTRA_CHAT, 0).let { if (it != 0L) stop(this, it) else stopAll(this) }; return START_NOT_STICKY }
        ensureChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.kirox.app.R.drawable.ic_stat_kirox)
            .setContentTitle(I18n.t("Вы транслируете геопозицию"))
            .setContentText(I18n.t("kiroX обновляет её, пока трансляция не закончится"))
            .setOngoing(true)
            .addAction(0, I18n.t("Остановить"), PendingIntent.getService(this, 1, Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE))
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        listen()
        return START_STICKY
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
        val now = System.currentTimeMillis() / 1000
        val shares = _active.value
        val (live, ended) = shares.values.partition { it.until > now }
        ended.forEach { _active.value = _active.value - it.chatId }
        if (live.isEmpty()) { stopSelf(); return }
        val client = (application as KiroXApplication).telegram.client
        live.filter { it.messageId > 0 }.forEach { share ->
            client.send(TdApi.EditMessageLiveLocation(share.chatId, share.messageId, null, TdApi.LiveLocation(location.toTd(), 0, 0, 0)))
        }
    }

    override fun onDestroy() {
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
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val _active = MutableStateFlow<Map<Long, LiveShare>>(emptyMap())
        /** Chats where your location is being shared now. */
        val active: StateFlow<Map<Long, LiveShare>> = _active.asStateFlow()
        @Volatile private var handlerAdded = false

        private fun Location.toTd() = TdApi.Location(latitude, longitude, accuracy.toDouble())

        /** Sends a live location to [chatId] for [seconds] from [location] and starts moving it. */
        fun start(context: Context, chatId: Long, seconds: Int, location: Location) {
            val app = context.applicationContext as KiroXApplication
            val client = app.telegram.client
            if (!handlerAdded) {
                handlerAdded = true
                // The sent message gets its real id from Telegram; the share follows it.
                client.addUpdateHandler { update ->
                    if (update is TdApi.UpdateMessageSendSucceeded) _active.value.values.firstOrNull { it.messageId == update.oldMessageId }?.let { share ->
                        _active.value = _active.value + (share.chatId to share.copy(messageId = update.message.id))
                    }
                }
            }
            scope.launch {
                runCatching {
                    client.query(TdApi.SendMessage(chatId, null, null, null, null, TdApi.InputMessageLiveLocation(TdApi.LiveLocation(location.toTd(), seconds, 0, 0))))
                }.onSuccess { message ->
                    _active.value = _active.value + (chatId to LiveShare(chatId, message.id, System.currentTimeMillis() / 1000 + seconds))
                    runCatching { context.startForegroundService(Intent(context, LiveLocationService::class.java)) }
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
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, I18n.t("Трансляция геопозиции"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            )
        }
    }
}
