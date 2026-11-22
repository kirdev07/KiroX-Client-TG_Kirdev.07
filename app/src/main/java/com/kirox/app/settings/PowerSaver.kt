package com.kirox.app.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * «Энергосбережение»: while it is active kiroX turns off animations, glass blur and heavy effects
 * and stops GIFs from playing by themselves. «Как в системе» follows the phone's battery saver.
 */
object PowerSaver {
    private val systemSaver = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _battery = MutableStateFlow(100)
    private val charging = MutableStateFlow(false)
    private val _threshold = MutableStateFlow(0)
    private var prefs: android.content.SharedPreferences? = null

    /** Battery level, %. */
    val battery: StateFlow<Int> = _battery
    /** «Как в системе» also turns saving on at this battery level and below (not while charging); 0 = system only (default, so the old app behaves as before; the v3 Power board sets it). */
    val threshold: StateFlow<Int> = _threshold

    val active: StateFlow<Boolean> = combine(KiroxTweaks.state, systemSaver, combine(_battery, charging, _threshold) { b, c, t -> t > 0 && !c && b <= t }) { tweaks, system, low ->
        when (tweaks.powerSaving) {
            PowerSaving.On -> true
            PowerSaving.Off -> false
            PowerSaving.Auto -> system || low
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    fun init(context: Context) {
        val power = context.getSystemService(PowerManager::class.java)
        systemSaver.value = power?.isPowerSaveMode == true
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { systemSaver.value = power?.isPowerSaveMode == true }
        }, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        prefs = context.getSharedPreferences("power_saver", Context.MODE_PRIVATE).also { _threshold.value = it.getInt("threshold", 0) }
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) _battery.value = level * 100 / scale
                charging.value = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0
            }
        }
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { batteryReceiver.onReceive(context, it) }
    }

    fun setThreshold(percent: Int) {
        _threshold.value = percent.coerceIn(0, 99)
        prefs?.edit()?.putInt("threshold", _threshold.value)?.apply()
    }
}
