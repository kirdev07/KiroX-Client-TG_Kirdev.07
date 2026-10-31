package com.kirox.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** «Автоблокировка»: how long kiroX may stay in the background before it asks for the code again. */
enum class AutoLock(val title: String, val seconds: Long) {
    Immediately("Сразу", 0),
    Minute("Через 1 минуту", 60),
    FiveMinutes("Через 5 минут", 5 * 60),
    Hour("Через 1 час", 60 * 60),
    FiveHours("Через 5 часов", 5 * 60 * 60)
}

data class PasscodeSettings(
    val enabled: Boolean = false,
    val autoLock: AutoLock = AutoLock.Minute,
    /** Unlock with the fingerprint (or face) the phone already knows; the code always works too. */
    val biometric: Boolean = true,
    /** Blank in the recent apps list and no screenshots while the code is on. */
    val hideContent: Boolean = true
)

/**
 * Local passcode of kiroX (not the Telegram cloud password): a 4-digit code that locks the app on
 * start and after it stayed in the background for [AutoLock]. Only a salted PBKDF2 hash is stored,
 * on this device. Five wrong codes in a row block entry for 30 seconds.
 */
object PasscodeLock {
    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(PasscodeSettings())
    private val _locked = MutableStateFlow(false)
    val settings: StateFlow<PasscodeSettings> = _settings.asStateFlow()
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundSince = 0L
    private var failures = 0
    /** Elapsed-realtime moment until which no code is accepted. */
    @Volatile var blockedUntil = 0L
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("passcode", Context.MODE_PRIVATE)
        _settings.value = PasscodeSettings(
            enabled = prefs.contains(HASH_KEY),
            autoLock = AutoLock.entries.firstOrNull { it.name == prefs.getString(AUTO_KEY, null) } ?: AutoLock.Minute,
            biometric = prefs.getBoolean(BIOMETRIC_KEY, true),
            hideContent = prefs.getBoolean(HIDE_KEY, true)
        )
        // A fresh process always starts locked.
        _locked.value = _settings.value.enabled
    }

    /** From AppVisibility: going to the background starts the timer, coming back checks it. */
    fun onVisibilityChanged(visible: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!visible) { backgroundSince = now; return }
        val since = backgroundSince
        backgroundSince = 0L
        if (_settings.value.enabled && since > 0 && now - since >= _settings.value.autoLock.seconds * 1000) _locked.value = true
    }

    /** Checks [code] on the lock screen; true unlocks. */
    fun unlock(code: String): Boolean {
        if (SystemClock.elapsedRealtime() < blockedUntil) return false
        if (matches(code)) { failures = 0; _locked.value = false; return true }
        failures++
        if (failures >= MAX_FAILURES) { failures = 0; blockedUntil = SystemClock.elapsedRealtime() + BLOCK_MS }
        return false
    }

    /** The fingerprint was accepted by the system. */
    fun unlockByBiometric() { failures = 0; _locked.value = false }

    fun matches(code: String): Boolean {
        val salt = prefs.getString(SALT_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val stored = prefs.getString(HASH_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(hash(code, salt), stored)
    }

    /** Turns the code on, or changes it; the app stays unlocked. */
    fun setCode(code: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH_KEY, Base64.encodeToString(hash(code, salt), Base64.NO_WRAP))
            .apply()
        _settings.value = _settings.value.copy(enabled = true)
    }

    fun disable() {
        prefs.edit().remove(SALT_KEY).remove(HASH_KEY).apply()
        _settings.value = _settings.value.copy(enabled = false)
        _locked.value = false
    }

    fun lockNow() { if (_settings.value.enabled) _locked.value = true }

    fun setAutoLock(value: AutoLock) { prefs.edit().putString(AUTO_KEY, value.name).apply(); _settings.value = _settings.value.copy(autoLock = value) }
    fun setBiometric(value: Boolean) { prefs.edit().putBoolean(BIOMETRIC_KEY, value).apply(); _settings.value = _settings.value.copy(biometric = value) }
    fun setHideContent(value: Boolean) { prefs.edit().putBoolean(HIDE_KEY, value).apply(); _settings.value = _settings.value.copy(hideContent = value) }

    private fun hash(code: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)).encoded

    const val LENGTH = 4
    private const val MAX_FAILURES = 5
    private const val BLOCK_MS = 30_000L
    private const val ITERATIONS = 20_000
    private const val SALT_KEY = "salt"
    private const val HASH_KEY = "hash"
    private const val AUTO_KEY = "auto_lock"
    private const val BIOMETRIC_KEY = "biometric"
    private const val HIDE_KEY = "hide_content"
}
