package com.kirox.app.voip

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.kirox.app.telegram.CallPhase
import com.kirox.app.telegram.TelegramActiveCall
import com.kirox.app.telegram.isLive

/**
 * Call audio and screen state that must follow the call itself, not the call screen: the screen stops
 * updating in the background and is recreated on rotation, while the ringtone, the audio route and the
 * proximity lock have to end exactly when the call does. Main thread only; [sync] runs on every change.
 */
object CallMedia {
    private var routerCallId = -1
    private var router: CallAudioRouter? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var ringFocus: AudioFocusRequest? = null
    private var ringAudio: AudioManager? = null
    private var proximity: PowerManager.WakeLock? = null

    /** The router of call [callId], kept for the whole call (rotation does not reset the route). */
    fun router(context: Context, callId: Int): CallAudioRouter {
        val current = router
        if (current != null && routerCallId == callId) return current
        current?.stop()
        return CallAudioRouter(context.applicationContext).also { router = it; routerCallId = callId }
    }

    /** Applies [call]'s state; called by [CallController] whenever the call changes, in any app state. */
    fun sync(context: Context, call: TelegramActiveCall?) {
        if (call?.phase != CallPhase.Incoming) stopRinging()
        val talking = call != null && (call.phase is CallPhase.Active || call.phase == CallPhase.Connecting)
        if (talking) router(context, call.id).let { if (!it.isRunning) it.start(call.isVideo) }
        else { router?.stop(); if (call == null || !call.phase.isLive()) { router = null; routerCallId = -1 } }
        if (call == null || !call.phase.isLive()) setProximity(context, false)
    }

    fun startRinging(context: Context) {
        if (ringtone != null) return
        val app = context.applicationContext
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
        val audio = app.getSystemService(AudioManager::class.java)
        ringAudio = audio
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            ringFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).build()
                .also { runCatching { audio?.requestAudioFocus(it) } }
        } else @Suppress("DEPRECATION") runCatching { audio?.requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) }
        ringtone = runCatching {
            RingtoneManager.getRingtone(app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply {
                audioAttributes = attributes
                // Android 7–8.1 cannot loop a Ringtone; the ringtone plays once there (most are long enough).
                if (android.os.Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }.getOrNull()
        // wave5 (fixer-media): Android 7–8.1 — start the ringtone again whenever it ends, until answered/declined.
        if (android.os.Build.VERSION.SDK_INT < 28) ringLoop.postDelayed(ringAgain, 1000)
        vibrator = app.getSystemService(Vibrator::class.java)?.also { v ->
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 900), 0))
                else @Suppress("DEPRECATION") v.vibrate(longArrayOf(0, 600, 900), 0)
            }
        }
    }

    private val ringLoop = android.os.Handler(android.os.Looper.getMainLooper())
    private val ringAgain: Runnable = object : Runnable {
        override fun run() {
            val r = ringtone ?: return
            runCatching { if (!r.isPlaying) r.play() }
            ringLoop.postDelayed(this, 1000)
        }
    }

    fun stopRinging() {
        ringLoop.removeCallbacks(ringAgain)
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        if (android.os.Build.VERSION.SDK_INT >= 26) ringFocus?.let { request -> runCatching { ringAudio?.abandonAudioFocusRequest(request) } }
        else @Suppress("DEPRECATION") runCatching { ringAudio?.abandonAudioFocus(null) }
        ringFocus = null
        ringAudio = null
    }

    /** Screen off at the ear during a voice call through the earpiece. */
    fun setProximity(context: Context, on: Boolean) {
        if (!on) {
            proximity?.let { if (it.isHeld) runCatching { it.release() } }
            proximity = null
            return
        }
        if (proximity != null) return
        val power = context.applicationContext.getSystemService(PowerManager::class.java) ?: return
        if (!power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
        proximity = power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "kirox:call-proximity").apply { acquire(3 * 60 * 60 * 1000L) }
    }
}
