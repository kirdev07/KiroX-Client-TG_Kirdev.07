package com.kirox.app.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Interface languages. Russian is the source text, the others come from assets/i18n/<code>.json. */
enum class AppLanguage(val code: String, val title: String) { Russian("ru", "Русский"), English("en", "English") }

/**
 * Text engine of kiroX. Every text the app shows passes through [t] (the app's own Text does it):
 * the Russian source text is the key, so nothing in the screens needs ids.
 *  - a translation for the chosen [language] replaces it;
 *  - the user's own name for it ([overrides], «Свои названия») wins over both, in any language.
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
    var overrides by mutableStateOf<Map<String, String>>(emptyMap())
        private set

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
        overrides = prefs.getString(OVERRIDES_KEY, null)?.let { json ->
            runCatching { JSONObject(json).let { o -> o.keys().asSequence().associateWith { o.getString(it) } } }.getOrNull()
        }.orEmpty()
        rebuildTemplates()
    }

    /** All texts that can be renamed, in the order of the source; from the English dictionary. */
    val keys: List<String> get() = dictionary(AppLanguage.English).keys.toList()

    fun chooseLanguage(value: AppLanguage) {
        language = value
        prefs.edit().putString(LANGUAGE_KEY, value.code).apply()
        synchronized(cache) { cache.clear() }
    }

    /** Own name for [key]; null or blank brings the normal text back. */
    fun setOverride(key: String, value: String?) {
        overrides = if (value.isNullOrBlank()) overrides - key else overrides + (key to value)
        save()
    }

    fun clearOverrides() { overrides = emptyMap(); save() }

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
        prefs.edit().putString(OVERRIDES_KEY, JSONObject(overrides).toString()).apply()
        synchronized(cache) { cache.clear() }
    }

    private const val LANGUAGE_KEY = "language"
    private const val OVERRIDES_KEY = "overrides"
    private const val CACHE_SIZE = 3000
    private const val MAX_LENGTH = 400
    /** Short templates like «{} с» would catch ordinary words in chat titles and previews. */
    private const val MIN_TEMPLATE_TEXT = 3
}
