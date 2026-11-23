package com.kirox.app.media.music

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kirox.app.ui.chat.VoicePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Kinds of outputs, as the player shows them. */
enum class OutputKind { Phone, Wired, Bluetooth, Usb }

/** One output: [id] is the AudioDeviceInfo id; [battery] 0–100, or -1 when unknown. */
data class OutputRoute(val id: Int, val name: String, val kind: OutputKind, val address: String = "", val battery: Int = -1)

data class OutputState(
    val routes: List<OutputRoute> = emptyList(),
    /** Where the sound goes now. */
    val current: OutputRoute? = null,
    /** The output chosen in kiroX; null = the system decides. */
    val chosen: Int? = null,
    val volume: Int = 0,
    val maxVolume: Int = 15,
    /** BLUETOOTH_CONNECT is missing (Android 12+): names and battery of Bluetooth headphones may be hidden. */
    val needsBluetoothPermission: Boolean = false,
)

/**
 * The audio output for the music player: the list of outputs (phone speaker, wired, Bluetooth,
 * USB), the current one, Bluetooth headphones' battery (Android's hidden getBatteryLevel, kept up to
 * date by ACTION_BATTERY_LEVEL_CHANGED) and the media volume, which follows the volume keys.
 * Listens only between [start] and [stop] (while the player shows it).
 */
