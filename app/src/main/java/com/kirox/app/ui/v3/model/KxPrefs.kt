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
    // Оформление
    /** 0..11: colour look of the Custom board (11 = «Своя»). */
    const val THEME = "theme"
    /** Anim board: animations on (1/0), scene 0 Авто 1 Основная 2 Хэллоуин 3 Зима 4 Новый год, speed 0 Обычные 1 Меньше 2 Выкл., quality 0 Авто 1 Высокое 2 Лёгкое. */
    const val ANIM_ON = "anim.on"
    const val ANIM_SCENE = "anim.scene"
    const val ANIM_SPEED = "anim.speed"
    const val ANIM_QUAL = "anim.qual"
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

    /** Dock: the «Музыка» tab is shown (1/0, off by default). */
    const val MUSIC_TAB = "dock.music"
    const val WALL_DIM = "wall.dim"

    val defaults: Map<String, Int> = mapOf(
        THEME to 0, OWN_THEMES to 1, CHAT_LOOKS to 2, ANIM_ON to 1, ANIM_SCENE to 1, ANIM_SPEED to 0, ANIM_QUAL to 0,
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
fun KxStore.pref(key: String): Int = prefs[key] ?: KxPrefs.defaults[key] ?: KxPrefs.defaults[key.substringBefore(':')] ?: KxPrefsAccount.defaults[key] ?: 0
fun KxStore.prefOn(key: String): Boolean = pref(key) != 0
fun KxStore.togglePref(key: String) = putPref(key, if (prefOn(key)) 0 else 1)

// wave3-account ---------------------------------------------------------------------------------

/** kiroX-local settings of the account/tools boards (wave3). Ints through [KxStore.prefs], texts through [KxStore.prefTexts]. */
object KxPrefsAccount {
    // Фильтр слов (the app's MessageFilters: on + words; the rest is kiroX-v3 only)
    const val FILTER_ON = "filter.on"
    /** 0 Скрыть, 1 Размыть, 2 Отметить. */
    const val FILTER_ACTION = "filter.action"
    /** Bits: 1 Группы, 2 Каналы, 4 Личные чаты. */
    const val FILTER_SCOPE = "filter.scope"
    /** Bits of the ready-made sets (Реклама, Крипта, Спойлеры). */
    const val FILTER_PRESETS = "filter.presets"
    /** Text: the own words, '\n'-separated. */
    const val FILTER_LIST = "filter.words"

    // Не беспокоить (silences kiroX's own notifications on this phone)
    const val DND_ON = "dnd.on"
    /** Unix seconds when it turns off by itself, 0 = until turned off. */
    const val DND_UNTIL = "dnd.until"
    /** 0 1 час, 1 3 часа, 2 До утра, 3 Пока не выключу. */
    const val DND_DUR = "dnd.dur"
    const val DND_CALLS = "dnd.calls"
    const val DND_REPEAT = "dnd.repeat"
    const val DND_SCHED = "dnd.sched"
    const val DND_FROM = "dnd.from"
    const val DND_TO = "dnd.to"
    /** Text: chat ids that still ring. */
    const val DND_PEOPLE = "dnd.people"

    // Жесты в переписке: option index per gesture
    const val GEST_SWIPE_L = "gest.swipeL"
    const val GEST_DOUBLE = "gest.double"
    const val GEST_HOLD = "gest.hold"
    const val GEST_SWIPE_R = "gest.swipeR"
    // Жесты списка чатов: 0 read, 1 pin, 2 mute, 3 archive, 4 delete, 5 folder
    const val SWIPE_LEFT = "swipe.left"
    const val SWIPE_RIGHT = "swipe.right"

    // Специальные возможности
    /** Text scale in %, 85..150. */
    const val ACCESS_SCALE = "access.scale"
    /** Bits: 1 bold, 2 contrast, 4 outlines, 8 big buttons, 16 animations, 32 captions. */
    const val ACCESS_FLAGS = "access.flags"
    /** 0 Обычные, 1 Дейтеранопия, 2 Тританопия, 3 Монохром. */
    const val ACCESS_PALETTE = "access.palette"

    /** Text: recent settings searches, newest first. */
    const val SEARCH_RECENT = "search.recent"
    /** Text: hidden chat ids. */
    const val HIDDEN_CHATS = "hidden.chats"
    /** Widgets board style index. */
    const val WIDGET_STYLE = "widget.style"
    /** Shortcuts board: launcher badge on. */
    const val SHORTCUT_BADGE = "shortcut.badge"
    /** Backup board: weekly auto backup. */
    const val BACKUP_AUTO = "backup.auto"
    /** The last backup time, unix seconds (0 = never); the Backup board writes it. */
    const val BACKUP_LAST = "backup.last"
    /** Read-only (real store): minutes kiroX was open today / this week (local counting). */
    const val USAGE_TODAY = "usage.today"
    const val USAGE_WEEK = "usage.week"
    /** Text, read-only (real store): minutes kiroX was open on each of the last 7 days, oldest first, comma-separated. */
    const val USAGE_DAYS = "usage.days"
    /** Text, read-only (real store): the day counting started, yyyy-MM-dd (empty = not yet). */
    const val USAGE_SINCE = "usage.since"
    /** Wellbeing: daily limit in minutes (0 = off), break reminder, quiet after 23:00. */
    const val WELL_LIMIT = "well.limit"
    const val WELL_BREAK = "well.break"
    const val WELL_QUIET = "well.quiet"
    /** Archive sheet: «Оставлять в архиве» (TDLib keep_unmuted_chats_archived), chats from folders (keep_chats_from_folders_archived),
     *  «Скрыть счётчики» and the dismissed tip are local. */
    const val ARCHIVE_KEEP = "archive.keepUnmuted"
    const val ARCHIVE_FOLDERS = "archive.folders"
    const val ARCHIVE_HIDE_BADGE = "archive.hideBadge"
    const val ARCHIVE_TIP = "archive.tip"

    val defaults: Map<String, Int> = mapOf(
        FILTER_ON to 1, FILTER_ACTION to 0, FILTER_SCOPE to 3, FILTER_PRESETS to 0,
        DND_ON to 0, DND_UNTIL to 0, DND_DUR to 2, DND_CALLS to 1, DND_REPEAT to 1, DND_SCHED to 1, DND_FROM to 23, DND_TO to 7,
        GEST_SWIPE_L to 0, GEST_DOUBLE to 0, GEST_HOLD to 0, GEST_SWIPE_R to 0, SWIPE_LEFT to 0, SWIPE_RIGHT to 3,
        ACCESS_SCALE to 100, ACCESS_FLAGS to 48, ACCESS_PALETTE to 0,
        WIDGET_STYLE to 0, SHORTCUT_BADGE to 1, BACKUP_AUTO to 0, BACKUP_LAST to 0, USAGE_TODAY to 0, USAGE_WEEK to 0,
        WELL_LIMIT to 0, WELL_BREAK to 0, WELL_QUIET to 0,
        ARCHIVE_KEEP to 1, ARCHIVE_FOLDERS to 0, ARCHIVE_HIDE_BADGE to 1, ARCHIVE_TIP to 1,
    )
    val textDefaults: Map<String, String> = mapOf(FILTER_LIST to "финал\nскидка")
}

/** A text setting split into its lines. */
fun KxStore.prefList(key: String): List<String> = (prefTexts[key] ?: KxPrefsAccount.textDefaults[key].orEmpty()).split('\n').filter { it.isNotBlank() }
fun KxStore.putPrefList(key: String, items: List<String>) = putPrefText(key, items.joinToString("\n"))
