package com.kirox.app.ui.v3.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/*
 * features: kiroX-only tools that live on the phone (nothing goes to Telegram or another server):
 * reply templates, bookmarks of messages, personal notes on chats / people and «прочитать позже»
 * reminders. Read through [KxStore.features]; the demo store keeps them in memory with demo values,
 * the TDLib store saves them per account (data/KxFeatureStorage).
 */

/** A reply template: typed as «/[key]» in the composer; [text] may hold variables (see [KxTemplateVars]). */
@Immutable
data class KxTemplate(val id: Long, val key: String, val text: String, val uses: Int = 0)

/** A bookmarked message: what is needed to list it and to jump back to it. [date] = message unix time, [added] = ms. */
@Immutable
data class KxBookmark(val chatId: Long, val messageId: Long, val chatTitle: String, val sender: String, val text: String, val date: Int, val added: Long)

/** Personal note on a chat or a person (private chat id = the user). Only on this phone. */
@Immutable
data class KxNote(val chatId: Long, val title: String, val text: String, val updated: Long)

/** «Напомнить прочитать позже»: a local notification at [at] (ms) that opens the message. */
@Immutable
data class KxReminder(val id: Int, val chatId: Long, val messageId: Long, val chatTitle: String, val text: String, val at: Long)

/** The local feature data of the current account. Lists are snapshot state (screens recompose). */
interface KxFeatureData {
    val templates: List<KxTemplate>
    /** [t].id 0 = a new template (gets a fresh id); otherwise replaces the one with that id. */
    fun saveTemplate(t: KxTemplate)
    fun deleteTemplate(id: Long)
    /** Moves the template to position [to] of [templates]. */
    fun moveTemplate(id: Long, to: Int)
    fun templateUsed(id: Long)

    /** Newest first. */
    val bookmarks: List<KxBookmark>
    fun isBookmarked(chatId: Long, messageId: Long): Boolean = bookmarks.any { it.chatId == chatId && it.messageId == messageId }
    fun addBookmark(b: KxBookmark)
    fun removeBookmark(chatId: Long, messageId: Long)

    /** Notes by chat id (blank notes are removed). */
    val notes: Map<Long, KxNote>
    fun note(chatId: Long): String = notes[chatId]?.text.orEmpty()
    fun setNote(chatId: Long, title: String, text: String)

    /** Soonest first. */
    val reminders: List<KxReminder>
    /** Schedules the notification; returns the reminder with its id. */
    fun addReminder(r: KxReminder): KxReminder
    fun removeReminder(id: Int)

    /** Simple feature settings (see [KxFeatureKeys]); 0/1 switches and small indices. */
    val settings: Map<String, Int>
    fun putSetting(key: String, value: Int)

    companion object {
        /** Stores without features: nothing is kept. */
        val None: KxFeatureData = KxMemoryFeatures()
    }
}

object KxFeatureKeys {
    /** Translation models download only on Wi-Fi (1, default) or on any network (0). */
    const val TR_WIFI_ONLY = "tr.wifiOnly"
    /** Translation on the phone for free accounts: 1 on (default), 0 = use Telegram's (may need Premium). */
    const val TR_ON_DEVICE = "tr.onDevice"
    /** Speech recognition language: index in [KxFeatureLangs.speech]. */
    const val SPEECH_LANG = "speech.lang"
    /** Speech on the phone for free accounts: 1 on (default), 0 = Telegram's (Premium / free tries). */
    const val SPEECH_ON_DEVICE = "speech.onDevice"
    /** «/» in the composer suggests templates: 1 on (default). */
    const val SLASH = "tpl.slash"

    val defaults = mapOf(TR_WIFI_ONLY to 1, TR_ON_DEVICE to 1, SPEECH_LANG to 0, SPEECH_ON_DEVICE to 1, SLASH to 1)
}

