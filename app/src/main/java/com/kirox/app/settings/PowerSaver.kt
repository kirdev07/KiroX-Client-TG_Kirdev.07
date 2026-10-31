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

    val active: StateFlow<Boolean> = combine(KiroxTweaks.state, systemSaver) { tweaks, system ->
        when (tweaks.powerSaving) {
            PowerSaving.On -> true
            PowerSaving.Off -> false
            PowerSaving.Auto -> system
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    fun init(context: Context) {
        val power = context.getSystemService(PowerManager::class.java)
        systemSaver.value = power?.isPowerSaveMode == true
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { systemSaver.value = power?.isPowerSaveMode == true }
        }, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    }
}
