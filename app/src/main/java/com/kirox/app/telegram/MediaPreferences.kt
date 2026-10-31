package com.kirox.app.telegram

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** «Экономия трафика» in calls, as in Telegram. */
enum class CallDataSaving(val title: String) { Never("Никогда"), Mobile("Только в мобильной сети"), Always("Всегда") }

/**
 * «Сохранять в галерею» per chat type and call data saving, kept on this device
 * (Telegram keeps these per app as well).
 */
class MediaPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("media_prefs", Context.MODE_PRIVATE)
    /** Files already copied to the gallery, so one photo is never saved twice. */
    private val saved = context.getSharedPreferences("gallery_saved", Context.MODE_PRIVATE)
    private val _saveToGallery = MutableStateFlow(TelegramChatKind.entries.filter { prefs.getBoolean(galleryKey(it), false) }.toSet())
    private val _callDataSaving = MutableStateFlow(CallDataSaving.entries.firstOrNull { it.name == prefs.getString(CALLS_KEY, null) } ?: CallDataSaving.Never)

    /** Chat types whose incoming photos and videos are copied to the gallery once downloaded. */
    val saveToGallery: StateFlow<Set<TelegramChatKind>> = _saveToGallery.asStateFlow()
    val callDataSaving: StateFlow<CallDataSaving> = _callDataSaving.asStateFlow()

    fun setSaveToGallery(kind: TelegramChatKind, enabled: Boolean) {
        _saveToGallery.value = if (enabled) _saveToGallery.value + kind else _saveToGallery.value - kind
        prefs.edit { putBoolean(galleryKey(kind), enabled) }
    }

    fun setCallDataSaving(value: CallDataSaving) {
        _callDataSaving.value = value
        prefs.edit { putString(CALLS_KEY, value.name) }
    }

    /** Whether a call started now on [network] should use less data. */
    fun callDataSavingOn(network: DeviceNetwork): Boolean = when (_callDataSaving.value) {
        CallDataSaving.Never -> false
        CallDataSaving.Always -> true
        CallDataSaving.Mobile -> network == DeviceNetwork.Mobile || network == DeviceNetwork.MobileRoaming
    }

    /** Marks [path] as saved to the gallery; false when it already was. */
    @Synchronized
    fun markSaved(path: String): Boolean {
        if (saved.contains(path)) return false
        saved.edit { putBoolean(path, true) }
        return true
    }

    private fun galleryKey(kind: TelegramChatKind) = "gallery_${kind.name}"

    private companion object { const val CALLS_KEY = "call_data_saving" }
}
