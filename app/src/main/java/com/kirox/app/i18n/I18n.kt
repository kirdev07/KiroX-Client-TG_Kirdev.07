package com.kirox.app.i18n

import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** A set of own names («набор названий»): several can be installed, one is used at a time. */
data class NamePack(val id: String, val title: String, val names: Map<String, String>)

/** Interface languages. Russian is the source text, the others come from assets/i18n/<code>.json. */
enum class AppLanguage(val code: String, val title: String) { Russian("ru", "Русский"), English("en", "English") }

/**
 * Text engine of kiroX. Every text the app shows passes through [t] (the app's own Text does it):
 * the Russian source text is the key, so nothing in the screens needs ids.
 *  - a translation for the chosen [language] replaces it;
 *  - the user's own name for it ([overrides], «Свои названия») wins over both, in any language.
 *    Names live in [packs]; the active pack is the one used, packs can be exported and imported.
 * Texts with values inside («Удалить чат с «{}»?») match by template: {} stands for any value and
 * the values move into the translation in the same order. User content without Cyrillic is never
 * touched; a message that happens to equal a UI text could be, which is accepted.
 */
object I18n {
    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    /** Compose state: changing it redraws every text. */
    var language by mutableStateOf(AppLanguage.Russian)
        private set
    var packs by mutableStateOf<List<NamePack>>(emptyList())
        private set
    /** Pack in use; null — normal names. */
    var activePackId by mutableStateOf<String?>(null)
        private set
    /** Names of the active pack. */
    val overrides: Map<String, String> get() = packs.firstOrNull { it.id == activePackId }?.names.orEmpty()

