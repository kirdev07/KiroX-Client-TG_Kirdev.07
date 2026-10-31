package com.kirox.app.telegram

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/** «Хранить медиа»: cached files not opened for this long are deleted; 0 keeps them. */
enum class KeepMedia(val title: String, val seconds: Int) {
    ThreeDays("3 дня", 3 * 86_400),
    Week("1 неделя", 7 * 86_400),
    Month("1 месяц", 30 * 86_400),
    Forever("Всегда", 0)
}

/** «Максимальный размер кэша»: the oldest files go first once the cache grows past it; 0 is unlimited. */
enum class CacheLimit(val title: String, val bytes: Long) {
    Gb5("5 ГБ", 5L shl 30),
    Gb16("16 ГБ", 16L shl 30),
    Gb32("32 ГБ", 32L shl 30),
    Unlimited("Без ограничений", 0)
}

/**
 * Cache rules of «Данные и память», kept on this device. TDLib does not clean the cache by itself,
 * so the rules are applied with OptimizeStorage when the account opens and whenever they change.
 */
class StoragePolicy(context: Context, private val client: TelegramClient) {
    private val prefs = context.getSharedPreferences("storage_policy", Context.MODE_PRIVATE)
    private val _keepMedia = MutableStateFlow(KeepMedia.entries.firstOrNull { it.name == prefs.getString(KEEP_KEY, null) } ?: KeepMedia.Forever)
    private val _cacheLimit = MutableStateFlow(CacheLimit.entries.firstOrNull { it.name == prefs.getString(LIMIT_KEY, null) } ?: CacheLimit.Unlimited)
    val keepMedia: StateFlow<KeepMedia> = _keepMedia.asStateFlow()
    val cacheLimit: StateFlow<CacheLimit> = _cacheLimit.asStateFlow()

    init {
        client.addUpdateHandler { update ->
            if (update is TdApi.UpdateAuthorizationState && update.authorizationState is TdApi.AuthorizationStateReady) apply()
        }
    }

    fun setKeepMedia(value: KeepMedia) {
        _keepMedia.value = value
        prefs.edit().putString(KEEP_KEY, value.name).apply()
        apply()
    }

    fun setCacheLimit(value: CacheLimit) {
        _cacheLimit.value = value
        prefs.edit().putString(LIMIT_KEY, value.name).apply()
        apply()
    }

    /** Deletes what the rules allow; nothing is sent when both are off (TDLib's own defaults would delete more). */
    private fun apply() {
        val keep = _keepMedia.value
        val limit = _cacheLimit.value
        if (keep == KeepMedia.Forever && limit == CacheLimit.Unlimited) return
        client.send(
            TdApi.OptimizeStorage(
                if (limit == CacheLimit.Unlimited) NO_SIZE_LIMIT else limit.bytes,
                if (keep == KeepMedia.Forever) Int.MAX_VALUE else keep.seconds,
                Int.MAX_VALUE, -1, null, null, null, false, 0
            )
        )
    }

    private companion object {
        const val KEEP_KEY = "keep_media"
        const val LIMIT_KEY = "cache_limit"
        const val NO_SIZE_LIMIT = 1L shl 50
    }
}
