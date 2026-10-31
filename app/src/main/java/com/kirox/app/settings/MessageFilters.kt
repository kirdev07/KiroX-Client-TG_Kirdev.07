package com.kirox.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * «Фильтры сообщений»: in groups and channels, messages with chosen words or from chosen people are
 * folded away (a tap shows them). Private chats are never filtered. Kept on this phone only.
 */
data class Filters(val enabled: Boolean = false, val words: List<String> = emptyList(), val people: Map<Long, String> = emptyMap()) {
    /** Whether [text] from [senderId] is hidden by these filters. */
    fun hides(senderId: Long, text: String): Boolean {
        if (!enabled) return false
        if (senderId != 0L && senderId in people) return true
        return words.any { it.isNotBlank() && text.contains(it, ignoreCase = true) }
    }
}

object MessageFilters {
    const val PREFS = "message_filters"
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(Filters())
    val state: StateFlow<Filters> = _state.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = Filters(
            prefs.getBoolean("enabled", false),
            prefs.getString("words", "").orEmpty().split('\n').filter { it.isNotBlank() },
            runCatching { JSONObject(prefs.getString("people", "{}")!!).let { o -> o.keys().asSequence().associate { it.toLong() to o.getString(it) } } }.getOrDefault(emptyMap())
        )
    }

    fun update(value: Filters) {
        _state.value = value
        prefs.edit {
            putBoolean("enabled", value.enabled)
            putString("words", value.words.joinToString("\n"))
            putString("people", JSONObject(value.people.mapKeys { it.key.toString() }).toString())
        }
    }

    fun hidePerson(id: Long, name: String) = update(_state.value.copy(enabled = true, people = _state.value.people + (id to name)))
}