    private val dictionaries = HashMap<AppLanguage, Map<String, String>>()
    private var templates: List<Pair<Regex, String>> = emptyList()
    /** Result per shown text; cleared when the language or a name changes. */
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > CACHE_SIZE
    }
    private val cyrillic = Regex("[А-Яа-яЁё]")

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("i18n", Context.MODE_PRIVATE)
        language = AppLanguage.entries.firstOrNull { it.code == prefs.getString(LANGUAGE_KEY, null) } ?: AppLanguage.Russian
        packs = prefs.getString(PACKS_KEY, null)?.let { json ->
            runCatching {
                val array = org.json.JSONArray(json)
                (0 until array.length()).map { i -> array.getJSONObject(i).let { NamePack(it.getString("id"), it.getString("title"), it.getJSONObject("names").toMap()) } }
            }.getOrNull()
        }.orEmpty()
        activePackId = prefs.getString(ACTIVE_KEY, null)?.takeIf { id -> packs.any { it.id == id } }
        // Names saved before packs existed become the first pack.
        prefs.getString(OVERRIDES_KEY, null)?.let { json ->
            val names = runCatching { JSONObject(json).toMap() }.getOrNull().orEmpty()
            if (names.isNotEmpty()) { val pack = NamePack(newId(), "Мои названия", names); packs = packs + pack; activePackId = pack.id }
            prefs.edit { remove(OVERRIDES_KEY) }
            save()
        }
        rebuildTemplates()
    }

    /** All texts that can be renamed, in the order of the source; from the English dictionary. */
    val keys: List<String> get() = dictionary(AppLanguage.English).keys.toList()

    fun chooseLanguage(value: AppLanguage) {
        language = value
        prefs.edit { putString(LANGUAGE_KEY, value.code) }
        synchronized(cache) { cache.clear() }
    }

    /** Own name for [key] in the active pack (a first pack is made when none is chosen); null or blank brings the normal text back. */
    fun setOverride(key: String, value: String?) {
        val active = packs.firstOrNull { it.id == activePackId } ?: createPack("Мои названия")
        val names = if (value.isNullOrBlank()) active.names - key else active.names + (key to value)
        packs = packs.map { if (it.id == active.id) it.copy(names = names) else it }
        save()
    }

    /** Empties the active pack. */
    fun clearOverrides() {
        packs = packs.map { if (it.id == activePackId) it.copy(names = emptyMap()) else it }
        save()
    }

    /** A new empty pack, chosen at once. */
    fun createPack(title: String): NamePack {
        val pack = NamePack(newId(), uniqueTitle(title.ifBlank { "Мои названия" }), emptyMap())
        packs = packs + pack
        activePackId = pack.id
        save()
        return pack
    }

    /** Uses [id]; null goes back to normal names. */
    fun activatePack(id: String?) { activePackId = id?.takeIf { pid -> packs.any { it.id == pid } }; save() }

    fun renamePack(id: String, title: String) {
        if (title.isBlank()) return
        packs = packs.map { if (it.id == id) it.copy(title = title.trim().take(TITLE_MAX)) else it }
        save()
    }

    fun deletePack(id: String) {
        packs = packs.filterNot { it.id == id }
        if (activePackId == id) activePackId = null
        save()
    }

    /** The pack as text to send: open format, readable and safe to paste into a chat. */
    fun exportPack(id: String): String? {
        val pack = packs.firstOrNull { it.id == id } ?: return null
        return JSONObject().put("kirox_names", 1).put("title", pack.title).put("names", JSONObject(pack.names)).toString(2)
    }

    /**
     * Installs a pack from [text] (a file or a pasted message). Only names of known kiroX texts are
     * taken, each at most a few dozen characters, so a foreign pack can't put anything else in the app.
     * Returns the installed pack (not chosen yet), or null when the text is not a kiroX pack.
     */
    fun importPack(text: String): NamePack? {
        val json = runCatching { JSONObject(text.substring(text.indexOf('{').coerceAtLeast(0), (text.lastIndexOf('}') + 1).coerceAtLeast(0))) }.getOrNull() ?: return null
        if (!json.has("kirox_names") || !json.has("names")) return null
        val known = dictionary(AppLanguage.English).keys
        val names = runCatching { json.getJSONObject("names").toMap() }.getOrNull().orEmpty()
            .filter { (key, value) -> key in known && value.isNotBlank() }
            .mapValues { it.value.replace('\n', ' ').trim().take(NAME_MAX) }
            .entries.take(MAX_NAMES).associate { it.key to it.value }
        if (names.isEmpty()) return null
        val pack = NamePack(newId(), uniqueTitle(json.optString("title").trim().take(TITLE_MAX).ifBlank { "Набор названий" }), names)
        packs = packs + pack
        save()
        return pack
    }

    private fun uniqueTitle(title: String): String {
        if (packs.none { it.title == title }) return title
        var n = 2
        while (packs.any { it.title == "$title ($n)" }) n++
        return "$title ($n)"
    }

    private fun newId() = java.util.UUID.randomUUID().toString()

    private fun JSONObject.toMap(): Map<String, String> = keys().asSequence().associateWith { getString(it) }

    /** What [key] shows now without the user's name: the translation or the source. */
    fun base(key: String): String = if (language == AppLanguage.Russian) key else dictionary(language)[key] ?: key

    /** What [key] shows now. */
    fun shown(key: String): String = overrides[key] ?: base(key)

    fun t(text: String): String {
        val lang = language
        val own = overrides
        if (lang == AppLanguage.Russian && own.isEmpty()) return text
        if (text.length > MAX_LENGTH || !cyrillic.containsMatchIn(text)) return text
        synchronized(cache) { cache[text] }?.let { return it }
        val result = resolve(text, lang, own)
        synchronized(cache) { cache[text] = result }
        return result
    }

    private fun resolve(text: String, lang: AppLanguage, own: Map<String, String>): String {
        val dictionary = if (lang == AppLanguage.Russian) emptyMap() else dictionary(lang)
        own[text]?.let { return it }
        dictionary[text]?.let { return it }
        for ((regex, key) in templates) {
            val match = regex.matchEntire(text) ?: continue
            val target = own[key] ?: dictionary[key] ?: return text
            val values = match.groupValues.drop(1).iterator()
            return buildString {
                var rest = target
                while (true) {
                    val at = rest.indexOf("{}")
                    if (at < 0 || !values.hasNext()) { append(rest); break }
                    append(rest, 0, at).append(values.next())
                    rest = rest.substring(at + 2)
                }
            }
        }
        return text
    }

    private fun dictionary(lang: AppLanguage): Map<String, String> = synchronized(dictionaries) {
        dictionaries.getOrPut(lang) {
            if (lang == AppLanguage.Russian || !::appContext.isInitialized) emptyMap()
            else runCatching {
                val json = JSONObject(appContext.assets.open("i18n/${lang.code}.json").bufferedReader().use { it.readText() })
                json.keys().asSequence().associateWith { json.getString(it) }
            }.getOrDefault(emptyMap())
        }
    }

    /** Templates from every known key with {} and some text of its own; longer ones are tried first. */
    private fun rebuildTemplates() {
        templates = dictionary(AppLanguage.English).keys.filter { "{}" in it && it.replace("{}", "").trim().length >= MIN_TEMPLATE_TEXT }
            .sortedByDescending { it.replace("{}", "").length }
            .map { key -> Regex(key.split("{}").joinToString("(.+?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL) to key }
    }

    private fun save() {
        val array = org.json.JSONArray()
        packs.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("names", JSONObject(it.names))) }
        prefs.edit {
            putString(PACKS_KEY, array.toString())
            putString(ACTIVE_KEY, activePackId)
        }
        synchronized(cache) { cache.clear() }
    }

    private const val LANGUAGE_KEY = "language"
    /** Names before packs; read once and moved into a pack. */
    private const val OVERRIDES_KEY = "overrides"
    private const val PACKS_KEY = "packs"
    private const val ACTIVE_KEY = "active_pack"
    private const val TITLE_MAX = 40
    private const val NAME_MAX = 40
    private const val MAX_NAMES = 200
    private const val CACHE_SIZE = 3000
    private const val MAX_LENGTH = 400
    /** Short templates like «{} с» would catch ordinary words in chat titles and previews. */
    private const val MIN_TEMPLATE_TEXT = 3
}