class AudioOutput(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(OutputState())
    val state: StateFlow<OutputState> = _state.asStateFlow()
    /** Battery levels by Bluetooth address, from the broadcast. */
    private val batteries = HashMap<String, Int>()
    private var users = 0

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
    }
    private val volumeObserver = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) = refreshVolume()
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = deviceOf(intent)
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (device != null) { runCatching { device.address }.getOrNull()?.let { batteries[it] = level }; refresh() }
                }
                VOLUME_CHANGED_ACTION -> refreshVolume()
                else -> refresh()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun deviceOf(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    fun start() {
        if (users++ > 0) { refresh(); return }
        audio?.registerAudioDeviceCallback(deviceCallback, main)
        runCatching { app.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeObserver) }
        val filter = IntentFilter().apply {
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(VOLUME_CHANGED_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        // System / Bluetooth-stack broadcasts: they come from another process, so the receiver is exported.
        runCatching { ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED) }
        refresh()
    }

    fun stop() {
        if (users == 0 || --users > 0) return
        audio?.unregisterAudioDeviceCallback(deviceCallback)
        runCatching { app.contentResolver.unregisterContentObserver(volumeObserver) }
        runCatching { app.unregisterReceiver(receiver) }
    }

    private fun hasBluetoothPermission() = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(app, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Reads everything again (after the permission was granted, an output changed…). */
    fun refresh() {
        val am = audio ?: return
        val chosen = VoicePlayer.output.value
        val routes = routes(am)
        val current = routes.firstOrNull { it.id == chosen } ?: systemRoute(am, routes)
        _state.value = OutputState(routes, current, chosen?.takeIf { id -> routes.any { it.id == id } },
            am.getStreamVolume(AudioManager.STREAM_MUSIC), am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            needsBluetoothPermission = !hasBluetoothPermission() && routes.any { it.kind == OutputKind.Bluetooth })
    }

    private fun refreshVolume() {
        val am = audio ?: return
        val v = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (v != _state.value.volume) _state.value = _state.value.copy(volume = v)
    }

    fun setVolume(value: Int) {
        val am = audio ?: return
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, value.coerceIn(0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0) }
        refreshVolume()
    }

    /** Sends the music to [id] (an output of [OutputState.routes]); null returns the choice to the system. */
    fun choose(id: Int?) {
        val device = id?.let { i -> audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull { it.id == i } }
        VoicePlayer.setOutput(device)
        refresh()
    }

    /**
     * Android's own output switcher (the media output panel): Android 14+ through MediaRouter2,
     * Android 11–13 through System UI / Settings. False when this phone has none — the player's own
     * list is shown then.
     */
    fun openSystemSwitcher(context: Context): Boolean {
        val pkg = app.packageName
        if (Build.VERSION.SDK_INT >= 34) {
            if (runCatching { android.media.MediaRouter2.getInstance(app).showSystemOutputSwitcher() }.getOrDefault(false)) return true
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val sent = runCatching {
                app.sendBroadcast(Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG").setPackage("com.android.systemui").putExtra("package_name", pkg))
                true
            }.getOrDefault(false)
            // Some skins (MIUI / HyperOS) ignore the broadcast; their Settings panel still works.
            if (sent && !Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) return true
        }
        return runCatching {
            context.startActivity(Intent("com.android.settings.panel.action.MEDIA_OUTPUT").putExtra("com.android.settings.panel.extra.PACKAGE_NAME", pkg)
                .apply { if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            true
        }.getOrDefault(false)
    }

    private fun routes(am: AudioManager): List<OutputRoute> {
        val bluetooth = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            if (Build.VERSION.SDK_INT >= 31) { add(AudioDeviceInfo.TYPE_BLE_HEADSET); add(AudioDeviceInfo.TYPE_BLE_SPEAKER) }
            if (Build.VERSION.SDK_INT >= 33) add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        }
        val wired = setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET)
        val usb = setOf(AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE)
        val bonded = bondedDevices()
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { d ->
            when (d.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputRoute(d.id, "Телефон", OutputKind.Phone)
                in bluetooth -> {
                    val address = runCatching { d.address }.getOrDefault("")
                    val dev = bonded.firstOrNull { it.first.equals(address, true) }
                    val name = d.productName?.toString()?.takeIf { it.isNotBlank() } ?: dev?.second ?: "Bluetooth"
                    OutputRoute(d.id, name, OutputKind.Bluetooth, address, battery(address))
                }
                in wired -> OutputRoute(d.id, "Проводные наушники", OutputKind.Wired)
                in usb -> OutputRoute(d.id, d.productName?.toString()?.takeIf { it.isNotBlank() } ?: "USB-наушники", OutputKind.Usb)
                else -> null
            }
        }.distinctBy { it.kind to it.name }.sortedBy { it.kind.ordinal }
    }

    /** Where Android sends media now (Android 13+ says it exactly; before, headphones win over the speaker). */
    private fun systemRoute(am: AudioManager, routes: List<OutputRoute>): OutputRoute? {
        if (Build.VERSION.SDK_INT >= 33) {
            val attrs = android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build()
            val ids = runCatching { am.getAudioDevicesForAttributes(attrs).map { it.id } }.getOrDefault(emptyList())
            routes.firstOrNull { it.id in ids }?.let { return it }
        }
        return routes.firstOrNull { it.kind == OutputKind.Bluetooth } ?: routes.firstOrNull { it.kind == OutputKind.Wired }
            ?: routes.firstOrNull { it.kind == OutputKind.Usb } ?: routes.firstOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun bondedDevices(): List<Pair<String, String>> {
        if (!hasBluetoothPermission()) return emptyList()
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        return runCatching { adapter.bondedDevices.orEmpty().map { it.address to (it.name ?: "") } }.getOrDefault(emptyList())
    }

    /** Battery of the Bluetooth device at [address], 0–100, or -1 when the headphones do not report it. */
    @SuppressLint("MissingPermission")
    private fun battery(address: String): Int {
        if (address.isBlank()) return -1
        batteries[address]?.takeIf { it in 0..100 }?.let { return it }
        if (!hasBluetoothPermission()) return -1
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter ?: return -1
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return -1
        // Hidden but present on Android 8.1+ (the level the status bar shows); -1 when unknown.
        val level = runCatching { BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as Int }.getOrDefault(-1)
        if (level in 0..100) batteries[address] = level
        return level.takeIf { it in 0..100 } ?: -1
    }

    companion object {
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

        @Volatile private var instance: AudioOutput? = null
        fun get(context: Context): AudioOutput = instance ?: synchronized(this) { instance ?: AudioOutput(context).also { instance = it } }
    }
}
