package com.kirox.app.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * «Оформление чата»: a wallpaper and a color theme for one chat instead of the ones from
 * Кастомизация. Kept on this phone only; the other side never sees it.
 */
object ChatWallpapers {
    private fun prefs(context: Context) = context.getSharedPreferences("chat_wallpapers", Context.MODE_PRIVATE)
    private const val THEME = "t_"

    private val _revision = MutableStateFlow(0)
    /** Grows on every change, so open chats repaint at once. */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /** Own wallpaper of [chatId]; null — as in the settings. */
    fun get(context: Context, chatId: Long): ChatWallpaper? =
        prefs(context).getString(chatId.toString(), null)?.let { name -> ChatWallpaper.entries.firstOrNull { it.name == name } }

    fun set(context: Context, chatId: Long, value: ChatWallpaper?) {
        prefs(context).edit { if (value == null) remove(chatId.toString()) else putString(chatId.toString(), value.name) }
        _revision.value++
    }

    /** Id of a ready-made or own theme for [chatId] (see [CustomThemes.find]); null — the app's theme. */
    fun theme(context: Context, chatId: Long): String? = prefs(context).getString(THEME + chatId, null)

    fun setTheme(context: Context, chatId: Long, themeId: String?) {
        prefs(context).edit { if (themeId == null) remove(THEME + chatId) else putString(THEME + chatId, themeId) }
        _revision.value++
    }

    /** How many chats have their own look. */
    fun count(context: Context): Int =
        prefs(context).all.keys.map { it.removePrefix(THEME) }.toSet().size

    fun clearAll(context: Context) {
        prefs(context).edit { clear() }
        _revision.value++
    }
}