object KxFeatureLangs {
    /** BCP-47 tag → name shown, for speech recognition. */
    val speech = listOf("ru-RU" to "Русский", "en-US" to "English", "uk-UA" to "Українська", "kk-KZ" to "Қазақша")
    /** ISO code → Russian name, for the translation consent text. */
    fun name(code: String): String = when (code) {
        "ru" -> "русского"; "en" -> "английского"; "uk" -> "украинского"; "de" -> "немецкого"; "fr" -> "французского"; "es" -> "испанского"
        "it" -> "итальянского"; "tr" -> "турецкого"; "pl" -> "польского"; "zh" -> "китайского"; "ja" -> "японского"; "ko" -> "корейского"
        "ar" -> "арабского"; "pt" -> "португальского"; "be" -> "белорусского"; "kk" -> "казахского"; "uz" -> "узбекского"; "fa" -> "персидского"
        "hi" -> "хинди"; "he" -> "иврита"; "nl" -> "нидерландского"; "cs" -> "чешского"; "ka" -> "грузинского"
        else -> "«$code»"
    }
    /** ISO code → Russian name in the nominative («Язык перевода: русский»). */
    fun nominative(code: String): String = when (code) {
        "hi" -> "хинди"; "he" -> "иврит"
        else -> name(code).let { g -> if (g.endsWith("ого")) g.dropLast(3) + "ий" else g } // русского → русский
    }
}

/** Template variables, expanded when a template is inserted (unit-tested). Unknown {words} stay as typed. */
object KxTemplateVars {
    data class Vars(val peerName: String = "", val myName: String = "", val chatTitle: String = "", val now: Calendar = Calendar.getInstance())

    /** Shown in the editor as hints. */
    val names = listOf("{имя}" to "имя собеседника", "{полное_имя}" to "имя и фамилия", "{моё_имя}" to "ваше имя", "{чат}" to "название чата",
        "{дата}" to "сегодняшняя дата", "{время}" to "время сейчас", "{завтра}" to "завтрашняя дата")

    private val rx = Regex("""\{([\p{L}_]+)\}""")

    fun expand(text: String, v: Vars): String = rx.replace(text) { m ->
        val first = v.peerName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        when (m.groupValues[1].lowercase().replace('ё', 'е')) {
            "имя", "name" -> first
            "полное_имя", "fullname" -> v.peerName.trim()
            "мое_имя", "я", "myname" -> v.myName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            "чат", "chat" -> v.chatTitle.trim()
            "дата", "date" -> date(v.now)
            "время", "time" -> "%02d:%02d".format(v.now.get(Calendar.HOUR_OF_DAY), v.now.get(Calendar.MINUTE))
            "завтра", "tomorrow" -> date((v.now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) })
            else -> m.value
        }
    }

    private val months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    private fun date(c: Calendar) = "${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)]}"

    /** Shortcut names: letters, digits, «_», 2–24 chars. */
    fun validKey(key: String): Boolean = Regex("^[\\p{L}0-9_]{2,24}$").matches(key)

    /** Templates for what is typed after «/» (prefix first, then «contains»); null when the text is not a «/word». */
    fun matches(typed: String, all: List<KxTemplate>): List<KxTemplate>? {
        if (!typed.startsWith("/") || typed.contains(' ') || typed.contains('\n')) return null
        val q = typed.drop(1).lowercase()
        val starts = all.filter { it.key.lowercase().startsWith(q) }
        val inside = all.filter { q.isNotEmpty() && it !in starts && it.key.lowercase().contains(q) }
        return starts + inside
    }
}

/** «Отправить, когда будет в сети» rules (unit-tested). */
object KxSendWhen {
    /** Telegram sends «when online» only while the peer's last seen is visible to you (not «недавно», «давно»…). */
    fun lastSeenVisible(status: String): Boolean {
        val s = status.trim()
        if (s == "в сети") return true
        // Neutral wording (critique-A S): «в сети 5 минут назад» = visible; «в сети недавно / на этой неделе…», «давно не в сети» = hidden.
        if (!s.startsWith("в сети") && !s.startsWith("был")) return false
        // old wording («был(а) недавно») maps to the same hidden set
        return (if (s.startsWith("был")) "в сети " + s.substringAfter(' ').trim() else s).let { if (it == "в сети давно") "давно не в сети" else it } !in Hidden
    }
    private val Hidden = setOf("в сети недавно", "в сети на этой неделе", "в сети в этом месяце", "давно не в сети")

    /** Unix time for the scheduling sheet: -1 = when online, 0 = now, else the time. */
    const val WHEN_ONLINE = -1
}

