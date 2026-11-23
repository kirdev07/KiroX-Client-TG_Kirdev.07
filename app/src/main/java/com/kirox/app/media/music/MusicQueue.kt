package com.kirox.app.media.music

import com.kirox.app.ui.chat.RepeatMode
import kotlin.random.Random

/**
 * The music queue as a pure value (unit-tested): [items] in play order and the [index] playing.
 * Shuffle reorders [items] itself (the current track first, the rest mixed) and keeps the order it
 * had in [original], so the queue view always shows what comes next and «перемешать» off restores
 * the old order. Reorder / remove keep the current track current.
 */
data class MusicQueue<T>(val items: List<T> = emptyList(), val index: Int = -1, val original: List<T>? = null) {
    val shuffled: Boolean get() = original != null
    val current: T? get() = items.getOrNull(index)
    val size: Int get() = items.size

    /**
     * The track after this one. [auto]: the track ended by itself — «повтор одного» plays it again;
     * a tap on «next» always moves on (and wraps around when any repeat is on). Null at the end.
     */
    fun nextIndex(repeat: RepeatMode, auto: Boolean): Int? {
        if (items.isEmpty()) return null
        if (auto && repeat == RepeatMode.One) return index.coerceIn(0, items.lastIndex)
        val next = index + 1
        return when {
            next in items.indices -> next
            repeat != RepeatMode.Off -> 0
            else -> null
        }
    }

    /** The track before this one; wraps to the last when repeat is on, null at the start otherwise. */
    fun previousIndex(repeat: RepeatMode): Int? {
        if (items.isEmpty()) return null
        val prev = index - 1
        return when {
            prev in items.indices -> prev
            repeat != RepeatMode.Off -> items.lastIndex
            else -> null
        }
    }

    fun jump(to: Int): MusicQueue<T> = if (to in items.indices) copy(index = to) else this

    /** Current track first, the others in random order; the order before is kept for [unshuffle]. */
    fun shuffle(random: Random = Random.Default): MusicQueue<T> {
        if (items.isEmpty()) return copy(original = original ?: items)
        val cur = current
        val rest = items.filterIndexed { i, _ -> i != index }.shuffled(random)
        val mixed = if (cur != null) listOf(cur) + rest else rest
        return MusicQueue(mixed, if (cur != null) 0 else -1, original ?: items)
    }

    /** Back to the order before [shuffle]; tracks added since go at the end, removed ones stay removed. */
    fun unshuffle(): MusicQueue<T> {
        val before = original ?: return this
        val left = items.toMutableList()
        val restored = ArrayList<T>(items.size)
        before.forEach { if (left.remove(it)) restored += it }
        restored += left
        val cur = current
        return MusicQueue(restored, if (cur == null) -1 else restored.indexOf(cur), null)
    }

    /** Moves the item at [from] to [to]; the playing track stays the playing one. */
    fun move(from: Int, to: Int): MusicQueue<T> {
        if (from !in items.indices || to !in items.indices || from == to) return this
        val list = items.toMutableList().apply { add(to, removeAt(from)) }
        val newIndex = when {
            index == from -> to
            from < index && to >= index -> index - 1
            from > index && to <= index -> index + 1
            else -> index
        }
        return copy(items = list, index = newIndex)
    }

    /**
     * Removes the item at [at]. When it is the playing one, [index] then points at the track that
     * followed it (or the new last one), which the player starts; -1 when the queue is empty.
     */
    fun remove(at: Int): MusicQueue<T> {
        if (at !in items.indices) return this
        val item = items[at]
        val list = items.toMutableList().apply { removeAt(at) }
        val newIndex = when {
            list.isEmpty() -> -1
            at < index -> index - 1
            at == index -> at.coerceAtMost(list.lastIndex)
            else -> index
        }
        return MusicQueue(list, newIndex, original?.toMutableList()?.apply { remove(item) })
    }

    /** Puts [item] right after the playing track (or plays it first in an empty queue). */
    fun playNext(item: T): MusicQueue<T> {
        val at = (index + 1).coerceIn(0, items.size)
        return copy(items = items.toMutableList().apply { add(at, item) }, index = if (index < 0) 0 else index,
            original = original?.plus(item))
    }

    /** The tracks after the current one, in play order (what the player can download ahead). */
    fun upcoming(repeat: RepeatMode, count: Int = 1): List<Int> {
        val out = ArrayList<Int>()
        var q = this
        for (k in 0 until count) {
            val n = q.nextIndex(repeat, auto = false) ?: return out
            if (n == index || n in out) return out
            out += n; q = q.copy(index = n)
        }
        return out
    }
}
