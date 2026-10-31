package com.kirox.app.settings

import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a double tap on a message does. */
enum class DoubleTapAction(val title: String) { None("Ничего"), Reaction("Реакция ❤️"), Reply("Ответить"), Copy("Копировать текст") }

/** kiroX's own chat and interface options («kiroX Настройки»), kept on this device. */
data class Tweaks(
    /** The forward sheet starts with «Без автора» on. */
    val forwardWithoutAuthor: Boolean = false,
    val confirmSticker: Boolean = false,
    val confirmGif: Boolean = false,
    val confirmVoice: Boolean = false,
    val confirmCall: Boolean = false,
    val doubleTap: DoubleTapAction = DoubleTapAction.None,
    val hideCallsTab: Boolean = false,
    val hideStoriesTab: Boolean = false,
    /** User and chat ids in profiles; a tap copies them. */
    val showIds: Boolean = false,
    /** «Режим призрака»: the master switch; the parts below apply only while it is on. */
    val ghost: Boolean = false,
    val ghostNoRead: Boolean = true,
    val ghostNoTyping: Boolean = true,
    val ghostOffline: Boolean = true,
    val ghostStories: Boolean = true,
    /** Scrolling a chat closes the keyboard. */
    val hideKeyboardOnScroll: Boolean = false,
    /** «Сохранять удалённые сообщения»: copies kept on this phone (MessageArchive). */
    val keepDeleted: Boolean = false,
    /** «Сохранять историю изменений». */
    val keepEdits: Boolean = false
) {
    /** Opened chats are not marked read for the other side. */
    val ghostRead get() = ghost && ghostNoRead
    /** No «печатает…» or «записывает…» is sent. */
    val ghostTyping get() = ghost && ghostNoTyping
    /** Telegram is not told you are online. */
    val ghostOnline get() = ghost && ghostOffline
    /** Watched stories are not marked as viewed. */
    val ghostStory get() = ghost && ghostStories
}

object KiroxTweaks {
    /** SharedPreferences file name; the settings backup reads and writes it. */
    const val PREFS = "kirox_tweaks"
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(Tweaks())
    val state: StateFlow<Tweaks> = _state.asStateFlow()
    val current: Tweaks get() = _state.value

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = Tweaks(
            forwardWithoutAuthor = prefs.getBoolean("forward_without_author", false),
            confirmSticker = prefs.getBoolean("confirm_sticker", false),
            confirmGif = prefs.getBoolean("confirm_gif", false),
            confirmVoice = prefs.getBoolean("confirm_voice", false),
            confirmCall = prefs.getBoolean("confirm_call", false),
            doubleTap = DoubleTapAction.entries.firstOrNull { it.name == prefs.getString("double_tap", null) } ?: DoubleTapAction.None,
            hideCallsTab = prefs.getBoolean("hide_calls_tab", false),
            hideStoriesTab = prefs.getBoolean("hide_stories_tab", false),
            showIds = prefs.getBoolean("show_ids", false),
            ghost = prefs.getBoolean("ghost", false),
            ghostNoRead = prefs.getBoolean("ghost_read", true),
            ghostNoTyping = prefs.getBoolean("ghost_typing", true),
            ghostOffline = prefs.getBoolean("ghost_online", true),
            ghostStories = prefs.getBoolean("ghost_stories", true),
            hideKeyboardOnScroll = prefs.getBoolean("hide_keyboard_on_scroll", false),
            keepDeleted = prefs.getBoolean("keep_deleted", false),
            keepEdits = prefs.getBoolean("keep_edits", false)
        )
    }

    fun update(value: Tweaks) {
        _state.value = value
        prefs.edit {
            putBoolean("forward_without_author", value.forwardWithoutAuthor)
            putBoolean("confirm_sticker", value.confirmSticker)
            putBoolean("confirm_gif", value.confirmGif)
            putBoolean("confirm_voice", value.confirmVoice)
            putBoolean("confirm_call", value.confirmCall)
            putString("double_tap", value.doubleTap.name)
            putBoolean("hide_calls_tab", value.hideCallsTab)
            putBoolean("hide_stories_tab", value.hideStoriesTab)
            putBoolean("show_ids", value.showIds)
            putBoolean("ghost", value.ghost)
            putBoolean("ghost_read", value.ghostNoRead)
            putBoolean("ghost_typing", value.ghostNoTyping)
            putBoolean("ghost_online", value.ghostOffline)
            putBoolean("ghost_stories", value.ghostStories)
            putBoolean("hide_keyboard_on_scroll", value.hideKeyboardOnScroll)
            putBoolean("keep_deleted", value.keepDeleted)
            putBoolean("keep_edits", value.keepEdits)
        }
    }
}
