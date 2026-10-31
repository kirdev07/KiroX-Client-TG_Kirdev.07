package com.kirox.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A track in a playlist: the music message it comes from, with its name kept for showing offline. */
data class PlaylistItem(val chatId: Long, val messageId: Long, val title: String, val artist: String, val duration: Int)

data class Playlist(val id: Long, val name: String, val items: List<PlaylistItem>) {
    val totalSeconds: Int get() = items.sumOf { it.duration }
    fun has(chatId: Long, messageId: Long) = items.any { it.chatId == chatId && it.messageId == messageId }
}

/**
 * The user's playlists, on this phone only (they go into the settings backup). «Любимое» always
 * exists and is first; the heart in the player adds to it. Tracks point at messages, so the music
 * itself stays in Telegram.
 */
object Playlists {
    const val PREFS = "playlists"
    const val LIKED = 0L
    private const val LIKED_NAME = "Любимое"
    private lateinit var prefs: SharedPreferences
    private val _all = MutableStateFlow(listOf(Playlist(LIKED, LIKED_NAME, emptyList())))
    val all: StateFlow<List<Playlist>> = _all.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val read = runCatching { decode(prefs.getString("all", null) ?: "[]") }.getOrDefault(emptyList())
        _all.value = withLiked(read)
    }

    private fun withLiked(list: List<Playlist>) =
        if (list.any { it.id == LIKED }) list.sortedBy { if (it.id == LIKED) 0 else 1 } else listOf(Playlist(LIKED, LIKED_NAME, emptyList())) + list

    private fun save(list: List<Playlist>) {
        _all.value = withLiked(list)
        prefs.edit { putString("all", encode(_all.value)) }
    }

    fun get(id: Long) = _all.value.firstOrNull { it.id == id }

    /** A new empty playlist; returns its id. */
    fun create(name: String): Long {
        val id = (_all.value.maxOfOrNull { it.id } ?: 0) + 1
        save(_all.value + Playlist(id, name.trim().ifBlank { "Плейлист" }, emptyList()))
        return id
    }

    fun rename(id: Long, name: String) { if (id != LIKED && name.isNotBlank()) save(_all.value.map { if (it.id == id) it.copy(name = name.trim()) else it }) }

    fun delete(id: Long) { if (id != LIKED) save(_all.value.filterNot { it.id == id }) }

    fun add(id: Long, item: PlaylistItem) = save(_all.value.map { p ->
        if (p.id == id && !p.has(item.chatId, item.messageId)) p.copy(items = p.items + item) else p
    })

    fun remove(id: Long, chatId: Long, messageId: Long) = save(_all.value.map { p ->
        if (p.id == id) p.copy(items = p.items.filterNot { it.chatId == chatId && it.messageId == messageId }) else p
    })

    /** Moves the track at [from] by [delta] places. */
    fun move(id: Long, from: Int, delta: Int) = save(_all.value.map { p ->
        val to = from + delta
        if (p.id != id || from !in p.items.indices || to !in p.items.indices) p
        else p.copy(items = p.items.toMutableList().apply { add(to, removeAt(from)) })
    })

    fun isLiked(chatId: Long, messageId: Long) = get(LIKED)?.has(chatId, messageId) == true

    fun toggleLiked(item: PlaylistItem) = if (isLiked(item.chatId, item.messageId)) remove(LIKED, item.chatId, item.messageId) else add(LIKED, item)

    internal fun encode(list: List<Playlist>): String = JSONArray(list.map { p ->
        JSONObject().put("id", p.id).put("name", p.name).put("items", JSONArray(p.items.map { i ->
            JSONObject().put("c", i.chatId).put("m", i.messageId).put("t", i.title).put("a", i.artist).put("d", i.duration)
        }))
    }).toString()

    internal fun decode(json: String): List<Playlist> {
        val array = JSONArray(json)
        return (0 until array.length()).map { n ->
            val p = array.getJSONObject(n)
            val items = p.optJSONArray("items") ?: JSONArray()
            Playlist(p.getLong("id"), p.getString("name"), (0 until items.length()).map { k ->
                val i = items.getJSONObject(k)
                PlaylistItem(i.getLong("c"), i.getLong("m"), i.optString("t"), i.optString("a"), i.optInt("d"))
            })
        }
    }
}
