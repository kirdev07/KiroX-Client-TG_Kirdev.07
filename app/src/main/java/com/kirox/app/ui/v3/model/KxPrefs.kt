package com.kirox.app.ui.v3.model

/**
 * kiroX-local settings (no TDLib): small ints by name, read through [KxStore.prefs] and written with
 * [KxStore.putPref]. Values are the boards' own indices (option order as drawn), so screens use them
 * directly. Switches are 0/1. [defaults] are the boards' starting values (the demo store keeps them).
 *
 * The TDLib store maps keys onto the existing app settings where one exists (KiroXSettingsRepository,
 * KiroxTweaks, PowerSaver, I18n, MediaPreferences…) and keeps the rest in its own preferences.
 * Read-only keys (counts, battery) ignore [KxStore.putPref]. Wallpaper keys may name one chat: "wall.preset:<chatId>".
 */
object KxPrefs {
    // Кастомизация
    /** 0..11: colour look of the Custom board (11 = «Своя»). */
    const val THEME = "theme"
    /** Read-only: own colour themes. */
    const val OWN_THEMES = "ownThemes"
    /** Read-only: chats with their own look. */
    const val CHAT_LOOKS = "chatLooks"

    // Вид чатов
    /** 0 Острые, 1 Мягкие, 2 Круглые. */
    const val BUBBLES = "chat.bubbles"
    /** 0 Тема, 1 Узор, 2 Градиент, 3 Ночь, 4 Нет. */
    const val CHAT_WALLPAPER = "chat.wallpaper"
    /** 0..3: 13 / 15 / 17 / 19 sp. */
    const val TEXT_SIZE = "chat.textSize"
    /** 0 Компактно, 1 Обычно, 2 Свободно. */
    const val DENSITY = "chat.density"

    // Язык
    /** 0 Русский, 1 English. */
    const val LANGUAGE = "language"
    /** «Перевод сообщений» in the message menu. */
    const val TRANSLATE = "translate"
    /** Translate board target: 0 Русский, 1 Английский, 2 Казахский. */
    const val TRANSLATE_TO = "translate.to"

    // Энергосбережение / Функции
    /** Battery threshold in %: 0 = never, 100 = always. */
    const val POWER_THRESHOLD = "power.threshold"
    /** Bit i set = feature i of the Power board is turned off while saving (6 features). */
    const val POWER_OFF = "power.off"
    /** Read-only: power saving is on right now (real store only; the demo board computes it). */
    const val POWER_ACTIVE = "power.active"
    /** Read-only: battery level, %. */
    const val BATTERY = "battery"
    /** Features board: 0 Как в системе, 1 Всегда, 2 Никогда. */
    const val POWER_MODE = "power.mode"
    /** Features board: 0 Ничего, 1 Реакция, 2 Ответить, 3 Копировать. */
    const val DOUBLE_TAP = "doubleTap"
    /** Read-only: words in the message filter. */
    const val FILTER_WORDS = "filterWords"

    // Звонки
    const val NOISE = "call.noise"
    const val ECHO = "call.echo"
    /** 0 Волна kiroX, 1 Лёгкий звон, 2 Классика, 3 Без звука. */
    const val RINGTONE = "call.ring"
    /** 0 Высокое, 1 Сбалансированно, 2 Экономно. */
    const val CALL_DATA = "call.data"

    // Ночной режим
    /** 0 Всегда, 1 По расписанию, 2 Закат–рассвет, 3 Как в системе. */
    const val NIGHT_MODE = "night.mode"
    const val NIGHT_FROM = "night.from"
    const val NIGHT_TO = "night.to"
    /** Night theme of the board, 0..n. */
    const val NIGHT_THEME = "night.theme"

    // Обои
    /** Wallpaper preset index of the Wallpaper board. */
    const val WALL = "wall.preset"
    const val WALL_BLUR = "wall.blur"
    const val WALL_MOTION = "wall.motion"
    const val WALL_DIM = "wall.dim"

    val defaults: Map<String, Int> = mapOf(
        THEME to 0, OWN_THEMES to 1, CHAT_LOOKS to 2,
        BUBBLES to 1, CHAT_WALLPAPER to 0, TEXT_SIZE to 1, DENSITY to 1,
        LANGUAGE to 0, TRANSLATE to 1, TRANSLATE_TO to 0,
        POWER_THRESHOLD to 15, POWER_OFF to 0b010111, BATTERY to 62, POWER_MODE to 0, DOUBLE_TAP to 0, FILTER_WORDS to 3,
        NOISE to 1, ECHO to 1, RINGTONE to 0, CALL_DATA to 1,
        NIGHT_MODE to 1, NIGHT_FROM to 22, NIGHT_TO to 7, NIGHT_THEME to 0,
        WALL to 0, WALL_BLUR to 0, WALL_MOTION to 1, WALL_DIM to 1,
    )
}

/** An invite link before joining (store.inviteLinkInfo). [member]: already inside — [chatId] opens it. */
data class KxInviteInfo(val chatId: Long, val title: String, val channel: Boolean, val public: Boolean, val members: Int, val knownNames: List<String> = emptyList(),
                        val description: String = "", val byRequest: Boolean = false, val photo: String? = null, val member: Boolean = false)

/** A first message from someone not in the contacts (Telegram's «report / add / block» bar). */
data class KxRequest(val chatId: Long, val userId: Long, val name: String, val meta: String, val text: String, val mutual: Int = 0, val risky: Boolean = false,
                     /** «Фото скрыто» etc. when the message carries media, null otherwise. */
                     val media: String? = null, val avatar: String? = null, val colorIndex: Int = -1)

/** Someone who viewed an own story; [userId] is also the private chat id (reply). */
data class KxStoryViewer(val userId: Long, val name: String, val time: String, val reaction: String = "", val avatar: String? = null)

/** Value of a [KxPrefs] key (the default when the store has none). */
fun KxStore.pref(key: String): Int = prefs[key] ?: KxPrefs.defaults[key] ?: KxPrefs.defaults[key.substringBefore(':')] ?: 0
fun KxStore.prefOn(key: String): Boolean = pref(key) != 0
fun KxStore.togglePref(key: String) = putPref(key, if (prefOn(key)) 0 else 1)
