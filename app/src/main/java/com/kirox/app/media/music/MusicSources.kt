package com.kirox.app.media.music

import android.content.Context
import androidx.core.content.edit

/** What kind of chat a music source is. */
enum class MusicSourceKind { Channel, Group, Saved, Private }

/** A chat with music: its [audioCount] comes from Telegram's audio search counter. */
data class MusicSource(val chatId: Long, val title: String, val kind: MusicSourceKind, val audioCount: Int)

/**
 * Which of the user's chats are «music channels» (unit-tested). A channel counts from a few tracks,
 * a group only when music is clearly what it is about, Saved Messages from one track; private chats
 * only when the user adds them. The user's own choice wins: added chats always show, removed never.
 */
object MusicSourceRules {
    const val CHANNEL_MIN = 3
    const val GROUP_MIN = 10

    fun isMusicSource(kind: MusicSourceKind, audioCount: Int): Boolean = when (kind) {
        MusicSourceKind.Channel -> audioCount >= CHANNEL_MIN
        MusicSourceKind.Group -> audioCount >= GROUP_MIN
        MusicSourceKind.Saved -> audioCount >= 1
        MusicSourceKind.Private -> false
    }

    /**
     * The sources to show: detected ones not removed, plus the ones the user added (from [known],
     * which has the last counts of every chat seen). Added first, then by number of tracks.
     */
    fun visible(detected: List<MusicSource>, added: List<Long>, removed: Set<Long>, known: Map<Long, MusicSource>): List<MusicSource> {
        val byId = LinkedHashMap<Long, MusicSource>()
        added.forEach { id -> if (id !in removed) (known[id] ?: detected.firstOrNull { it.chatId == id })?.let { byId[id] = it } }
        detected.filter { it.chatId !in removed && it.chatId !in byId && isMusicSource(it.kind, it.audioCount) }
            .sortedWith(compareByDescending<MusicSource> { it.audioCount }.thenBy { it.title.lowercase() })
            .forEach { byId[it.chatId] = it }
        return byId.values.toList()
    }

    /** Sources as text for the preferences: one per line, `id\tkind\tcount\ttitle`. */
    fun encode(list: List<MusicSource>): String = list.joinToString("\n") { s ->
        "${s.chatId}\t${s.kind.name}\t${s.audioCount}\t${s.title.replace('\n', ' ').replace('\t', ' ')}"
    }

    fun decode(text: String?): List<MusicSource> = text.orEmpty().lineSequence().mapNotNull { line ->
        val p = line.split('\t', limit = 4)
        if (p.size < 4) return@mapNotNull null
        val id = p[0].toLongOrNull() ?: return@mapNotNull null
        val kind = MusicSourceKind.entries.firstOrNull { it.name == p[1] } ?: return@mapNotNull null
        MusicSource(id, p[3], kind, p[2].toIntOrNull() ?: 0)
    }.toList()
}

/**
 * The music sources on this phone: the last scan of the user's chats, chats the user added and
 * chats the user removed. Plain shared preferences («music_sources»).
 */
class MusicSourceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music_sources", Context.MODE_PRIVATE)

    var scanned: List<MusicSource>
        get() = MusicSourceRules.decode(prefs.getString("scanned", null))
        set(value) = prefs.edit { putString("scanned", MusicSourceRules.encode(value)); putLong("scannedAt", System.currentTimeMillis()) }

    val scannedAt: Long get() = prefs.getLong("scannedAt", 0L)

    /** Chats the user added by hand, with their last known counts. */
    var added: List<MusicSource>
        get() = MusicSourceRules.decode(prefs.getString("added", null))
        set(value) = prefs.edit { putString("added", MusicSourceRules.encode(value)) }

    var removed: Set<Long>
        get() = prefs.getStringSet("removed", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        set(value) = prefs.edit { putStringSet("removed", value.map { it.toString() }.toSet()) }
}
