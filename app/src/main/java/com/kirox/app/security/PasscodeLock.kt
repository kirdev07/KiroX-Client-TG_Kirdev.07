package com.kirox.app.security

import android.content.Context
import androidx.core.content.edit
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
    val hideContent: Boolean = true,
    /** The code is a graphic key (dots joined on a 3×3 grid) instead of 4 digits. */
    val pattern: Boolean = false
)

/**
 * Local passcode of kiroX (not the Telegram cloud password): a 4-digit code that locks the app on
 * start and after it stayed in the background for [AutoLock]. Only a salted PBKDF2 hash is stored,
 * on this device. Five wrong codes in a row block entry (30 s, growing with each block); the counter is kept across restarts.
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
            hideContent = prefs.getBoolean(HIDE_KEY, true),
            pattern = prefs.getBoolean(PATTERN_KEY, false)
        )
        // A fresh process always starts locked.
        _locked.value = _settings.value.enabled
        // Wrong-code counter and block survive a process kill (no brute force by swiping the app away).
        failures = prefs.getInt(FAILURES_KEY, 0)
        // wave5: the block is kept on the boot clock (elapsedRealtime), so moving the wall clock forward doesn't end it.
        // Same boot → the saved end as is; after a reboot (boot clock restarted) a block not seen ending runs again in full.
        if (prefs.contains(BLOCKED_ELAPSED_KEY)) {
            val end = prefs.getLong(BLOCKED_ELAPSED_KEY, 0L)
            val now = SystemClock.elapsedRealtime()
            val full = blockMs(prefs.getInt(BLOCKS_KEY, 1))
            when {
                prefs.getInt(BOOT_KEY, -2) == bootCount(context) -> if (end > now) blockedUntil = end.coerceAtMost(now + full) else prefs.edit { remove(BLOCKED_ELAPSED_KEY) }
                else -> { blockedUntil = now + full; prefs.edit { putLong(BLOCKED_ELAPSED_KEY, blockedUntil); putInt(BOOT_KEY, bootCount(context)) } }
            }
        }
        boot = bootCount(context)
    }

    private var boot = -1
    /** Boot number (Android 7+ global setting); -1 when unknown, then every start counts as a new boot. */
    private fun bootCount(context: Context): Int =
        runCatching { android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT) }.getOrDefault(-1)

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
        if (blockedUntil > 0 && prefs.contains(BLOCKED_ELAPSED_KEY)) prefs.edit { remove(BLOCKED_ELAPSED_KEY) } // the block ran out
        if (matches(code)) { resetFailures(); _locked.value = false; return true }
        failures++
        if (failures >= MAX_FAILURES) {
            // Each new block in a row is longer: 30 s, 1 min, 5 min, 15 min, then 1 h.
            val blocks = prefs.getInt(BLOCKS_KEY, 0) + 1
            val ms = blockMs(blocks)
            failures = 0
            blockedUntil = SystemClock.elapsedRealtime() + ms
            prefs.edit { putInt(BLOCKS_KEY, blocks); putLong(BLOCKED_ELAPSED_KEY, blockedUntil); putInt(BOOT_KEY, boot); remove(BLOCKED_WALL_KEY) }
        }
        prefs.edit { putInt(FAILURES_KEY, failures) }
        return false
    }

    /** The fingerprint was accepted by the system. */
    fun unlockByBiometric() { resetFailures(); _locked.value = false }

    private fun resetFailures() {
        failures = 0
        if (prefs.contains(FAILURES_KEY) || prefs.contains(BLOCKS_KEY)) prefs.edit { remove(FAILURES_KEY); remove(BLOCKS_KEY); remove(BLOCKED_WALL_KEY); remove(BLOCKED_ELAPSED_KEY); remove(BOOT_KEY) }
    }

    private fun blockMs(blocks: Int): Long = BLOCKS_MS[(blocks - 1).coerceIn(0, BLOCKS_MS.lastIndex)]

    fun matches(code: String): Boolean {
        val salt = prefs.getString(SALT_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val stored = prefs.getString(HASH_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(hash(code, salt, prefs.getString(ALG_KEY, ALG_SHA256) ?: ALG_SHA256), stored)
    }

    /** Turns the code on, or changes it; [pattern] — it is a graphic key. The app stays unlocked. */
    fun setCode(code: String, pattern: Boolean = _settings.value.pattern) {
        prefs.edit { putBoolean(PATTERN_KEY, pattern) }
        _settings.value = _settings.value.copy(pattern = pattern)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val alg = currentAlg()
        prefs.edit {
            putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(HASH_KEY, Base64.encodeToString(hash(code, salt, alg), Base64.NO_WRAP))
            putString(ALG_KEY, alg)
        }
        _settings.value = _settings.value.copy(enabled = true)
    }

    fun disable() {
        prefs.edit {
            remove(SALT_KEY)
            remove(HASH_KEY)
            remove(ALG_KEY)
        }
        _settings.value = _settings.value.copy(enabled = false)
        _locked.value = false
    }

    fun lockNow() { if (_settings.value.enabled) _locked.value = true }

    fun setAutoLock(value: AutoLock) { prefs.edit { putString(AUTO_KEY, value.name) }; _settings.value = _settings.value.copy(autoLock = value) }
    fun setBiometric(value: Boolean) { prefs.edit { putBoolean(BIOMETRIC_KEY, value) }; _settings.value = _settings.value.copy(biometric = value) }
    fun setHideContent(value: Boolean) { prefs.edit { putBoolean(HIDE_KEY, value) }; _settings.value = _settings.value.copy(hideContent = value) }

    /** PBKDF2WithHmacSHA256 exists from Android 8; Android 7 uses PBKDF2WithHmacSHA1. The algorithm is stored with the hash
     *  (no key = SHA-256, as written before this fallback). */
    private fun currentAlg() = if (android.os.Build.VERSION.SDK_INT >= 26) ALG_SHA256 else ALG_SHA1

    private fun hash(code: String, salt: ByteArray, alg: String): ByteArray =
        SecretKeyFactory.getInstance(alg).generateSecret(PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)).encoded

    const val LENGTH = 4
    private const val MAX_FAILURES = 5
    private val BLOCKS_MS = longArrayOf(30_000L, 60_000L, 5 * 60_000L, 15 * 60_000L, 60 * 60_000L)
    private const val FAILURES_KEY = "failures"
    private const val BLOCKS_KEY = "blocks"
    private const val BLOCKED_WALL_KEY = "blocked_until_wall"
    private const val BLOCKED_ELAPSED_KEY = "blocked_until_elapsed"
    private const val BOOT_KEY = "blocked_boot"
    private const val ITERATIONS = 20_000
    private const val SALT_KEY = "salt"
    private const val ALG_KEY = "alg"
    private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"
    private const val HASH_KEY = "hash"
    private const val AUTO_KEY = "auto_lock"
    private const val BIOMETRIC_KEY = "biometric"
    private const val HIDE_KEY = "hide_content"
    private const val PATTERN_KEY = "pattern"
}
