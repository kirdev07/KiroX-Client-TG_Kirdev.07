package com.kirox.app.settings

import android.content.Context

/** «Обои чата»: a wallpaper for one chat instead of the one from Appearance; kept on this phone. */
object ChatWallpapers {
    private fun prefs(context: Context) = context.getSharedPreferences("chat_wallpapers", Context.MODE_PRIVATE)

    /** Own wallpaper of [chatId]; null — as in the settings. */
    fun get(context: Context, chatId: Long): ChatWallpaper? =
        prefs(context).getString(chatId.toString(), null)?.let { name -> ChatWallpaper.entries.firstOrNull { it.name == name } }

    fun set(context: Context, chatId: Long, value: ChatWallpaper?) {
        prefs(context).edit().apply { if (value == null) remove(chatId.toString()) else putString(chatId.toString(), value.name) }.apply()
    }
}