/** In-memory implementation (demo, tests, base of the saved one). [changed] runs after every change. */
open class KxMemoryFeatures(
    templates: List<KxTemplate> = emptyList(), bookmarks: List<KxBookmark> = emptyList(),
    notes: Map<Long, KxNote> = emptyMap(), reminders: List<KxReminder> = emptyList(), settings: Map<String, Int> = emptyMap(),
) : KxFeatureData {
    override var templates by mutableStateOf(templates)
        protected set
    override var bookmarks by mutableStateOf(bookmarks.sortedByDescending { it.added })
        protected set
    override var notes by mutableStateOf(notes)
        protected set
    override var reminders by mutableStateOf(reminders.sortedBy { it.at })
        protected set
    private var own by mutableStateOf(settings)
    override val settings: Map<String, Int> get() = KxFeatureKeys.defaults + own

    protected open fun changed(what: Char) {}
    protected open fun scheduled(r: KxReminder) {}
    protected open fun cancelled(id: Int) {}

    override fun saveTemplate(t: KxTemplate) {
        val clean = t.copy(key = t.key.trim().removePrefix("/").lowercase(), text = t.text.trim())
        templates = if (t.id == 0L) templates + clean.copy(id = (templates.maxOfOrNull { it.id } ?: 0L) + 1)
        else templates.map { if (it.id == t.id) clean.copy(uses = it.uses) else it }
        changed('t')
    }
    override fun deleteTemplate(id: Long) { templates = templates.filterNot { it.id == id }; changed('t') }
    override fun moveTemplate(id: Long, to: Int) {
        val i = templates.indexOfFirst { it.id == id }; if (i < 0) return
        val list = templates.toMutableList(); val t = list.removeAt(i); list.add(to.coerceIn(0, list.size), t)
        templates = list; changed('t')
    }
    override fun templateUsed(id: Long) { templates = templates.map { if (it.id == id) it.copy(uses = it.uses + 1) else it }; changed('t') }

    override fun addBookmark(b: KxBookmark) {
        bookmarks = (listOf(b) + bookmarks.filterNot { it.chatId == b.chatId && it.messageId == b.messageId }).take(MAX_BOOKMARKS)
        changed('b')
    }
    override fun removeBookmark(chatId: Long, messageId: Long) { bookmarks = bookmarks.filterNot { it.chatId == chatId && it.messageId == messageId }; changed('b') }

    override fun setNote(chatId: Long, title: String, text: String) {
        notes = if (text.isBlank()) notes - chatId else notes + (chatId to KxNote(chatId, title, text.trim(), System.currentTimeMillis()))
        changed('n')
    }

    override fun addReminder(r: KxReminder): KxReminder {
        val id = if (r.id != 0) r.id else ((reminders.maxOfOrNull { it.id } ?: 0) + 1).let { if (it < FIRST_ID) FIRST_ID + it else it }
        val made = r.copy(id = id)
        reminders = (reminders.filterNot { it.id == id } + made).sortedBy { it.at }
        changed('r'); scheduled(made)
        return made
    }
    override fun removeReminder(id: Int) { reminders = reminders.filterNot { it.id == id }; changed('r'); cancelled(id) }
    /** The notification fired: the reminder leaves the list. */
    fun fired(id: Int) { if (reminders.any { it.id == id }) { reminders = reminders.filterNot { it.id == id }; changed('r') } }

    override fun putSetting(key: String, value: Int) { own = own + (key to value); changed('s') }

    companion object {
        const val MAX_BOOKMARKS = 2000
        /** Reminder ids start here so their notification ids never meet the chat notifications. */
        const val FIRST_ID = 0x5E0000

        /** Demo values for the preview app. */
        fun demo(): KxMemoryFeatures {
            val now = System.currentTimeMillis()
            return KxMemoryFeatures(
                templates = listOf(
                    KxTemplate(1, "привет", "Здравствуйте, {имя}! Спасибо, что написали 🙂 Отвечу в течение часа.", 24),
                    KxTemplate(2, "адрес", "Мы на ул. Примерная, 12, вход со двора. Есть парковка.", 9),
                    KxTemplate(3, "позже", "{имя}, сейчас не могу говорить — перезвоню после {время}.", 5),
                ),
                bookmarks = listOf(
                    KxBookmark(11, 101, "Анна", "Анна", "Вот ссылка на билеты: поезд в 8:40, вагон 5", (now / 1000 - 3600).toInt(), now - 3_600_000),
                    KxBookmark(12, 202, "Команда kiroX", "Максим", "Релиз в пятницу, чек-лист в закреплённом", (now / 1000 - 86_400).toInt(), now - 86_400_000),
                ),
                notes = mapOf(11L to KxNote(11, "Анна", "День рождения 12 марта. Любит кофе без сахара.", now - 7_200_000)),
                reminders = listOf(KxReminder(FIRST_ID + 1, 12, 202, "Команда kiroX", "Релиз в пятницу, чек-лист в закреплённом", now + 3 * 3_600_000)),
            )
        }
    }
}
